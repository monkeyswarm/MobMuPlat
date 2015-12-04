//
//  DanControl.m
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/21/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//
//  Superclass for all GUI widgets. Should not be instantiatied itself.

#import "MeControl.h"


@implementation MeControl
@synthesize address, color, controlDelegate;


+(UIColor*)colorFromRGBArray:(NSArray*)rgbArray{
    return [UIColor colorWithRed:[[rgbArray objectAtIndex:0] floatValue] green:[[rgbArray objectAtIndex:1] floatValue] blue:[[rgbArray objectAtIndex:2] floatValue] alpha:1];
}

+(UIColor*)inverseColorFromRGBArray:(NSArray*)rgbArray{
  return [UIColor colorWithRed:fmodf([[rgbArray objectAtIndex:0] floatValue] + .5, 1.0f) green:fmodf([[rgbArray objectAtIndex:1] floatValue] + .5, 1.0f) blue:fmodf([[rgbArray objectAtIndex:2]  floatValue] + .5, 1.0f) alpha:1];
}

+(UIColor*)colorFromRGBAArray:(NSArray*)rgbaArray{
    return [UIColor colorWithRed:[[rgbaArray objectAtIndex:0] floatValue] green:[[rgbaArray objectAtIndex:1] floatValue] blue:[[rgbaArray objectAtIndex:2] floatValue] alpha:[[rgbaArray objectAtIndex:3] floatValue]];
}

//"receiveList" can be called on MeControl, even though it is overridden by all subclasses
-(void)receiveList:(NSArray*)inArray{
  if ([inArray count] >= 2 &&
      [inArray[0] isKindOfClass:[NSString class]] &&
      [inArray[0] isEqualToString:@"enable"] &&
      [inArray[1] isKindOfClass:[NSNumber class]]) {
    self.enabled = ([inArray[1] floatValue] > 0);
  }
}

- (void)setEnabled:(BOOL)enabled {
  [super setEnabled:enabled];
  self.alpha = enabled ? 1 : .2;
}


@end
