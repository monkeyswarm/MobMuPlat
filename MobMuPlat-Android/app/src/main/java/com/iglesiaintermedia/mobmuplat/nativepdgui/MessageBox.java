package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.Arrays;

public class MessageBox extends AtomWidget {

    private String symbolValue;
    private boolean highlighted;

    public MessageBox(Context context, String[] atomline, float scale, int fontSize) {
        super(context, scale, fontSize);
        //TODO check length >=5
        float x = Float.parseFloat(atomline[2]) * scale;
        float y = Float.parseFloat(atomline[3]) * scale;

        //last two atoms (added via post-MMP-processing) will be the send/rec names!!!
        symbolValue = TextUtils.join(" ", Arrays.copyOfRange(atomline, 4, atomline.length-2));
        sendname = sanitizeLabel(atomline[atomline.length - 2]);
        setReceiveName(sanitizeLabel(atomline[atomline.length-1]));
        //ignoring min/max value...

        // graphics setup
        paint.setColor(Color.BLACK);
        Rect rect = new Rect();
        paint.setTextSize(fontSize * scale);
        paint.getTextBounds(symbolValue, 0, symbolValue.length(), rect);
        RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + rect.width() + 8*scale), Math.round(y + (fontSize + 4)*scale));

        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            sendBang();
            highlighted = true; //start
            invalidate();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    highlighted = false; //stop
                    invalidate();
                }
            }, 250);
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
        float h = getHeight();
        paint.setStrokeWidth(highlighted ? 5 : lineWidth);
        float indent = 4 * scale;
        canvas.drawLine(0,0, w,0, paint);
        canvas.drawLine(w,0, w-indent,indent, paint);
        canvas.drawLine(w-indent,indent, w-indent,h-indent, paint);
        canvas.drawLine(w-indent,h-indent, w,h, paint);
        canvas.drawLine(0,0,0,h, paint);
        canvas.drawLine(0,h,w,h, paint);
        float val = getValue();
        canvas.drawText(symbolValue, 0, h-(3*scale), paint);

        drawLabel(canvas);
    }

    //
    protected void receiveSetSymbol(String val) {
        symbolValue = val;
        Rect rect = new Rect();
        paint.getTextBounds(symbolValue, 0, symbolValue.length(), rect);
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = (int)(rect.width()+ 8*scale);
        setLayoutParams(params);
        invalidate();
    }
}
