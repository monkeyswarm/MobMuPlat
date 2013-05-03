//
//  MeGrid.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/26/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeGrid : MeControl{
    NSMutableArray* gridButtons;
    int dimX; int dimY;
}

-(void)setDimX:(int)inX Y:(int)inY;

@property(nonatomic)int borderThickness;
@property(nonatomic)int cellPadding;//spacing between cells


@end
