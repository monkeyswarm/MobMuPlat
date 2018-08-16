package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.widget.RelativeLayout;
import android.graphics.Paint;

public class CanvasRect extends IEMWidget {

	public CanvasRect(Context context, String[] atomline, float scale) {
		super(context, scale);

		float x = Float.parseFloat(atomline[2]);
		float y = Float.parseFloat(atomline[3]);
		float w = Float.parseFloat(atomline[6]);
		float h = Float.parseFloat(atomline[7]);
		originalRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),
				Math.round(y + h));
		
		sendname = atomline[8];
		setReceiveName(atomline[9]);
		labelString = sanitizeLabel(atomline[10]);
		labelpos[0] = Float.parseFloat(atomline[11]);
		labelpos[1] = Float.parseFloat(atomline[12]);
		labelfont = Integer.parseInt(atomline[13]);
		labelsize = (int)(Float.parseFloat(atomline[14]));
		bgcolor = getColor(Integer.parseInt(atomline[15]));
		labelcolor = getColor(Integer.parseInt(atomline[16]));

		reshape();
		// graphics setup
		/*RectF dRect = new RectF(Math.round(x), Math.round(y), Math.round(x + w),Math.round(y + h));
		setLayoutParams(new RelativeLayout.LayoutParams((int)dRect.width(), (int)dRect.height()));
		setX(dRect.left);
		setY(dRect.top);*/
	}
	
	//@Override
	protected void onDraw(Canvas canvas) {
		float w = getWidth(); //TODO check if this is much slower than storing as an ivar one0time.
		float h = getHeight();
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(bgcolor);			
		canvas.drawRect(0,0, w, h, paint); 
		drawLabel(canvas);
	}
}
