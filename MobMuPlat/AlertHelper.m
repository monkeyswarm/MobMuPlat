//
//  AlertHelper.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/18/13.
//  Copyright (c) 2013 Daniel Iglesia. All rights reserved.
//

#import "AlertHelper.h"

@implementation AlertHelper

+(void)showBindErrorHelper{
    //printf("\nhelper! ismain? %d", [NSThread isMainThread]);
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Cannot connect UDP Socket"
                          message: @"Another app is on this port: networking will not work. To use networking, completely quit that app and reload this document."
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
}

+(void)showBindError{//can be called from non-main thread
    [AlertHelper performSelectorOnMainThread:@selector(showBindErrorHelper) withObject:nil waitUntilDone:NO];
}

@end
