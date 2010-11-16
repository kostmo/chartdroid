package org.crittr.browse;

import org.crittr.shared.browser.provider.DatabaseTaxonomy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class StorageLowReceiver extends BroadcastReceiver{
    

    @Override 
	public void onReceive(Context c, Intent i) {
    	
    	// FIXME - This is untested.
    	
    	// We clear the cached taxons.
    	DatabaseTaxonomy helper = new DatabaseTaxonomy(c);
    	helper.clear_cached_taxons();
    	
    	// Next clear the browser cache.
    	
    	
    	/*
        NotificationManager nm = (NotificationManager)
        context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notifyWithText(R.id.alarm,
                          "Alarm!!!",
                          NotificationManager.LENGTH_SHORT,
                          null);
    	 */
	}
}   