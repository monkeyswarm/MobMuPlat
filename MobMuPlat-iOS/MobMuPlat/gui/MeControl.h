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
-(UIColor*)patchBackgroundColor;
-(UIInterfaceOrientation)orientation;
@end

@interface MeControl : UIControl{
    
}

+(UIColor*) colorFromRGBArray:(NSArray*)rgbArray;//take three floats and turn into color
+(UIColor*)inverseColorFromRGBArray:(NSArray*)rgbArray;
+(UIColor*) colorFromRGBAArray:(NSArray*)rgbaArray;//take 4 floats and turn into color with translucency

-(void)receiveList:(NSArray*)inArray;



@property (nonatomic) UIViewController<ControlDelegate> *controlDelegate;
@property (nonatomic) NSString* address;
@property (nonatomic) UIColor* color;
@property (nonatomic) UIColor* highlightColor;

@end
