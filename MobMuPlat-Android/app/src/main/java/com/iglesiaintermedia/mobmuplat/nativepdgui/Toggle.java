package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class Toggle extends IEMWidget {
    private static final String TAG = "Toggle";

    float nonZeroValue;

    public Toggle(Context context, String[] atomline, float scale) {
        super(context, scale);

        /*float x = Float.parseFloat(atomline[2]) * scale;
        float y = Float.parseFloat(atomline[3]) * scale;
        float w = Float.parseFloat(atomline[5]) * scale;
        float h = Float.parseFloat(atomline[5]) * scale;*/
        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);
        float w = Float.parseFloat(atomline[5]);
        float h = Float.parseFloat(atomline[5]);
        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),
                Math.round(y + h));

        sendValueOnInit = Integer.parseInt(atomline[6]) > 0;
        sendname = sanitizeLabel(atomline[7]);
        setReceiveName(sanitizeLabel(atomline[8]));
        labelString = sanitizeLabel(atomline[9]);
        labelpos[0] = Float.parseFloat(atomline[10]) ;
        labelpos[1] = Float.parseFloat(atomline[11]) ;
        labelfont = Integer.parseInt(atomline[12]);
        labelsize = (int)(Float.parseFloat(atomline[13]) );
        bgcolor = getColor(Integer.parseInt(atomline[14]));
        fgcolor = getColor(Integer.parseInt(atomline[15]));
        labelcolor = getColor(Integer.parseInt(atomline[16]));

        nonZeroValue = Float.parseFloat(atomline[18]);
        if (sendValueOnInit) {
            setValue(Float.parseFloat(atomline[17]));
        }

        reshape();

        // graphics setup
        /*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),
                Math.round(y + h));

        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);*/
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
        float h = getHeight();

        paint.setColor(bgcolor);
        paint.setStyle(Paint.Style.FILL);
        //canvas.drawRect(dRect,paint);
        canvas.drawPaint(paint); //fill whole canvas

        paint.setColor(Color.BLACK);
        //paint.setStrokeWidth(scale); // aka stroke width of one, times scale
            /*canvas.drawLine(dRect.left, dRect.top, dRect.right, dRect.top, paint);
            canvas.drawLine(dRect.left , dRect.bottom, dRect.right, dRect.bottom, paint);
            canvas.drawLine(dRect.left, dRect.top , dRect.left, dRect.bottom, paint);
            canvas.drawLine(dRect.right, dRect.top , dRect.right, dRect.bottom, paint);*/
            /*canvas.drawLine(postLayoutInnerRect.left, postLayoutInnerRect.top, postLayoutInnerRect.right, postLayoutInnerRect.top, paint);
            canvas.drawLine(postLayoutInnerRect.left , postLayoutInnerRect.bottom, postLayoutInnerRect.right, postLayoutInnerRect.bottom, paint);
            canvas.drawLine(postLayoutInnerRect.left, postLayoutInnerRect.top , postLayoutInnerRect.left, postLayoutInnerRect.bottom, paint);
            canvas.drawLine(postLayoutInnerRect.right, postLayoutInnerRect.top , postLayoutInnerRect.right, postLayoutInnerRect.bottom, paint);
        */
        paint.setStrokeWidth(lineWidth);
        canvas.drawLine(0,0, w,0, paint);
        canvas.drawLine(0,h, w,h, paint);
        canvas.drawLine(0,0,0,h, paint);
        canvas.drawLine(w,0,w,h, paint);

        if (getValue() != 0) {

            paint.setColor(fgcolor);
            paint.setStrokeWidth(3*scale);
            float padding = 2 * scale;
            canvas.drawLine(padding, padding, w - padding, h - padding, paint);
            canvas.drawLine(padding, h - padding, w - padding, padding, paint);

        }
        drawLabel(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            toggle();
            sendFloat(getValue());
        }
        return true;
    }

    public void toggle() {
        float value = getValue();
        if (value == 0) {
            setValue(nonZeroValue);
        } else {
            setValue(0);
        }
    }

    //
    protected void receiveSetBang() {
        toggle();
    }

    protected void receiveSetFloat(float inVal) {
        setValue(inVal);
    }

    protected void receiveBangFromSource(String source) {
        toggle();
        //sendFloat(getValue());
    }

    protected void receiveFloatFromSource(float inVal, String source) {
        setValue(inVal);
        //sendFloat(getValue());
    }
}
