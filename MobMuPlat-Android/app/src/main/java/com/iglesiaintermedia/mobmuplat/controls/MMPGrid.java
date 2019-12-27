package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

public class MMPGrid extends MMPControl {

	public int borderThickness;
	public int cellPadding;
	public int mode;//0 = toggle, 1 = momentary, 2 = hybrid (touch up inside momentary, touch up outside leave on)

	private int _dimX, _dimY;
	private GridCellView _buttonViewArray[][];
	
	public MMPGrid(Context context, float screenRatio) {
		super(context, screenRatio);
		mode = 0;
		borderThickness = 2;//TODO get ios defaults
		cellPadding = 1;
		setDimXY(4,3);
	}
	
	// assuming this is called before on measure
	public void setDimXY(int dimX, int dimY) { //todo checking for too large!
		_dimX = dimX; _dimY = dimY;
		//_buttonValArray = new boolean[_dimX][_dimY];
		//_myRect = new RectF();
		//_buttonRectArray = new RectF[_dimX][_dimY];
		//_buttonInnerRectArray = new RectF[_dimX][_dimY];
		_buttonViewArray = new GridCellView[_dimX][_dimY];
		for (int i=0;i<_dimX;i++) {
        	for (int j=0;j<_dimY;j++) {
        		//_buttonRectArray[i][j] = new RectF();
        		//_buttonInnerRectArray[i][j] = new RectF();
        		_buttonViewArray[i][j] = new GridCellView(this.getContext(), screenRatio, this, i, j);
        		//_buttonViewArray[i][j].setBackgroundColor(Color.BLACK);
        		this.addView(_buttonViewArray[i][j]);
        		
        	}
        }
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			int width = right - left;
			int height = bottom - top;
			//Log.i("GRID", "onlayout "+left+" "+top+" "+width+" "+height);
			for (int i=0;i<_dimX;i++) {
				for (int j=0;j<_dimY;j++) {
					float cellLeft = (float)i/_dimX * width;
					float cellTop = (float)j/_dimY * height;
					float cellWidth = (float)width/_dimX - cellPadding*this.screenRatio;
					float cellHeight = (float)height/_dimY - cellPadding*this.screenRatio;
					_buttonViewArray[i][j].layout((int)cellLeft, (int)cellTop, (int)(cellLeft+cellWidth), (int)(cellTop+cellHeight));
				}
			}
		}
	}
	
	public void sendValue(int x, int y, boolean val) {
    	List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Integer.valueOf(x));
    	args.add(Integer.valueOf(y));
    	args.add(Integer.valueOf(val == true ? 1 : 0));
    	this.controlDelegate.sendGUIMessageArray(args);
    }
	
	public void receiveList(List<Object> messageArray){ 
		super.receiveList(messageArray);
    	boolean sendVal  = true;
		//if message preceded by "set", then set "sendVal" flag to NO, and strip off set and make new messages array without it
	    if (messageArray.size()>0 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("set") ){
	    	messageArray = new ArrayList<Object>(messageArray.subList(1, messageArray.size() ) );
	        sendVal=false;
	    }
	    
	    //if message is three numbers,  set a cell's value, outputting value if required
	    if (messageArray.size()==3 && (messageArray.get(0) instanceof Float) && (messageArray.get(1) instanceof Float) && (messageArray.get(2) instanceof Float)){
	        int indexX =(int)(((Float)(messageArray.get(0))).floatValue());
	        int indexY =(int)(((Float)(messageArray.get(1))).floatValue());
	        int val =(int)(((Float)(messageArray.get(2))).floatValue());
	        if(indexX >= 0 && indexX<_dimX && indexY >=0 && indexY<_dimY) {
	        	boolean val2 = val > 0 ? true : false;
	        	_buttonViewArray[indexX][indexY].setValue(val2);
	        	if (sendVal){
	        		sendValue(indexX, indexY, val2);//or call it _buttonViewArray[][].sendValue()?
	        	}
	        	//generate Rect to invalidate
	        	/*RectF rectF = _buttonRectArray[indexX][indexY];
	        	Rect rect = new Rect((int)rectF.left, (int)rectF.top, (int)rectF.right, (int)rectF.bottom);//ugly
	        	invalidate(rect);*/
	        }
	    }
	    
	  //else if message starts with "getColumn", spit out array of that column's values
	    if (messageArray.size() == 2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("getcolumn") &&  (messageArray.get(1) instanceof Float)) {
	    	int colIndex =(int)(((Float)(messageArray.get(1))).floatValue());
	    	if (colIndex >= 0 && colIndex <_dimX) {
	    		//send list of vals
	    		List<Object> args = new ArrayList<Object>();
	        	args.add(this.address);
	        	args.add("column");
	        	for (int i = 0;i < _dimY;i++) {
	        		boolean val = _buttonViewArray[colIndex][i].getValue();
	        		args.add(Integer.valueOf(val == true ? 1 : 0));
	        	}
	        	this.controlDelegate.sendGUIMessageArray(args);
	    	}
	    }
	    
	  //else if message starts with "getRow", spit out array of that row's values
	    if (messageArray.size() == 2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("getrow") &&  (messageArray.get(1) instanceof Float)) {
	    	int rowIndex =(int)(((Float)(messageArray.get(1))).floatValue());
	    	if (rowIndex >= 0 && rowIndex <_dimY) {
	    		//send list of vals
	    		List<Object> args = new ArrayList<Object>();
	        	args.add(this.address);
	        	args.add("row");
	        	for (int i = 0;i < _dimX;i++) {
	        		boolean val = _buttonViewArray[i][rowIndex].getValue();
	        		args.add(Integer.valueOf(val == true ? 1 : 0));
	        	}
	        	this.controlDelegate.sendGUIMessageArray(args);
	    	}
	    }
	    //clear
	    if (messageArray.size() == 1 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("clear")){
	    	for (int i=0;i<_dimX;i++) {
	        	for (int j=0;j<_dimY;j++) {
	        		_buttonViewArray[i][j].setValue(false);
	        	}
	    	}
	    }
	}
}

