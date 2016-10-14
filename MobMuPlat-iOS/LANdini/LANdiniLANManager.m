#import "LANdiniLANManager.h"

#import "MMPNetworkingUtils.h"

#define SC_DEFAULT_PORT 57120

static const NSTimeInterval LANdiniLanCheckInterval = 0.5;
static const NSTimeInterval LANdiniDropUserInterval = 2.0;
static const NSTimeInterval LANdiniCheckUserInterval = 0.3;
static const NSTimeInterval LANdiniBroadcastInterval = 1.0;
static const NSTimeInterval LANdiniPingInterval = 0.33;
static const NSTimeInterval LANdiniSyncRequestInterval = 0.33;

@interface LANdiniLANManager () {
  BOOL _connected;
  NSMutableArray *_userList;

  int _toLocalPort;
  int _fromLocalPort;

  float _version;
  BOOL _iHaveBeenWarned;

  NSTimer *_connectionTimer;
  NSTimer *_broadcastTimer;
  NSTimer *_dropUserTimer;
  NSTimer *_pingAndMsgIDsTimer;
  NSTimer *_syncTimer;
  NSMutableDictionary<NSString*, NSString *> *_apiResponders;
  NSMutableDictionary<NSString*, NSString *> *_networkResponders;

  NSString *_syncServerName;
  BOOL _inSync;
  NSTimeInterval _smallestRtt; // Smallest round-trip time.
  NSTimeInterval _adjustmentToGetNetworkTime;

  // For network time.
  NSDate *_startDate;

  OSCManager *_oscManager;
  OSCOutPort *_broadcastAppAddr;
  OSCOutPort *_targetAppAddr;
  OSCInPort *_inPortLocal;
  OSCInPort *_inPortNetwork;
}

@end


@implementation LANdiniLANManager

+ (OSCMessage*)OSCMessageFromArray:(NSArray *)vals {
  OSCMessage *msg = [OSCMessage createWithAddress:(NSString *)vals[0]];
  // Parse array elements int strings or float/int.
  for (id item in [vals subarrayWithRange:NSMakeRange(1, [vals count] - 1)]) {
    if ([item isKindOfClass:[NSString class]]) {
      [msg addString:item];
    } else if ([item isKindOfClass:[NSNumber class]]) {
      if (CFNumberIsFloatType((CFNumberRef)item)) {
        [msg addFloat:[item floatValue]];
      } else {
        [msg addInt:[item intValue]];
      }
    }
  }
  return msg;
}

- (id)init{
  self = [super init];
  if (self) {
    _userList = [[NSMutableArray alloc]init];
    _toLocalPort = 50505;
    _fromLocalPort = 50506;
    _smallestRtt = 1;

    _startDate = [NSDate date];
    _version = .22;

    _syncServerName = @"noSyncServer";

    _oscManager = [[OSCManager alloc] init];
    [_oscManager setDelegate:self];
  }
  return self;
}

- (void)sendMsgToApp:(NSArray *)msgArray{
  OSCMessage *msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
  [_targetAppAddr sendThisPacket:[OSCPacket createWithContent:msg]];
  //TODO just use PdBase sendList
  //[PdBase sendList:msgArray ];
  [self.logDelegate logMsgInput:msgArray];
}

- (void)setEnabled:(BOOL)enabled {
  if (enabled == _enabled) {
    return;
  }
  _enabled = enabled;

  if (_enabled) {
    [self connectOSC];
  } else {
    [self disconnectOSC];
  }
}

- (void)connectOSC{
  _targetAppAddr = [_oscManager createNewOutputToAddress:@"127.0.0.1" atPort:_toLocalPort];
  _broadcastAppAddr = [_oscManager createNewOutputToAddress:@"224.0.0.1" atPort:SC_DEFAULT_PORT];
  // network responders from other landinis:
  _inPortNetwork = [_oscManager createNewInputForPort:SC_DEFAULT_PORT];
  // api responders from local user app:
  _inPortLocal = [_oscManager createNewInputForPort:_fromLocalPort];

  [self checkForLAN];
}

