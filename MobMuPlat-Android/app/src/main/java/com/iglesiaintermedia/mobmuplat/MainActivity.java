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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
//import org.puredata.core.PdReceiver;

// wear
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageApi.MessageListener;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.common.ConnectionResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.iglesiaintermedia.mobmuplat.PatchFragment.CanvasType;
import com.iglesiaintermedia.mobmuplat.controls.*;
import com.iglesiaintermedia.mobmuplat.nativepdgui.MMPPdGuiUtils;
import com.illposed.osc.OSCMessage;
import com.example.inputmanagercompat.InputManagerCompat;
import com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener;

import cx.mccormick.pddroidparty.PdParser;

public class MainActivity extends FragmentActivity implements LocationListener, SensorEventListener, AudioDelegate, InputDeviceListener, OnBackStackChangedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, PreviewSurface.Callback {
	private static final String TAG = "MobMuPlat MainActivity";
	public static final boolean VERBOSE = false;
	//
	private FrameLayout _topLayout;
	public CanvasType hardwareScreenType;
	// Bookmark it between config change
	private Object _fileDataToLoad; //mmp patches = Json string, pd patches = List<String[]>atomLines
	private String _fileDataToLoadParentPathString;
    private boolean _fileDataToLoadIsPatchOnly;

	//Pd
	private PdService pdService = null;
	private int openPdFileHandle; //aka dollarZero.

	//HID
	private InputManagerCompat _inputManager;
	private SparseArray<InputDeviceState> _inputDeviceStates;

	//
	//public final static float VERSION = 1.6f; //necc?

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

	PreviewSurface mSurface;

	private BroadcastReceiver _bc;
	private boolean _backgroundAudioAndNetworkEnabled;

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
	
	// wear
  WorkerThread wt;
  private GoogleApiClient mGoogleApiClient;
	private static final long CONNECTION_TIME_OUT_MS = 100;
	private String nodeId;

