//
//  ABInputPort.h
//  Audiobus
//
//  Created by Michael Tyson on 03/03/2012.
//  Copyright (c) 2012 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "ABCommon.h"
#import "ABPort.h"

extern NSString * ABInputPortPortAddedNotification;
extern NSString * ABInputPortPortRemovedNotification;

extern NSString * ABInputPortPortKey;

@class ABInputPort;
@class ABLiveBufferManager;

/*!
 * Audio input block type
 *
 *  Set the @link ABInputPort::audioInputBlock audioInputBlock @endlink property of the input port to a block
 *  that has this form, to be notified as audio becomes available.
 *
 *  Then, call @link ABInputPort::ABInputPortReceive ABInputPortReceive @endlink to receive the audio.
 *
 * @param inputPort         The input port.
 * @param lengthInFrames    The audio length, in frames in the @link ABInputPort::clientFormat client audio format @endlink.
 * @param nextTimestamp     The timestamp of the next available audio.
 * @param sourcePortOrNil   If you are receiving mixed audio (@link ABInputPort::receiveMixedAudio @endlink is YES), then nil. Otherwise, the audio's source port.
 */
typedef void (^ABInputPortAudioInputBlock)(ABInputPort *inputPort, UInt32 lengthInFrames, AudioTimeStamp *nextTimestamp, ABPort *sourcePortOrNil);


/*!
 * Input port
 *
 *  This class is used to receive audio sent by other Audiobus-compatible apps. Create an instance using the
 *  @link ABAudiobusController::addInputPortNamed:title: addInputPortNamed:title: @endlink
 *  method of @link ABAudiobusController @endlink.
 *
 *  You can receive Audiobus audio in a number of ways.
 *
 *  The easiest is to use the @link ABAudiobusAudioUnitWrapper audio unit wrapper @endlink, which takes care
 *  of all receiving and/or sending for you, if your app is built upon Remote IO.
 *
 *  Alternatively, you can use this class directly in one of two ways.
 *
 *  If you provide an @link audioInputBlock audio input block @endlink to this class, then
 *  you will be notified whenever audio becomes available.  Then, you can call @link ABInputPortReceive @endlink in
 *  order to access the received audio.
 *
 *  Alternately, you can let this class perform buffering for you, and simply call @link ABInputPortReceive @endlink (and
 *  possibly @link ABInputPortPeek @endlink) when you need audio.
 *
 *  You can receive audio as separate streams (one per source) or as a single mixed audio stream (see @link receiveMixedAudio @endlink).
 *
 *  Both ABInputPortPeek and ABInputPortReceive are C functions designed for use within a realtime Core Audio context.
 *  That means they don't hold locks, don't allocate memory and don't call Objective-C methods. You should keep a local pointer
 *  to this class, to be passed as the first parameter to these functions.
 */
@interface ABInputPort : ABPort