- (void)disconnectOSC {
  [_userList removeAllObjects];
  [self receiveGetNamesRequest];
  [self receiveGetNumUsersRequest];
  [self.userDelegate landiniUserStateChanged:_userList];

  [_oscManager deleteAllInputs];
  [_oscManager deleteAllOutputs];
  _inPortNetwork = nil;
  _inPortLocal = nil;
  _targetAppAddr = nil;
  _broadcastAppAddr = nil;
  _syncServerName = @"noSyncServer";

  //if backgrounded when looking for LAN, then that timer keeps firing and overlaps with its
  // re-creation to init the LAN twice, adding "me" twice. This attempts to prevent that.
  if (_connectionTimer != nil) {
    [_connectionTimer invalidate];
    _connectionTimer = nil;
  }
}

- (NSTimeInterval) elapsedTime {
  return fabs( [_startDate timeIntervalSinceNow] );
}

- (void)checkForLAN {
  if (!_connectionTimer) {
    _connectionTimer = [NSTimer scheduledTimerWithTimeInterval:LANdiniLanCheckInterval
                                                        target:self
                                                      selector:@selector(connectionTimerMethod:)
                                                      userInfo:nil
                                                       repeats:YES];
  }
}

- (void)connectionTimerMethod:(NSTimer *)timer {
  NSString *address = [MMPNetworkingUtils ipAddress];
  if (address) {
    [_connectionTimer invalidate];
    _connectionTimer = nil;
    [self initLAN:address];
  } else {
    NSLog(@"Still looking for a LAN...");
  }
}

- (void)initLAN:(NSString *)address {
  [self.userDelegate syncServerChanged:_syncServerName];

  NSString *myIP = address;
  int myPort = SC_DEFAULT_PORT; // supercollider default port
  NSString *myName = [[UIDevice currentDevice] name];

  // rare cases of double adding on to/from background, so take "me" out before adding.
  LANdiniUser *findMe = [self userInUserListWithName:myName]; //TODO name collisions??
  if (findMe) {
    [_userList removeObject:findMe];
  }

  _me = [[LANdiniUser alloc] initWithName:myName IP:myIP port:myPort network:self];
  _connected = YES;
  [_userList addObject:_me];

  [self.userDelegate landiniUserStateChanged:_userList];
  [self receiveGetNamesRequest];
  [self receiveGetNumUsersRequest];

  [self.logDelegate logLANdiniOutput:
      @[ [NSString stringWithFormat:@"connected to LAN at %@ on port %d", _me.ip, _me.port] ] ];

  static dispatch_once_t once;
  dispatch_once(&once, ^{
    NSLog(@"got to the responders");
    [self setupAPIResponders];
    NSLog(@"got to the network responders");
    [self setUpNetworkResponders];
    NSLog(@"got to broadcast task");
    [self startBroadcastTimer];
    NSLog(@"got to ping task");
    [self startPingAndMsgIDsTimer];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
      NSLog(@"starting drop user task");
      [self startDropUserTimer];
    });
  });
}

- (void)setupAPIResponders {
  _apiResponders = [[NSMutableDictionary alloc]init];

  // Map addresses to selectors.
  [_apiResponders setObject:NSStringFromSelector(@selector(processApiMsg:)) forKey:@"/send"];
  [_apiResponders setObject:NSStringFromSelector(@selector(processApiMsg:)) forKey:@"/send/GD"];
  [_apiResponders setObject:NSStringFromSelector(@selector(processApiMsg:)) forKey:@"/send/OGD"];
  [_apiResponders setObject:NSStringFromSelector(@selector(receiveGetNumUsersRequest))
                     forKey:@"/numUsers"];
  [_apiResponders setObject:NSStringFromSelector(@selector(receiveGetNamesRequest))
                     forKey:@"/userNames"];
  // Note: "stagemap" is unimplemented.
  [_apiResponders setObject:NSStringFromSelector(@selector(receiveGetNetworkTimeRequest))
                     forKey:@"/networkTime"];
  [_apiResponders setObject:NSStringFromSelector(@selector(receiveMyNameRequest))
                     forKey:@"/myName"];

  NSLog(@"setup %lu new api responders", (unsigned long)[_apiResponders count]);
}

