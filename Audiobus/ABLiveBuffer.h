//
//  ABLiveBuffer.h
//  Audiobus
//
//  Created by Michael Tyson on 24/10/2012.
//  Copyright (c) 2012 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <Foundation/Foundation.h>
#import <AudioToolbox/AudioToolbox.h>

/*!
 * Live buffer source
 *
 *  A unique pointer to represent a source. This can be
 *  anything you like, as long as it's unique.
 */
typedef void* ABLiveBufferSource;

/*!
 * Live buffer
 *
 *  This utility class offers live buffering functionality outside of
 *  Audiobus itself, for the purposes of enqueueing audio for live monitoring.
 *
 *  Its intended use is within receiver apps that:
 *
 *  1. Record or otherwise process lossless (as opposed to live) Audiobus audio,
 *  2. Manipulate the audio (such as adding effects), and
 *  3. Wish to offer live monitoring of the manipulated audio.
 *
 *  In this scenario, you receive and process the lossless
 *  audio via [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive). That means
 *  the Audiobus live buffer ([ABInputPortReceiveLive](@ref ABInputPort::ABInputPortReceiveLive))
 *  won't reflect the manipulated audio.
 *
 *  What's required is a method to buffer and latency-adjust the processed audio for
 *  live monitoring.
 *
 *  Hence this class. You receive and process the audio streams from the input port,
 *  then enqueue them via @link ABLiveBufferEnqueue @endlink. In your audio system's render
 *  callback, instead of calling [ABInputPortReceiveLive](@ref ABInputPort::ABInputPortReceiveLive),
 *  call @link ABLiveBufferDequeue @endlink to retrieve a mixed, latency-adjusted and hole-repaired
 *  audio stream to use for live monitoring.
 *
 *  See the section in the Audiobus programming guide on
 *  [Audio Monitoring for Lossless Audio Streams](@ref Monitoring-Lossless) and 
 *  [Providing Live Monitoring when Receiving and Manipulating Separate Streams](@ref Receive-Streams-Monitoring)
 *  for discussion on using this class.
 *
 *  See the "AB Multitrack Receiver" and "AB Multitrack Oscillator" sample apps for demonstrations.
 *
 *  Thread Safety
 *  =============
 *
 *  You may call ABLiveBufferDequeue and ABLiveBufferEnqueue from different threads, but all calls
 *  to ABLiveBufferDequeue must be performed on the same thread, and all calls to ABLiveBufferEnqueue 
 *  must be performed on the same thread.
 *
 *  ABLiveBufferMarkSourceIdle can be used from any thread.
 */
@interface ABLiveBuffer : NSObject

/*!
 * Initializer
 *
 *  @param clientFormat     The audio format to use for this class
 */
- (id)initWithClientFormat:(AudioStreamBasicDescription)clientFormat;

/*!
 * Enqueue audio
 *
 *  Use this to enqueue a single audio stream. You can provide as many streams as you like,
 *  as long as you provide a unique value as the `source' parameter. You can start or stop
 *  enqueuing audio for a source at any time and the buffer will adjust.
 *
 * @param liveBuffer        The live buffer
 * @param source            The source for this stream - an arbitrary pointer
 * @param bufferList        The audio to enqueue in the format described by the clientFormat parameter provided on init
 * @param lengthInFrames    The length of the audio, in frames
 * @param timestamp         The timestamp of the audio - this must have a valid mSampleTime field
 */
void ABLiveBufferEnqueue(ABLiveBuffer *liveBuffer, ABLiveBufferSource source, AudioBufferList *bufferList, UInt32 lengthInFrames, const AudioTimeStamp *timestamp);

/*!
 * Dequeue audio
 *
 *  Use this to pull mixed audio out of the live buffer for playback.
 *
 * @param liveBuffer        The live buffer
 * @param bufferList        The buffer list to fill. If you provide a buffer list with NULL mData pointers, a buffer will be provided automatically.
 * @param lengthInFrames    The length of audio to receive
 * @param outTimestamp      If not NULL, on output will be set to the timestamp of the audio
 */
void ABLiveBufferDequeue(ABLiveBuffer *liveBuffer, AudioBufferList *bufferList, UInt32 lengthInFrames, AudioTimeStamp *outTimestamp);

/*!
 * Dequeue an individual source
 *
 *  Call this function, passing in a source identifier, to receive separate, synchronized audio streams.
 *
 *  Do not use this function together with ABLiveBufferDequeue.
 *
 *  You MUST call ABLiveBufferEndTimeInterval at the end of each time interval if using this function.
 *
 *  This can safely be used in a different thread from the enqueue function.
 *
 * @param liveBuffer        The live buffer
 * @param source            The audio source
 * @param bufferList        The buffer list to write audio to. The mData pointers
 *                          may be NULL, in which case an internal buffer will be provided.
 * @param lengthInFrames    The length of audio to receive
 * @param outTimestamp      On output, the timestamp of the first audio sample
 */
void ABLiveBufferDequeueSingleSource(ABLiveBuffer *liveBuffer, ABLiveBufferSource source, AudioBufferList *bufferList, UInt32 lengthInFrames, AudioTimeStamp *outTimestamp);

/*!
 * Mark end of time interval
 *
 *  When receiving each audio source separately via ABLiveBufferDequeueSingleSource (instead of mixed
 *  with ABLiveBufferDequeue), you MUST call this function at the end of each time interval in order
 *  to inform the buffer that you are finished with that audio segment. Any sources that have not
 *  been dequeued will have their audio discarded in order to retain synchronization.
 *
 * @param liveBuffer The live stream buffer.
 */
void ABLiveBufferEndTimeInterval(ABLiveBuffer *liveBuffer);

/*!
 * Mark the given source as idle
 *
 *  Normally, if the live buffer doesn't receive any audio for a given source within
 *  a certain time interval, the buffer will wait, allowing no live frames to be dequeued 
 *  until either further audio is received for the source, or the idle time threshold is met.
 *
 *  To avoid this delay and immediately mark a given source as idle, use this function.
 *
 *  It can be used from any thread, completes quickly, and may be used for every time 
 *  interval (render callbacks, for example) without causing performance problems.
 *
 * @param liveBuffer        The live buffer
 * @param source            The source to mark as idle
 */
void ABLiveBufferMarkSourceIdle(ABLiveBuffer *liveBuffer, ABLiveBufferSource source);

/*!
 * Set volume for source
 */
- (void)setVolume:(float)volume forSource:(ABLiveBufferSource)source;

/*!
 * Get volume for source
 */
- (float)volumeForSource:(ABLiveBufferSource)source;

/*!
 * Set pan for source
 */
- (void)setPan:(float)pan forSource:(ABLiveBufferSource)source;

/*!
 * Get pan for source
 */
- (float)panForSource:(ABLiveBufferSource)source;

/*!
 * Set a different AudioStreamBasicDescription for a source
 *
 *  Important: Do not change this property while using enqueue/dequeue.
 *  You must stop enqueuing or dequeuing audio first.
 */
- (void)setAudioDescription:(AudioStreamBasicDescription)audioDescription forSource:(ABLiveBufferSource)source;

/*!
 * Client audio format
 *
 *  Important: Do not change this property while using enqueue/dequeue.
 *  You must stop enqueuing or dequeuing audio first.
 */
@property (nonatomic, assign) AudioStreamBasicDescription clientFormat;

@end

#ifdef __cplusplus
}
#endif