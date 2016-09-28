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

        // parse off first 4 to get the text array
        atomline = Arrays.copyOfRange(atomline, 4, atomline.length);
        int overridenCharWidth = 0;
        // try to derive the ", f N" at the end for manually resized objects. PdParser does not put comma into
        // its own element, so we need to scan and see if any element ends with it.
        int indexOfElementWithTrailingComma = -1;//Arrays.asList(atomline).indexOf(",");
        for (int i=0; i<atomline.length; i++) {
            if (atomline[i].endsWith(",")) {
                indexOfElementWithTrailingComma = i;
                // strip off comma from the text element
                atomline[i] = atomline[i].substring(0,atomline[i].length()-1);
                break;
            }
        }

        if (indexOfElementWithTrailingComma > -1) { // found comma, parse it off and grab width
            if (atomline.length >= indexOfElementWithTrailingComma + 3 && atomline[indexOfElementWithTrailingComma + 1].equals("f")) {
                overridenCharWidth = Integer.parseInt(atomline[indexOfElementWithTrailingComma + 2]);
            }
            // slice off after comma
            atomline = Arrays.copyOfRange(atomline, 0, indexOfElementWithTrailingComma+1);
        }
        symbolValue = TextUtils.join(" ", Arrays.copyOfRange(atomline, 0, atomline.length));

        //ignoring min/max value...

        // graphics setup
        Rect rect = new Rect();
        paint.setTextSize(fontSize * scale);
        paint.getTextBounds(symbolValue, 0, symbolValue.length(), rect);
        float width = rect.width();

        if (overridenCharWidth>0) {
            Rect singleCharWidthRect = new Rect();
            paint.getTextBounds("0", 0, 1, singleCharWidthRect);
            width = singleCharWidthRect.width() * overridenCharWidth;
        }

        RectF dRect = new RectF(
                Math.round(x),
                Math.round(y),
                Math.round(x + width+4*scale),
                Math.round(y + (fontSize + 4)*scale)); //todo use fontMetrics to get height

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
