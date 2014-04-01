//
//  ABOutputPort.h
//  Audiobus
//
//  Created by Michael Tyson on 25/11/2011.
//  Copyright (c) 2011 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>
#import "ABCommon.h"
#import "ABPort.h"

/*!
 * Output port
 *
 *  This class is used to transmit audio. Create an instance using the
 *  @link ABAudiobusController::addOutputPortNamed:title: addOutputPortNamed:title: @endlink
 *  method of @link ABAudiobusController @endlink.
 *
 *  There are two ways to send audio with Audiobus.
 *
 *  The easiest is to use the @link ABAudiobusAudioUnitWrapper audio unit wrapper @endlink, which takes care
 *  of all sending and/or receiving for you, if your app is built upon Remote IO.
 *
 *  Alternatively, you can use this class directly, after specifying your @link clientFormat audio format @endlink.
 *
 *  Use this class by keeping a pointer to it from within your audio generation context, then send the audio using the
 *  @link ABOutputPortSendAudio ABOutputPortSendAudio @endlink C function.
 *
 *  This class has been designed to be used from within a realtime Core Audio thread. The ABOutputPortSendAudio function
 *  does not hold locks, allocate memory, or call Objective-C methods.  You should keep a local pointer
 *  to this class, to be passed as the first parameter to these functions.
 */
@interface ABOutputPort : ABPort

/*!
 * Send audio
 *
 *  This C function is used to send audio. It's suitable for use within a realtime thread, as it does not hold locks,
 *  allocate memory or call Objective-C methods.  You should keep a local pointer to the ABOutputPort instance, to be
 *  passed as the first parameter.
 *
 * @param outputPort        Output port.
 * @param audio             Audio buffer list to send, in the @link clientFormat client format @endlink.
 * @param lengthInFrames    Length of the audio, in frames.
 * @param timestamp         The timestamp of the audio.
 * @param metadata          Packet metadata, or NULL.
 */
BOOL ABOutputPortSendAudio(ABOutputPort* outputPort, const AudioBufferList *audio, UInt32 lengthInFrames, const AudioTimeStamp *timestamp, ABMetadataBlockList *metadata);

/*!
 * Determine if the output port is currently connected to any destinations
 *
 *  This function is suitable for use from within a realtime Core Audio context.
 *
 * @param outputPort        Output port.
 * @return YES if there are currently destinations connected; NO otherwise.
 */
BOOL ABOutputPortIsConnected(ABOutputPort* outputPort);

/*!
 * Get connected port attributes
 *
 *  This C function allows you to determine the attributes of connected ports efficiently. Use it in a Core Audio
 *  context to determine whether you need to mute the output, for example, if the ABInputPortAttributePlaysLiveAudio bit
 *  is set (`attributes & ABInputPortAttributePlaysLiveAudio`).
 *
 *  Note that output muting is already taken care of for you if you use the 
 *  @link ABAudiobusAudioUnitWrapper audio unit wrapper @endlink.
 *
 * @param outputPort        Output port.
 * @return Connected port attributes.
 */
ABInputPortAttributes ABOutputPortGetConnectedPortAttributes(ABOutputPort *outputPort);

/*!
 * Get average latency
 *
 *  This C function returns the average transmission latency across all connected remote ports. Where appropriate, use
 *  it to offset generated audio to compensate.
 *
 * @param outputPort        Output port.
 * @return Latency, in seconds.
 */
NSTimeInterval ABOutputPortGetAverageLatency(ABOutputPort *outputPort);

/*!
 * Client format
 *
 *  Use this to specify what audio format your app uses. Audio will be automatically
 *  converted to the Audiobus line format.
 *
 *  The default value is @link ABAudioStreamBasicDescription @endlink.
 */
@property (nonatomic, assign) AudioStreamBasicDescription clientFormat;

/*!
 * Currently-connected destinations
 *
 *  This is an array of @link ABPort ABPorts @endlink.
 */
@property (nonatomic, retain, readonly) NSArray *destinations;

/*!
 * The title of the port, for display to the user.
 */
@property (nonatomic, retain, readwrite) NSString *title;

/*!
 * The port icon (a 32x32 image)
 *
 *  This is optional if your app only has one output port, but if your app
 *  defines multiple output ports, it is highly recommended that you provide icons
 *  for each, for easy identification by the user.
 */
@property (nonatomic, retain, readwrite) UIImage *icon;

/*!
 * Port attributes
 *
 *  Currently unused.
 */
@property (nonatomic, assign) ABOutputPortAttributes attributes;

/*!
 * The attributes of all downstream ports connected to this port
 *
 *  These attributes indicate to your app what the connected app(s) are doing. Your app should
 *  respond appropriately (that is, if the ABInputPortAttributePlaysLiveAudio flag is present
 *  (`attributes & ABInputPortAttributePlaysLiveAudio`), then the audio on the remote port that your
 *  app is sending to will be played out loud by the other app, so your app should mute its output).
 *
 *  Note that output muting is already taken care of for you if you use the
 *  @link ABAudiobusAudioUnitWrapper audio unit wrapper @endlink.
 */
@property (nonatomic, readonly) ABInputPortAttributes connectedPortAttributes;

@end

#ifdef __cplusplus
}
#endif