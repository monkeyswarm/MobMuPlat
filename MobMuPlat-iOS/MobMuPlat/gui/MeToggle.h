//
//  MeToggle.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/24/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"


@interface MeToggle : MeControl{
    UIButton* theButton;
}
@property (nonatomic) int value;
@property (nonatomic) int borderThickness;
@end
