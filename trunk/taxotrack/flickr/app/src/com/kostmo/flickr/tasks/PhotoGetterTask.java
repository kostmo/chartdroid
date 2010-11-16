package com.kostmo.flickr.tasks;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.Photo;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.tools.SemaphoreHost;


public class PhotoGetterTask extends AsyncTask<Long, Void, Photo> {
	

	static final String TAG = Market.DEBUG_TAG;
	
	Context context;
	SemaphoreHost semaphore_host;
	protected PhotoGetterTask(Context context) {
		this.context = context;
		this.semaphore_host = (SemaphoreHost) context;
	}
	
	
	@Override
	public void onPreExecute() {
		this.semaphore_host.incSemaphore();
	}
	
	@Override
	protected Photo doInBackground(Long... photo_ids) {

		long photo_id = photo_ids[0];

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

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.READ);
        requestContext.setAuth(auth);
		


		Photo photo = null;
		try {
			photo = flickr.getPhotosInterface().getPhoto( Long.toString(photo_id) );
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (FlickrException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		}

		return photo;
	}

    @Override
    protected void onPostExecute(Photo photo) {
    	this.semaphore_host.decSemaphore();
    }
 }