//
//  MMPPdArrayWidget.m
//  MobMuPlat
//
//  Created by diglesia on 1/26/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdArrayWidget.h"

#import "Gui.h"

@implementation MMPPdArrayWidget {
  int _arraySize;
  float *_arrayData;
  CGPoint _touchDownPoint;
  int _lastTableIndex;

  CGFloat _displayXRangeLow, _displayXRangeHigh, _displayYRangeLow, _displayYRangeHigh;

  CGContextRef _cacheContext;
  int _displayMode; //poly,point,bez
}

- (id)initWithAtomLine:(NSArray *)atomLine // #X array
            valuesLine:(NSArray *)arrayValueLine
            coordsLine:(NSArray *)coordsLine
           restoreLine:(NSArray *)restoreLine
                andGui:(Gui *)gui {
  self = [super initWithAtomLine:atomLine andGui:gui];

  self.originalFrame = CGRectMake([restoreLine[2] floatValue],
                                  [restoreLine[3] floatValue],
                                  [coordsLine[6] floatValue],
                                  [coordsLine[7] floatValue]);
  // communicate with array directly via PdBase.
  // receive name is just to handle messages sent to array that
  // require visual refresh. Check ordering (i.e. that internal array is set before we refresh)
  self.sendName = nil;
  self.receiveName = self.label.text = atomLine[2];

  //_arraySize = [atomLine[3] integerValue]; Nope will be set in loading data.
  _displayXRangeLow = [coordsLine[2] floatValue]; //Not using these yet.
  _displayXRangeHigh = [coordsLine[4] floatValue]; //Not using these yet.
  _displayYRangeLow = [coordsLine[5] floatValue];
  _displayYRangeHigh = [coordsLine[3] floatValue];

  _displayMode = [atomLine[5] intValue] >> 1; //Not used yet, defaulting to "points"
  self.userInteractionEnabled = NO;
  return self;
}

- (void)reshape {
  [super reshape];
  _cacheContext = CGBitmapContextCreate (nil, (int)self.frame.size.width, (int)self.frame.size.height, 8, 0, CGColorSpaceCreateDeviceRGB(),  kCGImageAlphaPremultipliedLast  );
  CGContextSetLineWidth(_cacheContext, 1);
  CGContextSetShouldAntialias(_cacheContext, NO);


  self.label.font = [UIFont fontWithName:self.gui.fontName size:self.gui.fontSize * self.gui.scaleX];
  [self.label sizeToFit];

  // set the label pos from the LRUD setting
  int labelPosY = -self.label.frame.size.height - (2 * self.gui.scaleX);
  self.label.frame = CGRectMake(0, labelPosY,
                                CGRectGetWidth(self.label.frame), CGRectGetHeight(self.label.frame));
}

- (void)drawRect:(CGRect)rect {

  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextTranslateCTM(context, 0.5, 0.5); // snap to nearest pixel
  CGContextSetLineWidth(context, 1.0);

  // background
  //CGContextSetFillColorWithColor(context, self.fillColor.CGColor);
  //CGContextFillRect(context, rect);
  //CGContextSetFillColorWithColor(context, [UIColor clearColor].CGColor);

  // border
  CGContextSetStrokeColorWithColor(context, self.frameColor.CGColor);
  //CGContextStrokeRect(context, CGRectMake(0, 0, CGRectGetWidth(rect)-1, CGRectGetHeight(rect)-1));
  CGContextStrokeRect(context, CGRectMake(0, 0, self.bounds.size.width-1, self.bounds.size.height-1)); //does this frefresh all pizels inside?

  // contents
  CGImageRef cacheImage = CGBitmapContextCreateImage(_cacheContext);
  CGContextDrawImage(context, self.bounds, cacheImage);
  CGImageRelease(cacheImage);
}

- (void)setup {
  [super setup];
  [self copyFromPDAndDraw]; // check loadbang has set underlying array.
}

-(void)copyFromPDAndDraw {

  int newSize = [PdBase arraySizeForArrayNamed:self.receiveName];
  if(newSize!=_arraySize){//new, or resize if needed
    _arraySize = newSize;
    if(_arrayData) {
      free(_arrayData);
    }
    _arrayData = (float*)malloc(_arraySize*sizeof(float));

    self.userInteractionEnabled = YES;
  }
  //but copy even if no resize
  [PdBase copyArrayNamed:self.receiveName withOffset:0 toArray:_arrayData count:_arraySize];

  [self draw];
}

