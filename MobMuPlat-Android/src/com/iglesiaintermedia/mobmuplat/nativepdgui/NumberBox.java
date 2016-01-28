package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class NumberBox extends Widget {
	private DecimalFormat fmt;
	private int numWidth;
	private float min, max;
	private float touchYPrev;

	public NumberBox(Context context, String[] atomline, float scale, int fontSize) {
		super(context, scale);

		float x = Float.parseFloat(atomline[2]) * scale;
		float y = Float.parseFloat(atomline[3]) * scale;
		
		// calculate screen bounds for the numbers that can fit
		numWidth = Integer.parseInt(atomline[4]);
		StringBuffer calclen = new StringBuffer();
		for (int s=0; s<numWidth; s++) {
			if (s == 1) {
				calclen.append(".");
			} else {
				calclen.append("#");
			}
		}
		fmt = new DecimalFormat(calclen.toString());
		
		min = Float.parseFloat(atomline[5]);
		max = Float.parseFloat(atomline[6]);
		sendname = atomline[10];//app.app.replaceDollarZero(atomline[10]);
		setReceiveName(atomline[9]);
		label = sanitizeLabel(atomline[8]);
		labelpos[0] = x; // TODO label pos??? NONONONONO
		labelpos[1] = y;
		
		//setval(0, 0);
		
		// listen out for floats from Pd
		//setupreceive();
		
		// graphics setup
				Rect rect = new Rect();
				paint.setTextSize(fontSize * scale);
				paint.getTextBounds(calclen.toString(), 0, calclen.length(), rect);
				RectF dRect = new RectF(rect);
				dRect.sort();
				dRect.offset((int)x, (int)y + fontSize * scale);
				
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
		String string = stringForFloat(val, numWidth, fmt);
		//Log.i("NUM", "draw val "+val+" width "+numWidth+" string "+string);
		canvas.drawText(string, 0, h, paint);
		
		drawLabel(canvas);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		//Log.i("Number Box", "action "+action);
        if (action == MotionEvent.ACTION_DOWN) {
        	touchYPrev = event.getY();
        } else if (action == MotionEvent.ACTION_MOVE) {
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view - move?
        	float yPos = event.getY();
        	float diff = touchYPrev - yPos;
        	touchYPrev = yPos;
        	setValue(getValue()+(diff/scale));
        	sendFloat(getValue());
        }
        return true;
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
		sendFloat(getValue());
	}
	
	protected void receiveSymbolFromSource(String symbol, String source) {
		setValue(0);
		sendFloat(getValue());
	}
}
