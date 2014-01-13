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

@protocol SettingsViewControllerDelegate;
@protocol AudioSettingsDelegate;


typedef enum{
    canvasTypeIPhone3p5Inch = 0,
    canvasTypeIPhone4Inch = 1,
    canvasTypeIPad = 2,
} canvasType;

@interface SettingsViewController : UIViewController<UITableViewDataSource, UITableViewDelegate>{
    canvasType hardwareCanvasType;
    
    UITableView* filesTableView, *midiSourceTableView, *midiDestinationTableView;
    NSArray *MMPFiles, *allFiles;
    UIButton* dspButton, *loadDocButton, *showFilesButton, *consoleButton, *clearConsoleButton;
    BOOL mmpOrAll;

    UILabel* tickValueLabel;
    NSString* consoleTextString;
    
    UIButton* audioEnableButton;
    NSMutableArray* consoleStringQueue;
    
    //views
    UIView* filesView, *consoleView, *audioMIDIView;
    UITextView *consoleTextView;
    UISegmentedControl* tickSeg;
    UISwitch* audioInputSwitch;
    
    int rateValueArray[6];
    int requestedBlockCount;
    
    MPVolumeView *myVolumeView;

}

@property (nonatomic, assign) id <SettingsViewControllerDelegate> delegate;
@property (nonatomic, assign) id <AudioSettingsDelegate> audioDelegate;

-(void)reloadFileTable;
-(void)consolePrint:(NSString*)message;
-(void)reloadMidiSources;

@end

@protocol SettingsViewControllerDelegate
- (void)settingsViewControllerDidFinish:(SettingsViewController *)controller;
- (BOOL)loadScene:(NSDictionary*)sceneDict;
- (BOOL)loadScenePatchOnly:(NSString*)filename;
- (void)setAudioInputEnabled:(BOOL)enabled; //for mic input vs vibration
@end

@protocol AudioSettingsDelegate //audio+midi stuff stuff
-(int)blockSize;
-(int)setTicksPerBuffer:(int)newTick;//returns actual ticks
-(int)actualTicksPerBuffer;
-(int)setRate:(int)inRate;
-(int)sampleRate;
-(PGMidi*) midi;
-(void)setMidiSourceIndex:(int)index;
-(void)setMidiDestinationIndex:(int)index;
@property BOOL backgroundAudioEnabled;

@end