#import <Foundation/Foundation.h>

@interface UIAlertView (MMPBlocks)

- (void)showWithCompletion:(void(^)(UIAlertView *alertView, NSInteger buttonIndex))completion;

@end
