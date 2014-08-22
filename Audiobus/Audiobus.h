//
//  Audiobus.h
//  Audiobus
//
//  Created by Michael Tyson on 10/12/2011.
//  Copyright (c) 2011-2014 Audiobus. All rights reserved.
//

#import "ABCommon.h"
#import "ABReceiverPort.h"
#import "ABSenderPort.h"
#import "ABFilterPort.h"
#import "ABAudiobusController.h"
#import "ABPeer.h"
#import "ABPort.h"
#import "ABTrigger.h"
#import "ABButtonTrigger.h"
#import "ABAnimatedTrigger.h"
#import "ABMultiStreamBuffer.h"

#define ABSDKVersionString @"2.1.2"

/*!
@mainpage

@section Introduction

 <blockquote>
 If you're already familiar with Audiobus and are integrating the 2.x version of the Audiobus SDK,
 then check out the [Migration Guide](@ref Migration-Guide) to find out what's changed.
 </blockquote>
 
 Audiobus is an SDK and accompanying [controller app](http://audiob.us/download) that allows iOS 
 apps to stream audio to one another. Just like audio cables, Audiobus lets you connect apps 
 together as modules, to  build sophisticated audio production and processing configurations.

 The Audiobus SDK provides all you need to make your app Audiobus compatible.  It's designed to be
 extremely easy to use: depending on your app, you should be up and running with Audiobus well within 
 a couple of hours.

 The SDK contains:

- The Audiobus library and headers
- An Xcode project with a number of sample apps that you can build and run
- A README file with a link to this documentation

@section Dont-Panic Don't Panic!

 We've worked hard to make Audiobus a piece of cake to integrate. Most developers will be able
 to have a functional integration within thirty minutes. Really.

 If your app's based around Remote IO audio units, then there's very little you'll need to
 do, particularly if it just produces audio and doesn't record it. This document will take you 
 through the process of integrating Audiobus.
 
 The process involves:
 - [Adding the Audiobus SDK files to your project](@ref Project-Setup), with or without CocoaPods,
 - [Enabling Inter-App Audio and Background Audio](@ref Audio-Setup),
 - Creating a [launch URL](@ref Launch-URL) and [registering your app with us](@ref Register-App),
 - Creating instances of the [Audiobus Controller](@ref Create-Controller), and
   [Receiver](@ref Create-Receiver-Port), [Filter](@ref Create-Filter-Port) 
   and/or [Sender](@ref Create-Sender-Port) ports.
 - [Testing](@ref Test)
 - [Going live](@ref Go-Live)

 Easy-peasy.

 If you wanna do some more advanced stuff, like 
 [receiving individual audio streams separately](@ref Receiving-Separate-Streams),
 allowing your app to be [controlled remotely](@ref Triggers), or implementing 
 [state saving](@ref State-Saving) then this document will explain how that's done, too.

 Finally, if you need a little extra help, or just wanna meet and talk with us or other
 Audiobus-compatible app developers, come say hello on the [developer community forum](http://heroes.audiob.us).

@section Capabilities Capabilities: Inputs, Effects and Outputs

 <img src="overview.png" width="570" height="422" title="Audiobus Peers and Ports" />
 
 Audiobus defines three different capabilities that an Audiobus-compatible app can have: sending,
 filtering and receiving. Your app can perform several of these roles at once. You create sender,
 receiver and/or filter ports when your app starts, and/or as your app's state changes.

 **Senders** transmit audio to other apps (receivers or filters). A sender will typically send the
 audio that it's currently playing out of the device's audio output device. For
 example, a musical instrument app will send the sounds the user is currently playing.

 **Receivers** accept audio from sender or filter apps. What is done with the received audio depends
 on the app. A simple recorder app might just save the recorded audio to disk. A multi-track recorder 
 might save the audio from each sender app as a separate track. An audio analysis app might display 
 information about the nature of the received audio, live.
 
 **Filters** accept audio input, process it, then send it onwards to another app over Audiobus. This
 allows applications to apply effects to the audio stream. Filters also behave as inputs or receivers,
 and can go in the "Input" and "Output" positions in Audiobus.

 Receiver apps can receive from one or more sources, and filters can accept audio from multiple sources.
 
 Receivers can receive audio from connected source apps in two ways: mixed down to a single stereo stream,
 or with one stereo stream per connected source.
 
 By setting the [receiveMixedAudio](@ref ABReceiverPort::receiveMixedAudio) property of the port to YES
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
 audio from Audiobus, set aside ten minutes to read through to make sure you have a
 clear picture of how it all works.
 </blockquote>
 
 Many app developers will be able to implement Audiobus in just thirty minutes or so.

 This quick-start guide assumes your app uses Remote IO. If this is not the case, most of it
 will still be relevant, but you'll need to do some additional integration work which is beyond
 the scope of this documentation.
 
@section General-Principles General Design Principles
 
 We've worked hard to make Audiobus as close as possible to an "it just works" experience for 
 users. We think music on iOS should be easy and open to everyone, not just those technical 
 enough to understand convoluted settings.
 
 That means you should add **no switches to enable/disable Audiobus, no settings that users need 
 to configure to enable your app to run in the background while connected to Audiobus**.

 If you're a sender app or a filter app (i.e. you have an ABSenderPort and/or an 
 ABFilterPort, and only send audio to other apps or filter audio from other apps), you shouldn't
 need to ever add any Audiobus-specific UI. Audiobus takes care of all session management for
 you. If you're a receiver app (you have an ABReceiverPort) then unless you're doing nifty things
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
 
 Audiobus' sender port is extremely light when not connected: the send function ABSenderPortSend
 will consume a negligible amount of CPU, so you can use it even while not connected to Audiobus, for
 convenience.
 
 If you find yourself implementing stuff that seems like it should've been in Audiobus, tell us. 
 It's probably already in there. If it's not, we'd be happy to consider putting it in ourselves 
 so you, and those who come after you, don't have to.
 
 In short: whenever possible, keep it simple. Your users will thank you, and you'll have more
 development time to devote to the things you care about.
 
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

 <blockquote class="alert" id="audiobus-ios7-bug">
 Due to an iOS 7 bug, in order to make your app work with Audiobus on iOS 7 you must ensure that:
 
 1. Your app's Bundle Name is identical to your app's Product Name.
 2. Both Bundle Name and Product Name are less than 16 characters in length.
 
 If you do not adhere to these limitations, you will see an Audiobus error
 within your app's console output informing you that "There was a problem setting up Audiobus communication".
 
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
 
 <blockquote class="alert" id="retronyms-audioio-bug">
 There is a sample project, [audioIO](http://blog.retronyms.com/2013/09/ios7-remoteio-inter-app-audiobus-and-you.html),
 which can be used as starting point for audio apps. There's a problem in this code's
 `AudioUnitPropertyChangeDispatcher` function, where it calls `[audio addAudioUnitPropertyListener]`.
 This modifies the audio unit property change notification dispatch table mid-dispatch, which causes a
 data integrity error that causes other registered notify callbacks to not be called. This causes problems
 within the Audiobus library, which relies on these notifications - in particular, it causes silent audio
 in sender and filter ports, among other things.
 
 Removing this `[audio addAudioUnitPropertyListener]` line addresses the problem.
 </blockquote>
 
 <blockquote class="alert" id="audio-session-warning">
 If you're interacting with the audio session (via AVAudioSession or the old C API), you **must** set the
 audio session category and "mix with others" flag *before* setting the audio session active. If you do
 this the other way around, you'll get some weird behaviour, like silent output when used with IAA.
 </blockquote>
 
@section Project-Setup 2. Add the Audiobus SDK to Your Project

 Audiobus is distributed as a static library, plus the associated header files.
 
 The easiest way to add Audiobus to your project is using [CocoaPods](http://cocoapods.org):
 
 1. Add "pod 'Audiobus'" to your Podfile, or, if you don't have one: at the top level of your project
    folder, create a file called "Podfile" with the following content:
    @code
    pod 'Audiobus'
    @endcode
 2. Then, in the terminal and in the same folder, type:
    @code
    pod install
    @endcode
    You will be asked for a login: use your Audiobus developer center email and password.
    
    In the future when you're updating your app, use `pod outdated` to check for available updates,
    and `pod update` to apply those updates.
 
 Alternatively, if you aren't using CocoaPods:

 1. Copy libAudiobus.a and the associated header files into an appropriate place within
    your project directory. We recommend putting these within an "Audiobus" folder within a "Library"
    folder (`Library/Audiobus`).
 2. Drag both the header files and libAudiobus.a into your project. In the sheet that appears,  make
    sure your app target is selected. Note that this will modify your app's "Header Search Paths" and
    "Library Search Paths" build settings.
 3. Ensure the following frameworks are added to your build process (to add frameworks,
    select your app target's "Link Binary With Libraries" build phase, and click the "+"
    button):
    - CoreGraphics
    - Accelerate
    - AudioToolbox
    - QuartzCore
    - Security
 
 Note that for technical reasons the Audiobus SDK supports iOS 7.0 and up only.

@section Audio-Setup 3. Enable Background Audio and Inter-App Audio

 If you haven't already done so, you must enable background audio and Inter-App Audio in your app.
 
 To enable these:

 1. Open your app target screen within Xcode by selecting your project entry at the top of Xcode's 
    Project Navigator, and selecting your app from under the "TARGETS" heading.
 2. Select the "Capabilities" tab.
 3. Underneath the "Background Modes" section, make sure you have "Audio and AirPlay" ticked.
 4. To the right of the "Inter-App Audio" title, turn the switch to the "ON" position -- this will
    cause Xcode to update your App ID with Apple's "Certificates, Identifiers & Profiles" portal,
    and create or update an Entitlements file.

@subsection Lifecycle Managing Your App's Life-Cycle

 Your app will only continue to run in the background if you have an *active, running*
 audio system. This means that if you stop your audio system while your app is in the background
 or moving to the background, your app will cease to run and will become unresponsive to
 Audiobus.
 
 Consequently, care must be taken to ensure your app is running and available when it needs to be.
 
 Firstly, **you must ensure you have a running and active audio session** once your app is connected
 via Audiobus, regardless of the state of your app. You can do this two ways: 
 
 1. Make sure you only instantiate the Audiobus controller ([Step 7](@ref Create-Controller))
    once your audio system is running.
 2. Register to receive [ABConnectionsChangedNotification](@ref ABConnectionsChangedNotification)
    notifications (or observe ABAudiobusController's connected property), and start your audio engine
    if the Audiobus controller is [connected](@ref ABAudiobusController::connected).
 
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
 
 The following describes the background policy we strongly recommend for use with Audiobus.
 
 1. When your app moves to the background, you should only stop your audio engine if (a) you are
    not currently connected via either Audiobus or Inter-App Audio, which can be determined via
    the [connected](@ref ABAudiobusController::connected) property of ABAudiobusController and 
    (b) Audiobus is not active, which can be determined via the 
    [audiobusAppRunning](@ref ABAudiobusController::audiobusAppRunning) property. For example:
 
     @code
     -(void)applicationDidEnterBackground:(NSNotification *)notification {
         if ( !_audiobusController.connected && !_audiobusController.audiobusAppRunning && _audioEngine.running ) {
            // Stop the audio engine, suspending the app, if Audiobus isn't running
            [_audioEngine stop];
         }
     }
     @endcode

 2. Your app should continue to remain active in the background while connected and while Audiobus is running.
    When you are disconnected and Audiobus quits, your app should suspend too. You can do this by observing
    the two above properties. Once both are NO, stop your audio engine as appropriate:
 
    @code
    static void * kAudiobusRunningOrConnectedChanged = &kAudiobusRunningOrConnectedChanged;
 
    ...
 
    // Watch the audiobusAppRunning and connected properties
    [_audiobusController addObserver:self 
                          forKeyPath:@"connected"
                             options:0 
                             context:kAudiobusRunningOrConnectedChanged];
    [_audiobusController addObserver:self 
                          forKeyPath:@"audiobusAppRunning" 
                             options:0 
                             context:kAudiobusRunningOrConnectedChanged];
 
    
    ...
 
    -(void)observeValueForKeyPath:(NSString *)keyPath
                         ofObject:(id)object
                           change:(NSDictionary *)change
                          context:(void *)context {
 
        if ( context == kAudiobusRunningOrConnectedChanged ) {
            if ( [UIApplication sharedApplication].applicationState == UIApplicationStateBackground
                   && !_audiobusController.connected
                   && !_audiobusController.audiobusAppRunning
                   && _audioEngine.running ) {
 
                // Audiobus has quit. Time to sleep.
                [_audioEngine stop];
            }
        } else {
            [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
        }
    }
    @endcode
 
 3. When your app moves to the foreground, start your audio engine:
 
    @code
    -(void)applicationWillEnterForeground:(NSNotification *)notification {
        if ( !_audioEngine.running ) {
            // Start the audio system if it wasn't running
            [_audioEngine start];
        }
    }
    @endcode
 
 Note that during development, if you have not yet registered your app with Audiobus
 ([Step 5](@ref Register-App)), the Audiobus app will only be able to see your app while
 it is running. Consequently we **strongly recommend** registering your app before you 
 begin testing.

@section Launch-URL 4. Set up a Launch URL

 Audiobus needs a URL (like `YourApp-1.0.audiobus://`) that can be used to launch and switch to
 your app, and used to determine if your app is installed.
 
 The URL scheme needs to end in ".audiobus", to ensure that Audiobus app URLs are unique. This URL 
 also needs to be unique to each version of your app, so Audiobus can tell each version apart, 
 which is important when you add new Audiobus features.
 
 Here's how to add the new URL scheme to your app.

 1. Open your app target screen within Xcode by selecting your project entry at the top of Xcode's 
    Project Navigator, and selecting your app from under the "TARGETS" heading.
 2. Select the "Info" tab.
 3. Open the "URL types" group at the bottom.
 4. If you don't already have a URL type created, click the "Add" button at the bottom 
    left. Then enter an identifier for the URL (a reverse DNS string that identifies your app, like 
    "com.yourcompany.yourapp", will suffice).
 5. If you already have existing URL schemes defined for your app, add a comma and space (", ") 
    after the last one in URL Schemes field (Note: the space after the comma is important).
 6. Now enter the new Audiobus URL scheme for your app, such as "YourApp-1.0.audiobus". Note
    that this is just the URL scheme component, not including the "://" characters).

<img src="url-scheme.jpg" title="Adding a URL Scheme" width="570" height="89" />

 Other apps will now be able to switch to your app by opening the `YourApp-1.0.audiobus://` URL.
 
@section Register-App 5. Register Your App and Generate Your API Key

 Audiobus contains an app registry which is used to enumerate Audiobus-compatible apps that
 are installed. This allows apps to be seen by Audiobus even if they are not actively running
 in the background. The registry also allows users to discover and purchase apps that support Audiobus.
 
 Register your app, and receive an Audiobus API key, at the 
 [Audiobus app registration page](http://developer.audiob.us/new-app).

 You'll need to provide various details about your app, and you'll need to provide a copy of your
 compiled Info.plist from your app bundle, which Audiobus will use to populate the required fields.
 You'll be able to edit all of these details up until the time you go live with your app.
 
 After you register, we will briefly review your application. Upon approval, you will be notified via
 email, which will include your Audiobus API key, and the app will be added to the Audiobus registry.
 
 You can always look up your API key by visiting http://developer.audiob.us/apps and clicking on your
 app. The API key is at the top of the app details page.
 
 The API key is a string that you provide when you use the Audiobus SDK. It is unique to each version
 of your app, and tied to your bundle name and launch URL. It will be checked by the SDK upon 
 initialisation, to provide automatic error checking. No network connection is required to verify the key.
 
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
 > give Audiobus users the ability to purchase your app, you need to you make your app live
 > ([Step 10](@ref Go-Live)). Do this only when the Audiobus-compatible
 > version of your app goes live on the App Store, so as not to confuse users.
 
 As you develop your app further, beyond this initial integration of Audiobus, we recommend you register
 new versions of your app with us when you add new Audiobus functionality, like adding new ports or
 implementing features like state saving. This will both allow Audiobus to correctly advertise the new
 features in your new version, and will boost your sales when your app appears at the top of our
 compatible apps directly again. You can register new versions of your app by clicking "Add Version" on
 your app page.
 
@section Enable-Mixing 6. Enable mixing audio with other apps

 When you use audio on iOS, you typically select one of several audio session categories,
 usually either `AVAudioSessionCategoryPlayAndRecord` or `AVAudioSessionCategoryPlayback`.

 By default, both of these categories will cause iOS to interrupt the audio session of any other
 app running at the time your app is started, **forcing the other app to suspend**.

 If you are using either `PlayAndRecord` or `MediaPlayback`, then in order to use Audiobus you
 need to **override this default**, and tell iOS to allow other apps to run at the same time and
 mix the output of all running apps.

 To do this, you need to set the `AVAudioSessionCategoryOptionMixWithOthers` flag, like so:

 @code
 NSString *category = AVAudioSessionCategoryPlayAndRecord;
 AVAudioSessionCategoryOptions options = AVAudioSessionCategoryOptionMixWithOthers;

 NSError *error = nil;
 if ( ![[AVAudioSession sharedInstance] setCategory:category withOptions:options error:&error] ) {
     NSLog(@"Couldn't set audio session category: %@", error);
 }
 @endcode

 Note that with the old Audio Session C API, adjusting other session properties can interfere with
 this property setting, causing other apps to be interrupted despite the mix property being set.
 Consequently, be sure to reset the `kAudioSessionProperty_OverrideCategoryMixWithOthers` property value
 whenever you assign any audio session properties.
 
 <blockquote class="alert" id="audio-session-warning">
 If you're interacting with the audio session (via AVAudioSession or the old C API), you **must** set the
 audio session category and "mix with others" flag *before* setting the audio session active. If you do
 this the other way around, you'll get some weird behaviour, like silent output when used with IAA.
 </blockquote>

@section Create-Controller 7. Instantiate the Audiobus Controller

 Next, you need to create a strong property for an instance of the Audiobus Controller. A convenient place
 to do this is in your app's delegate, or within your audio engine class.

 First, import the Audiobus header from your class's implementation file:

 @code
 #import "Audiobus.h"
 @endcode

 Next declare a strong (retaining) property for the instance from within a class extension:

 @code
 @interface MyAppDelegate ()
 @property (strong, nonatomic) ABAudiobusController *audiobusController;
 @end
 @endcode

 Now you'll need to create an instance of the Audiobus controller. A convenient place to do this
 is in your app delegate's `application:didFinishLaunchingWithOptions:` method, or perhaps within your
 audio engine's initialiser, but there are three very important caveats:
 
 First: you must either **start your audio system at the same time as you initialise Audiobus**, or you must watch for
 @link ABConnectionsChangedNotification @endlink and **start your audio system when the ABConnectionsChangedNotification
 is received**.  This is because as soon as your app is connected via Audiobus, your app **must have a running and active
 audio system**, or a race condition may occur wherein your app may suspend in the background
 before an Audiobus connection has been completed.
 
 Second: you must instantiate the Audiobus controller **on the main thread only**. If you do not, Audiobus
 will trigger an assertion.
 
 Third: you **must not hold up the main thread after initialising the Audiobus controller**. Due to
 an issue in Apple's service browser code, if the main thread is blocked for more than a couple of seconds,
 Audiobus peer discovery will fail, causing your app to refuse to respond to the Audiobus app. If you
 need to take more than a second or two to initialise your app, initialise the Audiobus controller afterwards,
 or do that processing in a background thread.
 
 > You must initialise ABAudiobusController as close to app launch as is possible, and you must keep the instance
 > around for the entire life of your app. If you release and create a new instance of ABAudiobusController, you
 > will see some odd behaviour, such as your app failing to connect to Audiobus.
 
 Create the ABAudiobusController instance, passing it the API key that you generated when you registered 
 your app in [Step 5](@ref Register-App):

 @code
 self.audiobusController = [[ABAudiobusController alloc] initWithApiKey:@"YOUR-API-KEY"];
 @endcode

 At certain times, Audiobus will display the Connection Panel within your app. This is a slim
 panel that appears at the side of the screen, that users can drag off the screen, and swipe
 from the edge of the screen to re-display, a bit like the iOS notification screen.

 By default, the Connection Panel appears at the right of the screen. If this does not work
 well with your app's UI, you can select [another location](@ref ABConnectionPanelPosition)
 for the panel:

 @code
 self.audiobusController.connectionPanelPosition = ABConnectionPanelPositionLeft;
 @endcode

 You can change this value at any time (such as after significant user interface orientation changes),
 and Audiobus will automatically animate the panel to the new location.
 
 > If the connection panel is on the bottom of the screen, it cannot be hidden by
 > the user. This is to avoid interference by the iOS Control Center panel.

@section Create-Ports 8. Create Ports

 Now you're ready to create your Audiobus ports.
 
 You can make as many ports as you like. For example, a multi-track recorder could provide per-track outputs, or
 an effect app with side-chain processing could create a main effect port, and a sidechain port. We recommend
 being generous with your port offering, to enable maximum flexibility, such as per-track routing. Take a look at
 Loopy or Loopy HD for an example of the use of multiple ports.
 
 Note that you should create all your ports when your app starts, regardless of whether you intend to use them 
 straight away, or you'll get some weird behaviour. If you're not using them, just keep them silent (or inactive, 
 by not calling the receive/send functions).

 @subsection Create-Sender-Port Sender Port
 
 If you intend to send audio, then you'll need to create an ABSenderPort.

 The first sender port you define will be the one that Audiobus will connect to when the user taps your app
 in the port picker within Audiobus, so it's best to define the port with the most general, default behaviour
 first.

 Firstly, you'll need to create an AudioComponents entry within your app's Info.plist. This identifies your
 port to other apps. If you have integrated Inter-App Audio separately, and you already have an AudioComponents entry,
 you can use these values with your ABSenderPort without issue. Otherwise:

 1. Open your app target screen within Xcode by selecting your project entry at the top of Xcode's 
    Project Navigator, and selecting your app from under the "TARGETS" heading.
 2. Select the "Info" tab.
 3. If you don't already have an "AudioComponents" group, then under the "Custom iOS Target Properties" 
    group, right-click and select "Add Row", then name it "AudioComponents". Set the type to "Array" in
    the second column.
 4. Open up the "AudioComponents" group by clicking on the disclosure triangle, then right-click on 
    "AudioComponents" and select "Add Row". Set the type of the row in the second column to "Dictionary". 
    Now make sure the new row is selected, and open up the new group using its disclosure triangle.
 5. Create five different new rows, by pressing Enter to create a new row and editing its properties:
    - "manufacturer" (of type String): set this to any four-letter code that identifies you (like "abus")
    - "type" (of type String): set this to "aurg", which means that we are identifying a "Remote Generator" audio unit,
      or "auri" for a "Remote Instrument" unit.
    - "subtype" (of type String): set this to any four-letter code that identifies the port.
    - "name" (of type String): set this to the name of your app, or another string that identifies your app and the port.
    - "version" (of type Number): set this to any integer (whole number) you like. "1" is a good place to start.

 Once you're done, it should look something like this:

 <img src="audio-component.jpg" title="Adding an Audio Component" width="456" height="129" />

 > It's very important that you use a different AudioComponentDescription for each port. If you don't have a
 > unique AudioComponentDescription per port, you'll get all sorts of Inter-App Audio errors (like error -66750 or
 > -10879).
 
 Now it's time to create an ABSenderPort instance. You provide a port name, for internal use, and a port 
 title which is displayed to the user. You can localise the port title.
 
 You may choose to provide your IO audio unit (of type kAudioUnitSubType_RemoteIO), which will cause the sender port 
 to automatically capture and send the audio output. This is the recommended, easiest, and most efficient approach.
 
 Alternatively, if you're creating secondary ports, or have another good reason for not using your IO audio unit with the
 sender port at all, then you send audio by calling @link ABSenderPort::ABSenderPortSend ABSenderPortSend @endlink,
 then mute your audio output depending on the value of @link ABSenderPort::ABSenderPortIsMuted ABSenderPortIsMuted @endlink.

 > ABSenderPort when initialized without an audio unit will create and publish its own audio unit with the
 > AudioComponentDescription you pass into the initializer. If you are planning on using ABSenderPort without an audio
 > unit (you're not passing an audio unit into the initializer), then you **must not** publish any other audio unit with
 > the same AudioComponentDescription. Otherwise, *two audio units will be published with the same AudioComponentDescription*, 
 > which would be bad, and would result in unexpected behaviour like silent output.
 > <br/>
 > If you're using ABSenderPort without an audio unit for the purposes of offering a new, separate audio stream
 > with a different AudioComponentDescription, though, you're fine.
 
 > If you are using a sender port and *not* initialising it with your audio unit, you **must**
 > mute your app's corresponding audio output when needed, depending on the value of the
 > @link ABSenderPort::ABSenderPortIsMuted ABSenderPortIsMuted @endlink function. This is very important and
 > both avoids doubling up the audio signal, and lets your app go silent when removed from Audiobus. See the 
 > [Sender Port recipe](@ref Sender-Port-Recipe) and the AB Receiver sample app for details.

 Finally, you need to pass in an AudioComponentDescription structure that contains the same details as the
 AudioComponents entry you added earlier.

 @code
 ABSenderPort *sender = [[ABSenderPort alloc] initWithName:@"Audio Output"
                                                     title:NSLocalizedString(@"Main App Output", @"")
                                 audioComponentDescription:(AudioComponentDescription) {
                                     .componentType = kAudioUnitType_RemoteGenerator,
                                     .componentSubType = 'aout', // Note single quotes
                                     .componentManufacturer = 'you!' }
                                                 audioUnit:_audioUnit];
 
 [_audiobusController addSenderPort:sender];
 @endcode
 
 If your sender port's audio audio comes from the system audio input (such as a microphone),
 then you should set the port's @link ABSenderPort::derivedFromLiveAudioSource derivedFromLiveAudioSource @endlink
 property to YES to allow Audiobus to be able to warn users if they are in danger of creating audio feedback.
 
 You may also optionally provide an icon (a 32x32 mask, with transparency) via the [icon](@ref ABSenderPort::icon) property, 
 which is also displayed to the user and can change dynamically. We strongly recommend providing icons if you
 publish more than one port, so these can be recognized from one another. If you provide an icon here, you should
 also add that icon to the port on your app's registry on our developer site, so it can be displayed to users
 prior to your app being launched.

 If you've already integrated Inter-App Audio separately, you should hide your IAA transport and
 app switching UI while connected with Audiobus. This is to avoid confusion with the Audiobus Connection Panel.
 You can determine when your app is connected specifically to Audiobus, and not Inter-App Audio, via
 ABAudiobusController's [audiobusConnected](@ref ABAudiobusController::audiobusConnected) property.
 
 Conversely, note that when your app's ABSenderPort is hosted within an Inter-App Audio-compatible
 app outside of Audiobus, you are responsible for implementing the appropriate transport and app switch UI if
 you choose to do so.

 @subsection Create-Filter-Port Filter Port
 
 If you intend to filter audio, to act as an audio effect, then create an ABFilterPort.

 This process is very similar to [creating a sender port](@ref Create-Sender-Port). You need to create an
 Info.plist AudioComponents entry for your port, this time using 'aurx' as the type, which identifies the 
 port as a Remote Effect, or 'aurm' which identifies it as a Remote Music Effect.

 > It's very important that you use a different AudioComponentDescription for each port. If you don't have a
 > unique AudioComponentDescription per port, you'll get all sorts of Inter-App Audio errors (like error -66750 or
 > -10879).
 
 Then you create an ABFilterPort instance, passing in the port name, for internal use, and a title for
 display to the user.

 Again, you may provide your IO audio unit (of type kAudioUnitSubType_RemoteIO, with input enabled), which will cause 
 the filter to use your audio unit for processing. This is the easiest, most efficient and recommended approach.
 
 @code
 self.filter = [[ABFilterPort alloc] initWithName:@"Main Effect"
                                            title:@"Main Effect"
                        audioComponentDescription:(AudioComponentDescription) {
                            .componentType = kAudioUnitType_RemoteEffect,
                            .componentSubType = 'myfx',
                            .componentManufacturer = 'you!' }
                                        audioUnit:_ioUnit];
 
 [_audiobusController addFilterPort:_filter];
 @endcode
 
 Alternatively, if you have a good reason for not using your IO audio unit with the filter port, you can use ABFilterPort's
 @link ABFilterPort::initWithName:title:audioComponentDescription:processBlock:processBlockSize: process block initializer @endlink.
 This allows you to pass in a block to use for audio processing.

 @code
 self.filter = [[ABFilterPort alloc] initWithName:@"Main Effect"
                                            title:@"Main Effect"
                        audioComponentDescription:(AudioComponentDescription) {
                            .componentType = kAudioUnitType_RemoteEffect,
                            .componentSubType = 'myfx',
                            .componentManufacturer = 'you!' }
                                     processBlock:^(AudioBufferList *audio, UInt32 frames, AudioTimeStamp *timestamp) {
                                         // Process audio here
                                     } processBlockSize:0];
 @endcode
 
 Note that if you intend to use a process block instead of an audio unit, you are responsible for muting
 your app's normal audio output when the filter port is connected. See the 
 [Filter Port recipe](@ref Filter-Port-Recipe) for details.
 
 > ABFilterPort, when initialized with a filter block (instead of an audio unit) will create and publish its own audio unit with the
 > AudioComponentDescription you pass into the initializer. If you are planning on using ABFilterPort with a process block,
 > instead of an audio unit, then you **must not** publish any other audio unit with
 > the same AudioComponentDescription. Otherwise, *two audio units will be published with the same AudioComponentDescription*,
 > which would be bad, and would result in unexpected behaviour like silent output.
 > <br/>
 > If you're using ABFilterPort with a filter block for the purposes of offering a new, separate audio processing
 > facility, separate from your published audio unit, and with a different AudioComponentDescription, though, you're fine.

 You may also optionally provide an icon (a 32x32 mask, with transparency) via the [icon](@ref ABFilterPort::icon) property, 
 which is also displayed to the user and can change dynamically. We strongly recommend providing icons if you
 publish more than one port, so these can be recognized from one another. If you provide an icon here, you should
 also add that icon to the port on your app's registry on our developer site, so it can be displayed to users
 prior to your app being launched.

 If, outside of Audiobus, your app processes audio from the system audio input, and provides monitoring via the system
 output (it probably does!), we strongly suggest muting your app when it's launched from Audiobus. This will avoid the
 case where the user experiences feedback in the second or so after your app is initialized, but before your app is 
 connected within Audiobus. Take a look at the AB Filter sample app for a demonstration of how this can be achieved,
 by checking to see if your app was launched via its Audiobus launch URL, and silencing the audio engine for the duration:

 @code
 @implementation AppDelegate
 ...
 - (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
     if ( [[url scheme] hasSuffix:@".audiobus"] ) {
         // Tell the audio engine we were launched from Audiobus
         [_audioEngine setLaunchedFromAudiobus];
     }
     
     return YES;
 }
 ...
 @end

 ...

 @implementation MyAudioEngine
 ...
 -(void)setLaunchedFromAudiobus {
     // If this effect app has been launched from within Audiobus, we need to silence our output for a little while
     // to avoid feedback issues while the connection is established.
     _launchedFromAudiobus = YES;
     if ( _launchedFromAudiobus && !_audiobusController.connected ) {
         // Mute
         self.muted = YES;
         
         // Set a timeout for three seconds, after which we can assume there's actually no
         // Audiobus connection forthcoming (something went wrong), and we should unmute
         dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3.0 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
             self.muted = NO;
         });
     }
 }
 ...
 -(void)portConnectionsChanged:(NSNotification*)notification {
     if ( _muted ) {
         // Unmute now if we were launched from Audiobus
         self.muted = NO;
     }
 }
 ...
 @end
 @endcode

 If you've already integrated Inter-App Audio separately, you should hide your IAA transport and
 app switching UI while connected with Audiobus. This is to avoid confusion with the Audiobus Connection Panel.
 You can determine when your app is connected specifically to Audiobus, and not Inter-App Audio, via
 ABAudiobusController's [audiobusConnected](@ref ABAudiobusController::audiobusConnected) property.
 
 Conversely, note that when your app's ABFilterPort is hosted within an Inter-App Audio-compatible
 app outside of Audiobus, you are responsible for implementing the appropriate transport and app switch UI if
 you choose to do so.
 
 @subsection Create-Receiver-Port Receiver Port
 
 If you intend to receive audio, then you create an ABReceiverPort.

 ABReceiverPort works slightly differently to ABSenderPort and ABFilterPort: it does not use an audio unit,
 nor does it require an AudioComponentDescription. Instead, you call 
 @link ABReceiverPort::ABReceiverPortReceive ABReceiverPortReceive @endlink to receive audio.

 First, create the receiver, and store it so you can use it to receive audio:

 @code
 @property (nonatomic, strong) ABReceiverPort *receiverPort;
 @endcode

 @code
 self.receiverPort = [[ABReceiverPort alloc] initWithName:@"Audio Input"
                                                    title:NSLocalizedString(@"Main App Input", @"")];
 [_audiobusController addReceiverPort:_receiverPort];
 @endcode

 Now set up the port's @link ABReceiverPort::clientFormat clientFormat @endlink property to whatever
 PCM `AudioStreamBasicDescription` you are using (such as non-interleaved stereo floating-point PCM):

 @code
 AudioStreamBasicDescription audioDescription = {
    .mFormatID          = kAudioFormatLinearPCM,
    .mFormatFlags       = kAudioFormatFlagIsFloat | kAudioFormatFlagIsPacked | kAudioFormatFlagIsNonInterleaved,
    .mChannelsPerFrame  = 2,
    .mBytesPerPacket    = sizeof(float),
    .mFramesPerPacket   = 1,
    .mBytesPerFrame     = sizeof(float),
    .mBitsPerChannel    = 8 * sizeof(float),
    .mSampleRate        = 44100.0
 };
 _receiverPort.clientFormat = audioDescription;
 @endcode
 
 Now you may receive audio using @link ABReceiverPort::ABReceiverPortReceive ABReceiverPortReceive @endlink, 
 in a similar fashion to calling  `AudioUnitRender` on an audio unit. For example, within a Remote iO input 
 callback, you might write:
 
 @code
 AudioTimeStamp timestamp = *inTimeStamp;
 if ( ABReceiverPortIsConnected(self->_receiverPort) ) {
    // Receive audio from Audiobus, if connected. Note that we also fetch the timestamp here, which is
    // useful for latency compensation, where appropriate.
    ABReceiverPortReceive(self->_receiverPort, nil, ioData, inNumberFrames, &timestamp);
 } else {
    // Receive audio from system input otherwise
    AudioUnitRender(self->_audioUnit, ioActionFlags, inTimeStamp, 1, inNumberFrames, ioData);
 }
 @endcode
 
 > The receiver port assumes you provide monitoring - where you pass the incoming audio to the system output
 > so the user can hear it. If you do not do so, the user won't be able to hear any apps that send audio to
 > your app. If that's the case, ABReceiverPort provides an automatic monitoring facility for you: just set
 > @link ABReceiverPort::automaticMonitoring automaticMonitoring @endlink to YES to use it.
 
 See [The Receiver Port](@ref Receiver-Port) or the [Receiver Port recipe](@ref Receiver-Port-Recipe) for 
 more info on receiving.
 
 You may also optionally provide an icon (a 32x32 mask, with transparency) via the [icon](@ref ABReceiverPort::icon) property, 
 which is also displayed to the user and can change dynamically. We strongly recommend providing icons if you
 publish more than one port, so these can be recognized from one another. If you provide an icon here, you should
 also add that icon to the port on your app's registry on our developer site, so it can be displayed to users
 prior to your app being launched.
 
 If you wish to receive multi-channel audio, with one audio stream for each connected app, see the section on
 [receiving separate streams](@ref Receiving-Separate-Streams).

 @subsection Update-Registry Update the Audiobus Registry
 
 Once you've set up your ports, open your [app page](http://developer.audiob.us/apps) on the Audiobus
 Developer Center and fill in any missing port details.
 
 We **strongly recommend** that you drop your compiled Info.plist into the indicated area in order to automatically
 populate the fields:
 
 1. This is much faster than putting them in yourself.
 2. This will ensure the details are free of errors, which could otherwise cause some "Port Unavailable" errors to be seen.
 3. This checks that you're not using AudioComponent fields that are already in use in another app, which would cause problems.
 
 Filling in the port details here allows all of your app's ports to be seen within Audiobus prior to your
 app being launched.
 
 > It's important that you fill in the "Ports" section correctly, matching the values you are using with your
 > instances of the ABSender, ABFilter and ABReceiver ports. If you don't do this correctly, you will see
 > "Port Unavailable" messages within Audiobus when trying to use your app.
 
@section Test 9. Test
 
 To test your app with Audiobus, you'll need the Audiobus app (http://audiob.us/download).
 
 You'll find a number of fully-functional sample apps in the "Samples" folder of the Audiobus SDK
 distribution. Use these to test your app with, along with other Audiobus-compatible apps you may own.
  
 <blockquote class="alert">We reserve the right to **ban your app** from the Compatible Apps listing or even from
 Audiobus entirely, if it does not work correctly with Audiobus. It's critical that you test your app properly.</blockquote>
 
@section Go-Live 10. Go Live
 
 Once the Audiobus-compatible version of your app has been approved by Apple and hits the App
 Store, you should visit the [apps page](http://developer.audiob.us/apps) and click "Go Live".
 
 This will result in your app being added to the Compatible Applications listing
 within Audiobus, and shown on Audiobus's website in various locations. We will also include your app
 in our daily app mailing list, and if anyone has subscribed at our [compatible apps listing](http://audiob.us/apps) 
 to be notified specifically when your app gains Audiobus support, they will be notified by email.
 
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

 If your app provides both an ABSenderPort and an ABReceiverPort, you may wish to allow users to 
 connect your app's output back to its input. If your app supports this kind of functionality, you can set the 
 @link ABAudiobusController::allowsConnectionsToSelf allowsConnectionsToSelf @endlink
 property to YES, and select the "Allows Connections To Self" checkbox on the app details
 page at [developer.audiob.us](http://developer.audiob.us/apps), once you've ensured that your app doesn't
 exhibit feedback issues in this configuration. See the documentation for
 @link ABSenderPort::ABSenderPortIsConnectedToSelf ABSenderPortIsConnectedToSelf @endlink
 /@link ABReceiverPort::ABReceiverPortIsConnectedToSelf ABReceiverPortIsConnectedToSelf @endlink for discussion,
 and the AB Receiver sample app for a demonstration.
 
 If you'd like to make your app more interactive, you can implement [triggers](@ref Triggers) that
 allow users to trigger actions in your app (like toggling recording, playback, etc) from other
 apps and devices.

 Finally, tell your users that you support Audiobus! We provide a set of graphical resources
 you can use on your site and in other promotional material. Take a look at
 the [resources page](http://developer.audiob.us/resources) for the details.

 Read on if you want to know about more advanced uses of Audiobus, such as multi-track
 [receiving](@ref Receiver-Port), [triggers](@ref Triggers), or [state saving](@ref State-Saving).

@page Migration-Guide 1.x-2.x Migration Guide
 
 This section is intended for developers who are already familiar with the 1.x version of the Audiobus SDK,
 who wish to update their apps to the 2.x version. It explains what's changed, and what you need to do to migrate.

 Note that for technical reasons the 2.x SDK is supported on iOS 7.0 and up only.

 @section Migration-Guide-Version Create New Version On Our Registry
 
 Before you begin your migration, you should create a new version of your app on our 
 [developer site](http://developer.audiob.us/apps). Make sure you select the correct Audiobus SDK version on the
 form: this will allow Audiobus on iOS 8 to recognize your app as compatible. Also make sure you're using a new,
 unique launch URL: this is how Audiobus will recognize the iOS 8-compatible version of your app.
 
 You'll also notice the new "Ports" section, replacing the "Has Input/Filter/Output Port" checkboxes. This allows
 your app to advertise multiple ports, ahead of launch time. **It's important to fill this section out correctly**,
 matching the "Name", and the new type, subtype and manufacturer fields, to the values you're using with your
 ports (see below for more details on the new Audiobus ports). If these fields are incorrect, you'll see
 "Port Unavailable" messages in Audiobus when trying to use your app.
 
 > If you don't perform this step, your app won't appear in Audiobus.
 
 When your app's update has gone live on the App Store, remember to mark this version as live, too, so we
 can report that your app supports iOS 8.
 
 @section Migration-Guide-IAA Inter-App Audio

 Audiobus now integrates Apple's Inter-App Audio system for audio communication. For users, this makes little
 functional difference aside from improved latency. For developers, integrating the Audiobus SDK automatically
 gives your app the ability to work with other Inter-App Audio hosts, outside of Audiobus.

 Audiobus' IAA integration works alongside existing IAA integrations, so there should be no conflicts as long as
 you heed the following guidelines:

 - Where possible, always use the audio unit initialisers for ABSenderPort and ABFilterPort -
   @link ABSenderPort::initWithName:title:audioComponentDescription:audioUnit: ABSenderPort's initWithName:title:audioComponentDescription:audioUnit: @endlink and
   @link ABFilterPort::initWithName:title:audioComponentDescription:audioUnit: ABFilterPort's initWithName:title:audioComponentDescription:audioUnit: @endlink.
   This will cause the ports to use your own audio units for generating or processing audio, which is both more efficient and most
   compatible with current IAA integrations.
 - If you must use the other, non-audio unit initialisers, you must either use a different AudioComponentDescription,
   or you must avoid publishing your own audio unit. Otherwise, both your own audio unit and the port's internal
   unit will be published with the same AudioComponentDescription, resulting in unexpected behaviour like silent output.
 
 > It's very important that you use a different AudioComponentDescription for each port. If you don't have a
 > unique AudioComponentDescription per port, you'll get all sorts of Inter-App Audio errors (like error -66750 or
 > -10879).
 
 Additionally, the new SDK remains fully backwards-compatible with apps using the 1.x version of the Audiobus
 SDK, on iOS 7.

 With the new IAA integration, your app needs to have Inter-App Audio enabled, and each
 sender port and filter port needs to have a corresponding entry in an "AudioComponents" section within your app's 
 Info.plist. Take a look at the [audio setup](@ref Audio-Setup) and [ports](@ref Create-Ports) sections of the 
 integration guide for details.

 If you've already integrated Inter-App Audio separately, you should hide your IAA transport and
 app switching UI while connected with Audiobus. This is to avoid confusion with the Audiobus Connection Panel.
 Conversely, note that when your app's ABSenderPort or ABFilterPort is hosted within an Inter-App Audio-compatible
 app outside of Audiobus, you are responsible for implementing the appropriate transport and app switch UI if you
 choose to do so.
 
 Note that, prior to the 2.1 Audiobus SDK, you could create and remove sender or filter ports dynamically, as needed.
 Now, as we're using Inter-App Audio under the hood, they need to be all created at startup, or you'll see some weird
 behaviour. If you've got a number of ports that you don't always need (for example, for multi-track playback), create
 them at startup and keep them silent until you need them.

 @section Migration-Guide-State-Saving State Saving

 A new feature of Audiobus 2.x is presets and state saving, allowing Audiobus to save state from every connected
 app as part of a preset, and recall it later. This lets users store and recall their entire workspace, as well
 as sharing their workspaces with others.

 Take a look at the [state saving](@ref State-Saving) documentation, and the
 @link ABAudiobusControllerStateIODelegate @endlink protocol for details.
 
 @section Migration-Guide-Controller The Audiobus Controller

 ABAudiobusController's initialization has been simplified a bit - the launchURL parameter has been removed in
 favour of automatically finding this in your app's Info.plist.
 
 @section Migration-Guide-Ports Ports

 A number of changes have been made to Audiobus' ports. Firstly, ABInputPort and ABOutputPort have been renamed
 to ABReceiverPort and ABSenderPort in order to clarify their function.

 Ports are now created by allocing and initializing them, then using ABAudiobusController's addSenderPort:,
 addFilterPort: and addReceiverPort: methods. ABSenderPort and ABFilterPort's initializers now take an
 AudioComponentDescription parameter that identifies the corresponding Inter-App Audio node.

 The Audio Unit Wrapper (ABAudiobusAudioUnitWrapper) is no more, instead replaced by functionality within the
 ports themselves. Now, you pass your audio unit in via the ABSenderPort/ABFilterPort initializer.
 
 We've improved support for having multiple ports, and encourage you to make use of this functionality. Now, all
 your app's ports can be registered at our Developer Center, allowing Audiobus to be aware of your extra ports before
 your app is running.
 
 > It's important that you fill in the new "Ports" section correctly, matching the values you are using with your
 > instances of the ABSender, ABFilter and ABReceiver ports. If you don't do this correctly, you will see
 > "Port Unavailable" messages within Audiobus when trying to use your app.

 @subsection Migration-Guide-Sender Sender ports

 ABOutputPort is now called ABSenderPort.

 If you're using ABSenderPort without the audio unit initializer, and are thus calling
 @link ABSenderPort::ABSenderPortSend ABSenderPortSend @endlink manually, you need to check 
 @link ABSenderPort::ABSenderPortIsMuted ABSenderPortIsMuted @endlink, and mute your audio if the return
 value is YES (like you used to with `(ABOutputPortGetConnectedPortAttributes() & ABInputPortAttributePlaysLiveAudio)`).
 
 ABSenderPort adds a new property, @link ABSenderPort::derivedFromLiveAudioSource derivedFromLiveAudioSource @endlink,
 which you can use to indicate that your port's audio comes from the system audio input (such as a microphone). If
 so, you should set this property to YES to allow Audiobus to be able to warn users if they are in danger of creating
 feedback.

 @subsection Migration-Guide-Filter Filter ports

 ABFilterPort is much easier to use now, with an audio unit initializer that lets you use your normal audio 
 system with no further coding required. You also don't need to do monitoring via ABFilterPortGetOutput any more.
 
 Filter ports can now work as senders and receivers, too, meaning they can appear in the "Input" and "Output"
 positions within Audiobus. That means you no longer need to implement sender and/or receiver ports in addition
 to filter ports, if you want your app to work in these positions.

 @subsection Migration-Guide-Receiver Receiver ports

 ABInputPort is now called ABReceiverPort.

 ABReceiverPort is now used by calling @link ABReceiverPort::ABReceiverPortReceive ABReceiverPortReceive @endlink
 directly; take a look at the [receiver port recipe](@ref Receiver-Port-Recipe) for an example, or read through
 the updated [integration guide section on the receiver port](@ref Create-Receiver-Port).

 Port attributes have been removed - you no longer need to set the 'PlaysLiveAudio' attribute to inform sources
 that your receiver app is doing monitoring. That's because we now assume you will always provide monitoring when
 connected, which is generally a safe bet. If for some reason you don't have audio monitoring built in - such as
 in the case of guitar tuner apps, etc - you can set ABReceiverPort's 
 @link ABReceiverPort::automaticMonitoring automaticMonitoring @endlink property to YES, and ABReceiverPort will
 do the monitoring for you.

 The live/lossless audio streams have been replaced with one audio stream type, which is automatically
 latency-adjusted and error-corrected when receiving from sources outside of the new Inter-App Audio system.
 This means receiving and recording is much simpler - you now no longer need to think about live vs lossless.

 The "allowsMultipleInstancesInConnectionGraph" property has been renamed to
 @link ABAudiobusController::allowsConnectionsToSelf allowsConnectionsToSelf @endlink to clarify its purpose,
 and @link ABSenderPort::ABSenderPortIsConnectedToSelf ABSenderPortIsConnectedToSelf @endlink
 /@link ABReceiverPort::ABReceiverPortIsConnectedToSelf ABReceiverPortIsConnectedToSelf @endlink functions have
 been added to determine when a port is connected to another port from the same app. Check out the discussion on
 the documentation for those methods, and the AB Receiver sample app for details.
 
 @section Migration-Guide-Lifecycle Lifecycle

 We've added a new key-value-observable property to ABAudiobusController,
 @link ABAudiobusController::audiobusAppRunning audiobusAppRunning @endlink, which lets you determine whether the 
 Audiobus app is running on the device, and we've updated our recommended app lifecycle policy. We now recommend
 that you keep your app running in the background whenever (a) your app is 
 [connected](@ref ABAudiobusController::connected), or (b) Audiobus is running, and only allow the app to suspend
 (by stopping your audio engine) once the Audiobus app closes. This keeps your app alive and responsive to
 connection changes while the Audiobus session is active.

 Take a look at the [Lifecycle](@ref Lifecycle) section of the integration guide, and the
 [Lifecycle recipe](@ref Lifecycle-Recipe) or the sample apps for some code examples.
 
 @section Migration-Guide-Triggers Triggers

 Triggers have not changed substantially, although the triggerWithTitle:icon:block: method of ABTrigger has
 been deprecated, in favour of the 
 @link ABButtonTrigger::buttonTriggerWithTitle:icon:block: buttonTriggerWithTitle:icon:block: @endlink
 factory method on the new ABButtonTrigger class. A similar method on the new
 ABAnimatedTrigger class lets you create animated triggers, to reflect rapidly-changing state.
 Take a look at the [triggers section](@ref Triggers) for details.
 
 @section Migration-Guide-Sample-Apps Sample Apps

 There are four brand new sample apps contained within the Audiobus SDK distribution.
 
@page Recipes Common Recipes

 This section contains code samples illustrating a variety of common Audiobus-related tasks.
 More sample code is available within the "Samples" folder of the SDK distribution.
 
 @section Sender-Port-Recipe Create a sender port and send audio manually

 This code snippet demonstrates how to create a sender port, and then send audio through it 
 manually, without using ABSenderPort's audio unit initialiser. Note that the audio unit method is
 recommended as it's much simpler, but there may be circumstances under which more control is needed, 
 such as when you are publishing multiple sender ports.
 
 The code below also demonstrates how to use the result of 
 @link ABSenderPort::ABSenderPortIsMuted ABSenderPortIsMuted @endlink to determine when to mute output.
 
 @code
 @interface MyAudioEngine ()
 @property (strong, nonatomic) ABAudiobusController *audiobusController;
 @property (strong, nonatomic) ABSenderPort *sender;
 @end
 
 @implementation MyAudioEngine
 
 -(id)init {
    ...
 
    self.audiobusController = [[ABAudiobusController alloc] initWithApiKey:@"YOUR-API-KEY"];
 
    ABSenderPort *sender = [[ABSenderPort alloc] initWithName:@"Audio Output"
                                                        title:NSLocalizedString(@"Main App Output", @"")
                                    audioComponentDescription:(AudioComponentDescription) {
                                        .componentType = kAudioUnitType_RemoteGenerator,
                                        .componentSubType = 'aout',
                                        .componentManufacturer = 'you!' }];
    sender.clientFormat = [MyAudioEngine myAudioDescription];
    [_audiobusController addSenderPort:_sender];

    ...
 }
 
 ...
 
 static OSStatus audioUnitRenderCallback(void *inRefCon, 
                                         AudioUnitRenderActionFlags *ioActionFlags,
                                         const AudioTimeStamp *inTimeStamp, 
                                         UInt32 inBusNumber, 
                                         UInt32 inNumberFrames, 
                                         AudioBufferList *ioData) {

    __unsafe_unretained MyAudioEngine *self = (__bridge MyAudioEngine*)inRefCon;

    // Do rendering, resulting in audio in ioData
    ...
 
    // Now send audio through Audiobus
    ABSenderPortSend(self->_sender, ioData, inNumberFrames, inTimeStamp);
 
    // Now mute, if appropriate
    if ( ABSenderPortIsMuted(self->_sender) ) {
        // If we should be muted, then mute
        for ( int i=0; i<ioData->mNumberBuffers; i++ ) {
            memset(ioData->mBuffers[i].mData, 0, ioData->mBuffers[i].mDataByteSize);
        }
        *ioActionFlags |= kAudioUnitRenderAction_OutputIsSilence;
    }
 }
 @endcode

 @section Filter-Port-Recipe Create a filter port with a process block

 This demonstrates how to create and implement a filter port with a process block. Using
 a process block is more complex than using ABFilterPort's audio unit initialiser, but
 may provide more flexibility under certain circumstances, such as when you are publishing
 multiple filter ports.
 
 The code creates a filter port, providing a processing implementation block which is
 invoked whenever audio arrives on the input side of the filter. After the block is called,
 during which your app processes the audio in place, Audiobus will automatically send the
 processed audio onwards.
 
 The code also demonstrates how to mute your audio system when the filter port is connected.
 
 @code
 @interface MyAudioEngine ()
 @property (strong, nonatomic) ABAudiobusController *audiobusController;
 @property (strong, nonatomic) ABFilterPort *filter;
 @end
 
 @implementation MyAudioEngine
 
  -(id)init {
    ...
 
    self.audiobusController = [[ABAudiobusController alloc] initWithApiKey:@"YOUR-API-KEY"];
 
    self.filter = [[ABFilterPort alloc] initWithName:@"Main Effect"
                                               title:@"Main Effect"
                           audioComponentDescription:(AudioComponentDescription) {
                               .componentType = kAudioUnitType_RemoteEffect,
                               .componentSubType = 'myfx',
                               .componentManufacturer = 'you!' }
                                        processBlock:^(AudioBufferList *audio, UInt32 frames, AudioTimeStamp *timestamp) {
                                            processAudio(audio);
                                        } processBlockSize:0];

    filter.clientFormat = [MyAudioEngine myAudioDescription];
    [_audiobusController addFilterPort:_filter];
 
    ...
 }
 
 ...
 
 static OSStatus audioUnitRenderCallback(void *inRefCon, 
                                         AudioUnitRenderActionFlags *ioActionFlags,
                                         const AudioTimeStamp *inTimeStamp, 
                                         UInt32 inBusNumber, 
                                         UInt32 inNumberFrames, 
                                         AudioBufferList *ioData) {

    __unsafe_unretained MyAudioEngine *self = (__bridge MyAudioEngine*)inRefCon;
 
    // Mute and exit, if filter is connected
    if ( ABFilterPortIsConnected(self->_filter) ) {
        for ( int i=0; i<ioData->mNumberBuffers; i++ ) {
            memset(ioData->mBuffers[i].mData, 0, ioData->mBuffers[i].mDataByteSize);
        }
        *ioActionFlags |= kAudioUnitRenderAction_OutputIsSilence;
        return noErr;
    }


    ...
 
 }
 @endcode
 
 @section Receiver-Port-Recipe Create a receiver port and receive audio

 This code illustrates the typical method of receiving audio from Audiobus.
 
 The code creates a single receiver port, assigns an AudioStreamBasicDescription describing the audio format to
 use, then uses the port to receive audio from within a Remote IO input callback.
 
 @code
 @interface MyAudioEngine ()
 @property (strong, nonatomic) ABAudiobusController *audiobusController;
 @property (strong, nonatomic) ABReceiverPort *receiver;
 @end
 
 @implementation MyAudioEngine
 
 -(id)init {
    ...
 
    self.audiobusController = [[ABAudiobusController alloc] initWithApiKey:@"YOUR-API-KEY"];
 
    self.receiver = [[ABReceiverPort alloc] initWithName:@"Main" title:NSLocalizedString(@"Main Input", @"")];
    _receiver.clientFormat = [MyAudioEngine myAudioDescription];
    [_audiobusController addReceiverPort:_receiver];

    ...
 }
 
 ...
 
 static OSStatus audioUnitRenderCallback(void *inRefCon, 
                                         AudioUnitRenderActionFlags *ioActionFlags,
                                         const AudioTimeStamp *inTimeStamp, 
                                         UInt32 inBusNumber, 
                                         UInt32 inNumberFrames, 
                                         AudioBufferList *ioData) {

    __unsafe_unretained MyAudioEngine *self = (__bridge MyAudioEngine*)inRefCon;

    AudioTimeStamp timestamp = *inTimeStamp;
 
    if ( ABReceiverPortIsConnected(self->_receiver) ) {
       // Receive audio from Audiobus, if connected.
       ABReceiverPortReceive(self->_receiver, nil, ioData, inNumberFrames, &timestamp);
    } else {
       // Receive audio from system input otherwise
       AudioUnitRender(self->_audioUnit, ioActionFlags, inTimeStamp, 1, inNumberFrames, ioData);
    }
    
    // Do something with audio in 'ioData', and 'timestamp'
 }
 @endcode


 @section Trigger-Recipe Create a trigger

 This demonstrates how to create a trigger, which can be invoked remotely to perform some action within your app.
 
 The sample creates a trigger, passing in a block that toggles the recording state of a fictional transport controller.
 
 It also observes the recording state of the controller, and updates the trigger's state when the recording state
 changes, so that the appearance of the user interface element corresponding to the trigger on remote apps changes
 appropriately.
 
 @code
 static void * kTransportControllerRecordingStateChanged = &kTransportControllerRecordingStateChanged;

 ...

 self.recordTrigger = [ABTrigger triggerWithSystemType:ABTriggerTypeRecordToggle block:^(ABTrigger *trigger, NSSet *ports) {
    if ( self.transportController.recording ) {
        [self.transportController endRecording];
    } else {
        [self.transportController beginRecording];
    }
 }];
 [self.audiobusController addTrigger:self.recordTrigger];
 
 // Watch recording status of our controller class so we can update the trigger state
 [self.transportController addObserver:self forKeyPath:@"recording" options:0 context:kTransportControllerRecordingStateChanged];

 ...

 -(void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    // Update trigger state to reflect recording status
    if ( context == kTransportControllerRecordingStateChanged ) {
        self.recordTrigger.state = self.transportController.recording ? ABTriggerStateSelected : ABTriggerStateNormal;
    } else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
 }
 @endcode

 @section Lifecycle-Recipe Manage application life-cycle
 
 This example demonstrates the recommended way to manage your application's life-cycle.
 
 The example assumes the app in question has been registered at 
 [developer.audiob.us/register](http://developer.audiob.us/register), and is therefore able
 to be connected and launched from the Audiobus app.
 
 As soon as your app is connected via Audiobus, it must have a running and active audio system.
 This means you must either only instantiate the Audiobus controller at the same time you start 
 your audio system, or you must watch for @link ABConnectionsChangedNotification @endlink and start your
 audio system when the notification is observed.
 
 Once your app is connected via Audiobus, it should not under any circumstances suspend its 
 audio system when moving into the background. We also strongly recommend remaining active in the
 background while the Audiobus app is running, to keep your app available for use without needing
 to be re-launched. When moving to the background, the app can check the 
 [connected](@ref ABAudiobusController::connected) and
 [audiobusAppRunning](@ref ABAudiobusController::audiobusAppRunning) properties of the Audiobus controller,
 and only stop the audio system if the return value for each is NO.
 
 If your app is in the background when the above properties become negative, indicating that your app
 is disconnected and that Audiobus has quit, we recommend shutting down the audio engine, as appropriate.
 
 @code
 static void * kAudiobusRunningOrConnectedChanged = &kAudiobusRunningOrConnectedChanged;
 
 -(BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // ...

    // Watch the audiobusAppRunning and connected properties
    [_audiobusController addObserver:self
                         forKeyPath:@"connected"
                            options:0
                            context:kAudiobusRunningOrConnectedChanged];
    [_audiobusController addObserver:self
                         forKeyPath:@"audiobusAppRunning"
                            options:0
                            context:kAudiobusRunningOrConnectedChanged];

    // ...
 }
 
 -(void)dealloc {
     [_audiobusController removeObserver:self forKeyPath:@"connected"];
     [_audiobusController removeObserver:self forKeyPath:@"audiobusAppRunning"];
 }
 
 -(void)observeValueForKeyPath:(NSString *)keyPath
                     ofObject:(id)object
                       change:(NSDictionary *)change
                      context:(void *)context {

    if ( context == kAudiobusRunningOrConnectedChanged ) {
        if ( [UIApplication sharedApplication].applicationState == UIApplicationStateBackground
               && !_audiobusController.connected
               && !_audiobusController.audiobusAppRunning
               && _audioEngine.running ) {

            // Audiobus has quit. Time to sleep.
            [_audioEngine stop];
        }
    } else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
 }
 
 -(void)applicationDidEnterBackground:(NSNotification *)notification {
     if ( !_audiobusController.connected && !_audiobusController.audiobusAppRunning && _audioEngine.running ) {
        // Stop the audio engine, suspending the app, if Audiobus isn't running
        [_audioEngine stop];
     }
 }
 
 -(void)applicationWillEnterForeground:(NSNotification *)notification {
     if ( !_audioEngine.running ) {
         // Start the audio system if it wasn't running
         [_audioEngine start];
     }
 }
 @endcode

 @section Determine-Connected Determine if app is connected via Audiobus
 
 The following code demonstrates one way to monitor and determine whether any Audiobus ports are
 currently connected.

 You can also:

 - Observe (via KVO) the 'connected' property of ABAudiobusController or any of the port classes,
   or any of the 'sources'/'destinations' properties of the port classes
 - Watch for `ABReceiverPortConnectionsChangedNotification`, `ABReceiverPortPortAddedNotification`,
   `ABReceiverPortPortRemovedNotification`, `ABSenderPortConnectionsChangedNotification`, or
   `ABFilterPortConnectionsChangedNotification`.
 - Use `ABReceiverPortIsConnected`, `ABSenderPortIsConnected`, and `ABFilterPortIsConnected` from
   a Core Audio thread.
 
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

 -(void)connectionsChanged:(NSNotification*)notification {
    if ( _audiobusController.connected ) {
        // We are connected
 
    } else {
        // Not connected
    }
 }
 @endcode
 
 @section Enumerate-Connections Enumerate apps connected to a receiver port
 
 This illustrates how to inspect each individual source of a receiver port. This information
 can be used to update the user interface, or configure models to represent each audio stream.
 
 @code
 for ( ABPort *connectedPort in _receiverPort.sources ) {
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
                                [NSPredicate predicateWithFormat:@"type = %d", ABPortTypeSender]];
 @endcode
 
 Note: similarly, you can obtain a list of all filters by replacing the `ABPortTypeSender` identifier with
 `ABPortTypeFilter`, and a list of all receivers with the `ABPortTypeReceiver`.
 
 @section Receiver-Port-Separate-Streams Receive audio as separate streams
 
 This example demonstrates how to use ABReceiverPort's separate-stream receive mode
 ([receiveMixedAudio](@ref ABReceiverPort::receiveMixedAudio) = NO) to receive each audio stream from 
 each connected app separately, rather than as a single mixed-down audio stream.
 
 The code below maintains a C array of currently-connected sources, in order to be able to enumerate them
 within a Core Audio thread without calling any Objective-C methods (note that Objective-C methods should
 never be called on a Core Audio thread due to the risk of priority inversion, resulting in stuttering audio).

 The sample code monitors connection changes, then updates the C array accordingly.
 
 Then within the audio unit render callback, the code iterates through this array to receive each audio stream.
 
 @code
 static const int kMaxSources = 30; // Some reasonably high number
 static void * kReceiverSourcesChanged = &kReceiverSourcesChanged;
 
 // A structure used to make up our source table
 struct port_entry_t { void *port; BOOL pendingRemoval; };
 
 // Our class continuation, where we define a source port table
 @interface MyAudioEngine () {
    struct port_entry_t _portTable[kMaxSources];
 }
 @end
 
 @implementation MyAudioEngine

 -(id)init {
    ...
 
    self.audiobusController = [[ABAudiobusController alloc] initWithApiKey:@"YOUR-API-KEY"];
 
    self.receiver = [[ABReceiverPort alloc] initWithName:@"Main" title:NSLocalizedString(@"Main Input", @"")];
    _receiver.clientFormat = [MyAudioEngine myAudioDescription];
    _receiver.receiveMixedAudio = NO;
    [_audiobusController addReceiverPort:_receiver];

    // Watch the receiver's 'sources' property to be notified when the sources change
    [_receiver addObserver:self forKeyPath:@"sources" options:0 context:kReceiverSourcesChanged];
 }
 
 -(void)dealloc {
     [_receiver removeObserver:self forKeyPath:@"sources"];
 }

 // Table lookup facility, to make lookups easier
 -(struct port_entry_t*)entryForPort:(ABPort*)port {
     for ( int i=0; i<kMaxSources; i++ ) {
         if ( _portTable[i].port == (__bridge void*)port ) {
             return &_portTable[i];
         }
     }
     return NULL;
 }

 -(void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary *)change
                       context:(void *)context {
 
     if ( context == kReceiverSourcesChanged ) {
         
         // When the connections change, add any new sources to our C array
         for ( ABPort *source in _receiver.sources ) {
             if ( ![self entryForPort:source] ) {
                 struct port_entry_t *emptySlot = [self entryForPort:nil];
                 if ( emptySlot ) {
                     emptySlot->port = (__bridge void*)source;
                 }
             }
         }
     
         // Prepare to remove old sources (this will be done on the Core Audio thread, so removals are thread-safe)
         for ( int i=0; i<kMaxSources; i++ ) {
             if ( _portTable[i].port && ![_receiver.sources containsObject:(__bridge ABPort*)_portTable[i].port] ) {
                 _portTable[i].pendingRemoval = YES;
             }
         }
 
     } else {
         [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
     }
 }
 
 ...
 
 static OSStatus audioUnitRenderCallback(void *inRefCon, 
                                         AudioUnitRenderActionFlags *ioActionFlags,
                                         const AudioTimeStamp *inTimeStamp, 
                                         UInt32 inBusNumber, 
                                         UInt32 inNumberFrames, 
                                         AudioBufferList *ioData) {

     __unsafe_unretained MyAudioEngine *self = (__bridge MyAudioEngine*)inRefCon;

     // Remove sources pending removal (which we did in the change handler above)
     for ( int i=0; i<kMaxSources; i++ ) {
         if ( self->_portTable[i].port && self->_portTable[i].pendingRemoval ) {
             self->_portTable[i].pendingRemoval = NO;
             self->_portTable[i].port = NULL;
         }
     }
 
    if ( ABReceiverPortIsConnected(self->_receiver) ) {

        // Now we can iterate through the source port table without using Objective-C:
        for ( int i=0; i<kMaxSources; i++ ) {
            if ( self->_portTable[i].port ) {
                AudioTimeStamp timestamp;
                ABReceiverPortReceive(self->_receiver, (__bridge ABPort*)self->_portTable[i].port, ioData, inNumberFrames, &timestamp);
                
                // Do something with this audio
            }
        }

        // Mark the end of this time interval
        ABReceiverPortEndReceiveTimeInterval(self->_receiver);

    } else {
       // Receive audio from system input otherwise
       AudioUnitRender(self->_audioUnit, ioActionFlags, inTimeStamp, 1, inNumberFrames, ioData);
       
       // Do something with this audio
    }
 }
 
 @endcode
 
 @section Audio-Queue-Input Use Audiobus input in an Audio Queue
 
 This example demonstrates the Audio Queue versions of the receiver port receive functions, which
 take an AudioQueueBufferRef argument instead of an AudioBufferList.
 
 Illustrated is an input callback which replaces the incoming microphone audio with audio from
 Audiobus, which represents a quick and easy way to implement receiver ports in an app that uses
 Audio Queues and microphone input.
 
 @code
 static void MyAQInputCallback(void *inUserData,
                               AudioQueueRef inQueue,
                               AudioQueueBufferRef inBuffer,
                               const AudioTimeStamp *inStartTime,
                               UInt32 inNumPackets,
                               const AudioStreamPacketDescription *inPacketDesc) {
 
    __unsafe_unretained MyController *self = (MyController*)inUserData;
 
    // Intercept audio, replacing it with Audiobus input
    AudioTimeStamp timestamp = *inStartTime;
    ABReceiverPortReceiveAQ(self->_audiobusReceiverPort,
                         nil,
                         inBuffer,
                         &inNumPackets,
                         &timestamp,
                         NULL);
 
    // Now do something with audio in inBuffer...
 
 }
 @endcode

 
@page Receiver-Port Receiving: The Audiobus Receiver Port

 The Audiobus receiver port class ABReceiverPort provides an interface for receiving audio,
 either as separate audio streams (one per connected sender), or as a single audio stream with all
 sources mixed together.

 Receiving audio tends to be a little more involved than sending or filtering audio, so this section aims
 to discuss some of the finer points of using ABReceiverPort.

 See the [Receiver Port](@ref Create-Receiver-Port) section of the integration guide for an initial overview.

@section Latency Dealing with Latency
 
 Audiobus receivers are given timestamps along with every piece of audio they receive. These
 timestamps are vital for compensating for latency when recording in a time-sensitive context.

 This works in exactly the same way that timestamps in Core Audio do.

 If your app records the audio it receives over Audiobus and the timing is important (for example,
 you record audio in time with some other track, such as a looper or a multi-track recorder), then
 use these timestamps when saving the received audio to negate the effects of latency.

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

@section Receiving-Separate-Streams Receiving Separate Streams

 You can receive audio as separate stereo streams - one per source - or as a single mixed stereo audio stream.
 By default, Audiobus will return the audio as a single, mixed stream.
 
 If you wish to receive separate streams for each source, however, you can set
 [receiveMixedAudio](@ref ABReceiverPort::receiveMixedAudio) to `NO`. Then, each source will have
 its own audio stream, accessed by passing in a pointer to the source port in
 @link ABReceiverPort::ABReceiverPortReceive ABReceiverPortReceive @endlink.
 
 After calling ABReceiverPortReceive for each source, you must then call
 @link ABReceiverPort::ABReceiverPortEndReceiveTimeInterval ABReceiverPortEndReceiveTimeInterval @endlink
 to mark the end of the current interval. 

 Please see the ['Receive Audio as Separate Streams'](@ref Receiver-Port-Separate-Streams) sample recipe,
 the documentation for [ABReceiverPortReceive](@ref ABReceiverPort::ABReceiverPortReceive)
 and [ABReceiverPortEndReceiveTimeInterval](@ref ABReceiverPort::ABReceiverPortEndReceiveTimeInterval),
 and the AB Multitrack sample app for more info.
 
 > Note you should not access the `sources` property, or any other Objective-C methods, from
 > a Core Audio thread, as this may cause the thread to block, resulting in audio glitches. You
 > should obtain a pointer to the ABPort objects in advance, and use these pointers directly, as
 > demonstrated in the ['Receive Audio as Separate Streams'](@ref Receiver-Port-Separate-Streams) sample recipe
 > and within the "AB Multitrack Receiver" sample.
 
@subsection Receiving-Separate-Streams-With-Core-Audio-Input Receiving Separate Streams Alongside Core Audio Input
 
 If you wish to simultaneously incorporate audio from other sources as well as Audiobus - namely, the device's audio
 input - then depending on your app, it may be very important that all sources are synchronised and delivered in a
 consistent fashion. This will be true if you provide live audio monitoring, or if you apply effects in a
 synchronised way across all audio streams.
 
 The Audiobus SDK provides the ABMultiStreamBuffer class for buffering and synchronising
 multiple audio streams, so that you can do this. You enqueue separate, un-synchronised audio streams on one side,
 and then dequeue synchronised streams from the other side, ready for further processing.

 Typical usage is as follows:
 
 1. You receive audio from the system audio input, typically via a Remote IO input callback and AudioUnitRender,
    then enqueue it on the ABMultiStreamBuffer.
 2. You receive audio from each connected Audiobus source, also enqueuing the audio on the ABMultiStreamBuffer 
    ([ABMultiStreamBufferEnqueue](@ref ABMultiStreamBuffer::ABMultiStreamBufferEnqueue)).
 3. You then dequeue each source from ABMultiStreamBuffer ([ABMultiStreamBufferDequeueSingleSource](@ref ABMultiStreamBuffer::ABMultiStreamBufferDequeueSingleSource)).
    Audio will be buffered and synchronised via the timestamps of the enqueued audio.

@page Triggers Triggers

 Audiobus provides a system where apps can define actions that can be triggered by users from other
 apps, via the Audiobus Connection Panel.

 You can use a set of built-in system triggers (see
 @link ABTrigger::triggerWithSystemType:block: triggerWithSystemType:block: @endlink and
 @link ABTriggerSystemType @endlink), or [create your own](@ref ABButtonTrigger). You can also
 create [animated triggers](@ref ABAnimatedTrigger), which lets you represent rapidly-changing dynamic app state.

 @section Use-of-Triggers Use of Triggers
 
 Triggers are designed to provide limited remote-control functionality over Audiobus apps. If your
 app has functions that may be usefully activated from a connected app, then you should expose them
 using the Audiobus triggers mechanism.
 
 Note, however, that apps should only provide a small number of these triggers - no more than four -
 to avoid cluttering up the Audiobus Connection Panel interface.
 
 Additionally, your app should only provide triggers that are *relevant to the current state*. Take, for
 example, an app that has the capability of behaving as an Audiobus input and an output. If the app
 presents a "Record" trigger, but is currently acting as an input to another Audiobus app, this
 may lead to confusion: the app is serving in an audio generation role, not an audio consumption role,
 and consequently a "Record" function is not relevant to the current state.
 
 You can add and remove triggers at any time, so you should make use of this functionality to only
 offer users relevant actions.
 
 @section Creating-a-Trigger Creating a Trigger
 
 **Whenever possible, you should use a built-in trigger type, accessible via
 @link ABTrigger::triggerWithSystemType:block: triggerWithSystemType:block: @endlink.**
 
 If you *must* create a custom trigger, then you can create a button trigger with 
 @link ABButtonTrigger::buttonTriggerWithTitle:icon:block: ABButtonTrigger's buttonTriggerWithTitle:icon:block: @endlink.
 Alternatively, you can create an animated trigger with
 @link ABAnimatedTrigger::animatedTriggerWithTitle:initialIcon:block: ABAnimatedTrigger's animatedTriggerWithTitle:initialIcon:block: @endlink,
 register animation frames with @link ABAnimatedTrigger::registerNewFrame:withIdentifier: registerNewFrame:withIdentifier: @endlink,
 then display animation frames with @link ABAnimatedTrigger::currentFrameIdentifier currentFrameIdentifier @endlink.
 
 Note that icons should be an image of no greater than 80x80 pixels, and will be
 used as a mask to draw a styled button.  If you do not provide 'selected' or 'alternate' state icons or colours 
 for a toggle button, then the same icon will be drawn with a default style to indicate the state change.

 When you create a trigger, you provide a [block](@ref ABTriggerPerformBlock) to perform when the trigger is
 activated remotely. The block accepts two arguments: the trigger, and a set of your app's ports to which the app
 from which the trigger was activated is connected. This port set will typically be just one port,
 but may be multiple ports.

 You may wish to use the ports set to determine what elements within your app to apply the
 result of the trigger to. For example, if your trigger is @link ABTriggerTypeRecordToggle @endlink,
 and the connected port refers to one track of a multi-track recording app, then you may wish
 to begin recording this track.

 If you are implementing a two-state trigger, such as @link ABTriggerTypeRecordToggle @endlink,
 @link ABTriggerTypePlayToggle @endlink or a custom trigger with multiple states, you should update the
 [trigger state](@ref ABTrigger::state) as appropriate, when the state to which it refers changes.

 Note that you can also update the icon of custom triggers at any time. The user interface across
 all connected devices and apps will be updated accordingly. If you intend to perform rapid icon updates,
 use ABAnimatedTrigger.
 
 Have a look at the [Trigger recipe](@ref Trigger-Recipe) and the "AB Receiver" and "AB Filter" sample apps 
 for examples.
 
 System triggers are automatically ordered in the connection panel as follows: 
 ABTriggerTypeRewind, ABTriggerTypePlayToggle, ABTriggerTypeRecordToggle.

@page State-Saving State Saving

 State saving allows your app to provide workspace configuration information that can be stored,  
 recalled and shared by users when saving or loading an Audiobus preset. This allows users to save and share
 their entire workspaces, across all the apps they are using.

 If you're not familiar with Audiobus presets and state saving, here're two videos explaining each:
 
 @htmlonly
 <iframe width="560" height="315" src="//www.youtube.com/embed/aDNesaca0do" frameborder="0" allowfullscreen
    style="display: block; margin: 0 auto;"></iframe>
 
 <iframe width="560" height="315" src="//www.youtube.com/embed/tE347uTXKms" frameborder="0" allowfullscreen
    style="display: block; margin: 0 auto;"></iframe>
 @endhtmlonly
 
 To support state saving, you need to implement the @link ABAudiobusControllerStateIODelegate @endlink
 protocol, and identify your State IO delegate to the Audiobus controller via its
 @link ABAudiobusController::stateIODelegate stateIODelegate @endlink property.

 The State IO delegate protocol consists of two methods: one which is invoked when a preset is being saved,
 @link ABAudiobusControllerStateIODelegate::audiobusStateDictionaryForCurrentState audiobusStateDictionaryForCurrentState @endlink,
 and one which is invoked when a preset is being loaded, 
 @link ABAudiobusControllerStateIODelegate::loadStateFromAudiobusStateDictionary:responseMessage: loadStateFromAudiobusStateDictionary:responseMessage: @endlink.

 You use the former to provide Audiobus with a dictionary of keys and values that represent your app's current
 state. The latter provides you with the same keys and values you provided when the preset was saved, which you
 use to restore that state. If there was a problem restoring the state (for example, the state relies on functionality
 accessible only via an In-App Purchase, or content that hasn't been downloaded yet), you may return a message
 that will be displayed to the user within Audiobus.

 What data you provide via this system is up to you: you can provide NSData blobs, NSStrings, and any other
 Property List types (see Apple's 
 [About Property Lists](https://developer.apple.com/library/mac/documentation/Cocoa/Conceptual/PropertyLists/AboutPropertyLists/AboutPropertyLists.html)
 documentation).
 
 For example: a synth app should save current patch settings. An effects app should save the parameters. A multi-track
 recorder app may choose to save the current project, including the audio tracks. A sampler should save the loaded
 audio samples.

 We currently require that you **do not return data larger than 20MB**. Any audio data in presets should preferably be
 in a compressed format. We previously asked developers working with the 2.0 SDK version not to save MIDI settings 
 information: we have since relaxed this requirement, and **we now allow MIDI settings to be saved**, such as a set 
 of active MIDI connections.

 State saving is a very new feature that will undergo further evolution as we see what users and developers
 are doing with it. Consequently, these guidelines may change over time. If you have feedback, let us know on 
 the [developer forums!](http://heroes.audiob.us).

@page Good-Citizen Being a Good Citizen

 Beyond being an audio transmission protocol or platform, Audiobus is a community of applications. The
 experience that users have is strongly dependent on how well these apps work together. So, these are
 a set of rules/guidelines that your app should follow, in order to be a good Audiobus citizen.

@section Senders-Timestamps Senders, Send Correct Audio Timestamps

 Audiobus sender apps are responsible for sending correct audio timestamps along with their
 audio. If you are using an audio unit with the sender port, then this is taken care of for you. Otherwise, 
 if you are using ABSenderPort directly, make sure you are sending audio timestamps correctly. 
 Usually, this is just a matter of providing the `AudioTimeStamp` structure given to you by Core Audio.

@section Receivers-Timestamps Receivers, Use Audio Timestamps

 When dealing with multiple effect pipelines, latency is an unavoidable factor that is very important to 
 address when timing is important.
 
 Audiobus deals with latency by providing you, the developer, with timestamps that correspond
 to the creation time of each block of audio.
 
 If you are recording audio, and are mixing it with other live signals or if timing is 
 otherwise important, then it is **vital** that you make full use of these timestamps in order 
 to compensate for system latency. How you use these timestamps depends on your app - you may
 already be using timestamps from Core Audio, which means there's nothing special that you need
 to do.

 See [Dealing with Latency](@ref Latency) for more info.

@section Low-Buffer-Durations Use Low IO Buffer Durations, If You Can

 Core Audio allows apps to set a preferred IO buffer duration via the audio session (see
 AVAudioSession's `preferredIOBufferDuration` property in the Core Audio documentation). This
 setting configures the length of the buffers the audio system manages. Shorter buffers mean
 lower latency. By the time you receive a 5ms buffer from the system input, for example,
 roughly 5ms have elapsed since the audio reached the microphone.  Similarly, by the time a
 5ms buffer has been played by the system's speaker, 5ms or so have elapsed since the
 audio was generated.

 The tradeoff of small IO buffer durations is that your app has to work harder, per time unit,
 as it's processing smaller blocks of audio, more frequently. So, it's up to you to figure out
 how low your app's latency can go - but remember to save some CPU cycles for other apps as well!

@section Background-Mode In the Background Suspend When Possible, But Not While Audiobus Is Running
 
 It's up to you whether it's appropriate to suspend your app in the background, but there are a few
 things to keep in mind.
 
 Most important: you should never, ever suspend your app if it's connected via Audiobus. You can tell
 whether your app's connected at any time via the [connected](@ref ABAudiobusController::connected)
 property of the Audiobus controller.  If the value is YES, then you mustn't suspend.
 
 Secondly, we strongly recommend that your app remain active in the background while the Audiobus app
 is running. This keeps your app available for being re-added to a connection graph (or reloaded from a
 preset) without needing to be manually launched again. Once the Audiobus app closes, then your app can
 suspend in the background. 

 See the [Lifecycle](@ref Lifecycle) section of the integration guide, or the [associated recipe](@ref Lifecycle-Recipe)
 for further details.
 
 Note that during development, if your app has not yet been [registered](http://developer.audiob.us/new-app)
 with Audiobus, Audiobus will not be able to see the app if it is not actively running in the background.
 Consequently, we **strongly recommend** that you register your app at the beginning of development.
 
@section Efficient Be Efficient!

 Audiobus leans heavily on iOS multitasking! You could be running three synth apps, two filter apps,
 and be recording into a live-looper or a DAW. That requires a lot of juice.

 So, be kind to your fellow developers. Profile your app and find places where you can back off
 the CPU a bit. Never, ever wait on locks, allocate memory, or call Objective-C functions from Core
 Audio. Use plain old C in time-critical places (or even drop to assembly). Take a look at the
 Accelerate framework if you're not familiar with it, and use its vector operations instead of
 scalar operations within loops - it makes a huge difference.

*/
