package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class NumberBox2 extends IEMWidget {
    private DecimalFormat fmt;
    private int numWidth;
    private float min, max;
    private float touchYPrev;
    private boolean log, steady;
    private float logHeight;
    private double convFactor;

    public NumberBox2(Context context, String[] atomline, float scale, float fontSize) {
        super(context, scale);
        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);

        setLogHeight(256);

        // calculate screen bounds for the numbers that can fit
        numWidth = Integer.parseInt(atomline[5]);
        StringBuffer calclen = new StringBuffer();
        for (int s=0; s<numWidth; s++) {
            if (s == 1) {
                calclen.append(".");
            } else {
                calclen.append("#");
            }
        }
        fmt = new DecimalFormat(calclen.toString());

        min = Float.parseFloat(atomline[7]);
        max = Float.parseFloat(atomline[8]);
        log = Integer.parseInt(atomline[9]) > 0;
        if (log) {
            checkMinAndMax();
        }
        sendValueOnInit = Integer.parseInt(atomline[10]) > 0;
        sendname = sanitizeLabel(atomline[11]);
        setReceiveName(sanitizeLabel(atomline[12]));
        labelString = sanitizeLabel(atomline[13]);
        labelpos[0] = Float.parseFloat(atomline[14]) ;
        labelpos[1] = Float.parseFloat(atomline[15]) ;
        labelsize = Integer.parseInt(atomline[17]) ;
        fontSize = labelsize;

        if (sendValueOnInit) {
            setValue(Float.parseFloat(atomline[21]));
        }

        if(atomline.length > 22) {
            logHeight = Float.parseFloat(atomline[22]);
        }

        Rect rect = new Rect();
        paint.setTextSize(fontSize * scale);
        paint.getTextBounds(calclen.toString(), 0, calclen.length(), rect);

        rect.right += rect.height() *.75;//add width for triangle (h/2) and notch (h/4)
        RectF dRect = new RectF(rect);
        dRect.sort();

        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + dRect.width()/scale),
                Math.round(y + dRect.height()/scale+ 4));
        reshape();
//        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)(dRect.height()+ 4*scale)));
//        setX(dRect.left);
//        setY(dRect.top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
        float h = getHeight();
        paint.setColor(bgcolor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);
        float notch = h / 4;
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(lineWidth);
        canvas.drawLine(0, 0, w - notch, 0, paint); //top
        canvas.drawLine(0,0,0,h, paint); //left
        canvas.drawLine(w, notch, w, h, paint); //right
        canvas.drawLine(w - notch, 0, w, notch, paint); //notch
        canvas.drawLine(0,h,w,h,paint);//bottom
        float triangleWidth = h/2;
        canvas.drawLine(0,0,triangleWidth, triangleWidth,paint); //tri top
        canvas.drawLine(0,h,triangleWidth, triangleWidth ,paint); //tri bottom
        //canvas.drawText("" + fmt.format(getValue()), 3f,h*.75f, paint);
        float val = getValue();
        String string = stringForFloat(val, numWidth, fmt);
        paint.setColor(fgcolor);
        canvas.drawText(string, triangleWidth, h-(2*scale), paint);
        drawLabel(canvas);
    }

    //TODO consolidate this with number box.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        //Log.i("Number Box", "action "+action);
        if (action == MotionEvent.ACTION_DOWN) {
            touchYPrev = event.getY();
        } else if (action == MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view - move?
            float yPos = event.getY();
            float diff = (touchYPrev - yPos) / scale;
            if(diff != 0) {
                double v = getValue();
                if(log) {
                    v *= Math.pow(convFactor, -diff);
                }
                else {
                    v += diff;
                }
                setValue((float)v);
                sendFloat((float)v);
            }
            touchYPrev = yPos;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touchYPrev = 0;
        }
        return true;
    }

    public void setValue(float value) {
        if (min != 0 || max != 0) {
            value = Math.min(Math.max(value, min), max);
        }
        //TODO red color.
        super.setValue(value);
    }

    private void setLogHeight(float logHeight) {
        if(logHeight < 10) {
            logHeight = 10;
        }
        this.logHeight = logHeight;
        if (log) {
            convFactor = Math.exp(Math.log(max / min) / (double)logHeight);
        } else {
            convFactor = 1.0;
        }
    }

    private void checkMinAndMax() {
        if(log) {
            if((min == 0.0) && (max == 0.0)) {
                max = 1.0f;
            }
            if(max > 0.0) {
                if(min <= 0.0) {
                    min = 0.01f * max;
                }
            }
            else {
                if(min > 0.0) {
                    max = 0.01f * min;
                }
            }
        }
        if(getValue() < min) {
            setValue(min);
        }
        if(getValue() > max) {
            setValue(max);
        }
        if(log) {
            convFactor = Math.exp(Math.log(max / min) / (double)(logHeight));
        }
        else {
            convFactor = 1.0;
        }
    }

    //
    protected void receiveSetFloat(float val) {
        setValue(val);
    }

    protected void receiveSetSymbol(String val) {
        // swallows set symbols
    }

    protected void receiveBangFromSource(String source) {
        sendFloat(getValue());
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
