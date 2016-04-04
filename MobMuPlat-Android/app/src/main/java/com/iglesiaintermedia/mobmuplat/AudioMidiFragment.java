package com.iglesiaintermedia.mobmuplat;

import java.util.Observable;
import java.util.Observer;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.noisepages.nettoyeur.usb.midi.UsbMidiDevice;

public class AudioMidiFragment extends Fragment implements Observer, SegmentedControlListener {
	
	//private Button _refreshMidiButton;
	private UsbMidiController _usbMidiController;
	private ListView _listViewInput, _listViewOutput;
	//private TextView _bufferSizeTextView, _samplingRateTextView;
	//public AudioDelegate audioDelegate;
	private ArrayAdapter<String> adapterInput;
	private ArrayAdapter<String> adapterOutput;
	private SegmentedControlView _rateSeg;
	private int[] _rates = new int[]{8000,11025,22050,32000,44100,48000};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_audiomidi, container,
				false);
		
		_usbMidiController = ((MainActivity)getActivity()).usbMidiController;
		_usbMidiController.addObserver(this);
		
		
		_listViewInput = (ListView)rootView.findViewById(R.id.listViewInput);
		_listViewInput.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		_listViewOutput = (ListView)rootView.findViewById(R.id.listViewOutput);
		_listViewOutput.setChoiceMode(ListView.CHOICE_MODE_SINGLE); // PdBase only one receiver
		
		 adapterInput = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, _usbMidiController.midiInputStringList);
		 _listViewInput.setAdapter(adapterInput);
		 adapterOutput = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, _usbMidiController.midiOutputStringList);
		 _listViewOutput.setAdapter(adapterOutput);
		
		_listViewInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
				//TextView clickedView = (TextView) view;
				//Toast.makeText(getActivity(), "Item with id ["+id+"] - Position ["+position+"] - Planet ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
                UsbMidiDevice.UsbMidiInput input = _usbMidiController.midiInputList.get(position);
                if (_usbMidiController.isConnectedToInput(input)) {
                    _usbMidiController.disconnectMidiInput(input);
                } else { //not connected
                    _usbMidiController.connectMidiInput(input);
                }
				refreshList();
			}
		});
		
		_listViewOutput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
				//TextView clickedView = (TextView) view;
				//Toast.makeText(getActivity(), "Item with id ["+id+"] - Position ["+position+"] - Planet ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
				//_usbMidiController.connectMidiOutput(_usbMidiController.midiOutputList.get(position));
                UsbMidiDevice.UsbMidiOutput output = _usbMidiController.midiOutputList.get(position);
                if (_usbMidiController.isConnectedToOutput(output)) {
                    _usbMidiController.disconnectMidiOutput(output);
                } else { //not connected
                    _usbMidiController.connectMidiOutput(output);
                }
				refreshList();//see if the index was selected, then highlight
			}
		});

		Button button = (Button) rootView.findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				//Toast.makeText(AudioMidiActivity.this, "Button Clicked", Toast.LENGTH_SHORT).show();
				_usbMidiController.refreshDevices();
			}
		});
		
		Switch _backgroundAudioSwitch = (Switch)rootView.findViewById(R.id.switch1);
		_backgroundAudioSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			   @Override
			   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				   ((MainActivity)getActivity()).setBackgroundAudioEnabled(isChecked);
			   }
		});
		
		_rateSeg = (SegmentedControlView)rootView.findViewById(R.id.segView1);
		_rateSeg.setTextSize(16);
		_rateSeg.setItems(new String[]{"8000","11025","22050","32000","44100","48000"});
		//_rateSeg.setSelectedIndex(4); //TODO double check this is default
		_rateSeg.setColor(Color.WHITE);
		_rateSeg.setSeethroughColor(Color.parseColor("#E85330"));
		_rateSeg.segmentedControlListener = this;
		
		// get rate
		int index = -1;
		int rate = ((MainActivity)getActivity()).getSampleRate();
		SparseIntArray rateToIndexSparseIntArray = new SparseIntArray();
		rateToIndexSparseIntArray.put(8000, 0);
		rateToIndexSparseIntArray.put(11025, 1);
		rateToIndexSparseIntArray.put(22050, 2);
		rateToIndexSparseIntArray.put(32000, 3);
		rateToIndexSparseIntArray.put(44100, 4);
		rateToIndexSparseIntArray.put(48000, 5);
		index = rateToIndexSparseIntArray.get(rate);
		_rateSeg.setSelectedIndex(index);
		
		/*_bufferSizeTextView = (TextView)rootView.findViewById(R.id.textViewBufferSize);
		if(audioDelegate!=null)_bufferSizeTextView.setText("Audio Buffer Size: "+audioDelegate.getBufferSizeMS()+" ms");
		_samplingRateTextView = (TextView)rootView.findViewById(R.id.textViewSamplingRate);
		if(audioDelegate!=null)_samplingRateTextView.setText("Sampling Rate: "+audioDelegate.getSampleRate());*/
		
		/*_bufferSeg = (SegmentedControlView)rootView.findViewById(R.id.segmentedControlViewBufferSize);
		_bufferSeg.setItems(new String[]{"1","2","4","8","16","32","64"});
		_bufferSeg.setSelectedIndex(3); //TODO double check this is default
		_bufferSeg.setColor(Color.WHITE);
		_bufferSeg.setSeethroughColor(Color.parseColor("#E85330"));
		_bufferSeg.segmentedControlListener = this;
		*/
		
		return rootView;
	}
	
	public void onSegmentedControlChange(SegmentedControlView segmentedControl, int sectionIndex) {
		boolean success = ((MainActivity)getActivity()).setSampleRate(_rates[sectionIndex]);
	}

	@Override  // from midi controller that it has refreshed data
	public void update(Observable observable, Object data) {
		refreshList();
		
	}
	
	private void refreshList(){
		adapterInput.notifyDataSetChanged();
		adapterOutput.notifyDataSetChanged();

        // redo checks - since the view is recycled the checkmark could be stale.
        // TODO: do this in a custom adapter's getView().
        for (int i=0;i<_usbMidiController.midiInputList.size();i++) {
            UsbMidiDevice.UsbMidiInput input = _usbMidiController.midiInputList.get(i);
            _listViewInput.setItemChecked(i, _usbMidiController.isConnectedToInput(input));
        }
        for (int i=0;i<_usbMidiController.midiOutputList.size();i++) {
            UsbMidiDevice.UsbMidiOutput output = _usbMidiController.midiOutputList.get(i);
            _listViewOutput.setItemChecked(i, _usbMidiController.isConnectedToOutput(output));
        }

		
	}

	
}