- (void)receiveMyNameRequest {
  //output message format:
  // '/landini/myName', me.name

  [self sendMsgToApp:@[ @"/landini/myName", _me.name ]];
}

- (void)receiveGetNamesRequest {
  //output message format:
  // '/landini/userNames, <list of user names>...

  //Note difference from super collider version of landini:
  //this returns a list of _all_ users (including me), not all _but_ me

  NSMutableArray *msgArray = [[NSMutableArray alloc]init];
  [msgArray addObject:@"/landini/userNames"];
  for (LANdiniUser *user in _userList) {
    [msgArray addObject:user.name];
  }

  [self sendMsgToApp:msgArray];
}

- (void)receiveGetNumUsersRequest {
  //output message format:
  // '/landini/numUsers', <number of users in userlist>

  [self sendMsgToApp:@[ @"/landini/numUsers", @([_userList count]) ]];
}

//omit receive_get_stage_map_request

- (void)receiveGetNetworkTimeRequest {
  //output message format:
  // '/landini/networkTime, <double: time elapsed in seconds>

  NSTimeInterval time;
  if (_inSync) {
    time = [self networkTime];
  } else {
    time = -1;
  }
  [self sendMsgToApp:@[ @"/landini/networkTime", @(time) ]];
}

- (void)processApiMsg:(NSArray*)msgArray {
  //NSLog(@"process: %@", msgArray);
  //input message format:
  // protocol ("/send, /sendGD, /sendOGD"),
  // name of recipient (or "all"/"allButMe"),
  // user msg address
  // user msg values......

  //check first two are strings
  if (msgArray.count < 2 ||
      ![msgArray[0] isKindOfClass:[NSString class]] ||
      ![msgArray[1] isKindOfClass:[NSString class]]) {
    return;
  }

  NSString *protocol = msgArray[0];
  NSString *name = msgArray[1];  // LANDini user name or "all"/"allButMe"

  // Strip off first two elements, leaving user message address and values
  NSArray *msgArray2 = [msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)];

  if ([name isEqualToString:@"all"]) {
    if ([protocol isEqualToString:@"/send"]) {
      for (LANdiniUser *user in _userList) {
        [self sendUser:user msg:msgArray2];
      }
    } else if ([protocol isEqualToString:@"/send/GD"]) {
      for (LANdiniUser *user in _userList) {
        [self sendGD:user msg:msgArray2];
      }
    } else if ([protocol isEqualToString:@"/send/OGD"]) {
      for (LANdiniUser *user in _userList) {
        [self sendOGDUser:user msg:msgArray2];
      }
    }
  } else if ([name isEqualToString:@"allButMe"]) {
    if ([protocol isEqualToString:@"/send"]) {
      for (LANdiniUser *user in _userList) {
        if (user!=_me) {
          [self sendUser:user msg:msgArray2];
        }
      }
    } else if ([protocol isEqualToString:@"/send/GD"]) {
      for (LANdiniUser *user in _userList) {
        if (user!=_me) {
          [self sendGD:user msg:msgArray2];
        }
      }
    } else if ([protocol isEqualToString:@"/send/OGD"]) {
      for (LANdiniUser *user in _userList) {
        if (user!=_me) {
          [self sendOGDUser:user msg:msgArray2];
        }
      }
    }
  } else { //user name
    LANdiniUser *usr = [self userInUserListWithName:name];
    if (!usr) {
      [self.logDelegate logLANdiniOutput:
          @[ [NSString stringWithFormat:@"error: sending to bad user name %@", name] ] ];
      return;
    }

    if ([protocol isEqualToString:@"/send"]) {
      [self sendUser:usr msg:msgArray2];
    } else if ([protocol isEqualToString:@"/send/GD"]) {
      [self sendGD:usr msg:msgArray2];
    } else if ([protocol isEqualToString:@"/send/OGD"]) {
      [self sendOGDUser:usr msg:msgArray2];
    }
  }
}

//====network stuff
// responders for osc messages coming from other copies of LANdiniOSC on the network

