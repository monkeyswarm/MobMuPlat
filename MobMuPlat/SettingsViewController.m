//
//  SettingsViewController.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/30/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//
//  This object creates the viewcontroller and views when you hit the "info" button on the main screen.
//  It contains 3 buttons at the bottom, to show three subviews
//  -filesView contains a table showing the documents in the Documents directory, selectable to load
//  -audioMIDIView shows some audio and DSP options and lets you select the midi source
//  -consoleView has a TextView to print out PureData console messages (including anything sent to a [print] object in the PD patch)

#import "SettingsViewController.h"
#import <QuartzCore/QuartzCore.h>
#import "ZipArchive.h"

#import <AVFoundation/AVFoundation.h>
#import "ViewController.h"

@interface SettingsViewController () {
    __weak NSArray* _LANdiniUserArray;
    NSTimer* _networkTimer;
    NSString* _LANdiniSyncServerName;
}
@end

@implementation SettingsViewController
@synthesize delegate;

//what kind of device am I one? iphone 3.5", iphone 4", or ipad
+(canvasType)getCanvasType{
    canvasType hardwareCanvasType;
    if([[UIDevice currentDevice]userInterfaceIdiom]==UIUserInterfaceIdiomPhone)
    {
        if ([[UIScreen mainScreen] bounds].size.height == 568)hardwareCanvasType=canvasTypeIPhone4Inch;
        else hardwareCanvasType=canvasTypeIPhone3p5Inch;
    }
    else hardwareCanvasType=canvasTypeIPad;
    return hardwareCanvasType;
}

//return a list of items in documents. if argument==NO, get everything, if YES, only get .mmp files
+ (NSArray *)getDocumentsOnlyMMP:(BOOL)onlyMMP{
    
    NSMutableArray *retval = [[NSMutableArray alloc]init];
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];
    NSError *error;
    NSArray *files = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:publicDocumentsDir error:&error];
    
    
    for(NSString* file in files){
        if(!onlyMMP) [retval addObject:file];//everything
        
        else if ([[file pathExtension] isEqualToString: @"mmp"]) {//just mmp
            [retval addObject:file];
        }
    }
    return retval;
}

