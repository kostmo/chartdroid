package org.crittr.browse.activity.prefs;

import org.crittr.browse.R;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Window;
import android.widget.Toast;
public class PrefsTaxonNavigator extends PreferenceActivity {

	

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);

		addPreferencesFromResource( R.xml.prefs_taxon_navigator );


		/*
	       DialogPreference clear_cached_taxons = (DialogPreference) findPreference("clear_cached_taxons");
	       clear_cached_taxons.getDialog().setOnDismissListener(new OnDismissListener() {
		 */

		Preference clear_cached_taxons = (Preference) findPreference("clear_cached_taxons");
		clear_cached_taxons.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				AlertDialog d = new AlertDialog.Builder(PrefsTaxonNavigator.this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("Clear cache")
				.setMessage("Clear taxon cache?")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						DatabaseTaxonomy helper = new DatabaseTaxonomy(PrefsTaxonNavigator.this);
						helper.clear_cached_taxons();

						Toast.makeText(PrefsTaxonNavigator.this, "Cache cleared.", Toast.LENGTH_SHORT).show();
					}

				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

					}
				})
				.create();
				d.show();

				return true;
			}
		});


		/*	       
	        PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
	        inlinePrefCat.setTitle("Taxon Cache");
	        getPreferenceScreen().addPreference(inlinePrefCat);

	        // Toggle preference
	        CustomDialogPreference togglePref = new CustomDialogPreference(this, Resources.Theme.obtainStyledAttributes(android.R.style.Theme, new int[] {}));
	        togglePref.setKey("toggle_preference");
	        togglePref.setTitle(R.string.title_toggle_preference);
	        togglePref.setSummary(R.string.summary_toggle_preference);
	        inlinePrefCat.addPreference(togglePref);
		 */
	}


	/*
	   public class CustomDialogPreference extends DialogPreference
	   {

		public CustomDialogPreference(Context context, AttributeSet attrs) {
			super(context, attrs);
			// TODO Auto-generated constructor stub
		}

		 protected void onDialogClosed(boolean positiveResult) {


		 }

	   }

	 */
}
