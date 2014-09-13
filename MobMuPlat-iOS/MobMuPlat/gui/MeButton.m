//
//  MeButton.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/23/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeButton.h"

#import <QuartzCore/QuartzCore.h>
#define EDGE_RADIUS 5;

@implementation MeButton

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.address=@"/unnamedButton";
        _value=0;
        theButton = [UIButton buttonWithType:UIButtonTypeCustom ];
        theButton.frame=CGRectMake(0, 0, self.frame.size.width, self.frame.size.height);
        theButton.backgroundColor=[UIColor whiteColor];//default color
        [theButton addTarget:self action:@selector(buttonHitDown) forControlEvents:UIControlEventTouchDown];
        [theButton addTarget:self action:@selector(buttonHitUp) forControlEvents:UIControlEventTouchUpInside|UIControlEventTouchCancel|UIControlEventTouchUpOutside];
        theButton.layer.cornerRadius=EDGE_RADIUS;
        
        [self addSubview: theButton];
        
    }
    return self;
}

-(void)setColor:(UIColor *)inColor{
     [super setColor:inColor];
    theButton.backgroundColor=inColor;
}


-(void)setValue:(int)inValue{
    _value=inValue;
    if(_value==0)theButton.backgroundColor=self.color;
    else if(_value==1)theButton.backgroundColor=self.highlightColor;
    [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithInt:_value], nil]];    
}

-(void)buttonHitDown{
    [self setValue:1];
}

-(void)buttonHitUp{
    [self setValue:0];
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
//for button, any message means a instantaneous touch down and touch up
//it does not respond to "set" anything
-(void)receiveList:(NSArray *)inArray{
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSNumber class]]){
        [self setValue:1];
        [self setValue:0];
    }
}

@end
