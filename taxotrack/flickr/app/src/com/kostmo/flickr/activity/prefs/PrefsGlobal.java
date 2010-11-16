package com.kostmo.flickr.activity.prefs;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.kostmo.flickr.activity.ListActivityPhotoTags;
import com.kostmo.flickr.activity.LiveSlideshowActivity;
import com.kostmo.flickr.activity.Main;
import com.kostmo.flickr.activity.PhotoListActivity;
import com.kostmo.flickr.activity.PhotoMap;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.graphics.NonScalingBackgroundDrawable;
public class PrefsGlobal extends PreferenceActivity {

	static final String TAG = Market.DEBUG_TAG; 


	public static String[] dialog_prefkey_list = {
		Main.PREFKEY_SHOW_UPLOAD_INSTRUCTIONS,
		PhotoListActivity.PREFKEY_SHOW_PHOTOLIST_INSTRUCTIONS,
		PhotoListActivity.PREFKEY_SHOW_PHOTOLIST_INSTRUCTIONS,
		PhotoMap.PREFKEY_MAP_INSTRUCTIONS,
		ListActivityPhotoTags.PREFKEY_SHOW_TAGGING_INSTRUCTIONS,
		LiveSlideshowActivity.PREFKEY_SHOW_SLIDESHOW_INSTRUCTIONS
	};


	public static final String PREFKEY_PREVIOUS_VERSION_CODE = "PREFKEY_PREVIOUS_VERSION_CODE";


	@Override
	public void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);


		// This has an awful, freaky effect
		//		   setTheme(R.style.CustomPrefsBackgroundTheme);

		addPreferencesFromResource( R.xml.prefs_global );

		getListView().setCacheColorHint(0);	// Required for custom background images


		Preference default_upload_group = findPreference("reset_help_dialogs");
		default_upload_group.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {

				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PrefsGlobal.this);

				for (String prefkey : dialog_prefkey_list)
					settings.edit().putBoolean(prefkey, false).commit();


				Toast.makeText(PrefsGlobal.this, "Dialogs reset.", Toast.LENGTH_SHORT).show();
				return true;
			}
		});


		Preference prefs_link_uploads = findPreference("prefs_link_uploads");
		prefs_link_uploads.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(PrefsGlobal.this, PrefsUpload.class));
				return true;
			}
		});


		// Method 1:
		int resources_based_orientation = getResources().getConfiguration().orientation;
		Log.d(TAG, "Resources-based orientation: " + resources_based_orientation);

		Log.d(TAG, "'Configuration' Portrait constant: " + Configuration.ORIENTATION_PORTRAIT);
		Log.d(TAG, "'Configuration' Landscape constant: " + Configuration.ORIENTATION_LANDSCAPE);

		// Method 2:
		int display_based_orientation = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
		Log.d(TAG, "Display-based orientation: " + display_based_orientation);



		// Method 3:
		int activity_based_orientation = getRequestedOrientation();
		Log.d(TAG, "Activity-based orientation: " + activity_based_orientation);

		Log.d(TAG, "'ActivityInfo' Portrait constant: " + ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Log.d(TAG, "'ActivityInfo' Landscape constant: " + ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


		// Approach 1: Use static images made to fit:
		/*
	       if (resources_based_orientation == Configuration.ORIENTATION_PORTRAIT)
	    	   getListView().setBackgroundResource(R.drawable.seahorse_vertical);
	       else
	    	   getListView().setBackgroundResource(R.drawable.seahorse_horizontal);
		 */




		// Approach 2: Use a custom drawable

		getListView().setCacheColorHint(0);	// Required for custom background images


		Drawable d = new NonScalingBackgroundDrawable(this, getListView(), -1);
		d.setAlpha(0x20);	// mostly transparent
		getListView().setBackgroundDrawable(d);

		//	       Log.w("Crittr", "Listview height in onCreate(): " + getListView().getHeight());
	}


	@Override
	public void onStart() {
		super.onStart();
		//	       Log.w("Crittr", "Listview height in onStart(): " + getListView().getHeight());
	}

	@Override
	public void onPause() {
		super.onPause();
		/*
	       Log.i("Crittr", "Listview height in onPause(): " + getListView().getHeight());
	       Log.i("Crittr", "Listview width in onPause(): " + getListView().getWidth());
		 */
	}

}