/*!
 * Receive audio
 *
 *  Use this C function to receive lossless but potentially high-latency audio.
 *  It's suitable for use from within a realtime Core Audio context.
 *
 *  This function will allow you to receive audio that is lossless, but may introduce some latency in the event of
 *  network loss or highly-loaded device CPU.  If you intend to play audio live, use @link ABInputPortReceiveLive @endlink.
 *
 *  Please note that if you are receiving separate streams (@link receiveMixedAudio @endlink is NO), then this function will
 *  provide synchronized streams for each connected source port. If you are not using an @link audioInputBlock @endlink, then
 *  certain procedures must be followed:
 *
 *  - All calls to ABInputPortReceive/ABInputPortPeek must be performed on the same thread.
 *  - You must call @link ABInputPortEndReceiveTimeInterval @endlink at the end of each time interval (such as for each render
 *    of your audio system, or each input notification), to tell Audiobus that you are finished with all audio for that interval.
 *    Audio for any sources that you did not receive audio for will be discarded.
 *
 * <blockquote class="alert">
 * It's critically important that you **DO NOT** call ABInputPortReceive with NULL buffers to discard
 * audio in order to achieve low-latency output. This will introduce audio artefacts. If you are recording audio,
 * **these artefacts will be evident in the recording**.
 *
 * If you wish to produce low-latency audio output, use ABLiveBuffer.
 * </blockquote>
 *
 * @param inputPort         The input port.
 * @param sourcePortOrNil   If you are receiving separate streams (@link receiveMixedAudio @endlink is NO), this must be a valid source port - one of the ports from the
 *                          @link sources @endlink array. Otherwise, if you are receiving a mixed stream, pass nil.
 * @param bufferList        The audio buffer list to receive audio into, in the format specified by @link clientFormat @endlink. If NULL, then audio will simply be discarded. 
 *                          If 'mData' pointers are NULL, then an internal buffer will be provided.
 * @param ioLengthInFrames  On input, the number of frames requested. On output, the number of frames received.
 * @param outTimestamp      On output, if not NULL, the timestamp of the returned audio.
 * @param ioMetadataBlockList If receiving separate streams (@link receiveMixedAudio @endlink is NO), and not NULL, this @link ABMetadataBlockList metadata block list @endlink 
 *                          will be filled with the metadata corresponding to the received audio. If the `bytes' field is NULL, then an internal buffer will be provided (not
 *                          thread-safe). The `packet_index' field of each block will identify one or more packets that the returned audio spans.
 */
void ABInputPortReceive(ABInputPort *inputPort, ABPort *sourcePortOrNil, AudioBufferList *bufferList, UInt32 *ioLengthInFrames, AudioTimeStamp *outTimestamp, ABMetadataBlockList *ioMetadataBlockList);

/*!
 * When receiving separate streams, mark the end of the current time interval
 *
 *  When you are receiving separate streams (@link receiveMixedAudio @endlink is NO), and not using an 
 *  @link audioInputBlock @endlink then this function must be called at the end of each time interval to 
 *  signal to Audiobus that you have finished receiving the incoming audio for the given interval.
 *
 *  Note that there's no need to use this function if you are using an @link audioInputBlock @endlink.
 *
 * @param inputPort         The input port.
 */
void ABInputPortEndReceiveTimeInterval(ABInputPort *inputPort);

/*!
 * Receive live, low-latency audio
 *
 *  Use this C function to receive low-latency, but lossy audio.
 *  It's suitable for use from within a realtime Core Audio context.
 *
 *  This function will allow you to receive audio that is of slightly less quality, but adjusted to be low latency
 *  for use with live audio output. If you intend to record audio instead of playing it out the speaker, live, you
 *  should use @link ABInputPortReceive @endlink instead.
 *
 *  Note that if you are recording and playing audio out loud, you can use @link ABInputPortReceive @endlink or 
 *  @link audioInputBlock @endlink for recording and ABInputPortReceiveLive for playing aloud, simultaneously.
 *
 * @param inputPort         The input port.
 * @param bufferList        The audio buffer list to receive audio into, in the format specified by @link clientFormat @endlink. Must not be NULL.
 *                          If 'mData' pointers are NULL, then an internal buffer will be provided (not thread-safe).
 * @param lengthInFrames    The number of frames requested. This method will never return less than the requested frames.
 * @param outTimestamp      On output, if not NULL, the timestamp of the returned audio.
 */
void ABInputPortReceiveLive(ABInputPort *inputPort, AudioBufferList *bufferList, UInt32 lengthInFrames, AudioTimeStamp *outTimestamp);

/*!
 * Audio Queue version of ABInputPortReceive
 *
 *  You can use this function to pull audio from Audiobus into an Audio Queue buffer. This may be used
 *  inside an AudioQueueInputCallback to replace the audio received from the microphone with audio
 *  from Audiobus, for instance.
 *
 *  See discussion for @link ABInputPortReceive @endlink.
 *
 * @param inputPort         The input port.
 * @param sourcePortOrNil   If you are receiving separate streams (@link receiveMixedAudio @endlink is NO), this must be nil. Otherwise, pass the port to receive audio from.
 * @param bufferList        The buffer list to receive audio into, in the format specified by @link clientFormat @endlink. If NULL, then audio will simply be discarded.
 * @param ioLengthInFrames  On input, the number of frames requested. On output, the number of frames received.
 * @param outTimestamp      On output, if not NULL, the timestamp of the returned audio.
 * @param ioMetadataBlockList If receiving separate streams (@link receiveMixedAudio @endlink is NO), and not NULL, this @link ABMetadataBlockList metadata block list @endlink
 *                          will be filled with the metadata corresponding to the received audio. If the `bytes' field is NULL, then an internal buffer will be provided (not
 *                          thread-safe). The `packet_index' field of each block will identify one or more packets that the returned audio spans.
 */
