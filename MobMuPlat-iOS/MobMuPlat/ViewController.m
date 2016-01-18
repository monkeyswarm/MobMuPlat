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

#import "ViewController.h"

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

#import "Gui.h"
#import "PdParser.h"

extern void expr_setup(void);
extern void bonk_tilde_setup(void);
extern void choice_setup(void);
extern void fiddle_tilde_setup(void);
extern void loop_tilde_setup(void);
extern void lrshift_tilde_setup(void);
extern void sigmund_tilde_setup(void);
extern void pique_setup(void);

@implementation ViewController {
  NSMutableArray *_keyCommandsArray;
  Gui *_pdGui; //keep strong around for widgets to use (weakly).
  CGFloat _settingsButtonDim;
  CGFloat _settingsButtonOffset;
  MMPPdDispatcher *_mmpPdDispatcher;
  MMPMenuButton * _settingsButton;
}

@synthesize audioController, settingsVC;

- (NSArray *)keyCommands {
  if (!_keyCommandsArray) {
    if ([[[UIDevice currentDevice] systemVersion] intValue] <7) return nil;
    _keyCommandsArray = [NSMutableArray arrayWithObjects:
                         [UIKeyCommand keyCommandWithInput: UIKeyInputUpArrow modifierFlags: 0 action: @selector(handleKey:)],
                         [UIKeyCommand keyCommandWithInput: UIKeyInputDownArrow modifierFlags: 0 action: @selector(handleKey:)],
                         [UIKeyCommand keyCommandWithInput: UIKeyInputLeftArrow modifierFlags: 0 action: @selector(handleKey:)],
                         [UIKeyCommand keyCommandWithInput: UIKeyInputRightArrow modifierFlags: 0 action: @selector(handleKey:)],
                         [UIKeyCommand keyCommandWithInput: UIKeyInputEscape modifierFlags: 0 action: @selector(handleKey:)],
                         nil];
    // add all ascii range
    for (int charVal = 0; charVal < 128; charVal++) {
      NSString* string = [NSString stringWithFormat:@"%c" , charVal];
      [_keyCommandsArray addObject:[UIKeyCommand keyCommandWithInput: string modifierFlags: 0 action: @selector(handleKey:)]];
    }
  }
  return _keyCommandsArray;
}

- (void) handleKey: (UIKeyCommand *) keyCommand {
  int val;
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

  NSArray* msgArray=[NSArray arrayWithObjects:@"/key", [NSNumber numberWithInt:val], nil];
  [PdBase sendList:msgArray toReceiver:@"fromSystem"];
}

//what kind of device am I one? iphone 3.5", iphone 4", or ipad
+(canvasType)getCanvasType{
  canvasType hardwareCanvasType;
  if([[UIDevice currentDevice]userInterfaceIdiom]==UIUserInterfaceIdiomPhone)
  {
    if ([[UIScreen mainScreen] bounds].size.height >= 568)hardwareCanvasType=canvasTypeTallPhone;
    else hardwareCanvasType=canvasTypeWidePhone; //iphone <=4
  }
  else hardwareCanvasType=canvasTypeWideTablet;//ipad
  return hardwareCanvasType;
}

-(id)init{
  self=[super init];

  mixingEnabled = YES;

  channelCount = 2;
  samplingRate = 44100;

  openPDFile=nil;

  allGUIControl = [[NSMutableDictionary alloc]init];

  _outputIpAddress = @"224.0.0.1";
  _outputPortNumber = DEFAULT_OUTPUT_PORT_NUMBER;
  _inputPortNumber = DEFAULT_INPUT_PORT_NUMBER;

  //for using the flash
  avCaptureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];

  //MIDI library
  midi=[[PGMidi alloc]init];
  [midi setNetworkEnabled:YES];
  [midi setVirtualDestinationEnabled:YES];
  [midi.virtualDestinationSource addDelegate:self];
  //out
  [midi setVirtualEndpointName:@"MobMuPlat"];
  [midi setVirtualSourceEnabled:YES];
  //[midi.virtualSourceDestination]


  settingsVC = [[SettingsViewController alloc] initWithNibName:nil bundle:nil];
  int ticksPerBuffer;

#if TARGET_IPHONE_SIMULATOR
  ticksPerBuffer = 8;  // No other value seems to work with the simulator.
#else
  ticksPerBuffer=8;
  //was 16 - trying lower for audiobus
  //was 32 NO - means buffer is 64 blocksize * 64 ticks per buffer=4096
#endif

  //OSC setup
  manager = [[OSCManager alloc] init];
  [manager setDelegate:self];


  //libPD setup

  audioController = [[PdAudioController alloc] initWithAudioUnit:[[MobMuPlatPdAudioUnit alloc] init]] ;
  [audioController configurePlaybackWithSampleRate:samplingRate numberChannels:channelCount inputEnabled:YES mixingEnabled:mixingEnabled];
  [audioController configureTicksPerBuffer:ticksPerBuffer];
  //[audioController print];

  if (SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")) {
    // do stuff for iOS 7 and newer
    [self setupAudioBus];
  }


  //access to PD externals not normally part of libPD
  expr_setup();
  bonk_tilde_setup();
  choice_setup();
  fiddle_tilde_setup();
  loop_tilde_setup();
  lrshift_tilde_setup();
  sigmund_tilde_setup();
  pique_setup();


  //start device motion detection
  motionManager = [[CMMotionManager alloc] init];

  //start accelerometer
  NSOperationQueue *motionQueue = [[NSOperationQueue alloc] init];

  if (motionManager.accelerometerAvailable){
    [motionManager startAccelerometerUpdatesToQueue:motionQueue withHandler:^(CMAccelerometerData  *accelerometerData, NSError *error) {
      [self accelerometerDidAccelerate:accelerometerData.acceleration];
    }];
  }

  if (motionManager.deviceMotionAvailable){

    [motionManager startDeviceMotionUpdatesToQueue:motionQueue withHandler:^ (CMDeviceMotion *devMotion, NSError *error){
      CMAttitude *currentAttitude = devMotion.attitude;
      /*float xRotation = currentAttitude.roll*180/M_PI;
       float yRotation = currentAttitude.pitch*180/M_PI;
       float zRotation = currentAttitude.yaw*180/M_PI;
       printf("\n %.2f, %.2f %.2f", xRotation, yRotation, zRotation);*/
      NSArray* motionArray=[NSArray arrayWithObjects:@"/motion", [NSNumber numberWithFloat:currentAttitude.roll],[NSNumber numberWithFloat:currentAttitude.pitch], [NSNumber numberWithFloat:currentAttitude.yaw], nil];

      [PdBase sendList:motionArray toReceiver:@"fromSystem"];

    }];
  } else {
    NSLog(@"No device motion on device.");
  }

  //gyro
  if(motionManager.gyroAvailable){
    [motionManager startGyroUpdatesToQueue:motionQueue withHandler:^(CMGyroData *gyroData, NSError *error) {

      NSArray* gyroArray=[NSArray arrayWithObjects:@"/gyro", [NSNumber numberWithFloat:gyroData.rotationRate.x],[NSNumber numberWithFloat:gyroData.rotationRate.y], [NSNumber numberWithFloat:gyroData.rotationRate.z], nil];

      [PdBase sendList:gyroArray toReceiver:@"fromSystem"];
      //printf("\n %.2f, %.2f %.2f", gyroData.rotationRate.x, gyroData.rotationRate.y, gyroData.rotationRate.z);


    }];
  } else {
    NSLog(@"No gyro info on device.");
  }

  //GPS location

  locationManager = [[CLLocationManager alloc] init] ;
  locationManager.delegate = self;
  [locationManager setDistanceFilter:1.0];
  //[locationManager startUpdatingLocation ];

  //landini
  llm = [[LANdiniLANManager alloc] init];
  llm.userDelegate=settingsVC;
  //(don't enable yet)
  //dev only
  //llm.logDelegate=self;

  //ping and connect
  pacm = [[PingAndConnectManager alloc] init];
  pacm.userStateDelegate = settingsVC;

  //reachibility
  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(reachabilityChanged:)
                                               name:kReachabilityChangedNotification
                                             object:nil];
  reach = [Reachability reachabilityForLocalWiFi];
  reach.reachableOnWWAN = NO;
  [reach startNotifier];


  //copy bundle stuff if not there, i.e. first time we are running it on a new version #

  canvasType hardwareCanvasType = [ViewController getCanvasType];

  //first run on new version
  NSString *bundleVersion = [[NSBundle mainBundle] objectForInfoDictionaryKey:(NSString *)kCFBundleVersionKey];

  NSString *appFirstStartOfVersionKey = [NSString stringWithFormat:@"first_start_%@", bundleVersion];

  NSNumber *alreadyStartedOnVersion =[[NSUserDefaults standardUserDefaults] objectForKey:appFirstStartOfVersionKey];
  //printf("\n bundle %s  already started %d", [bundleVersion cString], [alreadyStartedOnVersion boolValue]);

  if(!alreadyStartedOnVersion || [alreadyStartedOnVersion boolValue] == NO) {
    NSMutableArray* defaultPatches = [NSMutableArray array];
    if(hardwareCanvasType==canvasTypeWidePhone ){
      [defaultPatches addObjectsFromArray:@[ @"MMPTutorial0-HelloSine.mmp", @"MMPTutorial1-GUI.mmp", @"MMPTutorial2-Input.mmp", @"MMPTutorial3-Hardware.mmp", @"MMPTutorial4-Networking.mmp",@"MMPTutorial5-Files.mmp",@"MMPExamples-Vocoder.mmp", @"MMPExamples-Motion.mmp", @"MMPExamples-Sequencer.mmp", @"MMPExamples-GPS.mmp", @"MMPTutorial6-2DGraphics.mmp", @"MMPExamples-LANdini.mmp", @"MMPExamples-Arp.mmp", @"MMPExamples-TableGlitch.mmp" ]];
    }
    else if (hardwareCanvasType==canvasTypeTallPhone){
      [defaultPatches addObjectsFromArray:@[ @"MMPTutorial0-HelloSine-ip5.mmp", @"MMPTutorial1-GUI-ip5.mmp", @"MMPTutorial2-Input-ip5.mmp", @"MMPTutorial3-Hardware-ip5.mmp", @"MMPTutorial4-Networking-ip5.mmp",@"MMPTutorial5-Files-ip5.mmp", @"MMPExamples-Vocoder-ip5.mmp", @"MMPExamples-Motion-ip5.mmp", @"MMPExamples-Sequencer-ip5.mmp",@"MMPExamples-GPS-ip5.mmp", @"MMPTutorial6-2DGraphics-ip5.mmp", @"MMPExamples-LANdini-ip5.mmp", @"MMPExamples-Arp-ip5.mmp",  @"MMPExamples-TableGlitch-ip5.mmp" ]];
    }
    else{//pad
      [defaultPatches addObjectsFromArray:@[ @"MMPTutorial0-HelloSine-Pad.mmp", @"MMPTutorial1-GUI-Pad.mmp", @"MMPTutorial2-Input-Pad.mmp", @"MMPTutorial3-Hardware-Pad.mmp", @"MMPTutorial4-Networking-Pad.mmp",@"MMPTutorial5-Files-Pad.mmp", @"MMPExamples-Vocoder-Pad.mmp", @"MMPExamples-Motion-Pad.mmp", @"MMPExamples-Sequencer-Pad.mmp",@"MMPExamples-GPS-Pad.mmp", @"MMPTutorial6-2DGraphics-Pad.mmp", @"MMPExamples-LANdini-Pad.mmp", @"MMPExamples-Arp-Pad.mmp",  @"MMPExamples-TableGlitch-Pad.mmp" ]];
    }

    //NOTE InterAppOSC & Ping and connect, one version.
    [defaultPatches addObjectsFromArray: @[ @"MMPTutorial0-HelloSine.pd",@"MMPTutorial1-GUI.pd", @"MMPTutorial2-Input.pd", @"MMPTutorial3-Hardware.pd", @"MMPTutorial4-Networking.pd",@"MMPTutorial5-Files.pd",@"cats1.jpg", @"cats2.jpg",@"cats3.jpg",@"clap.wav",@"Welcome.pd",  @"MMPExamples-Vocoder.pd", @"vocod_channel.pd", @"MMPExamples-Motion.pd", @"MMPExamples-Sequencer.pd", @"MMPExamples-GPS.pd", @"MMPTutorial6-2DGraphics.pd", @"MMPExamples-LANdini.pd", @"MMPExamples-Arp.pd", @"MMPExamples-TableGlitch.pd", @"anderson1.wav", @"MMPExamples-InterAppOSC.mmp", @"MMPExamples-InterAppOSC.pd", @"MMPExamples-PingAndConnect.pd", @"MMPExamples-PingAndConnect.mmp", @"MMPExamples-NativeGUI.pd" ]];

    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];
    NSString* bundlePath = [[NSBundle mainBundle] bundlePath];

    for(NSString* patchName in defaultPatches){//copy all from default and common
      NSString* patchDocPath = [publicDocumentsDir stringByAppendingPathComponent:patchName];

      NSString* patchBundlePath = [bundlePath stringByAppendingPathComponent:patchName];
      NSError* error = nil;
      if([[NSFileManager defaultManager] fileExistsAtPath:patchDocPath])
        [[NSFileManager defaultManager] removeItemAtPath:patchDocPath error:&error];
      [[NSFileManager defaultManager] copyItemAtPath:patchBundlePath toPath:patchDocPath error:&error];
    }

    [[NSUserDefaults standardUserDefaults] setObject:[NSNumber numberWithBool:YES] forKey:appFirstStartOfVersionKey];

    //Force recopy of shims on upgrade
    [MMPPdPatchDisplayUtils maybeCreatePdGuiFolderAndFiles:YES];
  }
  //end first run and copy

  return self;
}

