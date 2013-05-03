
#import <UIKit/UIKit.h>


@interface NSString (OSCStringAdditions)

- (NSString *) trimFirstAndLastSlashes;
- (NSString *) stringByDeletingFirstPathComponent;
- (NSString *) firstPathComponent;
- (NSString *) stringBySanitizingForOSCPath;

@end
