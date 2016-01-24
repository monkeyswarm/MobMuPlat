//
//  MMPPdSymbol.m
//  MobMuPlat
//
//  Created by diglesia on 1/18/16.
//  Copyright © 2016 Daniel Iglesia. All rights reserved.
//

#import "MMPPdSymbol.h"

@implementation MMPPdSymbol


#pragma mark WidgetListener

- (void)receiveBangFromSource:(NSString *)source {
  //[self sendSymbol:self.symbol];
}

- (void)receiveFloat:(float)received fromSource:(NSString *)source {
  self.symbol = @"float";
  //[self sendSymbol:self.symbol];
}

- (void)receiveSymbol:(NSString *)symbol fromSource:(NSString *)source {
  self.symbol = symbol;
  //[self sendSymbol:self.symbol];
}


@end
