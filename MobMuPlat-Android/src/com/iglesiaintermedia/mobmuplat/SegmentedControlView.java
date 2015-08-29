package com.iglesiaintermedia.mobmuplat;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class SegmentedControlView extends View {

	private Paint _paint;
	private RectF _myRectF;
	private RectF _myInnerRectF;
	private Rect _textRect;
	
	private float _strokeWidth;
	private float _cornerRadiusPixels;
	private int _itemCount;
	private int _selectedIndex;
	private String[] _itemStrings;
	private int _color;
	private int _backgroundColor;//"see-through"
	public SegmentedControlListener segmentedControlListener;
	
	public SegmentedControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		_strokeWidth = 4; //use attr for density??
		_paint = new Paint();
		_paint.setStyle(Paint.Style.STROKE);
		_paint.setTextAlign(Align.CENTER);
		_paint.setStrokeWidth(_strokeWidth);
		_paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		_myRectF = new RectF();
		_myInnerRectF = new RectF();
		_textRect = new Rect();
		_itemCount = 1;//to prevent divide by zero?
		_color = Color.BLACK;
		setTextSize(14);
		
		//just for layout editor
		setItems(new String[]{"hi", "this", "is", "fun"});
		_backgroundColor = Color.WHITE;
		
	}
	
	public void setItems(String[] strings) {
		_itemCount = strings.length;
		_itemStrings = strings;
		invalidate();
	}
	
	public void setSelectedIndex(int index) {
		_selectedIndex = index;
		invalidate();
	}
	
	public void setColor(int color) {
		_color = color;
		invalidate();
	}
	public void setSeethroughColor(int color) {
		_backgroundColor = color;
		invalidate();
	}
	public void setTextSize(int size) {
		 float scale = getResources().getDisplayMetrics().density;
		_paint.setTextSize(size * scale);
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRectF.set(0, 0, right-left, bottom-top);
			_myInnerRectF.set(_strokeWidth/2, _strokeWidth/2, right-left-_strokeWidth/2, bottom-top-_strokeWidth/2);

			Resources r = getResources();
			_cornerRadiusPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics());
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
        
        if (action == MotionEvent.ACTION_DOWN) {
        	int section = (int)(event.getX()/(getWidth() / _itemCount));
        	_selectedIndex = section;
        	invalidate();
        	if (segmentedControlListener != null) segmentedControlListener.onSegmentedControlChange(this, section);
        }
        return true;
	}
	
	protected void onDraw(Canvas canvas) {
		//border
		/*this.paint.setColor(this.color);
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeWidth(5);
	    canvas.drawRoundRect(_myRect,5,5,paint);*/
		_paint.setColor(_color);
		_paint.setStyle(Paint.Style.STROKE);
	    canvas.drawRoundRect(_myInnerRectF,_cornerRadiusPixels,_cornerRadiusPixels,_paint);
	    
	    _paint.setStyle(Paint.Style.FILL);
	    float width = getWidth()/_itemCount;
	    for(int i=0;i<_itemCount;i++ ) {
	    	float x = i*width;
	    	String titleString = _itemStrings[i];
	    	_paint.getTextBounds(titleString, 0, titleString.length(), _textRect);
	    	//draw box if selected
	    	
	    	if(i==_selectedIndex) {
	    		_paint.setColor(_color);
	    		canvas.drawRect(x,_myRectF.top, x+width, _myRectF.bottom, _paint);
	    		_paint.setColor(_backgroundColor);
	    		canvas.drawText(titleString, x+width/2, _myRectF.centerY()-_textRect.centerY() , _paint);
	    	} else {
	    		_paint.setColor(_color);
	    		canvas.drawText(titleString, x+width/2, _myRectF.centerY()-_textRect.centerY() , _paint);
	    	}
	    	
	    	//draw line at left
	    	_paint.setColor(_color);
	    	if( i > 0) {
	    		
	    		canvas.drawLine(x, _myRectF.top, x, _myRectF.bottom, _paint);
	    	}
	    }
	    
	    
	}
}
