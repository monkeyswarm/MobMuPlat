package com.iglesiaintermedia.mobmuplat;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class FlashlightController implements SurfaceHolder.Callback{
	private String TAG = "Flashlight";
	private Camera mCamera;
	  private boolean lightOn;
	  private boolean previewOn;
	  
	  private SurfaceView surfaceView;
	  private SurfaceHolder surfaceHolder;

	
	  public FlashlightController(SurfaceView sv) { //NW
		  	surfaceView = sv;//(SurfaceView) this.findViewById(R.id.surfaceview);
		    surfaceHolder = surfaceView.getHolder();
		    surfaceHolder.addCallback(this);
		    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		  
	  }
	  
	  public void startCamera() {
		  if(mCamera == null) {
			  getCamera();
			  startPreview();
		  }
	  }
	  
	  public void stopCamera() {
		  turnLightOff();
		  if (mCamera != null) {
		      stopPreview();
		      mCamera.release();
		      mCamera = null;
		    }; 
	  }

	  private void getCamera() {
	    if (mCamera == null) {
	      try {
	        mCamera = Camera.open();
	      } catch (RuntimeException e) {
	        Log.e(TAG, "Camera.open() failed: " + e.getMessage());
	      }
	    }
	  }

	  /*
	   * Called by the view (see main.xml)
	   */
	  public void toggleLight(View view) {
	    toggleLight();
	  }

	  private void toggleLight() {
	    if (lightOn) {
	      turnLightOff();
	    } else {
	      turnLightOn();
	    }
	  }

	  public void turnLightOn() {
	    
	    if (mCamera == null) {
//	      Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG);
	      // Use the screen as a flashlight (next best thing)
//	      button.setBackgroundColor(COLOR_WHITE);
	      return;
	    }
	    lightOn = true;
	    Parameters parameters = mCamera.getParameters();
	    if (parameters == null) {
	      // Use the screen as a flashlight (next best thing)
//	      button.setBackgroundColor(COLOR_WHITE);
	      return;
	    }
	    List<String> flashModes = parameters.getSupportedFlashModes();
	    // Check if camera flash exists
	    if (flashModes == null) {
	      // Use the screen as a flashlight (next best thing)
//	      button.setBackgroundColor(COLOR_WHITE);
	      return;
	    }
	    String flashMode = parameters.getFlashMode();
	    if(MainActivity.VERBOSE)Log.i(TAG, "Flash mode: " + flashMode);
	    if(MainActivity.VERBOSE) Log.i(TAG, "Flash modes: " + flashModes);
	    if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
	      // Turn on the flash
	      if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
	        parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
	        mCamera.setParameters(parameters);
//	        button.setBackgroundColor(COLOR_LIGHT);
//	        startWakeLock();
	      } else {
//	        Toast.makeText(this, "Flash mode (torch) not supported",
//	            Toast.LENGTH_LONG);
	        // Use the screen as a flashlight (next best thing)
//	        button.setBackgroundColor(COLOR_WHITE);
	        Log.e(TAG, "FLASH_MODE_TORCH not supported");
	      }
	    }
	  }

	  public void turnLightOff() {
	    if (lightOn) {
	      // set the background to dark
//	      button.setBackgroundColor(COLOR_DARK);
	      lightOn = false;
	      if (mCamera == null) {
	        return;
	      }
	      Parameters parameters = mCamera.getParameters();
	      if (parameters == null) {
	        return;
	      }
	      List<String> flashModes = parameters.getSupportedFlashModes();
	      String flashMode = parameters.getFlashMode();
	      // Check if camera flash exists
	      if (flashModes == null) {
	        return;
	      }
	      if(MainActivity.VERBOSE)Log.i(TAG, "Flash mode: " + flashMode);
	      if(MainActivity.VERBOSE)Log.i(TAG, "Flash modes: " + flashModes);
	      if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
	        // Turn off the flash
	        if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
	          parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
	          mCamera.setParameters(parameters);
//	          stopWakeLock();
	        } else {
	          Log.e(TAG, "FLASH_MODE_OFF not supported");
	        }
	      }
	    }
	  }

	  private void startPreview() {
	    if (!previewOn && mCamera != null) {
	      mCamera.startPreview();
	      previewOn = true;
	    }
	  }

	  private void stopPreview() {
	    if (previewOn && mCamera != null) {
	      mCamera.stopPreview();
	      previewOn = false;
	    }
	  }

	  @Override
	  public void surfaceChanged(SurfaceHolder holder, int I, int J, int K) {
	    //Log.d(TAG, "surfaceChanged");
	  }

	  @Override
	  public void surfaceCreated(SurfaceHolder holder) {
	    //Log.d(TAG, "surfaceCreated");
	    if (mCamera != null) {
	    	try {
	    		mCamera.setPreviewDisplay(holder);
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    }
	  }

	  @Override
	  public void surfaceDestroyed(SurfaceHolder holder) {
	    //Log.d(TAG, "surfaceDestroyed");
	  }
	}


