package com.iglesiaintermedia.mobmuplat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import org.apache.http.conn.util.InetAddressUtils;
import org.puredata.core.PdBase;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iglesiaintermedia.LANdini.LANdiniLANManager;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class NetworkController extends Observable{
	public enum NetworkSubfragmentType {
		MULTICAST_AND_DIRECT, PING_AND_CONNECT, LANDINI
	}
	public NetworkSubfragmentType networkSubfragmentType = NetworkSubfragmentType.MULTICAST_AND_DIRECT;
	//	private static NetworkController mInstance = null;
	public MainActivity delegate; //TODO make interface?
	private OSCPortIn receiver;
	private OSCPortOut sender;
	//private OSCPortIn landiniPortIn;
	//private OSCPortOut landiniPortOut;
	public AsyncExceptionListener asyncExceptionListener = null;
	//MOre for landini!

	public String outputIPAddressString;
	//public String multicastGroupAddressString;
	public int outputPortNumber;
	public int inputPortNumber;

	static public int DEFAULT_PORT_NUMBER = 54321;

	public LANdiniLANManager landiniManager;
	public PingAndConnectManager pingAndConnectManager;
	private Activity _activity;
	
	//private String _ssid;

	final Handler mHandler = new Handler() { //make un-anonymous
    	@Override
    	public void handleMessage(Message msg) {
			//Log.d("HANDLE", String.format("Handler.handleMessage(): msg=%s", msg));
    		delegate.receiveOSCMessage((OSCMessage)msg.obj); // Passed to delegate (MainActivity)
    	}
    };
    
    public static OSCMessage OSCMessageFromList(List<Object> msgList) {
		if (msgList.get(0) instanceof String == false ) return null;
		OSCMessage msg = new OSCMessage((String)msgList.get(0), msgList.subList(1, msgList.size()).toArray()); //last arg exclusive
		return msg;
	}
    
	public OSCListener oscListener = new OSCListener() {
		//@Override
		public void acceptMessage(java.util.Date time, OSCMessage message) {
			/*System.out.println("Message received2! instance"+ this);
    		System.out.println(message.getAddress());
    		Object[] args = message.getArguments();
    		for(int i=0;i<args.length;i++)
    			System.out.print(" "+args[i]);//type Integer, Float*/
			/*for(MMPController controller:controllerArrayList){
        		controller.receiveMessage(message);
        	}*/
			//Log.i("NETWORK", "receive osc!");
			if (delegate != null){
				//delegate.receiveOSCMessage(message);
				Message msg = Message.obtain();
				msg.obj = message;
				mHandler.sendMessage(msg);
			}
		}
	};

	public NetworkController(Activity activity) {
		super();
		_activity = activity;
		setupOSC();
		landiniManager = new LANdiniLANManager(this);
		pingAndConnectManager = new PingAndConnectManager(this);
		
	}

    public void stop() { // only called if app-in-background switch is off
        pingAndConnectManager.stop();
        landiniManager.stop();
    }
    public void maybeRestart() { // called every re-foreground
        pingAndConnectManager.maybeRestart();
        landiniManager.maybeRestart();
    }

	private void setupOSC() {
		/*outputPortNumber = 54321;
		inputPortNumber = 54322;
		outputIPAddressString = "224.0.0.1";
		*/
		// get user pref numbers
		SharedPreferences sp = _activity.getPreferences(Activity.MODE_PRIVATE);	
		outputIPAddressString = sp.getString("outputIPAddress", "224.0.0.1");
		outputPortNumber = sp.getInt("outputPortNumber", 54321);
		inputPortNumber = sp.getInt("inputPortNumber", 54322);
		
		
		resetOutput();
		resetInput();	
	}
	/*
	private void showBadHostAlert(String string) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(string);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		  {
		    public void onClick(DialogInterface dialog, int id)
		    {
		      dialog.dismiss();

		    }
		  });
		AlertDialog alert = builder.create();
		alert.show();
	}*/

	/*public void setMulticastGroupAddress(String newIP) {
		multicastGroupAddressString = newIP;
		resetInput();
	}*/

	public void setOutputIPAddress(String newIP) {
		outputIPAddressString = newIP;
		resetOutput();
		
		SharedPreferences settings = _activity.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("outputIPAddress", outputIPAddressString);
		editor.commit();
	}

	
	public void setOutputPortNumber(int number) {
		/*if (number < 1000 || number > 65535) {
			return;
		}*/
		outputPortNumber = number;
		resetOutput();
		resetInput();
		
		SharedPreferences settings = _activity.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("outputPortNumber", outputPortNumber);
		editor.commit();
	}
	public void setInputPortNumber(int number) {
		/*if (number < 1000 || number > 65535) {
			return;
		}*/
		inputPortNumber = number;
		resetOutput();
		resetInput();
		
		SharedPreferences settings = _activity.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("inputPortNumber", inputPortNumber);
		editor.commit();
		
	}
	

	public void resetOutput(){
		new SetupOSCTask().execute();
	}

	public void resetInput() {
		//if (receiver!=null)receiver.close();
		try{
			receiver = new OSCPortIn(inputPortNumber); //added to multicast group 224.0.0.1
			receiver.addListener(".*", oscListener); //pattern no longer matters, hacked class to send everything to all listeners
			receiver.startListening();

		}catch(SocketException e){//not called with multicastsocket
			if(MainActivity.VERBOSE)Log.e("NETWORK","receiver socket exception");	
			if(asyncExceptionListener != null) {
				asyncExceptionListener.receiveException(e, "Unable to listen on port "+inputPortNumber+". Perhaps another application is using this port (or you are not connected to a wifi network).", "port");
			}
		} catch (IOException e) {
			if(MainActivity.VERBOSE)Log.e("NETWORK","receiver IO exception from multi");
			if(asyncExceptionListener != null) {
				asyncExceptionListener.receiveException(e, "Multicast receiver IO error. Perhaps your hardware does not support multicast.", "ip");
			}
		} catch (IllegalArgumentException e){
			if(asyncExceptionListener != null) {
				asyncExceptionListener.receiveException(e, "Bad port number, try a value between 1000 to 65535", "port");
			}
		}
	}

	public void sendMessage(Object[] args){
		new SendOSCTask().execute(args);
	}

	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces()); //lots of allocaiton here :(
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								return delim<0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			if(MainActivity.VERBOSE)Log.e("NETWORK", "failed to get my ip");
		} // for now eat exceptions
		return null;
	}

	public static String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		String serial = Build.SERIAL;
		if (model.startsWith(manufacturer)) {
			return model+" "+serial;
		} else {
			return manufacturer + " " + model+" "+serial;
		}
	}

    public void handlePDMessage(Object[] args) {
        if (args.length == 0 )return;
        if (!(args[0] instanceof String)) {
            // Network message is not starting with a string.
            Log.e("NETWORK", "Received message array does not start with a string");
            return;
        }
        String address = (String)args[0];

        //look for LANdini - this clause looks for /send, /send/GD, /send/OGD
        //String address = (String)args[0];//check for isntanceof string first
        // TODO pass through on no landini enabled?
        if(address.equals("/send") || address.equals("/send/GD") || address.equals("/send/OGD")) {
            if (landiniManager.isEnabled()) {
                //send directly, not through localhost!
                OSCMessage msg = NetworkController.OSCMessageFromList(Arrays.asList(args));
                landiniManager.oscListener.acceptMessage(null, msg);
            }
            if (pingAndConnectManager.isEnabled()) {
                // send/X becomes /send
                address = "/send";
                OSCMessage msg = NetworkController.OSCMessageFromList(Arrays.asList(args));
                pingAndConnectManager.oscListener.acceptMessage(null, msg);
            }
            /*else {
            //landini disabled: remake message without the first 2 landini elements and send out normal port
            if([list count]>2){
             NSArray* newList = [list subarrayWithRange:NSMakeRange(2, [list count]-2)];
             [outPort sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:newList]]];
            }
        }*/
        } else if (address.equals("/networkTime") || //other landini messages, keep passing to landini
                address.equals("/numUsers") ||
                address.equals("/userNames") ||
                address.equals("/myName")) {

            OSCMessage msg = NetworkController.OSCMessageFromList(Arrays.asList(args));
            landiniManager.oscListener.acceptMessage(null, msg); //DANGEROUS! responders might not be set up...
        } else if (address.equals("/playerCount") || address.equals("/playerNumberSet") || address.equals("/myPlayerNumber")) {//other ping and connect messages
            OSCMessage msg = NetworkController.OSCMessageFromList(Arrays.asList(args));
            pingAndConnectManager.oscListener.acceptMessage(null, msg);
        } else if (address.equals("/landini/enable") &&
                args.length >= 2 &&
                args[1] instanceof Float) { //set
            boolean val = ((Float)args[1]).floatValue() > 0;
            landiniManager.setEnabled(val);
        } else if (address.equals("/pingAndConnect/enable") &&
                args.length >= 2 &&
                args[1] instanceof Float) { //set
            boolean val = ((Float)args[1]).floatValue() > 0;
            pingAndConnectManager.setEnabled(val);
        } else if (address.equals("/landini/isEnabled")) { //get
            int val = landiniManager.isEnabled() ? 1 : 0;
            Object[] msgArray = new Object[]{"/landini/isEnabled", Float.valueOf(val)};
            PdBase.sendList("fromNetwork", msgArray);
        } else if (address.equals("/pingAndConnect/isEnabled")) { //get
            int val = pingAndConnectManager.isEnabled() ? 1 : 0;
            Object[] msgArray = new Object[]{"/pingAndConnect/isEnabled", Float.valueOf(val)};
            PdBase.sendList("fromNetwork", msgArray);
        } else if (address.equals("/pingAndConnect/myPlayerNumber") && args.length >= 2) {
            if ((args[1] instanceof String) && args[1].equals("server")) {
                pingAndConnectManager.setPlayerNumber(-1); //server val
            } else if (args[1] instanceof Float) {
                // no bounds/error checking!
                int val = ((Float)args[1]).intValue();
                pingAndConnectManager.setPlayerNumber(val);
            }
        } else{ //not for landini or P&C - send out regular!
            sendMessage(args);
        }
    }

	public void newSSIDData(){
		setChanged();
		notifyObservers();
	}
	
	public String getSSID() {
		WifiManager wifiManager = (WifiManager) delegate.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSSID();
	}

	private class SetupOSCTask extends AsyncTask<Void, Void, Void> {
		UnknownHostException uhe = null;
		SocketException se = null;
		@Override
		protected Void doInBackground(Void... values) {
			if (sender!=null)sender.close();
			try{
				InetAddress outputIPAddress = InetAddress.getByName(outputIPAddressString);
				sender = new OSCPortOut(outputIPAddress, outputPortNumber);	
			}catch(UnknownHostException e){
				if(MainActivity.VERBOSE)Log.e("NETWORK","unknown host exception");
				uhe = e;
			}catch(SocketException e){
				if(MainActivity.VERBOSE)Log.e("NETWORK","sender socket exception");	
				se = e;
				//JOptionPane.showMessageDialog(null, "Unable to create OSC sender on port 54300. \nI won't be able to receive messages from PD. \nPerhaps another application, or instance of this editor, is on this port.");			
			}

			return null;

		}

		@Override
		protected void onPostExecute(Void value) {
			//Log.i("NETWORK", "completed setup");
			if (uhe!=null) {
				if(asyncExceptionListener != null) {
					asyncExceptionListener.receiveException(uhe, "Unknown host. This IP address is invalid", "ip");
				}
			}
			if (se!=null) {
				if(asyncExceptionListener != null) {
					asyncExceptionListener.receiveException(se, "Unable to send on port "+outputPortNumber+".", "port");
				}
			}
		}
	}

	private class SendOSCTask extends AsyncTask<Object, Void, Void> {
		@Override
		protected Void doInBackground(Object... args) {

			if (args.length == 0 || !(args[0] instanceof String) || sender == null) return null;
			try{
				String address = (String)args[0];
				Object args2[] = Arrays.copyOfRange(args, 1, args.length); //last arg is exclusive
				OSCMessage msg = new OSCMessage(address, args2);
				sender.send(msg);
			}
			catch (IOException e) {
				Log.e("NETWORK", "Couldn't send OSC message,  "+e.getMessage());
			}

			return null;
		}

		/*
		@Override
		protected void onPostExecute(Void value) {
			
		}*/
	}
}
