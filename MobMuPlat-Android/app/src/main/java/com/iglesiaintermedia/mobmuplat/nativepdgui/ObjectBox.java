package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.widget.RelativeLayout;

import java.util.Arrays;

public class ObjectBox extends AtomWidget {
    private String symbolValue;

    public ObjectBox(Context context, String[] atomline, float scale, int fontSize) {
        super(context, scale, fontSize);
        //TODO check length >=5
        float x = Float.parseFloat(atomline[2]) * scale;
        float y = Float.parseFloat(atomline[3]) * scale;

        symbolValue = TextUtils.join(" ", Arrays.copyOfRange(atomline, 4, atomline.length));
        //ignoring min/max value...

        // graphics setup
        Rect rect = new Rect();
        paint.setTextSize(fontSize * scale);
        paint.getTextBounds(symbolValue, 0, symbolValue.length(), rect);
        RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + rect.width() + 4*scale), Math.round(y + (fontSize + 4)*scale));

        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
        float h = getHeight();
        paint.setColor(Color.BLACK);

        canvas.drawLine(0,0, w,0, paint);
        canvas.drawLine(0,h, w,h, paint);
        canvas.drawLine(0,0,0,h, paint);
        canvas.drawLine(w,0,w,h, paint);
        float val = getValue();
        canvas.drawText(symbolValue, 0, h-(3*scale), paint);

        drawLabel(canvas);
    }
}
