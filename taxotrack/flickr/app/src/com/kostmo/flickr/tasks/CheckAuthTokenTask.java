package com.kostmo.flickr.tasks;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.AuthInterface;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.containers.TaskHostActivity;
import com.kostmo.flickr.keys.ApiKeys;


public class CheckAuthTokenTask extends AsyncTask<Void, Void, Boolean> {
	

	static final String TAG = Market.DEBUG_TAG;
	
	String error_string;
	boolean network_unavailable = false;
	
	TaskHostActivity host;
	public CheckAuthTokenTask(TaskHostActivity context) {
		updateActivity(context);
	}
	
	public void updateActivity(TaskHostActivity context) {
		this.host = context;
	}

    @Override
    public void onPreExecute() {
    	host.showToast("Verifying login...");
    }
	
	@Override
    protected Boolean doInBackground(Void... photos) {

    	Log.d(TAG, "Running checkToken()");
	    return checkToken();
	}

    @Override
    protected void onPostExecute(Boolean has_auth_token) {

    	Log.e(TAG, "has token? " + has_auth_token);

	    if (!has_auth_token && !network_unavailable) {
	    	
	    	// XXX It's bad to clear the token here; it could be simply that the internet connection
	    	// is temporarily gone.
	    	/*
	    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            settings.edit().putString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null).commit();
			*/
	    	host.launchAuthenticationActivity();

	    } else if (error_string != null) {
    		host.showErrorDialog(error_string);
	    }
    }

    // =============================================    
    boolean checkToken() {
        
        Flickr flickr = null;
        try {
    		flickr = new Flickr(
    				ApiKeys.FLICKR_API_KEY,	// My API key
    				ApiKeys.FLICKR_API_SECRET,	// My API secret
    		    new REST()
    		);
    	} catch (ParserConfigurationException e1) {
    		e1.printStackTrace();
    	}

    	AuthInterface authInterface = flickr.getAuthInterface();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(host.getContext());
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);

    	try {
			Auth auth = authInterface.checkToken(stored_auth_token);
			Log.d(TAG, "Logged in as " + auth.getUser().getRealName() + " (" + auth.getUser().getUsername() + "), NSID " + auth.getUser().getId());

			return true;
			
    	} catch (UnknownHostException e) {
    		
    		network_unavailable = true;
    		error_string = "Could not contact Flickr!";

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
			
			if (new Integer(FlickrAuthRetrievalActivity.FLICKR_CODE_INVALID_AUTH_TOKEN).toString().equals( e.getErrorCode() )) {
				error_string = "Auth token invalid!";
				Log.e(TAG, error_string);
			} else if (new Integer(FlickrAuthRetrievalActivity.FLICKR_CODE_INVALID_SIGNATURE).toString().equals( e.getErrorCode() )) {
				
				error_string = "Signature invalid!";
				Log.e(TAG, error_string);				
			} else {

				error_string = "Error in checkToken()";
				Log.e(TAG, error_string);
			}
			
			e.printStackTrace();
		}
		

		return false;
    }
}