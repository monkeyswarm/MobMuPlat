package com.iglesiaintermedia.mobmuplat;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.http.conn.util.InetAddressUtils;

import com.iglesiaintermedia.LANdini.LANdiniLANManager;
import com.iglesiaintermedia.LANdini.LANdiniTimer;
import com.iglesiaintermedia.LANdini.LANdiniTimerListener;
import com.iglesiaintermedia.LANdini.LANdiniUser;
import com.iglesiaintermedia.LANdini.UserStateDelegate;










//import android.app.Fragment;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

public class NetworkFragment extends Fragment implements SegmentedControlListener, Observer{

	private MultiDirectFragment _multidirectFragment;
	private LandiniFragment _landiniFragment;
	private SegmentedControlView _seg;
	private NetworkController _networkController;
	private TextView _ssidTextView;
	
	// Problem with nested fragments
	// http://stackoverflow.com/questions/14929907/causing-a-java-illegalstateexception-error-no-activity-only-when-navigating-to
	// https://code.google.com/p/android/issues/detail?id=42601
	private static final Field sChildFragmentManagerField;
	static {
        Field f = null;
        try {
            f = Fragment.class.getDeclaredField("mChildFragmentManager");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.e("NETWORK", "Error getting mChildFragmentManager field", e);
        }
        sChildFragmentManagerField = f;
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_network, container,
				false);
		
		_networkController = ((MainActivity)getActivity()).networkController;
		_networkController.addObserver(this);
		
		_multidirectFragment = new MultiDirectFragment();
		_landiniFragment = new LandiniFragment();
		_networkController.landiniManager.userStateDelegate = (UserStateDelegate)_landiniFragment; //why cast?
		_networkController.asyncExceptionListener = _multidirectFragment; 
		
		_ssidTextView = (TextView)rootView.findViewById(R.id.textView1);
		update(null, null);//set text
		
		_seg = (SegmentedControlView)rootView.findViewById(R.id.segView1);
		_seg.setItems(new String[]{"Multicast & Direct", "LANdini"});
		_seg.setSeethroughColor(Color.parseColor("#74CEFF"));
		_seg.segmentedControlListener = this;
		
		//Log.i("NETWORK", "ip: "+getIPAddress(true));
		
		
		//TODO based on if landini is enabled....
		if (savedInstanceState == null) {
			getChildFragmentManager().beginTransaction()
					.add(R.id.container, _multidirectFragment).commit();
		}
		
