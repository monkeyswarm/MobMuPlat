//
//  AppDelegate.m
//  MobMuPlat
//
//  Created by Daniel Iglesia on 11/15/12.
//  Copyright (c) 2012 Daniel Iglesia. All rights reserved.
//

#import "AppDelegate.h"

#import "ViewController.h"
#import "ZipArchive.h"
#import "SplashViewController.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    
    //intro splash
    SplashViewController* splashController = [[SplashViewController alloc]init];
    splashController.delegate=self;
    [self.window setRootViewController:splashController];
    [splashController launchSplash];
   
    //main VC - loads and then is set as rootVC when splash is finished
    self.viewController = [[ViewController alloc] init];
    
    [self.window makeKeyAndVisible];
    
    return YES;
}

-(void)dismissSplash{
    [self.window setRootViewController:self.viewController];
}

//app opened with file
-(BOOL) application:(UIApplication *)application handleOpenURL:(NSURL *)url {
    return [self getFileFromURL:url];
}

//MobMuPlat is associated with .pd, .mmp, and .zip files. This handles importing those files into the Documents folder
-(BOOL)getFileFromURL:(NSURL*)url{
    if([url.absoluteString isEqualToString:@"MobMuPlat.audiobus://"] ||
       [url.absoluteString isEqualToString:@"MobMuPlat-v2.audiobus://"]) {
      return YES;//it sends this on connection to audiobus
    }

    NSString* filename = [[url path] lastPathComponent];
    NSString *suffix = [[filename componentsSeparatedByString:@"."] lastObject];
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *publicDocumentsDir = [paths objectAtIndex:0];
    
    //if a zip, unpack to documents, and overwrite all files with same name, then delete the zip
    if([suffix isEqualToString:@"zip"]){
        ZipArchive* za = [[ZipArchive alloc] init];
        
        if( [za UnzipOpenFile:[url path]] ) {
            if( [za UnzipFileTo:publicDocumentsDir overWrite:YES] != NO ) {
                UIAlertView *alert = [[UIAlertView alloc]
                                      initWithTitle: @"Archive Decompressed"
                                      message: [NSString stringWithFormat:@"Decompressed contents of %@ to MobMuPlat Documents", filename]
                                      delegate: nil
                                      cancelButtonTitle:@"OK"
                                      otherButtonTitles:nil];
                [alert show];
                NSError* error;
                [[NSFileManager defaultManager]removeItemAtURL:url error:&error];//delete the orig zip file
                [[self.viewController settingsVC] reloadFileTable];

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
    
    
    else{//not zip - manually overwrite file
    
        NSError *error;
        
        NSString* dstPath = [publicDocumentsDir stringByAppendingPathComponent:filename];
        if([[NSFileManager defaultManager] fileExistsAtPath:dstPath]) [[NSFileManager defaultManager] removeItemAtPath:dstPath error:&error];
        BOOL moved = [[NSFileManager defaultManager]moveItemAtURL:url toURL:[NSURL fileURLWithPath:dstPath] error:&error];
               
        if(moved){
            UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle: @"File Copied"
                          message: [NSString stringWithFormat:@"Copied %@ to MobMuPlat Documents", filename]
                          delegate: nil
                          cancelButtonTitle:@"OK"
                          otherButtonTitles:nil];
            [alert show];
            [[NSFileManager defaultManager]removeItemAtURL:url error:&error];//delete original
            [[self.viewController settingsVC] reloadFileTable];
            
        }
        else{
            UIAlertView *alert = [[UIAlertView alloc]
                                  initWithTitle: @"File not copied"
                                  message: [NSString stringWithFormat:@"Could not copy %@ to MobMuPlat Documents", filename]
                                  delegate: nil
                                  cancelButtonTitle:@"OK"
                                  otherButtonTitles:nil];
            [alert show];
        }
    }
    
        
    return YES;

}

- (void)applicationWillResignActive:(UIApplication *)application
{
// Now we are leaving these on all the time...
 //   [self.viewController disconnectPorts];//disconnect OSC ports on resign, to avoid conflicts
    
  if(![self.viewController backgroundAudioEnabled] &&
     !self.viewController.audiobusController.connected &&
     !self.viewController.audiobusController.audiobusAppRunning) {
    [[self.viewController audioController] setActive:NO];//shut down audio processing
  }
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    [[UIApplication sharedApplication] setIdleTimerDisabled:NO];
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
 // Leaving them on all the time...
 //   [self.viewController connectPorts];//reconnect OSC ports
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
    //if(![self.viewController backgroundAudioEnabled])//if we shut off audio on resign, restart it
  if(![[self.viewController audioController] isActive]) {
      [[self.viewController audioController] setActive:YES];
  }
}

- (void)applicationWillTerminate:(UIApplication *)application
{
  [[NSUserDefaults standardUserDefaults] synchronize];
  [self.viewController disconnectPorts];
}

@end
