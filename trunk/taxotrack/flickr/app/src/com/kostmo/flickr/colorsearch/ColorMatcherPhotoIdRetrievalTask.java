package com.kostmo.flickr.colorsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotosInterface;
import com.kostmo.flickr.adapter.FlickrPhotoAdapter;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.AdapterHost;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tasks.AsyncTaskModified;
import com.kostmo.flickr.tools.RestClient;


public class ColorMatcherPhotoIdRetrievalTask extends AsyncTask<Void, Photo, List<Photo>> {

	static final String TAG = Market.DEBUG_TAG; 
	
	// Single threaded seems to go faster!  Weird.
	final boolean single_threaded_mode = true;
	

	ProgressDialog wait_dialog;
	
	String[] hex_colors;
	int TOTAL_ITEMS;
	int current_page;

	FlickrPhotoAdapter adapter;
	Flickr flickr = null;
	long[] photo_id_list;
	List<Photo> photo_list = new ArrayList<Photo>();

	
	Context context;
	public ColorMatcherPhotoIdRetrievalTask(Context c, final String[] colors, int photos_per_page, int current_page) {
		this.context = c;
		
		this.hex_colors = colors;
		this.TOTAL_ITEMS = photos_per_page;
		this.current_page = current_page;
		this.adapter = (FlickrPhotoAdapter) ((AdapterHost) context).getAdapter();
	}
	
	
	
	void instantiate_latent_wait_dialog() {

		wait_dialog = new ProgressDialog(context);
		wait_dialog.setMessage("Fetching image...");
		wait_dialog.setIndeterminate(true);
		wait_dialog.setCancelable(false);
		wait_dialog.show();
	}
	
	
	@Override
    public void onCancelled() {
		
		Log.e(TAG, "You cancelled the thread!");

	}
	
	
	@Override
    public void onPreExecute() {

    	adapter.photo_populator.incSemaphore();

    	
    	// DON'T USE A MODAL WAIT DIALOG!!!
/*
    	if (single_threaded_mode)
    		instantiate_latent_wait_dialog();
*/
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
			    new REST()
			);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
    }

	private String composeURL(int current_page, String[] colors) {
		
    	String concat_list = TextUtils.join(",", hex_colors);
    	String url = "http://labs.ideeinc.com/coloursearch/services/rest/?method=color.search&quantity=" + TOTAL_ITEMS + "&page=" + current_page + "&colors=" + concat_list + "&imageset=flickr";
    	return url;    	
	}
	
    public List<Photo> doInBackground(Void... params) {

    	// NOTE: Page ordinality is 0-based for this service
    	String url = composeURL(current_page - 1, hex_colors);
    	
    	JSONArray images = RestClient.connect(url);
    	
    	if (images == null) {
    		Log.e(TAG, "Could not retrieve images.");
    		this.cancel(true);
    	}
    	
		photo_id_list = new long[images.length()];
		try {
			for (int i=0; i<images.length(); i++) {
	
				photo_id_list[i] = images.getJSONArray(i).getLong(RestClient.FLICKR_PHOTO_ID_COLUMN);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	

		
		
		
		if (single_threaded_mode) {
			int i=0;
			PhotosInterface photoInt = flickr.getPhotosInterface();

			for (long id : photo_id_list) {
				if (this.isCancelled()) {
					cleanup();
					break;
				}
				
				try {
					Photo p = photoInt.getPhoto( Long.toString(id) );
		    		this.publishProgress( p );
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (FlickrException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}
				
				i++;
			}
		}
    	
   		return photo_list;
    }

    @Override
//    public void onProgressUpdate(ProgressPacket... progress_packet) {
    public void onProgressUpdate(Photo... photo) {

    	List<Photo> temp_list = new ArrayList<Photo>();
    	temp_list.add( photo[0] );
    	adapter.refresh_list( temp_list );
    	
    	TextView tv = (TextView) ((Activity) context).findViewById(R.id.photo_list_heading);
    	tv.setText("Page " + current_page + " (Found " + adapter.getCount() + " image" + (adapter.getCount() != 1 ? "s" : "") +")");
    }


    @Override
    public void onPostExecute(List<Photo> photo_list) {
    	
    	
    	if (!single_threaded_mode) {

			PhotosInterface photoInt = flickr.getPhotosInterface();
			for (long id : photo_id_list)
				new SinglePhotoFetcherTask(photoInt).execute(id);
    	} else {

    		// DON'T USE A MODAL WAIT DIALOG!!!
//			wait_dialog.dismiss();
    	}
    	
    	
    	cleanup();
    	
//    	adapter.add_photopairs(photo_list);
    }
    
    void cleanup() {
    	adapter.photo_populator.decSemaphore();
    }
    
    // =================================================
    
    public class SinglePhotoFetcherTask extends AsyncTaskModified<Long, Void, Photo> {
		PhotosInterface photoInt;
    	SinglePhotoFetcherTask(PhotosInterface pi) {
    		photoInt = pi;
    	}

    	@Override
        public void onPreExecute() {
        	adapter.photo_populator.incSemaphore();
        }
    	
		protected Photo doInBackground(Long... photo_ids) {
			if (this.isCancelled()) {
				cleanup();
			}
			
			try {
				
				Log.e(TAG, "I'm going asyncrhonously; what gives?");
				
				return photoInt.getPhoto( Long.toString(photo_ids[0]) );

				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FlickrException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		
	    @Override
	    public void onPostExecute(Photo photo) {
	    	
	    	if (photo != null) {
		    	List<Photo> temp_list = new ArrayList<Photo>();
		    	temp_list.add( photo );
		    	adapter.refresh_list( temp_list );
		    	
		    	TextView tv = (TextView) ((Activity) context).findViewById(R.id.photo_list_heading);
		    	tv.setText("Found " + adapter.getCount() + " image(s)");
	    	}
	    	
	    	cleanup();
	    }
    	
    }

}
