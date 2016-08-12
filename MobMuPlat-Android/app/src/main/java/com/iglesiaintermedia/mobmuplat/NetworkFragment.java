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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class NetworkFragment extends Fragment implements SegmentedControlListener, Observer{
	private MultiDirectFragment _multidirectFragment;
	private PingAndConnectFragment _pingAndConnectFragment;
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
		_pingAndConnectFragment = new PingAndConnectFragment();
		_landiniFragment = new LandiniFragment();
		_networkController.landiniManager.userStateDelegate = (UserStateDelegate)_landiniFragment; //why cast? // MOVE TO SELF
		_networkController.pingAndConnectManager.userStateDelegate = (PingAndConnectUserStateDelegate)_pingAndConnectFragment;
		_networkController.asyncExceptionListener = _multidirectFragment; 
		
		_ssidTextView = (TextView)rootView.findViewById(R.id.textView1);
		update(null, null);//set text
		
		_seg = (SegmentedControlView)rootView.findViewById(R.id.segView1);
		_seg.setItems(new String[]{"Multicast & Direct", "Ping & Connect", "LANdini"});
		_seg.setSeethroughColor(Color.parseColor("#74CEFF"));
		_seg.segmentedControlListener = this;
		

        // Set initial fragment based on value stored in network controller.
		if (savedInstanceState == null) {
            Fragment fragment = null;
            NetworkController.NetworkSubfragmentType networkSubfragmentType =
                    _networkController.networkSubfragmentType;
            switch (networkSubfragmentType) {
                case MULTICAST_AND_DIRECT:
                    fragment = _multidirectFragment;
                    _seg.setSelectedIndex(0);
                    break;
                case PING_AND_CONNECT:
                     fragment = _pingAndConnectFragment;
                    _seg.setSelectedIndex(1);
                    break;
                case LANDINI:
                    fragment = _landiniFragment;
                    _seg.setSelectedIndex(2);
                    break;
            }
            if(fragment != null) {
                getChildFragmentManager().beginTransaction()
                        .add(R.id.container, fragment).commit();
            }
		}
		
		return rootView;
	}
	
	public void onSegmentedControlChange(SegmentedControlView segmentedControl, int sectionIndex) {
		if (sectionIndex == 0) {
			getChildFragmentManager().beginTransaction()
				.replace(R.id.container, _multidirectFragment).commit();
            _networkController.networkSubfragmentType = NetworkController.NetworkSubfragmentType.MULTICAST_AND_DIRECT;
		} else if (sectionIndex == 1) { //Ping & Connect
			getChildFragmentManager().beginTransaction()
				.replace(R.id.container, _pingAndConnectFragment).commit();
            _networkController.networkSubfragmentType = NetworkController.NetworkSubfragmentType.PING_AND_CONNECT;
		} else if (sectionIndex == 2) { //LANdini
			getChildFragmentManager().beginTransaction()
			    .replace(R.id.container, _landiniFragment).commit();
            _networkController.networkSubfragmentType = NetworkController.NetworkSubfragmentType.LANDINI;
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
	
	
	public static class PingAndConnectFragment extends Fragment implements PingAndConnectUserStateDelegate, OnItemSelectedListener {

		private ArrayAdapter<String> _adapter;
		private ListView _listView;
		private List<String> _userNamesList;
		private Switch _enablePingAndConnectSwitch;
		private NetworkController _networkController;
		
		public PingAndConnectFragment() {
			super();
			_userNamesList = new ArrayList<String>();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_pingandconnect,
					container, false);

			_networkController = ((MainActivity)getActivity()).networkController;
			_listView = (ListView)rootView.findViewById(R.id.listView1);
			_adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, _userNamesList) ;
			_listView.setAdapter(_adapter);
			_enablePingAndConnectSwitch = (Switch)rootView.findViewById(R.id.switch1);
			
			_enablePingAndConnectSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				   @Override
				   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					   _networkController.pingAndConnectManager.setEnabled(isChecked);
				   }
			});

			Spinner spinner = (Spinner) rootView.findViewById(R.id.playernumber_spinner);
			// Create an ArrayAdapter using the string array and a default spinner layout
			String[] spinnerStrings = new String[]{"server","none","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"};
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, spinnerStrings);
			// Specify the layout to use when the list of choices appears
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// Apply the adapter to the spinner
			spinner.setAdapter(adapter);
			spinner.setSelection(1); // default = 1 = "none"
			spinner.setOnItemSelectedListener(this);
			
			//set checked state in case we arriving and ping and connect is running.
			if(_networkController.pingAndConnectManager.isEnabled()) {
				_enablePingAndConnectSwitch.setChecked(true);
				//refresh my player number
				int num = _networkController.pingAndConnectManager.getPlayerNumber();
				spinner.setSelection(num+1);
				_networkController.pingAndConnectManager.updateUserState(); // send me a user update
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
		
		
		@Override
		public void userStateChanged(String[] userStringList) {
			_userNamesList.clear();
			for (String string : userStringList) {
				_userNamesList.add(string);
			}
			if (_adapter != null) {
				_adapter.notifyDataSetChanged();
			}
		}
		
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	        // An item was selected. You can retrieve the selected item using
	        // parent.getItemAtPosition(pos)
			_networkController.pingAndConnectManager.setPlayerNumber(pos-1);
			
	    }
		@Override
	    public void onNothingSelected(AdapterView<?> parent) {
	        // Another interface callback
	    }
		
	}
	
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
			} //TODO update list on arrive on view.
			
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
			if (_adapter!=null) {
				_adapter.notifyDataSetChanged();
			}
		}

		@Override
		public void syncServerChanged(String newServerName) {
			//_textView.setText(newServerName);
			_syncServerName = newServerName;
		}
		
	}

}
