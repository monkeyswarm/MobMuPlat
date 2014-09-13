//
//  MeMenu.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 4/8/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeMenu : MeControl < UITableViewDataSource, UITableViewDelegate>
@property (strong, nonatomic) NSString* titleString;
@end