- (void)setUpNetworkResponders {
  _networkResponders = [[NSMutableDictionary alloc]init];

  //map addresses to selectors - am assuming this is faster than a switch statement
  [_networkResponders setObject:NSStringFromSelector(@selector(receiveMemberBroadcast:))
                         forKey:@"/landini/member/broadcast"];
  [_networkResponders setObject:NSStringFromSelector(@selector(receiveMemberReply:))
                         forKey:@"/landini/member/reply"];
  [_networkResponders setObject:NSStringFromSelector(@selector(receivePingAndMsgIDs:))
                         forKey:@"/landini/member/ping_and_msg_IDs"];
  [_networkResponders setObject:NSStringFromSelector(@selector(receiveMsg:))
                         forKey:@"/landini/msg"];
  [_networkResponders setObject:NSStringFromSelector(@selector(receiveGD:))
                         forKey:@"/landini/msg/GD"];
  [_networkResponders setObject:NSStringFromSelector(@selector(receiveOGD:))
                         forKey:@"/landini/msg/OGD"];

  [_networkResponders setObject:NSStringFromSelector(@selector(receiveMissingGDRequest:))
                         forKey:@"/landini/request/missing/GD"];
  [_networkResponders setObject:NSStringFromSelector(@selector(receiveMissingOGDRequest:))
                         forKey:@"/landini/request/missing/OGD"];

  //Note "/landini/sync/new_server" unimplemented in original, and unimplemented here.

  [_networkResponders setObject:NSStringFromSelector(@selector(receiveSyncRequest:))
                         forKey:@"/landini/sync/request"];
  [_networkResponders setObject:NSStringFromSelector(@selector(receiveSyncReply:))
                         forKey:@"/landini/sync/reply"];

  //Note "/landini/sync/stop" and "/landini/sync/got_stop" unimplemented in original, and
  // unimplemented here

  NSLog(@"setup %lu new network responders", (unsigned long)[_networkResponders count]);
}

#pragma mark OSCManager delegate

- (void)receivedOSCMessage:(OSCMessage *)m{
  NSString *address = [m address];

  //look in both network responders and API responders
  NSString *selectorString = [_networkResponders objectForKey:address];//look in network responders

  if (!selectorString) {//if not found, look again in apiresponders
    selectorString = [_apiResponders objectForKey:address];
  }

  //if selector still not found, will be passed to sendMsgToApp below
  SEL aSelector = NSSelectorFromString(selectorString);

  //create blank message array for sending to pd
  NSMutableArray *msgArray = [[NSMutableArray alloc]init];
  NSMutableArray *tempOSCValueArray = [[NSMutableArray alloc]init];

  //VV library handles receiving a value confusingly: if just one value, it has a single value in message "m" and no valueArray, if more than one value, it has valuearray. here we just shove either into tempOSCValueArray to iterate over

  if ([m valueCount] == 1) {
    [tempOSCValueArray addObject:[m value]];
  } else {
    for (OSCValue *val in [m valueArray]) {
      [tempOSCValueArray addObject:val];
    }
  }

  //first element in msgArray is address
  [msgArray addObject:address];

  //then iterate over all values
  for (OSCValue *val in tempOSCValueArray) {//unpack OSC value to NSNumber or NSString
    if ([val type] == OSCValInt) {
      [msgArray addObject:@([val intValue])];
    } else if ([val type] == OSCValFloat) {
      [msgArray addObject:@([val floatValue])];
    } else if ([val type] == OSCValString) {
      [msgArray addObject:[val stringValue]];
    }
  }

  if (selectorString) {
    //has a potential memory leak warning: just don't return any retained objects with the selector, which isn't an issue since all our selectors return void
    [self performSelector:aSelector withObject:msgArray];
  } else { // no landini process, just send into app
    [self sendMsgToApp:msgArray];
  }

  [self.logDelegate logLANdiniInput:msgArray];
}

// broadcast stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

- (void)startBroadcastTimer {
  if (!_broadcastTimer) {
    _broadcastTimer = [NSTimer scheduledTimerWithTimeInterval:LANdiniBroadcastInterval
                                                       target:self
                                                     selector:@selector(broadcastTimerMethod:)
                                                     userInfo:nil
                                                      repeats:YES];
  }
}

