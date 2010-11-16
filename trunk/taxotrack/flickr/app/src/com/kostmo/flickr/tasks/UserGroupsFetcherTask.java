package com.kostmo.flickr.tasks;

import java.io.IOException;
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
import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.tags.Tag;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.keys.ApiKeys;

public class UserGroupsFetcherTask extends AsyncTask<Void, Void, String> {


	public static String PREFKEY_DEFUALT_UPLOAD_GROUP = "default_upload_group";
	
	String photo_id;
	Tag old_tag;
	String new_tag;

	Flickr flickr = null;
	ProgressDialog wait_dialog;
	
	CharSequence[] id_list;
	CharSequence[] name_list;
	
	Context context;
	public UserGroupsFetcherTask(Context c) {
		context = c;
	}
	
	@Override
    public void onPreExecute() {

		wait_dialog = new ProgressDialog(context);
		wait_dialog.setMessage("Fetching groups...");
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
	protected String doInBackground(Void... new_tags) {
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.READ);
        requestContext.setAuth(auth);
		
		List<Group> groups_list = null;
		try {

			/*
			String user_nsid = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USER_NSID, null);
			Log.d(TAG, "my_id: " + user_nsid);


			PeopleInterface pi = flickr.getPeopleInterface();
			Log.d(TAG, "PeopleInterface: " + pi);
			

			groups_list = (ArrayList<Group>) pi.getPublicGroups( user_nsid );
			Log.d(TAG, "groups_list: " + groups_list);
			*/
			
			// NOTE: This is how to get invite-only groups!!
			groups_list = (List<Group>) flickr.getPoolsInterface().getGroups();

			id_list = new CharSequence[groups_list.size()];
			name_list = new CharSequence[groups_list.size()];
			
			for (int i=0; i < groups_list.size(); i++) {
				Group g = (Group) groups_list.get(i);
				
				id_list[i] = g.getId();
				name_list[i] = g.getName();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (SAXException e) {
			e.printStackTrace();
			return e.getMessage();
		} catch (FlickrException e) {
			e.printStackTrace();
			return e.getErrorMessage();
		}

		
		return null;
	}
	
    @Override
    public void onPostExecute(String error_message) {

    	wait_dialog.dismiss();
    }
}