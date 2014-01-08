//
//  LANdiniUser.m
//  LANdiniDemo
//
//  Created by Daniel Iglesia on 12/17/13.
//  Copyright (c) 2013 IglesiaIntermedia. All rights reserved.
//

#import "LANdiniUser.h"

//forward dec in header
#import "LANdiniLANManager.h"

@interface LANdiniUser () {
    NSTimer* _newSyncServerAnnouncementTimer;
    NSTimer* _stopSyncAnnouncementTimer;
}

@end

@implementation LANdiniUser

-(id)initWithName:(NSString*)name IP:(NSString*)ip port:(int)port network:(LANdiniLANManager*)network{
    self = [super init];
    if(self) {
        self.name = name;
        self.ip=ip;
        self.port=port;
        self.network=network;
        
        //GD bookkeeping
        _lastOutgoingGDID=-1;
        _lastIncomingGDID=-1;
        _minGDID=-1;
        _performedGDIDs = [[NSMutableSet alloc]init];
        _sentGDMsgs = [[NSMutableDictionary alloc] init];
        
        //OGD bookkeeping
        _lastOutgoingOGDID = -1;
        _lastIncomingOGDID = -1;
        _lastPerformedOGDID = -1;
        _missingOGDIDs = [[NSMutableArray alloc]init];
        _msgQueueForOGD = [[NSMutableDictionary alloc] init];
        _sentOGDMsgs = [[NSMutableDictionary alloc] init];
        
        _addr = [[OSCOutPort alloc]initWithAddress:ip andPort:port];
        _lastPing = [network elapsedTime];
    }
    return self;
}

-(void)stopOSC{
    _addr = nil;
}

-(void)restartOSC{
    _addr = [[OSCOutPort alloc]initWithAddress:self.ip andPort:self.port];
}

-(void)receivePing:(NSArray*)vals{//do much error checking to do - their x, their y, lastGDID, etc
    int lastGDIDTheySentMe = [[vals objectAtIndex:2] intValue];
    int theirMinGD = [[vals objectAtIndex:3] intValue];
    int lastOGDIDTheySentMe = [[vals objectAtIndex:4] intValue];
    int lastOGDTheyPerformed = [[vals objectAtIndex:5] intValue];
    
    _lastPing = [_network elapsedTime];
    //NSLog(@"receive ping %@ time %.2f", _name, _lastPing);
    //update which msg I should be at
    if(lastGDIDTheySentMe > _lastIncomingGDID){
        _lastIncomingGDID = lastGDIDTheySentMe;
    }
    
    //remove unneeded storage msg
    NSMutableArray* obsoleteGDIDs = [[NSMutableArray alloc]init];
    for(NSNumber* num in  [_sentGDMsgs allKeys]){
        if([num intValue]<theirMinGD) [obsoleteGDIDs addObject:num];
    }
    for(NSNumber* num in obsoleteGDIDs)[_sentGDMsgs removeObjectForKey:num];
    
    //request missing msgs - TODO optimize
    NSMutableArray* tempMissingGDs = [[NSMutableArray alloc]init];
    for(int i=_minGDID+1;i<=_lastIncomingGDID;i++){
        if(![_performedGDIDs containsObject:[NSNumber numberWithInt:i]]){
            //NSLog(@"don't have %d", i);
            [tempMissingGDs addObject:[NSNumber numberWithInt:i]];
        }
    }
    
    if([tempMissingGDs count]>0)[self requestMissingGDs:tempMissingGDs];
    
    //OGD
    NSMutableArray* obsoleteOGDIDs = [[NSMutableArray alloc]init];
    for(NSNumber* num in  [_sentOGDMsgs allKeys]){
        if([num intValue]<lastOGDTheyPerformed) [obsoleteOGDIDs addObject:num];
    }
    for(NSNumber* num in obsoleteOGDIDs)[_sentOGDMsgs removeObjectForKey:num];
    
    if(lastOGDIDTheySentMe > _lastPerformedOGDID){
        NSArray* ids = [_msgQueueForOGD allKeys];
        NSMutableArray* missing = [[NSMutableArray alloc]init];
        for(int i=_lastPerformedOGDID+1 ; i<=lastOGDIDTheySentMe ;i++){
            if(![ids containsObject:[NSNumber numberWithInt:i]]){
                [missing addObject:[NSNumber numberWithInt:i]];
            }
        }
        [self requestMissingOGDs:missing];
    }
    
}

//====sync stuff

-(void)startAnnouncingNewSyncServer{
    if(_newSyncServerAnnouncementTimer==nil){
        _newSyncServerAnnouncementTimer = [NSTimer scheduledTimerWithTimeInterval:.5 target:self selector:@selector(syncServerTimerMethod:) userInfo:nil repeats:YES];
    }
}

