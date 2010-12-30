package com.kostmo.commute.activity.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kostmo.commute.R;

public class TriggerPreferences extends PreferenceActivity {

	public static final String TAG = "TriggerPreferences";
	
	
	public static final String PREFKEY_ENABLE_RECORD_BREADCRUMBS = "enable_record_breadcrumbs";
	public static final boolean DEFAULT_ENABLE_RECORD_BREADCRUMBS = false;
	
	public static final String PREFKEY_ENABLE_WIFI_TRIGGER = "enable_wifi_trigger";
	public static final boolean DEFAULT_ENABLE_WIFI_TRIGGER = false;
	
	
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
