//
//  PingAndConnectManager.m
//  MobMuPlat
//
//  Created by diglesia on 11/25/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "PingAndConnectManager.h"

#import "LANdiniLANManager.h" //just for the OSC message maker...move it elsewhere
#import "MMPNetworkingUtils.h"
#import "VVOSC.h"

#define SC_DEFAULT_PORT 57120
#define SERVER_PLAYER_NUMBER -1

@interface PingAndConnectUser : NSObject

@property(nonatomic, copy) NSString *ipAddress;
@property(nonatomic) NSInteger playerNumber; //-1 server, 0 unassigned, 1-N player number
@property(nonatomic, readonly, strong) OSCOutPort *outPort;
@property(nonatomic) NSTimeInterval lastPingTime;

- (instancetype)initWithIpAddress:(NSString *)ipAddress playerNumber:(NSInteger)playerNumber;

@end


@implementation PingAndConnectUser

- (instancetype)initWithIpAddress:(NSString *)ipAddress playerNumber:(NSInteger)playerNumber {
  self = [super init];
  if (self) {
    _ipAddress = ipAddress;
    _playerNumber = playerNumber;
    _outPort = [[OSCOutPort alloc]initWithAddress:ipAddress andPort:SC_DEFAULT_PORT];
  }
  return self;
}

@end


@implementation PingAndConnectManager {

  NSInteger _myPlayerNumber; //-1 server, 0 unassigned, 1-N player number
  NSString *_myIP;
  __weak PingAndConnectUser *_meUser;

  NSMutableDictionary *_ipToUserMap; //key = IP address, value = PingAndConnectUser.

  NSTimeInterval _dropUserInterval;
  NSTimeInterval _checkUserInterval;
  NSTimeInterval _broadcastInterval;

  int _toLocalPort;
  int _fromLocalPort;

  NSTimer* _connectionTimer;
  NSTimer* _broadcastTimer;
  NSTimer* _dropUserTimer;
  NSTimer* _pingAndMsgIDsTimer;
  NSMutableDictionary* _apiResponders;
  NSMutableDictionary* _networkResponders;

  OSCManager* _oscManager;
  OSCOutPort* _broadcastAppAddr;
  OSCOutPort* _targetAppAddr;
  OSCInPort* _inPortLocal;
  OSCInPort* _inPortNetwork;

  //for time since last ping
  NSDate* _startDate;
}


+ (OSCMessage*) OSCMessageFromArray:(NSArray*)vals{
  OSCMessage* msg = [OSCMessage createWithAddress:(NSString*)[vals objectAtIndex:0] ];
  for(id item in [vals subarrayWithRange:NSMakeRange(1, [vals count]-1)]){
    if([item isKindOfClass:[NSString class]]) [msg addString:item];
    else if([item isKindOfClass:[NSNumber class]]){
      if(CFNumberIsFloatType((CFNumberRef)item))[msg addFloat:[item floatValue]];
      else [msg addInt:[item intValue]];
    }
  }
  return msg;
}


-(id)init{
  self = [super init];
  if(self){

    _ipToUserMap = [[NSMutableDictionary alloc] init];

    _dropUserInterval = 8.0;
    _checkUserInterval = 2.0;
    _broadcastInterval = 1.0;

    _toLocalPort = 50505;
    _fromLocalPort = 50506;

    _oscManager = [[OSCManager alloc] init];
    [_oscManager setDelegate:self];

    _startDate = [NSDate date];

    //not called on startup
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(willEnterFG:) name:UIApplicationWillEnterForegroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(willEnterBG:) name:UIApplicationWillResignActiveNotification object:nil];
  }
  return self;
}

-(void)sendMsgToApp:(NSArray*)msgArray{
  OSCMessage* msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
  [_targetAppAddr sendThisPacket:[OSCPacket createWithContent:msg]];

  //if([self.logDelegate respondsToSelector:@selector(logMsgInput:)] )
    //[self.logDelegate logMsgInput:msgArray];
}

