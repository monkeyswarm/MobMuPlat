package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class MMPToggle extends MMPControl {

	public int borderThickness;
	private RectF _myRect;
	private RectF _myInnerRect;//to stroke border within visible bounds
	private int _value;
	
	public MMPToggle(Context context, float screenRatio) {
        super(context, screenRatio);
        borderThickness = 2;
        _myRect = new RectF();
        _myInnerRect = new RectF();
    }
	
	private void setValue(int value) {
		_value = value;
		invalidate();
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (!this.isEnabled()) return true;
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
        	if (!this.isEnabled()) return false; //reject touch down if disabled.
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
        	setValue(1 - _value);
        	sendValue();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        	//getParent().requestDisallowInterceptTouchEvent(false);
    	}
        
        return true;
    }
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left,bottom-top);
			float halfStroke = (borderThickness * this.screenRatio * .5f);
			_myInnerRect.set(halfStroke, halfStroke, right-left - halfStroke, bottom-top - halfStroke);
		}
	}
	
	private void sendValue() {
    	List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Float.valueOf(_value));
    	this.controlDelegate.sendGUIMessageArray(args);
    }
	
	protected void onDraw(Canvas canvas) {
		//fill
		if (_value == 1) {
	    	this.paint.setStyle(Paint.Style.FILL);
	    	this.paint.setColor(this.highlightColor);
	    	canvas.drawRoundRect(_myInnerRect,5,5,paint);
	    }
		
		//border - push in
		this.paint.setColor(this.color);
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeWidth(borderThickness * this.screenRatio);
		
	    canvas.drawRoundRect(_myInnerRect,5,5,paint); 
	}
	
	 public void receiveList(List<Object> messageArray){ 	
		 super.receiveList(messageArray);
		//Log.i("MobMuPlat", "receve list "+messageArray);
	    boolean sendVal  = true;
		//if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("set") ){
	    	messageArray = new ArrayList<Object>(messageArray.subList(1, messageArray.size() ) );
	        sendVal=false;
	    }
	    //set new value
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof Integer) ){
	        setValue( ((Integer)(messageArray.get(0))).intValue() );
	        if(sendVal)sendValue();
	    }
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof Float) ){
	        setValue( (int)((Float)(messageArray.get(0))).floatValue() );//ugly!
	        if(sendVal)sendValue();
	    }
	 }  
}
