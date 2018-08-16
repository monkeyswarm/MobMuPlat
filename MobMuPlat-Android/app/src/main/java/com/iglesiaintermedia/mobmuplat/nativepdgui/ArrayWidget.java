package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import org.puredata.core.PdBase;

public class ArrayWidget extends Widget {
    private float displayXRangeLow, displayXRangeHigh; //not used yet
    private float displayYRangeLow, displayYRangeHigh;
    private int displayMode;//not used yet
    private String address; //same as receiveName

    private int _tableSize;
    private float _tableData[];
    int _prevTableIndex;

    private int width, height;

    public ArrayWidget(Context context,
                       String[] atomline,
                       String[] coordsLine,
                       String[] restoreLine,
                       float scale,
                       int fontSize) {
        super(context, scale);

        //_clipRect = new Rect(); // dirty rect doesn't work

        float x = Float.parseFloat(restoreLine[2]);
        float y = Float.parseFloat(restoreLine[3]);
        float w = Float.parseFloat(coordsLine[6]);
        float h = Float.parseFloat(coordsLine[7]);
        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),
                Math.round(y + h));

        // communicate with array directly via PdBase.
        // receive name is just to handle messages sent to array that
        // require visual refresh. Check ordering (i.e. that internal array is set before we refresh)
        // sendname remains null.
        address = labelString = atomline[2]; //no sanitization?
        setReceiveName(address); //no sanitization?

        displayYRangeLow = Float.parseFloat(coordsLine[5]);
        displayYRangeHigh = Float.parseFloat(coordsLine[3]);
        labelpos[1] = -fontSize; //TODO nudge
        labelsize = fontSize;

        reshape();
        // graphics setup
        /*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),Math.round(y + h));
        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);*/
    }

    public void setup() {
        super.setup();
        copyFromPDAndDraw();
    }

    private void copyFromPDAndDraw(){
        int newSize = PdBase.arraySize(address);
        if (newSize <=0 )return; //returns -1 on no table found...
        if (newSize != _tableSize) {//new or resize
            _tableSize = newSize;
            if (_tableSize == 0) return;
            _tableData = new float[_tableSize];
            //userInteractionEnabled = YES;
        }
        PdBase.readArray(_tableData, 0, this.address, 0, _tableSize);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
        float h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgcolor);
        canvas.drawPaint(paint); //fill whole canvas

        paint.setColor(Color.BLACK);
        //paint.setStrokeWidth(1);
        canvas.drawLine(0, 0, w, 0, paint);
        canvas.drawLine(0, h, w, h, paint);
        canvas.drawLine(0, 0, 0, h, paint);
        canvas.drawLine(w, 0, w, h, paint);

        drawLabel(canvas);

        paint.setStyle(Paint.Style.STROKE);

        if(_tableData==null)return;

        /*canvas.getClipBounds(_clipRect);
        int indexDrawPointA = _clipRect.left;
        int indexDrawPointB = _clipRect.right;*/

        //canvas.drawRect(_myRectF, this.paint);


        this.paint.setStyle(Paint.Style.STROKE);
        //paint.setStrokeWidth(scale);
        //this.paint.setColor(this.highlightColor);

        for (int i = 0; i<width; i++) {
            int tableIndex = (int)((float)i/getWidth()*_tableSize);
            float y = _tableData[tableIndex];
            /*float unflippedY = 1-( (y-displayYRangeLow)/(displayYRangeHigh - displayYRangeLow));
            unflippedY *= getHeight();
            if (i==0) {
                _path.moveTo(i, unflippedY);
            } else {
                _path.lineTo(i, unflippedY);
            }*/
            int prevIndex = (int)((float)(i-1)/w*_tableSize);
            int nextIndex = (int)((float)(i+1)/w*_tableSize); //could be out of bounds.
            //if(indexA==indexB && indexA<currIndex && indexA>prevIndex) tableIndex = indexA;

            float minValForPoint = _tableData[tableIndex], maxValForPoint = _tableData[tableIndex];
            // scan all data points for min/max val
            for (int index = tableIndex; index<nextIndex && index<_tableSize; index++) {
                float val = _tableData[index];
                if (val>maxValForPoint)maxValForPoint = val;
                if (val<minValForPoint)minValForPoint = val;
            }


            // Scale lo to hi to flipped 0 to frame height.
            float unflippedMinY = 1-( (minValForPoint-displayYRangeLow)/(displayYRangeHigh - displayYRangeLow));
            unflippedMinY *= h;
            float unflippedMaxY = 1-( (maxValForPoint-displayYRangeLow)/(displayYRangeHigh - displayYRangeLow));
            unflippedMaxY *= h;

            canvas.drawLine(i,unflippedMinY,i,unflippedMaxY+scale,paint);
            //_path.moveTo(i, unflippedMinY);
            //_path.lineTo(i, unflippedMaxY + scale); //plus "scale" = plus one "point", so that line is one point thick
            //Log.i("GRAPH", ""+i+" "+unflippedMinY+" "+unflippedMaxY+1);
        }
        // draw line
        //canvas.drawPath(_path, this.paint);
        // ALSO draw fill on fill mode...
        /*if (displayMode == 1) {// fill
            this.paint.setStyle(Paint.Style.FILL);
            float yPointOfTableZero = 1 - ((0 -displayYRangeLow)/(displayYRangeHigh - displayYRangeLow));
            yPointOfTableZero *= getHeight();
            int lastIndex = getWidth() - 1;
            _path.lineTo(lastIndex, yPointOfTableZero);
            _path.lineTo(0, yPointOfTableZero);
            _path.close();
            canvas.drawPath(_path, this.paint);
        }*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        float touchX = event.getX();
        float touchY = event.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view

            float normalizedX = touchX/getWidth(); //assuming touch down x is within bounds
            int touchDownTableIndex = (int)(normalizedX*_tableSize);
            _prevTableIndex = touchDownTableIndex;
            float normalizedY = touchY/getHeight();//change to -1 to 1
            //float flippedY = (1-normalizedY)*2-1;
            float flippedY = (1 - normalizedY)*(displayYRangeHigh - displayYRangeLow) + displayYRangeLow;

            //NSLog(@"touchDownTableIndex %d", touchDownTableIndex);

            _tableData[touchDownTableIndex] = flippedY;//check bounds
            //draw(touchDownTableIndex,touchDownTableIndex);
            invalidate();

            //make one-element array to send in
            PdBase.writeArray(this.address, touchDownTableIndex, new float[]{flippedY}, 0, 1);

        }
        if (action == MotionEvent.ACTION_MOVE) {

            float normalizedX = touchX/getWidth();
            normalizedX = Math.max(Math.min(normalizedX,1),0);
            int dragTableIndex = (int)(normalizedX*_tableSize);
            if(dragTableIndex >= _tableSize) dragTableIndex = _tableSize - 1;

            float normalizedY = touchY/getHeight();//change to -1 to 1
            normalizedY = Math.max(Math.min(normalizedY,1),0);
            //float flippedY = (1-normalizedY)*2-1;
            float flippedY = (1 - normalizedY)*(displayYRangeHigh - displayYRangeLow) + displayYRangeLow;

            //NSLog(@"dragTableIndex %d", dragTableIndex);

            //compute size, including self but not prev
            int traversedElementCount = Math.abs(dragTableIndex-_prevTableIndex);
            if(traversedElementCount==0)traversedElementCount=1;
            float touchValArray[] = new float[traversedElementCount];
            //float* touchValArray = (float*)malloc(traversedElementCount*sizeof(float));

            _tableData[dragTableIndex] = flippedY;
            //just one
            if(traversedElementCount==1) {

                //draw(dragTableIndex, dragTableIndex);
                touchValArray[0] = flippedY;
                PdBase.writeArray(this.address, dragTableIndex, touchValArray, 0, 1);
                invalidate();

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
                //draw(minIndex, maxIndex);
                invalidate();
                PdBase.writeArray(this.address, minIndex+1, touchValArray, 0, traversedElementCount);
            }
            _prevTableIndex = dragTableIndex;

        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            //getParent().requestDisallowInterceptTouchEvent(false);

        }

        return true;
    }

    //

    @Override
    public void receiveBang(String source) {
        copyFromPDAndDraw();
    }

    @Override
    public void receiveList(String source, Object... args) {
        copyFromPDAndDraw();
    }

    @Override
    public void receiveMessage(String source, String message, Object... args) {
        copyFromPDAndDraw();
    }
}
