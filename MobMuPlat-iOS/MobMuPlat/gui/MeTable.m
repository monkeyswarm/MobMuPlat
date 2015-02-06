//
//  MeTable.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 4/24/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeTable.h"

@implementation MeTable {
  CGContextRef _cacheContext;
  CGContextRef _cacheContextSelection;
  float fR,fG,fB,fA;//FRGBA
  float sR,sG,sB,sA;//FRGBA
  NSUInteger tableSize;
  float *tableData;
  
  CGPoint touchDownPoint;
  int lastTableIndex;
  //int touchDownTableIndex;
  BOOL _tableSeemsBad;
}

- (id)initWithFrame:(CGRect)frame
{
  self = [super initWithFrame:frame];
  if (self) {
    self.address = @"/unnamedTable";
    self.userInteractionEnabled = NO;//until table load
    self.selectionColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:.5];
    //defaults
    _displayRangeLo = -1;
    _displayRangeHi = 1;

    _cacheContext = CGBitmapContextCreate (nil, (int)frame.size.width, (int)frame.size.height, 8, 0, CGColorSpaceCreateDeviceRGB(),  kCGImageAlphaPremultipliedLast  );
    CGContextSetLineWidth(_cacheContext, 2);
    _cacheContextSelection = CGBitmapContextCreate (nil, (int)frame.size.width, (int)frame.size.height, 8, 0, CGColorSpaceCreateDeviceRGB(),kCGImageAlphaPremultipliedLast  );
    
  }
  return self;
}

-(void)loadTable {
  [self copyFromPDAndDraw];
}

-(void)copyFromPDAndDraw{
  
  int newSize = [PdBase arraySizeForArrayNamed:self.address];
  if(newSize!=tableSize){//new, or resize if needed
    tableSize = newSize;
    if(tableData)free(tableData);
    tableData = (float*)malloc(tableSize*sizeof(float));
    if(!tableData){//bad table - no good way to test for no table of "name"
      free(tableData);
      _tableSeemsBad = YES;
      return;
    }
    _tableSeemsBad = NO;
    self.userInteractionEnabled = YES;
  }
  //but copy even if no resize
  [PdBase copyArrayNamed:self.address withOffset:0 toArray:tableData count:tableSize];
  
  if(!_tableSeemsBad)[self draw];
}

-(void)draw{
  [self drawFromIndex:0 toIndex:tableSize-1];
}

-(void)drawFromIndex:(int)indexA toIndex:(int)indexB {
  // check for div by zero
  if (_displayRangeHi == _displayRangeLo)return;

  // line
  CGContextSetRGBStrokeColor(_cacheContext, fR,fG,fB,fA);
  // fill
  CGContextSetRGBFillColor(_cacheContext, fR, fG, fB, fA);
  CGContextBeginPath(_cacheContext);

  CGContextMoveToPoint(_cacheContext, 0,0);
	int padding = 3;
  int indexDrawPointA = (int)((float)MIN(indexA,indexB)/tableSize*self.frame.size.width)-padding;
  indexDrawPointA = MIN(MAX(indexDrawPointA,0),self.frame.size.width-1);
  int indexDrawPointB = (int)((float)(MAX(indexA,indexB)+1)/(tableSize)*self.frame.size.width)+padding;
  indexDrawPointB = MIN(MAX(indexDrawPointB,0),self.frame.size.width-1);
  
  CGRect rect = CGRectMake(indexDrawPointA, 0, indexDrawPointB-indexDrawPointA, self.frame.size.height);
  CGContextClearRect(_cacheContext, rect);

  
  for(int i=indexDrawPointA; i<=indexDrawPointB; i++){
    float x = (float)i;//(float)i/self.frame.size.width;
    int index = (int)((float)i/self.frame.size.width*tableSize);
    
    //if touch down one point, make sure that point is represented in redraw and not skipped over
    int prevIndex = (int)((float)(i-1)/self.frame.size.width*tableSize);
    if(indexA==indexB && indexA<index && indexA>prevIndex) index = indexA;
    
    float y = tableData[index];
    // Scale lo to hi to flipped 0 to frame height.
    float unflippedY = 1-( (y-_displayRangeLo)/(_displayRangeHi - _displayRangeLo));
    unflippedY *= self.frame.size.height;

    /*float unflippedY;
    if (_displayRange == 0) { //polar
      unflippedY = (1-((y+1)/2)) *self.frame.size.height;
    } else { //0 to 1
      unflippedY = (1-y) *self.frame.size.height;
    }*/

    //NSLog(@"i %d x %.2f index %d y %.2f unflip %.2f", i,x,index,y, unflippedY);
    if(i==indexDrawPointA){
      CGContextMoveToPoint(_cacheContext, x,unflippedY);
    }
    else {
      CGContextAddLineToPoint(_cacheContext, x, unflippedY);
      //CGContextMoveToPoint(_cacheContext, x,unflippedY);
    }
  }

  if (_displayMode == 0) { //line
   CGContextStrokePath(_cacheContext);
  } else { // fill
    // add points and close
    CGFloat yPointOfTableZero = 1 - ((0 -_displayRangeLo)/(_displayRangeHi - _displayRangeLo));
    yPointOfTableZero *= self.frame.size.height;
    CGContextAddLineToPoint(_cacheContext, indexDrawPointB, yPointOfTableZero);
    CGContextAddLineToPoint(_cacheContext, indexDrawPointA, yPointOfTableZero);
    CGContextClosePath(_cacheContext);
    CGContextDrawPath(_cacheContext, kCGPathEOFillStroke);
  }
  CGRect newRect = CGRectMake(indexDrawPointA, 0, indexDrawPointB,self.frame.size.height);
  [self setNeedsDisplayInRect:newRect];
}

