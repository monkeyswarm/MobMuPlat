//
//  SettingsViewController.h
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/30/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "PGMidi.h"
#import "MeSlider.h"
#import <MediaPlayer/MediaPlayer.h>
#import "LANdiniLANManager.h"
#import "PingAndConnectManager.h"
#import "Reachability.h"

@protocol SettingsViewControllerDelegate;
@protocol AudioSettingsDelegate;
@protocol LANdiniDelegate;
@protocol PingAndConnectDelegate;

typedef NS_ENUM(NSUInteger, MMPDeviceCanvasType) {
    canvasTypeWidePhone = 0,
    canvasTypeTallPhone,
    canvasTypeWideTablet,
    canvasTypeTallTablet
};

@interface SettingsViewController : UIViewController<UITableViewDataSource, UITableViewDelegate, LANdiniUserStateDelegate, PingAndConnectUserStateDelegate, UITextFieldDelegate>{
    MMPDeviceCanvasType hardwareCanvasType;
    
    NSMutableArray *MMPFiles, *allFiles;

    NSString* consoleTextString;
    
    NSMutableArray* consoleStringQueue;
    
    //views
    
    int rateValueArray[6];
    int requestedBlockCount;
    
    MPVolumeView *audioRouteView;
    int outputChannelCount;

}

@property (nonatomic, strong) IBOutlet UIButton* consoleViewButton;
@property (nonatomic, strong) IBOutlet UIButton* documentViewButton;
@property (nonatomic, strong) IBOutlet UIButton* audioMidiViewButton;
@property (nonatomic, strong) IBOutlet UIButton* networkViewButton;

@property (nonatomic, strong) IBOutlet UIView* consoleView;
@property (nonatomic, strong) IBOutlet UIView* documentView;
@property (nonatomic, strong) IBOutlet UIView* networkView;
@property (nonatomic, strong) IBOutlet UIScrollView* audioMidiScrollView;

@property (nonatomic, strong) IBOutlet UITableView* documentsTableView;
@property (nonatomic, strong) IBOutlet UIButton* showFilesButton;
@property (nonatomic, strong) IBOutlet UIButton* flipInterfaceButton;
@property (nonatomic, strong) IBOutlet UIButton* autoLoadButton;


@property (nonatomic, strong) IBOutlet UITextView* consoleTextView;
@property (nonatomic, strong) IBOutlet UIButton* clearConsoleButton;

//audio midi
@property (nonatomic, strong) IBOutlet UIView* audioMidiContentView;
@property (nonatomic, strong) IBOutlet UITableView* midiSourceTableView;
@property (nonatomic, strong) IBOutlet UITableView* midiDestinationTableView;
@property (nonatomic, strong) IBOutlet UISegmentedControl* tickSeg;
@property (nonatomic, strong) IBOutlet UILabel* tickValueLabel;
@property (nonatomic, strong) IBOutlet UISegmentedControl* rateSeg;
@property (nonatomic, strong) IBOutlet UIButton* audioEnableButton;
@property (nonatomic, strong) IBOutlet UISwitch* audioInputSwitch;
@property (nonatomic, strong) IBOutlet UIView* audioRouteContainerView;
@property (nonatomic, strong) IBOutlet UILabel* audioRouteLabel;

//network view
@property (nonatomic, strong) IBOutlet UISegmentedControl* networkTypeSeg;
@property (nonatomic, strong) IBOutlet UIView *networkingSubView;

@property (nonatomic, strong) IBOutlet UIView *LANdiniSubView;
@property (nonatomic, strong) IBOutlet UIView *pingAndConnectSubView;
@property (nonatomic, strong) IBOutlet UIView *multiDirectConnectionSubView;

@property (nonatomic, strong) IBOutlet UITextField *ipAddressTextField;
@property (nonatomic, strong) IBOutlet UIButton *ipAddressResetButton;
@property (nonatomic, strong) IBOutlet UITextField *outputPortNumberTextField;
@property (nonatomic, strong) IBOutlet UITextField *inputPortNumberTextField;

@property (nonatomic, strong) IBOutlet UILabel* LANdiniNetworkLabel;
@property (nonatomic, strong) IBOutlet UISwitch* LANdiniEnableSwitch;
@property (nonatomic, strong) IBOutlet UILabel* LANdiniTimeLabel;
@property (nonatomic, strong) IBOutlet UITableView* LANdiniUserTableView;

@property (nonatomic, strong) IBOutlet UISwitch* pingAndConnectEnableSwitch;
@property (nonatomic, strong) IBOutlet UISegmentedControl* pingAndConnectPlayerNumberSeg;
@property (nonatomic, strong) IBOutlet UITableView* pingAndConnectUserTableView;


@property (nonatomic, weak) id <SettingsViewControllerDelegate> delegate;
@property (nonatomic, weak) id <AudioSettingsDelegate> audioDelegate;
@property (nonatomic, weak) id <LANdiniDelegate> LANdiniDelegate;
@property (nonatomic, weak) id <PingAndConnectDelegate> pingAndConnectDelegate;


-(void)reloadFileTable;
-(void)consolePrint:(NSString*)message;
-(void)reloadMidiSources;
- (void)updateAudioRouteLabel;
- (void)updateAudioState;

@end

@protocol SettingsViewControllerDelegate <NSObject>
- (void)settingsViewControllerDidFinish:(SettingsViewController *)controller;
- (BOOL)loadMMPSceneFromDocPath:(NSString *)docPath;
- (BOOL)loadScenePatchOnlyFromDocPath:(NSString*)docPath;
- (void)flipInterface:(BOOL)isFlipped;
@property(copy, nonatomic) NSString *outputIpAddress;
@property(nonatomic)int inputPortNumber;
@property(nonatomic)int outputPortNumber;

@end

@protocol AudioSettingsDelegate <NSObject>//audio+midi stuff
-(int)blockSize;
-(int)setTicksPerBuffer:(int)newTick;//returns actual ticks
-(int)actualTicksPerBuffer;
-(int)setRate:(int)inRate;
-(int)sampleRate;
-(PGMidi*) midi;

- (void)connectMidiSource:(PGMidiSource *)source;
- (void)disconnectMidiSource:(PGMidiSource *)source;
- (void)connectMidiDestination:(PGMidiDestination *)destination;
- (void)disconnectMidiDestination:(PGMidiDestination *)destination;

// Whether we are connected to a source or destination.
- (BOOL)isConnectedToConnection:(PGMidiConnection *)connection;

//-(int)setChannelCount:(int)newChannelCount;
- (void)setAudioInputEnabled:(BOOL)enabled; //for mic input vs vibration
-(BOOL)isAudioBusConnected;
@property BOOL backgroundAudioAndNetworkEnabled;

@end


@protocol LANdiniDelegate <NSObject>
@property(nonatomic) BOOL LANdiniEnabled;
-(float)getLANdiniTime;
-(Reachability*)getReachability; //move
@end


@protocol PingAndConnectDelegate <NSObject>
@property(nonatomic) BOOL pingAndConnectEnabled;
-(void)setPingAndConnectPlayerNumber:(NSInteger)playerNumber;
@end

