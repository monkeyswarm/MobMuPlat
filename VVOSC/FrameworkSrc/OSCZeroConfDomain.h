
#import <UIKit/UIKit.h>
//#import <VVBasics/VVBasics.h>
#import "MutLockArray.h"
#import "VVBasicMacros.h"
#import <pthread.h>
#import <sys/socket.h>
#include <arpa/inet.h>




@interface OSCZeroConfDomain : NSObject {
	NSString				*domainString;
	NSNetServiceBrowser		*serviceBrowser;
	
	MutLockArray			*servicesArray;
	
	id						domainManager;
}

+ (id) createWithDomain:(NSString *)d andDomainManager:(id)m;
- (id) initWithDomain:(NSString *)d andDomainManager:(id)m;

//	NSNetServiceBrowser delegate methods
- (void)netServiceBrowser:(NSNetServiceBrowser *)n didFindService:(NSNetService *)x moreComing:(BOOL)m;
- (void)netServiceBrowser:(NSNetServiceBrowser *)n didNotSearch:(NSDictionary *)err;
- (void)netServiceBrowser:(NSNetServiceBrowser *)n didRemoveService:(NSNetService *)s moreComing:(BOOL)m;

//	NSNetService delegate methods
- (void)netService:(NSNetService *)n didNotResolve:(NSDictionary *)err;
- (void)netServiceDidResolveAddress:(NSNetService *)n;

@end