void ABInputPortReceiveAQ(ABInputPort *inputPort, ABPort *sourcePortOrNil, AudioQueueBufferRef bufferList, UInt32 *ioLengthInFrames, AudioTimeStamp *outTimestamp, ABMetadataBlockList *ioMetadataBlockList);

/*!
 * Audio Queue version of ABInputPortReceiveLive
 *
 *  You can use this function to pull audio from Audiobus into an Audio Queue buffer. This may be used
 *  inside an AudioQueueInputCallback to replace the audio received from the microphone with audio
 *  from Audiobus, for instance.
 *
 *  See discussion for @link ABInputPortReceiveLive @endlink.
 *
 * @param inputPort         The input port.
 * @param bufferList        The buffer list to receive audio into, in the format specified by @link clientFormat @endlink. Must not be NULL.
 * @param lengthInFrames    The number of frames requested. This method will never return less than the requested frames.
 * @param outTimestamp      On output, if not NULL, the timestamp of the returned audio.
 */
void ABInputPortReceiveLiveAQ(ABInputPort *inputPort, AudioQueueBufferRef bufferList, UInt32 lengthInFrames, AudioTimeStamp *outTimestamp);

/*!
 * Peek the audio buffer
 *
 *  Use this C function to determine how much audio is currently buffered, and the corresponding next timestamp.
 *  It's suitable for use from within a realtime Core Audio context.
 *
 *  This function will allow you to receive audio that is lossless, but may introduce some latency in the event of
 *  network loss or highly-loaded device CPU.  If you intend to play audio live, use @link ABInputPortReceiveLive @endlink.
 *
 * @param inputPort         The input port.
 * @param outNextTimestamp  If not NULL, the timestamp of the next available audio.
 * @return Number of frames of available audio, in the specified @link clientFormat audio format @endlink.
 */
UInt32 ABInputPortPeek(ABInputPort *inputPort, AudioTimeStamp *outNextTimestamp);

/*!
 * Determine if the input port is currently connected to any sources
 *
 *  This function is suitable for use from within a realtime Core Audio context.
 *
 * @param inputPort The input port.
 * @return YES if there are currently sources connected; NO otherwise.
 */
BOOL ABInputPortIsConnected(ABInputPort *inputPort);

/*!
 * Set the volume level for a particular source
 *
 *  Note that this applies to the mixed stream as accessed via
 *  ABInputPortReceive when the receiveMixedAudio property is YES,
 *  as well as the live mixed stream as accessed via ABInputPortReceiveLive.
 *
 *  It does not affect separate streams accessed via ABInputPortReceive
 *  when receiveMixedAudio is NO.
 *
 * @param volume            Volume level (0 - 1); default 1
 * @param port              Source port
 */
- (void)setVolume:(float)volume forSourcePort:(ABPort*)port;

/*!
 * Get the volume level for a source
 *
 * @param port              Source port
 * @return Volume for the given port (0 - 1)
 */
- (float)volumeForSourcePort:(ABPort*)port;

/*!
 * Set the pan for a particular source
 *
 *  Note that this applies to the mixed stream as accessed via
 *  ABInputPortReceive when the receiveMixedAudio property is YES,
 *  as well as the live mixed stream as accessed via ABInputPortReceiveLive.
 *
 *  It does not affect separate streams accessed via ABInputPortReceive
 *  when receiveMixedAudio is NO.
 *
 * @param pan               Pan (-1.0 - 1.0); default 0.0
 * @param port              Source port
 */
