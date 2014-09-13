//
//  LANdiniUser.h
//  LANdiniDemo
//
//  Created by Daniel Iglesia on 12/17/13.
//  Copyright (c) 2013 IglesiaIntermedia. All rights reserved.
//

#import <Foundation/Foundation.h>

//#import "LANdiniLANManager.h"
#import "VVOSC.h"

@class LANdiniLANManager;

@interface LANdiniUser : NSObject

@property (strong, nonatomic) NSString* name;
@property (strong, nonatomic) NSString* ip;
@property (nonatomic) int port;
@property (strong, nonatomic) OSCOutPort* addr;
@property (nonatomic) NSTimeInterval lastPing;
@property (weak, nonatomic) LANdiniLANManager* network;

//guaranteed delivery vars
@property (nonatomic, readonly) int lastOutgoingGDID;
@property (nonatomic, readonly) int lastIncomingGDID;
@property (nonatomic, readonly) NSMutableSet* performedGDIDs;
@property (nonatomic) int minGDID;
@property (strong, nonatomic, readonly) NSMutableDictionary* sentGDMsgs;

//ordered guaranteed delivery vars
@property (nonatomic, readonly) int lastOutgoingOGDID;
@property (nonatomic, readonly) int lastIncomingOGDID;
@property (nonatomic, readonly) int lastPerformedOGDID;
@property (strong, nonatomic, readonly) NSMutableArray* missingOGDIDs;
@property (strong, nonatomic, readonly) NSMutableDictionary* msgQueueForOGD;
@property (strong, nonatomic, readonly) NSMutableDictionary* sentOGDMsgs;

//sync vars


//user map


-(id)initWithName:(NSString*)name IP:(NSString*)ip port:(int)port network:(LANdiniLANManager*)network;

-(void)sendMsg:(NSArray*)msgArray;
-(void)sendGD:(NSArray*)msg;
-(void)sendOGD:(NSArray*)msg;

-(void)receivePing:(NSArray*)vals;
-(void)receiveMsg:(NSArray*)vals;
-(void)receiveGD:(NSNumber*)idNumber msg:(NSArray*)msgArray;
-(void)receiveMissingGDRequest:(NSArray*)vals;
-(void)receiveOGD:(NSNumber*)idNumber msg:(NSArray*)msgArray;
-(void)receiveMissingOGDRequest:(NSArray*)vals;


-(void)restartOSC;
-(void)stopOSC;
@end
