//
//  MMPPdObjectBox.m
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPPdObjectBox.h"

#import "Gui.h"

@implementation MMPPdObjectBox

+ (id)objectBoxFromAtomLine:(NSArray *)line withGui:(Gui *)gui {
  MMPPdObjectBox *ob = [[MMPPdObjectBox alloc] init];


  ob.originalFrame = CGRectMake(
                               [[line objectAtIndex:2] floatValue], [[line objectAtIndex:3] floatValue],
                               0,0); //deduce width and height...

  ob.valueLabel.text = [[line subarrayWithRange:NSMakeRange(4, [line count] - 4)] componentsJoinedByString:@" "];  //check size
  //t.backgroundColor = [UIColor colorWithRed:0 green:0 blue:1 alpha:.2];
  return ob;
}

- (void)drawRect:(CGRect)rect {

  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextTranslateCTM(context, 0.5, 0.5); // snap to nearest pixel
  CGContextSetLineWidth(context, 1.0);

  // bounds as path
  CGMutablePathRef path = CGPathCreateMutable();
  CGPathMoveToPoint(path, NULL, 0, 0);
  CGPathAddLineToPoint(path, NULL, rect.size.width-1, 0);
  CGPathAddLineToPoint(path, NULL, rect.size.width-1, rect.size.height-1);
  CGPathAddLineToPoint(path, NULL, 0, rect.size.height-1);
  CGPathAddLineToPoint(path, NULL, 0, 0);

  // background
  CGContextSetFillColorWithColor(context, self.backgroundColor.CGColor);
  CGContextAddPath(context, path);
  CGContextFillPath(context);

  // border
  CGContextSetStrokeColorWithColor(context, self.frameColor.CGColor);
  CGContextAddPath(context, path);
  CGContextStrokePath(context);

  CGPathRelease(path);
}

- (void)reshapeForGui:(Gui *)gui { //don't use self.valueWidth

  // value label
  self.valueLabel.font = [UIFont fontWithName:GUI_FONT_NAME size:gui.fontSize * gui.scaleX];
  CGSize charSize = [@"0" sizeWithFont:self.valueLabel.font]; // assumes monspaced font
  self.valueLabel.preferredMaxLayoutWidth = charSize.width * MAX(self.label.text.length,3) + 1;//self.valueWidth;
  [self.valueLabel sizeToFit];
  CGRect valueLabelFrame = self.valueLabel.frame;
  if(valueLabelFrame.size.width < self.valueLabel.preferredMaxLayoutWidth) {
    // make sure width matches valueWidth
    valueLabelFrame.size.width = self.valueLabel.preferredMaxLayoutWidth;
  }
  valueLabelFrame.origin = CGPointMake(round(gui.scaleX), round(gui.scaleX));
  self.valueLabel.frame = valueLabelFrame;

  // bounds from value label size
  self.frame = CGRectMake(
                          round(self.originalFrame.origin.x * gui.scaleX),
                          round(self.originalFrame.origin.y * gui.scaleY),
                          round(CGRectGetWidth(self.valueLabel.frame) + (2 * gui.scaleX)),
                          round(CGRectGetHeight(self.valueLabel.frame) + (2 * gui.scaleX)));
}




@end
