package org.crittr.shared.tracker.provider;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.crittr.browse.Market;
import org.crittr.task.NetworkUnavailableException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;


public class AppEngineDataFrontend {


	final static String TAG = Market.DEBUG_TAG;
	

    public static Map<Long, Integer> taxon_popularity(Context context, String query_argument) throws UnknownHostException, NetworkUnavailableException {
    	Map<Long, Integer> popularity_hash = new HashMap<Long, Integer>();
    	
		String[] projection = new String[] {
				AppEngineData.TaxoData.COLUMN_TSN,
				AppEngineData.TaxoData.COLUMN_TAXON_HITS
				};
		
 		Uri mySuggestion = AppEngineData.TaxoData.TAXON_POPULARITY_URI;
 		Cursor c = ((Activity) context).managedQuery(
 				mySuggestion,
 				projection,
 				query_argument, null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
 		while (c.moveToNext()) {
 			popularity_hash.put(c.getLong(0), c.getInt(1));
 		}
 		c.close();
 		
                
        return popularity_hash;
    }

}

