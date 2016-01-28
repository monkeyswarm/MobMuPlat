//
//  MMPPdNumber.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdNumber.h"

#import "Gui.h"

// Expose superclass instance variables for use in touch drag computation.
@interface Number () {
  @protected
  int touchPrevY;
  bool isOneFinger;
}
@end


@implementation MMPPdNumber

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

#pragma mark Touches

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
	UITouch *touch = [touches anyObject];
	CGPoint pos = [touch locationInView:self];
	CGFloat diff = (self->touchPrevY - pos.y) / self.gui.scaleY; // Added div by scale.
	if(diff != 0) {
		if(isOneFinger) {
			self.value = self.value + diff;
		}
		else {
			// mult & divide by ints to avoid float rounding errors ...
			self.value = ((self.value*100) + (double) ((diff * 10) / 1000.f)*100)/100;
		}
		[self sendFloat:self.value];
	}
	touchPrevY = pos.y;
}

@end
