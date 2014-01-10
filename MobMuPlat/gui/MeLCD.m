//
//  MeLCD.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/8/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeLCD.h"

@interface MeLCD () {
 
    float fR,fG,fB,fA;//FRGBA
    float bR,bG,bB,bA;//BRGBA
    CGPoint penPoint;
    float penWidth;
}

@end

@implementation MeLCD

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        self.width = 5;
        _cacheContext = CGBitmapContextCreate (nil, (int)frame.size.width, (int)frame.size.height, 8, 0, CGColorSpaceCreateDeviceRGB(),  kCGImageAlphaPremultipliedLast  );
        //[self clear];HERE how to make cachecontext alpha background
        //NSTimer* timer = [NSTimer scheduledTimerWithTimeInterval:1. target:self selector:@selector(drawSquare) userInfo:nil repeats:YES];
        penPoint = CGPointMake(0, 0);
        penWidth = 1;
        
    }
    return self;
}

-(void)setColor:(UIColor *)color{
    [super setColor:color];
   
    CGColorRef cgcolor = [color CGColor];
    int numComponents = CGColorGetNumberOfComponents(cgcolor);
    
    if (numComponents == 4)
    {
        const CGFloat *components = CGColorGetComponents(cgcolor);
        bR  = components[0];
        bG  = components[1];
        bB  = components[2];
        bA  = components[3];
    }
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
//

-(void)clear{
    CGContextClearRect(_cacheContext, self.bounds);
    [self setNeedsDisplayInRect:self.bounds];
}

-(void)paintRectX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2 R:(float)r G:(float)g B:(float)b A:(float)a{
    CGContextSetRGBFillColor(_cacheContext, r,g,b,a);
	CGRect newRect = CGRectMake( MIN(x,x2)*self.frame.size.width, MIN(y,y2)*self.frame.size.height, fabsf(x2-x)*self.frame.size.width, fabs(y2-y)*self.frame.size.height);
    CGContextFillRect(_cacheContext, newRect);
    [self setNeedsDisplayInRect:newRect];
}

-(void)paintRectX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2{
    [self paintRectX:x Y:y X2:x2 Y2:y2 R:fR G:fG B:fB A:fA];
}

-(void)frameRectX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2 R:(float)r G:(float)g B:(float)b A:(float)a{
    CGContextSetRGBStrokeColor(_cacheContext, r,g,b,a);
	CGRect newRect = CGRectMake( MIN(x,x2)*self.frame.size.width, MIN(y,y2)*self.frame.size.height, fabsf(x2-x)*self.frame.size.width, fabs(y2-y)*self.frame.size.height);
    CGContextStrokeRect(_cacheContext, newRect);
    
    newRect = CGRectMake( newRect.origin.x-penWidth, newRect.origin.y-penWidth, newRect.size.width+(2*penWidth), newRect.size.height+(2*penWidth));
    [self setNeedsDisplayInRect:newRect];
}

-(void)frameRectX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2{
    [self frameRectX:x Y:y X2:x2 Y2:y2 R:fR G:fG B:fB A:fA];
}

-(void)frameOvalX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2 R:(float)r G:(float)g B:(float)b A:(float)a{
    CGContextSetRGBStrokeColor(_cacheContext, r,g,b,a);
	CGRect newRect = CGRectMake( MIN(x,x2)*self.frame.size.width, MIN(y,y2)*self.frame.size.height, fabsf(x2-x)*self.frame.size.width, fabs(y2-y)*self.frame.size.height);
    CGContextStrokeEllipseInRect(_cacheContext, newRect);
    
    newRect = CGRectMake( newRect.origin.x-penWidth, newRect.origin.y-penWidth, newRect.size.width+(2*penWidth), newRect.size.height+(2*penWidth));
    [self setNeedsDisplayInRect:newRect];
}

-(void)frameOvalX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2{
    [self frameOvalX:x Y:y X2:x2 Y2:y2 R:fR G:fG B:fB A:fA];
}

-(void)paintOvalX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2 R:(float)r G:(float)g B:(float)b A:(float)a{
    CGContextSetRGBFillColor(_cacheContext, r,g,b,a);
	CGRect newRect = CGRectMake( MIN(x,x2)*self.frame.size.width, MIN(y,y2)*self.frame.size.height, fabsf(x2-x)*self.frame.size.width, fabs(y2-y)*self.frame.size.height);
    CGContextFillEllipseInRect(_cacheContext, newRect);
    [self setNeedsDisplayInRect:newRect];
}

-(void)paintOvalX:(float)x Y:(float)y X2:(float)x2 Y2:(float)y2{
    [self paintOvalX:x Y:y X2:x2 Y2:y2 R:fR G:fG B:fB A:fA];
}

-(void)moveToX:(float)x Y:(float)y {
    penPoint.x = x*self.frame.size.width;
    penPoint.y = y*self.frame.size.height;
    
}

-(void)lineToX:(float)x Y:(float)y R:(float)r G:(float)g B:(float)b A:(float)a{
    //convert to coords
    x = x*self.frame.size.width;
    y = y*self.frame.size.height;
    
    NSLog(@"pen x %.2f y %.2f TO x %.2f y %.2f ", penPoint.x, penPoint.y, x, y);
    CGContextSetRGBStrokeColor(_cacheContext, r,g,b,a);
    CGContextMoveToPoint(_cacheContext, penPoint.x,penPoint.y);
	CGContextAddLineToPoint(_cacheContext, x, y);
	CGContextStrokePath(_cacheContext);
    CGRect newRect = CGRectMake(MIN(penPoint.x, x)-penWidth, MIN(penPoint.y, y)-penWidth, fabs(penPoint.x-x)+(2*penWidth), fabs(penPoint.y-y)+(2*penWidth));
    [self setNeedsDisplayInRect:newRect];
    //[self setNeedsDisplay];
    
    CGContextMoveToPoint(_cacheContext, x,y);
    penPoint.x = x;
    penPoint.y = y;
    
}

-(void)lineToX:(float)x Y:(float)y{
    [self lineToX:x Y:y R:fR G:fG B:fB A:fA];
}

-(void)setPenWidth:(float)w{
    penWidth = w;
    CGContextSetLineWidth(_cacheContext, w);
}

//touch sent out

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event{
	CGPoint point = [[touches anyObject] locationInView:self];
    float valX = point.x/self.frame.size.width;
	float valY = point.y/self.frame.size.height;
    if(valX>1)valX=1; if(valX<0)valX=0;
    if(valY>1)valY=1; if(valY<0)valY=0;
    
    [self sendValueState:1.f X:valX Y:valY];
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event{
	CGPoint point = [[touches anyObject] locationInView:self];
    float valX = point.x/self.frame.size.width;
	float valY = point.y/self.frame.size.height;
    if(valX>1)valX=1; if(valX<0)valX=0;
    if(valY>1)valY=1; if(valY<0)valY=0;
    
    [self sendValueState:2.f X:valX Y:valY];
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event{
    CGPoint point = [[touches anyObject] locationInView:self];
    float valX = point.x/self.frame.size.width;
	float valY = point.y/self.frame.size.height;
    if(valX>1)valX=1; if(valX<0)valX=0;
    if(valY>1)valY=1; if(valY<0)valY=0;
    
    [self sendValueState:0.f X:valX Y:valY];
}
-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event{
	[self touchesEnded:touches withEvent:event];
}

-(void)sendValueState:(float)state X:(float)x Y:(float)y{
    [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithFloat:state], [NSNumber numberWithFloat:x], [NSNumber numberWithFloat:y], nil]];
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
    if([inArray count]==5 && [[inArray objectAtIndex:0] isEqualToString:@"paintrect"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self paintRectX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue]];
    }
    else if([inArray count]==9 && [[inArray objectAtIndex:0] isEqualToString:@"paintrect"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self paintRectX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue] R:[[inArray objectAtIndex:5] floatValue] G:[[inArray objectAtIndex:6] floatValue] B:[[inArray objectAtIndex:7] floatValue] A:[[inArray objectAtIndex:8] floatValue]];
    }
    else if([inArray count]==5 && [[inArray objectAtIndex:0] isEqualToString:@"framerect"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self frameRectX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue]];
    }
    else if([inArray count]==9 && [[inArray objectAtIndex:0] isEqualToString:@"framerect"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self frameRectX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue] R:[[inArray objectAtIndex:5] floatValue] G:[[inArray objectAtIndex:6] floatValue] B:[[inArray objectAtIndex:7] floatValue] A:[[inArray objectAtIndex:8] floatValue]];
    }
    else if([inArray count]==5 && [[inArray objectAtIndex:0] isEqualToString:@"paintoval"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self paintOvalX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue]];
    }
    else if([inArray count]==9 && [[inArray objectAtIndex:0] isEqualToString:@"paintoval"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self paintOvalX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue] R:[[inArray objectAtIndex:5] floatValue] G:[[inArray objectAtIndex:6] floatValue] B:[[inArray objectAtIndex:7] floatValue] A:[[inArray objectAtIndex:8] floatValue]];
    }
    else if([inArray count]==5 && [[inArray objectAtIndex:0] isEqualToString:@"frameoval"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self frameOvalX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue]];
    }
    else if([inArray count]==9 && [[inArray objectAtIndex:0] isEqualToString:@"frameoval"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self frameOvalX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] X2:[[inArray objectAtIndex:3] floatValue] Y2:[[inArray objectAtIndex:4] floatValue] R:[[inArray objectAtIndex:5] floatValue] G:[[inArray objectAtIndex:6] floatValue] B:[[inArray objectAtIndex:7] floatValue] A:[[inArray objectAtIndex:8] floatValue]];
    }
    else if([inArray count]==3 && [[inArray objectAtIndex:0] isEqualToString:@"lineto"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self lineToX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue]  ];
    }
    else if([inArray count]==7 && [[inArray objectAtIndex:0] isEqualToString:@"lineto"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self lineToX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue] R:[[inArray objectAtIndex:3] floatValue] G:[[inArray objectAtIndex:4] floatValue] B:[[inArray objectAtIndex:5] floatValue] A:[[inArray objectAtIndex:6] floatValue] ];
    }
    else if([inArray count]==3 && [[inArray objectAtIndex:0] isEqualToString:@"moveto"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self moveToX:[[inArray objectAtIndex:1] floatValue] Y:[[inArray objectAtIndex:2] floatValue]  ];
    }
    else if([inArray count]==2 && [[inArray objectAtIndex:0] isEqualToString:@"penwidth"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        [self setPenWidth: [[inArray objectAtIndex:1] floatValue]  ];
    }
    else if ([inArray count]==1 && [[inArray objectAtIndex:0] isEqualToString:@"clear"]){
        [self clear];
    }
}



