//
//  MMPPdNumber2.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdNumber2.h"

@implementation MMPPdNumber2

#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  //[self sendFloat:self.value];
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.value = received;
  //[self sendFloat:self.value];
}

//TODO touch diff per scale
@end
