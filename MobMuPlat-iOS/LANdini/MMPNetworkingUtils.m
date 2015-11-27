//
//  MMPNetworkingUtils.m
//  MobMuPlat
//
//  Created by diglesia on 11/25/15.
//  Copyright © 2015 Daniel Iglesia. All rights reserved.
//

#import "MMPNetworkingUtils.h"


//ip address
#import <ifaddrs.h>
#import <arpa/inet.h>

@implementation MMPNetworkingUtils

+ (NSString *)ipAddress {
  NSString *address=nil;//return nil on not found
  struct ifaddrs *interfaces = NULL;
  struct ifaddrs *temp_addr = NULL;
  int success = 0;
  // retrieve the current interfaces - returns 0 on success
  success = getifaddrs(&interfaces);
  if (success == 0) {
    // Loop through linked list of interfaces
    temp_addr = interfaces;
    while(temp_addr != NULL) {
      if(temp_addr->ifa_addr->sa_family == AF_INET) {
        // Check if interface is en0 which is the wifi connection on the iPhone
        if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
          // Get NSString from C String
          address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
          NSLog(@"found ip %@", address);
        }
      }
      temp_addr = temp_addr->ifa_next;
    }
  }
  // Free memory
  freeifaddrs(interfaces);
  NSLog(@"my ip is %@", address);
  return address;

}

@end