- (void)viewDidLoad{
  [super viewDidLoad];

  // Look for new output address and port
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

  self.view.backgroundColor = [UIColor grayColor];


  //setup upper left info button, but don't add it anywhere yet
  _settingsButton = [[MMPMenuButton alloc] init];//[UIButton buttonWithType:UIButtonTypeCustom];
  [_settingsButton addTarget:self action:@selector(showInfo:) forControlEvents:UIControlEventTouchUpInside];

  _settingsButtonOffset = self.view.frame.size.width * SETTINGS_BUTTON_OFFSET_PERCENT;
  _settingsButtonDim = MAX(25, self.view.frame.size.width * SETTINGS_BUTTON_DIM_PERCENT);


  //midi setup
  midi.delegate=self;
  if([midi.sources count]>0){
    [self setMidiSourceIndex:0];//connect to first device in MIDI source list
  }
  if([midi.destinations count]>0){
    [self setMidiDestinationIndex:0];//connect to first device in MIDI source list
  }

  //delegate for file loading, etc
  settingsVC.delegate = self;
  //delegate (from audio+midi screen of settingsVC) for setting audio+midi parameters (sampling rate, MIDI source, etc)
  settingsVC.audioDelegate = self;
  settingsVC.LANdiniDelegate = self;
  settingsVC.pingAndConnectDelegate = self;

  navigationController = [[UINavigationController alloc] initWithRootViewController:settingsVC];
  navigationController.navigationBar.barStyle = UIBarStyleBlack;
  navigationController.modalTransitionStyle = UIModalTransitionStyleFlipHorizontal;


  //PD setup
  // set self as PdRecieverDelegate to recieve messages from Libpd
  [PdBase setDelegate:self];
  [PdBase setMidiDelegate:self];

  [PdBase subscribe:@"toGUI"];
  [PdBase subscribe:@"toNetwork"];
  [PdBase subscribe:@"toSystem"];

  [audioController setActive:YES];

  _pdGui = [[Gui alloc] init];
  _mmpPdDispatcher = [[MMPPdDispatcher alloc] init];
  [Widget setDispatcher:_mmpPdDispatcher];
  [PdBase setDelegate:_mmpPdDispatcher];

  _mmpPdDispatcher.printDelegate = self;

  //start default intro patch
  canvasType hardwareCanvasType = [ViewController getCanvasType];
  NSString* path;
  if(hardwareCanvasType==canvasTypeWidePhone )
    path = [[NSBundle mainBundle] pathForResource:@"Welcome" ofType:@"mmp"];
  else if (hardwareCanvasType==canvasTypeTallPhone)
    path = [[NSBundle mainBundle] pathForResource:@"Welcome-ip5" ofType:@"mmp"];
  else//pad
    path = [[NSBundle mainBundle] pathForResource:@"Welcome-Pad" ofType:@"mmp"];

  NSString* jsonString = [NSString stringWithContentsOfFile: path encoding:NSUTF8StringEncoding error:nil];

  if(jsonString){
    //[self loadScene:[jsonString objectFromJSONString]];
    NSData *data = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
    [self loadScene:[NSJSONSerialization JSONObjectWithData:data options:nil error:nil]];
  } else {
    //still put butotn
    _settingsButton.transform = CGAffineTransformMakeRotation(0);
    _settingsButton.frame = CGRectMake(_settingsButtonOffset, _settingsButtonOffset, _settingsButtonDim, _settingsButtonDim);
    [self.view addSubview:_settingsButton];
  }
}

-(void)setupAudioBus {

  //audioBus check for any audio issues restart audio if detected
  UInt32 channels;
  UInt32 size; //= sizeof(channels);
  OSStatus result = AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareInputNumberChannels, &size, &channels);

  if ( result == kAudioSessionIncompatibleCategory ) {
    // Audio session error (rdar://13022588). Power-cycle audio session.
    AudioSessionSetActive(false);
    AudioSessionSetActive(true);
    result = AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareInputNumberChannels, &size, &channels);
    if ( result != noErr ) {
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

  AudioComponentDescription acd;
  acd.componentType = kAudioUnitType_RemoteGenerator;
  acd.componentSubType = 'aout';
  acd.componentManufacturer = 'igle';

  ABSenderPort *sender = [[ABSenderPort alloc] initWithName:@"MobMuPlat Sender"
                                                      title:NSLocalizedString(@"MobMuPlat Sender", @"")
                                  audioComponentDescription:acd
                                                  audioUnit:self.audioController.audioUnit.audioUnit];
  [_audiobusController addSenderPort:sender];

  ABFilterPort *filterPort = [[ABFilterPort alloc] initWithName:@"MobMuPlat Filter"
                                                          title:@"MobMuPlat Filter"
                                      audioComponentDescription:(AudioComponentDescription) {
                                        .componentType = kAudioUnitType_RemoteEffect,
                                        .componentSubType = 'afil',
                                        .componentManufacturer = 'igle' }
                                                      audioUnit:self.audioController.audioUnit.audioUnit];
  [_audiobusController addFilterPort:filterPort];

  ABReceiverPort *receiverPort = [[ABReceiverPort alloc] initWithName:@"MobMuPlat Receiver"
                                                                title:NSLocalizedString(@"MobMuPlat Receiver", @"")];
  [_audiobusController addReceiverPort:receiverPort];

  receiverPort.clientFormat = [self.audioController.audioUnit
                               ASBDForSampleRate:samplingRate
                               numberChannels:channelCount];
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
      //[_audioEngine stop];
      [self.audioController setActive:NO];
    }
  } else {
    [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
  }
}

