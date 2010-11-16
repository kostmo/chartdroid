package com.kostmo.flickr.activity;

import java.lang.ref.SoftReference;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.Size;
import com.kostmo.flickr.bettr.ApplicationState;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.tasks.AsyncPhotoPopulator;
import com.kostmo.flickr.tasks.AsyncTaskModified;
import com.kostmo.flickr.tasks.ImageSizesGetterTask;
import com.kostmo.flickr.tools.FlickrFetchRoutines;
import com.kostmo.tools.SemaphoreHost;

public class ImageZoomActivity extends Activity implements SemaphoreHost {

	public final static String TAG = "ImageZoomActivity";

	public AsyncPhotoPopulator photo_populator;

	List<Size> sorted_sizes;
	Map<Size, Boolean> image_too_big = new HashMap<Size, Boolean>();
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

    // =============================================

	boolean controls_visible = true;

    ZoomControls zoom_controls;
    View zoom_reset;
    ImageView zoomTest;
    
    float translate_x = 0;
    float translate_y = 0;

    float scale_exponent = 0;
    final float SCALE_BASE = 1.5f;

    boolean initial_centering_untouched = true;
    Photo flickr_photo;
    Bitmap bitmap;
    ImageSizesGetterExtended sizes_getter_task;
    DuplicatedThumbnailBitmapRetrievalTask bitmap_retrieval_task;
    
	// ====================================
    double logArgs(double base, double x) {
    	return Math.log(x)/Math.log(base);
    }
    
    float getScale() {
        return (float) Math.pow(SCALE_BASE, scale_exponent);
    }

    void setScale(float new_scale) {
    	scale_exponent = (float) logArgs(SCALE_BASE, new_scale);
    }
    
    // =============================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_zoom);
        
    	photo_populator = new AsyncPhotoPopulator(this);
    	
        zoomTest = (ImageView) findViewById(R.id.zoom_test);

        ApplicationState my_app = (ApplicationState) getApplication();
        flickr_photo = my_app.active_flickr_photo;
        
        
        bitmap = my_app.active_thumbnail_bitmap;

