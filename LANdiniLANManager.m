//
//  LANdiniLANManager.m
//  LANdiniDemo
//
//  Created by Daniel Iglesia on 12/17/13.
//  Copyright (c) 2013 IglesiaIntermedia. All rights reserved.
//

#import "LANdiniLANManager.h"


//ip address
#import <ifaddrs.h>
#import <arpa/inet.h>

#define SC_DEFAULT_PORT 57120

@interface LANdiniLANManager () {
    BOOL _connected;
    NSTimeInterval _lanCheckInterval;
    NSTimeInterval _dropUserInterval;
    NSTimeInterval _checkUserInterval;
    NSTimeInterval _broadcastInterval;
    NSTimeInterval _pingInterval;
    NSTimeInterval _requestWaitInterval;
    NSTimeInterval _syncRequestInterval;
    NSMutableArray* _userList;
    
    int _toLocalPort;
    int _fromLocalPort;
    
    float _version;//string?
    
    NSTimer* _connectionTimer;
    NSTimer* _broadcastTimer;
    NSTimer* _dropUserTimer;
    NSTimer* _pingAndMsgIDsTimer;
    NSTimer* _syncTimer;
    NSMutableDictionary* _apiResponders;
    NSMutableDictionary* _networkResponders;
    
    NSString* _syncServerName;
    BOOL _inSync;
    int _smallestRtt;
    float _adjustmentToGetNetworkTime;
    
    //for network time
    NSDate* _startDate;
    
    OSCManager* _oscManager;
    OSCOutPort* _broadcastAppAddr;
    OSCOutPort* _targetAppAddr;
    OSCInPort* _inPortLocal;
    OSCInPort* _inPortNetwork;
}
@end

@implementation LANdiniLANManager


+ (OSCMessage*) OSCMessageFromArray:(NSArray*)vals{
    OSCMessage* msg = [OSCMessage createWithAddress:(NSString*)[vals objectAtIndex:0] ];
    for(id item in [vals subarrayWithRange:NSMakeRange(1, [vals count]-1)]){
        if([item isKindOfClass:[NSString class]]) [msg addString:item];
        else if([item isKindOfClass:[NSNumber class]]){
            if(CFNumberIsFloatType((CFNumberRef)item))[msg addFloat:[item floatValue]];
            else [msg addFloat:[item intValue]];
        }
    }
    return msg;
}

//-(void)sendMsgAddress:(NSString*)address vals:(NSArray*)vals{ //send to localapp  - vals
-(void)sendMsgToApp:(NSArray*)msgArray{
    OSCMessage* msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
    [_targetAppAddr sendThisPacket:[OSCPacket createWithContent:msg]];
    
    if([self.logDelegate respondsToSelector:@selector(logMsgInput:)] )
        [self.logDelegate logMsgInput:msgArray];
}


-(id)init{
    self = [super init];
    if(self){
        _version = 0.18;
        
        _lanCheckInterval = .5;
        _dropUserInterval = 2.0;
        _checkUserInterval = .3;
        _broadcastInterval = 1.0;
        _pingInterval = 0.33;
        _requestWaitInterval = .001;
        _syncRequestInterval = .33;
        
        _userList = [[NSMutableArray alloc]init];
        _toLocalPort = 50505;
        _fromLocalPort = 50506;
        _smallestRtt=1;
        
        _startDate = [NSDate date];
        
        _oscManager = [[OSCManager alloc] init];
        [_oscManager setDelegate:self];
        [self restartOSC];
        
        [self checkForLAN];
    }
    return self;
}

-(void)restartOSC{
    _targetAppAddr = [_oscManager createNewOutputToAddress:@"127.0.0.1" atPort:_toLocalPort];
    _broadcastAppAddr = [_oscManager createNewOutputToAddress:@"224.0.0.1" atPort:SC_DEFAULT_PORT];
    _inPortNetwork = [_oscManager createNewInputForPort:SC_DEFAULT_PORT];//network responders from other landinis
    _inPortLocal = [_oscManager createNewInputForPort:_fromLocalPort];//api responders from local user app
    
    for(LANdiniUser* user in _userList){
        [user restartOSC];
    }
}

