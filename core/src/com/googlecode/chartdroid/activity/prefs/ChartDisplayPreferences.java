package com.googlecode.chartdroid.activity.prefs;

import com.googlecode.chartdroid.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ChartDisplayPreferences extends PreferenceActivity {

	public final static String PREFKEY_BAR_SHADING = "bar_shading";

	public final static String SHARED_PREFS_NAME = "chart_display_prefs";
	
	// ========================================================================
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
//        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.chart_display_settings);
    }
}
