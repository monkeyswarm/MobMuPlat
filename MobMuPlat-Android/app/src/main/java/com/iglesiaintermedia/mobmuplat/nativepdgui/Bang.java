package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class Bang extends IEMWidget {

    private long holdTime;
    private long interrupt;
    private boolean bangIsOn;
    private long lastBangTime, elapsedHoldTime;
    private static final int IEM_BNG_MINHOLDFLASHTIME = 50;
    private static final int IEM_BNG_MINBREAKFLASHTIME = 10;

    public Bang(Context context, String[] atomline, float scale) {
        super(context, scale);

        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);
        float w = Float.parseFloat(atomline[5]);
        float h = Float.parseFloat(atomline[5]);
        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),
                Math.round(y + h));

        holdTime = Math.max(IEM_BNG_MINHOLDFLASHTIME, (int) Float.parseFloat(atomline[6]));
        interrupt = Math.max(IEM_BNG_MINBREAKFLASHTIME, (int) Float.parseFloat(atomline[7]));
        sendValueOnInit = Integer.parseInt((atomline[8])) > 0; //DEI check val
        // checkFlashTimes = swaps if interrupt<hold time.

        sendname = sanitizeLabel(atomline[9]);
        setReceiveName(sanitizeLabel(atomline[10]));
        labelString = sanitizeLabel(atomline[11]);
        labelpos[0] = Float.parseFloat(atomline[12]);
        labelpos[1] = Float.parseFloat(atomline[13]);
        labelfont = Integer.parseInt(atomline[14]);
        labelsize = Integer.parseInt(atomline[15]);
        bgcolor = getColor(Integer.parseInt(atomline[16]));
        fgcolor = getColor(Integer.parseInt(atomline[17]));
        labelcolor = getColor(Integer.parseInt(atomline[18]));

        reshape();
        // graphics setup
        /*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
        setLayoutParams(new RelativeLayout.LayoutParams((int) dRect.width(), (int) dRect.height()));
        setX(dRect.left);
        setY(dRect.top);*/
    }

    @Override
    public void onDraw(Canvas canvas) {
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
        float h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgcolor);
        canvas.drawPaint(paint); //fill whole canvas

        paint.setColor(Color.BLACK);
        canvas.drawLine(0, 0, w, 0, paint);
        canvas.drawLine(0, h, w, h, paint);
        canvas.drawLine(0, 0, 0, h, paint);
        canvas.drawLine(w, 0, w, h, paint);
        if (bangIsOn) {
            paint.setColor(fgcolor);
            canvas.drawCircle(w / 2, h / 2, Math.min(w, h) / 2, paint);
        }
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(w / 2, h / 2, Math.min(w, h) / 2, paint);
        drawLabel(canvas);
    }

    // TODO handle interrupt time.
    private void bang() {
        if (!bangIsOn) { // not interrupted, start flash
            bangIsOn = true; //start
            invalidate();
            lastBangTime = System.nanoTime() / 1000000;
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bangIsOn = false; //stop
                    invalidate();
                    elapsedHoldTime = 0;
                }
            }, holdTime);
        } else { //interrupted
            elapsedHoldTime = (System.nanoTime() / 1000000) - lastBangTime;
            // resume flash
            final Handler handler = new Handler();
            long resumeHoldTime = holdTime - elapsedHoldTime;
            if (resumeHoldTime > 0) {
                bangIsOn = true; //start
                invalidate();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bangIsOn = false; //stop
                        invalidate();
                        elapsedHoldTime = 0;
                    }
                }, resumeHoldTime);
            } else { //stop
                bangIsOn = false;
                invalidate();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            bang();
            sendBang();
        }
        return false;
    }

    //
    protected void receiveBangFromSource(String source) {
        bang();
        //sendBang();
    }

    protected void receiveFloatFromSource(float val, String source) {
        bang();
        //sendBang();
    }

    public void receiveMessage(String source, String message, Object... args) {
        boolean wasEditMessage = super.receiveEditMessage(message, args);
        if (!wasEditMessage)
            bang();
        //sendBang();
    }

    public void receiveList(String source, Object... args) {
        bang();
        //sendBang();
    }
}
