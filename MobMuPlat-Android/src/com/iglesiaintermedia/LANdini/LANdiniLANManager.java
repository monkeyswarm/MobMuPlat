package com.iglesiaintermedia.LANdini;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;

import com.iglesiaintermedia.mobmuplat.NetworkController;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class LANdiniLANManager {
	public static final boolean VERBOSE = false;
	public static final int SC_DEFAULT_PORT = 57120;
	private static boolean _initLANDispatchOnce;
	
	public LANdiniUser me;
	
	private boolean _enabled;
	
	//private boolean _connected;
    private final float _lanCheckInterval = .5f;
    private final float _dropUserInterval = 2f;
    private final float _checkUserInterval = .3f;
    private final float _broadcastInterval = 1f;
    private final float _pingInterval = .33f;
    private final float _syncRequestInterval = .33f;
    private List<LANdiniUser> _userList;
    
    private int _toLocalPort = 50505;
    private int _fromLocalPort = 50506;
    
    private float _version = .22f;
    private boolean _iHaveBeenWarned;
    
    public UserStateDelegate userStateDelegate;
    private NetworkController _parentNetworkController;
    
    private Comparator<LANdiniUser> _userNameComparator = (new Comparator<LANdiniUser>() {
        public int compare(LANdiniUser lu1, LANdiniUser lu2) {
            return lu1.name.compareTo(lu2.name);
        }
    });
     
    private LANdiniTimer _connectionTimer = new LANdiniTimer((int)(_lanCheckInterval * 1000), new LANdiniTimerListener() {
		@Override
		public void onTimerFire() {
			String address = NetworkController.getIPAddress(true); ///bad memory!
		    if(address != null){
		        _connectionTimer.stopRepeatingTask();
		        initLAN(address);
		    }
		    else{
		        if(VERBOSE)Log.i("LANDINI manager", "Still looking for a LAN...");
		    }
		}
	});
    
    private LANdiniTimer _broadcastTimer = new LANdiniTimer((int)(_broadcastInterval * 1000), new LANdiniTimerListener() {
		@Override
		public void onTimerFire() {
			if(me.ip!=null) {
				List<Object> msgList = Arrays.asList((Object)"/landini/member/broadcast", me.name, me.ip, Integer.valueOf(me.port), Float.valueOf(_version));
				broadcastMsg(msgList);
			}
	    }
	});
    
	List<LANdiniUser> usersToDropList = new ArrayList<LANdiniUser>();
    private LANdiniTimer _dropUserTimer = new LANdiniTimer((int)(_checkUserInterval) * 1000, new LANdiniTimerListener() {
		@Override
		public void onTimerFire() {
			usersToDropList.clear();
		    //for(LANdiniUser user : _userList) { //iterator bad memory??
			for (int i=0 ; i<_userList.size();i++) {
				LANdiniUser user = _userList.get(i);
					if(user != me) {
		            //NSLog(@"check user %@ last ping %.2f", user.name, user.lastPing);
		            if(getElapsedTime() - user.lastPing > _dropUserInterval) {
		                usersToDropList.add(user);
		            }
		        }
		    }
		    //for(LANdiniUser user : usersToDropList) { //iterator bad memory???
		    for (int i=0 ; i<usersToDropList.size() ; i++) {
		    	LANdiniUser user = usersToDropList.get(i);
		    	if(VERBOSE)Log.i("LANdini", "dropped user "+user.name);//"- my time %.2f userlastping time %.2f", user.name, [self elapsedTime], user.lastPing);
		        if (user.name.equals(_syncServerName)) { //LANCHANGE _syncServerIP
		            _syncTimer.stopRepeatingTask();
		            resetSyncVars();
		        }
		        _userList.remove(user);
		        //DEI edit not in original supercollider: send the user client new users list
		        receiveGetNamesRequest();
		        receiveGetNumUsersRequest();
		        if (userStateDelegate != null) {
		        	userStateDelegate.userStateChanged(_userList); //put this outside to only send once
		        }
		    }
		}
	});
    
    private LANdiniTimer _pingAndMsgIDsTimer = new LANdiniTimer((int)(_pingInterval * 1000), new LANdiniTimerListener() {
		@Override
		public void onTimerFire() {
			for(LANdiniUser user : _userList){
		        if(user != me){
		        	List<Object> msgList = 
		        			Arrays.asList((Object)"/landini/member/ping_and_msg_IDs",
		        					me.name,
		        					Integer.valueOf(0),//x position unimplemented
		        					Integer.valueOf(0),//y position unimplemented
		        					Integer.valueOf(user.lastOutgoingGDID),
		        					Integer.valueOf(user.minGDID),
		        					Integer.valueOf(user.lastOutgoingOGDID),
		        					Integer.valueOf(user.lastPerformedOGDID),
		        					_syncServerName); //LANCHANGE
		        	
						user.send(LANdiniLANManager.OSCMessageFromList(msgList));
				}
		    }
		}
	});
    
    private LANdiniTimer _syncTimer = new LANdiniTimer((int)(_syncRequestInterval * 1000), new LANdiniTimerListener() {
		@Override
		public void onTimerFire() {
			LANdiniUser server = getUserInUserListWithName(_syncServerName);//getUserInUserListWithIP(_syncServerIP);//getUserInUserListWithName(_syncServerName); LANCHANGE
			
			if(server!=null && server!=me){//should this be sent even if I am server?

				OSCMessage msg = new OSCMessage("/landini/sync/request");
				msg.addArgument(me.name);//ip
				msg.addArgument(Float.valueOf(getElapsedTime() / 1000f));
				
				server.send(msg);
			}
		}
	});
    
    private Map<String, LANdiniResponder> _apiAndNetworkResponders;
    
    private String _syncServerName;
    //private String _syncServerIP; //LANCHANGE
    boolean _inSync;
    float _smallestRtt;
    float _adjustmentToGetNetworkTime; //float seconds vs int ms? depends on how we get system time
    
    private long _startTimeMillis;
    
    OSCPortOut _broadcastAppPortOut; //was ...addr
    OSCPortOut _targetAppPortOut;
    OSCPortIn _localPortIn; //was _inPortLocal
    OSCPortIn _networkPortIn; // was _inPortNetwork
    
    static class ReceiveOSCHandler extends Handler {
        private final WeakReference<LANdiniLANManager> mManagerRef; 

        ReceiveOSCHandler(LANdiniLANManager manager) {
        	mManagerRef = new WeakReference<LANdiniLANManager>(manager);
        }
        @Override
        public void handleMessage(Message msg)
        {
        	LANdiniLANManager manager = mManagerRef.get();
             if (manager != null) {
                  manager.receiveOSCMessage((OSCMessage)msg.obj);
             }
        }
    }
    ReceiveOSCHandler mHandler = new ReceiveOSCHandler(this);
    
  //listener
    public OSCListener oscListener = new OSCListener() {
		//@Override
        public void acceptMessage(java.util.Date time, OSCMessage message) { //secondar thread from internal, called on main thread(?) from NetworkController (no localhost)
    		
        	if(VERBOSE) {
        		StringBuffer buff = new StringBuffer();
        		buff.append(message.getAddress()+" ");
        		for (Object obj : message.getArguments()) {
        			buff.append(obj+" ");
        		}
        		Log.i("NETWORK", "LANDINI receive osc: "+buff.toString());
        	}
        	//if (delegate != null) delegate.receiveOSCMessage(message);
     //   	receiveOSCMessage(message);
        	if (_enabled && _initLANDispatchOnce) {
        		Message msg = Message.obtain();
        		msg.obj = message;
        		mHandler.sendMessage(msg);
        	}
        }
       };
	
    public static OSCMessage OSCMessageFromList(List<Object> msgList) {
		if (msgList.get(0) instanceof String == false ) return null;
		OSCMessage msg = new OSCMessage((String)msgList.get(0), msgList.subList(1, msgList.size()).toArray()); //last arg exclusive
		return msg;
	}
    
    public LANdiniLANManager(NetworkController nc) {
    	_userList = new ArrayList<LANdiniUser>();
    	_startTimeMillis = System.currentTimeMillis();
    	_syncServerName = "noSyncServer";
    	_parentNetworkController = nc;
    	
        //TODO get app FG/BG calls...link to main activity? no, just implement onPause/Resume and get called
    }
    
    public void sendMsgToApp(List<Object> msgList) /*throws IOException*/ {
        OSCMessage  msg = LANdiniLANManager.OSCMessageFromList(msgList);
        //new SendTargetOSCTask().execute(msg); no more localhost, send direct
        _parentNetworkController.oscListener.acceptMessage(null, msg); //TODO interface/delegate
        
        //if([self.logDelegate respondsToSelector:@selector(logMsgInput:)] )
          //  [self.logDelegate logMsgInput:msgArray];
    }

    public void setEnabled(boolean enabled) { //HERE shut off timers
        _enabled = enabled;
        
        if(_enabled) {
            connectOSC();
        }
        else{
            disconnectOSC();
        }
    }
    
    public boolean isEnabled() {
    	return _enabled;
    }

    private void connectOSC() {
    	//outputs 
    	new SetupOSCTask().execute();
    	//inputs
    	try{
			_networkPortIn = new OSCPortIn(SC_DEFAULT_PORT); //added to multicast group 224.0.0.1
			_networkPortIn.addListener(".*", oscListener); //pattern no longer matters, hacked class to send everything to all listeners
			_networkPortIn.startListening();
			_localPortIn = new OSCPortIn(_fromLocalPort); //added to multicast group 224.0.0.1
			_localPortIn.addListener(".*", oscListener); //pattern no longer matters, hacked class to send everything to all listeners
			_localPortIn.startListening();
	  
		}catch(SocketException e){//not called with multicastsocket
			if(VERBOSE)Log.e("NETWORK","receiver socket exception");	
			if(VERBOSE)Log.e("NETWORK", "Unable to create OSC receiver on port 54321");//. \nI won't be able to receive messages from PD. \nPerhaps another application, or instance of this editor, is on this port.");
		} catch (IOException e) {
			if(VERBOSE)Log.e("NETWORK","receiver IO exception from multi");
		}
    		
        _connectionTimer.startRepeatingTask(); //was [self checkForLAN];
    }

    private void disconnectOSC() {
        //NSLog(@"disconnectOSC llm %p", self);
        
    	for(LANdiniUser user : _userList) { //necc? prob not is cleaned up immediately...
    		if(user.oscPortOut!=null) {
    			user.oscPortOut.close();
    			user.oscPortOut = null;
    		}
    	}
    	_userList.clear();
        
        receiveGetNamesRequest();
        receiveGetNumUsersRequest();
        
        if(userStateDelegate != null) {
        	userStateDelegate.userStateChanged(_userList);
        }
        
        if(_networkPortIn!=null) {
        	_networkPortIn.stopListening();
        	_networkPortIn.close();
        	_networkPortIn = null;
        }
        
        if(_localPortIn!=null) {
        	_localPortIn.stopListening();
        	_localPortIn.close();
        	_localPortIn = null;
        }
        
        if (_targetAppPortOut!=null) {
        	_targetAppPortOut.close();
        	_targetAppPortOut = null;
        }
        
        if(_broadcastAppPortOut!=null) {
        	_broadcastAppPortOut.close();
        	_broadcastAppPortOut = null;
        //need to set above to nil? yes, will prevent sending messages on closed ports.
        }
        _syncServerName = "noSyncServer";
        
        //CHECK IF THIS STILL HAPPENS
        //if backgrounded when looking for LAN, then that timer keeps firing and overlaps with its recreation to init the LAN twice, adding "me" twice
        //this attempts to prevent that
        /*if(_connectionTimer!=nil){
            [_connectionTimer invalidate];
            _connectionTimer=nil;
        }*/
    }
    
    public float getElapsedTime() {
    	return (System.currentTimeMillis() - _startTimeMillis) / 1000f;
    }
    
    private void initLAN(String address) {
    	
        if (userStateDelegate != null) {
        	userStateDelegate.syncServerChanged(_syncServerName); //"noSyncServer"
        }
        
        String myIP = address;
        int myPort = SC_DEFAULT_PORT; // supercollider default port
        String myName = NetworkController.getDeviceName();//[[UIDevice currentDevice] name];
        
        //rare cases of double adding on to/from background
//        LANdiniUser* findMe = [self userInUserListWithName:myName];
//        if(findMe){
//            [_userList removeObject:findMe];
            //NSLog(@"LANdini initLAN: redundant me");
//        }
        
        me = new LANdiniUser(myName, myIP, myPort, this);//[[LANdiniUser alloc] initWithName:myName IP:myIP port:myPort network:self];
        //_connected = true;
        _userList.add(me);
        
        if(userStateDelegate != null) {
        	userStateDelegate.userStateChanged(_userList);
        }
        receiveGetNamesRequest();
        receiveGetNumUsersRequest();
        
        //NSLog(@"connected to LAN at %@ on port %d", _me.ip, _me.port);
        //if([self.logDelegate respondsToSelector:@selector(logLANdiniOutput:)] )
          //  [self.logDelegate logLANdiniOutput:@[ [NSString stringWithFormat:@"connected to LAN at %@ on port %d", _me.ip, _me.port] ] ];
        
        
        if (_initLANDispatchOnce == false ) { //TODO can any of these be done in init, without ip address?


        	if(VERBOSE)Log.i("LANdini", "got to the api and network responders");
        	setupAPIResponders();
        	if(VERBOSE)Log.i("LANdini", "got to broadcast task");
        	_broadcastTimer.startRepeatingTask(); //startBroadcastTimer();
        	if(VERBOSE)Log.i("LANdini", "got to ping task");
        	_pingAndMsgIDsTimer.startRepeatingTask(); //startPingAndMsgIDsTimer();
        	//NSLog(@"got to show gui");
        	//this.show_gui; //GUI/Stage map currently unimplemented
//        	Handler handler = new Handler();
 //       	handler.postDelayed(new Runnable(){
 //       		@Override
 //       		public void run(){
        			if(VERBOSE)Log.i("LANdini", "starting drop user task");
        			_dropUserTimer.startRepeatingTask();//startDropUserTimer();
 //       		}
 //       	}, 3000);      
        	_initLANDispatchOnce = true;
        }
       
    }
    
    private void setupAPIResponders() { //setup both api and network responders

    	_apiAndNetworkResponders = new HashMap<String, LANdiniResponder>();
        
        //was processApiMsg:
        LANdiniResponder _processApiMsgResponder = new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				processApiMsg(msgList);
			}
        };
        
        _apiAndNetworkResponders.put("/send", _processApiMsgResponder); 
        _apiAndNetworkResponders.put("/send/GD", _processApiMsgResponder);
        _apiAndNetworkResponders.put("/send/OGD", _processApiMsgResponder);
       
        _apiAndNetworkResponders.put("/numUsers", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveGetNumUsersRequest();
			}
        });
        _apiAndNetworkResponders.put("/userNames", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveGetNamesRequest();
			}
        });
        _apiAndNetworkResponders.put("/networkTime", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveGetNetworkTimeRequest();
			}
        });
        _apiAndNetworkResponders.put("/myName", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveMyNameRequest();
			}
        });
        
        //network responders
        _apiAndNetworkResponders.put("/landini/member/broadcast", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveMemberBroadcast(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/member/reply", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveMemberReply(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/member/ping_and_msg_IDs", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receivePingAndMsgIDs(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/msg", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveMsg(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/msg/GD", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveGD(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/msg/OGD", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveOGD(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/request/missing/GD", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveMissingGDRequest(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/request/missing/OGD", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveMissingOGDRequest(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/sync/request", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveSyncRequest(msgList);
			}
        });
        
        _apiAndNetworkResponders.put("/landini/sync/reply", new LANdiniResponder() {
			@Override
			public void runCommand(List<Object> msgList) {
				receiveSyncReply(msgList);
			}
        });
        
        //NSLog(@"setup %lu new api responders", (unsigned long)[_apiResponders count]);
    }
    
    //
    
    private void receiveMyNameRequest() {
        //output message format:
        // '/landini/myName', me.name
        sendMsgToApp(Arrays.asList( (Object)"/landini/myName", me.name)); //cast to specify List<T> as object, not List<String>
    }

    private void receiveGetNamesRequest() {
        //output message format:
        // '/landini/userNames, <list of user names>...
        
        //DEI edit - difference from super collider version:
        //this returns a list of _all_ users (including me), not all but me
        
    	List<Object> msgList = new ArrayList<Object>();
    	msgList.add("/landini/userNames");
    	for (LANdiniUser user : _userList) {
    		msgList.add(user.name);
    	}
    	
        sendMsgToApp(msgList);
    }

    public void receiveGetNumUsersRequest() {
        //output message format:
        // '/landini/numUsers', <number of users in userlist>
        sendMsgToApp(Arrays.asList((Object)"/landini/numUsers", Integer.valueOf(_userList.size())));
    }

    public void receiveGetNetworkTimeRequest() {
        //output message format:
        // '/landini/networkTime, <double: time elapsed in seconds>
        
        float time;
        if(_inSync){
        	time = getNetworkTime();
        }
        else time=-1;
        sendMsgToApp(Arrays.asList((Object)"/landini/networkTime", Float.valueOf(time)));    
    }
    
    public void processApiMsg(List<Object>msgList) {
        //NSLog(@"process: %@", msgArray);
        //input message format:
        // protocol ("/send, /sendGD, /sendOGD"),
        // name of recipient (or "all"/"allButMe"),
        // user msg address
        // user msg values......
        
        //check first two are strings
    	if( msgList.size()<3 || (msgList.get(0) instanceof String) == false || (msgList.get(1) instanceof String) == false) {
    		return;//error?
    	}
        
    	String protocol = (String)msgList.get(0);
    	String name = (String)msgList.get(1); //LANDini User name or "all"/"allButMe"
    	
        List<Object> msgList2 = msgList.subList(2, msgList.size()); //strip off first two elements, leaving user message address and values
    	
        if(name.equals("all")) {
            if(protocol.equals("/send")) {
                for (LANdiniUser user : _userList) {
                	sendUser(user, msgList2);
                }
            }
            else if(protocol.equals("/send/GD")) {
                for (LANdiniUser user : _userList) {
                	sendGDUser(user, msgList2);
                }
            }
            else if(protocol.equals("/send/OGD")){
                for (LANdiniUser user : _userList){
                	sendOGDUser(user, msgList2);
                }
            }
        } else if(name.equals("allButMe")) {
            if (protocol.equals("/send")) {
                for (LANdiniUser user : _userList) {
                    if (user!=me) {
                    	sendUser(user, msgList2);
                    }
                }
            }
            else if(protocol.equals("/send/GD")){
                for(LANdiniUser user : _userList){
                    if (user!=me) {
                    	sendGDUser(user, msgList2);
                    }
                }
            }
            if(protocol.equals("/send/OGD")){
                for(LANdiniUser user : _userList){
                    if(user!=me) {
                    	sendOGDUser(user, msgList2);
                    }
                }
            }
        } else { //user name
            LANdiniUser user = getUserInUserListWithName(name); //LANCHANGE ok for now, return multiple
            if (user == null) {
                //NSLog(@"invalid user name");
                //if ([self.logDelegate respondsToSelector:@selector(logLANdiniOutput:)] )
                  //  [self.logDelegate logLANdiniOutput:@[ [NSString stringWithFormat:@"error: sending to bad user name %@", name] ] ];
                return;
            }
            
            if(protocol.equals("/send")) {
            	sendUser(user, msgList2);
            } else if(protocol.equals("/send/GD")) {
            	sendGDUser(user, msgList2);
            } else if(protocol.equals("/send/OGD")) {
            	sendOGDUser(user, msgList2);
            }
        }
    }

    private void receiveOSCMessage(OSCMessage msg) {
    	String address = msg.getAddress();
    	
    	LANdiniResponder responder = _apiAndNetworkResponders.get(address);
    	List<Object> msgList = new ArrayList<Object>();
    	msgList.add(address);  //first element in msgArray is address
    	for (Object obj : msg.getArguments()) {
    		msgList.add(obj);
    	}
    	responder.runCommand(msgList);
    	
        //if([self.logDelegate respondsToSelector:@selector(logLANdiniInput:)] )
          //  [self.logDelegate logLANdiniInput:msgArray];
    }
    
 // broadcast stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

   private void broadcastMsg(List<Object> msgList) {
	   //Log.i("broadcastMsg", ""+msgList.get(2)+" "+msgList.get(3));
       OSCMessage msg = LANdiniLANManager.OSCMessageFromList(msgList);
       new SendBroadcastOSCTask().execute(msg);
	   
        //if([self.logDelegate respondsToSelector:@selector(logLANdiniOutput:)] )
          // [self.logDelegate logLANdiniOutput:msgArray];
    }
    
   private void receiveMemberBroadcast(List<Object> msgList) {//address, name, ip, port, version
	    //input message format:
	    //address ("/landini/member/broadcast/")
	    //member name
	    //member IP (NSString)
	    //member port (NSNumber)
	   // version
	   
	    //check types
	    if(msgList.size() !=5 ||
	    		(msgList.get(0) instanceof String)==false ||
	    		(msgList.get(1) instanceof String)==false ||
	    		(msgList.get(2) instanceof String)==false ||
	    		(msgList.get(3) instanceof Integer)==false ||	
	    		(msgList.get(4) instanceof Float)==false ) {
	    	return;
	    }

	    String theirName = (String)msgList.get(1);
	    String theirIP = (String)msgList.get(2);
	    int theirPort = ((Integer)msgList.get(3)).intValue();
	    float theirVersion = ((Float)msgList.get(4)).floatValue();
	    
	    if(VERBOSE)Log.i("LAN", "receive member b: "+theirIP+" "+theirPort+" "+theirName);
	       
	    
	    if((theirVersion > _version+.001) && (_iHaveBeenWarned == false)){
	        _iHaveBeenWarned = true;
	        /*UIAlertView *alert = [[UIAlertView alloc]
	                              initWithTitle: @"Warning"
	                              message: [NSString stringWithFormat:@"You are not using the latest version of LANdini.\nYou are using version %.2f,\nand someone else is using version %.2f", _version, theirVersion]
	                              delegate: nil
	                              cancelButtonTitle:@"OK"
	                              otherButtonTitles:nil];
	        dispatch_async(dispatch_get_main_queue(), ^{
	            [alert show];
	        });*/
	    }
	    
	    List<Object> replyList = new ArrayList<Object>();
	    replyList.add("/landini/member/reply");
	    
	    LANdiniUser fromUser = null;
	    
	    if(theirIP.equals(me.ip) == false) {
	        //optimize
	        for(LANdiniUser user : _userList){
	            if(user.ip.equals(theirIP)) fromUser = user;
	        }
	        if(fromUser == null) {//no user found, create and add to user list
	            fromUser = assimilateMemberInfoName(theirName, theirIP, theirPort);
	        }
	       if (fromUser == null){
	    	   if(VERBOSE)Log.e("LAN", "mysteriosuly couldn't add "+theirName);
	       }
	      //sending just my info STILL NEEDED???
	       /* [replyMsg addObject:_me.name];
	        [replyMsg addObject:_me.ip];
	        [replyMsg addObject:[NSNumber numberWithInt:_me.port]];
	        [[fromUsr addr] sendThisPacket:[OSCPacket createWithContent:[LANdiniLANManager OSCMessageFromArray:replyMsg]]];
	        */
	    }
	}


	private void receiveMemberReply(List<Object> msgList) {
	    //input message format:
	    //landini address
	    //list of triplets of user data (name, IP, port, name, IP, port,...)
	    
	/*    if(([addrs count]-1)/3 != [_userList count]){//if doesn't match our current user list size
	        
	        for(int i=1;i<[addrs count];i+=3){
	            NSString* newName = (NSString*)[addrs objectAtIndex:i];
	            NSString* newIP = (NSString*)[addrs objectAtIndex:i+1];
	            int newPort = [(NSNumber*)[addrs objectAtIndex:i+2] intValue];
	            
	            [self assimilateMemberInfoName:newName IP:newIP port:newPort];
	        }
	    }*/ //still needed??
	}
   

// ping and msg ID stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void receivePingAndMsgIDs(List<Object> msgList) {
	    //input format:
	    //  landini address (NSString)
	    //  name LANCHANGE will be IP name (NSString)
	    //  xpos (NSNumber)
	    //  ypos (NSNumber)
	    //  lastGDID (NSNumber)
	    //  theirMinGD (NSNumber)
	    //  lastOGDID (NSNumber)
	    //  lastPerformedOGDID (NSNumber)
	    //  syncServerPingName (NSString)
	    
	    //if([msgArray count]!=9)return;
	    
	    String name = (String)msgList.get(1);
		//String ip = (String)msgList.get(1);
	    //NSLog(@"receivePingAndMsg from %@", name);
	    LANdiniUser user = getUserInUserListWithName(name); //here: insert IP address in message input?//LANCHANGE
//	    LANdiniUser user = getUserInUserListWithIP(ip);
	    if(user != null){//found user
	        //output format to receivePing method:user info
	        List<Object> userList = msgList.subList(2, msgList.size());//xpos, ypos, ID values...
	        user.receivePing(userList);
	    }
	    
	    String syncServerPingName = (String)msgList.get(8);
	    if( syncServerPingName.equals("noSyncServer") == true || 
	    		syncServerPingName.equals(_syncServerName) == false) {
	        dealWithNewSyncServerName(syncServerPingName);
	    }
	}
   
