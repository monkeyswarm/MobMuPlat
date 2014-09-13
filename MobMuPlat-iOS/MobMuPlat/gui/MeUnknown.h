//
//  MeUnknown.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/11/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeUnknown : MeControl {
    UILabel* warningLabel;
}

-(void)setWarning:(NSString*)badName;

@end
