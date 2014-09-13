//
//  ABSenderPort.h
//  Audiobus
//
//  Created by Michael Tyson on 25/11/2011.
//  Copyright (c) 2011-2014 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "ABCommon.h"
#import "ABPort.h"
    
/*!
 * Sender port connections changed
 *
 *  Sent when the port's connections have changed, caused by connections
 *  or disconnections from within the Audiobus app.
 */
extern NSString * const ABSenderPortConnectionsChangedNotification;

/*!
 * Sender port
 *
 *  This class is used to transmit audio.
 *
 *  See the integration guide on using the [Sender Port](@ref Create-Sender-Port)
 *  for discussion.
 */
@interface ABSenderPort : ABPort

/*!
 * Initialize
 *
 *  Initializes a new sender port. Use @link ABSenderPortSend @endlink to send audio.
 *
 *  On iOS 7, note that unlike the @link initWithName:title:audioComponentDescription:audioUnit: @endlink
 *  initializer, audio sent via sender ports initialized with this version will incur a
 *  small latency penalty equal to the current hardware buffer duration (e.g. 5 ms) due to necessary
 *  buffering. Initialize with an audio unit to avoid this.
 *
 * @param name Name of port, for internal use
 * @param title Title of port, show to the user
 * @param description The AudioComponentDescription that identifiers this port.
 *          This must match the entry in the AudioComponents dictionary of your Info.plist, and must be
 *          of type kAudioUnitType_RemoteGenerator.
 */
- (id)initWithName:(NSString *)name title:(NSString*)title audioComponentDescription:(AudioComponentDescription)description;

/*!
 * Initialize, with an audio unit
 *
 *  Initializes a new sender port, with an audio unit to be used for generating audio.
 *
 *  Note: The audio unit you pass here must be an output unit (kAudioUnitSubType_RemoteIO). If you wish
 *  to use a different kind of audio unit, you'll need to use the 
 *  @link initWithName:title:audioComponentDescription: non-AudioUnit initialiser @endlink and call
 *  @link ABSenderPortSend @endlink with the output from that audio unit.
 *
 * @param name Name of port, for internal use
 * @param title Title of port, show to the user
 * @param description The AudioComponentDescription that identifiers this port.
 *          This must match the entry in the AudioComponents dictionary of your Info.plist, and must be
 *          of type kAudioUnitType_RemoteGenerator.
 * @param audioUnit The output audio unit to use for sending audio. The audio unit's output will be transmitted.
 */
- (id)initWithName:(NSString *)name title:(NSString*)title audioComponentDescription:(AudioComponentDescription)description audioUnit:(AudioUnit)audioUnit;

/*!
 * Send audio
 *
 *  This C function is used to send audio. It's suitable for use within a realtime thread, as it does not hold locks,
 *  allocate memory or call Objective-C methods.  You should keep a local pointer to the ABSenderPort instance, to be
 *  passed as the first parameter.
 *
 *  Note: If you provided an audio unit when you initialized this class, you cannot use this function.
 *
 * @param senderPort        Sender port.
 * @param audio             Audio buffer list to send, in the @link clientFormat client format @endlink.
 * @param lengthInFrames    Length of the audio, in frames.
 * @param timestamp         The timestamp of the audio.
 */
void ABSenderPortSend(ABSenderPort* senderPort, const AudioBufferList *audio, UInt32 lengthInFrames, const AudioTimeStamp *timestamp);

/*!
 * Determine if the sender port is currently connected to any destinations
 *
 *  This function is suitable for use from within a realtime Core Audio context.
 *
 * @param senderPort        Sender port.
 * @return YES if there are currently destinations connected; NO otherwise.
 */
BOOL ABSenderPortIsConnected(ABSenderPort* senderPort);

/*
 * Whether the port is connected to another port from the same app
 *
 *  This returns YES when the sender port is connected to a receiver port also belonging to your app.
 *
 *  If your app supports connections to self (ABAudiobusController's
 *  @link ABAudiobusController::allowsConnectionsToSelf allowsConnectionsToSelf @endlink
 *  is set to YES), then you should take care to avoid feedback issues when the app's input is being fed from
 *  its own output.
 *
 *  Primarily, this means not sending output derived from the input through the sender port.
 *
 *  You can use @link ABSenderPortIsConnectedToSelf @endlink and the equivalent ABReceiverPort function,
 *  @link ABReceiverPortIsConnectedToSelf @endlink to determine this state from the Core Audio realtime
 *  thread, and perform muting/etc as appropriate.
 *
 * @param senderPort        Sender port.
 * @return YES if one of this port's destinations belongs to this app
 */