-(void)stopOSC{
    [_oscManager deleteAllInputs];
    [_oscManager deleteAllOutputs];
    _inPortNetwork = nil;//ness?
    _inPortLocal = nil;
    _targetAppAddr = nil;
    _broadcastAppAddr = nil;
    
    for(LANdiniUser* user in _userList){
        [user stopOSC];
    }
}

-(NSTimeInterval) elapsedTime{
    return fabs( [_startDate timeIntervalSinceNow] );
}

-(void)checkForLAN{
    if(_connectionTimer==nil){
        _connectionTimer = [NSTimer scheduledTimerWithTimeInterval:_lanCheckInterval target:self selector:@selector(connectionTimerMethod:) userInfo:nil repeats:YES];
    }
}

-(void)connectionTimerMethod:(NSTimer*)timer{
    NSString* address = [self getIPAddress];
    if(address!=nil){
        [_connectionTimer invalidate];
        _connectionTimer=nil;
        [self initLAN:address];
    }
    else{
        NSLog(@"Still looking for a LAN...");
    }
}

-(void)initLAN:(NSString*)address{
    
    _syncServerName = @"noSyncServer";
    if([self.logDelegate respondsToSelector:@selector(refreshSyncServer:)] )
        [self.logDelegate refreshSyncServer:_syncServerName];
    
    NSString* myIP = address;
    int myPort = SC_DEFAULT_PORT; // supercollider default port
    NSString* myName = [[UIDevice currentDevice] name];
    _me = [[LANdiniUser alloc] initWithName:myName IP:myIP port:myPort network:self];
    _connected = YES;
    [_userList addObject:_me];
    
    //NSLog(@"connected to LAN at %@ on port %d", _me.ip, _me.port);
    if([self.logDelegate respondsToSelector:@selector(logLANdiniOutput:)] )
        [self.logDelegate logLANdiniOutput:@[ [NSString stringWithFormat:@"connected to LAN at %@ on port %d", _me.ip, _me.port] ] ];
    
    NSLog(@"got to the responders");
    [self setupAPIResponders];
    NSLog(@"got to the network responders");
    [self setUpNetworkResponders];
    NSLog(@"got to broadcast task");
    [self startBroadcastTimer];
    NSLog(@"got to ping task");
    [self startPingAndMsgIDsTimer];
    //NSLog(@"got to show gui");
    //this.show_gui; //GUI/Stage map currently unimplemented
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        NSLog(@"starting drop user task");
        [self startDropUserTimer];
    });
}

-(void) setupAPIResponders{

    _apiResponders = [[NSMutableDictionary alloc]init];
    
    //map addresses to selectors  - am assuming that this is faster than a switch statement
    [_apiResponders setObject:NSStringFromSelector(@selector(processApiMsg:)) forKey:@"/send"];
    [_apiResponders setObject:NSStringFromSelector(@selector(processApiMsg:)) forKey:@"/send/GD"];
    [_apiResponders setObject:NSStringFromSelector(@selector(processApiMsg:)) forKey:@"/send/OGD"];
    [_apiResponders setObject:NSStringFromSelector(@selector(receiveGetNumUsersRequest)) forKey:@"/numUsers"];
    [_apiResponders setObject:NSStringFromSelector(@selector(receiveGetNamesRequest)) forKey:@"/userNames"];
    //[_apiResponders setObject:NSStringFromSelector(@selector(receiveGetStageMapRequest)) forKey:@"/stageMap"];//unimplmeneted
    [_apiResponders setObject:NSStringFromSelector(@selector(receiveGetNetworkTimeRequest)) forKey:@"/networkTime"];
    [_apiResponders setObject:NSStringFromSelector(@selector(receiveMyNameRequest)) forKey:@"/myName"];
    
    NSLog(@"setup %lu new api responders", (unsigned long)[_apiResponders count]);
}

- (void)receiveMyNameRequest {
    //output message format:
    // '/landini/myName', me.name
    
    [self sendMsgToApp:[NSArray arrayWithObjects:@"/landini/myName", _me.name, nil]];
}

