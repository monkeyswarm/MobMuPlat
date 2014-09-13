package com.iglesiaintermedia.LANdini;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

public class LANdiniUser {
	
	public String name;
	public String ip;
	public int port;
	public OSCPortOut oscPortOut;//was "addr"
	public float lastPing; //match LANdiniManager elpased time, change to long?
	public LANdiniLANManager manager; //was "network"

	//guaranteed delivery vars - were readonly except for minGID
	public int lastOutgoingGDID;
	public int lastIncomingGDID;
	public Set<Integer> performedGDIDs;
	public int minGDID;
	public SparseArray<List<Object>> sentGDMsgs;

	//ordered guaranteed delivery vars - all read only
	public int lastOutgoingOGDID;
	public int lastIncomingOGDID;
	public int lastPerformedOGDID;
	public List<Integer> missingOGDIDs;
	public SparseArray<List<Object>> msgQueueForOGD;
	public SparseArray<List<Object>> sentOGDMsgs;
	
	private LANdiniTimer _newSyncServerAnnouncementTimer;
	
	public LANdiniUser(String name, String ip, int port, LANdiniLANManager manager) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.manager = manager;
		
		//GD bookkeeping
		lastOutgoingOGDID=-1;
        lastIncomingGDID=-1;
        minGDID=-1;
        performedGDIDs = new HashSet<Integer>();
        sentGDMsgs = new SparseArray<List<Object>>();
        
        //OGD bookkeeping
        lastOutgoingOGDID = -1;
        lastIncomingOGDID = -1;
        lastPerformedOGDID = -1;
        missingOGDIDs = new ArrayList<Integer>();
        msgQueueForOGD = new SparseArray<List<Object>>();
        sentOGDMsgs = new SparseArray<List<Object>>();
        
        try{
			InetAddress outputIPAddress = InetAddress.getByName(ip);
			oscPortOut = new OSCPortOut(outputIPAddress, port);	
		}catch(UnknownHostException e){
			Log.e("NETWORK","unknown host exception");
		}catch(SocketException e){
			Log.e("NETWORK","sender socket exception");	
		}		

        lastPing = manager.getElapsedTime();
       
