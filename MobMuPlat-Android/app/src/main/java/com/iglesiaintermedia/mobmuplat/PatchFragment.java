package com.iglesiaintermedia.mobmuplat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import com.iglesiaintermedia.mobmuplat.nativepdgui.MMPPdGui;
import com.iglesiaintermedia.mobmuplat.nativepdgui.MMPPdGuiUtils;
import com.iglesiaintermedia.mobmuplat.nativepdgui.Widget;

public class PatchFragment extends Fragment implements ControlDelegate, PagingScrollViewDelegate, PdListener{
    private String TAG = "PatchFragment";
    private static int VERSION = 2; // Spec version, incremented on breaking changes. 2 = new "range" spec for sliders+knobs. Only used if MMP JSON doesn't have version tag, which it should.
    public PagingHorizontalScrollView scrollContainer;
    public RelativeLayout scrollRelativeLayout; //content view of the scroll view.

    public enum CanvasType{ canvasTypeWidePhone ,canvasTypeTallPhone , canvasTypeWideTablet, canvasTypeTallTablet, canvasTypeCustom }

    CanvasType _canvasType;
    boolean _isOrientationLandscape;
    boolean _isPageScrollShortEnd;

    int _pageCount;
    int _startPageIndex;
    int _version = VERSION;

    public MainActivity _mainActivity;

    Map<String, ArrayList<MMPControl>> _allGUIControlMap; //control address, array of objects with that address. Allows multiple items with same address.
    Set<String> _wearAddressSet;

    int _bgColor;
    private Button _catchButton; // Just to catch a11y focus and allow footswitch.
    private MMPMenuButton _settingsButton;
    private View _container;
    PdUiDispatcher _dispatcher;
    MMPPdGui mPdGui;

    public String loadedWearString; //track gui sent to wear, for main activity to resend on foreground.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _mainActivity = (MainActivity)getActivity();
        _allGUIControlMap = new HashMap<String,ArrayList<MMPControl>>();
        _wearAddressSet = new HashSet<String>();
        mPdGui = new MMPPdGui();
        _dispatcher = new PdUiDispatcher() {
            @Override
            public void print(String s) {
                ConsoleLogController.getInstance().append(s);
            }
        };
        // Moved to loadSceneCommonReset
        /*_dispatcher.addListener("toGUI", this);
        _dispatcher.addListener("toNetwork", this);
        _dispatcher.addListener("toSystem", this);*/
        PdBase.setReceiver(_dispatcher);
        Widget.setDispatcher(_dispatcher);

        AssetManager am = _mainActivity.getApplicationContext().getAssets();
        Typeface typeface = Typeface.createFromAsset(am, "fonts/DejaVuSansMono.ttf");
        Widget.setDefaultTypeface(typeface);

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
        _catchButton = (Button)rootView.findViewById(R.id.catchFocusButton);
        _settingsButton = (MMPMenuButton)rootView.findViewById(R.id.button1);
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

                loadSceneFromJSON(fileJSON, null);
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

    private static int inverseColorFromColor(int color) {
        int a = ((color >> 24) & 0xFF) << 24;
        int r =
                ((color >> 16) & 0xFF) + 128 % 256 << 16;
        int g =
                ((color >> 8) & 0xFF)  + 128 % 256 << 8;
        int b =
                (color & 0xFF)  + 128 % 256;
        int result =  a |r |g |b;

        return result;
    }

    private void loadSceneCommonReset() {
        //TODO check recursion of file paths.
        loadedWearString = null;
        _wearAddressSet.clear();
        _allGUIControlMap.clear();
        scrollRelativeLayout.removeAllViews();

        mPdGui.widgets.clear(); //remove all widgets from pd gui

        _dispatcher.release();//Unsubscribe from Pd messages and release all resources
        // (re-)Add self as dispatch recipient for MMP symbols.
        _dispatcher.addListener("toGUI", this);
        _dispatcher.addListener("toNetwork", this);
        _dispatcher.addListener("toSystem", this);

        //TODO: is there a pdfile to close here?

        _mainActivity.stopLocationUpdates();

        _settingsButton.setBarColor(Color.BLACK);
    }

