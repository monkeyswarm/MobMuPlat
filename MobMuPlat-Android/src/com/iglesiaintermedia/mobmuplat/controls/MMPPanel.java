package com.iglesiaintermedia.mobmuplat.controls;

import java.io.File;
import java.util.List;

import com.iglesiaintermedia.mobmuplat.MainActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.MotionEvent;

public class MMPPanel extends MMPControl {
	
	//private String _imagePath;
	private boolean _shouldPassTouches;
	private boolean _showWarningText;
	private boolean _highlighted;
	private RectF _myRect;
	private Bitmap _imageBitmap;
	public MMPPanel(Context context, float screenRatio) {
		super(context, screenRatio);
		_myRect = new RectF();
	}
	
	public void setImagePath(String path) {//takes full path
		if(_imageBitmap!=null) _imageBitmap.recycle();
		_imageBitmap = BitmapFactory.decodeFile(path);
		invalidate();
	}
	
	public void setShouldPassTouches(boolean shouldPassTouches) {
		_shouldPassTouches = shouldPassTouches;
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left, bottom-top);
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if(!_shouldPassTouches)getParent().requestDisallowInterceptTouchEvent(true);
		return true;
	}
	
	protected void onDraw(Canvas canvas) {
		
        	//this.paint.setStyle(Paint.Style.FILL);
        if (_highlighted) this.paint.setColor(this.highlightColor);
        else this.paint.setColor(this.color);
        canvas.drawRect(_myRect, this.paint);
        
        if(_imageBitmap!=null){
        	canvas.drawBitmap(_imageBitmap, null, _myRect, this.paint);
        }
        
		if(_showWarningText){
			this.paint.setColor(Color.BLACK);
			paint.setTextSize(20*this.screenRatio);
			canvas.drawText("image file not found", 10,20*this.screenRatio,this.paint);
		}
	}

	public void receiveList(List<Object> messageArray){ 
    	//image path
		if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("image")){
			String path =  (String)messageArray.get(1);
			File extFile = new File(/*Environment.getExternalStorageDirectory()*/ MainActivity.getDocumentsFolderPath(), path);
            setImagePath(extFile.getAbsolutePath());
		}
		//highlight
	    if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("highlight")){
	    	_highlighted = ((int)(((Float)(messageArray.get(1))).floatValue()) > 0);//ugly
	    	invalidate();
	    }
	}
}
