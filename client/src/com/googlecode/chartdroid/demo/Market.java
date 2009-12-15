package com.googlecode.chartdroid.demo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class Market {
    
    
    public static final int NO_RESULT = -1;

    static final String TAG = "ChartDroid";
    
    
    public static final String MARKET_AUTHOR_SEARCH_PREFIX = "pub:";
    public static final String MARKET_AUTHOR_NAME = "Karl Ostmo";
    public static final String MARKET_AUTHOR_SEARCH_STRING = MARKET_AUTHOR_SEARCH_PREFIX + "\"" + MARKET_AUTHOR_NAME + "\"";
    
    public static final String MARKET_PACKAGE_SEARCH_PREFIX = "pname:";
    public static final String MARKET_PACKAGE_NAME = "com.googlecode.chartdroid";
    public static final String MARKET_PACKAGE_SEARCH_STRING = MARKET_PACKAGE_SEARCH_PREFIX + MARKET_PACKAGE_NAME;

    
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
            Uri market_uri = Uri.parse("market://search?q=" + market_search);
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
}
