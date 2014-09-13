//
//  ABAudiobusController.h
//  Audiobus
//
//  Created by Michael Tyson on 09/12/2011.
//  Copyright (c) 2011-2014 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <Foundation/Foundation.h>
#import "ABCommon.h"
#import "ABFilterPort.h"

#pragma mark Notifications
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

#pragma mark State IO Protocol
/** @name State IO Protocol */
///@{
  
/*!
 * State input/output delegate protocol
 *
 *  This protocol is used to provide app-specific state data when a preset
 *  is saved within Audiobus. This state data will then be presented back to
 *  your app when the user loads the saved preset within Audiobus, so your 
 *  app can restore the prior state.
 *
 *  The nature of the state information to be saved and restored is up to you.
 *  It should be enough to put your app into the same operating state as when
 *  the preset was saved, but should not necessarily contain the user's content.
 *  Presets should represent workspaces, rather than complete projects.
 *
 *  To assist in streamlining your app initialization, when your app is being
 *  launched from a preset within Audiobus, Audiobus will launch your app by
 *  providing the string "incoming-preset" to the host part of the app launch
 *  URL. For example, if your Audiobus launch URL is "myapp.audiobus://", launching
 *  your app from within an Audiobus preset will cause the app to be launched
 *  with the URL "myapp.audiobus://incoming-preset". You can then detect this
 *  condition from within `application:openURL:sourceApplication:annotation:`
 */
@protocol ABAudiobusControllerStateIODelegate <NSObject>

/*!
 * Provide a dictionary to represent the current app state
 *
 *  This dictionary can represent any state you deem relevant to the saved
 *  preset, for later restoration in 
 *  @link loadStateFromAudiobusStateDictionary:responseMessage: @endlink.
 *  It may only contain values that can be represented in a Property List
 *  (See Apple's "Property List Types and Objects" documentation).
 *
 *  You may include NSData objects representing larger resources, if
 *  appropriate, such as audio data for a sampler. To avoid loading large
 *  files into memory all at once, you can request that the NSData use
 *  memory-mapping via the NSDataReadingMappedIfSafe hint.
 *
 *  Note: You should not spend more than a couple hundred milliseconds
 *  (at most) gathering state information in this method.
 *
 * @return A dictionary containing state information for your app.
 */
- (NSDictionary*)audiobusStateDictionaryForCurrentState;
    
/*!
 * Load state from previously-created state dictionary
 *
 *  This method is called when the user loads a preset from within Audiobus.
 *  You will receive the state dictionary originally provided via
 *  @link audiobusStateDictionaryForCurrentState @endlink, and should apply this state
 *  information to restore your app to the state it was in when saved.
 *
 *  If you wish, you may provide a message to be displayed to the user within 
 *  Audiobus, via the 'outResponseMessage' parameter. This can be used to notify
 *  the user of any issues with the state load, like the case where the state
 *  relies on some In-App Purchase content the user hasn't bought yet.
 *
 * @param dictionary The state dictionary, as originally provided via
 *      @link audiobusStateDictionaryForCurrentState @endlink. In addition to the keys
 *      you provided, the value of the key ABStateDictionaryPresetNameKey will contain
 *      the name of the preset, as set by the user.
 * @param outResponseMessage Response message to be displayed to the user (optional)
 */
- (void)loadStateFromAudiobusStateDictionary:(NSDictionary*)dictionary responseMessage:(NSString**)outResponseMessage;

@end

extern NSString * const ABStateDictionaryPresetNameKey;
    
#pragma mark -
///@}

/*!
 * Peer key, used with notifications
 */
extern NSString * const ABPeerKey;

@class ABReceiverPort;
@class ABSenderPort;
@class ABFilterPort;
@class ABPeer;
@class ABPort;
@class ABTrigger;

/*!
 * Audiobus Controller
 *
 *  The main Audiobus class.  Create an instance of this then
 *  create and add receiver, sender and/or filter ports as required.
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
 * @param apiKey Your app's API key (find this at the bottom of your app's details screen accessible from http://developer.audiob.us/apps)
 */
- (id)initWithApiKey:(NSString*)apiKey;

#pragma mark - Triggers
/** @name Triggers */
///@{

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

///@}
#pragma mark - Audio ports
/** @name Audio ports */
///@{

/*!
 * Add a sender port
 *
 *  Sender ports let your app send audio to other apps.
 *
 *  You can create several sender ports to offer several separate audio streams. For example, a multi-track
 *  recorder could define additional sender ports for each track, so each track can be routed to a different place.
 *
 *  Ideally, the first port you create should perform some sensible default behaviour: This will be the port
 *  that is selected by default when the user taps your app in the Audiobus port picker.
 *
 * @param port The port to add
 */
- (void)addSenderPort:(ABSenderPort*)port;

/*!
 * Access a sender port
 *
 *  If you are sending audio from a Core Audio thread, then you should not use this method from within
 *  the thread.  Instead, obtain a reference to the sender object ahead of time, on the main thread, then store 
 *  the pointer in a context directly accessible in the Core Audio thread, to avoid making any Objective-C calls from within
 *  the thread.
 *
 * @param name Name of port
 * @return Sender port
 */
- (ABSenderPort*)senderPortNamed:(NSString*)name;

/*!
 * Remove a sender port
 *
 *  It is your responsibility to make sure you stop accessing the port prior to calling this method.
 *
 * @param port The port to remove
 */
- (void)removeSenderPort:(ABSenderPort*)port;

