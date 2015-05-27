//
//  MeLCD.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/8/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeLCD.h"

@interface MeLCD () {
    CGContextRef _cacheContext;
    float fR,fG,fB,fA;//FRGBA
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
        _cacheContext = CGBitmapContextCreate (nil, (int)frame.size.width, (int)frame.size.height, 8, 0, CGColorSpaceCreateDeviceRGB(),  kCGImageAlphaPremultipliedLast  );
        CGContextSetRGBFillColor(_cacheContext, 1., 0., 1., 1.);
        penPoint = CGPointMake(0, 0);
      [self setPenWidth:1];
      
    }
    return self;
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
    
    //NSLog(@"pen x %.2f y %.2f TO x %.2f y %.2f ", penPoint.x, penPoint.y, x, y);
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


-(void)framePolyRGBA:(NSArray*)pointArray R:(float)r G:(float)g B:(float)b A:(float)a {
    //points are normalized, NSNumber floats
    if([pointArray count]<4)return;
    
    CGContextSetRGBStrokeColor(_cacheContext, r,g,b,a);
    CGContextMoveToPoint(_cacheContext, [[pointArray objectAtIndex:0] floatValue]*self.frame.size.width, [[pointArray objectAtIndex:1] floatValue]*self.frame.size.height );
	
    for(int i = 2; i < [pointArray count]; i+=2)
	{
		CGContextAddLineToPoint(_cacheContext, [[pointArray objectAtIndex:i] floatValue]*self.frame.size.width, [[pointArray objectAtIndex:i+1] floatValue]*self.frame.size.height);
	}
	
	CGContextClosePath(_cacheContext);
    
    CGContextDrawPath(_cacheContext, kCGPathStroke);
    //todo bounding redraw rect
    [self setNeedsDisplay];
}

-(void)framePoly:(NSArray*)pointArray{
    [self framePolyRGBA:pointArray R:fR G:fG B:fB A:fA];
}

-(void)paintPolyRGBA:(NSArray*)pointArray R:(float)r G:(float)g B:(float)b A:(float)a {
    //points are normalized, NSNumber floats
    if([pointArray count]<4)return;
    
    CGContextSetRGBFillColor(_cacheContext, r,g,b,a);
    CGContextMoveToPoint(_cacheContext, [[pointArray objectAtIndex:0] floatValue]*self.frame.size.width, [[pointArray objectAtIndex:1] floatValue]*self.frame.size.height );
	
    for(int i = 2; i < [pointArray count]; i+=2)
	{
		CGContextAddLineToPoint(_cacheContext, [[pointArray objectAtIndex:i] floatValue]*self.frame.size.width, [[pointArray objectAtIndex:i+1] floatValue]*self.frame.size.height);
	}
	
	CGContextClosePath(_cacheContext);
    
    CGContextDrawPath(_cacheContext, kCGPathFill);
    //todo bounding redraw rect
    [self setNeedsDisplay];
}

-(void)paintPoly:(NSArray*)pointArray{
    [self paintPolyRGBA:pointArray R:fR G:fG B:fB A:fA];
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
  [super receiveList:inArray];
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
    else if([inArray count]%2==1 && [[inArray objectAtIndex:0] isEqualToString:@"framepoly"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        NSArray* pointArray = [inArray subarrayWithRange:NSMakeRange(1, [inArray count]-1) ];//strip off "framepoly"
        
        [self framePoly:pointArray];
    }
    else if([inArray count]>0 && [inArray count]%2==1 && [[inArray objectAtIndex:0] isEqualToString:@"framepolyRGBA"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        NSArray* pointArray = [inArray subarrayWithRange:NSMakeRange(1, [inArray count]-5) ];//strip off "framepolyRGBA"
        NSInteger RGBAStartIndex = [inArray count]-4;
        
        [self framePolyRGBA:pointArray R:[[inArray objectAtIndex:RGBAStartIndex] floatValue] G:[[inArray objectAtIndex:RGBAStartIndex+1] floatValue] B:[[inArray objectAtIndex:RGBAStartIndex+2] floatValue] A:[[inArray objectAtIndex:RGBAStartIndex+3] floatValue] ];
    }
    else if([inArray count]%2==1 && [[inArray objectAtIndex:0] isEqualToString:@"paintpoly"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        NSArray* pointArray = [inArray subarrayWithRange:NSMakeRange(1, [inArray count]-1) ];//strip off "paintpoly"
        
        [self paintPoly:pointArray];
    }
    else if([inArray count]>0 && [inArray count]%2==1 && [[inArray objectAtIndex:0] isEqualToString:@"paintpolyRGBA"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        NSArray* pointArray = [inArray subarrayWithRange:NSMakeRange(1, [inArray count]-5) ];//strip off "paintpolyRGBA"
        NSInteger RGBAStartIndex = [inArray count]-4;
        
        [self paintPolyRGBA:pointArray R:[[inArray objectAtIndex:RGBAStartIndex] floatValue] G:[[inArray objectAtIndex:RGBAStartIndex+1] floatValue] B:[[inArray objectAtIndex:RGBAStartIndex+2] floatValue] A:[[inArray objectAtIndex:RGBAStartIndex+3] floatValue] ];
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

- (void)drawRect:(CGRect)rect {

    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGImageRef cacheImage = CGBitmapContextCreateImage(_cacheContext);
    CGContextDrawImage(context, self.bounds, cacheImage);
    CGImageRelease(cacheImage);
    
}


@end
