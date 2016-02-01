package com.iglesiaintermedia.mobmuplat.controls;

import java.util.List;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class MMPControl extends RelativeLayout {
	
	public String address;
	public int color;
	public int highlightColor;
	public ControlDelegate controlDelegate;
	protected TextPaint paint;
	protected float screenRatio;
	public boolean enabled;
	
	public MMPControl(Context context, float screenRatio) {
		super(context);
		
		int color = Color.TRANSPARENT;
        Drawable background = this.getBackground();
        if (background instanceof ColorDrawable)
            color = ((ColorDrawable) background).getColor();
		
		this.setBackgroundColor(Color.TRANSPARENT);//without this, no widgets are viewable. no idea why!
		this.screenRatio = screenRatio;
		color = Color.BLUE;
		paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
	}
	
	 public void setColor(int color){
	    this.color = color;
	    paint.setColor(color);
	 }
	 public void setHighlightColor(int color) {
		 this.highlightColor = color;
	 }
	
	public void receiveList(List<Object> messageArray){
		if (messageArray.size()>=2 && 
				(messageArray.get(0) instanceof String) && 
				messageArray.get(0).equals("enable") && 
				(messageArray.get(1) instanceof Float)) {
			this.setEnabled(((Float)(messageArray.get(1))).floatValue() > 0);
			this.setAlpha(this.isEnabled() ? 1.0f : .2f );
		}
	}
}
