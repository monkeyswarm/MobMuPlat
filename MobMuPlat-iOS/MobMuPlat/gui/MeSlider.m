//
//  MeSlider.m
//  JSONToGUI
//
//  Created by Daniel Iglesia on 11/22/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeSlider.h"
#import <QuartzCore/QuartzCore.h>

#define SLIDER_TROUGH_WIDTH 10
#define SLIDER_TROUGH_TOPINSET 10
#define SLIDER_THUMB_HEIGHT 20

@implementation MeSlider


- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    
    self.address=@"/unnamedSlider";
    
    troughView=[[UIView alloc]initWithFrame: CGRectMake((frame.size.width-10)/2, SLIDER_TROUGH_TOPINSET, SLIDER_TROUGH_WIDTH, frame.size.height-(SLIDER_TROUGH_TOPINSET*2))];
    troughView.backgroundColor=[UIColor whiteColor];
    troughView.layer.cornerRadius=3;
    troughView.userInteractionEnabled=NO;
    [self addSubview:troughView];
    
    thumbView=[[UIView alloc]initWithFrame:CGRectMake(0,frame.size.height-SLIDER_THUMB_HEIGHT, frame.size.width, SLIDER_THUMB_HEIGHT)];
    thumbView.backgroundColor=[UIColor whiteColor];
    thumbView.layer.cornerRadius=5;
    thumbView.userInteractionEnabled=NO;
    [self addSubview:thumbView];
    [self setRange:1];
    //[self setColor:[UIColor whiteColor]];

    return self;
}

-(void)setColor:(UIColor *)inColor{
    [super setColor:inColor];
    troughView.backgroundColor=inColor;
    thumbView.backgroundColor=inColor;
    if(tickViewArray)for (UIView* tick in tickViewArray)tick.backgroundColor=inColor;
}

-(void)setHorizontal{//must do this before setrange
    isHorizontal=YES;
    //update layout
    //trough
    CGRect newFrame= CGRectMake(SLIDER_TROUGH_TOPINSET, (self.frame.size.height-10)/2, self.frame.size.width-(SLIDER_TROUGH_TOPINSET*2), SLIDER_TROUGH_WIDTH);
    troughView.frame = newFrame;
    //thumbview
    newFrame = CGRectMake(0, 0, SLIDER_THUMB_HEIGHT, self.frame.size.height);
    thumbView.frame = newFrame;
}

- (void)setLegacyRange:(int)range {
  // old mode, default range is 2, which is range 0 to 1 float. translate that to new range of "1".
  if (range == 2) range = 1;
  [self setRange:range];
}

-(void)setRange:(int)inRange{
     _range=inRange;
    if(_range>1){
        tickViewArray = [[NSMutableArray alloc]initWithCapacity:_range];
        
        for(int i=0;i<_range;i++){
            UIView* tick;
            if(!isHorizontal)
                tick = [[UIView alloc]initWithFrame:CGRectMake((self.frame.size.width-10)/4, SLIDER_TROUGH_TOPINSET+i*(self.frame.size.height-(SLIDER_TROUGH_TOPINSET*2))/(_range-1)-1, (self.frame.size.width-10)/2+10, 2)];
            else
                tick = [[UIView alloc]initWithFrame:CGRectMake( SLIDER_TROUGH_TOPINSET+i*(self.frame.size.width-(SLIDER_TROUGH_TOPINSET*2))/(_range-1)-1, (self.frame.size.height-10)/4, 2, (self.frame.size.height-10)/2+10)];
           
            tick.backgroundColor=self.color;
            tick.layer.cornerRadius=1;
            tick.userInteractionEnabled=NO;
            [tickViewArray addObject:tick];
            [self addSubview:tick];
            [self sendSubviewToBack:tick];
        }
    }
}


-(void)setValue:(float)inVal{
    _value=inVal;
	[self updateThumb];
}

-(void)sendValue{
    if(_range>1)
        [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithInt:_value], nil]];
    else [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithFloat:_value], nil]];
}

-(void)updateThumb{
    CGRect newFrame;
  NSUInteger effectiveRange = _range == 1 ? 1 : _range -1 ; // range == 1 handled differently

    if(!isHorizontal)
        newFrame = CGRectMake( 0, (1.0-(_value/effectiveRange))*(self.frame.size.height-(SLIDER_TROUGH_TOPINSET*2)), self.frame.size.width, SLIDER_THUMB_HEIGHT );
    else  newFrame = CGRectMake( (_value/effectiveRange)*(self.frame.size.width-(SLIDER_TROUGH_TOPINSET*2)),0, SLIDER_THUMB_HEIGHT, self.frame.size.height  );
    
	thumbView.frame=newFrame;
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
	
    CGPoint point = [[touches anyObject] locationInView:self];
	float tempFloatValue;
    if(!isHorizontal) tempFloatValue=1.0-(float)((point.y-SLIDER_TROUGH_TOPINSET)/(self.frame.size.height-(SLIDER_TROUGH_TOPINSET*2)));//0-1
    else tempFloatValue=(float)((point.x-SLIDER_TROUGH_TOPINSET)/(self.frame.size.width-(SLIDER_TROUGH_TOPINSET*2)));//0-1
    
    if(_range==1 && tempFloatValue<=1 && tempFloatValue>=0  && tempFloatValue!=_value){
      [self setValue: tempFloatValue];
      [self sendValue];
      return;
    }
	float tempValue = (float)(int)((tempFloatValue*(_range-1))+.5);//round to 0-4
	if(_range>1 && tempValue<=_range-1 && tempValue>=0  && tempValue!=_value){[self setValue: tempValue ]; [self sendValue];}
    
    thumbView.backgroundColor=self.highlightColor;
	troughView.backgroundColor=self.highlightColor;
	 if(tickViewArray)for (UIView* tick in tickViewArray)tick.backgroundColor=self.highlightColor;
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
	CGPoint point = [[touches anyObject] locationInView:self];
	float tempFloatValue;
    if(!isHorizontal)tempFloatValue=1.0-(float)((point.y-SLIDER_TROUGH_TOPINSET)/(self.frame.size.height-(SLIDER_TROUGH_TOPINSET*2)));
    else tempFloatValue=(float)((point.x-SLIDER_TROUGH_TOPINSET)/(self.frame.size.width-(SLIDER_TROUGH_TOPINSET*2)));
        
	if(_range==1 && tempFloatValue<=1 && tempFloatValue>=0 && tempFloatValue!=_value){
    [self setValue: tempFloatValue ];
    [self sendValue];
    return;}

  float tempValue = (float)(int)((tempFloatValue*(_range-1))+.5);
	if(_range>1 && tempValue<=_range-1 && tempValue>=0 && tempValue!=_value){
    [self setValue: tempValue ];
    [self sendValue];
  }
	
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
	thumbView.backgroundColor=self.color;
	troughView.backgroundColor= self.color;
    if(tickViewArray)for (UIView* tick in tickViewArray)tick.backgroundColor=self.color;
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
        //printf("\nset!");
        sendVal=NO;
    }
    
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSNumber class]]){
        [self setValue:[(NSNumber*)[inArray objectAtIndex:0] floatValue]];
        if(sendVal)[self sendValue];
    }
}

@end