-(void)clearHighlight{
  CGContextClearRect(_cacheContextSelection, self.bounds);
  [self setNeedsDisplay];
}

-(void)drawHighlightBetween:(CGPoint)pointA and:(CGPoint)pointB{
  CGContextSetRGBFillColor(_cacheContextSelection, sR,sG,sB,sA);
	CGRect newRect = CGRectMake( MIN(pointA.x,pointB.x), 0, MAX(fabsf(pointB.x-pointA.x),2), self.frame.size.height);
  CGContextClearRect(_cacheContextSelection, self.bounds);//todo could optimize by computing what needs to be cleared
  CGContextFillRect(_cacheContextSelection, newRect);
  [self setNeedsDisplay];
}

-(void)setColor:(UIColor *)color{
  [super setColor:color];
  self.backgroundColor = color;
  
}

-(void)setHighlightColor:(UIColor *)highlightColor{
  [super setHighlightColor:highlightColor];
  
  CGColorRef color = [highlightColor CGColor];
  int numComponents = CGColorGetNumberOfComponents(color);
  
  if (numComponents == 4)
  {
    const CGFloat *components = CGColorGetComponents(color);
    fR = components[0];
    fG = components[1];
    fB = components[2];
    fA = components[3];
  }
  
}

-(void)setSelectionColor:(UIColor *)selectionColor{
  _selectionColor = selectionColor;
  
  CGColorRef color = [_selectionColor CGColor];
  int numComponents = CGColorGetNumberOfComponents(color);
  
  if (numComponents == 4)
  {
    const CGFloat *components = CGColorGetComponents(color);
    sR = components[0];
    sG = components[1];
    sB = components[2];
    sA = components[3];
  }
  
}

- (void)drawRect:(CGRect)rect {
  
  CGContextRef context = UIGraphicsGetCurrentContext();
  
  CGImageRef cacheImageSelection = CGBitmapContextCreateImage(_cacheContextSelection);
  CGContextDrawImage(context, self.bounds, cacheImageSelection);
  CGImageRelease(cacheImageSelection);
  
  CGImageRef cacheImage = CGBitmapContextCreateImage(_cacheContext);
  CGContextDrawImage(context, self.bounds, cacheImage);
  CGImageRelease(cacheImage);
  
}

//
-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
  touchDownPoint = [[touches anyObject] locationInView:self];

  if(_mode==0){
    //float normalizedX = touchDownPoint.x/self.frame.size.width;
    //touchDownTableIndex = (int)(normalizedX*tableSize);
    //NSLog(@"touchDownTableIndex %d", touchDownTableIndex);

    float normalizedX = touchDownPoint.x/self.frame.size.width;
    normalizedX = MAX(MIN(normalizedX,1),0);
    int touchTableIndex = (int)(normalizedX*tableSize);
    if(touchTableIndex>tableSize-1)touchTableIndex=tableSize-1;//clip

    [self sendRangeMessageFromIndex:touchTableIndex toIndex:touchTableIndex];

    //clear prev selection
    CGContextClearRect(_cacheContextSelection, self.bounds);
    [self setNeedsDisplay];
    [self drawHighlightBetween:touchDownPoint and:touchDownPoint];//sliver
   
  } else if (_mode==1){//draw mode
    float normalizedX = touchDownPoint.x/self.frame.size.width;
    int touchDownTableIndex = (int)(normalizedX*tableSize);
    lastTableIndex = touchDownTableIndex;
    float normalizedY = touchDownPoint.y/self.frame.size.height;//change to -1 to 1

    float flippedY = (1 - normalizedY)*(_displayRangeHi - _displayRangeLo) + _displayRangeLo;
    /*float flippedY;
    if (_displayRange == 0) { // polar
      flippedY = (1-normalizedY)*2-1;
    } else { //0 to 1
      flippedY = 1-normalizedY;
    }*/
    
    //NSLog(@"touchDownTableIndex %d", touchDownTableIndex);
    
    tableData[touchDownTableIndex] = flippedY;//check bounds
    [self drawFromIndex:touchDownTableIndex toIndex:touchDownTableIndex];
    
    //make one-element array to send in
    float* touchValArray = (float*)malloc(1*sizeof(float));
    touchValArray[0] = flippedY;
    [PdBase copyArray:touchValArray toArrayNamed:self.address withOffset:touchDownTableIndex count:1];//put this in draw?
    free(touchValArray);
    
  }
}

