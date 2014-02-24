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

-(void)layoutAtPoint:(CGPoint)point;

@end