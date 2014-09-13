package com.iglesiaintermedia.mobmuplat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.InputDevice.MotionRange;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iglesiaintermedia.mobmuplat.PatchFragment.CanvasType;
import com.iglesiaintermedia.mobmuplat.controls.*;
import com.illposed.osc.OSCMessage;
import com.example.inputmanagercompat.InputManagerCompat;
import com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener;

public class MainActivity extends FragmentActivity implements LocationListener, SensorEventListener, AudioDelegate, InputDeviceListener, OnBackStackChangedListener {
	private static final String TAG = "MobMuPlat MainActivity";
	//
	private FrameLayout _topLayout;
	public CanvasType hardwareScreenType;
	// Bookmark it between config change
	private String _fileToLoad;

	//Pd
	private PdService pdService = null;
	private int openPDFileHandle;
	
	//HID
	private InputManagerCompat _inputManager;
	private SparseArray<InputDeviceState> _inputDeviceStates;
	
	//
	public final static float VERSION = 1.6f; //necc?

	// important public!
	public UsbMidiController usbMidiController;
	public NetworkController networkController;

	// new arch!
	private PatchFragment _patchFragment;
	private DocumentsFragment _documentsFragment;
	private AudioMidiFragment _audioMidiFragment;
	private NetworkFragment _networkFragment;
	private ConsoleFragment _consoleFragment;
	private HIDFragment _hidFragment;
	private Fragment _lastFrag;
	private String _lastFragTitle;
	
	public FlashlightController flashlightController;
	
	boolean _stopAudioWhilePaused = true; 

	private LocationManager locationManagerA, locationManagerB;
	
	//sensor
	float[] _rawAccelArray;
	float[] _cookedAccelArray; 
	Object[] _tiltsMsgArray; //addr+2 FL
	Object[] _accelMsgArray; //addr+3 FL
	Object[] _gyroMsgArray;
	Object[] _rotationMsgArray;
	Object[] _compassMsgArray; 
	private boolean _shouldSwapAxes = false;

	
	public void onBackStackChanged() { //TODO move
		int count = getSupportFragmentManager().getBackStackEntryCount();//testing
		String title = "MobMuPlat";
		if (count > 0) {
			title = getSupportFragmentManager().getBackStackEntryAt(count - 1).getName();
		}
		getActionBar().setTitle(title);
		if (count == 0 && getActionBar().isShowing()) getActionBar().hide();
		else if (count > 0 && !getActionBar().isShowing())getActionBar().show();
	}
			       
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
          //      WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		//getActionBar().setDisplayShowTitleEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
		getActionBar().hide(); //necc on L, not necc on kitkat
		
		//layout view 
		setContentView(R.layout.activity_main);
		
		processIntent();
		
		_inputManager = InputManagerCompat.Factory.getInputManager(this);
        _inputManager.registerInputDeviceListener(this, null);
        _inputDeviceStates = new SparseArray<InputDeviceState>();

        //device type (just for syncing docs)
        
        hardwareScreenType = CanvasType.canvasTypeIPhone3p5Inch; //default
        CharSequence deviceFromValues = getResources().getText(R.string.screen_type);
		if (deviceFromValues.equals("phone")) {
			//derive closer phone type
			Display display = getWindowManager().getDefaultDisplay();
	        Point size = new Point();
	        display.getSize(size);
	        int width = size.x;
	        int height = size.y;
	        float aspect = (float)height/width;
	        
	        if (aspect > 1.6375) {
	        	hardwareScreenType = CanvasType.canvasTypeIPhone4Inch; 
	        } else {
	        	hardwareScreenType = CanvasType.canvasTypeIPhone3p5Inch; 
	        }
		} else if (deviceFromValues.equals("7inch")) {
			hardwareScreenType = CanvasType.canvasTypeAndroid7Inch;
		} else if(deviceFromValues.equals("10inch")) {
			hardwareScreenType = CanvasType.canvasTypeIPad;
		} else {
			
		}
		