//

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
  _touchDownPoint = [[touches anyObject] locationInView:self];

  float normalizedX = _touchDownPoint.x/self.frame.size.width;
  int touchDownTableIndex = (int)(normalizedX * _arraySize);
  _lastTableIndex = touchDownTableIndex;
  float normalizedY = _touchDownPoint.y/self.frame.size.height;//change to -1 to 1

  float flippedY = (1 - normalizedY)*(_displayYRangeHigh - _displayYRangeLow) + _displayYRangeLow;
    
  _arrayData[touchDownTableIndex] = flippedY;//check bounds
  [self drawFromIndex:touchDownTableIndex toIndex:touchDownTableIndex];
    
  //make one-element array to send in
  float* touchValArray = (float*)malloc(1*sizeof(float));
  touchValArray[0] = flippedY;
  [PdBase copyArray:touchValArray toArrayNamed:self.receiveName withOffset:touchDownTableIndex count:1];
  free(touchValArray);
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
  CGPoint dragPoint = [[touches anyObject] locationInView:self];

  float normalizedX = dragPoint.x/self.frame.size.width;
  normalizedX = MAX(MIN(normalizedX,1),0);
  int dragTableIndex = (int)(normalizedX * _arraySize);
  if(dragTableIndex >= _arraySize) dragTableIndex = _arraySize - 1;
  float normalizedY = dragPoint.y/self.frame.size.height;//change to -1 to 1
  normalizedY = MAX(MIN(normalizedY,1),0);

  float flippedY = (1 - normalizedY)*(_displayYRangeHigh - _displayYRangeLow) + _displayYRangeLow;
    
  //compute size, including self but not prev
  int traversedElementCount = abs(dragTableIndex - _lastTableIndex);
  if(traversedElementCount==0)traversedElementCount=1;
  float* touchValArray = (float*)malloc(traversedElementCount*sizeof(float));
    
  _arrayData[dragTableIndex] = flippedY;
  //just one
  if(traversedElementCount==1) {
    [self drawFromIndex:dragTableIndex toIndex:dragTableIndex];
    touchValArray[0] = flippedY;
    [PdBase copyArray:touchValArray toArrayNamed:self.receiveName withOffset:dragTableIndex count:1];
    free(touchValArray);
  } else {
    //NSLog(@"multi!");
    int minIndex = MIN(_lastTableIndex, dragTableIndex);
    int maxIndex = MAX(_lastTableIndex, dragTableIndex);
      
    float minValue = _arrayData[minIndex];
    float maxValue = _arrayData[maxIndex];
      //NSLog(@"skip within %d (%.2f) to %d(%.2f)", minTouchIndex, [[_valueArray objectAtIndex:minTouchIndex] floatValue], maxTouchIndex, [[_valueArray objectAtIndex:maxTouchIndex] floatValue]);
    for(int i=minIndex+1;i<=maxIndex;i++){
      float percent = ((float)(i-minIndex))/(maxIndex-minIndex);
      float interpVal = (maxValue - minValue) * percent  + minValue ;
        //NSLog(@"%d %.2f %.2f", i, percent, interpVal);
      _arrayData[i]=interpVal;
      touchValArray[i-(minIndex+1)]=interpVal;
    }
    [self drawFromIndex:minIndex toIndex:maxIndex];
    [PdBase copyArray:touchValArray toArrayNamed:self.receiveName withOffset:minIndex+1 count:traversedElementCount];
    free(touchValArray);
  }
  _lastTableIndex = dragTableIndex;
}

-(void)draw{
  [self drawFromIndex:0 toIndex:_arraySize-1];
}

