//
//  ABAudiobusAudioUnitWrapper.h
//  Audiobus
//
//  Created by Michael Tyson on 23/01/2012.
//  Copyright (c) 2012 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <Foundation/Foundation.h>
#import <AudioUnit/AudioUnit.h>
#import "ABInputPort.h"
#import "ABOutputPort.h"
#import "ABFilterPort.h"

@class ABAudiobusController;

enum {
    /*!
     * Audiobus flag
     *
     *  This flag is present in the audio unit input callback ioActionFlags parameter if the audio came from Audiobus
     */
    kAudioUnitRenderAction_IsFromAudiobus = 1 << 12
};

/*!
 * Audiobus audio unit wrapper
 *
 *  This is a utility class that wraps around a Remote IO audio unit and performs
 *  all receiving and sending tasks.  This is the easiest and fastest way to implement the Audiobus
 *  API.
 *
 *  See the [Integration Guide](@ref Integration-Guide) for usage instructions.
 */
@interface ABAudiobusAudioUnitWrapper : NSObject

/*!
 * Initialize
 *
 * @param audiobusController The Audiobus controller
 * @param audioUnit          Your app's Remote IO audio unit
 * @param output             The output port, or `nil` if you do not send audio
 * @param input              The input port, or `nil` if you do not receive audio
 */
- (id)initWithAudiobusController:(ABAudiobusController*)audiobusController audioUnit:(AudioUnit)audioUnit output:(ABOutputPort*)output input:(ABInputPort*)input;

/*!
 * Add a filter port
 *
 *  Filter ports that are added here will automatically have their audio output sent to
 *  the system audio output, muting the existing output if the port is connected.
 *
 *  If you add a filter port this way, you do not need to (and should not) use the
 *  [ABFilterPortGetOutput](@ref ABFilterPort::ABFilterPortGetOutput) function yourself.
 *
 * @param filterPort         Filter port to add
 */
- (void)addFilterPort:(ABFilterPort*)filterPort;

/*!
 * Remove a filter port
 *
 * @param filterPort         Filter port to remove
 */
- (void)removeFilterPort:(ABFilterPort*)filterPort;

/*!
 * Whether to use the lossy but low-latency audio stream, instead of the lossless stream
 *
 *  If you are playing received audio out loud, and low latency is a higher priority than
 *  perfect, lossless audio, set this property to YES.
 *
 *  See the [discussion on lossless vs. low-latency audio](@ref Lossless-vs-Low-Latency) for more information.
 */
@property (nonatomic, assign) BOOL useLowLatencyInputStream;

/*!
 * The audio unit
 *
 *  You may set this property at any time: The wrapper will reconfigure to use the new
 *  audio unit.
 */
@property (nonatomic, assign) AudioUnit audioUnit;

@end

#ifdef __cplusplus
}
#endif