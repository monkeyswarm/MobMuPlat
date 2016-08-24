//
//  MeKnob.m
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/22/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeKnob.h"
#import <QuartzCore/QuartzCore.h>

#define ROTATION_PAD_RAD .7
#define DRAG_RANGE 100
#define EXTRA_RADIUS 10 //how many pixels ticks are away from edge of knob 
#define TICK_DIM 10

@implementation MeKnob


- (id)initWithFrame:(CGRect)frame {
    
    self = [super initWithFrame:frame];
    if (self) {
		
        self.address=@"/unnamedKnob";
        dim = frame.size.width-((EXTRA_RADIUS+TICK_DIM)*2);
        
		//rounded up to nearest int - fo corner radius
		radius = (float)(int)(dim/2+.5);
		
        knobView = [[UIView alloc]initWithFrame:CGRectMake(EXTRA_RADIUS+TICK_DIM, EXTRA_RADIUS+TICK_DIM, dim, dim)];
        knobView.layer.cornerRadius=radius;
        knobView.userInteractionEnabled=NO;
        
        [self addSubview:knobView];
        
		indicatorDim=dim/2+2;
        int indicatorThickness = dim/8;
        
        centerPoint=CGPointMake(dim/2+EXTRA_RADIUS+TICK_DIM, dim/2+EXTRA_RADIUS+TICK_DIM);
		indicatorView=[[UIView alloc]initWithFrame:CGRectMake(dim/2-indicatorThickness/2,-2, indicatorThickness, indicatorDim)];
		indicatorView.layer.cornerRadius=3;
		indicatorView.userInteractionEnabled=NO;
		
        [self addSubview:indicatorView];
        [self setRange:1];
		[self updateIndicator];//rotates indicator to zero
        [self setColor:self.color];
    }
    return self;
}

-(void)setColor:(UIColor *)inColor{
    [super setColor:inColor];
    knobView.backgroundColor=inColor;
    if(tickViewArray)for (UIView* tick in tickViewArray)tick.backgroundColor=inColor;
}

- (void)setLegacyRange:(int)range {
  // old mode, default range is 2, which is range 0 to 1 float. translate that to new range of "1".
  if (range == 2) range = 1;
  [self setRange:range];
}

-(void)setRange:(int)inRange{
    _range=inRange;

  // clear ticks.
  for (UIView *tick in tickViewArray) {
    [tick removeFromSuperview];
  }

    NSUInteger effectiveRange = _range == 1 ? 2 : _range;
    tickViewArray = [[NSMutableArray alloc]initWithCapacity:effectiveRange];
    for(int i=0;i<effectiveRange;i++){
        UIView* dot = [[UIView alloc]init];
        float angle= (float)i/(effectiveRange-1)* (M_PI*2-ROTATION_PAD_RAD*2)+ROTATION_PAD_RAD+M_PI*.5;
        float xPos=(dim/2+EXTRA_RADIUS+TICK_DIM/2)*cos(angle);
        float yPos=(dim/2+EXTRA_RADIUS+TICK_DIM/2)*sin(angle);
        dot.backgroundColor=[self color];
        dot.layer.cornerRadius=TICK_DIM/2;
        [dot setFrame:CGRectMake(centerPoint.x+xPos-(TICK_DIM/2),centerPoint.y+yPos-(TICK_DIM/2), TICK_DIM, TICK_DIM)];
        dot.userInteractionEnabled=NO;
        [self addSubview:dot];
        [tickViewArray addObject:dot];
    }
}


-(void)setValue:(float)inVal{
    if(inVal!=_value){//only on change
        _value=inVal;
        [self updateIndicator];
    }
}

-(void)sendValue{
    if(_range>1)
        [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithInt:_value], nil]];
    else [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithFloat:_value], nil]];
}

-(void)setIndicatorColor:(UIColor *)inColor{
    _indicatorColor = inColor;
	indicatorView.backgroundColor=inColor;
}


-(void)updateIndicator{
    float newRad;
    if(_range==1)
         newRad= _value*(M_PI*2-ROTATION_PAD_RAD*2)+ROTATION_PAD_RAD+M_PI*.5;
    else if (_range>1)
         newRad= (_value/(_range-1))*(M_PI*2-ROTATION_PAD_RAD*2)+ROTATION_PAD_RAD+M_PI*.5;
    else return;//should not get here, range < 1
    CGPoint newCenter = CGPointMake(cos(newRad)*indicatorDim/2+centerPoint.x, sin(newRad)*indicatorDim/2+centerPoint.y);
	indicatorView.center=newCenter;
	indicatorView.transform=CGAffineTransformMakeRotation(newRad-M_PI/2);
}


-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
   [self touchesMoved:touches withEvent:event];
	knobView.backgroundColor=self.highlightColor;
	for (UIView* tick in tickViewArray)tick.backgroundColor=self.highlightColor;
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
	CGPoint point = [[touches anyObject] locationInView:self];
	float touchX = point.x-centerPoint.x;
	float touchY = point.y-centerPoint.y;
	//printf("\n%.2f %.2f %.2f %.2f", touchX, touchY, centerPoint.x, centerPoint.y);
	//now:angle of finger position!theta at 6 o'clock - ugly as hell, + and mod to put -pi to pi as 0 to 2pi
	double theta = atan2(touchY, touchX);
	double updatedTheta = fmod( theta+M_PI/2+M_PI , (M_PI*2) );
    
    //continuous
    if(_range==1){
        if(updatedTheta<ROTATION_PAD_RAD){[self setValue:0]; [self sendValue];}
        else if(updatedTheta>(M_PI*2-ROTATION_PAD_RAD)) {[self setValue:1]; [self sendValue];}
        else{ [self setValue:(updatedTheta-ROTATION_PAD_RAD)/(M_PI*2-2*ROTATION_PAD_RAD) ]; [self sendValue];}
    }
    //segmented, snap to tick
    else if (_range>1){
        if(updatedTheta<ROTATION_PAD_RAD){[self setValue:0]; [self sendValue];}
        else if(updatedTheta>(M_PI*2-ROTATION_PAD_RAD)){ [self setValue:_range-1]; [self sendValue];}
        else{
            [self setValue:(float) (  (int)((updatedTheta-ROTATION_PAD_RAD)/(M_PI*2-2*ROTATION_PAD_RAD)*(_range-1)+.5)  ) ];//round to nearest tick!
            [self sendValue];
        }
	}
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
	knobView.backgroundColor=self.color;
  for (UIView* tick in tickViewArray)tick.backgroundColor=self.color;
}

-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event{
	[self touchesEnded:touches withEvent:event];
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
    //take the value and set myself to it, sending out value if necessary
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSNumber class]]){
        [self setValue:[(NSNumber*)[inArray objectAtIndex:0] floatValue]];
        if(sendVal)[self sendValue];
    }
}

@end
