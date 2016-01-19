//
//  MMPPdMessageBox.h
//  MobMuPlat
//
//  Created by diglesia on 11/30/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "AtomWidget.h"

@class Gui;

@interface MMPPdMessageBox : AtomWidget

+ (id)messageBoxFromAtomLine:(NSArray *)line withGui:(Gui *)gui;

@end
