//
//  MMPGui.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPGui.h"

// MobMuPlat additions.
#import "MMPPdObjectBox.h"
#import "MMPPdMessageBox.h"

// MobMuPlat gui object subclasses
#import "MMPPdNumber.h"
#import "MMPPdSymbol.h"
#import "MMPPdBang.h"
#import "MMPPdSlider.h"
#import "MMPPdRadio.h"
#import "MMPPdNumber2.h"
#import "MMPPdToggle.h"

@implementation MMPGui

- (BOOL)addObjectType:(NSString *)type fromAtomLine:(NSArray *)atomLine {
  BOOL retVal = [super addObjectType:type fromAtomLine:atomLine];
  if (retVal) return YES; // super handled it
  // Render object box
  [self addMMPPdObjectBox:atomLine];
  return YES;
}

- (void)addWidgetsFromAtomLines:(NSArray *)lines {
  [super addWidgetsFromAtomLines:lines];
  // super handles calling |addObjectType|.
  // MMP wants a few more things.

  int level = 0;
  for(NSArray *line in lines) {

    if(line.count >= 4) {

      NSString *lineType = [line objectAtIndex:1];

      // find canvas begin and end line
      if([lineType isEqualToString:@"canvas"]) {
        level++;
      }
      else if([lineType isEqualToString:@"restore"]) {
        level -= 1;
        // TODO render pd box as box, but not a graph/canvas!
        if (level == 1) {
          // render object [pd name]
          [self addMMPPdObjectBox:line];
        }
      }
      else if([lineType isEqualToString:@"msg"]) {
        [self addMMPPdMessageBox:line]; //
      }
    }
  }
}

#

- (void)addMMPPdObjectBox:(NSArray *)atomLine {
  MMPPdObjectBox *objectBox = [MMPPdObjectBox objectBoxFromAtomLine:atomLine withGui:self];
  if (objectBox) {
    [self.widgets addObject:objectBox];
  }
}

- (void)addMMPPdMessageBox:(NSArray *)atomLine {
  MMPPdMessageBox *messageBox = [MMPPdMessageBox messageBoxFromAtomLine:atomLine withGui:self];
  if (messageBox) {
    [self.widgets addObject:messageBox];
  }
}


// Override super to add MMP subclasses.
#pragma mark Add Widgets

- (void)addNumber:(NSArray *)atomLine {
  MMPPdNumber *n = [MMPPdNumber numberFromAtomLine:atomLine withGui:self];
  if(n) {
    [self.widgets addObject:n];
    DDLogVerbose(@"Gui: added %@", n.type);
  }
}

- (void)addSymbol:(NSArray *)atomLine {
  MMPPdSymbol *s = [MMPPdSymbol symbolFromAtomLine:atomLine withGui:self];
  if(s) {
    [self.widgets addObject:s];
    DDLogVerbose(@"Gui: added %@", s.type);
  }
}

- (void)addBang:(NSArray *)atomLine {
  MMPPdBang *b = [MMPPdBang bangFromAtomLine:atomLine withGui:self];
  if(b) {
    [self.widgets addObject:b];
    DDLogVerbose(@"Gui: added %@", b.type);
  }
}

- (void)addToggle:(NSArray *)atomLine {
  MMPPdToggle *t = [MMPPdToggle toggleFromAtomLine:atomLine withGui:self];
  if(t) {
    [self.widgets addObject:t];
    DDLogVerbose(@"Gui: added %@", t.type);
  }
}

- (void)addNumber2:(NSArray *)atomLine {
  MMPPdNumber2 *n = [MMPPdNumber2 number2FromAtomLine:atomLine withGui:self];
  if(n) {
    [self.widgets addObject:n];
    DDLogVerbose(@"Gui: added %@", n.type);
  }
}

/*- (void)addSlider:(NSArray *)atomLine withOrientation:(WidgetOrientation)orientation {
  Slider *s = [Slider sliderFromAtomLine:atomLine withOrientation:orientation withGui:self];
  if(s) {
    [self.widgets addObject:s];
    DDLogVerbose(@"Gui: added %@", s.type);
  }
}

- (void)addRadio:(NSArray *)atomLine withOrientation:(WidgetOrientation)orientation {
  Radio *r = [Radio radioFromAtomLine:atomLine withOrientation:orientation withGui:self];
  if(r) {
    [self.widgets addObject:r];
    DDLogVerbose(@"Gui: added %@", r.type);
  }
}*/

@end
