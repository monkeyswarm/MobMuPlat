//
//  ViewController.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/15/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//
//  The main class of MobMuPlat.
//  Functionality includes:
//  -initializing libPD audio processing
//  -loading a MMP file (from its JSON String) into the right canvas size and orientation, and laying out the GUI widget objects it specifies
//  -loading the PD patch the above file specifies
//  -starting the device motion data and sending it into PD
//  -methods (called from SettingsViewController) to change audio/DSP/MIDI parameters
//  -sending messages from PD to the appropriate GUI object
//  -receving messages from GUI objects to send into PD
//  -intializing and sending/receiving network data and sending it in/out of PD
//  -receiving MIDI bytes (via PGMidi objects), formatting into messages to send to PD
//


#define DEFAULT_OUTPUT_PORT_NUMBER 54321
#define DEFAULT_INPUT_PORT_NUMBER 54322

#define SETTINGS_BUTTON_OFFSET_PERCENT .02 // percent of screen width
#define SETTINGS_BUTTON_DIM_PERCENT .1 // percent of screen width

#import "MMPViewController.h"

#import "VVOSC.h"

#import <SystemConfiguration/CaptiveNetwork.h>//for ssid info

#import "MeSlider.h"
#import "MeKnob.h"
#import "MeLabel.h"
#import "MeButton.h"
#import "MeToggle.h"
#import "MeXYSlider.h"
#import "MeGrid.h"
#import "MePanel.h"
#import "MeMultiSlider.h"
#import "MeLCD.h"
#import "MeMultiTouch.h"
#import "MeUnknown.h"
#import "MeMenu.h"
#import "MeTable.h"

#import "AudioHelpers.h"
#import "MobMuPlatPdAudioUnit.h"
#import "MobMuPlatUtil.h"
#import "MMPNetworkingUtils.h"
#import "MMPPdPatchDisplayUtils.h"
#import "MMPMenuButton.h"

#import "MMPGui.h"
#import "PdParser.h"

#import "UIAlertView+MMPBlocks.h"

@implementation MMPViewController {
  NSMutableArray *_keyCommands; // BT key commands to listen for.
  MMPGui *_pdGui; // Keep strong reference here, for widgets to refer to weakly.
  CGFloat _settingsButtonDim; // Width/height of settings menu button.
  CGFloat _settingsButtonOffset; // Offset of menu button from edge of screen.
  MMPMenuButton *_settingsButton;

  // Midi connections.
  NSMutableArray<PGMidiSource *> *_connectedMidiSources; //TODO Use a set.
  NSMutableArray<PGMidiDestination *> *_connectedMidiDestinations;

  //audio settings
  int _samplingRate;
  BOOL _mixingEnabled;
  int _channelCount;
  int _ticksPerBuffer;
  BOOL _inputEnabled;

  // key = address, value = array of objects with that address.
  NSMutableDictionary<NSString *, NSMutableArray<MeControl *> *> *_addressToGUIObjectsDict;

  // layout
  UIScrollView *_scrollView; // MMP gui
  UIView *_scrollInnerView;
  UIView *_pdPatchView; //Native gui

  OSCManager *_oscManager;
  OSCInPort *_oscInPort;
  OSCOutPort *_oscOutPort;

  //LANdini and Ping&Connect
  OSCInPort *_inPortFromNetworkingModules;
  OSCOutPort *_outPortToNetworkingModules;

  //midi
  PGMidi *midi;

  BOOL _uiIsFlipped; // Whether the UI has been inverted by the user.
  BOOL _isLandscape;
  int _pageCount;

  PdFile *_openPDFile;
  AVCaptureDevice *_avCaptureDevice;//for flash

  UINavigationController *_navigationController;

  CMMotionManager *_motionManager;
  CLLocationManager *_locationManager;

  Reachability *_reach;
}

@synthesize audioController = _audioController, settingsVC;
@synthesize inputPortNumber = _inputPortNumber, outputPortNumber = _outputPortNumber;
@synthesize outputIpAddress = _outputIpAddress;

//what kind of device am I one? iphone 3.5", iphone 4", or ipad
+ (MMPDeviceCanvasType)getCanvasType {
  MMPDeviceCanvasType hardwareCanvasType;
  if ([[UIDevice currentDevice]userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
    if ([[UIScreen mainScreen] bounds].size.height >= 568) {
      hardwareCanvasType = canvasTypeTallPhone;
    } else {
      hardwareCanvasType = canvasTypeWidePhone; //iphone <=4
    }
  } else {
    hardwareCanvasType=canvasTypeWideTablet;//ipad
  }
  return hardwareCanvasType;
}

- (NSArray *)keyCommands {
  return _keyCommands;
}

// Send "/key XX" messages from BT into Pd.
- (void)handleKey:(UIKeyCommand *)keyCommand {
  int val = 0;
  if (keyCommand.input == UIKeyInputUpArrow) val = 30;
  else if (keyCommand.input == UIKeyInputDownArrow) val = 31;
  else if (keyCommand.input == UIKeyInputLeftArrow) val = 28;
  else if (keyCommand.input == UIKeyInputRightArrow) val = 29;
  else if (keyCommand.input == UIKeyInputEscape) val = 27;
  else {
    if (keyCommand.input.length != 1) return;
    val = [keyCommand.input characterAtIndex: 0];
    if (val >= 128) return;
  }

  NSArray *msgArray=[NSArray arrayWithObjects:@"/key", [NSNumber numberWithInt:val], nil];
  [PdBase sendList:msgArray toReceiver:@"fromSystem"];
}

-(instancetype)init {
  return [self initWithAudioBusEnabled:YES];
}

-(instancetype)initWithAudioBusEnabled:(BOOL)audioBusEnabled {
  self = [super initWithNibName:nil bundle:nil];

  // Setup key commands
  if ([UIKeyCommand class]) { // iOS 7 and up.
    _keyCommands = [NSMutableArray arrayWithObjects:
        [UIKeyCommand keyCommandWithInput: UIKeyInputUpArrow modifierFlags:0 action: @selector(handleKey:)],
        [UIKeyCommand keyCommandWithInput: UIKeyInputDownArrow modifierFlags:0 action: @selector(handleKey:)],
        [UIKeyCommand keyCommandWithInput: UIKeyInputLeftArrow modifierFlags:0 action: @selector(handleKey:)],
        [UIKeyCommand keyCommandWithInput: UIKeyInputRightArrow modifierFlags:0 action: @selector(handleKey:)],
        [UIKeyCommand keyCommandWithInput: UIKeyInputEscape modifierFlags:0 action: @selector(handleKey:)],
        nil];

    // add all ascii range
    for (int charVal = 0; charVal < 128; charVal++) {
      NSString *string = [NSString stringWithFormat:@"%c" , charVal];
      [_keyCommands addObject:
          [UIKeyCommand keyCommandWithInput: string modifierFlags: 0 action: @selector(handleKey:)]];
    }
  }

  // audio setup.
  _mixingEnabled = YES;
  _channelCount = 2;
  // update after intial audio config, but setting 44100 here is not supported on iphone 6s speaker.
  _samplingRate = [AVAudioSession sharedInstance].sampleRate ; 
  _inputEnabled = YES;
#if TARGET_IPHONE_SIMULATOR
  _ticksPerBuffer = 8;  // No other value seems to work with the simulator.
#else
  _ticksPerBuffer = 8; // Audiobus likes this value.
#endif

  _openPDFile = nil;
  _addressToGUIObjectsDict = [[NSMutableDictionary alloc]init];

  _outputIpAddress = @"224.0.0.1";
  _outputPortNumber = DEFAULT_OUTPUT_PORT_NUMBER;
  _inputPortNumber = DEFAULT_INPUT_PORT_NUMBER;

  // for using the flash
  _avCaptureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];

  // MIDI
  midi = [[PGMidi alloc] init];
  [midi setNetworkEnabled:YES];
  [midi setVirtualDestinationEnabled:YES];
  [midi.virtualDestinationSource addDelegate:self];
  [midi setVirtualEndpointName:@"MobMuPlat"];
  [midi setVirtualSourceEnabled:YES];

  _connectedMidiSources = [NSMutableArray array];
  _connectedMidiDestinations = [NSMutableArray array];

  // If iOS 5, then use non-auto-layout xib files. - these xibs are no longer supported.
  /*if (SYSTEM_VERSION_LESS_THAN(@"6.0")) {
    settingsVC = [[SettingsViewController alloc] initWithNibName:@"SettingsViewControllerIOS5" bundle:nil];
  } else {*/
    settingsVC = [[SettingsViewController alloc] initWithNibName:nil bundle:nil];
  //}

  // OSC setup
  _oscManager = [[OSCManager alloc] init];
  [_oscManager setDelegate:self];

  // libPD setup.
  // Special audio unit that handles Audiobus.
  _audioController =
      [[PdAudioController alloc] initWithAudioUnit:[[MobMuPlatPdAudioUnit alloc] init]] ;
  [self updateAudioState];

  if (SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0") && audioBusEnabled) {
    // do stuff for iOS 7 and newer
    [self setupAudioBus];
  }

  // device motion/accel/gyro.
  _motionManager = [[CMMotionManager alloc] init];

  NSOperationQueue *motionQueue = [[NSOperationQueue alloc] init];
  if (_motionManager.isAccelerometerAvailable) {
    [_motionManager startAccelerometerUpdatesToQueue:motionQueue withHandler:
        ^(CMAccelerometerData *accelerometerData, NSError *error) {
          [self accelerometerDidAccelerate:accelerometerData.acceleration];
        }];
  } else {
    NSLog(@"No accelerometer on device.");
  }

  if (_motionManager.isDeviceMotionAvailable) {
    [_motionManager startDeviceMotionUpdatesToQueue:motionQueue withHandler:
        ^(CMDeviceMotion *devMotion, NSError *error) {
          CMAttitude *currentAttitude = devMotion.attitude;
          NSArray *motionArray = @[ @"/motion",
                                    @(currentAttitude.roll),
                                    @(currentAttitude.pitch),
                                    @(currentAttitude.yaw) ];

          [PdBase sendList:motionArray toReceiver:@"fromSystem"];
        }];
  } else {
    NSLog(@"No device motion on device.");
  }

  if (_motionManager.isGyroAvailable) {
    [_motionManager startGyroUpdatesToQueue:motionQueue withHandler:
        ^(CMGyroData *gyroData, NSError *error) {
          NSArray *gyroArray = @[ @"/gyro",
                                  @(gyroData.rotationRate.x),
                                  @(gyroData.rotationRate.y),
                                  @(gyroData.rotationRate.z) ];
          [PdBase sendList:gyroArray toReceiver:@"fromSystem"];
        }];
  } else {
    NSLog(@"No gyro info on device.");
  }

  // GPS location - not enabled yet.
  _locationManager = [[CLLocationManager alloc] init] ;
  _locationManager.delegate = self;
  [_locationManager setDistanceFilter:1.0];

  // landini - not enabled yet
  _llm = [[LANdiniLANManager alloc] init];
  _llm.userDelegate = settingsVC;

  //dev only
  //_llm.logDelegate = self;

  // ping and connect
  _pacm = [[PingAndConnectManager alloc] init];
  _pacm.userStateDelegate = settingsVC;

  // reachibility
  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(reachabilityChanged:)
                                               name:kReachabilityChangedNotification
                                             object:nil];
  _reach = [Reachability reachabilityForLocalWiFi];
  _reach.reachableOnWWAN = NO;
  [_reach startNotifier];

  //PD setup
  // set self as PdRecieverDelegate to recieve messages from Libpd
  [PdBase setMidiDelegate:self];

  _pdGui = [[MMPGui alloc] init];
  _mmpPdDispatcher = [[MMPPdDispatcher alloc] init];
  [Widget setDispatcher:_mmpPdDispatcher];
  [PdBase setDelegate:_mmpPdDispatcher];

  _mmpPdDispatcher.printDelegate = self;

  // Test for first run on new app version. Copy patches if so.
  MMPDeviceCanvasType hardwareCanvasType = [MMPViewController getCanvasType];
  NSString *bundleVersion =
      [[NSBundle mainBundle] objectForInfoDictionaryKey:(NSString *)kCFBundleVersionKey];

  NSString *appFirstStartOfVersionKey =
      [NSString stringWithFormat:@"first_start_%@", bundleVersion];

  NSNumber *alreadyStartedOnVersion =
      [[NSUserDefaults standardUserDefaults] objectForKey:appFirstStartOfVersionKey];

  if (!alreadyStartedOnVersion || [alreadyStartedOnVersion boolValue] == NO) {
    // New version detected.
    NSMutableArray *defaultPatches = [NSMutableArray array];
    if (hardwareCanvasType == canvasTypeWidePhone) {
      [defaultPatches addObjectsFromArray:@[ @"MMPTutorial0-HelloSine.mmp", @"MMPTutorial1-GUI.mmp", @"MMPTutorial2-Input.mmp", @"MMPTutorial3-Hardware.mmp", @"MMPTutorial4-Networking.mmp",@"MMPTutorial5-Files.mmp",@"MMPExamples-Vocoder.mmp", @"MMPExamples-Motion.mmp", @"MMPExamples-Sequencer.mmp", @"MMPExamples-GPS.mmp", @"MMPTutorial6-2DGraphics.mmp", @"MMPExamples-LANdini.mmp", @"MMPExamples-Arp.mmp", @"MMPExamples-TableGlitch.mmp" ]];
    } else if (hardwareCanvasType==canvasTypeTallPhone) {
      [defaultPatches addObjectsFromArray:@[ @"MMPTutorial0-HelloSine-ip5.mmp", @"MMPTutorial1-GUI-ip5.mmp", @"MMPTutorial2-Input-ip5.mmp", @"MMPTutorial3-Hardware-ip5.mmp", @"MMPTutorial4-Networking-ip5.mmp",@"MMPTutorial5-Files-ip5.mmp", @"MMPExamples-Vocoder-ip5.mmp", @"MMPExamples-Motion-ip5.mmp", @"MMPExamples-Sequencer-ip5.mmp",@"MMPExamples-GPS-ip5.mmp", @"MMPTutorial6-2DGraphics-ip5.mmp", @"MMPExamples-LANdini-ip5.mmp", @"MMPExamples-Arp-ip5.mmp",  @"MMPExamples-TableGlitch-ip5.mmp" ]];
    } else { //pad
      [defaultPatches addObjectsFromArray:@[ @"MMPTutorial0-HelloSine-Pad.mmp", @"MMPTutorial1-GUI-Pad.mmp", @"MMPTutorial2-Input-Pad.mmp", @"MMPTutorial3-Hardware-Pad.mmp", @"MMPTutorial4-Networking-Pad.mmp",@"MMPTutorial5-Files-Pad.mmp", @"MMPExamples-Vocoder-Pad.mmp", @"MMPExamples-Motion-Pad.mmp", @"MMPExamples-Sequencer-Pad.mmp",@"MMPExamples-GPS-Pad.mmp", @"MMPTutorial6-2DGraphics-Pad.mmp", @"MMPExamples-LANdini-Pad.mmp", @"MMPExamples-Arp-Pad.mmp",  @"MMPExamples-TableGlitch-Pad.mmp" ]];
    }
    // Common files.
    // NOTE InterAppOSC & Ping and connect all use one mmp version.
    [defaultPatches addObjectsFromArray: @[ @"MMPTutorial0-HelloSine.pd",@"MMPTutorial1-GUI.pd", @"MMPTutorial2-Input.pd", @"MMPTutorial3-Hardware.pd", @"MMPTutorial4-Networking.pd",@"MMPTutorial5-Files.pd",@"cats1.jpg", @"cats2.jpg",@"cats3.jpg",@"clap.wav",@"Welcome.pd",  @"MMPExamples-Vocoder.pd", @"vocod_channel.pd", @"MMPExamples-Motion.pd", @"MMPExamples-Sequencer.pd", @"MMPExamples-GPS.pd", @"MMPTutorial6-2DGraphics.pd", @"MMPExamples-LANdini.pd", @"MMPExamples-Arp.pd", @"MMPExamples-TableGlitch.pd", @"anderson1.wav", @"MMPExamples-InterAppOSC.mmp", @"MMPExamples-InterAppOSC.pd", @"MMPExamples-PingAndConnect.pd", @"MMPExamples-PingAndConnect.mmp", @"MMPExamples-NativeGUI.pd" ]];

    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];
    NSString *bundlePath = [[NSBundle mainBundle] bundlePath];

    for (NSString *patchName in defaultPatches) { //copy all from default and common into user folder
      NSString *patchDocPath = [publicDocumentsDir stringByAppendingPathComponent:patchName];
      NSString *patchBundlePath = [bundlePath stringByAppendingPathComponent:patchName];
      NSError *error = nil;
      if ([[NSFileManager defaultManager] fileExistsAtPath:patchDocPath]) {
        [[NSFileManager defaultManager] removeItemAtPath:patchDocPath error:&error];
      }
      [[NSFileManager defaultManager] copyItemAtPath:patchBundlePath toPath:patchDocPath error:&error];
    }

    // update version # that we've seen.
    [[NSUserDefaults standardUserDefaults] setObject:[NSNumber numberWithBool:YES]
                                              forKey:appFirstStartOfVersionKey];
  } //end first run and copy

  if (SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"6.0")) {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(audioRouteChange:)
                                                 name:AVAudioSessionRouteChangeNotification
                                               object:nil];
  }
  return self;
}

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

