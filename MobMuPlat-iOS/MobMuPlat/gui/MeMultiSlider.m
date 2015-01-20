//
//  MeMultiSlider.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 3/28/13.
//  Copyright (c) 2013 Daniel Iglesia. All rights reserved.
//

#import "MeMultiSlider.h"
#import <QuartzCore/QuartzCore.h>
#define SLIDER_HEIGHT 20

@implementation MeMultiSlider

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.address=@"/unnamedMultiSlider";
        currHeadIndex=-1;
        //defaults
        [self setColor:[UIColor whiteColor]];
        [self setHighlightColor:[UIColor blueColor]];
        
        box = [[UIView alloc] initWithFrame:CGRectMake(0, SLIDER_HEIGHT/2, self.frame.size.width, self.frame.size.height-SLIDER_HEIGHT)];
        box.layer.borderWidth=2;
        box.layer.borderColor=[self.color CGColor];
        box.userInteractionEnabled=NO;
        [self addSubview:box];
        
        //default
        [self setRange:8];
    }
    return self;
}


-(void)setRange:(int)inRange{
    _range=inRange;
    if(_range>0){
        if(headViewArray)for(UIView* head in headViewArray)[head removeFromSuperview];
        headViewArray = [[NSMutableArray alloc]init];
        _valueArray = [[NSMutableArray alloc] init];
        headWidth=self.frame.size.width/_range;
        for(int i=0;i<_range;i++){
            [_valueArray addObject:[NSNumber numberWithFloat:0]];
            
            UIView* headView;
            headView = [[UIView alloc]initWithFrame:CGRectMake( i*headWidth, self.frame.size.height-SLIDER_HEIGHT, headWidth, 20)];
            headView.backgroundColor=self.color;
            headView.layer.cornerRadius=4;
            headView.userInteractionEnabled=NO;
            [headViewArray addObject:headView];
            [self addSubview:headView];
        }
    }
}

-(void)setColor:(UIColor *)inColor{
    [super setColor:inColor];
    box.layer.borderColor=[inColor CGColor];
    if(headViewArray)for (UIView* head in headViewArray)head.backgroundColor=inColor;
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{ 
    CGPoint point = [[touches anyObject] locationInView:self];
	int headIndex = (int)(point.x/headWidth);
    headIndex = MAX(MIN(headIndex, _range-1), 0);//clip to range
    
    float clippedPointY = MAX(MIN(point.y, self.frame.size.height-SLIDER_HEIGHT/2), SLIDER_HEIGHT/2);
    float headVal = 1.0-( (clippedPointY-SLIDER_HEIGHT/2) / (self.frame.size.height - SLIDER_HEIGHT) );
    [_valueArray setObject:[NSNumber numberWithFloat:headVal] atIndexedSubscript:headIndex];
    // send out
    if (_touchMode == 1) {
      [self sendSliderIndex:headIndex value:headVal];
    } else  {
      [self sendValue];
    }

    UIView* currHead = [headViewArray objectAtIndex:headIndex];
    CGRect newFrame = CGRectMake(headIndex*headWidth, clippedPointY-SLIDER_HEIGHT/2, headWidth, SLIDER_HEIGHT);
    currHead.frame=newFrame;
    currHead.backgroundColor=self.highlightColor;
    currHeadIndex=headIndex;

}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
	CGPoint point = [[touches anyObject] locationInView:self];
	int headIndex = (int)(point.x/headWidth);
     headIndex = MAX(MIN(headIndex, _range-1), 0);
    
    float clippedPointY = MAX(MIN(point.y, self.frame.size.height-SLIDER_HEIGHT/2), SLIDER_HEIGHT/2);
    float headVal = 1.0-( (clippedPointY-SLIDER_HEIGHT/2) / (self.frame.size.height - SLIDER_HEIGHT) );
    [_valueArray setObject:[NSNumber numberWithFloat:headVal] atIndexedSubscript:headIndex];
  
  UIView* currHead = [headViewArray objectAtIndex:headIndex];
  CGRect newFrame = CGRectMake(headIndex*headWidth, clippedPointY-SLIDER_HEIGHT/2, headWidth, SLIDER_HEIGHT);
  currHead.frame=newFrame;
  
    //also set elements between prev touch and move, to avoid "skipping" on fast drag
  if(abs(headIndex-currHeadIndex)>1){
    int minTouchIndex = MIN(headIndex, currHeadIndex);
    int maxTouchIndex = MAX(headIndex, currHeadIndex);
    
    float minTouchedValue = [[_valueArray objectAtIndex:minTouchIndex] floatValue];
    float maxTouchedValue = [[_valueArray objectAtIndex:maxTouchIndex] floatValue];
    //NSLog(@"skip within %d (%.2f) to %d(%.2f)", minTouchIndex, [[_valueArray objectAtIndex:minTouchIndex] floatValue], maxTouchIndex, [[_valueArray objectAtIndex:maxTouchIndex] floatValue]);
    for(int i=minTouchIndex+1;i<maxTouchIndex;i++){
      float percent = ((float)(i-minTouchIndex))/(maxTouchIndex-minTouchIndex);
      float interpVal = (maxTouchedValue - minTouchedValue) * percent  + minTouchedValue ;
      //NSLog(@"%d %.2f %.2f", i, percent, interpVal);
      [_valueArray setObject:[NSNumber numberWithFloat:interpVal] atIndexedSubscript:i];
      if(_touchMode==1) {
        [self sendSliderIndex:i value:interpVal];
      }
    }
    //[self updateThumbs];//TODO optimize - this does everything
    [self updateThumbsFrom:minTouchIndex+1 to:maxTouchIndex-1];
  }
  
  // send out
  if (_touchMode == 1) {
    [self sendSliderIndex:headIndex value:headVal];
  } else  {
    [self sendValue];
  }

    if(headIndex!=currHeadIndex){//dragged to new head
        UIView* prevHead = [headViewArray objectAtIndex:currHeadIndex];
        prevHead.backgroundColor=self.color;//change prev head back
        currHead.backgroundColor=self.highlightColor;
        currHeadIndex=headIndex;
    }

}

