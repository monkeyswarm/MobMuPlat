package com.iglesiaintermedia.mobmuplat.controls;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import com.iglesiaintermedia.mobmuplat.MainActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

public class MMPPanel extends MMPControl {
	
	//private String _imagePath;
	private boolean _shouldPassTouches;
	private boolean _showWarningText;
	private boolean _highlighted;
	private RectF _myRect;
	private Bitmap _imageBitmap;
	private int opaqueColor = 0xFFFFFFFF;
	private String _imagePath;
	public MMPPanel(Context context, float screenRatio) {
		super(context, screenRatio);
		_myRect = new RectF();
	}
	
	public void setImagePath(String path) {//takes full path. widget may not be laid out.
		/*if(_imageBitmap!=null) _imageBitmap.recycle();
		_imageBitmap = BitmapFactory.decodeFile(path);
		invalidate();*/
		if (path!=null && !path.equals(_imagePath)) {
			_imagePath = path;
			maybeRefreshImage();
		}
	}
	
	public void setShouldPassTouches(boolean shouldPassTouches) {
		_shouldPassTouches = shouldPassTouches;
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed == true) {
			_myRect.set(0,0,right-left, bottom-top);
			maybeRefreshImage();
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (!this.isEnabled()) return false; //reject touch down if disabled.
		if(!_shouldPassTouches)getParent().requestDisallowInterceptTouchEvent(true);
		return true;
	}
	
	private void maybeRefreshImage() {
		if (_imagePath!=null && !_myRect.isEmpty()) {
			if(_imageBitmap!=null) _imageBitmap.recycle();
			//_imageBitmap = decodeSampledBitmapFromFile(_imagePath, (int)_myRect.width(), (int)_myRect.height());
			//invalidate();
			BitmapWorkerTask task = new BitmapWorkerTask(this, _imagePath, (int)_myRect.width(), (int)_myRect.height());
			task.execute();
		}
	}
	
	protected void onDraw(Canvas canvas) {
		
        	//this.paint.setStyle(Paint.Style.FILL);
		this.paint.setFilterBitmap(true);
        if (_highlighted) this.paint.setColor(this.highlightColor);
        else this.paint.setColor(this.color);
        canvas.drawRect(_myRect, this.paint);
        
        //set paint to opaque
        this.paint.setColor(opaqueColor);
        if(_imageBitmap!=null){
        	canvas.drawBitmap(_imageBitmap, null, _myRect, this.paint);
        }
        
		if(_showWarningText){
			this.paint.setColor(Color.BLACK);
			paint.setTextSize(20*this.screenRatio);
			canvas.drawText("image file not found", 10,20*this.screenRatio,this.paint);
		}
	}

	public void receiveList(List<Object> messageArray){ 
		super.receiveList(messageArray);
    	//image path
		if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("image")){
			String path =  (String)messageArray.get(1);
			File extFile = new File(/*Environment.getExternalStorageDirectory()*/ MainActivity.getDocumentsFolderPath(), path);
            setImagePath(extFile.getAbsolutePath());
		}
		//highlight
	    if (messageArray.size()==2 && (messageArray.get(0) instanceof String) && ((String)(messageArray.get(0))).equals("highlight")){
	    	_highlighted = ((int)(((Float)(messageArray.get(1))).floatValue()) > 0);//ugly
	    	invalidate();
	    }
	}
	
	// Image unpacking, from http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	// caps size to 1024 in either direction - otherwise we can run out of ram too quickly
	static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			//final int halfHeight = height / 2;
			//final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			// AND keep downsizing until larger dim is <= 1024
			while (((height / (inSampleSize * 2)) > reqHeight && (width / (inSampleSize * 2)) > reqWidth) ) { //|| 
					//((height / inSampleSize) > 1024 || (width / inSampleSize) > 1024) ){
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}
	
	static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(path, options);

	    //checkBitmapFitsInMemory(options.outWidth, options.outHeight, 1);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeFile(path, options);
	}
	
	// Not used.
	public static boolean checkBitmapFitsInMemory(long bmpwidth,long bmpheight, int bmpdensity ){

	    long reqsize=bmpwidth*bmpheight*bmpdensity*4;
	    long allocNativeHeap = Debug.getNativeHeapAllocatedSize();
	    long maxMemory = Runtime.getRuntime().maxMemory();

	    final long heapPad=(long) Math.max(4*1024*1024,maxMemory*0.1);
	    boolean over = ((reqsize + allocNativeHeap + heapPad) >= maxMemory);
	    Log.i("IMAGE ALLOC", "heap "+allocNativeHeap+" req "+reqsize+" max "+maxMemory+" over? "+ over);
	    
	    /*{
	        return false;
	    }*/
	    return true;

	}
	
	class BitmapWorkerTask extends AsyncTask<Void, Void, Bitmap> {
	    private final WeakReference<MMPPanel> panelReference;
	    //private int data = 0;
	    private final String path;
	    private final int width, height;
	    
	    public BitmapWorkerTask(MMPPanel panel, String path, int width, int height) {
	        // Use a WeakReference to ensure the ImageView can be garbage collected
	    	panelReference = new WeakReference<MMPPanel>(panel);
	    	this.path = path;
	    	this.width = width;
	    	this.height = height;
	    	
	    }

	    // Decode image in background.
	    @Override
	    protected Bitmap doInBackground(Void... params) {
	        //data = params[0];
	        return decodeSampledBitmapFromFile(path, width, height);
	        //return decodeSampledBitmapFromResource(getResources(), data, 100, 100));
	    }

	    // Once complete, see if ImageView is still around and set bitmap.
	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	        if (panelReference != null && bitmap != null) {
	            final MMPPanel panel = panelReference.get();
	            if (panel != null) {
	                //imageView.setImageBitmap(bitmap);
	            	panel._imageBitmap = bitmap;
	            	panel.invalidate();
	            }
	        }
	    }
	}
}