        _newSyncServerAnnouncementTimer = new LANdiniTimer(500, new LANdiniTimerListener() {
			@Override
			public void onTimerFire() {
				//below is: addr.sendMsg('/landini/sync/new_server', network.me.name);
			    //Object[] msgArray = new Object[]{"/landini/sync/new_server", manager.me.name);
			    List<Object> msgList = Arrays.asList((Object)"/landini/sync/new_server", LANdiniUser.this.manager.me.ip);
				OSCMessage msg  = LANdiniLANManager.OSCMessageFromList(msgList);
				send(msg);   
			}
		});
        
        
	}
	
	private void resetMsgVars(){
		  lastOutgoingGDID=-1;
		  lastIncomingGDID=-1;
		  minGDID=-1;
		  lastOutgoingOGDID = -1;
		  lastIncomingOGDID = -1;
		  lastPerformedOGDID = -1;
		  performedGDIDs.clear();
		  sentGDMsgs.clear();
		  missingOGDIDs.clear();
		  msgQueueForOGD.clear();
		  sentOGDMsgs.clear();
	}
	
	//ArrayList<Integer> obsoleteGDIDs = new ArrayList<Integer>();
	public void receivePing(List<Object> vals) {
	    int lastGDIDTheySentMe = ((Integer)vals.get(2)).intValue();
	    int theirMinGD = ((Integer)vals.get(3)).intValue();
	    int lastOGDIDTheySentMe = ((Integer)vals.get(4)).intValue();
	    int lastOGDTheyPerformed = ((Integer)vals.get(5)).intValue();
	    
	    lastPing = manager.getElapsedTime();
	  
	    // check if remote user has reset itself (e.g. disconnected and reconnected before this client dropped remote user)
	    // if so, reset all the bookeeping vals
	    if(lastGDIDTheySentMe == -1 && lastOGDIDTheySentMe == -1 && (lastIncomingGDID > -1 || lastIncomingOGDID > -1)) {
	      resetMsgVars();
	    }
	    
	    //NSLog(@"receive ping %@ time %.2f", _name, _lastPing);
	    //update which msg I should be at
	    if(lastGDIDTheySentMe > lastIncomingGDID){
	        lastIncomingGDID = lastGDIDTheySentMe;
	    }
	    
	    //remove unneeded storage msg - TODO see if we can remove while iterating...
	    List<Integer> obsoleteGDIDs = new ArrayList<Integer>();
	    for (int i=0; i<sentGDMsgs.size(); i++) {
	    	Integer num = sentGDMsgs.keyAt(i);
	        if(num.intValue() < theirMinGD) {
	        	obsoleteGDIDs.add(num);
	        }
	    }
	    for(Integer num : obsoleteGDIDs) {
	    	sentGDMsgs.remove(num); 
	    }
	    
	    //request missing msgs - TODO optimize
	    List<Integer> tempMissingGDs = new ArrayList<Integer>();
	    for(int i = minGDID+1 ; i <= lastIncomingGDID ; i++) {
	        if(performedGDIDs.contains(Integer.valueOf(i)) == false) {
	            //NSLog(@"don't have %d", i);
	            tempMissingGDs.add(Integer.valueOf(i));
	        }
	    }
	    
	    if(tempMissingGDs.size() > 0) {
	    	requestMissingGDs(tempMissingGDs);
	    }
	    
	    //OGD
	    List<Integer> obsoleteOGDIDs = new ArrayList<Integer>();
	    for (int i=0; i<sentOGDMsgs.size(); i++) {
	    	Integer num = sentOGDMsgs.keyAt(i);
	        if(num.intValue() < lastOGDTheyPerformed) {
	        	obsoleteOGDIDs.add(num);
	        }
	    }
	    for(Integer num : obsoleteOGDIDs) {
	    	sentOGDMsgs.remove(num); 
	    }
	    
	    if(lastOGDIDTheySentMe > lastPerformedOGDID){
	    	List<Integer> missingIDs = new ArrayList<Integer>();
	    	for (int i=lastPerformedOGDID+1 ; i<=lastOGDIDTheySentMe ; i++) {
	    		if (msgQueueForOGD.get(i)!=null) {// if contains id number
	    			missingIDs.add(Integer.valueOf(i));
	    		}
	    	}
	    	requestMissingOGDs(missingIDs);
	    }   
	}
	
//====sync stuff

	private void startAnnouncingNewSyncServer() {
		_newSyncServerAnnouncementTimer.startRepeatingTask();
	}

	private void stopAnnouncingNewSyncServer() {
		_newSyncServerAnnouncementTimer.stopRepeatingTask();
	}
	