// called after user changing sample rate, channel count, etc. (But not ticks per buffer).
- (void)updateAudioState {
  _audioController.active = NO; //necc?
  [_audioController configurePlaybackWithSampleRate:_samplingRate
                                     numberChannels:_channelCount
                                       inputEnabled:_inputEnabled
                                      mixingEnabled:_mixingEnabled];
  // updating rate sets TPB to 1, so attempt to reset to prev value.
  if (_ticksPerBuffer !=_audioController.ticksPerBuffer) {
    [_audioController configureTicksPerBuffer:_ticksPerBuffer];
  }
  _audioController.active = YES;
  // set actual values
  _samplingRate = [_audioController sampleRate];
  _channelCount = [_audioController numberChannels];
  _ticksPerBuffer = [_audioController ticksPerBuffer];

  // tell settings to refresh display.
  [settingsVC updateAudioState];
}

- (void)viewDidLoad{
  [super viewDidLoad];

  // Look for output address and ports specified by user.
  if ([[NSUserDefaults standardUserDefaults] objectForKey:@"outputIPAddress"]) {
    _outputIpAddress = [[NSUserDefaults standardUserDefaults] objectForKey:@"outputIPAddress"];
  }
  if ([[NSUserDefaults standardUserDefaults] objectForKey:@"outputPortNumber"]) {
    _outputPortNumber = [[[NSUserDefaults standardUserDefaults] objectForKey:@"outputPortNumber"] intValue];
  }
  if ([[NSUserDefaults standardUserDefaults] objectForKey:@"inputPortNumber"]) {
    _inputPortNumber = [[[NSUserDefaults standardUserDefaults] objectForKey:@"inputPortNumber"] intValue];
  }

  [self connectPorts];

  // view
  self.view.backgroundColor = [UIColor grayColor];

  //setup upper left menu button, but don't add it anywhere yet.
  _settingsButton = [[MMPMenuButton alloc] init];
  [_settingsButton addTarget:self
                      action:@selector(showInfo:)
            forControlEvents:UIControlEventTouchUpInside];

  _settingsButtonOffset = self.view.frame.size.width * SETTINGS_BUTTON_OFFSET_PERCENT;
  _settingsButtonDim = MAX(25, self.view.frame.size.width * SETTINGS_BUTTON_DIM_PERCENT);

  // midi setup
  midi.delegate = self; // move to init?
  if ([midi.sources count] > 0) {
    [self connectMidiSource:midi.sources[0]]; //connect to first device in MIDI source list
  }
  if ([midi.destinations count] > 0) {
    [self connectMidiDestination:midi.destinations[0]]; //connect to first device in MIDI dest list
  }

  //delegates from settigns view for file loading, audio settings, networking, etc.
  settingsVC.delegate = self;
  settingsVC.audioDelegate = self;
  settingsVC.LANdiniDelegate = self;
  settingsVC.pingAndConnectDelegate = self;

  _navigationController = [[UINavigationController alloc] initWithRootViewController:settingsVC];
  _navigationController.navigationBar.barStyle = UIBarStyleBlack;
  _navigationController.modalTransitionStyle = UIModalTransitionStyleFlipHorizontal;

  [_audioController setActive:YES];

  // grab from defaults. TODO interact with settignsVC.
  _uiIsFlipped =
      [[NSUserDefaults standardUserDefaults] boolForKey:@"MMPFlipInterface"];

  // autoload a patch on startup.
  BOOL autoLoad = [[NSUserDefaults standardUserDefaults] boolForKey:@"MMPAutoLoadLastPatch"];
  if (autoLoad) {
    NSString *lastDocPath =
        [[NSUserDefaults standardUserDefaults] objectForKey:@"MMPLastOpenedInterfaceOrPdPath"];
    NSString *suffix = [[lastDocPath componentsSeparatedByString: @"."] lastObject];
    BOOL loaded = NO;
    if ([suffix isEqualToString:@"mmp"]) {
      loaded = [self loadMMPSceneFromDocPath:lastDocPath];
    } else if ([suffix isEqualToString:@"pd"]) {
      loaded = [self loadScenePatchOnlyFromDocPath:lastDocPath];
    }
    if (loaded) { //success.
      return;
    } else { //failure
      // TODO: this shows double-alert. Fix.
      UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Could not auto-load"
                          message: [NSString stringWithFormat:@"Could not auto-load %@, perhaps it moved?", lastDocPath]
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
    }
  }

  // load default intro patch
  MMPDeviceCanvasType hardwareCanvasType = [MMPViewController getCanvasType];
  NSString *path;
  if (hardwareCanvasType == canvasTypeWidePhone) {
    path = [[NSBundle mainBundle] pathForResource:@"Welcome" ofType:@"mmp"];
  } else if (hardwareCanvasType == canvasTypeTallPhone) {
    path = [[NSBundle mainBundle] pathForResource:@"Welcome-ip5" ofType:@"mmp"];
  } else { //pad
    path = [[NSBundle mainBundle] pathForResource:@"Welcome-Pad" ofType:@"mmp"];
  }

  BOOL loaded = [self loadMMPSceneFromFullPath:path];
  if (!loaded) {
    //still put menu button on failure.
    _settingsButton.transform = CGAffineTransformMakeRotation(0); //reset rotation.
    _settingsButton.frame =
        CGRectMake(_settingsButtonOffset, _settingsButtonOffset, _settingsButtonDim, _settingsButtonDim);
    [self.view addSubview:_settingsButton];
  }
}

//I believe next two methods were neccessary to receive "shake" gesture
- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  [self becomeFirstResponder];
}

- (void)viewWillDisappear:(BOOL)animated {
  [self resignFirstResponder];
  [super viewWillDisappear:animated];
}