// group stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private LANdiniUser assimilateMemberInfoName(String name, String ip, int port) {
	    LANdiniUser user = null;
	    //DEI edit not in original supercollider source: check for existence of user in list by name
	    user = getUserInUserListWithName(name); //getUserInUserListWithIP(ip); //LANCHANGE
	    if(user == null){//not found
	        //end DEI edit
	    	user = new LANdiniUser(name, ip, port, this);
	        _userList.add(user);
	        if(VERBOSE)Log.i("LANdini", "added user "+user.name);
	        
	        //DEI edit not in original supercollider source:send new message to user app with names
	        receiveGetNamesRequest();
	        receiveGetNumUsersRequest();
	        if(userStateDelegate != null) {
	        	userStateDelegate.userStateChanged(_userList);
	        //end DEI edit
	        }
	    }
	    return user;
	}

	// sync stuff - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void becomeSyncServer() {
	    if(VERBOSE)Log.i("LANdini", "becoming sync server!");
	    _syncTimer.stopRepeatingTask();
	    //_syncServerIP = me.ip;
	    _syncServerName = me.name;
	    _adjustmentToGetNetworkTime = 0;
	    _inSync = true;
	    
	    if(userStateDelegate != null) {
        	userStateDelegate.syncServerChanged(me.name);
	    }
	}


	private void dealWithNewSyncServerName(String newName) {
	//private void dealWithNewSyncServerIP(String ip) {    //LANCHANGE whole method
	    //if(ip.equals("noSyncServer")) {
		if(newName.equals("noSyncServer")) {
	        //NSLog(@"pinged sync server name is noSyncServer");
/*	        List<String> namesList = new ArrayList<String>();
	    	for(LANdiniUser user : _userList){
	    		namesList.add(user.name);
	        }
	*/        
	        Collections.sort(_userList, _userNameComparator); //LANCHANGE edited out above, and just work on userlist
	        //if(verbose)Log.i("LANdini", "here's allNames: "+ namesList.toString());
	        if(_userList.size() > 0 && _userList.get(0) == me) {
	            //if(_syncServerIP.equals(me.ip) == false){
	        	if (_syncServerName.equals(me.name) == false) {
	                becomeSyncServer();
	            }
	            else{
	                if(VERBOSE)Log.i("LANdini", "i am already the sync server");
	            }
	        }
	    } else {
	        LANdiniUser user = null;
	        user = getUserInUserListWithName(newName); //getUserInUserListWithIP(ip); LANCHANGE
	        if(user != null) {//if found
	            //_syncServerIP = ip;
	            _syncServerName = newName;
	            if(userStateDelegate != null) {
	            	userStateDelegate.syncServerChanged(user.name);
	            }
	        }
	        
	        //[self startSyncTimer];
	        //[self performSelectorOnMainThread:@selector(startSyncTimer) withObject:nil waitUntilDone:NO];
	        _syncTimer.startRepeatingTask();
	    }
	}

	private void resetSyncVars() {
	    _adjustmentToGetNetworkTime = 0;
	    _inSync = false;
	    //_syncServerIP = "noSyncServer";
	    _syncServerName = "noSyncServer";
	    _smallestRtt = 1;
	    if(userStateDelegate != null) {
        	userStateDelegate.syncServerChanged(_syncServerName); //make ip
	    }
	}

	//i get this when I am the time server, I should not be getting messages from me
	private void receiveSyncRequest(List<Object> msgList) {
	    //input format:
	    // landini address
	    // user IP //LANCHANGE
	    // user time
	   
	    if ( msgList.size() !=3 ||
	    		(msgList.get(1) instanceof String) == false ||
	    		(msgList.get(2) instanceof Float) == false ) { //double check it is float
	    	return;
	    }
	    
	    //String theirIP = (String)msgList.get(1);
	    String theirName = (String)msgList.get(1);
	    Float theirTimeFloat = (Float)msgList.get(2);
	    
	    LANdiniUser user = getUserInUserListWithName(theirName);//getUserInUserListWithIP(theirIP);
	    if(user != null) {
	        if(user.name.equals(me.name) == false) { //LANCHANGE
	            List<Object> msgList2 =
	            		Arrays.asList((Object) "/landini/sync/reply",
	                             me.name, //ip
	                             theirTimeFloat,
	                             Float.valueOf(getElapsedTime()) );
	        
	            OSCMessage msg = LANdiniLANManager.OSCMessageFromList(msgList2);
				user.send(msg);
	        } else{
	            if(VERBOSE)Log.i("LANdini", "i should not be sending myself sync requests");
	            _syncTimer.stopRepeatingTask();
	        }
	    } else{
	    	if(VERBOSE)Log.i("LANdini", "time server is not in the userlist");
	       _syncTimer.stopRepeatingTask();
	        resetSyncVars();
	    }
	}


	private void receiveSyncReply(List<Object> msgList) {//address, etc
	    //TimeServerName:(NSString*)timeServerName myOldTime:(NSTimeInterval)myOldTime timeServertime:(NSTimeInterval)timeServerTime{
	    String timeServerName = (String)msgList.get(1);// UP
	    float myOldTime = ((Float)msgList.get(2)).floatValue(); //make a double??? what comes from oscmessage?
	    float timeServerTime = ((Float)msgList.get(3)).floatValue();

	    LANdiniUser user = getUserInUserListWithName(timeServerName);
	    
	    if(user!=null && timeServerName.equals(_syncServerName)) {
	        _inSync = true;
	        float now = getElapsedTime();
	        float rtt = now-myOldTime;
	        _smallestRtt = Math.min(_smallestRtt, rtt);
	        float serverTime = timeServerTime + (_smallestRtt/2);
	        _adjustmentToGetNetworkTime = serverTime - now;
	    }
	    else{
	        if(VERBOSE)Log.i("LANdini", "stopping sync task because of sync server name discrepancy");
	        _syncTimer.stopRepeatingTask();
	        resetSyncVars();
	    }
	}

