package com.kostmo.flickr.tasks;

import java.util.List;

import android.content.Context;
import android.widget.Toast;

import com.aetrion.flickr.contacts.Contact;
import com.kostmo.flickr.containers.UserListClient;

public class UserContactsDialogFetcherTask extends UserContactsFetcherTask {


	public UserContactsDialogFetcherTask(Context c) {
		super(c);
	}

    @Override
    public void onPostExecute(List<Contact> contacts) {
    	super.onPostExecute(contacts);
    	
    	if (error_message != null) {
    		String err = "Users not loaded; " + error_message;
			Toast.makeText(context, err, Toast.LENGTH_SHORT).show();
    	} else {
    		((UserListClient) context).generateManagedUsersDialog(name_list, id_list);
    	}
    }
}