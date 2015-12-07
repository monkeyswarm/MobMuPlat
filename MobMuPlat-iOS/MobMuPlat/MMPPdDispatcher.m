//
//  MMPPdDispatcher.m
//  MobMuPlat
//
//  Created by diglesia on 12/6/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPPdDispatcher.h"

#import "PdBase.h"

@implementation MMPPdDispatcher

// Pass all prints to print delegate.
- (void)receivePrint:(NSString *)message {
  if ([self.printDelegate respondsToSelector:@selector(receivePrint:)]) {
      [self.printDelegate receivePrint:message];
  } else {
    // NSLog(@"Unhandled print from %@", source);
  }
}

@end