-(void)receiveGetNamesRequest {
    //output message format:
    // '/landini/userNames, <list of user names>...
    
    //DEI edit - difference from super collider version:
    //this returns a list of _all_ users (including me), not all but me
    
    NSMutableArray* msgArray = [[NSMutableArray alloc]init];
    [msgArray addObject:@"/landini/userNames"];
    for(LANdiniUser* user in _userList){
        [msgArray addObject:user.name];
    }
    
    [self sendMsgToApp:msgArray];
}

- (void)receiveGetNumUsersRequest {
    //output message format:
    // '/landini/numUsers', <number of users in userlist>
    
    [self sendMsgToApp:[NSArray arrayWithObjects:@"/landini/numUsers", [NSNumber numberWithInteger:[_userList count]], nil]];
}

//omit receive_get_stage_map_request

-(void) receiveGetNetworkTimeRequest{
    //output message format:
    // '/landini/networkTime, <double: time elapsed in seconds>
    
    NSTimeInterval time;
    if(_inSync){
        time = [self networkTime];
    }
    else time=-1;
    [self sendMsgToApp:[NSArray arrayWithObjects:@"/landini/networkTime", [NSNumber numberWithDouble:time], nil]];
}

-(void) processApiMsg:(NSArray*)msgArray{
    //NSLog(@"process: %@", msgArray);
    //input message format:
    // protocol ("/send, /sendGD, /sendOGD"),
    // name of recipient (or "all"/"allButMe"),
    // user msg address
    // user msg values......
    
    //check first two are strings
    if( ![[msgArray objectAtIndex:0] isKindOfClass:[NSString class]] ||  ![[msgArray objectAtIndex:1] isKindOfClass:[NSString class]] ) return;
    
    NSString* protocol = [msgArray objectAtIndex:0];
    NSString* name = [msgArray objectAtIndex:1];  //LANDini User name or "all"/"allButMe"
    
    NSArray* msgArray2 = [msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)];//strip off first two elements, leaving user message address and values
    
    if([name isEqualToString:@"all"]){
        if([protocol isEqualToString:@"/send"]){
            for(LANdiniUser* user in _userList){
                [self sendUser:user msg:msgArray2];
            }
        }
        else if([protocol isEqualToString:@"/send/GD"]){
            for(LANdiniUser* user in _userList){
                [self sendGD:user msg:msgArray2];
            }
        }
        else if([protocol isEqualToString:@"/send/OGD"]){
            for(LANdiniUser* user in _userList){
                [self sendOGDUser:user msg:msgArray2];
            }
        }
    }
    else if([name isEqualToString:@"allButMe"]){
        if([protocol isEqualToString:@"/send"]){
            for(LANdiniUser* user in _userList){
                if(user!=_me)[self sendUser:user msg:msgArray2];
            }
        }
        else if([protocol isEqualToString:@"/send/GD"]){
            for(LANdiniUser* user in _userList){
                if(user!=_me)[self sendGD:user msg:msgArray2];
            }
        }
        if([protocol isEqualToString:@"/send/OGD"]){
            for(LANdiniUser* user in _userList){
                if(user!=_me)[self sendOGDUser:user msg:msgArray2];
            }
        }
    }
    else { //user name
        LANdiniUser* usr = [self userInUserListWithName:name];
        if(usr==nil){
            //NSLog(@"invalid user name");
            if([self.logDelegate respondsToSelector:@selector(logLANdiniOutput:)] )
                [self.logDelegate logLANdiniOutput:@[ [NSString stringWithFormat:@"error: sending to bad user name %@", name] ] ];
            return;
        }
        
        if([protocol isEqualToString:@"/send"])[self sendUser:usr msg:msgArray2];
        else if([protocol isEqualToString:@"/send/GD"])[self sendGD:usr msg:msgArray2];
        else if([protocol isEqualToString:@"/send/OGD"])[self sendOGDUser:usr msg:msgArray2];
    }
}

//====network stuff
// responders for osc messages coming from other copies of LANdiniOSC on the network

