package com.kostmo.flickr.activity.prefs;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Window;

import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.graphics.NonScalingBackgroundDrawable;

public class PrefsBrowsing extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

		addPreferencesFromResource( R.xml.prefs_browsing );

		getListView().setCacheColorHint(0);
		Drawable d = new NonScalingBackgroundDrawable(this, getListView(), -1);
		d.setAlpha(0x20);	// mostly transparent
//		d.setColorFilter(new PorterDuffColorFilter(Color.CYAN, PorterDuff.Mode.SRC_ATOP));
		getListView().setBackgroundDrawable(d);


		Preference prefs_link_uploads = findPreference("prefs_link_search");
		prefs_link_uploads.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(PrefsBrowsing.this, PrefsSearchOptions.class));
				return true;
			}
		});

		Preference prefs_link_slideshow = findPreference("prefs_link_slideshow");
		prefs_link_slideshow.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(PrefsBrowsing.this, PrefsSlideshow.class));
				return true;
			}
		});
	}
}
