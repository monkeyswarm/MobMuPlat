package com.iglesiaintermedia.mobmuplat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;
import android.support.v4.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.iglesiaintermedia.mobmuplat.controls.ControlDelegate;
import com.iglesiaintermedia.mobmuplat.controls.MMPButton;
import com.iglesiaintermedia.mobmuplat.controls.MMPControl;
import com.iglesiaintermedia.mobmuplat.controls.MMPGrid;
import com.iglesiaintermedia.mobmuplat.controls.MMPKnob;
import com.iglesiaintermedia.mobmuplat.controls.MMPLCD;
import com.iglesiaintermedia.mobmuplat.controls.MMPLabel;
import com.iglesiaintermedia.mobmuplat.controls.MMPMenu;
import com.iglesiaintermedia.mobmuplat.controls.MMPMultiSlider;
import com.iglesiaintermedia.mobmuplat.controls.MMPMultiTouch;
import com.iglesiaintermedia.mobmuplat.controls.MMPPanel;
import com.iglesiaintermedia.mobmuplat.controls.MMPSlider;
import com.iglesiaintermedia.mobmuplat.controls.MMPTable;
import com.iglesiaintermedia.mobmuplat.controls.MMPToggle;
import com.iglesiaintermedia.mobmuplat.controls.MMPUnknown;
import com.iglesiaintermedia.mobmuplat.controls.MMPXYSlider;

public class PatchFragment extends Fragment implements ControlDelegate, PagingScrollViewDelegate{
	private String TAG = "PatchFragment";

	public PagingHorizontalScrollView scrollContainer;
	public RelativeLayout scrollRelativeLayout;

	private float screenRatio = 1;

	public enum CanvasType{ canvasTypeWidePhone ,canvasTypeTallPhone , canvasTypeWideTablet, canvasTypeTallTablet }

	CanvasType _canvasType;
	boolean _isOrientationLandscape;
	boolean _isPageScrollShortEnd;

	int _pageCount;
	int _startPageIndex;
	int _port;
	float _version;

	public MainActivity _mainActivity;

	Map<String, ArrayList<MMPControl>> _allGUIControlMap; //control address, array of objects with that address. Allows multiple items with same address.
	Set<String> _wearAddressSet; 
	
	int _bgColor;

	public ImageButton _settingsButton; //TODO make private again...is set in mainactivity loadScenePatchOnly
	private View _container;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_mainActivity = (MainActivity)getActivity();
		_allGUIControlMap = new HashMap<String,ArrayList<MMPControl>>();
		_wearAddressSet = new HashSet<String>();
		//init pd
		PdBase.setReceiver(receiver);
		PdBase.subscribe("toGUI");//from pd
		PdBase.subscribe("toNetwork");
		PdBase.subscribe("toSystem");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		_container = container;
		final View rootView = inflater.inflate(R.layout.fragment_patch, container,false);

		scrollContainer = (PagingHorizontalScrollView) rootView.findViewById(R.id.horizontalScrollView);
		scrollContainer.pagingDelegate = this;
		scrollContainer.setBackgroundColor(Color.BLACK);

