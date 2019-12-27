package com.iglesiaintermedia.mobmuplat.controls;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.widget.TextView;

public class MMPLabel extends MMPControl {

    /*private enum MMPHorizontalTextAlignment {
        LEFT, CENTER, RIGHT
    }

    private enum MMPVerticalTextAlignment {
        TOP, CENTER, BOTTOM
    }*/

	static final String DEFAULT_FONT = "HelveticaNeue";
	static final int PAD = 5;
	TextView _textView;
	public int _textSize;
	public String _stringValue;
	public String _fontName;
	public String _fontFamily;
	
	//for fonts from assets
	Context _context;
    private Alignment _alignment;
    private int _verticalAlignment;

	public MMPLabel(Context context, float screenRatio) {
		super(context, screenRatio);
		_context = context;
		this.address="/myLabel";//TODO or "unnamed?"
		_fontFamily="Default";
		_fontName = "";
		setTextSize(16);
		_stringValue = "my text goes here";
        _alignment = Alignment.ALIGN_NORMAL;
	}
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
         StaticLayout staticLayout =
                 new StaticLayout(_stringValue,
                        this.paint,
                         Math.max(0,getWidth()-(int)(10*screenRatio)),//vs right-left?
                _alignment, 1, 1, true);

        int canvasHeight = getHeight();
        int textHeight = staticLayout.getHeight();
        int top = 0;
        int left = (int)(5 * this.screenRatio);

        switch (_verticalAlignment) {
            case 0:
                break;
            case 1:
                top = Math.max((canvasHeight - textHeight ) / 2, 0);
                break;
            case 2:
                top = Math.max(canvasHeight - textHeight , 0);
                break;
        }
        canvas.translate(left, top );
	     staticLayout.draw(canvas);
	}
	
	public void setStringValue(String stringValue) {
		_stringValue = stringValue;
	}
	
	public void setTextSize(int size){
		_textSize = size;
		this.paint.setTextSize(_textSize*this.screenRatio);
	}
	
	public void setFontFamilyAndName(String family, String name) {//family is always null for now
		Typeface typeface = null;
		if (name.equals("Roboto-Regular")) {
			typeface = Typeface.create("sans-serif", Typeface.NORMAL);
		} else if (name.equals("Roboto-Italic")) {
			typeface = Typeface.create("sans-serif", Typeface.ITALIC);
		} else if (name.equals("Roboto-Bold")) {
			typeface = Typeface.create("sans-serif", Typeface.BOLD);
		} else if (name.equals("Roboto-BoldItalic")) {
			typeface = Typeface.create("sans-serif", Typeface.BOLD_ITALIC);
		} else if (name.equals("Roboto-Light")) {
			typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
		} else if (name.equals("Roboto-LightItalic")) {
			typeface = Typeface.create("sans-serif-light", Typeface.ITALIC);
		} else if (name.equals("Roboto-Thin")) {
			typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL); //4.2
		} else if (name.equals("Roboto-ThinItalic")) {
			typeface = Typeface.create("sans-serif-thin", Typeface.ITALIC); //4.2
		} else if (name.equals("RobotoCondensed-Regular")) {
			typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
		} else if (name.equals("RobotoCondensed-Italic")) {
			typeface = Typeface.create("sans-serif-condensed", Typeface.ITALIC);
		} else if (name.equals("RobotoCondensed-Bold")) {
			typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD);
		} else if (name.equals("RobotoCondensed-BoldItalic")) {
			typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);
		}
		
		if (typeface!=null) {
			this.paint.setTypeface(typeface);
		}
		/*Regular
		Italic
		Bold
		Bold-italic
		Light
		Light-italic
		Condensed regular
		Condensed italic
		Condensed bold
		Condensed bold-italic*/
		//Translate to
		/* "sans-serif" for regular Roboto
		"sans-serif-light" for Roboto Light
		"sans-serif-condensed"
		then normal/bold/italic
		*/
	}

	public void setHorizontalTextAlignment(int h) {
        switch (h) {
            case 0:
                _alignment = Alignment.ALIGN_NORMAL;
                break;
            case 1:
                _alignment = Alignment.ALIGN_CENTER;
                break;
            case 2:
                _alignment = Alignment.ALIGN_OPPOSITE;
        }
    }

    public void setVerticalTextAlignment(int v) {
        _verticalAlignment = v;
    }

	public void receiveList(List<Object> messageArray){ 
		super.receiveList(messageArray);
		//ignore enable message
		if (messageArray.size()>=2 && 
				(messageArray.get(0) instanceof String) && 
				messageArray.get(0).equals("enable") && 
				(messageArray.get(1) instanceof Float)) {
			return;
		}
	    //set new value
	    //System.out.print("\nms size "+messageArray.size()+" "+messageArray.get(0));
	    if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && (messageArray.get(1) instanceof Float) && messageArray.get(0).equals("highlight")) {
	        	float val = ((Float)messageArray.get(1)).floatValue();
	        	if (val > 0) this.paint.setColor(this.highlightColor);
	        	else this.paint.setColor(this.color);
	        	invalidate();
	    }
	    
	    else {
	    	//concatenate
	    	StringBuffer buf = new StringBuffer();
	    	for (Object item : messageArray){
	    		if (item instanceof Float) {//formatting 
	    			float val = ((Float)item).floatValue();
	    			if (val % 1.0 == 0) { //whole number, show as no decimal
	    				buf.append(String.format("%d", (int)val));
	    			} else {
	    				buf.append(String.format("%.3f", val));
	    			}
	    		}
	    		else if (item instanceof String || item instanceof Integer) {
                    buf.append(item);

	    		}
                buf.append(" ");
	    	}
	    	setStringValue(buf.toString());
	    	invalidate();
	    }
	}


	

}
