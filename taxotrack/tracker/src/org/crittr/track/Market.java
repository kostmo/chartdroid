package org.crittr.track;

import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class Market {

	public static final String DEBUG_TAG = "TaxoTrack";

	
	public final static String PACKAGE_NAME_CALENDAR_PICKER = "org.openintents.calendarpicker";
	public final static String CALENDAR_PICKER_WEBSITE = "http://www.openintents.org/en/calendarpicker";
	public final static Uri APK_DOWNLOAD_URI_CALENDAR_PICKER = Uri.parse(CALENDAR_PICKER_WEBSITE);

	
	
	
    public static final String MARKET_PACKAGE_DETAILS_PREFIX = "market://details?id=";
    public static final String MARKET_AUTHOR_SEARCH_PREFIX = "market://search?q=";
    
    
    public static final String MARKET_AUTHOR_PREFIX = "pub:";
    public static final String MARKET_AUTHOR_NAME = "Karl Ostmo";
    public static final String MARKET_AUTHOR_SEARCH_STRING = MARKET_AUTHOR_SEARCH_PREFIX + MARKET_AUTHOR_PREFIX  + "\"" + MARKET_AUTHOR_NAME + "\"";
    

	
	public static final String CRITTR_BROWSER_PACKAGE = "org.crittr.browse";
	public static final String FLICKR_PACKAGE = "com.kostmo.flickr.bettr";
	public static final String FILE_MANAGER_PACKAGE = "org.openintents.filemanager";
	public static final String CHARTDROID_PACKAGE = "com.googlecode.chartdroid";
	
	
	
	

	
	
	public static final String MARKET_CRITTR_BROWSER_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + CRITTR_BROWSER_PACKAGE;
	public static final String MARKET_FILE_MANAGER_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + FILE_MANAGER_PACKAGE;
	public static final String MARKET_FLICKR_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + FLICKR_PACKAGE;
	public static final String MARKET_CHARTDROID_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + CHARTDROID_PACKAGE;

	
	public static final int DEFAULT_PHOTOS_PER_PAGE = 25;
	

	public static final int THUMBNAIL_SIZE = 75;
	
	

	static final String TAG = Market.DEBUG_TAG; 
	
    public static void intentLaunchMarketFallback(Activity context, String market_search, Intent intent, int return_code) {

    	Log.d(TAG, "Checking to see whether activity is available...");
		if (isIntentAvailable(context, intent)) {
			
			Log.i(TAG, "It is!");
			
			if (return_code < 0)
				context.startActivity(intent);
			else
				context.startActivityForResult(intent, return_code);
		} else {
			
			Log.e(TAG, "It is not.");
			
	    	// Launch market intent
	    	Uri market_uri = Uri.parse(market_search);
	    	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
	    	try {
	    		context.startActivity(i);
	    	} catch (ActivityNotFoundException e) {
	            Toast.makeText(context, "Android Market not available.", Toast.LENGTH_LONG).show();
	    	}
    	}
    }


	public static boolean isIntentAvailable(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();
	    List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
	                    PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}
	
	// ================================================
	public static Intent getMarketDownloadIntent(String package_name) {
		Uri market_uri = Uri.parse(MARKET_PACKAGE_DETAILS_PREFIX + package_name);
		return new Intent(Intent.ACTION_VIEW, market_uri);
	}
}