- (void)viewDidLoad{
    [super viewDidLoad];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(checkReach)
                                                 name:UIApplicationDidBecomeActiveNotification object:nil];
    
    //ios 7 don't have it go under the nav bar
    if ([self respondsToSelector:@selector(edgesForExtendedLayout)])
        self.edgesForExtendedLayout = UIRectEdgeNone;
    
    self.view.backgroundColor=[UIColor colorWithRed:.4 green:.4 blue:.4 alpha:1];
   
  
    
    self.navigationItem.title = @"Select Document";
    UIBarButtonItem* doneButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone
                                                                                target:self
                                                                                action:@selector(done:)];
    self.navigationItem.leftBarButtonItem = doneButton;
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(reachabilityChanged:)
                                                 name:kReachabilityChangedNotification
                                               object:nil];
  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(connectionsChanged:)
                                               name:ABConnectionsChangedNotification
                                             object:nil];
  //doesn't catch it on creation, so check it now
  [self connectionsChanged:nil];
  
  
    //match default pdaudiocontroller settings
    outputChannelCount = 2;
    
    //allowed sampling rate values for use in the segmented control
    rateValueArray[0]=8000;
    rateValueArray[1]=11025;
    rateValueArray[2]=22050;
    rateValueArray[3]=32000;
    rateValueArray[4]=44100;
    rateValueArray[5]=48000;
    requestedBlockCount = 16;
    
    consoleTextString = @"";
    consoleStringQueue = [[NSMutableArray alloc]init];
    //causes a timer to constantly see if new strings are waiting to be written to the console
    [NSTimer scheduledTimerWithTimeInterval:.25 target:self selector:@selector(consolePrintFunction) userInfo:nil repeats:YES];
    
    
    MMPFiles = [SettingsViewController getDocumentsOnlyMMP:YES];
    allFiles = [SettingsViewController getDocumentsOnlyMMP:NO];
   
    hardwareCanvasType = [SettingsViewController getCanvasType];
    
    int cornerRadius;
    int buttonRadius;
    if(hardwareCanvasType==canvasTypeIPad){
        cornerRadius=20;
        buttonRadius=10;
    }
    else{
         cornerRadius=10;
        buttonRadius=5;
    }
    
    //top buttons
    [_documentViewButton addTarget:self action:@selector(showLoadDoc:) forControlEvents:UIControlEventTouchUpInside];
    _documentViewButton.layer.cornerRadius = buttonRadius;
    _documentViewButton.layer.borderWidth = 1;
    _documentViewButton.layer.borderColor = [UIColor whiteColor].CGColor;
    
    [_consoleViewButton addTarget:self action:@selector(showConsole:) forControlEvents:UIControlEventTouchUpInside];
    _consoleViewButton.layer.cornerRadius = buttonRadius;
    _consoleViewButton.layer.borderWidth = 1;
    _consoleViewButton.layer.borderColor = [UIColor whiteColor].CGColor;
    
    [_audioMidiViewButton addTarget:self action:@selector(showDSP:) forControlEvents:UIControlEventTouchUpInside];
    _audioMidiViewButton.layer.cornerRadius = buttonRadius;
    _audioMidiViewButton.layer.borderWidth = 1;
    _audioMidiViewButton.layer.borderColor = [UIColor whiteColor].CGColor;
    
    [_LANdiniViewButton addTarget:self action:@selector(showLANdini:) forControlEvents:UIControlEventTouchUpInside];
    _LANdiniViewButton.layer.cornerRadius = buttonRadius;
    _LANdiniViewButton.layer.borderWidth = 1;
    _LANdiniViewButton.layer.borderColor = [UIColor whiteColor].CGColor;
    
    //documents
    _documentsTableView.delegate = self;
    _documentsTableView.dataSource = self;
    [_showFilesButton addTarget:self action:@selector(showFilesButtonHit:) forControlEvents:UIControlEventTouchUpInside];
    _showFilesButton.layer.cornerRadius = buttonRadius;
    _showFilesButton.layer.borderWidth = 1;
    _showFilesButton.layer.borderColor = [UIColor whiteColor].CGColor;
  _showFilesButton.titleLabel.adjustsFontSizeToFitWidth = YES;
  
    [_flipInterfaceButton addTarget:self action:@selector(flipInterfaceButtonHit:) forControlEvents:UIControlEventTouchUpInside];
    _flipInterfaceButton.layer.cornerRadius = buttonRadius;
    _flipInterfaceButton.layer.borderWidth = 1;
    _flipInterfaceButton.layer.borderColor = [UIColor whiteColor].CGColor;
  
    //console
    [_clearConsoleButton addTarget:self action:@selector(clearConsole:) forControlEvents:UIControlEventTouchUpInside];
    _clearConsoleButton.layer.cornerRadius = buttonRadius;
    _clearConsoleButton.layer.borderWidth = 1;
    _clearConsoleButton.layer.borderColor = [UIColor whiteColor].CGColor;
    
    //audio midi
    _midiSourceTableView.delegate = self;
    _midiSourceTableView.dataSource = self;
    _midiDestinationTableView.delegate = self;
    _midiDestinationTableView.dataSource = self;
    
    NSIndexPath *topIndexPath = [NSIndexPath indexPathForRow:0 inSection: 0];
    if( [[[self.audioDelegate midi] sources] count] >0 )
        [_midiSourceTableView selectRowAtIndexPath:topIndexPath animated:NO scrollPosition:UITableViewScrollPositionTop];
    if( [[[self.audioDelegate midi] destinations] count] >0 )
        [_midiDestinationTableView selectRowAtIndexPath:topIndexPath animated:NO scrollPosition:UITableViewScrollPositionTop];
    
    [_audioMidiScrollView setContentSize:_audioMidiContentView.frame.size];
    
    int actualTicks = [self.audioDelegate actualTicksPerBuffer];
    _tickSeg.selectedSegmentIndex = (int)log2(actualTicks);
    [_tickSeg addTarget:self action:@selector(tickSegChanged:) forControlEvents:UIControlEventValueChanged];
    [_rateSeg addTarget:self action:@selector(rateSegChanged:) forControlEvents:UIControlEventValueChanged];
    [self tickSegChanged:_tickSeg];//set label
    
    [_audioEnableButton addTarget:self action:@selector(audioEnableButtonHit ) forControlEvents:UIControlEventTouchDown];
    _audioEnableButton.layer.cornerRadius = 5;
    _audioEnableButton.layer.borderWidth = 1;
    _audioEnableButton.layer.borderColor = [UIColor whiteColor].CGColor;
	[_audioInputSwitch addTarget:self action:@selector(audioInputSwitchHit) forControlEvents:UIControlEventValueChanged];
    
    audioRouteView =  [[MPVolumeView alloc] initWithFrame:_audioRouteContainerView.frame];
    audioRouteView.showsRouteButton = YES;
    audioRouteView.showsVolumeSlider = NO;
    [_audioMidiContentView addSubview:audioRouteView];
    [audioRouteView sizeToFit];
    
    //LANdini
    [_LANdiniEnableSwitch addTarget:self action:@selector(LANdiniSwitchHit:) forControlEvents:UIControlEventValueChanged];
    _LANdiniUserTableView.delegate = self;
    _LANdiniUserTableView.dataSource = self;
    
    
    
   
    //
    _documentView.layer.cornerRadius = cornerRadius;
    _consoleView.layer.cornerRadius = cornerRadius;
    _audioMidiScrollView.layer.cornerRadius = cornerRadius;
    _LANdiniView.layer.cornerRadius = cornerRadius;
    
    _documentsTableView.layer.cornerRadius = cornerRadius;
    _consoleTextView.layer.cornerRadius = cornerRadius;
    
    _midiSourceTableView.layer.cornerRadius = cornerRadius;
    _midiDestinationTableView.layer.cornerRadius = cornerRadius;
    _LANdiniUserTableView.layer.cornerRadius = cornerRadius;

    if(hardwareCanvasType==canvasTypeIPhone3p5Inch){
        if(SYSTEM_VERSION_LESS_THAN(@"7.0")){
            //segmented
            UIFont *font = [UIFont boldSystemFontOfSize:12.0f];
            NSDictionary *attributes = [NSDictionary dictionaryWithObject:font forKey:UITextAttributeFont];
            [_tickSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
            [_rateSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
            
            CGRect frame= _tickSeg.frame;
            [_tickSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, 30)];
            frame= _rateSeg.frame;
            [_rateSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, 30)];
        }
    }
    else if(hardwareCanvasType==canvasTypeIPhone4Inch){
        if(SYSTEM_VERSION_LESS_THAN(@"7.0")){
            //segmented
            UIFont *font = [UIFont boldSystemFontOfSize:12.0f];
            NSDictionary *attributes = [NSDictionary dictionaryWithObject:font forKey:UITextAttributeFont];
            [_tickSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
            [_rateSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
            
            CGRect frame= _tickSeg.frame;
            [_tickSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, 30)];
            frame= _rateSeg.frame;
            [_rateSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, 30)];
        }
    }
    else{//ipad
        
        UIFont *font = [UIFont boldSystemFontOfSize:24.0f];
        NSDictionary *attributes = [NSDictionary dictionaryWithObject:font forKey:UITextAttributeFont];
        [_tickSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
        [_rateSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
        
        if(SYSTEM_VERSION_LESS_THAN(@"7.0")){
            //segmented
             CGRect frame= _tickSeg.frame;
            [_tickSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, 60)];
            frame= _rateSeg.frame;
            [_rateSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, 60)];
        }
        else{//ios 7
            CGRect frame= _tickSeg.frame;
            [_tickSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, frame.size.height*2)];
            frame= _rateSeg.frame;
            [_rateSeg setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.width, frame.size.height*2)];
        }
    }
    
    [self showLoadDoc:nil];
    [self updateAudioRouteLabel];
    

    if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"6.0")){
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(audioRouteChange:) name:AVAudioSessionRouteChangeNotification object:nil];
    }
}

