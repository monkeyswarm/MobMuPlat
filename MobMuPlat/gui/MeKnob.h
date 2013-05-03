//
//  MeKnob.h
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/22/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeKnob : MeControl{
    UIView* knobView;
    UIView* indicatorView;
	id targetObject;
	SEL targetSelector;
	float dim; //diameter
	float radius;
	float indicatorDim;//radius
	
	CGPoint centerPoint;
    NSMutableArray *tickViewArray;
}

@property (nonatomic)float value;
@property (nonatomic)int range;
@property (retain,nonatomic) UIColor* indicatorColor;




@end