/*!
 * Sort the sender ports
 *
 *  This method allows you to assign an order to the sender ports. This is the
 *  order in which the ports will appear within Audiobus.
 *
 * @param cmptr Comparitor block used to provide the order
 */
- (void)sortSenderPortsUsingComparitor:(NSComparator)cmptr;

/*!
 * Add a receiver port
 *
 *  Receiver ports allow your app to receive audio from other apps.
 *
 *  Note that any receiver port can receive inputs from any number of sources. You do not need to
 *  create additional receiver ports to receive audio from multiple sources.
 *
 *  Ideally, the first port you create should perform some sensible default behaviour: This will be the port
 *  that is selected by default when the user taps your app icon in the Audiobus port picker.
 *
 * @param port The receiver port
 */
- (void)addReceiverPort:(ABReceiverPort*)port;

/*!
 * Access a receiver port
 *
 *  If you are receiving audio from a Core Audio thread, then you should not use this method from within
 *  the thread.  Instead, obtain a reference to the receiver object ahead of time, on the main thread, then store 
 *  the pointer in a context directly accessible in the Core Audio thread, to avoid making any Objective-C calls from within
 *  the thread.
 *
 * @param name Name of port.
 * @return Receiver port
 */
- (ABReceiverPort*)receiverPortNamed:(NSString*)name;

/*!
 * Remove a receiver port
 *
 *  It is your responsibility to make sure you stop accessing the port prior to calling this method.
 *
 * @param port The port to remove
 */
- (void)removeReceiverPort:(ABReceiverPort*)port;

/*!
 * Sort the receiver ports
 *
 *  This method allows you to assign an order to the receiver ports. This is the
 *  order in which the ports will appear within Audiobus.
 *
 * @param cmptr Comparitor block used to provide the order
 */
- (void)sortReceiverPortsUsingComparitor:(NSComparator)cmptr;

/*!
 * Add a filter port
 *
 *  Filter ports expose audio processing functionality to the Audiobus ecosystem, allowing users to use your
 *  app as an audio filtering node.
 *
 *  When you create a filter port, you pass in a block to be used to process the audio as it comes in.
 *
 * @param port The filter port
 */
- (void)addFilterPort:(ABFilterPort*)port;

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
 * Sort the filter ports
 *
 *  This method allows you to assign an order to the fiter ports. This is the
 *  order in which the ports will appear within Audiobus.
 *
 * @param cmptr Comparitor block used to provide the order
 */
- (void)sortFilterPortsUsingComparitor:(NSComparator)cmptr;

/*!
 * Currently defined sender ports
 *
 *  The sender ports you have registered with @link addSenderPort: @endlink, as an
 *  array of ABSenderPort.
 */
@property (nonatomic, readonly) NSArray *senderPorts;

/*!
 * Currently defined receiver ports
 *
 *  The receiver ports you have registered with @link addReceiverPort: @endlink, as an
 *  array of ABReceiverPort.
 */
@property (nonatomic, readonly) NSArray *receiverPorts;

/*!
 * Currently defined filter ports
 *
 *  The filter ports you have registered with @link addFilterPort: @endlink, as an
 *  array of ABFilterPort.
 */
@property (nonatomic, readonly) NSArray *filterPorts;

///@}
#pragma mark - Properties
/** @name Properties */
///@{

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
@property (nonatomic, assign) BOOL allowsConnectionsToSelf;

/*!
 * Connection panel position
 *
 *  This defines where the connection panel appears within your app, when necessary.
 *
 *  You can set this at any time, and the panel, if visible, will animate to the new location.
 */
@property (nonatomic, assign) ABConnectionPanelPosition connectionPanelPosition;

/*!
 * All available @link ABPeer peers @endlink
 */
@property (nonatomic, strong, readonly) NSSet *peers;

/*!
 * All @link ABPeer peers @endlink that are connected as part of the current session
 */
@property (nonatomic, strong, readonly) NSSet *connectedPeers;

/*!
 * All @link ABPort ports @endlink that are connected as part of the current session
 */
@property (nonatomic, strong, readonly) NSSet *connectedPorts;

/*!
 * Whether the app is connected to anything via Audiobus or Inter-App Audio
 */
@property (nonatomic, readonly) BOOL connected;

/*!
 * Whether the app is connected to anything via Audiobus specifically (not Inter-App Audio)
 */
@property (nonatomic, readonly) BOOL audiobusConnected;

/*!
 * Whether the Audiobus app is running on this device
 *
 *  You should observe this property in order to manage your app's lifecycle: If your
 *  app moves to the background and this property is YES, the app should remain active
 *  in the background and continue monitoring this property. If the user quits the Audiobus
 *  app, and this property changes to NO, your app should immediately stop its audio engine
 *  and suspend, where appropriate.
 *
 *  See the [Lifecycle](@ref Lifecycle) section of the integration guide for
 *  futher discussion, or see the sample applications in the SDK distribution for example
 *  implementations.
 */
@property (nonatomic, readonly) BOOL audiobusAppRunning;

/*!
 * State input/output delegate
 *
 *  This delegate provides methods to save and load state specific to your
 *  app, in response to preset save and load operations from within Audiobus.
 *
 *  This feature is optional but recommended, as it allows your users to save
 *  and restore the state of your app as part of their workspace.
 */
@property (nonatomic, assign) id<ABAudiobusControllerStateIODelegate> stateIODelegate;

@end

#ifdef __cplusplus
}
#endif