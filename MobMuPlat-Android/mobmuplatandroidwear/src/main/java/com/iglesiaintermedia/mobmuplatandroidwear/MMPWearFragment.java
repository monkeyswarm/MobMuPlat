package com.iglesiaintermedia.mobmuplatandroidwear;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MMPWearFragment extends Fragment{
	//String mTemplateString;
	
	static final int MARGIN = 10;
	static final int SWIPEZONE_HEIGHT = 60;

	private TextView mTitleTextView;
	private JsonObject mPageDict;//hold until load view

	public MMPControl control; //instantiated after setup view

	public void setPageDict(JsonObject pageDict) {
        mPageDict = pageDict;
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {//onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		final View view = inflater.inflate(R.layout.fragment_buttoncard, null);
		mTitleTextView = (TextView)view.findViewById(R.id.textView1);
		/*Button button = (Button) view.findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click

            }
        });*/
		final FrameLayout fl = (FrameLayout)view;
		
		ViewTreeObserver observer = fl.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				fl.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				MMPWearFragment.this.setupGUI(view);
				  
			}
		});
		return view;
	}
	
	private void setupGUI(View view) {
		if (mPageDict == null) return;;
		// title
		if(mPageDict.get("title")!=null) {
			mTitleTextView.setText(mPageDict.get("title").getAsString());
		}
		
		if(mPageDict.get("pageGui")==null) return;
		// gui widget dict
		JsonObject guiDict = mPageDict.get("pageGui").getAsJsonObject();
		if(guiDict.get("class")==null) return;
		String classString = guiDict.get("class").getAsString();
		
		//color
		int color = Color.BLUE;
		if(guiDict.getAsJsonArray("color")!=null){
			JsonArray colorArray = guiDict.getAsJsonArray("color");
			//if(colorArray.size()==4)
				color = colorFromRGBAArray(colorArray);
		}

		//highlight color
		int highlightColor = Color.RED;
		if(guiDict.getAsJsonArray("highlightColor")!=null){
			JsonArray highlightColorArray = guiDict.getAsJsonArray("highlightColor");
			//if(highlightColorArray.size()==4)
				highlightColor= colorFromRGBAArray(highlightColorArray);
		}
		// address
		String address = "/unknownAddress";
		if(guiDict.get("address")!=null){
			address= guiDict.get("address").getAsString();
		}
		
		float screenRatio = 1.0f;
		// copied from MMP!
		//check by MMPControl subclass, and alloc/init object
		if(classString.equals("MMPLabel")){
			control = new MMPLabel(getActivity(), screenRatio);

			if(guiDict.get("text")!=null) 
				((MMPLabel)control).setStringValue( guiDict.get("text").getAsString() );
			if(guiDict.get("textSize")!=null)
				((MMPLabel)control).setTextSize( guiDict.get("textSize").getAsInt()  );
			if(guiDict.get("androidFont")!=null /*&& guiDict.get("androidFontFamily")!=null*/) {//family = always roboto for now
				// family can be null for now
				String familyName = guiDict.get("androidFontFamily") == null ? null : guiDict.get("androidFontFamily").getAsString();
				((MMPLabel)control).setFontFamilyAndName( familyName, guiDict.get("androidFont").getAsString() );
			}
		}
		else if(classString.equals("MMPXYSlider")){
			control = new MMPXYSlider(getActivity(), screenRatio);
		}
		else if(classString.equals("MMPGrid")){
			control = new MMPGrid(getActivity(), screenRatio);
			if(guiDict.get("dim")!=null){
				JsonArray dim = guiDict.get("dim").getAsJsonArray();
				((MMPGrid)control).setDimXY(dim.get(0).getAsInt(), dim.get(1).getAsInt());
			}
			if(guiDict.get("mode")!=null)
				((MMPGrid)control).mode =  guiDict.get("mode").getAsInt()  ;
			if(guiDict.get("borderThickness")!=null)
				((MMPGrid)control).borderThickness =  guiDict.get("borderThickness").getAsInt()  ;
			if(guiDict.get("cellPadding")!=null)
				((MMPGrid)control).cellPadding =  guiDict.get("cellPadding").getAsInt() ;
		}
		else if(classString.equals("MMPMultiSlider")) {
			control = new MMPMultiSlider(getActivity(), screenRatio);
			if (guiDict.get("range") != null)
				((MMPMultiSlider) control).setRange(guiDict.get("range").getAsInt());
		} else if (classString.equals("MMPMenu")) {
			control = new MMPMenu(getActivity(), screenRatio);

		} else {
			control = new MMPUnknown(getActivity(), screenRatio);
			((MMPUnknown)control).badNameString = classString;
		}
		
		
	/*	if (mTemplateString == "MultiSlider1") {
			MMPMultiSlider slider = new MMPMultiSlider(this.getActivity(), 1.0f);
			control = slider;
			slider.address = "/mySlider";
		} else if (mTemplateString == "MultiSlider2") {
			MMPMultiSlider slider = new MMPMultiSlider(this.getActivity(), 1.0f);
			slider.setRange(2);
			control = slider;
			slider.address = "/mySlider";
		} else {
			return;
		}*/
		
		control.setColor(color);
		control.setHighlightColor(highlightColor);
		control.address = address;
		control.controlDelegate = (MainActivity)getActivity();
		//control.setColor(control.controlDelegate.getPatchForegroundColor());
		//control.setHighlightColor(control.controlDelegate.getPatchHighlightColor());
		mTitleTextView.setTextColor(color);
		
		FrameLayout fl = (FrameLayout)view;
		fl.addView(control);
		
		Rect newFrame = new Rect(MARGIN,MARGIN+SWIPEZONE_HEIGHT,fl.getWidth()-MARGIN,fl.getHeight()-MARGIN);
		control.setLayoutParams(new FrameLayout.LayoutParams(newFrame.width(), newFrame.height()));
		control.setX(newFrame.left);
		control.setY(newFrame.top);
	}
	
	//create a color with translucency
	static int colorFromRGBAArray(JsonArray rgbaArray){
		return Color.argb((int)(rgbaArray.get(3).getAsFloat()*255), (int)(rgbaArray.get(0).getAsFloat()*255), (int)(rgbaArray.get(1).getAsFloat()*255), (int)(rgbaArray.get(2).getAsFloat()*255)  );
	}
}
