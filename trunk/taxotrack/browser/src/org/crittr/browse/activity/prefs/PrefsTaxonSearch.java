package org.crittr.browse.activity.prefs;


import org.crittr.browse.R;
import org.crittr.browse.R.drawable;
import org.crittr.browse.R.xml;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;
public class PrefsTaxonSearch extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);

		addPreferencesFromResource( R.xml.prefs_taxon_search );
	}
}
