//
//  MMPPdObjectBox.h
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "AtomWidget.h"

@class Gui;

@interface MMPPdObjectBox : AtomWidget

+ (id)objectBoxFromAtomLine:(NSArray *)line withGui:(Gui *)gui;

@end

