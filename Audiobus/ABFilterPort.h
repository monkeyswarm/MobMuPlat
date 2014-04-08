//
//  ABFilterPort.h
//  Audiobus
//
//  Created by Michael Tyson on 04/05/2012.
//  Copyright (c) 2012 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "ABPort.h"
#import "ABCommon.h"

/*!
 * Filter port
 *
 *  This class is used to filter audio.  Create an instance using the
 *  @link ABAudiobusController::addFilterPortNamed:title:processBlock: addFilterPortNamed:title:processBlock: @endlink
 *  method of @link ABAudiobusController @endlink, passing in a filter implementation via the processBlock
 *  parameter.
 *
 *  When your filter port is connected as part of an Audiobus connection, the block will be
 *  called as audio enters the system, to process the audio.
 */
@interface ABFilterPort : ABPort

/*!
 * Get output audio, for playback
 *
 *  Use this C function to receive audio frames at the output of this filter port.
 *  You must feed this audio to your audio engine's output so that users of your
 *  filter will be able to hear the filtered audio.
 *
 *  Suitable for use from within a realtime Core Audio context.
 *
 * @param filterPort        The filter port.
 * @param bufferList        The audio buffer list to receive audio into, in the format specified by @link clientFormat @endlink. If NULL, then audio will simply be discarded.
 *                          If 'mData' pointers are NULL, then an internal buffer will be provided (not thread-safe).
 * @param lengthInFrames    The number of frames requested.
 * @param outTimestamp      On output, if not NULL, the timestamp of the returned audio.
 */
void ABFilterPortGetOutput(ABFilterPort *filterPort, AudioBufferList *bufferList, UInt32 lengthInFrames, AudioTimeStamp *outTimestamp);

/*!
 * Determine if the filter port is currently connected
 *
 *  This function is suitable for use from within a realtime Core Audio context.
 *
 * @param filterPort        The filter port.
 * @return YES the filter port is currently connected; NO otherwise.
 */
BOOL ABFilterPortIsConnected(ABFilterPort *filterPort);

/*!
 * Client format
 *
 *  Use this to specify what audio format your app uses. Audio will be automatically
 *  converted to and from the Audiobus line format.
 *
 *  The default value is @link ABAudioStreamBasicDescription @endlink.
 */
@property (nonatomic, assign) AudioStreamBasicDescription clientFormat;

/*!
 * Audio output client format
 *
 *  Use this to specify the audio format to use for your app's audio output.
 *  Audio returned by @link ABFilterPortGetOutput @endlink will be returned in this format.
 *
 *  If not set directly, this will be the same value as clientFormat.
 */
@property (nonatomic, assign) AudioStreamBasicDescription audioOutputClientFormat;

/*!
 * Audio buffer size for processing
 *
 *  Use this property to specify the number of frames you want to process each time
 *  the filter block is called. Audiobus will automatically queue up frames until this
 *  number is reached, then call the audio block for this number of frames.
 *
 *  For example, if you have a piece of DSP code that requires audio to be processed
 *  512 frames at a time, set this value to 512.
 *
 *  Lower values means lower latency.
 *
 *  The default value is 256.
 */
@property (nonatomic, assign) UInt32 audioBufferSize;
 
/*!
 * Currently-connected sources
 *
 *  This is an array of @link ABPort ABPorts @endlink.
 */
@property (nonatomic, retain, readonly) NSArray *sources;

/*!
 * Currently-connected destinations
 *
 *  This is an array of @link ABPort ABPorts @endlink.
 */
@property (nonatomic, retain, readonly) NSArray *destinations;

/*!
 * The attributes of all downstream ports connected to this port
 *
 *  Currently unused.
 */
@property (nonatomic, readonly) ABInputPortAttributes connectedDownstreamPortAttributes;

/*!
 * The attributes of all upstream ports connected to this port
 *
 *  Currently unused.
 */
@property (nonatomic, readonly) ABOutputPortAttributes connectedUpstreamPortAttributes;

/*!
 * The title of the port, for display to the user
 */
@property (nonatomic, retain, readwrite) NSString *title;

/*!
 * The port icon (a 32x32 image)
 *
 *  This is optional if your app only has one filter port, but if your app
 *  defines multiple filter ports, it is highly recommended that you provide icons
 *  for each, for easy identification by the user.
 */
@property (nonatomic, retain, readwrite) UIImage *icon;

@end

#ifdef __cplusplus
}
#endif
