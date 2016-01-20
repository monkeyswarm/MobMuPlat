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

//+ (id)objectBoxFromAtomLine:(NSArray *)line withGui:(Gui *)gui {
- (id)initWithAtomLine:(NSArray *)line andGui:(Gui *)gui {
  if(line.count < 5) { // sanity check
    DDLogWarn(@"MMPPdObjectBox: cannot create, atom line length < 5");
    return nil;
  }
  self = [super initWithAtomLine:line andGui:gui];
  if(self) {


  self.originalFrame = CGRectMake(
                               [[line objectAtIndex:2] floatValue], [[line objectAtIndex:3] floatValue],
                               0,0); //deduce width and height...

  self.valueLabel.text = [[line subarrayWithRange:NSMakeRange(4, [line count] - 4)] componentsJoinedByString:@" "];  //check size
  }
  return self;
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

- (void)reshape { //don't use self.valueWidth

  // value label
  self.valueLabel.font = [UIFont fontWithName:GUI_FONT_NAME size:self.gui.fontSize * self.gui.scaleX];
  CGSize charSize = [@"0" sizeWithFont:self.valueLabel.font]; // assumes monspaced font
  self.valueLabel.preferredMaxLayoutWidth = charSize.width * MAX(self.label.text.length,3) + 1;//self.valueWidth;
  [self.valueLabel sizeToFit];
  CGRect valueLabelFrame = self.valueLabel.frame;
  if(valueLabelFrame.size.width < self.valueLabel.preferredMaxLayoutWidth) {
    // make sure width matches valueWidth
    valueLabelFrame.size.width = self.valueLabel.preferredMaxLayoutWidth;
  }
  valueLabelFrame.origin = CGPointMake(round(self.gui.scaleX), round(self.gui.scaleX));
  self.valueLabel.frame = valueLabelFrame;

  // bounds from value label size
  self.frame = CGRectMake(
                          round(self.originalFrame.origin.x * self.gui.scaleX),
                          round(self.originalFrame.origin.y * self.gui.scaleY),
                          round(CGRectGetWidth(self.valueLabel.frame) + (2 * self.gui.scaleX)),
                          round(CGRectGetHeight(self.valueLabel.frame) + (2 * self.gui.scaleX)));
}




@end
