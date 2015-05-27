package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View.MeasureSpec;

public class MMPButton extends MMPControl {
	private RectF _myRect;
	private int _value;
	
	public MMPButton(Context context, float screenRatio) {
        super(context, screenRatio);
        _myRect = new RectF();
    }
	
	public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
        	if (!this.isEnabled()) return false; //reject touch down if disabled.
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
        	_value = 1;
        	sendValue();
        	this.paint.setColor(this.highlightColor);
        	invalidate();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        	//getParent().requestDisallowInterceptTouchEvent(false);
        	_value = 0;
        	sendValue();
        	this.paint.setColor(this.color);
        	invalidate();
    	}
        
        return true;
    }
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(left, top, right-left, bottom-top);
		}
	}
	
	private void sendValue() {
    	List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Float.valueOf(_value));
    	this.controlDelegate.sendGUIMessageArray(args);
    }
	
	protected void onDraw(Canvas canvas) {
		//border
		/*this.paint.setColor(this.color);
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeWidth(5);
	    canvas.drawRoundRect(_myRect,5,5,paint);*/
	   
	    canvas.drawRoundRect(_myRect,5*this.screenRatio, 5*this.screenRatio, this.paint);
	}
	
	 public void receiveList(List<Object> messageArray){ 
		super.receiveList(messageArray);
		
		if (messageArray.size()>0 && (messageArray.get(0) instanceof Float)){
			_value = 1;
			sendValue();
			_value = 0;
			sendValue();
		}
	 }    	
}
