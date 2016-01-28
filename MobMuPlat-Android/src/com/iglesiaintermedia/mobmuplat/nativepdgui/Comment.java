package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;
import android.graphics.Canvas;

public class Comment extends Widget {
	
	public Comment(Context context, String[] atomline, float scale) {
		super(context, scale);

		// create the comment string
		StringBuffer buffer = new StringBuffer();
		for (int i=4; i<atomline.length; i++) {
			buffer.append(atomline[i]);
				if (i < atomline.length - 1) {
					buffer.append(" ");
				}
			}
				
		label = buffer.toString();
		labelpos[0] = Float.parseFloat(atomline[2]) ;
		labelpos[1] = Float.parseFloat(atomline[3]) ;
	}
			
	
	public void draw(Canvas canvas) {
		drawLabel(canvas);
	}


}
