//
//  MMPPdPartyGui.h
//  MobMuPlat
//
//  Created by diglesia on 11/29/15.
//

// This class is a modification of PdParty's "Gui" class, with certain class dependancies
// removed for compatibility, and new additional classes (for object and message boxes) added.

// Notice for original base code:
/*
 * Copyright (c) 2013 Dan Wilcox <danomatika@gmail.com>
 *
 * BSD Simplified License.
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 *
 * See https://github.com/danomatika/PdParty for documentation
 *
 */
#import "Widget.h"

#import "Log.h"
#import "Util.h"

// default pd gui font, loading custom fonts:
// http://stackoverflow.com/questions/11047900/cant-load-custom-font-on-ios
#define GUI_FONT_NAME @"DejaVu Sans Mono"

// pd gui wraps lines at 60 chars
#define GUI_LINE_WRAP 60

@class PdFile;

@interface Gui : NSObject

@property (strong, nonatomic) NSMutableArray *widgets;	// widget array
@property (assign, nonatomic) CGSize parentViewSize; // current view size

// pixel size of original pd patch
@property (assign, readonly, nonatomic) int patchWidth;
@property (assign, readonly, nonatomic) int patchHeight;

// font size loaded from patch
@property (assign, readonly, nonatomic) int fontSize;

// scale amount between view bounds and original patch size, calculated when bounds is set
@property (assign, readonly, nonatomic) float scaleX;
@property (assign, readonly, nonatomic) float scaleY;

// add widgets from an array of atom lines
- (void)addWidgetsFromAtomLines:(NSArray *)lines;

// add widgets from a pd patch
- (void)addWidgetsFromPatch:(NSString *)patch;

// reposition/resize widgets based on scale amounts & font size
- (void)reshapeWidgets;

#pragma Utils

// replace any occurrances of "//$0" or "$0" with the given patches' dollar zero id
- (NSString *)replaceDollarZeroStringsIn:(NSString *)string fromPatch:(PdFile *)patch;

// convert atom string empty values to an empty string
// nil, @"-", & @"empty" -> @""
+ (NSString *)filterEmptyStringValues:(NSString *)atom;

@end
