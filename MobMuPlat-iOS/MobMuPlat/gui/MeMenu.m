//
//  MeMenu.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 4/8/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeMenu.h"
#import <QuartzCore/QuartzCore.h>
#define EDGE_RADIUS 5
#define DEFAULT_FONT @"HelveticaNeue"
#define DEFAULT_FONTSIZE 18
#define TAB_WIDTH 30
#import "MobMuPlatUtil.h"
#import "MenuViewController.h"
#import "MenuNavigationController.h"





@implementation MeMenu {
  UIButton* theButton;
  UIView* downView;
  MenuViewController* mvc;
  NSMutableArray* _dataArray;
}
static NSString *CellIdentifier = @"MenuCell";


- (id)initWithFrame:(CGRect)frame
{
  self = [super initWithFrame:frame];
  if (self) {
    self.address=@"/unnamedMenu";
    //_value=0;
    
    downView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, TAB_WIDTH, self.frame.size.height) ];
    downView.layer.borderColor = [[UIColor whiteColor] CGColor];
    downView.layer.borderWidth = 2;
    downView.layer.cornerRadius=EDGE_RADIUS;
    [self addSubview:downView];

    
    theButton = [UIButton buttonWithType:UIButtonTypeCustom ];
    theButton.frame=CGRectMake(0, 0, self.frame.size.width, self.frame.size.height);
    //theButton.backgroundColor=[UIColor whiteColor];//default color
    [theButton addTarget:self action:@selector(buttonHitDown) forControlEvents:UIControlEventTouchDown];
    [theButton addTarget:self action:@selector(buttonHitUp) forControlEvents:UIControlEventTouchUpInside|UIControlEventTouchCancel|UIControlEventTouchUpOutside];
    theButton.layer.cornerRadius=EDGE_RADIUS;
    
    _titleString = @"Menu";
    [self setTitleString:_titleString];
    //[theButton setTitle:_titleString forState:UIControlStateNormal];
    theButton.titleLabel.font = [UIFont fontWithName:DEFAULT_FONT size:DEFAULT_FONTSIZE];
    //theButton.titleLabel.edge
    /*CGRect titleFrame = theButton.titleLabel.frame;
    titleFrame.origin.x = 100;
    [theButton.titleLabel setFrame:titleFrame];
    */
    [theButton setTitleEdgeInsets:UIEdgeInsetsMake(0.0f, TAB_WIDTH, 0.0f, 0.0f)];
    [theButton setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    [theButton setTitleColor:[UIColor blueColor] forState:UIControlStateHighlighted];
    theButton.backgroundColor = [UIColor clearColor];
    theButton.layer.borderColor = [[UIColor whiteColor] CGColor];
    theButton.layer.borderWidth = 2;
    
    [self addSubview: theButton];
   
  }
  return self;
}

-(void)setColor:(UIColor *)inColor{
  [super setColor:inColor];
  [theButton setTitleColor:inColor forState:UIControlStateNormal];
  theButton.layer.borderColor = inColor.CGColor;
  downView.layer.borderColor = inColor.CGColor;
}


-(void)setHighlightColor:(UIColor *)highlightColor{
  [super setHighlightColor:highlightColor];
  [theButton setTitleColor:highlightColor forState:UIControlStateHighlighted];
}

-(void)buttonHitDown{
  theButton.layer.borderColor = self.highlightColor.CGColor;
  downView.layer.borderColor = self.highlightColor.CGColor;
  
  mvc = [[MenuViewController alloc] init];
  mvc.tableView.dataSource = self;
  mvc.tableView.delegate = self;
  mvc.title = _titleString;
  mvc.tableView.backgroundColor = self.controlDelegate.patchBackgroundColor;
  //once we stop supporting ios5: [mvc.tableView registerClass:[UITableViewCell class] forCellReuseIdentifier:CellIdentifier];
  
  //set orientation
  MenuNavigationController* navigationController = [[MenuNavigationController alloc] initWithRootViewController:mvc ];
  navigationController.orientation = [self.controlDelegate orientation];
  navigationController.navigationBar.barStyle = UIBarStyleBlack;

  //[self.controlDelegate presentViewController:navigationController animated:YES completion:nil];
  [self.controlDelegate presentModalViewController:navigationController animated:YES];
}

-(void)buttonHitUp{
  theButton.layer.borderColor = self.color.CGColor;
  downView.layer.borderColor = self.color.CGColor;
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
  return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section{
  // Return the number of rows in the section.
  return [_dataArray count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath{
  
  UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
  //still supporting ios5, so not yet: [tableView dequeueReusableCellWithIdentifier:CellIdentifier forIndexPath:indexPath];
  
  //remove this on stop supporting ios5
  if (cell == nil) {
    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:CellIdentifier];
  }
  // Configure the cell...
  [cell.textLabel setText:[_dataArray objectAtIndex:indexPath.row]];
  cell.textLabel.textAlignment = UITextAlignmentCenter;
  cell.textLabel.textColor = self.color;
  UIView* bgView = [[UIView alloc]init];
  bgView.backgroundColor = self.highlightColor;
  cell.selectedBackgroundView = bgView;
  return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  //send
 [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address, [NSNumber numberWithInt:(int)indexPath.row], [_dataArray objectAtIndex:indexPath.row], nil]];
  //clear
  [mvc dismissModalViewControllerAnimated:YES];
  //display
  //[theButton setTitle:[_dataArray objectAtIndex:indexPath.row] forState:UIControlStateNormal];
}

- (void)setTitleString:(NSString *)titleString{
  _titleString = titleString;
  [theButton setTitle:titleString forState:UIControlStateNormal];
}

-(void)tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath {
  cell.backgroundColor = self.controlDelegate.patchBackgroundColor;
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
  NSMutableArray* dataArray = [[NSMutableArray alloc] init];
  
  for(id thing in inArray){
    if([thing isKindOfClass:[NSString class]]){
      [dataArray addObject:(NSString*)thing];
    }
    else if ([thing isKindOfClass:[NSNumber class]]){
      NSNumber* thingNumber = (NSNumber*)thing;
      if ([MobMuPlatUtil numberIsFloat:thingNumber] ){ //todo put in separate class
        //pd sends floats :(
        if(fmod([thingNumber floatValue],1)==0) {
          [dataArray addObject:[NSString stringWithFormat:@"%d", (int)[thingNumber floatValue]]];//print whole numbers as ints
        } else {
          [dataArray addObject:[NSString stringWithFormat:@"%.3f", [thingNumber floatValue]]];
        }
      }
      else {
        [dataArray addObject:[NSString stringWithFormat:@"%d", [thingNumber intValue]]];
      }
    }
  }
  _dataArray=dataArray;
  
  if(mvc){
    [mvc.tableView reloadData];
  }
  
}


@end