- (void)broadcastTimerMethod:(NSTimer *)timer {
  NSArray *msgArray = @[ @"/landini/member/broadcast",
                         _me.name,
                         _me.ip,
                         @(_me.port),
                         @(_version) ];
  [self broadcastMsg:msgArray];
}

- (void)broadcastMsg:(NSArray *)msgArray {
  OSCMessage *msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
  [_broadcastAppAddr sendThisPacket:[OSCPacket createWithContent:msg]];
  [self.logDelegate logLANdiniOutput:msgArray];
}


- (void)receiveMemberBroadcast:(NSArray *)msgArray {
  //input message format:
  //address ("/landini/member/broadcast/")
  //member name
  //member IP (NSString)
  //member port (NSNumber)
  //version (NSNumber)

  //check types
  if ([msgArray count] < 5  ||
     ![msgArray[0] isKindOfClass:[NSString class]] ||
     ![msgArray[1] isKindOfClass:[NSString class]] ||
     ![msgArray[2] isKindOfClass:[NSString class]] ||
     ![msgArray[3] isKindOfClass:[NSNumber class]] ||
     ![msgArray[4] isKindOfClass:[NSNumber class]] ) {
    return;
  }

  NSString *theirName = msgArray[1];
  NSString *theirIP = msgArray[2];
  NSNumber *theirPortNumber = msgArray[3];
  int theirPort = [theirPortNumber intValue];
  NSNumber *theirVersionNumber = msgArray[4];
  float theirVersion = [theirVersionNumber floatValue];


  if ((theirVersion > _version + FLT_EPSILON) && (!_iHaveBeenWarned)) {
    _iHaveBeenWarned = YES;
    dispatch_async(dispatch_get_main_queue(), ^{
      NSString *messageString =
          [NSString stringWithFormat:@"You are not using the latest version of LANdini.\nYou are "
           "using version %.2f,\nand someone else is using version %.2f", _version, theirVersion];
      UIAlertView *alert =
          [[UIAlertView alloc] initWithTitle:@"Warning"
                                     message:messageString
                                    delegate:nil
                           cancelButtonTitle:@"OK"
                           otherButtonTitles:nil];
      [alert show];
    });
  }

  NSMutableArray *replyMsgArray = [NSMutableArray array];
  [replyMsgArray addObject:@"/landini/member/reply"];

  LANdiniUser *fromUsr = nil;

  if (![theirIP isEqualToString:_me.ip]) { // if received from someone other than me.
    //optimize
    for (LANdiniUser *user in _userList) {
      if ([user.ip isEqualToString:theirIP]) {
        fromUsr = user;
        break;
      }
    }
    if (!fromUsr) { //no user found, create and add to user list
      fromUsr = [self assimilateMemberInfoName:theirName IP:theirIP port:theirPort];
    }
    //WAS sending triplets of connected user data
    /*for (LANdiniUser *user in _userList) {
     [replyMsg addObject:user.name];
     [replyMsg addObject:user.ip];
     [replyMsg addObject:[NSNumber numberWithInt:user.port]];
     }*/
    //NOW sending just my info
    [replyMsgArray addObject:_me.name];
    [replyMsgArray addObject:_me.ip];
    [replyMsgArray addObject:@(_me.port)];
    [[fromUsr addr] sendThisPacket:
        [OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:replyMsgArray]]];
  }
}

- (void)receiveMemberReply:(NSArray *)addrs {
  // Unimplemented. Supercollider version receives list of other players that other member knows
  // about and assimilates them. But in this case, just handle those players when I receive their
  // broadcast.
}

// ping and msg ID stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

- (void)startPingAndMsgIDsTimer{
  if (!_pingAndMsgIDsTimer) {
    _pingAndMsgIDsTimer = [NSTimer scheduledTimerWithTimeInterval:LANdiniPingInterval
                                                           target:self
                                                         selector:@selector(pingTimerMethod:)
                                                         userInfo:nil
                                                          repeats:YES];
  }
}

