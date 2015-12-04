//
//  MMPMenuButton.m
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPMenuButton.h"

#define PADDING_PERCENT .1
#define INTRABAR_PERCENT .1

@implementation MMPMenuButton {
  UIColor *_barColor;
}

- (void)drawRect:(CGRect)rect {
  CGContextRef context = UIGraphicsGetCurrentContext();
  //CGContextTranslateCTM(context, 0.5, 0.5); // snap to nearest pixel
  //CGContextSetLineWidth(context, 1.0);

  CGContextSetFillColorWithColor(context, _barColor.CGColor);
  CGFloat barHorizPadding = PADDING_PERCENT * rect.size.width;
  CGFloat barVerticalPadding = PADDING_PERCENT * rect.size.height;
  CGFloat intraBarHeight = INTRABAR_PERCENT * rect.size.height;
  CGFloat barHeight = (rect.size.height - intraBarHeight * 2 - barVerticalPadding * 2) / 3.0f;
  CGFloat barWidth = rect.size.width - barHorizPadding * 2;
  for (NSUInteger i=0;i<3;i++) {
    CGRect barFrame =
        CGRectMake(barHorizPadding, barVerticalPadding + i * (barHeight + intraBarHeight), barWidth, barHeight);
    CGContextFillRect(context, barFrame);
  }
}

- (void)setBarColor:(UIColor *)color {
  _barColor = color;
  [self setNeedsDisplay];
}

@end