		zoomTest.setImageBitmap(bitmap);
		centerImage();

        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	sorted_sizes = a.stored_dialog_photosizes;
        	image_too_big = a.too_big_map;
        } else {
        	if (my_app.active_flickr_photo != null) {
	    		sizes_getter_task = new ImageSizesGetterExtended(this);
	    		sizes_getter_task.execute(my_app.active_flickr_photo);
        	} else {
        		Log.e(TAG, "Active flickr photo was null!");
        	}
        }

        
		

        zoom_reset = findViewById(R.id.zoom_reset);
        zoom_reset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				centerImage();
			}
        });
        
        zoom_controls = (ZoomControls) findViewById(R.id.zc);
        zoom_controls.setOnZoomInClickListener(new ZoomControls.OnClickListener() {
			@Override
			public void onClick(View v) {
				zoom_in(); 
			}
        });
        zoom_controls.setOnZoomOutClickListener(new ZoomControls.OnClickListener() {
			@Override
			public void onClick(View v) {
				zoom_out();
			}
        });
        
        final GestureDetector gestureDetector = new GestureDetector(this, new MyGestureDetector());
        zoomTest.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
            	return gestureDetector.onTouchEvent(event);
            }
        });
    }
    
    // =============================================

    class StateRetainer {
		List<Size> stored_dialog_photosizes;
		Map<Size, Boolean> too_big_map;
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {

    	StateRetainer a = new StateRetainer();
		a.stored_dialog_photosizes = sorted_sizes;
		a.too_big_map = image_too_big;
        return a;
    }

	// =============================================
 	private class ImageSizesGetterExtended extends ImageSizesGetterTask {

	    protected ImageSizesGetterExtended(Context context) {
			super(context);
		}

		@Override
	    protected void onPostExecute(Collection<Size> sizes) {
	    	super.onPostExecute(sizes);
	    	
	    	image_too_big.clear();
			sorted_sizes = new ArrayList<Size>();
//			sorted_sizes.addAll(sizes);
			for (Size s : sizes)
				if (s.getLabel() != Size.SizeType.SQUARE.ordinal())	// FIXME
					sorted_sizes.add(s);
			
			Collections.sort(sorted_sizes, new Comparator<Size>() {
				public int compare(Size object1, Size object2) {
					return new Integer(object1.getWidth()).compareTo(object2.getWidth());
				}
			});

	    	centerImage();
	    }
	}

    // =============================================
	Size getLeastLargerSize(List<Size> sorted_sizes, Bitmap bitmap) {

    	float desired_scale = getScale();
//    	Log.e(TAG, "Target scale: " + desired_scale);
    	
    	int desired_width = (int) (desired_scale * bitmap.getWidth());
//    	Log.e(TAG, "Target width: " + desired_width);
    	
		Size smallest_larger_size = null;
		for (Size s : sorted_sizes) {
			smallest_larger_size = s;
//			if (s.getWidth() >= desired_width && s.getLabel() != Size.SQUARE)
			if (s.getWidth() >= desired_width)
				break;
		}
		
//		Log.e(TAG, "Width to fetch: " + smallest_larger_size.getWidth());
		return smallest_larger_size;
	}

    // ======================================
    void centerImage() {
//    	Log.d(TAG, "Called centerImage()");
    	
    	if (bitmap != null) {
            int bitmap_width = bitmap.getWidth();
            int bitmap_height = bitmap.getHeight();
            
            Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            int window_width = display.getWidth();
            int window_height = display.getHeight();
            
        	translate_x = (window_width - bitmap_width) / 2f;
        	translate_y = (window_height - bitmap_height) / 2f;

//        	Log.e(TAG, "Current bitmap width: " + bitmap_width);
        	
        	float bitmap_relative_new_scale = getBitmapRelativeFitScale();
//        	Log.e(TAG, "Bitmap-relative new scale: " + bitmap_relative_new_scale);
        	setScale(bitmap_relative_new_scale);
    	}

    	upgradeImageQuality(true);
    	initial_centering_untouched = true;
    }

    // ======================================
    
    float getBitmapRelativeFitScale() {
//    	Log.d(TAG, "Called getBitmapRelativeFitScale()");
    	
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
    	
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int window_width = display.getWidth();
        int window_height = display.getHeight();
        

		float intrinsic_aspect_ratio = w / (float) h;
		float padded_canvas_aspect_ratio = window_width / (float) window_height;
		if (intrinsic_aspect_ratio > padded_canvas_aspect_ratio)
			return window_width / (float) w;
		else
			return window_height / (float) h;
    }
    
    // ======================================
    void updateImage() {
//    	Log.d(TAG, "Called updateImage()");
    	
    	if (bitmap == null) {
    		Log.e(TAG, "The bitmap was null (it shouldn't be...");
    		return;	// XXX
    	}
    	
        Matrix mtrx = new Matrix();

        int bitmap_width = bitmap.getWidth();
        int bitmap_height = bitmap.getHeight();
        
        float scale = getScale();
        mtrx.postScale(scale, scale, bitmap_width/2f, bitmap_height/2f);
 
        mtrx.postTranslate(translate_x, translate_y);
        zoomTest.setImageMatrix(mtrx);
        zoomTest.invalidate(); 
    }

    // ======================================
    // PRECONDITION: Must have list of sizes available.
    void upgradeImageQuality(boolean recenter) {
//    	Log.d(TAG, "Called upgradeImageQuality()");
    	
    	if (sorted_sizes == null) {
        	updateImage();	// XXX
    		return;
    	}
    	
    	getBitmap(getLeastLargerSize(sorted_sizes, bitmap), zoomTest, recenter);
    }
    
    // ======================================
    public void zoom_in() {
//    	Log.i(TAG, "Zoom in");
    	
    	scale_exponent++;

    	reposition();
    	updateImage();	// Note: Calling this twice is intentional!
    	
    	upgradeImageQuality(false);
    }

    // ======================================
    public void zoom_out() {
//    	Log.i(TAG, "Zoom out");
    	
    	scale_exponent--;
    	
    	reposition();
    	updateImage();	// Note: Calling this twice is intentional!
    	
    	upgradeImageQuality(false);
    }

	// ====================================
	// Repositions and rescales appropriately
    void reconfigureNewBitmap(Bitmap new_bitmap) {
//    	Log.d(TAG, "Called reconfigureNewBitmap()");

    	
    	int old_bitmap_width = bitmap.getWidth();
    	int old_bitmap_height = bitmap.getHeight();
    	
    	int new_bitmap_width = new_bitmap.getWidth();
    	int new_bitmap_height = new_bitmap.getHeight();

    	// Multiply the scale by the size ratio
    	float old_scale = getScale();
    	float new_scale = old_scale * old_bitmap_width / (float) new_bitmap_width;
//    	Log.d(TAG, "Recieved new bitmap (" + old_bitmap_width + " replaced by " + new_bitmap_height + "); adjusting scale from " + old_scale + " to " + new_scale);
    	setScale(new_scale);

    	bitmap = new_bitmap;
		zoomTest.setImageBitmap(bitmap);
		
    	translate_x -= (new_bitmap_width - old_bitmap_width)/2f;
    	translate_y -= (new_bitmap_height - old_bitmap_height)/2f;
 

		updateImage();
    }

    // ======================================
    void getBitmap(Size size, ImageView image_view, boolean recenter) {
    	String image_url = size.getSource();

    	
		if (photo_populator.bitmapReferenceMap.containsKey(image_url)) {
			
			Bitmap bm = photo_populator.bitmapReferenceMap.get(image_url).get();
			if (bm != null) {
				reconfigureNewBitmap(bm);
				return;
			}
		} else if (image_too_big.containsKey(size) && image_too_big.get(size)) {
			
			// TODO: Check all smaller sizes for the same flag?
			
			Log.e(TAG, "Size was previously established as too big.");
			return;
		}
		
		if (bitmap_retrieval_task != null)
			bitmap_retrieval_task.cancel(true);
		
		bitmap_retrieval_task = new DuplicatedThumbnailBitmapRetrievalTask(image_view, size, recenter);
		bitmap_retrieval_task.execute();
    }

	// ====================================
    public class DuplicatedThumbnailBitmapRetrievalTask extends AsyncTaskModified<Void, Void, Bitmap> {

    	ImageView flickr_view_holder;
    	String image_url;
    	
    	String error_message = "Couldn't fetch image.";
    	Size target_size;
    	boolean recenter;
    	DuplicatedThumbnailBitmapRetrievalTask(ImageView holder, Size size, boolean recenter) {
    		target_size = size;
        	
        	String image_url = size.getSource();
    		
    		this.image_url = image_url;
    		this.flickr_view_holder = holder;
    		this.recenter = recenter;
    	}
    	
    	void cleanup() {
    		decSemaphore();
    	}
		
		@Override
		protected void onCancelled() {
			cleanup();
		}
		
	    @Override
	    public void onPreExecute() {

	    	incSemaphore();
    		
	    	/*
		   	if (bitmapMap.containsKey(image_url)) {
		   		Bitmap d = bitmapMap.get(image_url);
		   		set_view_elements(d);
		   		cancel(false);
		   	}
		   	*/
	    }
    	
		protected Bitmap doInBackground(Void... params) {
			try {
				return FlickrFetchRoutines.networkFetchBitmap(image_url);
			} catch (UnknownHostException e) {
				
				
			} catch (OutOfMemoryError e) {
				
				image_too_big.put(target_size, true);
				
				String too_large = "Image too large for memory.";
				error_message = too_large;
				Log.w(TAG, too_large);
			}
			return null;
		}
		
	    @Override
	    public void onPostExecute(Bitmap fetched_bitmap) {

	    	if (fetched_bitmap != null) {
	    		photo_populator.bitmapReferenceMap.put(image_url, new SoftReference<Bitmap>(fetched_bitmap));
	    	
	    		reconfigureNewBitmap(fetched_bitmap);
	    		if (this.recenter)
	    			centerImage();
	    	} else {

	            Toast.makeText(ImageZoomActivity.this, error_message, Toast.LENGTH_SHORT).show();
	    	}
			
	    	cleanup();
	    }
    }
    
    // ======================================
    void reposition() {
    	initial_centering_untouched = false;
    	
//    	Log.d(TAG, "Called reposition()");
    	
    	// Case 1: Bitmap is wider than the screen
    	// Case 2: Bitmap fits within the screen
    	
    	// Case 1a: The left edge of the bitmap moves away from and to the right of
    	// the left edge of the screen
    	//     -> Move it back to the left edge
    	// Case 1b: The right edge of the bitmap moves away from and to the left of
    	// the right edge of the screen
    	//     -> Move it back to the right edge
    	
    	Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

    	// Horizontal and vertical behavior are independent.

        float scale = getScale();
        translate_x = limitPosition(translate_x, scale, bitmap.getWidth(), display.getWidth());
        translate_y = limitPosition(translate_y, scale, bitmap.getHeight(), display.getHeight());
        
    }

    // ======================================
    
    private float limitPosition(float translation, float scale, int bitmap_size, int window_size) {
    	
    	float scaled_bitmap_size = bitmap_size*scale;
		float near_stop = (scaled_bitmap_size - bitmap_size)/2f;
		float far_stop = window_size - (scaled_bitmap_size + bitmap_size)/2f;
    	if (scaled_bitmap_size > window_size) {
    		// Case 1
    		if ( translation > near_stop )
    			return near_stop;
    		else if ( translation < far_stop )
    			return far_stop;
    	} else {
    		// Case 2
    		if ( translation < near_stop )
    			return near_stop;
    		else if ( translation > far_stop )
    			return far_stop;
    	}
    	return translation;
    }
    
    // ======================================

    class MyGestureDetector extends SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
			return true;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        	
        	translate_x -= distanceX;
        	translate_y -= distanceY;

        	reposition();
        	updateImage();

        	return false;
        }
    }
    
	// ======================================
    
    public void incSemaphore() {

		findViewById(R.id.progress_enhancing).setVisibility(View.VISIBLE);
    	retrieval_tasks_semaphore.incrementAndGet();
    }
    
    public void decSemaphore() {

    	boolean still_going = retrieval_tasks_semaphore.decrementAndGet() > 0;
    	if (!still_going)
    		findViewById(R.id.progress_enhancing).setVisibility(View.GONE);
        
    }

    // =============================================
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_zoom, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_toggle_controls:
        	
        	controls_visible = !controls_visible;
        	int visibility = controls_visible ? View.VISIBLE : View.GONE;
        	
        	zoom_reset.setVisibility(visibility);
            zoom_controls.setVisibility(visibility);
        	
        	break;
        }

        return super.onOptionsItemSelected(item);
    }
    
	// =============================================
	@Override
	protected void onDestroy() {
		
		if (sizes_getter_task != null)
			sizes_getter_task.cancel(true);
		
		if (bitmap_retrieval_task != null)
			bitmap_retrieval_task.cancel(true);
		
		super.onDestroy();
	}
}