- (void)pingTimerMethod:(NSTimer*)timer{
  for (LANdiniUser *user in _userList) {
    if (user!=_me) {
      NSArray *msgArray = @[ @"/landini/member/ping_and_msg_IDs",
                             _me.name,
                             @(0),//x position unimplemented
                             @(0),//y position unimplemented
                             @(user.lastOutgoingGDID),
                             @(user.minGDID),
                             @(user.lastOutgoingOGDID),
                             @(user.lastPerformedOGDID),
                             _syncServerName ];

      [[user addr] sendThisPacket:
          [OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:msgArray]]];
    }
  }
}

- (void)receivePingAndMsgIDs:(NSArray *)msgArray {
  //input format:
  //  landini address (NSString)
  //  name (NSString)
  //  xpos (NSNumber)
  //  ypos (NSNumber)
  //  lastGDID (NSNumber)
  //  theirMinGD (NSNumber)
  //  lastOGDID (NSNumber)
  //  lastPerformedOGDID (NSNumber)
  //  syncServerPingName (NSString)

  if (msgArray.count < 9 ||
     ![msgArray[1] isKindOfClass:[NSString class]] ||
     ![msgArray[8] isKindOfClass:[NSString class]]) {
    return;
  }

  NSString *name = msgArray[1];
  LANdiniUser *usr = [self userInUserListWithName:name];
  if (usr) { //found user
    //output format to receivePing method:user info
    NSArray *userArray = [msgArray subarrayWithRange:NSMakeRange(2, 6)];//xpos, ypos, ID values...
    [usr receivePing:userArray];
  }

  NSString *syncServerPingName = msgArray[8];
  if ([syncServerPingName isEqualToString:@"noSyncServer"] ||
     ![syncServerPingName isEqualToString:_syncServerName] ) {
    [self dealWithNewSyncServerName:syncServerPingName];
  }
}

// group stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

- (LANdiniUser *)assimilateMemberInfoName:(NSString *)name IP:(NSString *)ip port:(int)port {
  LANdiniUser *usr = [self userInUserListWithName:name];
  //Note, not in original supercollider source: check for existence of user in list by name first.
  if (!usr) { //user not found
    usr = [[LANdiniUser alloc] initWithName:name IP:ip port:port network:self];
    [_userList addObject:usr];
    NSLog(@"added user %@", usr.name);

    //Note: next three lines not in original supercollider source:
    // send new message to user app with names
    [self receiveGetNamesRequest];
    [self receiveGetNumUsersRequest];
    [self.userDelegate landiniUserStateChanged:_userList];
  }
  return usr;
}

- (void)startDropUserTimer {
  if (!_dropUserTimer) {
    _dropUserTimer = [NSTimer scheduledTimerWithTimeInterval:LANdiniCheckUserInterval
                                                      target:self
                                                    selector:@selector(startDropUserTimerMethod:)
                                                    userInfo:nil
                                                     repeats:YES ];
  }
}

- (void)startDropUserTimerMethod:(NSTimer *)timer {
  NSMutableArray *usersToDrop = [NSMutableArray array];
  for (LANdiniUser *user in _userList) {
    if (user != _me) {
      if ([self elapsedTime] - user.lastPing > LANdiniDropUserInterval) {
        [usersToDrop addObject:user];
      }
    }
  }
  for (LANdiniUser *user in usersToDrop) {
    NSLog(@"dropped user %@ - my time %.2f userlastping time %.2f",
          user.name, [self elapsedTime], user.lastPing);
    if ([user.name isEqualToString:_syncServerName]) {
      [self stopSyncTimer];
      [self resetSyncVars];
    }
    [_userList removeObject:user];
    //Note, not in original supercollider: send the user client new users list
    [self receiveGetNamesRequest];
    [self receiveGetNumUsersRequest];
    [self.userDelegate landiniUserStateChanged:_userList];
  }
}

// sync stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

- (NSTimeInterval)networkTime {
  return [self elapsedTime] + _adjustmentToGetNetworkTime;
}

- (void)becomeSyncServer {
  NSLog(@"becoming sync server!");
  [self stopSyncTimer];
  _syncServerName = _me.name;
  _adjustmentToGetNetworkTime = 0;
  _inSync = YES;
  [self.userDelegate syncServerChanged:_me.name];
}