- (void)printAudioSessionUnitInfo {
  //audio info
  AVAudioSession *audioSession = [AVAudioSession sharedInstance];
  NSLog(@" aft Buffer size %f \n category %@ \n sample rate %f \n input latency %f \n other app playing? %d \n audiosession mode %@ \n audiosession output latency %f",audioSession.IOBufferDuration,audioSession.category,audioSession.sampleRate,audioSession.inputLatency,audioSession.isOtherAudioPlaying,audioSession.mode,audioSession.outputLatency);

  [self.audioController.audioUnit print];

}

-(BOOL)isAudioBusConnected {
  //return ABFilterPortIsConnected(self.filterPort) || ABInputPortIsConnected(self.inputPort) || ABOutputPortIsConnected(self.outputPort);
  return self.audiobusController.connected;
}


//I believe next two methods were neccessary to receive "shake" gesture
- (void)viewDidAppear:(BOOL)animated
{
  [super viewDidAppear:animated];
  [self becomeFirstResponder];
}

- (void)viewWillDisappear:(BOOL)animated {
  [self resignFirstResponder];
  [super viewWillDisappear:animated];
}

//called often by accelerometer, package accel values and send to PD
-(void)accelerometerDidAccelerate:(CMAcceleration)acceleration{
  //first, "cook" the values to get a nice tilt value without going beyond -1 to 1

  //printf("\naccel %.2f %.2f %.2f", acceleration.x, acceleration.y, acceleration.z);
  float cookedX = acceleration.x;
  float cookedY = acceleration.y;
  //cook it via Z accel to see when we have tipped it beyond 90 degrees

  if(acceleration.x>0 && acceleration.z>0) cookedX=(2-acceleration.x); //tip towards long side
  else if(acceleration.x<0 && acceleration.z>0) cookedX=(-2-acceleration.x); //tip away long side

  if(acceleration.y>0 && acceleration.z>0) cookedY=(2-acceleration.y); //tip right
  else if(acceleration.y<0 && acceleration.z>0) cookedY=(-2-acceleration.y); //tip left

  //clip
  if(cookedX<-1)cookedX=-1;
  else if(cookedX>1)cookedX=1;
  if(cookedY<-1)cookedY=-1;
  else if(cookedY>1)cookedY=1;

  //send the cooked values as "tilts", and the raw values as "accel"

  NSArray* msgArray=[NSArray arrayWithObjects:@"/tilts", [NSNumber numberWithFloat:cookedX],[NSNumber numberWithFloat:cookedY], nil];

  [PdBase sendList:msgArray toReceiver:@"fromSystem"];

  NSArray* accelArray=[NSArray arrayWithObjects:@"/accel", [NSNumber numberWithFloat:acceleration.x],[NSNumber numberWithFloat:acceleration.y], [NSNumber numberWithFloat:acceleration.z], nil];

  [PdBase sendList:accelArray toReceiver:@"fromSystem"];

}

- (void)showInfo:(id)sender {
  [self presentModalViewController:navigationController animated:YES];
}

//=====audio settings delegate methods

-(int)blockSize{
  return [PdBase getBlockSize];
}

-(int)setTicksPerBuffer:(int)newTick{ //return actual value
  [audioController configureTicksPerBuffer:newTick];
  return [audioController ticksPerBuffer];
}

-(int)setRate:(int)inRate{//return actual value
  samplingRate=inRate;
  [self.audioController configurePlaybackWithSampleRate:samplingRate numberChannels:channelCount inputEnabled:YES mixingEnabled:mixingEnabled];
  // NSLog(@"sample rate set to %d", [self.audioController sampleRate]);

  return [self.audioController sampleRate];
}

-(int)setChannelCount:(int)newChannelCount{
  channelCount = newChannelCount;
  [self.audioController configurePlaybackWithSampleRate:samplingRate numberChannels:channelCount inputEnabled:YES mixingEnabled:mixingEnabled];

  return [self.audioController numberChannels];
}

-(int)sampleRate{
  return [self.audioController sampleRate];
}

-(int)actualTicksPerBuffer{
  //NSLog(@"actual ticks is %d",[audioController ticksPerBuffer] );
  return [audioController ticksPerBuffer];
}

-(PGMidi*) midi{
  return midi;
}

-(void)connectPorts{//could probably use some error checking...
  if(_outputPortNumber>0){
    outPort = [manager createNewOutputToAddress:_outputIpAddress atPort:_outputPortNumber];
  }
  if(_inputPortNumber > 0){
    inPort = [manager createNewInputForPort:_inputPortNumber];
  }
  outPortToNetworkingModules = [manager createNewOutputToAddress:@"127.0.0.1" atPort:50506];
  inPortFromNetworkingModules = [manager createNewInputForPort:50505];
  _isPortsConnected = YES;
}

-(void)disconnectPorts{
  [manager deleteAllInputs];
  [manager deleteAllOutputs];
  _isPortsConnected = NO;
}

-(void)setOutputIpAddress:(NSString *)outputIpAddress {
  if ([_outputIpAddress isEqualToString:outputIpAddress]) return;
  _outputIpAddress = outputIpAddress;
  [manager removeOutput:outPort];
  outPort = [manager createNewOutputToAddress:_outputIpAddress atPort:_outputPortNumber];
  [[NSUserDefaults standardUserDefaults] setObject:outputIpAddress forKey:@"outputIPAddress"];
}

- (void)setOutputPortNumber:(int)outputPortNumber {
  if(_outputPortNumber == outputPortNumber) return;
  _outputPortNumber = outputPortNumber;
  [manager removeOutput:outPort];
  outPort = [manager createNewOutputToAddress:_outputIpAddress atPort:_outputPortNumber];
  [[NSUserDefaults standardUserDefaults] setObject:@(_outputPortNumber) forKey:@"outputPortNumber"];
}

- (void)setInputPortNumber:(int)inputPortNumber {
  if(_inputPortNumber == inputPortNumber) return;
  _inputPortNumber = inputPortNumber;
  [manager removeOutput:inPort];
  inPort = [manager createNewInputForPort:_inputPortNumber];
  [[NSUserDefaults standardUserDefaults] setObject:@(_inputPortNumber) forKey:@"inputPortNumber"];
}

//====settingsVC delegate methods

- (void)settingsViewControllerDidFinish:(SettingsViewController *)controller{
  [self dismissModalViewControllerAnimated:YES];
}

-(void)flipInterface{
  isFlipped = !isFlipped;
  if(isFlipped) {
    scrollView.transform = pdPatchView.transform = CGAffineTransformMakeRotation(M_PI+isLandscape*M_PI_2);
  } else {
    scrollView.transform = pdPatchView.transform = CGAffineTransformMakeRotation(isLandscape*M_PI_2);
  }
}

-(BOOL)loadScenePatchOnly:(NSString*)filename{
  if (!filename) return NO;

  [self loadSceneCommonReset];
  [_settingsButton setBarColor:[UIColor blackColor]];

  [MMPPdPatchDisplayUtils maybeCreatePdGuiFolderAndFiles:NO]; //No = don't force overwrite if there.

  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];

  NSString *fromPath = [publicDocumentsDir stringByAppendingPathComponent:filename];
  NSString *toPath = [publicDocumentsDir stringByAppendingPathComponent:@"tempPdFile"];

  NSArray *originalAtomLines = [PdParser getAtomLines:[PdParser readPatch:fromPath]];

  // Process original atom lines into a set of gui lines and a set of shimmed patch lines.
  NSArray *processedAtomLinesTuple = [MMPPdPatchDisplayUtils proccessAtomLines:originalAtomLines];
  if (!processedAtomLinesTuple) {
    return NO;
  }
  NSArray *patchAtomLines = processedAtomLinesTuple[0];
  NSArray *guiAtomLines = processedAtomLinesTuple[1];

  NSMutableString *outputString = [NSMutableString string];
  for (NSArray *line in patchAtomLines) {
    [outputString appendString:[line componentsJoinedByString:@" "]];
    [outputString appendString:@";\n"];
  }

  // Write temp file to disk.
  NSError *error;
  [outputString writeToFile:toPath atomically:YES encoding:NSASCIIStringEncoding error:&error];
  //

