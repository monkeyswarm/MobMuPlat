//
//  MobMuPlatPdAudioUnit.h
//  MobMuPlat
//
//  Created by diglesia on 1/19/15.
//  Copyright (c) 2015 Daniel Iglesia. All rights reserved.
//

#import "PdAudioUnit.h"

#import "Audiobus.h"

@interface MobMuPlatPdAudioUnit : PdAudioUnit

@property (nonatomic, assign)ABReceiverPort *inputPort;

@end
