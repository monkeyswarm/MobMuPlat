package com.iglesiaintermedia.mobmuplat;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
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

public class NetworkFragment extends Fragment implements SegmentedControlListener{

	private MultiDirectFragment _multidirectFragment;
	private LandiniFragment _landiniFragment;
	private SegmentedControlView _seg;
	private NetworkController _networkController;
	
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
		
		_multidirectFragment = new MultiDirectFragment();
		_multidirectFragment.setNetworkController(_networkController);
		_landiniFragment = new LandiniFragment();
		_landiniFragment.setNetworkController(_networkController);
		_networkController.landiniManager.userStateDelegate = (UserStateDelegate)_landiniFragment; //why cast?
		_networkController.asyncExceptionListener = _multidirectFragment; 
		
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
    public void onDetach() {
        super.onDetach();

        if (sChildFragmentManagerField != null) {
            try {
                sChildFragmentManagerField.set(this, null);
            } catch (Exception e) {
                Log.e("NETWORK", "Error setting mChildFragmentManager field", e);
            }
        }
    }
	

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class MultiDirectFragment extends Fragment implements AsyncExceptionListener{

		private TextView _deviceIPTextView;
		private EditText _outputIPEditText;
		//private EditText _multicastGroupEditText;
		private EditText _portEditText;
		private Button _resetMulticastButton;
		//private Button _resetMulticastGroupButton;
		private Button _resetPortButton;
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
			_portEditText = (EditText)rootView.findViewById(R.id.portEditText);
			
			_resetMulticastButton = (Button)rootView.findViewById(R.id.resetMulticastButton);
			//_resetMulticastGroupButton = (Button)rootView.findViewById(R.id.resetMulticastGroupButton);
			_resetPortButton = (Button)rootView.findViewById(R.id.resetPortButton);
			
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
			
			int portNumber = ((MainActivity)getActivity()).networkController.portNumber;
			_portEditText.setText(""+portNumber);
			_portEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			    @Override
			    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			        if (actionId == EditorInfo.IME_ACTION_DONE) { 
			        	//String newValue = _portEditText.getText().toString();//TODO sanitize here vs controller?
			        	int newPort = 54321;
			        	try{
			        		newPort = Integer.parseInt(_portEditText.getText().toString());
			        	} catch(NumberFormatException e) {
			        		//too big
			        		receiveException(e, "Bad port number, try something 1000-65535", "port");
			        	}
			        	//Log.i("NETWORK", "get text "+newPort);
			        	/*if (newPort < 1000 || newPort > 65535) {
			        		_portEditText.setText(""+((MainActivity)getActivity()).networkController.getPortNumber());
			        		return false;
			        	}*/
			        	
			        	((MainActivity)getActivity()).networkController.setPortNumber(newPort);
			        	
			        }
			        return false;//dismiss keyboard
			    }
			});
			
			_resetMulticastButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetIP();
				}
			});
			
			
			
			_resetPortButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetPort();
				}
			});
			
			return rootView;
		}
		
		private void resetPort(){
			int newPort = 54321;
			_portEditText.setText(""+newPort);
			_networkController.setPortNumber(newPort);
		}
		private void resetIP(){
			String address = "224.0.0.1";
			_outputIPEditText.setText(address);
			((MainActivity)getActivity()).networkController.setOutputIPAddress(address);
		}
		
		public void setNetworkController(NetworkController nc) {
			_networkController = nc;
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
			
			_networkTimeTextView = (TextView)rootView.findViewById(R.id.textView2);
			_listView = (ListView)rootView.findViewById(R.id.listView1);
			_adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, _userNamesList) ;
			_listView.setAdapter(_adapter);
			_enableLANdiniSwitch = (Switch)rootView.findViewById(R.id.switch1);
			_enableLANdiniSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				 
				   @Override
				   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					   _networkController.landiniManager.setEnabled(isChecked);
				    /*if(isChecked){
				     switchStatus.setText("Switch is currently ON");
				    }else{
				     switchStatus.setText("Switch is currently OFF");
				    }*/
					if(isChecked) {
						_networkTimeDisplayTimer.startRepeatingTask();
					} else {
						_networkTimeDisplayTimer.stopRepeatingTask();
						_networkTimeTextView.setText("Network Time:");
					}
				 
				 }
			});
			
			return rootView;
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
		
		public void setNetworkController(NetworkController nc) {
			_networkController = nc;
		}
	}

	
}
