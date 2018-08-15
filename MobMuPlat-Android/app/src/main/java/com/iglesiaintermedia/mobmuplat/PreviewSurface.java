/*
Copyright 2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

// I(MobMuPlat), added some hacks below...
package com.iglesiaintermedia.mobmuplat;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class PreviewSurface extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG = "PreviewSurface";
    SurfaceHolder mHolder;
    Context mContext;
    Camera mCamera;
    int mHeight;
    int mWidth;
    Camera.Parameters mParameters;
    Callback mCallback;
    Activity mActivity;
    boolean hasCamera = false;
    boolean hasSurface = false;
    boolean isViewfinder = false;

    private static boolean cameraInfoSupported = false;

    private static void checkCameraInfoAvailable() throws NoClassDefFoundError {
        cameraInfoSupported = android.hardware.Camera.CameraInfo.class != null;
    }

    static {
        try {
            checkCameraInfoAvailable();
        } catch (NoClassDefFoundError e) {
            cameraInfoSupported = false;
        }
    }

    PreviewSurface(Context context) {
        super(context);
        mContext = context;
        initHolder();
    }

    public PreviewSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initHolder();
    }

    public PreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHolder();
    }

    private void initHolder() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        mHolder = holder;
        initCamera();
        //Log.d(TAG, "SURFACE CREATED");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        releaseCamera();
        //Log.d(TAG, "SURFACE DESTROYED");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        mWidth = w;
        mHeight = h;
        if (hasCamera) {

            setParameters();
            mCamera.startPreview();
            mCallback.cameraReady();

        }
        hasSurface = true;
        //Log.d(TAG, "SURFACE CHANGED");
    }

    public void kick() { //on permission grant after surface creation/display
        initCamera();
        if (hasCamera) {
            setParameters();
            mCamera.startPreview();
            mCallback.cameraReady();
        }
    }

    private void setParameters() {
        mParameters = mCamera.getParameters();

        if (isViewfinder) {
            List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
            // Pass the height and width BACKWARDS because this is portrait,
            // but the "supported" sizes are for landscape
            Camera.Size optimalSize = getOptimalPreviewSize(sizes, mHeight, mWidth);
            mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
            mCamera.setParameters(mParameters);
        }

        //setCameraDisplayOrientation();
    }

    private void setCameraDisplayOrientation() {
        if (cameraInfoSupported) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK, info);
            int rotation = mActivity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }

            int result = (info.orientation - degrees + 360) % 360;
            mCamera.setDisplayOrientation(result);
        } else {
            mCamera.setDisplayOrientation(90);
        }
    }

    public void initCamera() {
        if (!hasCamera) {
            try {
                mCamera = Camera.open();
                hasCamera = true;
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not open Camera"+ e);
                hasCamera = false;
                mCallback.cameraNotAvailable();
                return;
            }
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException exception) {
                Log.e(TAG, "Could not set preview surface");
                mCamera.release();
                mCamera = null;
                hasCamera = false;
                // TODO: add more exception handling logic here
            }
        }
    }

    public void lightOff() {
        if (hasSurface && hasCamera) {
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mParameters);
        }
    }

    public void lightOn() {
        if (this.isShown() && hasCamera) {
            if (mParameters == null) {
                setParameters();
            }
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mParameters);
        } else {
            initCamera();
        }
    }

    public boolean hasCamera() {
        return hasCamera;
    }

    public void setCallback(Callback c) {
        mCallback = c;
        mActivity = (Activity) c;
    }

    public void setIsViewfinder() {
        isViewfinder = true;
    }

    public void releaseCamera() {
        if (hasCamera) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            hasCamera = false;
        }
    }

    public void startPreview() {
        if (hasCamera) {
            setParameters();
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        }
    }

    // Thank you, ApiDemos :)
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    public interface Callback {
        public void cameraReady();
        public void cameraNotAvailable();
    }
}
