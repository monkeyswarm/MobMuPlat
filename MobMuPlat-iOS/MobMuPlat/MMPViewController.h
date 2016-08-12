//
//  ViewController.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/15/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreMotion/CoreMotion.h>
#import <CoreLocation/CoreLocation.h>
#import "PdAudioController.h"
#import "PdBase.h"
#import "PdFile.h"

#import "Reachability.h"

#import "SettingsViewController.h"
#import "MeControl.h"
#import "PGMidi.h"
#import "VVOSC.h"
#import "LANdiniLANManager.h"
#import "PingAndConnectManager.h"

#import "MMPPdDispatcher.h"

//@class OSCManager, OSCInPort, OSCOutPort;

#import "Audiobus.h"

@interface MMPViewController : UIViewController<PdReceiverDelegate, UIAccelerometerDelegate,  SettingsViewControllerDelegate, ControlDelegate, UIScrollViewDelegate, AudioSettingsDelegate, PGMidiDelegate, PGMidiSourceDelegate, OSCDelegateProtocol, PdMidiReceiverDelegate, CLLocationManagerDelegate, LANdiniDelegate, PingAndConnectDelegate,MMPPdDispatcherPrintDelegate>{
    
    
    UIView* scrollInnerView;
    
    NSMutableDictionary* allGUIControl;
    UIScrollView* scrollView; // MMP gui
  UIView *pdPatchView; //Native gui

    OSCManager *manager;
    OSCInPort *inPort;
    OSCOutPort *outPort;
    
    //LANdini and Ping&Connect
    OSCInPort* inPortFromNetworkingModules;
    OSCOutPort* outPortToNetworkingModules;
    
    //midi
    PGMidi *midi;
	
  BOOL mixingEnabled;
    BOOL isLandscape;
    
   
    
    PdFile *openPDFile;
    AVCaptureDevice *avCaptureDevice;//for flash
    
    //audio settings
    int samplingRate;
    BOOL inputOrMixing;
     int channelCount;
    
    UINavigationController *navigationController;
    
    //new
    CMMotionManager* motionManager;
    CLLocationManager *locationManager;
    
    LANdiniLANManager* llm;
  PingAndConnectManager *pacm;

    Reachability* reach;
  int pageCount;
  //
  @public // exposed for testing
  MMPPdDispatcher *_mmpPdDispatcher;
}

// TODO separate audiobus.
- (instancetype)initWithAudioBusEnabled:(BOOL)audioBusEnabled NS_DESIGNATED_INITIALIZER;

-(void)connectPorts;
-(void)disconnectPorts;
-(BOOL)isAudioBusConnected;
+(NSString*)fetchSSIDInfo;
+(canvasType)getCanvasType;

@property BOOL backgroundAudioAndNetworkEnabled; //clean up
@property BOOL isPortsConnected; //clean up
@property (retain) PdAudioController* audioController;
@property (retain) SettingsViewController* settingsVC;
@property (copy, nonatomic) NSString *outputIpAddress;
@property (nonatomic) int inputPortNumber;
@property (nonatomic) int outputPortNumber;

//audio bus - make private
@property (strong, nonatomic) ABAudiobusController *audiobusController; //clean up
@property (assign,nonatomic)NSInteger ticks;

// called from app delegate
- (void)applicationWillResignActive;
- (void)applicationDidBecomeActive;

//testing
- (BOOL)loadScenePatchOnlyFromBundle:(NSBundle *)bundle filename:(NSString *)filename;

@end
