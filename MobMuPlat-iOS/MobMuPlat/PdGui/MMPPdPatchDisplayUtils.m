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
                                             @"vradio" : @[@(9),@(10)],
                                             //@"vu" : @[@(-1), @(7)] not yet supported
                                             }; //canvas ("cnv") doesn't need one

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
          // assume all obj boxes at level 1 are created by this point of gettign connections at level 1
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


+ (NSArray *)shimAtomLineFromAtomLine:(NSArray *)atomLine guiIndex:(NSUInteger)index sendNameIndex:(NSUInteger)sendNameIndex recNameIndex:(NSUInteger)recNameIndex{
  //DEI check length
  NSMutableArray *result = [[atomLine subarrayWithRange:NSMakeRange(0, 4)] mutableCopy]; //copy first 4
  [result setObject:@"obj" atIndexedSubscript:1]; //change floatatom,etc to obj

  // vu meter doesn't have a "send" - not yet supported
  /*if ([atomLine[4] isEqualToString:@"vu"]) {
    [result addObject:@"MMPPdGuiFiles/MMPPdGuiNoSetShim"];
    [result addObject: [NSString stringWithFormat:@"%lu-gui-send", (unsigned long)index]];//send name
    [result addObject: [NSString stringWithFormat:@"%lu-gui-rec", index]];
    [result addObject: [self shimSanitizeAtom:@"empty"]];
    [result addObject: [self shimSanitizeAtom:atomLine[recNameIndex]]];
    return result;
  }*/

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

+ (void)maybeCreatePdGuiFolderAndFiles:(BOOL)shouldForce {
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];

  // Copy pd gui shim files if they are not there.
  NSString *patchDocFolderPath = [publicDocumentsDir stringByAppendingPathComponent:@"MMPPdGuiFiles"];
  if(![[NSFileManager defaultManager] fileExistsAtPath:patchDocFolderPath]) { //if doesn't exist, create folder.
    [[NSFileManager defaultManager] createDirectoryAtPath:patchDocFolderPath withIntermediateDirectories:NO attributes:nil error:nil];
  }
  NSArray *pdGuiPatchFiles = @[ @"MMPPdGuiShim.pd", @"MMPPdGuiNoSetShim.pd", @"MMPPdGuiMessageShim.pd"];
  for(NSString* patchName in pdGuiPatchFiles){
    NSString* patchDocPath = [patchDocFolderPath stringByAppendingPathComponent:patchName];
    NSString* patchBundlePath = [[[NSBundle mainBundle] bundlePath] stringByAppendingPathComponent:patchName];
    NSError* error = nil;
    if (shouldForce) { //force overwrite
      if([[NSFileManager defaultManager] fileExistsAtPath:patchDocPath]) {
        [[NSFileManager defaultManager] removeItemAtPath:patchDocPath error:&error];
      }
      [[NSFileManager defaultManager] copyItemAtPath:patchBundlePath toPath:patchDocPath error:&error];
    } else {
      if(![[NSFileManager defaultManager] fileExistsAtPath:patchDocPath]) { //if doesn't exist, copy.
        [[NSFileManager defaultManager] copyItemAtPath:patchBundlePath toPath:patchDocPath error:&error];
      }
    }
  }
}

@end