- (void)dealWithNewSyncServerName:(NSString *)newName {
  if ([newName isEqualToString:@"noSyncServer"]) {
    NSLog(@"pinged sync server name is noSyncServer");
    NSMutableArray *namesArray = [[NSMutableArray alloc]init];
    for (LANdiniUser *user in _userList) {
      [namesArray addObject:user.name];
    }

    [namesArray sortUsingSelector:@selector(compare:)]; //sort by string
    NSLog(@"here's allNames: %@", namesArray);
    if (namesArray.count > 0 && [namesArray[0] isEqualToString:_me.name]) {
      if (![_syncServerName isEqualToString:_me.name]) {
        [self becomeSyncServer];
      } else{
        NSLog(@"i am already the sync server");
      }
    }
  } else {
    LANdiniUser *user = [self userInUserListWithName:newName];
    if (user) { //if user found
      _syncServerName = newName;
      [self.userDelegate syncServerChanged:newName];
    }

    [self performSelectorOnMainThread:@selector(startSyncTimer) withObject:nil waitUntilDone:NO];
  }
}

- (void)resetSyncVars{
  _adjustmentToGetNetworkTime = 0;
  _inSync = NO;
  _syncServerName = @"noSyncServer";
  _smallestRtt = 1;
  [self.userDelegate syncServerChanged:_syncServerName];
}

//i get this when I am the time server, I should not be getting messages from me.
- (void)receiveSyncRequest:(NSArray*)msgArray{
  //input format:
  // landini address
  // user name
  // user time

  if (msgArray.count < 3 ||
     ![msgArray[1] isKindOfClass:[NSString class]] ||
     ![msgArray[2] isKindOfClass:[NSNumber class]]) {
    return;
  }

  NSString *theirName = msgArray[1];
  NSNumber *theirTimeNumber = msgArray[2];

  LANdiniUser *usr = [self userInUserListWithName:theirName];
  if (usr) {
    if (![usr.name isEqualToString:_me.name]) {
      NSArray *msgArray2 = @[ @"/landini/sync/reply",
                              _me.name,
                              theirTimeNumber,
                              @([self elapsedTime]) ];

      OSCMessage *msg = [LANdiniLANManager OSCMessageFromArray:msgArray2];
      [[usr addr] sendThisPacket:[OSCPacket createWithContent:msg]];
    } else {
      NSLog(@"i should not be sending myself sync requests");
      [self stopSyncTimer];
    }
  } else {
    NSLog(@"time server is not in the userlist");
    [self stopSyncTimer];
    [self resetSyncVars];
  }
}

- (void)receiveSyncReply:(NSArray *)msgArray {//address, etc
  if (msgArray.count < 4 ||
      ![msgArray[1] isKindOfClass:[NSString class]] ||
      ![msgArray[2] isKindOfClass:[NSNumber class]] ||
      ![msgArray[3] isKindOfClass:[NSNumber class]]) {
    return;
  }

  NSString *timeServerName = msgArray[1];
  NSTimeInterval myOldTime = [msgArray[2] doubleValue];
  NSTimeInterval timeServerTime = [msgArray[3] doubleValue];

  LANdiniUser *usr = [self userInUserListWithName:timeServerName];

  if (usr && [timeServerName isEqualToString:_syncServerName]) {
    _inSync = YES;
    NSTimeInterval now = [self elapsedTime];
    NSTimeInterval rtt = now-myOldTime;
    _smallestRtt = MIN(_smallestRtt, rtt);
    NSTimeInterval serverTime = timeServerTime + (_smallestRtt/2);
    _adjustmentToGetNetworkTime = serverTime - now;
  } else {
    NSLog(@"stopping sync task because of sync server name discrepancy");
    [self stopSyncTimer];
    [self resetSyncVars];
  }
}

- (void)startSyncTimer {
  if (!_syncTimer) {
    _syncTimer = [NSTimer scheduledTimerWithTimeInterval:LANdiniSyncRequestInterval
                                                  target:self
                                                selector:@selector(syncTimerMethod:)
                                                userInfo:nil
                                                 repeats:YES];
  }
}

