package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import org.puredata.core.PdBase;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

public class MMPTable extends MMPControl {

	public int mode; //0=selection, 1=draw;
	public int selectionColor;
	public float displayRangeLo;
	public float displayRangeHi;
	public int displayMode;
	
	private int _tableSize;
	private float _tableData[];
	private RectF _myRectF;
	
	private Rect _clipRect;
	private Path _path;
	//private float _drawPoints[];
	
	// draw bookkeeping
	private float _touchDownPointX, _dragPointX;
	private int _prevTableIndex;
	
	public MMPTable(Context context, float screenRatio) {
		super(context, screenRatio);
		_myRectF = new RectF();
		_clipRect = new Rect();
		_path = new Path();
		this.paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		this.paint.setStrokeWidth(screenRatio);
		selectionColor = 0x800000FF;//blue, half transparent, should get overwritten.
		this.displayRangeLo = -1;
		this.displayRangeHi = 1;
	}

	public void loadTable() {
		copyFromPDAndDraw();
	}
	
	private void copyFromPDAndDraw(){
		int newSize = PdBase.arraySize(this.address);
		if (newSize <=0 )return; //returns -1 on no table found...
		if (newSize != _tableSize) {//new or resize
			_tableSize = newSize;
			if (_tableSize == 0) return;
			_tableData = new float[_tableSize];
			//userInteractionEnabled = YES;
		}
		PdBase.readArray(_tableData, 0, this.address, 0, _tableSize);
		//if !tableSeemsBad {
		draw();
	}
	
	private void draw() {
		draw(0, _tableSize-1);
	}
	
	private void draw(int startIndex, int endIndex) {
		invalidate();
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRectF.set(0,0,right-left, bottom-top);
			//_drawPoints = new float[(getWidth() + 3) * 4]; //three extra points used by fill mode
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        
        float touchX = event.getX();
        float touchY = event.getY();
        
        if (action == MotionEvent.ACTION_DOWN) {
        	if (!this.isEnabled()) return false; //reject touch down if disabled.
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
        	
        	if (mode == 0) {
    			
    			_touchDownPointX = touchX;
    			_dragPointX = touchX+1;
    			float clippedX = Math.max(Math.min(_touchDownPointX,getWidth()-1),0);
        		float normalizedX = clippedX/getWidth();
        	    int touchTableIndex = (int)(normalizedX*_tableSize);
    			
    			sendRangeMessage(touchTableIndex, touchTableIndex);//sliver
    			
    			invalidate();//rect?
    		} if (mode == 1) {
        		float normalizedX = touchX/getWidth(); //assuming touch down x is within bounds
        	    int touchDownTableIndex = (int)(normalizedX*_tableSize);
        	    _prevTableIndex = touchDownTableIndex;
        	    float normalizedY = touchY/getHeight();//change to -1 to 1
        	    //float flippedY = (1-normalizedY)*2-1;
        	    float flippedY = (1 - normalizedY)*(displayRangeHi - displayRangeLo) + displayRangeLo;
        	    
        	    //NSLog(@"touchDownTableIndex %d", touchDownTableIndex);
        	    
        	    _tableData[touchDownTableIndex] = flippedY;//check bounds
        	    draw(touchDownTableIndex,touchDownTableIndex);
        	    
        	    //make one-element array to send in
        	    PdBase.writeArray(this.address, touchDownTableIndex, new float[]{flippedY}, 0, 1);
        	}
        }
        if (action == MotionEvent.ACTION_MOVE) {
        	if (mode == 0) {
        		_dragPointX = touchX;
        		
        		float normalizedXA = _touchDownPointX/getWidth();
        		normalizedXA = Math.max(Math.min(normalizedXA,1),0);
        	    int dragTableIndexA = (int)(normalizedXA*_tableSize);
        	    
        	    float normalizedXB = touchX/getWidth();
        	    Math.max(Math.min(normalizedXB,getWidth()-1),0);
        	    int dragTableIndexB = (int)(normalizedXB*_tableSize);
        	    if(dragTableIndexB>_tableSize-1)dragTableIndexB=_tableSize-1;//clip
        	    
        	    sendRangeMessage(Math.min(dragTableIndexA, dragTableIndexB), Math.max(dragTableIndexA, dragTableIndexB));
        	    
        		invalidate();//rect?
        	} else if (mode == 1) { //draw mode
        	    float normalizedX = touchX/getWidth();
        	    normalizedX = Math.max(Math.min(normalizedX,1),0);
        	    int dragTableIndex = (int)(normalizedX*_tableSize);
        	    if(dragTableIndex >= _tableSize) dragTableIndex = _tableSize - 1;
        	    
        	    float normalizedY = touchY/getHeight();//change to -1 to 1
        	    normalizedY = Math.max(Math.min(normalizedY,1),0);
        	    //float flippedY = (1-normalizedY)*2-1;
        	    float flippedY = (1 - normalizedY)*(displayRangeHi - displayRangeLo) + displayRangeLo;
        	    
        	    //NSLog(@"dragTableIndex %d", dragTableIndex);
        	    
        	    //compute size, including self but not prev
        	    int traversedElementCount = Math.abs(dragTableIndex-_prevTableIndex);
        	    if(traversedElementCount==0)traversedElementCount=1;
        	    float touchValArray[] = new float[traversedElementCount];
        	    //float* touchValArray = (float*)malloc(traversedElementCount*sizeof(float));
        	    
        	    _tableData[dragTableIndex] = flippedY;
        	    //just one
        	    if(traversedElementCount==1) {
        	      
        	      draw(dragTableIndex, dragTableIndex);
        	      touchValArray[0] = flippedY;
        	      PdBase.writeArray(this.address, dragTableIndex, touchValArray, 0, 1);
        	     
        	    } else {
        	      //NSLog(@"multi!");
        	      int minIndex = Math.min(_prevTableIndex, dragTableIndex);
        	      int maxIndex = Math.max(_prevTableIndex, dragTableIndex);
        	      
        	      float minValue = _tableData[minIndex];
        	      float maxValue = _tableData[maxIndex];
        	      //NSLog(@"skip within %d (%.2f) to %d(%.2f)", minTouchIndex, [[_valueArray objectAtIndex:minTouchIndex] floatValue], maxTouchIndex, [[_valueArray objectAtIndex:maxTouchIndex] floatValue]);
        	      for(int i=minIndex+1;i<=maxIndex;i++){
        	        float percent = ((float)(i-minIndex))/(maxIndex-minIndex);
        	        float interpVal = (maxValue - minValue) * percent  + minValue ;
        	        //NSLog(@"%d %.2f %.2f", i, percent, interpVal);
        	        _tableData[i]=interpVal;
        	        touchValArray[i-(minIndex+1)]=interpVal;
        	      }
        	      draw(minIndex, maxIndex);
        	      PdBase.writeArray(this.address, minIndex+1, touchValArray, 0, traversedElementCount);
        	    }
        	    _prevTableIndex = dragTableIndex;
        	  }
        	//_prevPointX = touchX;
        	//_prevPointY = touchY;
        	
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        	//getParent().requestDisallowInterceptTouchEvent(false);
        	
    	}
        
        return true;
    }
	