//===normal send methods

	private void sendUser(LANdiniUser user, List<Object> msgList) {
	    if(user != null){
	        user.sendMsg(msgList);
	    }
	}

	private void receiveMsg(List<Object> msgList) {//api address, user IP LANCHANGE, user msg addess, user args...
	    //String fromIP = (String)msgList.get(1);//todo checking
		String fromName = (String)msgList.get(1);
		LANdiniUser user = getUserInUserListWithName(fromName);
		
	    if(user != null){
	        user.receiveMsg(msgList.subList(2, msgList.size())); //strip from name
	    }
	}

	//====GD methods

	private void sendGDUser(LANdiniUser user, List<Object> msgList) { //TODO should match others, i.e. sendGDUser
	    if(user != null){
	        user.sendGD(msgList);
	    }
	}


	private void receiveGD(List<Object> msgList) {// landini network api address, from user name, ID, user address, user vals
		//String fromIP = (String)msgList.get(1);//todo checking string, integer
		String fromName = (String)msgList.get(1);
		LANdiniUser user = getUserInUserListWithName(fromName);
		
	    if(user != null) {
	        Integer idInteger = (Integer)msgList.get(2);
	        user.receiveGD(idInteger, msgList.subList(3, msgList.size()));
	    }
	}


	private void receiveMissingGDRequest(List<Object> msgList) {
		//String fromIP = (String)msgList.get(1);//todo checking string, integer
		String fromName = (String)msgList.get(1);
		LANdiniUser user = getUserInUserListWithName(fromName);
		
	    if(user != null){
	        user.receiveMissingGDRequest(msgList.subList(2, msgList.size()));
	    }
	}

