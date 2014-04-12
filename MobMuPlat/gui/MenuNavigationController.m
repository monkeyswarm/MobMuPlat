//
//  MenuNavigationController.m
//  MobMuPlat
//
//  Created by diglesia on 4/11/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MenuNavigationController.h"

@interface MenuNavigationController ()

@end

@implementation MenuNavigationController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
      _orientation = UIInterfaceOrientationPortrait;
    }
    return self;
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation
{
  return _orientation;
}


- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
