package com.kostmo.flickr.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageSwitcher;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.SearchParameters;
import com.aetrion.flickr.photos.Size;
import com.kostmo.flickr.activity.prefs.PrefsSlideshow;
import com.kostmo.flickr.adapter.FlickrPhotoAdapter;
import com.kostmo.flickr.adapter.FlickrPhotoDualAdapter;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;
import com.kostmo.flickr.tasks.GenericBitmapDownloadTask;
import com.kostmo.flickr.tasks.ImageSizesGetterTask;
import com.kostmo.flickr.tasks.RetrievalTaskFlickrPhotolist;
import com.kostmo.flickr.tasks.RetrievalTaskFlickrPhotoset;

public class LiveSlideshowActivity extends SearchResultsViewerActivity {

	final int DIALOG_INSTRUCTIONS = 1;

	public static final String PREFKEY_SHOW_SLIDESHOW_INSTRUCTIONS = "PREFKEY_SHOW_SLIDESHOW_INSTRUCTIONS";

	public static final String EXTRA_STARTING_SLIDE = "INTENT_EXTRA_STARTING_SLIDE";
	
	FlickrPhotoAdapter flickr_adapter;
	ImageSwitcher image_holder;
	Handler handler = new Handler();
	SlideChangerRunnable slide_changer_instance = new SlideChangerRunnable();
	View progress_enhancing;
	View new_search_empty_button;
	
	ImageSizesGetterExtended image_sizes_getter_task;
 	GenericBitmapDownloadTaskExtended bitmap_downloader_task;

	PowerManager.WakeLock wl;
	boolean slideshow_playing = false;
	boolean cold_start = true;
    int current_requested_screen_orientation_index = 0;
	
