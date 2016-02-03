//
//  MePanel.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/4/13.
//  Copyright (c) 2013 Daniel Iglesia. All rights reserved.
//

#import "MePanel.h"

@implementation MePanel

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.address=@"/unnamedPanel";
        imageView = [[UIImageView alloc]initWithFrame:CGRectMake(0, 0, frame.size.width, frame.size.height)];
        [imageView setContentMode:UIViewContentModeScaleToFill];
        [self addSubview:imageView];
        
        //if image doesn't load, show this label, but keep it hidden for now
        theLabel = [[UILabel alloc]initWithFrame:CGRectMake(0, 0, frame.size.width, frame.size.height)];
        theLabel.backgroundColor=[UIColor clearColor];
        [theLabel setText:@"image file not found"];
        [theLabel setHidden:YES];
        [imageView addSubview:theLabel];
        
        //default
        [self setColor:[UIColor whiteColor]];

    }
    return self;
}

-(void)setImagePath:(NSString*)imagePath{
  if([imagePath length] == 0) return;
    _imagePath=imagePath;
    NSString* filename = [[imagePath componentsSeparatedByString:@"/"] lastObject];
    /*NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];
    NSString* docPath = [publicDocumentsDir stringByAppendingPathComponent:filename];
    if([[NSFileManager defaultManager] fileExistsAtPath:docPath])*/
    NSString *path = [self findFile:filename];
  if (path) {
         [imageView setImage:[[UIImage alloc]initWithContentsOfFile:path] ];
  } else{//file not found
        [imageView setImage:nil];
        theLabel.hidden=NO;
    }
}

// Search whole doc directory tree for file.
// TODO batch this somehow.
- (NSString *)findFile:(NSString *)filename {
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *publicDocumentsDir = [paths objectAtIndex:0];
  //NSString *path = [publicDocumentsDir stringByAppendingPathComponent:@"testlib"];

  NSDirectoryEnumerator *enumerator =
  [[NSFileManager defaultManager] enumeratorAtURL:[NSURL URLWithString:publicDocumentsDir]
                       includingPropertiesForKeys:@[]
                                          options:NSDirectoryEnumerationSkipsHiddenFiles
                                     errorHandler:nil];

  NSURL *fileURL;
  while ((fileURL = [enumerator nextObject]) != nil){
    if ([filename isEqualToString:[[fileURL path] lastPathComponent]]) {
      return [fileURL path];
    }
  }
  return nil;

}

-(void)setColor:(UIColor *)color{
    [super setColor:color];
    imageView.backgroundColor=color;
}

-(void)setShouldPassTouches:(BOOL)shouldPassTouches{
  _shouldPassTouches = shouldPassTouches;
  self.userInteractionEnabled = !shouldPassTouches;
  //theLabel.userInteractionEnabled = !shouldPassTouches;
  //imageView.userInteractionEnabled = !shouldPassTouches;
}

//receive messages from PureData (via [send toGUI]), routed from ViewController via the address to this object
-(void)receiveList:(NSArray *)inArray{
  [super receiveList:inArray];
    //image path
    if([inArray count]==2 &&[[inArray objectAtIndex:0] isEqualToString:@"image"]  ){
    NSString* newPath;
     if ([[inArray objectAtIndex:1] isKindOfClass:[NSString class]]){
         newPath = [inArray objectAtIndex:1];
        [self setImagePath:newPath];
        }
    }
    //highlight color on/off
     else if([inArray count]==2 &&[[inArray objectAtIndex:0] isEqualToString:@"highlight"]){
         int val = [[inArray objectAtIndex:1] intValue];//0,1
         if(val>0)imageView.backgroundColor=self.highlightColor;
         else imageView.backgroundColor=self.color;
     }

}


@end