	// new permissions pattern
	String[] STARTUP_PERMISSIONS = {
			Manifest.permission.RECORD_AUDIO
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		//getActionBar().setDisplayShowTitleEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
		getActionBar().hide(); //necc on L, not necc on kitkat

		//layout view 
		setContentView(R.layout.activity_main);
		mSurface = (PreviewSurface) findViewById(R.id.surface);
		mSurface.setCallback(this);

		processIntent();

		_inputManager = InputManagerCompat.Factory.getInputManager(this);
		_inputManager.registerInputDeviceListener(this, null);
		_inputDeviceStates = new SparseArray<InputDeviceState>();

		//device type (just for syncing docs)

		hardwareScreenType = CanvasType.canvasTypeWidePhone; //default
		CharSequence deviceFromValues = getResources().getText(R.string.screen_type);
		
		//derive screen ratio
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		float aspect = (float)height/width;
		
		if (deviceFromValues.equals("phone")) {
			if (aspect > 1.6375) { // 
				hardwareScreenType = CanvasType.canvasTypeTallPhone; 
			} else {
				hardwareScreenType = CanvasType.canvasTypeWidePhone; 
			}
		} else if (deviceFromValues.equals("7inch") || 
				   deviceFromValues.equals("10inch")) {
			if (aspect > 1.42) { // 
				hardwareScreenType = CanvasType.canvasTypeTallTablet; 
			} else {
				hardwareScreenType = CanvasType.canvasTypeWideTablet; 
			}
		} 


		//
		//AudioParameters.init(this);
		PdPreferences.initPreferences(getApplicationContext());
		initSensors();
//		initLocation();
		usbMidiController = new UsbMidiController(this); //matched close in onDestroy...move closer in?

		//allow multicast
		WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if(wifi != null){
			WifiManager.MulticastLock lock = wifi.createMulticastLock("MulticastLockTag");
			lock.acquire();
		}  //Automatically released on app exit/crash.

		networkController = new NetworkController(this);
		networkController.delegate = this;



    //wear
    mGoogleApiClient = new GoogleApiClient.Builder(this)
     	          .addApi(Wearable.API)
        .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

		//fragments
		_patchFragment = new PatchFragment();
		_documentsFragment = new DocumentsFragment();
		_networkFragment = new NetworkFragment();
		_consoleFragment = new ConsoleFragment();
		_hidFragment = new HIDFragment();
		_audioMidiFragment = new AudioMidiFragment();
		//_audioMidiFragment.audioDelegate = this;
		// bookmark for launch from info button
		_lastFrag = _documentsFragment;
		_lastFragTitle = "Documents";


		_topLayout = (FrameLayout)findViewById(R.id.container);

		// Go full screen and lay out to top of screen.
		// Only on SDK >= 16. This means that patch will not go truly fullscreen on ICS. Separate layouts for 14+ vs 16+ for the fragment margins.
		_topLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

		// axes for table
		if (getDeviceNaturalOrientation() == Configuration.ORIENTATION_LANDSCAPE) _shouldSwapAxes = true;

		// set action bar title on fragment stack changes
		getSupportFragmentManager().addOnBackStackChangedListener(this);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		for (String permission : STARTUP_PERMISSIONS) {
			if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this,STARTUP_PERMISSIONS, MMP_PERMISSIONS_REQUEST_AUDIO);
				return; // something missing, so ask for permissions
			}
		}
		// REached here if didn't break oout of permisisons loop
		postStartupPermissions();
		if (savedInstanceState == null) {
			launchSplash();
		}
	}

	private void postStartupPermissions() {
		// copy docs
		//version
		boolean shouldCopyDocs = false;
		boolean shouldShowVersion26UpgradeNotice = false; // Whether to show alert about new file system changes.
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
			// Notice shown if user is upgrading from < 26 to >= 26 (and hasn't already seen the
			// upgrade notice). Not shown on fresh installs.
			if (lastOpenedVersionCode > 0 && lastOpenedVersionCode < 26 && versionCode >=26) {
				boolean sawNotice = sp.getBoolean("sawVersion26UpgradeNotice", false);
				if (!sawNotice) {
					shouldShowVersion26UpgradeNotice = true;
				}
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		//temp
		//shouldCopyDocs = true;
		//shouldShowVersion26UpgradeNotice = true;

		//copy
		if(shouldCopyDocs) {
			List<String> defaultPatchesList;
			if(hardwareScreenType == CanvasType.canvasTypeWidePhone || hardwareScreenType == CanvasType.canvasTypeTallTablet){
				defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine.mmp", "MMPTutorial1-GUI.mmp", "MMPTutorial2-Input.mmp", "MMPTutorial3-Hardware.mmp", "MMPTutorial4-Networking.mmp","MMPTutorial5-Files.mmp","MMPExamples-Vocoder.mmp", "MMPExamples-Motion.mmp", "MMPExamples-Sequencer.mmp", "MMPExamples-GPS.mmp", "MMPTutorial6-2DGraphics.mmp", "MMPExamples-LANdini.mmp", "MMPExamples-Arp.mmp", "MMPExamples-TableGlitch.mmp", "MMPExamples-HID.mmp", "MMPExamples-PingAndConnect.mmp", "MMPExamples-Watch.mmp");
			}
			else if (hardwareScreenType==CanvasType.canvasTypeTallPhone){
				defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine-ip5.mmp", "MMPTutorial1-GUI-ip5.mmp", "MMPTutorial2-Input-ip5.mmp", "MMPTutorial3-Hardware-ip5.mmp", "MMPTutorial4-Networking-ip5.mmp","MMPTutorial5-Files-ip5.mmp", "MMPExamples-Vocoder-ip5.mmp", "MMPExamples-Motion-ip5.mmp", "MMPExamples-Sequencer-ip5.mmp","MMPExamples-GPS-ip5.mmp", "MMPTutorial6-2DGraphics-ip5.mmp", "MMPExamples-LANdini-ip5.mmp", "MMPExamples-Arp-ip5.mmp",  "MMPExamples-TableGlitch-ip5.mmp", "MMPExamples-HID-ip5.mmp", "MMPExamples-PingAndConnect.mmp", "MMPExamples-Watch-ip5.mmp");
			}
			else{//wide tablet/pad
				defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine-Pad.mmp", "MMPTutorial1-GUI-Pad.mmp", "MMPTutorial2-Input-Pad.mmp", "MMPTutorial3-Hardware-Pad.mmp", "MMPTutorial4-Networking-Pad.mmp","MMPTutorial5-Files-Pad.mmp", "MMPExamples-Vocoder-Pad.mmp", "MMPExamples-Motion-Pad.mmp", "MMPExamples-Sequencer-Pad.mmp","MMPExamples-GPS-Pad.mmp", "MMPTutorial6-2DGraphics-Pad.mmp", "MMPExamples-LANdini-Pad.mmp", "MMPExamples-Arp-Pad.mmp",  "MMPExamples-TableGlitch-Pad.mmp", "MMPExamples-HID-Pad.mmp", "MMPExamples-PingAndConnect.mmp", "MMPExamples-Watch-Pad.mmp");
			}

			List<String> commonFilesList = Arrays.asList("MMPTutorial0-HelloSine.pd","MMPTutorial1-GUI.pd", "MMPTutorial2-Input.pd", "MMPTutorial3-Hardware.pd", "MMPTutorial4-Networking.pd","MMPTutorial5-Files.pd","cats1.jpg", "cats2.jpg","cats3.jpg","clap.wav","Welcome.pd",  "MMPExamples-Vocoder.pd", "vocod_channel.pd", "MMPExamples-Motion.pd", "MMPExamples-Sequencer.pd", "MMPExamples-GPS.pd", "MMPTutorial6-2DGraphics.pd", "MMPExamples-LANdini.pd", "MMPExamples-Arp.pd", "MMPExamples-TableGlitch.pd", "anderson1.wav", "MMPExamples-HID.pd", "MMPExamples-InterAppOSC.mmp", "MMPExamples-InterAppOSC.pd", "MMPExamples-PingAndConnect.pd", "MMPExamples-NativeGUI.pd", "MMPExamples-Watch.pd");

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
				if (shouldShowVersion26UpgradeNotice) {
					editor.putBoolean("sawVersion26UpgradeNotice", true);
				}
				editor.commit();
			}
		}

		// show upgrade notice
		if (shouldShowVersion26UpgradeNotice) {
			showAlert("Thanks for using MobMuPlat!\n\nDue to Android requirements, the app now handles files differently. \nIt no longer uses a shared external folder; you may delete the external \"MobMuPlat\" folder, as the app no longer has access to it. \nSee the 'Info' button in the Documents page for details on file import.");
		}

		// pd service
		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
		//wifi
		_bc = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) { //having trouble with this, reports "0x", doesn't repond to supplicant stuff
				networkController.newSSIDData();
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		//intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) ;

		registerReceiver(_bc, intentFilter);

	}

	//'dangerous' permissions that must be requested: audio, camera, location
	private static final int MMP_PERMISSIONS_REQUEST_AUDIO = 1; // Startup permissions.
	private static final int MMP_PERMISSIONS_REQUEST_LOCATION = 2;
	private static final int MMP_PERMISSIONS_REQUEST_CAMERA = 3;

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MMP_PERMISSIONS_REQUEST_AUDIO: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length == 3
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED
						&& grantResults[1] == PackageManager.PERMISSION_GRANTED
						&& grantResults[2] == PackageManager.PERMISSION_GRANTED) {
					postStartupPermissions();
					launchSplash();
				} else {
					// permission denied
					showAlert("MobMuPlat needs disk and audio access to work.", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							ActivityCompat.requestPermissions(MainActivity.this,STARTUP_PERMISSIONS, MMP_PERMISSIONS_REQUEST_AUDIO);
						}
					});
				}
				return;
			}
			case MMP_PERMISSIONS_REQUEST_LOCATION: {
				if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					startLocationUpdates();
				}
			}
			case MMP_PERMISSIONS_REQUEST_CAMERA: {
				if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					mSurface.kick();
					enableLight(true);
				}
			}
		}
	}

    // wear
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
              NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                    //Log.i("WEAR", "gotNODE");
                }
            }
        }).start();
    } 
    
    class WorkerThread extends Thread {
        public Handler mHandler;
        public void run() {	 
            Looper.prepare();
           mHandler = new Handler(){
               @Override
               public void handleMessage(Message msg) {
                   Bundle u = msg.getData();
                   //Log.i(TAG, "received a msg to worker thread: " + u.getString("path"));
                   String path = u.getString("path");
                   String message = u.getString("message");
                   if (path == null || message == null) return;;
                   //
                   NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                   for (Node node : nodes.getNodes()) {
                       MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                       if (result.getStatus().isSuccess()) {
                           //Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                       } else {
                           // Log an error
                           //Log.v("myTag", "ERROR: failed to send Message");
                       }
                   }
               }
           };
            Looper.loop();
        }
    }

    public void sendWearMessage(String inPath, String message) {
        //new SendToDataLayerThread(inPath, message).start();
        Handler workerHandler = wt.mHandler;
        // obtain a msg object from global msg pool
        Message m = workerHandler.obtainMessage();
        Bundle b = m.getData();
        b.putString("path", inPath);
        b.putString("message", message);
        workerHandler.sendMessage(m);
    }

    /*
    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message!=null?message.getBytes():null).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }
    }*/
    //===end wear
    
	@Override
	public void onBackStackChanged() {
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
	protected void onNewIntent(Intent intent) { //called by open with file
		super.onNewIntent(intent);
		setIntent(intent);
		processIntent();
	}

	private void processIntent() {
		Intent i = getIntent();
		if(i!=null) {
			String action = i.getAction();

			// opening a file...action = action view.
			// from Drive, on android L, scheme = CONTENT, has MIME type
			// Intent { act=android.intent.action.VIEW dat=content://com.google.android.apps.docs.storage.legacy/enc=1TXBVPSUVcZyy_e6ARqL7qR2ylm6FqHkA1YENDi1tQwUeL-D typ=application/zip flg=0x10400000 cmp=com.iglesiaintermedia.mobmuplat/.MainActivity (has extras) }
			// from dropbox, on android L, scheme = FILE, has MIME type
			// Intent { act=android.intent.action.VIEW dat=file:///storage/emulated/0/Android/data/com.dropbox.android/files/u24124447/scratch/MobMuPlat/RealityDenied.zip typ=application/zip flg=0x10400003 cmp=com.iglesiaintermedia.mobmuplat/.MainActivity (has extras) }

				// from Drive, on android N, scheme = CONTENT, has MIME type
			// Intent { act=android.intent.action.VIEW dat=content://com.google.android.apps.docs.storage.legacy/enc=MLhhWe_CsSIACYqGLUuuXDkv8WFfFyAQ2--0kCmxAK_8RvxW typ=application/zip flg=0x10000000 cmp=com.iglesiaintermedia.mobmuplat/.MainActivity (has extras) }
			// from dropbox, on andoird N, scheme = content, no MIME type
			// Intent { act=android.intent.action.VIEW dat=content://com.dropbox.android.FileCache/filecache/d8b4e518-54ec-4a26-97a7-11adb2e39f90 flg=0x10000003 cmp=com.iglesiaintermedia.mobmuplat/.MainActivity (has extras) }

			if (action.equals(Intent.ACTION_VIEW)){
				Uri dataUri = i.getData();
				if(dataUri!=null){
					if(VERBOSE)Log.i(TAG, "receive intent data " + dataUri.toString());
					String scheme = i.getScheme(); // e.g. "content" or "file"
					if (scheme == null) {
						showAlert("Cannot access attachment.");
						return;
					}
					if (scheme.equals("content")) { //received via email attachment; more stuff uses this now in N
						try {
							InputStream attachment = getContentResolver().openInputStream(dataUri);
							if (attachment == null) {
								if(VERBOSE)Log.e("onCreate", "cannot access mail attachment");
								showAlert("Cannot access mail attachment.");
							} else {
								String attachmentFileName = null;
								//try to get file name
								Cursor c = getContentResolver().query(dataUri, null, null, null, null);
								c.moveToFirst();
								final int fileNameColumnId = c.getColumnIndex(
										MediaStore.MediaColumns.DISPLAY_NAME);
								if (fileNameColumnId >= 0) {
									attachmentFileName = c.getString(fileNameColumnId);
									String type = i.getType(); //mime type - can be null (e.g. from dropbox on Android N)
									String suffix = attachmentFileName.substring(attachmentFileName.lastIndexOf(".") + 1);
									if (type!=null && type.equals("application/zip")) {
										unpackZipInputStream(attachment, attachmentFileName);
									} else if (type==null && suffix!=null && suffix.equals("zip")) { // try again if no type and file ends in .zip
										unpackZipInputStream(attachment, attachmentFileName);
									} else {
										copyInputStream(attachment, attachmentFileName, true);
									}
								} else {
									showAlert("Cannot get filename for attachment.");
								}
							}
						}catch(Exception e) {
							showAlert("Cannot access attachment.");
						}
					} else {//scheme "file"
						String filename = dataUri.getLastPathSegment();
						String fullPath = dataUri.getPath();
						int lastSlashPos = fullPath.lastIndexOf('/');

						String parentPath = fullPath.substring(0, lastSlashPos+1);
						String suffix = filename.substring(filename.lastIndexOf('.'));
						if (suffix.equals(".zip")) { 
							unpackZip(parentPath, filename);
						} else {
							copyUri(dataUri);
						}
					}
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSurface.lightOff();
	}

	@Override
	protected void onStop() {
		if(!_backgroundAudioAndNetworkEnabled) {
			stopAudio();
      		networkController.stop();
		}
    	if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
        	mGoogleApiClient.disconnect();
    	}
    	super.onStop();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
    	networkController.maybeRestart();
		if(pdService!=null && !pdService.isRunning()) {
			pdService.startAudio();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
        //==== wear

		mGoogleApiClient.connect();
		retrieveDeviceNode(); //TODO on connection?
        // message send thread
        wt = new WorkerThread();
        wt.start();

        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {

                String path = messageEvent.getPath();
                //TODO system calls,: 1) get load completion from wear to send buffered messages, 2) page swipe?
                //try {
                    String dataString = new String(messageEvent.getData());//, "UTF-8");
                    String[] messageStringArray = dataString.split(" ");
                    List<Object> objList = new ArrayList<Object>();
                    objList.add(path); //add address first, then rest of list
                    for (String token : messageStringArray) {
                        try {
                            Float f = Float.valueOf(token);
                            objList.add(f);
                        } catch (NumberFormatException e) {
                            // not a number, add as string.
                            objList.add(token);
                        }
                    }
                    PdBase.sendList("fromGUI", objList.toArray());
				/*} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}*/
			}
		});
        // end wear
	}

	public void loadScene(String filenameToLoad) { //filename is relative to doc dir.
		Log.i(TAG, "filename to load:"+filenameToLoad);
		getActionBar().hide();

		String parentPathString =  new File(MainActivity.getDocumentsFolderPath(this), filenameToLoad).getParentFile().getAbsolutePath();
		Log.i(TAG, "parentPathString to load:"+parentPathString);
		String fileJSON = readMMPToString(filenameToLoad, this);
		if (fileJSON == null) { //could not load/find filename...i.e. filename is stale
			showAlert("Cannot load "+filenameToLoad+", please tap the \"show files\" button twice to refresh the file list");
		}

		boolean requestedChange = setOrientationOfMMP(fileJSON);

        _fileDataToLoad = fileJSON;
        _fileDataToLoadParentPathString = parentPathString;
        _fileDataToLoadIsPatchOnly = false;

		if(!requestedChange){
			_patchFragment.loadSceneFromJSON(fileJSON, parentPathString);
			_fileDataToLoad = null;
			_fileDataToLoadParentPathString = null;
		}
		//otherwise is being loaded in onConfigChange
	}

	// filename is relative to documents dir.
	public void loadScenePatchOnly(String filenameToLoad) {
		//don't bother with rotation for now...
		stopLocationUpdates(); //move? handled in common reset?
		
		getActionBar().hide();

        String path = MainActivity.getDocumentsFolderPath(this) + File.separator + filenameToLoad;
        ArrayList<String[]> originalAtomLines = PdParser.parsePatch(path);

        _fileDataToLoad = originalAtomLines;
        _fileDataToLoadIsPatchOnly = true;

        boolean requestedChange = setOrientationOfMMPPatchOnly(originalAtomLines);

		if(!requestedChange){
			_patchFragment.loadScenePatchOnly(originalAtomLines);
			_fileDataToLoad = null;
		}
        //otherwise is being loaded in onConfigChange

		//TODO RESET ports?
		//_patchFragment.loadScenePatchOnly(filenameToLoad); // calls loadPdFile
	}

	public int loadPdFile(String pdFilename, String parentPathString) {
		// load pd patch
		if(openPdFileHandle != 0) {
			PdBase.closePatch(openPdFileHandle); 
			openPdFileHandle = 0;
		}
		//open
		// File patchFile = null;
		if(pdFilename!=null) {
			try {
				File pdFile = new File(parentPathString != null ? parentPathString : MainActivity.getDocumentsFolderPath(this), pdFilename);
				openPdFileHandle = PdBase.openPatch(pdFile);
			} catch (FileNotFoundException e) {
				showAlert("PD file "+pdFilename+" not found.");
				openPdFileHandle = 0;
			} catch (IOException e) {
				showAlert("I/O error on loading PD file "+pdFilename+".");
				openPdFileHandle = 0;
			}
		}
		return openPdFileHandle;
	}

	static public String readMMPToString(String filename, Context context) {
		if (filename == null) return null;
		File file = new File(MainActivity.getDocumentsFolderPath(context),filename);
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

//	private void initLocation() {
//		locationManagerA = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//		//locationManagerA.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
//		locationManagerB = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//		//locationManagerB.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
//	}

	public void startLocationUpdates() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MMP_PERMISSIONS_REQUEST_LOCATION);
			return;
		}
		if (locationManagerA == null) locationManagerA = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if (locationManagerB == null) locationManagerB = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		Location lastKnownLocation = null;
		//if (locationManagerA!=null){
			locationManagerA.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
			// send initial val from this
			lastKnownLocation = locationManagerA.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (lastKnownLocation != null)onLocationChanged(lastKnownLocation);
		//}
		//if (locationManagerB!=null){
			locationManagerB.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, this);
			//if initial didn't work, try here
			if (lastKnownLocation==null) {
				lastKnownLocation = locationManagerB.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (lastKnownLocation != null)onLocationChanged(lastKnownLocation);
			}
		//}
		
	}

	public void stopLocationUpdates() {
		if (locationManagerA!=null)locationManagerA.removeUpdates(this);
		if (locationManagerB!=null)locationManagerB.removeUpdates(this);
	}

	// flashslight
	public void enableLight(boolean isOn) {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MMP_PERMISSIONS_REQUEST_CAMERA);
			return;
		}

		if (isOn) {
			mSurface.lightOn();
		} else {
			mSurface.lightOff();
		}
	}

	// PreviewSurface callbacks
	public void cameraReady(){};
	public void cameraNotAvailable(){};


	private void initSensors() { //TODO allow sensors on default thread for low-power devices (or just shutoff)
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
		int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
		//
		// onSensorChanged is now called on a background thread. NOPE! leads to arcane "NewStringUTF" crash? no.
		HandlerThread handlerThread = new HandlerThread("sensorThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
		handlerThread.start();
		Handler handler = new Handler(handlerThread.getLooper());

		
		Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accel,sensorDelay, handler);
		Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorManager.registerListener(this,  gyro, sensorDelay, handler);
		Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorManager.registerListener(this,  rotation, sensorDelay, handler);
		Sensor compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sensorManager.registerListener(this,  compass, sensorDelay, handler);

		//

		/*Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this,  accel, sensorDelay);//TODO rate
		Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorManager.registerListener(this,  gyro, sensorDelay);//TODO rate
		Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorManager.registerListener(this,  rotation, sensorDelay);
		Sensor compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sensorManager.registerListener(this,  compass, sensorDelay);//TODO rate*/
		 
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

    private boolean setOrientationOfMMP(String jsonString) { //returns whether there was a change
        JsonParser parser = new JsonParser();
        try {
            JsonObject topDict = parser.parse(jsonString).getAsJsonObject();//top dict
            int mmpOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			if(topDict.get("isOrientationLandscape")!=null) {
				boolean isOrientationLandscape = topDict.get("isOrientationLandscape").getAsBoolean();
				if (isOrientationLandscape)mmpOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			}

            return setOrientation(mmpOrientation);

        } catch(JsonParseException e) {
            showAlert("Unable to parse interface file.");
		}
		return false;
    }

    private boolean setOrientationOfMMPPatchOnly(List<String[]> originalAtomLines) {
        // DEI check for zero/bad values
        float docCanvasSizeWidth = Float.parseFloat(originalAtomLines.get(0)[4]);
        float docCanvasSizeHeight = Float.parseFloat(originalAtomLines.get(0)[5]);

        boolean isOrientationLandscape = (docCanvasSizeWidth > docCanvasSizeHeight);
        return setOrientation(isOrientationLandscape ?
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

	private boolean setOrientation(int mmpOrientation){  //returns whether there was a change

        int screenOrientation = this.getWindow().getWindowManager().getDefaultDisplay().getRotation();// on tablet rotation_0 = "natural"= landscape

        int naturalOrientation = getDeviceNaturalOrientation();
        if (naturalOrientation == Configuration.ORIENTATION_PORTRAIT) { //phones, 7" tablets
            if ((mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && screenOrientation!=Surface.ROTATION_0) ||
                    (mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && screenOrientation!=Surface.ROTATION_90)) {
                if(VERBOSE)Log.i(TAG, "requesting orientation...surface = "+screenOrientation);
                this.setRequestedOrientation(mmpOrientation);
                return true;
            }
        }
        //"natural" = landscape = big tablet
        else if (naturalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && screenOrientation!=Surface.ROTATION_270) || //weird that it thinks that rotation 270 is portrait
                    (mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && screenOrientation!=Surface.ROTATION_0)) {
                if(VERBOSE)Log.i(TAG, "requesting orientation...surface = "+screenOrientation);
                this.setRequestedOrientation(mmpOrientation);
                return true;
            }
        }
        return false;
	}

	private void copyInputStream(InputStream in, String filename, boolean showAlert) {
		File file = new File(MainActivity.getDocumentsFolderPath(this), filename);
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] data = new byte[in.available()];
			in.read(data);
			out.write(data);
			in.close();
			out.close();

			if(showAlert)showAlert("File "+filename+" copied to MobMuPlat Documents");
		} catch (IOException e) {
      		Log.i(TAG, "Unable to copy file: "+e.getMessage());
			if(showAlert)showAlert("File "+filename+" copied to MobMuPlat Documents");
		}
	}

	public void copyUri(Uri uri){
		String sourcePath = uri.getPath();
		String sourceFilename = uri.getLastPathSegment();
		try {
			InputStream in = new FileInputStream(sourcePath);
			copyInputStream(in, sourceFilename, true);
		} catch (FileNotFoundException e) {
			Log.i(TAG, "Unable to copy file: "+e.getMessage());
		}
	}

	public void copyAsset(String assetFilename){
		AssetManager assetManager = getAssets();

		try {
			InputStream in = assetManager.open(assetFilename);
			copyInputStream(in, assetFilename, false);
			//Log.i(TAG, "Copied file: "+assetFilename);
		} catch (IOException e) {
			Log.i(TAG, "Unable to copy file: "+e.getMessage());
		}
	}

	//TODO consolidate this with version in fragment
	private void showAlert(String string, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(string);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", listener);
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void showAlert(String string) {
		showAlert(string, null);
	}

	// on orientation changed
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ViewTreeObserver observer = _topLayout.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                _topLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                if (_fileDataToLoad != null) {
                    if (_fileDataToLoadIsPatchOnly) {
                        _patchFragment.loadScenePatchOnly((List<String[]>)_fileDataToLoad);
                    } else {
                        _patchFragment.loadSceneFromJSON((String) _fileDataToLoad, _fileDataToLoadParentPathString);
                    }
                    _fileDataToLoad = null;
                    _fileDataToLoadParentPathString = null;

                    //DEI need to load tables?
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
			//Simulator breaks on audio input, so here's a little flag
			boolean amOnSimulator = false;
			pdService.initAudio(44100, amOnSimulator ? 0 : -1, -1, -1);   // negative values will be replaced with defaults/preferences
			pdService.startAudio();
		} catch (IOException e) {
			Log.e(TAG, "Audio init error: " + e.toString());
		}
	}
	
	public boolean setSampleRate(int rate) {
		pdService.stopAudio();
		try {
			pdService.initAudio(rate, -1, -1, -1);   // negative values will be replaced with defaults/preferences
			//pdService.startAudio();
		} catch (IOException e) {
			Log.e(TAG, "Audio init error: "+e.toString());
		}
		pdService.startAudio();
		return true;
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
	}

	protected void onDestroy() {
		usbMidiController.close(); //solves "Intent Receiver Leaked: ... are you missing a call to unregisterReceiver()?"
		unregisterReceiver(_bc);
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
                if (this != null && !isFinishing()) {
                    fragmentManager.beginTransaction().remove(sf).commitAllowingStateLoss(); //When just commit(), would get java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
                    getSupportFragmentManager().beginTransaction().add(R.id.container, _patchFragment).commitAllowingStateLoss();
				}
			}
		}, 4000);
	}

	public void launchFragment(Fragment frag, String title) {
		FragmentManager fragmentManager = getSupportFragmentManager();

		// if already what we are looking at, return (otherwise it pops but isn't re-added)
		if (fragmentManager.getBackStackEntryCount()==1 && fragmentManager.getBackStackEntryAt(0).getName() == title) {
			return;
		}
		// if something else is on the stack above patch, pop it, but remove listener to not remove/readd action bar.
		if (fragmentManager.getBackStackEntryCount() > 0) {
			fragmentManager.removeOnBackStackChangedListener(this);
			fragmentManager.popBackStackImmediate();
			fragmentManager.addOnBackStackChangedListener(this);
		}

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(R.id.container, frag);
		fragmentTransaction.addToBackStack(title);
		fragmentTransaction.commit(); 
		fragmentManager.executePendingTransactions(); //Do it immediately, not async.
	}

	public void launchSettings() { //launch the last settings frag we were looking at.
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

	public static String getDocumentsFolderPath(Context context) {
		// In-app file system; no more external directory.
		File fileDir = context.getFilesDir();
		fileDir.mkdir(); // make mobmuplat dir if not there
		return fileDir.getAbsolutePath();
	}

	// OSC
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
		//Log.i(TAG, "loc "+location.getLatitude()+" "+location.getLongitude()+" "+location.getAltitude());

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
	public void setBackgroundAudioAndNetworkEnabled(boolean backgroundAudioAndNetworkEnabled) {
		_backgroundAudioAndNetworkEnabled = backgroundAudioAndNetworkEnabled;
	}
	
	private class UnzipTask extends AsyncTask<Void, Void, Boolean> {
		InputStream _is;
		String _zipname;
		Context _context;
		public UnzipTask(InputStream is, String zipname, Context context) {
			_is = is;
			_zipname = zipname;
			_context = context;
		}
		@Override
		protected Boolean doInBackground(Void... args) {
			String documentsPath = MainActivity.getDocumentsFolderPath(_context);
			ZipInputStream zis;

				String filename;

				zis = new ZipInputStream(new BufferedInputStream(_is));          
				ZipEntry ze;
				byte[] buffer = new byte[1024];
				int count;
				try {
				while ((ze = zis.getNextEntry()) != null) {

					filename = ze.getName();
					Log.i("ZIP", "opening "+filename);

					// Need to create directories if doesn't exist.
					if (ze.isDirectory()) {
						File fmd = new File(documentsPath, filename);
						fmd.mkdirs();
						continue;
					}

					File outFile = new File(documentsPath, filename);
					if(VERBOSE)Log.i(TAG, "zip writes to: "+outFile.getAbsolutePath());
					try {
						FileOutputStream fout = new FileOutputStream(outFile);

						while ((count = zis.read(buffer)) != -1) {
							fout.write(buffer, 0, count);
						}

						fout.close();
						if(VERBOSE)Log.i(TAG, "zip wrote "+filename);
					} catch (FileNotFoundException e) {
						// continue if cannot find file. This happens with OSX-made zips, for the hidden files, which can be ignored.
						if(VERBOSE)Log.i(TAG, "zip could not write "+filename);
					}
					zis.closeEntry();
				}

				zis.close();
				return true;
				
			} catch (IOException e) {
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success.booleanValue()==true) {
				showAlert("Unzipped contents of "+_zipname+" into Documents folder.");
				_documentsFragment.refreshFileList();
			} else {
				showAlert("Error unzipping contents of "+_zipname);
			}
		}
	}

	private void unpackZipInputStream(InputStream is, String zipname) {
		Toast.makeText(this, "Unzipping "+zipname+" to Documents", Toast.LENGTH_LONG).show();;
		new UnzipTask(is,zipname, this).execute();
	}

	private void unpackZip(String path, String zipname) {    
		//Log.i("ZIP", "unzipping "+path+" "+zipname);
		try {
			InputStream is = new FileInputStream(path + zipname);
			unpackZipInputStream(is, zipname);
		} catch(Exception e) {
			e.printStackTrace();
			showAlert("Error unzipping contents of "+zipname);
		} 
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

	public void refreshMenuFragment(MMPMenu menu) { //TODO move
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

			if(VERBOSE)Log.i(TAG, device.toString());
		}
		return state;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getSource()==InputDevice.SOURCE_KEYBOARD) {
			Object[] args = new Object[]{"/key", Integer.valueOf(keyCode)};
			PdBase.sendList("fromSystem", args);
			//don't consume - hardware hits show up here.
		} else { //HID
			InputDeviceState state = getInputDeviceState(event);
			if (state != null && state.onKeyDown(event)) { //pd message sent in state.onKeyUp/Down
				if (_hidFragment!=null && _hidFragment.isVisible()) {
					_hidFragment.show(state);
        }
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.getSource()==InputDevice.SOURCE_KEYBOARD) {
			Object[] args = new Object[]{"/keyUp", Integer.valueOf(keyCode)};
			PdBase.sendList("fromSystem", args);
			//don't consume - hardware hits show up here.
		} else { //HID
			InputDeviceState state = getInputDeviceState(event);
			if (state != null && state.onKeyUp(event)) { //pd message sent in state.onKeyUp/Down
				if (_hidFragment!=null && _hidFragment.isVisible()) {
					_hidFragment.show(state);
        }
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		_inputManager.onGenericMotionEvent(event);

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
				if (_hidFragment!=null && _hidFragment.isVisible()) {
					_hidFragment.show(state);
				}
				return true;
			}
		}
		return super.onGenericMotionEvent(event);
	}

    //wear connection
    // Send a message when the wear data layer connection is successful. Called on app start and foreground
    @Override
    public void onConnected(Bundle connectionHint) {
		// Now, activity is started via loadGUI call.
        // Call startActivity, on foreground, to restart wear app on _current_ loaded GUI
        if (_patchFragment!=null && _patchFragment.loadedWearString!=null) {
            sendWearMessage("/loadGUI", _patchFragment.loadedWearString);
        }
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

	//
	private static final int IMPORT_FILES_REQUEST_CODE = 1234;
	public void requestImportFiles() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		intent.setType("*/*");
//		String[] mimeTypes = new String[]{"application/x-binary,application/octet-stream"};
//		if (mimeTypes.length > 0) {
//			intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
//		}

   		startActivityForResult(Intent.createChooser(intent, "foo"), IMPORT_FILES_REQUEST_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,Intent resultData) {
		if (requestCode == IMPORT_FILES_REQUEST_CODE && resultCode ==Activity.RESULT_OK) {
			if (resultData == null) return;
			// multi selection
			if(resultData.getClipData() != null) {
				int count = resultData.getClipData().getItemCount();
				for (int i=0;i<count;i++) {
					Uri uri = resultData.getClipData().getItemAt(i).getUri();
					importFileFromContentUri(uri, false);
				}
				showAlert("Imported "+count+" files");
			} else if (resultData.getData() != null) {
				Uri uri = resultData.getData();
				importFileFromContentUri(uri, true);
			}
		}
	}

	private void importFileFromContentUri(Uri uri, boolean showAlert) {
		if (uri == null || !uri.getScheme().equals("content")) {
			showAlert("Could not copy to MobMuPlat Documents");
			return;
		}

		ContentResolver contentResolver = getContentResolver();
		Cursor cursor = contentResolver.query(uri, null, null, null, null);
		if (cursor == null || !cursor.moveToFirst()) return;
		String	filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
		String mimeType = contentResolver.getType(uri);
		try {
			InputStream is = contentResolver.openInputStream(uri);
			if (mimeType.equals("application/zip")) {
				if(showAlert)Toast.makeText(this, "Unzipping " + filename + " to Documents", Toast.LENGTH_LONG).show();
				new UnzipTask(is, filename, this).execute();
			} else {
				copyInputStream(is, filename, showAlert);
				_documentsFragment.refreshFileList();
			}
		} catch (FileNotFoundException e) {
			showAlert("Could not copy " + filename + " to MobMuPlat Documents");
		}
	}
}