    // ========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	this.wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Slideshow");
    	
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        if (settings.getBoolean(PrefsSlideshow.PREFKEY_HIDE_TITLE, true))
        	requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (settings.getBoolean(PrefsSlideshow.PREFKEY_FULL_SCREEN, true))
	        getWindow().setFlags(
	        		WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	                WindowManager.LayoutParams.FLAG_FULLSCREEN );
        
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        super.onCreate(savedInstanceState);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        progress_enhancing = findViewById(R.id.progress_enhancing);
        image_holder = (ImageSwitcher) findViewById(R.id.image_display);
        new_search_empty_button = findViewById(R.id.new_search_empty_button);
        new_search_empty_button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				launch_search_dialog();
			}
        });

		if (savedInstanceState == null) {
			if (!settings.getBoolean(PREFKEY_SHOW_SLIDESHOW_INSTRUCTIONS, false)) {
				showDialog(DIALOG_INSTRUCTIONS);
			}
		}

		this.cold_start = getLastNonConfigurationInstance() == null;

		globally_stored_selected_thumbnail_index = getIntent().getIntExtra(EXTRA_STARTING_SLIDE, 0) - 1;
		

        flickr_adapter = new FlickrPhotoDualAdapter(this);
		runInitialSearch(flickr_adapter);
		
		if (globally_stored_dialog_bitmap != null)
			setNewSlideshowBitmap(globally_stored_dialog_bitmap);

		if (getAdapter().getCount() > 0)
			new_search_empty_button.setVisibility(View.GONE);
    }

    // ========================================================================
    @Override
    protected void onResume() {
    	super.onResume();

		wl.acquire();
		
    	if (slideshow_playing) {
    		if (this.cold_start) {
    			advanceSlide(true);
    		} else {
    			beginSlideTimer();
    		}
    	}
    }

    // ========================================================================
    @Override
    protected void onPause() {
    	super.onPause();

		stopSlideshow();
		wl.release();
    }

    // ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	super.onSaveInstanceState(out_bundle);

    	out_bundle.putBoolean("slideshow_playing", slideshow_playing);
    	out_bundle.putInt("current_requested_screen_orientation", current_requested_screen_orientation_index);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	super.onRestoreInstanceState(in_bundle);

    	slideshow_playing = in_bundle.getBoolean("slideshow_playing");
    	current_requested_screen_orientation_index = in_bundle.getInt("current_requested_screen_orientation");
    }

    // ========================================================================
 	private class ImageSizesGetterExtended extends ImageSizesGetterTask {

	    protected ImageSizesGetterExtended(Context context) {
			super(context);
		}

	    @Override
	    public void onPreExecute() {
			// Override for no progress dialog
	    	
	    	if (settings.getBoolean("show_loading_bar", true))
	    		progress_enhancing.setVisibility(View.VISIBLE);
	    }
	    
		@Override
	    protected void onPostExecute(Collection<Size> sizes) {
//	    	super.onPostExecute(sizes);
	    	
	    	List<Size> sorted_sizes = new ArrayList<Size>();
			for (Size s : sizes)
				if (s.getLabel() != Size.SizeType.SQUARE.ordinal())	// FIXME
					sorted_sizes.add(s);
			
			if (sorted_sizes.size() <= 0) return;
			
			Collections.sort(sorted_sizes, new Comparator<Size>() {
				public int compare(Size object1, Size object2) {
					return new Integer(object1.getWidth()).compareTo(object2.getWidth());
				}
			});

			
			// Gets next larger size
	        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			int desired_width = display.getWidth();
			
//			Log.d(TAG, "Desired width: " + desired_width);
			
			Size size = getMinWidthSize(sorted_sizes, desired_width);
			bitmap_downloader_task = new GenericBitmapDownloadTaskExtended(context);
			bitmap_downloader_task.execute( size.getSource() );
	    }
	}
 	
    // ========================================================================
 	Size getMinWidthSize(List<Size> sorted_sizes, int width) {
 		for (Size s : sorted_sizes) {
 			if (s.getWidth() >= width) {
// 				Log.d(TAG, "Found appropriate size: " + s.getWidth() + "x" + s.getHeight());
 				return s;
 			}
 		}
 		return sorted_sizes.get(sorted_sizes.size() - 1);
 	}
 	
    // ========================================================================
	void picture_cycler(int pos) {
		
		globally_stored_selected_thumbnail_index = pos;

		Photo photo = (Photo) getAdapter().getItem(globally_stored_selected_thumbnail_index);
		
		if (photo != null) {
			image_sizes_getter_task = new ImageSizesGetterExtended(this);
			image_sizes_getter_task.execute(photo);
		} else {
			Log.e(TAG, "End of the line; photo not available!");
		}
	}

    // ========================================================================
    void advanceSlide(boolean forward) {

    	if (getAdapter().getCount() > 0) {
    		int inc = forward ? 1 : -1;
	    	int next_selection = (globally_stored_selected_thumbnail_index + inc + getAdapter().getCount()) % getAdapter().getCount();

//	    	Log.d(TAG, "Advancing slide to: " + next_selection);
			picture_cycler(next_selection);
    	}
    }
    
    // ========================================================================
    public class RetrievalTaskFlickrPhotolistExtended extends RetrievalTaskFlickrPhotolist {

    	public RetrievalTaskFlickrPhotolistExtended(
    			Context context,
    			RefreshablePhotoListAdapter adapter,
    			TextView grid_title,
    			SearchParameters task_search_params,
    			int current_page,
    			int photos_per_page,
    			AtomicInteger task_semaphore) {

    		super(context,
        			adapter,
        			grid_title,
        			task_search_params,
        			current_page,
        			photos_per_page,
        			task_semaphore);
    	}


        @Override
        public void onProgressUpdate(List<Photo>... progress_packet) {
        	updatePhotoList(target_list_adapter, progress_packet[0], total_matches);
        }
    }

    // ========================================================================
    void updatePhotoList(RefreshablePhotoListAdapter target_list_adapter, List<Photo> photolist, int total_matches) {
    	if (target_list_adapter != null) {
    		target_list_adapter.refresh_list(photolist);

    		target_list_adapter.setTotalResults(total_matches);
    		
    		
    		if (photolist.size() > 0) {
    			new_search_empty_button.setVisibility(View.GONE);

    			slideshow_playing = true;
    			advanceSlide(true);
    			return;
    		}

    	} else
    		Log.e(TAG, "Why list adapter null?");
    	
    	
    	new_search_empty_button.setVisibility(View.VISIBLE);
    }
    
    // ========================================================================
    public class RetrievalTaskFlickrPhotosetExtended extends RetrievalTaskFlickrPhotoset {

    	public RetrievalTaskFlickrPhotosetExtended(
    			Context context,
    			RefreshablePhotoListAdapter adapter,
    			TextView grid_title,
    			String photoset_id,
    			int current_page,
    			int photos_per_page,
    			AtomicInteger task_semaphore) {

    		super(context,
        			adapter,
        			grid_title,
        			photoset_id,
        			current_page,
        			photos_per_page,
        			task_semaphore);
    	}


        @Override
        public void onProgressUpdate(List<Photo>... progress_packet) {
        	updatePhotoList(target_list_adapter, progress_packet[0], total_matches);
        }
    }
    
    // ========================================================================
 	class GenericBitmapDownloadTaskExtended extends GenericBitmapDownloadTask {

		GenericBitmapDownloadTaskExtended(Context context) {
			super(context);
		}

	    @Override
	    public void onPreExecute() {
			// Override for no progress dialog
	    }
	    

	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
//	    	super.onPostExecute(bitmap);	// No progress dialog to dismiss
	    	

	    	progress_enhancing.setVisibility(View.GONE);
	    	
	    	if (bitmap != null) {
//		    	Log.d(TAG, "Downloaded bitmap with size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

	    		setNewSlideshowBitmap(bitmap);
		    	
//		    	Log.d(TAG, "Image dimensions: " + image_holder.getWidth() + "x" + image_holder.getHeight());

	    	} else {

				Toast.makeText(context, "Couldn't download image.", Toast.LENGTH_SHORT).show();
	    	}
	    	
            if (slideshow_playing)
            	beginSlideTimer();
	    }
	}
 	
    // ========================================================================
 	void setNewSlideshowBitmap(Bitmap bitmap) {
    	globally_stored_dialog_bitmap = bitmap;
    	
    	Drawable drawable = new BitmapDrawable(getResources(), bitmap);
//    	Rect padding_rect = new Rect();
//    	drawable.getPadding(padding_rect);
//    	Log.d(TAG, "Drawable dimensions: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());

    	image_holder.setImageDrawable(drawable);
 	}
	
    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
        case DIALOG_FLICKR_AVAILABLE_SIZES:
        {
        	break;
        }
        default:
        	break;
        }
        
        super.onPrepareDialog(id, dialog);
    }
	
    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
        switch (id) {
        case DIALOG_INSTRUCTIONS:
        {
	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

	        ((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_slideshow);
	        
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Slideshow")
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

            		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            		settings.edit().putBoolean(PREFKEY_SHOW_SLIDESHOW_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
                }
            })
            .create();
        }
        }
        return super.onCreateDialog(id);
    }

    // ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_slideshow, menu);

        return true;
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.menu_slideshow_pause).setVisible(slideshow_playing);
        menu.findItem(R.id.menu_slideshow_start).setVisible(!slideshow_playing);

        return super.onPrepareOptionsMenu(menu);
    }

    // ========================================================================
    class SlideChangerRunnable implements Runnable {

		@Override
		public void run() {
			advanceSlide(true);
		}
    }
    
    // ========================================================================
    void stopSlideshow() {
    	handler.removeCallbacks(slide_changer_instance);
    }
    
    // ========================================================================
    void beginSlideTimer() {
    	long delayMillis = settings.getInt("slide_time", 10)*1000;
    	handler.postDelayed(slide_changer_instance, delayMillis);
    }
    
    // ========================================================================
    final static int[] SCREEN_ORIENTATION_OPTIONS = {
    		ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    		ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
    		ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    };
    
    final static String[] SCREEN_ORIENTATION_LABELS = {
    		"Unspecified",
    		"Landscape",
    		"Portrait"
    };
    
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        switch (item.getItemId()) {
        
        case R.id.menu_change_orientation:
        {
        	current_requested_screen_orientation_index = (current_requested_screen_orientation_index + 1) % SCREEN_ORIENTATION_OPTIONS.length;
        	
			Toast.makeText(this, "Orientation: " + SCREEN_ORIENTATION_LABELS[current_requested_screen_orientation_index], Toast.LENGTH_SHORT).show();
        	setRequestedOrientation(SCREEN_ORIENTATION_OPTIONS[current_requested_screen_orientation_index]);
            return true;
        }
        case R.id.menu_preferences:
        {	
        	Intent i = new Intent();
        	i.setClass(this, PrefsSlideshow.class);
        	startActivity(i);
            return true;
        }
        case R.id.menu_help:
        {	
            showDialog(DIALOG_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_slideshow_previous:
        case R.id.menu_slideshow_next:
        {	
        	stopSlideshow();
            slideshow_playing = false;
            
        	advanceSlide(item.getItemId() == R.id.menu_slideshow_next);
            return true;
        }        
        case R.id.menu_slideshow_start:
        {
            slideshow_playing = true;
        	advanceSlide(true);
            
            return true;
        }
        case R.id.menu_slideshow_pause:
        {	
        	
        	stopSlideshow();
            slideshow_playing = false;

            return true;
        }
        }
        return super.onOptionsItemSelected(item);
    }

    // ========================================================================
	@Override
	int getMainLayoutResource() {
		return R.layout.slideshow;
	}
	
    // ========================================================================
	@Override
	public ListAdapter getAdapter() {
		return flickr_adapter;
	}

    // ========================================================================
	@Override
	void executeSearchTask(Context context, RefreshablePhotoListAdapter adapter,
			TextView grid_title, SearchParameters sp, int current_page,
			AtomicInteger retrieval_tasks_semaphore) {

		int per_page;
		if (settings.getBoolean("slide_count_use_search_count", true))
			per_page = settings.getInt("photos_per_page", Market.DEFAULT_PHOTOS_PER_PAGE);
		else
			per_page = settings.getInt("independent_slide_count", Market.DEFAULT_PHOTOS_PER_PAGE);
		
		
		photo_search_task = new RetrievalTaskFlickrPhotolistExtended(
    			this,
    			adapter,
    			grid_title,
    			sp,
    			current_page,
    			per_page,
    			retrieval_tasks_semaphore);
    	
    	photo_search_task.execute();
	}

    // ========================================================================
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w(TAG, "onDestroy()");
		
		
		if (image_sizes_getter_task != null)
			image_sizes_getter_task.cancel(true);
		

		if (bitmap_downloader_task != null)
			bitmap_downloader_task.cancel(true);
		

		if (photo_search_task != null)
			photo_search_task.cancel(true);
    }

    // ========================================================================
	@Override
	void executePhotosetSearchTask(Context context, RefreshablePhotoListAdapter adapter,
			TextView gridTitle, String photosetId, int currentPage, AtomicInteger taskSemaphore) {


		int per_page;
		if (settings.getBoolean("slide_count_use_search_count", true))
			per_page = settings.getInt("photos_per_page", Market.DEFAULT_PHOTOS_PER_PAGE);
		else
			per_page = settings.getInt("independent_slide_count", Market.DEFAULT_PHOTOS_PER_PAGE);
		
		
		photo_search_task = new RetrievalTaskFlickrPhotosetExtended(
    			this,
    			adapter,
    			gridTitle,
    			photosetId,
    			currentPage,
    			per_page,
    			taskSemaphore);
    	
    	photo_search_task.execute();
	}
}
