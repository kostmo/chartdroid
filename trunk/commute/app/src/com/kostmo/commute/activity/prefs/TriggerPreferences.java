package com.kostmo.commute.activity.prefs;

import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kostmo.commute.R;
import com.kostmo.tools.DurationStrings.TimescaleTier;

public class TriggerPreferences extends PreferenceActivity {

	public static final String TAG = "TriggerPreferences";
	
	
	public static final String PREFKEY_ENABLE_RECORD_BREADCRUMBS = "enable_record_breadcrumbs";
	public static final boolean DEFAULT_ENABLE_RECORD_BREADCRUMBS = false;
	
	public static final String PREFKEY_ENABLE_WIFI_TRIGGER = "enable_wifi_trigger";
	public static final boolean DEFAULT_ENABLE_WIFI_TRIGGER = false;
	

	public static final String PREFKEY_TRIP_COMPLETION_RADIUS = "trip_completion_radius";
	public static final float DEFAULT_TRIP_COMPLETION_RADIUS = 200;	// meters


	public static final String PREFKEY_TRIP_EXPIRATION_MS = "trip_expiration_ms";
	public static final long DEFAULT_TRIP_EXPIRATION_MS = TimescaleTier.HOURS.millis*2;	// Two hours max
	

	public static final String PREFKEY_LOCATION_SOURCE = "location_source";
	public static final String DEFAULT_LOCATION_SOURCE = LocationManager.GPS_PROVIDER;
	
	
	SharedPreferences settings;
	
	// ========================================================================
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
//        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.settings_triggers);

        this.settings = PreferenceManager.getDefaultSharedPreferences(this);
        
    }
}
