package com.kostmo.flickr.tasks;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.aetrion.flickr.contacts.Contact;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;

public class UserContactsFetcherTask extends ContactsFetcherTask {

	CharSequence[] id_list;
	CharSequence[] name_list;
	
	public UserContactsFetcherTask(Context c) {
		super(c);
	}
	
	@Override
    public void onPreExecute() {
		super.onPreExecute();
		
		wait_dialog.setMessage("Fetching users...");
	}

    @Override
    public void onPostExecute(List<Contact> contacts) {
    	super.onPostExecute(contacts);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	String username = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USERNAME, null);
		String userid = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USER_NSID, null);


    	List<Contact> augmented_contacts_list = new ArrayList<Contact>();
    	if (userid != null && username != null) {
			Contact me = new Contact();
			me.setUsername(username);
			me.setId(userid);
			augmented_contacts_list.add(me);
    	}
    	augmented_contacts_list.addAll(contacts);

		
		id_list = new CharSequence[augmented_contacts_list.size()];
		name_list = new CharSequence[augmented_contacts_list.size()];
		
		for (int i=0; i < augmented_contacts_list.size(); i++) {
			Contact g = (Contact) augmented_contacts_list.get(i);
			
			id_list[i] = g.getId();
			name_list[i] = g.getUsername();
		}
    }
}