		scrollRelativeLayout = (RelativeLayout)rootView.findViewById(R.id.relativeLayout);
		//scrollRelativeLayout.setBackgroundColor(Color.GREEN);
		_settingsButton = (ImageButton)rootView.findViewById(R.id.button1);
		_settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				_mainActivity.launchSettings();
			}			
		});
		// attach hardware keyboard
		/*scrollRelativeLayout.setOnKeyListener(new View.OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						Object[] args = new Object[]{"/fromSystem", Integer.valueOf(keyCode)};
						PdBase.sendList("fromSystem", args);
						return true; //consume
					}
		});*/

		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				//String fileJSON = MainActivity.readMMPToString("Welcome-ip5.mmp");
				String fileJSON = "";
				try {
					if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeWidePhone) {
						fileJSON = MainActivity.readMMPAssetToString(getActivity().getAssets().open("Welcome.mmp"));
					} else if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeTallPhone) {
						fileJSON = MainActivity.readMMPAssetToString(getActivity().getAssets().open("Welcome-ip5.mmp"));
					} else if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeWideTablet) {
						fileJSON = MainActivity.readMMPAssetToString(getActivity().getAssets().open("Welcome-Pad.mmp"));
					} else if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeTallTablet) {
						fileJSON = MainActivity.readMMPAssetToString(getActivity().getAssets().open("Welcome-and7.mmp"));
					} 
				} catch (IOException e) {

				}

				loadSceneFromJSON(fileJSON);
			}
		});

		//_rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	//keep this for opening old versions of mobmuplat files that use three elements
	static int colorFromRGBArray(JsonArray rgbArray){
		return Color.rgb((int)(rgbArray.get(0).getAsFloat()*255), (int)(rgbArray.get(1).getAsFloat()*255), (int)(rgbArray.get(2).getAsFloat()*255));
	}

	//get array of NSNumber floats from a color
	static ArrayList<Float> RGBAArrayFromColor(int color){
		//System.out.print("\narray from color with red "+color.getRed());
		ArrayList<Float> retval = new ArrayList<Float>();
		retval.add(Float.valueOf(Color.red(color)/255f));
		retval.add(Float.valueOf(Color.green(color)/255f));
		retval.add(Float.valueOf(Color.blue(color)/255f));
		retval.add(Float.valueOf(Color.alpha(color)/255f));
		return retval;

	}
	//create a color with translucency
	static int colorFromRGBAArray(JsonArray rgbaArray){
		return Color.argb((int)(rgbaArray.get(3).getAsFloat()*255), (int)(rgbaArray.get(0).getAsFloat()*255), (int)(rgbaArray.get(1).getAsFloat()*255), (int)(rgbaArray.get(2).getAsFloat()*255)  );
	}
	
	public void loadScenePatchOnly(String filenameToLoad) {
		//don't bother with rotation for now...
		scrollRelativeLayout.removeAllViews();

		//reset scroll to one page
		scrollContainer.setLayoutParams(new FrameLayout.LayoutParams(_container.getWidth(),_container.getHeight()));
		scrollRelativeLayout.setLayoutParams(new FrameLayout.LayoutParams(_container.getWidth(),_container.getHeight()));
		scrollRelativeLayout.setBackgroundColor(Color.GRAY);
		TextView tv = new TextView(_mainActivity);
		tv.setText("running "+filenameToLoad+"\nwith no interface\n\n(any network data will be\n on default port "+NetworkController.DEFAULT_PORT_NUMBER+")");
		tv.setTextColor(Color.WHITE);
		tv.setGravity(Gravity.CENTER);

		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		rlp.addRule(RelativeLayout.CENTER_VERTICAL); 
		rlp.addRule(RelativeLayout.CENTER_HORIZONTAL); 
		tv.setLayoutParams(rlp);
		scrollRelativeLayout.addView(tv);
		//TODO RESET ports!

		//TODO move to patch so we can add 
		scrollRelativeLayout.addView(_settingsButton);
		_settingsButton.setVisibility(View.VISIBLE);//not really necc, since initial welcome load should set it visible...
	}

	public void loadSceneFromJSON(String inString){
		_allGUIControlMap.clear();
		_wearAddressSet.clear();
		if (inString.isEmpty()) return;
				
		try {
			JsonParser parser = new JsonParser();
			JsonObject topDict = parser.parse(inString).getAsJsonObject();//top dict - exception on bad JSON

			_mainActivity.stopLocationUpdates();
			scrollRelativeLayout.removeAllViews();
			//unlike ios, can just set orientation without physical rotation
			//
			//
			int screenWidth = _container.getWidth();//_rootView.getWidth();
			int screenHeight = _container.getHeight();//_rootView.getHeight();
			if(MainActivity.VERBOSE)Log.i(TAG, "load scene _container dim "+screenWidth+" "+screenHeight);
			if(screenWidth==0 || screenHeight == 0) return;//error

			// get view-layout-needed parameters
			if(topDict.get("canvasType")!=null){
				// check deprecated strings!
				String canvasTypeString = topDict.get("canvasType").getAsString();
				if(canvasTypeString.equals("iPhone3p5Inch") ||
				   canvasTypeString.equals("widePhone") ) { 
					_canvasType=CanvasType.canvasTypeWidePhone;
				}
				else if(canvasTypeString.equals("iPhone4Inch") ||
					canvasTypeString.equals("tallPhone")) {
					_canvasType=CanvasType.canvasTypeTallPhone;
				}
				else if(canvasTypeString.equals("iPad") ||
						canvasTypeString.equals("wideTablet")) {
					_canvasType=CanvasType.canvasTypeWideTablet;
				}
				else if(canvasTypeString.equals("android7Inch") ||
						canvasTypeString.equals("tallTablet")) {
					_canvasType=CanvasType.canvasTypeTallTablet;
				}
			}
			if(topDict.get("isOrientationLandscape")!=null)
				_isOrientationLandscape= topDict.get("isOrientationLandscape").getAsBoolean();
			if(topDict.get("isPageScrollShortEnd")!=null)
				_isPageScrollShortEnd=topDict.get("isPageScrollShortEnd").getAsBoolean();
			if(topDict.get("pageCount")!=null)
				_pageCount=topDict.get("pageCount").getAsInt();//TODO error check and try to deduce?
			if(topDict.get("startPageIndex")!=null)
				_startPageIndex=topDict.get("startPageIndex").getAsInt();

			int referenceWidth=320, referenceHeight=480;
			if(!_isOrientationLandscape) {
				switch (_canvasType) {
				case canvasTypeWidePhone:
					referenceWidth = 320;
					referenceHeight = 480;
					break;
				case canvasTypeTallPhone:
					referenceWidth = 320;
					referenceHeight = 568;
					break;
				case canvasTypeWideTablet:
					referenceWidth = 768;
					referenceHeight = 1024;
					break;
				case canvasTypeTallTablet://half of 1200 1824 aspect 1.52 NOW half of 1200 1920
					referenceWidth = 600;
					referenceHeight = 960;
				}
			} else { //landscape
				switch (_canvasType) {
				case canvasTypeWidePhone:
					referenceWidth = 480;
					referenceHeight = 320;
					break;
				case canvasTypeTallPhone:
					referenceWidth = 568;
					referenceHeight = 320;
					break;
				case canvasTypeWideTablet:
					referenceWidth = 1024;
					referenceHeight = 768;
					break;
				case canvasTypeTallTablet://half of 1920 1104 NOW half of 1920 x 1200
					referenceWidth = 960;
					referenceHeight = 600;
				}	
			}
			double xRatio = (double)screenWidth/referenceWidth;
			double yRatio = (double)screenHeight/referenceHeight;
			screenRatio = (float)Math.min(xRatio, yRatio);
			if(MainActivity.VERBOSE)Log.i(TAG, "ref size "+referenceWidth+" "+referenceHeight+" pgc "+_pageCount);

			FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams((int)(referenceWidth*screenRatio),(int)(referenceHeight*screenRatio));
			flp.gravity = Gravity.CENTER;
			scrollContainer.setLayoutParams(flp);
			//???_scrollContainer.pageScroll(2);// = _pageCount;
			scrollContainer.pageCount = _pageCount;

			if (_startPageIndex<_pageCount) {
				scrollContainer.setScrollX((int)(referenceWidth*screenRatio*_startPageIndex));
			} else scrollContainer.setScrollX((int)(referenceWidth*screenRatio*(_pageCount-1)));//last page

			int layoutWidth = (int)(referenceWidth*screenRatio*_pageCount);
			int layoutHeight = (int)(referenceHeight*screenRatio);
			if(MainActivity.VERBOSE)Log.i(TAG, "set scrollrelativelayout dim "+layoutWidth+" "+layoutHeight);
			scrollRelativeLayout.setLayoutParams(new FrameLayout.LayoutParams(layoutWidth,layoutHeight));

			//
			if(topDict.getAsJsonArray("backgroundColor")!=null){
				JsonArray colorArray = topDict.getAsJsonArray("backgroundColor");
				if(colorArray.size()==4)
					_bgColor = colorFromRGBAArray(colorArray);
				else if (colorArray.size()==3)
					_bgColor = colorFromRGBArray(colorArray);
				scrollRelativeLayout.setBackgroundColor(_bgColor);
			}

			final String pdFilename;
			if(topDict.get("pdFile")!=null) {
				pdFilename = topDict.get("pdFile").getAsString();// objectForKey:@"pdFile"]];
			}
			else {
				pdFilename = null;
				showAlert("This interface has not been linked to a PureData file. Add it in the editor!");

			}

			if(topDict.get("port")!=null)
				_port=topDict.get("port").getAsInt();
			//TODO SET PORT IN NETWORK CONTROLLER
			if(topDict.get("version")!=null)
				_version=topDict.get("version").getAsFloat();

			JsonArray controlDictArray;//array of dictionaries, one for each gui element

			// WEAR GUI
			if (topDict.get("wearGui")!=null) {
				JsonArray pageGuiArray = topDict.get("wearGui").getAsJsonArray();
				// construct new dict with background color, and wear gui array
				JsonObject wearDict = new JsonObject();
				if(topDict.getAsJsonArray("backgroundColor")!=null){
					JsonArray colorArray = topDict.getAsJsonArray("backgroundColor");
					wearDict.add("backgroundColor", colorArray);
				}
				wearDict.add("wearGui", pageGuiArray);
				String jsonString = wearDict.toString();
				_mainActivity.sendWearMessage("/loadGUI", jsonString.getBytes(Charset.forName("UTF-8")));
				
				// iterate through wear pages and get addresses
				for(int i=0;i<pageGuiArray.size();i++){ 
					JsonObject pageDict = pageGuiArray.get(i).getAsJsonObject();//page-level dict
					if (pageDict.get("pageGui")==null)continue;
					JsonObject pageGuiDict = pageDict.get("pageGui").getAsJsonObject();
					if(pageGuiDict.get("address")==null)continue;// if doesn't have an address, skip out of loop
					String address = pageGuiDict.get("address").getAsString();
					_wearAddressSet.add(address);
				}
			}
			
			// MAIN GUI
			if(topDict.get("gui")!=null){
				controlDictArray = topDict.get("gui").getAsJsonArray();//[topDict objectForKey:@"gui"];//array of dictionaries, one for each gui control
				//for(JsonObject guiDict : controlDictArray){//for each one
				for(int i=0;i<controlDictArray.size();i++){    
					JsonObject guiDict = controlDictArray.get(i).getAsJsonObject();//???
					MMPControl control;
					if(guiDict.get("class")==null)continue;// if doesn't have a class, skip out of loop

					String classString = guiDict.get("class").getAsString();// objectForKey:@"class"];
					//frame
					//default
					Rect newFrame = new Rect(0, 0, (int)(100*xRatio), (int)(100*yRatio));
					if(guiDict.get("frame")!=null){
						JsonArray frameRectArray = guiDict.getAsJsonArray("frame");
						//convert Left Top width height to Left Top right bottom
						int left = (int)(frameRectArray.get(0).getAsFloat() * screenRatio);
						int top = (int)(frameRectArray.get(1).getAsFloat() * screenRatio);
						int width = (int)(frameRectArray.get(2).getAsFloat() * screenRatio);
						int height = (int)(frameRectArray.get(3).getAsFloat() * screenRatio);
						newFrame = new Rect(left, top, left+width, top+height);
						//Log.i(TAG, "newFrame "+left+" "+top+" "+width+" "+height+" ");
					}
					//color
					int color = Color.BLUE;
					if(guiDict.getAsJsonArray("color")!=null){
						JsonArray colorArray = guiDict.getAsJsonArray("color");
						if(colorArray.size()==4)
							color = colorFromRGBAArray(colorArray);
						else if (colorArray.size()==3)
							color = colorFromRGBArray(colorArray);
						//Log.i(TAG, "color "+color);
					}

					//highlight color
					int highlightColor = Color.RED;
					if(guiDict.getAsJsonArray("highlightColor")!=null){
						JsonArray highlightColorArray = guiDict.getAsJsonArray("highlightColor");
						if(highlightColorArray.size()==4)
							highlightColor= colorFromRGBAArray(highlightColorArray);
						else if (highlightColorArray.size()==3)
							highlightColor= colorFromRGBArray(highlightColorArray);
					}

					String address = "/unknownAddress";
					if(guiDict.get("address")!=null){
						address= guiDict.get("address").getAsString();
					}


					//check by MMPControl subclass, and alloc/init object
					if(classString.equals("MMPSlider")){
						control = new MMPSlider(getActivity(), screenRatio);
						// TODO: can theis be general at the end of this.

						if(guiDict.get("isHorizontal")!=null) 
							((MMPSlider)control).setIsHorizontal( guiDict.get("isHorizontal").getAsBoolean() );
						if(guiDict.get("range")!=null)
							((MMPSlider)control).setRange( guiDict.get("range").getAsInt()  );
					}
					else if(classString.equals("MMPKnob")){
						control = new MMPKnob(getActivity(), screenRatio);
						int indicatorColor = Color.WHITE;
						if(guiDict.get("indicatorColor")!=null){
							indicatorColor = colorFromRGBAArray(guiDict.get("indicatorColor").getAsJsonArray());
						}
						((MMPKnob)control).indicatorColor = indicatorColor;

						if(guiDict.get("range")!=null)
							((MMPKnob)control).setRange( guiDict.get("range").getAsInt()  );
					}
					else if(classString.equals("MMPButton")){
						control = new MMPButton(getActivity(), screenRatio);
					}
					else if(classString.equals("MMPToggle")){
						control = new MMPToggle(getActivity(), screenRatio);
						if(guiDict.get("borderThickness")!=null)
							((MMPToggle)control).borderThickness =  guiDict.get("borderThickness").getAsInt() ;
					}
					else if(classString.equals("MMPLabel")){
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
					else if(classString.equals("MMPPanel")){
						control = new MMPPanel(getActivity(), screenRatio);
						if(guiDict.get("imagePath")!=null) {
							//convert relative image path to full external storage path TODO merge with panel recevelist code. static method?
							String path = guiDict.get("imagePath").getAsString();
							File extFile = new File(MainActivity.getDocumentsFolderPath(), path);
							((MMPPanel)control).setImagePath(extFile.getAbsolutePath());
						}
						if(guiDict.get("passTouches")!=null)
							((MMPPanel)control).setShouldPassTouches( guiDict.get("passTouches").getAsBoolean() );

					}

					else if(classString.equals("MMPMultiSlider")){
						control = new MMPMultiSlider(getActivity(), screenRatio);
						if(guiDict.get("range")!=null)
							((MMPMultiSlider)control).setRange( guiDict.get("range").getAsInt()  );
					}
					else if(classString.equals("MMPLCD")){
						control = new MMPLCD(getActivity(), screenRatio);
					}
					else if (classString.equals("MMPMultiTouch")) {
						control = new MMPMultiTouch(getActivity(), screenRatio);
					}
					else if (classString.equals("MMPMenu")) {
						control = new MMPMenu(getActivity(), screenRatio);
						if(guiDict.get("title")!=null)
							((MMPMenu)control).titleString = guiDict.get("title").getAsString();
					}
					else if (classString.equals("MMPTable")) {
						control = new MMPTable(getActivity(), screenRatio);
						if(guiDict.get("mode")!=null)
							((MMPTable)control).mode = guiDict.get("mode").getAsInt();
						if(guiDict.get("selectionColor")!=null) {
							int selectionColor = colorFromRGBAArray(guiDict.get("selectionColor").getAsJsonArray());
							((MMPTable)control).selectionColor = selectionColor;
						}
						if(guiDict.get("displayMode")!=null) {
							((MMPTable)control).displayMode = guiDict.get("displayMode").getAsInt();
						}
						if(guiDict.get("displayRangeLo")!=null) {
							((MMPTable)control).displayRangeLo = guiDict.get("displayRangeLo").getAsFloat();
						}
						if(guiDict.get("displayRangeHi")!=null) {
							((MMPTable)control).displayRangeHi = guiDict.get("displayRangeHi").getAsFloat();
						}
					}
					//no class
					else { 
						control = new MMPUnknown(getActivity(), screenRatio);
						((MMPUnknown)control).badNameString = classString;
					}

					//common
					control.setLayoutParams(new RelativeLayout.LayoutParams(newFrame.width(), newFrame.height()));
					control.setX(newFrame.left);
					control.setY(newFrame.top);
					control.controlDelegate = this;
					control.setColor(color);
					control.setHighlightColor(highlightColor);
					control.address = address;
					scrollRelativeLayout.addView(control);

					ArrayList<MMPControl> addressArray = _allGUIControlMap.get(control.address);
					if (addressArray == null) {
						addressArray = new ArrayList<MMPControl>();
						_allGUIControlMap.put(control.address, addressArray);
					}
					addressArray.add(control);

				}
			}

			//settings button
			scrollRelativeLayout.addView(_settingsButton);
			_settingsButton.setVisibility(View.VISIBLE);

			if(MainActivity.VERBOSE)Log.i(TAG, "end of layout loop...");

			//end of big loop through widgets

			// listen for completion of layout before loading patch
			//LinearLayout layout = (LinearLayout)findViewById(R.id.YOUR_VIEW_ID);
			ViewTreeObserver vto = scrollRelativeLayout.getViewTreeObserver(); 
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
				@Override 
				public void onGlobalLayout() { 
					if(MainActivity.VERBOSE)Log.i(TAG, "layout complete...");
					scrollRelativeLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this); 
					// scroll to start
					//_scrollContainer.setScrollX(0);

					_mainActivity.loadPdFile(pdFilename); 

					//load tables! TODO optimize!
					for (ArrayList<MMPControl> addressArray : _allGUIControlMap.values()) {
						for (MMPControl control : addressArray)  {
							if (control instanceof MMPTable) {
								((MMPTable)control).loadTable();
							}
						}
					}
				} 
			});
		}catch(JsonParseException e) {
			showAlert("Unable to parse interface file.");
		}

	}

	// ControlDelegate interface
	@Override
	public void sendGUIMessageArray(List<Object> message) {
		if(MainActivity.VERBOSE) {
			String listString = "";
			for (Object obj : message) listString += obj.toString()+" ";
			Log.i(TAG, "send list to pd fromGUI: " + listString);
		}
		PdBase.sendList("fromGUI", message.toArray());
	}

	// Receive from pd
	private PdReceiver receiver = new PdReceiver() {

		@Override
		public void print(String s) {
			ConsoleLogController.getInstance().append(s);
		}

		@Override
		public void receiveBang(String source) {
			//Log.i(TAG, "bang");
		}

		@Override
		public void receiveFloat(String source, float x) {
			//Log.i(TAG, "float: " + x);
		}

		@Override
		public void receiveList(String source, Object... args) {
			if(MainActivity.VERBOSE)Log.i(TAG, "receive list from pd "+source+": " + Arrays.toString(args));
			if (source.equals("toNetwork")) {
				_mainActivity.networkController.handlePDMessage(args);
			} else if (source.equals("toGUI")) {
				if (args.length==0) return;
				Object addressObj = args[0];
				ArrayList<MMPControl> addressArray = _allGUIControlMap.get(addressObj);
				if(addressArray!=null) {//null if there is no objects by that address!
					for (MMPControl control : addressArray) {
						List<Object> newList = Arrays.asList(Arrays.copyOfRange(args, 1,args.length));
						control.receiveList(newList); 
					}
				}
				// If wear has the address, send it out.
				if (_wearAddressSet.contains(addressObj)) {
					Object argsNoAddress[] = Arrays.copyOfRange(args, 1, args.length);
					String message = TextUtils.join(" ",argsNoAddress); //rest is turned into list, delimited by space.
					byte[] data = message.getBytes(Charset.forName("UTF-8"));
					_mainActivity.sendWearMessage((String)addressObj, data);
				}
			} else if (source.equals("toSystem")) {
				if (args.length==0) return;

				if (args.length>=1 && args[0].equals("/vibrate") ){
					//orig spec could send just "vibrate" or with arg 0 or 1
					int duration = 0; 
					if (args.length>=2 && args[1] instanceof Float) {
						duration = (int)(((Float)args[1]).floatValue());
					}
					if (duration >= 0 && duration < 3) duration = 500; //orig ios compatibility 
					if (duration > 10000) duration = 10000; //max of 10 seconds
					// Vibrate for x milliseconds
					Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
					if (v!=null)v.vibrate(duration);
				}
				if (args.length>=2 && args[0].equals("/flash") && args[1] instanceof Float) { //move stuff to parent...
					float val =  ((Float)args[1]).floatValue();
					if (val > 0) _mainActivity.flashlightController.turnLightOn();
					else _mainActivity.flashlightController.turnLightOff();

				} /*else if (args.length==2 && args[0].equals("/setSensorFrequency") && args[1] instanceof Float) {
					float val = ((Float)args[1]).floatValue();
					if(val<0.01)val=0.01f;//clip
		            if(val>100)val=100;
		            //if(val>0)
		            _mainActivity.send sensorManager.re
				}*/
				else if (args.length==2 && args[0].equals("/enableLocation") &&  args[1] instanceof Float) {
					float val= ((Float)args[1]).floatValue();
					if(val > 0){
						_mainActivity.startLocationUpdates();
					} else {
						_mainActivity.stopLocationUpdates();
					}
				} else if (args.length==2 && args[0].equals("/setPage") &&  args[1] instanceof Float) {
					int pageIndex = (int)((Float)args[1]).floatValue();
					if(pageIndex<=0){ //scroll to start page
						scrollContainer.smoothScrollTo(0, 0);  
					} else if (pageIndex>=_pageCount) {
						scrollContainer.smoothScrollTo((int)(scrollContainer.getWidth()*(_pageCount-1)),0);//last page
					} else scrollContainer.smoothScrollTo((int)(scrollContainer.getWidth()*pageIndex),0);

				} else if (args.length > 0 && args[0].equals("/getTime")) {
					Object[] msgArray = new Object[]{"/timeList",
							Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR)),
							Integer.valueOf(Calendar.getInstance().get(Calendar.MONTH)),
							Integer.valueOf(Calendar.getInstance().get(Calendar.DATE)),
							Integer.valueOf(Calendar.getInstance().get(Calendar.HOUR)),
							Integer.valueOf(Calendar.getInstance().get(Calendar.MINUTE)),
							Integer.valueOf(Calendar.getInstance().get(Calendar.SECOND)),
							Integer.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
					};
					PdBase.sendList("fromSystem", msgArray);

					Date now = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss z, d MMMM yyy");
					String formattedTime = sdf.format(now);
					Object[] msgArray2 = new Object[]{"/timeString", formattedTime};
					PdBase.sendList("fromSystem", msgArray2);
				}
			}
		}


		@Override
		public void receiveMessage(String source, String symbol, Object... args) {
			//Log.i(TAG, "message: " + Arrays.toString(args));
		}

		@Override
		public void receiveSymbol(String source, String symbol) {
			//pdPost("symbol: " + symbol);
		}
	};


	private void showAlert(String string) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(string);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", null);
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public Color getPatchBackgroundColor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void launchMenuFragment(MMPMenu menu) {
		MenuFragment menuFrag = new MenuFragment(menu, _bgColor);
		_mainActivity.launchFragment(menuFrag, menu.titleString);
	}

	@Override
	public void refreshMenuFragment(MMPMenu menu) {
		_mainActivity.refreshMenuFragment(menu);
	}

	@Override
	public void onPage(int pageIndex) {
		Object[] args = new Object[]{"/page", Integer.valueOf(pageIndex)};
		PdBase.sendList("fromSystem", args);	
	}
}