-(void)syncServerTimerMethod:(NSTimer*)timer{
    //below is: addr.sendMsg('/landini/sync/new_server', network.me.name);
    
    NSArray* msgArray= [[NSArray alloc]initWithObjects:@"/landini/sync/new_server", _network.me.name, nil];
    [_addr sendThisPacket:[OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:msgArray]]];
    
}

-(void)stopAnnouncingNewSyncServer{
    if(_newSyncServerAnnouncementTimer!=nil){
        [_newSyncServerAnnouncementTimer invalidate];
        _newSyncServerAnnouncementTimer=nil;
    }
}

-(void)startAnnouncingStopSync{
    if(_stopSyncAnnouncementTimer==nil){
        _stopSyncAnnouncementTimer = [NSTimer scheduledTimerWithTimeInterval:.5 target:self selector:@selector(stopServerTimerMethod:) userInfo:nil repeats:YES];
    }
}

-(void)stopServerTimerMethod:(NSTimer*)timer{
    NSArray* msgArray = [[NSArray alloc]initWithObjects:@"/landini/sync/stop", _network.me.name, nil];
    [_addr sendThisPacket:[OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:msgArray]]];
   
}

-(void)stopAnnouncingStopSync{
    if(_stopSyncAnnouncementTimer!=nil){
        [_stopSyncAnnouncementTimer invalidate];
        _stopSyncAnnouncementTimer=nil;
    }
}

//===normal send methods

-(void)sendMsg:(NSArray*)msgArray{//address, vals
    //addr.sendMsg('/landini/msg', network.me.name, *msg);
    
    NSMutableArray* msgArray2 = [msgArray mutableCopy];
    [msgArray2 insertObject:@"/landini/msg" atIndex:0];
    [msgArray2 insertObject:_network.me.name atIndex:1];
    
    OSCMessage* msg = [LANdiniLANManager OSCMessageFromArray:msgArray2];
    [_addr sendThisPacket:[OSCPacket createWithContent:msg]];
    
    //[_network.logDelegate logLANdiniInput:@[[NSString stringWithFormat:@"<from user %@ ", _name]] ];
    [_network.logDelegate logLANdiniOutput:msgArray2];
}

-(void)receiveMsg:(NSArray*)msgArray{//arg?
    // network.target_app_addr.sendMsg(*msg);
    //TODO error check
    
    [_network sendMsgToApp:msgArray];
}



//GD methods

-(void)sendGD:(NSArray*)msgArray{//address, vals
    //optomize with ++
    int ID = _lastOutgoingGDID+1;
    _lastOutgoingGDID=ID;
    [_sentGDMsgs setObject:msgArray forKey:[NSNumber numberWithInt:ID]];
    //below is addr.sendMsg('/landini/msg/GD', network.me.name, id, *msg);
    
    
    
    NSMutableArray* msgArray2 = [msgArray mutableCopy];
    [msgArray2 insertObject:@"/landini/msg/GD" atIndex:0];
    [msgArray2 insertObject:_network.me.name atIndex:1];
    [msgArray2 insertObject:[NSNumber numberWithInt:ID] atIndex:2];
    
    [_addr sendThisPacket:[OSCPacket createWithContent: [LANdiniLANManager OSCMessageFromArray:msgArray2]]];
    [_network.logDelegate logLANdiniOutput:msgArray2];
}

-(void)receiveGD:(NSNumber*)idNumber msg:(NSArray*)msgArray{
    if( ([idNumber intValue]>_minGDID) && (![_performedGDIDs containsObject:idNumber]) ){
        NSMutableSet* ids = [_performedGDIDs mutableCopy];//opto
        int min = _minGDID;
        
        // network.target_app_addr.sendMsg(*msg);
        //[_network.targetAppAddr sendThisPacket:[OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:msgArray]]];
        [_network sendMsgToApp:msgArray];
        
        [ids addObject:idNumber];
        //TODO optimize
        while([ids containsObject:[NSNumber numberWithInt:min+1]]){
            [ids removeObject:[NSNumber numberWithInt:min+1]];
            min++;
        }
        _minGDID=min;
        _performedGDIDs=ids;
    }
}

-(void)requestMissingGDs:(NSArray*)missingGDs{
    // send OSC   addr.sendMsg('/landini/request/missing/GD', network.me.name, *missing);
    NSMutableArray* msgArray = [NSMutableArray arrayWithObjects:@"/landini/request/missing/GD", _network.me.name, nil];
    for(id item in missingGDs){
        [msgArray addObject:item];
    }
    [_addr sendThisPacket:[OSCPacket createWithContent: [LANdiniLANManager OSCMessageFromArray:msgArray]]];
}

