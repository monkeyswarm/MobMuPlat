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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageBox extends AtomWidget {
    static final int lineWrapSize = 60;
    static final int verticalTextOffset = 0;
    static final int rightPadding = 8;
    static final int bottomPadding = 4;

    float lineHeight;
    //private String symbolValue;
    private boolean highlighted;
    List<String> stringList = new ArrayList<String>();

    public MessageBox(Context context, String[] atomline, float scale, int fontSize) {
        super(context, scale, fontSize);
        //TODO check length >=5
        float x = Float.parseFloat(atomline[2]);
        float y = Float.parseFloat(atomline[3]);


        //last two atoms (added via post-MMP-processing) will be the send/rec names!!!
        //symbolValue = TextUtils.join(" ", Arrays.copyOfRange(atomline, 4, atomline.length-2));
        sendname = sanitizeLabel(atomline[atomline.length - 2]);
        setReceiveName(sanitizeLabel(atomline[atomline.length-1]));
        //ignoring min/max value...

        lineHeight = fontSize; //* scale;

        setText(Arrays.copyOfRange(atomline, 4, atomline.length - 2));

        // graphics setup
        int maxWidth = 0; //maxWidth is initially computed as post-scaled, since "paint" has a scaled font size.

        Rect rect = new Rect();
        for (String stringLine : stringList) {
            paint.getTextBounds(stringLine, 0, stringLine.length(), rect);
            if (rect.width() > maxWidth) {
                maxWidth = rect.width();
            }
        }
        maxWidth += lineWidth+(rightPadding*scale);
        maxWidth /= scale;

        originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + maxWidth),
                Math.round(y + (lineHeight * stringList.size()) + bottomPadding));
        reshape();
        /*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + maxWidth), Math.round(y + (lineHeight * stringList.size()) + (bottomPadding*scale)));
        setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
        setX(dRect.left);
        setY(dRect.top);*/
    }

    private void setText(Object[] textAtoms){
        // create the comment string as an array of lines, handle escaped chars
        stringList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(); //rebuilt on each line
        boolean spaceBeforeAtom = false;
        for (int i = 0; i<textAtoms.length;i++) {
            String atom;
            if (textAtoms[i] instanceof String) {
                atom = (String)textAtoms[i];
            } else if (textAtoms[i] instanceof Float) {
                atom = ((Float)(textAtoms[i])).toString();
            } else {
                continue;
            }

            if (atom.equals("\\,")) {
                atom = ",";
                spaceBeforeAtom = false;
            } else if (atom.equals("\\;")) {
                atom = ";";
                spaceBeforeAtom = false;
            } else if (atom.equals("\\$")) {
                atom = "$";
            }

            // parse

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
                //if (i == textAtoms.length - 1)
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
        if (sb.length()>0) {
            stringList.add(sb.toString());
        }
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
        canvas.drawLine(0, h, w, h, paint);
        float val = getValue();
        //canvas.drawText(symbolValue, 0, h - (3 * scale), paint);

        //drawLabel(canvas);
        float yPos = (lineHeight - verticalTextOffset)*scale;
        for (String stringLine : stringList) {
            canvas.drawText(stringLine, 0,yPos, paint);
            //canvas.drawCircle(0,yPos,5,paint);
            yPos += lineHeight;
        }
    }

    //
    /*protected void receiveSetSymbol(String val) {
        //symbolValue = val;
        Rect rect = new Rect();
        //paint.getTextBounds(symbolValue, 0, symbolValue.length(), rect);
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = (int)(rect.width()+ 8*scale);
        setLayoutParams(params);
        invalidate();
    }*/

    public void receiveMessage(String source, String message, Object... args) {
        if(message.equals("set") ) {
            setText(args);
            refreshLayout();
        }
    }

    private void refreshLayout() {
        //re-layout
        int maxWidth = 0;
        Rect rect = new Rect();
        for (String stringLine : stringList) {
            paint.getTextBounds(stringLine, 0, stringLine.length(), rect);
            if (rect.width() > maxWidth) {
                maxWidth = rect.width();
            }
        }
        maxWidth += lineWidth+(rightPadding*scale);
        maxWidth /= scale;

        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = maxWidth;
        params.height = (int)((lineHeight * stringList.size()) + (bottomPadding/**scale*/));
        setLayoutParams(params);
        invalidate();
    }
}