package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.text.DecimalFormat;

public class Symbol extends AtomWidget {

    private int charWidth;
    private String symbolValue;

    public Symbol(Context context, String[] atomline, float scale, int fontSize) {
        super(context, scale, fontSize);

        float x = Float.parseFloat(atomline[2]) * scale;
        float y = Float.parseFloat(atomline[3]) * scale;

        charWidth = Integer.parseInt(atomline[4]);
        //ignoring min/max value...
        symbolValue = "symbol";

        sendname = sanitizeLabel(atomline[10]);
        setReceiveName(sanitizeLabel(atomline[9]));
        labelString = sanitizeLabel(atomline[8]);
        labelPos = Integer.parseInt(atomline[7]);

        // graphics setup
        Rect rect = new Rect();
        paint.setTextSize(fontSize * scale);

        float width = 0;
        if (charWidth == 0) { //resize to "symbol"
            paint.getTextBounds("symbol", 0, 6, rect);
            width = rect.width();
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<charWidth;i++)sb.append("0");
            paint.getTextBounds(sb.toString(), 0, charWidth, rect);
            width = rect.width();
        }
        RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + width + 8*scale), Math.round(y + (fontSize + 4)*scale));

        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);
    }

    private void setSymbolValue(String val) {
        symbolValue = val;
        if(charWidth == 0) { //resize
            Rect rect = new Rect();
            paint.getTextBounds(symbolValue, 0, symbolValue.length(), rect);
            //getLayoutParams().width = rect.width(); //TODO test
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = (int)(rect.width()+8*scale);
            setLayoutParams(params);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); // draw box, label.
        float h = getHeight();
        String val = symbolValue;
        if (charWidth!=0 && val.length()>charWidth) { //truncate
            val = val.substring(0,charWidth-1)+">";
        }
        canvas.drawText(val, 0, h-(3*scale), paint);
    }

    //
    protected void receiveSetFloat(float val) {
        setSymbolValue("float");
    }

    protected void receiveSetSymbol(String symbol) {
        setSymbolValue(symbol);
    }

    protected void receiveBangFromSource(String source) {
        sendFloat(getValue());
    }

    protected void receiveFloatFromSource(float val, String source) {
        setSymbolValue("float");
    }

    protected void receiveSymbolFromSource(String symbol, String source) {
        setSymbolValue(symbol);
    }

}
