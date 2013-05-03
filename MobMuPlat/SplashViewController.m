//
//  SplashViewController.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 2/23/13.
//  Copyright (c) 2013 Daniel Iglesia. All rights reserved.
//

#import "SplashViewController.h"


@implementation SplashViewController

-(void)launchSplash{//launch screen
    UIImageView* titleRing, *titleText, *titleCross, *titleResistor;
	
    self.view.backgroundColor=[UIColor blackColor];
    
	if(UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad){
		titleRing=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_ring_185x110"]];
		titleRing.frame=CGRectMake(292, -110, 185, 110);
		[self.view addSubview:titleRing];
		titleText=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_text_142x46"]];
		titleText.frame=CGRectMake(313, 1024, 142, 46);
		[self.view addSubview:titleText];
		titleCross=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_cross_50x50"]];
		titleCross.frame=CGRectMake(-50, 487, 50, 50);
		[self.view addSubview:titleCross];
		titleResistor=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_resistor_21x70"]];
		titleResistor.frame=CGRectMake(768, 477, 21, 70);
		[self.view addSubview:titleResistor];
		
		
		[UIView beginAnimations:nil context:nil];
		[UIView setAnimationDuration:2.0];
		[UIView setAnimationCurve:UIViewAnimationCurveEaseOut];
		[UIView setAnimationDelegate:self];
		[UIView setAnimationDidStopSelector:@selector(startupAnimationDone:finished:context:)];
		
		 titleRing.frame=CGRectMake(292, 457, 185, 110);
		titleText.frame=CGRectMake( 313 ,572,142,46);
		titleCross.frame=CGRectMake(292+33,487,50,50);
		titleResistor.frame=CGRectMake(292+115,477,21,71);
		[UIView commitAnimations];
	}
	
	else{
		
		titleRing=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_ring_110x67"]];
		titleRing.frame=CGRectMake(105, -67, 110, 67);
		[self.view addSubview:titleRing];
		titleText=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_text_71x27"]];
		titleText.frame=CGRectMake(125 ,480,71,27);
		[self.view addSubview:titleText];
		titleCross=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_cross_30x30"]];
		titleCross.frame=CGRectMake(-30,225,30,30);
		[self.view addSubview:titleCross];
		titleResistor=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_resistor_13x40"]];
		titleResistor.frame=CGRectMake(320,220,13,40);
		[self.view addSubview:titleResistor];
		
		[UIView beginAnimations:nil context:nil];
		[UIView setAnimationDuration:2.0];
		[UIView setAnimationCurve:UIViewAnimationCurveEaseOut];
		[UIView setAnimationDelegate:self];
		[UIView setAnimationDidStopSelector:@selector(startupAnimationDone:finished:context:)];
		
        titleRing.frame=CGRectMake(105, 206, 110, 67);
		titleText.frame=CGRectMake( 125 ,276,71,27);
		titleCross.frame=CGRectMake(105+21,225,30,30);
		titleResistor.frame=CGRectMake(105+67,220,13,40);
		[UIView commitAnimations];
		
	}
	
}

- (void)startupAnimationDone:(NSString *)animationID finished:(NSNumber *)finished context:(void *)context {
    [NSTimer scheduledTimerWithTimeInterval:1 target:self.delegate selector:@selector(dismissSplash) userInfo:nil repeats:NO];
	
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
