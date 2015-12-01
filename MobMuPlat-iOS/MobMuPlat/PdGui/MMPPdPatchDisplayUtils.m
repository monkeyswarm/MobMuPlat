//
//  MMPPdPatchDisplayUtils.m
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPPdPatchDisplayUtils.h"

@implementation MMPPdPatchDisplayUtils

+ (NSArray *)proccessNativeWidgetsFromAtomLines:(NSArray *)lines {
  int level = 0;
  NSUInteger index = 0;
  NSMutableArray *result = [NSMutableArray array];

  for(NSArray *line in lines) {

    if(line.count >= 4) {

      NSString *lineType = [line objectAtIndex:1];

      // find canvas begin and end line
      if([lineType isEqualToString:@"canvas"]) {
        level++;
        if(level == 1) {
          [result addObject:line];
        }
      }
      else if([lineType isEqualToString:@"restore"]) {
        level -= 1;
        [result addObject:line];
      }
      // find different types of UI element in the top level patch
      else if(level == 1) {
        if (line.count >= 2) {

          // built in pd things
          if([lineType isEqualToString:@"floatatom"]) {
            [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:10 recNameIndex:9]];
            //[self addNumber:line];
          }
          else if([lineType isEqualToString:@"symbolatom"]) {
            [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:10 recNameIndex:9]];
          }
          else if([lineType isEqualToString:@"text"]) {
            [result addObject:line];
          }
          else if([lineType isEqualToString:@"obj"] && line.count >= 5) {
            NSString *objType = [line objectAtIndex:4];

            // iem gui objects
            if([objType isEqualToString:@"bng"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:9 recNameIndex:10]];
            }
            else if([objType isEqualToString:@"tgl"]) {
              //[self addToggle:line];
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:7 recNameIndex:8]];
            }
            else if([objType isEqualToString:@"nbx"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:11 recNameIndex:12]];
              //[self addNumberbox2:line];
            }
            else if([objType isEqualToString:@"hsl"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:11 recNameIndex:12]];
              //[self addSlider:line withOrientation:WidgetOrientationHorizontal];
            }
            else if([objType isEqualToString:@"vsl"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:11 recNameIndex:12]];
              //[result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++]];
              //[self addSlider:line withOrientation:WidgetOrientationVertical];
            }
            else if([objType isEqualToString:@"hradio"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:9 recNameIndex:10]];
              //[self addRadio:line withOrientation:WidgetOrientationHorizontal];
            }
            else if([objType isEqualToString:@"vradio"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:9 recNameIndex:10]];
              //[self addRadio:line withOrientation:WidgetOrientationVertical];
            }
            else if([objType isEqualToString:@"vu"]) {
              //[self addVUMeter:line];
            }
            else if([objType isEqualToString:@"cnv"]) {
              //[self addCanvas:line];
            } else { //regular obj
              // put all other lines at level 1back in DEI refactor!
              [result addObject:line];
            }
          } else {
            // put all other lines at level 1back in DEI refactor!
            [result addObject:line];
          }
        }
      } else {
        // put all other lines ato other levels back in DEI refactro!
        [result addObject:line];
      }
    }
  }
  return result;
}

+ (NSArray *)shimAtomLineFromAtomLine:(NSArray *)atomLine guiIndex:(NSUInteger)index sendNameIndex:(NSUInteger)sendNameIndex recNameIndex:(NSUInteger)recNameIndex{
  //DEI check length
  NSMutableArray *result = [[atomLine subarrayWithRange:NSMakeRange(0, 4)] mutableCopy]; //copy first 4
  [result setObject:@"obj" atIndexedSubscript:1]; //change floatatom,etc to obj
  // Bang gets special blocking shim, all others respect "set" and get default shim
  if ([atomLine[4] isEqualToString:@"bng"]) {
    [result addObject:@"MMPGuiUnSet"];
  } else {
    [result addObject: @"MMPGui"];
  }
  [result addObject: [NSString stringWithFormat:@"%lu-gui-send", (unsigned long)index]];//send name
  [result addObject: [NSString stringWithFormat:@"%lu-gui-rec", index]];
  [result addObject: [self shimSanitizeAtom:atomLine[sendNameIndex]]];
  [result addObject: [self shimSanitizeAtom:atomLine[recNameIndex]]]; //Check range
  return result;
}