class MenuFragment extends Fragment{

	private MMPMenu _menu;
	private int _bgColor;
	private ListView _listView;
	private ArrayAdapter<String> _adapter;

	public MenuFragment(MMPMenu menu, int bgColor) {
		super();
		_menu = menu;
		_bgColor = bgColor;
	}

	public MMPMenu getMenu() {
		return _menu;
	}

	public void refresh() {
		_adapter.notifyDataSetChanged();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_menu, container,
				false);

		final int color = _menu.color;
		//highlightColor = intent.getIntExtra("highlightColor", Color.RED);
		List<String> stringList = _menu.stringList;

		_listView = (ListView)rootView.findViewById(R.id.listView1);
		FrameLayout frameLayout = (FrameLayout)rootView.findViewById(R.id.container);
		frameLayout.setBackgroundColor(_bgColor);

		_adapter = new ArrayAdapter<String>(getActivity(), R.layout.centered_text, stringList) {
			@Override
			public View getView(int position, View convertView,
					ViewGroup parent) {
				View view =super.getView(position, convertView, parent);
				TextView textView=(TextView) view.findViewById(android.R.id.text1);
				textView.setTextColor(color);
				//textView.setHighlightColor(MenuActivity.this.highlightColor);//doesn't work...
				return view;
			}
		};
		_listView.setAdapter(_adapter);

