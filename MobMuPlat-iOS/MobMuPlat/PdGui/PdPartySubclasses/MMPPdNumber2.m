//
//  MMPPdNumber2.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdNumber2.h"

#import "Gui.h"

// Expose superclass instance variables for use in touch drag computation.
@interface Number2 () {
  @protected
  int touchPrevY;
  bool isOneFinger;
  double convFactor;
}
@end

@implementation MMPPdNumber2

#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  //[self sendFloat:self.value];
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.value = received;
  //[self sendFloat:self.value];
}

#pragma mark Touches

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
  UITouch *touch = [touches anyObject];
  CGPoint pos = [touch locationInView:self];
  CGFloat diff = (self->touchPrevY - pos.y) / self.gui.scaleY; // Added div by scale.
  if(diff != 0) {
    double k2 = 1.0;
    double v = self.value;
    if(!isOneFinger) {
      k2 = 0.01;
    }
    if(self.log) {
      v *= pow(convFactor, -k2 * diff);
    }
    else {
      v += k2 * diff;
    }
    self.value = v;
    [self sendFloat:self.value];
  }
  touchPrevY = pos.y;
}

@end
