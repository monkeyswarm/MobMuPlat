//
//  MobMuPlatPdAudioUnit.m
//  MobMuPlat
//
//  Created by diglesia on 1/19/15.
//  Copyright (c) 2015 Daniel Iglesia. All rights reserved.
//

#import "MobMuPlatPdAudioUnit.h"

#import "AudioHelpers.h"
#import "PdBase.h"

static const AudioUnitElement kInputElement = 1;
static const AudioUnitElement kOutputElement = 0;

@implementation MobMuPlatPdAudioUnit {
  BOOL _inputEnabled;
  int _blockSizeAsLog;
}

static OSStatus AudioRenderCallback(void *inRefCon,
                                    AudioUnitRenderActionFlags *ioActionFlags,
                                    const AudioTimeStamp *inTimeStamp,
                                    UInt32 inBusNumber,
                                    UInt32 inNumberFrames,
                                    AudioBufferList *ioData) {

  MobMuPlatPdAudioUnit *mmppdAudioUnit = (__bridge MobMuPlatPdAudioUnit *)inRefCon;
  // Original logic.
  /*Float32 *auBuffer = (Float32 *)ioData->mBuffers[0].mData;
  if (mmppdAudioUnit->_inputEnabled) {
    AudioUnitRender(mmppdAudioUnit.audioUnit, ioActionFlags, inTimeStamp, kInputElement, inNumberFrames, ioData);
  }
  int ticks = inNumberFrames >> mmppdAudioUnit->_blockSizeAsLog; // this is a faster way of computing (inNumberFrames / blockSize)
  [PdBase processFloatWithInputBuffer:auBuffer outputBuffer:auBuffer ticks:ticks];
  return noErr;*/

  AudioTimeStamp timestamp = *inTimeStamp;
  if ( ABReceiverPortIsConnected(mmppdAudioUnit->_inputPort) ) {
    // Receive audio from Audiobus, if connected. Note that we also fetch the timestamp here, which is
    // useful for latency compensation, where appropriate.
    ABReceiverPortReceive(mmppdAudioUnit->_inputPort, nil, ioData, inNumberFrames, &timestamp);
  } else {
    // Receive audio from system input otherwise
    if (mmppdAudioUnit->_inputEnabled) {
      AudioUnitRender(mmppdAudioUnit.audioUnit, ioActionFlags, inTimeStamp, kInputElement, inNumberFrames, ioData);
    }
  }

  int ticks = inNumberFrames >> mmppdAudioUnit->_blockSizeAsLog; // this is a faster way of computing (inNumberFrames / blockSize)

  Float32 *auBuffer = (Float32 *)ioData->mBuffers[0].mData;
  [PdBase processFloatWithInputBuffer:auBuffer outputBuffer:auBuffer ticks:ticks];

  return noErr;
}


- (AURenderCallback)renderCallback {
  return AudioRenderCallback;
}

- (int)configureWithSampleRate:(Float64)sampleRate numberChannels:(int)numChannels inputEnabled:(BOOL)inputEnabled {
  _blockSizeAsLog = log2int([PdBase getBlockSize]);
  _inputEnabled = inputEnabled;
  return [super configureWithSampleRate:sampleRate numberChannels:numChannels inputEnabled:inputEnabled];
}

@end
