//
//  ABTrigger.h
//  Audiobus
//
//  Created by Michael Tyson on 16/05/2012.
//  Copyright (c) 2012 Audiobus. All rights reserved.
//

#ifdef __cplusplus
extern "C" {
#endif

#import <Foundation/Foundation.h>
#import "ABCommon.h"

@class ABTrigger;

/*!
 * Trigger perform block
 *
 * @param trigger The trigger being performed
 * @param ports   The port(s) of your app that the triggering peer is connected to. May be an empty set if triggered from the Audiobus app.
 */
typedef void (^ABTriggerPerformBlock)(ABTrigger *trigger, NSSet *ports);

/*!
 * @enum ABTriggerSystemType System trigger types
 *
 * @var ABTriggerTypeRecordToggle
 *      
 *  Toggle record. Appears as a circular button with the engraved word "REC", and
 *  turns red when in state ABTriggerStateSelected. When in state ABTriggerStateAlternate,
 *  appears with a green colour to indicate a 'primed' state.
 *
 * @var ABTriggerTypePlayToggle
 *
 *  Toggle playback. Appears as a triangle (standard transport play symbol) when
 *  in state ABTriggerStateNormal, and two vertical bars (pause symbol) when in
 *  state ABTriggerStateSelected.
 *
 * @var ABTriggerTypeRewind
 *
 *  Rewind button. Appears as a triangle pointing to the left, with a vertical bar
 *  at the apex.
 */
typedef enum {
    ABTriggerTypeRecordToggle = 1,
    ABTriggerTypePlayToggle,
    ABTriggerTypeRewind,
    
    kABTotalTriggerTypes
} ABTriggerSystemType;

/*!
 * @enum ABTriggerState Trigger states
 */
typedef enum {
    ABTriggerStateNormal,
    ABTriggerStateSelected,
    ABTriggerStateDisabled,
    ABTriggerStateAlternate
} ABTriggerState;

/*!
 *  Trigger
 *
 *  This class defines actions that can be performed on your app by other Audiobus apps.
 *  Triggers you define and add to the Audiobus controller via 
 *  @link ABAudiobusController::addTrigger: addTrigger: @endlink
 *  will be displayed within the Audiobus Connection Panel for other apps.
 *
 *  You can use a [system trigger type](@ref ABTriggerSystemType), or define your own
 *  custom triggers.
 */
@interface ABTrigger : NSObject

/*!
 * Create a trigger with a system type
 *
 * You should use this method as much as possible. Only use 
 * @link triggerWithTitle:icon:block: @endlink
 * if it is *absolutely* necessary that you create a custom trigger type.
 *
 * System triggers are automatically ordered in the connection panel as follows:
 * ABTriggerTypeRewind, ABTriggerTypePlayToggle, ABTriggerTypeRecordToggle.
 *
 * @param type One of the system type identifiers
 * @param block Block to be called when trigger is activated
 */
+ (ABTrigger*)triggerWithSystemType:(ABTriggerSystemType)type block:(ABTriggerPerformBlock)block;

/*!
 * Create a custom trigger
 *
 * @param title A user-readable title (used for accessibility)
 * @param icon A icon of maximum dimensions 80x80, to use to draw the trigger button. This icon will be used
 *             as a mask to render the inset button effect. Icon size should be divisible by 2.
 * @param block Block to be called when trigger is activated
 */
+ (ABTrigger*)triggerWithTitle:(NSString*)title icon:(UIImage*)icon block:(ABTriggerPerformBlock)block;

/*!
 * Set the title for a given state
 *
 * @param title User-readable title (used for accessibility)
 * @param state State to apply title to
 */
- (void)setTitle:(NSString*)title forState:(ABTriggerState)state;

/*!
 * Set the icon for a given state
 *
 * @param icon A icon of maximum dimensions 80x80, to use to draw the trigger button. This icon will be used
 *             as a mask to render the inset button effect. Icon size should be divisible by 2.
 * @param state State to apply icon to
 */
- (void)setIcon:(UIImage*)icon forState:(ABTriggerState)state;

/*!
 * Set the color for a given state
 *
 *  By default, normal state icons are drawn in 50% grey, selected icons are drawn in 20% grey
 *  unless a custom selected state icon is provided, in which case it is also drawn in 50% grey.
 *  Alternate state icons are drawn in green. Triggers with system state ABTriggerTypeRecordToggle
 *  are drawn in red.
 *
 * @param color The color to use to render the icon for the given state
 * @param state State to apply color to
 */
- (void)setColor:(UIColor*)color forState:(ABTriggerState)state;

/*!
 * Trigger state
 *
 *  Updates to this property will affect the corresponding UI in connected applications.
 */
@property (nonatomic, assign) ABTriggerState state;

@end

#ifdef __cplusplus
}
#endif