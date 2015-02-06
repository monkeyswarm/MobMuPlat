//
//  MeMultiSlider.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 3/28/13.
//  Copyright (c) 2013 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeMultiSlider : MeControl

@property(nonatomic) int range;
@property(nonatomic) NSUInteger outputMode; //0=all values, 1=individual element index+value
@end