- (void)setPan:(float)pan forSourcePort:(ABPort*)port;

/*!
 * Get the pan level for a source
 *
 * @param port              Source port
 * @return Pan for the given port (-1.0 - 1.0)
 */
- (float)panForSourcePort:(ABPort*)port;

/*!
 * Currently-connected sources
 *
 *  This is an array of @link ABPort ABPorts @endlink.
 */
@property (nonatomic, retain, readonly) NSArray *sources;

/*!
 * The audio input block
 *
 *  Set this property to a block to be called whenever there is audio available. This
 *  will receive audio that is lossless, but may introduce some latency in the event of
 *  network loss or highly-loaded device CPU.  If you intend to play audio live, use
 *  @link ABInputPortReceiveLive @endlink instead.
 */
@property (nonatomic, copy) ABInputPortAudioInputBlock audioInputBlock;

/*!
 * Block to be called at the end of each audio block
 *
 *  If @link receiveMixedAudio @endlink is NO and @link audioInputBlock @endlink is,
 *  assigned, then a block assigned to this property will be called after 
 *  @link audioInputBlock @endlink has been invoked for each source, for a given 
 *  time interval.
 *
 *  This can be used to do any port-wide audio processing for each time step, such
 *  as managing a global effects bus.
 */
@property (nonatomic, copy) void(^endOfAudioTimeIntervalBlock)(AudioTimeStamp *timestamp);

/*!
 * Whether to receive audio as a mixed stream
 *
 *  This setting applies to the lossless receive mode only, not the live, low-latency mode (ABInputPortReceiveLive).
 *
 *  If YES (default), then all incoming audio across all sources will be mixed to a single audio stream.
 *  Otherwise, you will receive separate audio streams for each connected port.
 */
@property (nonatomic, assign) BOOL receiveMixedAudio;

/*!
 * Client format
 *
 *  Use this to specify what audio format your app uses. Audio will be automatically
 *  converted from the Audiobus line format.
 *
 *  The default value is @link ABAudioStreamBasicDescription @endlink.
 */
@property (nonatomic, assign) AudioStreamBasicDescription clientFormat;

/*!
 * The title of the port, for display to the user
 */
@property (nonatomic, retain, readwrite) NSString *title;

/*!
 * The port icon (a 32x32 image)
 *
 *  This is optional if your app only has one input port, but if your app
 *  defines multiple input ports, it is highly recommended that you provide icons
 *  for each, for easy identification by the user.
 */
@property (nonatomic, retain, readwrite) UIImage *icon;

/*!
 * Port attributes
 *
 *  This is a combination of zero or more attributes that are sent to peers to inform them how your app behaves with this port.
 *  For example, if your app is a receiver and plays the received audio out the speaker, then you must indicate
 *  this with the flag ABInputPortAttributePlaysLiveAudio, so the sender knows to mute its output.
 *
 *  You may change these flags at any time, and other peers will be informed.
 */
@property (nonatomic, assign) ABInputPortAttributes attributes;

/*!
 * The attributes of all upstream ports connected to this port
 *
 *  Currently unused
 */
@property (nonatomic, readonly) ABOutputPortAttributes connectedPortAttributes;

/*!
 * Mute live audio when connected to self
 *
 *  This determines whether the port will mute the audio received via ABInputPortReceiveLive when
 *  it detects an output port from the same app connected. This avoids feedback in the event
 *  that the output port's audio signal carries a component from the input.
 *
 *  Default value is YES.
 */
@property (nonatomic, assign) BOOL muteLiveAudioInputWhenConnectedToSelf;

/*!
 * Whether the port is connected to self
 *
 *  This returns YES when the input port detects a connected output port from the same app.
 *  If you set @link muteLiveAudioInputWhenConnectedToSelf @endlink to NO, and are providing
 *  live monitoring of the input signal, you should mute your live monitoring if this is YES.
 *
 *  This property is key-value observable.
 */
@property (nonatomic, readonly) BOOL connectedToSelf;

@end

#ifdef __cplusplus
}
#endif