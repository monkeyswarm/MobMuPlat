package com.iglesiaintermedia.mobmuplat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;

import org.puredata.android.midi.MidiToPdAdapter;
import org.puredata.android.midi.PdToMidiAdapter;
import org.puredata.core.PdBase;
import org.puredata.core.PdMidiReceiver;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.noisepages.nettoyeur.midi.MidiReceiver;
import com.noisepages.nettoyeur.usb.ConnectionFailedException;
import com.noisepages.nettoyeur.usb.DeviceInfo;
import com.noisepages.nettoyeur.usb.DeviceNotConnectedException;
import com.noisepages.nettoyeur.usb.InterfaceNotAvailableException;
import com.noisepages.nettoyeur.usb.UsbBroadcastHandler;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiInput;
import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice.UsbMidiOutput;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiInputSelector;
import com.noisepages.nettoyeur.usb.midi.util.UsbMidiOutputSelector;
import com.noisepages.nettoyeur.usb.util.AsyncDeviceInfoLookup;
import com.noisepages.nettoyeur.usb.util.UsbDeviceSelector;
//import com.noisepages.nettoyeur.usbmididemo.UsbMidiDemo;

public class UsbMidiController extends Observable{

	private Activity _activity;
	
	private UsbMidiDevice midiDevice;
	
	public List<UsbMidiInput>midiInputList;
	public List<UsbMidiOutput>midiOutputList; 
	public List<String>midiInputStringList;
	public List<String>midiOutputStringList;
	
	//private UsbMidiDevice openMidiDevice = null;
	//private UsbMidiOutput midiOutput;
	private UsbMidiInput currMidiInput;
	private int currInputIndex = -1;
	private int currOutputIndex = -1;
	
	private MidiReceiver midiOutReceiver = null;
	//private PdToMidiAdapter pdToMidiAdapter;
	
	//ovveride to avoid block useage on output...things would hang
	private class DEIPdToMidiAdapter extends PdToMidiAdapter {
		
		public DEIPdToMidiAdapter(MidiReceiver outReceiver){
			super(outReceiver);
		}
		@Override
		public boolean beginBlock() {
			return false;
		}

		@Override
		public void endBlock() {
			
		}
	};
	
	private final MidiToPdAdapter receiver = new MidiToPdAdapter();
	
	public UsbMidiController(Activity activity) {
		super();
		_activity = activity;
		//_context = context.getApplicationContext();
		midiInputList = new ArrayList<UsbMidiInput>();
	    midiOutputList = new ArrayList<UsbMidiOutput>();
	    midiInputStringList = new ArrayList<String>();
	    midiOutputStringList = new ArrayList<String>();
	    
	   
		 
	    UsbMidiDevice.installBroadcastHandler(_activity, new UsbBroadcastHandler() {
	    	
		      @Override
		      public void onPermissionGranted(UsbDevice device) {
		        if (midiDevice == null || !midiDevice.matches(device)) return;
		        try {
		        	midiDevice.open(_activity);
		        } catch (ConnectionFailedException e) {
		          toast("USB connection failed");
		          midiDevice = null;
		          return;
		        }
		        
		        //list outputs
		        midiOutputList.clear();
		        midiInputList.clear();
		        midiInputStringList.clear();
		        midiOutputStringList.clear();
		    	  
		        for (int i = 0; i < midiDevice.getInterfaces().size(); ++i) {
		            for (int j = 0; j < midiDevice.getInterfaces().get(i).getOutputs().size(); ++j) {
		            	midiOutputList.add(midiDevice.getInterfaces().get(i).getOutputs().get(j));//("Interface " + i + ", Output " + j);
		            	midiOutputStringList.add("Interface " + i + ", Output " + j);
		            }
		            for (int j = 0; j < midiDevice.getInterfaces().get(i).getInputs().size(); ++j) {
		          	  	midiInputList.add(midiDevice.getInterfaces().get(i).getInputs().get(j));
		          	  	midiInputStringList.add("Interface " + i + ", Input " + j);
		            }
		        }
		        if(midiInputList.size()>0)connectInputAtIndex(0);
		        if(midiOutputList.size()>0)connectOutputAtIndex(0);
		        //listener notification that devices were authorized...
		        setChanged();
			    notifyObservers();
		      }
		       
		        
		      
		      @Override
		      public void onPermissionDenied(UsbDevice device) {
		        if (midiDevice == null || !midiDevice.matches(device)) return;
		        toast("Permission denied for device " + midiDevice.getCurrentDeviceInfo());
		        midiDevice = null;
		      }

		      @Override
		      public void onDeviceDetached(UsbDevice device) {
		        if (midiDevice == null || !midiDevice.matches(device)) return;
		        midiDevice.close();
		        midiDevice = null;
		        clearLists();
		        setChanged();
			    notifyObservers();
		    	
		        toast("MIDI device disconnected");
		        
		      }
		      /* nope...
		      @Override
		      public void onDeviceAttached(UsbDevice device) {
		    	  toast("MIDI device ATTACHED!!!");
		      }*/
		    });
	}
		
		
	/*public void onDeviceAttached(UsbDevice device) { //use device param? 
		// see if our midi list has changed, if so, refresh.. is this updated fast enough to catch this?
		//List<UsbMidiDevice> foundDevices = UsbMidiDevice.getMidiDevices(_activity) ;
		//
		refreshDevices(); 
	}*/
	
	
	    		
