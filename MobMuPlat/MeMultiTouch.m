//
//  MeMultiTouch.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 2/23/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeMultiTouch.h"

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
        self.address=@"/unnamedButton";
        self.backgroundColor = [UIColor purpleColor];
        [self setMultipleTouchEnabled:YES];
        self.clipsToBounds = YES;
        
        _cursorStack = [[NSMutableArray alloc] init];
        _touchStack = [[NSMutableArray alloc] init];
        _touchByVoxArray = [[NSMutableArray alloc] init];
    }
    return self;
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
	//printf("\nbegain %d, event %d", [touches count], [[event allTouches] count]);
    for(UITouch* touch in touches){
        
        //stack
        [_touchStack addObject:touch];
        //cursor
        Cursor* cursor = [[Cursor alloc]init];
        [self addSubview:cursor.cursorX];
        [self addSubview:cursor.cursorY];
        cursor.cursorX.backgroundColor = [UIColor yellowColor];
        cursor.cursorY.backgroundColor = [UIColor yellowColor];
        
        [_cursorStack addObject:cursor];
        
        //poly vox
        BOOL added=NO;
        for(id element in _touchByVoxArray){//find NSNull vox slot
            if(element == [NSNull null]){
                [_touchByVoxArray replaceObjectAtIndex:[_touchByVoxArray indexOfObject:element] withObject:touch];
                added=YES;
                break;
            }
        }
        if(!added){
            [_touchByVoxArray addObject:touch];//add to end
        }
        
        
        CGPoint currPoint= [touch locationInView:self];
        NSLog(@"send touches poly %d : %.2f %.2f", [_touchByVoxArray indexOfObject:touch]+1, currPoint.x, currPoint.y);
        [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"touch", [NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1], [NSNumber numberWithInt:1], [NSNumber numberWithFloat:currPoint.x], [NSNumber numberWithFloat:currPoint.y], nil]];
    }
    [self sendState];
    [self redrawCursors];
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
	//printf("\nmoved %d, event %d", [touches count], [[event allTouches] count]);
    for(UITouch* touch in touches){
        for(UITouch* myTouch in _touchStack){//TODO optimze! just get object by reference somehow? or use "indexOfObject"!
			if(myTouch==touch){
                
                CGPoint currPoint= [touch locationInView:self];
                NSLog(@"move touches in poly %d : %.2f %.2f", [_touchByVoxArray indexOfObject:myTouch]+1, currPoint.x, currPoint.y);
                [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"touch", [NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1], [NSNumber numberWithInt:2], [NSNumber numberWithFloat:currPoint.x], [NSNumber numberWithFloat:currPoint.y], nil]];
            }
        }
    }
    [self sendState];
    [self redrawCursors];
}
-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
    NSMutableArray* touchesToRemoveArray = [[NSMutableArray alloc] init];
    
	for(UITouch* touch in touches){
        for(UITouch* myTouch in _touchStack){//TODO optimze! just get object by reference somehow? or use "indexOfObject"!
			if(myTouch==touch){
                [touchesToRemoveArray addObject:touch];
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
    for(UITouch* touch in touchesToRemoveArray){
        CGPoint currPoint= [touch locationInView:self];
        NSLog(@"remove touches poly %d: %.2f %.2f", [_touchByVoxArray indexOfObject:touch]+1, currPoint.x, currPoint.y);
        
        [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"touch", [NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1], [NSNumber numberWithInt:0], [NSNumber numberWithFloat:currPoint.x], [NSNumber numberWithFloat:currPoint.y], nil]];
        
        [_touchByVoxArray replaceObjectAtIndex:[_touchByVoxArray indexOfObject:touch] withObject:[NSNull null]];
    }
    
    [self sendState];
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
    for(UITouch* touch in valArray){
        CGPoint currPoint= [touch locationInView:self];
        [msgArray addObject:[NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1]];//float or int?
        [msgArray addObject:[NSNumber numberWithFloat:currPoint.x/self.frame.size.width]];
        [msgArray addObject:[NSNumber numberWithFloat:currPoint.y/self.frame.size.height]];
    }
    [self.controlDelegate sendGUIMessageArray:msgArray];
    
    //sort via vox
    [valArray sortUsingComparator:^NSComparisonResult(UITouch* touch1, UITouch* touch2){
        if([_touchByVoxArray indexOfObject:touch1] < [_touchByVoxArray indexOfObject:touch2]) return NSOrderedAscending;
        else if ([_touchByVoxArray indexOfObject:touch1] > [_touchByVoxArray indexOfObject:touch2]) return NSOrderedDescending;
        else return NSOrderedSame;
    }];
    
    [msgArray removeAllObjects];
    [msgArray addObject:self.address];
    [msgArray addObject:@"touchesByVox"];
    for(UITouch* touch in valArray){
        CGPoint currPoint= [touch locationInView:self];
        [msgArray addObject:[NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1]];//float or int?
        [msgArray addObject:[NSNumber numberWithFloat:currPoint.x/self.frame.size.width]];
        [msgArray addObject:[NSNumber numberWithFloat:currPoint.y/self.frame.size.height]];
    }
    [self.controlDelegate sendGUIMessageArray:msgArray];
    
    //sort via X
    [valArray sortUsingComparator:^NSComparisonResult(UITouch* touch1, UITouch* touch2){
        CGPoint currPoint1= [touch1 locationInView:self];
        CGPoint currPoint2= [touch2 locationInView:self];
        if(currPoint1.x < currPoint2.x) return NSOrderedAscending;
        else if (currPoint1.x > currPoint2.x) return NSOrderedDescending;
        else return NSOrderedSame;
    }];
    
    [msgArray removeAllObjects];
    [msgArray addObject:self.address];
    [msgArray addObject:@"touchesByX"];
    for(UITouch* touch in valArray){
        CGPoint currPoint= [touch locationInView:self];
        [msgArray addObject:[NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1]];//float or int?
        [msgArray addObject:[NSNumber numberWithFloat:currPoint.x/self.frame.size.width]];
        [msgArray addObject:[NSNumber numberWithFloat:currPoint.y/self.frame.size.height]];
    }
    [self.controlDelegate sendGUIMessageArray:msgArray];
    
    
    
    
    //[self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"touch", [NSNumber numberWithInt:[_touchByVoxArray indexOfObject:touch]+1], [NSNumber numberWithInt:0], [NSNumber numberWithFloat:currPoint.x], [NSNumber numberWithFloat:currPoint.y], nil]];
}

-(void)redrawCursors{
	for(UITouch* touch in _touchStack){
        [[_cursorStack objectAtIndex:[_touchStack indexOfObject:touch]] layoutAtPoint:[touch locationInView:self]];
    }
    
}


/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    // Drawing code
}
*/

@end

#define CURSOR_WIDTH 2;



@implementation Cursor //handle add/remove from view, and color, touch property in main class

-(id)init{
    self = [super init];
    if(self){
        _cursorX = [[UIView alloc] init];
        _cursorY = [[UIView alloc] init];
    }
    return self;
}

-(void)layoutAtPoint:(CGPoint)point {
    [_cursorX setFrame:CGRectMake(0, 0, 50, 50)];
    [_cursorX setCenter:point];
}
@end