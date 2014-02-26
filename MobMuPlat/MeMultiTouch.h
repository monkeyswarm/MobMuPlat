//
//  MeMultiTouch.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 2/23/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeMultiTouch : MeControl

@end


@interface Cursor : NSObject
@property (strong, nonatomic) UIView* cursorX;
@property (strong, nonatomic) UIView* cursorY;

//-(void)layoutAtPoint:(CGPoint)point;

@end

@interface MyTouch : NSObject
@property (nonatomic) CGPoint point;
@property (nonatomic) int state;//0,1,2
@property (nonatomic) int polyVox;
@property (weak, nonatomic) UITouch* origTouch;
@end