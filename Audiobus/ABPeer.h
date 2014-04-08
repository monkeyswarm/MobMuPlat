//
//  ABPeer.h
//  Audiobus
//
//  Created by Michael Tyson on 10/12/2011.
//  Copyright (c) 2011 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <UIKit/UIKit.h>
#import "ABCommon.h"

@class ABPort;

/*!
 * Audiobus peer
 *
 *  This class represents another Audiobus-compatible app, either running on the
 *  local device, or on another device accessed over the network.
 */
@interface ABPeer : NSObject
@property (nonatomic, retain, readonly) NSString *name;         //!< Peer name
@property (nonatomic, readonly) NSString *deviceName;           //!< Name of device peer is on
@property (nonatomic, retain, readonly) NSString *displayName;  //!< Peer display name
@property (nonatomic, readonly) BOOL present;                   //!< Whether the peer is currently present and accessible
@property (nonatomic, retain, readonly) NSURL *launchURL;       //!< The app's launch URL
@property (nonatomic, readonly) UIImage *icon;                  //!< App icon
@property (nonatomic, retain, readonly) NSArray *inputPorts;    //!< The input ports offered by the peer (array of @link ABPort @endlink).
@property (nonatomic, retain, readonly) NSArray *outputPorts;   //!< The output ports offered by the peer (array of @link ABPort @endlink).
@property (nonatomic, retain, readonly) NSArray *filterPorts;   //!< The filter ports offered by the peer (array of @link ABPort @endlink).
@end

#ifdef __cplusplus
}
#endif