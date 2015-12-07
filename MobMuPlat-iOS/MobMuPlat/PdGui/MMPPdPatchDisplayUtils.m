//
//  MMPPdPatchDisplayUtils.m
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPPdPatchDisplayUtils.h"

@implementation MMPPdPatchDisplayUtils

// Return two-item tuple
// [0] = processed lines to be written as the pd patch to execute
// [1] = proccessed lines to generate to the MMP GUI
+ (NSArray *)proccessAtomLines:(NSArray *)lines {
  NSMutableArray *patchLines = [NSMutableArray array];
  NSMutableArray *guiLines = [NSMutableArray array];
  NSUInteger level = 0;
  NSUInteger objIndex = 0;

  // Bookeeping for message box connections
  // key = message box index, val = set of connection tuples (obj index, outlet index) that connect into that message box.
  NSMutableDictionary *messageBoxIndexToIncomingConnectionIndices = [NSMutableDictionary dictionary];

  // Bookkeeping for grabbing send/rec names
  // key = linetype string, val = tuple of send and rec indices.

  NSDictionary *objTypeToSendRecIndeces = @{
                                             @"floatatom" : @[@(10),@(9)],
                                             @"symbolatom" : @[@(10),@(9)],
                                             @"bng" : @[@(9),@(10)],
                                             @"tgl" : @[@(7),@(8)],
                                             @"nbx" : @[@(11),@(12)],
                                             @"hsl" : @[@(11),@(12)],
                                             @"vsl" : @[@(11),@(12)],
                                             @"hradio" : @[@(9),@(10)],
                                             @"vradio" : @[@(9),@(10)]
                                             }; //TODO vu/canvas

  @try {
    for(NSArray *line in lines) {
      NSArray *patchLine = line;//[line copy];
      NSArray *guiLine = line;//[line copy];

      NSString *lineType = [line objectAtIndex:1]; // canvas/restore/obj/connect/floatatom/symbolatom
      // find canvas begin and end line
      if([lineType isEqualToString:@"canvas"]) {
        level++;
      }
      else if([lineType isEqualToString:@"restore"]) {
        level--;
      }
        // find different types of UI element in the top level patch
      else if(level == 1) {
        // built in pd things
        NSString *objType = [line[1] isEqualToString:@"obj"] ? line[4] : line[1];
        if (objTypeToSendRecIndeces[objType]) {
          // floatatom, symbolatom, bng, tgl, nbox, etc....
          NSArray *sendRecIndeces = objTypeToSendRecIndeces[objType];
          patchLine = [self shimAtomLineFromAtomLine:line
                                            guiIndex:objIndex
                                       sendNameIndex:[sendRecIndeces[0] unsignedIntegerValue]
                                        recNameIndex:[sendRecIndeces[1] unsignedIntegerValue]];
          guiLine = [self guiAtomLineFromAtomLine:line
                                         guiIndex:objIndex
                                    sendNameIndex:[sendRecIndeces[0] unsignedIntegerValue]
                                     recNameIndex:[sendRecIndeces[1] unsignedIntegerValue]];
        } else if ([objType isEqualToString:@"msg"]) {
          //
          [messageBoxIndexToIncomingConnectionIndices setObject:[NSMutableSet set]
                                                         forKey:[NSString stringWithFormat:@"%lu", objIndex]];
          //
          guiLine = [self guiMsgAtomLineFromAtomLine:line guiIndex:objIndex];
        } else if ([objType isEqualToString:@"connect"]) {
          // assume all obj boxes are created by this point
          if (messageBoxIndexToIncomingConnectionIndices[line[4]]) { //if dict contains (msg box index) as key
            NSMutableSet *connectionSet = messageBoxIndexToIncomingConnectionIndices[line[4]];
            [connectionSet addObject:@[ line[2], line[3] ]]; //add tuple of obj index string, outlet index string
          }
        } else {
          // "text", other "obj"...
        }
      }

      // add line to patch output
      [patchLines addObject:patchLine];
      [guiLines addObject:guiLine]; // todo, not necc if level > 1?
      if ([line[0] isEqualToString:@"#X"] && level == 1 && ![line[1] isEqualToString:@"connect"]) { //DEI just line index...
        objIndex++;
      }
    }
  } @catch (NSException *exception) {
    // catch out of bounds errors, return as bad load.
    return nil;
  }

  // Post-process the message boxes to connect to the message shims.
  for (NSNumber *msgBoxIndexNum in [messageBoxIndexToIncomingConnectionIndices keyEnumerator]) {
    // create shim
    NSInteger msgBoxIndex = [msgBoxIndexNum integerValue];
    [patchLines addObject:@[ @"#X", @"obj", @"0", @"0", @"MMPPdGuiFiles/MMPPdGuiMessageShim",
                         [NSString stringWithFormat:@"%lu-gui-send", msgBoxIndex],
                         [NSString stringWithFormat:@"%lu-gui-rec", msgBoxIndex]
                         ]];
    // for objects going into message box, connect them to shim as well
    NSSet *connectionSet = messageBoxIndexToIncomingConnectionIndices[msgBoxIndexNum];
    for (NSArray *connectionTuple in connectionSet) {
      [patchLines addObject:@[ @"#X", @"connect",
                           connectionTuple[0],
                           connectionTuple[1],
                           [NSString stringWithFormat:@"%ld", (long)objIndex],
                           @"0" ]];
    }

    // connect shim to message box
    [patchLines addObject:@[ @"#X", @"connect", [NSString stringWithFormat:@"%ld", (long)objIndex],
                         @"0",
                         [NSString stringWithFormat:@"%ld", (long)msgBoxIndex],
                         @"0"] ];
    // inc
    objIndex++;
  }

  return @[patchLines, guiLines];
}