-(void)setUpNetworkResponders{
    
    _networkResponders = [[NSMutableDictionary alloc]init];
    
    //map addresses to selectors - am assuming this is faster than a switch statement
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveMemberBroadcast:)) forKey:@"/landini/member/broadcast"];
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveMemberReply:)) forKey:@"/landini/member/reply"];
    [_networkResponders setObject:NSStringFromSelector(@selector(receivePingAndMsgIDs:)) forKey:@"/landini/member/ping_and_msg_IDs"];
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveMsg:)) forKey:@"/landini/msg"];
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveGD:)) forKey:@"/landini/msg/GD"];
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveOGD:)) forKey:@"/landini/msg/OGD"];
    
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveMissingGDRequest:)) forKey:@"/landini/request/missing/GD"];
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveMissingOGDRequest:)) forKey:@"/landini/request/missing/OGD"];
    
    //unimplemented in original
    //[_networkResponders setObject:NSStringFromSelector(@selector(receiveNewSyncServer:)) forKey:@"/landini/sync/new_server"];
    
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveSyncRequest:)) forKey:@"/landini/sync/request"];
    [_networkResponders setObject:NSStringFromSelector(@selector(receiveSyncReply:)) forKey:@"/landini/sync/reply"];
    
    //unimplemented in original
    //[_networkResponders setObject:NSStringFromSelector(@selector(receiveSyncStop:)) forKey:@"/landini/sync/stop"];
    //[_networkResponders setObject:NSStringFromSelector(@selector(receiveSyncStopAcknowledgement:)) forKey:@"/landini/sync/got_stop"];
    
    NSLog(@"setup %lu new network responders", (unsigned long)[_networkResponders count]);
    
}

#pragma mark OSCManager delegate

- (void) receivedOSCMessage:(OSCMessage *)m	{ 
	NSString *address = [m address];

    //look in both network responders and API responders
    NSString* selectorString = [_networkResponders objectForKey:address];//look in network responders
    
    if(selectorString==nil)//if not found, look again in apiresponders
        selectorString = [_apiResponders objectForKey:address];
    
    if(selectorString==nil)return;//if still not found, return
    
    SEL aSelector = NSSelectorFromString(selectorString);
    
    NSMutableArray* msgArray = [[NSMutableArray alloc]init];//create blank message array for sending to pd
    NSMutableArray* tempOSCValueArray = [[NSMutableArray alloc]init];
    
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
    
    [self performSelector:aSelector withObject:msgArray]; //has a potential memory leak warning: just don't return any retained objects with the selector, which isn't an issue since all our selectors return void

    if([self.logDelegate respondsToSelector:@selector(logLANdiniInput:)] )
        [self.logDelegate logLANdiniInput:msgArray];

}


// broadcast stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

-(void) startBroadcastTimer{
    if(_broadcastTimer==nil){
        _broadcastTimer = [NSTimer scheduledTimerWithTimeInterval:_broadcastInterval target:self selector:@selector(broadcastTimerMethod:) userInfo:nil repeats:YES];
    }
}

-(void)broadcastTimerMethod:(NSTimer*)timer{
    NSMutableArray* msgArray = [[NSMutableArray alloc]initWithObjects:@"/landini/member/broadcast", _me.name, _me.ip, [NSNumber numberWithInt:_me.port], nil];
    
    [self broadcastMsg:msgArray];
}

-(void)broadcastMsg:(NSArray*)msgArray{
    
    OSCMessage* msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
    [_broadcastAppAddr sendThisPacket:[OSCPacket createWithContent:msg]];
    if([self.logDelegate respondsToSelector:@selector(logLANdiniOutput:)] )
       [self.logDelegate logLANdiniOutput:msgArray];
}