-(void)setupAudioBus {
  //audioBus check for any audio issues, restart audio if detected.
  UInt32 channels;
  UInt32 size; //= sizeof(channels);
  OSStatus result =
      AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareInputNumberChannels, &size, &channels);

  if (result == kAudioSessionIncompatibleCategory) {
    // Audio session error (rdar://13022588). Power-cycle audio session.
    AudioSessionSetActive(false);
    AudioSessionSetActive(true);
    result =
        AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareInputNumberChannels, &size, &channels);
    if (result != noErr) {
      NSLog(@"Got error %d while querying input channels", (int)result);
    }
  }

  self.audiobusController = [[ABAudiobusController alloc] initWithApiKey:@"MCoqKk1vYk11UGxhdCoqKk1vYk11UGxhdC12Mi5hdWRpb2J1czovLw==:HJ1QCorzfgJBpb5B5TRb6zhp6o7wHg6UWI7RIkuJJmSMz9e5I+2F7R+cYSQjv9t0WoxYoxIBbYy/XDPGA2VslqaVcBUJ78WQkQ4KDrbKY/N5NndAHPhiAA+2DORdN733"];


  // Watch the audiobusAppRunning and connected properties
  [_audiobusController addObserver:self
                        forKeyPath:@"connected"
                           options:0
                           context:kAudiobusRunningOrConnectedChanged];
  [_audiobusController addObserver:self
                        forKeyPath:@"audiobusAppRunning"
                           options:0
                           context:kAudiobusRunningOrConnectedChanged];

  self.audiobusController.connectionPanelPosition = ABConnectionPanelPositionLeft;

  AudioComponentDescription senderACD;
  senderACD.componentType = kAudioUnitType_RemoteGenerator;
  senderACD.componentSubType = 'aout';
  senderACD.componentManufacturer = 'igle';

  ABSenderPort *sender = [[ABSenderPort alloc] initWithName:@"MobMuPlat Sender"
                                                      title:@"MobMuPlat Sender"
                                  audioComponentDescription:senderACD
                                                  audioUnit:self.audioController.audioUnit.audioUnit];
  [_audiobusController addSenderPort:sender];

  AudioComponentDescription filterACD;
  filterACD.componentType = kAudioUnitType_RemoteEffect;
  filterACD.componentSubType = 'afil';
  filterACD.componentManufacturer = 'igle';

  ABFilterPort *filterPort = [[ABFilterPort alloc] initWithName:@"MobMuPlat Filter"
                                                          title:@"MobMuPlat Filter"
                                      audioComponentDescription:filterACD
                                                      audioUnit:self.audioController.audioUnit.audioUnit];
  [_audiobusController addFilterPort:filterPort];

  ABReceiverPort *receiverPort = [[ABReceiverPort alloc] initWithName:@"MobMuPlat Receiver"
                                                                title:@"MobMuPlat Receiver"];
  [_audiobusController addReceiverPort:receiverPort];

  receiverPort.clientFormat = [self.audioController.audioUnit ASBDForSampleRate:_samplingRate
                                                                 numberChannels:_channelCount];

  if ([self.audioController.audioUnit isKindOfClass:[MobMuPlatPdAudioUnit class]]) {
    MobMuPlatPdAudioUnit *mmppdAudioUnit = (MobMuPlatPdAudioUnit *)self.audioController.audioUnit;
    mmppdAudioUnit.inputPort = receiverPort; //tell PD callback to look at it
  }
}

// observe audiobus from background, stop audio if we disconnect from audiobus
static void * kAudiobusRunningOrConnectedChanged = &kAudiobusRunningOrConnectedChanged;

-(void)observeValueForKeyPath:(NSString *)keyPath
                     ofObject:(id)object
                       change:(NSDictionary *)change
                      context:(void *)context {
  if ( context == kAudiobusRunningOrConnectedChanged ) {
    if ( [UIApplication sharedApplication].applicationState == UIApplicationStateBackground
        && !_audiobusController.connected
        && !_audiobusController.audiobusAppRunning
        && self.audioController.isActive ) {
      // Audiobus has quit. Time to sleep.
      [self.audioController setActive:NO];
    }
  } else {
    [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
  }
}

/*- (void)printAudioSessionUnitInfo {
  //audio info
  AVAudioSession *audioSession = [AVAudioSession sharedInstance];
  NSLog(@" aft Buffer size %f \n category %@ \n sample rate %f \n input latency %f \n other app playing? %d \n audiosession mode %@ \n audiosession output latency %f",audioSession.IOBufferDuration,audioSession.category,audioSession.sampleRate,audioSession.inputLatency,audioSession.isOtherAudioPlaying,audioSession.mode,audioSession.outputLatency);

  [self.audioController.audioUnit print];
}*/

#pragma mark - Private
// called often by accelerometer, package accel values and send to PD
- (void)accelerometerDidAccelerate:(CMAcceleration)acceleration {
  //first, "cook" the values to get a nice tilt value without going beyond -1 to 1
  float cookedX = acceleration.x;
  float cookedY = acceleration.y;

  //cook it via Z accel to see when we have tipped it beyond 90 degrees
  if (acceleration.x > 0 && acceleration.z > 0) {
    cookedX = (2-acceleration.x); //tip towards long side
  } else if (acceleration.x < 0 && acceleration.z > 0) {
    cookedX = (-2-acceleration.x); //tip away long side
  }

  if (acceleration.y > 0 && acceleration.z > 0) {
    cookedY = (2-acceleration.y); //tip right
  } else if (acceleration.y < 0 && acceleration.z > 0) {
    cookedY = (-2-acceleration.y); //tip left
  }

  //clip
  cookedX = MIN(MAX(cookedX, -1), 1);
  cookedY = MIN(MAX(cookedY, -1), 1);

  //send the cooked values as "tilts", and the raw values as "accel"
  NSArray *tiltsArray = @[ @"/tilts", @(cookedX), @(cookedY)];
  [PdBase sendList:tiltsArray toReceiver:@"fromSystem"];

  NSArray *accelArray = @[ @"/accel",@(acceleration.x), @(acceleration.y), @(acceleration.z)];
  [PdBase sendList:accelArray toReceiver:@"fromSystem"];
}

- (void)showInfo:(id)sender {
  if ([self respondsToSelector:@selector(presentViewController:animated:completion:)]) {
    // ios 6 and up.
    [self presentViewController:_navigationController animated:YES completion:nil];
  } else {
    // ios 5.
    [self presentModalViewController:_navigationController animated:YES];
  }
}

#pragma mark - AudioSettingsDelegate

- (BOOL)isAudioBusConnected {
  return self.audiobusController.connected;
}
- (int)blockSize {
  return [PdBase getBlockSize];
}

//return actual value. Note doesn't call [self updateAudioState]
- (int)setTicksPerBuffer:(int)newTick {
  [_audioController configureTicksPerBuffer:newTick];
  _ticksPerBuffer = [_audioController ticksPerBuffer];
  [settingsVC updateAudioState]; //send back to settings VC
  return _ticksPerBuffer;
}

//return actual value
- (int)setRate:(int)inRate {
  _samplingRate = inRate;
  [self updateAudioState]; // updates _samplingRate to actual val
  return _samplingRate;
}

- (int)setChannelCount:(int)newChannelCount {
  _channelCount = newChannelCount;
  [self updateAudioState]; // updates _channelCount to actual val
  return _channelCount;
}

- (int)sampleRate {
  return [self.audioController sampleRate];
}

- (int)actualTicksPerBuffer {
  //NSLog(@"actual ticks is %d",[audioController ticksPerBuffer] );
  return [_audioController ticksPerBuffer];
}

- (PGMidi*)midi {
  return midi;
}

- (void)connectPorts {//could probably use some error checking...
  if (_outputPortNumber > 0) {
    _oscOutPort = [_oscManager createNewOutputToAddress:_outputIpAddress atPort:_outputPortNumber];
  }
  if (_inputPortNumber > 0) {
    _oscInPort = [_oscManager createNewInputForPort:_inputPortNumber];
  }
  _outPortToNetworkingModules = [_oscManager createNewOutputToAddress:@"127.0.0.1" atPort:50506];
  _inPortFromNetworkingModules = [_oscManager createNewInputForPort:50505];
  _isPortsConnected = YES;
}

- (void)disconnectPorts {
  [_oscManager deleteAllInputs];
  [_oscManager deleteAllOutputs];
  _isPortsConnected = NO;
}

- (void)setOutputIpAddress:(NSString *)outputIpAddress {
  if ([_outputIpAddress isEqualToString:outputIpAddress]) {
    return;
  }
  _outputIpAddress = outputIpAddress;
  [_oscManager removeOutput:_oscOutPort];
  _oscOutPort = [_oscManager createNewOutputToAddress:_outputIpAddress atPort:_outputPortNumber];
  [[NSUserDefaults standardUserDefaults] setObject:outputIpAddress forKey:@"outputIPAddress"];
}

- (void)setOutputPortNumber:(int)outputPortNumber {
  if (_outputPortNumber == outputPortNumber) {
    return;
  }
  _outputPortNumber = outputPortNumber;
  [_oscManager removeOutput:_oscOutPort];
  _oscOutPort = [_oscManager createNewOutputToAddress:_outputIpAddress atPort:_outputPortNumber];
  [[NSUserDefaults standardUserDefaults] setObject:@(_outputPortNumber) forKey:@"outputPortNumber"];
}

- (void)setInputPortNumber:(int)inputPortNumber {
  if (_inputPortNumber == inputPortNumber) {
    return;
  }
  _inputPortNumber = inputPortNumber;
  [_oscManager removeOutput:_oscInPort];
  _oscInPort = [_oscManager createNewInputForPort:_inputPortNumber];
  [[NSUserDefaults standardUserDefaults] setObject:@(_inputPortNumber) forKey:@"inputPortNumber"];
}

//====settingsVC delegate methods

- (void)settingsViewControllerDidFinish:(SettingsViewController *)controller{
  [self dismissViewControllerAnimated:YES completion:nil];
}

-(void)flipInterface:(BOOL)isFlipped {
  _uiIsFlipped = isFlipped;
  if (isFlipped) {
    _scrollView.transform =
        _pdPatchView.transform = CGAffineTransformMakeRotation(M_PI+_isLandscape*M_PI_2);
  } else {
    _scrollView.transform =
        _pdPatchView.transform = CGAffineTransformMakeRotation(_isLandscape*M_PI_2);
  }
}

- (BOOL)loadScenePatchOnlyFromBundle:(NSBundle *)bundle filename:(NSString *)filename { //testing
  if (!filename) return NO;
  NSString *bundlePath = [bundle resourcePath] ;
  NSString *patchBundlePath = [bundlePath stringByAppendingPathComponent:filename];
  return [self loadScenePatchOnlyFromPath:patchBundlePath];
}

// assumes document dir
-(BOOL)loadScenePatchOnlyFromDocPath:(NSString *)docPath {
  if (!docPath.length) {
    return NO;
  }
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];
  NSString *fromPath = [publicDocumentsDir stringByAppendingPathComponent:docPath];

  BOOL loaded = [self loadScenePatchOnlyFromPath:fromPath];
  if (loaded) {
    [self trackLastOpenedDocPath:docPath];
  }
  return loaded;
}

