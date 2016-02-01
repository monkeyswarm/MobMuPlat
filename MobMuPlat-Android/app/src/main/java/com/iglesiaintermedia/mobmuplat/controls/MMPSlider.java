package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MMPSlider extends MMPControl{
	
    private float value;//0-1
    public int range=2;
	public boolean isHorizontal;
	
	final static int SLIDER_TROUGH_WIDTH=10;
	final static int SLIDER_TROUGH_TOPINSET=10;
	final static int SLIDER_THUMB_HEIGHT=20;
	
    private RectF _touchRect;
    private RectF _grooveRect;
    public MMPSlider(Context context, float screenRatio) {
        super(context, screenRatio);
        //this.setBackgroundColor(Color.RED);
        _touchRect = new RectF();
        _grooveRect = new RectF();
        
    }
    
    public void setIsHorizontal(boolean inIsHoriz){
		isHorizontal = inIsHoriz;
    }
    
    public void setRange(int inRange){
    	range = inRange;
    }
    
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	if (changed == true) {
    		if (!isHorizontal) {
    			//_touchRect.set(0, 0, right-left, SLIDER_THUMB_HEIGHT*this.screenRatio);
    			float grooveLeft = (right-left-(SLIDER_TROUGH_WIDTH*this.screenRatio))/2;
    			_grooveRect.set(grooveLeft, SLIDER_TROUGH_TOPINSET*this.screenRatio, grooveLeft + SLIDER_TROUGH_WIDTH*this.screenRatio, bottom-top-(SLIDER_TROUGH_TOPINSET*this.screenRatio));
    		} else {
    			//_touchRect.set(0, 0, SLIDER_THUMB_HEIGHT, bottom-top);//TODO move
    			float grooveTop = ((float)bottom-top - (SLIDER_TROUGH_WIDTH*this.screenRatio))/2f;
    			_grooveRect.set(SLIDER_TROUGH_TOPINSET*this.screenRatio, grooveTop, right-left- (SLIDER_TROUGH_TOPINSET*this.screenRatio), grooveTop + SLIDER_TROUGH_WIDTH*this.screenRatio);//TODO move
    		}
    		updateThumb();
    	}
	}
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
        	if (!this.isEnabled()) return false; //reject touch down if disabled.
        	this.paint.setColor(this.highlightColor);
        	invalidate();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        	this.paint.setColor(this.color);
        	invalidate();
    	}
        	
       
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view - move?
            
        	
        	float tempFloatValue;
	        if(!isHorizontal) tempFloatValue=1.0f-(((float)event.getY()-SLIDER_TROUGH_TOPINSET)/(getHeight()-(SLIDER_TROUGH_TOPINSET*2)));//0-1
	        else tempFloatValue=(((float)event.getX()-SLIDER_TROUGH_TOPINSET)/(getWidth()-(SLIDER_TROUGH_TOPINSET*2)));//0-1
	        
	        if(range==2 && tempFloatValue<=1 && tempFloatValue>=0  && tempFloatValue!=value){
	            setValue(tempFloatValue);
	            sendValue();
	        }
	        float tempValue = (float)(int)((tempFloatValue*(range-1))+.5);//round to 0-(range-1)
	        if(range>2 && tempValue<=range-1 && tempValue>=0  && tempValue!=value){
	        	setValue(tempValue);
		        sendValue();
	        }
        	
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        	//getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }
 
    
    private void sendValue() {
    	List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Float.valueOf(value));//new Float(val));
    	this.controlDelegate.sendGUIMessageArray(args);
    }

    protected void onDraw(Canvas canvas) {
      canvas.drawRoundRect(_grooveRect,3*this.screenRatio,3*this.screenRatio,paint);
      canvas.drawRoundRect(_touchRect,5*this.screenRatio,5*this.screenRatio,paint);
      int width = getWidth();
      int height = getHeight();
      if (range > 2) {
    	  for(int i=0; i<=range;i++){
    		  if (!isHorizontal){
    			  float left = width/4;
    			  float top = SLIDER_TROUGH_TOPINSET*this.screenRatio+i*(height-SLIDER_TROUGH_TOPINSET*this.screenRatio*2)/(range-1)-1;
    			  canvas.drawRect( left, top, width*3/4, top+2, paint);
    		  } else { //horiz
    			  float left = SLIDER_TROUGH_TOPINSET*this.screenRatio+i*(width-SLIDER_TROUGH_TOPINSET*this.screenRatio*2)/(range-1)-1;
    			  float top =  height/4;
    			  canvas.drawRect( left, top, left+2, height*3/4, paint);//rounded rect?
    		  }	
    	  }
      }  
    }
    
    private void setValue(float inVal) {
    	//Log.i("MobMuPlat", this.address+" setval "+newVal);
    	if(range==2){//clip 0.-1.
	        if(inVal>1)inVal=1;
	        if(inVal<0)inVal=0;
	    }
	    else{
	        if((inVal % 1.0)!=0.0)inVal=(float)(int)inVal;//round down to integer
	        if (inVal>=range) {
	            inVal=(float)(range-1);//clip if necessary
	        }
	    }
    	value = inVal;
    	updateThumb();
    }
    private void updateThumb(){
    	 if(!isHorizontal) {
    		 int top = (int)((1.0-(value/(range-1)))*(getHeight()-(SLIDER_TROUGH_TOPINSET*this.screenRatio*2)));
    		 _touchRect.set(0, top, getWidth(), top+SLIDER_THUMB_HEIGHT*this.screenRatio );
    	 } else {
    		 int left = (int)((value/(range-1))*(getWidth()-(SLIDER_TROUGH_TOPINSET*this.screenRatio*2)));
    		 _touchRect.set(left, 0, left+SLIDER_THUMB_HEIGHT*this.screenRatio, getHeight() );
    	 }
 	   
    	/*if(!isHorizontal) {
    		float newY = (1.0f-val)*getHeight();
    		touchRect.set(0,newY-10, getWidth(), newY+10);
    	} else {
    		float newX = val*getWidth();
    		touchRect.set(newX-10, 0, newX+10, getHeight());
    	}*/
        invalidate();
    }
    
    //
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
	    //System.out.print("\nms size "+messageArray.size()+" "+messageArray.get(0));
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof Integer) ){
	        setValue( ((Integer)(messageArray.get(0))).intValue() );
	        if(sendVal)sendValue();
	    }
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof Float) ){
	        setValue( ((Float)(messageArray.get(0))).floatValue() );
	        if(sendVal)sendValue();
	    }

	}
}