-(void) receiveMemberBroadcast:(NSArray*)msgArray{//address, name, ip, port
    //input message format:
    //address ("/landini/member/broadcast/")
    //member name
    //member IP (NSString)
    //member port (NSNumber)

    //check types
    if([msgArray count]!=4 ||
       ![[msgArray objectAtIndex:0] isKindOfClass:[NSString class]] ||
       ![[msgArray objectAtIndex:1] isKindOfClass:[NSString class]] ||
       ![[msgArray objectAtIndex:2] isKindOfClass:[NSString class]] ||
       ![[msgArray objectAtIndex:3] isKindOfClass:[NSNumber class]]
    ) return;

    NSString* theirName = [msgArray objectAtIndex:1];
    NSString* theirIP = [msgArray objectAtIndex:2];
    NSNumber* theirPortNumber = [msgArray objectAtIndex:3];
    int theirPort = [theirPortNumber intValue];
    
    NSMutableArray* replyMsg = [[NSMutableArray alloc]init];
    [replyMsg addObject:@"/landini/member/reply"];
    
    LANdiniUser* fromUsr = nil;
    
    if(![theirIP isEqualToString:_me.ip]){
        //optimize
        for(LANdiniUser* user in _userList){
            if([user.ip isEqualToString:theirIP]) fromUsr = user;
        }
        if(fromUsr==nil){//no user found, create and add to user list
            fromUsr = [self assimilateMemberInfoName:theirName IP:theirIP port:theirPort];
        }
        //sending triplets of connected user data?
       for(LANdiniUser* user in _userList){
           [replyMsg addObject:user.name];
           [replyMsg addObject:user.ip];
           [replyMsg addObject:[NSNumber numberWithInt:user.port]];
       }
        //[self sendUser:fromUsr msg:replyMsg]; no, this was prepending /landini/msg...through following orig???
        [[fromUsr addr] sendThisPacket:[OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:replyMsg]]];
        
    }
}


-(void) receiveMemberReply:(NSArray*)addrs{
    //input message format:
    //landini address
    //list of triplets of user data (name, IP, port, name, IP, port,...)
    
    if(([addrs count]-1)/3 != [_userList count]){//if doesn't match our current user list size
        
        for(int i=1;i<[addrs count];i+=3){
            NSString* newName = (NSString*)[addrs objectAtIndex:i];
            NSString* newIP = (NSString*)[addrs objectAtIndex:i+1];
            int newPort = [(NSNumber*)[addrs objectAtIndex:i+2] intValue];
            
            [self assimilateMemberInfoName:newName IP:newIP port:newPort];
        }
    }
}

// ping and msg ID stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

-(void)startPingAndMsgIDsTimer{
    if(_pingAndMsgIDsTimer==nil){
        _pingAndMsgIDsTimer = [NSTimer scheduledTimerWithTimeInterval:_pingInterval target:self selector:@selector(pingTimerMethod:) userInfo:nil repeats:YES];
    }
}

-(void)pingTimerMethod:(NSTimer*)timer{
    
    for(LANdiniUser* user in _userList){
        if(user!=_me){
            NSArray* msgArray = [[NSArray alloc]initWithObjects:
                                 @"/landini/member/ping_and_msg_IDs",
                                 _me.name,
                                 [NSNumber numberWithFloat:0],//x position unimplemented
                                 [NSNumber numberWithFloat:0],//y position unimplemented
                                 [NSNumber numberWithInt:user.lastOutgoingGDID],
                                 [NSNumber numberWithInt:user.minGDID],
                                 [NSNumber numberWithInt:user.lastOutgoingOGDID],
                                 [NSNumber numberWithInt:user.lastPerformedOGDID],
                                 _syncServerName, nil];
            
            [[user addr] sendThisPacket:[OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:msgArray]]];
        }
    }
}

- (void) receivePingAndMsgIDs:(NSArray*)msgArray{
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
    
    //if([msgArray count]!=9)return;
    
    NSString* name = (NSString*)[msgArray objectAtIndex:1];
    //NSLog(@"receivePingAndMsg from %@", name);
    LANdiniUser* usr = [self userInUserListWithName:name];
    if(usr!=nil){//found user
        //output format to receivePing method:user info
        NSArray* userArray = [msgArray subarrayWithRange:NSMakeRange(2, 6)];//xpos, ypos, ID values...
        [usr receivePing:userArray];
    }
    
    NSString* syncServerPingName = [msgArray objectAtIndex:8];//todo add check
    if( [_syncServerName isEqualToString:@"noSyncServer"] || [syncServerPingName isEqualToString:_syncServerName] ){
        [self dealWithNewSyncServerName:syncServerPingName];//syncServerPingName
    }
}

