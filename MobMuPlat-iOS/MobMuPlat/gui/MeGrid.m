//
//  MeGrid.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/26/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "MeGrid.h"
#import <QuartzCore/QuartzCore.h>

@implementation MeGrid

- (id)initWithFrame:(CGRect)frame{
    self = [super initWithFrame:frame];
    if (self) {
        self.address=@"/unnamedGrid";
        //defaults
        _cellPadding=1;
        _borderThickness=2;
        [self setDimX:4 Y:3];
  }
    return self;
}

-(void)setCellPadding:(int)cellPadding{//do before setting the dim
    _cellPadding=cellPadding;
    [self setDimX:dimX Y:dimY];
}

-(void)setBorderThickness:(int)borderThickness{//do before setting the dim
    _borderThickness = borderThickness;
    for(UIButton* button in gridButtons)button.layer.borderWidth=_borderThickness;
    
}


-(void)setDimX:(int)inX Y:(int)inY{
    if(gridButtons)for(UIButton* button in gridButtons)[button removeFromSuperview];
    
    dimX=inX; dimY=inY;
    float buttonWidth = self.frame.size.width/dimX;
    float buttonHeight = self.frame.size.height/dimY;
    
    gridButtons = [[NSMutableArray alloc]initWithCapacity:dimX*dimY];
     for(int j=0;j<dimY;j++){
         for(int i=0;i<dimX;i++){
       
            UIButton* newButton = [UIButton buttonWithType:UIButtonTypeCustom];
            newButton.frame = CGRectMake(i*buttonWidth, j*buttonHeight, buttonWidth-_cellPadding, buttonHeight-_cellPadding);
            [newButton addTarget:self action:@selector(buttonDown:) forControlEvents:UIControlEventTouchDown];
           
           if(_mode==1)
              [newButton addTarget:self action:@selector(buttonUp:) forControlEvents:UIControlEventTouchUpInside | UIControlEventTouchDragOutside];
            else if (_mode==2)
              [newButton addTarget:self action:@selector(buttonUp:) forControlEvents:UIControlEventTouchUpInside ];
           
            [newButton addTarget:self action:@selector(buttonCancel:) forControlEvents:UIControlEventTouchCancel/*|UIControlEventTouchDragExit*/];
            newButton.layer.cornerRadius=2;
            newButton.layer.borderWidth=_borderThickness;
            newButton.layer.borderColor=[[UIColor whiteColor]CGColor];
            [gridButtons addObject:newButton];
            [self addSubview:newButton];
            
        }
    }
    
}

-(void)doOn:(UIButton*)button{
  button.tag = 1;
  button.backgroundColor=[self highlightColor];
  [self sendValueForButton:button];
}

-(void)doOff:(UIButton*)button{
  button.tag=0;
  button.backgroundColor=[UIColor clearColor];
  [self sendValueForButton:button];
}

- (void)sendValueForButton:(UIButton*)button{
  int objIndex=[gridButtons indexOfObject:button];
  
  [self.controlDelegate sendGUIMessageArray:[NSArray arrayWithObjects:self.address , [NSNumber numberWithInt:objIndex%dimX], [NSNumber numberWithInt:objIndex/dimX], [NSNumber numberWithInt:button.tag], nil]];
}

-(void)buttonCancel:(UIButton*)theButton{//not super elegant, but oh well...
    if(theButton.tag==1){
        theButton.backgroundColor=[self highlightColor];
    }
    else if(theButton.tag==0){
        theButton.backgroundColor=[UIColor clearColor];
    }
}


-(void)buttonDown:(UIButton*)theButton{
  
  if(_mode==0){//toggle
    if(theButton.tag==0)[self doOn:theButton];
    else [self doOff:theButton];
  }
  else if (_mode==1){//momentary, so just set on
    if(theButton.tag==0)[self doOn:theButton];
  }
  else {//hybrid, toggle
    if(theButton.tag==0)[self doOn:theButton];
    else [self doOff:theButton];
  }
}

