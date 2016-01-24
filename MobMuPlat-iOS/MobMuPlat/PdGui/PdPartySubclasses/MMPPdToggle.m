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
  //[self sendFloat:self.value]; //Don't send
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.value = received;
  //[self sendFloat:received]; // Don't send
}

@end
