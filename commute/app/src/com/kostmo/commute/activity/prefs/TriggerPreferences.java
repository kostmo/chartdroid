package com.kostmo.commute.activity.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kostmo.commute.R;

public class TriggerPreferences extends PreferenceActivity {

	public static final String TAG = "TriggerPreferences";
	
	
	// TODO
	public static final String PREFKEY_ALTERNATE_DEVICE_ID = "alternate_device_id";
	

	public static final int DEFAULT_MAX_HTTP_THREADS = 30;
	
	
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
