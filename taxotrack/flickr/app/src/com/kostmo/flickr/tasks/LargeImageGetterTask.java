package com.kostmo.flickr.tasks;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.Size;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.containers.ProgressHostActivity;
import com.kostmo.flickr.containers.ProgressStylePacket;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tools.FlickrFetchRoutines;


public class LargeImageGetterTask extends AsyncTask<Photo, ProgressStylePacket, Bitmap> {
	
	
	public enum DownloadObjective {SHOW_IN_DIALOG, OTHER};
	

	static final String TAG = Market.DEBUG_TAG;
	
	boolean use_medium_size;

	ProgressHostActivity activity;
	
	ProgressStylePacket last_progress_style;
	protected DownloadObjective download_objective;
	
	protected LargeImageGetterTask(ProgressHostActivity activity, boolean use_medium_size, DownloadObjective download_objective) {
		this.use_medium_size = use_medium_size;
		this.download_objective = download_objective;
		updateActivity(activity);
	}
	

	// Used for orientation changes
	public void updateActivity(ProgressHostActivity activity) {
		this.activity = activity;
		if (last_progress_style != null)
			publishProgress(last_progress_style);
	}

	// ====================================================================
    @Override
    public void onPreExecute() {
		activity.showProgressDialog();
    }

    // ====================================================================
	@Override
	protected void onProgressUpdate(ProgressStylePacket... progress) {
		ProgressStylePacket progress_stage = progress[0];
		last_progress_style = progress_stage;

		// XXX Might this grab the old reference?
		ProgressDialog progress_dialog = activity.getProgressDialog();
		progress_dialog.setMessage("Fetching " + (use_medium_size ? "medium" : "large") + " image...");
	}
    
	// ====================================================================
	@Override
    protected Bitmap doInBackground(Photo... photos) {
		Photo photo = photos[0];
		String large_url = null;
		
		if (use_medium_size) {
			large_url = photo.getMediumUrl().toString();
		}
		else {

	    	Flickr flickr = null;
	        try {
				flickr = new Flickr(
						ApiKeys.FLICKR_API_KEY,	// My API key
						ApiKeys.FLICKR_API_SECRET,	// My API secret
			        new REST()
			    );
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			
			PhotosInterface photoInt = flickr.getPhotosInterface();
			try {

				for (Size size : (Collection<Size>) photoInt.getSizes( photo.getId() ))
					if (size.getLabel() == Size.SizeType.LARGE.ordinal()) {	// FIXME
						Log.e(TAG, "Found large size!");
						
						large_url = photo.getLargeUrl().toString();
						break;
					}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (FlickrException e) {
				e.printStackTrace();
			}
			
			// Revert to medium URL if needed...
			if (large_url == null) {
				Log.e(TAG, "Did not find large size!");
				large_url = photo.getMediumUrl().toString();
			}
		}
		
		Bitmap bitmap = null;
		try {
			bitmap = FlickrFetchRoutines.networkFetchBitmap(large_url);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

    @Override
    protected void onPostExecute(Bitmap bitmap) {
    	activity.dismissProgressDialog();
    }
 }