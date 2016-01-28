package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class NumberBox2 extends Widget {
	private DecimalFormat fmt;
	private int numWidth;
	private float min, max;
	private float touchYPrev;

	public NumberBox2(Context context, String[] atomline, float scale, float fontSize) {
		super(context, scale);
		float x = Float.parseFloat(atomline[2]) * scale ;
		float y = Float.parseFloat(atomline[3]) * scale ;
		//Rect tRect = new Rect();
		
		// calculate screen bounds for the numbers that can fit
		numWidth = Integer.parseInt(atomline[5]);
		StringBuffer calclen = new StringBuffer();
		for (int s=0; s<numWidth; s++) {
			if (s == 1) {
				calclen.append(".");
			} else {
				calclen.append("#");
			}
		}
		fmt = new DecimalFormat(calclen.toString());
		
		min = Float.parseFloat(atomline[7]);
		max = Float.parseFloat(atomline[8]);
		sendValueOnInit = Integer.parseInt(atomline[10]) > 0;
		sendname = atomline[11];//app.app.replaceDollarZero(atomline[11]);
		setReceiveName(atomline[12]);
		label = sanitizeLabel(atomline[13]);
		labelpos[0] = Float.parseFloat(atomline[14]) ;
		labelpos[1] = Float.parseFloat(atomline[15]) ;
		
		// set the value to the init value if needed
		if (sendValueOnInit) {
			setValue(Float.parseFloat(atomline[21]));
			// To be sent out on post-init method.
		}		
		
		Rect rect = new Rect();
		paint.setTextSize(fontSize * scale);
		paint.getTextBounds(calclen.toString(), 0, calclen.length(), rect);
		float h = rect.height();
		rect.right += h*.75;//add width for triangle (h/2) and notch (h/4)
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
		float notch = h / 4;
		canvas.drawLine(0, 0, w - notch, 0, paint); //top
		canvas.drawLine(0,0,0,h, paint); //left
		canvas.drawLine(w, notch, w, h, paint); //right
		canvas.drawLine(w - notch, 0, w, notch, paint); //notch
		canvas.drawLine(0,h,w,h,paint);//bottom
		float triangleWidth = h/2;
		canvas.drawLine(0,0,triangleWidth, triangleWidth,paint); //tri top
		canvas.drawLine(0,h,triangleWidth, triangleWidth ,paint); //tri bottom
		//canvas.drawText("" + fmt.format(getValue()), 3f,h*.75f, paint);
		float val = getValue();
		String string = stringForFloat(val, numWidth, fmt);
		canvas.drawText(string, triangleWidth, h, paint); //h/2 is triangle dim
	}

	//TODO consolidate this with number box.
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		Log.i("Number Box", "action "+action);
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
