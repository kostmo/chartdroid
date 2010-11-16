package com.kostmo.market.revenue.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {

	// NOTE: We also schedule the service from within an activity,
	// because we want it to run even if the user has
	// not restarted his phone yet.
	
	public static final String PREFKEY_CHECKIN_ALARM_SCHEDULED = "PREFKEY_CHECKIN_ALARM_SCHEDULED";

	@Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            CheckUpdateService.schedule(context);
            
            SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
            mPreferences.edit().putBoolean(PREFKEY_CHECKIN_ALARM_SCHEDULED, true).commit();
        } else if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
        	
        	// NOTE: It seems that the alarm we have scheduled on BOOT_COMPLETED is
        	// canceled when we replace the package, so we have to schedule it again.
        	
        	
//        	Log.d(Meta.DEBUG_TAG, "Package has been replaced.");

        	boolean is_my_package = intent.getData().getSchemeSpecificPart().equals(context.getPackageName());
//        	Log.i(Meta.DEBUG_TAG, "Is this package mine? " + is_my_package);
        	
        	if (is_my_package) {
        		
                CheckUpdateService.schedule(context);
                
                SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
                mPreferences.edit().putBoolean(PREFKEY_CHECKIN_ALARM_SCHEDULED, true).commit();
        	}
        }
    }
}
