package com.iglesiaintermedia.mobmuplat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Set;

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
    private List<UsbMidiDevice> devices;
    private List<UsbMidiDevice> deviceRequestQueue;

    public List<UsbMidiInput>midiInputList;
    public List<UsbMidiOutput>midiOutputList;
    public List<String>midiInputStringList;
    public List<String>midiOutputStringList;

    private Set<UsbMidiInput>_connectedMidiInputSet;
    private Set<UsbMidiOutput>_connectedMidiOutputSet;

    private MidiReceiver midiOutReceiver = null;

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
        //midiDeviceSet = new HashSet<UsbMidiDevice>();
        deviceRequestQueue = new ArrayList<UsbMidiDevice>();
        midiInputList = new ArrayList<UsbMidiInput>();
        midiOutputList = new ArrayList<UsbMidiOutput>();
        midiInputStringList = new ArrayList<String>();
        midiOutputStringList = new ArrayList<String>();
        _connectedMidiInputSet = new HashSet<UsbMidiInput>();
        _connectedMidiOutputSet = new HashSet<UsbMidiOutput>();


        UsbMidiDevice.installBroadcastHandler(_activity, new UsbBroadcastHandler() {

            @Override
            public void onPermissionGranted(UsbDevice device) {
                // find it
                //ConsoleLogController.getInstance().append("\npermission granted "+device.toString());
                UsbMidiDevice midiDevice = findUsbMidiDeviceMatching(device);
                if (midiDevice == null) {
                    requestNextDevice();
                    return; // not found
                }

                try {
                    midiDevice.open(_activity);
                } catch (ConnectionFailedException e) {
                    toast("USB connection failed");
                    //midiDevice = null;
                    // return;
                }

                //list outputs
		        /*midiOutputList.clear();
		        midiInputList.clear();
		        midiInputStringList.clear();
		        midiOutputStringList.clear();*/

                String name="";
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    name = device.getProductName();
                }
                for (int i = 0; i < midiDevice.getInterfaces().size(); ++i) {
                    for (int j = 0; j < midiDevice.getInterfaces().get(i).getOutputs().size(); ++j) {
                        midiOutputList.add(midiDevice.getInterfaces().get(i).getOutputs().get(j));//("Interface " + i + ", Output " + j);
                        midiOutputStringList.add(""+name+": Interface " + i + ", Output " + j);
                    }
                    for (int j = 0; j < midiDevice.getInterfaces().get(i).getInputs().size(); ++j) {
                        //ConsoleLogController.getInstance().append("\nfind input "+midiDevice.getInterfaces().get(i).getInputs().get(j));
                        midiInputList.add(midiDevice.getInterfaces().get(i).getInputs().get(j));
                        midiInputStringList.add(""+name+": Interface " + i + ", Input " + j);
                    }
                }
                //Connect first item
                if(midiInputList.size()>0) {
                    connectMidiInput(midiInputList.get(0));
                }
                if(midiOutputList.size()>0) {
                    connectMidiOutput(midiOutputList.get(0));
                }
                //listener notification that devices were authorized...
                setChanged();
                notifyObservers();
                requestNextDevice();
            }



            @Override
            public void onPermissionDenied(UsbDevice device) {
                UsbMidiDevice midiDevice = findUsbMidiDeviceMatching(device);
                if (midiDevice != null) {
                    //if (midiDevice == null || !midiDevice.matches(device)) return;
                    toast("Permission denied for device " + midiDevice.getCurrentDeviceInfo());
                //midiDevice = null;
                }
                requestNextDevice();
            }

            @Override
            public void onDeviceDetached(UsbDevice device) {
                UsbMidiDevice midiDevice = findUsbMidiDeviceMatching(device);
                if (midiDevice == null) return; // not found
                //if (midiDevice == null || !midiDevice.matches(device)) return;
                midiDevice.close();
                //midiDevice = null;
                clearLists();//TODO don't clear everything else on disconnect...requires tracking approved devices.
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

    public boolean isConnectedToInput(UsbMidiInput input) {
        return _connectedMidiInputSet.contains(input);
    }

    public boolean isConnectedToOutput(UsbMidiOutput output) {
        return _connectedMidiOutputSet.contains(output);
    }

    private UsbMidiDevice findUsbMidiDeviceMatching(UsbDevice usbDevice) {
        UsbMidiDevice midiDevice = null;
        if (devices != null) {
            for (UsbMidiDevice currMidiDevice : devices) {
                if (currMidiDevice.matches(usbDevice)) {
                    midiDevice = currMidiDevice;
                }
            }
        }
        return midiDevice;
    }

    private void requestNextDevice() {
        //ConsoleLogController.getInstance().append("\nrequesting device");
        if (deviceRequestQueue.size() > 0) {
            deviceRequestQueue.get(0).requestPermission(_activity);
            deviceRequestQueue.remove(0);
        }
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
        _connectedMidiInputSet.clear();
        _connectedMidiOutputSet.clear();
    }


    public void refreshDevices() {//TODO does this matter activity vs application context??? //return count found
        clearLists();
        devices = UsbMidiDevice.getMidiDevices(_activity);
        //ConsoleLogController.getInstance().append("\ncontroller refresh devices: "+devices); //DEI

        int deviceCount = devices.size();
        toast("Found "+deviceCount+" MIDI device(s)");
        
        /*if (deviceCount > 1) {
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
        }*/

        deviceRequestQueue.clear();

        for (UsbMidiDevice device : devices) {
            //HERE add to structure, then check in permission granted.
            //device.requestPermission(_activity);
            deviceRequestQueue.add(device);
        }

        requestNextDevice();

        setChanged();
        notifyObservers();



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
                    Log.i(TAG, "    Type: " + mEndpoint.getType());
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


    public void connectMidiInput(UsbMidiInput input) {
        if (input == null) return;
        try {
            input.start(); // exceptions from here
            input.setReceiver(receiver);
            _connectedMidiInputSet.add(input);
        } catch (DeviceNotConnectedException e) {
            toast("MIDI device has been disconnected");
        } catch (InterfaceNotAvailableException e) {
            toast("MIDI interface is unavailable");
        }
    }

    public void disconnectMidiInput(UsbMidiInput input) {
        if (input == null) return;
        input.stop();
        input.setReceiver(null);
        _connectedMidiInputSet.remove(input);
    }

    // TODO PdBase only has one midi receiver at a time...once multiple output is properly supported,
    // will have to set a new receiver with multi output routing
    public void connectMidiOutput(UsbMidiOutput output) {
        if (output == null) return;
        try {
            midiOutReceiver = output.getMidiOut();
            DEIPdToMidiAdapter pdToMidiAdapter = new DEIPdToMidiAdapter(midiOutReceiver); //receive from pd, out to midi
            PdBase.setMidiReceiver(pdToMidiAdapter);
            _connectedMidiOutputSet.add(output);
        } catch (DeviceNotConnectedException e) {
            toast("MIDI device has been disconnected");
        } catch (InterfaceNotAvailableException e) {
            toast("MIDI interface is unavailable");
        }
    }

    public void disconnectMidiOutput(UsbMidiOutput output) {
        if (output == null) return;
        PdBase.setMidiReceiver(null);
        _connectedMidiOutputSet.remove(output);
    }

    public void close() {
        UsbMidiDevice.uninstallBroadcastHandler(_activity);
    }
}
