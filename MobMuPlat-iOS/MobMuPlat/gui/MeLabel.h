//
//  MeLabel.h
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/22/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeControl.h"

typedef NS_ENUM(NSUInteger, MMPHorizontalTextAlignment) {
  kMMPHorizontalTextAlignmentLeft = 0,
  kMMPHorizontalTextAlignmentCenter,
  kMMPHorizontalTextAlignmentRight
};

typedef NS_ENUM(NSUInteger, MMPVerticalTextAlignment) {
  kMMPVerticalTextAlignmentTop = 0,
  kMMPVerticalTextAlignmentCenter,
  kMMPVerticalTextAlignmentBottom
};

@interface MeLabel : MeControl{
    UILabel* theLabel;
}

@property (nonatomic) int textSize;
@property (nonatomic) NSString* stringValue;
@property(nonatomic, readonly) NSString *fontName;
@property(nonatomic, readonly) NSString *fontFamily;
@property(nonatomic) MMPHorizontalTextAlignment horizontalTextAlignment;
@property(nonatomic) MMPVerticalTextAlignment verticalTextAlignment;

-(void)setFontFamily:(NSString *)fontFamily fontName:(NSString*)fontName;
    
@end
