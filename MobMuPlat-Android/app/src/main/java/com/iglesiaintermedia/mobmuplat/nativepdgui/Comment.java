package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.widget.RelativeLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Comment extends Widget {
    static final int lineWrapSize = 60;
    float lineHeight;
    List<String> stringList = new ArrayList<String>();

    public Comment(Context context, String[] atomline, float scale, int fontSize) {
		super(context, scale);
        paint.setTextSize(fontSize * scale);
        //paint.setStrokeWidth(1); //draws text filled in instead of line
        paint.setColor(Color.BLACK);
        lineHeight = fontSize * scale;

        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);

        // create the comment string as an array of lines, handle escaped chars
        stringList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(); //rebuilt on each line
        boolean spaceBeforeAtom = false;
        for (int i = 4; i<atomline.length;i++) {
            String atom = atomline[i];
            if (atom.equals("\\,")) {
                atom = ",";
                spaceBeforeAtom = false;
            } else if (atom.equals("\\;")) {
                atom = ";";
                spaceBeforeAtom = false;
            } else if (atom.equals("\\$")) {
                atom = "$";
            }

            while (atom.length() > lineWrapSize)  { // chop down super long words
                // add any existing stuff in buffer as a line
                if (sb.length() > 0) {
                    stringList.add(sb.toString());
                    sb.setLength(0); //clear buffer
                }
                stringList.add(atom.substring(0,lineWrapSize)); // first 60 as line
                atom = atom.substring(lineWrapSize); // second chunk
            }

            if (sb.length() + atom.length() > lineWrapSize) { //new line, then append atom
                stringList.add(sb.toString());
                sb.setLength(0); //clear buffer
                sb.append(atom);
            } else if (atom.equals(";")) { //append, then new line
                sb.append(atom);
                stringList.add(sb.toString());
                sb.setLength(0); //clear buffer
            } else { //atom, with space
                if (spaceBeforeAtom)sb.append(" ");
                sb.append(atom);
                spaceBeforeAtom = true;
            }
        }
        // add remainder of string builder as string
        stringList.add(sb.toString());

        // graphics setup
        int maxWidth = 0;

        Rect rect = new Rect();
        for (String stringLine : stringList) {
            paint.getTextBounds(stringLine, 0, stringLine.length(), rect);
            if (rect.width() > maxWidth) {
                maxWidth = rect.width();
            }
        }
        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + maxWidth),
                Math.round(y + (lineHeight * stringList.size())));
        reshape();
        /*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + maxWidth), Math.round(y + (lineHeight * stringList.size())));
        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);*/
	}


    @Override
    protected void onDraw(Canvas canvas) {
        /*    float w = getWidth();
            float h = getHeight();
            canvas.drawLine(0,0, w,0, paint);
            canvas.drawLine(0,h, w,h, paint);
            canvas.drawLine(0,0,0,h, paint);
            canvas.drawLine(w,0,w,h, paint);
*/

        float yPos = lineHeight;
        for (String stringLine : stringList) {
            canvas.drawText(stringLine, 0,yPos, paint);
            yPos += lineHeight;
        }
    }
}
