//
//  MMPPdToggle.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdToggle.h"

@implementation MMPPdToggle

#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  [self toggle];
  //[self sendFloat:self.value];
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.value = received;
  //[self sendFloat:received]; // Pd 0.46+ doesn't clip incoming values
}

@end
