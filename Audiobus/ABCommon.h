//
//  ABCommon.h
//  Audiobus
//
//  Created by Michael Tyson on 27/01/2012.
//  Copyright (c) 2012 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <Foundation/Foundation.h>
#import <AudioToolbox/AudioToolbox.h>

/*!
 * Input port attributes
 */
enum {
    ABInputPortAttributeNone            = 0x0,//!< No attributes
    ABInputPortAttributePlaysLiveAudio  = 0x1  //!< The receiver will play the received audio out loud, live.
                                               //!< Connected senders should mute their output.
};
typedef uint32_t ABInputPortAttributes;

/*!
 * Output port attributes (currently unused)
 */
enum {
    ABOutputPortAttributeNone            = 0x0,
};
typedef uint32_t ABOutputPortAttributes;

/*!
 * App capabilities
 */
typedef enum {
    ABAppCapabilitySend = 1,
    ABAppCapabilityReceive = 2,
    ABAppCapabilityFilter = 4
} ABAppCapability;

/*!
 * Connection panel position
 *
 *  Defines the positioning of the connection panel in your app, when it is visible.
 */
typedef enum {
    ABAudiobusConnectionPanelPositionRight,
    ABAudiobusConnectionPanelPositionLeft,
    ABAudiobusConnectionPanelPositionBottom
} ABAudiobusConnectionPanelPosition;

/*!
 * Peer resource identifier
 */
typedef uint32_t ABPeerResourceID;

/*!
 * Audiobus line format's sample rate
 */
#define ABAudioFormatSampleRate             44100.0

/*!
 * Audiobus line format
 */
extern const AudioStreamBasicDescription ABAudioStreamBasicDescription;

/*!
 * Determine if ASBD is compatible with the Audiobus line format without automatic conversion
 */
bool ABASBDIsCompatible(AudioStreamBasicDescription asbd);

/*!
 * Metadata block
 *
 *  This defines a single metadata block, defined by a developer-defined application ID. See
 *  [Metadata](@ref Metadata).
 */
typedef struct {
    void     *source_port;                //!< The port that sent this metadata
    uint8_t   packet_index;               //!< When receiving, this identifies the index of the packet that this metadata applies to
    uint32_t  application_id;             //!< Unique value (eg. fourcc) specific to the particular metadata content
    uint16_t  length;                     //!< Length of metadata in bytes
    void      *bytes;                     //!< Pointer to metadata
} ABMetadataBlock;

/*!
 * List of metadata blocks
 */
typedef struct {
    int numberOfBlocks;                   //!< Total number of blocks in list
    void *bytes;                          //!< Storage for metadata block data (used when receiving metadata, not sending)
    int bytes_length;                     //!< Length of 'bytes'
    ABMetadataBlock blocks[1];            //!< First block (successive blocks follow)
} ABMetadataBlockList;

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

int _ABAssert(int condition, char* msg, char* file, int line);

#define ABAssert(condition,msg) (_ABAssert((condition),(msg),strrchr(__FILE__, '/')+1,__LINE__))

    
#ifdef __cplusplus
}
#endif