-(void)receiveMissingGDRequest:(NSArray*)missingIDs{//Array of NSNumbers
    for(NSNumber* missedID in missingIDs){
        NSArray* msgArray = [_sentGDMsgs objectForKey:missedID];
        if(msgArray!=nil){
            // send OSC addr.sendMsg('/landini/msg/GD', network.me.name, missedID, *msg);
            NSMutableArray* msgArray2 = [NSMutableArray arrayWithObjects:@"/landini/msg/GD", _network.me.name, missedID, nil];
            for(id item in msgArray){
                [msgArray2 addObject:item];
            }
            
            [_addr sendThisPacket:[OSCPacket createWithContent: [LANdiniLANManager OSCMessageFromArray:msgArray2]]];
        }
    }
}

//===OGD messages

-(void)sendOGD:(NSArray *)msgArray{
    int ID = _lastOutgoingOGDID + 1;
    _lastOutgoingOGDID = ID;
    [_sentOGDMsgs setObject:msgArray forKey:[NSNumber numberWithInt:ID]];
     
     NSMutableArray* msgArray2 = [msgArray mutableCopy];
     [msgArray2 insertObject:@"/landini/msg/OGD" atIndex:0];
     [msgArray2 insertObject:_network.me.name atIndex:1];
     [msgArray2 insertObject:[NSNumber numberWithInt:ID] atIndex:2];
     
     [_addr sendThisPacket:[OSCPacket createWithContent: [LANdiniLANManager OSCMessageFromArray:msgArray2]]];
     [_network.logDelegate logLANdiniOutput:msgArray2];

}

-(void) receiveOGD:(NSNumber *)idNumber msg:(NSArray *)msgArray{
    int ID = [idNumber intValue];
    if(ID>_lastPerformedOGDID){
        
        if(ID > _lastIncomingOGDID){
            
            if( ID - _lastIncomingOGDID > 1 ){
                //create array of missing IDs
                NSMutableArray* missing;
                for(int i=_lastIncomingOGDID+1;i<=ID;i++){
                    [missing addObject:[NSNumber numberWithInt:i]];
                }
                [self requestMissingOGDs:missing];
            }
            _lastIncomingOGDID = ID;
        }
        int nextID = _lastPerformedOGDID + 1;
        if([_msgQueueForOGD objectForKey:idNumber]==nil){
            [_msgQueueForOGD setObject:msgArray forKey:idNumber];
        }
        [_missingOGDIDs removeObject:idNumber];
        
        NSArray* nextMsg = [_msgQueueForOGD objectForKey:[NSNumber numberWithInt:nextID]];
        while(nextMsg!=nil){
            [_network sendMsgToApp:nextMsg];
            [_msgQueueForOGD removeObjectForKey:[NSNumber numberWithInt:nextID]];
            _lastPerformedOGDID = nextID;
            nextID++;
            nextMsg = [_msgQueueForOGD objectForKey:[NSNumber numberWithInt:nextID]];
        }
    }
}

-(void)requestMissingOGDs:(NSArray*)missingGDs{
    for(NSNumber* number in missingGDs){
        if (![_missingOGDIDs containsObject:number]){
            [_missingOGDIDs addObject:number];
        }
    }
    if([_missingOGDIDs count]>0){
        [_missingOGDIDs sortUsingSelector:@selector(compare:)];
        // send OSC   addr.sendMsg('/landini/request/missing/OGD', network.me.name, *missing);
        NSMutableArray* msgArray = [NSMutableArray arrayWithObjects:@"/landini/request/missing/OGD", _network.me.name, nil];
        for(id item in _missingOGDIDs){
            [msgArray addObject:item];
        }
        [_addr sendThisPacket:[OSCPacket createWithContent: [LANdiniLANManager OSCMessageFromArray:msgArray]]];
    }
}

-(void) receiveMissingOGDRequest:(NSArray *)missedIDs{
    for(NSNumber* missedID in missedIDs){
        NSArray* msgArray = [_sentOGDMsgs objectForKey:missedID];
        if(msgArray!=nil){
            // send OSC addr.sendMsg('/landini/msg/OGD', network.me.name, missedID, *msg);
            NSMutableArray* msgArray2 = [NSMutableArray arrayWithObjects:@"/landini/msg/OGD", _network.me.name, missedID, nil];
            for(id item in msgArray){
                [msgArray2 addObject:item];
            }
            
            [_addr sendThisPacket:[OSCPacket createWithContent: [LANdiniLANManager OSCMessageFromArray:msgArray2]]];
        }
    }
}

@end