	    private void clearLists() {
	    	midiOutputList.clear();
	        midiInputList.clear();
	        midiInputStringList.clear();
	        midiOutputStringList.clear();
	    }

	
	public void refreshDevices() {//TODO does this matter activity vs application context??? //return count found
		
		clearLists();
        final List<UsbMidiDevice> devices = UsbMidiDevice.getMidiDevices(_activity);
        int deviceCount = devices.size();
        toast("Found "+deviceCount+" MIDI device(s)");
        
        if (deviceCount > 1) {
            new UsbDeviceSelector<UsbMidiDevice>(devices) {

              @Override
              protected void onDeviceSelected(UsbMidiDevice device) {
            	  midiDevice = device;
            	  midiDevice.requestPermission(_activity);
              }

              @Override
              protected void onNoSelection() {
                toast("No device selected");
              }
            }.show(_activity.getFragmentManager(), null);
        } else if (deviceCount == 1) {
        	if (devices.get(0) != midiDevice) {
        		midiDevice = devices.get(0);
        		midiDevice.requestPermission(_activity);
        	}
        } else if (deviceCount == 0) {
        	
        	setChanged();
		    notifyObservers();
        }
       
          
	}
	
	private void checkHID() {
		String TAG = "checkhid";
		UsbManager mManager = (UsbManager) _activity.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = mManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

		while (deviceIterator.hasNext())
		    {
		        UsbDevice device = deviceIterator.next();
		        Log.i(TAG,"Model: " + device.getDeviceName());
		        Log.i(TAG,"ID: " + device.getDeviceId());
		        Log.i(TAG,"Class: " + device.getDeviceClass());
		        Log.i(TAG,"Protocol: " + device.getDeviceProtocol());
		        Log.i(TAG,"Vendor ID " + device.getVendorId());
		        Log.i(TAG,"Product ID: " + device.getProductId());
		        Log.i(TAG,"Interface count: " + device.getInterfaceCount());
		        Log.i(TAG,"---------------------------------------");
		   // Get interface details
		        for (int index = 0; index < device.getInterfaceCount(); index++)
		        {
		        UsbInterface mUsbInterface = device.getInterface(index);
		        Log.i(TAG,"  *****     *****");
		        Log.i(TAG,"  Interface index: " + index);
		        Log.i(TAG,"  Interface ID: " + mUsbInterface.getId());
		        Log.i(TAG,"  Inteface class: " + mUsbInterface.getInterfaceClass());
		        Log.i(TAG,"  Interface protocol: " + mUsbInterface.getInterfaceProtocol());
		        Log.i(TAG,"  Endpoint count: " + mUsbInterface.getEndpointCount());
		    // Get endpoint details 
		            for (int epi = 0; epi < mUsbInterface.getEndpointCount(); epi++)
		        {
		            UsbEndpoint mEndpoint = mUsbInterface.getEndpoint(epi);
		            Log.i(TAG,"    ++++   ++++   ++++");
		            Log.i(TAG,"    Endpoint index: " + epi);
		            Log.i(TAG,"    Attributes: " + mEndpoint.getAttributes());
		            Log.i(TAG,"    Direction: " + mEndpoint.getDirection());
		            Log.i(TAG,"    Number: " + mEndpoint.getEndpointNumber());
		            Log.i(TAG,"    Interval: " + mEndpoint.getInterval());
		            Log.i(TAG,"    Packet size: " + mEndpoint.getMaxPacketSize());
		            Log.i(TAG,"    Type: " + mEndpoint.getType());
		        }
		        }
		    }
		    Log.i(TAG," No more devices connected.");
		}
	
	
	 private void toast(String msg) {
		 final String msg2 = msg;
		 _activity.runOnUiThread(new Runnable() {
		      @Override
		      public void run() {
		        
		    	  String TAG = "USBMIDI";
		    	  Toast toast = Toast.makeText(_activity, "", Toast.LENGTH_SHORT);
     
		    	  toast.setText(TAG + ": " + msg2);
		    	  toast.show();
		      }
		 });
	 }
	 
	 public int getCurrInputIndex() {
		 return currInputIndex;
	 }
	 
	 public int getCurrOutputIndex() {
		 return currOutputIndex;
	 }
	 
	 public void connectOutputAtIndex(int index) {
		 if (midiOutputList.size()>index) {
		        UsbMidiOutput midiOutput = midiOutputList.get(index);
		        try {
		        	midiOutReceiver = midiOutput.getMidiOut();
		            DEIPdToMidiAdapter pdToMidiAdapter = new DEIPdToMidiAdapter(midiOutReceiver); //receive from pd, out to midi
		            PdBase.setMidiReceiver(pdToMidiAdapter);
		            currOutputIndex = index;
		        } catch (DeviceNotConnectedException e) {
		        	toast("MIDI device has been disconnected");
		        	currOutputIndex = -1;
		        } catch (InterfaceNotAvailableException e) {
		              toast("MIDI interface is unavailable");
		              currOutputIndex = -1;
		        }
		 } else {
			toast("Selection out of bounds...try refreshing midi devices."); 
		 }
	 }
	 
	 public void connectInputAtIndex(int index) {
		 if(currMidiInput != null) {
			 currMidiInput.setReceiver(null);
			 currMidiInput = null;
		 }
		 if (midiInputList.size()>index) {
			 UsbMidiInput midiInput = midiInputList.get(index);
		        midiInput.setReceiver(receiver);
	            try {
	            	midiInput.start();
	            	currInputIndex = index;
	            	currMidiInput = midiInput;
	            } catch (DeviceNotConnectedException e) {
	            	toast("MIDI device has been disconnected");
	            	currInputIndex = -1;
	              //return;
	            } catch (InterfaceNotAvailableException e) {
	            	toast("MIDI interface is unavailable");
	            	currInputIndex = -1;
	            	//return;
	            }
		 } else {
			toast("Selection out of bounds...try re-detecting midi device."); 
		 }
	 }
	 
	 public void close() {
		 UsbMidiDevice.uninstallBroadcastHandler(_activity);
	 }
}
