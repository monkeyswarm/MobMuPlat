//
//  MMPPdPatchDisplayUtils.h
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface MMPPdPatchDisplayUtils : NSObject

/// Take pd file lines and replace gui elements with "shim" custom objects for gui-to-patch communication.
+ (NSArray *)proccessNativeWidgetsFromAtomLines:(NSArray *)lines;

/// Take pd file lines and extract gui elements, swap in custom to/from names.
+ (NSArray *)proccessGuiWidgetsFromAtomLines:(NSArray *)lines;

@end
