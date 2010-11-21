package com.kostmo.flickr.bettr;

import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class Market {

	public static final String DEBUG_TAG = "OpenFlickr";

	
    // Color Picker constants
	public final static String EXTRA_COLOR = "org.openintents.extra.COLOR";
	public final static String ACTION_PICK_COLOR = "org.openintents.action.PICK_COLOR";
	public final static String PACKAGE_NAME_COLOR_PICKER = "org.openintents.colorpicker";
	public final static String COLOR_PICKER_WEBSITE = "http://www.openintents.org/en/colorpicker";
//	public final static Uri APK_DOWNLOAD_URI_COLOR_PICKER = Uri.parse(COLOR_PICKER_WEBSITE);

	public final static String APK_DOWNLOAD_URL_PREFIX = "http://openintents.googlecode.com/files/";
	public final static String APK_APP_NAME = "ColorPicker";
	public final static String APK_VERSION_NAME = "1.1.0";
	public final static Uri APK_DOWNLOAD_URI = Uri.parse(APK_DOWNLOAD_URL_PREFIX + APK_APP_NAME + "-" + APK_VERSION_NAME + ".apk");
	
    
	

	

    public static final String MARKET_PACKAGE_DETAILS_PREFIX = "market://details?id=";
    public static final String MARKET_AUTHOR_SEARCH_PREFIX = "market://search?q=";
    
    public static final String MARKET_AUTHOR_PREFIX = "pub:";
    public static final String MARKET_AUTHOR_NAME = "Karl Ostmo";
    public static final String MARKET_AUTHOR_SEARCH_STRING = MARKET_AUTHOR_SEARCH_PREFIX + MARKET_AUTHOR_PREFIX  + "\"" + MARKET_AUTHOR_NAME + "\"";
    

	
	
	
	public static final String CHARTDROID_PACKAGE = "com.googlecode.chartdroid";
	public static final String MARKET_CHARTDROID_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + CHARTDROID_PACKAGE;

	public static final String FLICKR_PACKAGE = "com.kostmo.flickr.bettr";
	public static final String MARKET_FLICKR_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + FLICKR_PACKAGE;

	public static final String FILE_NAVIGATOR_PACKAGE = "lysesoft.andexplorer";
	public static final String FILE_NAVIGATOR_PACKAGE_SEARCH = MARKET_PACKAGE_DETAILS_PREFIX + FILE_NAVIGATOR_PACKAGE;

	

	
	public static final int DEFAULT_PHOTOS_PER_PAGE = 25;
	
	public static final String TAG = Market.DEBUG_TAG; 
	
    public static void intentLaunchMarketFallback(Activity context, String market_search, Intent intent, int request_code) {

    	Log.d(TAG, "Checking to see whether activity is available...");
		if (isIntentAvailable(context, intent)) {
			
			Log.i(TAG, "It is!");
			
			if (request_code < 0)
				context.startActivity(intent);
			else
				context.startActivityForResult(intent, request_code);
		} else {
			
			Log.e(TAG, "It is not.");
			
	    	// Launch market intent
	    	Uri market_uri = Uri.parse(market_search);
	    	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
	    	try {
	    		context.startActivity(i);
	    		Toast.makeText(context, "This app is required.", Toast.LENGTH_LONG).show();
	    	} catch (ActivityNotFoundException e) {
	            Toast.makeText(context, "Android Market not available.", Toast.LENGTH_LONG).show();
	    	}
    	}
    }

    // ================================================
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
