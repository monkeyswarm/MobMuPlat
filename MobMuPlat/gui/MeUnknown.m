//
//  MeUnknown.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 1/11/14.
//  Copyright (c) 2014 Daniel Iglesia. All rights reserved.
//

#import "MeUnknown.h"

@implementation MeUnknown

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        self.backgroundColor = [UIColor darkGrayColor];
        warningLabel = [[UILabel alloc] initWithFrame:self.bounds];
        warningLabel.backgroundColor = [UIColor clearColor];
        warningLabel.textAlignment = UITextAlignmentCenter;
        warningLabel.textColor = [UIColor whiteColor];
        warningLabel.numberOfLines = -1;
        warningLabel.font = [UIFont systemFontOfSize:12];
        [self addSubview:warningLabel];
        
    }
    return self;
}

-(void)setWarning:(NSString*)badName{
    warningLabel.text = [NSString stringWithFormat:@"interface object %@ not found", badName];
}


/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    // Drawing code
}
*/

@end
