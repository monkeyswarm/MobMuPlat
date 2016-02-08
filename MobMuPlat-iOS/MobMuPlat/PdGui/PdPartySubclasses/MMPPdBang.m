//
//  MMPPdBang.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdBang.h"

// Expose some superclass methods.
@interface Bang()

- (void)bang;
- (void)sendBang;

@end

@implementation MMPPdBang


#pragma mark Touches

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
  //[self receiveBangFromSource:@""];
  [self bang];
  [self sendBang];
}

#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  [self bang]; //Just display, don't send out.
  //[self sendBang];
}

@end
