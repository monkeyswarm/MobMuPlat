//
//  MMPPdRadio.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdRadio.h"

@implementation MMPPdRadio

- (void)sendInitValue {
  [super sendInitValue];
  [self sendFloat:self.value];
}

#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  //[self sendFloat:self.value];
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.value = received;
  //[super sendFloat:received]; // Pd 0.46+ doesn't clip incoming values
}

@end
