//
//  MeLabel.h
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/22/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

@interface MeLabel : MeControl{
    UITextView* theLabel;
}


@property (nonatomic) int textSize;
@property (nonatomic) NSString* stringValue;
@property(nonatomic, readonly) NSString *fontName;
@property(nonatomic, readonly) NSString *fontFamily;

-(void)setFontFamily:(NSString *)fontFamily fontName:(NSString*)fontName;
    
@end
