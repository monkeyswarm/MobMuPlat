//
//  DanControl.m
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/21/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//
//  Superclass for all GUI widgets. Should not be instantiatied itself.

#import "MeControl.h"


@implementation MeControl
@synthesize address, color, controlDelegate;


+(UIColor*)colorFromRGBArray:(NSArray*)rgbArray{
    return [UIColor colorWithRed:[[rgbArray objectAtIndex:0] floatValue] green:[[rgbArray objectAtIndex:1] floatValue] blue:[[rgbArray objectAtIndex:2] floatValue] alpha:1];
}

+(UIColor*)colorFromRGBAArray:(NSArray*)rgbaArray{
    return [UIColor colorWithRed:[[rgbaArray objectAtIndex:0] floatValue] green:[[rgbaArray objectAtIndex:1] floatValue] blue:[[rgbaArray objectAtIndex:2] floatValue] alpha:[[rgbaArray objectAtIndex:3] floatValue]];
}

//empty implementation so that "receiveList" can be called on MeControl, even though it is overridden by all subclasses
-(void)receiveList:(NSArray*)inArray{}
//TODO do the cmd:selectornot implemented thing

@end
