//
//  DanControl.h
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/21/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "PdBase.h"//??

@protocol ControlDelegate <NSObject>
-(void)sendGUIMessageArray:(NSArray*)msgArray;
@end

@interface MeControl : UIControl{
    
}

+(UIColor*) colorFromRGBArray:(NSArray*)rgbArray;//take three floats and turn into color
+(UIColor*) colorFromRGBAArray:(NSArray*)rgbaArray;//take 4 floats and turn into color with translucency

//this is here, but blank, so can be optionally overridden by subclass
-(void)receiveList:(NSArray*)inArray;



@property (nonatomic) id<ControlDelegate> controlDelegate;
@property (nonatomic) NSString* address;
@property (nonatomic) UIColor* color;
@property (nonatomic) UIColor* highlightColor;
@end
