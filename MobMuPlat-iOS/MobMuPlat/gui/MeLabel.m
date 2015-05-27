//
//  MeLabel.m
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/22/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeLabel.h"
#import "MobMuPlatUtil.h"

#define DEFAULT_FONT @"HelveticaNeue"
#define WIDTH_PAD 10 //ugly hack - iOS and OSX textview have slighty different padding, so offset the ios labels to more closely match
#define INSET_X 4
#define INSET_Y -2

@implementation MeLabel



- (id)initWithFrame:(CGRect)frame{
    
    self = [super initWithFrame:frame];
    if (self) {
        self.address=@"/unnamedLabel";
        self.backgroundColor=[UIColor clearColor];
        
        theLabel = [[UITextView alloc]initWithFrame:CGRectMake(INSET_X, INSET_Y, self.frame.size.width+WIDTH_PAD, self.frame.size.height)];
        theLabel.backgroundColor=[UIColor clearColor];
    
        
        [theLabel setTextColor:self.color];
        [theLabel setFont:[UIFont fontWithName:DEFAULT_FONT size:16]];
        theLabel.contentInset = UIEdgeInsetsMake(-8,-4,0,0);
       
        //default
        _textSize=12;
 
        theLabel.userInteractionEnabled = NO;
        self.userInteractionEnabled=NO;
        [self addSubview: theLabel];
       
    }
    return self;
}

-(void)setStringValue:(NSString *)stringValue{
    _stringValue=stringValue;
    [theLabel setText:stringValue];
}

-(void)setTextSize:(int)inTextSize{
    _textSize=inTextSize;
    if([_fontFamily isEqualToString:@"Default"])[theLabel setFont:[UIFont fontWithName:DEFAULT_FONT size:_textSize]];
    else [theLabel setFont:[UIFont fontWithName:_fontName size:_textSize]];
}

-(void)setFontFamily:(NSString*)fontFamily fontName:(NSString *)fontName{
    _fontName=fontName;
    _fontFamily = fontFamily;
    if([_fontFamily isEqualToString:@"Default"])[theLabel setFont:[UIFont fontWithName:DEFAULT_FONT size:_textSize]];
    if(![UIFont fontWithName:fontName size:12])[theLabel setFont:[UIFont fontWithName:DEFAULT_FONT size:_textSize]];//no font of that name
    else [theLabel setFont:[UIFont fontWithName:fontName size:_textSize]];
}

-(void)setColor:(UIColor*)inColor{
  [super setColor:inColor];
    [theLabel setTextColor:inColor];
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
  [super receiveList:inArray];
  // ignore enable message
  if ([inArray count] >= 2 &&
      [inArray[0] isKindOfClass:[NSString class]] &&
      [inArray[0] isEqualToString:@"enable"] &&
      [inArray[1] isKindOfClass:[NSNumber class]]) {
    return;
  }
    //"highlight 0/1"
    if(([inArray count]==2) && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"highlight"]){
        if([[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
            if ([[inArray objectAtIndex:1] intValue]>0)[theLabel setTextColor:self.highlightColor];
            else [theLabel setTextColor:self.color];
        }
    }
    //otherwise assume it is a list of words/number to be formatted into a string. PureData only sends floats, no ints
    else{
        NSMutableString* newString = [[NSMutableString alloc]init];
        for(id thing in inArray){
            if([thing isKindOfClass:[NSString class]]){
                [newString appendString:(NSString*)thing];
            }
            else if ([thing isKindOfClass:[NSNumber class]]){
              NSNumber* thingNumber = (NSNumber*)thing;
              if ([MobMuPlatUtil numberIsFloat:thingNumber] ){ //todo put in separate class
                //pd sends floats :(
                if(fmod([thingNumber floatValue],1)==0)[newString appendString:[NSString stringWithFormat:@"%d", (int)[thingNumber floatValue]]];//print whole numbers as ints
                else [newString appendString:[NSString stringWithFormat:@"%.3f", [thingNumber floatValue]]];
              }
              else {
                [newString appendString:[NSString stringWithFormat:@"%d", [thingNumber intValue]]];
              }
            }
            [newString appendString:@" "];
        }
        theLabel.text=newString;
    }
}

@end
