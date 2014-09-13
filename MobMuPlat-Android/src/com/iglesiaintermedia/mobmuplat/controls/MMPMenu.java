package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.view.MotionEvent;

public class MMPMenu extends MMPControl {
	static final int BORDER_THICKNESS = 3;
	static final int DEFAULT_FONTSIZE = 18;
	static final int TAB_WIDTH = 30;
	
	private RectF _myInnerRect;
	private RectF _myInnerTabRect;
	private Rect _textRect;
	public List<String> stringList;
	public String titleString;
	
	public MMPMenu(Context context, float screenRatio) {
        super(context, screenRatio);
        _myInnerRect = new RectF();
        _myInnerTabRect = new RectF();
        _textRect = new Rect();
        stringList = new ArrayList<String>();
        titleString = "title goes here!";
        
        this.paint.setColor(this.color);
		this.paint.setTextAlign(Align.CENTER);
		this.paint.setTextSize(DEFAULT_FONTSIZE*this.screenRatio);
		
    }

	public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
        	//getParent().requestDisallowInterceptTouchEvent(true);
        	this.controlDelegate.launchMenuFragment(this);
        	
        	//_isShowingActivity = true;
        }
        return true;
	}
	
	public void didSelect(int index){//sent with -1 on no selection
		//_isShowingActivity = false;
		if (index<0 || index >= stringList.size() ) return;
		List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Integer.valueOf(index));
    	args.add(stringList.get(index));
    	this.controlDelegate.sendGUIMessageArray(args);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			float halfStroke = BORDER_THICKNESS * this.screenRatio * .5f; //move up?
			_myInnerRect.set(halfStroke, halfStroke, right - left -halfStroke, bottom-top-halfStroke);
			_myInnerTabRect.set(halfStroke, halfStroke, TAB_WIDTH * this.screenRatio, bottom-top-halfStroke);
		}
	}
	
	protected void onDraw(Canvas canvas) {
		this.paint.setStyle(Paint.Style.FILL);
		this.paint.getTextBounds(titleString, 0, titleString.length(), _textRect);//move to set title?
		canvas.drawText(titleString, _myInnerRect.centerX(), _myInnerRect.centerY() - (_textRect.centerY()), paint);
		//border - push in
		//todo move to constructor
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeWidth(BORDER_THICKNESS * this.screenRatio);
		
	    canvas.drawRoundRect(_myInnerRect,5,5,paint); 
	    canvas.drawRoundRect(_myInnerTabRect, 5, 5, paint);
	}
	
	public void receiveList(List<Object> messageArray){
		List<String> newDataArray = new ArrayList<String>();
		
	 //put all elements in list into a string array
    	
	   	for(Object ob: messageArray){
	    	if(ob instanceof String) newDataArray.add((String)ob);
	   		else if(ob instanceof Float) newDataArray.add(String.format("%.3f",((Float)(ob)).floatValue()) );
	   		else if(ob instanceof Integer) newDataArray.add(String.format("%d",((Integer)(ob)).intValue()) );
	   	}
    	
	   	stringList = newDataArray;
	   	
	   	this.controlDelegate.refreshMenuFragment(this);//refreshes list if already visible
	}
}

