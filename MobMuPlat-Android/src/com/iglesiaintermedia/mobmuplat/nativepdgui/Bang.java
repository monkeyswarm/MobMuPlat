package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class Bang extends Widget {

	float holdTime;
	float interrupt;
	boolean bangIsOn;
	
	public Bang(Context context, String[] atomline, float scale) {
		super(context, scale);

		float x = Float.parseFloat(atomline[2]) * scale;
		float y = Float.parseFloat(atomline[3]) * scale;
		float w = Float.parseFloat(atomline[5]) * scale;
		float h = Float.parseFloat(atomline[5]) * scale;
		
		holdTime = (int)Float.parseFloat(atomline[6]) ;
		interrupt = (int)Float.parseFloat(atomline[7]) ;
		sendValueOnInit = Integer.parseInt((atomline[8])) > 0 ; //DEI check val

		sendname = atomline[9];//DEI app.app.replaceDollarZero(atomline[9]);
		setReceiveName(atomline[10]);
		label = sanitizeLabel(atomline[11]);
		labelpos[0] = Float.parseFloat(atomline[12]) ;
		labelpos[1] = Float.parseFloat(atomline[13]) ;
		labelfont = Integer.parseInt(atomline[14]);
		labelsize = (int)(Float.parseFloat(atomline[15]));
		bgcolor = getColor(Integer.parseInt(atomline[16]));
		fgcolor = getColor(Integer.parseInt(atomline[17]));
		labelcolor = getColor(Integer.parseInt(atomline[18]));
		
		// graphics setup
		RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),Math.round(y + h));
		setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
		setX(dRect.left);
		setY(dRect.top);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
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
		if (bangIsOn) {
			paint.setColor(fgcolor);
			canvas.drawCircle(w/2, h/2, Math.min(w,h) / 2, paint);
		}
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawCircle(w/2, h/2, Math.min(w,h) / 2, paint);
		drawLabel(canvas);
	}

	// TODO handle interrupt time.
	private void bang() {
		bangIsOn = true;
		invalidate();
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
		  @Override
		  public void run() {
		    bangIsOn = false;
		    invalidate();
		  }
		}, (long)holdTime);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
			bang();
			sendBang();
		}
		return false;
	}
	//
	//
	protected void receiveBangFromSource(String source) {
		bang();
		sendBang();
	}

	protected void receiveFloatFromSource(float val, String source) {
		bang();
		sendBang();
	}

	public void receiveMessage(String source, String message, Object... args) {
		// need to ignore scripting messages?
		bang();
		sendBang();
	}
	
	public void receiveList(String source, Object... args) {
		bang();
		sendBang();
	}
}
