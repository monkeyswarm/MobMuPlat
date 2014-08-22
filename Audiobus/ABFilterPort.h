//
//  ABFilterPort.h
//  Audiobus
//
//  Created by Michael Tyson on 04/05/2012.
//  Copyright (c) 2011-2014 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "ABPort.h"
#import "ABCommon.h"

/*!
 * Filter port connections changed
 *
 *  Sent when the port's connections have changed, caused by connections
 *  or disconnections from within the Audiobus app.
 */
extern NSString * const ABFilterPortConnectionsChangedNotification;

/*!
 * Audio processing block
 *
 *  This block is called when there is audio to be processed.
 *  Your app should modify the audio in the `audio' parameter.
 *
 * @param audio Audio to be filtered, in the client format you specified
 * @param frames Number of frames of audio
 * @param timestamp The timestamp of the audio
 */
typedef void (^ABAudioFilterBlock)(AudioBufferList *audio, UInt32 frames, AudioTimeStamp *timestamp);
    
/*!
 * Filter port
 *
 *  This class is used to filter audio.
 *
 *  See the integration guide's section on using the [Filter Port](@ref Create-Filter-Port)
 *  for discussion.
 */
@interface ABFilterPort : ABPort

/*!
 * Initialize
 *
 *  Initializes a new filter port, with a block to use for filtering
 *
 * @param name The name of the filter port, for internal use
 * @param title Title of port, show to the user
 * @param description The AudioComponentDescription that identifiers this port. 
 *          This must match the entry in the AudioComponents dictionary of your Info.plist, and must be
 *          of type kAudioUnitType_RemoteEffect or kAudioUnitType_RemoteMusicEffect.
 * @param processBlock A block to use to process audio as it arrives at the filter port
 * @param processBlockSize Specify the number of frames you want to process each time
 *          the filter block is called. Audiobus will automatically queue up frames until this
 *          number is reached, then call the audio block for this number of frames. Set to
 *          0 if this value is unimportant, and Audiobus will use whatever number of frames results
 *          in the least latency.
 */
- (id)initWithName:(NSString *)name title:(NSString*)title audioComponentDescription:(AudioComponentDescription)description processBlock:(ABAudioFilterBlock)processBlock processBlockSize:(UInt32)processBlockSize;

/*!
 * Initialize
 *
 *  Initializes a new filter port, with an audio unit for filtering
 *
 *  Note: The audio unit you pass here must be an output unit (kAudioUnitSubType_RemoteIO). If you wish
 *  to use a different kind of audio unit, you'll need to use the
 *  @link initWithName:title:audioComponentDescription:processBlock:processBlockSize: process block initialiser @endlink 
 *  and call AudioUnitRender on the audio unit yourself from within the process block, while feeding its input via a
 *  callback that passes in the audio given to you in the process block.
 *
 * @param name The name of the filter port, for internal use
 * @param title Title of port, show to the user
 * @param description The AudioComponentDescription that identifiers this port.
 *          This must match the entry in the AudioComponents dictionary of your Info.plist, and must be
 *          of type kAudioUnitType_RemoteEffect or kAudioUnitType_RemoteMusicEffect.
 * @param audioUnit The output audio unit to use for processing. The audio unit's input will be replaced with the audio to process.
 */
- (id)initWithName:(NSString *)name title:(NSString*)title audioComponentDescription:(AudioComponentDescription)description audioUnit:(AudioUnit)audioUnit;

/*!
 * Determine if the filter port is currently connected
 *
 *  If you are using the @link initWithName:title:audioComponentDescription:processBlock:processBlockSize: processBlock @endlink
 *  initializer, then you must mute your app's main audio output when this function returns YES.
 *
 *  This function is suitable for use from within a realtime Core Audio context.
 *
 * @param filterPort        The filter port.
 * @return YES the filter port is currently connected; NO otherwise.
 */
BOOL ABFilterPortIsConnected(ABFilterPort *filterPort);

/*!
 * Audio unit
 *
 *  The audio unit to use for processing. If you uninitialize the audio unit passed
 *  to this class's initializer, be sure to set this property to NULL immediately beforehand.
 *
 *  If you did not provide an audio unit when initializing the port, this property
 *  will allow you to gain access to the internal audio unit used for audio transport, for
 *  the purposes of custom Inter-App Audio interactions such as transport control or MIDI exchange.
 */
@property (nonatomic, assign) AudioUnit audioUnit;

/*!
 * Client format
 *
 *  If you're using a process block for processing, use this to specify what audio 
 *  format to use. Audio will be automatically converted to and from the Audiobus line format.
 *
 *  The default value is non-interleaved stereo floating-point PCM.
 */
@property (nonatomic, assign) AudioStreamBasicDescription clientFormat;

/*!
 * Currently-connected sources
 *
 *  This is an array of @link ABPort ABPorts @endlink.
 */
@property (nonatomic, strong, readonly) NSArray *sources;

/*!
 * Currently-connected destinations
 *
 *  This is an array of @link ABPort ABPorts @endlink.
 */
@property (nonatomic, strong, readonly) NSArray *destinations;

/*!
 * Whether the port is connected
 */
@property (nonatomic, readonly) BOOL connected;

/*!
 * The AudioComponentDescription, of type kAudioUnitType_RemoteEffect or kAudioUnitType_RemoteMusicEffect,
 * which identifies this port's published audio unit
 */
@property (nonatomic, readonly) AudioComponentDescription audioComponentDescription;

/*!
 * The title of the port, for display to the user
 */
@property (nonatomic, strong, readwrite) NSString *title;

/*!
 * The port icon (a 32x32 image)
 *
 *  This is optional if your app only has one filter port, but if your app
 *  defines multiple filter ports, it is highly recommended that you provide icons
 *  for each, for easy identification by the user.
 */
@property (nonatomic, strong, readwrite) UIImage *icon;

/*!
 * The constant latency of this filter, in frames
 *
 *  If your filter code adds a constant amount of latency to the audio stream
 *  (such as an FFT or lookahead operation), you should specify that here in order
 *  to have Audiobus automatically account for it.
 *
 *  This is important when users have the same input signal going through different
 *  paths, so that Audiobus can synchronize these properly at the output. If you don't
 *  specify the correct latency, the user will hear phasing due to incorrectly aligned
 *  signals at the output.
 *
 *  Default: 0
 */
@property (nonatomic, assign) UInt32 latency;

/*!
 * Whether the port is bypassed
 *
 *  This property value is automatically managed by an in-built trigger that appears
 *  to users from within the Connection Panel. If your filter app also provides its own
 *  bypass controls, you can use this property to keep the state of the built-in bypass
 *  feature and your app's own bypass feature synchronized.
 */
@property (nonatomic, assign) BOOL bypassed;

@end

#ifdef __cplusplus
}
#endif