/*+ (NSArray *)proccessNativeWidgetsFromAtomLines:(NSArray *)lines {
  int level = 0;
  NSUInteger index = 0;
  NSMutableArray *result = [NSMutableArray array];
  // key = message box index, val = set of connection tuples (obj index, outlet index) that connect into that message box.
  NSMutableDictionary *messageBoxIndexToIncomingConnectionIndices = [NSMutableDictionary dictionary];

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
            [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:10 recNameIndex:9]];
            //[self addNumber:line];
          }
          else if([lineType isEqualToString:@"symbolatom"]) {
            [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:10 recNameIndex:9]];
          }
          else if([lineType isEqualToString:@"text"]) {
            [result addObject:line]; //DEI we could strip out.
          } else if ([lineType isEqualToString:@"msg"]) {
            [result addObject:line]; //add unprocessed message box. key is a _string_ of the number
            [messageBoxIndexToIncomingConnectionIndices setObject:[NSMutableSet set]
                                                           forKey:[NSString stringWithFormat:@"%lu", index]];
          }
          else if([lineType isEqualToString:@"obj"] && line.count >= 5) {
            NSString *objType = [line objectAtIndex:4];

            // iem gui objects
            if([objType isEqualToString:@"bng"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:9 recNameIndex:10]];
            }
            else if([objType isEqualToString:@"tgl"]) {
              //[self addToggle:line];
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:7 recNameIndex:8]];
            }
            else if([objType isEqualToString:@"nbx"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:11 recNameIndex:12]];
              //[self addNumberbox2:line];
            }
            else if([objType isEqualToString:@"hsl"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:11 recNameIndex:12]];
              //[self addSlider:line withOrientation:WidgetOrientationHorizontal];
            }
            else if([objType isEqualToString:@"vsl"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:11 recNameIndex:12]];
              //[result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index++]];
              //[self addSlider:line withOrientation:WidgetOrientationVertical];
            }
            else if([objType isEqualToString:@"hradio"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:9 recNameIndex:10]];
              //[self addRadio:line withOrientation:WidgetOrientationHorizontal];
            }
            else if([objType isEqualToString:@"vradio"]) {
              [result addObject:[self shimAtomLineFromAtomLine:line guiIndex:index sendNameIndex:9 recNameIndex:10]];
              //[self addRadio:line withOrientation:WidgetOrientationVertical];
            }
            else if([objType isEqualToString:@"vu"]) {
              //[self addVUMeter:line];
              [result addObject:line];
            }
            else if([objType isEqualToString:@"cnv"]) {
              //[self addCanvas:line];
              [result addObject:line];
            } else { //regular obj
              // put all other lines at level 1back in DEI refactor!
              [result addObject:line];
            }
          } else if ([lineType isEqualToString:@"connect"]) { // #X connect 87 0 88 0;
            [result addObject:line];
            // assume that connections are made after iterating through all objects
            //if this connection connects _to_ a message, store it
            if (messageBoxIndexToIncomingConnectionIndices[line[4]]) { //if dict contains msg box index as key
              NSMutableSet *connectionSet = messageBoxIndexToIncomingConnectionIndices[line[4]];
              [connectionSet addObject:@[ line[2], line[3] ]]; //add tuple of obj index, outlet index
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
    // increment obj index
    if ([line[0] isEqualToString:@"#X"] && ![line[1] isEqualToString:@"connect"]) {
      index++;
    }
  }

  // Post-process the message boxes to connect to the message shims.
  //NSArray *msgBoxIndices = [messageBoxIndexToIncomingConnectionIndices allKeys];
  for (NSNumber *msgBoxIndexNum in [messageBoxIndexToIncomingConnectionIndices keyEnumerator]) {
    // create shim
    NSInteger msgBoxIndex = [msgBoxIndexNum integerValue];
    [result addObject:@[ @"#X", @"obj", @"0", @"0", @"MMPMessageShim",
                         [NSString stringWithFormat:@"%lu-gui-send", msgBoxIndex],
                         [NSString stringWithFormat:@"%lu-gui-rec", msgBoxIndex]
                         ]];
    // for objects going into message box, connect them to shim as well
    NSSet *connectionSet = messageBoxIndexToIncomingConnectionIndices[msgBoxIndexNum];
    for (NSArray *connectionTuple in connectionSet) {
    [result addObject:@[ @"#X", @"connect",
                        [NSString stringWithFormat:@"%ld", (long)[connectionTuple[0] integerValue]],
                        [NSString stringWithFormat:@"%ld", (long)[connectionTuple[1] integerValue]],
                        [NSString stringWithFormat:@"%ld", (long)index], @"0" ]];
    }

    // connect shim to message box
    [result addObject:@[ @"#X", @"connect", [NSString stringWithFormat:@"%ld", (long)index],
                                             @"0",
                                             [NSString stringWithFormat:@"%ld", (long)msgBoxIndex],
                                              @"0"] ];
    // inc
     index++;
  }

  return result;
}*/

