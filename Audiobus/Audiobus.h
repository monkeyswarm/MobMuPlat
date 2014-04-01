//
//  Audiobus.h
//  Audiobus
//
//  Created by Michael Tyson on 10/12/2011.
//  Copyright (c) 2011 Audiobus. All rights reserved.
//

#import "ABCommon.h"
#import "ABInputPort.h"
#import "ABOutputPort.h"
#import "ABFilterPort.h"
#import "ABAudiobusController.h"
#import "ABAudiobusAudioUnitWrapper.h"
#import "ABPeer.h"
#import "ABPort.h"
#import "ABTrigger.h"
#import "ABAnimatedTrigger.h"
#import "ABLiveBuffer.h"
#import "ABMultiStreamBuffer.h"

#define ABSDKVersionString @"1.0.2"

/*!
@mainpage

@section Introduction

 Audiobus is a protocol (and accompanying controller app) that allows iOS apps to stream audio
 to one another. Just like audio cables, Audiobus lets you connect apps together as modules, to 
 build sophisticated audio production and processing configurations.

 This SDK implements the Audiobus protocol and provides all you need to make your app Audiobus
 compatible.  It's designed to be extremely easy to use: depending on your app, you should be
 up and running with Audiobus well within a couple of hours. It will work with any audio format, and
 automatically converts audio to and from the Audiobus line format.

 The SDK contains:

- The Audiobus library and headers
- An Xcode project with a number of sample apps that you can build and run
- A README file with a link to this documentation

@section Dont-Panic Don't Panic!

 We've worked hard to make Audiobus a piece of cake to integrate. Most developers will be able
 to have a functional integration within thirty minutes. Really.

 If your app's based around Remote IO audio units, then there's very little you'll need to
 do, particularly if it just produces audio and doesn't record it. This document will take you 
 through the process of integrating Audiobus using the Audio Unit Wrapper, a class we provide 
 that will do almost all of the work for you.
 
 The process involves:
 - [Setting up your project](@ref Project-Setup),
 - enabling [background audio](@ref Background-Audio),
 - creating a [launch URL](@ref Launch-URL) and [registering your app](@ref Register-App),
 - making sure [Audio Session mixing is enabled](@ref Enable-Mixing),
 - getting access to your app's [Audio Unit](@ref Expose-Audio-Unit),
 - then [creating instances](@ref Create-Properties) of the Audiobus Controller, 
   [input](@ref Instantiate-Input-Port) and/or [output](@ref Instantiate-Output-Port)
   ports, and the [Audio Unit Wrapper](@ref Audio-Unit-Wrapper) from your app delegate.

 Easy-peasy.

 If you aren't using Remote IO, or you have an app that records audio and you want to do
 something special, then you'll need to do a little more work, and interact with the
 [Audiobus output and/or input ports](@ref Sending-Receiving) directly, after
 [setting up your project](@ref Project-Setup) - but don't worry, it's still pretty simple.

 If you wanna do some more advanced stuff, like [receiving individual audio streams separately](@ref Receive-Streams),
 exposing multiple audio [ports](@ref Ports), allowing your app to be [controlled remotely](@ref Triggers),
 sending and receiving [metadata](@ref Metadata), or building [Audiobus filters](@ref Filtering), then this document
 will explain how it's done, too.

 Finally, if you need a little extra help, or just wanna meet and talk with us or other
 Audiobus-compatible app developers, come say hello on the [developer community forum](http://heroes.audiob.us).

@section Capabilities Capabilities: Inputs, Outputs and Filters

 <img src="overview.png" style="float: right; margin: 20px;" title="Audiobus Peers and Ports" />
 
 Audiobus defines three different capabilities that an Audiobus-compatible app can have: Sending,
 Receiving and Filtering. Your app can perform several of these roles at once. You create input,
 output and/or filter ports when your app starts, and/or as your app's state changes.

 **Inputs** have ports that transmit audio to other apps (receivers or filters). A sender will
 typically send the audio that it's currently playing out of the device's audio output device. For
 example, a musical instrument app will send the sounds the user is currently playing.

 **Outputs** have ports that accept audio from other sender or filter apps. What is done with
 the received audio depends on the app. A simple recorder app might just save the recorded audio
 to disk. A multi-track recorder might save the audio from each sender app as a separate track. An
 audio analysis app might display information about the nature of the received audio, live.

 **Filters** accept audio input, process it, then send it onwards to another app over Audiobus. This
 allows applications to apply effects to the audio stream.

 <img src="mixed-or-separate.png" style="float: right; margin: 20px;" title="Mixed or separate audio streams" />
 
 Output apps can send to one or more destinations at once. Similarly, inputs can receive from one
 or more sources, and filters can accept audio from multiple sources, and send to multiple destinations.
 
 Outputs can receive audio from connected source apps in two ways: Mixed down to a single stereo stream,
 or with one stereo stream per connected source.
 
 By setting the [receiveMixedAudio](@ref ABInputPort::receiveMixedAudio) property of the port to YES
 (the default), the port will automatically mix all the sources together, giving your application one
 stereo stream.
 
 If you set the property to NO, the port will offer you separate streams, one per connected app. This
 can be useful for providing users with per-app mixing controls, or multi-track recording.

 <div style="clear: both;"></div>

@section More-Help More Help
 
 If you need any additional help integrating Audiobus into your application, or if you have
 any suggestions, then please join us on the [developer community forum](http://heroes.audiob.us).
 
@page Integration-Guide Integration Guide

 <blockquote>
 Please read this guide carefully, as there are some important things you need to
 know to effectively support Audiobus in your app. Particularly if you intend to receive
 audio from Audiobus, set aside ten or twenty minutes to read through to make sure you have a
 clear picture of how it all works.
 </blockquote>
 
 Many app developers will be able to implement Audiobus in just thirty minutes or so.

 If your app is based upon the Remote IO audio unit, then you have the option of using the
 [Audiobus Audio Unit Wrapper](@ref ABAudiobusAudioUnitWrapper). This does most of the
 work for you, intercepting playing audio and sending it over Audiobus, for senders, and
 simulating audio input when connected to Audiobus as a receiver. It will also automatically
 convert whatever audio format your app uses to and from the Audiobus line format, including
 doing sample rate conversion, so you don't need to think about audio formats.

 If your requirements are relatively simple, then the Audio Unit Wrapper is a very fast way
 to begin supporting Audiobus.
 
 > You can either use the Audio Unit Wrapper to handle sending/receiving for you, for
 > a given set of ports, or you can interact with those ports directly. **You cannot do both**:
 > You may not use the Audio Unit Wrapper with a particular port, and also send or receive with that
 > port manually. You may use some ports with the Audio Unit Wrapper, and some without, though.

 This quick-start guide assumes your app uses Remote IO. If this is not the case, most of it
 will still be relevant, but you'll need to read on to learn about [Sending](@ref Audiobus-Output)
 and [Receiving](@ref Audiobus-Input) using the API directly. Don't worry - this is still
 very straightforward.

 If you have a more complicated app and want to do things like exposing [multiple ports](@ref Ports),
 [receiving multi-channel audio](@ref Receive-Streams), or if you just choose not to use the
 Audio Unit Wrapper, then you can interact with the input and output ports directly.
 These easy-to-use classes provide C functions to transmit and receive audio. See
 [Common Recipes](@ref Recipes), [Sending](@ref Audiobus-Output) and [Receiving](@ref Audiobus-Input)
 for more info.
 
 If you want to create a filter app, then see [Filtering](@ref Filtering), as the procedure is
 different to implementing a sender or receiver app.
 
@section General-Principles General Design Principles
 
 We've worked hard to make Audiobus as close as possible to an "it just works" experience for 
 users. We think music on iOS should be easy and open to everyone, not just those technical 
 enough to understand convoluted settings.
 
 That means you should add no switches to enable/disable Audiobus, no settings that users need 
 to configure to enable your app to run in the background while connected to Audiobus.

 If you're a sender app or a filter app (i.e. you have an ABOutputPort and/or an 
 ABFilterPort, and only send audio to other apps or filter audio from other apps), you shouldn't
 need to ever add any Audiobus-specific UI. Audiobus takes care of all session management for
 you. If you're a receiver app (you have an ABInputPort) then unless you're doing nifty things
 with multitrack recording, you shouldn't need to add Audiobus UI either.
 
 Additionally, you should not offer Audiobus support as an in-app purchase, as this violates the
 "just works" principle.  We would be unable to list such apps in our Compatible Applications
 directory due to the customer frustration and support requests this would generate.
 
 <blockquote class="alert">
 We reserve the option to remove apps offering Audiobus support as an in-app purchase from the
 Audiobus Compatible Apps directory, or to ban them from Audiobus entirely.
 </blockquote>
 
 We've also worked hard to make Audiobus a robust and smart transport protocol, with automatic 
 latency adjustment, live audio stutter repair, seamless audio format and sample rate 
 conversion and other niceties. So, generally speaking, there shouldn't be much menial work 
 that you need to do to make it work.
 
 Audiobus' output port is extremely light when not connected: The send function ABOutputPortSendAudio 
 will consume a negligible amount of CPU, so you can use it even while your user isn't using Audiobus.
 
 If you find yourself implementing stuff that seems like it should've been in Audiobus, tell us. 
 It's probably already in there. If it's not, we'd be happy to consider putting it in ourselves 
 so you, and those who come after you, don't have to.
 
 In short: Whenever possible, keep it simple. Your users will thank you, and you'll have more 
 development time to devote to the things you care about.

@section iOS-Audio-Errors A Note About kAudioSessionIncompatibleCategory Error on iOS 5 and 6
 
 There is bug apparent on both iOS 5 and 6 (but not iOS 7) that manifests in apps that
 use the PlayAndRecord audio session category under certain multitasking conditions. The issue is triggered 
 reliably via the following steps:
 
 1. Open one app that uses both PlayAndRecord and background audio.
 2. Open a second app that also uses PlayAndRecord and background audio.
 3. Quit the first app, via the multitasking bar.
 4. Open the first app again. The app will experience the error.
 
 The error can also be caused by launching certain combinations of apps.
 
 It results in a completely inoperable audio session, as well as a
 `kAudioSessionIncompatibleCategory` ('!cat') error when querying the 
 `kAudioSessionProperty_CurrentHardwareInputNumberChannels` audio session property.
 
 Once the error appears, the problem can only be resolved by quitting some or all running audio apps - unless
 the workaround described below is used.
 
 See [rdar://13022588](http://openradar.appspot.com/13022588) for further description.
 
 We strongly recommend writing code to recognize this error state, and responds appropriately: First by
 implementing the workaround described below, and in the event of a failure in the workaround, informing
 the user accordingly, recommending that the user relaunches their apps.
 
 Here's our proposed message to the user:
 
 <blockquote class="plain">
 You’ve discovered a known issue with the iOS audio system that we have no control over.
 
 Please relaunch all your music apps, making sure you launch the Audiobus app first.
 </blockquote>
 
 You can frequently recover from this error by deactivating and reactivating the audio session:
 
 @code
 AudioSessionSetActive(false);
 AudioSessionSetActive(true);
 @endcode
 
 This seems to resolve the issue when it arises sometimes, but at other times, it has no effect.
 
 Nevertheless, we recommend making use of it to mitigate some of the effects of the bug, until Apple fix it.
 
 The implementation of the workaround is simple: Early in your app's audio initialization, check the
 `kAudioSessionProperty_CurrentHardwareInputNumberChannels` property. If you get a 
 `kAudioSessionIncompatibleCategory` error, restart the audio session:
 
 @code
 UInt32 channels;
 OSStatus result = AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareInputNumberChannels, &size, &channels);
 if ( result == kAudioSessionIncompatibleCategory ) {
    // Audio session error (rdar://13022588). Power-cycle audio session.
    AudioSessionSetActive(false);
    AudioSessionSetActive(true);
    result = AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareInputNumberChannels, &size, &channels);
    if ( result != noErr ) {
        NSLog(@"Got error %d while querying input channels", result);
    }
 }
 @endcode
 
@section Preparation 1. Determine if your app will work with Audiobus
 
 Audiobus relies heavily on multitasking, and one thing that is vital in apps that work together is
 that they are able to perform adequately alongside other apps, in a low-latency Audiobus environment.
 
 The primary factor affecting whether your app will work with Audiobus is whether your app can perform
 properly with a hardware IO buffer duration of 5ms (256 frames at 44.1kHz, 128 frames at 22kHz, etc)
 while other apps are running.
 
 **Your app *must* be prepared to handle a buffer length of 5ms (256 frames), when running alongside other apps,
 without glitching, on the iPad 3 and above, or iPhone 5 and above**.  You can test this prior to beginning 
 implementation of Audiobus support by opening the Audiobus app, with your app closed, then opening your app
 afterwards, which should force your app to a 5ms buffer duration. Push your app hard, and listen for glitches
 in the audio output. Ideally, you should also test while running additional audio apps in the background.
 
 <blockquote class="alert">
 If your app does not support a hardware buffer duration of 5ms without demonstrating performance problems
 on the iPad 3 and up, or the iPhone 5 and up, then we reserve the option to not list it in the Audiobus-compatible 
 app listing on our website and within the Audiobus app, or to ban it from Audiobus entirely.
 </blockquote>
 
@section Project-Setup 2. Project Setup

 Audiobus is distributed as a static library, plus the associated header files. Follow these
 steps to set up your project with Audiobus:

1. Copy libAudiobus.a and the associated header files into an appropriate place within
   your project directory.
2. Drag libAudiobus.a into the "Frameworks" group of your project (or in another group,
   if you prefer). In the sheet that appears, select your app target.
3. Open up your app's build settings, by clicking on your app's project icon in the
   project navigator, selecting your app target, and clicking the "Build Settings" tab.
   Locate the "Header Search Paths" parameter, and add the path to the folder you placed
   the Audiobus header files in.  For example, `"$(SRCROOT)/Library/Audiobus"`.
4. Ensure the following frameworks are added to your build process (to add frameworks,
   select your app target's "Link Binary With Libraries" build phase, and click the "+"
   button):
   - CoreGraphics
   - Accelerate
   - AudioToolbox
   - QuartzCore
   - Security

<blockquote class="alert" id="audiobus-ios7-bug">
 Due to an iOS 7 bug, in order to make your app work with Audiobus you must ensure that:
 
 1. Your app's Bundle Name is identical to your app's Product Name.
 2. Both Bundle Name and Product Name are less than 16 characters in length.
 
 If you do not adhere to these limitations, you will see an Audiobus error dialog appearing
 within your app informing you that "There was a problem setting up Audiobus communication".
 
 Note that you can safely change these values without causing problems with an already-live
 app on the App Store. These changes will not be visible to users.
</blockquote>
 
<blockquote class="alert" id="audiobus-ios7-status-bar-bug">
 Due to another iOS 7 issue, it's important that you do not use the UIViewController property
 'prefersStatusBarHidden' to hide the iOS status bar. This will result in the Audiobus Connection
 Panel becoming unresponsive when placed on the left side of the screen.
 
 Instead, we recommend adding a `UIViewControllerBasedStatusBarAppearance` entry to your app's
 Info.plist with the boolean value NO, then hide your status bar as under iOS 6, with a
 `UIStatusBarHidden` entry in Info.plist, or via UIApplication's 'statusBarHidden' property.
</blockquote>

@section Background-Audio 3. Enabling Background Audio

 If you haven't already done so, you must enable background audio in your app, to allow
 it to run while in the background.
 
 To do so:

 1. Open your app's `Info.plist` editor, by clicking your app's project icon in the Project
    Navigator again, selecting your app target, and clicking the "Info" tab.
 2. Right-click in the table at the top, and select "Add Row".
 3. Select "Required Background Modes" from the Key menu that appears.
 4. Click the disclosure triangle at the left of the new row to open the sub-items list.
 5. Double-click in the "Value" column for "Item 0", and enter the text "audio" (without
    the quotes).

 > Your app will only continue to run in the background if you have an *active, running*
 > audio system. This means that if you stop your audio system while your app is in the background
 > or moving to the background, your app will cease to run and will become unresponsive to
 > Audiobus.
 
 Consequently, care must be taken to ensure your app is running and available when it needs to be.
 
 Firstly, **you must ensure you have a running and active audio session** once your app is connected
 via Audiobus, regardless of the state of your app. You can do this two ways: 
 
 1. Make sure you only instantiate the Audiobus controller ([Step 9](@ref Instantiate-Controller)) once 
 your audio system is running.
 2. Register to receive [ABConnectionsChangedNotification](@ref ABConnectionsChangedNotification)
 notifications, and start your audio engine if the Audiobus controller is 
 [connected](@ref ABAudiobusController::connected) (see [sample code](@ref Application-State)).
 
 If do not do this correctly, your app may suspend in the background before an Audiobus connection 
 has been completed, rendering it unable to work with Audiobus.
 
 Secondly, you may choose to suspend your app (by stopping your audio system) when it moves to the
 background under certain conditions. For example, you might have a 'Run in Background' 
 setting that the user can disable, or you may choose to always suspend your app if the app 
 is idle.
 
 This is fine - in fact, we recommend doing this by default, in order to avoid the
 possibility of overloading a user's device without their understanding why.
 
 If you do this however, you **must not** under any circumstances suspend your app if the
 [connected](@ref ABAudiobusController::connected) property of the Audiobus controller is
 YES. If you do, then Audiobus will **cease to function properly** with your app.
 
 For example:
 
 @code
 -(void) applicationDidEnterBackground:(NSNotification *)notification {
    if ( ![[NSUserDefaults standardUserDefaults] boolForKey:@"Play in Background"]
            && !_audiobusController.connected ) { // This part is important
     
        [_audioController stop];
    }
 }
 @endcode
 
 We also highly recommend shutting down your app in the background *10 seconds* after it
 has been disconnected from Audiobus. If a reconnection happens within that interval,
 the app should continue running.
 
 You can see a [sample implementation](@ref Application-State) of this in the Common
 Recipes section or within the sample projects bundled with the SDK distribution, and 
 further discussion in the [Background Mode](@ref Background-Mode) section.
 
 Note that during development, if you have not yet registered your app with Audiobus
 ([Step 5](@ref Register-App)), the Audiobus app will only be able to see your app while
 it is running. Consequently we **strongly recommend** registering your app before you 
 begin testing.

@section Launch-URL 4. Set up a Launch URL

 Audiobus needs a URL (like `YourApp.audiobus://`) that can be used to launch and switch to
 your app.
 
 The URL scheme needs to end in ".audiobus", to ensure that Audiobus app URLs are unique.
 Please be sure to use this exact URL scheme in [Step 9](@ref Instantiate-Controller), as
 your app will not work properly with Audiobus otherwise.
 
 Here's how to add the new URL scheme to your app.

1. Open your app's `Info.plist` editor, by clicking your app's project icon in the Project
   Navigator again, selecting your app target, and clicking the "Info" tab.
2. If you don't already have a "URL Types" section, click the "Add" button at the bottom 
   right, and select "Add URL Type". Then expand the URL Type you just created and
   enter an identifier for the URL (a reverse DNS string that identifies your app, like 
   "com.yourcompany.yourapp", will suffice).
3. If you already have existing URL schemes defined for your app, add a comma and space (", ") 
    after the last one in URL Schemes field (Note: The space after the comma is important).
4. Now enter the new Audiobus URL scheme for your app, such as "YourApp.audiobus". Note
   that this is just the URL scheme, not including the "://" characters).

<img src="url-scheme.png" title="Adding a URL Scheme" />

 Other apps will now be able to switch to your app by opening the `YourApp.audiobus://` URL.

@section Register-App 5. Register your app and generate your API key

 Audiobus contains an app registry which is used to enumerate Audiobus-compatible apps that
 are installed. This allows apps to be seen by Audiobus even if they are not actively running
 in the background.
 
 Register your app, and receive an Audiobus API key, at the 
 [Audiobus app registration page](http://developer.audiob.us/new-app).

 After you register, we will review your application. Upon approval, you will be notified via
 email, which will include your Audiobus API key, and the app will be added to the Audiobus registry.
 
 You can always look up your API key by visiting http://developer.audiob.us/apps and clicking on your
 app. The API key is at the bottom of the app details page.
 
 The app-specific API key is a string that you provide when you use the Audiobus SDK. It will
 be checked by the SDK upon initialization. No network connection is required to verify the key.
 
 > Note that while registering your app will *not* cause it to appear on our website or in the "Apps"
 > tab in the app, it *will* cause it to appear within the XML feed that Audiobus downloads
 > to keep track of which of the installed apps support Audiobus.
 > 
 > This will not cause your app to appear within Audiobus' app listings, because you chose a new, unique 
 > URL in [Step 4](@ref Launch-URL), but a dedicated user with a packet sniffer may see your app in the 
 > XML stream. Additionally, while we do not make the URL to this feed public, the feed itself is 
 > publicly-accessible.

 The Audiobus app downloads registry updates from our servers once every 30 minutes, so once we approve
 your submission, we recommend that you reinstall the Audiobus app to force it to update immediately,
 so you can begin working.
 
 > To make your app appear on the Audiobus website or in the in-app Compatible Apps directory, and therefore
 > have Audiobus users given the option to purchase your app, you need to you make your app live 
 > ([Step 12](@ref Go-Live)). Do this only when the Audiobus-compatible
 > version of your app goes live on the App Store, so as not to confuse users.
 
@section Enable-Mixing 6. Enable mixing audio with other apps

 When you use audio on iOS, you typically select one of several audio session categories,
 usually either `kAudioSessionCategory_PlayAndRecord` or `kAudioSessionCategory_MediaPlayback`.

 By default, both of these categories will cause iOS to interrupt the audio session of any other
 app running at the time your app is started, **forcing the other app to suspend**.

 If you are using either `PlayAndRecord` or `MediaPlayback`, then in order to use Audiobus you
 need to **override this default**, and tell iOS to allow other apps to run at the same time and
 mix the output of all running apps.

 To do this, you need to set the `kAudioSessionProperty_OverrideCategoryMixWithOthers` flag, like so:

 @code
 #import <AudioToolbox/AudioToolbox.h>
 ...
 UInt32 allowMixing = YES;
 AudioSessionSetProperty(kAudioSessionProperty_OverrideCategoryMixWithOthers, sizeof (allowMixing), &allowMixing);
 @endcode

 You need to do this every time you change your audio session category, as the property is reset
 with every category change.

 Note also that due to an as-yet not well understood iOS bug, adjusting other session properties such as
 `kAudioSessionProperty_OverrideCategoryDefaultToSpeaker` can interfere with this property setting,
 causing other apps to be interrupted despite the mix property being set.

 Consequently, be sure to reset the `kAudioSessionProperty_OverrideCategoryMixWithOthers` property value
 whenever you assign any audio session properties.

@section Expose-Audio-Unit 7. Expose your Audio Unit

 You'll need to get access to your IO AudioUnit, in order to hand it to the Audio Unit Wrapper.

 How you do this depends on your project's setup. It might involve adding a property to your
 audio controller class, for example.

@section Create-Properties 8. Create Audiobus Controller and Audio Unit Wrapper Properties

 Next, you need to create properties for an instance of the Audiobus Controller, and an
 instance of the Audio Unit Wrapper.  A convenient place to do this is in your app's
 delegate.

 First, import the Audiobus headers from your app delegate's header (or the implementation
 file, if you like):

 @code
 #import "Audiobus.h"
 @endcode

 Next declare strong (retaining) properties for the two instances:

 @code
 @property (strong, nonatomic) ABAudiobusController *audiobusController;
 @property (strong, nonatomic) ABAudiobusAudioUnitWrapper *audiobusAudioUnitWrapper;
 @endcode

 ...And, optionally, synthesize them from your \@implementation block:

 @code
 @synthesize audiobusController = _audiobusController;
 @synthesize audiobusAudioUnitWrapper = _audiobusAudioUnitWrapper;
 @endcode

@section Instantiate-Controller 9. Instantiate the Audiobus Controller

 Now you'll need to create an instance of the Audiobus controller. A convenient place to do this
 is in your app delegate's `application:didFinishLaunchingWithOptions:` method, but there are three
 very important caveats:
 
 First: You must either **start your audio system before initializing Audiobus**, or you must watch for
 @link ABConnectionsChangedNotification @endlink and **start your audio system when the ABConnectionsChangedNotification
 is received** (see [sample code](@ref Application-State)).
 This is because as soon as your app is connected via Audiobus, your app **must have a running and active
 audio system**, or a race condition may occur wherein your app may suspend in the background
 before an Audiobus connection has been completed.
 
 Second: You must instantiate the Audiobus controller **on the main thread only**. If you do not, Audiobus
 will trigger an assertion.
 
 Third: You **must not hold up the main thread after initializing the Audiobus controller**. Due to
 a bug in Apple's service browser code, if the main thread is blocked for more than a couple of seconds,
 Audiobus peer discovery will fail, causing your app to refuse to respond to the Audiobus app. If you
 need to take more than a second or two to initialize your app, initialize the Audiobus controller afterwards.
 
 Create the instance, passing it the launch URL you created in [Step 4](@ref Launch-URL), and your API
 key that you generated when you registered your app in [Step 5](@ref Register-App).

 > You must use the exact URL you created in Step 4. If you do not pass the URL correctly, your
 > app will not work properly with Audiobus.
 
 With ARC:

@code
 self.audiobusController = [[ABAudiobusController alloc]
                                      initWithAppLaunchURL:[NSURL URLWithString:@"YourApp.audiobus://"]
                                                    apiKey:@"YOUR-API-KEY"];
 @endcode

 Or, without ARC:

 @code
 self.audiobusController = [[[ABAudiobusController alloc]
                                     initWithAppLaunchURL:[NSURL URLWithString:@"YourApp.audiobus://"]
                                                   apiKey:@"YOUR-API-KEY"]
                                         autorelease];
 @endcode

 At certain times, Audiobus will display the Connection Panel within your app. This is a slim
 panel that appears at the side of the screen, that users can drag off the screen, and swipe
 from the edge of the screen to re-display, a bit like the iOS notification screen.

 By default, the Connection Panel appears at the right of the screen. If this does not work
 well with your app's UI, you can select [another location](@ref ABAudiobusConnectionPanelPosition)
 for the panel:

 @code
 self.audiobusController.connectionPanelPosition = ABAudiobusConnectionPanelPositionLeft;
 @endcode

 You can change this value at any time (such as after significant user interface orientation changes),
 and Audiobus will automatically animate the panel to the new location.
 
 > On iOS 7 and up, if the connection panel is on the bottom of the screen, it cannot be hidden by
 > the user. This is to avoid interference by the new Control Center panel.

@section Instantiate-Wrapper 10. Create Ports and Instantiate and Configure the Audiobus Audio Unit Wrapper

 Now you're ready to create a few ports, and create the Audio Unit Wrapper.

 @subsection Instantiate-Output-Port Output Port
 
 If you intend to send audio, then you'll need to create an output port.

 The first output port you define will be the one that Audiobus will connect to when the user taps your app
 in the port picker within Audiobus, so it's best to define the port with the most general, default behaviour
 first.

 You provide a port name, for internal use, and a port title and optionally an icon (32x32), which are
 displayed to the user, and can both change dynamically, if necessary. You can localize the port title.

 @code
 ABOutputPort *output = [self.audiobusController addOutputPortNamed:@"Audio Output"
                                                              title:NSLocalizedString(@"Main App Output", @"")];
 @endcode
 
 > If you are using an output port and *not* using the Audio Unit Wrapper, you **must** handle system audio
 > output appropriately, muting or playing your app's audio output depending on the presence of the
 > @link ABInputPortAttributePlaysLiveAudio @endlink flag on the input port's
 > @link ABInputPort::attributes attributes @endlink property. See [Designated Output](@ref Designated-Output)
 > for details.

 @subsection Instantiate-Input-Port Input Port
 
 Similarly, if you intend to receive audio, then create an input port:

 @code
 ABInputPort *input = [self.audiobusController addInputPortNamed:@"Audio Input"
                                                           title:NSLocalizedString(@"Main App Input", @"")];
 @endcode
 
 Next, you may need to configure the input port to work with your app.

 If your app produces audio output (via a render callback, for example) that is based on the audio input - such as live audio
 monitoring in a recorder app, or live output from an audio effects processor app - then you **must** inform Audiobus of this
 fact.
 
 If you produce audio output based on the audio input, and do not tell Audiobus this fact, then the app from which you are
 receiving audio will continue to produce audio through the device's audio output, which results in two copies of the same
 audio being played: One from the source app, and one from your app.
 
 <img src="plays-live-audio.png" title="Demonstration of the ABInputPortAttributePlaysLiveAudio flag" />
 
 You can tell Audiobus that your app produces audio output based on the audio received from the input port by setting
 the @link ABInputPortAttributePlaysLiveAudio @endlink flag on the input port's
 @link ABInputPort::attributes attributes @endlink property:
 
 @code
 input.attributes = ABInputPortAttributePlaysLiveAudio;
 @endcode
 
 This will cause sources connected to your input port to mute their output to avoid duplicate audio
 (see [Designated Output](@ref Designated-Output) for further information).
 
 > Only set this attribute if you produce and play audio based on the audio input. If set this attribute, and
 > don't output some form of the audio coming in, the user will not be able to hear anything.
 
 If you stop or start playing output based on the input while your app is running, you must also update the `attributes
 ` property accordingly. You can do this at any time.
 
 If you intend to [create more than one port](@ref Ports), it's recommended that you provide icons for each.
 These will be displayed to the user in various locations, and aid the user in comprehending the function of
 your app's ports. Port icons should be 32x32 pixel images, which will be used as a mask to draw styled icons.
 You may change port icons dynamically, and the user interface across all connected devices and apps will
 change accordingly.

 @code
 input.icon = [UIImage imageNamed:@"Port-Icon.png"];
 @endcode
 
 If you wish to receive multi-channel audio, with one audio stream for each connected app, then you'll need
 to interact with the [input port directly](@ref Receive-Streams). If this is the case, you can simply
 pass `nil` to the Audio Unit Wrapper for the input parameter.

 @subsection Audio-Unit-Wrapper Audio Unit Wrapper
 
 Now create the Audio Unit Wrapper, passing in the primary output and/or input ports (pass `nil` for any
 you don't need).

 @code
 self.audiobusAudioUnitWrapper = [[ABAudiobusAudioUnitWrapper alloc]
                                     initWithAudiobusController:self.audiobusController
                                                      audioUnit:self.audioController.audioUnit
                                                         output:output
                                                          input:input];
 @endcode
 
 If you're not using ARC, don't forget the `autorelease`, and set the `audiobusController`
 and `audiobusAudioUnitWrapper` properties to `nil` in your `dealloc` method.
 
 > You can either use the Audio Unit Wrapper to handle sending/receiving for you, for
 > a given set of ports, or you can interact with those ports directly. **You cannot do both**:
 > You may not use the Audio Unit Wrapper with a particular port, and also send or receive with that
 > port manually.

 If your app doesn't record for later playback and only plays the audio it receives out of the 
 speaker or headphone, then low latency is probably going to be a higher priority than perfect, lossless
 audio. In that case, set the
 [useLowLatencyInputStream](@ref ABAudiobusAudioUnitWrapper::useLowLatencyInputStream) property 
 to `YES`. 
 
 <blockquote class="alert">
 Never use the low-latency audio stream (either via ABAudiobusAudioUnitWrapper's useLowLatencyInputStream
 property, or via [ABInputPortReceiveLive](@ref ABInputPort::ABInputPortReceiveLive)) for *recording*.
 This stream is not designed to be recorded, and if you attempt this, you will record glitchy audio.
 
 Use only the lossless Audiobus stream for recording.
 </blockquote>
 
 If you need to record audio *and* perform audio monitoring, you may use both streams separately, or
 use the lossless stream for recording, then pass the audio through ABLiveBuffer to apply latency-adjustment
 and error-concealing for live monitoring.
 
 See the [discussion on lossless vs. low-latency audio](@ref Lossless-vs-Low-Latency)
 for more information.
 
 @subsection Input-Callbacks An Important Note About Receiving Input With The Wrapper
 
 Receiving input via the Remote IO audio unit can be done in two ways:
 
 1. By registering an input callback via `kAudioOutputUnitProperty_SetInputCallback` and calling
    `AudioUnitRender` from within it. This is the easiest, and recommended, technique for use with 
    Audiobus. Or,
 2. by calling `AudioUnitRender` from within a render callback, registered via
    `kAudioUnitProperty_SetRenderCallback` or `AUGraphSetNodeInputCallback`.
 
 @subsubsection Input-Callback-Method Input Callback Method
 
 If you use the first technique in your app (`kAudioOutputUnitProperty_SetInputCallback`), then there isn't
 a great deal you need to do: ABAudiobusAudioUnitWrapper will do everything for you. 
 
 **However, be aware that your input callback may be called multiple times per render interval**. This will 
 happen at times when the Audiobus audio stream has stalled, due to factors such as WiFi interference for 
 network connections, and is catching up.  You can test your app under this scenario by using it with the
 "AB Torture Test" sample app as a source.
 
 Additionally, if your app records audio in a fashion where timing is important - such as recording against
 other tracks, etc - then you must use the `AudioTimeStamp` provided to you in the input callback to
 align your recorded audio correctly in your app's timeline. If you do not, your app will suffer from laggy 
 recording via Audiobus under certain circumstances.
 
 > If timing is important in your app, it is **vital** that you use this timestamp correctly.
 > If you do not, users will experience lag when recording in your app.
 
 See the section [Receivers, Use Audio Timestamps](@ref Receivers-Timestamps) for further
 discussion on timestamps and latency compensation.
 
 @subsubsection Render-Callback-Method Render Callback Method
 
 **If, however, you are receiving audio from a render callback, there are two very important things you
 need to know**:
 
 Firstly, there may be times if you are using lossless audio
 ([useLowLatencyInputStream](@ref ABAudiobusAudioUnitWrapper::useLowLatencyInputStream) is NO) when
 the Audiobus connection stalls - due to WiFi interference, for example. If this happens, your app
 will experience a period during which 0 frames are returned from Audiobus, followed by a 
 greater-than-normal number of frames as the audio stream catches up.
 
 If you continue to use `AudioUnitRender` as normal, then **this will result in a period of silence,
 followed by increased latency** as your app is never able to catch up again. Audio, recorded audio
 in particular, may be glitchy if you do not address this, like the following:
 
 @htmlonly
 <audio src="AudioUnitRender-Incorrect.wav" controls></audio>
 <a href="AudioUnitRender-Incorrect.wav">Glitchy audio</a>
 @endhtmlonly
 
 When correctly implemented, as described below, the same operating conditions result in clear, clean audio:
 
 @htmlonly
 <audio src="AudioUnitRender-Correct.wav" controls></audio>
  <a href="AudioUnitRender-Correct.wav">Clean audio</a>
 @endhtmlonly
 
 You can test your app under this scenario by using it with the "AB Torture Test" sample app as a source.
 

 
 In order to deal with this scenario, we **highly recommend replacing the `AudioUnitRender` call** with
 a call to [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive) instead, placed within a loop
 to process frames in variable amounts.
 
 That is, replace:
 
 @code
 // In your render callback
 AudioBufferList *bufferList = ...;
 OSStatus err = AudioUnitRender(THIS->_ioAudioUnit, ioActionFlags, inTimeStamp, 1, inNumberFrames, bufferList);
 
 // Do something with 'inNumberFrames' frames of audio in 'bufferList'
 @endcode
 
 with something like the following:
 
 @code
 AudioBufferList *bufferList = ...;
 
 BOOL hasMoreAudio = YES;
 while ( hasMoreAudio ) {
     AudioTimeStamp timestamp;
     UInt32 frames;
 
     if ( ABInputPortIsConnected(THIS->_inputPort) ) {
         // Our input port is connected
         int bufferSizeToProcess = inNumberFrames;
         
         frames = ABInputPortPeek(THIS->_inputPort, &timestamp);
 
         if ( frames < bufferSizeToProcess ) break;
         frames = MIN(bufferSizeToProcess, frames);
        
         // Optionally, clear the buffer list pointers to have Audiobus provide buffers for you;
         // you probably don't want to do this if you're using the ioData buffers provided to you
         // via the callback parameters
         for ( int i=0; i<bufferList->mNumberBuffers; i++ ) {
             bufferList->mBuffers[i].mData = NULL;
             bufferList->mBuffers[i].mDataByteSize = 0;
         }
        
         ABInputPortReceive(THIS->_inputPort, nil, bufferList, &frames, NULL, NULL);
 
     } else {
        // Your original render code
        timestamp = *inTimeStamp;
        frames = inNumberFrames;
        OSStatus err = AudioUnitRender(THIS->_ioAudioUnit, ioActionFlags, inTimeStamp, 1, frames, bufferList);
        // TODO: handle errors
 
        hasMoreAudio = NO;
     }
     
     // Do something with the 'frames' frames of audio in 'bufferList', and the timestamp in 'timestamp'
 }
 @endcode
 
 This will ensure that your users never record glitches when an impaired communication channel
 is in use with Audiobus.
 
 Note that this stategy is unnecessary if you are using the
 [useLowLatencyInputStream](@ref ABAudiobusAudioUnitWrapper::useLowLatencyInputStream) setting.
 
 Secondly, if recording in your app is time-sensitive - if you're recording against other tracks, for
 instance - then you must manually fetch the current timestamp from Audiobus using
 [ABInputPortPeek](@ref ABInputPort::ABInputPortPeek) (note that this is already done in the sample 
 code above):
 
 @code
 AudioTimeStamp timestamp;
 ABInputPortPeek(THIS->_inputPort, &timestamp);
 @endcode
 
 You must use the `AudioTimeStamp` value returned to align your recorded audio correctly in your 
 app's timeline. If you do not, your app will suffer from laggy recording via Audiobus under 
 certain circumstances.
 
 > If timing is important in your app, it is **vital** that you use this timestamp correctly.
 > If you do not, users will experience lag when recording in your app.
 
 See the section [Receivers, Use Audio Timestamps](@ref Receivers-Timestamps) for further
 discussion on timestamps and latency compensation.

@section Test 11. Test
 
 To test your app with Audiobus, you'll need the Audiobus app (http://audiob.us/download).
 
 You'll find a number of fully-functional sample apps in the "Samples" folder of the Audiobus SDK
 distribution.
 
 If you're creating an app with input ports, then try the "AB Sender" and "AB Torture Test" samples.
 
 If you're creating an app with output ports, then try the "AB Receiver" sample, which both records audio and
 plays audio live, or the "AB Multitrack Receiver", which plays audio live and displays the incoming
 waveforms from each separate input app. *Make sure that your app works correctly with the "AB Receiver" sample
 app with monitoring turned on and off in the sample app.* Try turning monitoring on in AB Receiver, and adjust
 the volume to make sure it is reflected correctly in the audible output.
 
 If you're creating an app with filter ports, then you can
 try different combinations of the Audiobus mic input, the "AB Sender" sample, the Audiobus speaker
 output and the "AB Receiver" app.
 
 To build the sample apps, just open up the Xcode project file, replace the prefix of the bundle ID 
 with your own company's prefix, to match your provisioning profile, then build and run.
 
 <blockquote class="alert">We reserve the right to **ban your app** from the Compatible Apps listing or even from
 Audiobus entirely, if it does not work correctly with Audiobus. It's critical that you test your app properly.</blockquote>
 
@section Go-Live 12. Go Live
 
 Once the Audiobus-compatible version of your app has been approved by Apple and hits the App
 Store, you should visit the [your apps page](http://developer.audiob.us/apps) and click "Go Live".
 
 This will result in your app being added to the Compatible Applications listing
 within Audiobus, and shown on Audiobus's website in various locations. If anyone has subscribed at our
 [compatible apps listing](http://audiob.us/apps) to be notified when your app gains Audiobus support,
 they will be notified by email automatically.
 
 > If you forget this step, potential new users will never find your app through our app directories,
 > losing you sales!
 
@section Youre-Done You're Done!

 Unless you want to do more advanced stuff, that's it, you're done. Run your app, open the
 Audiobus app, and you should see your app appear in the appropriate port picker in the Audiobus app,
 depending on the ports you created.

 Congratulations! You are now Audiobus compatible.

 The next thing to do is read the important notes on [Being a Good Citizen](@ref Good-Citizen) to
 make sure your app behaves nicely with others. In particular, if your app records audio, it's
 important to make correct use of audio timestamps so Audiobus's latency compensation works
 properly in your app and those your app connects to.
 
 Please note that you should not split up stereo Audiobus streams into two separate channels,
 treated differently. You should always treat audio from Audiobus as one, 2-channel stream.

 If your app plays received audio live, then you will need to read the section about the
 [Audiobus Input Port](@ref Audiobus-Input), so you use the correct, low-latency audio stream.

 If your app provides more than one kind of port, you may wish to allow users to connect your app
 multiple times in the same Audiobus setup, such as in the input and the output positions simultaneously.
 If your app supports this kind of functionality, you can set the 
 @link ABAudiobusController::allowsMultipleInstancesInConnectionGraph allowsMultipleInstancesInConnectionGraph @endlink
 property to YES. 
 
 > Please note that if you set `allowsMultipleInstancesInConnectionGraph` to YES and you have both input and output ports,
 > [ABInputPortReceiveLive](@ref ABInputPort::ABInputPortReceiveLive) will return silence if your input port is connected to
 > your output port, to avoid feedback issues.
 
 If you'd like to make your app more interactive, you can implement [triggers](@ref Triggers) that
 allow users to trigger actions in your app (like toggling recording, playback, etc) from other
 apps and devices.

 You can test your app with other Audiobus compatible apps, and you can also build the sample
 apps that are included in the Audiobus SDK distribution. Just change the Bundle Identifier of
 the apps you want to run to include your own company prefix, and you'll be ready to build and
 run.

 Finally, tell your users that you support Audiobus! We provide a set of graphical resources
 you can use on your site and in other promotional material. Take a look at
 the [resources page](http://developer.audiob.us/resources) for the details.

 Read on if you want to know about more advanced uses of Audiobus, such as multi-track
 [receiving](@ref Audiobus-Input), exposing multiple audio [ports](@ref Ports), sending and
 receiving [metadata](@ref Metadata), or building [filter apps](@ref Filtering)

@page Recipes Common Recipes

 This section contains code samples illustrating a variety of common Audiobus-related tasks.
 More sample code is avaliable within the "Samples" folder of the SDK distribution.
 
 @section Audio-Unit-Wrapper-Recipe Create input and output ports and use the Audio Unit Wrapper

 This sample describes the basic scenario using one input port and one output port with the
 Audio Unit Wrapper, which takes care of everything, given an AudioUnit instance.
 
 For most apps, this is all that's required to support Audiobus.
 
 This usage pattern is described more fully in the '[Integration Guide](@ref Integration-Guide)'
 setup guide.
 
 @code
 self.audiobusController = [[[ABAudiobusController alloc] initWithAppLaunchURL:[NSURL URLWithString:@"MyApp.audiobus://"]
                                                                        apiKey:@"MY-API-KEY"]
                                    autorelease];
 self.audiobusAudioUnitWrapper = [[[ABAudiobusAudioUnitWrapper alloc]
                                     initWithAudiobusController:self.audiobusController
                                                      audioUnit:self.audioController.audioUnit
                                                         output:[self.audiobusController addOutputPortNamed:@"Main" title:@"Main Output"]
                                                          input:[self.audiobusController addInputPortNamed:@"Main" title:@"Main Input"]]
                                        autorelease];
 @endcode

 @section Input-Port-Recipe Create an input port and manually receive audio

 This code illustrates the typical method of receiving audio from Audiobus manually, without using the Audio
 Unit Wrapper.
 
 The code creates a single input port, assigns an AudioStreamBasicDescription describing the audio format to
 use, allocates an appropriate AudioBufferList for storing audio, then defines an input block which is
 called whenever audio arrives at the input port.
 
 @code
 ABInputPort *input = [self.audiobusController addInputPortNamed:@"Main" title:NSLocalizedString(@"Main Input", @"")];
 input.clientFormat = myAudioDescription;

 // Prepare an audio buffer list (eg: non-interleaved stereo audio)
 int numberOfChannels = 2;
 AudioBufferList *bufferList = (AudioBufferList*)malloc(sizeof(AudioBufferList) + ((numberOfChannels-1) * sizeof(AudioBuffer)));
 bufferList->mNumberBuffers = numberOfChannels;
 for ( int i=0; i<bufferList->mNumberBuffers; i++ ) {
     bufferList->mBuffers[i].mNumberChannels = 1;
     bufferList->mBuffers[i].mDataByteSize = 0;
     bufferList->mBuffers[i].mData = NULL; // Set to NULL to make Audiobus provide its own buffer
 }

 input.audioInputBlock = ^(ABInputPort    *inputPort,
                           UInt32          lengthInFrames,
                           AudioTimeStamp *nextTimestamp,
                           ABPort         *sourcePortOrNil) {

     // Receive audio into buffer list
     ABInputPortReceive(inputPort, sourcePortOrNil, bufferList, &lengthInFrames, nextTimestamp, NULL);

     // ... Do something with the lengthInFrames frames of audio in bufferList
 };

 // ...don't forget to free(bufferList) when finished with it
 @endcode

 @section Output-Port-Recipe Create an output port and send audio

 This code snippet demonstrates how to create an output port, and then send audio through it.
 
 It also demonstrates how to monitor the @link ABInputPortAttributePlaysLiveAudio @endlink
 attribute and mute the audio output as necessary - see [Designated Output](@ref Designated-Output) for
 details.
 
 @code
 // In app initialisation...
 self.output = [self.audiobusController addOutputPortNamed:@"Main" title:@"Main Output"];
 self.output.clientFormat = myAudioDescription;
 
 // In an input callback/etc
 ABOutputPortSendAudio(_output, ioData, inNumberFrames, inTimestamp, NULL);
 
 if ( ABOutputPortGetConnectedPortAttributes(_output) & ABInputPortAttributePlaysLiveAudio ) {
    // Mute your audio output if the connected port plays it for us instead
    for ( int i=0; i<ioData->mNumberBuffers; i++ ) {
        memset(ioData->mBuffers[i].mData, 0, ioData->mBuffers[i].mDataByteSize);
    }
 }
 @endcode

 @section Filter-Port-Recipe Create a filter port

 This demonstrates how to create and implement a filter port, allowing your app to
 serve as an effects filter for other apps.
 
 The code creates a filter port, providing a processing implementation block which is
 invoked whenever audio arrives on the input side of the filter. After the block is called,
 during which your app processes the audio in place, Audiobus will automatically send the
 processed audio onwards.
 
 Apps that implement filter ports are also required to play the filtered audio out of
 the device's audio system. The Audio Unit Wrapper can handle this automatically, but this
 sample demonstrates how to do so manually using the 
 [ABFilterPortGetOutput](@ref ABFilterPort::ABFilterPortGetOutput) function.
 
 @code
 // In app initialisation...
 self.filterPort = [_audiobusController addFilterPortNamed:@"Main"
                                                     title:@"Main Filter"
                                              processBlock:^(AudioBufferList* audio, UInt32 frames, AudioTimeStamp *timestamp) {
    // Filter the audio...
 }];
 _filterPort.clientFormat = myAudioDescription;
 
 // From your render callback for your audio system...
 MyClass *THIS = (MyClass*)inRefCon;
 if ( ABFilterPortIsConnected(THIS->_filterPort) ) {
    // Pull output audio from the filter port
    // Note: The following line isn't necessary if you're using the Audio Unit Wrapper - it'll do this for you.
    ABFilterPortGetOutput(THIS->_filterPort, ioData, inNumberFrames, NULL);
 
    // Skip normal processing
    return;
 }
 @endcode

 @section Trigger-Recipe Create a trigger

 This demonstrates how to create a trigger, which can be invoked remotely to perform some action within your app.
 
 The sample creates a trigger, passing in a block that toggles the recording state of a fictional transport controller.
 
 It also observes the recording state of the controller, and updates the trigger's state when the recording state
 changes, so that the appearance of the user interface element corresponding to the trigger on remote apps changes
 appropriately.
 
 @code
 self.recordTrigger = [ABTrigger triggerWithSystemType:ABTriggerTypeRecordToggle block:^(ABTrigger *trigger, NSSet *ports) {
    if ( self.transportController.recording ) {
        [self.transportController endRecording];
    } else {
        [self.transportController beginRecording];
    }
 }];
 [self.audiobusController addTrigger:self.recordTrigger];
 
 // Watch recording status of our controller class so we can update the trigger state
 [self.transportController addObserver:self forKeyPath:@"recording" options:0 context:NULL];

 ...

 -(void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    // Update trigger state to reflect recording status
    if ( object == self.transportController ) {
        self.recordTrigger.state = self.transportController.recording ? ABTriggerStateSelected : ABTriggerStateNormal;
    }
 }
 @endcode
 
 @section Determine-Connected Determine if app is connected via Audiobus
 
 The following code demonstrates how to monitor and determine whether any Audiobus ports are
 currently connected.
 
 @code
 // In app delegate/etc, watch for connection change notifications
 [[NSNotificationCenter defaultCenter] addObserver:self 
                                          selector:@selector(connectionsChanged:) 
                                              name:ABConnectionsChangedNotification 
                                            object:nil];
 
 // On cleanup...
 [[NSNotificationCenter defaultCenter] removeObserver:self 
                                                 name:ABConnectionsChangedNotification 
                                               object:nil];

 - (void)connectionsChanged:(NSNotification*)notification {
    if ( _audiobusController.connected ) {
        // We are connected
 
        // Note: You can also use the functions ABInputPortIsConnected, ABOutputPortIsConnected
        // and ABFilterPortIsConnected to determine which specific input/output/filter ports
        // are connected.
 
    } else {
        // Not connected
        
        // Note: If your app is in the background at this point, you may wish to set a 10 second
        // timer to stop your audio engine, thereby suspending the app
    }
 }
 @endcode
 
 @section Enumerate-Connections Enumerate apps connected to an input port
 
 This illustrates how to inspect each individual source of an input port. This information
 can be used to update the user interface, or configure models to represent each audio stream.
 
 @code
 for ( ABPort *connectedPort in _inputPort.sources ) {
    NSLog(@"Port '%@' of app '%@' is connected", connectedPort.displayName, connectedPort.peer.displayName);
 }
 @endcode
 
 @section Get-All-Sources Get all sources of the current Audiobus session
 
 This example demonstrates how to obtain a list of all source ports of the current session; that is,
 all ports that correspond to the 'Inputs' position in the Audiobus app. Note that this is a different
 list of ports than the ones enumerated in the prior sample, as this is list of all inputs, not just the
 ones directly connected to a given port.
 
 @code
 NSArray *allSessionSources = [_audiobusController.connectedPorts filteredArrayUsingPredicate:
                                [NSPredicate predicateWithFormat:@"type = %d", ABPortTypeOutput]];
 @endcode
 
 Note: Similarly, you can obtain a list of all filters by replacing the `ABPortTypeOutput` identifier with
 `ABPortTypeFilter`, and a list of all destinations with the `ABPortTypeInput`.
 
 @section Maintain-C-Array-Of-Connections Maintain a C array of connected sources
 
 This example demonstrates how to maintain a C array of currently-connected sources, in order
 to be able to enumerate them within a Core Audio thread without calling any Objective-C methods. Note
 that Objective-C methods should never be called on a Core Audio thread due to the risk of priority
 inversion, resulting in stuttering audio.
 
 The sample code monitors connection changes, then updates the C array accordingly.
 
 @code
 // A structure used to make up our source table
 struct port_entry_t { ABPort *port; BOOL pendingRemoval; };
 
 // Our class continuation, where we define a source port table
 @interface MyClass () {
    struct port_entry_t _portTable[kMaxSources];
 }
 @end
 
 // ...

 - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {

     // ...
 
     // Register to receive notifications when the Audiobus connections change, so we can update our C source array
     [[NSNotificationCenter defaultCenter] addObserver:self
                                              selector:@selector(audiobusConnectionsChanged:)
                                                  name:ABConnectionsChangedNotification
                                                object:nil];
 }

 // Table lookup facility, to make lookups easier
 - (struct port_entry_t*)entryForPort:(ABPort*)port {
     for ( int i=0; i<kMaxSources; i++ ) {
         if ( _portTable[i].port == port ) {
             return &_portTable[i];
         }
     }
     return NULL;
 }

 - (void)audiobusConnectionsChanged:(NSNotification*)notification {
     // When the connections change, add any new sources to our C array
     for ( ABPort *source in _input.sources ) {
         if ( ![self entryForPort:source] ) {
             struct port_entry_t *emptySlot = [self entryForPort:nil];
             if ( emptySlot ) {
                 emptySlot->port = source;
             }
         }
     }
 
     // Prepare to remove old sources (this will be done on the Core Audio thread, so removals are thread-safe)
     for ( int i=0; i<kMaxSources; i++ ) {
         if ( _portTable[i].port && ![_input.sources containsObject:_portTable[i].port] ) {
             _portTable[i].pendingRemoval = YES;
         }
     }
 }

 
 // In an input/output Remote IO callback, etc.
 MyClass *THIS = (MyClass*)inRefCon;
 
 // Remove sources pending removal (which we did in the connection change handler above)
 for ( int i=0; i<kMaxSources; i++ ) {
     if ( THIS->_portTable[i].port && THIS->_portTable[i].pendingRemoval ) {
         THIS->_portTable[i].pendingRemoval = NO;
         THIS->_portTable[i].port = nil;
     }
 }

 // Now we can iterate through the source port table without using Objective-C:
 for ( int i=0; i<kMaxSources; i++ ) {
     if ( THIS->_portTable[i].port ) {
        // ...Do something with THIS->_portTable[i].port
     }
 }
 @endcode

 @section Application-State Manage application state
 
 This example demonstrates the recommended way to manage your application's state.
 
 The example assumes the app in question has been registered at 
 [developer.audiob.us/register](http://developer.audiob.us/register), and is therefore able
 to be connected and launched from the Audiobus app.
 
 As soon as your app is connected via Audiobus, it must have a running and active audio system.
 This means you must either only instantiate the Audiobus controller *after* starting your audio
 system, or you must watch for @link ABConnectionsChangedNotification @endlink and start your
 audio system when the notification is observed.
 
 Once your app is connected via Audiobus, it should not under any circumstances suspend its 
 audio system when moving into the background. When moving to the background, the app can check
 the [connected](@ref ABAudiobusController::connected) property of the Audiobus controller,
 and only stop the audio system if the result is negative.
 
 If your app is in the background when it is disconnected from Audiobus, we recommend that
 your app shut down its audio system after a 10 second delay, if appropriate.
 
 @code
 - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // ...
 
    // Watch for connections
    [[NSNotificationCenter defaultCenter] addObserver:self 
                                             selector:@selector(connectionsChanged:) 
                                                 name:ABConnectionsChangedNotification
                                               object:nil];
 
    // ...
 }
 
 - (void)connectionsChanged:(NSNotification*)notification {
    // Cancel any scheduled shutdown
    [NSObject cancelPreviousPerformRequestsWithTarget:_audioEngine selector:@selector(stop) object:nil];
 
    if ( _audiobusController.connected && !_audioEngine.running ) {
        // Start the audio system upon connection, if it's not running already
        [_audioEngine start];
    } else if ( !_audiobusController.connected && _audioEngine.running
                && [[UIApplication sharedApplication] applicationState] == UIApplicationStateBackground ) {
        // Shut down after 10 seconds if we disconnected while in the background
        [_audioEngine performSelector:@selector(stop) withObject:nil afterDelay:10.0];
    }
 }
 
 -(void)applicationDidEnterBackground:(NSNotification *)notification {
    // Only stop the audio system if Audiobus isn't connected
    if ( !_audiobusController.connected ) {
        [_audioEngine stop];
    }
 }
 
 -(void)applicationWillEnterForeground:(UIApplication *)application {
    // Cancel any scheduled shutdown
    [NSObject cancelPreviousPerformRequestsWithTarget:_audioEngine selector:@selector(stop) object:nil];

    // Start the audio system if it's not already running
    if ( !_audioEngine.running ) {
        [_audioEngine start];
    }
 }
 @endcode
 
 @section Audio-Queue-Input Use Audiobus input in an Audio Queue
 
 This example demonstrates the Audio Queue versions of the input port receive functions, which
 take an AudioQueueBufferRef argument instead of an AudioBufferList.
 
 Illustrated is an input callback which replaces the incoming microphone audio with audio from
 Audiobus, which represents a quick and easy way to implement input ports in an app that uses
 Audio Queues and microphone input.
 
 @code
 static void MyAQInputCallback(void *inUserData,
                               AudioQueueRef inQueue,
                               AudioQueueBufferRef inBuffer,
                               const AudioTimeStamp *inStartTime,
                               UInt32 inNumPackets,
                               const AudioStreamPacketDescription *inPacketDesc) {
 
    MyController *THIS = (MyController*)inUserData;
 
    // Intercept audio, replacing it with Audiobus input
    AudioTimeStamp timestamp = *inStartTime;
    ABInputPortReceiveAQ(THIS->_audiobusInputPort,
                         nil,
                         inBuffer,
                         &inNumPackets,
                         &timestamp,
                         NULL);
 
    // Now do something with audio in inBuffer...
 
 }
 @endcode
 
 @section Input-Port-Multichannel Receive multi-channel audio
 
 This sample code describes a scenario where the app has a single input port that receives separate
 audio streams from each source, and records them to a fictional track model object.
 
 The app creates an input port and directs it to receive audio as separate streams. Then it begins
 observing connection change notifications.
 
 When the connection changes, the sample code enumerates added and removed sources of the input port,
 and creates or removes instances of a fictional track model object, storing added track objects in
 a dictionary that maps between source ports and tracks.
 
 The app allocates an appropriate AudioBufferList, and provides an audio input block, which is called
 whenever audio arrives at the app's input port.
 
 The input block receives the new audio, then obtains a reference to the track model that corresponds to
 the source for which audio has just been received. Finally, the received audio is passed to the track
 model, which could store the audio to an audio file, and cause the user interface to be updated, showing
 a waveform, for example.
 
 Note the use of the `@synchronized` keyword, ensuring mutually exclusive access to the mapping
 dictionary. As connection changes are reported on the main thread, but audio input occurs on a
 secondary thread, care must be taken.
 
 Note also that although it is not shown here, you may use 
 [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive)/[ABInputPortPeek](@ref ABInputPort::ABInputPortPeek)
 without an [audioInputBlock](@ref ABInputPort::audioInputBlock), to allow you to do things like receive
 audio from within the Core Audio thread, such as from inside an input callback. To do so, however,
 certain care must be taken. Please see the documentation for [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive)
 and [ABInputPortEndReceiveTimeInterval](@ref ABInputPort::ABInputPortEndReceiveTimeInterval) for more info.
 
 See the "AB Multitrack Receiver" sample app for more.

 If you wish to manipulate the audio and provide live monitoring that reflects the manipulations, you can use 
 the [ABLiveBuffer](@ref ABLiveBuffer) class to manually enqueue each processed audio stream, and dequeue mixed
 audio with automatic latency adjustment and gap filling. Use of ABLiveBuffer is demonstrated with the 
 "AB Multitrack Receiver" and "AB Multitrack Oscilloscope" sample apps, and discussed under
 [Providing Live Monitoring when Receiving and Manipulating Separate Streams](@ref Receive-Streams-Monitoring).
 
 If you wish to incorporate audio from other sources - namely, the device's audio input - you will probably 
 want to use the [ABMultiStreamBuffer](@ref ABMultiStreamBuffer) class for synchronizing the incoming audio streams
 from Audiobus and the device audio input, or other sources. This class will perform all the stream synchronization 
 for you, so you simply enqueue each audio stream, then dequeue synchronized streams from the other side. See the
 "AB Multitrack Receiver" sample app for an example of its use.
 
 @code
 self.inputPort = [self.audiobusController addInputPortNamed:@"Main" title:NSLocalizedString(@"Main Input", @"")];
 _inputPort.clientFormat = myAudioDescription;
 _inputPort.receiveMixedAudio = NO;
 
 // Keep a dictionary to map between sources and track objects
 self.sourceToTrackMapping = [NSMutableDictionary dictionary];
 
 // Watch for connection changes
 [[NSNotificationCenter defaultCenter] addObserver:self 
                                          selector:@selector(connectionsChanged:) 
                                              name:ABConnectionsChangedNotification 
                                            object:nil];
 

 // Prepare an audio buffer list (eg: non-interleaved stereo audio)
 int numberOfChannels = 2;
 AudioBufferList *bufferList = (AudioBufferList*)malloc(sizeof(AudioBufferList) + ((numberOfChannels-1) * sizeof(AudioBuffer)));
 bufferList->mNumberBuffers = numberOfChannels;
 for ( int i=0; i<bufferList->mNumberBuffers; i++ ) {
     bufferList->mBuffers[i].mNumberChannels = 1;
     bufferList->mBuffers[i].mDataByteSize = 0;
     bufferList->mBuffers[i].mData = NULL; // Set to NULL to make Audiobus provide its own buffer
 }

 _inputPort.audioInputBlock = ^(ABInputPort    *inputPort,
                                UInt32          lengthInFrames,
                                AudioTimeStamp *nextTimestamp,
                                ABPort         *sourcePort) {
 
     // Receive audio for source sourcePort into buffer list
     ABInputPortReceive(inputPort, sourcePort, bufferList, &lengthInFrames, nextTimestamp, NULL);

     // Ensure mutually exclusive access to the mapping dictionary
     @synchronized ( _sourceToTrackMapping ) {
        // Get the corresponding track
        MYFictionalTrackModelObject *track = [_sourceToTrackMapping objectForKey:[NSValue valueWithPointer:sourcePort]];
 
        if ( !track ) return;
 
        // Pass audio to our fictional track model object
        [track addAudio:bufferList length:lengthInFrames atTime:nextTimestamp];
     }
 };

 
 // ...
 
 // Monitor connections, and add or remove tracks as necessary
 - (void)connectionsChanged:(NSNotification*)notification {
    for ( ABPort *port in _inputPort.sources ) {
        NSValue *portKey = [NSValue valueWithPointer:port];
        if ( ![_sourceToTrackMapping objectForKey:portKey] ) {
            // This is a new source connection. Create a track object to record the audio for this source.
            MYFictionalTrackModelObject *track = [[MYFictionalTrackModelObject alloc] initWithTrackName:port.peer.name];
            [self.mainViewController addTrack:track];
            [track startRecording];
            @synchronized ( _sourceToTrackMapping ) { // Ensure mutually exclusive access to the mapping dictionary
                [_sourceToTrackMapping setObject:track forKey:portKey];
            }
        }
    }
    // Find old sources no longer connected
    for ( NSValue *portKey in [_sourceToTrackMapping allKeys] ) {
        ABPort *port = (ABPort*)[portKey pointerValue];
        if ( ![_inputPort.sources containsObject:port] ) {
            // This port is no longer connected
            MYFictionalTrackModelObject *track = [_sourceToTrackMapping objectForKey:portKey];
            [track stopRecording];
            [self.mainViewController removeTrack:track];
            @synchronized ( _sourceToTrackMapping ) { // Ensure mutually exclusive access to the mapping dictionary
                [_sourceToTrackMapping removeObjectForKey:portKey];
            }
        }
    }
 }
 
 // ...don't forget to free(bufferList) and remove ourselves from NSNotificationCenter on cleanup
 @endcode

@page Ports Ports
 
 Audiobus ports are the endpoints for audio. [Input ports](@ref Audiobus-Input) receive audio,
 [output ports](@ref Audiobus-Output) send it, and [filter ports](@ref Filtering) process received
 audio and send it onwards.
 
 Advanced apps can produce more than one separate audio stream by creating multiple output ports,
 or accept audio input to different places by creating multiple input ports.
 
 All Audiobus apps have at least one port of one kind or another. Additional ports can be added at
 any time by the application, via ABAudiobusController's
 @link ABAudiobusController::addOutputPortNamed:title: addOutputPortNamed:title: @endlink,
 @link ABAudiobusController::addInputPortNamed:title: addInputPortNamed:title: @endlink and
 @link ABAudiobusController::addFilterPortNamed:title:processBlock: addFilterPortNamed:title:processBlock: @endlink
 methods.
 
 For example, a multi-track player might define one output port for every track, in addition to
 the main port. Then, certain tracks could be routed to other iOS devices, or filtered by a
 filter app.
 
 Alternately, a mixer app might define several input ports that apps can send audio to, allowing
 the user to crossfade between the ports.
 
 If your app has multiple ports, it's highly recommended than you provide icons for each port, via the
 [icon](@ref ABOutputPort::icon) property for the port. These
 will be displayed to the user in various locations, and aid the user in comprehending the function of
 your app's ports. Port icons should be 32x32 pixel images, which will be used as a mask to draw styled icons.
 You may change port icons dynamically, and the user interface across all connected devices and apps will
 change accordingly.
 
@page Sending-Receiving Sending and Receiving

 If you desire more control over how your application sends and/or receives audio, this section
 will describe how to use Audiobus input and output ports directly.

 Both input and output ports have very simple interfaces that are written in C and optimised for use
 within Core Audio. That means they don't hold locks, don't allocate memory and don't call
 Objective-C methods.  If you use either of these interfaces from a Core Audio context, be sure to
 store a pointer to the input or output port object from a scope you can access directly, without
 calling any Objective-C methods (this includes accessing properties). If you perform any
 Objective-C method calls, your code may block, risking audio glitches.

@section Audiobus-Output Sending: The Audiobus Output Port

 The Audiobus output port class @link ABOutputPort @endlink provides a simple interface for
 transmitting audio in `AudioBufferLists`.

 To use the output port, first specify the audio format (`AudioStreamBasicDescription`) that your app
 uses, by setting the port's [clientFormat](@ref ABOutputPort::clientFormat) property. Once
 set, the port will automatically perform conversion to the Audiobus line format.

 Then, simply call the [ABOutputPortSendAudio](@ref ABOutputPort::ABOutputPortSendAudio) C
 function with your audio buffer list, the number of frames, and the timestamp that corresponds
 to your audio. You can optionally also transmit arbitrary metadata along with your audio. See
 [Metadata](@ref Metadata) for more info.

 Note that it is vital you include the correct audio timestamp, so that Audiobus can perform
 latency compensation correctly. See [Being a Good Citizen](@ref Good-Citizen) for more info.
 
 > If you are using an output port and *not* using the Audio Unit Wrapper, you **must** handle system audio
 > output appropriately, muting or playing your app's audio output depending on the presence of the
 > @link ABInputPortAttributePlaysLiveAudio @endlink flag on the input port's
 > @link ABInputPort::attributes attributes @endlink property. See [Designated Output](@ref Designated-Output)
 > for details.
 
@section Audiobus-Input Receiving: The Audiobus Input Port

 The Audiobus input port class @link ABInputPort @endlink provides an interface for receiving audio,
 either as separate audio streams (one per connected sender), or as a single audio stream with all
 sources mixed together.

 To begin using the class, as with the output port, you must specify the audio format
 (`AudioStreamBasicDescription`) that your app uses, by setting the
 [clientFormat](@ref ABInputPort::clientFormat) property. Once set, the input port will
 automatically perform conversion from the Audiobus line format.

@subsection Lossless-vs-Low-Latency Lossless vs Low-Latency Audio

 The input port provides two separate versions of the audio stream: One containing lossless
 production-quality audio that may exhibit some latency, and one lossy but very low-latency stream
 suitable for live playback for monitoring.

 <img src="abinputport.png" title="Input port block diagram, showing lossless vs low-latency streams" />

 If you are recording audio, then use the lossless stream, which benefits from Audiobus's
 network error recovery system.

 If you wish to play received audio live, then use the low-latency stream.

 Note that you may use both streams simultaneously, if you are recording while playing audio
 live: Simply record using the lossless stream, and use the low-latency stream for the live
 monitoring.

 Take a look at the "AB Receiver" sample app in the SDK distribution for an example of how to play
 live audio, while using the ordinary lossless methods to obtain a clean (but higher-latency) audio feed
 for production-quality recording.

 Note that if you are recording audio from Audiobus and simultaneously playing it aloud, we recommend
 you record the raw audio feed as it comes directly from the input port, and use the live buffer for
 playback only, as artefacts may be introduced in the live stream when old audio is discarded.
 
 <blockquote class="alert">
 It's critically important that you use the correct stream versions in the correct way. If you use
 the lossless stream format, accessible via [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive),
 but skip audio by passing NULL buffers in order to achieve low-latency output, you will hear audio
 artefacts. If you are recording audio, **these artefacts will be evident in the recording**.
 
 If you wish to produce low-latency audio output based on the lossless audio stream, use ABLiveBuffer,
 described below.
 
 Conversely, you must not record audio from the low-latency audio stream 
 ([ABInputPortReceiveLive](@ref ABInputPort::ABInputPortReceiveLive)). If you do, you will record
 audio artefacts. **Only ever record from the lossless audio stream**.
 </blockquote>
 
 @subsubsection Monitoring-Lossless Audio Monitoring for Lossless Audio Streams

 If you wish to provide both recording and audio monitoring of *processed* audio -- for example, if
 your app has audio effects that the user can apply to the incoming audio -- then use ABLiveBuffer to
 provide low-latency audio monitoring of the processed lossless stream after passing the audio to your recorder.
 
 ABLiveBuffer allows you to use the already-processed lossless audio stream instead of requiring your app
 to perform the audio processing *again* on the live, low-latency stream.
 
 Note that **it is vital that your monitor audio is latency-adjusted and error-concealed**. If it is not,
 then your users will be able to hear stutters in the Audiobus audio stream when audio from sources are delayed:
 
 @htmlonly
 <audio src="AudioUnitRender-Incorrect.wav" controls></audio>
 <a href="AudioUnitRender-Incorrect.wav">Glitchy audio</a>
 @endhtmlonly
 
 ABLiveBuffer will perform all necessary latency adjustment and error concealing, and it provides an easy-to-use interface.
 
 <img src="ablivebuffer.png" title="ABLiveBuffer typical usage scenario" style="margin-left: -20px;" />
 
 Typical usage is as follows:
 
 1. First, as normal, you receive the Audiobus audio from the input port. **Note: You must process all the audio
    that is present, and you must not expect any particular quantity of audio to be available.** In the event of
    stalls, you may receive 0 frames at a given time interval, then an increased number of frames later, while
    the system is catching up. If you only ever process a set amount of audio, then your app will never catch up,
    and will experience greater and greater latency.
 2. Your app manipulates the audio - such as adding audio effects, etc.
 3. Your app may record the audio, display it, etc. (optional)
 4. Enqueue each buffer of audio into the ABLiveBuffer instance ([ABLiveBufferEnqueue](@ref ABLiveBuffer::ABLiveBufferEnqueue)).
 5. Dequeue the latency-adjusted, error-concealed audio stream from ABLiveBuffer and pass to Core Audio for
    output ([ABLiveBufferDequeue](@ref ABLiveBuffer::ABLiveBufferDequeue)).
 
 See the "AB Multitrack Receiver" and "AB Multitrack Oscilloscope" for demonstrations of the use of
 ABLiveBuffer.

@subsection Receive-Lossless Receiving Lossless Audio

 You can receive lossless audio in one of two ways: Set a block to be called when audio is available,
 or pull audio whenever you need it.

 If you provide an [audio input block](@ref ABInputPort::audioInputBlock) to this class,
 then it will be called whenever an Audiobus audio packet is received.  Then, you can call
 [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive) in order to access the
 received audio. See the [input port example recipe](@ref Input-Port-Recipe) above for an
 example.

 Alternately, you can let this class perform the buffering for you, and simply call
 [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive) when you need audio.
 
 If you are using Audio Queues, you may wish to use [ABInputPortReceiveAQ](@ref ABInputPort::ABInputPortReceiveAQ)
 in order to receive audio from within an AudioQueueInputCallback, replacing the audio
 received from the microphone. See the [Audio Queue callback example](@ref Audio-Queue-Input) for details.

 At any point, you can also call [ABInputPortPeek](@ref ABInputPort::ABInputPortPeek)
 to determine how much buffered audio is available and to determine the timestamp of the next
 buffered audio frames.

 You can also call [ABInputPortIsConnected](@ref ABInputPort::ABInputPortIsConnected)
 to determine if any sources are currently connected.

 The audio you receive will be in the audio format you specified via the `clientFormat` property.
 
 See the [multi-channel audio recipe](@ref Input-Port-Multichannel) and the "AB Multitrack Receiver"
 sample app for examples.

@subsection Receive-Low-Latency Receiving Low-Latency Audio

 If your app plays the audio it receives live, then it is strongly recommended that you make use
 of the low-latency receive function @link ABInputPort::ABInputPortReceiveLive
 ABInputPortReceiveLive @endlink.
 The audio streams accessed via this function are manipulated to minimise audio latency. This may include
 throwing away old audio, and may occasionally introduce minor artefacts in the presence of network loss
 or jitter or an overloaded device. If less audio is available than you requested, Audiobus will
 automatically pad the audio with a pattern-matching audio replacement algorithm to fill the gap.

 Note that if you are using the [Audio Unit Wrapper](@ref Instantiate-Wrapper), then you may simply
 set the @link ABAudiobusAudioUnitWrapper::useLowLatencyInputStream useLowLatencyInputStream @endlink
 property to YES to begin receiving low-latency audio.

@subsection Latency Dealing with Latency
 
 When dealing with lossless Audiobus audio, latency is an unavoidable factor that it's very
 important to address when timing is important.
 
 Audiobus deals with latency by providing you, the developer, with timestamps that correspond
 to the creation time of each block of audio. These timestamps are carefully latency-adjusted by
 Audiobus.
 
 If you are recording audio, and are mixing it with other live signals or if timing is 
 otherwise important, then it is **vital** that you make full use of these timestamps in order 
 to compensate for system latency. How you use these timestamps depends on your app - you may
 already be using timestamps from Core Audio, which means there's nothing special that you need
 to do.
 
 See [Being a Good Citizen](@ref Good-Citizen) for more information.

@subsection Receive-Streams Receiving Separate Streams

 You can receive lossless audio as separate streams - one per source - or as a single mixed audio stream.
 By default, Audiobus will return the audio as a single, mixed stream.
 
 If you wish to receive separate streams for each source, however, you can set
 [receiveMixedAudio](@ref ABInputPort::receiveMixedAudio) to `NO`. Then, each source will have
 its own audio stream, accessed by passing in a pointer to the source port in
 @link ABInputPort::ABInputPortReceive ABInputPortReceive @endlink.
 
 If you have provided a block via the `audioInputBlock` property, then the block will be called for
 audio packet arrivals for each source.
 
@subsubsection Receive-Streams-Sources Enumerating Current Sources
 
 At any time, you can also get a list of the connected sources via the
 [sources](@ref ABInputPort::sources) property, which returns an array of @link ABPort @endlink. 
 
@subsubsection Receive-Streams-Core-Audio-Callback Receiving From Within a Core Audio Callback
 
 You may also receive audio outside of a block provided via `audioInputBlock`, to allow you to do
 things like receive audio from within the Core Audio thread, such as from inside an input callback. 
 To do so, you call ABInputPortPeek and ABInputPortReceive passing the source (@link ABPort @endlink) 
 you are interested in as the second argument.
 
 However, certain care must be taken. Please see the documentation for [ABInputPortReceive](@ref ABInputPort::ABInputPortReceive)
 and [ABInputPortEndReceiveTimeInterval](@ref ABInputPort::ABInputPortEndReceiveTimeInterval) for more info.
 
 In particular, you should not access the `sources` property, or any other Objective-C methods, from
 a Core Audio thread, as this may cause the thread to block, resulting in audio glitches. You
 should obtain a pointer to the ABPort objects in advance, and use these pointers directly, as
 demonstrated in [this sample code](@ref Maintain-C-Array-Of-Connections) and within the
 "AB Multitrack Receiver" sample.
 
@subsubsection Receive-Streams-Monitoring Providing Live Monitoring when Receiving and Manipulating Separate Streams

 If you wish to manipulate the audio of each stream, and provide live monitoring that reflects the 
 manipulations, **it's vital that your monitor audio is latency-adjusted and error-concealed**. If it is not,
 then your users will be able to hear stutters in the Audiobus audio stream when audio from sources are delayed:
 
 @htmlonly
 <audio src="AudioUnitRender-Incorrect.wav" controls></audio>
 <a href="AudioUnitRender-Incorrect.wav">Glitchy audio</a>
 @endhtmlonly
 
 The Audiobus SDK provides the ABLiveBuffer class to perform all necessary latency adjustment
 and error concealing, and it provides an easy-to-use interface.

 <img src="ablivebuffer-streams.png" title="ABLiveBuffer typical usage scenario: Streams" style="margin-left: -20px;" />
 
 Typical usage is as follows:
 
 1. First, as normal, you receive each audio stream from the input port. **Note: You must process all the audio
    that is present, and you must not expect any particular quantity of audio to be available.** In the event of
    stalls, you may receive 0 frames at a given time interval, then an increased number of frames later, while
    the system is catching up. If you only ever process a set amount of audio, then your app will never catch up,
    and will experience greater and greater latency.
 2. Your app manipulates the audio streams - such as adding audio effects, etc.
 3. Your app may record the audio, display it, etc. (optional)
 4. Enqueue each stream into the ABLiveBuffer instance ([ABLiveBufferEnqueue](@ref ABLiveBuffer::ABLiveBufferEnqueue)).
 5. Dequeue the latency-adjusted, error-concealed mixed audio stream from ABLiveBuffer and pass to Core Audio for
    output ([ABLiveBufferDequeue](@ref ABLiveBuffer::ABLiveBufferDequeue)). Alternatively, if you wish to gain 
    access to the separate, un-mixed streams still, you may use [ABLiveBufferDequeueSingleSource](@ref ABLiveBuffer::ABLiveBufferDequeueSingleSource).
 
 See the "AB Multitrack Receiver" and "AB Multitrack Oscilloscope" sample apps for example uses of ABLiveBuffer.
 
@subsubsection Receive-Streams-With-Core-Audio-Input Receiving Separate Streams Alongside Core Audio Input
 
 If you wish to simultaneously incorporate audio from other sources as well as Audiobus - namely, the device's audio
 input - then depending on your app, it may be very important that all sources are synchronized and delivered in a 
 consistent fashion. This will be true if you provide live audio monitoring, or if you apply effects in a
 synchronized way across all audio streams.
 
 The Audiobus SDK provides the ABMultiStreamBuffer class for buffering and synchronizing
 multiple audio streams, so that you can do this. You enqueue separate, unsynchronized audio streams on one side,
 and then dequeue synchronized streams from the other side, ready for further processing.
 
 <img src="abmultistreambuffer.png" title="ABMultiStreamBuffer typical usage scenario" style="margin-left: -200px;" />
 
 Typical usage is as follows:
 
 1. You receive audio from the system audio input, typically via a Remote IO input callback and AudioUnitRender,
    then enqueue it on the ABMultiStreamBuffer.
 2. You receive audio from each connected Audiobus source, also enqueuing the audio on the ABMultiStreamBuffer 
    ([ABMultiStreamBufferEnqueue](@ref ABMultiStreamBuffer::ABMultiStreamBufferEnqueue)).
 3. You then dequeue each source from ABMultiStreamBuffer ([ABMultiStreamBufferDequeueSingleSource](@ref ABMultiStreamBuffer::ABMultiStreamBufferDequeueSingleSource)).
    Audio will be buffered and synchronized via the timestamps of the enqueued audio. Note that if the Audiobus
    connection stalls, this will temporarily delay all other audio streams so that synchronization is maintained.
 4. Your app manipulates the audio streams - such as adding audio effects, etc.
 5. Your app may record the audio, display it, etc. (optional)
 6. Enqueue each stream into the ABLiveBuffer instance ([ABLiveBufferEnqueue](@ref ABLiveBuffer::ABLiveBufferEnqueue)).
 7. Dequeue the latency-adjusted, error-concealed mixed audio stream from ABLiveBuffer and pass to Core Audio for
    output ([ABLiveBufferDequeue](@ref ABLiveBuffer::ABLiveBufferDequeue)). Alternatively, if you wish to gain
    access to the separate, un-mixed streams still, you may use [ABLiveBufferDequeueSingleSource](@ref ABLiveBuffer::ABLiveBufferDequeueSingleSource).

 See the "AB Multitrack Receiver" sample app for an example usage scenario involving ABMultiStreamBuffer.

@page Filtering Filtering

 Audiobus provides for a new class of app - a filter. Filter apps will receive audio from Audiobus,
 process it, then send it on. This allows users to create processing chains using apps as audio
 processing modules.

 To create a filter app:

 1. You create an instance of the Audiobus controller, as normal.
 2. Create one or more filter ports by calling
    @link ABAudiobusController::addFilterPortNamed:title:processBlock: addFilterPortNamed:title:processBlock: @endlink,
    where you pass in a block that Audiobus will call when it's time to process audio.
 3. Set the [client format](@ref ABFilterPort::clientFormat) on the filter port, to tell it what kind of audio format 
    you wish to work with while filtering. Audio will automatically be converted to and from these formats. You may also
    want to set the [buffer size](@ref ABFilterPort::audioBufferSize), which tells the filter port how many frames you
    wish to process for each call of the processing block. The default is 256 frames.
 4. Mute your app's audio output if the filter port is connected - see 
    [Adding Audiobus Filtering Capabilities to an Existing App](@ref Filtering-Existing-App).
 5. Either,
    a) Add the filter port to the Audio Unit Wrapper via [addFilterPort:](@ref ABAudiobusAudioUnitWrapper::addFilterPort:),
       and let it handle everything, or
    b) Manually play the output from the filter port through your audio system, using 
       @link ABFilterPort::ABFilterPortGetOutput ABFilterPortGetOutput @endlink.
 
 The filter block has the form:

 @code
 typedef void (^ABAudioFilterBlock)(AudioBufferList *audio, UInt32 frames, AudioTimeStamp *timestamp);
 @endcode

 As audio comes in, you receive the audio via this block, and are expected to manipulate the audio
 in place, leaving `frames` frames of audio in the buffer list afterwards.
 
 As mentioned in (5), your app also needs to play the filtered audio through the device audio output. If you're using the Audio Unit Wrapper,
 you can simply tell it about the filter port via the [addFilterPort:](@ref ABAudiobusAudioUnitWrapper::addFilterPort:)
 method, and the Wrapper will do the rest.
 
 Alternatively, you can do this by calling @link ABFilterPort::ABFilterPortGetOutput ABFilterPortGetOutput @endlink
 from your audio output callback, and sending the returned audio to the system audio output. The audio
 will be delivered in the format you specified via the [audioOutputClientFormat](@ref ABFilterPort::audioOutputClientFormat)
 property, or if it is not specifically set, the [clientFormat](@ref ABFilterPort::clientFormat) property.

 Filter ports automatically add a "bypass" trigger, which is shown in the Connection Panel. When the user
 taps this, the filter port will automatically bypass your audio processing block, sending the audio onwards
 unaffected.
 
 Take a look at the "AB Filter" sample app in the SDK distribution for an example of how a filter app is
 put together, or see the [filter example](@ref Filter-Port-Recipe) above.

 @section Filtering-Existing-App Adding Audiobus Filtering Capabilities to an Existing App

 <img src="filter-app-flow.png" style="float: right; margin-left: 20px;" title="Audiobus filter app flow" />

 If you already have an app that filters audio coming in the microphone, playing it out of the speaker
 or headphones, then you will need to make some adjustments to your app to enable it to behave as an
 Audiobus filter.
 
 You need to perform this step whether or not you're using the Audio Unit Wrapper.

 If your app is connected as an Audiobus filter, you must disable your app's normal audio stream.
 If you do not, then your app will continue to receive input from the mic and play it out the
 speaker, while simultaneously processing an entirely different audio stream for Audiobus!
 
 > Note that muting your app's normal output while behaving as a filter is only necessary if your app generates
 > audio continuously, without direct user interaction.

 The easiest way to determine when to mute normal output when acting as a filter is to check the
 result of the function [ABFilterPortIsConnected](@ref ABFilterPort::ABFilterPortIsConnected). If the result 
 is YES, then your app should mute its normal output and bypass normal processing, and instead output 
 the audio as returned from [ABFilterPortGetOutput](@ref ABFilterPort::ABFilterPortGetOutput).

 @code
 // In your app initialization...
 self.filterPort = [_audiobusController addFilterPortNamed:@"Main"
                                                     title:@"Main Filter"
                                              processBlock:^(AudioBufferList *audio,
                                                             UInt32           frames,
                                                             AudioTimeStamp  *timestamp) {
    // Filter the audio
 }];
 
 // In your audio system...
 MyClass *THIS = (MyClass*)inRefCon;
 if ( ABFilterPortIsConnected(THIS->_filterPort) ) {
    // Don't perform any processing: Instead, play the processed audio from the filter port
    // Note: The following line isn't necessary if you're using the Audio Unit Wrapper - it'll do this for you.
    ABFilterPortGetOutput(THIS->_filterPort, ioData, inNumberFrames, NULL);
    return;
 }
 @endcode

 Note that stopping your audio graph with `AUGraphStop` will not have the correct effect: This will
 cause your application to suspend in the background, stopping it from both processing audio
 and playing the processed audio aloud.

 @section Filtering-And-Receiving Being a Filter and a Receiver

 If you just provide an Audiobus filter port, then users will need to create an Audiobus connection graph
 with the Audiobus microphone input, your app as the filter, and the Audiobus speaker output.

 You may wish to offer users more flexible operation by also offering an input port, which will allow
 users to specify your app as an output in the Audiobus connection graph.

 You can create both a filter port, as described above, as well as an [input port](@ref Audiobus-Input),
 and use them independently.

 As you will be playing received audio live, you should use the low-latency audio stream as your input
 source. If you are using the [Audio Unit Wrapper](@ref Instantiate-Wrapper), then you merely need to
 set the @link ABAudiobusAudioUnitWrapper::useLowLatencyInputStream useLowLatencyInputStream @endlink
 property to `YES`. If you are not using the wrapper, then you should use the
 [low-latency input methods](@ref Receive-Low-Latency) of the Audiobus input port.

@page Triggers Triggers

 Audiobus provides a system where apps can define actions that can be triggered by users from other
 apps, via the Audiobus Connection Panel.

 You can use a set of built-in system triggers (see
 @link ABTrigger::triggerWithSystemType:block: triggerWithSystemType:block: @endlink and
 @link ABTriggerSystemType @endlink), or create your own, specifying your own button icon (see
 @link ABTrigger::triggerWithTitle:icon:block: triggerWithTitle:icon:block: @endlink).

 @section Use-of-Triggers Use of Triggers
 
 Triggers are designed to provide limited remote-control functionality over Audiobus apps. If your
 app has functions that may be usefully activated from a connected app, then you should expose them
 using the Audiobus triggers mechanism.
 
 Note, however, that apps should only provide a small number of these triggers - no more than four -
 to avoid cluttering up the Audiobus Connection Panel interface.
 
 Additionally, your app should only provide triggers that are *relevant to the current state*. Take, for
 example, an app that has the capability of behaving as an Audiobus input and an output. If the app
 presents a "Record" trigger, but is currently acting as an input to another Audiobus app, this
 may lead to confusion: The app is serving in an audio generation role, not an audio consumption role,
 and consequently a "Record" function is not relevant to the current state.
 
 You can add and remove triggers at any time, so you should make use of this functionality to only
 offer users relevant actions.
 
 @section Creating-a-Trigger Creating a Trigger
 
 **Whenever possible, you should use a built-in trigger type, accessible via
 @link ABTrigger::triggerWithSystemType:block: triggerWithSystemType:block: @endlink.**
 
 If you *must* create a custom trigger, then you can use 
 @link ABTrigger::triggerWithTitle:icon:block: triggerWithTitle:icon:block: @endlink, passing
 in the title and an icon. The icon should be an image of no greater than 80x80 pixels, and will be
 used as a mask to draw a styled button.
 If you do not provide a 'selected' state icon for a toggle button, then the same icon will
 be drawn with an alternate style to indicate the state change.

 Create a trigger using one of the class methods of @link ABTrigger @endlink, providing a 
 [block](@ref ABTriggerPerformBlock) to perform when the trigger is activated remotely and optionally
 configure it further (for example, by setting the @link ABTrigger::setIcon:forState: selected icon @endlink).
 Then add the trigger to the Audiobus controller with
 @link ABAudiobusController::addTrigger: addTrigger: @endlink.

 The block accepts two arguments: The trigger, and a set of your app's ports to which the app
 from which the trigger was activated is connected. This port set will typically be just one port,
 but may be multiple ports.

 You may wish to use the ports set to determine what elements within your app to apply the
 result of the trigger to. For example, if your trigger is @link ABTriggerTypeRecordToggle @endlink,
 and the connected port refers to one track of a multi-track recording app, then you may wish
 to begin recording this track.

 If you are implementing a two-state trigger, such as @link ABTriggerTypeRecordToggle @endlink,
 @link ABTriggerTypePlayToggle @endlink or a custom trigger with multiple states, you should update the
 [trigger state](@ref ABTrigger::state) as appropriate, when the state to which it refers changes.
 Have a look at the "AB Receiver" and "AB Filter" sample apps for examples.

 Note that you can also update the icon of custom triggers at any time. The user interface across
 all connected devices and apps will be updated accordingly.
 
 System triggers are automatically ordered in the connection panel as follows: 
 ABTriggerTypeRewind, ABTriggerTypePlayToggle, ABTriggerTypeRecordToggle.

@page Metadata Metadata

 Audiobus allows you to send arbitrary metadata along with every audio packet. Such metadata could,
 for example, include MIDI data that corresponds to the audio, or structural information like key
 and BPM.

 Metadata is defined by a four-character application ID. If you wish to create a new metadata format,
 you must email us at metadata@audiob.us and explain the use and structure of your new format, and
 the application ID you wish to use. We will reserve the application ID for you and document your
 metadata here on the Audiobus developer site so other developers can make use of it.

@section Metadata-Formats Currently Defined Metadata Formats

 There are not yet any metadata formats defined.

@section Sending-Metadata Sending Metadata

 To send metadata, you create a @link ABMetadataBlockList @endlink, then populate one or more
 @link ABMetadataBlock @endlink structures. If you are familiar with `AudioBufferLists`, then this
 should be quite familiar.

 For example:

 First, we define the structures of our metadata:

 @code
 struct my_sample_metadata_structure {
  uint8_t sample_field_1;
  uint16_t sample_field_2;
 } __attribute__((packed)); // Important: You must use the packed attribute to makes sure
                            // there's no compiler-dependent padding in your structure.

 struct my_other_sample_metadata_structure {
  uint16_t sample_field_1;
  uint32_t sample_field_2;
 } __attribute__((packed));
 @endcode

 Then, we create some space for storing our metadata. If you are sending from a Core Audio thread,
 then be sure to either allocate the space in advance (on the main thread), or simply define it
 on the stack.  The following example creates some space on the stack:

 @code
 int numberOfMetadataBlocks = 2;
 char metadataBlockListBuffer[sizeof(ABMetadataBlockList)
                                + (numberOfMetadataBlocks-1)*sizeof(ABMetadataBlock)];

 const uint32_t sample_application_id = 'samp';
 const uint32_t other_sample_application_id = 'othr';

 struct my_sample_metadata_structure        my_metadata;
 struct my_other_sample_metadata_structure  my_other_metadata;
 @endcode

 Note that, similarly to `AudioBufferList`, `ABMetadataBlockList` contains the first
 `ABMetadataBlock`, so we need to add space for `(number of metadata blocks - 1)` extra blocks.

 Now we initialise the structures.

 @code
 memset(metadataBlockListBuffer, 0, sizeof(metadataBlockListBuffer));
 ABMetadataBlockList *metadataBlockList = (ABMetadataBlockList*)metadataBlockListBuffer;
 metadataBlockList->numberOfBlocks = numberOfMetadataBlocks;
 metadataBlockList->blocks[0].application_id = sample_application_id;
 metadataBlockList->blocks[0].length         = sizeof(my_metadata);
 metadataBlockList->blocks[0].bytes          = &my_metadata;
 metadataBlockList->blocks[1].application_id = other_sample_application_id;
 metadataBlockList->blocks[1].length         = sizeof(my_other_metadata);
 metadataBlockList->blocks[1].bytes          = &my_other_metadata;
 @endcode

 Audiobus uses a little-endian line format, which means that you will need to perform a byte
 swap on all but 8-bit scalars within your custom metadata structures. Core Foundation offers
 some convenient functions to do this: `CFSwapInt16HostToLittle`, `CFSwap32HostToLittle`, etc.
 When you receive metadata, you convert back to the host endian format using the reverse
 functions: `CFSwapInt16LittleToHost`, etc.

 @code
 my_metadata.sample_field_1 = sample_value_1;
 my_metadata.sample_field_2 = CFSwapInt16HostToLittle(sample_value_2);
 my_other_metadata.sample_field_1 = CFSwapInt16HostToLittle(other_sample_value_1);
 my_other_metadata.sample_field_2 = CFSwapInt32HostToLittle(other_sample_value_2);
 @endcode

 Once your metadata and metadata block list have been initialised, you pass a pointer to the
 block list when you send your audio:

 @code
 ABOutputPortSendAudio(_outputPort,
                       bufferList,
                       lengthInFrames,
                       audioTimestamp,
                       metadataBlockList);
 @endcode

@section Receiving-Metadata Receiving Metadata

 To receive metadata, you pass in an empty metadata block list, which will be populated with
 the metadata that corresponds to the audio packets you receive.

 The metadata block list structure contains a `bytes` field that lets you specify where the
 incoming metadata is to be placed. If this field is NULL (recommended), then Audiobus will
 provide its own buffer for you.

 First, create and initialize the metadata block list - again, you should never allocate
 memory from the  Core Audio thread. Instead, you should either allocate the memory in advance
 (on the main thread), or simply make some space on the stack. Here, we make some space on the
 stack:

 @code
 int maximumNumberOfBlocks 10;
 char metadataBlockListBuffer[sizeof(ABMetadataBlockList)
                                + (maximumNumberOfBlocks-1)*sizeof(ABMetadataBlock)];

 memset(metadataBlockListBuffer, 0, sizeof(metadataBlockListBuffer));
 ABMetadataBlockList *metadataBlockList = (ABMetadataBlockList*)metadataBlockListBuffer;
 metadataBlockList->numberOfBlocks = maximumNumberOfBlocks;
 @endcode

 If you wish, you can set the `bytes` field to your own memory buffer, and set the
 `bytes_length` field to the size of this buffer. If you do not, then Audiobus will provide
 its own buffer for you.

 Now pass this metadata block list to the `ABInputPortReceive` function when you receive
 audio:

 @code
 ABInputPortReceive(_inputPort,
                    sourcePort,
                    bufferList,
                    &lengthInFrames,
                    &timestamp,
                    metadataBlockList);
 @endcode

 Now `metadataBlockList->numberOfBlocks` will give you the metadata block count. Each block
 (accessible via the `blocks` member) identifies:

- The port it came from (`source_port`),
- The index of the original audio packet (`packet_index`), useful if the audio
  returned spans more than one audio packet (could be any number in the range 0-255, and
  monotonically increasing),
- The application ID (`application_id`), already converted to the host endian fomat.
- The length of the metadata (`length`), and
- A pointer to the metadata itself (`bytes`).

 So, if you are looking for certain metadata types, then iterate through the returned block
 list, checking the application IDs. Then, when you recognize one, validate it, then cast the
 `bytes` field to the type represented by the application ID. Remember to convert
 multi-byte scalars from the little endian line format to the host format.

 @code
 for ( int i=0; i<metadataBlockList->numberOfBlocks; i++ ) {
  ABMetadataBlock *block = &metadataBlockList->blocks[i];
  if ( block->application_id == sample_application_id ) {
    if ( block->length != sizeof(struct my_sample_metadata_structure) ) {
      // Metadata is invalid
      continue;
    }

    struct my_sample_metadata_structure *metadata = (struct my_sample_metadata_structure*)block->bytes;
    char sample_value_1 = metadata->sample_field_1;
    int sample_value_2  = CFSwapInt16LittleToHost(metadata->sample_field_2);

    // Do something with sample_value_1 and 2...
  }
 }
 @endcode


@page Good-Citizen Being a Good Citizen

 Beyond being an audio transmission protocol or platform, Audiobus is a community of applications. The
 experience that users have is strongly dependent on how well these apps work together. So, these are
 a set of rules/guidelines that your app should follow, in order to be a good Audiobus citizen.

@section Senders-Timestamps Senders, Send Audio Timestamps

 Audiobus sender apps are responsible for sending correct audio timestamps along with their
 audio. If you are using the [Audio Unit Wrapper](@ref ABAudiobusAudioUnitWrapper), then this is taken
 care of for you. Otherwise, if you are using @link ABOutputPort @endlink directly, make sure you are
 sending audio timetamps correctly. Usually, this is just a matter of providing the
 `AudioTimeStamp` structure given to you by Core Audio.

@section Receivers-Timestamps Receivers, Use Audio Timestamps

 Audiobus receivers are given timestamps along with every piece of audio they receive. These
 timestamps are vital for compensating for latency when recording in a time-sensitive context, 
 particularly when Audiobus is used over a WiFi network.

 This works in exactly the same way that timestamps in Core Audio do.

 When you receive them, these timestamps are already translated from the originating device or app's
 time base, and adjusted to offset the effects of latency.
 
 Note that if you're using the Audio Unit Wrapper with the input port and you receive Core Audio input
 via an input callback (`kAudioOutputUnitProperty_SetInputCallback`), then you can simply use the timestamp
 given to you in the input callback, which comes straight from Audiobus.
 
 If you are using the Audio Unit Wrapper with the input port but receiving audio via the audio unit's 
 render callback instead (`AUGraphSetNodeInputCallback` or `kAudioUnitProperty_SetRenderCallback`), 
 you will need to use [ABInputPortPeek](@ref ABInputPort::ABInputPortPeek) to retrieve the timestamp.

 If your app records the audio it receives over Audiobus and the timing is important (for example,
 you record audio in time with some other track, such as a looper or a multi-track recorder), then
 use these timestamps when saving the received audio to negate the effects of latency.

 > Note that if your app plays audio live, then there's nothing you can do about this kind of
 > latency (at least, not unless you have a time machine handy). You just have to play audio as you
 > get it, although you should use the [live audio access methods](@ref Receive-Low-Latency)
 > to minimize this latency.

 An app that correctly uses timestamps can expect to see time discrepancies in the recorded audio
 of just a few milliseconds. An app that does not correctly use timestamps will see at least the same
 latency as apps that play the audio live - greater than 20ms.

 If your app already records from the microphone, then you are probably already using the
 `AudioTimeStamp` values given to you by Core Audio, in order to compensate for audio hardware
 latency. If this is the case, then there's probably nothing more you need to do, other than making
 sure this mechanism is using the timestamps generated by Audiobus.

 For example, a looper app might record audio while other loops are playing. The audio must be
 recorded in time so that the beats in the new recording match the beats in the already-playing
 loop tracks. If such an app has a time base (such as the time the app was started) which is used
 to determine the playback position of the loops, then this same time base can be used with the
 timestamps from the incoming audio in order to determine when the newly-recorded track should be
 played back.

@section Designated-Output Designated Output

 Audiobus is all about sending audio around between apps, one or more of which may be capable of
 playing that audio out loud. If you're playing a synth app, for example, into a filter app and
 then out the Audiobus speaker output port, then you don't want the synth app playing the same
 audio that will be coming out of the speaker.

 So, Audiobus defines [port attributes](@ref ABInputPort::attributes) which allow your app to
 announce to others certain facts about the port, such as that it will play audio aloud. In
 response, all other apps up the chain must mute their audio output, so we don't hear two
 copies of the audio.

 @subsection Receivers-Announce-Live-Playback Receivers: Announce Live Playback

 
 If your app produces audio output (via a render callback, for example) that is based on the audio input - such as live audio
 monitoring in a recorder app, or live output from an audio effects processor app - then you must inform Audiobus of this
 fact.
 
 If you produce audio output based on the audio input, and do not tell Audiobus this fact, then the app from which you are
 receiving audio will continue to produce audio through the device's audio output, which results in two copies of the same
 audio being played: One from the source app, and one from your app.
 
 <img src="plays-live-audio.png" title="Demonstration of the ABInputPortAttributePlaysLiveAudio flag" />
 
 You can tell Audiobus that your app produces audio output based on the audio received from the input port by setting
 the @link ABInputPortAttributePlaysLiveAudio @endlink flag on the input port's
 @link ABInputPort::attributes attributes @endlink property:
 
 @code
 input.attributes = ABInputPortAttributePlaysLiveAudio;
 @endcode

 You can change this property whenever you like; any other apps you're connected to will be informed
 of the change automatically.

 @subsection Senders-Mute Senders: Mute Output When Necessary

 The Audiobus output ports provide you with the attributes for all currently connected receivers via
 the [connectedPortAttributes](@ref ABOutputPort::connectedPortAttributes) property. It's your responsibility
 to watch this value (it's a key-value-observable property) and behave accordingly.
 
 Note that you can also retrieve this property value via the C function 
 [ABOutputPortGetConnectedPortAttributes](@ref ABOutputPort::ABOutputPortGetConnectedPortAttributes), which
 can safely be used in a Core Audio context, unlike the Objective-C equivalent.
 
 if:
 
 @code
 (theOutputPort.connectedPortAttributes & ABInputPortAttributePlaysLiveAudio)
 @endcode
 
 or, in C only,
 
 @code
 (ABOutputPortGetConnectedPortAttributes(theOutputPort) & ABInputPortAttributePlaysLiveAudio)
 @endcode
 
 that is, if the `ABInputPortAttributePlaysLiveAudio` flag is present - then you need to mute your app's
 output.
 
 See the [sample code](@ref Output-Port-Recipe) for an example implementation.

 If you are using the Audio Unit Wrapper, then there's nothing you need to do - the Wrapper takes
 care of muting your output for you. Otherwise, take heed!

@section Low-Buffer-Durations Use Low IO Buffer Durations, If You Can

 Core Audio allows apps to set a preferred IO buffer duration via the audio session (see
 `kAudioSessionProperty_PreferredHardwareIOBufferDuration` in the Core Audio documentation). This
 setting configures the length of the buffers the audio system manages. Shorter buffers mean
 lower latency. By the time you receive a 5ms buffer from the system input, for example,
 roughly 5ms have elapsed since the audio reached the microphone.  Similarly, by the time a
 5ms buffer has been played by the system's speaker, 5ms or so have elapsed since the
 audio was generated.

 As Audiobus's transport system requires a little bit of buffer juggling, the longer your app's
 buffers are, the greater the latency will be.

 The tradeoff of small IO buffer durations is that your app has to work harder, per time unit,
 as it's processing smaller blocks of audio, more frequently. So, it's up to you to figure out
 how low your app's latency can go - but remember to save some CPU cycles for other apps as well!

@section Background-Mode In the Background Suspend When Possible, But Not When Connected
 
 It's up to you whether it's appropriate to suspend your app in the background, but there are a few
 things to keep in mind.
 
 Most important: You should never, ever suspend your app if it's connected via Audiobus. You can tell
 whether your app's connected at any time via the [connected](@ref ABAudiobusController::connected)
 property of the Audiobus controller.  If the value is YES, then you mustn't suspend.
 
 Generally speaking, unless there's a good reason not to, you should suspend your app in the background
 to free up resources for other apps. Many users still aren't familiar with the iOS task manager, and
 having an app that runs all the time until manually quit can confuse users, aside from running the
 device battery down more quickly.
 
 See the sample projects bundled with the SDK distribution, or the 
 [application state example code](@ref Application-State) for a proposed implementation.
 
 Note that during development, if your app has not yet been [registered](http://developer.audiob.us/new-app)
 with Audiobus, Audiobus will not be able to see the app if it is not actively running in the background.
 Consequently, we **strongly recommend** that you register your app at the beginning of development.
 
@section Efficient Be Efficient!

 Audiobus leans heavily on iOS multitasking! You could be running three synth apps, two filter apps,
 and be recording into a live-looper or a DAW. That requires a lot of juice.

 So, be kind to your fellow developers. Profile your app and find places where you can back off
 the CPU a bit. Never, ever hold locks, allocate memory, or call Objective-C functions from Core
 Audio. Use plain old C in time-critical places (or even drop to assembly). Take a look at the
 Accelerate framework if you're not familiar with it, and use its vector operations instead of
 scalar operations within loops - it makes a huge difference.

*/