package com.iglesiaintermedia.LANdini;

import android.os.Handler;
import android.os.Looper;

public class LANdiniTimer {
	private int mInterval = 5000; // 5 seconds by default, can be changed later
	  private Handler mHandler;
	  private LANdiniTimerListener mListener;
	  private boolean mRunning;
	  public LANdiniTimer(int interval, LANdiniTimerListener listener) {
		  super();
		  mInterval = interval;
		  mListener = listener;
		  mHandler = new Handler();
	  }

	  Runnable mOnTimerFire = new Runnable() {
		  @Override 
		  public void run() {
        	  mListener.onTimerFire(/*LANdiniTimer.this*/);
          }
	  };
	  
	  Runnable mStatusChecker = new Runnable() { //HERE getting exception can't create handler inside thread...
	    @Override 
	    public void run() {
	    	//Looper.prepare(); //???
	    	if(mRunning==false)return;
	    	if(mListener!=null) {
	    		mHandler.post(mOnTimerFire); //put in main thread
	    	}
	      //updateStatus(); //this function can change value of mInterval.
	      mHandler.postDelayed(mStatusChecker, mInterval);
	    }
	  };

	  public void startRepeatingTask() {
		if (mRunning == true) return;
		mRunning = true;
	    mStatusChecker.run(); 
	  }

	  public void stopRepeatingTask() {
	    mHandler.removeCallbacks(mStatusChecker);
	    mRunning = false;
	  }
}
