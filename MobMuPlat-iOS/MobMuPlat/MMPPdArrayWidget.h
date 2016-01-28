//
//  MMPPdArrayWidget.h
//  MobMuPlat
//
//  Created by diglesia on 1/26/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "Widget.h"

@interface MMPPdArrayWidget : Widget

- (id)initWithAtomLine:(NSArray *)atomLine
            valuesLine:(NSArray *)arrayValueLine
            coordsLine:(NSArray *)coordsLine
           restoreLine:(NSArray *)restoreLine
                andGui:(Gui *)gui;

@end
