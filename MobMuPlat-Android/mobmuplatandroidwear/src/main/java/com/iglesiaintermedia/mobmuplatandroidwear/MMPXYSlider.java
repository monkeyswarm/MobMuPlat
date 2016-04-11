package com.iglesiaintermedia.mobmuplatandroidwear;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

public class MMPXYSlider extends MMPControl {

	private RectF _myRect;
	private RectF _myInnerRect;
	private float _valueX, _valueY;
	private float _borderThickness;
	
	public MMPXYSlider(Context context, float screenRatio) {
        super(context, screenRatio);
        _borderThickness = 2 * screenRatio;
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(_borderThickness);
        _myRect = new RectF();
        _myInnerRect = new RectF();
        this.paint.setColor(this.color);
    }
	
	private void setValues(float valueX, float valueY) {
		_valueX = valueX;
		_valueY = valueY;
		invalidate();
	}
	
	public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();//TODO seitch to masked
        if (action == MotionEvent.ACTION_DOWN) {
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
        	this.paint.setColor(this.highlightColor);
        }
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
        	float valueX = event.getX() / this.getWidth();
        	float valueY = 1.0f - (event.getY() / this.getHeight() );
        	if(valueX>1)valueX=1; if(valueX<0)valueX=0;
            if(valueY>1)valueY=1; if(valueY<0)valueY=0;
        	setValues(valueX, valueY);
        	sendValues();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        	//getParent().requestDisallowInterceptTouchEvent(false);
        	this.paint.setColor(this.color);
        	invalidate();
    	}
        
        return true;
    }

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left, bottom-top);
			_myInnerRect.set(_borderThickness/2f, _borderThickness/2f, right-left-_borderThickness/2f, bottom-top-_borderThickness/2f);
		}
	}
	
	private void sendValues() {
    	List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Float.valueOf(_valueX));
    	args.add(Float.valueOf(_valueY));
    	this.controlDelegate.sendGUIMessageArray(args);
    }
	
	protected void onDraw(Canvas canvas) {
		
		//border 
		 canvas.drawRect(_myInnerRect,paint); 
		 
		 //lines
		 float x = _valueX*this.getWidth();
		 float y = (1.0f-_valueY)*this.getHeight();
		 canvas.drawLine(0, y, this.getWidth(), y, paint);
		 canvas.drawLine(x, 0, x, this.getHeight(),paint);		
	}
	
	 public void receiveList(List<Object> messageArray){ 	
		//Log.i("MobMuPlat", "receve list "+messageArray);
	    boolean sendVal  = true;
		//if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("set") ){
	    	messageArray = new ArrayList<Object>(messageArray.subList(1, messageArray.size() ) );
	        sendVal=false;
	    }
	    //set new value
	    
	    if (messageArray.size()>1 && (messageArray.get(0) instanceof Float) && (messageArray.get(1) instanceof Float) ){
	        setValues( ((Float)(messageArray.get(0))).floatValue() , ((Float)(messageArray.get(1))).floatValue() );
	        if(sendVal)sendValues();
	    }
	 }  
}
