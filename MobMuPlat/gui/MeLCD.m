//
//  MeLCD.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/8/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeLCD.h"

@implementation MeLCD

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        self.width = 5;
        
        NSTimer* timer = [NSTimer scheduledTimerWithTimeInterval:1. target:self selector:@selector(drawSquare) userInfo:nil repeats:YES];
        
    }
    return self;
}

-(void)drawSquare{
    [self setNeedsDisplay];
}

// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    
    // Drawing code
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    NSLog(@"===draw %p", context);
    
    // Drawing with a white stroke color
	CGContextSetRGBStrokeColor(context, 1.0, 1.0, 1.0, 1.0);
	// And drawing with a blue fill color
	CGContextSetRGBFillColor(context, 0.0, 0.0, 1.0, 1.0);
	// Draw them with a 2.0 stroke width so they are a bit more visible.
	CGContextSetLineWidth(context, 2.0);
	
    CGContextStrokeRect(context, CGRectMake(arc4random() % 200, arc4random() % 200, 60.0, 60.0));
	
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
}


@end