- (BOOL)loadScenePatchOnlyFromPath:(NSString *)fromPath {
  if (!fromPath.length) {
    return NO;
  }
  [self loadSceneCommonReset];
  [_settingsButton setBarColor:[UIColor blackColor]];

  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];
  NSString *toPath = [publicDocumentsDir stringByAppendingPathComponent:@"tempPdFile"];

  NSArray *originalAtomLines = [PdParser getAtomLines:[PdParser readPatch:fromPath]];

  // Detect bad pd file.
  if ([originalAtomLines count] == 0 ||
      [originalAtomLines[0] count] < 6 ||
      ![originalAtomLines[0][1] isEqualToString:@"canvas"] ) {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Pd file not parsed"
                          message: [NSString stringWithFormat:@"Pd file not readable"]
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
    return NO;
  }

  // Process original atom lines into a set of gui lines and a set of shimmed patch lines.
  NSArray *processedAtomLinesTuple = [MMPPdPatchDisplayUtils proccessAtomLines:originalAtomLines];
  if (!processedAtomLinesTuple || processedAtomLinesTuple.count != 2) {
    return NO;
  }
  NSArray *patchAtomLines = processedAtomLinesTuple[0];
  NSArray *guiAtomLines = processedAtomLinesTuple[1];

  // Reformat patchAtomLines into a pd file.
  NSMutableString *outputMutableString = [NSMutableString string];
  for (NSArray *line in patchAtomLines) {
    [outputMutableString appendString:[line componentsJoinedByString:@" "]];
    [outputMutableString appendString:@";\n"];
  }

  //handle outputString as non-mutable.
  NSString *outputString = (NSString *)outputMutableString;

  // Write temp pd file to disk.
  if (![outputString canBeConvertedToEncoding:NSASCIIStringEncoding] ) {
    // Writing to ascii would fail in Automatism patches. Check first and do lossy conversion.
    NSData *asciiData = [outputString dataUsingEncoding:NSASCIIStringEncoding
                                   allowLossyConversion:YES];
    outputString = [[NSString alloc] initWithData:asciiData encoding:NSASCIIStringEncoding];
  }

  NSError *error;
  [outputString writeToFile:toPath atomically:YES encoding:NSASCIIStringEncoding error:&error];
  if (error) {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Pd file not parsed"
                          message: [NSString stringWithFormat:@"Pd file not parseable for native display"]
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
    return NO;
  }

  // Compute canvas size
  CGSize docCanvasSize = CGSizeMake([originalAtomLines[0][4] floatValue], [originalAtomLines[0][5] floatValue]);
  // TODO check for zero/bad values
  BOOL isOrientationLandscape = (docCanvasSize.width > docCanvasSize.height);
  CGSize hardwareCanvasSize = CGSizeZero;
  if (isOrientationLandscape) {
    hardwareCanvasSize = CGSizeMake([[UIScreen mainScreen] bounds].size.height,
                                    [[UIScreen mainScreen] bounds].size.width);
  } else {
    hardwareCanvasSize = CGSizeMake([[UIScreen mainScreen] bounds].size.width,
                                    [[UIScreen mainScreen] bounds].size.height);
  }
  CGFloat hardwareCanvasRatio = hardwareCanvasSize.width / hardwareCanvasSize.height;
  CGFloat canvasRatio = docCanvasSize.width / docCanvasSize.height;

  CGFloat canvasWidth = 0, canvasHeight = 0;
  if (canvasRatio > hardwareCanvasRatio) {
    // The doc canvas has a wider aspect ratio than the hardware canvas;
    // It will take the width of the screen and get letterboxed on top.
    canvasWidth = hardwareCanvasSize.width ;
    canvasHeight = canvasWidth / canvasRatio;
    _pdPatchView = [[UIView alloc] initWithFrame:
                       CGRectMake(0,
                                  (hardwareCanvasSize.height - canvasHeight) / 2.0f,
                                  canvasWidth,
                                  canvasHeight)];
  } else {
    // The doc canvas has a taller aspect ratio thatn the hardware canvas;
    // It will take the height of the screen and get letterboxed on the sides.
    canvasHeight = hardwareCanvasSize.height;
    canvasWidth = canvasHeight * canvasRatio;
    _pdPatchView = [[UIView alloc] initWithFrame:
                       CGRectMake((hardwareCanvasSize.width - canvasWidth) / 2.0f,
                                  0,
                                  canvasWidth,
                                  canvasHeight)];
  }

  _pdPatchView.clipsToBounds = YES; // Keep Pd gui boxes rendered within the view.
  _pdPatchView.backgroundColor = [UIColor whiteColor];
  [self.view addSubview:_pdPatchView];

  if (isOrientationLandscape) { //rotate
    _isLandscape = YES;
    _pdPatchView.center =
        CGPointMake(hardwareCanvasSize.height / 2.0f, hardwareCanvasSize.width / 2.0f);
    if (_uiIsFlipped) {
      _pdPatchView.transform = CGAffineTransformMakeRotation(M_PI_2+M_PI);
      _settingsButton.transform = CGAffineTransformMakeRotation(M_PI_2+M_PI);
      _settingsButton.frame =
          CGRectMake(_settingsButtonOffset,
                     self.view.frame.size.height - _settingsButtonOffset - _settingsButtonDim,
                     _settingsButtonDim,
                     _settingsButtonDim);
    } else {
      _pdPatchView.transform = CGAffineTransformMakeRotation(M_PI_2);
      _settingsButton.frame =
          CGRectMake(self.view.frame.size.width - _settingsButtonDim - _settingsButtonOffset,
                     _settingsButtonOffset,
                     _settingsButtonDim,
                     _settingsButtonDim);
      _settingsButton.transform = CGAffineTransformMakeRotation(M_PI_2);
    }
  } else {
    _isLandscape = NO;
    if (_uiIsFlipped) {
      _pdPatchView.transform = CGAffineTransformMakeRotation(M_PI);
      _settingsButton.transform = CGAffineTransformMakeRotation(M_PI);
      _settingsButton.frame =
          CGRectMake(self.view.frame.size.width - _settingsButtonDim - _settingsButtonOffset,
                     self.view.frame.size.height -_settingsButtonDim -_settingsButtonOffset,
                     _settingsButtonDim,
                     _settingsButtonDim);

    } else {
      _settingsButton.transform = CGAffineTransformMakeRotation(0);
      _settingsButton.frame =
          CGRectMake(_settingsButtonOffset,
                     _settingsButtonOffset,
                     _settingsButtonDim,
                     _settingsButtonDim);
    }
  }
  //DEI todo update button pos/rot on flipping.

  _pdGui.parentViewSize = CGSizeMake(canvasWidth, canvasHeight);
  [_pdGui addWidgetsFromAtomLines:guiAtomLines]; // create widgets first

  //
  _openPDFile = [PdFile openFileNamed:@"tempPdFile" path:publicDocumentsDir]; //widgets get loadbang
  if (!_openPDFile) {
    return NO;
  }

  for(Widget *widget in _pdGui.widgets) {
    [widget replaceDollarZerosForGui:_pdGui fromPatch:_openPDFile];
    [_pdPatchView addSubview:widget];
  }
  [_pdGui reshapeWidgets];

  for(Widget *widget in _pdGui.widgets) {
    [widget setup];
  }

  [self.view addSubview:_settingsButton];

  return YES;
}

- (void)trackLastOpenedDocPath:(NSString *)docPath {
  // Can't store full path, need to store path relative to documents.
  [[NSUserDefaults standardUserDefaults] setObject:docPath
                                            forKey:@"MMPLastOpenedInterfaceOrPdPath"];
}

- (void)loadSceneCommonReset {
  // Recurse and add subfolders to Pd directory paths. TODO: only do on file system change?
  [PdBase clearSearchPath];
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];
  // Add documents directory.
  [PdBase addToSearchPath:publicDocumentsDir];
  // Iterate subfolders.
  NSDirectoryEnumerator *enumerator =
      [[NSFileManager defaultManager] enumeratorAtURL:[NSURL URLWithString:publicDocumentsDir]
                           includingPropertiesForKeys:@[ NSURLIsDirectoryKey ]
                                              options:NSDirectoryEnumerationSkipsHiddenFiles
                                         errorHandler:nil];

  // Add bundle folder reference that has the "extra"s abstractions
  NSString *extrasDir =
      [[[NSBundle mainBundle] bundlePath] stringByAppendingPathComponent:@"extra"];
  [PdBase addToSearchPath:extrasDir];

  NSURL *fileURL;
  NSNumber *isDirectory;
  while ((fileURL = [enumerator nextObject]) != nil) {
    BOOL success = [fileURL getResourceValue:&isDirectory forKey:NSURLIsDirectoryKey error:nil];
    if (success && [isDirectory boolValue]) {
      [PdBase addToSearchPath:[fileURL path]];
    }
  }
  // End recursion of file paths.

  if (_scrollView) {
    [_scrollView removeFromSuperview];
    _scrollView = nil;
  }

  [_addressToGUIObjectsDict removeAllObjects];

  for (Widget *widget in _pdGui.widgets) {
    [widget removeFromSuperview];
  }
  [_pdGui.widgets removeAllObjects];

  if (_pdPatchView) {
    [_pdPatchView removeFromSuperview];
    _pdPatchView = nil;
  }

  // Dispatcher holds strong references to listeners, manually remove so that they get dealloced.
  [_mmpPdDispatcher removeAllListeners];

  // (re-)Add self as dispatch recipient for MMP symbols.
  [_mmpPdDispatcher addListener:self forSource:@"toGUI"];
  [_mmpPdDispatcher addListener:self forSource:@"toNetwork"];
  [_mmpPdDispatcher addListener:self forSource:@"toSystem"];

  //if pdfile is open, close it
  if (_openPDFile != nil) {
    [_openPDFile closeFile];
    _openPDFile = nil;
  }

  [_locationManager stopUpdatingLocation];
  [_locationManager stopUpdatingHeading];
}

- (BOOL)loadMMPSceneFromFullPath:(NSString *)fullPath {
  NSString *jsonString =
      [NSString stringWithContentsOfFile:fullPath encoding:NSUTF8StringEncoding error:nil];
  NSData *data = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  if (!data) {
    return NO;
  }
  NSDictionary *sceneDict = [NSJSONSerialization JSONObjectWithData:data options:nil error:nil];

  return [self loadMMPSceneFromJSON:sceneDict];
}

- (BOOL)loadMMPSceneFromDocPath:(NSString *)docPath {
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];
  NSString *fullPath = [publicDocumentsDir stringByAppendingPathComponent:docPath];

  BOOL loaded = [self loadMMPSceneFromFullPath:fullPath];
  if (loaded) {
    [self trackLastOpenedDocPath:docPath];
  }
  return loaded;
}