-(void)buttonUp:(UIButton*)theButton{
  if((_mode == 1 || _mode==2) && theButton.tag==1){
    [self doOff:theButton];
  }
}


-(void)setColor:(UIColor *)inColor{
    [super setColor:inColor];
    for(UIButton* currButton in gridButtons){
        currButton.layer.borderColor=[inColor CGColor];
    }
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
  [super receiveList:inArray];
    BOOL sendVal=YES;
    //if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
    if ([inArray count]>0 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"set"]){
        NSRange newRange = (NSRange){1, [inArray count]-1};
        inArray = [inArray subarrayWithRange: newRange];
        //printf("\nset!");
        sendVal=NO;
    }

    
    //if message is three numbers, look at message and set my value, outputting value if required
    if([inArray count]==3 && [[inArray objectAtIndex:0] isKindOfClass:[NSNumber class]] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]] && [[inArray objectAtIndex:2] isKindOfClass:[NSNumber class]]){
        int indexX = (int)[[inArray objectAtIndex:0] floatValue];
        int indexY = (int)[[inArray objectAtIndex:1] floatValue];
        int val = (int)[[inArray objectAtIndex:2] floatValue];
        if(indexX<dimX && indexY<dimY && indexX>=0 && indexY>=0){
            
            UIButton* currButton = [gridButtons objectAtIndex:indexX+indexY*dimX];
            if(val>1)val=1;if(val<0)val=0;
            currButton.tag=val;
            if(val==0)currButton.backgroundColor=[UIColor clearColor];
            else currButton.backgroundColor=self.highlightColor;
            
            if(sendVal){ //use sendValueForButton?
                NSMutableArray *msgArray = [NSMutableArray arrayWithObjects:self.address, nil];
                [msgArray addObject:[NSNumber numberWithInt:indexX]];
                [msgArray addObject:[NSNumber numberWithInt:indexY]];
                [msgArray addObject:[NSNumber numberWithInt:val]];
                [PdBase sendList:msgArray toReceiver:@"fromGUI"];
            }
            
        }
    }

    //else if message starts with "getColumn", spit out array of that column's values
    if([inArray count]==2 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"getcolumn"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        int colIndex = [[inArray objectAtIndex:1] intValue];
        if(colIndex>=0 && colIndex<dimX){
        NSMutableArray *msgArray = [NSMutableArray arrayWithObjects:self.address, @"column", nil];
        for(int i=0;i<dimY;i++){
            int val = (int)[[gridButtons objectAtIndex:(colIndex+dimX*i)] tag];//0 or 1
            [msgArray addObject:[NSNumber numberWithInt:val]];
        }
        [PdBase sendList:msgArray toReceiver:@"fromGUI"];
        }
    }
    
    //else if message starts with "getRow", spit out array of that row's values
    else if([inArray count]==2 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"getrow"] && [[inArray objectAtIndex:1] isKindOfClass:[NSNumber class]]){
        int rowIndex = [[inArray objectAtIndex:1] intValue];
        if(rowIndex>=0 && rowIndex<dimY){
        NSMutableArray *msgArray = [NSMutableArray arrayWithObjects:self.address,@"row", nil];
        for(int i=0;i<dimX;i++){
            int val = (int)[[gridButtons objectAtIndex:(i+dimX*rowIndex)] tag];//0 or 1
            [msgArray addObject:[NSNumber numberWithInt:val]];
        }
        [PdBase sendList:msgArray toReceiver:@"fromGUI"];
        }
    }
    //clear
    else if([inArray count]==1 && [[inArray objectAtIndex:0] isKindOfClass:[NSString class]] && [[inArray objectAtIndex:0] isEqualToString:@"clear"]){
        for(UIButton* currButton in gridButtons){
            currButton.tag=0;
            currButton.backgroundColor=[UIColor clearColor];
        }
    }
    
}


@end