//===normal send methods
	
	public void sendMsg(List<Object> msgList) {//address, vals
	    //aka addr.sendMsg('/landini/msg', network.me.name, *msg);
	    List<Object> msgList2 = new ArrayList<Object>(msgList); //shallow copy
	    msgList2.add(0, "/landini/msg");
	    //msgList2.add(1, manager.me.ip);
	    msgList2.add(1, manager.me.name);
	    
	    OSCMessage msg = LANdiniLANManager.OSCMessageFromList(msgList2);
	    send(msg);
        
	   
		//log - make listener/observer
	    //[_network.logDelegate logLANdiniOutput:msgArray2];
	}

	public void receiveMsg(List<Object> msgList) {
	    //aka network.target_app_addr.sendMsg(*msg);
	    //TODO error check
	    manager.sendMsgToApp(msgList);
	}

	//GD methods

	public void sendGD(List<Object> msgList) {//address, vals
	    //optomize with ++ //just make lastOoutgoingID+=1?
	    int ID = lastOutgoingGDID+1;
	    lastOutgoingGDID=ID;
	    sentGDMsgs.put(ID, msgList);
	    
	    //aka iaddr.sendMsg('/landini/msg/GD', network.me.name, id, *msg);
	    
	    List<Object> msgList2 = new ArrayList<Object>(msgList); //shallow copy
	    msgList2.add(0, "/landini/msg/GD");
	    //msgList2.add(1, manager.me.ip);
	    msgList2.add(1,manager.me.name);
	    msgList2.add(2, Integer.valueOf(ID));
	    
	    OSCMessage msg = LANdiniLANManager.OSCMessageFromList(msgList2);
	    send(msg);
		//log/listener
	    //[_network.logDelegate logLANdiniOutput:msgArray2];
	}

	public void receiveGD(Integer idInteger, List<Object> msgList) {
		if((idInteger.intValue() > minGDID) && (performedGDIDs.contains(idInteger) == false)) {
			Set<Integer> ids = new HashSet<Integer>(performedGDIDs);//test without using temp set
			int min = minGDID;
	        
	        manager.sendMsgToApp(msgList);
	        
	        ids.add(idInteger);
	        //TODO optimize
	        while(ids.contains(Integer.valueOf(min+1))) {
	        	ids.remove(Integer.valueOf(min+1));
	        	min++;
	        }
	       
	        minGDID=min;
	        performedGDIDs=ids;
	    }
	  //[self requestMissingGDs:ids];needed? 
	}
	
	private void requestMissingGDs(List<Integer> missingGDs) {
	    // aka   addr.sendMsg('/landini/request/missing/GD', network.me.name, *missing);
	    List<Object> msgList = new ArrayList<Object>();//todo, only send when missingGDs count > 0?
	    msgList.add("/landini/request/missing/GD");
	    //msgList.add(manager.me.ip);
	    msgList.add(manager.me.name);
	    for (Integer IDInteger : missingGDs) {
	    	msgList.add(IDInteger);
	    }
	    
	    OSCMessage msg  = LANdiniLANManager.OSCMessageFromList(msgList);
	    send(msg);
	}

	public void receiveMissingGDRequest(List<Object> missingIDs) { //coming from full msgList, so <Object>, but at this point we can assume/check they are all integers
	    for (Object missedIDObject : missingIDs) {
	    	if (missedIDObject instanceof Integer == false) continue; //vs break/error
	    	Integer missedIDInteger = (Integer)missedIDObject;
	    	List<Object> msgList = sentGDMsgs.get(missedIDInteger.intValue());
	    	if (msgList != null) {
	    		// aka OSC addr.sendMsg('/landini/msg/GD', network.me.name, missedID, *msg);
	    		List<Object> msgList2 = new ArrayList<Object>();
	    	    msgList2.add("/landini/msg/GD");
	    	    //msgList2.add(manager.me.ip);
	    	    msgList2.add(manager.me.name);
	    	    msgList2.add(missedIDInteger);
	    	    for (Object obj : msgList) {
	    	    	msgList2.add(obj);
	    	    } //todo copy from msgList and insert in front
	    	    
	    	    OSCMessage msg  = LANdiniLANManager.OSCMessageFromList(msgList2);
	    	    send(msg);
	    	}
	    }
	}
	