- (BOOL)loadMMPSceneFromJSON:(NSDictionary *)sceneDict {
  if (!sceneDict) {
    return NO;
  }

  [self loadSceneCommonReset];

  // patch specification version, incremented on breaking changes.
  // current version is 2, lower versions have old slider range behavior.
  NSUInteger version = [sceneDict[@"version"] unsignedIntegerValue];

  //type of canvas used in the _editor_ to make the interface. If it doesn't match the above
  // hardwareCnvasType, then we will be scaling to fit
  MMPDeviceCanvasType editorCanvasType = canvasTypeWidePhone; //default.
  // include deprecated strings
  if (sceneDict [@"canvasType"]) {
    NSString *typeString = (NSString*)sceneDict[@"canvasType"];
    if ([typeString isEqualToString:@"iPhone3p5Inch"] || [typeString isEqualToString:@"widePhone"]) {
      editorCanvasType = canvasTypeWidePhone;
    } else if ([typeString isEqualToString:@"iPhone4Inch"] || [typeString isEqualToString:@"tallPhone"]) {
      editorCanvasType = canvasTypeTallPhone;
    } else if ([typeString isEqualToString:@"android7Inch"] || [typeString isEqualToString:@"tallTablet"]) {
      editorCanvasType = canvasTypeTallTablet;
    } else if ([typeString isEqualToString:@"iPad"] || [typeString isEqualToString:@"wideTablet"]) {
      editorCanvasType = canvasTypeWideTablet;
    }
  }

  // get two necessary layout values from the JSON file
  // page count
  _pageCount = 1; //default.
  if (sceneDict[@"pageCount"]) {
    _pageCount = [sceneDict[@"pageCount"] intValue];
    if (_pageCount <= 0) {
      _pageCount = 1;
    }
  }

  // get orientation and init scrollview
  BOOL isOrientationLandscape = NO; //default.
  if (sceneDict[@"isOrientationLandscape"]) {
    isOrientationLandscape = [sceneDict[@"isOrientationLandscape"] boolValue];
  }

  //get layout of the scrollview that holds the GUI
  float zoomFactor = 1;
  CGRect scrollViewFrame;

  CGSize hardwareCanvasSize;
  if (isOrientationLandscape) {
    hardwareCanvasSize = CGSizeMake([[UIScreen mainScreen] bounds].size.height,
                                    [[UIScreen mainScreen] bounds].size.width);
  } else {
    hardwareCanvasSize = CGSizeMake([[UIScreen mainScreen] bounds].size.width,
                                    [[UIScreen mainScreen] bounds].size.height);
  }
  CGFloat hardwareCanvasRatio = hardwareCanvasSize.width / hardwareCanvasSize.height;

  CGSize docCanvasSize;
  CGFloat canvasWidth, canvasHeight;
  CGFloat canvasRatio;
  switch(editorCanvasType) {
    case canvasTypeWidePhone:
      docCanvasSize = isOrientationLandscape ? CGSizeMake(480, 320) : CGSizeMake(320, 480);
      break;
    case canvasTypeTallPhone:
      docCanvasSize = isOrientationLandscape ? CGSizeMake(568, 320) : CGSizeMake(320, 568);
      break;
    case canvasTypeTallTablet:
      docCanvasSize = isOrientationLandscape ? CGSizeMake(950, 600) : CGSizeMake(600, 950);
      break;
    case canvasTypeWideTablet:
      docCanvasSize = isOrientationLandscape ? CGSizeMake(1024, 768) : CGSizeMake(768, 1024);
      break;
  }

  canvasRatio = docCanvasSize.width / docCanvasSize.height;

  if (canvasRatio > hardwareCanvasRatio) {
    // The doc canvas has a wider aspect ratio than the hardware canvas;
    // It will take the width of the screen and get letterboxed on top.
    zoomFactor = hardwareCanvasSize.width / docCanvasSize.width;
    canvasWidth = hardwareCanvasSize.width ;
    canvasHeight = canvasWidth / canvasRatio;
    scrollViewFrame = CGRectMake(0,
                                 (hardwareCanvasSize.height - canvasHeight) / 2.0f,
                                 canvasWidth,
                                 canvasHeight);
  } else {
    // The doc canvas has a taller aspect ratio thatn the hardware canvas;
    // It will take the height of the screen and get letterboxed on the sides.
    zoomFactor = hardwareCanvasSize.height/ docCanvasSize.height;
    canvasHeight = hardwareCanvasSize.height;
    canvasWidth = canvasHeight * canvasRatio;
    scrollViewFrame = CGRectMake((hardwareCanvasSize.width - canvasWidth) / 2.0f,
                                 0,
                                 canvasWidth,
                                 canvasHeight);
  }

  _scrollView = [[UIScrollView alloc]initWithFrame:scrollViewFrame];
  _scrollInnerView = [[UIView alloc]initWithFrame:CGRectMake(0,
                                                             0,
                                                             docCanvasSize.width*_pageCount,
                                                             docCanvasSize.height)];

  [_scrollView setContentSize:_scrollInnerView.frame.size];
  [_scrollView addSubview:_scrollInnerView];

  if (isOrientationLandscape) { //rotate
    _isLandscape = YES;
    CGPoint rotatePoint =
        CGPointMake(hardwareCanvasSize.height / 2.0f, hardwareCanvasSize.width / 2.0f);
    _scrollView.center = rotatePoint;
    if (_uiIsFlipped) {
      _scrollView.transform = CGAffineTransformMakeRotation(M_PI_2+M_PI);
    } else {
      _scrollView.transform = CGAffineTransformMakeRotation(M_PI_2);
    }
  } else {
    _isLandscape = NO;
    if (_uiIsFlipped) {
      _scrollView.transform = CGAffineTransformMakeRotation(M_PI);
    }
  }

  _scrollView.pagingEnabled = YES;
  _scrollView.delaysContentTouches = NO;
  _scrollView.maximumZoomScale = zoomFactor;
  _scrollView.minimumZoomScale = zoomFactor;
  [_scrollView setDelegate:self];
  [self.view addSubview:_scrollView];

  // start page
  int startPageIndex = 0;
  if (sceneDict[@"startPageIndex"]) {
    startPageIndex = [sceneDict[@"startPageIndex"] intValue];
    //check if beyond pageCount, then set to last page
    if (startPageIndex > _pageCount) {
      startPageIndex = _pageCount - 1;
    }
  }

  // bg color
  if (sceneDict[@"backgroundColor"]) {
    _scrollView.backgroundColor = [MeControl colorFromRGBArray:sceneDict[@"backgroundColor"]];
    [_settingsButton setBarColor:[MeControl inverseColorFromRGBArray:sceneDict[@"backgroundColor"]]];
  } else {
    [_settingsButton setBarColor:[UIColor whiteColor]]; //default, but shouldn't happen.
  }

  if (sceneDict[@"menuButtonColor"]) {
    [_settingsButton setBarColor:[MeControl colorFromRGBAArray:sceneDict[@"menuButtonColor"]]];
  }

  // get array of all widgets
  NSArray *controlArray = sceneDict[@"gui"];
  if (!controlArray) {
    return NO;
  }

  // check that it is an array of NSDictionary
  if (controlArray.count > 0 && ![controlArray[0] isKindOfClass:[NSDictionary class]]) {
    return NO;
  }

  // step through each gui widget, big loop each time
  for (NSDictionary *currDict in controlArray) {
    MeControl *currObject;

    //start with elements common to all widget subclasses
    //frame - if no frame is found, skip this widget
    NSArray *frameRectArray = currDict[@"frame"];
    if (frameRectArray.count != 4) {
      continue;
    }

    CGRect frame = CGRectMake([frameRectArray[0] floatValue],
                              [frameRectArray[1] floatValue],
                              [frameRectArray[2] floatValue],
                              [frameRectArray[3] floatValue]);


    // widget color
    UIColor *color = [UIColor colorWithRed:1 green:1 blue:1 alpha:1]; // default.
    if (currDict[@"color"]) {
      NSArray *colorArray = currDict[@"color"];
      if (colorArray.count == 3) { //old format before translucency
        color = [MeControl colorFromRGBArray:colorArray];
      } else if (colorArray.count == 4) { //newer format including transulcency
        color = [MeControl colorFromRGBAArray:colorArray];
      }
    }

    //widget highlight color
    UIColor *highlightColor = [UIColor grayColor]; // default.
    if (currDict[@"highlightColor"]) {
      NSArray *highlightColorArray = currDict[@"highlightColor"];
      if (highlightColorArray.count == 3) {
        highlightColor = [MeControl colorFromRGBArray:highlightColorArray];
      } else if (highlightColorArray.count == 4) {
        highlightColor = [MeControl colorFromRGBAArray:highlightColorArray];
      }
    }

    // get the subclass type, and do subclass-specific stuff
    NSString *newObjectClass = currDict[@"class"];
    if (!newObjectClass) {
      continue;
    }
    if ([newObjectClass isEqualToString:@"MMPSlider"]) {
      MeSlider *slider = [[MeSlider alloc] initWithFrame:frame];
      currObject = slider;
      if ([currDict[@"isHorizontal"] boolValue] == YES) {
        [slider setHorizontal];
      }

      if (currDict[@"range"]) {
        int range = [currDict[@"range"] intValue];
        if (version < 2) {
          // handle old style of slider ranges.
          [slider setLegacyRange:range];
        } else {
          [slider setRange:range];
        }
      }
    } else if ([newObjectClass isEqualToString:@"MMPKnob"]) {
      MeKnob *knob = [[MeKnob alloc] initWithFrame:frame];
      currObject = knob;
      if (currDict[@"range"]) {
        int range = [currDict[@"range"] intValue];
        if (version < 2) {
          // handle old style of knob ranges.
          [knob setLegacyRange:range];
        } else {
          [knob setRange:range];
        }
      }
      UIColor *indicatorColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:1];
      if (currDict[@"indicatorColor"]) {
        indicatorColor = [MeControl colorFromRGBAArray:currDict[@"indicatorColor"]];
      }
      [knob setIndicatorColor:indicatorColor];
    } else if ([newObjectClass isEqualToString:@"MMPLabel"]) {
      MeLabel *label = [[MeLabel alloc] initWithFrame:frame];
      currObject = label;

      if (currDict[@"text"]) {
        label.stringValue = currDict[@"text"];
      }
      if (currDict[@"textSize"]) {
        label.textSize = [currDict[@"textSize"] intValue];
      }
      if (currDict[@"textFont"] && currDict[@"textFontFamily"]) {
        [label setFontFamily:currDict[@"textFontFamily"] fontName:currDict[@"textFont"]];
      }
      if ([currDict[@"hAlign"] isKindOfClass:[NSNumber class]]) {
        label.horizontalTextAlignment = [currDict[@"hAlign"] integerValue];
      }
      if ([currDict[@"vAlign"] isKindOfClass:[NSNumber class]]) {
        label.verticalTextAlignment = [currDict[@"vAlign"] integerValue];
      }
      [label sizeToFit];
    } else if ([newObjectClass isEqualToString:@"MMPButton"]) {
      currObject = [[MeButton alloc] initWithFrame:frame];
    } else if ([newObjectClass isEqualToString:@"MMPToggle"]) {
      MeToggle *toggle = [[MeToggle alloc] initWithFrame:frame];
      currObject = toggle;
      if (currDict[@"borderThickness"]) {
        toggle.borderThickness = [currDict[@"borderThickness"] intValue];
      }
    } else if ([newObjectClass isEqualToString:@"MMPXYSlider"]) {
      currObject = [[MeXYSlider alloc]initWithFrame:frame];
    } else if ([newObjectClass isEqualToString:@"MMPGrid"]) {
      MeGrid *grid = [[MeGrid alloc] initWithFrame:frame];
      currObject = grid;
      if (currDict[@"mode"]) {
        grid.mode = [currDict[@"mode"] intValue]; //needs to be done before setting dim.
      }
      if (currDict[@"dim"]) {
        NSArray *dimArray = currDict[@"dim"];
        if (dimArray.count == 2) {
          [grid setDimX:[dimArray[0] intValue] Y:[dimArray[1] intValue]];
        }
      }
      if (currDict[@"cellPadding"]) {
        grid.cellPadding = [currDict[@"cellPadding"] intValue];
      }
      if (currDict[@"borderThickness"]) {
        grid.borderThickness = [currDict[@"borderThickness"] intValue];
      }
    } else if ([newObjectClass isEqualToString:@"MMPPanel"]) {
      MePanel *panel = [[MePanel alloc] initWithFrame:frame];
      currObject = panel;
      if (currDict[@"imagePath"]) {
        panel.imagePath = currDict[@"imagePath"];
      }
      if (currDict[@"passTouches"]) {
        panel.shouldPassTouches = [currDict[@"passTouches"] boolValue];
      }
    } else if ([newObjectClass isEqualToString:@"MMPMultiSlider"]) {
      MeMultiSlider *multiSlider = [[MeMultiSlider alloc] initWithFrame:frame];
      currObject = multiSlider;
      if (currDict[@"range"]) {
        multiSlider.range = [currDict [@"range"] intValue];
      }
      if (currDict[@"outputMode"]) {
        multiSlider.outputMode = [currDict [@"outputMode"] integerValue];
      }
    } else if ([newObjectClass isEqualToString:@"MMPLCD"]) {
      currObject = [[MeLCD alloc] initWithFrame:frame];
    } else if ([newObjectClass isEqualToString:@"MMPMultiTouch"]) {
      currObject = [[MeMultiTouch alloc] initWithFrame:frame];
    } else if ([newObjectClass isEqualToString:@"MMPMenu"]) {
      MeMenu *menu = [[MeMenu alloc] initWithFrame:frame];
      currObject = menu;
      if (currDict[@"title"]) {
        menu.titleString = currDict[@"title"];
      }
    } else if ([newObjectClass isEqualToString:@"MMPTable"]) {
      MeTable *table = [[MeTable alloc] initWithFrame:frame];
      currObject = table;
      if (currDict[@"mode"]) {
        table.mode = [currDict [@"mode"] intValue];
      }
      if (currDict[@"selectionColor"]) {
        table.selectionColor = [MeControl colorFromRGBAArray:currDict[@"selectionColor"]];
      }
      if (currDict [@"displayRangeLo"]) {
        table.displayRangeLo = [currDict[@"displayRangeLo"] floatValue];
      }
      if (currDict [@"displayRangeHi"]) {
        table.displayRangeHi = [currDict[@"displayRangeHi"] floatValue];
      }
      if (currDict [@"displayMode"]) {
        table.displayMode = [currDict[@"displayMode"] integerValue];
      }
    } else {
      MeUnknown *unknownWidget = [[MeUnknown alloc] initWithFrame:frame];
      currObject = unknownWidget;
      [unknownWidget setWarning:newObjectClass];
    }
    //end subclass-specific list

    if (!currObject) { // failed to create object
      continue;
    } else { // if successfully created object
      currObject.controlDelegate = self;
      [currObject setColor:color];
      [currObject setHighlightColor:highlightColor];

      // set OSC address for widget
      NSString *address = @"dummy";
      if (currDict[@"address"]) {
        address = currDict[@"address"];
      }
      currObject.address = address;

      // Add to address array in _addressToGUIObjectsDict
      NSMutableArray *addressArray = _addressToGUIObjectsDict[currObject.address];
      if (!addressArray) {
        addressArray = [NSMutableArray array];
        _addressToGUIObjectsDict[currObject.address] = addressArray;
      }
      [addressArray addObject:currObject];

      [_scrollInnerView addSubview:currObject];
    }
  }
  //end of big loop through widgets

  //scroll to start page, and put settings button on top
  [_scrollView zoomToRect:CGRectMake(docCanvasSize.width * startPageIndex,
                                     0,
                                     docCanvasSize.width,
                                     docCanvasSize.height)
                 animated:NO];
  _settingsButton.transform = CGAffineTransformMakeRotation(0);
  _settingsButton.frame = CGRectMake(_settingsButtonOffset,
                                     _settingsButtonOffset,
                                     _settingsButtonDim,
                                     _settingsButtonDim);
  [_scrollView addSubview:_settingsButton];

  ///===PureData patch
  if (sceneDict[@"pdFile"]) {
    NSString *filename = sceneDict[@"pdFile"];

    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];

    _openPDFile = [PdFile openFileNamed:filename path:publicDocumentsDir];
    NSLog(@"open pd file %@", filename );

    if (_openPDFile == nil) { //failure to load the named patch
      NSLog(@"did not find named patch!" );
      NSString *message =
          [NSString stringWithFormat:@"Pd file %@ not found, make sure you add it to Documents in iTunes",
               filename];
      UIAlertView *alert = [[UIAlertView alloc]
                            initWithTitle: @"Pd file not found"
                            message:message
                            delegate: nil
                            cancelButtonTitle:@"OK"
                            otherButtonTitles:nil];
      [alert show];

    } else { //success
      //refresh tables
      //TODO optimize! make an array of tables only
      for (NSArray *addressArray in [_addressToGUIObjectsDict allValues]) {
        for(MeControl *control in addressArray) {
          if ([control isKindOfClass:[MeTable class]]) {
            // use set to quash multiple loads of same table/address - not needed in app, but needed in editor.
            [(MeTable*)control loadTable];
          }
        }
      }
    }
  } else {//if no JSON entry found for file, say so
    _openPDFile=nil;
    NSLog(@"did not find a patch name!" );
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Pd file not specified"
                          message: @"This interface has not been linked to a Pd file. Add it in the editor!"
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
  }

  return YES;
}

