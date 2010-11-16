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

public class PrefsSlideshow extends PreferenceActivity {


	public static final String PREFKEY_HIDE_TITLE = "hide_title";
	public static final String PREFKEY_FULL_SCREEN = "full_screen";
	
	   @Override
	   public void onCreate(Bundle savedInstanceState) {
		   getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		   super.onCreate(savedInstanceState);
		   getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

	       addPreferencesFromResource( R.xml.prefs_slideshow );

	       getListView().setCacheColorHint(0);
	       Drawable d = new NonScalingBackgroundDrawable(this, getListView(), -1);
	       d.setAlpha(0x20);	// mostly transparent
//	       d.setColorFilter(new PorterDuffColorFilter(Color.CYAN, PorterDuff.Mode.SRC_ATOP));
	       getListView().setBackgroundDrawable(d);
	       
	       
	       
			Preference prefs_link_uploads = findPreference("prefs_link_browsing");
			prefs_link_uploads.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					startActivity(new Intent(PrefsSlideshow.this, PrefsBrowsing.class));
					return true;
				}
			});
	       
	       
	   }
}