// group stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

- (LANdiniUser*) assimilateMemberInfoName:(NSString*)name IP:(NSString*)ip port:(int)port{
    LANdiniUser* usr = nil;
    //DEI edit not in original supercollider source: check for existence of user in list by name
    usr = [self userInUserListWithName:name ];
    if(usr==nil){//not found
        //end DEI edit
    
        LANdiniUser* usr = [[LANdiniUser alloc] initWithName:name IP:ip port:port network:self];
        [_userList addObject:usr];
        NSLog(@"added user %@", usr.name);
        
        //DEI edit not in original supercollider source:send new message to user app with names
        [self receiveGetNamesRequest];
        //end DEI edit
    }
    return usr;
    
}

-(void)startDropUserTimer{
    if(_dropUserTimer==nil){
        _dropUserTimer = [NSTimer scheduledTimerWithTimeInterval:_checkUserInterval target:self selector:@selector(startDropUserTimerMethod:) userInfo:nil repeats:YES ];
    }
}

-(void)startDropUserTimerMethod:(NSTimer*)timer{

    NSMutableArray* usersToDrop = [[NSMutableArray alloc]init];
    for(LANdiniUser* user in _userList){
        if(user!=_me){
            //NSLog(@"check user %@ last ping %.2f", user.name, user.lastPing);
            if([self elapsedTime]-user.lastPing > _dropUserInterval){
                [usersToDrop addObject:user];
            }
        }
    }
    for(LANdiniUser* user in usersToDrop){
        NSLog(@"dropped user %@ - my time %.2f userlastping time %.2f", user.name, [self elapsedTime], user.lastPing);
        if([user.name isEqualToString:_syncServerName]){
            [self stopSyncTimer];
            [self resetSyncVars];
        }
        [_userList removeObject:user];
        //DEI edit not in original supercollider: send the user client new users list
        [self receiveGetNamesRequest];
    }
    
}

// sync stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

-(NSTimeInterval)networkTime{
     return [self elapsedTime] + _adjustmentToGetNetworkTime;
}


-(void)becomeSyncServer{
    NSLog(@"becoming sync server!");
    [self stopSyncTimer];
    _syncServerName = _me.name;
    _adjustmentToGetNetworkTime = 0;
    _inSync = YES;
    
    if([self.logDelegate respondsToSelector:@selector(refreshSyncServer:)] )
        [self.logDelegate refreshSyncServer:_me.name];
}


-(void) dealWithNewSyncServerName:(NSString*)newName{
    
    if([newName isEqualToString:@"noSyncServer"]){
        //NSLog(@"pinged sync server name is noSyncServer");
        NSMutableArray* namesArray = [[NSMutableArray alloc]init];
        for(LANdiniUser* user in _userList){
            [namesArray addObject:user.name];
        }
        
        [namesArray sortUsingSelector:@selector(compare:)];//sort by string
        NSLog(@"here's allNames: %@", namesArray);
        if([namesArray objectAtIndex:0]==_me.name){
            if(![_syncServerName isEqualToString:_me.name]){
                [self becomeSyncServer];
            }
            else{
                NSLog(@"i am already the sync server");
            }
        }
    }
    
    else{
        LANdiniUser* user = nil;
        user = [self userInUserListWithName:newName];
        if(user!=nil){//if found
            _syncServerName = newName;
            
            if([self.logDelegate respondsToSelector:@selector(refreshSyncServer:)] )
                [self.logDelegate refreshSyncServer:newName];
        }
        
        //[self startSyncTimer];
        [self performSelectorOnMainThread:@selector(startSyncTimer) withObject:nil waitUntilDone:NO];
    }
}

-(void)resetSyncVars{
    _adjustmentToGetNetworkTime = 0;
    _inSync = NO;
    _syncServerName = @"noSyncServer";
    _smallestRtt = 1;
    if([self.logDelegate respondsToSelector:@selector(refreshSyncServer:)] )
        [self.logDelegate refreshSyncServer:_syncServerName];
}

