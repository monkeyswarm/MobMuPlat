//
//  MMPPdSlider.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdSlider.h"
#import "Gui.h"

@implementation MMPPdSlider


// TODO pd renders sliders weird, which means that something aligned in pd desktop gui is not aligned here.
/*- (void)reshape {
  [super reshape];
  // MMP: tweak to add some left or top padding (horizontal gets -5 points, vertical gets -2 top, pre scaling);
  CGRect frame = self.frame;
  if (self.orientation == WidgetOrientationHorizontal) {
    frame.origin.x -= 3 * self.gui.scaleX;
  } else {
    frame.origin.y -= 2 * self.gui.scaleY;
  }
 // TODO also alter the width/length
  self.frame = frame;
}*/

#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  // No-op, don't send out.
  //[self sendFloat:self.value];
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.value = received;
  //[super sendFloat:received]; // Don't send out.
}

@end
