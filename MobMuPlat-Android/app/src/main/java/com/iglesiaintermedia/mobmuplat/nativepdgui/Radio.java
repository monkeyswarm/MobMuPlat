package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class Radio extends IEMWidget {
    float cellSize; //i.e. width/height, post-scale
    int numCells;
    boolean isHorizontal;

    public Radio(Context context, String[] atomline, float scale, boolean isHorizontal) {
        // TODO check atomline length >= 20.
        super(context, scale);
        this.isHorizontal = isHorizontal;
        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);

        cellSize = Integer.parseInt(atomline[5]);
        numCells = Integer.parseInt(atomline[8]);

        float w,h; // pre-scaling
        if (isHorizontal) {
            w = cellSize * numCells;
            h = cellSize;
        } else {
            w = cellSize;
            h	= cellSize * numCells;
        }

        // index 6 is the "new/old" switch, unused.

        sendValueOnInit = Integer.parseInt(atomline[7]) > 0;  //DEI check that send val on init works.
        sendname = sanitizeLabel(atomline[9]);
        setReceiveName(sanitizeLabel(atomline[10]));
        labelString = sanitizeLabel(atomline[11]);
        labelpos[0] = Float.parseFloat(atomline[12]) ;
        labelpos[1] = Float.parseFloat(atomline[13]) ;
        labelfont = Integer.parseInt(atomline[14]);
        labelsize = (int)(Float.parseFloat(atomline[15]) );
        bgcolor = getColor(Integer.parseInt(atomline[16]));
        fgcolor = getColor(Integer.parseInt(atomline[17]));
        labelcolor = getColor(Integer.parseInt(atomline[18]));

        // if on, set and send val.
        if (sendValueOnInit) {
            setValue(Float.parseFloat(atomline[19]));
        }

        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
        reshape();
        // graphics setup
//        RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),Math.round(y + h));
//        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
//        setX(dRect.left);
//        setY(dRect.top);
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
        canvas.drawLine(0,0, w,0, paint);
        canvas.drawLine(0,h, w,h, paint);
        canvas.drawLine(0,0,0,h, paint);
        canvas.drawLine(w,0,w,h, paint);

        float scaledCellSize = cellSize * scale;

        // dividers
        for (int i = 1; i < numCells; i++) {
            if (isHorizontal) {
                canvas.drawLine(i * scaledCellSize, 0, i * scaledCellSize, h, paint);
            } else {
                canvas.drawLine(0, i * scaledCellSize, w, i * scaledCellSize, paint);
            }
        }
        // selected square
        float value = getValue();
        paint.setColor(fgcolor);
        float scaledCellOffset = scaledCellSize/4;
        if (isHorizontal) {
            canvas.drawRect(value * scaledCellSize + scaledCellOffset, scaledCellOffset, (value + 1) * scaledCellSize - scaledCellOffset, h - scaledCellOffset, paint);
        } else {
            canvas.drawRect(scaledCellOffset, value * scaledCellSize + scaledCellOffset, w - scaledCellOffset, (value + 1) * scaledCellSize - scaledCellOffset, paint);
        }

        drawLabel(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            float val;
            if (isHorizontal) {
                val = (int)(event.getX() / (cellSize * scale));
            } else {
                val = (int)(event.getY() / (cellSize * scale));
            }
            setValue(val);
            sendFloat(getValue());
        }
        return true;
    }

    public void setValue(float value) {
        value = (float)Math.floor(Math.min(Math.max(value, 0), numCells-1));
        super.setValue(value);
    }

    //
    protected void receiveSetFloat(float inVal) {
        setValue(inVal);
    }

    protected void receiveBangFromSource(String source) {
        //sendFloat(getValue());
    }

    protected void receiveFloatFromSource(float inVal, String source) {
        setValue(inVal);
        //sendFloat(getValue());
    }
}