//i get this when I am the time server, I should not be getting messages from me
-(void)receiveSyncRequest:(NSArray*)msgArray{
    //input format:
    // landini address
    // user name
    // user time
   
    if([msgArray count]!=3 ||
       ![[msgArray objectAtIndex:1] isKindOfClass:[NSString class]] ||
       ![[msgArray objectAtIndex:2] isKindOfClass:[NSNumber class]]
       ) return;
    
    NSString* theirName = [msgArray objectAtIndex:1];
    NSNumber* theirTimeNumber = [msgArray objectAtIndex:2];
    
    LANdiniUser* usr = [self userInUserListWithName:theirName];
    if(usr!=nil){
        if(![usr.name isEqualToString:_me.name]){
            NSArray* msgArray = [NSArray arrayWithObjects:
                             @"/landini/sync/reply",
                             _me.name,
                             theirTimeNumber,
                             [NSNumber numberWithDouble:[self elapsedTime]],
                             nil];
        
        OSCMessage* msg = [LANdiniLANManager OSCMessageFromArray:msgArray];
        [[usr addr] sendThisPacket:[OSCPacket createWithContent:msg]];
        }
        else{
            NSLog(@"i should not be sending myself sync requests");
            [self stopSyncTimer];
        }
    }
    else{
        NSLog(@"time server is not in the userlist");
        [self stopSyncTimer];
        [self resetSyncVars];
    }
}


-(void) receiveSyncReply:(NSArray*)msgArray{//address, etc
    //TimeServerName:(NSString*)timeServerName myOldTime:(NSTimeInterval)myOldTime timeServertime:(NSTimeInterval)timeServerTime{
    NSString* timeServerName = [msgArray objectAtIndex:1];
    NSTimeInterval myOldTime = [[msgArray objectAtIndex:2] doubleValue];
    NSTimeInterval timeServerTime = [[msgArray objectAtIndex:3] doubleValue];

    LANdiniUser* usr = [self userInUserListWithName:timeServerName];
    
    
    if(usr && [timeServerName isEqualToString:_syncServerName]){
        _inSync = YES;
        NSTimeInterval now = [self elapsedTime];
        NSTimeInterval rtt = now-myOldTime;
        _smallestRtt = MIN(_smallestRtt, rtt);
        NSTimeInterval serverTime = timeServerTime + (_smallestRtt/2);
        _adjustmentToGetNetworkTime = serverTime - now;
    }
    else{
        NSLog(@"stopping sync task because of sync server name discrepancy");
        [self stopSyncTimer];
        [self resetSyncVars];
    }
}

-(void) startSyncTimer{
    if(_syncTimer==nil){
        _syncTimer = [NSTimer scheduledTimerWithTimeInterval:_syncRequestInterval target:self selector:@selector(syncTimerMethod:) userInfo:nil repeats:YES];
    }
}

-(void)syncTimerMethod:(NSTimer*)timer{
    
    LANdiniUser* server = [self userInUserListWithName:_syncServerName];
    /*for(LANdiniUser* currUser in _userList){
        if([_syncServerName isEqualToString:currUser.name]){
            server = currUser;
            break;
        }
    }*/
    if(server && server!=_me){//should this be sent even if I am server?

        OSCMessage *msg = [OSCMessage createWithAddress:@"/landini/sync/request"];
        [msg addString:_me.name];
        [msg addFloat:[self elapsedTime]];
        [[server addr] sendThisPacket:[OSCPacket createWithContent:msg]];
    }
}

-(void)stopSyncTimer{
    if(_syncTimer){
        [_syncTimer invalidate];
        _syncTimer = nil;
    }
}




//===normal send methods

-(void)sendUser:(LANdiniUser*)user msg:(NSArray*)msg{
    if(user!=nil){
        [user sendMsg:msg];
    }
}