//OGD

	private void sendOGDUser(LANdiniUser user, List<Object> msgList) {
	    if(user != null){
	        user.sendOGD(msgList);
	    }
	}

	private void receiveOGD(List<Object> msgList) {
		//String fromIP = (String)msgList.get(1);//todo checking string, integer
		String fromName = (String)msgList.get(1);
		LANdiniUser user = getUserInUserListWithName(fromName);
	    
	    if (user != null) {
	    	Integer idInteger = (Integer)msgList.get(2);
	        user.receiveOGD(idInteger, msgList.subList(3, msgList.size()));
	    }
	}

	private void receiveMissingOGDRequest(List<Object> msgList) {
		//String fromIP = (String)msgList.get(1);//todo checking string, integer
	    String fromName = (String)msgList.get(1);
		LANdiniUser user = getUserInUserListWithName(fromName);
	    
	    if(user != null) {
	        user.receiveMissingOGDRequest(msgList.subList(2, msgList.size()));
	    }
	    
	}

//=== utility
	private LANdiniUser getUserInUserListWithName(String userName) { //BAD comp
	    LANdiniUser user = null;
	    for(LANdiniUser currUser : _userList){
	        if(userName.equals(currUser.name)){ //return list of users with name?
	            user = currUser;
	            break;
	        }
	    }
	    return user;
	}
	
	/*private LANdiniUser getUserInUserListWithIP(String ip) {
		LANdiniUser user = null;
	    for(LANdiniUser currUser : _userList){
	        if(ip.equals(currUser.ip)){ 
	            user = currUser;
	            break;
	        }
	    }
	    return user;
	}*/
	    
	//
    public float getNetworkTime() {
        //return [self elapsedTime] + _adjustmentToGetNetworkTime;
    	return getElapsedTime() + _adjustmentToGetNetworkTime;
   }
    
    private class SetupOSCTask extends AsyncTask<Void, Void, Void> {
		@Override
        protected Void doInBackground(Void... values) {
			if (_targetAppPortOut!=null) {
				_targetAppPortOut.close();
			}
			if (_broadcastAppPortOut!=null) {
				_broadcastAppPortOut.close();
			}
			try{
				InetAddress outputIPAddress = InetAddress.getByName("127.0.0.1");
				_targetAppPortOut = new OSCPortOut(outputIPAddress, _toLocalPort);	
				InetAddress outputIPAddress2 = InetAddress.getByName("224.0.0.1");
				_broadcastAppPortOut = new OSCPortOut(outputIPAddress2, SC_DEFAULT_PORT);
			}catch(UnknownHostException e){
				if(VERBOSE)Log.e("NETWORK","unknown host exception");
			}catch(SocketException e){
				if(VERBOSE)Log.e("NETWORK","sender socket exception");	
				//JOptionPane.showMessageDialog(null, "Unable to create OSC sender on port 54300. \nI won't be able to receive messages from PD. \nPerhaps another application, or instance of this editor, is on this port.");			
			}
			
            return null;
            
        }

        @Override
        protected void onPostExecute(Void value) {
        	if(VERBOSE)Log.i("NETWORK", "completed connection task");
        }
	}
    
    private class SendBroadcastOSCTask extends AsyncTask<OSCMessage, Void, Void> { //TODO shut this off on disable!
		
    	@Override
        protected Void doInBackground(OSCMessage... msgInput) {
			if (msgInput.length == 0 || _broadcastAppPortOut == null) return null;
			OSCMessage msg = msgInput[0];
			if(VERBOSE)Log.i("LANdini", "send broadcast: "+msg.toString());
			try{
				_broadcastAppPortOut.send(msg);
			} catch (IOException e) {
				 if(VERBOSE)Log.e("NETWORK", "Couldn't send,  "+e.getMessage());
			}
            return null;
        }
	}
    
    /*private class SendTargetOSCTask extends AsyncTask<OSCMessage, Void, Void> {
		@Override
        protected Void doInBackground(OSCMessage... msgInput) {
			if (msgInput.length == 0) return null;
			OSCMessage msg = msgInput[0];
			try{
				_targetAppPortOut.send(msg);
			} catch (IOException e) {
				 if(VERBOSE)Log.e("NETWORK", "Couldn't send,  "+e.getMessage());
			}
            return null;
        }
	}*/
    
	
}

interface LANdiniResponder {
    //void runCommand();
    void runCommand(List<Object> msgList);
}