-(void)viewDidAppear:(BOOL)animated{
    [super viewDidAppear:animated];
    [self checkReach];
    
}
-(void)checkReach{
    [self updateNetworkLabel:[self.LANdiniDelegate getReachability] ];
}


- (void)audioRouteChange:(NSNotification*)notif{
    
    if(outputChannelCount<=2 && [[AVAudioSession sharedInstance] outputNumberOfChannels]>2){
        if ([self.audioDelegate respondsToSelector:@selector(setChannelCount:)]) {
            [self.audioDelegate setChannelCount:[[AVAudioSession sharedInstance] outputNumberOfChannels] ];
        }
    }
    else if(outputChannelCount>2 && [[AVAudioSession sharedInstance] outputNumberOfChannels]<=2) {
        if ([self.audioDelegate respondsToSelector:@selector(setChannelCount:)]) {
            [self.audioDelegate setChannelCount:2];
        }
    }
    
    [self updateAudioRouteLabel];//also prints to console
}

-(void)updateAudioRouteLabel{
    if([[AVAudioSession sharedInstance] respondsToSelector:@selector(currentRoute)]){//ios 5 doesn't find selector
        
        AVAudioSessionRouteDescription* asrd = [[AVAudioSession sharedInstance] currentRoute];
        NSString* inputString = @"input:(none)";
        if([[asrd inputs] count] > 0 ){
            AVAudioSessionPortDescription* aspd = [[asrd inputs] objectAtIndex:0];
            inputString = [NSString stringWithFormat:@"input:%@ channels:%d", aspd.portName, [[AVAudioSession sharedInstance] inputNumberOfChannels] ];
        }
        NSString* outputString = @"output:(none)";
        if([[asrd outputs] count] > 0 ){
            AVAudioSessionPortDescription* aspd = [[asrd outputs] objectAtIndex:0];
            outputString = [NSString stringWithFormat:@"output:%@ channels:%d", aspd.portName, [[AVAudioSession sharedInstance] outputNumberOfChannels] ];
        }
        _audioRouteLabel.text = [NSString stringWithFormat:@"%@\n%@", inputString, outputString];
        //[self consolePrint:[NSString stringWithFormat:@"%@\n%@", inputString, outputString] ];
    }
    
    
}

