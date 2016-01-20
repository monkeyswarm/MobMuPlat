//
//  MMPPdNumber.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdNumber.h"

@implementation MMPPdNumber

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
  //[self sendFloat:self.value];
}

- (void)receiveSymbol:(NSString *)symbol fromSource:(NSString *)source {
  self.value = 0;
  //[self sendFloat:self.value];
}

@end