+ (NSArray *)shimAtomLineFromAtomLine:(NSArray *)atomLine guiIndex:(NSUInteger)index sendNameIndex:(NSUInteger)sendNameIndex recNameIndex:(NSUInteger)recNameIndex{
  //DEI check length
  NSMutableArray *result = [[atomLine subarrayWithRange:NSMakeRange(0, 4)] mutableCopy]; //copy first 4
  [result setObject:@"obj" atIndexedSubscript:1]; //change floatatom,etc to obj
  // Bang gets special blocking shim, all others respect "set" and get default shim
  if ([atomLine[4] isEqualToString:@"bng"]) {
    [result addObject:@"MMPPdGuiFiles/MMPPdGuiNoSetShim"];
  } else {
    [result addObject: @"MMPPdGuiFiles/MMPPdGuiShim"];
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

/*+ (NSArray *)proccessGuiWidgetsFromAtomLines:(NSArray *)lines {
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
            [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:10 recNameIndex:9]];
          }
          else if([lineType isEqualToString:@"symbolatom"]) {
            [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:10 recNameIndex:9]];
          }
          else if([lineType isEqualToString:@"text"]) {
            [result addObject:line]; // no gui index increment
          }
          else if([lineType isEqualToString:@"obj"] && line.count >= 5) {
            NSString *objType = [line objectAtIndex:4];

            // iem gui objects
            if([objType isEqualToString:@"bng"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:9 recNameIndex:10]];
            }
            else if([objType isEqualToString:@"tgl"]) {
              //[self addToggle:line];
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:7 recNameIndex:8]];
            }
            else if([objType isEqualToString:@"nbx"]) {
              //[self addNumberbox2:line];
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:11 recNameIndex:12]];
            }
            else if([objType isEqualToString:@"hsl"]) {
              //[self addSlider:line withOrientation:WidgetOrientationHorizontal];
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:11 recNameIndex:12]];
            }
            else if([objType isEqualToString:@"vsl"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:11 recNameIndex:12]];

              //[self addSlider:line withOrientation:WidgetOrientationVertical];
            }
            else if([objType isEqualToString:@"hradio"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:9 recNameIndex:10]];

              //[self addRadio:line withOrientation:WidgetOrientationHorizontal];
            }
            else if([objType isEqualToString:@"vradio"]) {
              [result addObject:[self guiAtomLineFromAtomLine:line guiIndex:index sendNameIndex:9 recNameIndex:10]];

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
          } else if ([lineType isEqualToString:@"msg"]){
            //[result addObject:line]; //TODO msg shim! TODO strip off preceding slashes for $1
            [result addObject:[self guiMsgAtomLineFromAtomLine:line guiIndex:index]];
          }
        }
      }
    }
    // increment obj index
    if ([line[0] isEqualToString:@"#X"] && ![line[1] isEqualToString:@"connect"]) {
      index++;
    }
  }
  return result;
}*/

+ (NSArray *)guiAtomLineFromAtomLine:(NSArray *)atomLine guiIndex:(NSUInteger)index sendNameIndex:(NSUInteger)sendNameIndex recNameIndex:(NSUInteger)recNameIndex {
  //DEI check length
  NSMutableArray *result = [atomLine mutableCopy];
  result[sendNameIndex] = [NSString stringWithFormat:@"%lu-gui-send", (unsigned long)index];//send name
  result[recNameIndex] = [NSString stringWithFormat:@"%lu-gui-rec", (unsigned long)index];
  return result;
}

+ (NSArray *)guiMsgAtomLineFromAtomLine:(NSArray *)atomLine guiIndex:(NSUInteger)index {
  //DEI check length
  NSMutableArray *result = [atomLine mutableCopy];
  // TODO Sanitize any \$1
  // append send/rec names to line
  [result addObject: [NSString stringWithFormat:@"%lu-gui-send", (unsigned long)index]];//send name
  [result addObject: [NSString stringWithFormat:@"%lu-gui-rec", (unsigned long)index]];
  return result;
}

+ (void)maybeCreatePdGuiFolderAndFiles {
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];

  // Copy pd gui shim files if they are not there.
  NSString *patchDocFolderPath = [publicDocumentsDir stringByAppendingPathComponent:@"MMPPdGuiFiles"];
  if(![[NSFileManager defaultManager] fileExistsAtPath:patchDocFolderPath]) { //if doesn't exist, copy.
    [[NSFileManager defaultManager] createDirectoryAtPath:patchDocFolderPath withIntermediateDirectories:NO attributes:nil error:nil];
  }
  NSArray *pdGuiPatchFiles = @[ @"MMPPdGuiShim.pd", @"MMPPdGuiNoSetShim.pd", @"MMPPdGuiMessageShim.pd"];
  for(NSString* patchName in pdGuiPatchFiles){
    NSString* patchDocPath = [patchDocFolderPath stringByAppendingPathComponent:patchName];
    NSString* patchBundlePath = [[[NSBundle mainBundle] bundlePath] stringByAppendingPathComponent:patchName];
    NSError* error = nil;
    if(![[NSFileManager defaultManager] fileExistsAtPath:patchDocPath]) { //if doesn't exist, copy.
      [[NSFileManager defaultManager] copyItemAtPath:patchBundlePath toPath:patchDocPath error:&error];
    }
  }
}

@end