-(void)viewWillAppear:(BOOL)animated{
    _consoleTextView.text = consoleTextString;
    //ios 7 bug
    if(hardwareCanvasType==canvasTypeIPhone3p5Inch || hardwareCanvasType==canvasTypeIPhone4Inch)
        [_consoleTextView setFont:[UIFont systemFontOfSize:16]];
    else [_consoleTextView setFont:[UIFont systemFontOfSize:24]];
    
    [_consoleTextView scrollRangeToVisible:(NSRange){consoleTextString.length-1, 1}];
    

}

-(void)refreshAudioEnableButton{
  if(self.audioDelegate.backgroundAudioEnabled){
    [_audioEnableButton setTitle:@"enabled" forState:UIControlStateNormal];
    [_audioEnableButton setBackgroundColor:[UIColor whiteColor]];
    [_audioEnableButton setTitleColor:[UIColor orangeColor] forState:UIControlStateNormal];
  }
  
  else{
    [_audioEnableButton setTitle:@"disabled" forState:UIControlStateNormal];
    [_audioEnableButton setBackgroundColor:[UIColor clearColor]];
    [_audioEnableButton setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
  }
}

-(void)audioEnableButtonHit{
    self.audioDelegate.backgroundAudioEnabled=!self.audioDelegate.backgroundAudioEnabled;
  [self refreshAudioEnableButton];
}

BOOL audioSwitchBool;
-(void)audioInputSwitchHit{
    
    if(audioSwitchBool!=_audioInputSwitch.on){
        audioSwitchBool=_audioInputSwitch.on;
        
        if(_audioInputSwitch.on){
            [self.audioDelegate setAudioInputEnabled:NO];//overide to turn mic off, vib on;
        }
        else [self.audioDelegate setAudioInputEnabled:YES];
    }
}

BOOL LANdiniSwitchBool;
-(void)LANdiniSwitchHit:(UISwitch*)sender{
    if(LANdiniSwitchBool!=_LANdiniEnableSwitch.on){
        LANdiniSwitchBool=_LANdiniEnableSwitch.on;
        if([self.LANdiniDelegate respondsToSelector:@selector(enableLANdini:)]){
            [self.LANdiniDelegate enableLANdini:[sender isOn]];
        }
    
        if([sender isOn]){
            _networkTimer = [NSTimer scheduledTimerWithTimeInterval:.25 target:self selector:@selector(networkTime:) userInfo:nil repeats:YES];
        }
        else{
            [_networkTimer invalidate];
            _networkTimer = nil;
            _LANdiniTimeLabel.text = @"Network time:";
        }
    }
}

-(void)networkTime:(NSTimer*)timer{
    _LANdiniTimeLabel.text = [NSString stringWithFormat:@"Network time via %@:\n%.2f", _LANdiniSyncServerName, [self.LANdiniDelegate getLANdiniTime] ];
}


- (void)done:(id)sender {
    [self.delegate settingsViewControllerDidFinish:self];
}


-(void)showFilesButtonHit:(id)sender{
    if([_showFilesButton tag]==0){//is showing mmp, change to show all
        mmpOrAll=YES;
        [self reloadFileTable];
        [_showFilesButton setTitle:@"show only mmp files" forState:UIControlStateNormal];
      _showFilesButton.backgroundColor = [UIColor whiteColor];
      [_showFilesButton setTitleColor:[UIColor purpleColor] forState:UIControlStateNormal];
        _showFilesButton.tag=1;
    }
    else{
        mmpOrAll=NO;
        [self reloadFileTable];
        [_showFilesButton setTitle:@"show all files" forState:UIControlStateNormal];
      _showFilesButton.backgroundColor = [UIColor purpleColor];
      [_showFilesButton setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
        _showFilesButton.tag=0;
    }
}

-(void)flipInterfaceButtonHit:(id)sender{
  //self.view.transform = CGAffineTransformMakeRotation(M_PI);
  [self.delegate flipInterface];
  if([_flipInterfaceButton tag]==0){//is showing mmp, change to show all
    [_flipInterfaceButton setTitle:@"unflip interface" forState:UIControlStateNormal];
    _flipInterfaceButton.backgroundColor = [UIColor whiteColor];
    [_flipInterfaceButton setTitleColor:[UIColor purpleColor] forState:UIControlStateNormal];
    _flipInterfaceButton.tag=1;
  }
  else{
    
    [_flipInterfaceButton setTitle:@"flip interface" forState:UIControlStateNormal];
    _flipInterfaceButton.backgroundColor = [UIColor purpleColor];
    [_flipInterfaceButton setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    _flipInterfaceButton.tag=0;
  }
}

-(void) showLoadDoc:(id)sender{
    _documentViewButton.enabled=NO;
    _audioMidiViewButton.enabled=YES;
    _consoleViewButton.enabled=YES;
    _LANdiniViewButton.enabled = YES;
    [self.view bringSubviewToFront:_documentView];
    self.navigationItem.title = @"Select Document";
    
}
-(void)showConsole:(id)sender{
    _documentViewButton.enabled=YES;
    _audioMidiViewButton.enabled=YES;
    _consoleViewButton.enabled=NO;
    _LANdiniViewButton.enabled = YES;
    [self.view bringSubviewToFront:_consoleView];
    self.navigationItem.title = @"Pd Console";

}

- (void)showDSP:(id)sender {
    _documentViewButton.enabled=YES;
    _audioMidiViewButton.enabled=NO;
    _consoleViewButton.enabled=YES;
    _LANdiniViewButton.enabled = YES;
    [self.view bringSubviewToFront:_audioMidiScrollView];
    self.navigationItem.title = @"Audio MIDI Settings";
    
}

- (void)showLANdini:(id)sender {
    _documentViewButton.enabled=YES;
    _audioMidiViewButton.enabled=YES;
    _consoleViewButton.enabled=YES;
    _LANdiniViewButton.enabled = NO;
    [self.view bringSubviewToFront:_LANdiniView];
    self.navigationItem.title = @"LANdini";
    
}

-(void)clearConsole:(id)sender{
    consoleTextString=@"";
    _consoleTextView.text = consoleTextString;
}

//adds string to queue
-(void)consolePrint:(NSString *)message{
    [consoleStringQueue addObject:message];
}

//called often by timer
-(void)consolePrintFunction{
    
    if([consoleStringQueue count]==0)return;//nothing to print

    //take all the string in the queue and shove them into one big string
    NSString* newString = [consoleStringQueue componentsJoinedByString:@"\n"];
    consoleTextString = [consoleTextString stringByAppendingFormat:@"\n%@", newString];//append to currently shown string
    int startPoint = [consoleTextString length]-2000; if (startPoint<0)startPoint=0;
    NSRange stringRange = {startPoint, MIN([consoleTextString length], 2000)};//chop off front of string to fit
    consoleTextString = [consoleTextString substringWithRange:stringRange];
    [consoleStringQueue removeAllObjects];
    
    if (self.isViewLoaded && self.view.window) {//if I am on screen, show and scroll
        _consoleTextView.text = consoleTextString;
        [_consoleTextView scrollRangeToVisible:(NSRange){consoleTextString.length-1, 1}];
        
        //ios 7 bug, font needs to be set after setting text
        if(hardwareCanvasType==canvasTypeIPhone3p5Inch || hardwareCanvasType==canvasTypeIPhone4Inch)
            [_consoleTextView setFont:[UIFont systemFontOfSize:16]];
        else [_consoleTextView setFont:[UIFont systemFontOfSize:24]];
            
    }
  
}

-(void)tickSegChanged:(UISegmentedControl*)sender{
    int index = [sender selectedSegmentIndex];
    requestedBlockCount = (int)pow(2, index);
    int blockSize = [self.audioDelegate blockSize];
    
    
    int actualTicks = [self.audioDelegate setTicksPerBuffer:requestedBlockCount];
    [_tickValueLabel setText:[NSString stringWithFormat:@"request: %d * block size (%d) = %d samples \nactual: %d * block size (%d) = %d samples", requestedBlockCount, blockSize, requestedBlockCount*blockSize, actualTicks, blockSize, actualTicks*blockSize  ]];
    
    if(actualTicks!=requestedBlockCount){
        int actualIndex = (int)log2(actualTicks);
        sender.selectedSegmentIndex=actualIndex;
    }
}

-(void)rateSegChanged:(UISegmentedControl*)sender{
    int index = [sender selectedSegmentIndex];
    int newRate = rateValueArray[index];
    int actualRate = [self.audioDelegate setRate:newRate];
    int actualTicks = [self.audioDelegate actualTicksPerBuffer];
    int blockSize = [self.audioDelegate blockSize];
    
    if (requestedBlockCount!=actualTicks) {
        actualTicks = [self.audioDelegate setTicksPerBuffer:requestedBlockCount];//redundant?
        if( fmod(log2(actualTicks), 1)==0){
            int newBlockIndex = (int)log2(actualTicks);
            [_tickSeg setSelectedSegmentIndex:newBlockIndex];
        }
        else [_tickSeg setSelectedSegmentIndex:UISegmentedControlNoSegment];
        
    }
    if(newRate!=actualRate){
      [_rateSeg setSelectedSegmentIndex:UISegmentedControlNoSegment];
      for(int i=0;i<6;i++){
        if(rateValueArray[i]==actualRate) [_rateSeg setSelectedSegmentIndex:i];
      }
    }
    
    [_tickValueLabel setText:[NSString stringWithFormat:@"request: %d * block size (%d) = %d samples \nactual: %d * block size (%d) = %d samples", requestedBlockCount, blockSize, requestedBlockCount*blockSize, actualTicks, blockSize, actualTicks*blockSize  ]];
}


-(void)reloadFileTable{
    MMPFiles = [SettingsViewController getDocumentsOnlyMMP:YES];
    allFiles = [SettingsViewController getDocumentsOnlyMMP:NO];
    [_documentsTableView reloadData];
}

-(void)reloadMidiSources{
    [_midiSourceTableView reloadData];
    [_midiDestinationTableView reloadData];
}

//landini


//load a pure data file from an index path on the filesTable
-(void)selectHelper:(NSIndexPath*)indexPath{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];
    
    //pull filename from either allFiles or MMPFiles, depending on which list we are looking at
    NSString* filename = [(mmpOrAll ? allFiles : MMPFiles)objectAtIndex:[indexPath row]];
    NSString* fullPath = [publicDocumentsDir stringByAppendingPathComponent:filename];
    NSString* suffix = [[filename componentsSeparatedByString: @"."] lastObject];
    
    //if an MMP file, open JSONString and load it 
    if([suffix isEqualToString:@"mmp"]){
        NSString* jsonString = [NSString stringWithContentsOfFile:fullPath encoding:NSUTF8StringEncoding error:nil];
        //NSDictionary* sceneDict = [jsonString JSONValue];
        NSData *data = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
        NSDictionary* sceneDict = [NSJSONSerialization JSONObjectWithData:data options:nil error:nil];
        BOOL loaded = [self.delegate loadScene:sceneDict];
        if(loaded)[self.delegate settingsViewControllerDidFinish:self];//successful load, flip back to main ViewController
        else{//failed load
            UIAlertView *alert = [[UIAlertView alloc]
                                  initWithTitle: @"Bad format"
                                  message: @"This .mmp file is not formatted correctly"
                                  delegate: nil
                                  cancelButtonTitle:@"OK"
                                  otherButtonTitles:nil];
            [alert show];
        }
    }
    
    //zip file, attempt to unarchive and copy contents into documents folder
    else if ([suffix isEqualToString:@"zip"]){
        ZipArchive* za = [[ZipArchive alloc] init];
        
        if( [za UnzipOpenFile:fullPath] ) {
            if( [za UnzipFileTo:publicDocumentsDir overWrite:YES] != NO ) {
                UIAlertView *alert = [[UIAlertView alloc]
                                      initWithTitle: @"Archive Decompressed"
                                      message: [NSString stringWithFormat:@"Decompressed contents of %@ to MobMuPlat Documents", filename]
                                      delegate: nil
                                      cancelButtonTitle:@"OK"
                                      otherButtonTitles:nil];
                [alert show];
                NSError* error;
                [[NSFileManager defaultManager]removeItemAtPath:fullPath error:&error];
                [self reloadFileTable];
            }
            else{
                UIAlertView *alert = [[UIAlertView alloc]
                                      initWithTitle: @"Archive Failure"
                                      message: [NSString stringWithFormat:@"Could not decompress contents of %@", filename]
                                      delegate: nil
                                      cancelButtonTitle:@"OK"
                                      otherButtonTitles:nil];
                [alert show];
            }
            
            [za UnzipCloseFile];
        }
    }
    
    //pd file, load the file via "loadScenePatchOnly"
    else if ([suffix isEqualToString:@"pd"]){
        BOOL loaded = [self.delegate loadScenePatchOnly:filename];
        if(loaded)[self.delegate settingsViewControllerDidFinish:self];
        else{//not sure why I commented this out...
            /*UIAlertView *alert = [[UIAlertView alloc]
                                  initWithTitle: @"Bad PD format"
                                  message: @"Could not open PD file"
                                  delegate: nil
                                  cancelButtonTitle:@"OK"
                                  otherButtonTitles:nil];
            [alert show];*/
        }
    }
}

//tableView delegate methods
- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {

    if(tableView==_documentsTableView){
        UITableViewCell* cell = [tableView cellForRowAtIndexPath:indexPath];
        
        //add an activity indicator
        UIActivityIndicatorView* aiv = [[UIActivityIndicatorView alloc]initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
        aiv.frame = CGRectMake(0, 0, 24, 24);
        [cell setAccessoryView:aiv];
        [aiv startAnimating];
        
        //load the pd file
        [self performSelector:@selector(selectHelper:) withObject:indexPath afterDelay:0];
       
        //done
        [aiv performSelector:@selector(stopAnimating) withObject:nil afterDelay:0];//performSelector: puts method call on next run loop
        
        
    }
    
    else if (tableView==_midiSourceTableView){
        [self.audioDelegate setMidiSourceIndex:[indexPath indexAtPosition:1] ];
	}
    else if (tableView==_midiDestinationTableView){
        [self.audioDelegate setMidiDestinationIndex:[indexPath indexAtPosition:1] ];
	}
    /*else if (tableView==_LANdiniUserTableView){
        [self.LANdiniDelegate ]
    }*/
}


- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    if(tableView == _documentsTableView)return [(mmpOrAll ? allFiles : MMPFiles) count];
    else if (tableView==_midiSourceTableView)return [[[self.audioDelegate midi] sources]  count];
    else if (tableView==_midiDestinationTableView)return [[[self.audioDelegate midi] destinations]  count];
    else if (tableView==_LANdiniUserTableView) return [_LANdiniUserArray count];
    else return 0;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)aTableView {
    return 1;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath{
    if(tableView == _documentsTableView){
        if (hardwareCanvasType==canvasTypeIPad)return 70;
        else return 35;
    }
	else {//midi and landini tables
        if (hardwareCanvasType==canvasTypeIPad)return 45;
        else return 22.5;
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    if(tableView == _documentsTableView){
        static NSString* CellIdentifier = @"ValueCell";
        UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    
        if (cell == nil) {
            cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:CellIdentifier];
            if (hardwareCanvasType==canvasTypeIPad)cell.textLabel.font=[UIFont systemFontOfSize:32];
            else cell.textLabel.font=[UIFont systemFontOfSize:16];
        }
    
        cell.textLabel.text=[(mmpOrAll ? allFiles : MMPFiles) objectAtIndex:[indexPath row]];
        NSString* suffix = [[[(mmpOrAll ? allFiles : MMPFiles) objectAtIndex:[indexPath row]] componentsSeparatedByString: @"."] lastObject];
        if([suffix isEqualToString:@"mmp"] || [suffix isEqualToString:@"zip"] || [suffix isEqualToString:@"pd"]){
            cell.textLabel.textColor = [UIColor blackColor];
            cell.userInteractionEnabled=YES;
        }
        else{
            cell.textLabel.textColor = [UIColor grayColor];
            cell.userInteractionEnabled=NO;

        }
    
        return cell;
    }
    
    else if (tableView==_midiSourceTableView){
        PGMidiConnection* currSource = [[[self.audioDelegate midi] sources] objectAtIndex: [indexPath indexAtPosition:1]];
		NSString* currMidiSourceName = currSource.name;
		UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:currMidiSourceName];
		
        if(cell==nil){
			cell = [[UITableViewCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:currMidiSourceName] ;
			
			if (hardwareCanvasType==canvasTypeIPad)cell.textLabel.font=[UIFont systemFontOfSize:24];
            else cell.textLabel.font=[UIFont systemFontOfSize:12];
		}
        [cell textLabel].text=currMidiSourceName;
		return cell;
	}
    
    else if (tableView==_midiDestinationTableView){
        PGMidiConnection* currDestination = [[[self.audioDelegate midi] destinations] objectAtIndex: [indexPath indexAtPosition:1]];
		NSString* currMidiDestName = currDestination.name;
		UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:currMidiDestName];
		
        if(cell==nil){
			cell = [[UITableViewCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:currMidiDestName] ;
			
			if (hardwareCanvasType==canvasTypeIPad)cell.textLabel.font=[UIFont systemFontOfSize:24];
            else cell.textLabel.font=[UIFont systemFontOfSize:12];
		}
        [cell textLabel].text=currMidiDestName;
		return cell;

    }
    
    else /*if (tableView==_LANdiniUserTableView)*/{
        UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:@"LANdiniUserCell"];
		LANdiniUser* user = [_LANdiniUserArray objectAtIndex:[indexPath row]];
        
        if(cell==nil){
			cell = [[UITableViewCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"LANdiniUserCell"] ;
			if (hardwareCanvasType==canvasTypeIPad)cell.textLabel.font=[UIFont systemFontOfSize:24];
            else cell.textLabel.font=[UIFont systemFontOfSize:12];
		}
        [cell textLabel].text=[NSString stringWithFormat:@"%@ - %@", user.name, user.ip];
		return cell;
    }

}

#pragma mark LANdiniUserDelegate - can be on non-main threads

-(void)userStateChanged:(NSArray*)userArray{
    _LANdiniUserArray = userArray;
    dispatch_async(dispatch_get_main_queue(), ^{
        [_LANdiniUserTableView reloadData];
    });
}

-(void)syncServerChanged:(NSString*)newServerName{
    _LANdiniSyncServerName = newServerName;
}

#pragma mark reachability from vC
-(void)reachabilityChanged:(NSNotification*)note {
    Reachability* reach = (Reachability*)note.userInfo;
    [self updateNetworkLabel:reach];
    
}

-(void)updateNetworkLabel:(Reachability*)reach{
    NSString* network = [ViewController fetchSSIDInfo];
    [_LANdiniNetworkLabel setText:[NSString stringWithFormat:@"wifi network %@: %@", [reach isReachable] ? @"enabled" : @"disabled", network ? network : @""]];
}

# pragma mark AudioBus

- (void)connectionsChanged:(NSNotification*)notification {
  if([self.audioDelegate respondsToSelector:@selector(isAudioBusConnected)]){
    if([self.audioDelegate isAudioBusConnected]) {
      _rateSeg.enabled=NO;
      _tickSeg.enabled=NO;
      _audioEnableButton.enabled=NO;
      [_audioEnableButton setTitle:@"AudioBus" forState:UIControlStateNormal];
    }
    else{
      _rateSeg.enabled=YES;
      _tickSeg.enabled=YES;
      _audioEnableButton.enabled=YES;
      [self refreshAudioEnableButton];
    }
  }

}

# pragma mark cleanup

-(void)viewDidUnload{
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:UIApplicationDidBecomeActiveNotification
                                                  object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:kReachabilityChangedNotification
                                                  object:nil];
    if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"6.0")){
        [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionRouteChangeNotification object:nil];
    }

}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
