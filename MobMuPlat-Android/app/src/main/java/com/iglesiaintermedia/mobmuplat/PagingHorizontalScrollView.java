package com.iglesiaintermedia.mobmuplat;

import android.content.Context;
import android.util.AttributeSet;
//import android.util.Log;
import androidx.core.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class PagingHorizontalScrollView extends HorizontalScrollView  {

	private GestureDetectorCompat _gestureDetector;
	public PagingScrollViewDelegate pagingDelegate;
	private boolean didFling;
	public int pageCount = 1;
	
	public PagingHorizontalScrollView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		// TODO Auto-generated constructor stub
		_gestureDetector = new GestureDetectorCompat(context, new MyGestureListener());
	}

	
	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
		didFling = false;
		_gestureDetector.onTouchEvent(event); //detect fling
		// if no fling, see if our touch up should trigger scroll
		if (!didFling){
		switch(event.getAction()) {
    		case MotionEvent.ACTION_UP:
    				//Log.i("MobMuPlat", "touch up should scroll "+ this.getScrollX());
    				paginate(0);
    				return true; //I consume
			
		}
		}
        return super.onTouchEvent(event);
    }
	
	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures"; 
        
        @Override
        public boolean onDown(MotionEvent event) { 
            //Log.d(DEBUG_TAG,"onDown: " + event.toString()); 
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, 
                float velocityX, float velocityY) {
            //Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
            //Log.d(DEBUG_TAG, "vel "+velocityX); //neg to next, pos to prev
            didFling = true;
            paginate(velocityX < 0 ? 1 : -1);
            return true;
        }
    }
	
	private void paginate(int direction){//-1 to prev, 0 to wherever we are now, 1 to next 
		int pageIndex = this.getScrollX() / this.getWidth();
		if (direction == 0) { //round to nearest
			pageIndex = (int)Math.round((float)this.getScrollX() / this.getWidth());
		} else if (direction == -1) {
			pageIndex++;//hacK: if negative direction, it thinks we are on previous page, so add one to start page
			if (pageIndex>0) pageIndex--;
		} else if (direction == 1) {
			if (pageIndex<pageCount-1) pageIndex++;
		}
		final int newOffset = pageIndex * this.getWidth();
		final PagingHorizontalScrollView me = this;
		this.post(new Runnable() { //double check if this is needed
            public void run() { 
            	me.smoothScrollTo(newOffset, 0);
            } 
        });
		if(pagingDelegate!=null)pagingDelegate.onPage(pageIndex);
	}
	
}
