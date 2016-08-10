//
//  MeSlider.h
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/22/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "MeControl.h"

@interface MeSlider : MeControl{
    UIView* thumbView;
    UIView* troughView;
        
    id targetObject;
    SEL targetSelector;
    
    BOOL isHorizontal;
    
    NSMutableArray* tickViewArray;

}

@property(nonatomic) int range;
@property(nonatomic) float value; //TODO hide?

-(void)setHorizontal;
- (void)setLegacyRange:(int)range;

@end