//

-(void)drawSquare{
    
    CGContextSetRGBStrokeColor(_cacheContext, 1.0, 1.0, 1.0, 0.5);
	// And drawing with a blue fill color
	CGContextSetRGBFillColor(_cacheContext, 0.0, 0.0, 1.0, 1.0);
	// Draw them with a 2.0 stroke width so they are a bit more visible.
	CGContextSetLineWidth(_cacheContext, 2.0);
	CGRect newRect = CGRectMake(arc4random() % 200, arc4random() % 200, 60.0, 60.0);
    CGContextStrokeRect(_cacheContext, newRect);
   
    //[self setNeedsDisplay];
    [self setNeedsDisplayInRect:newRect];
}


// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    
    // Drawing code
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGImageRef cacheImage = CGBitmapContextCreateImage(_cacheContext);
    CGContextDrawImage(context, self.bounds, cacheImage);
    CGImageRelease(cacheImage);
    
    NSLog(@"===draw %p", context);
    
  /*  // Drawing with a white stroke color
	CGContextSetRGBStrokeColor(context, 1.0, 1.0, 1.0, 1.0);
	// And drawing with a blue fill color
	CGContextSetRGBFillColor(context, 0.0, 0.0, 1.0, 1.0);
	// Draw them with a 2.0 stroke width so they are a bit more visible.
	CGContextSetLineWidth(context, 2.0);
	
    CGContextStrokeRect(context, CGRectMake(arc4random() % 200, arc4random() % 200, 60.0, 60.0));
	*/
    /*
    // Add Rect to the current path, then stroke it
	CGContextAddRect(context, CGRectMake(30.0, 30.0, 60.0, 60.0));
	CGContextStrokePath(context);
	
	// Stroke Rect convenience that is equivalent to above
	CGContextStrokeRect(context, CGRectMake(30.0, 120.0, 60.0, 60.0));
	
    
    // Create filled rectangles via two different paths.
	// Add/Fill path
	CGContextAddRect(context, CGRectMake(210.0, 30.0, 60.0, 60.0));
	CGContextFillPath(context);
	// Fill convienience.
	CGContextFillRect(context, CGRectMake(210.0, 120.0, 60.0, 60.0));
    */
    /*
    // Drawing lines with a white stroke color
	CGContextSetRGBStrokeColor(context, 1.0, 1.0, 1.0, 1.0);
	
	// Preserve the current drawing state
	CGContextSaveGState(context);
	
	// Setup the horizontal line to demostrate caps
	CGContextMoveToPoint(context, 40.0, 30.0);
	CGContextAddLineToPoint(context, 280.0, 30.0);
    
	// Set the line width & cap for the cap demo
	CGContextSetLineWidth(context, self.width);
	//CGContextSetLineCap(context, self.cap);
	CGContextStrokePath(context);
	
	// Restore the previous drawing state, and save it again.
	CGContextRestoreGState(context);
	CGContextSaveGState(context);
	
	// Setup the angled line to demonstrate joins
	CGContextMoveToPoint(context, 40.0, 190.0);
	CGContextAddLineToPoint(context, 160.0, 70.0);
	CGContextAddLineToPoint(context, 280.0, 190.0);
    
	// Set the line width & join for the join demo
	CGContextSetLineWidth(context, self.width);
	//CGContextSetLineJoin(context, self.join);
	CGContextStrokePath(context);
    
	// Restore the previous drawing state.
	CGContextRestoreGState(context);
    
	// If the stroke width is large enough, display the path that generated these lines
	if (self.width >= 4.0) // arbitrarily only show when the line is at least twice as wide as our target stroke
	{
		CGContextSetRGBStrokeColor(context, 1.0, 0.0, 0.0, 1.0);
		CGContextMoveToPoint(context, 40.0, 30.0);
		CGContextAddLineToPoint(context, 280.0, 30.0);
		CGContextMoveToPoint(context, 40.0, 190.0);
		CGContextAddLineToPoint(context, 160.0, 70.0);
		CGContextAddLineToPoint(context, 280.0, 190.0);
		CGContextSetLineWidth(context, 2.0);
		CGContextStrokePath(context);
	}
    */
    
    /*CGContextSetRGBFillColor (context, 1, 0, 0, 1);// 3
    CGContextFillRect (context, CGRectMake (0, 0, 200, 100 ));// 4
    CGContextSetRGBFillColor (context, 0, 0, 1, .5);// 5
    CGContextFillRect (context, CGRectMake (0, 0, 100, 200));
     */
}


@end
