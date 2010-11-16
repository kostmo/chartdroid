package com.kostmo.market.revenue.task;

import java.util.List;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.kostmo.market.revenue.activity.prefs.MainPreferences;

public class PublisherPreferenceFetcherTask extends PublishersFetcherTask {

	public PublisherPreferenceFetcherTask(Context c) {super(c);}

	// ========================================================================
	public static void assignItemsToListPreference(List<String> items, ListPreference list_preference) {

		String[] server_hostnames = new String[items.size()];
		
		int i=0;
		for (String server : items) {
			server_hostnames[i] = server;
			i++;
		}
		
		list_preference.setEntries(server_hostnames);
		list_preference.setEntryValues(server_hostnames);
	}
	

	// ========================================================================
    @Override
    public void onPostExecute(List<String> items) {
    	super.onPostExecute(items);
    	
    	if (items == null) {
    		String err = "Servers not loaded.";
			Toast.makeText(context, err, Toast.LENGTH_SHORT).show();
    	} else {

			ListPreference list_preference = (ListPreference) ((PreferenceActivity) context).findPreference(MainPreferences.PREFKEY_PREFERRED_PUBLISHER);
			assignItemsToListPreference(items, list_preference);
    	}
    }
}