-(void)sendRangeMessageFromIndex:(int)indexA toIndex:(int)indexB {
  [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, @"range", [NSNumber numberWithInt:indexA], [NSNumber numberWithInt:indexB], nil]];
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
  CGPoint dragPoint = [[touches anyObject] locationInView:self];
  
  if(_mode==0){
    
    float normalizedXA = touchDownPoint.x/self.frame.size.width;
    normalizedXA = MAX(MIN(normalizedXA,1),0);
    int dragTableIndexA = (int)(normalizedXA*tableSize);
    
    float normalizedXB = dragPoint.x/self.frame.size.width;
    normalizedXB = MAX(MIN(normalizedXB,1),0);
    int dragTableIndexB = (int)(normalizedXB*tableSize);
    if(dragTableIndexB>tableSize-1)dragTableIndexB=tableSize-1;//clip
    
    [self sendRangeMessageFromIndex:MIN(dragTableIndexA,dragTableIndexB) toIndex:MAX(dragTableIndexA,dragTableIndexB)];
    
    [self drawHighlightBetween:touchDownPoint and:dragPoint];
    //touchDownTableIndex = (int)(normalizedX*tableSize);
    //NSLog(@"touchDownTableIndex %d", touchDownTableIndex);

  } else if(_mode==1){ //draw mode
    float normalizedX = dragPoint.x/self.frame.size.width;
    normalizedX = MAX(MIN(normalizedX,1),0);
    int dragTableIndex = (int)(normalizedX*tableSize);
    if(dragTableIndex >= tableSize) dragTableIndex = tableSize - 1;
    float normalizedY = dragPoint.y/self.frame.size.height;//change to -1 to 1
    normalizedY = MAX(MIN(normalizedY,1),0);

    float flippedY = (1 - normalizedY)*(_displayRangeHi - _displayRangeLo) + _displayRangeLo;
    /*float flippedY;
    if (_displayRange == 0) { // polar
      flippedY = (1-normalizedY)*2-1;
    } else { //0 to 1
      flippedY = 1-normalizedY;
    }*/
    //NSLog(@"dragTableIndex %d", dragTableIndex);
    
    //compute size, including self but not prev
    int traversedElementCount = abs(dragTableIndex-lastTableIndex);
    if(traversedElementCount==0)traversedElementCount=1;
    float* touchValArray = (float*)malloc(traversedElementCount*sizeof(float));
    
    tableData[dragTableIndex] = flippedY;
    //just one
    if(traversedElementCount==1) {
      
      [self drawFromIndex:dragTableIndex toIndex:dragTableIndex];
      touchValArray[0] = flippedY;
      [PdBase copyArray:touchValArray toArrayNamed:self.address withOffset:dragTableIndex count:1];
      free(touchValArray);
    } else {
      //NSLog(@"multi!");
      int minIndex = MIN(lastTableIndex, dragTableIndex);
      int maxIndex = MAX(lastTableIndex, dragTableIndex);
      
      float minValue = tableData[minIndex];
      float maxValue = tableData[maxIndex];
      //NSLog(@"skip within %d (%.2f) to %d(%.2f)", minTouchIndex, [[_valueArray objectAtIndex:minTouchIndex] floatValue], maxTouchIndex, [[_valueArray objectAtIndex:maxTouchIndex] floatValue]);
      for(int i=minIndex+1;i<=maxIndex;i++){
        float percent = ((float)(i-minIndex))/(maxIndex-minIndex);
        float interpVal = (maxValue - minValue) * percent  + minValue ;
        //NSLog(@"%d %.2f %.2f", i, percent, interpVal);
        tableData[i]=interpVal;
        touchValArray[i-(minIndex+1)]=interpVal;
      }
      [self drawFromIndex:minIndex toIndex:maxIndex];
      [PdBase copyArray:touchValArray toArrayNamed:self.address withOffset:minIndex+1 count:traversedElementCount];
      free(touchValArray);
    }
    
  
    lastTableIndex = dragTableIndex;
  }

}



-(void)receiveList:(NSArray *)inArray{
  if ([inArray count]==1 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"refresh"]) {
      [self copyFromPDAndDraw];//add range arguments? nah.
  }
  else if ([inArray count]==1 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"clearSelection"]) {
    if (_mode==0) {
      [self clearHighlight];
    }
  }
}



@end
