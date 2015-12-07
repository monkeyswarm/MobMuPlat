//
//  MMPPdDispatcher.h
//  MobMuPlat
//
//  Created by diglesia on 12/6/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

//#import <pd-osx/pd-osx.h>

#import "PdDispatcher.h"

@protocol MMPPdDispatcherPrintDelegate <NSObject>

- (void)receivePrint:(NSString *)message;

@end


@interface MMPPdDispatcher : PdDispatcher

@property(nonatomic, weak)id<MMPPdDispatcherPrintDelegate> printDelegate;

@end