- (void)sendSliderIndex:(int)index value:(float)value {
  NSArray* msgArray = [NSArray arrayWithObjects:self.address, @(index), @(value), nil];
  [self.controlDelegate sendGUIMessageArray:msgArray];
}

-(void)sendValue{
    NSArray* msgArray = [NSArray arrayWithObject:self.address];
    msgArray = [msgArray arrayByAddingObjectsFromArray:_valueArray];
    [self.controlDelegate sendGUIMessageArray:msgArray];
}

/*-(void)showVals{
    printf("\n");
    for(NSNumber *num in _valueArray) printf("%.2f ", [num floatValue]);
}*/

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
    for(UIView* head in headViewArray) head.backgroundColor=self.color;
}

-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event{
	[self touchesEnded:touches withEvent:event];
}

-(void)updateThumbsFrom:(int)start to:(int)end{
  for(int i=start;i<=end;i++){
    NSNumber* val = [_valueArray objectAtIndex:i];
    UIView* currHead = [headViewArray objectAtIndex:i];
    CGRect newFrame = CGRectMake(i*headWidth, (1.0-[val floatValue])*(self.frame.size.height-SLIDER_HEIGHT), headWidth, SLIDER_HEIGHT);
    currHead.frame=newFrame;
  }
}

-(void)updateThumbs{
  [self updateThumbsFrom:0 to:[_valueArray count]-1];
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
    BOOL sendVal=YES;
    //if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"set"]){
        NSRange newRange = (NSRange){1, [inArray count]-1};
        inArray = [inArray subarrayWithRange: newRange];
        //printf("\nset!");
        sendVal=NO;
    }
  
    if ([inArray count]>1 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"allVal"] ){
      
      float val = [[inArray objectAtIndex:1] floatValue];
      val = MAX(MIN(val, 1), 0);//clip
      for(int i=0;i<[_valueArray count];i++){
        [_valueArray setObject:[NSNumber numberWithFloat:val] atIndexedSubscript:i];
      }
      [self updateThumbs];
      if(sendVal)[self sendValue];
    }
  
    else if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSNumber class]] ){ //list to set values
        NSMutableArray* newValArray = [[NSMutableArray alloc]init];
        
        for(NSNumber* val in inArray)[newValArray addObject:val];
        
        if([newValArray count] != _range) [self setRange:[newValArray count]];
        [self setValueArray:newValArray];
        // TODO clip before set?
        for(int i=0;i<[_valueArray count];i++){
            NSNumber* val = [_valueArray objectAtIndex:i];
            if([val floatValue]<0 || [val floatValue]>1 ){
                float newFloat = MAX(MIN([val floatValue], 1), 0);//clip
                [_valueArray setObject:[NSNumber numberWithFloat:newFloat] atIndexedSubscript:i];
            }
        }
        [self updateThumbs];
        if(sendVal)[self sendValue];
    }
}


@end