BOOL ABSenderPortIsConnectedToSelf(ABSenderPort* senderPort);

/*!
 * Determine whether output should be muted
 *
 *  This C function allows you to determine whether your output should be muted. You only need to use this
 *  function when you *did not* pass an audio unit when initializing the port.
 *
 *  If the return value of
 *  this function is YES, you must silence your app's corresponding audio output to avoid doubling up
 *  the audio (which is being output at the other end), and to enable your app to go silent when disconnected
 *  from Audiobus.  You can do this by zeroing your buffers using memset, and/or setting the 
 *  `kAudioUnitRenderAction_OutputIsSilence` flag on the ioActionFlags variable in a render callback.
 *
 *  Note that this muting is handled for you automatically if you are using an audio unit with the port.
 *
 *  The @link muted @endlink property provides a key-value observable version of this method, which should
 *  only be used outside of the Core Audio realtime thread.
 *
 * @param senderPort        Sender port.
 * @return Whether the output should be muted
 */
BOOL ABSenderPortIsMuted(ABSenderPort *senderPort);

/*!
 * Get average latency
 *
 *  This C function returns the average transmission latency across all connected remote ports. Where appropriate, use
 *  it to offset generated audio to compensate.
 *
 * @param senderPort        Sender port.
 * @return Latency, in seconds.
 */
NSTimeInterval ABSenderPortGetAverageLatency(ABSenderPort *senderPort);

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
 * Whether the port is muted
 *
 *  See discussion for @link ABSenderPortIsMuted @endlink for details.
 *
 *  This property is observable.
 */
@property (nonatomic, readonly) BOOL muted;

/*!
 * Client format
 *
 *  Use this to specify what audio format your app uses. Audio will be automatically
 *  converted to the Audiobus line format.
 *
 *  Note: If you provided an audio unit when you initialized this class, you cannot use this property.
 *
 *  The default value is non-interleaved stereo floating-point PCM.
 */
@property (nonatomic, assign) AudioStreamBasicDescription clientFormat;

/*!
 * Whether the port's audio is derived from a live audio source
 *
 *  If this sender port's audio comes from the system audio input (such as a microphone),
 *  then you should set this property to YES to allow apps downstream to react accordingly.
 *  For example, an app that provides audio monitoring might want to disable monitoring by
 *  default when connected to a live audio source in order to prevent feedback.
 */
@property (nonatomic, assign) BOOL derivedFromLiveAudioSource;

/*!
 * Audio unit
 *
 *  The output audio unit to use for sending audio. The audio unit's output will be transmitted.
 *  If you uninitialize the audio unit passed to this class's initializer, be sure to set this
 *  property to NULL immediately beforehand.
 *
 *  If you did not provide an audio unit when initializing the port, this property will allow 
 *  you to gain access to the internal audio unit used for audio transport, for the purposes of 
 *  custom Inter-App Audio interactions such as transport control or MIDI exchange.
 */
@property (nonatomic, assign) AudioUnit audioUnit;

/*!
 * The AudioComponentDescription, of type kAudioUnitType_RemoteGenerator, which identifies this
 * port's published audio unit
 */
@property (nonatomic, readonly) AudioComponentDescription audioComponentDescription;

/*!
 * Whether the port is connected to another port from the same app
 *
 *  This is a key-value-observable property equivalent of ABSenderPortIsConnectedToSelf. See
 *  the documentation for ABSenderPortIsConnectedToSelf for details.
 */
@property (nonatomic, readonly) BOOL connectedToSelf;

/*!
 * The constant latency of this sender, in frames
 *
 *  If your audio generation code adds a constant amount of latency to the audio stream
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
 * The title of the port, for display to the user.
 */
@property (nonatomic, strong, readwrite) NSString *title;

/*!
 * The port icon (a 32x32 image)
 *
 *  This is optional if your app only has one sender port, but if your app
 *  defines multiple sender ports, it is highly recommended that you provide icons
 *  for each, for easy identification by the user.
 */
@property (nonatomic, strong, readwrite) UIImage *icon;

@end

#ifdef __cplusplus
}
#endif