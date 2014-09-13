package com.iglesiaintermedia.mobmuplat;

import java.util.Observable;
import java.util.Observer;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AudioMidiFragment extends Fragment implements Observer {
	
	//private Button _refreshMidiButton;
	private UsbMidiController _usbMidiController;
	private ListView _listViewInput, _listViewOutput;
	//private TextView _bufferSizeTextView, _samplingRateTextView;
	public AudioDelegate audioDelegate;
	private ArrayAdapter<String> adapterInput;
	private ArrayAdapter<String> adapterOutput;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_audiomidi, container,
				false);
		
		_usbMidiController = ((MainActivity)getActivity()).usbMidiController;
		_usbMidiController.addObserver(this);
		
		
		_listViewInput = (ListView)rootView.findViewById(R.id.listViewInput);
		_listViewInput.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		_listViewOutput = (ListView)rootView.findViewById(R.id.listViewOutput);
		_listViewOutput.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		 adapterInput = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, _usbMidiController.midiInputStringList);
		 _listViewInput.setAdapter(adapterInput);
		 adapterOutput = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice, _usbMidiController.midiOutputStringList);
		 _listViewOutput.setAdapter(adapterOutput);
		
		_listViewInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
				//TextView clickedView = (TextView) view;
				//Toast.makeText(getActivity(), "Item with id ["+id+"] - Position ["+position+"] - Planet ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
				_usbMidiController.connectInputAtIndex(position);
				refreshList();
			}
		});
		
		_listViewOutput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
				//TextView clickedView = (TextView) view;
				//Toast.makeText(getActivity(), "Item with id ["+id+"] - Position ["+position+"] - Planet ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
				_usbMidiController.connectOutputAtIndex(position);
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
		
		//refreshList();
		
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
		
		_rateSeg = (SegmentedControlView)rootView.findViewById(R.id.segmentedControlViewSamplingRate);
		_rateSeg.setTextSize(16);
		_rateSeg.setItems(new String[]{"8000","11025","22050","32000","44100","34800"});
		_rateSeg.setSelectedIndex(4); //TODO double check this is default
		_rateSeg.setColor(Color.WHITE);
		_rateSeg.setSeethroughColor(Color.parseColor("#E85330"));
		_rateSeg.segmentedControlListener = this;*/
		
		return rootView;
	}

	@Override  // from midi controller that it has refreshed data
	public void update(Observable observable, Object data) {
		refreshList();
		
	}
	
	private void refreshList(){
		adapterInput.notifyDataSetChanged();
		adapterOutput.notifyDataSetChanged();
		
		_listViewInput.clearChoices();
		int inputIndex = _usbMidiController.getCurrInputIndex(); //-1 for nothing
		if (inputIndex>=0)
			_listViewInput.setItemChecked(inputIndex, true);
		
		_listViewOutput.clearChoices();
		int outputIndex = _usbMidiController.getCurrOutputIndex(); //-1 for nothing
		if (outputIndex>=0)
			_listViewOutput.setItemChecked(outputIndex, true);
		
		
		//inputViewToHighlight.setBackgroundColor(Color.BLUE);
		 
		 //manage selection - temp reset to zero TODO fix!
		 //_listViewInput.setSelection(0);
		 //_listViewOutput.setSelection(0);
	}

	
}
