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

  CGFloat centerX = self.view.frame.size.width / 2;
  CGFloat centerY = self.view.frame.size.height / 2;

  if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
    titleRing=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_ring_185x110"]];
    titleText=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_text_142x46"]];
    titleCross=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_cross_50x50"]];
    titleResistor=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"titlepad_resistor_21x70"]];
  } else {
    titleRing=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_ring_110x67"]];
    titleText=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_text_71x27"]];
    titleCross=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_cross_30x30"]];
    titleResistor=[[UIImageView alloc]initWithImage:[UIImage imageNamed:@"title_resistor_13x40"]];
  }

  [self.view addSubview:titleRing];
  [self.view addSubview:titleText];
  [self.view addSubview:titleCross];
  [self.view addSubview:titleResistor];

  titleRing.center = CGPointMake(centerX, -titleRing.frame.size.height/2); //from top
  titleText.center = CGPointMake(centerX, self.view.frame.size.height+titleText.frame.size.height/2); //from bottom
  titleCross.center = CGPointMake(-titleCross.frame.size.width/2, centerY); //from left
  titleResistor.center = CGPointMake(self.view.frame.size.width+titleResistor.frame.size.width/2, centerY); //from right

  [UIView beginAnimations:nil context:nil];
  [UIView setAnimationDuration:2.0];
  [UIView setAnimationCurve:UIViewAnimationCurveEaseOut];
  [UIView setAnimationDelegate:self];
  [UIView setAnimationDidStopSelector:@selector(startupAnimationDone:finished:context:)];

  titleRing.center = CGPointMake(centerX,centerY);

  titleText.center = CGPointMake(centerX,centerY);//titleRing.frame.origin.y+titleRing.frame.size.height+2);
  CGRect frame = titleText.frame; //nudge below ring
  frame.origin.y = titleRing.frame.origin.y + titleRing.frame.size.height + 10;
  titleText.frame = frame;

  CGFloat offsetWithinRing = titleRing.frame.size.width * .15;
  titleCross.center = CGPointMake(centerX-offsetWithinRing,centerY);
  titleResistor.center = CGPointMake(centerX+offsetWithinRing,centerY);

  [UIView commitAnimations];
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
