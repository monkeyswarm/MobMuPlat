package com.iglesiaintermedia.mobmuplat.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.StaticLayout;
import android.text.Layout.Alignment;

public class MMPUnknown extends MMPControl {

	private RectF _myRectF;
	public String badNameString;
	private StaticLayout _staticLayout;
	
	public MMPUnknown(Context context, float screenRatio) {
		super(context, screenRatio);
		_myRectF = new RectF();
		this.paint.setTextSize(14 * this.screenRatio);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRectF.set(0,0,right-left, bottom-top);
			_staticLayout = new StaticLayout("interface object "+badNameString+" not found", this.paint, getWidth(),//vs right-left?
				Alignment.ALIGN_NORMAL, 1, 1, true);
		}
	}
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		this.paint.setStyle(Paint.Style.FILL);
		this.paint.setColor(Color.GRAY);
		canvas.drawRect(_myRectF, this.paint);
		
		this.paint.setColor(Color.WHITE);
		_staticLayout.draw(canvas);
	}
}