- (void)setAudioInputEnabled:(BOOL)enabled {
  _inputEnabled = enabled;
  [self updateAudioState];
}

#pragma mark - scrollview delegate

- (UIView *)viewForZoomingInScrollView:(UIScrollView *)scrollView {
  return _scrollInnerView;
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)inScrollView {
  if (inScrollView == _scrollView) {
    int page = inScrollView.contentOffset.x / inScrollView.frame.size.width;
    [PdBase sendList:@[ @"/page", @(page) ] toReceiver:@"fromSystem"];
  }
}

#pragma mark - ControlDelegate

//I want to send a message into PD patch from a gui widget
- (void)sendGUIMessageArray:(NSArray *)msgArray {
  [PdBase sendList:msgArray toReceiver:@"fromGUI"];
}

- (UIColor *)patchBackgroundColor {
  return _scrollView.backgroundColor;
}

- (UIInterfaceOrientation)orientation {
  if (_isLandscape) {
    if (_uiIsFlipped) {
      return UIInterfaceOrientationLandscapeLeft;
    } else {
      return UIInterfaceOrientationLandscapeRight;
    }
  } else {
    if (_uiIsFlipped) {
      return UIInterfaceOrientationPortraitUpsideDown;
    } else {
      return UIInterfaceOrientationPortrait;
    }
  }
}

//not used
/*- (void)receiveSymbol:(NSString *)symbol fromSource:(NSString *)source{
}*/

+ (OSCMessage*) oscMessageFromList:(NSArray *)list {
  if (!list.count) {
    return nil;
  }
  OSCMessage *msg = [OSCMessage createWithAddress:list[0]];
  for (id item in [list subarrayWithRange:NSMakeRange(1, list.count - 1)]) {
    if ([item isKindOfClass:[NSString class]]) [msg addString:item];
    else if ([item isKindOfClass:[NSNumber class]]) {
      NSNumber *itemNumber = (NSNumber*)item;
      if ([MobMuPlatUtil numberIsFloat:itemNumber]) {
        [msg addFloat:[item floatValue]];
      } else {
        [msg addInt:[item intValue]]; //never used, right?
      }
    }
  }
  return msg;
}

