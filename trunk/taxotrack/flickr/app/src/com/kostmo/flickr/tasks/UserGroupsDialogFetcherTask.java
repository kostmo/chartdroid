package com.kostmo.flickr.tasks;

import android.content.Context;
import android.widget.Toast;

import com.kostmo.flickr.activity.TabbedSearchActivity;

public class UserGroupsDialogFetcherTask extends UserGroupsFetcherTask {


	public UserGroupsDialogFetcherTask(Context c) {
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

    		
    		((TabbedSearchActivity) context).generateManagedGroupsDialog(name_list, id_list);
    	}
    }
}