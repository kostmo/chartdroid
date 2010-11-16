package com.kostmo.flickr.tasks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.photos.Photo;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;


abstract public class RetrievalTaskFlickrPhotosAbstract extends AsyncTask<Void, List<Photo>, String> {

	static final String TAG = Market.DEBUG_TAG; 

	boolean square_thumbnails = true;
	
	SharedPreferences settings;
	
	int photos_per_page;
	int current_page; 
	protected int total_matches = 0;
	int photo_count = 0;

	private AtomicInteger task_semaphore;
	protected RefreshablePhotoListAdapter target_list_adapter;

	ProgressDialog wait_dialog;

	protected Context context;
	
	int photo_source;
	TextView grid_title;
	
    // ========================================================================
    protected abstract void getFlickrPhotoMatches() throws FlickrException;

    // ========================================================================
	public RetrievalTaskFlickrPhotosAbstract(
			Context context,
			RefreshablePhotoListAdapter adapter,
			TextView grid_title,
			int current_page,
			int photos_per_page,
			AtomicInteger task_semaphore) {
		
		this.task_semaphore = task_semaphore;
		
		target_list_adapter = adapter;
		this.grid_title = grid_title;
		
		this.context = context;
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		this.photos_per_page = photos_per_page;
		this.current_page = current_page;

		square_thumbnails = settings.getBoolean("square_thumbnails", true);
		
//		Log.d(TAG, "Square thumbnails? " + square_thumbnails);
//		Log.i(TAG, "String setting: " + settings.getString("square_thumbnails", "1"));
//		Log.i(TAG, "Integer setting: " + settings.getInt("square_thumbnails", 1));
//		Log.i(TAG, "Boolean setting: " + settings.getBoolean("square_thumbnails", true));
	}


    // ========================================================================
	@Override
    public void onCancelled() {
		
		Log.e(TAG, "You cancelled the thread!");
		cleanup();
	}

    // ========================================================================
	void instantiate_latent_wait_dialog() {

		wait_dialog = new ProgressDialog(context);
		wait_dialog.setMessage("Searching...");
		wait_dialog.setIndeterminate(true);
		wait_dialog.setCancelable(true);
		wait_dialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				RetrievalTaskFlickrPhotosAbstract.this.cancel(true);
			}
		});
		wait_dialog.show();
	}

    // ========================================================================
	protected void cleanup() {
		((Activity) context).setProgressBarIndeterminateVisibility(task_semaphore.decrementAndGet() > 0);

		try {
			wait_dialog.dismiss();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Mystery bug...");
			e.printStackTrace();
		}
	}

    // ========================================================================
    @Override
    public void onPreExecute() {
		Log.d(TAG, "Beginnging Flickr search");
		

		instantiate_latent_wait_dialog();
		
		target_list_adapter.clear_list();
		
		
		
    	task_semaphore.incrementAndGet();
		((Activity) context).setProgressBarIndeterminateVisibility(true);
    }

    // ========================================================================
	protected String doInBackground(Void... params) {

		try {
			getFlickrPhotoMatches();
		} catch (FlickrException e) {
			return e.getErrorMessage();
		}

		return null;
	}
	
    // ========================================================================
    @Override
    public void onProgressUpdate(List<Photo>... progress_packet) {

    	String base_title = "Page " + current_page;
    	
    	String combined_title = base_title + " (" + photo_count + " fetched, " + total_matches + " total)";

    	if (grid_title != null)	// NOTE: DO NOT REMOVE THIS CHECK
    		grid_title.setText( combined_title );
    	else
    		Log.e(TAG, "Why is grid_title null?");

    	if (target_list_adapter != null) {
    		target_list_adapter.refresh_list(progress_packet[0]);

    		target_list_adapter.setTotalResults(total_matches);
    	} else
    		Log.e(TAG, "Why list adapter null?");
    }
	

    // ========================================================================
    @Override
    public void onPostExecute(String error_message) {
    	
    	if (error_message != null)
            Toast.makeText(context, error_message, Toast.LENGTH_LONG).show();
    	
    	Log.d(TAG, "Done with photo search.");
    	cleanup();
    }
}