-(void) receiveMsg:(NSArray*)msgArray{//api address, user name, user msg addess, user args...
    NSString* fromName = [msgArray objectAtIndex:1];//todo checking
    LANdiniUser* user = [self userInUserListWithName:fromName];
    /*for(LANdiniUser* currUser in _userList){
        if([fromName isEqualToString:currUser.name]){
            user = currUser;
            break;
        }
    }*/
    if(user){
        [user receiveMsg:[msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)]];//strip from name
    }
}



//====GD methods

-(void)sendGD:(LANdiniUser*)user msg:(NSArray*)msg{
    if(user!=nil){
        [user sendGD:msg];
    }
}


-(void)receiveGD:(NSArray*)msgArray{// landini network api address, from user name, ID, user address, user vals
    NSString* fromName = [msgArray objectAtIndex:1];
    LANdiniUser* usr = [self userInUserListWithName:fromName];
    /*for(LANdiniUser* currUser in _userList){
        if([fromName isEqualToString:currUser.name]){
            usr = currUser;
            break;
        }
    }*/
    if(usr){
        NSNumber* idNumber = [msgArray objectAtIndex:2];
        [usr receiveGD:idNumber msg:[msgArray subarrayWithRange:NSMakeRange(3, [msgArray count]-3)]];
    }
}


-(void)receiveMissingGDRequest:(NSArray*)msgArray{
    NSString* fromName = [msgArray objectAtIndex:1];
    LANdiniUser* usr = [self userInUserListWithName:fromName];
    /*for(LANdiniUser* currUser in _userList){
        if([fromName isEqualToString:currUser.name]){
            usr = currUser;
            break;
        }
    }*/
    if(usr){
        [usr receiveMissingGDRequest:[msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)]];
    }
    
}

//OGD

-(void)sendOGDUser:(LANdiniUser*)user msg:(NSArray*)msg{
    if(user!=nil){
        [user sendOGD:msg];
    }
}

-(void)receiveOGD:(NSArray*)msgArray{
    NSString* fromName = [msgArray objectAtIndex:1];
    LANdiniUser* usr = [self userInUserListWithName:fromName];
    /*for(LANdiniUser* currUser in _userList){
        if([fromName isEqualToString:currUser.name]){
            usr = currUser;
            break;
        }
    }*/
    if(usr){
        NSNumber* idNumber = [msgArray objectAtIndex:2];
        [usr receiveOGD:idNumber msg:[msgArray subarrayWithRange:NSMakeRange(3, [msgArray count]-3)]];
    }
}

-(void)receiveMissingOGDRequest:(NSArray*)msgArray{
    NSString* fromName = [msgArray objectAtIndex:1];
    LANdiniUser* usr = [self userInUserListWithName:fromName];
    /*for(LANdiniUser* currUser in _userList){
        if([fromName isEqualToString:currUser.name]){
            usr = currUser;
            break;
        }
    }*/
    if(usr){
        [usr receiveMissingOGDRequest:[msgArray subarrayWithRange:NSMakeRange(2, [msgArray count]-2)]];
    }
    
}



//-----

- (NSString *)getIPAddress {
    NSString *address=nil;//return nil on not found
    struct ifaddrs *interfaces = NULL;
    struct ifaddrs *temp_addr = NULL;
    int success = 0;
    // retrieve the current interfaces - returns 0 on success
    success = getifaddrs(&interfaces);
    if (success == 0) {
        // Loop through linked list of interfaces
        temp_addr = interfaces;
        while(temp_addr != NULL) {
            if(temp_addr->ifa_addr->sa_family == AF_INET) {
                // Check if interface is en0 which is the wifi connection on the iPhone
                if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
                    // Get NSString from C String
                    address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
                    NSLog(@"found ip %@", address);
                }
            }
            temp_addr = temp_addr->ifa_next;
        }
    }
    // Free memory
    freeifaddrs(interfaces);
    NSLog(@"my ip is %@", address);
    return address;
    
}

//=== utility
-(LANdiniUser*)userInUserListWithName:(NSString*)userName{
    LANdiniUser* usr=nil;
    for(LANdiniUser* currUser in _userList){
        if([userName isEqualToString:currUser.name]){
            usr = currUser;
            break;
        }
    }
    return usr;
}




@end
