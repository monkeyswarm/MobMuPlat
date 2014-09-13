package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

public class MMPMultiSlider extends MMPControl {

	private float[] _valueArray;
	private RectF _myRect;
	private RectF _myInnerRect;
	private RectF _currRect; //vs array of rect?
	private float SLIDER_HEIGHT;
	private int _currHeadIndex = -1;
	private float _headWidth;
	public MMPMultiSlider(Context context, float screenRatio) {
		super(context, screenRatio);
		_myRect = new RectF();
		SLIDER_HEIGHT = 20 * screenRatio;
		_currRect = new RectF();
		_myInnerRect = new RectF();
		_valueArray = new float[8];
		//_valueArray = new float[]{0f,.333f,.666f,1.0f};
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left, bottom-top);
			_myInnerRect.set(0,SLIDER_HEIGHT/2f, right - left, bottom - top -SLIDER_HEIGHT/2f);
			_headWidth = ((float)(right-left)/_valueArray.length);
		}
	}
	
	public void setRange(int newRange){
		_valueArray = new float[newRange];
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
        	getParent().requestDisallowInterceptTouchEvent(true);
        	
        	int headIndex = (int)(event.getX()/_headWidth);
        	headIndex = Math.max(0,Math.min(_valueArray.length-1, headIndex)); //clip to 0-(range-1)
        	float clippedPointY = Math.max(Math.min(event.getY(), getHeight()-SLIDER_HEIGHT/2f), SLIDER_HEIGHT/2f);
        	float headVal = 1.0f-( (clippedPointY-SLIDER_HEIGHT/2f) / (getHeight() - SLIDER_HEIGHT) );
            _valueArray[headIndex]= headVal;
            _currHeadIndex = headIndex;
            sendValue();
            //inv rect
            Rect invRect = new Rect((int)(headIndex*_headWidth-2), 0, (int)((headIndex+1) * _headWidth + 2), (int)_myRect.bottom);
	        invalidate(invRect);
        	
        } else if (action == MotionEvent.ACTION_MOVE) {
        	
        	int headIndex = (int)(event.getX()/_headWidth);
        	headIndex = Math.max(0,Math.min(_valueArray.length-1, headIndex)); //clip to 0-(range-1)
        	float clippedPointY = Math.max(Math.min(event.getY(), getHeight()-SLIDER_HEIGHT/2f), SLIDER_HEIGHT/2f);
        	float headVal = 1.0f-( (clippedPointY-SLIDER_HEIGHT/2f) / (getHeight() - SLIDER_HEIGHT) );
            _valueArray[headIndex]= headVal;
            
            //also set elements between prev touch and move, to avoid "skipping" on fast drag
            int minTouchIndex = headIndex;
            int maxTouchIndex = headIndex;
            if(Math.abs(headIndex-_currHeadIndex)>1){
              minTouchIndex = Math.min(headIndex, _currHeadIndex);
              maxTouchIndex = Math.max(headIndex, _currHeadIndex);
              
              float minTouchedValue = _valueArray[minTouchIndex];
              float maxTouchedValue = _valueArray[maxTouchIndex];
              
              for(int i=minTouchIndex+1;i<maxTouchIndex;i++){
                float percent = ((float)(i-minTouchIndex))/(maxTouchIndex-minTouchIndex);
                float interpVal = (maxTouchedValue - minTouchedValue) * percent  + minTouchedValue ;
                _valueArray[i] = interpVal;
                //[_valueArray setObject:[NSNumber numberWithFloat:interpVal] atIndexedSubscript:i];
              }
              //[self updateThumbsFrom:minTouchIndex+1 to:maxTouchIndex-1];
              
            } 
            
            sendValue();
            if(headIndex!=_currHeadIndex){
            	//invalidate previous head so it redraws to normal color
            	Rect invRect = new Rect((int)(_currHeadIndex*_headWidth-2), 0, (int)((_currHeadIndex+1) * _headWidth+2), (int)_myRect.bottom);
            	_currHeadIndex = headIndex;
            	invalidate(invRect);
            }
            //inv rect
            Rect invRect = new Rect((int)(minTouchIndex*_headWidth-2), 0, (int)((maxTouchIndex+1) * _headWidth+2), (int)_myRect.bottom);
	        invalidate(invRect);
         	
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
        	//getParent().requestDisallowInterceptTouchEvent(false);
        	Rect invRect = new Rect((int)(_currHeadIndex*_headWidth-2), 0, (int)((_currHeadIndex+1) * _headWidth+2), (int)_myRect.bottom);
        	_currHeadIndex = -1;
        	invalidate(invRect);
        }
        
        return true;
	}
	
	private void sendValue() {
		List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	for(int i=0;i<_valueArray.length;i++) {
    		args.add(Float.valueOf(_valueArray[i]));
    	}
    	this.controlDelegate.sendGUIMessageArray(args);
	}
	
	protected void onDraw(Canvas canvas) {
		
       this.paint.setStyle(Paint.Style.STROKE);
       this.paint.setStrokeWidth(2 * this.screenRatio);
       this.paint.setColor(this.color);
       canvas.drawRect(_myInnerRect, this.paint);
       
       this.paint.setStyle(Paint.Style.FILL);
       float step = _myRect.width()/_valueArray.length;//TODO check error
       for(int i=0;i<_valueArray.length;i++) {
    	   float left = i * step;
    	   float top = (1-_valueArray[i]) * (_myRect.height() - SLIDER_HEIGHT) ;
    	   _currRect.set(left, top, left+step, top+SLIDER_HEIGHT);
    	   if(_currHeadIndex == i)this.paint.setColor(this.highlightColor);
    	   else this.paint.setColor(this.color);
    	   canvas.drawRoundRect(_currRect, 4*this.screenRatio, 4*this.screenRatio, this.paint);
       }
	}
	
	public void receiveList(List<Object> messageArray){ 
    	boolean sendVal  = true;
		//if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("set") ){
	    	messageArray = new ArrayList<Object>(messageArray.subList(1, messageArray.size() ) );
	        sendVal=false;
	    }
	    //
	    if (messageArray.size()>1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("allVal") ){
	    	float newVal = ((Float)messageArray.get(1)).floatValue();
	    	newVal = Math.max(Math.min(newVal, 1), 0);
	    	for (int i=0;i<_valueArray.length;i++) {
	    		_valueArray[i] = newVal;
	    	}
	    	if(sendVal)sendValue();
	    	invalidate();
	    } else if (messageArray.size()>0 && (messageArray.get(0) instanceof Float) ) {//list to set   	
	    	_valueArray = new float[messageArray.size()];//TODO error check input
	    	for (int i=0;i<_valueArray.length;i++) {
	    		float newVal = ((Float)messageArray.get(i)).floatValue();
	    		newVal = Math.max(Math.min(newVal, 1), 0);
	    		_valueArray[i] = newVal;
	    	}
	    	
	    	_headWidth = ((float)getWidth()/_valueArray.length);
	    	
	    	if(sendVal)sendValue();
	    	invalidate();
	    }
	}
}