class GridCellView extends View {

	private boolean _value;
	private float _screenRatio;
	private RectF _myRect;
	private RectF _myInnerRect;
	private MMPGrid _parentGrid;
	private int _xPos,_yPos;
	public GridCellView(Context context, float screenRatio, MMPGrid parentGrid, int x, int y) {
		super(context);
		_screenRatio = screenRatio;
		_parentGrid = parentGrid;
		_xPos = x;
		_yPos = y;
		_myRect = new RectF();
		_myInnerRect = new RectF();
	}
	
	public void setValue(boolean value){
		_value = value;
		//if(_value==true)this.setBackgroundColor(_parentGrid.highlightColor);
		//else this.setBackgroundColor(_parentGrid.color);
		invalidate();
	}
	
	public boolean getValue(){
		return _value;
	}
	
	public void sendValue() {
		_parentGrid.sendValue(_xPos,_yPos,_value);
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
        //int index = event.getActionIndex();
        
        if (action == MotionEvent.ACTION_DOWN) {
        	if (!_parentGrid.isEnabled()) return false; //reject touch down if disabled.
        	_parentGrid.getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
        	
        	if (_parentGrid.mode == 0 || _parentGrid.mode == 2) { //toggle, or hybrid touch down like toggle
        		//flip
        		//Log.i("GRID", "flip "+_buttonValArray[touchedX][touchedY]);
        		setValue(!_value);
        		sendValue();
        	} else if (_parentGrid.mode == 1) {// momentary, just flip on
        		if (_value == false){
        			setValue(true);
        			sendValue();
        		}
        	}
        	
        	//RectF rectF = _buttonRectArray[touchedX][touchedY];
 //       	sendValue(touchedX, touchedY, _buttonValArray[touchedX][touchedY]);
        	//Rect rect = new Rect((int)rectF.left, (int)rectF.top, (int)rectF.right, (int)rectF.bottom);//ugly
        	//invalidate(rect);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
   //     	_parentGrid.getParent().requestDisallowInterceptTouchEvent(false);
        	// turn off in certain modes
        	if (_parentGrid.mode == 1 || _parentGrid.mode == 2) {
        		//int touchedX = (int)((float)event.getX(index)/this.getWidth() * _dimX);
            	//int touchedY = (int)((float)event.getY(index)/this.getHeight() * _dimY);
            	// if on, flip off
            	if (_value == true){
            		setValue(false);
            		sendValue();
            		//RectF rectF = _buttonRectArray[touchedX][touchedY];
//                	sendValue(touchedX, touchedY, _buttonValArray[touchedX][touchedY]);
                	//Rect rect = new Rect((int)rectF.left, (int)rectF.top, (int)rectF.right, (int)rectF.bottom);//ugly
                	//invalidate(rect);
            	}
        	}
        }
	    //}
        return true;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left, bottom-top);
			float halfStroke = _parentGrid.borderThickness * _parentGrid.screenRatio * .5f;
			_myInnerRect.set(halfStroke, halfStroke, right - left -halfStroke, bottom-top-halfStroke);
		}
	}
		
	//TODO make my own paint? static?
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		_parentGrid.paint.setStrokeWidth(_parentGrid.borderThickness * _parentGrid.screenRatio);
		if(_value==true){	
			_parentGrid.paint.setStyle(Paint.Style.FILL);
			_parentGrid.paint.setColor(_parentGrid.highlightColor);
        	canvas.drawRoundRect(_myInnerRect,5*_parentGrid.screenRatio,5*_parentGrid.screenRatio,_parentGrid.paint);
        }
		_parentGrid.paint.setStyle(Paint.Style.STROKE);
		_parentGrid.paint.setColor(_parentGrid.color);
        canvas.drawRoundRect(_myInnerRect,5*_parentGrid.screenRatio,5*_parentGrid.screenRatio,_parentGrid.paint);
	}
}

