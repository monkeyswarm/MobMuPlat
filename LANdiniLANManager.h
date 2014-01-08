//
//  LANdiniLANManager.h
//  LANdiniDemo
//
//  Created by Daniel Iglesia on 12/17/13.
//  Copyright (c) 2013 IglesiaIntermedia. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "VVOSC.h"
#import "LANdiniUser.h"

@protocol LANdiniDemoLogDelegate <NSObject>

-(void)logLANdiniOutput:(NSArray*)msgArray;
-(void)logMsgOutput:(NSArray*)msgArray;
-(void)logLANdiniInput:(NSArray*)msgArray;
-(void)logMsgInput:(NSArray*)msgArray;
-(void)refreshSyncServer:(NSString*)newServerName;

@end

@interface LANdiniLANManager : NSObject <OSCDelegateProtocol>


@property (strong, nonatomic) LANdiniUser * me;
@property id<LANdiniDemoLogDelegate> logDelegate;


+ (OSCMessage*) OSCMessageFromArray:(NSArray*)vals; //array of address, object vals -> OSCMessage
- (NSTimeInterval) elapsedTime;
-(void)sendMsgToApp:(NSArray*)msgArray;

-(void)stopOSC;
-(void)restartOSC;
@end