//===OGD
	public void sendOGD(List<Object> msgList) {//address, vals
	    //optomize with ++ //just make lastOoutgoingID+=1?
	    int ID = lastOutgoingOGDID+1;
	    lastOutgoingOGDID=ID;
	    sentOGDMsgs.put(ID, msgList);
	    
	    //aka iaddr.sendMsg('/landini/msg/GD', network.me.name, id, *msg);
	    
	    List<Object> msgList2 = new ArrayList<Object>(msgList); //shallow copy
	    msgList2.add(0, "/landini/msg/OGD");
	    //msgList2.add(1, manager.me.ip);
	    msgList2.add(1,manager.me.name);
	    msgList2.add(2, Integer.valueOf(ID));
	    
	    OSCMessage msg = LANdiniLANManager.OSCMessageFromList(msgList2);
	    send(msg);
		//log/listener
	    //[_network.logDelegate logLANdiniOutput:msgArray2];
	}

	public void receiveOGD(Integer idInteger, List<Object> msgList) {
	    int ID = idInteger.intValue();
	    if(ID > lastPerformedOGDID) {
	        
	        if(ID > lastIncomingOGDID) {
	            
	            if( ID - lastIncomingOGDID > 1 ) {
	                //create array of missing IDs
	                List<Integer> missing = new ArrayList<Integer>();
	                for(int i = lastIncomingOGDID + 1 ; i <= ID ; i++) {
	                	missing.add(Integer.valueOf(i));
	                }
	                requestMissingOGDs(missing);
	            }
	            lastIncomingOGDID = ID;
	        }
	        
	        int nextID = lastPerformedOGDID + 1;
	        if(msgQueueForOGD.get(ID) == null) {
	            //msgQueueForOGD.setValueAt(ID, msgList); wrong!
	        	msgQueueForOGD.put(ID, msgList);
	        }
	        missingOGDIDs.remove(idInteger);
	        
	        List<Object> nextMsg = msgQueueForOGD.get(nextID);
	        while(nextMsg != null){
	        	manager.sendMsgToApp(nextMsg);
	            msgQueueForOGD.remove(nextID);
	            lastPerformedOGDID = nextID;
	            nextID++;
	            nextMsg = msgQueueForOGD.get(nextID);
	        }
	    }
	}

	public void requestMissingOGDs(List<Integer> missingGDs) {
	    for(Object idObject : missingGDs){
	    	if (idObject instanceof Integer == false) continue;
	    	Integer idInteger = (Integer)idObject;
	        if (missingOGDIDs.contains(idInteger)) {
	            missingOGDIDs.add(idInteger);
	        }
	    }
	    
	    if (missingOGDIDs.size() > 0) {
	        Collections.sort(missingOGDIDs);//double check that this is sorting properly!
	        // aka  addr.sendMsg('/landini/request/missing/OGD', network.me.name, *missing);
	        List<Object> msgList = new ArrayList<Object>();
	        msgList.add("/landini/request/missing/OGD");
	        //msgList.add(manager.me.ip);
	        msgList.add(manager.me.name);
	        for(Integer item : missingOGDIDs){
	            msgList.add(item);
	        }
	        
	        OSCMessage msg  = LANdiniLANManager.OSCMessageFromList(msgList);
	        send(msg);
	    }
	}

	public void receiveMissingOGDRequest(List<Object> missedIDs) {
	    for(Object missedIDObject : missedIDs){
	    	if (missedIDObject instanceof Integer == false) continue;
	    	Integer missedIDInteger = (Integer)missedIDObject;
	        List<Object> msgList =sentOGDMsgs.get(missedIDInteger.intValue());
	        if(msgList != null){
	            // aka OSC addr.sendMsg('/landini/msg/OGD', network.me.name, missedID, *msg);
	            List<Object> msgList2 = new ArrayList<Object>(); //TODO copy msgList and prepend
	            msgList2.add("/landini/msg/OGD");
	            //msgList2.add(manager.me.ip);
	            msgList2.add(manager.me.name);
	            msgList2.add(missedIDInteger);
	            for(Object item : msgList){
	            	msgList2.add(item);
	            }
	            
	            OSCMessage msg  = LANdiniLANManager.OSCMessageFromList(msgList2);
	            send(msg);
	        }
	    }
	}
	
	public void send(OSCMessage msg) {
		new SendOSCTask().execute(msg);
	}
	//
	private class SendOSCTask extends AsyncTask<OSCMessage, Void, Void> {
		@Override
        protected Void doInBackground(OSCMessage... msgInput) {
            
			if (msgInput.length == 0) return null;
			OSCMessage msg = msgInput[0];
			try{
				oscPortOut.send(msg);
			} catch (IOException e) {
				 Log.e("NETWORK", "Couldn't send,  "+e.getMessage());
			}
			
            return null;
        }
	}
}
