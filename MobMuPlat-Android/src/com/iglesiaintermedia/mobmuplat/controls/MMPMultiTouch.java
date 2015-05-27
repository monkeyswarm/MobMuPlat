package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;

public class MMPMultiTouch extends MMPControl {
	
	private RectF _myRect;
	private RectF _myInnerRect;
	private boolean _highlight;
	private float _borderThickness;
	
	private SparseArray<MyPointF> _pointMap;//key is pointerID which works as poly vox
	private ArrayList<MyPointF> _pointStack;//for time order
	
	private PolyVoxComparator _polyVoxComparator;
	private XComparator _xComparator;
	private YComparator _yComparator;
	
	
	public MMPMultiTouch(Context context, float screenRatio) {
		super(context, screenRatio);
		_borderThickness = 3 * screenRatio;
		_myRect = new RectF();
        _myInnerRect = new RectF();
        _pointMap = new SparseArray<MyPointF>();
        _pointStack = new ArrayList<MyPointF>();
        
        _polyVoxComparator = new PolyVoxComparator();
        _xComparator = new XComparator();
        _yComparator = new YComparator();
        
        this.paint.setColor(this.color);
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left, bottom-top);
			_myInnerRect.set(_borderThickness/2f, _borderThickness/2f, right-left-_borderThickness/2f, bottom-top-_borderThickness/2f);
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		//todo move to right place up/down
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_DOWN) {
			if (!this.isEnabled()) return false; //reject touch down if disabled.
			getParent().requestDisallowInterceptTouchEvent(true);
			this.paint.setColor(this.highlightColor);
		} else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			//getParent().requestDisallowInterceptTouchEvent(false);
			this.paint.setColor(this.color);
		}
        int num = event.getPointerCount();
		int pointerIndex = event.getActionIndex();
		int pointerId = event.getPointerId(pointerIndex);
		switch (action) {

	    	case MotionEvent.ACTION_DOWN:
	    	case MotionEvent.ACTION_POINTER_DOWN: {
	    		// TODO use data
	    //		Log.i("MULTI", "down count:"+num+" idx:"+pointerIndex+" id"+pointerId);
	    		MyPointF point = new MyPointF();
	    		point.x = point.rawX = event.getX(pointerIndex);
	    		point.y = point.rawY = event.getY(pointerIndex);
	    		point.polyVox = pointerId;
	    	    normAndClipPoint(point);
	    	    _pointMap.put(pointerId, point);
	    	    _pointStack.add(point);
	    	    sendTouch(point, 1);
	    	    sendState();
	    		break;
	    	}
	    	case MotionEvent.ACTION_MOVE: { // a pointer was moved
	    		// but it doesn't tell me which...
	    		//Log.i("MULTI", "move count:"+num+" idx:"+pointerIndex);
	    		//iterates all
	    		for (int size = event.getPointerCount(), i = 0; i < size; i++) {
	    			MyPointF point = _pointMap.get(event.getPointerId(i));
	    	        if (point != null) {
	    	          //if changed
	    	        	if(point.rawX != event.getX(i) || point.rawY!=event.getY(i)) {
	    	        		point.x = point.rawX = event.getX(i);
	    	        		point.y = point.rawY = event.getY(i);
	    	        		normAndClipPoint(point);
	    	        		sendTouch(point, 2);
	    	        	}
	    	        }
	    		}
	    		sendState();
	    		break;
	    	}
	    	case MotionEvent.ACTION_UP:
	    	case MotionEvent.ACTION_POINTER_UP:
	    	case MotionEvent.ACTION_CANCEL: {
	    //		Log.i("MULTI", "up count:"+num+" idx:"+pointerIndex+" id"+pointerId);
	    		MyPointF point = _pointMap.get(pointerId);
	    		_pointMap.remove(pointerId);
	    		_pointStack.remove(point);
	    		sendTouch(point, 0);
	    		sendState();
	    		break;
	    	}
	    }
		
		invalidate();
		
		return true;
	}
	
	private void normAndClipPoint(PointF point){
		point.x = point.x/getWidth();
		point.x = Math.min(1, Math.max(-1, point.x));
		point.y = 1.0f-(point.y/getHeight());
		point.y = Math.min(1, Math.max(-1, point.y));
	}
	
	private void sendTouch(MyPointF point, int state) {
		List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add("touch");
    	args.add(Integer.valueOf(point.polyVox));
    	args.add(Integer.valueOf(state));
    	args.add(Float.valueOf(point.x));
    	args.add(Float.valueOf(point.y));
    	this.controlDelegate.sendGUIMessageArray(args);
	}
	
	private void sendState(){
		
		ArrayList<MyPointF> pointStackCopy = new ArrayList<MyPointF>(_pointStack);
		
		List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add("touchesByTime");
    	for(MyPointF point : pointStackCopy) {
    		args.add(Integer.valueOf(point.polyVox));
    		args.add(Float.valueOf(point.x));
    		args.add(Float.valueOf(point.y));
    	}
    	this.controlDelegate.sendGUIMessageArray(args);
    	
    	//sort by vox
    	Collections.sort(pointStackCopy, _polyVoxComparator);
    	args.clear();
    	args.add(this.address);
    	args.add("touchesByVox");
    	for(MyPointF point : pointStackCopy) {
    		args.add(Integer.valueOf(point.polyVox));
    		args.add(Float.valueOf(point.x));
    		args.add(Float.valueOf(point.y));
    	}
    	this.controlDelegate.sendGUIMessageArray(args);
    	
    	//sort by x
    	Collections.sort(pointStackCopy, _xComparator);
    	args.clear();
    	args.add(this.address);
    	args.add("touchesByX");
    	for(MyPointF point : pointStackCopy) {
    		args.add(Integer.valueOf(point.polyVox));
    		args.add(Float.valueOf(point.x));
    		args.add(Float.valueOf(point.y));
    	}
    	this.controlDelegate.sendGUIMessageArray(args);
    	
    	//sort by y
    	Collections.sort(pointStackCopy, _yComparator);
    	args.clear();
    	args.add(this.address);
    	args.add("touchesByY");
    	for(MyPointF point : pointStackCopy) {
    		args.add(Integer.valueOf(point.polyVox));
    		args.add(Float.valueOf(point.x));
    		args.add(Float.valueOf(point.y));
    	}
    	this.controlDelegate.sendGUIMessageArray(args);
		
	}
	
	protected void onDraw(Canvas canvas) {
		//if (highlight) this.paint.setColor(this.highlightColor);
		//this.paint.setColor(this.color);
		
		//border 
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeWidth(_borderThickness);
		 canvas.drawRect(_myInnerRect,paint); 
		 
		 //lines
		 for(int i=0;i<_pointMap.size();i++){
			 PointF point = _pointMap.valueAt(i);
			 canvas.drawLine(0, (1f-point.y)*getHeight(), this.getWidth(), (1f-point.y)*getHeight(), paint);
			 canvas.drawLine(point.x*getWidth(), 0, point.x*getWidth(), this.getHeight(),paint);
		 }
			
	}
	
}

class MyPointF extends PointF {
	public int polyVox;
	public float rawX;
	public float rawY;
}
class PolyVoxComparator implements Comparator<MyPointF> {
    @Override
    public int compare(MyPointF a, MyPointF b) {
        if (a.polyVox < b.polyVox) return -1;
        else if (a.polyVox == b.polyVox) return 0;
        else return 1;
    }
}

class XComparator implements Comparator<MyPointF> {
    @Override
    public int compare(MyPointF a, MyPointF b) {
        if (a.x < b.x) return -1;
        else if (a.x == b.x) return 0;
        else return 1;
    }
}

class YComparator implements Comparator<MyPointF> {
    @Override
    public int compare(MyPointF a, MyPointF b) {
        if (a.y < b.y) return -1;
        else if (a.y == b.y) return 0;
        else return 1;
    }
}