    public void loadScenePatchOnly(List<String[]>originalAtomLines) {
        loadSceneCommonReset();
        //don't bother with rotation for now...

        List<String[]>[] processedAtomLinesTuple = MMPPdGuiUtils.proccessAtomLines(originalAtomLines);
        if (processedAtomLinesTuple == null) {
            showAlert("Could not open pd file");
            _mainActivity.launchSettings(); //back to documents
            return;
        }

        List<String[]> patchAtomLines = processedAtomLinesTuple[0];
        List<String[]> guiAtomLines = processedAtomLinesTuple[1];

        // Format temp file
        StringBuilder outputStringBuilder = new StringBuilder();
        for (String[] line : patchAtomLines) {
            outputStringBuilder.append(TextUtils.join(" ", line));
            outputStringBuilder.append(";\n");
        }

        // Write file to disk
        File file = new File(MainActivity.getDocumentsFolderPath(), "tempPdFile");
        FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(file);//_mainActivity.openFileOutput(, Context.MODE_PRIVATE);
            outputStream.write(outputStringBuilder.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // derive patch layout
        if (guiAtomLines.size() == 0 || guiAtomLines.get(0).length < 6 || !guiAtomLines.get(0)[1].equals("canvas") ) {
            // ALERT! or move this check to main activity
            showAlert("Could not open pd file");
            _mainActivity.launchSettings(); //back to documents
            return;
        }

        float docCanvasSizeWidth = Float.parseFloat(guiAtomLines.get(0)[4]);
        float docCanvasSizeHeight = Float.parseFloat(guiAtomLines.get(0)[5]);
        // DEI check for zero/bad values
        boolean isOrientationLandscape = (docCanvasSizeWidth > docCanvasSizeHeight);

        int screenWidth = _container.getWidth();//_rootView.getWidth();
        int screenHeight = _container.getHeight();//_rootView.getHeight();
        //if(MainActivity.VERBOSE)Log.i(TAG, "load patch _container dim "+screenWidth+" "+screenHeight);
        if(screenWidth==0 || screenHeight == 0) return;//error

        double xRatio = (double)screenWidth/docCanvasSizeWidth;
        double yRatio = (double)screenHeight/docCanvasSizeHeight;
        float scale = (float)Math.min(xRatio, yRatio);
        //if(MainActivity.VERBOSE)Log.i(TAG, "ref size "+referenceWidth+" "+referenceHeight+" pgc "+_pageCount);

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams((int)(docCanvasSizeWidth*scale),(int)(docCanvasSizeHeight*scale));
        flp.gravity = Gravity.CENTER;
        scrollContainer.setLayoutParams(flp);

        FrameLayout.LayoutParams flp2 = new FrameLayout.LayoutParams((int)(docCanvasSizeWidth*scale),(int)(docCanvasSizeHeight*scale));
        flp2.gravity = Gravity.LEFT;
        //reset scroll to one page
        //scrollContainer.setLayoutParams(new FrameLayout.LayoutParams(_container.getWidth(),_container.getHeight()));
        scrollRelativeLayout.setLayoutParams(flp2);//new FrameLayout.LayoutParams(_container.getWidth(),_container.getHeight()));
        scrollRelativeLayout.setBackgroundColor(Color.WHITE);
        scrollRelativeLayout.setClipChildren(false); //move. allow rendering of labels outside widget bounds. EATS DRAW CPU!?

        //float scale = _container.getWidth() / docCanvasSizeWidth;
        mPdGui.buildUI(_mainActivity, guiAtomLines, scale); // create widgets

        int dollarSignZero = _mainActivity.loadPdFile("tempPdFile", null);//filenameToLoad); // loadbang to widgets

        for (Widget widget : mPdGui.widgets) {
            widget.replaceDollarZero(dollarSignZero);
            scrollRelativeLayout.addView(widget);
            widget.setup();
        }
        //scrollRelativeLayout.setClipChildren(true); //set back to true after labels are drawn.???
        //TODO call post-init method on gui objects.


        //TODO move to patch so we can add
        scrollRelativeLayout.addView(_catchButton);
        _catchButton.setVisibility(View.VISIBLE);
        scrollRelativeLayout.addView(_settingsButton);
        _settingsButton.setVisibility(View.VISIBLE);//not really necc, since initial welcome load should set it visible...
    }

    public void loadSceneFromJSON(String inString, final String parentPathString){
        loadSceneCommonReset();
        if (inString.isEmpty()) return;

        try {
            JsonParser parser = new JsonParser();
            JsonObject topDict = parser.parse(inString).getAsJsonObject();//top dict - exception on bad JSON
            //unlike ios, can just set orientation without physical rotation
            //
            //
            int screenWidth = _container.getWidth();//_rootView.getWidth();
            int screenHeight = _container.getHeight();//_rootView.getHeight();
            int customWidth = 0;
            int customHeight = 0;
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
                else if(canvasTypeString.split("[x]").length == 2){
                    _canvasType=CanvasType.canvasTypeCustom;
                    String[] tokens = canvasTypeString.split("[x]");
                    customWidth = Integer.parseInt(tokens[0]);
                    customHeight = Integer.parseInt(tokens[1]);
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
                        break;
                    case canvasTypeCustom:
                        referenceWidth = customHeight;
                        referenceHeight = customWidth;
                        break;
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
                        break;
                    case canvasTypeCustom:
                        referenceWidth = customWidth;
                        referenceHeight = customHeight;
                        break;
                }
            }
            double xRatio = (double)screenWidth/referenceWidth;
            double yRatio = (double)screenHeight/referenceHeight;
            float screenRatio = (float)Math.min(xRatio, yRatio);
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
            scrollRelativeLayout.setClipChildren(true);

            //
            if(topDict.getAsJsonArray("backgroundColor")!=null){
                JsonArray colorArray = topDict.getAsJsonArray("backgroundColor");
                if(colorArray.size()==4)
                    _bgColor = colorFromRGBAArray(colorArray);
                else if (colorArray.size()==3)
                    _bgColor = colorFromRGBArray(colorArray);
                scrollRelativeLayout.setBackgroundColor(_bgColor);
                _settingsButton.setBarColor(inverseColorFromColor(_bgColor));
            }

            final String pdFilename;
            if(topDict.get("pdFile")!=null) {
                pdFilename = topDict.get("pdFile").getAsString();// objectForKey:@"pdFile"]];
            }
            else {
                pdFilename = null;
                showAlert("This interface has not been linked to a PureData file. Add it in the editor!");

            }

            int version = VERSION;
            if(topDict.get("version")!=null) { // check for older versions.
                version = topDict.get("version").getAsInt();
                _version = version;
            }

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
                _mainActivity.sendWearMessage("/loadGUI", jsonString);
                // track the wear dict
                loadedWearString = jsonString;

                // iterate through wear pages and get addresses
                for(int i=0;i<pageGuiArray.size();i++){
                    JsonObject pageDict = pageGuiArray.get(i).getAsJsonObject();//page-level dict
                    if (pageDict.get("pageGui")==null)continue;
                    JsonObject pageGuiDict = pageDict.get("pageGui").getAsJsonObject();
                    if(pageGuiDict.get("address")==null)continue;// if doesn't have an address, skip
                    String address = pageGuiDict.get("address").getAsString();
                    _wearAddressSet.add(address);
                }
            }// end wear

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
                        if(guiDict.get("range")!=null) {
                            int range = guiDict.get("range").getAsInt();
                            if (version < 2) {
                                ((MMPSlider)control).setLegacyRange(range);
                            } else {
                                ((MMPSlider)control).setRange(range);
                            }
                        }
                    }
                    else if(classString.equals("MMPKnob")){
                        control = new MMPKnob(getActivity(), screenRatio);
                        int indicatorColor = Color.WHITE;
                        if(guiDict.get("indicatorColor")!=null){
                            indicatorColor = colorFromRGBAArray(guiDict.get("indicatorColor").getAsJsonArray());
                        }
                        ((MMPKnob)control).indicatorColor = indicatorColor;

                        if(guiDict.get("range")!=null) {
                            int range = guiDict.get("range").getAsInt();
                            if (version < 2) {
                                ((MMPKnob) control).setLegacyRange(range);
                            } else {
                                ((MMPKnob) control).setRange(range);
                            }
                        }
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
                        if (guiDict.get("hAlign")!=null) {
                            ((MMPLabel)control).setHorizontalTextAlignment(guiDict.get("hAlign").getAsInt());
                        }
                        if (guiDict.get("vAlign")!=null) {
                            ((MMPLabel)control).setVerticalTextAlignment(guiDict.get("vAlign").getAsInt());
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
                            String filename = guiDict.get("imagePath").getAsString();
                            //File extFile = new File(MainActivity.getDocumentsFolderPath(), path);
                            File imageFile = new File(parentPathString != null ? parentPathString : MainActivity.getDocumentsFolderPath(), filename);
                            ((MMPPanel)control).setImagePath(imageFile.getAbsolutePath());
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
            scrollRelativeLayout.addView(_catchButton);
            _catchButton.setVisibility(View.VISIBLE);
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

                    _mainActivity.loadPdFile(pdFilename, parentPathString);

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
//	private PdReceiver receiver = new PdReceiver() {

    //@Override
        /*public void print(String s) {
            ConsoleLogController.getInstance().append(s);
        }*/

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
                _mainActivity.sendWearMessage((String)addressObj, message);
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
                if (val > 0) _mainActivity.enableLight(true);
                else _mainActivity.enableLight(false);

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
            } else if (args.length > 0 && args[0].equals("/getIpAddress")) {
                String ipAddress = NetworkController.getIPAddress(true);
                if (ipAddress==null)ipAddress = "none";
                Object[] msgArray = new Object[]{"/ipAddress", ipAddress};
                PdBase.sendList("fromSystem", msgArray);
            } else if (args.length > 1 && args[0].equals("/textDialog") && (args[1] instanceof String)) {
                String tag = (String)args[1];
                //String title = Arrays.copyOfRange(args,2,args.length-1).toString();
                Object[] titleArray = Arrays.copyOfRange(args, 2, args.length);
                StringBuilder builder = new StringBuilder();
                for(Object s : titleArray) {
                    builder.append(s);
                    builder.append(" ");
                }
                showTextDialog(tag, builder.toString());
            } else if (args.length > 1 && args[0].equals("/confirmationDialog") && (args[1] instanceof String)) {
                String tag = (String)args[1];
                //String title = Arrays.copyOfRange(args,2,args.length-1).toString();
                Object[] titleArray = Arrays.copyOfRange(args, 2, args.length);
                StringBuilder builder = new StringBuilder();
                for(Object s : titleArray) {
                    builder.append(s);
                    builder.append(" ");
                }
                showConfirmationDialog(tag, builder.toString());
            }
        }
    }

    private void showConfirmationDialog(String tag, String title) {
        Context context = _mainActivity;
        final String finalTag = tag;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set title
        alertDialogBuilder.setTitle(title);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Object[] msgArray = new Object[]{"/confirmationDialog", finalTag, Integer.valueOf(1)};
                        PdBase.sendList("fromSystem", msgArray);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Object[] msgArray = new Object[]{"/confirmationDialog", finalTag, Integer.valueOf(0)};
                        PdBase.sendList("fromSystem", msgArray);
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void showTextDialog(String tag, String title) {
        Context context = _mainActivity;
        final String finalTag = tag;
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.text_input_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);


        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        TextView titleView = (TextView) promptsView.findViewById(R.id.textView1);
        titleView.setText(title);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                Object[] msgArray = new Object[]{"/textDialog", finalTag, userInput.getText().toString()};
                                PdBase.sendList("fromSystem", msgArray);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    @Override
    public void receiveMessage(String source, String symbol, Object... args) {
        //Log.i(TAG, "message: " + Arrays.toString(args));
    }

    @Override
    public void receiveSymbol(String source, String symbol) {
        //pdPost("symbol: " + symbol);
    }

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
        MenuFragment menuFrag = new MenuFragment();
        menuFrag.setMenuAndColor(menu, _bgColor);
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

