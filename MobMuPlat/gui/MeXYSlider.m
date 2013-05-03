//
//  MeXYSlider.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/24/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeXYSlider.h"
#import <QuartzCore/QuartzCore.h>
#define LINE_WIDTH 3

@implementation MeXYSlider
@synthesize valueX, valueY;

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.address=@"/unnamedXYSlider";
        cursorHoriz = [[UIView alloc]initWithFrame:CGRectMake(0, 0, frame.size.width, LINE_WIDTH)];
        cursorVert = [[UIView alloc]initWithFrame:CGRectMake(0, 0, LINE_WIDTH, frame.size.height)];
        cursorHoriz.userInteractionEnabled=NO;
        cursorVert.userInteractionEnabled=NO;
        [self addSubview:cursorHoriz];
        [self addSubview:cursorVert];
		self.layer.borderWidth=LINE_WIDTH;
        
    }
    return self;
}

-(void)setColor:(UIColor *)inColor{
    [super setColor:inColor];
    [self updateColor:inColor];
}

-(void)updateColor:(UIColor*)inColor{
    cursorHoriz.backgroundColor=inColor;
    cursorVert.backgroundColor=inColor;
    self.layer.borderColor=[inColor CGColor];
}


-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
	[self touchesMoved:touches withEvent:event];
	[self updateColor:self.highlightColor];
	
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
	CGPoint point = [[touches anyObject] locationInView:self];
    float valX = point.x/self.frame.size.width;
	float valY = 1.0-(point.y/self.frame.size.height);
    if(valX>1)valX=1; if(valX<0)valX=0;
    if(valY>1)valY=1; if(valY<0)valY=0;
	[self setValueX:valX Y:valY];
    [self sendValue];
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
    [self updateColor: self.color];
}
-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event{
	[self touchesEnded:touches withEvent:event];
}	

-(void)setValueX:(float)inValX Y:(float)inValY{
    if(inValX!=valueX || inValY!=valueY){//only on change
        valueX=inValX; valueY=inValY;
        
        CGPoint HorizCenter=CGPointMake(self.frame.size.width/2, (1.0-valueY)*self.frame.size.height);
        CGPoint VertCenter=CGPointMake(valueX*self.frame.size.width, self.frame.size.height/2);
		cursorHoriz.center=HorizCenter;
		cursorVert.center=VertCenter;
    }
}

-(void)sendValue{
    [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithFloat:valueX], [NSNumber numberWithFloat:valueY], nil]];
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
    BOOL sendVal=YES;
    //if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"set"]){
        NSRange newRange = (NSRange){1, [inArray count]-1};
        inArray = [inArray subarrayWithRange: newRange];
        sendVal=NO;
    }
    
    if([inArray count]==2 && [[inArray objectAtIndex:0] isKindOfClass:[NSNumber class]] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self setValueX:[(NSNumber*)[inArray objectAtIndex:0] floatValue]  Y:[(NSNumber*)[inArray objectAtIndex:1] floatValue]];
        if(sendVal)[self sendValue];
    }
}

@end