// Compute canvas size
  if ([originalAtomLines count] == 0 || [originalAtomLines[0] count] < 6 || ![originalAtomLines[0][1] isEqualToString:@"canvas"] ) {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Pd file not parsed"
                          message: [NSString stringWithFormat:@"Pd file %@ not readable", filename]
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
    return NO;
  }

  CGSize docCanvasSize = CGSizeMake([originalAtomLines[0][4] floatValue], [originalAtomLines[0][5] floatValue]);
  // TODO chcek for zero/bad values
  BOOL isOrientationLandscape = (docCanvasSize.width > docCanvasSize.height);
  CGSize hardwareCanvasSize;
  if (isOrientationLandscape) {
    hardwareCanvasSize = CGSizeMake([[UIScreen mainScreen] bounds].size.height,
                                    [[UIScreen mainScreen] bounds].size.width);
  } else {
    hardwareCanvasSize = CGSizeMake([[UIScreen mainScreen] bounds].size.width,
                                    [[UIScreen mainScreen] bounds].size.height);
  }
  CGFloat hardwareCanvasRatio = hardwareCanvasSize.width / hardwareCanvasSize.height;

  CGFloat canvasWidth, canvasHeight;
  CGFloat canvasRatio;

  canvasRatio = docCanvasSize.width / docCanvasSize.height;

  if (canvasRatio > hardwareCanvasRatio) {
    // The doc canvas has a wider aspect ratio than the hardware canvas;
    // It will take the width of the screen and get letterboxed on top.
    canvasWidth = hardwareCanvasSize.width ;
    canvasHeight = canvasWidth / canvasRatio;
    pdPatchView = [[UIView alloc] initWithFrame:CGRectMake(0, (hardwareCanvasSize.height - canvasHeight) / 2.0f, canvasWidth, canvasHeight)];
  } else {
    // The doc canvas has a taller aspect ratio thatn the hardware canvas;
    // It will take the height of the screen and get letterboxed on the sides.
    canvasHeight = hardwareCanvasSize.height;
    canvasWidth = canvasHeight * canvasRatio;
    pdPatchView = [[UIView alloc] initWithFrame:CGRectMake((hardwareCanvasSize.width - canvasWidth) / 2.0f, 0, canvasWidth, canvasHeight)];
  }

  pdPatchView.clipsToBounds = YES; // Keep Pd gui boxes rendered within the view.
  pdPatchView.backgroundColor = [UIColor whiteColor];
  [self.view addSubview:pdPatchView];

  if(isOrientationLandscape){//rotate
    isLandscape = YES;
    CGPoint rotatePoint = CGPointMake(hardwareCanvasSize.height / 2.0f, hardwareCanvasSize.width / 2.0f);
    pdPatchView.center = rotatePoint;
    if(isFlipped){
      pdPatchView.transform = CGAffineTransformMakeRotation(M_PI_2+M_PI);

      _settingsButton.transform = CGAffineTransformMakeRotation(M_PI_2+M_PI);
      _settingsButton.frame = CGRectMake(_settingsButtonOffset, self.view.frame.size.height - _settingsButtonOffset - _settingsButtonDim, _settingsButtonDim, _settingsButtonDim);
    }
    else {
      pdPatchView.transform = CGAffineTransformMakeRotation(M_PI_2);

      _settingsButton.frame = CGRectMake(self.view.frame.size.width-_settingsButtonDim-_settingsButtonOffset, _settingsButtonOffset, _settingsButtonDim, _settingsButtonDim);
      _settingsButton.transform = CGAffineTransformMakeRotation(M_PI_2);
    }
  } else {
    isLandscape = NO;
    if(isFlipped){
      pdPatchView.transform = CGAffineTransformMakeRotation(M_PI);

      _settingsButton.transform = CGAffineTransformMakeRotation(M_PI);
      _settingsButton.frame = CGRectMake(self.view.frame.size.width-_settingsButtonDim-_settingsButtonOffset, self.view.frame.size.height
                                        -_settingsButtonDim-_settingsButtonOffset, _settingsButtonDim, _settingsButtonDim);

    } else {
      _settingsButton.transform = CGAffineTransformMakeRotation(0);
      _settingsButton.frame = CGRectMake(_settingsButtonOffset, _settingsButtonOffset, _settingsButtonDim, _settingsButtonDim);
    }
  }
  //DEI todo update button pos/rot on flipping.

  //

  _pdGui.parentViewSize = CGSizeMake(canvasWidth, canvasHeight);//pdPatchView.frame.size;//self.view.frame.size;

  [_pdGui addWidgetsFromAtomLines:guiAtomLines]; // create widgets first

  //
  openPDFile = [PdFile openFileNamed:@"tempPdFile" path:publicDocumentsDir]; //widgets get loadbang
  if (!openPDFile) {
    return NO;
  }

  for(Widget *widget in _pdGui.widgets) {
    [widget replaceDollarZerosForGui:_pdGui fromPatch:openPDFile];
    [pdPatchView addSubview:widget];
  }
  [_pdGui reshapeWidgets];

  for(Widget *widget in _pdGui.widgets) {
    [widget sendInitValue]; // We _DO_ need to send this, since gui objects hold the initial val, not the shims.
  }

  [self.view addSubview:_settingsButton];


  return YES;
}

//

-(void)loadSceneCommonReset {
  if(scrollView) {
    [scrollView removeFromSuperview];
    scrollView = nil;
  }

  [allGUIControl removeAllObjects];

  for (Widget *widget in _pdGui.widgets) {
    [widget removeFromSuperview];
  }
  [_pdGui.widgets removeAllObjects];

  if (pdPatchView) {
    [pdPatchView removeFromSuperview];
    pdPatchView = nil;
  }

  [_mmpPdDispatcher removeAllListeners]; // Hack because dispatcher holds strong references to listeners; they (pd native gui objects) won't get deallocated otherwise.

  // (re-)Add self as dispatch recipient for MMP symbols.
  [_mmpPdDispatcher addListener:self forSource:@"toGUI"];
  [_mmpPdDispatcher addListener:self forSource:@"toNetwork"];
  [_mmpPdDispatcher addListener:self forSource:@"toSystem"];

  //if pdfile is open, close it
  if(openPDFile != nil) {
    [openPDFile closeFile];
    openPDFile = nil;
  }

  [locationManager stopUpdatingLocation];
  [locationManager stopUpdatingHeading];
}

