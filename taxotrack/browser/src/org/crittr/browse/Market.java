package org.crittr.browse;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class Market {

	public static final String DEBUG_TAG = "CrittrBrowser";
	
    public static final String MARKET_PACKAGE_DETAILS_PREFIX = "market://details?id=";
    public static final String MARKET_AUTHOR_SEARCH_PREFIX = "market://search?q=";

	public static final String MARKET_AUTHOR_PREFIX = "pub:";
	public static final String MARKET_AUTHOR_NAME = "Karl Ostmo";
	public static final String MARKET_AUTHOR_SEARCH_STRING = MARKET_AUTHOR_SEARCH_PREFIX + MARKET_AUTHOR_PREFIX  + "\"" + MARKET_AUTHOR_NAME + "\"";
	
	
	public static final String FULL_VERSION_PACKAGE = "org.crittr.track";
	public static final String MARKET_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + FULL_VERSION_PACKAGE;

	

	public static final boolean SHOULD_ADVERTISE_PAID_VERSION = false;
	
	
	
	public static final int DEFAULT_PHOTOS_PER_PAGE = 25;
	

	public static final int THUMBNAIL_SIZE = 75;
	
	
	
	
    

	public static final String TAG = DEBUG_TAG;
    public static void intentLaunchMarketFallback(Activity context, String market_search, Intent intent, int return_code) {

    	Log.d(TAG, "Checking to see whether activity is available...");
		if (isIntentAvailable(context, intent)) {
			
			Log.i(TAG, "It is!");
			
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
    
    
	public static boolean isPackageInstalled(Context context, String package_name) {
	    final PackageManager packageManager = context.getPackageManager();
	    try {
	    	if (packageManager.getPackageInfo(package_name, 0) != null)
	    		return true;
	    } catch (NameNotFoundException e) {
	    	
	    }
	    return false;
	}

	public static boolean isIntentAvailable(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();
	    List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
	                    PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}
	
	
	// ================================================
	public static int getVersionCode(Context context, Class cls) {
		try {
			ComponentName comp = new ComponentName(context, cls);
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return -1;
		}
	}
}
