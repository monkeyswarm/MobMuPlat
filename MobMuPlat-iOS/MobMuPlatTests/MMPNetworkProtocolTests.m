//
//  MMPNetworkProtocolTests.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 8/14/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import <XCTest/XCTest.h>

#import "MMPViewController.h"

@interface MMPNetworkProtocolTests : XCTestCase

@end

@implementation MMPNetworkProtocolTests

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testExample {
    // This is an example of a functional test case.
    // Use XCTAssert and related functions to verify your tests produce the correct results.
  //UIViewController *vc = [[[UIApplication sharedApplication] keyWindow] rootViewController];
  MMPViewController *vc = [[MMPViewController alloc] init];
  vc->pacm.enabled = YES;
  BOOL enabled = vc->pacm.enabled;
  NSLog(@""); //TODO test state.
}

- (void)testPerformanceExample {
    // This is an example of a performance test case.
    [self measureBlock:^{
        // Put the code you want to measure the time of here.
    }];
}

@end
