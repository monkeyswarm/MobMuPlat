package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class AtomWidget extends Widget {

    //public int fontSize;
    public int labelPos; //LRUD enum val
    private float cornerSize;

    public AtomWidget(Context context, float scale, int fontSize) {
        super(context, scale);
        paint.setTextSize(fontSize * scale);
        cornerSize = 4 * scale;
    }

    public void drawLabel(Canvas canvas) {
        if (labelString != null) {
            float labelPosX = 0, labelPosY = 0;

            // compute label height
            Rect rect = new Rect();
            paint.getTextBounds(labelString, 0, labelString.length(), rect);
            float labelHeight = rect.height();

            switch(labelPos) {
                case 1: // RIGHT
                    labelPosX = getWidth() + (2 * scale);
                    labelPosY = labelHeight +  2 * scale;
                    break;
                case 2: // TOP
                    labelPosX = 0;
                    labelPosY = - (2 * scale);
                    break;
                case 3: // BOTTOM
                    labelPosX = 0;
                    labelPosY = getHeight() + labelHeight + (2 * scale);
                    break;
                default: // LEFT
                    paint.setTextAlign(Paint.Align.RIGHT);
                    labelPosX = - (2 * scale);
                    labelPosY = labelHeight + 2 * scale;
                    break;
            }

            //paint.setStrokeWidth(0); //draws text filled in instead of line
            paint.setColor(Color.BLACK);
            canvas.drawText(labelString, labelPosX, labelPosY, paint);
            //set it back
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
        float h = getHeight();
        paint.setColor(Color.BLACK);

        canvas.drawLine(0, 0, w - cornerSize, 0, paint);
        canvas.drawLine(w-cornerSize, 0, w, cornerSize, paint);
        canvas.drawLine(w,cornerSize,w,h,paint);
        canvas.drawLine(0,h, w,h, paint);
        canvas.drawLine(0, 0, 0, h, paint);

        drawLabel(canvas);
    }
}
