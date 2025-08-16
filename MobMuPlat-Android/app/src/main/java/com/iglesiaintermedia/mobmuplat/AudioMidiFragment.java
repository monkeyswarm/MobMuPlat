package com.iglesiaintermedia.mobmuplat;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.stream.Collectors;

import android.graphics.Color;
import android.media.midi.MidiDeviceInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class AudioMidiFragment extends Fragment implements Observer, SegmentedControlListener {

	private UsbMidiController _usbMidiController;
	private ListView _listViewInput, _listViewOutput;

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
		_listViewOutput = (ListView)rootView.findViewById(R.id.listViewOutput);

		refreshList(); // Set up device names and adapters for midi in/out lists

		Switch _backgroundAudioSwitch = (Switch)rootView.findViewById(R.id.switch1);
		_backgroundAudioSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			   @Override
			   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				   ((MainActivity)getActivity()).setBackgroundAudioAndNetworkEnabled(isChecked);
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
		List<String> deviceInputNames = _usbMidiController.deviceInfos.stream().filter(info->hasInputOrOutput(info, false)).map(info->info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME)).collect(Collectors.toList());
		List<String> deviceOutputNames = _usbMidiController.deviceInfos.stream().filter(info->hasInputOrOutput(info, true)).map(info->info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME)).collect(Collectors.toList());
		ArrayAdapter<String> adapterInput = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, deviceInputNames);
		_listViewInput.setAdapter(adapterInput);
		ArrayAdapter<String> adapterOutput = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, deviceOutputNames);
		_listViewOutput.setAdapter(adapterOutput);
	}

	boolean hasInputOrOutput(MidiDeviceInfo info, boolean wantOutput) {
		MidiDeviceInfo.PortInfo[] portInfos = info.getPorts();
		for (MidiDeviceInfo.PortInfo portInfo : portInfos) {
			if (!wantOutput && portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
				return true;
			}
			if (wantOutput && portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
				return true;
			}
		}
		return false;
	}
}

