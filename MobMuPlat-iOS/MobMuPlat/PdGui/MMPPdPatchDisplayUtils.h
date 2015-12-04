//
//  MMPPdPatchDisplayUtils.h
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface MMPPdPatchDisplayUtils : NSObject

/// Return two-item tuple
/// [0] = processed lines to be written as the pd patch to execute. This
/// [1] = proccessed lines to generate to the MMP GUI
+ (NSArray *)proccessAtomLines:(NSArray *)lines;


/// Call before opening a pd gui. Ensure that the Documents folder with the shim files exists.
+ (void)maybeCreatePdGuiFolderAndFiles;

@end
