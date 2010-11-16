package com.kostmo.flickr.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.ProgressDialog;
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
import com.aetrion.flickr.contacts.Contact;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.keys.ApiKeys;

public class ContactsFetcherTask extends AsyncTask<Void, Void, List<Contact>> {

	public static String PREFKEY_DEFUALT_UPLOAD_GROUP = "default_upload_group";
	

	Flickr flickr = null;
	ProgressDialog wait_dialog;
	
	protected String error_message;
	
	protected Context context;
	public ContactsFetcherTask(Context c) {
		context = c;
	}
	
	@Override
    public void onPreExecute() {

		wait_dialog = new ProgressDialog(context);
		wait_dialog.setMessage("Fetching contacts...");
		wait_dialog.setIndeterminate(true);
		wait_dialog.setCancelable(false);
		wait_dialog.show();
		
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected List<Contact> doInBackground(Void... new_tags) {
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.READ);
        requestContext.setAuth(auth);
		

		try {

			List<Contact> contacts_list = (List<Contact>) flickr.getContactsInterface().getList();
			return contacts_list;
			
		} catch (IOException e) {
			error_message = e.getLocalizedMessage();
			e.printStackTrace();
		} catch (SAXException e) {
			error_message = e.getLocalizedMessage();
			e.printStackTrace();
		} catch (FlickrException e) {
			error_message = e.getLocalizedMessage();
			e.printStackTrace();
		}

		
		return new ArrayList<Contact>();
	}
	
    @Override
    public void onPostExecute(List<Contact> contacts) {

    	wait_dialog.dismiss();
    }
}