		//vrsion
		boolean shouldCopyDocs = false;
		int versionCode = 0;
		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionCode = packageInfo.versionCode;
			SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
			int lastOpenedVersionCode = sp.getInt("lastOpenedVersionCode", 0);
			if (versionCode > lastOpenedVersionCode) {
				shouldCopyDocs = true;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		//temp
		shouldCopyDocs = true;
		//copy
		if(shouldCopyDocs) {//!alreadyStartedOnVersion || [alreadyStartedOnVersion boolValue] == NO) {
	        List<String> defaultPatchesList;
	        if(hardwareScreenType == CanvasType.canvasTypeIPhone3p5Inch || hardwareScreenType == CanvasType.canvasTypeAndroid7Inch){
	        	defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine.mmp", "MMPTutorial1-GUI.mmp", "MMPTutorial2-Input.mmp", "MMPTutorial3-Hardware.mmp", "MMPTutorial4-Networking.mmp","MMPTutorial5-Files.mmp","MMPExamples-Vocoder.mmp", "MMPExamples-Motion.mmp", "MMPExamples-Sequencer.mmp", "MMPExamples-GPS.mmp", "MMPTutorial6-2DGraphics.mmp", "MMPExamples-LANdini.mmp", "MMPExamples-Arp.mmp", "MMPExamples-TableGlitch.mmp", "MMPExamples-HID.mmp");
	        }
	        else if (hardwareScreenType==CanvasType.canvasTypeIPhone4Inch){
	        	defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine-ip5.mmp", "MMPTutorial1-GUI-ip5.mmp", "MMPTutorial2-Input-ip5.mmp", "MMPTutorial3-Hardware-ip5.mmp", "MMPTutorial4-Networking-ip5.mmp","MMPTutorial5-Files-ip5.mmp", "MMPExamples-Vocoder-ip5.mmp", "MMPExamples-Motion-ip5.mmp", "MMPExamples-Sequencer-ip5.mmp","MMPExamples-GPS-ip5.mmp", "MMPTutorial6-2DGraphics-ip5.mmp", "MMPExamples-LANdini-ip5.mmp", "MMPExamples-Arp-ip5.mmp",  "MMPExamples-TableGlitch-ip5.mmp", "MMPExamples-HID-ip5.mmp");
	        }
	        else{//pad
	        	defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine-Pad.mmp", "MMPTutorial1-GUI-Pad.mmp", "MMPTutorial2-Input-Pad.mmp", "MMPTutorial3-Hardware-Pad.mmp", "MMPTutorial4-Networking-Pad.mmp","MMPTutorial5-Files-Pad.mmp", "MMPExamples-Vocoder-Pad.mmp", "MMPExamples-Motion-Pad.mmp", "MMPExamples-Sequencer-Pad.mmp","MMPExamples-GPS-Pad.mmp", "MMPTutorial6-2DGraphics-Pad.mmp", "MMPExamples-LANdini-Pad.mmp", "MMPExamples-Arp-Pad.mmp",  "MMPExamples-TableGlitch-Pad.mmp", "MMPExamples-HID-Pad.mmp");
	        }
	        
	        List<String> commonFilesList = Arrays.asList("MMPTutorial0-HelloSine.pd","MMPTutorial1-GUI.pd", "MMPTutorial2-Input.pd", "MMPTutorial3-Hardware.pd", "MMPTutorial4-Networking.pd","MMPTutorial5-Files.pd","cats1.jpg", "cats2.jpg","cats3.jpg","clap.wav","Welcome.pd",  "MMPExamples-Vocoder.pd", "vocod_channel.pd", "MMPExamples-Motion.pd", "MMPExamples-Sequencer.pd", "MMPExamples-GPS.pd", "MMPTutorial6-2DGraphics.pd", "MMPExamples-LANdini.pd", "MMPExamples-Arp.pd", "MMPExamples-TableGlitch.pd", "anderson1.wav", "MMPExamples-HID.pd");
	        
	        //defaultPatches = [defaultPatches arrayByAddingObjectsFromArray:commonFiles];
	        
	        for (String filename : defaultPatchesList) {
	        	copyAsset(filename);
	        }
	        for (String filename : commonFilesList) {
	        	copyAsset(filename);
	        }
	        
	        //assuming success, write to user preference
	        if (versionCode > 0) {
	        	SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
	        	SharedPreferences.Editor editor = settings.edit();
	        	editor.putInt("lastOpenedVersionCode", versionCode);
	        	editor.commit();
	        }
	        
	    }
		
		//
		AudioParameters.init(this);
		PdPreferences.initPreferences(getApplicationContext());
		initSensors();
		initLocation();
		usbMidiController = new UsbMidiController(this); //matched close in onDestroy...move closer in?
			
		//allow multicast
		WifiManager wifi = (WifiManager)getSystemService( Context.WIFI_SERVICE );
		if(wifi != null){
			WifiManager.MulticastLock lock = wifi.createMulticastLock("Log_Tag");
			lock.acquire();
		}  //Automatically released on app exit/crash.
		
		networkController = new NetworkController();
		networkController.delegate = this;

		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
		
		//fragments
		_patchFragment = new PatchFragment(this); //reference to this for launching menu fragment on master stacj
		_documentsFragment = new DocumentsFragment();
		_networkFragment = new NetworkFragment();
		_consoleFragment = new ConsoleFragment();
		_hidFragment = new HIDFragment();
		_audioMidiFragment = new AudioMidiFragment();
		_audioMidiFragment.audioDelegate = this;
		// bookmark for launch from info button
		_lastFrag = _documentsFragment;
		_lastFragTitle = "Documents";
		
		
		_topLayout = (FrameLayout)findViewById(R.id.container);
		// Go full screen and lay out to top of screen.
		// Only on SDK >= 16. This means that patch will not go truly fullscreen on ICS. Separate layouts for 14+ vs 16+ for the fragment margins.
		_topLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		
		//Flashlight TODO make black
		SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
		flashlightController = new FlashlightController(surfaceView);
		flashlightController.startCamera();

		// axes for table
		if (getDeviceNaturalOrientation() == Configuration.ORIENTATION_LANDSCAPE) _shouldSwapAxes = true;
		
		// set action bar title on fragment stack changes
		getSupportFragmentManager().addOnBackStackChangedListener(this);
				
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				
		if (savedInstanceState == null) {
			launchSplash();
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) { //called by open with file
	    super.onNewIntent(intent);
	    setIntent(intent);
	    processIntent();
	}
	
	private void processIntent() {
		Intent i = getIntent();
		if(i!=null) {
			/*if(i.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
		        UsbDevice device  = (UsbDevice)i.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		        //TODO double check that on app-start processIntent is called after setting up usbmidicontroller
		        //usbMidiController.onDeviceAttached(device);
		        //here: set intent filter not to open mmp, but still get intents if open???
		        //getActionBar().setBackgroundDrawable(new ColorDrawable(Color.RED));
				
			}
			else {*///file
				
			//TODO check the action of this intent!!!!
			Uri dataUri = i.getData();
			if(dataUri!=null){
				Log.i(TAG, "receive intent data " + dataUri.toString());
				
				String filename = dataUri.getLastPathSegment();
				String fullPath = dataUri.getPath();
				int lastSlashPos = fullPath.lastIndexOf('/');

			    //if (lastSlashPos >= 0)
			    
			    String parentPath = fullPath.substring(0, lastSlashPos+1);
				String suffix = filename.substring(filename.lastIndexOf('.'));
				if (suffix.equals(".zip")) {
					unpackZip(parentPath, filename);
				}
				else {
					copyUri(dataUri);
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		flashlightController.stopCamera();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(_stopAudioWhilePaused) {
			stopAudio();
		}
		flashlightController.stopCamera();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if(pdService!=null && !pdService.isRunning()) {
			pdService.startAudio();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		flashlightController.startCamera();
	}

	public void loadScene(String filenameToLoad) {
		getActionBar().hide();
		
		_fileToLoad = filenameToLoad;
		String fileJSON = readMMPToString(filenameToLoad);
		boolean requestedChange = setOrientationOfMMP(fileJSON);
        
		if(!requestedChange){
			_patchFragment.loadSceneFromJSON(fileJSON);
			_fileToLoad = null;
		}
        //otherwise is being loaded in onConfigChange
	}
	
	public void loadScenePatchOnly(String filenameToLoad) {
		//don't bother with rotation for now...
		stopLocationUpdates();
		_patchFragment.scrollRelativeLayout.removeAllViews();
		
		getActionBar().hide();
		
		//reset scroll to one page
		_patchFragment.scrollContainer.setLayoutParams(new FrameLayout.LayoutParams(_topLayout.getWidth(),_topLayout.getHeight()));
		_patchFragment.scrollRelativeLayout.setLayoutParams(new FrameLayout.LayoutParams(_topLayout.getWidth(),_topLayout.getHeight()));
		_patchFragment.scrollRelativeLayout.setBackgroundColor(Color.GRAY);
		TextView tv = new TextView(this);
		tv.setText("running "+filenameToLoad+"\nwith no interface\n\n(any network data will be\n on default port "+NetworkController.DEFAULT_PORT_NUMBER+")");
		tv.setTextColor(Color.WHITE);
		tv.setGravity(Gravity.CENTER);
		
		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		rlp.addRule(RelativeLayout.CENTER_VERTICAL); 
		rlp.addRule(RelativeLayout.CENTER_HORIZONTAL); 
		tv.setLayoutParams(rlp);
		_patchFragment.scrollRelativeLayout.addView(tv);
		//TODO RESET ports!
		
		loadPdFile(filenameToLoad);
		//TODO move to patch so we can add 
		_patchFragment.scrollRelativeLayout.addView(_patchFragment._settingsButton);
		_patchFragment._settingsButton.setVisibility(View.VISIBLE);//not really necc, since initial welcome load should set it visible...
	}
	
	public void loadPdFile(String pdFilename) {
		// load pd patch
		if(openPDFileHandle != 0)PdBase.closePatch(openPDFileHandle); 
		//open
		// File patchFile = null;
		if(pdFilename!=null) {
			try {
				File pdFile = new File(MainActivity.getDocumentsFolderPath(), pdFilename);
				openPDFileHandle = PdBase.openPatch(pdFile);
			} catch (FileNotFoundException e) {
				showAlert("PD file "+pdFilename+" not found.");
			} catch (IOException e) {
				showAlert("I/O error on loading PD file "+pdFilename+".");
			}
		}
	}
	
	static public String readMMPToString(String filename) {
		if (filename == null) return null;
		File file = new File(MainActivity.getDocumentsFolderPath(),filename);
		if (!file.exists())return null;

		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			InputStream is = new FileInputStream(file);
			Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
			try {
				is.close();
			} catch (Exception e) {

			}
		} catch (Exception e) {
			//TODO - ioexception
		} finally {

		}

		String jsonString = writer.toString();

		return jsonString;
	}
	
	static public String readMMPAssetToString(InputStream is) {
		if (is == null) return null;
		//File file = new File(MainActivity.getDocumentsFolderPath(),filename);
		
		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
			try {
				is.close();
			} catch (Exception e) {

			}
		} catch (Exception e) {
			//TODO - ioexception
		} finally {

		}

		String jsonString = writer.toString();

		return jsonString;
	}

	private void initLocation() {
		locationManagerA = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//locationManagerA.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
		locationManagerB = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//locationManagerB.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
	}
	
	public void startLocationUpdates() {
		if (locationManagerA!=null)locationManagerA.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
		if (locationManagerB!=null)locationManagerB.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
	}
	
	public void stopLocationUpdates() {
		if (locationManagerA!=null)locationManagerA.removeUpdates(this);
		if (locationManagerB!=null)locationManagerB.removeUpdates(this);
	}
	
	private void initSensors() {

		//_camera = Camera.open();
		_rawAccelArray = new float[3];
		_cookedAccelArray = new float[3];
		_tiltsMsgArray = new Object[3];
		_tiltsMsgArray[0] = "/tilts";
		_accelMsgArray = new Object[4];
		_accelMsgArray[0] = "/accel";
		
		_gyroMsgArray = new Object[4];
		_gyroMsgArray[0] = "/gyro";
		_rotationMsgArray = new Object[4];
		_rotationMsgArray[0] = "/motion";
		_compassMsgArray = new Object[2];
		_compassMsgArray[0] = "/compass";
		
		
		SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this,  accel, SensorManager.SENSOR_DELAY_GAME);//TODO rate
		Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorManager.registerListener(this,  gyro, SensorManager.SENSOR_DELAY_GAME);//TODO rate
		Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorManager.registerListener(this,  rotation, SensorManager.SENSOR_DELAY_GAME);//TODO rate
		Sensor compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sensorManager.registerListener(this,  compass, SensorManager.SENSOR_DELAY_GAME);//TODO rate
	}

	//insane: 10" tablets can have a "natural" (Surface.ROTATION_0) at landscape, not portrait. Determine
	// the "natural" orientation so that the setOrientation() logic works as expected...
	public int getDeviceNaturalOrientation() {

		WindowManager windowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);

		Configuration config = getResources().getConfiguration();

		int rotation = windowManager.getDefaultDisplay().getRotation();

		if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
				config.orientation == Configuration.ORIENTATION_LANDSCAPE)
				|| ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&    
						config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
			return Configuration.ORIENTATION_LANDSCAPE;
		} else { 
			return Configuration.ORIENTATION_PORTRAIT;
		}
	}

	private boolean setOrientationOfMMP(String jsonString){ //returns whether there was a change
		JsonParser parser = new JsonParser();
		JsonObject topDict = parser.parse(jsonString).getAsJsonObject();//top dict
		int screenOrientation = this.getWindow().getWindowManager().getDefaultDisplay().getRotation();// on tablet rotation_0 = "natural"= landscape
		int mmpOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		if(topDict.get("isOrientationLandscape")!=null) {
			boolean isOrientationLandscape = topDict.get("isOrientationLandscape").getAsBoolean();
			if (isOrientationLandscape)mmpOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		}

		int naturalOrientation = getDeviceNaturalOrientation();
		if (naturalOrientation == Configuration.ORIENTATION_PORTRAIT) { //phones, 7" tablets
			if ((mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && screenOrientation!=Surface.ROTATION_0) ||
					(mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && screenOrientation!=Surface.ROTATION_90)) {
				Log.i(TAG, "requesting orientation...surface = "+screenOrientation);
				this.setRequestedOrientation(mmpOrientation);
				return true;
			}
		}
		//"natural" = landscape = big tablet
		else if (naturalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			if ((mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && screenOrientation!=Surface.ROTATION_270) || //weird that it thinks that rotation 270 is portrait
					(mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && screenOrientation!=Surface.ROTATION_0)) {
				Log.i(TAG, "requesting orientation...surface = "+screenOrientation);
				this.setRequestedOrientation(mmpOrientation);
				return true;
			}
		}
		return false;
	}

	/*public File getStorageDir(Context context, String albumName) {
	    // Get the directory for the app's private pictures directory. 
	    File file = new File(context.getExternalFilesDir(
	            Environment.DIRECTORY_PICTURES), albumName);
	    Log.i(TAG, "try to make "+file.getAbsolutePath());
	    if (!file.mkdirs()) {
	        Log.e(TAG, "Directory not created");
	    }
	    return file;	
	}*/

	//TODO add mem checking
	private void copyAllDocuments() { //here not working with linked files!
		//AssetManager am = getAssets();
		String [] list;
		try {
			list = getAssets().list("");
			if (list.length > 0) {
				// This is a folder
				for (String filename : list) {
					//if (!listAssetFiles(path + "/" + file))
					Log.i(TAG, "file "+filename); 
					//getAssets().open(fileName;)
					copyAsset(filename);
				}
			} else {
				Log.i(TAG, "file single ");
			}//path is a file
		} catch (IOException e) {

		}
	}

	public void copyUri(Uri uri){
		String sourcePath = uri.getPath();
		String sourceFilename = uri.getLastPathSegment();

		File file = new File(MainActivity.getDocumentsFolderPath(), sourceFilename);

		try {
			Log.i(TAG, "uri filename "+sourceFilename);
			InputStream in = new FileInputStream(sourcePath);
			OutputStream out = new FileOutputStream(file);
			byte[] data = new byte[in.available()];
			in.read(data);
			out.write(data);
			in.close();
			out.close();

			showAlert("File "+sourceFilename+" copied to MobMuPlat Documents");
		} catch (FileNotFoundException e) {
			Log.i("Test", "Setupf::copyResources - "+e.getMessage());
		} catch (IOException e) {
			Log.i("Test", "Setupi::copyResources - "+e.getMessage());
		}
	}

	//TODO consolidate this with version in fragment
	private void showAlert(String string) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(string);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", null);
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void copyAsset(String assetFilename){
		AssetManager assetManager = getAssets();
		File file = new File(MainActivity.getDocumentsFolderPath(), assetFilename);

		//String type = Environment.DIRECTORY_DOCUMENTS;
		//File path = Environment.getExternalStoragePublicDirectory(type);
		try {
			//Log.i(TAG, "filename "+assetFilename);
			InputStream in = assetManager.open(assetFilename);
			OutputStream out = new FileOutputStream(file);
			byte[] data = new byte[in.available()];
			in.read(data);
			out.write(data);
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			Log.i("Test", "Setupf::copyResources - "+e.getMessage());
		} catch (IOException e) {
			Log.i("Test", "Setupi::copyResources - "+e.getMessage());
		}

	}


	// on orientation changed
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.i(TAG, "ACT on config changed - file to load "+_fileToLoad);
		//if(_fileToLoad!=null)initGui(_fileToLoad);
		//initGui();
		ViewTreeObserver observer = _topLayout.getViewTreeObserver();
	    observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

	        @Override
	        public void onGlobalLayout() {
	        	_topLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
	        	if(_fileToLoad!=null) {	
	        		_patchFragment.loadSceneFromJSON(readMMPToString(_fileToLoad));
	        		_fileToLoad = null;
	        	}
	        }
	    });
	}

	private final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connected");
			pdService = ((PdService.PdBinder)service).getService();
			//initPd();
			initAndStartAudio(); 
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// this method will never be called
		}
	};

	private void initAndStartAudio() {
		
		try {
			//TODO flag for simulator which doesn't seem to have access to mic?
			pdService.initAudio(-1, /*-1 real 0 sim:*/-1, -1, -1);   // negative values will be replaced with defaults/preferences
			pdService.startAudio();
		} catch (IOException e) {
			Log.i(TAG, e.toString());
		}
	}

	private void stopAudio() {
		if(pdService!=null)pdService.stopAudio();
	}

	private void cleanup() {
		try {
			unbindService(pdConnection);
		} catch (IllegalArgumentException e) {
			// already unbound
			pdService = null;
		}
		flashlightController.stopCamera();
	}

	protected void onDestroy() {
		usbMidiController.close(); //solves "Intent Receiver Leaked: ... are you missing a call to unregisterReceiver()?"
		super.onDestroy();
		cleanup();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void launchSplash(){
		final SplashFragment sf = new SplashFragment();
		final FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(R.id.container, sf);
		fragmentTransaction.commit(); 
		
		Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if(this!=null && !isFinishing()){
                	fragmentManager.beginTransaction().remove(sf).commit(); //HERE:
                	/*09-07 22:22:32.893: E/AndroidRuntime(25605): FATAL EXCEPTION: main
09-07 22:22:32.893: E/AndroidRuntime(25605): Process: com.iglesiaintermedia.mobmuplat, PID: 25605
09-07 22:22:32.893: E/AndroidRuntime(25605): java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.support.v4.app.FragmentManagerImpl.checkStateLoss(FragmentManager.java:1354)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.support.v4.app.FragmentManagerImpl.enqueueAction(FragmentManager.java:1372)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.support.v4.app.BackStackRecord.commitInternal(BackStackRecord.java:595)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.support.v4.app.BackStackRecord.commit(BackStackRecord.java:574)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at com.iglesiaintermedia.mobmuplat.MainActivity$3.run(MainActivity.java:737)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.os.Handler.handleCallback(Handler.java:733)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.os.Handler.dispatchMessage(Handler.java:95)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.os.Looper.loop(Looper.java:137)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at android.app.ActivityThread.main(ActivityThread.java:4998)
09-07 22:22:32.893: E/AndroidRuntime(25605): 	at java.lang.reflect.Method.invokeNative(Native Method)
*/
                	//getActionBar().show();
                	getSupportFragmentManager().beginTransaction().add(R.id.container, _patchFragment).commit();	
                }
            }
        }, 4000);
	}

	public void launchFragment(Fragment frag, String title) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		//Log.i(TAG, "launch with stacj size "+fragmentManager.getBackStackEntryCount());
		
		
		//int count = fragmentManager.getBackStackEntryCount();
		//Fragment temp = fragmentManager.getBackStackEntryAt(count=1);
		// if already what we are looking at, return (otherwise it pops but isn't re-added)
		if (fragmentManager.getBackStackEntryCount()==1 && fragmentManager.getBackStackEntryAt(0).getName() == title) {
			return;
		}
		// if something else is on the stack above patch, pop it, but remove listener to not remove/readd action bar
		if (fragmentManager.getBackStackEntryCount() > 0) {
			fragmentManager.removeOnBackStackChangedListener(this);
			fragmentManager.popBackStackImmediate();
			fragmentManager.addOnBackStackChangedListener(this);
		}
		
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(R.id.container, frag);
		fragmentTransaction.addToBackStack(title);
		fragmentTransaction.commit(); 
		fragmentManager.executePendingTransactions();//do it immediately, not async
	}
	
	public void launchSettings() {
		//_topLayout.setFitsSystemWindows(true);
		//_topLayout.setSystemUiVisibility(0);
		//getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getActionBar().show();
		launchFragment(_lastFrag, _lastFragTitle);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if (id == R.id.action_documents) {
			launchFragment(_documentsFragment, "Documents");
			_lastFrag = _documentsFragment;
			_lastFragTitle = "Documents";
			return true;
		} else if (id == R.id.action_audiomidi) {
			launchFragment(_audioMidiFragment, "Audio & Midi");
			_lastFrag = _audioMidiFragment;
			_lastFragTitle = "Audio & Midi";
			return true;
		} else if (id == R.id.action_network) {
			launchFragment(_networkFragment, "Networking");
			_lastFrag = _networkFragment;
			_lastFragTitle = "Networking";
			return true;
		} else if (id == R.id.action_console) {
			launchFragment(_consoleFragment, "Console");
			_lastFrag = _consoleFragment;
			_lastFragTitle = "Console";
			return true;
		} else if (id == R.id.action_HID) {
			launchFragment(_hidFragment, "Human Interface Device");
			_lastFrag = _hidFragment;
			_lastFragTitle = "Human Interface Device";
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void cookAccel(float[] rawAccel, float outputAccel[]) { //input is -10 to 10, inverted on x
		//assumes in 3, out 2
		float cookedX = rawAccel[0];
		float cookedY = rawAccel[1];
		float accelZ = rawAccel[2];
		// cook it via Z accel to see when we have tipped it beyond 90 degrees
		
		if(cookedX>0 && accelZ>0) cookedX=(2-cookedX); //tip towards long side
		else if(cookedX<0 && accelZ>0) cookedX=(-2-cookedX); //tip away long side
		
		if(cookedY>0 && accelZ>0) cookedY=(2-cookedY); //tip right
		else if(cookedY<0 && accelZ>0) cookedY=(-2-cookedY); //tip left
		
	    //clip 
	    if(cookedX<-1)cookedX=-1;
	    else if(cookedX>1)cookedX=1;
	    if(cookedY<-1)cookedY=-1;
	    else if(cookedY>1)cookedY=1;
		//return new float[]{cookedX, cookedY};
	    outputAccel[0] = cookedX;
	    outputAccel[1] = cookedY;
	}
	
	
	
	@Override
	
	
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			//Log.i(TAG, "ac "+event.values[0]+" "+event.values[1]+" "+event.values[2]);
			//float rawAccel[] = {event.values[0], event.values[1], event.values[2]};
			for (int i=0 ; i < 3; i++) {
				_rawAccelArray[i] = event.values[i];
			}
			//invert and scale to match ios
			for(int i=0;i<3;i++)_rawAccelArray[i] = -_rawAccelArray[i]/10.0f;
			if (_shouldSwapAxes) _rawAccelArray[0] *= -1; //for big tablets, will become y axis, needs to be flipped
			cookAccel(_rawAccelArray, _cookedAccelArray);
			_tiltsMsgArray[1]=Float.valueOf(_cookedAccelArray[_shouldSwapAxes ? 1 : 0]); //{"/tilts", Float.valueOf(cookedAccel[0]), Float.valueOf(cookedAccel[1]) }; 
			_tiltsMsgArray[2]=Float.valueOf(_cookedAccelArray[_shouldSwapAxes ? 0 : 1]);
			PdBase.sendList("fromSystem", _tiltsMsgArray);
			//Log.v(TAG, "accel "+event.values[0]+" "+event.values[1]+" "+event.values[2] ); //-10 to 10
			//_accelArray = //{"/accel", Float.valueOf(rawAccel[0]), Float.valueOf(rawAccel[1]), Float.valueOf(rawAccel[2]) }; 
			_accelMsgArray[1] = Float.valueOf(_rawAccelArray[_shouldSwapAxes ? 1 : 0]);
			_accelMsgArray[2] = Float.valueOf(_rawAccelArray[_shouldSwapAxes ? 0 : 1]);
			_accelMsgArray[3] = Float.valueOf(_rawAccelArray[2]);
			PdBase.sendList("fromSystem", _accelMsgArray);
		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			//Log.v(TAG, "gyro "+event.values[0]+" "+event.values[1]+" "+event.values[2] );// diff axes than ios?
			//Object[] msgArray = {"/gyro", Float.valueOf(event.values[0]), Float.valueOf(event.values[1]), Float.valueOf(event.values[2]) }; 
			_gyroMsgArray[1] = Float.valueOf(event.values[0]);
			float yVal = event.values[1]; if (_shouldSwapAxes) yVal *= -1;
			_gyroMsgArray[2] = Float.valueOf(yVal); 
			_gyroMsgArray[3] = Float.valueOf(event.values[2]);
			PdBase.sendList("fromSystem", _gyroMsgArray);
		} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
			//Log.v(TAG, "rotation "+event.values[0]+" "+event.values[1]+" "+event.values[2]+" "+event.values[3] );//-1 to 1 scale to +/- pi and invert pitch
			//Object[] msgArray = {"/motion", Float.valueOf(event.values[0]*(float)Math.PI), Float.valueOf(-event.values[1]*(float)Math.PI), Float.valueOf(event.values[2]*(float)Math.PI) }; 
			_rotationMsgArray[1] = Float.valueOf(event.values[0]) * Math.PI;
			_rotationMsgArray[2] = Float.valueOf(event.values[1]) * Math.PI;
			_rotationMsgArray[3] = Float.valueOf(event.values[2]) * Math.PI;
			PdBase.sendList("fromSystem", _rotationMsgArray);
		} else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			//Log.i(TAG, "comp "+event.values[0]+" "+event.values[1]+" "+event.values[2]);
			//Object[] msgArray = {"/compass", Float.valueOf(event.values[0])}; 
			_compassMsgArray[1]=Float.valueOf(event.values[0]);
			PdBase.sendList("fromSystem", _compassMsgArray);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public static String getDocumentsFolderPath() {
		File fileDir = new File(Environment.getExternalStorageDirectory(), "MobMuPlat");//device/sdcard
		fileDir.mkdir(); // make mobmuplat dir if not there
		return fileDir.getAbsolutePath();
	}

	//

	public void receiveOSCMessage(OSCMessage message) {
		// message should have proper object types, but need to put address back in front of array
		Object[] messageArgs = message.getArguments();
		Object[] newArgs = new Object[messageArgs.length + 1];
		newArgs[0] = message.getAddress();
		for (int i=0;i<messageArgs.length;i++) {
			newArgs[i+1] = messageArgs[i];
		}

		PdBase.sendList("fromNetwork", newArgs);

	}

	@Override
	public void onLocationChanged(Location location) {
		Log.i(TAG, "loc "+location.getLatitude()+" "+location.getLongitude()+" "+location.getAltitude());
		
		int latRough = (int)( location.getLatitude()*1000);
	    int longRough = (int)(location.getLongitude()*1000);
	    int latFine = (int)Math.abs((location.getLatitude() % .001)*1000000);
	    int longFine = (int)Math.abs(( location.getLongitude() % .001)*1000000);

	    Object[] msgArray = {"/location", 
				Float.valueOf((float)location.getLatitude()), 
				Float.valueOf((float)location.getLongitude()),
				Float.valueOf((float)location.getAltitude()), 
				Float.valueOf((float)location.getAccuracy()),
				Float.valueOf((float)location.getAccuracy()),//repeat since no separat call for vertical accuracy 
				Integer.valueOf(latRough),
				Integer.valueOf(longRough),
				Integer.valueOf(latFine),
				Integer.valueOf(longFine)				
		};
		PdBase.sendList("fromSystem", msgArray);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSampleRate() {
		if(pdService!=null)return pdService.getSampleRate();
		else return 0;
	}

	@Override
	public void setBackgroundAudioEnabled(boolean backgroundAudioEnabled) {
		_stopAudioWhilePaused = !backgroundAudioEnabled;
	}

	private boolean unpackZip(String path, String zipname) {    
		Log.i("ZIP", "unzipping "+path+" "+zipname);
	     InputStream is;
	     ZipInputStream zis;
	     try {
	         String filename;
	         is = new FileInputStream(path + zipname);
	         zis = new ZipInputStream(new BufferedInputStream(is));          
	         ZipEntry ze;
	         byte[] buffer = new byte[1024];
	         int count;

	         while ((ze = zis.getNextEntry()) != null) {
	        	
	             // zapis do souboru
	             filename = ze.getName();
	             Log.i("ZIP", "opening "+filename);

	             // Need to create directories if not exists, or
	             // it will generate an Exception...
	             if (ze.isDirectory()) {
	                File fmd = new File(MainActivity.getDocumentsFolderPath(),  filename);
	                fmd.mkdirs();
	                continue;
	             }

	             File outFile = new File(MainActivity.getDocumentsFolderPath(), filename);
	             Log.i("ZIP", "to: "+outFile.getAbsolutePath());
	             FileOutputStream fout = new FileOutputStream(outFile);

	             // cteni zipu a zapis
	             while ((count = zis.read(buffer)) != -1) 
	             {
	                 fout.write(buffer, 0, count);             
	             }

	             fout.close();               
	             zis.closeEntry();
	             Log.i("ZIP", "wrote "+filename);
	         }

	         zis.close();
	         Log.i("ZIP", "complete");
	         showAlert("Unzipped contents of "+zipname+" into Documents folder.");
	     } 
	     catch(Exception e) {
	         e.printStackTrace();
	         showAlert("Error unzipping contents of "+zipname);
	         return false;
	     } 

	    return true;
	}
	
	// HID

	@Override
	public void onInputDeviceAdded(int deviceId) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "input device added!", Toast.LENGTH_SHORT).show();;
		
	}

	@Override
	public void onInputDeviceChanged(int deviceId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInputDeviceRemoved(int deviceId) {
		// TODO Auto-generated method stub
		
	}
	
	public void refreshMenuFragment(MMPMenu menu) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if(fragmentManager.getBackStackEntryCount()==1 && 
				(fragmentManager.getBackStackEntryAt(0) instanceof MenuFragment) &&
				((MenuFragment)fragmentManager.getBackStackEntryAt(0)).getMenu() == menu) {
			((MenuFragment)fragmentManager.getBackStackEntryAt(0)).refresh();
		}
	}
	
	private InputDeviceState getInputDeviceState(InputEvent event) {
        final int deviceId = event.getDeviceId();
        InputDeviceState state = _inputDeviceStates.get(deviceId);
        if (state == null) {
            final InputDevice device = event.getDevice();
            if (device == null) {
                return null;
            }
            state = new InputDeviceState(device);
            _inputDeviceStates.put(deviceId, state);

            Log.i(TAG, device.toString());
        }
        return state;
    }
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		InputDeviceState state = getInputDeviceState(event);
        if (state != null && state.onKeyDown(event)) { //pd message sent in state.onKeyUp/Down
       		if (_hidFragment.isVisible()) {
       			_hidFragment.show(state);
        	}
        	return true;   
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	InputDeviceState state = getInputDeviceState(event);
        if (state != null && state.onKeyUp(event)) { //pd message sent in state.onKeyUp/Down
       		if (_hidFragment.isVisible()) {
       			_hidFragment.show(state);
       		}
       		return true;
        }    
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        _inputManager.onGenericMotionEvent(event);
        //
        //int eventSourceDebug = event.getSource();
        //Toast.makeText(this, "event source "+eventSourceDebug, Toast.LENGTH_SHORT).show();
        
        
        
        // Check that the event came from a joystick or gamepad since a generic
        // motion event could be almost anything. API level 18 adds the useful
        // event.isFromSource() helper function.
        int eventSource = event.getSource();
        if ((((eventSource & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                ((eventSource & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK))
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            //int id = event.getDeviceId();
            InputDeviceState state = getInputDeviceState(event);
            if (state != null && state.onJoystickMotion(event)) { //pd message sent in state.onJoystickMotion
            	if (_hidFragment.isVisible()) {
        			_hidFragment.show(state);
        		}
            	return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

	///========================
}
class PatchFragment extends Fragment implements ControlDelegate, PagingScrollViewDelegate{
	private boolean verbose = false;
	private String TAG = "PatchFragment";

	public PagingHorizontalScrollView scrollContainer;
	public RelativeLayout scrollRelativeLayout;

	private float screenRatio = 1;

	public enum CanvasType{ canvasTypeIPhone3p5Inch ,canvasTypeIPhone4Inch , canvasTypeIPad, canvasTypeAndroid7Inch }

	CanvasType _canvasType;
	boolean _isOrientationLandscape;
	boolean _isPageScrollShortEnd;
	
	int _pageCount;
	int _startPageIndex;
	int _port;
	float _version;
	
	private MainActivity _mainActivity;
	//private View _rootView;

	Map<String, ArrayList<MMPControl>> _allGUIControlMap; //control address, array of objects with that address. Allows multiple items with same address.

	int _bgColor;
	
	public ImageButton _settingsButton; //TODO make private again...is set in mainactivity loadScenePatchOnly
	private View _container;

	public PatchFragment(MainActivity parent) {
		super();
		_mainActivity = parent;
		_allGUIControlMap = new HashMap<String,ArrayList<MMPControl>>();
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
		
		rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
		    	rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		    	//String fileJSON = MainActivity.readMMPToString("Welcome-ip5.mmp");
		    	String fileJSON = "";
		    	try {
		    	if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeIPhone3p5Inch) {
					fileJSON = MainActivity.readMMPAssetToString(getActivity().getAssets().open("Welcome.mmp"));
				} else if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeIPhone4Inch) {
					fileJSON = MainActivity.readMMPAssetToString(getActivity().getAssets().open("Welcome-ip5.mmp"));
				} else if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeIPad) {
					fileJSON = MainActivity.readMMPAssetToString(getActivity().getAssets().open("Welcome-Pad.mmp"));
				} else if (_mainActivity.hardwareScreenType == CanvasType.canvasTypeAndroid7Inch) {
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
	
	/*public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    Log.i(TAG, "frag config!");
	}*/
	@Override
	public void onResume() {
		super.onResume();
		// redraw MMPLCDs
		//for ()
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

	public void loadSceneFromJSON(String inString){
		_mainActivity.stopLocationUpdates();
		scrollRelativeLayout.removeAllViews();
		//unlike ios, can just set orientation without physical rotation
		//
		//
		int screenWidth = _container.getWidth();//_rootView.getWidth();
		int screenHeight = _container.getHeight();//_rootView.getHeight();
		/*if(verbose)*/Log.i(TAG, "load scene _container dim "+screenWidth+" "+screenHeight);
		if(screenWidth==0 || screenHeight == 0) return;//error


		JsonParser parser = new JsonParser();
		JsonObject topDict = parser.parse(inString).getAsJsonObject();//top dict

		// get view-layout-needed parameters
		if(topDict.get("canvasType")!=null){
			if((topDict.get("canvasType").getAsString()).equals("iPhone3p5Inch")) _canvasType=CanvasType.canvasTypeIPhone3p5Inch;// objectForKey:@"canvasType"] isEqualToString:@"iPhone3p5Inch"])[model setCanvasType:canvasTypeIPhone3p5Inch];
			if((topDict.get("canvasType").getAsString()).equals("iPhone4Inch")) _canvasType=CanvasType.canvasTypeIPhone4Inch;
			if((topDict.get("canvasType").getAsString()).equals("iPad")) _canvasType=CanvasType.canvasTypeIPad;
			if((topDict.get("canvasType").getAsString()).equals("android7Inch")) _canvasType=CanvasType.canvasTypeAndroid7Inch;

		}
		//TEMP
		//_canvasType=CanvasType.canvasTypeAndroid7Inch;
		
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
			case canvasTypeIPhone3p5Inch:
				referenceWidth = 320;
				referenceHeight = 480;
				break;
			case canvasTypeIPhone4Inch:
				referenceWidth = 320;
				referenceHeight = 568;
				break;
			case canvasTypeIPad:
				referenceWidth = 768;
				referenceHeight = 1024;
				break;
			case canvasTypeAndroid7Inch://half of 1200 1824 aspect 1.52 
				referenceWidth = 600;
				referenceHeight = 912;
			}
		} else { //landscape
			switch (_canvasType) {
			case canvasTypeIPhone3p5Inch:
				referenceWidth = 480;
				referenceHeight = 320;
				break;
			case canvasTypeIPhone4Inch:
				referenceWidth = 568;
				referenceHeight = 320;
				break;
			case canvasTypeIPad:
				referenceWidth = 1024;
				referenceHeight = 768;
				break;
			case canvasTypeAndroid7Inch://half of 1920 1104
				referenceWidth = 960;
				referenceHeight = 552;
			}	
		}
		double xRatio = (double)screenWidth/referenceWidth;
		double yRatio = (double)screenHeight/referenceHeight;
		screenRatio = (float)Math.min(xRatio, yRatio);
		Log.i(TAG, "ref "+referenceWidth+" "+referenceHeight+" pgc "+_pageCount);

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
		Log.i(TAG, "set scrollrelativelayout dim "+layoutWidth+" "+layoutHeight);
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

		
		//
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
		
		Log.i(TAG, "end of layout loop...");
		//end of big loop through widgets
		// TODO scroll to start page

		// listen for completion of layout before loading patch
		//LinearLayout layout = (LinearLayout)findViewById(R.id.YOUR_VIEW_ID);
		ViewTreeObserver vto = scrollRelativeLayout.getViewTreeObserver(); 
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
			@Override 
			public void onGlobalLayout() { 
				Log.i(TAG, "layout complete...");
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

	}

	// ControlDelegate interface
	@Override
	public void sendGUIMessageArray(List<Object> message) {
		if(verbose) {
			String listString = "";
			for (Object obj : message) listString += obj.toString()+" ";
			if(verbose)Log.i(TAG, "send list to pd fromGUI: " + listString);
		}
		PdBase.sendList("fromGUI", message.toArray());
	}


	// Receive from pd
	private PdReceiver receiver = new PdReceiver() {
		private void pdPost(String msg) {
			Log.i(TAG, "Pure Data says, \"" + msg + "\"");
		}

		@Override
		public void print(String s) {
			//Log.i(TAG,s); 
			ConsoleLogController.getInstance().append(s);
		}

		@Override
		public void receiveBang(String source) {
			Log.i(TAG, "bang");
		}

		@Override
		public void receiveFloat(String source, float x) {
			Log.i(TAG, "float: " + x);
		}

		@Override
		public void receiveList(String source, Object... args) {
			if(verbose)Log.i(TAG, "receive list from pd "+source+": " + Arrays.toString(args));
			if (source.equals("toNetwork")) {
				_mainActivity.networkController.handlePDMessage(args);
			} else if (source.equals("toGUI")) {
				if (args.length==0) return;

				ArrayList<MMPControl> addressArray = _allGUIControlMap.get(args[0]);
				if(addressArray!=null) {//null if there is no objects by that address!
					for (MMPControl control : addressArray) {
						List<Object> newList = Arrays.asList(Arrays.copyOfRange(args, 1,args.length));
						control.receiveList(newList); 
					}
				}
			} else if (source.equals("toSystem")) {
				/*for (Object o : args) {
						if (o instanceof String)
							Log.i(TAG, "obj is string");
						if (o instanceof Float)
							Log.i(TAG, "obj is float");
						if (o instanceof Integer)
							Log.i(TAG, "obj is int");
					}*/
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
					if(pageIndex<=0){
						scrollContainer.smoothScrollTo(0, 0);  //setScrollX(0);
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
			Log.i(TAG, "message: " + Arrays.toString(args));
		}

		@Override
		public void receiveSymbol(String source, String symbol) {
			pdPost("symbol: " + symbol);
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

