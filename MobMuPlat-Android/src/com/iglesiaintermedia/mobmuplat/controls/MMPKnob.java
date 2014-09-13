package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

public class MMPKnob extends MMPControl{
	
	final static float ROTATION_PAD_RAD=.7f;
	final static int EXTRA_RADIUS=10;
	final static int TICK_DIM=10;
	
	private int _range;
	private float _value;
	
	private RectF _myRectF;
	private float _radius;
	public int indicatorColor;
	private float _indicatorRadius;
	private float _indicatorWidth;
	private boolean _highlight;
	private float _tickCenters[][];

	public MMPKnob(Context context, float screenRatio) {
		super(context, screenRatio);
		setRange(2);//create _tickCenters[][]
		_myRectF = new RectF();
		indicatorColor = Color.WHITE;
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			int newDim = Math.min(right-left, bottom-top);//smaller of width or height
			_myRectF.set(left, top, left+newDim, top+newDim);
			_radius = newDim/2 - ((EXTRA_RADIUS+TICK_DIM)*this.screenRatio);
			_indicatorRadius = _radius+2*this.screenRatio;
			_indicatorWidth = _radius/4;

			//layout tick positions, assume that setRange has already been called
			for(int i = 0 ; i < _range ; i++){
				float angle= (float)((float)i/(_range-1)* (Math.PI*2-ROTATION_PAD_RAD*2)+ROTATION_PAD_RAD+Math.PI/2);
				float xPos=(float) ( (_radius+(EXTRA_RADIUS+TICK_DIM/2)*this.screenRatio)*Math.cos(angle) );
				float yPos=(float) ( (_radius+(EXTRA_RADIUS+TICK_DIM/2)*this.screenRatio)*Math.sin(angle) );
				_tickCenters[i][0] = xPos;
				_tickCenters[i][1] = yPos;
				//Log.i("KNOB", "tick center "+i+":"+_tickCenters[i][0]+" "+_tickCenters[i][1]);
			}
		}
	}
	
	public void setRange(int newRange){
		_range = newRange;
		_tickCenters = new float[_range][2];
		
		/*if (_value>=_range)_value = _range-1;
		invalidate();*/
	}
	
	
	private void setValue(float inVal){
		if(inVal!=_value){
			if(_range==2){//clip 0.-1.
		        if(inVal>1)inVal=1;
		        if(inVal<0)inVal=0;
		    }
			else{
		        if((inVal % 1.0)!=0.0)inVal=(float)(int)inVal;//round down to integer
		        if (inVal>=_range) {
		            inVal=(float)(_range-1);//clip if necessary
		        }
		    }
		    _value=inVal;
		}
		invalidate();//put outside clause in case we touch same value, still want highlight to change
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
        	getParent().requestDisallowInterceptTouchEvent(true);
        	_highlight = true;//invalidate() in setValue in next clause
        }  
        
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
        	//
        	float touchX = event.getX()-_myRectF.centerX();
	        float touchY = event.getY()-_myRectF.centerY();
	        double theta = Math.atan2(touchY, touchX);//raw theta (-pi to pi) =0 starting at 3 o'clock and going potive clockwise
	        
	        double updatedTheta = (theta-Math.PI/2+(Math.PI*2)) % (Math.PI*2) ;//theta (0 to 2pi) =0 at 6pm going positive clockwise
	        //Log.i("KNOB", "theta "+theta+" update "+updatedTheta);
	        if(_range==2){
	            if(updatedTheta<ROTATION_PAD_RAD)setValue(0);
	            else if(updatedTheta>(Math.PI*2-ROTATION_PAD_RAD)) setValue(1);
	            else  setValue( (float)( (updatedTheta-ROTATION_PAD_RAD)/(Math.PI*2-2*ROTATION_PAD_RAD) ));
	        
	        }
	        else if (_range>2){
	            if(updatedTheta<ROTATION_PAD_RAD)setValue(0);
	            else if(updatedTheta>(Math.PI*2-ROTATION_PAD_RAD)) setValue(_range-1);
	            else setValue( (float) (  (int)((updatedTheta-ROTATION_PAD_RAD)/(Math.PI*2-2*ROTATION_PAD_RAD)*(_range-1)+.5)  ) );//round to nearest tick!
	        }
	        sendValue();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
        	_highlight = false;
        	invalidate();
        }
        return true;
	}
	
	private void sendValue() {
		List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Float.valueOf(_value));
    	this.controlDelegate.sendGUIMessageArray(args);
	}
	
	protected void onDraw(Canvas canvas) {
		this.paint.setStyle(Paint.Style.FILL);
		this.paint.setColor(this.color);
		
		for(int i = 0 ; i < _range ; i++){
	    	canvas.drawCircle(_myRectF.centerX()+_tickCenters[i][0], _myRectF.centerY()+_tickCenters[i][1], TICK_DIM/2*this.screenRatio, this.paint);
	    }
		
		//body	
		if(_highlight) this.paint.setColor(this.highlightColor);
		canvas.drawCircle(_myRectF.centerX(), _myRectF.centerY(), _radius, this.paint);
		
		//indicator
		//this.paint.setStrokeCap(Cap.ROUND);
		this.paint.setColor(indicatorColor);
		this.paint.setStrokeWidth(_indicatorWidth);
		//value to radius
		double newRad=0;//TODO move
		if(_range==2) {
	        newRad=_value*(Math.PI*2-ROTATION_PAD_RAD*2)+ROTATION_PAD_RAD;
		}
		 else if (_range>2) {
		     newRad=(_value/(_range-1))*(Math.PI*2-ROTATION_PAD_RAD*2)+ROTATION_PAD_RAD;
		 }
		newRad += Math.PI/2;//quarter turn clockwise to align with bottom (6 o'clock) instead of 3 o'clock
		float destX = _myRectF.centerX() + (float)Math.cos(newRad) * _indicatorRadius;
		float destY = _myRectF.centerY() + (float)Math.sin(newRad) * _indicatorRadius;
		canvas.drawLine(_myRectF.centerX(), _myRectF.centerY(), destX, destY, paint);
	}
	
	public void receiveList(List<Object> messageArray){
		boolean sendVal  = true;
		//if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("set") ){
	        messageArray = new ArrayList<Object>(messageArray.subList(1, messageArray.size() ) );
	    	sendVal=false;
	    }
	    //set new value
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof Float) ){
	        setValue( ((Float)(messageArray.get(0))).floatValue() );
	        if(sendVal)sendValue();
	    }
	}
}