- (void)sendMsgToPort:(OSCOutPort *)port msg:(NSArray *)msgArray {
  OSCMessage* msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
  [port sendThisPacket:[OSCPacket createWithContent:msg]];
}

-(NSTimeInterval) elapsedTime{
  return fabs( [_startDate timeIntervalSinceNow] );
}

-(void)setEnabled:(BOOL)enabled{
  _enabled = enabled;

  if(_enabled){
    [self connectOSC];
  }
  else{
    [self disconnectOSC];
  }
}

- (void)setPlayerNumber:(NSInteger)playerNumber {
  _myPlayerNumber = playerNumber;
  [self updatePingAndConnectUserState];
}

-(void)willEnterFG:(NSNotification*)notif{
  if(_enabled){
    [self connectOSC];
  }
}

-(void)willEnterBG:(NSNotification*)notif{
  if(_enabled){
    [self disconnectOSC];
  }
}

- (void)updatePingAndConnectUserState {
  if (self.userStateDelegate) {
    //@synchronized(self) {

      NSMutableArray *userStrings = [NSMutableArray array];
      for (PingAndConnectUser *user in [_ipToUserMap allValues]) { //generate the string for display
        NSMutableString *string = [[NSMutableString alloc] init];
        [string appendString:user.ipAddress];
        if (user.playerNumber > 0) {
          [string appendFormat:@" - #%ld", (long)user.playerNumber];
        }
        else if (user.playerNumber == SERVER_PLAYER_NUMBER) {
          [string appendString:@" - server"];
        }
        if (user == _meUser) {
          [string appendString:@" - (me)"];
        }
        [userStrings addObject:string];
      }
      [self.userStateDelegate pingAndConnectUserStateChanged:userStrings];
		//} //end synchronized
  }
  // re-send player info into the app
  [self sendPlayerCountToApp];
  [self sendPlayerNumberSetToApp];
  [self sendPlayerNumberToApp];
}

- (void)sendPlayerNumberToApp {
  //output message format:
  // '/pingAndConnect/myPlayerNumber', player number

  [self sendMsgToApp:@[ @"/pingAndConnect/myPlayerNumber", @(_myPlayerNumber) ]];
}

- (void)sendPlayerNumberSetToApp {
  //output message format:
  // '/pingAndConnect/playerNumberSet', set of current player numbers

  NSMutableSet *playerNumberSet = [NSMutableSet set];
  for (PingAndConnectUser *user in [_ipToUserMap allValues]) {
    [playerNumberSet addObject:@(user.playerNumber)];
  }
  NSMutableArray *msg = [NSMutableArray array];
  [msg addObject:@"/pingAndConnect/playerNumberSet"];
  [msg addObjectsFromArray:[playerNumberSet allObjects]];

  [self sendMsgToApp:msg];
}

- (void)sendPlayerCountToApp {
  //output message format:
  // '/pingAndConnect/playerCount', (distinct) player count

  [self sendMsgToApp:@[ @"/pingAndConnect/playerCount", @([_ipToUserMap count]) ]];
}

-(void)connectOSC{
  NSLog(@"connectOSC pac %p", self);
  _targetAppAddr = [_oscManager createNewOutputToAddress:@"127.0.0.1" atPort:_toLocalPort];
  _broadcastAppAddr = [_oscManager createNewOutputToAddress:@"224.0.0.1" atPort:SC_DEFAULT_PORT];
  _inPortNetwork = [_oscManager createNewInputForPort:SC_DEFAULT_PORT];//network responders from other landinis
  _inPortLocal = [_oscManager createNewInputForPort:_fromLocalPort];//api responders from local user app

  _myIP = [MMPNetworkingUtils ipAddress]; //check for nil
  [self updatePingAndConnectUserState];

  // player number set from network controller
  if (_myIP) {
    [self startBroadcastTimer];
    [self startDropUserTimer];
  } else {
    // disconnect? alert?

  }
}