-(BOOL)loadScene:(NSDictionary*) sceneDict{
  if(!sceneDict)return NO;

  [self loadSceneCommonReset];

  //type of canvas for device
  //canvasType hardwareCanvasType = [ViewController getCanvasType];

  //type of canvas used in the _editor_ to make the interface. If it doesn't match the above hardwareCnvasType, then we will be scaling to fit
  canvasType editorCanvasType = canvasTypeWidePhone;
  // include deprecated strings
  if([sceneDict objectForKey:@"canvasType"]){
    NSString *typeString = (NSString*)[sceneDict objectForKey:@"canvasType"];
    if([typeString isEqualToString:@"iPhone3p5Inch"] || [typeString isEqualToString:@"widePhone"])editorCanvasType=canvasTypeWidePhone;
    else if([typeString isEqualToString:@"iPhone4Inch"] || [typeString isEqualToString:@"tallPhone"])editorCanvasType=canvasTypeTallPhone;
    else if([typeString isEqualToString:@"android7Inch"] || [typeString isEqualToString:@"tallTablet"])editorCanvasType=canvasTypeTallTablet;
    else if([typeString isEqualToString:@"iPad"] || [typeString isEqualToString:@"wideTablet"])editorCanvasType=canvasTypeWideTablet;
  }

  //get two necessary layout values from the JSON file
  // page count
  pageCount = 1;
  if([sceneDict objectForKey:@"pageCount"]){
    pageCount = [[sceneDict objectForKey:@"pageCount"] intValue];
    if(pageCount<=0)pageCount=1;
  }

  //orientation and init scrollview
  BOOL isOrientationLandscape = NO;
  if([sceneDict objectForKey:@"isOrientationLandscape"]){
    isOrientationLandscape = [[sceneDict objectForKey:@"isOrientationLandscape"] boolValue];
  }

  //get layout of the scrollview that holds the GUI
  float zoomFactor=1;
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
  switch(editorCanvasType){
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
    scrollViewFrame = CGRectMake(0, (hardwareCanvasSize.height - canvasHeight) / 2.0f, canvasWidth, canvasHeight);
  } else {
    // The doc canvas has a taller aspect ratio thatn the hardware canvas;
    // It will take the height of the screen and get letterboxed on the sides.
    zoomFactor = hardwareCanvasSize.height/ docCanvasSize.height;
    canvasHeight = hardwareCanvasSize.height;
    canvasWidth = canvasHeight * canvasRatio;
    scrollViewFrame = CGRectMake((hardwareCanvasSize.width - canvasWidth) / 2.0f, 0, canvasWidth, canvasHeight);
  }


  scrollView = [[UIScrollView alloc]initWithFrame:scrollViewFrame];
  scrollInnerView = [[UIView alloc]initWithFrame:CGRectMake(0, 0, docCanvasSize.width*pageCount, docCanvasSize.height)];

  [scrollView setContentSize:scrollInnerView.frame.size];
  [scrollView addSubview:scrollInnerView];

  if(isOrientationLandscape){//rotate
    isLandscape = YES;
    CGPoint rotatePoint = CGPointMake(hardwareCanvasSize.height / 2.0f, hardwareCanvasSize.width / 2.0f);
    scrollView.center = rotatePoint;
    if(isFlipped){
      scrollView.transform = CGAffineTransformMakeRotation(M_PI_2+M_PI);
    }
    else {
      scrollView.transform = CGAffineTransformMakeRotation(M_PI_2);
    }
  } else {
    isLandscape = NO;
    if(isFlipped){
      scrollView.transform = CGAffineTransformMakeRotation(M_PI);
    }
  }

  scrollView.pagingEnabled = YES;
  scrollView.delaysContentTouches=NO;
  scrollView.maximumZoomScale = zoomFactor;
  scrollView.minimumZoomScale = zoomFactor;
  [scrollView setDelegate:self];
  [self.view addSubview:scrollView];

  //[self connectPorts];//conencts to value of currPortNumber

  //start page
  int startPageIndex = 0;
  if([sceneDict objectForKey:@"startPageIndex"]){
    startPageIndex = [[sceneDict objectForKey:@"startPageIndex"] intValue];
    //check if beyond pageCount, then set to last page
    if(startPageIndex>pageCount)startPageIndex=pageCount-1;
  }

  //bg color
  if([sceneDict objectForKey:@"backgroundColor"]) {
    scrollView.backgroundColor=[MeControl colorFromRGBArray:[sceneDict objectForKey:@"backgroundColor"]];
    [_settingsButton setBarColor:[MeControl inverseColorFromRGBArray:[sceneDict objectForKey:@"backgroundColor"]]];
  } else {
    [_settingsButton setBarColor:[UIColor whiteColor]]; //default, but shouldn't happen.
  }

  if([sceneDict objectForKey:@"menuButtonColor"]) {
    [_settingsButton setBarColor:
         [MeControl colorFromRGBAArray:[sceneDict objectForKey:@"menuButtonColor"]]];
  }

  //get array of all widgets
  NSArray* controlArray = [sceneDict objectForKey:@"gui"];
  if(!controlArray)return NO;
  //check that it is an array of NSDictionary
  if([controlArray count]>0 && ![[controlArray objectAtIndex:0] isKindOfClass:[NSDictionary class]])return NO;

  //step through each gui widget, big loop each time
  for(NSDictionary* currDict in controlArray){
    MeControl* currObject;

    //start with elements common to all widget subclasses
    //frame - if no frame is found, skip this widget
    NSArray* frameRectArray = [currDict objectForKey:@"frame"];
    if(!frameRectArray)continue;

    CGRect frame = CGRectMake([[frameRectArray objectAtIndex:0] floatValue], [[frameRectArray objectAtIndex:1] floatValue], [[frameRectArray objectAtIndex:2] floatValue], [[frameRectArray objectAtIndex:3] floatValue]);


    //widget color
    UIColor* color = [UIColor colorWithRed:1 green:1 blue:1 alpha:1];
    if([currDict objectForKey:@"color"]){
      NSArray* colorArray = [currDict objectForKey:@"color"];
      if([colorArray count]==3)//old format before translucency
        color =[MeControl colorFromRGBArray:colorArray];
      else if([colorArray count]==4)//newer format including transulcency
        color =[MeControl colorFromRGBAArray:colorArray];
    }

    //widget highlight color
    UIColor* highlightColor = [UIColor grayColor];
    if([currDict objectForKey:@"highlightColor"]){
      NSArray* highlightColorArray = [currDict objectForKey:@"highlightColor"];
      if([highlightColorArray count]==3)
        highlightColor =[MeControl colorFromRGBArray:highlightColorArray];
      else if([highlightColorArray count]==4)
        highlightColor =[MeControl colorFromRGBAArray:highlightColorArray];
    }

    //get the subclass type, and do subclass-specific stuff
    NSString* newObjectClass =[currDict objectForKey:@"class"];
    if(!newObjectClass) continue;

    if([newObjectClass isEqualToString:@"MMPSlider"]){
      currObject = [[MeSlider alloc] initWithFrame:frame];
      if([currDict objectForKey:@"isHorizontal"] ){
        if( [[currDict objectForKey:@"isHorizontal"] boolValue]==YES )[(MeSlider*)currObject setHorizontal];
      }
      if([currDict objectForKey:@"range"])  [(MeSlider*)currObject setRange:[[currDict objectForKey:@"range"] intValue]];

    }
    else if([newObjectClass isEqualToString:@"MMPKnob"]){
      currObject=[[MeKnob alloc]initWithFrame:frame];// alloc] init];
      if([currDict objectForKey:@"range"])  [(MeKnob*)currObject setRange:[[currDict objectForKey:@"range"] intValue]];
      UIColor* indicatorColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:1];
      if([currDict objectForKey:@"indicatorColor"]){
        indicatorColor = [MeControl colorFromRGBAArray:[currDict objectForKey:@"indicatorColor"]];
      }
      [(MeKnob*)currObject setIndicatorColor:indicatorColor];
    }
    else if([newObjectClass isEqualToString:@"MMPLabel"]){
      currObject = [[MeLabel alloc]initWithFrame:frame];
      if([currDict objectForKey:@"text"]) [(MeLabel*)currObject setStringValue:[currDict objectForKey:@"text"]];
      if([currDict objectForKey:@"textSize"]) [(MeLabel*)currObject setTextSize:[[currDict objectForKey:@"textSize"] intValue] ];
      if([currDict objectForKey:@"textFont"] && [currDict objectForKey:@"textFontFamily"])
        [(MeLabel*)currObject setFontFamily:[currDict objectForKey:@"textFontFamily"] fontName:[currDict objectForKey:@"textFont"] ];
      [(MeLabel*)currObject sizeToFit];

    }
    else if([newObjectClass isEqualToString:@"MMPButton"]){
      currObject = [[MeButton alloc]initWithFrame:frame];
    }
    else if([newObjectClass isEqualToString:@"MMPToggle"]){
      currObject = [[MeToggle alloc]initWithFrame:frame];
      if([currDict objectForKey:@"borderThickness"])[(MeToggle*)currObject setBorderThickness:[[currDict objectForKey:@"borderThickness"] intValue]];
    }
    else if([newObjectClass isEqualToString:@"MMPXYSlider"]){
      currObject = [[MeXYSlider alloc]initWithFrame:frame];
    }
    else if([newObjectClass isEqualToString:@"MMPGrid"]){
      currObject = [[MeGrid alloc]initWithFrame:frame];
      if([currDict objectForKey:@"mode"])[(MeGrid*)currObject setMode:[[currDict objectForKey:@"mode"] intValue]];//needs to be done before setting dim.
      if([currDict objectForKey:@"dim"]){
        NSArray* dimArray =[currDict objectForKey:@"dim"];
        [(MeGrid*)currObject setDimX: [[dimArray objectAtIndex:0]intValue] Y:[[dimArray objectAtIndex:1]intValue] ];
      }
      if([currDict objectForKey:@"cellPadding"])[(MeGrid*)currObject setCellPadding:[[currDict objectForKey:@"cellPadding"] intValue]];
      if([currDict objectForKey:@"borderThickness"])[(MeGrid*)currObject setBorderThickness:[[currDict objectForKey:@"borderThickness"] intValue]];
    }
    else if([newObjectClass isEqualToString:@"MMPPanel"]){
      currObject = [[MePanel alloc]initWithFrame:frame];
      if([currDict objectForKey:@"imagePath"]) [(MePanel*)currObject setImagePath:[currDict objectForKey:@"imagePath"] ];
      if([currDict objectForKey:@"passTouches"]) ((MePanel*)currObject).shouldPassTouches = [[currDict objectForKey:@"passTouches"] boolValue] ;
    }
    else if([newObjectClass isEqualToString:@"MMPMultiSlider"]){
      currObject = [[MeMultiSlider alloc] initWithFrame:frame];
      if([currDict objectForKey:@"range"])
        [(MeMultiSlider*)currObject setRange:[[currDict objectForKey:@"range"] intValue]];
      if([currDict objectForKey:@"outputMode"])
        ((MeMultiSlider*)currObject).outputMode = [[currDict objectForKey:@"outputMode"] integerValue];
    }
    else if([newObjectClass isEqualToString:@"MMPLCD"]){
      currObject = [[MeLCD alloc] initWithFrame:frame];
    }
    else if([newObjectClass isEqualToString:@"MMPMultiTouch"]){
      currObject = [[MeMultiTouch alloc] initWithFrame:frame];
    }
    else if([newObjectClass isEqualToString:@"MMPMenu"]){
      currObject = [[MeMenu alloc] initWithFrame:frame];
      if([currDict objectForKey:@"title"])
        [(MeMenu*)currObject setTitleString:[currDict objectForKey:@"title"] ];
    }
    else if([newObjectClass isEqualToString:@"MMPTable"]){
      currObject = [[MeTable alloc] initWithFrame:frame];
      if([currDict objectForKey:@"mode"])
        [(MeTable*)currObject setMode:[[currDict objectForKey:@"mode"] intValue]];
      if([currDict objectForKey:@"selectionColor"])
        [(MeTable*)currObject setSelectionColor:[MeControl colorFromRGBAArray:[currDict objectForKey:@"selectionColor"]]];
      /*if([currDict objectForKey:@"displayRange"])
       [(MeTable*)currObject setDisplayRange:[[currDict objectForKey:@"displayRange"] integerValue]];*/
      if([currDict objectForKey:@"displayRangeLo"])
        [(MeTable*)currObject setDisplayRangeLo:[[currDict objectForKey:@"displayRangeLo"] floatValue]];
      if([currDict objectForKey:@"displayRangeHi"])
        [(MeTable*)currObject setDisplayRangeHi:[[currDict objectForKey:@"displayRangeHi"] floatValue]];
      if([currDict objectForKey:@"displayMode"])
        [(MeTable*)currObject setDisplayMode:[[currDict objectForKey:@"displayMode"] integerValue]];
    }
    else{//unkown
      currObject = [[MeUnknown alloc] initWithFrame:frame];
      [(MeUnknown*)currObject setWarning:newObjectClass];
    }
    //end subclass-specific list

    if(currObject){//if successfully created object
      currObject.controlDelegate=self;

      if([currObject respondsToSelector:@selector(setColor:)]){//in theory all mecontrol respond to these
        [currObject setColor:color];
      }
      if([currObject respondsToSelector:@selector(setHighlightColor:)]){
        [currObject setHighlightColor:highlightColor];
      }

      //set OSC address for widget
      NSString* address = @"dummy";
      if([currDict objectForKey:@"address"])address = [currDict objectForKey:@"address"];
      [currObject setAddress:address];

      // New data structure
      NSMutableArray *addressArray = [allGUIControl objectForKey:currObject.address];
      if (!addressArray) {
        addressArray = [[NSMutableArray alloc] init];
        [allGUIControl setObject:addressArray forKey:currObject.address];
      }
      [addressArray addObject:currObject];

      [scrollInnerView addSubview:currObject];
    }

  }
  //end of big loop through widgets

  //scroll to start page, and put settings button on top
  [scrollView zoomToRect:CGRectMake(docCanvasSize.width*startPageIndex, 0, docCanvasSize.width, docCanvasSize.height) animated:NO];
  _settingsButton.transform = CGAffineTransformMakeRotation(0);
  _settingsButton.frame = CGRectMake(_settingsButtonOffset, _settingsButtonOffset, _settingsButtonDim, _settingsButtonDim);
  [scrollView addSubview:_settingsButton];

  ///===PureData patch

  if([sceneDict objectForKey:@"pdFile"]){
    NSString* filename = [sceneDict objectForKey:@"pdFile"];

    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];

    openPDFile = [PdFile openFileNamed:filename path:publicDocumentsDir];//attempt to open
    NSLog(@"open pd file %@", filename );

    if(openPDFile==nil){//failure to load the named patch
      NSLog(@"did not find named patch!" );
      UIAlertView *alert = [[UIAlertView alloc]
                            initWithTitle: @"Pd file not found"
                            message: [NSString stringWithFormat:@"Pd file %@ not found, make sure you add it to Documents in iTunes", filename]
                            delegate: nil
                            cancelButtonTitle:@"OK"
                            otherButtonTitles:nil];
      [alert show];

    } else {//success
      //refresh tables
      //TODO optimize! make an array of tables only
      for (NSArray *addressArray in [allGUIControl allValues]) {
        for(MeControl *control in addressArray){
          if ([control isKindOfClass:[MeTable class]]) {
            // use set to quash multiple loads of same table/address - not needed in app, but needed in editor.
            [(MeTable*)control loadTable];

          }
        }
      }

    }
  }
  else{//if no JSON entry found for file, say so
    openPDFile=nil;
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
  if(enabled)
    [audioController configurePlaybackWithSampleRate:samplingRate numberChannels:channelCount inputEnabled:YES mixingEnabled:mixingEnabled];

  else
    [audioController configurePlaybackWithSampleRate:samplingRate numberChannels:channelCount inputEnabled:NO mixingEnabled:mixingEnabled];

}