-(void)drawFromIndex:(int)indexA toIndex:(int)indexB {
  // check for div by zero
  if (_displayYRangeHigh == _displayYRangeLow)return;

  // line
  CGContextSetRGBStrokeColor(_cacheContext, 0,0,0,1);
  // fill
  CGContextSetRGBFillColor(_cacheContext, 1,1,1,1);
  CGContextBeginPath(_cacheContext);

  CGContextMoveToPoint(_cacheContext, 0,0);
	int padding = 3;
  int indexDrawPointA = (int)((float)MIN(indexA,indexB)/_arraySize*self.frame.size.width)-padding;
  indexDrawPointA = MIN(MAX(indexDrawPointA,0),self.frame.size.width-1);
  int indexDrawPointB = (int)((float)(MAX(indexA,indexB)+1)/(_arraySize)*self.frame.size.width)+padding;
  indexDrawPointB = MIN(MAX(indexDrawPointB,0),self.frame.size.width-1);
  
  CGRect rect = CGRectMake(indexDrawPointA, 0, indexDrawPointB-indexDrawPointA, self.frame.size.height);
  CGContextClearRect(_cacheContext, rect);

  
  for(int i=indexDrawPointA; i<=indexDrawPointB; i++){
    float x = (float)i;//(float)i/self.frame.size.width;
    int currIndex = (int)((float)i/self.frame.size.width*_arraySize);
    
    //if touch down one point, make sure that point is represented in redraw and not skipped over
    int prevIndex = (int)((float)(i-1)/self.frame.size.width*_arraySize);
    int nextIndex = (int)((float)(i+1)/self.frame.size.width*_arraySize); //could be out of bounds.
    if(indexA==indexB && indexA<currIndex && indexA>prevIndex) currIndex = indexA;

    float minValForPoint = _arrayData[currIndex], maxValForPoint = _arrayData[currIndex];
    // scan all data points for min/max val
    for (int index = currIndex; index<nextIndex && index<_arraySize; index++) {
      float val = _arrayData[index];
      if (val>maxValForPoint)maxValForPoint = val;
      if (val<minValForPoint)minValForPoint = val;
    }


      // Scale lo to hi to flipped 0 to frame height.
    float unflippedMinY = 1-( (minValForPoint-_displayYRangeLow)/(_displayYRangeHigh - _displayYRangeLow));
    unflippedMinY *= self.frame.size.height;
    float unflippedMaxY = 1-( (maxValForPoint-_displayYRangeLow)/(_displayYRangeHigh - _displayYRangeLow));
    unflippedMaxY *= self.frame.size.height;

    CGContextMoveToPoint(_cacheContext, x,unflippedMinY);
    CGContextAddLineToPoint(_cacheContext, x,unflippedMaxY+1);

    // start point, or discontinuity to new value
    /*if(i==indexDrawPointA){// || prevIndex != index) {
      CGContextMoveToPoint(_cacheContext, x,unflippedMinY);
      CGContextAddLineToPoint(_cacheContext, x,unflippedMaxY+1);
    }
    else { //keep drawing current index
      CGContextAddLineToPoint(_cacheContext, x, unflippedY);
      //CGContextMoveToPoint(_cacheContext, x,unflippedY);
    }*/
  }

  CGContextStrokePath(_cacheContext);

  //if (_displayMode == 0) { //poly
  // CGContextStrokePath(_cacheContext);
  //} else {
    //CGCont
    // fill
    /*// add points and close
    CGFloat yPointOfTableZero = 1 - ((0 -_displayRangeLo)/(_displayRangeHi - _displayRangeLo));
    yPointOfTableZero *= self.frame.size.height;
    CGContextAddLineToPoint(_cacheContext, indexDrawPointB, yPointOfTableZero);
    CGContextAddLineToPoint(_cacheContext, indexDrawPointA, yPointOfTableZero);
    CGContextClosePath(_cacheContext);
    CGContextDrawPath(_cacheContext, kCGPathEOFillStroke);*/
  //}

  CGRect newRect = CGRectMake(indexDrawPointA, 0, indexDrawPointB,self.frame.size.height);
  [self setNeedsDisplayInRect:newRect];
  //[self setNeedsDisplay];
}

//

- (void)receiveBangFromSource:(NSString *)source {
  [self copyFromPDAndDraw];
}

- (void)receiveList:(NSArray *)list fromSource:(NSString *)source {
  [self copyFromPDAndDraw];
}

- (void)receiveMessage:(NSString *)message withArguments:(NSArray *)arguments fromSource:(NSString *)source {
  [self copyFromPDAndDraw];
}

@end
