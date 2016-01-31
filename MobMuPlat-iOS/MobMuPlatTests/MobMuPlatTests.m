//
//  MobMuPlatTests.m
//  MobMuPlatTests
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import <XCTest/XCTest.h>

#import "MMPViewController.h"
//#import "SettingsViewController.h"
//#import "Gui.h"
#define fequal(a,b) (fabs((a) - (b)) < FLT_EPSILON)

@interface MobMuPlatTests : XCTestCase //<PdReceiverDelegate>

@end


@interface MMPTestDelegate : NSObject<PdReceiverDelegate>
- (instancetype)initWithExpectationMap:(NSDictionary<NSString *,XCTestExpectation *> *)expectationMap;
@end

@implementation MobMuPlatTests {
  //NSUInteger _receiveFloatCount;
  //XCTestExpectation *_expectation;
  //NSMutableArray<XCTestExpectation *> *_expectations;
  //NSString *_currTestName;
}

- (void)setUp {
    [super setUp];
  //_expectations = [NSMutableArray array];
  //_receiveFloatCount = 0;
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testExample {
    // This is an example of a functional test case.
    // Use XCTAssert and related functions to verify your tests produce the correct results.
}

- (void)testPerformanceExample {
    // This is an example of a performance test case.
    [self measureBlock:^{
        // Put the code you want to measure the time of here.
    }];
}

/*- (void)testFloatShim {
  _currTestName = @"testFloatShim";
  [_expectations addObject:[self expectationWithDescription:@"testFloatShim1"]];
  [_expectations addObject:[self expectationWithDescription:@"testFloatShim2"]];

  MMPViewController *vc = [[MMPViewController alloc] init];
  BOOL loaded = [vc loadScenePatchOnlyFromBundle:[NSBundle bundleForClass:[self class]]
                                        filename:@"ShimTest1.pd"];
 [PdBase setDelegate:self pollingEnabled:NO];

  [PdBase subscribe:@"floatatomExternalSend"];
  [PdBase subscribe:@"0-gui-rec"];

  [PdBase sendFloat:5 toReceiver:@"floatatomExternalReceive"];
  [PdBase sendFloat:6 toReceiver:@"0-gui-send"];
  //[PdBase receiveMessages];

  [PdBase sendMessage:@"set" withArguments:@[@(4)] toReceiver:@"floatatomExternalReceive"];
  [PdBase sendMessage:@"set" withArguments:@[@(4)] toReceiver:@"0-gui-send"];

  [PdBase receiveMessages];



  [self waitForExpectationsWithTimeout:1.0 handler:^(NSError *error) {

  }];
}

//
- (void)receiveBangFromSource:(NSString *)source {

}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  XCTAssertTrue(fequal(received, 5));
  XCTAssertEqualObjects(source, @"floatatomExternalSend");
  //[_expectations[0] fulfill];
}
- (void)receiveSymbol:(NSString *)symbol fromSource:(NSString *)source {

}
- (void)receiveList:(NSArray *)list fromSource:(NSString *)source {

}
- (void)receiveMessage:(NSString *)message withArguments:(NSArray *)arguments fromSource:(NSString *)source {
  XCTAssertEqualObjects(source, @"0-gui-rec");
  XCTAssertEqualObjects(message, @"set");
  XCTAssertEqualObjects(arguments[0], @(5));
  //[_expectations[1] fulfill];
}

//

- (void)testLoadbang {
  [_expectations addObject:[self expectationWithDescription:@"testFloatShim1"]];
  [PdBase setDelegate:self pollingEnabled:NO];
  MMPViewController *vc = [[MMPViewController alloc] initWithAudioBusEnabled:NO];
  BOOL loaded = [vc loadScenePatchOnlyFromBundle:[NSBundle bundleForClass:[self class]]
                                        filename:@"LoadbangTest.pd"];

  [self waitForExpectationsWithTimeout:8.0 handler:^(NSError *error) {

  }];
}*/

- (void)test1 {
  // List of outgoing sends to test.
  NSArray *receiveHandles = @[ @"testInitToggleToNumber", @"testInitSliderToNumber" ];

  //NSMutableArray<XCTestExpectation *> *expectations = [NSMutableArray array];
  NSMutableDictionary<NSString *, XCTestExpectation *> *receiveHandleToExpectationMap = [NSMutableDictionary dictionary];
  MMPViewController *vc = [[MMPViewController alloc] initWithAudioBusEnabled:NO ];
  // undo vc behavior, reset pdbase delegate without polling here.
  [PdBase setDelegate:vc->_mmpPdDispatcher pollingEnabled:NO];

  //subscribe before loading
  for (NSString *receiveHandle in receiveHandles ){
    [PdBase subscribe:receiveHandle];
    receiveHandleToExpectationMap[receiveHandle] =
        [self expectationWithDescription:receiveHandle];
  }
  MMPTestDelegate *testDelegate = [[MMPTestDelegate alloc] initWithExpectationMap:receiveHandleToExpectationMap];

  BOOL loaded = [vc loadScenePatchOnlyFromBundle:[NSBundle bundleForClass:[self class]]
                                        filename:@"MMPPdGuiTest1.pd"];
  // add listener after loading (since dispatcher clears listeners on load)
  for (NSString *receiveHandle in receiveHandles ) {
    [vc->_mmpPdDispatcher addListener:testDelegate forSource:receiveHandle];
  }

  //[PdBase subscribe:@"0-gui-rec"];

  //[PdBase sendFloat:5 toReceiver:@"floatatomExternalReceive"];
  //[PdBase sendFloat:6 toReceiver:@"0-gui-send"];
  //[PdBase receiveMessages];

  //[PdBase sendMessage:@"set" withArguments:@[@(4)] toReceiver:@"floatatomExternalReceive"];
  //[PdBase sendMessage:@"set" withArguments:@[@(4)] toReceiver:@"0-gui-send"];

  [PdBase receiveMessages];



  [self waitForExpectationsWithTimeout:3.0 handler:^(NSError *error) {
    
  }];
}

@end

@implementation MMPTestDelegate {
  NSDictionary<NSString *,XCTestExpectation *> *_expectationMap;
}

- (instancetype)initWithExpectationMap:(NSDictionary<NSString *,XCTestExpectation *> *)expectationMap {
  self = [super init];
  if (self) {
    _expectationMap = expectationMap;
  }
  return self;
}

- (void)receiveBangFromSource:(NSString *)source {

}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  if (received > 0 && [source isEqualToString:@"testInitToggleToNumber"]) {
    [_expectationMap[source] fulfill];
  }
  if (received > 0 && [source isEqualToString:@"testInitSliderToNumber"]) {
    [_expectationMap[source] fulfill];
  }
}
- (void)receiveSymbol:(NSString *)symbol fromSource:(NSString *)source {

}
- (void)receiveList:(NSArray *)list fromSource:(NSString *)source {

}
- (void)receiveMessage:(NSString *)message withArguments:(NSArray *)arguments fromSource:(NSString *)source {

}

@end
