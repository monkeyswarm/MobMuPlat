//
//  MeXYSlider.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/24/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeXYSlider : MeControl{
    UIView* cursorHoriz, *cursorVert;
}

@property (nonatomic) float valueX;
@property (nonatomic) float valueY;

@end
