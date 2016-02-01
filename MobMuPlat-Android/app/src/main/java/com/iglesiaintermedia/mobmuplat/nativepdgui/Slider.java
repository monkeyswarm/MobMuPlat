package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class Slider extends Widget {
	private static final String TAG = "Slider";
	
	private boolean isHorizontal;
	
	private float min, max, steady; //steady 1 = don't jump on touch down
	private boolean log; 
	private float val0, x0, y0;
	
	public Slider(Context context, String[] atomline, float scale, boolean isHorizontal) {
		super(context, scale);
		this.isHorizontal = isHorizontal;
		
		float x = Float.parseFloat(atomline[2]) * scale;
		float y = Float.parseFloat(atomline[3]) * scale;
		float w = Float.parseFloat(atomline[5]) * scale;
		float h = Float.parseFloat(atomline[6]) * scale;
		
		min = Float.parseFloat(atomline[7]);
		max = Float.parseFloat(atomline[8]);
		log = Integer.parseInt(atomline[9]) > 0; //DEI check
		sendValueOnInit = Integer.parseInt(atomline[10]) > 0; //DEI check
		sendname = atomline[11];//app.app.replaceDollarZero(atomline[11]);
		setReceiveName(atomline[12]);
		label = sanitizeLabel(atomline[13]);
		labelpos[0] = Float.parseFloat(atomline[14]) ;
		labelpos[1] = Float.parseFloat(atomline[15]) ;
		labelfont = Integer.parseInt(atomline[16]);
		labelsize = (int)(Float.parseFloat(atomline[17]));
		bgcolor = getColor(Integer.parseInt(atomline[18]));
		fgcolor = getColor(Integer.parseInt(atomline[19]));
		labelcolor = getColor(Integer.parseInt(atomline[20]));
		steady = Integer.parseInt(atomline[22]);

		//DEI handle set value on init, min value alternate
		setValue((float)(Float.parseFloat(atomline[21]) * 0.01 * (max - min) / ((isHorizontal ? Float.parseFloat(atomline[5]) : Float.parseFloat(atomline[6])) - 1) + min));

		// graphics setup - TODO generalize in super
		RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h));
		setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
		setX(dRect.left);
		setY(dRect.top);
	}
	
	public void draw(Canvas canvas) {
		float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
		float h = getHeight();
		
		paint.setColor(bgcolor);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawPaint(paint);

			paint.setColor(Color.BLACK);
			//paint.setStrokeWidth(scale);
			
			canvas.drawLine(0,0, w,0, paint);
			canvas.drawLine(0,h, w,h, paint);
			canvas.drawLine(0,0,0,h, paint);
			canvas.drawLine(w,0,w,h, paint);
			
			paint.setColor(fgcolor);
			float strokeWidth = 3 * scale;
			paint.setStrokeWidth(strokeWidth);
			
			float val = getValue();
			if (isHorizontal) {
				float posX = Math.round( ((val - min) / (max - min)) * w);
				if (posX < strokeWidth / 2) {
					posX = strokeWidth / 2;
				} else if (posX > w - strokeWidth / 2) {
					posX = w - strokeWidth / 2;
				}
				canvas.drawLine(posX, 0, posX, Math.round(h), paint);
			} else {
				float posY = Math.round(h - ((val - min) / (max - min)) * h);
				if (posY < strokeWidth / 2) {
					posY = strokeWidth / 2;
				} else if (posY > h - strokeWidth / 2) {
					posY = h - strokeWidth / 2;
				}
				canvas.drawLine(0, posY, Math.round(w), posY, paint);
			}

		/*else if (!slider.none()) {
			if (orientation_horizontal) {
				sRect.offsetTo((val - min) / (max - min) * (dRect.width() - sRect.width()) + dRect.left, dRect.top);
			} else {
				sRect.offsetTo(dRect.left, (1 - (val - min) / (max - min)) * (dRect.height() - sRect.height()) + dRect.top);
			}
			slider.draw(canvas,sRect);
		}*/
		drawLabel(canvas);
	}
	
	//
	public float getValFromHorizontalX(float x) {
		return ((x  / getWidth()) * (max - min) + min);
	}
	
	public float getValFromVerticalY(float y) {
		float h = getHeight();
		return (((h - y) / h) * (max - min) + min);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		//Log.i("slider", "action  "+action+"");
        if (action == MotionEvent.ACTION_DOWN) {
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
        	val0 = getValue();
        	x0 = event.getX();
        	y0 = event.getY();
        	if (steady == 0) {
        		setValue(isHorizontal ? getValFromHorizontalX(x0) : getValFromVerticalY(y0));
        	}
        	sendFloat(getValue());
        } else if (action == MotionEvent.ACTION_MOVE) {
        	getParent().requestDisallowInterceptTouchEvent(true);// dont' send touches up to scroll view
        	float val;
        	if (isHorizontal) {
				val = steady * val0 + getValFromHorizontalX(event.getX()) - getValFromHorizontalX(x0) * steady; //DEI refactor
			} else {
				val = steady * val0 + getValFromVerticalY(event.getY()) - getValFromVerticalY(y0) * steady;
			}
        	//Log.i("slider", "move val "+val);
        	//clip to range
        	val = Math.min(max, (Math.max(min, val )));
        	
        	setValue(val);
        	sendFloat(getValue());
        }
        return true;
        
	}
	
	//
	protected void receiveBangFromSource(String source) {
		sendFloat(getValue());
	}

	protected void receiveFloatFromSource(float val, String source) {
		setValue(val);
		sendFloat(getValue());
	}

	protected void receiveSetFloat(float val) {
		setValue(val);
	}
}
