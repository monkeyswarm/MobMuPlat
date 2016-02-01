package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class Radio extends Widget {
	float size; //i.e. width/height, post-scale
	int count;
	boolean isHorizontal;
	public Radio(Context context, String[] atomline, float scale, boolean isHorizontal) {
		// TODO check atomline length >= 20.
		super(context, scale);
		this.isHorizontal = isHorizontal;
		float x = Float.parseFloat(atomline[2]) * scale;
		float y = Float.parseFloat(atomline[3]) * scale;
		
		size = Integer.parseInt(atomline[5]) * scale;
		count = Integer.parseInt(atomline[8]);

		float w,h;
		if (isHorizontal) {
			 w = size * count;
			 h = size;
		} else {
			 w = size;
			 h	= size * count;
		}
		
		// index 6 is the "new/old" switch, unused. TODO update iOS
		
		sendValueOnInit = Integer.parseInt(atomline[7]) > 0;  //DEI check that send val on init works.
		sendname = atomline[9]; //app.app.replaceDollarZero(atomline[7]); TODO handle dollar sign zero. necc? Never more than one instance...
		setReceiveName(atomline[10]);
		label = sanitizeLabel(atomline[11]);
		labelpos[0] = Float.parseFloat(atomline[12]) ;
		labelpos[1] = Float.parseFloat(atomline[13]) ;
		labelfont = Integer.parseInt(atomline[14]);
		labelsize = (int)(Float.parseFloat(atomline[15]) );
		bgcolor = getColor(Integer.parseInt(atomline[16]));
		fgcolor = getColor(Integer.parseInt(atomline[17]));
		labelcolor = getColor(Integer.parseInt(atomline[18]));

		// if on, set and send val.
		if (sendValueOnInit) {
			setValue(Float.parseFloat(atomline[19])); //DEI check that send val on init works.
			//sendFloat(getValue());//DEI HERE sent before other things are ready!
		}
		
		// graphics setup
		RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),Math.round(y + h));
		
		setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
		setX(dRect.left);
		setY(dRect.top);
	}
	
	public void draw(Canvas canvas) {
		float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
		float h = getHeight();
		
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(bgcolor);
		canvas.drawPaint(paint); //fill whole canvas

		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(1);
		canvas.drawLine(0,0, w,0, paint);
		canvas.drawLine(0,h, w,h, paint);
		canvas.drawLine(0,0,0,h, paint);
		canvas.drawLine(w,0,w,h, paint);
		
		// dividers
		for (int i = 1; i < count; i++) {
			if (isHorizontal) {
				canvas.drawLine(i * size, 0, i * size, h, paint);
			} else {
				canvas.drawLine(0, i * size, w, i * size, paint);
			}
		}
		// selected square
		float value = getValue();
		paint.setColor(fgcolor);
		float cellOffset = size/4;
		if (isHorizontal) {
			canvas.drawRect(value * size + cellOffset, cellOffset, (value + 1) * size - cellOffset, h - cellOffset, paint);
		} else {
			canvas.drawRect(cellOffset, value * size + cellOffset, w - cellOffset, (value + 1) * size - cellOffset, paint);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
        	float val;
        	if (isHorizontal) {
        		val = (int)(event.getX() / size);
        	} else {
        		val = (int)(event.getY() / size);
        	}
        	setValue(val);
        	sendFloat(getValue());
        }
        return true;
	}
	
	//
	protected void receiveSetFloat(float inVal) {
		setValue(inVal);
	}
	
	protected void receiveBangFromSource(String source) {
		sendFloat(getValue());
	}
	
	protected void receiveFloatFromSource(float inVal, String source) {
		setValue(inVal);
		sendFloat(getValue());
	}
}
