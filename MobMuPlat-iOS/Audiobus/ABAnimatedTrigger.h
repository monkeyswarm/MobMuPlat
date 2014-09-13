//
//  ABAnimatedTrigger.h
//  Audiobus
//
//  Created by Michael Tyson on 05/03/2013.
//  Copyright (c) 2011-2014 Audiobus. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "ABButtonTrigger.h"

/*!
 *  Animated trigger
 *
 *  This class implements a kind of [trigger](@ref ABTrigger) that appears as a button
 *  with an animated icon.
 */
@interface ABAnimatedTrigger : ABButtonTrigger

/*!
 * Create an animated trigger
 *
 * @param title A user-readable title (used for accessibility)
 * @param icon A icon of maximum dimensions 80x80, to use to draw the trigger button. This icon will be used
 *             as a mask to render the inset button effect. Icon size should be divisible by 2.
 * @param block Block to be called when trigger is activated
 */
+ (ABAnimatedTrigger*)animatedTriggerWithTitle:(NSString*)title initialIcon:(UIImage*)icon block:(ABTriggerPerformBlock)block;

/*!
 * Register a new animation frame
 *
 *  To animate, you use this method to register each frame associated with an
 *  identifier of your choice. You can register frames whenever you like, such as
 *  upon initialisation, or as you go.
 *
 *  Note that these frames are cached in memory, so you must ensure that you limit 
 *  the number of unique frames for your animation as much as is possible.
 *
 *  Icon should have maximum dimensions 80x80, and will be used to draw the trigger
 *  button. This icon will be used as a mask to render the inset button effect.
 *  Icon size should be divisible by 2.
 */
- (void)registerNewFrame:(UIImage*)frame withIdentifier:(uint32_t)identifier;

/*!
 * Retrieve an animation frame
 *
 *  You can use this method to determine whether a frame has been registered
 *  for a certain identifier, and inspect the image.
 *
 * @param identifier Frame identifier
 * @returns The frame associated with the given identifier, or nil
 */
- (UIImage*)frameForIdentifier:(uint32_t)identifier;

/*!
 * The current frame
 *
 *  To animate, you update this property for each frame of the animation, setting
 *  the value to an identifier that corresponds to a frame you added with
 *  @link registerNewFrame:withIdentifier: @endlink.
 *
 *  Do not update the icon more than five times a second.
 */
@property (nonatomic, assign) uint32_t currentFrameIdentifier;

/*!
 * The color
 *
 *  By default, icons are drawn in 50% grey.
 */
@property (nonatomic, strong) UIColor *color;

@end