#pragma mark scrollview delegate
- (UIView *)viewForZoomingInScrollView:(UIScrollView *)scrollView {
  return scrollInnerView;
}

-(void)scrollViewDidEndDecelerating:(UIScrollView *)inScrollView {
  if (inScrollView==scrollView) {
    int page = inScrollView.contentOffset.x / inScrollView.frame.size.width;
    [PdBase sendList:[NSArray arrayWithObjects:@"/page", [NSNumber numberWithInt:page], nil] toReceiver:@"fromSystem"];
  }
}

#pragma mark ControlDelegate
//I want to send a message into PD patch from a gui widget
-(void)sendGUIMessageArray:(NSArray*)msgArray{
  [PdBase sendList:msgArray toReceiver:@"fromGUI"];
}

-(UIColor*)patchBackgroundColor{
  return scrollView.backgroundColor;
}

-(UIInterfaceOrientation)orientation{
  if (isLandscape) {
    if (isFlipped) {
      return UIInterfaceOrientationLandscapeLeft;
    } else {
      return UIInterfaceOrientationLandscapeRight;
    }
  } else {
    if (isFlipped) {
      return UIInterfaceOrientationPortraitUpsideDown;
    } else {
      return UIInterfaceOrientationPortrait;
    }
  }
}
//not used
/*- (void)receiveSymbol:(NSString *)symbol fromSource:(NSString *)source{
 }*/

+ (OSCMessage*) oscMessageFromList:(NSArray*)list{
  OSCMessage *msg = [OSCMessage createWithAddress:[list objectAtIndex:0]];
  for(id item in [list subarrayWithRange:NSMakeRange(1, [list count]-1)]){
    if([item isKindOfClass:[NSString class]]) [msg addString:item];
    else if([item isKindOfClass:[NSNumber class]]){
      NSNumber* itemNumber = (NSNumber*)item;
      if([MobMuPlatUtil numberIsFloat:itemNumber]) {
        [msg addFloat:[item floatValue]];
      }
      else {
        [msg addInt:[item intValue]]; //never used, right?
      }
    }
  }
  return msg;
}

