package com.iglesiaintermedia.mobmuplat.controls;


import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.view.MotionEvent;

public class MMPLCD extends MMPControl {

	private Bitmap _bitmap;
	private Canvas _canvas;
	private RectF _currRectF;
	private Rect _currRect;
	
	private float fR, fG, fB, fA;
	
	float penWidth;
	PointF _penPoint;
	
	public MMPLCD(Context context, float screenRatio) {
		super(context, screenRatio);
		_currRectF = new RectF();
		_currRect = new Rect();
		_penPoint = new PointF();
		setPenWidth(1.0f);//gets multiplied by screenratio internally
	}
	
	public void setHighlightColor(int newColor){
		super.setHighlightColor(newColor);
		fR=Color.red(newColor) / 255f;
		fG=Color.green(newColor) / 255f;
		fB=Color.blue(newColor) / 255f;
		fA=Color.alpha(newColor) / 255f;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (_bitmap == null || changed == true) {
			_bitmap = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.ARGB_8888);
			_canvas = new Canvas();
			_canvas.setBitmap(_bitmap);
		}
		
		/*paintOval(.1f, .1f, .3f, .3f, 0,1f,0,1f);
		frameOval(.6f, .6f, .4f, .4f, 0,0,0,1);
		//framePolyRGBA(floatArrayToList(new float[]{.4f,.1f,.7f,.1f,.1f,.6f,.9f,.9f}), fR,fG,fB,1f);
		//paintPolyRGBA(floatArrayToList(new float[]{.1f,.1f,.3f,.1f,.4f,.6f,.8f,.9f}), fR,fG,fB,1f);
		lineTo(.3f, .5f);
		lineTo(.5f, .3f);
		moveTo(.9f, .9f);
		lineTo( .2f, .9f);*/
	}
	
	//test only
	/*private List<Object> floatArrayToList(float[] floatArray) {
		List<Object> message = new ArrayList<Object>();
		for(int i=0;i<floatArray.length;i++) {
			message.add(Float.valueOf(floatArray[i]));
		}
		return message;
	}*/
		
	@Override 
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
        if (_bitmap != null) {
            canvas.drawBitmap(_bitmap, 0, 0, null);
        }
    }
	
	public void setColor(int newColor){
		super.setColor(newColor);
		this.setBackgroundColor(newColor);
	}
	
	private void clear() {
        if (_canvas != null) {
        	_canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            invalidate();
        }
    }
	
	//internal util
	private void setPaintColorRGBA(float r, float g, float b, float a) {
		this.paint.setARGB((int)(a*255), (int)(r*255), (int)(g*255), (int)(b*255));
	}
	
	public void paintRect(float x, float y, float x2, float y2, float r, float g, float b, float a){
		this.paint.setStyle(Paint.Style.FILL);
		setPaintColorRGBA(r,g,b,a);
		_currRectF.set(x*getWidth(), y*getHeight(), x2*getWidth(), y2*getHeight());
		_canvas.drawRect(_currRectF, this.paint);
		_currRectF.roundOut(_currRect);
		invalidate(/*_currRect*/);
	}
	
	public void paintRect(float x, float y, float x2, float y2){
		paintRect(x,y,x2,y2,fR,fG,fB,fA);
	}
	
	public void frameRect(float x, float y, float x2, float y2, float r, float g, float b, float a){
		this.paint.setStyle(Paint.Style.STROKE);
		setPaintColorRGBA(r,g,b,a);
		_currRectF.set(x*getWidth(), y*getHeight(), x2*getWidth(), y2*getHeight());
		_canvas.drawRect(_currRectF, this.paint);
		float halfStroke = penWidth/2;
		_currRectF.set(Math.min(x, x2)*getWidth()-halfStroke, Math.min(y, y2)*getHeight()-halfStroke, Math.max(x, x2)*getWidth()+halfStroke, Math.max(y, y2)*getHeight()+halfStroke );
		_currRectF.roundOut(_currRect);
		invalidate(/*_currRect*/);
	}
	
	public void frameRect(float x, float y, float x2, float y2){
		frameRect(x,y,x2,y2,fR,fG,fB,fA);
	}
	
	public void paintOval(float x, float y, float x2, float y2, float r, float g, float b, float a){
		this.paint.setStyle(Paint.Style.FILL);
		setPaintColorRGBA(r,g,b,a);
		_currRectF.set(x*getWidth(), y*getHeight(), x2*getWidth(), y2*getHeight());
		_canvas.drawOval(_currRectF, this.paint);
		_currRectF.roundOut(_currRect);
		invalidate(/*_currRect*/);
	}
	
	public void paintOval(float x, float y, float x2, float y2){
		paintOval(x,y,x2,y2,fR,fG,fB,fA);
	}
	
	public void frameOval(float x, float y, float x2, float y2, float r, float g, float b, float a){
		this.paint.setStyle(Paint.Style.STROKE);
		setPaintColorRGBA(r,g,b,a);
		_currRectF.set(x*getWidth(), y*getHeight(), x2*getWidth(), y2*getHeight());
		_canvas.drawOval(_currRectF, this.paint);
		float halfStroke = penWidth/2;
		_currRectF.set(Math.min(x, x2)*getWidth()-halfStroke, Math.min(y, y2)*getHeight()-halfStroke, Math.max(x, x2)*getWidth()+halfStroke, Math.max(y, y2)*getHeight()+halfStroke );
		_currRectF.roundOut(_currRect);
		invalidate(/*_currRect*/);
	}
	
	public void frameOval(float x, float y, float x2, float y2){
		frameOval(x,y,x2,y2,fR,fG,fB,fA);
	}
	
	private void framePolyRGBA(List<Object> messageArray, float r, float g, float b, float a) {
	
		if(messageArray.size()<4)return;
	    
		float  minX=getWidth(), minY = getHeight(), maxX =0, maxY = 0;
		
		this.paint.setStyle(Paint.Style.STROKE);
		setPaintColorRGBA(r,g,b,a);
		
		Path polygon = new Path();
		float x = ((Float)(messageArray.get(0))).floatValue() * getWidth();
		float y = ((Float)(messageArray.get(1))).floatValue() * getHeight();
		polygon.moveTo(x,y);
		
		if(x<minX)minX=x; if(y<minY)minY=y; if(x>maxX)maxX=x; if(y>maxY)maxY=y;
	    
	    for(int i = 2; i < messageArray.size(); i+=2){
			x = ((Float)(messageArray.get(i))).floatValue() * getWidth();
			y = ((Float)(messageArray.get(i+1))).floatValue() * getHeight();
			polygon.lineTo(x,y);
			if(x<minX)minX=x; if(y<minY)minY=y; if(x>maxX)maxX=x; if(y>maxY)maxY=y;
		    
	    }
		
	    polygon.close();
	    _canvas.drawPath(polygon, this.paint);
	    
	    float halfStroke = penWidth/2;
		_currRectF.set(minX-halfStroke, minY-halfStroke, maxX+halfStroke, maxY+halfStroke );
		_currRectF.roundOut(_currRect);
		invalidate(_currRect);
	}
	
	private void framePoly(List<Object> messageArray){//can assume all Float
		framePolyRGBA(messageArray, fR, fG, fB, fA);
	}
	
	private void paintPolyRGBA(List<Object> messageArray, float r, float g, float b, float a){
		
		if(messageArray.size()<4)return;
	    
		float  minX=getWidth(), minY = getHeight(), maxX =0, maxY = 0;
		
		this.paint.setStyle(Paint.Style.FILL);
		setPaintColorRGBA(r,g,b,a);
		
		Path polygon = new Path();
		float x = ((Float)(messageArray.get(0))).floatValue() * getWidth();
		float y = ((Float)(messageArray.get(1))).floatValue() * getHeight();
		polygon.moveTo(x,y);
		
		if(x<minX)minX=x; if(y<minY)minY=y; if(x>maxX)maxX=x; if(y>maxY)maxY=y;
	    
	    for(int i = 2; i < messageArray.size(); i+=2){
			x = ((Float)(messageArray.get(i))).floatValue() * getWidth();
			y = ((Float)(messageArray.get(i+1))).floatValue() * getHeight();
			polygon.lineTo(x,y);
			if(x<minX)minX=x; if(y<minY)minY=y; if(x>maxX)maxX=x; if(y>maxY)maxY=y;  
	    }
		
	    polygon.close();
	    _canvas.drawPath(polygon, this.paint);
	    
	    _currRectF.set(minX, minY, maxX, maxY);
		_currRectF.roundOut(_currRect);
		invalidate(_currRect);
	}

	private void paintPoly(List<Object> messageArray){//can assume all Float
		paintPolyRGBA(messageArray, fR, fG, fB, fA);
	}
	
	private void moveTo(float x, float y){//input normalized floats
		_penPoint.x = x*getWidth();
		_penPoint.y = y*getHeight();
	}
	
	private void lineTo(float x, float y, float r, float g, float b, float a){//input normalized floats
		x = x*getWidth();
		y = y*getHeight();
		
		this.paint.setStyle(Paint.Style.STROKE);
		setPaintColorRGBA(r,g,b,a);
		
		Path path = new Path();
		path.moveTo(_penPoint.x,_penPoint.y);
		path.lineTo(x,y);
		
		_canvas.drawPath(path, paint);
		
		float halfStroke = penWidth/2;
		_currRectF.set(Math.min(x, _penPoint.x)-halfStroke, Math.min(y, _penPoint.y)-halfStroke, Math.max(x, _penPoint.x)+halfStroke, Math.max(y, _penPoint.y)+halfStroke );
		_currRectF.roundOut(_currRect);
		invalidate(_currRect);
			
		_penPoint.x = x;
		_penPoint.y = y;
	}
	
	private void lineTo(float x, float y) {
		lineTo(x,y,fR,fG,fB,fA);
	}
	
	public void setPenWidth(float newWidth){
		penWidth = (int)newWidth;
		this.paint.setStrokeWidth(penWidth*this.screenRatio);
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		
		float valX = (float)event.getX()/this.getWidth();   
	    float valY = (float)event.getY()/this.getHeight();
		if(valX>1)valX=1;if(valX<0)valX=0;
		if(valY>1)valY=1;if(valY<0)valY=0;
		
		if (action == MotionEvent.ACTION_DOWN) {
			if (!this.isEnabled()) return false; //reject touch down if disabled.
			getParent().requestDisallowInterceptTouchEvent(true);
		    sendValue(1, valX, valY);
		} else if (action == MotionEvent.ACTION_MOVE) {
			sendValue(2, valX, valY);
		} else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			//getParent().requestDisallowInterceptTouchEvent(false);
			sendValue(0, valX, valY);
		}
		
		return true;
	}
	
	private void sendValue(int state, float x, float y) {
		List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add(Integer.valueOf(state));
    	args.add(Float.valueOf(x));
    	args.add(Float.valueOf(y));
    	this.controlDelegate.sendGUIMessageArray(args);
	}
	
	public void receiveList(List<Object> messageArray){ 
		super.receiveList(messageArray);
		//preprocess integers into float - java OSC library mixes the two even though PD just sends floats
		for(int i=1;i < messageArray.size();i++){
			if (messageArray.get(i) instanceof Integer){
				Integer val = (Integer)(messageArray.get(i));
				messageArray.set(i, Float.valueOf(val));
			}
			//if it isn't a float/int, insert zero
			if (! (messageArray.get(i) instanceof Float) ){
				messageArray.set(i, Float.valueOf(0));
			}
		}
	    
		if(messageArray.size()==5 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("paintrect") ){
			   paintRect(((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue());
			}
		   else if (messageArray.size()==9 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("paintrect")  ){
			   paintRect(((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue(), ((Float)(messageArray.get(5))).floatValue(), ((Float)(messageArray.get(6))).floatValue(), ((Float)(messageArray.get(7))).floatValue(), ((Float)(messageArray.get(8))).floatValue());
			}
		   if(messageArray.size()==5 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("framerect")  ){
			   frameRect( ((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue());
		   }
		   else if (messageArray.size()==9 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("framerect")  ){
			   frameRect(((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue(), ((Float)(messageArray.get(5))).floatValue(), ((Float)(messageArray.get(6))).floatValue(), ((Float)(messageArray.get(7))).floatValue(), ((Float)(messageArray.get(8))).floatValue());
			}
		   
		   else if(messageArray.size()==5 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("paintoval")  ){
			   paintOval(((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue());
			}
		   else if (messageArray.size()==9 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("paintoval")  ){
			   paintOval(((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue(), ((Float)(messageArray.get(5))).floatValue(), ((Float)(messageArray.get(6))).floatValue(), ((Float)(messageArray.get(7))).floatValue(), ((Float)(messageArray.get(8))).floatValue());
			}
		   if(messageArray.size()==5 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("frameoval")  ){
			   frameOval( ((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue());
		   }
		   else if (messageArray.size()==9 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("frameoval")  ){
			   frameOval( ((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue(), ((Float)(messageArray.get(5))).floatValue(), ((Float)(messageArray.get(6))).floatValue(), ((Float)(messageArray.get(7))).floatValue(), ((Float)(messageArray.get(8))).floatValue());
			}
		   
		   else if (messageArray.size()%2==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("framepoly")  ){
			   List<Object> coordinatesArray =  messageArray.subList(1, messageArray.size()); //just coordinates
			   framePoly(coordinatesArray);
		   }
		   
		   else if (messageArray.size()%2==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("framepolyRGBA")  ){ 
			   List<Object> coordinatesArray =  messageArray.subList(1, messageArray.size()-4); //just coordinates
			   int RGBAStartIndex = messageArray.size()-4;
			   framePolyRGBA(coordinatesArray, ((Float)(messageArray.get(RGBAStartIndex))).floatValue(),  ((Float)(messageArray.get(RGBAStartIndex+1))).floatValue(), ((Float)(messageArray.get(RGBAStartIndex+2))).floatValue(), ((Float)(messageArray.get(RGBAStartIndex+3))).floatValue());
		   }
		   else if (messageArray.size()%2==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("paintpoly")  ){
			   List<Object> coordinatesArray =  messageArray.subList(1, messageArray.size()); //just coordinates
			   paintPoly(coordinatesArray);
		   }
		   else if (messageArray.size()%2==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("paintpolyRGBA")  ){ 
			   List<Object> coordinatesArray = messageArray.subList(1, messageArray.size()-4); //just coordinates
			   int RGBAStartIndex = messageArray.size()-4;
			   paintPolyRGBA(coordinatesArray, ((Float)(messageArray.get(RGBAStartIndex))).floatValue(),  ((Float)(messageArray.get(RGBAStartIndex+1))).floatValue(), ((Float)(messageArray.get(RGBAStartIndex+2))).floatValue(), ((Float)(messageArray.get(RGBAStartIndex+3))).floatValue());
		   }
		   
		   else if (messageArray.size()==7 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("lineto")  ){
			   lineTo( ((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue(), ((Float)(messageArray.get(3))).floatValue(), ((Float)(messageArray.get(4))).floatValue(), ((Float)(messageArray.get(5))).floatValue(), ((Float)(messageArray.get(6))).floatValue() );
			}
		   else if (messageArray.size()==3 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("lineto")  ){
			   lineTo( ((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue() );
			}
		   else if (messageArray.size()==3 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("moveto")  ){
			   moveTo(((Float)(messageArray.get(1))).floatValue(), ((Float)(messageArray.get(2))).floatValue() );
		   }
		   
		   else if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("penwidth")){
			   setPenWidth( ((Float)(messageArray.get(1))).floatValue());
		   }
		
		   else if (messageArray.size()==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("clear")){
			   clear();
		   }
	}
}
