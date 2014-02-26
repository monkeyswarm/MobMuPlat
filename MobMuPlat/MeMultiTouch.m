//
//  MeMultiTouch.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 2/23/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeMultiTouch.h"
#define BORDER_WIDTH 3
#define CURSOR_WIDTH 2

@interface MeMultiTouch () {
    NSMutableArray* _cursorStack;
    NSMutableArray* _touchStack;
    NSMutableArray* _touchByVoxArray;//add, then hold NSNull values for empty voices
}

@end

@implementation MeMultiTouch

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.address=@"/unnamedMultiTouch";
        //self.backgroundColor = [UIColor purpleColor];
        [self setMultipleTouchEnabled:YES];
        self.clipsToBounds = YES;
        self.layer.borderWidth=BORDER_WIDTH;
        
        _cursorStack = [[NSMutableArray alloc] init];
        _touchStack = [[NSMutableArray alloc] init];
        _touchByVoxArray = [[NSMutableArray alloc] init];
    }
    return self;
}

-(void)setColor:(UIColor *)inColor{
  [super setColor:inColor];
  self.layer.borderColor=[inColor CGColor];
}


-(CGPoint)normAndClipPoint:(CGPoint)inPoint{
  CGPoint outPoint;
  outPoint.x = inPoint.x/self.frame.size.width;
  outPoint.x = MIN(1, MAX(-1, outPoint.x));
  outPoint.y = 1.0-(inPoint.y/self.frame.size.height);
  outPoint.y = MIN(1, MAX(-1, outPoint.y));
  return outPoint;
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
	//printf("\nbegain %d, event %d", [touches count], [[event allTouches] count]);
    for(UITouch* touch in touches){
        
        //stack
      MyTouch* myTouch = [[MyTouch alloc]init];
      myTouch.point = [self normAndClipPoint:[touch locationInView:self]];
      myTouch.origTouch = touch;
      //myTouch.state = 1;
      
        [_touchStack addObject:myTouch];
      
      //cursor
        Cursor* cursor = [[Cursor alloc]init];
      
      
       cursor.cursorX = [[UIView alloc]initWithFrame:CGRectMake(5, 5, self.frame.size.width, CURSOR_WIDTH)];
       cursor.cursorY = [[UIView alloc]initWithFrame:CGRectMake(5, 5, CURSOR_WIDTH, self.frame.size.height)];
      cursor.cursorX.backgroundColor = self.highlightColor;
      cursor.cursorY.backgroundColor = self.highlightColor;
       cursor.cursorX.userInteractionEnabled=NO;
       cursor.cursorY.userInteractionEnabled=NO;
      [self addSubview:cursor.cursorX];
      [self addSubview:cursor.cursorY];
      
        [_cursorStack addObject:cursor];
        
        //poly vox
        BOOL added=NO;
        for(id element in _touchByVoxArray){//find NSNull vox slot
            if(element == [NSNull null]){
              int index = [_touchByVoxArray indexOfObject:element];
              myTouch.polyVox = index+1;
                [_touchByVoxArray replaceObjectAtIndex:index withObject:myTouch];
                added=YES;
                break;
            }
        }
        if(!added){
            [_touchByVoxArray addObject:myTouch];//add to end
            int index = [_touchByVoxArray indexOfObject:myTouch];
            myTouch.polyVox = index+1;
        }
        
        
      
        //NSLog(@"send touches poly %d : %.2f %.2f", myTouch.polyVox, myTouch.point.x, myTouch.point.y);
        [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"touch", [NSNumber numberWithInt:myTouch.polyVox], [NSNumber numberWithInt:1], [NSNumber numberWithFloat:myTouch.point.x], [NSNumber numberWithFloat:myTouch.point.y], nil]];
    }
    [self sendState];
  
    if([_touchStack count]>0) self.layer.borderColor=[self.highlightColor CGColor];
    [self redrawCursors];
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
	//printf("\nmoved %d, event %d", [touches count], [[event allTouches] count]);
    for(UITouch* touch in touches){
        for(MyTouch* myTouch in _touchStack){//TODO optimze! just get object by reference somehow? or use "indexOfObject"
          if(myTouch.origTouch==touch){
                myTouch.point = [self normAndClipPoint:[touch locationInView:self]];
                //CGPoint currPoint= [touch locationInView:self];
                //NSLog(@"move touches in poly %d : %.2f %.2f", [_touchByVoxArray indexOfObject:myTouch]+1, currPoint.x, currPoint.y);
                [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"touch", [NSNumber numberWithInt:myTouch.polyVox], [NSNumber numberWithInt:2], [NSNumber numberWithFloat:myTouch.point.x], [NSNumber numberWithFloat:myTouch.point.y], nil]];
            }
        }
    }
    [self sendState];
    [self redrawCursors];
}
-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
    NSMutableArray* touchesToRemoveArray = [[NSMutableArray alloc] init];
    
	for(UITouch* touch in touches){
        for(MyTouch* myTouch in _touchStack){//TODO optimze! just get object by reference somehow? or use "indexOfObject"!
			if(myTouch.origTouch==touch){
        myTouch.point = [self normAndClipPoint:[touch locationInView:self]];//necc??
                [touchesToRemoveArray addObject:myTouch];
                //CGPoint currPoint= [touch locationInView:self];
                //NSLog(@"remove touches : %.2f %.2f", currPoint.x, currPoint.y);
            }
        }
    }
    
    [_touchStack removeObjectsInArray:touchesToRemoveArray];
    //curosrs
    for(Cursor* cursor in [_cursorStack subarrayWithRange:NSMakeRange([_touchStack count], [_cursorStack count]-[_touchStack count])] ){
        [cursor.cursorX removeFromSuperview];
        [cursor.cursorY removeFromSuperview];
    }
    [_cursorStack removeObjectsInRange:NSMakeRange([_touchStack count], [_cursorStack count]-[_touchStack count])];
    
    //
    for(MyTouch* myTouch in touchesToRemoveArray){
      
      
      [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"touch", [NSNumber numberWithInt:myTouch.polyVox], [NSNumber numberWithInt:2], [NSNumber numberWithFloat:myTouch.point.x], [NSNumber numberWithFloat:myTouch.point.y], nil]];
      
        [_touchByVoxArray replaceObjectAtIndex:[_touchByVoxArray indexOfObject:myTouch] withObject:[NSNull null]];
    }
    
    [self sendState];
    if([_touchStack count]==0) self.layer.borderColor=[self.color CGColor];
    [self redrawCursors];
}