//PureData has sent out a message from the patch (from a receive object, we look for messages from "toNetwork","toGUI","toSystem")
- (void)receiveList:(NSArray *)list fromSource:(NSString *)source{
  if([list count]==0){
    NSLog(@"got zero args from %@", source);
    return;//protect against bad elements that got dropped from array...
  }
  if([source isEqualToString:@"toNetwork"]){
    if ([list count]==0) return;
    if (![[list objectAtIndex:0] isKindOfClass:[NSString class]]) {
      NSLog(@"toNetwork first element is not string");
      return;
    }
    //look for LANdini - this clause looks for /send, /send/GD, /send/OGD
    // TODO protect against cases like /sendsomethingelse while in landini!!!!
    if([[list objectAtIndex:0] rangeOfString:@"/send"].location == 0) {
      if (llm.enabled|| pacm.enabled ) {
        [outPortToNetworkingModules sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:list]]];
      }  else {
        //landini /ping & connect disabled: remake message without the first 2 landini elements and send out normal port
        if([list count]>2){
          NSArray* newList = [list subarrayWithRange:NSMakeRange(2, [list count]-2)];
          [outPort sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:newList]]];
        }
      }
    }
    //other landini/P&C messages, keep passing to landini
    // DEI TODO just compare strings, no location.
    else if ([[list objectAtIndex:0] rangeOfString:@"/networkTime"].location == 0 ||
             [[list objectAtIndex:0] rangeOfString:@"/numUsers"].location == 0 ||
             [[list objectAtIndex:0] rangeOfString:@"/userNames"].location == 0 ||
             [[list objectAtIndex:0] rangeOfString:@"/myName"].location == 0 ||
      //
             [[list objectAtIndex:0] rangeOfString:@"/playerCount"].location == 0 ||
             [[list objectAtIndex:0] rangeOfString:@"/playerNumberSet"].location == 0 ||
             [[list objectAtIndex:0] rangeOfString:@"/playerIpList"].location == 0 ||
             [[list objectAtIndex:0] rangeOfString:@"/myPlayerNumber"].location == 0 ) {


      [outPortToNetworkingModules sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:list]]];
    }
    //not for landini - send out regular!
    else{
      [outPort sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:list]]];
    }
  }

  else if([source isEqualToString:@"toGUI"]){
    NSMutableArray *addressArray = [allGUIControl objectForKey:[list objectAtIndex:0]];
    for (MeControl *control in addressArray) { //addressArray can be nil, and will just skip over this...
      [control receiveList:[list subarrayWithRange:NSMakeRange(1, [list count]-1)]];
    }
  }

  else if([source isEqualToString:@"toSystem"]){
    // TODO array size checking!
    //for some reason, there is a conflict with the audio session, and sending a command to vibrate doesn't work...
    if([[list objectAtIndex:0] isEqualToString:@"/vibrate"]){
      //printf("\nvib?");
      if([list count]>1 && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]] && [[list objectAtIndex:1] floatValue]==2){
        AudioServicesPlaySystemSound(1311); //"/vibrate 2"
      }
      else{ //"/vibrate" or "vibrate 1 or non-2
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate);
      }
    }
    //camera flash
    if([[list objectAtIndex:0] isEqualToString:@"/flash"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
      float val = [[list objectAtIndex:1] floatValue];
      if ([avCaptureDevice hasTorch]) {
        [avCaptureDevice lockForConfiguration:nil];
        if(val>0)[avCaptureDevice setTorchMode:AVCaptureTorchModeOn];  // use AVCaptureTorchModeOff to turn off
        else [avCaptureDevice setTorchMode:AVCaptureTorchModeOff];
        [avCaptureDevice unlockForConfiguration];
      }
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/setAccelFrequency"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
      float val = [[list objectAtIndex:1] floatValue];
      if(val<0.01)val=0.01;//clip
      if(val>100)val=100;
      if(val>0)
        motionManager.accelerometerUpdateInterval = 1.0/val;
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/getAccelFrequency"]){
      NSArray* msgArray=[NSArray arrayWithObjects:@"/accelFrequency", [NSNumber numberWithFloat:motionManager.accelerometerUpdateInterval], nil];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/setGyroFrequency"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
      float val = [[list objectAtIndex:1] floatValue];
      if(val<0.01)val=0.01;//clip
      if(val>100)val=100;
      if(val>0)
        motionManager.gyroUpdateInterval=1.0/val;
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/getGyroFrequency"]){
      NSArray* msgArray=[NSArray arrayWithObjects:@"/gyroFrequency", [NSNumber numberWithFloat:motionManager.gyroUpdateInterval], nil];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/setMotionFrequency"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
      float val = [[list objectAtIndex:1] floatValue];
      if(val<0.01)val=0.01;//clip
      if(val>100)val=100;
      if(val>0)
        motionManager.deviceMotionUpdateInterval=1.0/val;
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/getMotionFrequency"]){
      NSArray* msgArray=[NSArray arrayWithObjects:@"/motionFrequency", [NSNumber numberWithFloat:motionManager.deviceMotionUpdateInterval], nil];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    }
    //GPS
    else if([[list objectAtIndex:0] isEqualToString:@"/enableLocation"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
      float val = [[list objectAtIndex:1] floatValue];
      //printf("\nenable location %.2f", val );
      if(val>0){
        if ([locationManager respondsToSelector:@selector(requestWhenInUseAuthorization)]) {
          [locationManager performSelector:@selector(requestWhenInUseAuthorization)];
        }
        [locationManager startUpdatingLocation];
        [locationManager startUpdatingHeading];
      }
      else{
        [locationManager stopUpdatingLocation ];
        [locationManager stopUpdatingHeading];
      }
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/setDistanceFilter"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
      float val = [[list objectAtIndex:1] floatValue];
      //printf("\ndistance filter %.2f", val );
      if(val>0)[locationManager setDistanceFilter:val];
      else [locationManager setDistanceFilter:kCLDistanceFilterNone];
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/getDistanceFilter"]){
      NSArray* msgArray=[NSArray arrayWithObjects:@"/distanceFilter", [NSNumber numberWithFloat:locationManager.distanceFilter], nil];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    }
    /*else if([[list objectAtIndex:0] isEqualToString:@"/getMotionFrequency"]){
     NSArray* msgArray=[NSArray arrayWithObjects:@"/motionFrequency", [NSNumber numberWithFloat:self.motionFrequency], nil];
     [PdBase sendList:msgArray toReceiver:@"fromSystem"];
     }*/
    /*else if([[list objectAtIndex:0] isEqualToString:@"/enableLANdini"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
     float val = [[list objectAtIndex:1] floatValue];
     if(val>0)[llm restartOSC];
     else [llm stopOSC];
     }*/
    //Reachability
    else if([[list objectAtIndex:0] isEqualToString:@"/getReachability"]){
      NSArray* msgArray=[NSArray arrayWithObjects:@"/reachability", [NSNumber numberWithFloat:[reach isReachable]? 1.0f : 0.0f ], [ViewController fetchSSIDInfo], nil];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    }
    else if([[list objectAtIndex:0] isEqualToString:@"/setPage"] && [[list objectAtIndex:1] isKindOfClass:[NSNumber class]]){
      int page = [[list objectAtIndex:1] intValue];
      if(page<0)page=0; if (page>pageCount-1)page=pageCount-1;
      //NSLog(@"setting page %d width %2f", page, scrollView.frame.size.width);
      // WHY DOES THIS BOUNCE US SOMEWHERE ELSE??? could have just been touch
      //[scrollView setContentOffset:CGPointMake(page * scrollView.frame.size.width, 0) animated:YES];
      [scrollView zoomToRect:CGRectMake(page * scrollView.frame.size.width, 0, scrollView.frame.size.width, scrollView.frame.size.height) animated:YES];
    } else if ([[list objectAtIndex:0] isEqualToString:@"/getTime"] ){
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
      NSArray *msgArray = [NSArray arrayWithObjects:@"/timeList", [NSNumber numberWithInteger:[components year]], [NSNumber numberWithInteger:[components month]], [NSNumber numberWithInteger:[components day]], [NSNumber numberWithInteger:[components hour]], [NSNumber numberWithInteger:[components minute]], [NSNumber numberWithInteger:[components second]], [NSNumber numberWithInt:ms], nil];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];

      NSArray *msgArray2 = [NSArray arrayWithObjects:@"/timeString", humanDateString, nil];
      [PdBase sendList:msgArray2 toReceiver:@"fromSystem"];
    } else if ([[list objectAtIndex:0] isEqualToString:@"/getIpAddress"] ){
      NSString *ipAddress = [MMPNetworkingUtils ipAddress];//[LANdiniLANManager getIPAddress]; //String, nil if not found
      if (!ipAddress) {
        ipAddress = @"none";
      }
      NSArray *msgArray = [NSArray arrayWithObjects:@"/ipAddress", ipAddress, nil];
      [PdBase sendList:msgArray toReceiver:@"fromSystem"];
    }
  }
}


- (void)receivePrint:(NSString *)message
{
  [settingsVC consolePrint:message];
}

//receive OSC message from network, format into message to send into PureData patch
- (void) receivedOSCMessage:(OSCMessage *)m	{
  NSString *address = [m address];

  NSMutableArray* msgArray = [[NSMutableArray alloc]init];//create blank message array for sending to pd
  NSMutableArray* tempOSCValueArray = [[NSMutableArray alloc]init];

  //VV library handles receiving a value confusingly: if just one value, it has a single value in message "m" and no valueArray, if more than one value, it has valuearray. here we just shove either into a temp array to iterate over

  if([m valueCount]==1)[tempOSCValueArray addObject:[m value]];
  else for(OSCValue *val in [m valueArray])[tempOSCValueArray addObject:val];

  //first element in msgArray is address
  [msgArray addObject:address];

  //then iterate over all values
  for(OSCValue *val in tempOSCValueArray){//unpack OSC value to NSNumber or NSString
    if([val type]==OSCValInt){
      [msgArray addObject:[NSNumber numberWithInt:[val intValue]]];
    }
    else if([val type]==OSCValFloat){
      [msgArray addObject:[NSNumber numberWithFloat:[val floatValue]]];
    }
    else if([val type]==OSCValString){
      //libpd got _very_ unhappy when it received strings that it couldn't convert to ASCII. Have a check here and convert if needed. This occured when some device user names (coming from LANdini) had odd characters/encodings.
      if ( ![[val stringValue] canBeConvertedToEncoding:NSASCIIStringEncoding] ) {
        NSData *asciiData = [[val stringValue] dataUsingEncoding:NSASCIIStringEncoding allowLossyConversion:YES];
        NSString *asciiString = [[NSString alloc] initWithData:asciiData encoding:NSASCIIStringEncoding];
        [msgArray addObject:asciiString];
      }
      else{
        [msgArray addObject:[val stringValue]];
      }
    }

  }

  [PdBase sendList:msgArray toReceiver:@"fromNetwork"];
}


//receive shake gesture
- (void)motionEnded:(UIEventSubtype)motion withEvent:(UIEvent *)event
{
  if ( event.subtype == UIEventSubtypeMotionShake ){
    NSArray* msgArray=[NSArray arrayWithObjects:@"/shake", [NSNumber numberWithInt:1], nil];

    [PdBase sendList:msgArray toReceiver:@"fromSystem"];
  }

  /*//not sure if necessary, pass it up responder chain...
   if ( [super respondsToSelector:@selector(motionEnded:withEvent:)] )
   [super motionEnded:motion withEvent:event];*/
}

- (BOOL)canBecomeFirstResponder{
  return YES;
}

-(void)setMidiSourceIndex:(int)inIndex{
  currMidiSourceIndex=inIndex;
  [currMidiSource removeDelegate:self];
  currMidiSource = [midi.sources objectAtIndex:inIndex];
  [currMidiSource addDelegate:self];
  NSLog(@"set MidiSourceIndex to %d, %@", inIndex, currMidiSource.name);
}

-(void)setMidiDestinationIndex:(int)inIndex{
  currMidiDestinationIndex = inIndex;
  //[currMidiDestination]
  currMidiDestination=[midi.destinations objectAtIndex:inIndex];
  NSLog(@"set MidiDestinationIndex to %d, %@", inIndex, currMidiDestination.name);
}

//==== pgmidi delegate methods
- (void) midi:(PGMidi*)midi sourceAdded:(PGMidiSource *)source
{
  //printf("\nmidi source added");
  [settingsVC reloadMidiSources];

}

- (void) midi:(PGMidi*)midi sourceRemoved:(PGMidiSource *)source{
  [settingsVC reloadMidiSources];
}


- (void) midi:(PGMidi*)midi destinationAdded:(PGMidiDestination *)destination{
  //NSLog(@"added %@", destination.name);
  [settingsVC reloadMidiSources];
}

- (void) midi:(PGMidi*)midi destinationRemoved:(PGMidiDestination *)destination{
  //NSLog(@"removed %@", destination.name);
  [settingsVC reloadMidiSources];
}


-(NSMutableArray*)midiSourcesArray{//of PGMIDIConnection, get name connection.name
  return midi.sources;
}

