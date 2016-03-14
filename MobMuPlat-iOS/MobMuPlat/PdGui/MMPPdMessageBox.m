//
//  MMPPdMessageBox.m
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPPdMessageBox.h"

#import "Gui.h"

#define FIN_INDENT 4
#define RIGHT_PADDING 8

@implementation MMPPdMessageBox {
  CGFloat _strokeWidth;
  NSUInteger _numForcedLineBreaks;
}

- (id)initWithAtomLine:(NSArray *)line andGui:(Gui *)gui {
  if(line.count < 7) { // sanity check
   DDLogWarn(@"MMPPdMessageBox: cannot create, atom line length < 7");
   return nil;
  }
  self = [super initWithAtomLine:line andGui:gui];
  if(self) {
    _strokeWidth = 1;
    self.originalFrame = CGRectMake([[line objectAtIndex:2] floatValue],
                                    [[line objectAtIndex:3] floatValue],
                                    0,0); //deduce width and height when reshaping.

    //self.valueLabel.text =
      //  [[line subarrayWithRange:NSMakeRange(4, [line count] - 6)] componentsJoinedByString:@" "];  //check size
    //last two atoms (added via post-MMP-processing) will be the send/rec names!!!
    self.sendName = [line objectAtIndex:[line count]-2];
    self.receiveName = [line objectAtIndex:[line count]-1];

    self.valueLabel.numberOfLines = 0;
    self.valueLabel.lineBreakMode = NSLineBreakByWordWrapping;
    //self.valueLabel.backgroundColor = [UIColor redColor];
    [self setText:[line subarrayWithRange:NSMakeRange(4, line.count - 6)]];

  }
  return self;
}

- (void)setText:(NSArray *)textAtoms { //array of string or NSNumber float
  // create the comment string, handle escaped chars
  NSMutableString *text = [[NSMutableString alloc] init];
  BOOL appendSpace = NO;
  _numForcedLineBreaks = 0;

  for(int i = 0; i < textAtoms.count; i++) {
    NSString *textAtom;
    if ([textAtoms[i] isKindOfClass:[NSString class]]) {
      textAtom = (NSString *)textAtoms[i];
    } else if ([textAtoms[i] isKindOfClass:[NSNumber class]]) {
      textAtom = [NSString stringWithFormat:@"%@", textAtoms[i]];
    }

    if([textAtom isEqualToString:@"\\,"]) {
      [text appendString:@","];
    }
    else if([textAtom isEqualToString:@"\\;"]) {
      if (i==textAtoms.count-1) { //last element, don't append newline
        [text appendString:@";"];
      } else {
        [text appendString:@";\n"]; // semi ; force a line break in pd gui
        _numForcedLineBreaks++;
      }
      appendSpace = NO;
    }
    else if([textAtom isEqualToString:@"\\$"]) {
      [text appendString:@"$"];
    }
    else {
      if(appendSpace) {
        [text appendString:@" "];
      }
      appendSpace = YES;
      [text appendString:textAtom];
    }
  }
  self.valueLabel.text = text;
}

- (void)drawRect:(CGRect)rect {

  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextTranslateCTM(context, 0.5, 0.5); // snap to nearest pixel
  CGContextSetLineWidth(context, 1.0);

  // bounds as path
  CGMutablePathRef path = CGPathCreateMutable();
  CGFloat indent = FIN_INDENT * self.gui.scaleX;
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

- (void)reshape { //don't use self.valueWidth
  // value label
  self.valueLabel.font = [UIFont fontWithName:GUI_FONT_NAME size:self.gui.fontSize * self.gui.scaleX];
  CGSize charSize = [@"0" sizeWithFont:self.valueLabel.font]; // assumes monspaced font
  //NSUInteger lineWidth = MIN(MAX(self.valueLabel.text.length,3), GUI_LINE_WRAP);
  self.valueLabel.preferredMaxLayoutWidth = charSize.width * (GUI_LINE_WRAP - 1);
  CGSize maxLabelSize;
  maxLabelSize.width = charSize.width * (GUI_LINE_WRAP - 1);
  if(self.valueLabel.text.length > GUI_LINE_WRAP) { // force line wrapping based on size
    maxLabelSize.height = charSize.height * ((self.valueLabel.text.length / (GUI_LINE_WRAP - 1) + 1));
  }
  else {
    maxLabelSize.height = charSize.height;
  }
  if(_numForcedLineBreaks > 0) {
    maxLabelSize.height += charSize.height * _numForcedLineBreaks;
  }
  CGRect labelFrame = self.valueLabel.frame;
  labelFrame.size = [self.valueLabel.text sizeWithFont:self.valueLabel.font
                                constrainedToSize:maxLabelSize
                                    lineBreakMode:self.valueLabel.lineBreakMode];

  CGFloat padding = self.gui.scaleX;
  self.valueLabel.frame =
      CGRectMake(padding+1, padding, labelFrame.size.width, labelFrame.size.height);

  // bounds based on computed label size
  self.frame = CGRectMake(
                          round(self.originalFrame.origin.x * self.gui.scaleX),
                          round(self.originalFrame.origin.y * self.gui.scaleY),
                          CGRectGetWidth(self.valueLabel.frame)+ padding*2 + 1 + RIGHT_PADDING*self.gui.scaleX ,
                          CGRectGetHeight(self.valueLabel.frame)+ padding*4);

  //
  /*[self.valueLabel sizeToFit];

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
                          round(CGRectGetWidth(self.valueLabel.frame) + (2 * self.gui.scaleX) +INDENT ),
                          round(CGRectGetHeight(self.valueLabel.frame) + (4 * self.gui.scaleY))); //MMP was 2 * scaleX*/
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
    //self.valueLabel.text = [arguments componentsJoinedByString:@" "];
    [self setText:arguments];
    [self reshape]; //TODO DEi this breaks showing this in multiple guis.
    [self setNeedsDisplay]; //check if needed
  }
}

@end