-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event{
	[self touchesEnded:touches withEvent:event];
}

-(void)sendState{
    //all are triplets
    //send stack as is:"/touchesByTime"
    //send by vox with nulls removed:
    //send prev sorted by x,y
    
    NSMutableArray* valArray = [NSMutableArray arrayWithArray:_touchStack];
    /*for(UITouch* touch in _touchStack){
        CGPoint currPoint= [touch locationInView:self];
        [valArray addObject:[NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1]];//float or int?
        [valArray addObject:[NSNumber numberWithFloat:currPoint.x]];
        [valArray addObject:[NSNumber numberWithFloat:currPoint.y]];
    }*/
    
    //send as is
    NSMutableArray* msgArray = [[NSMutableArray alloc]init];
    [msgArray addObject:self.address];
    [msgArray addObject:@"touchesByTime"];
    for(MyTouch* myTouch in valArray){
        [msgArray addObject:[NSNumber numberWithInt:myTouch.polyVox]];//float or int?
        [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.x]];
        [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.y]];
    }
    [self.controlDelegate sendGUIMessageArray:msgArray];
    
    //sort via vox
    [valArray sortUsingComparator:^NSComparisonResult(MyTouch* myTouch1, MyTouch* myTouch2){
        if(myTouch1.polyVox < myTouch2.polyVox) return NSOrderedAscending;
        else if (myTouch1.polyVox > myTouch2.polyVox) return NSOrderedDescending;
        else return NSOrderedSame;
    }];
    
    [msgArray removeAllObjects];
    [msgArray addObject:self.address];
    [msgArray addObject:@"touchesByVox"];
    for(MyTouch* myTouch in valArray){
      [msgArray addObject:[NSNumber numberWithInt:myTouch.polyVox]];//float or int?
      [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.x]];
      [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.y]];
    }
    [self.controlDelegate sendGUIMessageArray:msgArray];
    
    //sort via X
    [valArray sortUsingComparator:^NSComparisonResult(MyTouch* myTouch1, MyTouch* myTouch2){
      
        if(myTouch1.point.x < myTouch2.point.x) return NSOrderedAscending;
        else if (myTouch1.point.x > myTouch2.point.x) return NSOrderedDescending;
        else return NSOrderedSame;
    }];
    
    [msgArray removeAllObjects];
    [msgArray addObject:self.address];
    [msgArray addObject:@"touchesByX"];
  for(MyTouch* myTouch in valArray){
    [msgArray addObject:[NSNumber numberWithInt:myTouch.polyVox]];//float or int?
    [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.x]];
    [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.y]];
  }
    [self.controlDelegate sendGUIMessageArray:msgArray];
  
  //sort via Y
  [valArray sortUsingComparator:^NSComparisonResult(MyTouch* myTouch1, MyTouch* myTouch2){
    
    if(myTouch1.point.y < myTouch2.point.y) return NSOrderedAscending;
    else if (myTouch1.point.y > myTouch2.point.y) return NSOrderedDescending;
    else return NSOrderedSame;
  }];
  
  [msgArray removeAllObjects];
  [msgArray addObject:self.address];
  [msgArray addObject:@"touchesByY"];
  for(MyTouch* myTouch in valArray){
    [msgArray addObject:[NSNumber numberWithInt:myTouch.polyVox]];//float or int?
    [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.x]];
    [msgArray addObject:[NSNumber numberWithFloat:myTouch.point.y]];
  }
  [self.controlDelegate sendGUIMessageArray:msgArray];

}

-(void)redrawCursors{
	for(MyTouch* myTouch in _touchStack){
    Cursor* currCursor = [_cursorStack objectAtIndex:[_touchStack indexOfObject:myTouch]] ;
    //[currCursor.cursorX setCenter:myTouch.point];
    CGPoint HorizCenter=CGPointMake(self.frame.size.width/2, (1.0-myTouch.point.y)*self.frame.size.height);
    CGPoint VertCenter=CGPointMake(myTouch.point.x*self.frame.size.width, self.frame.size.height/2);
		currCursor.cursorX.center=HorizCenter;
		currCursor.cursorY.center=VertCenter;

    }
  
}

@end


@implementation Cursor //handle add/remove from view, and color, touch property in main class

@end

@implementation MyTouch

@end