package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class Slider extends IEMWidget {
    private static final String TAG = "Slider";

    private boolean isHorizontal;

    private float minValue, maxValue;
    private boolean log, steady, isReversed; //steady 1 = don't jump on touch down
    //private float val0, x0, y0;
    private float originalFrameSize, prevPos, centerValue;

    private int _controlValue, controlPos;
    private double sizeConvFactor; //< scaling factor for lin/log value conversion

    public Slider(Context context, String[] atomline, float scale, boolean isHorizontal) {
        super(context, scale);
        this.isHorizontal = isHorizontal;

        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);
        float w = Float.parseFloat(atomline[5]);
        float h = Float.parseFloat(atomline[6]);
        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),
                Math.round(y + h));
        if (isHorizontal) {
            originalFrameSize = Float.parseFloat(atomline[5]); //original w
        } else {
            originalFrameSize = Float.parseFloat(atomline[6]); //original h
        }
        sizeConvFactor = 0;

        minValue = Float.parseFloat(atomline[7]);
        maxValue = Float.parseFloat(atomline[8]);
        log = Integer.parseInt(atomline[9]) > 0; //DEI check
        sendValueOnInit = Integer.parseInt(atomline[10]) > 0; //DEI check
        sendname = sanitizeLabel(atomline[11]);//app.app.replaceDollarZero(atomline[11]);
        setReceiveName(sanitizeLabel(atomline[12]));
        labelString = sanitizeLabel(atomline[13]);
        labelpos[0] = Float.parseFloat(atomline[14]) ;
        labelpos[1] = Float.parseFloat(atomline[15]) ;
        labelfont = Integer.parseInt(atomline[16]);
        labelsize = (int)(Float.parseFloat(atomline[17]));
        bgcolor = getColor(Integer.parseInt(atomline[18]));
        fgcolor = getColor(Integer.parseInt(atomline[19]));
        labelcolor = getColor(Integer.parseInt(atomline[20]));
        steady = Integer.parseInt(atomline[22]) > 0;

        // Important part of checkSize/checkMinMax
        if(log) {
            sizeConvFactor = Math.log(maxValue / minValue) / (double)(originalFrameSize - 1);
        }
        else {
            sizeConvFactor = (maxValue - minValue) / (double)(originalFrameSize - 1);
        }
        //

        if (sendValueOnInit) {
            setControlValue(Integer.parseInt(atomline[21]));
        }

        reshape();

        // graphics setup - TODO generalize in super
        /*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);*/
    }

    //
    public void setValue(float value) {
        double g;
        value = Math.min(Math.max(value, minValue), maxValue); //DEI check

        super.setValue(value);

        if(log) { // float to pos
            g = Math.log(value / minValue) / sizeConvFactor;
        }
        else {
            g = (value - minValue) / sizeConvFactor;
        }
        _controlValue = (int)(100.0*g + 0.49999);
        controlPos = _controlValue;
    }

    private void  setControlValue(int controlValue) {
        _controlValue = controlValue;
        controlPos = controlValue;

        double g;
        if (log) { // pos to float
            g = minValue * Math.exp(sizeConvFactor * (double)(controlValue) * 0.01);
        }
        else {
            g = (double)(controlValue) * 0.01 * sizeConvFactor + minValue;
        }

        if((g < 1.0e-10) && (g > -1.0e-10)) {
            g = 0.0;
        }
        setValue((float)g);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one-time.
        float h = getHeight();

        //float strokeWidth = 1;// * scale;
        paint.setColor(bgcolor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);
        // draw outline
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(lineWidth);

        canvas.drawLine(0,0, w,0, paint);
        canvas.drawLine(0,h, w, h, paint);
        canvas.drawLine(0,0,0, h, paint);
        canvas.drawLine(w, 0, w, h, paint);

        // draw fg
        paint.setColor(fgcolor);
        float strokeWidth = scale * 3;
        paint.setStrokeWidth(strokeWidth);

        float val = getValue();
        if (isHorizontal) {
            float posX = Math.round( ((val - minValue) / (maxValue - minValue)) * w);
            if (posX < strokeWidth / 2) {
                posX = strokeWidth / 2;
            } else if (posX > w - strokeWidth / 2) {
                posX = w - strokeWidth / 2;
            }
            canvas.drawLine(posX, 0, posX, Math.round(h), paint);
        } else {
            float posY = Math.round(h - ((val - minValue) / (maxValue - minValue)) * h);
            if (posY < strokeWidth / 2) {
                posY = strokeWidth / 2;
            } else if (posY > h - strokeWidth / 2) {
                posY = h - strokeWidth / 2;
            }
            canvas.drawLine(0, posY, Math.round(w), posY, paint);
        }

        drawLabel(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        //Log.i("slider", "action  "+action+"");
        float x = event.getX();
        float y = event.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
            //val0 = getValue();
            //x0 = event.getX();
            //y0 = event.getY();
            if (isHorizontal) {
                if (!steady) {
                    int v = (int) (100.0 * (x / scale));
                    v = (int) (Math.min(Math.max(v, 0), (100 * originalFrameSize - 100)));
                    setControlValue(v); //sets value;
                }
                sendFloat(getValue());
                prevPos = x;
            } else { //vertical
                if (!steady) {
                    //setValue(isHorizontal ? getValFromHorizontalX(x0) : getValFromVerticalY(y0));
                    int v = (int) (100.0 * ((getHeight() - y) / scale));
                    v = (int) (Math.min(Math.max(v, 0), (100 * originalFrameSize - 100)));
                    setControlValue(v); //sets value;
                }
                sendFloat(getValue());
                prevPos = y;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
            if (isHorizontal) {
                float delta = (x - prevPos ) / scale;
                float old = _controlValue;
                controlPos += 100 * delta;
                int v = controlPos;

                if(v > (100 * originalFrameSize - 100)) {
                    v = (int)(100 * originalFrameSize - 100);
                    controlPos += 50;
                    controlPos -= controlPos % 100;
                }
                if(v < 0) {
                    v = 0;
                    controlPos -= 50;
                    controlPos -= controlPos % 100;
                }
                setControlValue(v);
                controlPos = v;

                // don't resend old values
                if(old != v) {
                    sendFloat(getValue());
                }

                prevPos = x;
            } else { //vertical
                float delta = (y - prevPos) / scale;
                float old = _controlValue;
                controlPos -= 100 * (int)delta;
                int v = controlPos;

                if(v > (100 * originalFrameSize - 100)) {
                    v = (int)(100 * originalFrameSize - 100);
                    controlPos += 50;
                    controlPos -= controlPos % 100;
                }
                if(v < 0) {
                    v = 0;
                    controlPos -= 50;
                    controlPos -= controlPos % 100;
                }
                setControlValue(v);
                controlPos = v;

                // don't resend old values
                if(old != v) {
                    sendFloat(getValue());
                }

                prevPos = y;
            }
        }
        return true;
    }

    //
    protected void receiveBangFromSource(String source) {
        //sendFloat(getValue());
    }

    protected void receiveFloatFromSource(float val, String source) {
        setValue(val);
        //sendFloat(getValue());
    }

    protected void receiveSetFloat(float val) {
        setValue(val);
    }
}
