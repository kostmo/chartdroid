package com.kostmo.flickr.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.SearchParameters;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.containers.ThumbnailUrlPlusLinkContainer;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tasks.AsyncTaskModified;
import com.kostmo.tools.StreamUtils;

public class FlickrFetchRoutines {

	static final String TAG = Market.DEBUG_TAG;
	
	public final static int[] unknown_pics = {R.drawable.picture_unknown_1, R.drawable.picture_unknown_2, R.drawable.picture_unknown_3};
	public final Map<String, Bitmap> drawableMap = new Hashtable<String, Bitmap>();

	
	Context context;
	public FlickrFetchRoutines(Context c) {
		context = c;
	}

	// =============================================================
	public static Bitmap networkFetchBitmap(String urlString) throws UnknownHostException, OutOfMemoryError {

//	   	Log.d(this.getClass().getSimpleName(), "image url:" + urlString);
	   	try {
	   		InputStream is = fetch(urlString);
//	   		Drawable drawable = Drawable.createFromStream(is, "src");
//	    	Log.w(TAG, "Drawable " + image_counter++ + " in networkFetchThumbnail(): " + drawable);
			
			
	   		// FIXME: THIS IS A WORKAROUND FOR A KNOWN BUG:
	   		// See http://groups.google.com/group/android-developers/browse_thread/thread/4ed17d7e48899b26/
	   		final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
	   		BufferedOutputStream out = new BufferedOutputStream(dataStream, 4096);
			StreamUtils.copy(is, out);
			out.flush();
			final byte[] data = dataStream.toByteArray();
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

	   		is.close();

	   		return bitmap;
	   	} catch (UnknownHostException e) {
	   		Log.e(TAG, "fetchDrawable failed", e);
	   		throw e;
	   	} catch (MalformedURLException e) {
	   		Log.e(TAG, "fetchDrawable failed", e);
	   	} catch (OutOfMemoryError e) {
	   		Log.e(TAG, "fetchDrawable failed", e);
	   		throw e;
	   	} catch (IOException e) {
	   		Log.e(TAG, "fetchDrawable failed", e);
	   	}
	   	
	   	return null;
	}

