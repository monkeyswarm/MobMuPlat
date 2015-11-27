//
//  PingAndConnectManager.h
//  MobMuPlat
//
//  Created by diglesia on 11/25/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol PingAndConnectUserStateDelegate <NSObject>

-(void)pingAndConnectUserStateChanged:(NSArray*)userArray; //array of user strings.

@end


@interface PingAndConnectManager : NSObject

@property (weak, nonatomic) id<PingAndConnectUserStateDelegate> userStateDelegate;

@property (nonatomic) BOOL enabled;
@property (nonatomic) NSInteger playerNumber;

@end
