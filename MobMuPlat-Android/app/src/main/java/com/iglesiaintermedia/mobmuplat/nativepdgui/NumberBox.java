package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class NumberBox extends AtomWidget {
    private DecimalFormat fmt;
    private int numWidth;
    private float min, max;
    private float touchYPrev;

    public NumberBox(Context context, String[] atomline, float scale, int fontSize) {
        super(context, scale, fontSize);

        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);

        // calculate screen bounds for the numbers that can fit
        numWidth = Integer.parseInt(atomline[4]);
        StringBuffer calclen = new StringBuffer();
        for (int s=0; s<numWidth; s++) {
            if (s == 1) {
                calclen.append(".");
            } else {
                calclen.append("#");
            }
        }
        fmt = new DecimalFormat(calclen.toString());

        min = Float.parseFloat(atomline[5]);
        max = Float.parseFloat(atomline[6]);
        sendname = sanitizeLabel(atomline[10]);
        setReceiveName(sanitizeLabel(atomline[9]));
        labelString = sanitizeLabel(atomline[8]);
        labelPos = Integer.parseInt(atomline[7]);

        // graphics setup
        String adjustedString = calclen.append("0").toString(); //add one char space padding
        Rect rect = new Rect(); //computed post-scaling since paint scales its fontSize
        paint.setTextSize(fontSize * scale);
        paint.getTextBounds(adjustedString, 0, adjustedString.length(), rect);

        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + rect.width()/scale),
                Math.round(y + rect.height()/scale + 4));
        reshape();
        /*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + rect.width()), Math.round(y + rect.height() + 4*scale));
        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);*/
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float h = getHeight();
        float val = getValue();
        String string = stringForFloat(val, numWidth, fmt);
        canvas.drawText(string, 0, h-(2*scale), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        //Log.i("Number Box", "action "+action);
        if (action == MotionEvent.ACTION_DOWN) {
            touchYPrev = event.getY();
        } else if (action == MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view - move?
            float yPos = event.getY();
            float diff = touchYPrev - yPos;
            touchYPrev = yPos;
            setValue(getValue()+(diff/scale));
            sendFloat(getValue());
        }
        return true;
    }

    public void setValue(float value) {
        if (min != 0 || max != 0) {
            value = Math.min(Math.max(value, min), max);
        }
        super.setValue(value);
    }

    //
    protected void receiveSetFloat(float val) {
        setValue(val);
    }

    protected void receiveSetSymbol(String val) {
        // swallows set symbols
    }

    protected void receiveBangFromSource(String source) {
        //sendFloat(getValue());
    }

    protected void receiveFloatFromSource(float val, String source) {
        setValue(val);
        //sendFloat(getValue());
    }

    protected void receiveSymbolFromSource(String symbol, String source) {
        setValue(0);
        //sendFloat(getValue());
    }
}
