//
//  ABCommon.h
//  Audiobus
//
//  Created by Michael Tyson on 27/01/2012.
//  Copyright (c) 2011-2014 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <Foundation/Foundation.h>
#import <AudioToolbox/AudioToolbox.h>

/*!
 * Connection panel position
 *
 *  Defines the positioning of the connection panel in your app, when it is visible.
 */
typedef enum {
    ABConnectionPanelPositionRight,
    ABConnectionPanelPositionLeft,
    ABConnectionPanelPositionBottom
} ABConnectionPanelPosition;

/*!
 * Peer resource identifier
 */
typedef uint32_t ABPeerResourceID;

int _ABAssert(BOOL condition, char* msg, char* file, int line);

#define ABAssert(condition,msg) (_ABAssert((BOOL)(condition),(msg),strrchr(__FILE__, '/')+1,__LINE__))

    
#ifdef __cplusplus
}
#endif