	private void sendRangeMessage(int indexA, int indexB) {
		List<Object> args = new ArrayList<Object>();
    	args.add(this.address);
    	args.add("range");
    	args.add(Float.valueOf(indexA));
    	args.add(Float.valueOf(indexB));
    	this.controlDelegate.sendGUIMessageArray(args);
	}
	
	protected void onDraw(Canvas canvas) {
		_path.reset();
		canvas.getClipBounds(_clipRect);
		int indexDrawPointA = _clipRect.left;
		int indexDrawPointB = _clipRect.right;
		/*int padding = 3;
		int indexDrawPointA = (int)((float)MIN(indexA,indexB)/tableSize*self.frame.size.width)-padding;
		indexDrawPointA = MIN(MAX(indexDrawPointA,0),self.frame.size.width-1);
		int indexDrawPointB = (int)((float)(MAX(indexA,indexB)+1)/(tableSize)*self.frame.size.width)+padding;
		indexDrawPointB = MIN(MAX(indexDrawPointB,0),self.frame.size.width-1);
		  */
		this.paint.setStyle(Paint.Style.FILL);
		this.paint.setColor(this.color);
		canvas.drawRect(_myRectF, this.paint);
		
		
	   	this.paint.setStyle(Paint.Style.STROKE);
	    this.paint.setColor(this.highlightColor);
	    //this.paint.setStrokeWidth(2 * this.screenRatio);
	    
	    if(_tableData==null)return;
	    
	    //float prevY=1 - ((0 -displayRangeLo)/(displayRangeHi - displayRangeLo)); //DC offset zero...
	    //prevY*=getHeight();
	    //for (int i=0;i<getWidth();i++) {
	    for (int i = indexDrawPointA; i<indexDrawPointB; i++) {
	    	int tableIndex = (int)((float)i/getWidth()*_tableSize);
	    	float y = _tableData[tableIndex];
	        float unflippedY = 1-( (y-displayRangeLo)/(displayRangeHi - displayRangeLo));
		    unflippedY *= getHeight();
	    	//canvas.drawLine(i-1, prevY, i, unflippedY, this.paint);
	        /*_drawPoints[i*4]=i-1;
	        _drawPoints[i*4+1]=prevY;
	        _drawPoints[i*4+2]=i;
	        _drawPoints[i*4+3]=unflippedY;
	    	prevY = unflippedY;*/
		    if (i==0) {
		    	_path.moveTo(i, unflippedY);
		    } else {
		    	_path.lineTo(i, unflippedY);
		    }
	    }
	    // draw line
	    canvas.drawPath(_path, this.paint);
	    // ALSO draw fill on fill mode...
	    if (displayMode == 1) {// fill
	    	this.paint.setStyle(Paint.Style.FILL);
	    	float yPointOfTableZero = 1 - ((0 -displayRangeLo)/(displayRangeHi - displayRangeLo));
	        yPointOfTableZero *= getHeight();
	        int lastIndex = getWidth() - 1;
	    	_path.lineTo(lastIndex, yPointOfTableZero);
	    	_path.lineTo(0, yPointOfTableZero);
	    	_path.close();
	    	canvas.drawPath(_path, this.paint);
	    }
	    
	    
	    //selection
	    if (mode == 0) {
	    	this.paint.setStyle(Paint.Style.FILL);
			this.paint.setColor(selectionColor);
			canvas.drawRect(Math.min(_touchDownPointX, _dragPointX), 
					0, 
					Math.max(_touchDownPointX, _dragPointX), 
					getHeight(), 
					this.paint);
	    }
	}
	
	public void receiveList(List<Object> messageArray){
		super.receiveList(messageArray);
		if (messageArray.size()==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("refresh") ){
	        copyFromPDAndDraw();
	    }
	    if (messageArray.size()==1 && (messageArray.get(0) instanceof String) && messageArray.get(0).equals("clearSelection") ){
	        //hacky!
	    	_touchDownPointX = -1;
	    	_dragPointX = -1;
	    	invalidate();
	    		
	    }
	}
}