//PG midisource delegate
/*NSString *StringFromPacket(const MIDIPacket *packet)
 {
 // Note - this is not an example of MIDI parsing. I'm just dumping
 // some bytes for diagnostics.
 // See comments in PGMidiSourceDelegate for an example of how to
 // interpret the MIDIPacket structure.
 return [NSString stringWithFormat:@"  %u bytes: [%02x,%02x,%02x]",
 packet->length,
 (packet->length > 0) ? packet->data[0] : 0,
 (packet->length > 1) ? packet->data[1] : 0,
 (packet->length > 2) ? packet->data[2] : 0
 ];
 }*/


#if TARGET_CPU_ARM
// MIDIPacket must be 4-byte aligned
#define MyMIDIPacketNext(pkt)	((MIDIPacket *)(((uintptr_t)(&(pkt)->data[(pkt)->length]) + 3) & ~3))
#else
#define MyMIDIPacketNext(pkt)	((MIDIPacket *)&(pkt)->data[(pkt)->length])
#endif

- (void) midiSource:(PGMidiSource*)midi midiReceived:(const MIDIPacketList *)packetList{

  const MIDIPacket *packet = &packetList->packet[0];

  for (int i = 0; i < packetList->numPackets; ++i){
    //chop packets into messages, there could be more than one!

    int messageLength;//2 or 3
    /*Byte**/ const unsigned char* statusByte=nil;
    for(int i=0;i<packet->length;i++){//step throguh each byte, i
      if(((packet->data[i] >>7) & 0x01) ==1){//if a newstatus byte
        //send existing
        if(statusByte!=nil)[self performSelectorOnMainThread:@selector(parseMessageData:) withObject:[NSData dataWithBytes:statusByte length:messageLength] waitUntilDone:NO];
        messageLength=0;
        //now point to new start
        statusByte=&packet->data[i];
      }
      messageLength++;
    }
    //send what is left
    [self performSelectorOnMainThread:@selector(parseMessageData:) withObject:[NSData dataWithBytes:statusByte length:messageLength] waitUntilDone:NO];

    packet = MyMIDIPacketNext(packet);
  }

}

//take messageData, derive the MIDI message type, and send it into PD to be picked up by PD's midi objects
-(void)parseMessageData:(NSData*)messageData{//2 or 3 bytes


  Byte* bytePtr = ((Byte*)([messageData bytes]));
  char type = ( bytePtr[0] >> 4) & 0x07 ;//remove leading 1 bit 0-7
  char channel = (bytePtr[0] & 0x0F);

  for(int i=0;i<[messageData length];i++)
    [PdBase sendMidiByte:currMidiSourceIndex byte:(int)bytePtr[i]];


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
    case 6://pitch bend - lsb, msb
    {
      int bendValue;
      if([messageData length]==3)
        bendValue= (bytePtr[1] | bytePtr[2]<<7) -8192;
      else //2
        bendValue=(bytePtr[1] | bytePtr[1]<<7)-8192;
      [PdBase sendPitchBend:(int)channel value:bendValue];
    }
      break;

    default:
      break;
		}
}

//receive midi from PD, out to PGMidi

- (void)receiveNoteOn:(int)pitch withVelocity:(int)velocity forChannel:(int)channel {
  const UInt8 bytes[]  = { 0x90+channel, pitch, velocity };
  [currMidiDestination sendBytes:bytes size:sizeof(bytes)];
}

- (void)receiveControlChange:(int)value forController:(int)controller forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xB0+channel, controller, value };
  [currMidiDestination sendBytes:bytes size:sizeof(bytes)];
}

- (void)receiveProgramChange:(int)value forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xC0+channel, value };
  [currMidiDestination sendBytes:bytes size:sizeof(bytes)];
}

- (void)receivePitchBend:(int)value forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xE0+channel, (value-8192)&0x7F, ((value-8192)>>7)&0x7F };
  [currMidiDestination sendBytes:bytes size:sizeof(bytes)];
}

- (void)receiveAftertouch:(int)value forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xD0+channel, value };
  [currMidiDestination sendBytes:bytes size:sizeof(bytes)];
}

- (void)receivePolyAftertouch:(int)value forPitch:(int)pitch forChannel:(int)channel {
  const UInt8 bytes[]  = { 0xA0+channel, pitch, value };
  [currMidiDestination sendBytes:bytes size:sizeof(bytes)];
}

- (void)receiveMidiByte:(int)byte forPort: (int)port {
  const UInt8 shortByte = (UInt8)byte;
  [currMidiDestination sendBytes:&shortByte size:1];
}

///GPS
- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation {
  //printf("\ndidupdate! : %f %f", newLocation.coordinate.latitude, newLocation.coordinate.longitude);
  //[settingsVC consolePrint:[NSString stringWithFormat:@"\ngps %f %f\n", newLocation.coordinate.latitude, newLocation.coordinate.longitude]];

  ///location lat long alt horizacc vertacc, lat fine long fine
  int latRough = (int)( newLocation.coordinate.latitude*1000);
  int longRough = (int)( newLocation.coordinate.longitude*1000);
  int latFine = (int)fabs((fmod( newLocation.coordinate.latitude , .001)*1000000));
  int longFine = (int)fabs((fmod( newLocation.coordinate.longitude , .001)*1000000));

  //printf("\n %f %f %d %d %d %d", newLocation.coordinate.latitude, newLocation.coordinate.longitude, latRough, longRough, latFine, longFine);

  NSArray* locationArray=[NSArray arrayWithObjects:@"/location", [NSNumber numberWithFloat:newLocation.coordinate.latitude],[NSNumber numberWithFloat:newLocation.coordinate.longitude], [NSNumber numberWithFloat:newLocation.altitude], [NSNumber numberWithFloat:newLocation.horizontalAccuracy], [NSNumber numberWithFloat:newLocation.verticalAccuracy], [NSNumber numberWithInt:latRough], [NSNumber numberWithInt:longRough], [NSNumber numberWithInt:latFine], [NSNumber numberWithInt:longFine], nil];

  [PdBase sendList:locationArray toReceiver:@"fromSystem"];
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
  //printf("\nupdatefail!");
  if (![CLLocationManager locationServicesEnabled]){
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Location Fail"
                          message: @"Location Services Disabled"
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
  }



  else if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusDenied){
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Location Fail"
                          message: @"Location Services Denied - check iOS privacy settings"
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
  }

  else {
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"Location Fail"
                          message: @"Location and/or Compass Unavailable"
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
    [alert show];
  }


  /* if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorized)
   NSLog(@"location services are enabled");

   if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusNotDetermined)
   NSLog(@"about to show a dialog requesting permission");*/
}

- (void) locationManager:(CLLocationManager *)manager didUpdateHeading:(CLHeading *)newHeading {

  NSArray* locationArray=[NSArray arrayWithObjects:@"/compass", [NSNumber numberWithFloat:newHeading.magneticHeading], nil];

  [PdBase sendList:locationArray toReceiver:@"fromSystem"];
}

- (void)didReceiveMemoryWarning{
  [super didReceiveMemoryWarning];
  // Dispose of any resources that can be recreated.
}

#pragma mark LANdini log delegate - currently dev only
-(void)logLANdiniOutput:(NSArray*)msgArray{
}

-(void)logMsgOutput:(NSArray*)msgArray{
}

-(void)logLANdiniInput:(NSArray*)msgArray{
  [settingsVC consolePrint:@"INPUT:"];
  for(NSString* str in msgArray)
    [settingsVC consolePrint:str];

}

-(void)logMsgInput:(NSArray*)msgArray{
  [settingsVC consolePrint:@"OUTPUT:"];
  for(NSString* str in msgArray)
    [settingsVC consolePrint:str];

}

-(void) refreshSyncServer:(NSString*)newServerName{
  [settingsVC consolePrint:[NSString stringWithFormat:@"new server:%@", newServerName]];
}

#pragma mark Reachability

-(void)reachabilityChanged:(NSNotification*)note {
  NSString* network = [ViewController fetchSSIDInfo];
  NSArray* msgArray=[NSArray arrayWithObjects:@"/reachability", [NSNumber numberWithFloat:[reach isReachable]? 1.0f : 0.0f ], network , nil];
  [PdBase sendList:msgArray toReceiver:@"fromSystem"];
}

+ (NSString*)fetchSSIDInfo{
  NSArray *ifs = (__bridge id)CNCopySupportedInterfaces();
  //  NSLog(@"%s: Supported interfaces: %@", __func__, ifs);
  id info = nil;
  for (NSString *ifnam in ifs) {
    info = (__bridge id)CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifnam);
    NSLog(@"%s: %@ => %@", __func__, ifnam, info);

    //printf("\ninfo exists? %d count %d", info, [info count]);
    NSString* ssidString = [info objectForKey:@"SSID"];
    return ssidString;
  }
  return nil;
}

#pragma mark LANdini Delegate from settings
-(float)getLANdiniTime{
  return [llm networkTime];
}

-(Reachability*)getReachability{
  return reach;
}

-(void)enableLANdini:(BOOL)enabled{
  [llm setEnabled:enabled];
}

#pragma mark PingAndConnect delegate from settings

-(void)enablePingAndConnect:(BOOL)enabled {
  [pacm setEnabled:enabled];
}

-(void)setPingAndConnectPlayerNumber:(NSInteger)playerNumber {
  [pacm setPlayerNumber:playerNumber];
}

@end
