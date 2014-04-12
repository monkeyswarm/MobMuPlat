//
//  MePanel.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/4/13.
//  Copyright (c) 2013 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MePanel : MeControl{
    UIImageView* imageView;
    UILabel* theLabel;
}

@property (nonatomic, strong)NSString* imagePath;
@property (nonatomic) BOOL shouldPassTouches;
@end