-(void)disconnectOSC{
  NSLog(@"disconnectOSC pac %p", self);

  // stop timers
  [_broadcastTimer invalidate];
  _broadcastTimer = nil;
  [_dropUserTimer invalidate];
  _dropUserTimer = nil;

  [_ipToUserMap removeAllObjects];
  [self updatePingAndConnectUserState];

  [_oscManager deleteAllInputs];
  [_oscManager deleteAllOutputs];
  _inPortNetwork = nil;//necc?
  _inPortLocal = nil;
  _targetAppAddr = nil;
  _broadcastAppAddr = nil;

  //if backgrounded when looking for LAN, then that timer keeps firing and overlaps with its recreation to init the LAN twice, adding "me" twice
  //this attempts to prevent that
  if(_connectionTimer!=nil){
    [_connectionTimer invalidate];
    _connectionTimer=nil;
  }
}

#pragma mark OSCManager delegate

- (void)receivedOSCMessage:(OSCMessage *)m {
  NSString *address = [m address];
  NSMutableArray *tempOSCValueArray = [NSMutableArray array];
  NSMutableArray* msgArray = [NSMutableArray array];

  //VV library handles receiving a value confusingly: if just one value, it has a single value in message "m" and no valueArray, if more than one value, it has valuearray. here we just shove either into tempOSCValueArray to iterate over

  if([m valueCount]==1)[tempOSCValueArray addObject:[m value]];
  else for(OSCValue *val in [m valueArray])[tempOSCValueArray addObject:val];

  //first element in msgArray is address
  [msgArray addObject:address];

  //then iterate over all values
  for(OSCValue *val in tempOSCValueArray){//unpack OSC value to NSNumber or NSString
    if([val type]==OSCValInt){
      [msgArray addObject:[NSNumber numberWithInt:[val intValue]]];
    }
    else if([val type]==OSCValFloat){
      [msgArray addObject:[NSNumber numberWithFloat:[val floatValue]]];
    }
    else if([val type]==OSCValString){
      [msgArray addObject:[val stringValue]];
    }
  }

  if ([address isEqualToString:@"/pingandconnect/ping"]) { // accept [1]:ip or [1]:ip [2]:player number
    if ([msgArray count] > 1 && [msgArray[1] isKindOfClass:[NSString class]]) {
      NSString *ip = msgArray[1];
      PingAndConnectUser *user = nil;
      NSInteger playerNumber = 0;
      if ([msgArray count] > 2 && [msgArray[2] isKindOfClass:[NSNumber class]]) { //try string too?
        playerNumber = [msgArray[2] integerValue];
      } else if ([msgArray count] > 2 && [msgArray[2] isEqualToString:@"server"]) {
        playerNumber = SERVER_PLAYER_NUMBER; //tag as server
      }

      if (!_ipToUserMap[ip]) { // ip key not in map
        // Create user
        user = [[PingAndConnectUser alloc] initWithIpAddress:ip playerNumber:playerNumber];
        _ipToUserMap[ip] = user;
        if ([ip isEqualToString:_myIP]) {
          _meUser = user;
        }
        [self updatePingAndConnectUserState];
      } else {
        user = (PingAndConnectUser *)_ipToUserMap[ip];
        if (user.playerNumber != playerNumber) { // in map, but new player number
          user.playerNumber = playerNumber;
          [self updatePingAndConnectUserState];
        }
      }

      user.lastPingTime = [self elapsedTime];
    }
  } else if ([address isEqualToString:@"/send"]) {  //TODO test if sending a send is bad!
    // args[1] = all/allButMe/index #/server, [2] = user address, [3+] user args
    if ([msgArray count] < 3 || ![msgArray[2] isKindOfClass:[NSString class]]) return;

    NSArray *userMsg = [msgArray subarrayWithRange:NSMakeRange(2, [msgArray count] - 2)];
    NSObject *destObject = msgArray[1];

    if ([destObject isEqual:@"all"]) {
      for (PingAndConnectUser *user in [_ipToUserMap allValues]) {
        [self sendMsgToPort:user.outPort msg:userMsg];
      }
    } else if ([destObject isEqual:@"allButMe"]) {
      for (PingAndConnectUser *user in [_ipToUserMap allValues]) {
        if (user == _meUser) continue;
        [self sendMsgToPort:user.outPort msg:userMsg];
      }
    } else { //Something that signifies a player number. Try to parse nsnumber/string
      NSInteger playerNumber = 0;
      if ([destObject isEqual:@"server"]) {
        playerNumber = SERVER_PLAYER_NUMBER;
      } else if ([destObject isKindOfClass:[NSNumber class]]) {
        playerNumber = [(NSNumber *)destObject integerValue];
      } else if ([destObject isKindOfClass:[NSString class]]) {
        playerNumber = [(NSString *)destObject integerValue];
      }

      // iterate over users and send
      for (PingAndConnectUser *user in [_ipToUserMap allValues]) { //todo optimize
        if (user.playerNumber == playerNumber) {
          [self sendMsgToPort:user.outPort msg:userMsg];
        }
      }
    }

    } else if ([address isEqualToString:@"/playerCount"]) {
      [self sendPlayerCountToApp];
    } else if ([address isEqualToString:@"/playerNumberSet"]) {
      [self sendPlayerNumberSetToApp];
    } else if ([address isEqualToString:@"/playerIpList"]) {
      //sendPlayerIPListToApp();
    } else if ([address isEqualToString:@"/myPlayerNumber"]) {
      [self sendPlayerNumberToApp];
    } else { // normal from net, pass into app WHAIT BOTH IN AND OUT?
    		//OSCMessage  msg = NetworkController.OSCMessageFromList(msgList);
            //new SendTargetOSCTask().execute(msg); no more localhost, send direct
          //_parentNetworkController.oscListener.acceptMessage(null, msg); //TODO interface/delegate
      [self sendMsgToApp:msgArray];
    }
}