+ (NSString *)shimSanitizeAtom:(NSString *)atom {
  if ([atom characterAtIndex:[atom length]-1] == ',') { //strip off trailing comma (from floatatom line)
    return [atom substringToIndex:[atom length]-1];
  } else {
    return atom;
  }
}

+ (NSArray *)proccessGuiWidgetsFromAtomLines:(NSArray *)lines {
  int level = 0;
  NSUInteger index = 0;
  NSMutableArray *result = [NSMutableArray array];

  for(NSArray *line in lines) {

    if(line.count >= 4) {

      NSString *lineType = [line objectAtIndex:1];

      // find canvas begin and end line
      if([lineType isEqualToString:@"canvas"]) {
        level++;
        if(level == 1) {
          [result addObject:line];
        }
      }
      else if([lineType isEqualToString:@"restore"]) {
        level -= 1;
        [result addObject:line];
      }
      // find different types of UI element in the top level patch
      else if(level == 1) {
        if (line.count >= 2) {

          // built in pd things
          if([lineType isEqualToString:@"floatatom"]) {
            [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:10 recNameIndex:9]];
          }
          else if([lineType isEqualToString:@"symbolatom"]) {
            [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:10 recNameIndex:9]];
          }
          else if([lineType isEqualToString:@"text"]) {
            [result addObject:line]; // no gui index increment
          }
          else if([lineType isEqualToString:@"obj"] && line.count >= 5) {
            NSString *objType = [line objectAtIndex:4];

            // iem gui objects
            if([objType isEqualToString:@"bng"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:9 recNameIndex:10]];
            }
            else if([objType isEqualToString:@"tgl"]) {
              //[self addToggle:line];
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:7 recNameIndex:8]];
            }
            else if([objType isEqualToString:@"nbx"]) {
              //[self addNumberbox2:line];
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:11 recNameIndex:12]];
            }
            else if([objType isEqualToString:@"hsl"]) {
              //[self addSlider:line withOrientation:WidgetOrientationHorizontal];
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:11 recNameIndex:12]];
            }
            else if([objType isEqualToString:@"vsl"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:11 recNameIndex:12]];

              //[self addSlider:line withOrientation:WidgetOrientationVertical];
            }
            else if([objType isEqualToString:@"hradio"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:9 recNameIndex:10]];

              //[self addRadio:line withOrientation:WidgetOrientationHorizontal];
            }
            else if([objType isEqualToString:@"vradio"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index++ sendNameIndex:9 recNameIndex:10]];

              //[self addRadio:line withOrientation:WidgetOrientationVertical];
            }
            else if([objType isEqualToString:@"vu"]) {
              //[self addVUMeter:line];
            }
            else if([objType isEqualToString:@"cnv"]) {
              //[self addCanvas:line];
            } else {
              // custom - all other obj
              [result addObject:line];
            }
          }
        }
      }
    }
  }
  return result;
}

+ (NSArray *)guiAtomLineFromAtomLine:(NSArray *)atomLine guiIndex:(NSUInteger)index sendNameIndex:(NSUInteger)sendNameIndex recNameIndex:(NSUInteger)recNameIndex {
  //DEI check length
  NSMutableArray *result = [atomLine mutableCopy];
  result[sendNameIndex] = [NSString stringWithFormat:@"%lu-gui-send", (unsigned long)index];//send name
  result[recNameIndex] = [NSString stringWithFormat:@"%lu-gui-rec", (unsigned long)index];
  return result;
}


@end
