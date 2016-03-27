package com.iglesiaintermedia.mobmuplat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.iglesiaintermedia.mobmuplat.controls.MMPMenu;

public class MMPMenuButton extends RelativeLayout{

    private static final float PADDING_PERCENT = .1f;
    private static final float INTRABAR_PERCENT = .1f;
    private Paint paint;
    private int barColor;

    public MMPMenuButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barColor = Color.BLACK;
    }

    public void setBarColor(int color) {
        barColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();

        //paint.setColor(Color.BLUE);
        //paint.setStyle(Paint.Style.FILL);
        //canvas.drawPaint(paint);
        // draw outline
        paint.setColor(barColor);
        //paint.setStrokeWidth(lineWidth);

        float barHorizPadding = PADDING_PERCENT * w;
        float barVerticalPadding = PADDING_PERCENT *h;
        float intraBarHeight = INTRABAR_PERCENT * h;
        float barHeight = (h - intraBarHeight * 2 - barVerticalPadding * 2) / 3.0f;
        float barWidth = w - barHorizPadding * 2;
        for (int i=0;i<3;i++) {
            //CGRect barFrame =
              //      CGRectMake(barHorizPadding, barVerticalPadding + i * (barHeight + intraBarHeight), barWidth, barHeight);
            //CGContextFillRect(context, barFrame);
            float top = barVerticalPadding + i * (barHeight + intraBarHeight);
            canvas.drawRect(barHorizPadding, top, barHorizPadding + barWidth, top + barHeight, paint);
        }

    }
}