// broadcast stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

-(void)startBroadcastTimer{
  if(_broadcastTimer==nil){
    _broadcastTimer = [NSTimer scheduledTimerWithTimeInterval:_broadcastInterval target:self selector:@selector(broadcastTimerMethod:) userInfo:nil repeats:YES];
  }
}

-(void)broadcastTimerMethod:(NSTimer*)timer{
  NSMutableArray* msgArray = [[NSMutableArray alloc]initWithObjects:@"/pingandconnect/ping", _myIP, @(_myPlayerNumber), nil];

  [self broadcastMsg:msgArray];
}

-(void)broadcastMsg:(NSArray*)msgArray{

  OSCMessage* msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
  [_broadcastAppAddr sendThisPacket:[OSCPacket createWithContent:msg]];
  //if([self.logDelegate respondsToSelector:@selector(logLANdiniOutput:)] )
    //[self.logDelegate logLANdiniOutput:msgArray];
}

// group stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

-(void)startDropUserTimer{
  if(_dropUserTimer==nil){
    _dropUserTimer = [NSTimer scheduledTimerWithTimeInterval:_checkUserInterval target:self selector:@selector(startDropUserTimerMethod:) userInfo:nil repeats:YES ];
  }
}

-(void)startDropUserTimerMethod:(NSTimer*)timer{

  NSMutableArray* usersToDrop = [[NSMutableArray alloc]init];
  for(PingAndConnectUser* user in [_ipToUserMap allValues]){
    //if(user!=_me){
      //NSLog(@"check user %@ last ping %.2f", user.name, user.lastPing);
      if([self elapsedTime]-user.lastPingTime > _dropUserInterval){
        [usersToDrop addObject:user];
      }
    //}
  }
  if ([usersToDrop count] > 0) {
    @synchronized(self) {
      for(PingAndConnectUser* user in usersToDrop){
        //NSLog(@"dropped user %@ - my time %.2f userlastping time %.2f", user.ip, [self elapsedTime], user.lastPing);
        [_ipToUserMap removeObjectForKey:user.ipAddress];
      }
    }
    [self updatePingAndConnectUserState];
  }
}

-(void)dealloc{
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
