//
//  ABAudiobusController.h
//  Audiobus
//
//  Created by Michael Tyson on 09/12/2011.
//  Copyright (c) 2011 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <Foundation/Foundation.h>
#import "ABCommon.h"
#import "ABFilterPort.h"

/** @name Notifications */
///@{
/*!
 * Peer appeared
 *
 *  Sent when an Audiobus peer appears for the first time.
 *  Peer is accessible in the notification userinfo via the `ABPeerKey'.
 */
extern NSString * const ABPeerAppearedNotification;

/*!
 * Peer disappeared
 *
 *  Sent when an Audiobus peer disappears.
 *  Peer is accessible in the notification userinfo via the `ABPeerKey'.
 */
extern NSString * const ABPeerDisappearedNotification;

/*!
 * Connections changed
 *
 *  Sent when the local app's connections have changed, caused by connections
 *  or disconnections from within the Audiobus app.
 */
extern NSString * const ABConnectionsChangedNotification;

/*!
 * Peer attributes changed
 *
 *  Sent when one or more attributes of a peer changes.
 */
extern NSString * const ABPeerAttributesChangedNotification;

/*!
 * Connection Panel was shown
 *
 *  Sent whenever the connection panel appears, either when a new session begins,
 *  or when the user drags the connection panel back out after hiding it.
 */
extern NSString * const ABConnectionPanelShownNotification;

/*!
 * Connection Panel was hidden
 *
 *  Sent whenever the connection panel is hidden, either when the session ends,
 *  or when the user drags the connection panel off the screen.
 */
extern NSString * const ABConnectionPanelHiddenNotification;

///@}

/*!
 * Peer key, used with notifications
 */
extern NSString * ABPeerKey;

@class ABInputPort;
@class ABOutputPort;
@class ABPeer;
@class ABPort;
@class ABTrigger;

/*!
 * Audiobus Controller
 *
 *  The main Audiobus class.  Create an instance of this, passing it the URL to launch your app.  Then, 
 *  create input, output and/or filter ports as required, and either create and use an
 *  @link ABAudiobusAudioUnitWrapper @endlink instance, or use the port sender/receiver objects directly.
 */
@interface ABAudiobusController : NSObject

/*!
 * Reset all peered Audiobus controllers
 *
 *  Call this to forget all established connections with instances of the Audiobus app.
 *
 *  The first time there is an incoming connection from the Audiobus app, the Audiobus
 *  library will prompt the user for permission to allow the connection. Calling this method
 *  will forgot all granted permissions, so that the next incoming Audiobus connection will
 *  cause another prompt to accept or deny the connection.
 */
+(void)resetAllPeeredConnections;

/*!
 * Initializer
 *
 * @param launchURL The URL used to launch your app (e.g. yourapp://)
 * @param apiKey Your app's API key (find this at the bottom of your app's details screen accessible from http://developer.audiob.us/apps)
 */
- (id)initWithAppLaunchURL:(NSURL*)launchURL apiKey:(NSString*)apiKey;

/*!
 * Add a trigger
 *
 *  This method allows you to define and add triggers that the user can invoke from outside your 
 *  app, in order to make your app perform some function, such as toggling recording.
 *
 *  Calling this method more than once with the same trigger will have no effect the subsequent times.
 *
 * @param trigger       The trigger
 */
- (void)addTrigger:(ABTrigger*)trigger;

/*!
 * Remove a trigger
 *
 *  Calling this method more than once with the same trigger will have no effect the subsequent times.
 *
 * @param trigger       Trigger to remove
 */
- (void)removeTrigger:(ABTrigger*)trigger;

/*!
 * Add an output port
 *
 *  Output ports let your app send audio to other apps.
 *
 *  You can create several output ports to offer several separate audio streams. For example, a multi-track 
 *  recorder could define additional output ports for each track, so each track can be routed to a different place.
 *
 *  Ideally, the first port you create should perform some sensible default behaviour: This will be the port
 *  that is selected by default when the user taps your app in the Audiobus port picker.
 *
 * @param name Name of port, for internal use
 * @param title Title of port, show to the user
 * @return Output port
 */
- (ABOutputPort*)addOutputPortNamed:(NSString*)name title:(NSString*)title;

/*!
 * Access an output port
 *
 *  If you are sending audio from a Core Audio thread, then you should not use this method from within
 *  the thread.  Instead, obtain a reference to the sender object ahead of time, on the main thread, then store 
 *  the pointer in a context directly accessible in the Core Audio thread, to avoid making any Objective-C calls from within
 *  the thread.
 *
 * @param name Name of port
 * @return Output port
 */
- (ABOutputPort*)outputPortNamed:(NSString*)name;

