//
//  MeToggle.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/24/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeToggle.h"
#import <QuartzCore/QuartzCore.h>

@implementation MeToggle

#define EDGE_RADIUS 5

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        self.address=@"/unnamedToggle";
        theButton = [UIButton buttonWithType:UIButtonTypeCustom ];
        theButton.frame=CGRectMake(0, 0, self.frame.size.width, self.frame.size.height);
        theButton.backgroundColor=[UIColor clearColor];
        [theButton addTarget:self action:@selector(buttonHitDown) forControlEvents:UIControlEventTouchDown];
        //[theButton addTarget:self action:@selector(buttonHitUp) forControlEvents:UIControlEventTouchUpInside];//nothing happens on toggle touch up
        theButton.layer.cornerRadius=EDGE_RADIUS;
        
        [self setBorderThickness:4];//default
        theButton.layer.borderColor=[[UIColor blueColor]CGColor];//default

        [self addSubview: theButton];
    }
    return self;
}

-(void)setBorderThickness:(int)borderThickness{
    _borderThickness = borderThickness;
    theButton.layer.borderWidth = _borderThickness;
}

-(void)setValue:(int)inValue{
    _value=inValue;
    if(_value==1)theButton.backgroundColor=self.highlightColor;
    else if(_value==0)theButton.backgroundColor=[UIColor clearColor];
}

-(void)sendValue{
     [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithInt:self.value], nil]];
}

//nothing happend on toggle touch up
/*-(void)buttonHitUp{
}*/

-(void)buttonHitDown{
    [self setValue:1-self.value];
    [self sendValue];
}

-(void)setColor:(UIColor *)inColor{
    [super setColor:inColor];
    theButton.layer.borderColor=[inColor CGColor];
    theButton.backgroundColor=[UIColor clearColor];
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
  [super receiveList:inArray];
    BOOL sendVal=YES;
    //if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"set"]){
        NSRange newRange = (NSRange){1, [inArray count]-1};
        inArray = [inArray subarrayWithRange: newRange];
        sendVal=NO;
    }

    //look at message and set my value, outputting value if required
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSNumber class]]){
        [self setValue:(int)[(NSNumber*)[inArray objectAtIndex:0] floatValue]];
        if(sendVal)[self sendValue];
    }
}

@end