//PureData has sent out a message from the patch (from a receive object, we look for messages from "toNetwork","toGUI","toSystem")
- (void)receiveList:(NSArray *)list fromSource:(NSString *)source {
  if (!list.count) { //guarantee at least one item in array.
    NSLog(@"got zero args from %@", source);
    return; //protect against bad elements that got dropped from array...
  }
  if ([source isEqualToString:@"toNetwork"]) {
    NSString *address = list[0];
    if (![address isKindOfClass:[NSString class]]) {
      NSLog(@"toNetwork first element is not string");
      return;
    }
    //look for LANdini/P&C- this clause looks for /send, /send/GD, /send/OGD.
    if ([address isEqualToString:@"/send"] ||
       [address isEqualToString:@"/send/GD"] ||
       [address isEqualToString:@"/send/OGD"] ) {
      if (_llm.enabled || _pacm.enabled) {
        OSCMessage *message = [MMPViewController oscMessageFromList:list];
        if (message) {
          [_outPortToNetworkingModules sendThisPacket:[OSCPacket createWithContent:message]];
        }
      } else {
        //landini /ping & connect disabled: remake message without the first 2 landini elements and send out normal port
        if (list.count > 2) {
          NSArray *newList = [list subarrayWithRange:NSMakeRange(2, list.count - 2)];
          OSCMessage *message = [MMPViewController oscMessageFromList:newList];
          if (message) {
            [_oscOutPort sendThisPacket:[OSCPacket createWithContent:message]];
          }
        }
      }
    } else if ([address isEqualToString:@"/networkTime"] || //other landini/P&C messages, keep passing to landini
               [address isEqualToString:@"/numUsers"] ||
               [address isEqualToString:@"/userNames"] ||
               [address isEqualToString:@"/myName"] ||
               [address isEqualToString:@"/playerCount"] ||
               [address isEqualToString:@"/playerNumberSet"] ||
               [address isEqualToString:@"/playerIpList"] ||
               [address isEqualToString:@"/myPlayerNumber"]) {
      OSCMessage *message = [MMPViewController oscMessageFromList:list];
      if (message) {
        [_outPortToNetworkingModules sendThisPacket:[OSCPacket createWithContent:message]];
      }
    } else if ([address isEqualToString:@"/landini/enable"] &&
               list.count >= 2 &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      _llm.enabled = ([list[1] floatValue] > 0);
    } else if ([address isEqualToString:@"/pingAndConnect/enable"] &&
               list.count >= 2 &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      _pacm.enabled = ([list[1] floatValue] > 0);
    } else if ([address isEqualToString:@"/landini/isEnabled"]) {
      NSArray *msgArray = @[ @"/landini/isEnabled", [NSNumber numberWithBool:_llm.enabled] ];
      [PdBase sendList:msgArray toReceiver:@"fromNetwork"];
    } else if ([address isEqualToString:@"/pingAndConnect/isEnabled"]) {
      NSArray *msgArray = @[ @"/pingAndConnect/isEnabled", [NSNumber numberWithBool:_pacm.enabled] ];
      [PdBase sendList:msgArray toReceiver:@"fromNetwork"];
    } else if ([address isEqualToString:@"/pingAndConnect/myPlayerNumber"] && list.count >= 2) {
      if ([list[1] isKindOfClass:[NSString class]] && [list[1] isEqualToString:@"server"]) {
        _pacm.playerNumber = -1; // -1 is the "server" value.
      } else if ([list[1] isKindOfClass:[NSNumber class]]) {
        // no bounds/error checking!
        _pacm.playerNumber = [list[1] integerValue];
      }
    } else{ //not for landini - send out regular!
      OSCMessage *message = [MMPViewController oscMessageFromList:list];
      if (message) {
        [_oscOutPort sendThisPacket:[OSCPacket createWithContent:message]];
      }
    }
  } else if ([source isEqualToString:@"toGUI"]) {
    NSMutableArray *addressArray = _addressToGUIObjectsDict[list[0]]; // addressArray can be nil.
    for (MeControl *control in addressArray) {
      [control receiveList:[list subarrayWithRange:NSMakeRange(1, [list count]-1)]];
    }
  } else if ([source isEqualToString:@"toSystem"]) {
    // TODO array size checking!
    //for some reason, there is a conflict with the audio session, and sending a command to vibrate
    // doesn't work unless user flisp audio switch
    if ([list[0] isEqualToString:@"/vibrate"]) {
      if (list.count > 1 && [list[1] isKindOfClass:[NSNumber class]] && [list[1] floatValue] == 2) {
        AudioServicesPlaySystemSound(1311); //"/vibrate 2"
      } else{ //"/vibrate" or "vibrate 1 or non-2
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate);
      }
    } else if (list.count == 2 && //camera flash
               [list[0] isEqualToString:@"/flash"] &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      float val = [list[1] floatValue];
      if ([_avCaptureDevice hasTorch]) {
        [_avCaptureDevice lockForConfiguration:nil];
        if (val > 0) {
          [_avCaptureDevice setTorchMode:AVCaptureTorchModeOn];
        } else {
          [_avCaptureDevice setTorchMode:AVCaptureTorchModeOff];
        }
        [_avCaptureDevice unlockForConfiguration];
      }
    } else if (list.count == 2 &&
               [list[0] isEqualToString:@"/setAccelFrequency"] &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      float val = [list[1] floatValue];
      val = (MIN(MAX(val,0.01), 100)); // clip .01 to 100
      _motionManager.accelerometerUpdateInterval = 1.0 / val;
    } else if ([list[0] isEqualToString:@"/getAccelFrequency"]) {
      NSArray *msgArray = @[ @"/accelFrequency", @(_motionManager.accelerometerUpdateInterval) ];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    } else if (list.count == 2 &&
            [list[0] isEqualToString:@"/setGyroFrequency"] &&
            [list[1] isKindOfClass:[NSNumber class]]) {
      float val = [list[1] floatValue];
      val = (MIN(MAX(val,0.01), 100)); // clip .01 to 100
      _motionManager.gyroUpdateInterval = 1.0 / val;
    } else if ([list[0] isEqualToString:@"/getGyroFrequency"]) {
      NSArray *msgArray = @[ @"/gyroFrequency", @(_motionManager.gyroUpdateInterval)];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    } else if (list.count == 2 &&
               [list[0] isEqualToString:@"/setMotionFrequency"] &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      float val = [list[1] floatValue];
      val = (MIN(MAX(val,0.01), 100)); // clip .01 to 100
      _motionManager.deviceMotionUpdateInterval = 1.0 / val;
    } else if ([list[0] isEqualToString:@"/getMotionFrequency"]) {
      NSArray *msgArray = @[ @"/motionFrequency", @(_motionManager.deviceMotionUpdateInterval) ];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    } else if (list.count == 2 && //GPS
               [list[0] isEqualToString:@"/enableLocation"] &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      float val = [list[1] floatValue];
      if (val > 0) {
        if ([_locationManager respondsToSelector:@selector(requestWhenInUseAuthorization)]) {
          [_locationManager performSelector:@selector(requestWhenInUseAuthorization)];
        }
        [_locationManager startUpdatingLocation];
        [_locationManager startUpdatingHeading];
      }
      else {
        [_locationManager stopUpdatingLocation ];
        [_locationManager stopUpdatingHeading];
      }
    } else if (list.count == 2 &&
               [list[0] isEqualToString:@"/setDistanceFilter"] &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      float val = [list[1] floatValue];
      if (val > 0) {
        [_locationManager setDistanceFilter:val];
      } else {
        [_locationManager setDistanceFilter:kCLDistanceFilterNone];
      }
    } else if ([list[0] isEqualToString:@"/getDistanceFilter"]) {
      NSArray *msgArray = @[ @"/distanceFilter", @(_locationManager.distanceFilter) ];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    } else if ([list[0] isEqualToString:@"/getReachability"]) { //Reachability
      NSArray *msgArray = @[ @"/reachability",
                             @([_reach isReachable]? 1.0f : 0.0f),
                             [MMPViewController fetchSSIDInfo] ];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    } else if (list.count == 2 &&
               [list[0] isEqualToString:@"/setPage"] &&
               [list[1] isKindOfClass:[NSNumber class]]) {
      int pageIndex = [[list objectAtIndex:1] intValue];
      pageIndex = MIN(MAX(pageIndex,0), _pageCount-1); // clip 0 to pageCout-1;

      [_scrollView zoomToRect:CGRectMake(pageIndex * _scrollView.frame.size.width,
                                         0,
                                         _scrollView.frame.size.width,
                                         _scrollView.frame.size.height)
                     animated:YES];
    } else if ([list[0] isEqualToString:@"/getTime"] ) {
      NSDate *now = [NSDate date];
      NSDateFormatter *humanDateFormat = [[NSDateFormatter alloc] init];
      [humanDateFormat setDateFormat:@"hh:mm:ss z, d MMMM yyy"];
      NSString *humanDateString = [humanDateFormat stringFromDate:now];

      NSDateComponents *components =
      [[NSCalendar currentCalendar] components:
       NSDayCalendarUnit | NSMonthCalendarUnit | NSYearCalendarUnit |
       NSHourCalendarUnit | NSMinuteCalendarUnit | NSSecondCalendarUnit
                                      fromDate:now];
      int ms = (int)(fmod([now timeIntervalSince1970], 1) * 1000);
      NSArray *msgArray = @[ @"/timeList", @([components year]), @([components month]),
                             @([components day]), @([components hour]), @([components minute]),
                             @([components second]), @(ms)];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];

      NSArray *msgArray2 = @[ @"/timeString", humanDateString ];
      [PdBase sendList:msgArray2 toReceiver:@"fromSystem"];
    } else if ([[list objectAtIndex:0] isEqualToString:@"/getIpAddress"] ) {
      NSString *ipAddress = [MMPNetworkingUtils ipAddress]; //String, nil if not found
      if (!ipAddress) {
        ipAddress = @"none";
      }
      NSArray *msgArray = @[ @"/ipAddress", ipAddress ];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    } else if (list.count >= 2 &&
               [list[0] isEqualToString:@"/textDialog"] &&
               [list[1] isKindOfClass:[NSString class]]) {
      NSString *tag = list[1];
      NSString *title =
          [[list subarrayWithRange:NSMakeRange(2, list.count - 2)] componentsJoinedByString:@" "];
      [self showTextDialogWithTag:tag title:title];
    } else if (list.count >= 2 &&
               [list[0] isEqualToString:@"/confirmationDialog"] &&
               [list[1] isKindOfClass:[NSString class]]) {
      NSString *tag = list[1];
      NSString *title =
          [[list subarrayWithRange:NSMakeRange(2, list.count - 2)] componentsJoinedByString:@" "];
      [self showConfirmationDialogWithTag:tag title:title];
    }
  }
}

- (void)showTextDialogWithTag:(NSString *)tag title:(NSString *)title {
  UIAlertView *alert = [[UIAlertView alloc] initWithTitle:nil
                                                   message:title
                                                  delegate:self
                                         cancelButtonTitle:@"Cancel"
                                         otherButtonTitles:@"Ok", nil];
  alert.alertViewStyle = UIAlertViewStylePlainTextInput;
  // Use MMP category to capture the tag with the alert.
  [alert showWithCompletion:^(UIAlertView *alertView, NSInteger buttonIndex) {
    if (buttonIndex == 1 && [alertView textFieldAtIndex:0]) {
      NSArray *msgArray = @[ @"/textDialog", tag, [[alertView textFieldAtIndex:0] text] ];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    }
  }];
}

- (void)showConfirmationDialogWithTag:(NSString *)tag title:(NSString *)title {
  UIAlertView *alert = [[UIAlertView alloc] initWithTitle:nil
                                                  message:title
                                                 delegate:self
                                        cancelButtonTitle:@"Cancel"
                                        otherButtonTitles:@"Ok", nil];
  // Use MMP category to capture the tag with the alert.
  [alert showWithCompletion:^(UIAlertView *alertView, NSInteger buttonIndex) {
    NSArray *msgArray = @[ @"/confirmationDialog", tag, @(buttonIndex) ];
    [PdBase sendList:msgArray toReceiver:@"fromSystem"];
  }];
}

- (void)receivePrint:(NSString *)message {
  [settingsVC consolePrint:message];
}

//receive OSC message from network, format into message to send into PureData patch
- (void) receivedOSCMessage:(OSCMessage *)m	{
  NSString *address = [m address];

  NSMutableArray *msgArray = [[NSMutableArray alloc]init]; //create blank message array for sending to pd
  NSMutableArray *tempOSCValueArray = [[NSMutableArray alloc]init];

  //VV library handles receiving a value confusingly: if just one value, it has a single value in message "m" and no valueArray, if more than one value, it has valuearray. here we just shove either into a temp array to iterate over

  if ([m valueCount] == 1) {
    [tempOSCValueArray addObject:[m value]];
  } else {
    for(OSCValue *val in [m valueArray]) {
      [tempOSCValueArray addObject:val];
    }
  }

  //first element in msgArray is address
  [msgArray addObject:address];

  //then iterate over all values
  for(OSCValue *val in tempOSCValueArray) {//unpack OSC value to NSNumber or NSString
    if ([val type] == OSCValInt) {
      [msgArray addObject:[NSNumber numberWithInt:[val intValue]]];
    } else if ([val type] == OSCValFloat) {
      [msgArray addObject:[NSNumber numberWithFloat:[val floatValue]]];
    } else if ([val type] == OSCValString) {
      //libpd got _very_ unhappy when it received strings that it couldn't convert to ASCII. Have a check here and convert if needed. This occured when some device user names (coming from LANdini) had odd characters/encodings.
      if (![[val stringValue] canBeConvertedToEncoding:NSASCIIStringEncoding] ) {
        NSData *asciiData = [[val stringValue] dataUsingEncoding:NSASCIIStringEncoding allowLossyConversion:YES];
        NSString *asciiString = [[NSString alloc] initWithData:asciiData encoding:NSASCIIStringEncoding];
        [msgArray addObject:asciiString];
      } else{
        [msgArray addObject:[val stringValue]];
      }
    }
  }

  [PdBase sendList:msgArray toReceiver:@"fromNetwork"];
}

//receive shake gesture
- (void)motionEnded:(UIEventSubtype)motion withEvent:(UIEvent *)event {
  if (event.subtype == UIEventSubtypeMotionShake) {
    NSArray *msgArray = @[ @"/shake", @(1) ];
    [PdBase sendList:msgArray toReceiver:@"fromSystem"];
  }

  /*//not sure if necessary, pass it up responder chain...
   if ( [super respondsToSelector:@selector(motionEnded:withEvent:)] )
   [super motionEnded:motion withEvent:event];*/
}

- (BOOL)canBecomeFirstResponder{
  return YES;
}

- (void)connectMidiSource:(PGMidiSource *)source {
  [_connectedMidiSources addObject:source];
  [source addDelegate:self];
}

- (void)disconnectMidiSource:(PGMidiSource *)source {
  [source removeDelegate:self];
  [_connectedMidiSources removeObject:source];
}

- (void)connectMidiDestination:(PGMidiDestination *)destination {
  [_connectedMidiDestinations addObject:destination];
}

- (void)disconnectMidiDestination:(PGMidiDestination *)destination {
  [_connectedMidiDestinations removeObject:destination];
}

- (BOOL)isConnectedToConnection:(PGMidiConnection *)connection {
  return [_connectedMidiDestinations containsObject:connection] ||
         [_connectedMidiSources containsObject:connection];
}

#pragma mark - PGMidi delegate
- (void)midi:(PGMidi *)midi sourceAdded:(PGMidiSource *)source {
  [settingsVC reloadMidiSources];
}

- (void)midi:(PGMidi *)midi sourceRemoved:(PGMidiSource *)source {
  // remove if connected
  [source removeDelegate:self];
  [_connectedMidiSources removeObject:source];

  [settingsVC reloadMidiSources];
}

- (void)midi:(PGMidi *)midi destinationAdded:(PGMidiDestination *)destination {
  [settingsVC reloadMidiSources];
}

- (void)midi:(PGMidi *)midi destinationRemoved:(PGMidiDestination *)destination {
  [_connectedMidiDestinations removeObject:destination]; // remove if connected.
  [settingsVC reloadMidiSources];
}

#if TARGET_CPU_ARM
// MIDIPacket must be 4-byte aligned
#define MyMIDIPacketNext(pkt)	((MIDIPacket *)(((uintptr_t)(&(pkt)->data[(pkt)->length]) + 3) & ~3))
#else
#define MyMIDIPacketNext(pkt)	((MIDIPacket *)&(pkt)->data[(pkt)->length])
#endif

