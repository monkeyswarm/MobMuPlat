package com.iglesiaintermedia.mobmuplat;

import java.util.Observable;
import java.util.Observer;

import com.iglesiaintermedia.LANdini.LANdiniTimer;
import com.iglesiaintermedia.LANdini.LANdiniTimerListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class ConsoleFragment extends Fragment implements Observer, OnClickListener, LANdiniTimerListener{

	private TextView _textView;
	private ScrollView _scrollView;
	LANdiniTimer _pollTimer;
	boolean _hasConsoleData;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_console, container,
				false);
		
		_scrollView = (ScrollView)rootView.findViewById(R.id.scrollView1);
		_textView = (TextView)rootView.findViewById(R.id.textView1);
		Button clearButton = (Button)rootView.findViewById(R.id.button1);
		clearButton.setOnClickListener(this);
		
		ConsoleLogController.getInstance().addObserver(this);
		update(null,null);
		_pollTimer = new LANdiniTimer(250, this);
		
		return rootView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		_pollTimer.startRepeatingTask();
	}
	
	public void onStop() {
		super.onStop();
		_pollTimer.stopRepeatingTask();
	}
	
	public void update(Observable obs, Object x) {
		_hasConsoleData = true;
	 }
	
	public void onClick(View v) {
		ConsoleLogController.getInstance().clear();
	}

	@Override
	public void onTimerFire() {
		if (_hasConsoleData) {
			_textView.setText(ConsoleLogController.getInstance().toString());
			  _scrollView.post(new Runnable() {
				    @Override
				    public void run() {
				        _scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				    }
				});
			_hasConsoleData = false;
		}
	}
}
