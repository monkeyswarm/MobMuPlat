//
//  MobMuPlatUtil.m
//  MobMuPlat
//
//  Created by diglesia on 1/20/15.
//  Copyright (c) 2015 Daniel Iglesia. All rights reserved.
//

#import "MobMuPlatUtil.h"

@implementation MobMuPlatUtil

+ (BOOL)numberIsFloat:(NSNumber*)num {
  if(strcmp([num objCType], @encode(float)) == 0 || strcmp([num objCType], @encode(double)) == 0) {
    return YES;
  }
  else return NO;
}

@end