- (void)midiSource:(PGMidiSource *)midiSource midiReceived:(const MIDIPacketList *)packetList {

  const MIDIPacket *packet = &packetList->packet[0];

  for (int i = 0; i < packetList->numPackets; ++i) {
    //chop packets into messages, there could be more than one!

    int messageLength;//2 or 3
    const unsigned char* statusByte = nil;
    for(int i=0; i < packet->length; i++) { // step throguh each byte
      if (((packet->data[i] >>7) & 0x01) ==1) { // if a newstatus byte
        //send existing
        if (statusByte!=nil) {
          NSArray *obj = @[ midiSource, [NSData dataWithBytes:statusByte length:messageLength] ];
          [self performSelectorOnMainThread:@selector(parseMessageDataTuple:)
                                 withObject:obj
                              waitUntilDone:NO];
        }
        messageLength=0;
        //now point to new start
        statusByte=&packet->data[i];
      }
      messageLength++;
    }
    //send what is left
    NSArray *obj = @[ midiSource, [NSData dataWithBytes:statusByte length:messageLength] ];
    [self performSelectorOnMainThread:@selector(parseMessageDataTuple:)
                           withObject:obj
                        waitUntilDone:NO];
    packet = MyMIDIPacketNext(packet);
  }
}

// take messageData, derive the MIDI message type, and send it into PD to be picked up by PD's midi
// objects
- (void)parseMessageDataTuple:(NSArray *)sourceAndDataTuple {
  PGMidiSource *midiSource = sourceAndDataTuple[0];
  NSData *messageData = sourceAndDataTuple[1];

  Byte *bytePtr = (Byte*)([messageData bytes]);
  char type = ( bytePtr[0] >> 4) & 0x07; //remove leading 1 bit 0-7
  char channel = (bytePtr[0] & 0x0F);

  NSUInteger midiSourceIndex = [midi.sources indexOfObject:midiSource];
  if (midiSourceIndex != NSNotFound) {
    for(int i=0;i<[messageData length];i++) {
      [PdBase sendMidiByte:(int)midiSourceIndex byte:(int)bytePtr[i]];
    }
  }

  switch (type) {
    case 0://noteoff
      [PdBase sendNoteOn:(int)channel pitch:(int)bytePtr[1] velocity:0];
      break;
    case 1://noteon
      [PdBase sendNoteOn:(int)channel pitch:(int)bytePtr[1] velocity:(int)bytePtr[2]];
      break;
    case 2://poly aftertouch
      [PdBase sendPolyAftertouch:(int)channel pitch:(int)bytePtr[1] value:(int)bytePtr[2]];
      break;
    case 3://CC
      [PdBase sendControlChange:(int)channel controller:(int)bytePtr[1] value:(int)bytePtr[2]];
      break;
    case 4://prgm change
      [PdBase sendProgramChange:(int)channel value:(int)bytePtr[1]];
      break;
    case 5://aftertouch
      [PdBase sendAftertouch:(int)channel value:(int)bytePtr[1]];
      break;
    case 6: {//pitch bend - lsb, msb
      int bendValue;
      if ([messageData length]==3)
        bendValue= (bytePtr[1] | bytePtr[2]<<7) -8192;
      else //2
        bendValue=(bytePtr[1] | bytePtr[1]<<7)-8192;
      [PdBase sendPitchBend:(int)channel value:bendValue];
      break;
      }
    default:
      break;
		}
}

// receive midi from PD, out to PGMidi
- (void)receiveNoteOn:(int)pitch withVelocity:(int)velocity forChannel:(int)channel {
  const UInt8 bytes[]  = { 0x90+channel, pitch, velocity };
  [self sendToMidiDestinations:bytes size:sizeof(bytes)];
}

- (void)receiveControlChange:(int)value forController:(int)controller forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xB0+channel, controller, value };
  [self sendToMidiDestinations:bytes size:sizeof(bytes)];
}

- (void)receiveProgramChange:(int)value forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xC0+channel, value };
  [self sendToMidiDestinations:bytes size:sizeof(bytes)];
}

- (void)receivePitchBend:(int)value forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xE0+channel, (value-8192)&0x7F, ((value-8192)>>7)&0x7F };
  [self sendToMidiDestinations:bytes size:sizeof(bytes)];
}

- (void)receiveAftertouch:(int)value forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xD0+channel, value };
  [self sendToMidiDestinations:bytes size:sizeof(bytes)];
}

- (void)receivePolyAftertouch:(int)value forPitch:(int)pitch forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xA0+channel, pitch, value };
  [self sendToMidiDestinations:bytes size:sizeof(bytes)];
}

- (void)receiveMidiByte:(int)byte forPort: (int)port {
  const UInt8 bytes[]  = { byte };
  [self sendToMidiDestinations:bytes size:sizeof(bytes)];
}

// Send to all connected destinations.
- (void)sendToMidiDestinations:(const UInt8*)bytes size:(UInt32)size {
  for (PGMidiDestination *destination in _connectedMidiDestinations) {
    [destination sendBytes:bytes size:size];
  }
}

///GPS
- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation {
  ///location lat long alt horizacc vertacc, lat fine long fine
  int latRough = (int)(newLocation.coordinate.latitude * 1000);
  int longRough = (int)(newLocation.coordinate.longitude * 1000);
  int latFine = (int)fabs((fmod( newLocation.coordinate.latitude , .001)*1000000));
  int longFine = (int)fabs((fmod( newLocation.coordinate.longitude , .001)*1000000));

  //printf("\n %f %f %d %d %d %d", newLocation.coordinate.latitude, newLocation.coordinate.longitude, latRough, longRough, latFine, longFine);

  NSArray *locationArray= @[@"/location",
                            @(newLocation.coordinate.latitude),
                            @(newLocation.coordinate.longitude),
                            @(newLocation.altitude),
                            @(newLocation.horizontalAccuracy),
                            @(newLocation.verticalAccuracy),
                            @(latRough),
                            @(longRough),
                            @(latFine),
                            @(longFine) ];

  [PdBase sendList:locationArray toReceiver:@"fromSystem"];
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
  if (![CLLocationManager locationServicesEnabled]) {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Location Fail"
                          message: @"Location Services Disabled"
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
  } else if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusDenied) {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Location Fail"
                          message: @"Location Services Denied - check iOS privacy settings"
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
  } else {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Location Fail"
                          message: @"Location and/or Compass Unavailable"
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
  }
}

- (void)locationManager:(CLLocationManager *)manager didUpdateHeading:(CLHeading *)newHeading {
  NSArray *locationArray = @[ @"/compass", @(newHeading.magneticHeading) ];
  [PdBase sendList:locationArray toReceiver:@"fromSystem"];
}

- (void)didReceiveMemoryWarning{
  [super didReceiveMemoryWarning];
  // TODO use?
}

#pragma mark LANdini log delegate - currently dev only
-(void)logLANdiniOutput:(NSArray *)msgArray{
}

-(void)logMsgOutput:(NSArray *)msgArray{
}

-(void)logLANdiniInput:(NSArray *)msgArray{
  [settingsVC consolePrint:@"INPUT:"];
  for(NSString *str in msgArray) {
    [settingsVC consolePrint:str];
  }
}

-(void)logMsgInput:(NSArray *)msgArray{
  [settingsVC consolePrint:@"OUTPUT:"];
  for(NSString *str in msgArray) {
    [settingsVC consolePrint:str];
  }
}

-(void) refreshSyncServer:(NSString *)newServerName{
  [settingsVC consolePrint:[NSString stringWithFormat:@"new server:%@", newServerName]];
}

#pragma mark Reachability

-(void)reachabilityChanged:(NSNotification *)note {
  NSString *network = [MMPViewController fetchSSIDInfo];
  NSArray *msgArray = @[ @"/reachability", @([_reach isReachable] ? 1.0f : 0.0f), network == nil ? @"" : network ];
  [PdBase sendList:msgArray toReceiver:@"fromSystem"];
}

+ (NSString *)fetchSSIDInfo{
  NSArray *ifs = (__bridge id)CNCopySupportedInterfaces();
  id info = nil;
  for (NSString *ifnam in ifs) {
    info = (__bridge id)CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifnam);
    //NSLog(@"%s: %@ => %@", __func__, ifnam, info);
    NSString *ssidString = info[@"SSID"];
    if (ssidString) {
      return ssidString;
    }
  }
  return nil;
}

#pragma mark LANdini Delegate from settings
-(float)getLANdiniTime{
  return [_llm networkTime];
}

-(Reachability*)getReachability{
  return _reach;
}

-(void)setLANdiniEnabled:(BOOL)LANdiniEnabled {
  [_llm setEnabled:LANdiniEnabled]; //TODO just expose llm as property.
}

- (BOOL)LANdiniEnabled {
  return _llm.enabled;
}

#pragma mark PingAndConnect delegate from settings

-(void)setPingAndConnectEnabled:(BOOL)pingAndConnectEnabled {
  [_pacm setEnabled:pingAndConnectEnabled]; // TODO just expost pacm as property.
}

- (BOOL)pingAndConnectEnabled {
  return _pacm.enabled;
}

-(void)setPingAndConnectPlayerNumber:(NSInteger)playerNumber {
  [_pacm setPlayerNumber:playerNumber];
}

#pragma mark - from AppDelegate

//todo: this is hit even when showing lower drawer...maybe to didEnterBackground.
- (void)applicationWillResignActive {
  // audio & OSC
  if (!_backgroundAudioAndNetworkEnabled &&
     !_audiobusController.connected &&
     !_audiobusController.audiobusAppRunning) {

    _audioController.active = NO;//shut down audio processing
    [self disconnectPorts];//disconnect OSC ports on resign, to avoid conflicts
  }
  // networking
  if (!_backgroundAudioAndNetworkEnabled) {
    _pacm.enabled = NO;
    _llm.enabled = NO;
  }
}

- (void)applicationDidBecomeActive {
  // audio & OSC
  if (!_audioController.isActive) {
    _audioController.active = YES;
  }
  if (!self.isPortsConnected) {
    [self connectPorts];
  }
  // networking
  if (settingsVC.pingAndConnectEnableSwitch.isOn && !_pacm.enabled) {
    _pacm.enabled = YES;
  }
  if (settingsVC.LANdiniEnableSwitch.isOn && !_llm.enabled) {
    _llm.enabled = YES;
  }
}

- (void)audioRouteChange:(NSNotification*)notif{

  dispatch_async(dispatch_get_main_queue(), ^{
    // handle case of different supported audio rates, e.g. headphones set to 44.1, switch to 6s hardware which can only do 48K
    // TODO this clobbers intended settings, implement a per-route ledger of last preferred value.

    int newRate =  [AVAudioSession instancesRespondToSelector:@selector(sampleRate)] ?
                    [[AVAudioSession sharedInstance] sampleRate]: // ios 6+
                    [[AVAudioSession sharedInstance] currentHardwareSampleRate]; // ios 5-
    if (newRate != _samplingRate) {
      [self setRate:newRate];
    }

    if ([AVAudioSession instancesRespondToSelector:@selector(outputNumberOfChannels)]) {
      // ios >=6
      // don't care about setting channel count to 1, just handle vals >2

      if (_channelCount <= 2 && [[AVAudioSession sharedInstance] outputNumberOfChannels] > 2) {
        [self setChannelCount:[[AVAudioSession sharedInstance] outputNumberOfChannels] ];
      } else if (_channelCount > 2 && [[AVAudioSession sharedInstance] outputNumberOfChannels] <= 2) {
        [self setChannelCount:2];
      }
    }

    [settingsVC updateAudioRouteLabel];
  });
}

@end
