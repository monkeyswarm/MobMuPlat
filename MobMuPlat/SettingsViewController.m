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
#import "JSONKit.h"
#import <QuartzCore/QuartzCore.h>
#import "ZipArchive.h"


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
    
    //ios 7 don't have it go under the nav bar
    if ([self respondsToSelector:@selector(edgesForExtendedLayout)])
        self.edgesForExtendedLayout = UIRectEdgeNone;
    
    self.view.backgroundColor=[UIColor colorWithRed:.4 green:.4 blue:.4 alpha:1];
   
  
    
    self.navigationItem.title = @"Select Document";
    UIBarButtonItem* doneButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone
                                                                                target:self
                                                                                action:@selector(done:)];
    self.navigationItem.leftBarButtonItem = doneButton;
   
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
    if(hardwareCanvasType==canvasTypeIPad)cornerRadius=20;
    else cornerRadius=10;
    
    //set up the three main subviews
    filesView = [[UIView alloc] init];
    consoleView = [[UIView alloc] init];
    audioMIDIView = [[UIView alloc] init];
    
    filesView.backgroundColor=[UIColor purpleColor];
    consoleView.backgroundColor = [UIColor colorWithRed:.843 green:.23 blue:.351 alpha:1];//red
    audioMIDIView.backgroundColor = [UIColor colorWithRed:.902 green:.33 blue:.18 alpha:1];//orange
    
    filesView.layer.cornerRadius = cornerRadius;
    consoleView.layer.cornerRadius = cornerRadius;
    audioMIDIView.layer.cornerRadius = cornerRadius;
    
    loadDocButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    dspButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    consoleButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    
    //setup elements of the filesView
    filesTableView = [[UITableView alloc]init];
    filesTableView.layer.cornerRadius = cornerRadius;
    showFilesButton =[UIButton buttonWithType:UIButtonTypeRoundedRect];
    
    //setup element of the consoleView
    consoleTextView = [[UITextView alloc]init];
    consoleTextView.layer.cornerRadius = cornerRadius;
    clearConsoleButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    
    //setup elements of the audioMIDIView
    //UILabel* midiLabel = [[UILabel alloc]init ];
    UILabel* midiSourceLabel = [[UILabel alloc]init ];
    midiSourceTableView  = [[UITableView alloc]init];
    midiSourceTableView.layer.cornerRadius = cornerRadius;
    //UILabel* midiDestinationLabel = [[UILabel alloc]init ];
    midiDestinationTableView  = [[UITableView alloc]init];
    midiDestinationTableView.layer.cornerRadius = cornerRadius;
    
    UILabel* bufferLabel = [[UILabel alloc]init ];
    UILabel* rateLabel = [[UILabel alloc]init];
    tickValueLabel = [[UILabel alloc]init];
    UILabel* backgroundAudioEnableLabel= [[UILabel alloc]init];
    
    //setup buttons at bottom of main view
    [loadDocButton setTitle:@"Select Document" forState:UIControlStateNormal];
    [loadDocButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    [loadDocButton setTitleColor:[UIColor grayColor] forState:UIControlStateDisabled];
    [loadDocButton addTarget:self action:@selector(showLoadDoc:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:loadDocButton ];
    loadDocButton.enabled=NO;//disable a button when we are looking at its affiliated subview
    
    if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")){
        loadDocButton.layer.cornerRadius = 5;
        loadDocButton.layer.borderWidth = 1;
        loadDocButton.layer.borderColor = [UIColor blackColor].CGColor;
    }
    
    
    [dspButton setTitle:@"Audio & MIDI" forState:UIControlStateNormal];
    [dspButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    [dspButton setTitleColor:[UIColor grayColor] forState:UIControlStateDisabled];
    [dspButton addTarget:self action:@selector(showDSP:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:dspButton ];
    if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")){
        dspButton.layer.cornerRadius = 5;
        dspButton.layer.borderWidth = 1;
        dspButton.layer.borderColor = [UIColor blackColor].CGColor;
    }
    
    [consoleButton setTitle:@"Pd Console" forState:UIControlStateNormal];
    [consoleButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    [consoleButton setTitleColor:[UIColor grayColor] forState:UIControlStateDisabled];
    [consoleButton addTarget:self action:@selector(showConsole:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:consoleButton ];
    if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")){
        consoleButton.layer.cornerRadius = 5;
        consoleButton.layer.borderWidth = 1;
        consoleButton.layer.borderColor = [UIColor blackColor].CGColor;
    }
    
    //setup elements in filesview
    filesTableView.delegate=self;
    filesTableView.dataSource=self;
    [filesView addSubview:filesTableView];
    
    [showFilesButton setTitle:@"show all files" forState:UIControlStateNormal];
    [showFilesButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    [showFilesButton addTarget:self action:@selector(showFilesButtonHit:) forControlEvents:UIControlEventTouchUpInside];
    [filesView addSubview:showFilesButton];
    if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")){
        showFilesButton.layer.cornerRadius = 5;
        showFilesButton.layer.borderWidth = 1;
        showFilesButton.layer.borderColor = [UIColor blackColor].CGColor;
    }
    
    
    //layout audioMidiView ====
    
	midiSourceLabel.text=@"Select MIDI Input & Output";
	midiSourceLabel.backgroundColor=[UIColor clearColor];
	midiSourceLabel.textAlignment=UITextAlignmentCenter;
	midiSourceLabel.font=[UIFont systemFontOfSize:16];
	[audioMIDIView addSubview:midiSourceLabel];
    
    midiSourceTableView.delegate=self;
	midiSourceTableView.dataSource=self;
	midiSourceTableView.bounces=NO;
	[audioMIDIView addSubview:midiSourceTableView];
    
    midiDestinationTableView.delegate=self;
	midiDestinationTableView.dataSource=self;
	midiDestinationTableView.bounces=NO;
	[audioMIDIView addSubview:midiDestinationTableView];
    
    NSIndexPath *topIndexPath = [NSIndexPath indexPathForRow:0 inSection: 0];
    //connect to top element of midi source list
	if( [[[self.audioDelegate midi] sources] count] >0 )
        [midiSourceTableView selectRowAtIndexPath:topIndexPath animated:NO scrollPosition:UITableViewScrollPositionTop];
    if( [[[self.audioDelegate midi] destinations] count] >0 )
        [midiDestinationTableView selectRowAtIndexPath:topIndexPath animated:NO scrollPosition:UITableViewScrollPositionTop];
    
    
    bufferLabel.text=@"Audio Buffer Size (in Pd Blocks)";
	bufferLabel.backgroundColor=[UIColor clearColor];
	bufferLabel.textAlignment=UITextAlignmentCenter;
	bufferLabel.font=[UIFont systemFontOfSize:16];//[MBConstants paramLabelFont];
	[audioMIDIView addSubview:bufferLabel];
    
    NSArray* tickItems = [NSArray arrayWithObjects:@"1", @"2", @"4", @"8", @"16", @"32", @"64", nil];
    tickSeg = [[UISegmentedControl alloc]initWithItems:tickItems];
    UIFont *font = [UIFont boldSystemFontOfSize:12.0f];
    NSDictionary *attributes = [NSDictionary dictionaryWithObject:font forKey:UITextAttributeFont];
    [tickSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
    [tickSeg addTarget:self action:@selector(tickSegChanged:) forControlEvents:UIControlEventValueChanged];
    [audioMIDIView addSubview:tickSeg];
    
    int actualTicks = [self.audioDelegate actualTicksPerBuffer];
    tickSeg.selectedSegmentIndex = (int)log2(actualTicks);
    
    int blockSize = [self.audioDelegate blockSize];
    tickValueLabel.font=[UIFont systemFontOfSize:12];
    tickValueLabel.backgroundColor=[UIColor clearColor];
    tickValueLabel.numberOfLines=2;
    tickValueLabel.textAlignment=UITextAlignmentCenter;
    [tickValueLabel setText:[NSString stringWithFormat:@"request: %d * block size (%d) = %d samples \nactual: %d * block size (%d) = %d samples", requestedBlockCount, blockSize, requestedBlockCount*blockSize, actualTicks, blockSize, actualTicks*blockSize  ]];
    [audioMIDIView addSubview:tickValueLabel];
    
    rateLabel.text=@"Sampling Rate";
	rateLabel.backgroundColor=[UIColor clearColor];
	rateLabel.textAlignment=UITextAlignmentCenter;
	rateLabel.font=[UIFont systemFontOfSize:16];
	[audioMIDIView addSubview:rateLabel];
    
    NSArray* rateItems = [NSArray arrayWithObjects:@"8000", @"11025", @"22050", @"32000", @"44100", @"48000", nil];
    UISegmentedControl* rateSeg = [[UISegmentedControl alloc]initWithItems:rateItems];
    [rateSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
    [rateSeg addTarget:self action:@selector(rateSegChanged:) forControlEvents:UIControlEventValueChanged];
    [audioMIDIView addSubview:rateSeg];
    
    //int rate = [self.audioDelegate sampleRate];
    //I am going to assume that my sample rate is 44100, as I set it to such in ViewController before creating this object
    rateSeg.selectedSegmentIndex = 4;
    
    //background audio 
    backgroundAudioEnableLabel.text=@"app runs in background:\n(for iOS controllers/sequencers)";
    backgroundAudioEnableLabel.backgroundColor=[UIColor clearColor];
	backgroundAudioEnableLabel.textAlignment=UITextAlignmentRight;
    backgroundAudioEnableLabel.numberOfLines=2;
	backgroundAudioEnableLabel.font=[UIFont systemFontOfSize:12];
	[audioMIDIView addSubview:backgroundAudioEnableLabel];
    
    audioEnableButton = [UIButton buttonWithType:UIButtonTypeCustom];
	audioEnableButton.layer.cornerRadius=5;
	audioEnableButton.layer.borderWidth=2;
	[audioEnableButton setTitle:@"disabled" forState:UIControlStateNormal];
	audioEnableButton.layer.borderColor=[[UIColor whiteColor] CGColor];
	[audioEnableButton addTarget:self action:@selector(audioEnableButtonHit ) forControlEvents:UIControlEventTouchDown];
   	[audioMIDIView addSubview: audioEnableButton];
    
    
    
    //setup consoleView elements
    consoleTextView.editable=NO;
    [consoleView addSubview:consoleTextView];
    
    [clearConsoleButton setTitle:@"clear" forState:UIControlStateNormal];
    [clearConsoleButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    [clearConsoleButton addTarget:self action:@selector(clearConsole:) forControlEvents:UIControlEventTouchUpInside];
    [consoleView addSubview:clearConsoleButton];
    if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")){
        clearConsoleButton.layer.cornerRadius = 5;
        clearConsoleButton.layer.borderWidth = 1;
        clearConsoleButton.layer.borderColor = [UIColor blackColor].CGColor;
    }
    
    //layout everything by canvas size
    
    if(hardwareCanvasType==canvasTypeIPhone3p5Inch){
        CGRect innerViewRect = CGRectMake(10, 10, 300, 320);
        filesView.frame=innerViewRect;
        consoleView.frame=innerViewRect;
        audioMIDIView.frame=innerViewRect;
        
        consoleButton.frame = CGRectMake(10, 340, 145, 35);
        dspButton.frame = CGRectMake(10+145+10, 340, 145, 35);
        loadDocButton.frame=CGRectMake(10, 390, 300, 35);
        
        //load doc
        filesTableView.frame=CGRectMake(5, 5, 290, 280) ;
        showFilesButton.frame = CGRectMake(20, 290, 260, 25);
        
        //audio midi
        midiSourceLabel.frame = CGRectMake(5, 0, 290, 25);
        midiSourceTableView.frame = CGRectMake(10, 30, 135, 60);
        midiDestinationTableView.frame = CGRectMake(10+135+10, 30, 135, 60);
        bufferLabel.frame = CGRectMake(5, 100, 290, 25);
        [tickSeg setFrame:CGRectMake(10, 125, 280, 30)];
        tickValueLabel.frame = CGRectMake(10, 160, 280, 30);
        rateLabel.frame=CGRectMake(5,190,290, 25);
        rateSeg.frame=CGRectMake(10, 215, 280, 30);
        audioEnableButton.frame=CGRectMake(190,260,90,40);
        backgroundAudioEnableLabel.frame = CGRectMake(5, 260, 180, 40);
        
        //console
        consoleTextView.frame = CGRectMake(5, 5, 290, 280);
        consoleTextView.font=[UIFont systemFontOfSize:18];
        clearConsoleButton.frame = CGRectMake(20, 290, 260, 25);
        
    }
    else if(hardwareCanvasType==canvasTypeIPhone4Inch){
        CGRect innerViewRect = CGRectMake(10, 10, 300, 320+80);//+80 height
        filesView.frame=innerViewRect;
        consoleView.frame=innerViewRect;
        audioMIDIView.frame=innerViewRect;
        
        consoleButton.frame = CGRectMake(10, 340+86, 145, 35);//+86 ypos
        dspButton.frame = CGRectMake(10+145+10, 340+86, 145, 35);
        loadDocButton.frame=CGRectMake(10, 390+86, 300, 35);
        
        //load doc
        filesTableView.frame=CGRectMake(5, 5, 290, 280+80) ;
        showFilesButton.frame = CGRectMake(20, 290+80, 260, 25);
        
        //audio midi - same
        midiSourceLabel.frame = CGRectMake(5, 0, 290, 25);
        midiSourceTableView.frame = CGRectMake(10, 30, 135, 100);
        midiDestinationTableView.frame=CGRectMake(10+135+10, 30, 135, 100);
        bufferLabel.frame = CGRectMake(5, 145, 290, 25);
        [tickSeg setFrame:CGRectMake(10, 170, 280, 30)];
        
        tickValueLabel.frame = CGRectMake(10, 205, 280, 30);
        rateLabel.frame=CGRectMake(5,245,290, 25);
        rateSeg.frame=CGRectMake(10, 270, 280, 30);
        audioEnableButton.frame=CGRectMake(190,335,90,40);
        backgroundAudioEnableLabel.frame = CGRectMake(5, 335, 180, 40);
        
        //console
        consoleTextView.frame = CGRectMake(5, 5, 290, 280+80);
        consoleTextView.font=[UIFont systemFontOfSize:18];
        clearConsoleButton.frame = CGRectMake(20, 290+80, 260, 25);

    }
    else{ //ipad
        CGRect innerViewRect = CGRectMake(10*2.4, 10*2.133, 300*2.4, 320*2.133);//+448 wid, +544 height - factor 2.4 width 2.13333 height
        filesView.frame=innerViewRect;
        consoleView.frame=innerViewRect;
        audioMIDIView.frame=innerViewRect;
        
        consoleButton.frame = CGRectMake(10*2.4, 340*2.133, 145*2.4, 35*2.133);
        [consoleButton setFont:[UIFont systemFontOfSize:28]];
        dspButton.frame = CGRectMake((10+145+10)*2.4, 340*2.133, 145*2.4, 35*2.133);
        [dspButton setFont:[UIFont systemFontOfSize:28]];
        loadDocButton.frame=CGRectMake(10*2.4, 390*2.133, 300*2.4, 35*2.133);
        [loadDocButton setFont:[UIFont systemFontOfSize:28]];
        
        //load doc
        filesTableView.frame=CGRectMake(5*2.4, 5*2.133, 290*2.4, 280*2.133) ;
        showFilesButton.frame = CGRectMake(20*2.4, 290*2.133, 260*2.4, 25*2.133);
        [showFilesButton setFont:[UIFont systemFontOfSize:28]];
        
        //audio midi
        midiSourceLabel.frame = CGRectMake(5*2.4, 0, 290*2.4, 25*2.133);
        midiSourceLabel.font=[UIFont systemFontOfSize:32];
        midiSourceTableView.frame = CGRectMake(10*2.4, 30*2.133, 135*2.4, 60*2.133);
        midiDestinationTableView.frame=CGRectMake((10+135+10)*2.4, 30*2.133, 135*2.4, 60*2.133);
        bufferLabel.frame = CGRectMake(5*2.4, 100*2.133, 290*2.4, 25*2.133);
        bufferLabel.font=[UIFont systemFontOfSize:32];
        [tickSeg setFrame:CGRectMake(10*2.4, 125*2.133, 280*2.4, 30*2.133)];
        
        tickValueLabel.frame = CGRectMake(10*2.4, 160*2.133, 280*2.4, 30*2.133);
        tickValueLabel.font=[UIFont systemFontOfSize:24];
        
        rateLabel.frame=CGRectMake(5*2.4,195*2.133,290*2.4, 25*2.133);
        rateLabel.font = [UIFont systemFontOfSize:32];
        rateSeg.frame=CGRectMake(10*2.4, 220*2.133, 280*2.4, 30*2.133);
        
        audioEnableButton.frame=CGRectMake(190*2.4,260*2.133,90*2.4,40*2.133);
        audioEnableButton.font = [UIFont systemFontOfSize:32];
        backgroundAudioEnableLabel.frame = CGRectMake(5*2.4, 260*2.133, 180*2.4, 40*2.133);
        backgroundAudioEnableLabel.font=[UIFont systemFontOfSize:24];
        
        //console
        consoleTextView.frame = CGRectMake(5*2.4, 5*2.133, 290*2.4, 280*2.133);
        consoleTextView.font=[UIFont systemFontOfSize:36];
        clearConsoleButton.frame = CGRectMake(20*2.4, 290*2.133, 260*2.4, 25*2.133);
        [clearConsoleButton setFont:[UIFont systemFontOfSize:28]];
        //segmented
        UIFont *font = [UIFont boldSystemFontOfSize:24.0f];
        NSDictionary *attributes = [NSDictionary dictionaryWithObject:font forKey:UITextAttributeFont];
        [tickSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
        [rateSeg setTitleTextAttributes:attributes forState:UIControlStateNormal];
    }
    
    [self.view addSubview:consoleView];
    [self.view addSubview:audioMIDIView];
    [self.view addSubview:filesView];
    
}

-(void)viewWillAppear:(BOOL)animated{
    consoleTextView.text = consoleTextString;
    [consoleTextView scrollRangeToVisible:(NSRange){consoleTextString.length-1, 1}];

}

-(void)audioEnableButtonHit{
    self.audioDelegate.backgroundAudioEnabled=!self.audioDelegate.backgroundAudioEnabled;
    if(self.audioDelegate.backgroundAudioEnabled){
        [audioEnableButton setTitle:@"enabled" forState:UIControlStateNormal];
        [audioEnableButton setBackgroundColor:[UIColor whiteColor]];
        [audioEnableButton setTitleColor:[UIColor orangeColor] forState:UIControlStateNormal];
    }
    
    else{
        [audioEnableButton setTitle:@"disabled" forState:UIControlStateNormal];
        [audioEnableButton setBackgroundColor:[UIColor clearColor]];
        [audioEnableButton setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    }
}

- (void)done:(id)sender {
    [self.delegate settingsViewControllerDidFinish:self];
}

-(void)showFilesButtonHit:(id)sender{
    if([showFilesButton tag]==0){//is showing mmp, change to show all
        mmpOrAll=YES;
        [self reloadFileTable];
        [showFilesButton setTitle:@"show only mmp files" forState:UIControlStateNormal];
        showFilesButton.tag=1;
    }
    else{
        mmpOrAll=NO;
        [self reloadFileTable];
        [showFilesButton setTitle:@"show all files" forState:UIControlStateNormal];
        showFilesButton.tag=0;
    }
}

-(void) showLoadDoc:(id)sender{
    loadDocButton.enabled=NO;
    dspButton.enabled=YES;
    consoleButton.enabled=YES;
    [self.view bringSubviewToFront:filesView];
    self.navigationItem.title = @"Select Document";
    
}
-(void)showConsole:(id)sender{
    loadDocButton.enabled=YES;
    dspButton.enabled=YES;
    consoleButton.enabled=NO;
    [self.view bringSubviewToFront:consoleView];
    self.navigationItem.title = @"Pd Console";

}

- (void)showDSP:(id)sender {
    loadDocButton.enabled=YES;
    dspButton.enabled=NO;
    consoleButton.enabled=YES;
    [self.view bringSubviewToFront:audioMIDIView];
    self.navigationItem.title = @"Audio MIDI Settings";
    
    
}

-(void)clearConsole:(id)sender{
    consoleTextString=@"";
    consoleTextView.text = consoleTextString;
}

//adds string to queue
-(void)consolePrint:(NSString *)message{
    [consoleStringQueue addObject:message];
}

//called often by timer
-(void)consolePrintFunction{
    
    if([consoleStringQueue count]==0)return;//nothing to print

    //take all the string in the queue and shove them into one big string
    NSString* newString = [consoleStringQueue componentsJoinedByString:@""];
    consoleTextString = [consoleTextString stringByAppendingFormat:@"%@", newString];//append to currently shown string
    int startPoint = [consoleTextString length]-1000; if (startPoint<0)startPoint=0;
    NSRange stringRange = {startPoint, MIN([consoleTextString length], 1000)};//chop off front of string to fit
    consoleTextString = [consoleTextString substringWithRange:stringRange];
    [consoleStringQueue removeAllObjects];
    
    if (self.isViewLoaded && self.view.window) {//if I am on screen, show and scroll
        consoleTextView.text = consoleTextString;
        [consoleTextView scrollRangeToVisible:(NSRange){consoleTextString.length-1, 1}];
    }
  
}

-(void)tickSegChanged:(UISegmentedControl*)sender{
    int index = [sender selectedSegmentIndex];
    requestedBlockCount = (int)pow(2, index);
    int blockSize = [self.audioDelegate blockSize];
    
    
    int actualTicks = [self.audioDelegate setTicksPerBuffer:requestedBlockCount];
    [tickValueLabel setText:[NSString stringWithFormat:@"request: %d * block size (%d) = %d samples \nactual: %d * block size (%d) = %d samples", requestedBlockCount, blockSize, requestedBlockCount*blockSize, actualTicks, blockSize, actualTicks*blockSize  ]];
    
    if(actualTicks!=requestedBlockCount){
        int actualIndex = (int)log2(actualTicks/64);
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
        actualTicks = [self.audioDelegate setTicksPerBuffer:requestedBlockCount];
        if( fmod(log2(actualTicks), 1)==0){
            int newBlockIndex = (int)log2(actualTicks);
            [tickSeg setSelectedSegmentIndex:newBlockIndex];
        }
        else [tickSeg setSelectedSegmentIndex:UISegmentedControlNoSegment];
        
    }
    
    [tickValueLabel setText:[NSString stringWithFormat:@"request: %d * block size (%d) = %d samples \nactual: %d * block size (%d) = %d samples", requestedBlockCount, blockSize, requestedBlockCount*blockSize, actualTicks, blockSize, actualTicks*blockSize  ]];
}


-(void)reloadFileTable{
    MMPFiles = [SettingsViewController getDocumentsOnlyMMP:YES];
    allFiles = [SettingsViewController getDocumentsOnlyMMP:NO];
    [filesTableView reloadData];
}

-(void)reloadMidiSources{
    [midiSourceTableView reloadData];
    [midiDestinationTableView reloadData];
}

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
        NSString* jsonString = [NSString stringWithContentsOfFile: fullPath];
        NSDictionary* sceneDict = [jsonString objectFromJSONString];
        
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

    if(tableView==filesTableView){
        UITableViewCell* cell = [tableView cellForRowAtIndexPath:indexPath];
        
        //add an activity indicator
        UIActivityIndicatorView* aiv = [[UIActivityIndicatorView alloc]initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
        aiv.frame = CGRectMake(0, 0, 24, 24);
        [cell setAccessoryView:aiv];
        [aiv startAnimating];
    
        //load the pd file, then wait
        [self performSelector:@selector(selectHelper:) withObject:indexPath afterDelay:0];
 
        //done
        [aiv performSelector:@selector(stopAnimating) withObject:nil afterDelay:0];//performSelector: puts method call on next run loop
        [cell setAccessoryView:nil];//assuming that this causes ARC to release the aiv...
        
    }
    
    else if (tableView==midiSourceTableView){
        [self.audioDelegate setMidiSourceIndex:[indexPath indexAtPosition:1] ];
	}
    else if (tableView==midiDestinationTableView){
        [self.audioDelegate setMidiDestinationIndex:[indexPath indexAtPosition:1] ];
	}
    
}


- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    if(tableView == filesTableView)return [(mmpOrAll ? allFiles : MMPFiles) count];
    else if (tableView==midiSourceTableView)return [[[self.audioDelegate midi] sources]  count];
    else return [[[self.audioDelegate midi] destinations]  count];
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)aTableView {
    return 1;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath{
    if(tableView == filesTableView){
        if (hardwareCanvasType==canvasTypeIPad)return 70;
        else return 35;
    }
	else /*if (tableView==midiSourceTableView)*/{
        if (hardwareCanvasType==canvasTypeIPad)return 45;
        else return 22.5;
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    if(tableView == filesTableView){
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
    
    else if (tableView==midiSourceTableView){
        PGMidiConnection* currSource = [[[self.audioDelegate midi] sources] objectAtIndex: [indexPath indexAtPosition:1]];
		NSString* currMidiSourceName = currSource.name;
		UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:currMidiSourceName];
		
        if(cell==nil){
			cell = [[UITableViewCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:currMidiSourceName] ;
			[cell textLabel].text=currMidiSourceName;
			if (hardwareCanvasType==canvasTypeIPad)cell.textLabel.font=[UIFont systemFontOfSize:24];
            else cell.textLabel.font=[UIFont systemFontOfSize:12];
		}
		return cell;
	}
    
    else{//destination
        PGMidiConnection* currDestination = [[[self.audioDelegate midi] destinations] objectAtIndex: [indexPath indexAtPosition:1]];
		NSString* currMidiDestName = currDestination.name;
		UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:currMidiDestName];
		
        if(cell==nil){
			cell = [[UITableViewCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:currMidiDestName] ;
			[cell textLabel].text=currMidiDestName;
			if (hardwareCanvasType==canvasTypeIPad)cell.textLabel.font=[UIFont systemFontOfSize:24];
            else cell.textLabel.font=[UIFont systemFontOfSize:12];
		}
		return cell;

    }

}




- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
