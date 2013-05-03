
#import <UIKit/UIKit.h>

#import "OSCZeroConfDomain.h"
#import <pthread.h>




@interface OSCZeroConfManager : NSObject {
	NSNetServiceBrowser		*domainBrowser;
	
	NSMutableDictionary		*domainDict;
	pthread_rwlock_t		domainLock;
	
	id						oscManager;
}

- (id) initWithOSCManager:(id)m;

- (void) serviceRemoved:(NSNetService *)s;
- (void) serviceResolved:(NSNetService *)s;

//	NSNetServiceBrowser delegate methods
- (void)netServiceBrowser:(NSNetServiceBrowser *)n didFindDomain:(NSString *)d moreComing:(BOOL)m;
- (void)netServiceBrowser:(NSNetServiceBrowser *)n didNotSearch:(NSDictionary *)err;

@end
