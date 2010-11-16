package com.kostmo.flickr.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.Size;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.keys.ApiKeys;


public class ImageSizesGetterTask extends AsyncTask<Photo, Void, Collection<Size>> {

	static final String TAG = Market.DEBUG_TAG;
	
	protected Context context;
	protected Photo photo;
	protected String error_message;
	protected ImageSizesGetterTask(Context context) {
		this.context = context;
	}
	
	protected ProgressDialog wait_dialog;
	
	void instantiate_latent_wait_dialog() {

		this.wait_dialog = new ProgressDialog(context);
		this.wait_dialog.setMessage("Fetching sizes...");
		this.wait_dialog.setIndeterminate(true);
		this.wait_dialog.setCancelable(false);
		this.wait_dialog.show();
	}

    @Override
    public void onPreExecute() {
		instantiate_latent_wait_dialog();
    }
	
	@Override
    protected Collection<Size> doInBackground(Photo... photos) {
		this.photo = photos[0];
		if (this.photo == null) {
			Log.e(TAG, "Was passed a null photo...");
			return new ArrayList<Size>();
		}
			

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
		
		
		Auth auth = new Auth();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
		auth.setPermission(Permission.READ);
		
		RequestContext requestContext = RequestContext.getRequestContext();
		requestContext.setAuth(auth);
		
		PhotosInterface photoInt = flickr.getPhotosInterface();
		try {

			Log.d(TAG, "Getting sizes for photo: " + this.photo.getId());
			
			return (Collection<Size>) photoInt.getSizes( this.photo.getId() );

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
			this.error_message = e.getLocalizedMessage();
			e.printStackTrace();
		}

		return new ArrayList<Size>();
	}

    @Override
    protected void onPostExecute(Collection<Size> sizes) {
    	this.wait_dialog.dismiss();
    	if (this.error_message != null)
    		Toast.makeText(this.context, this.error_message, Toast.LENGTH_LONG).show();
    }
 }