	// =============================================================
	public static List<ThumbnailUrlPlusLinkContainer> extractFlickrThumbnailData(Context context, List<Photo> flickr_search_resultset) {

    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	boolean square_thumbnails = settings.getBoolean("square_thumbnails", true);

    	List<ThumbnailUrlPlusLinkContainer> photo_url_pair_builder = new ArrayList<ThumbnailUrlPlusLinkContainer>(); 
		for (Photo p : flickr_search_resultset) {
			ThumbnailUrlPlusLinkContainer photo_url_pair = new ThumbnailUrlPlusLinkContainer();
			
			photo_url_pair.setLink( p.getUrl() );
			try {
				photo_url_pair.setThumbnailUrl( new URL( square_thumbnails ? p.getSmallSquareUrl() : p.getThumbnailUrl()) );
				
				photo_url_pair.setLargeUrl( new URL( p.getMediumUrl() ) );
				
				photo_url_pair.setIdentifier( p.getId() );
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			photo_url_pair_builder.add(photo_url_pair);
		}
		
		return photo_url_pair_builder;
	}
	
	// =============================================================
	
    public static List<ThumbnailUrlPlusLinkContainer> getFlickrPhotoMatches(Context context, AsyncTaskModified task, MachineTag search_machinetag, int search_quota) {

		SearchParameters search_params = new SearchParameters();
//		search_params.setExtrasMachineTags(true);
		search_params.setSafeSearch("1");
		search_params.setMachineTagMode("all");
		search_params.setSort( SearchParameters.INTERESTINGNESS_DESC );
		search_params.setMachineTags(new String[] {search_machinetag.getQueryString()});

		List<Photo> flickr_search_resultset = aggregateFlickrResultPages(task, context, search_params, search_quota);

		List<ThumbnailUrlPlusLinkContainer> photo_url_pair_builder = extractFlickrThumbnailData(context, flickr_search_resultset);

		return photo_url_pair_builder;
    }
	// =============================================================
	
    public static PhotoList getSingleFlickrPhotosetPage( AsyncTask asyncTask, Context context, String photoset_id, Set<String> extras, int privacy_filter, int per_page, int current_page) throws FlickrException {

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
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

        // Get token from saved Settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.READ);
        requestContext.setAuth(auth);

		


		PhotoList flickr_search_resultset = new PhotoList();
		
		try {
			PhotoList pl = flickr.getPhotosetsInterface().getPhotos(photoset_id, extras, privacy_filter, per_page, current_page);
			
			Log.d(TAG, "There are " + pl.getTotal() + " total hits across " + pl.getPages() + " pages with " + per_page + " per page.");

			flickr_search_resultset.setTotal( pl.getTotal() );

			flickr_search_resultset.addAll( (List<Photo>) pl );
			
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();

		}


		
		return flickr_search_resultset;
    }
	// =============================================================
	
    public static PhotoList getSingleFlickrResultPage( AsyncTask asyncTask, Context context, SearchParameters search_params, int per_page, int current_page) throws FlickrException {

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
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

        // Get token from saved Settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.READ);
        requestContext.setAuth(auth);

		
		
		
		
		PhotosInterface photoInt = flickr.getPhotosInterface();
    	

		PhotoList flickr_search_resultset = new PhotoList();
		
		try {
			PhotoList pl = photoInt.search(search_params, per_page, current_page);
			
			Log.d(TAG, "There are " + pl.getTotal() + " total hits across " + pl.getPages() + " pages with " + per_page + " per page.");

			flickr_search_resultset.setTotal( pl.getTotal() );

			flickr_search_resultset.addAll( (List<Photo>) pl );
			
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();

		}


		
		return flickr_search_resultset;
    }
    
    
    
    
	// =============================================================
	
    public static PhotoList aggregateFlickrResultPages( AsyncTaskModified task, Context context, SearchParameters search_params, int search_quota) {

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
    	
    	
    	
    	
    	
		PhotoList flickr_search_resultset = new PhotoList();
    	
    	int perPage = 20;
    	int current_page = 1;
    	
		while (flickr_search_resultset.size() < search_quota) {

			if (task.isCancelled())
				break;
			
			
			
			PhotoList pl = null;
			
			try {
				pl = photoInt.search(search_params, perPage, current_page);
				
				Log.d(TAG, "There are " + pl.getTotal() + " total hits across " + pl.getPages() + " pages with " + perPage + " per page.");
				if (current_page == 1)
					flickr_search_resultset.setTotal( pl.getTotal() );
//					Toast.makeText(ListActivityTaxonExtendedInfo.this, "Found " + pl.getTotal() + " matches.", Toast.LENGTH_SHORT).show();

				flickr_search_resultset.addAll( (List<Photo>) pl );
				
				if (pl.size() < perPage) break;
				
				current_page++;
				
				
			} catch (IOException e1) {
				e1.printStackTrace();
				break;
			} catch (SAXException e1) {
				e1.printStackTrace();
				break;
			} catch (FlickrException e1) {
				e1.printStackTrace();
				break;
			}

		}
		
		return flickr_search_resultset;
    }


	public static InputStream fetch(String url_string) throws UnknownHostException, MalformedURLException, IOException {
		
		/*
	   	DefaultHttpClient httpClient = new DefaultHttpClient();
	   	HttpGet request = new HttpGet(urlString);
	   	HttpResponse response = httpClient.execute(request);
	   	return response.getEntity().getContent();
	   	*/
		
		
		URL image_url = new URL(url_string);
		HttpURLConnection conn = (HttpURLConnection) image_url.openConnection();
		conn.setDoInput(true);
		conn.connect();

		BufferedInputStream buf_is = new BufferedInputStream(conn.getInputStream(), 4096);
		return buf_is;
	}
}
