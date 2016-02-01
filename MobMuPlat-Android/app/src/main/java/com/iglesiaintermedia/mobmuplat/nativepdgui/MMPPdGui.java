package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class MMPPdGui {
	public final ArrayList<Widget> widgets;
	public int parentViewWidth; //current container view size
	public int parentViewHeight;
	public int fontSize; // font size loaded from patch
	public float scaleX; // scale amount between view bounds and original patch size, calculated when bounds is set
	public float scaleY;
	
	// pixel size of original pd patch
	private int patchWidth;
	private int patchHeight;

	public MMPPdGui() {
		super();
		widgets = new ArrayList<Widget>();
	}
	
	public void buildUI(Context context, List<String[]> atomlines, float scale) {
		//ArrayList<String> canvases = new ArrayList<String>();
		int level = 0;
		
		for (String[] line: atomlines) {
			if (line.length >= 4) {
				// find canvas begin and end lines
				if (line[1].equals("canvas")) {
					/*if (canvases.length == 0) {
						canvases.add(0, "self");
					} else {
						canvases.add(0, line[6]);
					}*/
					level += 1;
					if (level == 1) {
						patchWidth = Integer.parseInt(line[4]);
						patchHeight = Integer.parseInt(line[5]);
						fontSize = Integer.parseInt(line[6]);
					}
				} else if (line[1].equals("restore")) {
					//canvases.remove(0);
					level -= 1;
				// find different types of UI element in the top level patch
				} else if (level == 1) {
					if (line.length >= 2) {
						// builtin pd things
						if (line[1].equals("text")) {
							widgets.add(new Comment(context, line, scale));
						} else if (line[1].equals("floatatom")) {
							widgets.add(new NumberBox(context, line, scale, fontSize));
						} else if (line.length >= 5) {
							// pd objects
							if (line[4].equals("vsl")) {
								widgets.add(new Slider(context, line, scale, false));
							} else if (line[4].equals("hsl")) {
								widgets.add(new Slider(context, line, scale, true));
							} else if (line[4].equals("vradio")) {
								widgets.add(new Radio(context, line, scale, false));
							} else if (line[4].equals("hradio")) {
								widgets.add(new Radio(context, line, scale, true));
							} else if (line[4].equals("tgl")) {
								widgets.add(new Toggle(context, line, scale));
							} else if (line[4].equals("bng")) {
								widgets.add(new Bang(context, line, scale));
							} else if (line[4].equals("nbx")) {
								widgets.add(new NumberBox2(context, line, scale, fontSize));
							} else if (line[4].equals("cnv")) {
								widgets.add(new CanvasRect(context, line, scale));
							} 
				
						}
					}
				}
				
				
			}
		}
		//threadSafeInvalidate();
	}
}
