package com.iglesiaintermedia.mobmuplat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iglesiaintermedia.LANdini.LANdiniLANManager;
import com.iglesiaintermedia.LANdini.LANdiniTimer;
import com.iglesiaintermedia.LANdini.LANdiniTimerListener;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class PingAndConnectManager {
	public static final int SC_DEFAULT_PORT = 57120;
	public static final boolean VERBOSE = false;
	private static final int SERVER_PLAYER_NUMBER = -1;
	
	private boolean _enabled;
	private NetworkController _parentNetworkController;
	private String _ipaddress;
	private int _toLocalPort = 50505;
    private int _fromLocalPort = 50506;
    private final float _broadcastInterval = 1f;
    private final float _checkUserInterval = 2f;
    private final float _dropUserInterval = 8f;
    public PingAndConnectUserStateDelegate userStateDelegate; //todo naming\
    private PingAndConnectUser _meUser;
    private int _myPlayerNumber;
    
    // bookkeeping. key: ip address string.
    private Map<String, PingAndConnectUser> _ipToUserMap;
    
    private long _startTimeMillis;
    
    OSCPortOut _broadcastAppPortOut;
    OSCPortOut _targetAppPortOut;
    OSCPortIn _localPortIn;
    OSCPortIn _networkPortIn;
	
	static class ReceiveOSCHandler extends Handler {
        private final WeakReference<PingAndConnectManager> mManagerRef; 

        ReceiveOSCHandler(PingAndConnectManager manager) {
        	mManagerRef = new WeakReference<PingAndConnectManager>(manager);
        }
        @Override
        public void handleMessage(Message msg)
        {
        	PingAndConnectManager manager = mManagerRef.get();
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
    		
        	/*if(VERBOSE) {
        		StringBuffer buff = new StringBuffer();
        		buff.append(message.getAddress()+" ");
        		for (Object obj : message.getArguments()) {
        			buff.append(obj+" ");
        		}
        		Log.i("NETWORK", "LANDINI receive osc: "+buff.toString());
        	}*/
        	
        	if (_enabled) {
        		Message msg = Message.obtain();
        		msg.obj = message;
        		mHandler.sendMessage(msg);
        	}
        }
       };
       
    private LANdiniTimer _broadcastTimer = new LANdiniTimer((int)(_broadcastInterval * 1000), new LANdiniTimerListener() {
   		@Override
   		public void onTimerFire() {
   			if(_ipaddress!=null) {
   				List<Object> msgList = Arrays.asList((Object)"/pingandconnect/ping", _ipaddress, _myPlayerNumber);
   				broadcastMsg(msgList);
   			}
   	    }
   	});
    
    List<PingAndConnectUser> usersToDropList = new ArrayList<PingAndConnectUser>();
    private LANdiniTimer _dropUserTimer = new LANdiniTimer((int)(_checkUserInterval) * 1000, new LANdiniTimerListener() {
		@Override
		public void onTimerFire() {
			usersToDropList.clear();
		    for (PingAndConnectUser user : _ipToUserMap.values()) {
	            if(getElapsedTime() - user.lastPingTime > _dropUserInterval) {
	                usersToDropList.add(user);
	            }
		    }
			if (usersToDropList.size() > 0) {
				for (PingAndConnectUser user : usersToDropList) {
					user.portOut.close();
					_ipToUserMap.remove(user.ipAddress);
					Log.i("PingAndConnectManager", "dropped "+user.ipAddress+" last ping:"+user.lastPingTime+" now:"+getElapsedTime());
				}
				updateUserState();
			}
		   
		}
	});
    
    public float getElapsedTime() {
    	return (System.currentTimeMillis() - _startTimeMillis) / 1000f;
    }
       
    public PingAndConnectManager(NetworkController nc) {
       	_parentNetworkController = nc;  
       	//_ipToOSCPortMap = new HashMap<String, OSCPortOut>();
       	//_ipToLastPingMap = new HashMap<String, Float>();
       	_ipToUserMap = new HashMap<String, PingAndConnectUser>();
       	_startTimeMillis = System.currentTimeMillis();
    }
       
	public void setEnabled(boolean enabled) {
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
	
	public void setPlayerNumber(int playerNumber) {
		_myPlayerNumber = playerNumber;
		updateUserState();
	}
	public int getPlayerNumber() {
		return _myPlayerNumber;
	}
	
	public void updateUserState() {
		//userStateDelegate.userStateChanged(_ipToOSCPortMap.keySet().toArray(new String[0])); //makes string array from key set
		if (userStateDelegate != null) {
			String[] strings = new String[_ipToUserMap.size()];
			int i=0;
			for (PingAndConnectUser user : _ipToUserMap.values()) { //generate the string for display
				String string = user.ipAddress;
				if (user.playerNumber > 0) string += (" - #"+user.playerNumber);
				else if (user.playerNumber == SERVER_PLAYER_NUMBER) string += " - server";
				if (user == _meUser) string += " - (me)";
				
				strings[i++] = string;
			}
			userStateDelegate.userStateChanged(strings);
		}
		// re-send player info into the app
		sendPlayerCountToApp();
		sendPlayerNumberSetToApp();
		sendPlayerNumberToApp();
	}
	
	private void receiveOSCMessage(OSCMessage msg) { //is this both in and out?
    	String address = msg.getAddress();
    	
    	if (address.equals("/pingandconnect/ping")) { // accept [0]:ip or [0]:ip [1]:player number
    		Object[] args = msg.getArguments();
    		if (args.length > 0 && (args[0] instanceof String)) {
    			final String ip = (String)args[0];
    			PingAndConnectUser user = null;
    			int playerNumber = 0;
				if (args.length > 1 && (args[1] instanceof Integer)) {
					playerNumber = ((Integer)args[1]).intValue();
				} else if (args.length > 1 && args[1].equals("server")) {
					playerNumber = SERVER_PLAYER_NUMBER; //tag as server
				}
    			
    			if (!_ipToUserMap.containsKey(ip)) { // ip key not in map
					
						// create new port
						//InetAddress outputIPAddress = InetAddress.getByName(ip);
						// create new user
						final PingAndConnectUser newUser = new PingAndConnectUser(ip, playerNumber); //need to pass ip?
						user = newUser;
						// create port
						new SetupUserPortFromUserTask() { 
					        protected void onPostExecute(Boolean success) {
					            if (success == true) { //note that non-ip strings still get resolved?!?!?
					            	// is it me?
									if (ip.equals(_ipaddress)) {
										_meUser = newUser;
									}
									// add to maps
									_ipToUserMap.put(ip, newUser);
									// update
									updateUserState();
									if(VERBOSE)Log.i("NETWORK","added ip "+ip);
					            }
					        }
					    }.execute(newUser);
					    
    			} else if (_ipToUserMap.get(ip).playerNumber != playerNumber){ // in map, but new player number
    				user = _ipToUserMap.get(ip);
    				user.playerNumber = playerNumber;
    				updateUserState();
    			} else { //in map, stable, need to get user in order to update its time
    				user = _ipToUserMap.get(ip);
    			}
    			// record last time seen
    			//_ipToLastPingMap.put(ip, Float.valueOf(getElapsedTime())); //includes me, necc?
    			if (user!=null) {
    				user.lastPingTime = getElapsedTime();
    			}
    		}
    	} else if (address.equals("/send")) {  //TODO test if sending a send is bad!
    		Object[] args = msg.getArguments(); // all/allbutme/index/server, user address, user args
    		if (args.length < 2 || !(args[1] instanceof String)) return;
    		Object destObj = args[0];
    		// Generate message without incoming address or destination
    		OSCMessage msg2 = new OSCMessage((String)args[1], Arrays.copyOfRange(args, 2, args.length)); //last arg exclusive
    		
    		if (destObj.equals("all")) {
    			for (PingAndConnectUser user : _ipToUserMap.values()) {
    				send(user.portOut, msg2);
    			}
    		} else if (destObj.equals("allButMe")) {
    			for (PingAndConnectUser user : _ipToUserMap.values()) {
    				if (user == _meUser) continue;
    				send(user.portOut, msg2);
    			}
    		} else { // something that signifies a destination/player number. Try to handle Integer, Float, String
    			int playerNumber = 0;
    			if (destObj.equals("server")) {
    				playerNumber = SERVER_PLAYER_NUMBER;
    			} else if (destObj instanceof Integer) { //Integer
    				playerNumber = ((Integer)destObj).intValue();
    			} else if (destObj instanceof Float) { // Float
    				playerNumber = ((Float)destObj).intValue();
    			} else if (destObj instanceof String) { //string (of an integer)
    				try {
    					playerNumber = Integer.parseInt((String)destObj);
    				} catch (NumberFormatException e) {
    					Log.e("PingAndConnect", "Could not parse a destination player number from "+destObj);
    					return;
    				}
    			}
    			
    			// iterate over users and send 
    			for (PingAndConnectUser user : _ipToUserMap.values()) { //todo optimize
    				if (user.playerNumber == playerNumber) {
    					send(user.portOut, msg2);
    				}
    			}
    		}
    	} else if (address.equals("/playerCount")) {
    		sendPlayerCountToApp();
    	} else if (address.equals("/playerNumberSet")) {
    		sendPlayerNumberSetToApp();
    	} else if (address.equals("/playerIpList")) {
    		sendPlayerIPListToApp(); 
    	} else if (address.equals("/myPlayerNumber")) {
    		sendPlayerNumberToApp();
    	} else { // normal from net, pass into app WHAIT BOTH IN AND OUT?
    		//OSCMessage  msg = NetworkController.OSCMessageFromList(msgList);
            //new SendTargetOSCTask().execute(msg); no more localhost, send direct
            _parentNetworkController.oscListener.acceptMessage(null, msg); //TODO interface/delegate
    	}
	}
	
	private void sendPlayerCountToApp() {
		List<Object> msgList = Arrays.asList((Object)"/pingAndConnect/playerCount", Integer.valueOf(_ipToUserMap.size()));
		OSCMessage msg = NetworkController.OSCMessageFromList(msgList);
		_parentNetworkController.oscListener.acceptMessage(null, msg);
	}
	
	private void sendPlayerNumberSetToApp() {
		
		// compile set
		Set<Integer> playerNumberSet = new HashSet<Integer>();
		for (PingAndConnectUser user: _ipToUserMap.values()) {
			playerNumberSet.add(Integer.valueOf(user.playerNumber));
		}
		// add set elements to list TODO sort indeces
		List<Object> msgList = new ArrayList<Object>();
		msgList.add("/pingAndConnect/playerNumberSet");
		for (Integer playerNumber : playerNumberSet) {
			msgList.add(playerNumber);
		}
		OSCMessage msg = NetworkController.OSCMessageFromList(msgList);
		_parentNetworkController.oscListener.acceptMessage(null, msg);
	}
	
	private void sendPlayerIPListToApp() {
		//TODO unimplemented
	}
	
	private void sendPlayerNumberToApp () {
		List<Object> msgList = Arrays.asList((Object)"/pingAndConnect/myPlayerNumber", Integer.valueOf(_myPlayerNumber));
		OSCMessage msg = NetworkController.OSCMessageFromList(msgList);
		_parentNetworkController.oscListener.acceptMessage(null, msg);
	}
	
	public void send(OSCPortOut port, OSCMessage msg) {
		new SendOSCTask().execute(new Object[]{port, msg});
	}
	
	private class SendOSCTask extends AsyncTask<Object, Void, Void> {
		@Override
        protected Void doInBackground(Object... objInput) {
			if (objInput.length<2)return null;
			OSCPortOut oscPortOut = (OSCPortOut)objInput[0];
            OSCMessage msg = (OSCMessage)objInput[1];
			try{
				oscPortOut.send(msg);
			} catch (IOException e) {
				 if(LANdiniLANManager.VERBOSE)Log.e("NETWORK", "Couldn't send,  "+e.getMessage());
			} catch (IllegalArgumentException e){
				if(LANdiniLANManager.VERBOSE)Log.e("NETWORK", "illegal arg exception");
			}
			
            return null;
        }
	}
	
	private void broadcastMsg(List<Object> msgList) {
		   //Log.i("broadcastMsg", ""+msgList.get(2)+" "+msgList.get(3));
	      OSCMessage msg = NetworkController.OSCMessageFromList(msgList);
	      new SendBroadcastOSCTask().execute(msg);
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
				// TODO show alert
				if(VERBOSE)Log.e("NETWORK","receiver socket exception");	
				if(VERBOSE)Log.e("NETWORK", "Unable to create OSC receiver on port 54321");//. \nI won't be able to receive messages from PD. \nPerhaps another application, or instance of this editor, is on this port.");
			} catch (IOException e) {
				if(VERBOSE)Log.e("NETWORK","receiver IO exception from multi");
			}
	    		
	        //_connectionTimer.startRepeatingTask(); //was [self checkForLAN];
	    	updateUserState(); //to send blank lists into app. necc?
	    	
	    	_ipaddress = NetworkController.getIPAddress(true); ///bad memory!
		    if(_ipaddress != null){
		    	_broadcastTimer.startRepeatingTask();
		    }
		    _dropUserTimer.startRepeatingTask();
	    }

	    private void disconnectOSC() {
	        //NSLog(@"disconnectOSC llm %p", self);
	        
	    	_broadcastTimer.stopRepeatingTask();
	    	_dropUserTimer.stopRepeatingTask();
	    	
	    	for(PingAndConnectUser user : _ipToUserMap.values()) {
	    		user.portOut.close();	
	    	}
	    	_ipToUserMap.clear();
	    	updateUserState();
	        
	        //receiveGetNamesRequest();
	        //receiveGetNumUsersRequest();
	        
	        /*if(userStateDelegate != null) {
	        	userStateDelegate.userStateChanged(_userList);
	        }*/
	        
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
				if(VERBOSE)Log.i("PingAndConnect", "send broadcast: "+msg.toString());
				try{
					_broadcastAppPortOut.send(msg);
				} catch (IOException e) {
					 if(VERBOSE)Log.e("NETWORK", "Couldn't send,  "+e.getMessage());
				}
	            return null;
	        }
		}
	    
	    private class SetupUserPortFromUserTask extends AsyncTask<PingAndConnectUser, Void, Boolean> {
			@Override
	        protected Boolean doInBackground(PingAndConnectUser... users) {
				try{
					if (users.length == 0) return false;
					PingAndConnectUser user = users[0];
					InetAddress outputIPAddress = InetAddress.getByName(user.ipAddress);
					user.portOut = new OSCPortOut(outputIPAddress, SC_DEFAULT_PORT);	
				}catch(UnknownHostException e){
					if(LANdiniLANManager.VERBOSE)Log.e("NETWORK","unknown host exception");
					return false;
				}catch(SocketException e){
					if(LANdiniLANManager.VERBOSE)Log.e("NETWORK","sender socket exception");	
					return false;
				}catch(Exception e) {
					if(LANdiniLANManager.VERBOSE)Log.e("NETWORK","generic exception:"+e.getMessage());	
					return false;
				}
				return true;
	        }
			//@Override
	        /*protected void onPostExecute(Void value) {
	        	//if(VERBOSE)Log.i("NETWORK", "completed connection task");
	        }*/
		}

}

class PingAndConnectUser {
	public String ipAddress;
	public int playerNumber; //-1 server, 0 unassigned, 1-N player number
	public OSCPortOut portOut;
	public float lastPingTime; 
	
	public PingAndConnectUser(String ipAddress, int playerNumber) {
		this.ipAddress = ipAddress;
		this.playerNumber = playerNumber;
		
	}
}