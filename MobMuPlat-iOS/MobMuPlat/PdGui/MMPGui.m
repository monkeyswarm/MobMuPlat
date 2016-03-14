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
#import "MMPPdArrayWidget.h"

// MobMuPlat gui object subclasses
#import "MMPPdNumber.h"
#import "MMPPdSymbol.h"
#import "MMPPdBang.h"
#import "MMPPdSlider.h"
#import "MMPPdRadio.h"
#import "MMPPdNumber2.h"
#import "MMPPdToggle.h"
#import "MMPPdComment.h"

@implementation MMPGui {
  BOOL _inLevel2CanvasShowingArray;
}

- (BOOL)addObjectType:(NSString *)type fromAtomLine:(NSArray *)atomLine {
  BOOL retVal = [super addObjectType:type fromAtomLine:atomLine];
  if (retVal) return YES; // super handled it

  // check against restore graph, etc...
  //if ([type isEqualToString:@"obj"]) {
    // Render object box
    [self addMMPPdObjectBox:atomLine];
    return YES;
   /*else if ([type isEqualToString:@"array"]) {
    // Render graphical array.
    [self addMMPPdArrayWidget:atomLine];
  } else {
    return NO;
  }*/
  return NO;
}

- (void)addWidgetsFromAtomLines:(NSArray *)lines {
  [super addWidgetsFromAtomLines:lines];
  // super handles calling |addObjectType|.
  // MMP wants a few more things.

  int level = 0;

  for(NSUInteger lineIndex = 0; lineIndex<lines.count; lineIndex++) {
    NSArray* line = lines[lineIndex];
    if(line.count >= 4) {

      NSString *lineType = [line objectAtIndex:1];

      // find canvas begin and end line
      if([lineType isEqualToString:@"canvas"]) {
        level++;

      }
      else if([lineType isEqualToString:@"restore"]) {
        level -= 1;
        if (level == 1 && !_inLevel2CanvasShowingArray) {
          // render object [pd name]
          [self addMMPPdObjectBox:line];
        }
        _inLevel2CanvasShowingArray = NO;
      }
      else if([lineType isEqualToString:@"msg"] && level==1) {
        [self addMMPPdMessageBox:line];
      }
      //
      else if([lineType isEqualToString:@"array"] && level==2) {
        _inLevel2CanvasShowingArray = YES;
        //expect
        // #X array <arrayname> 100 float 3; (this line) last element is bit mask of save/no save and draw type (poly,points,bez)
        // #A <values, if save contents is on>
        // #A ...
        // ...
        // #X coords 0 1 100 -1 300 140 1 0 0;
        // #X restore 8 17 graph;
        //BOOL willHaveSaveData = ([line[5] integerValue] & 0x1) == 1;

        // scan over #A lines. We don't need to track array values here.
        lineIndex++;
        while ([lines[lineIndex][0] isEqualToString:@"#A"]) { //Add bounds checking
          lineIndex++;
        }
        // line index is now at "#X coords"
        NSArray *arrayCoordsLine = lines[lineIndex];
        NSArray *arrayRestoreLine = lines[lineIndex+1];
        [self addMMPPdArrayWidget:line coordsLine:arrayCoordsLine restoreLine:arrayRestoreLine];
      }
    }
  }
}

#

- (void)addMMPPdObjectBox:(NSArray *)atomLine {
  MMPPdObjectBox *objectBox = [[MMPPdObjectBox alloc] initWithAtomLine:atomLine andGui:self];
  if (objectBox) {
    [self.widgets addObject:objectBox];
  }
}

- (void)addMMPPdMessageBox:(NSArray *)atomLine {
  MMPPdMessageBox *messageBox = [[MMPPdMessageBox alloc] initWithAtomLine:atomLine andGui:self];
  if (messageBox) {
    [self.widgets addObject:messageBox];
  }
}

- (void)addMMPPdArrayWidget:(NSArray *)atomLine
                 coordsLine:(NSArray *)coordsLine
                restoreLine:(NSArray *)restoreLine {
  MMPPdArrayWidget *arrayWidget = [[MMPPdArrayWidget alloc] initWithAtomLine:atomLine
                                                                  coordsLine:coordsLine
                                                                 restoreLine:restoreLine
                                                                      andGui:self];
  if (arrayWidget) {
    [self.widgets addObject:arrayWidget];
  }
}

// Override super to add MMP subclasses.
#pragma mark Add Widgets

- (void)addNumber:(NSArray *)atomLine {
  MMPPdNumber *n = [[MMPPdNumber alloc] initWithAtomLine:atomLine andGui:self];
  if(n) {
    [self.widgets addObject:n];
    DDLogVerbose(@"Gui: added %@", n.type);
  }
}

- (void)addSymbol:(NSArray *)atomLine {
  MMPPdSymbol *s = [[MMPPdSymbol alloc] initWithAtomLine:atomLine andGui:self];
  if(s) {
    [self.widgets addObject:s];
    DDLogVerbose(@"Gui: added %@", s.type);
  }
}

- (void)addBang:(NSArray *)atomLine {
  MMPPdBang *b = [[MMPPdBang alloc] initWithAtomLine:atomLine andGui:self];
  if(b) {
    [self.widgets addObject:b];
    DDLogVerbose(@"Gui: added %@", b.type);
  }
}

- (void)addToggle:(NSArray *)atomLine {
  MMPPdToggle *t = [[MMPPdToggle alloc] initWithAtomLine:atomLine andGui:self];
  if(t) {
    [self.widgets addObject:t];
    DDLogVerbose(@"Gui: added %@", t.type);
  }
}

- (void)addNumber2:(NSArray *)atomLine {
  MMPPdNumber2 *n = [[MMPPdNumber2 alloc] initWithAtomLine:atomLine andGui:self];
  if(n) {
    [self.widgets addObject:n];
    DDLogVerbose(@"Gui: added %@", n.type);
  }
}

- (void)addSlider:(NSArray *)atomLine withOrientation:(WidgetOrientation)orientation {
  MMPPdSlider *s = [[MMPPdSlider alloc] initWithAtomLine:atomLine andGui:self];
  s.orientation = orientation;
  if(s) {
    [self.widgets addObject:s];
    DDLogVerbose(@"Gui: added %@", s.type);
  }
}

- (void)addRadio:(NSArray *)atomLine withOrientation:(WidgetOrientation)orientation {
  MMPPdRadio *r = [[MMPPdRadio alloc] initWithAtomLine:atomLine andGui:self];
  r.orientation = orientation;
  if(r) {
    [self.widgets addObject:r];
    DDLogVerbose(@"Gui: added %@", r.type);
  }
}

- (void)addComment:(NSArray *)atomLine {
  MMPPdComment *c = [[MMPPdComment alloc] initWithAtomLine:atomLine andGui:self];
  if(c) {
    [self.widgets addObject:c];
    DDLogVerbose(@"Gui: added %@", c.type);
  }
}

@end
