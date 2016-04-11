// Based on http://nscookbook.com/2013/04/ios-programming-recipe-22-simplify-uialertview-with-blocks/

#import <objc/runtime.h>
#import "UIAlertView+MMPBlocks.h"

@interface MMPAlertWrapper : NSObject

@property (copy) void(^completionBlock)(UIAlertView *alertView, NSInteger buttonIndex);

@end

@implementation MMPAlertWrapper

#pragma mark - UIAlertViewDelegate

// Called when a button is clicked. The view will be automatically dismissed after this call returns
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
  if (self.completionBlock)
    self.completionBlock(alertView, buttonIndex);
}

// Called when we cancel a view (eg. the user clicks the Home button). This is not called when the user clicks the cancel button.
// If not defined in the delegate, we simulate a click in the cancel button
- (void)alertViewCancel:(UIAlertView *)alertView
{
  // Just simulate a cancel button click
  if (self.completionBlock)
    self.completionBlock(alertView, alertView.cancelButtonIndex);
}

@end

static const char kMMPAlertWrapper;
@implementation UIAlertView (MMPBlocks)

#pragma mark - Class Public

- (void)showWithCompletion:(void(^)(UIAlertView *alertView, NSInteger buttonIndex))completion
{
  MMPAlertWrapper *alertWrapper = [[MMPAlertWrapper alloc] init];
  alertWrapper.completionBlock = completion;
  self.delegate = alertWrapper;

  // Set the wrapper as an associated object
  objc_setAssociatedObject(self, &kMMPAlertWrapper, alertWrapper, OBJC_ASSOCIATION_RETAIN_NONATOMIC);

  // Show the alert as normal
  [self show];
}

@end