		_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
				//TextView clickedView = (TextView) view;
				//Toast.makeText(MenuActivity.this, "Item with id ["+id+"] - Position ["+position+"] - ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
				_menu.didSelect(position);
				getActivity().getSupportFragmentManager().popBackStack();
			}	
		});

		return rootView;
	}
}

class SplashFragment extends Fragment{

	View rootView;
	ImageView ringView, titleView, crossView, resistorView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_splash, container,
				false);
		ringView = (ImageView)rootView.findViewById(R.id.imageViewRing);
		titleView = (ImageView)rootView.findViewById(R.id.imageViewTitle);
		crossView = (ImageView)rootView.findViewById(R.id.imageViewCross);
		resistorView = (ImageView)rootView.findViewById(R.id.imageViewResistor);

		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				animate();
			}
		});

		return rootView;
	}


	public void animate() {
		// fromX, toX, fromY, toY
		TranslateAnimation translateAnimationRing =
				new TranslateAnimation(Animation.ABSOLUTE, rootView.getWidth() / 2 - ringView.getWidth() / 2, 
						Animation.ABSOLUTE, rootView.getWidth() / 2 - ringView.getWidth() / 2, 
						Animation.ABSOLUTE, -ringView.getHeight() , 
						Animation.ABSOLUTE, rootView.getHeight() / 2 - ringView.getHeight() / 2);
		translateAnimationRing.setDuration(2000);
		translateAnimationRing.setFillAfter(true);
		translateAnimationRing.setFillEnabled(true);

		TranslateAnimation translateAnimationTitle =
				new TranslateAnimation(Animation.ABSOLUTE, rootView.getWidth() / 2 - titleView.getWidth() / 2, 
						Animation.ABSOLUTE, rootView.getWidth() / 2 - titleView.getWidth() / 2, 
						Animation.ABSOLUTE, rootView.getHeight() + titleView.getHeight() , 
						Animation.ABSOLUTE, rootView.getHeight() / 2 + ringView.getHeight() / 2 + 20 );// below ringview titleView.getHeight() / 2);
		translateAnimationTitle.setDuration(2000);
		translateAnimationTitle.setFillAfter(true);
		translateAnimationTitle.setFillEnabled(true);

		TranslateAnimation translateAnimationCross =
				new TranslateAnimation(Animation.ABSOLUTE,  -crossView.getWidth(), 
						Animation.ABSOLUTE, rootView.getWidth() / 2 - ringView.getWidth() / 6 - crossView.getWidth() / 2 , 
						Animation.ABSOLUTE, rootView.getHeight() / 2 - crossView.getHeight() / 2, 
						Animation.ABSOLUTE, rootView.getHeight() / 2 - crossView.getHeight() / 2);
		translateAnimationCross.setDuration(2000);
		translateAnimationCross.setFillAfter(true);
		translateAnimationCross.setFillEnabled(true);

		TranslateAnimation translateAnimationResistor =
				new TranslateAnimation(Animation.ABSOLUTE, rootView.getWidth() + resistorView.getWidth(), 
						Animation.ABSOLUTE, rootView.getWidth() / 2 + ringView.getWidth() / 6 - resistorView.getWidth() / 2, 
						Animation.ABSOLUTE, rootView.getHeight() / 2 - resistorView.getHeight() / 2,
						Animation.ABSOLUTE, rootView.getHeight() / 2 - resistorView.getHeight() / 2);
		translateAnimationResistor.setDuration(2000);
		translateAnimationResistor.setFillAfter(true);
		translateAnimationResistor.setFillEnabled(true);

		ringView.startAnimation(translateAnimationRing);
		titleView.startAnimation(translateAnimationTitle);
		crossView.startAnimation(translateAnimationCross);
		resistorView.startAnimation(translateAnimationResistor);
	}
}
