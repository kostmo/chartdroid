package org.crittr.track.activity;
import org.crittr.track.Market;
import org.crittr.track.R;
import org.crittr.track.R.drawable;
import org.crittr.track.R.xml;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Window;
import android.widget.Toast;
public class PrefsGlobal extends PreferenceActivity {


	public static String PREFKEY_DEFUALT_UPLOAD_GROUP = "default_upload_group";

	public static String[] dialog_prefkey_list = {
		Main.PREFKEY_SHOW_UPLOAD_INSTRUCTIONS,
		SightingsList.PREFKEY_SHOW_SIGHTINGS_INSTRUCTIONS,
	};



	static final String TAG = Market.DEBUG_TAG; 

	@Override
	public void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);


		// This has an awful, freaky effect
		//		   setTheme(R.style.CustomPrefsBackgroundTheme);

		addPreferencesFromResource( R.xml.prefs_global );

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
	}
}