/*!
 * Remove an output port
 *
 *  It is your responsibility to make sure you stop accessing the port prior to calling this method.
 *
 * @param port The port to remove
 */
- (void)removeOutputPort:(ABOutputPort*)port;

/*!
 * Add an input port
 *
 *  Input ports allow your app to receive audio from other apps.
 *
 *  Note that any input port can receive inputs from any number of sources. You do not need to
 *  create additional input ports to receive audio from multiple sources.
 *
 *  Ideally, the first port you create should perform some sensible default behaviour: This will be the port
 *  that is selected by default when the user taps your app icon in the Audiobus port picker.
 *
 * @param name Name of port, for internal use
 * @param title Title of port, show to the user
 * @return Input port
 */
- (ABInputPort*)addInputPortNamed:(NSString*)name title:(NSString*)title;

/*!
 * Access an input port
 *
 *  If you are receiving audio from a Core Audio thread, then you should not use this method from within
 *  the thread.  Instead, obtain a reference to the receiver object ahead of time, on the main thread, then store 
 *  the pointer in a context directly accessible in the Core Audio thread, to avoid making any Objective-C calls from within
 *  the thread.
 *
 * @param name Name of port.
 * @return Input port
 */
- (ABInputPort*)inputPortNamed:(NSString*)name;

/*!
 * Remove an input port
 *
 *  It is your responsibility to make sure you stop accessing the port prior to calling this method.
 *
 * @param port The port to remove
 */
- (void)removeInputPort:(ABInputPort*)port;

/*!
 * Add a filter port
 *
 *  Filter ports expose audio processing functionality to the Audiobus ecosystem, allowing users to use your
 *  app as an audio filtering node.
 *
 *  When you create a filter port, you pass in a block to be used to process the audio as it comes in.
 *
 * @param name The name of the filter port, for internal use
 * @param title Title of port, show to the user
 * @param processBlock A block to use to process audio as it arrives at the filter port
 * @return Filter port
 */
- (ABFilterPort*)addFilterPortNamed:(NSString*)name title:(NSString*)title processBlock:(ABAudioFilterBlock)processBlock;

/*!
 * Get the filter port
 *
 *  This is used to access the attributes of the connected ports. Note that the actual process of
 *  receiving and sending audio is handled automatically.
 *
 * @param name The name of the filter port
 * @return Filter port
 */
- (ABFilterPort*)filterPortNamed:(NSString*)name;

/*!
 * Remove a filter port
 *
 * @param port The port to remove
 */
- (void)removeFilterPort:(ABFilterPort*)port;

/*!
 * Whether to allow multiple instances of this app in one Audiobus connection graph
 *
 *  If you set this to YES, then Audiobus will allow users to add more than one
 *  instance of your app within one Audiobus setup, such as in the input and the output
 *  positions simultaneously.
 *
 *  By default, this is disabled, as some apps may not function properly if their
 *  audio pipeline is traversed multiple times in the same time step.
 */
@property (nonatomic, assign) BOOL allowsMultipleInstancesInConnectionGraph;

/*!
 * Connection panel position
 *
 *  This defines where the connection panel appears within your app, when necessary.
 *
 *  You can set this at any time, and the panel, if visible, will animate to the new location.
 */
@property (nonatomic, assign) ABAudiobusConnectionPanelPosition connectionPanelPosition;

/*!
 * Currently defined output ports
 *
 *  The output ports you have registered with @link addOutputPortNamed:title: @endlink, as an
 *  array of ABOutputPort.
 */
@property (nonatomic, readonly) NSArray *outputPorts;

/*!
 * Currently defined input ports
 *
 *  The input ports you have registered with @link addInputPortNamed:title: @endlink, as an
 *  array of ABInputPort.
 */
@property (nonatomic, readonly) NSArray *inputPorts;

/*!
 * Currently defined filter ports
 *
 *  The filter ports you have registered with @link addFilterPortNamed:title:processBlock: @endlink, as an
 *  array of ABFilterPort.
 */
@property (nonatomic, readonly) NSArray *filterPorts;

/*!
 * All available @link ABPeer peers @endlink
 */
@property (nonatomic, retain, readonly) NSSet *peers;

/*!
 * All @link ABPeer peers @endlink that are connected as part of the current session
 */
@property (nonatomic, retain, readonly) NSArray *connectedPeers;

/*!
 * All @link ABPort ports @endlink that are connected as part of the current session
 */
@property (nonatomic, retain, readonly) NSArray *connectedPorts;

/*!
 * Whether the app is connected to anything via Audiobus
 */
@property (nonatomic, readonly) BOOL connected;

@end

#ifdef __cplusplus
}
#endif