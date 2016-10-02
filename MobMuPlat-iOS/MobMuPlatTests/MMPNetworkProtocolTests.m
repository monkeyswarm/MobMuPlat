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

- (void)testLLMEnabled {
  MMPViewController *vc = [[MMPViewController alloc] init];
  vc->llm.enabled = YES;
  BOOL enabled = vc->llm.enabled;
  XCTAssertTrue(enabled);
}

- (void)testPACMEnabled {
  MMPViewController *vc = [[MMPViewController alloc] init];
  vc->pacm.enabled = YES;
  BOOL enabled = vc->pacm.enabled;
  XCTAssertTrue(enabled);
}

- (void)testPerformanceExample {
    // This is an example of a performance test case.
    [self measureBlock:^{
        // Put the code you want to measure the time of here.
    }];
}

@end
