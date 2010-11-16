package com.kostmo.flickr.tasks;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class UserGroupsPreferenceFetcherTask extends UserGroupsFetcherTask {


	public UserGroupsPreferenceFetcherTask(Context c) {
		super(c);
		context = c;
	}

    @Override
    public void onPostExecute(String error_message) {
    	super.onPostExecute(error_message);
    	
    	if (error_message != null) {
    		String err = "Groups not loaded; " + error_message;
			Toast.makeText(context, err, Toast.LENGTH_SHORT).show();
    	} else {

			ListPreference default_upload_group = (ListPreference) ((PreferenceActivity) context).findPreference(PREFKEY_DEFUALT_UPLOAD_GROUP);
			
			default_upload_group.setEntries(name_list);
			default_upload_group.setEntryValues(id_list);
    	}
    }
}