//
//  MeTable.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 4/24/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeTable : MeControl
@property (nonatomic, strong) UIColor *selectionColor;
@property (nonatomic) int mode;//0=select, 1=draw
-(void)loadTable;
@end