- (void)syncTimerMethod:(NSTimer *)timer {
  LANdiniUser *server = [self userInUserListWithName:_syncServerName];
  if (server && server != _me) {//should this be sent even if I am server?
    OSCMessage *msg = [OSCMessage createWithAddress:@"/landini/sync/request"];
    [msg addString:_me.name];
    [msg addFloat:[self elapsedTime]];
    [[server addr] sendThisPacket:[OSCPacket createWithContent:msg]];
  }
}

- (void)stopSyncTimer {
  if (_syncTimer) {
    [_syncTimer invalidate];
    _syncTimer = nil;
  }
}

//===normal send methods

- (void)sendUser:(LANdiniUser*)user msg:(NSArray *)msg {
  if (user) {
    [user sendMsg:msg];
  }
}

- (void) receiveMsg:(NSArray *)msgArray {
  //api address, user name, user msg addess, user args...
  if (msgArray.count < 3 || ![msgArray[1] isKindOfClass:[NSString class]]) {
    return;
  }

  NSString *fromName = msgArray[1];
  LANdiniUser *user = [self userInUserListWithName:fromName];
  if (user) {
    //strip from, name.
    [user receiveMsg:[msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)]];
  }
}

//====GD methods

- (void)sendGD:(LANdiniUser *)user msg:(NSArray *)msg{ //TODO should match others, i.e. sendGDUser
  if (user) {
    [user sendGD:msg];
  }
}

- (void)receiveGD:(NSArray *)msgArray {
  // landini network api address, from user name, ID, user address, user vals
  if (msgArray.count < 4 ||
      ![msgArray[1] isKindOfClass:[NSString class]] ||
      ![msgArray[2] isKindOfClass:[NSNumber class]]) {
    return;
  }

  NSString *fromName = msgArray[1];
  LANdiniUser *usr = [self userInUserListWithName:fromName];
  if (usr) {
    NSNumber *idNumber = msgArray[2];
    [usr receiveGD:idNumber msg:[msgArray subarrayWithRange:NSMakeRange(3, [msgArray count]-3)]];
  }
}

- (void)receiveMissingGDRequest:(NSArray *)msgArray {
  if (msgArray.count < 3 || ![msgArray[1] isKindOfClass:[NSString class]]) {
    return;
  }

  NSString *fromName = [msgArray objectAtIndex:1];
  LANdiniUser *usr = [self userInUserListWithName:fromName];
  if (usr) {
    [usr receiveMissingGDRequest:[msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)]];
  }
}

//OGD

- (void)sendOGDUser:(LANdiniUser *)user msg:(NSArray *)msg {
  if (user) {
    [user sendOGD:msg];
  }
}

- (void)receiveOGD:(NSArray *)msgArray {
  if (msgArray.count < 4 ||
      ![msgArray[1] isKindOfClass:[NSString class]] ||
      ![msgArray[2] isKindOfClass:[NSNumber class]]) {
    return;
  }
  NSString *fromName = msgArray[1];
  LANdiniUser *usr = [self userInUserListWithName:fromName];
  if (usr) {
    NSNumber *idNumber = msgArray[2];
    [usr receiveOGD:idNumber msg:[msgArray subarrayWithRange:NSMakeRange(3, [msgArray count]-3)]];
  }
}

- (void)receiveMissingOGDRequest:(NSArray *)msgArray {
  if (msgArray.count < 2 || ![msgArray[1] isKindOfClass:[NSString class]]) {
    return;
  }
  NSString *fromName = msgArray[1];
  LANdiniUser *usr = [self userInUserListWithName:fromName];
  if (usr) {
    [usr receiveMissingOGDRequest:[msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)]];
  }
}

//=== utility
- (LANdiniUser *)userInUserListWithName:(NSString *)userName {
  LANdiniUser *usr = nil;
  for (LANdiniUser *currUser in _userList) {
    if ([userName isEqualToString:currUser.name]) {
      usr = currUser;
      break;
    }
  }
  return usr;
}

@end