		return rootView;
	}
	
	public void onSegmentedControlChange(SegmentedControlView segmentedControl, int sectionIndex) {
		if (sectionIndex == 0) {
			getChildFragmentManager().beginTransaction()
				.replace(R.id.container, _multidirectFragment).commit();
		} else if (sectionIndex == 1) {
			getChildFragmentManager().beginTransaction()
				.replace(R.id.container, _landiniFragment).commit();
		}
		
	}
	
	
	@Override
	public void update(Observable observable, Object data) {
		String ssidString = _networkController.getSSID();
		_ssidTextView.setText("Wifi network: "+ssidString); //(ssidString!=null ? ": "+ssidString : " disconnected"));
	}
	
	@Override
    public void onDetach() {
        super.onDetach();

        if (sChildFragmentManagerField != null) {
            try {
                sChildFragmentManagerField.set(this, null);
            } catch (Exception e) {
                Log.e("NETWORK", "Error setting mChildFragmentManager field", e);
            }
        }
        //remove self as observer?
    }
	

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class MultiDirectFragment extends Fragment implements AsyncExceptionListener{

		private TextView _deviceIPTextView;
		private EditText _outputIPEditText;
		//private EditText _multicastGroupEditText;
		private EditText _outputPortEditText;
		private EditText _inputPortEditText;
		private Button _resetMulticastButton;
		//private Button _resetMulticastGroupButton;
		private NetworkController _networkController;
		//public MultiDirectFragment() {
		//}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_multidirect,
					container, false);
			//rootView.setBackgroundColor(Color.YELLOW);
			_deviceIPTextView = (TextView)rootView.findViewById(R.id.deviceIPTextView);
			_deviceIPTextView.setText(NetworkController.getIPAddress(true));
			
			_outputIPEditText = (EditText)rootView.findViewById(R.id.outputIPEditText);
			//_multicastGroupEditText = (EditText)rootView.findViewById(R.id.multicastGroupEditText);
			_outputPortEditText = (EditText)rootView.findViewById(R.id.outputPortEditText);
			_inputPortEditText = (EditText)rootView.findViewById(R.id.inputPortEditText);
			
			_resetMulticastButton = (Button)rootView.findViewById(R.id.resetMulticastButton);
			//_resetMulticastGroupButton = (Button)rootView.findViewById(R.id.resetMulticastGroupButton);
			
			_networkController = ((MainActivity)getActivity()).networkController;
			String outputAddress = _networkController.outputIPAddressString;
			_outputIPEditText.setText(outputAddress);
			_outputIPEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			    @Override
			    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			        if (actionId == EditorInfo.IME_ACTION_DONE) { 
			        	String newIPValue = _outputIPEditText.getText().toString();
			        	Log.i("NETWORK", "get text "+newIPValue);
			        	((MainActivity)getActivity()).networkController.setOutputIPAddress(newIPValue);
			        }
			        return false;//dismiss keyboard
			    }
			});
			
			/*String multicastGroupAddress = ((MainActivity)getActivity()).networkController.multicastGroupAddressString;
			_multicastGroupEditText.setText(multicastGroupAddress);
			_multicastGroupEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			    @Override
			    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			        if (actionId == EditorInfo.IME_ACTION_DONE) { 
			        	String newValue = _multicastGroupEditText.getText().toString();//TODO sanitize here vs controller?
			        	Log.i("NETWORK", "get text "+newValue);
			        	((MainActivity)getActivity()).networkController.setMulticastGroupAddress(newValue);
			        }
			        return false;//dismiss keyboard
			    }
			});*/
			
			int outputPortNumber = ((MainActivity)getActivity()).networkController.outputPortNumber;
			_outputPortEditText.setText(""+outputPortNumber);
			_outputPortEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

			    @Override
			    public void onFocusChange(View v, boolean hasFocus) {
			    /* When focus is lost check that the text field
			    * has valid values.
			    */
			      if (!hasFocus) {
			        	//String newValue = _portEditText.getText().toString();//TODO sanitize here vs controller?
			        	try{ //TODO MAKE BETTER RESET TO DEFAULT.
			        		int newPort = Integer.parseInt(_outputPortEditText.getText().toString());
			        		((MainActivity)getActivity()).networkController.setOutputPortNumber(newPort);
			        	} catch(NumberFormatException e) {
			        		//too big
			        		receiveException(e, "Bad port number, try something 1000-65535", "port");
			        		_outputPortEditText.setText(""+((MainActivity)getActivity()).networkController.outputPortNumber);
			        	}
			        	//Log.i("NETWORK", "get text "+newPort);
			        	/*if (newPort < 1000 || newPort > 65535) {
			        		_portEditText.setText(""+((MainActivity)getActivity()).networkController.getPortNumber());
			        		return false;
			        	}*/
			        	
			        	
			        	
			        }
			        //return false;//dismiss keyboard
			    }
			});
			int inputPortNumber = ((MainActivity)getActivity()).networkController.inputPortNumber;
			_inputPortEditText.setText(""+inputPortNumber);
			_inputPortEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

			    @Override
			    public void onFocusChange(View v, boolean hasFocus) {
			    /* When focus is lost check that the text field
			    * has valid values.
			    */
			      if (!hasFocus) {
			        	//String newValue = _portEditText.getText().toString();//TODO sanitize here vs controller
			        	try{
			        		int newPort = Integer.parseInt(_inputPortEditText.getText().toString());
			        		((MainActivity)getActivity()).networkController.setInputPortNumber(newPort);
			        	} catch(NumberFormatException e) {
			        		//too big
			        		receiveException(e, "Bad port number, try something 1000-65535", "port");
			        		_inputPortEditText.setText(""+((MainActivity)getActivity()).networkController.inputPortNumber);
			        	}
			        	//Log.i("NETWORK", "get text "+newPort);
			        	/*if (newPort < 1000 || newPort > 65535) {
			        		_portEditText.setText(""+((MainActivity)getActivity()).networkController.getPortNumber());
			        		return false;
			        	}*/
			        	
			        	
			        	
			        }
			        //return false;//dismiss keyboard
			    }
			});
			
			_resetMulticastButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetIP();
				}
			});
			
			

			
			return rootView;
		}
		
		private void resetIP(){
			String address = "224.0.0.1";
			_outputIPEditText.setText(address);
			((MainActivity)getActivity()).networkController.setOutputIPAddress(address);
		}
		
		public void receiveException(Exception e, String message, String elementKey) {
			// TODO Auto-generated method stub
			showAlert(message);
				
		}
	
	
	private void showAlert(String s) {
		new AlertDialog.Builder(getActivity())
	    .setTitle("Nope")
	    .setMessage(s)
	    .setPositiveButton(android.R.string.yes, null)
	    .setIcon(R.drawable.ic_launcher)
	     .show();
	}
	}
	
	
	/*private void showBadHostAlert(String string) {
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
	
	public static class LandiniFragment extends Fragment implements UserStateDelegate {

		private ArrayAdapter<String> _adapter;
		private ListView _listView;
		private List<String> _userNamesList;
		private TextView _networkTimeTextView;
		private Switch _enableLANdiniSwitch;
		private String _syncServerName = "noSyncServer";
		private NetworkController _networkController;
		
		public LandiniFragment() {
			super();
			_userNamesList = new ArrayList<String>();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_landini,
					container, false);
			//rootView.setBackgroundColor(Color.MAGENTA);
			
			_networkController = ((MainActivity)getActivity()).networkController;
			_networkTimeTextView = (TextView)rootView.findViewById(R.id.textView2);
			_listView = (ListView)rootView.findViewById(R.id.listView1);
			_adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, _userNamesList) ;
			_listView.setAdapter(_adapter);
			_enableLANdiniSwitch = (Switch)rootView.findViewById(R.id.switch1);
			
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				//_enableLANdiniSwitch.setEnabled(false);
			}
			
			_enableLANdiniSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				   @Override
				   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					      
					if(isChecked) {
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
							   showAlert("LANdini is only available on Jelly Bean (4.1) and above");
							   return;
						   }
						_networkController.landiniManager.setEnabled(true);
						_networkTimeDisplayTimer.startRepeatingTask();
					} else {
						_networkController.landiniManager.setEnabled(false);
						_networkTimeDisplayTimer.stopRepeatingTask();
						_networkTimeTextView.setText("Network Time:");
					}
				 
				 }
			});
			//set checked state in case we arriving and landini is running.
			if(_networkController.landiniManager.isEnabled()) {
				_enableLANdiniSwitch.setChecked(true);
			}
			
			return rootView;
		}

		private void showAlert(String s) {
			new AlertDialog.Builder(getActivity())
		    .setTitle("Nope")
		    .setMessage(s)
		    .setPositiveButton(android.R.string.yes, null)
		    .setIcon(R.drawable.ic_launcher)
		     .show();
		}
		
		private LANdiniTimer _networkTimeDisplayTimer = new LANdiniTimer(250, new LANdiniTimerListener() {
			@Override
			public void onTimerFire() {
				
				float time = _networkController.landiniManager.getNetworkTime();
				_networkTimeTextView.setText("Network Time via "+ _syncServerName+": "+time);
			}
		});
		@Override
		public void userStateChanged(List<LANdiniUser> userList) {
			_userNamesList.clear();
			for (LANdiniUser user : userList) {
				_userNamesList.add(""+user.name + " "+user.ip);
			}
			_adapter.notifyDataSetChanged();
		}

		@Override
		public void syncServerChanged(String newServerName) {
			//_textView.setText(newServerName);
			_syncServerName = newServerName;
		}
		
	}

}
