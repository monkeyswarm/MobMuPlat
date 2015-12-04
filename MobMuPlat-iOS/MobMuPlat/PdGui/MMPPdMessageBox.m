//
//  MMPPdMessageBox.m
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPPdMessageBox.h"

#import "Gui.h"

#define INDENT 4

@implementation MMPPdMessageBox {
  CGFloat _strokeWidth;
  Gui *_gui;
}

+ (id)messageBoxFromAtomLine:(NSArray *)line withGui:(Gui *)gui {
  MMPPdMessageBox *mb = [[MMPPdMessageBox alloc] initWithGui:gui];


  mb.originalFrame = CGRectMake(
                               [[line objectAtIndex:2] floatValue], [[line objectAtIndex:3] floatValue],
                               0,0); //deduce width and height...

  mb.valueLabel.text = [[line subarrayWithRange:NSMakeRange(4, [line count] - 6)] componentsJoinedByString:@" "];  //check size

  //last two atoms (added via post-MMP-processing) will be the send/rec names
  mb.sendName = [line objectAtIndex:[line count]-2];
  mb.receiveName = [line objectAtIndex:[line count]-1];
  return mb;
}

- (instancetype)initWithGui:(Gui *)gui {
  self = [super init];
  if (self) {
    _gui = gui;
    _strokeWidth = 1;
  }
  return self;
}

- (void)drawRect:(CGRect)rect {

  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextTranslateCTM(context, 0.5, 0.5); // snap to nearest pixel
  CGContextSetLineWidth(context, 1.0);

  // bounds as path
  CGMutablePathRef path = CGPathCreateMutable();
  CGFloat indent = INDENT * _gui.scaleX;
  CGPathMoveToPoint(path, NULL, 0, 0);
  CGPathAddLineToPoint(path, NULL, rect.size.width-1, 0);
  CGPathAddLineToPoint(path, NULL, rect.size.width-1-indent, indent);
  CGPathAddLineToPoint(path, NULL, rect.size.width-1-indent, rect.size.height-1-indent);
  CGPathAddLineToPoint(path, NULL, rect.size.width-1, rect.size.height-1);
  CGPathAddLineToPoint(path, NULL, 0, rect.size.height-1);
  CGPathAddLineToPoint(path, NULL, 0, 0);

  // background
  CGContextSetFillColorWithColor(context, self.backgroundColor.CGColor);
  CGContextAddPath(context, path);
  CGContextFillPath(context);

  // border
  CGContextSetLineWidth(context, _strokeWidth);
  CGContextSetStrokeColorWithColor(context, self.frameColor.CGColor);
  CGContextAddPath(context, path);
  CGContextStrokePath(context);

  CGPathRelease(path);
}

- (void)reshapeForGui:(Gui *)gui { //don't use self.valueWidth
  // value label
  self.valueLabel.font = [UIFont fontWithName:GUI_FONT_NAME size:gui.fontSize * gui.scaleX];
  CGSize charSize = [@"0" sizeWithFont:self.valueLabel.font]; // assumes monspaced font
  self.valueLabel.preferredMaxLayoutWidth = charSize.width * MAX(self.label.text.length,3);
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
                          round(CGRectGetWidth(self.valueLabel.frame) + (2 * gui.scaleX) +INDENT ),
                          round(CGRectGetHeight(self.valueLabel.frame) + (2 * gui.scaleX)));
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
  [self sendBang];
  _strokeWidth = 5;
  [self setNeedsDisplay];
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(.25 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
    _strokeWidth = 1;
    [self setNeedsDisplay];
  });
}

#pragma mark Widget Listener

- (void)receiveMessage:(NSString *)message withArguments:(NSArray *)arguments fromSource:(NSString *)source {

  // set message sets value without sending
  if([message isEqualToString:@"set"] && arguments.count > 0) {
    self.valueLabel.text = [arguments componentsJoinedByString:@" "];
    [self reshapeForGui:_gui]; //TODO DEi this breaks showing this in multiple guis.
    [self setNeedsDisplay]; //check if needed
  }
}

@end
