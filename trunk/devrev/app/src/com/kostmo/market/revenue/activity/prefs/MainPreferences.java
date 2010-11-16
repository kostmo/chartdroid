package com.kostmo.market.revenue.activity.prefs;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.AppsOverviewActivity;
import com.kostmo.market.revenue.activity.ConsolidationActivity;
import com.kostmo.market.revenue.activity.NewCommentsActivity;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.service.CheckUpdateService;
import com.kostmo.market.revenue.task.PublisherPreferenceFetcherTask;

public class MainPreferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String TAG = "MainPreferences";
	
	public static String[] dialog_prefkey_list = {
		ConsolidationActivity.PREFKEY_SHOW_CONSOLIDATION_EDITOR_INSTRUCTIONS,
		AppsOverviewActivity.PREFKEY_SHOW_APPS_OVERVIEW_INSTRUCTIONS,
		RevenueActivity.PREFKEY_SHOW_REVENUE_ANALYSIS_INSTRUCTIONS,
		NewCommentsActivity.PREFKEY_SHOW_NEW_COMMENTS_INSTRUCTIONS
	};
	
	// TODO
	public static final String PREFKEY_ALTERNATE_DEVICE_ID = "alternate_device_id";
	
	public static final String PREFKEY_RESET_HELP_DIALOGS = "reset_help_dialogs";
	public static final String PREFKEY_WIPE_CACHE = "wipe_cache";
	public static final String PREFKEY_MAX_COMMENTS = "max_comments";
	public static final String PREFKEY_MAX_FETCH_RETRIES = "max_fetch_retries";
	
	public static final String PREFKEY_MAX_HTTP_THREADS = "max_http_threads";
	public static final int DEFAULT_MAX_HTTP_THREADS = 30;
	
	
	public static final String PREFKEY_PREFERRED_PUBLISHER = "preferred_publisher";
	
	

	public static final String PREFKEY_PREVIOUS_VERSION_CODE = "PREFKEY_PREVIOUS_VERSION_CODE";
	
	
	
	public static final int DEFAULT_MAX_TASK_RETRIES = 2;
	
	
	public static final int DIALOG_WIPE_CACHE = 1;
	
	SharedPreferences settings;
	List<String> publishers_list;
    boolean[] checked_cache_items_array = {true, true, true, true};
	
	// ========================================================================
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
//        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.main_settings);

        this.settings = PreferenceManager.getDefaultSharedPreferences(this);
        
		Preference reset_help_dialogs = findPreference(PREFKEY_RESET_HELP_DIALOGS);
		reset_help_dialogs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {

				for (String prefkey : dialog_prefkey_list)
					settings.edit().putBoolean(prefkey, false).commit();

				Toast.makeText(MainPreferences.this, R.string.pref_help_reset_toast, Toast.LENGTH_SHORT).show();
				return true;
			}
		});
        
        this.findPreference(PREFKEY_WIPE_CACHE).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_WIPE_CACHE);
				return true;
			}
        });
        

        this.settings.registerOnSharedPreferenceChangeListener(this);
        
		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.publishers_list = state.publishers_list;
			ListPreference list_preference = (ListPreference) findPreference(MainPreferences.PREFKEY_PREFERRED_PUBLISHER);
			PublisherPreferenceFetcherTask.assignItemsToListPreference(this.publishers_list, list_preference);

			this.checked_cache_items_array = state.checked_cache_items_array;
		} else {
			new PublisherPreferenceFetcherTaskExtended(this).execute();
		}
    }

	// ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
        case DIALOG_WIPE_CACHE:
        {
        	return new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.pref_title_wipe_cache)
//	        .setMessage(R.string.pref_dialog_wipe_cache_confirm)
	        .setMultiChoiceItems(R.array.clear_cache_options, checked_cache_items_array, new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Log.d(TAG, "Checked item " + which + "? " + isChecked);
					checked_cache_items_array[which] = isChecked;
				}
	        })
	        .setPositiveButton(R.string.alert_dialog_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					DatabaseRevenue database = new DatabaseRevenue(MainPreferences.this);
					
					List<String> candidate_tables = new ArrayList<String>();
					for (int i=0; i<checked_cache_items_array.length; i++) {
						switch(i) {
						case 0:
							candidate_tables.add(DatabaseRevenue.TABLE_CACHE_SPANS);
							candidate_tables.add(DatabaseRevenue.TABLE_GOOGLE_CHECKOUT_PURCHASES);
							break;
						case 1:
							candidate_tables.add(DatabaseRevenue.TABLE_GOOGLE_CHECKOUT_PRODUCTS);
							break;
						case 2:
							candidate_tables.add(DatabaseRevenue.TABLE_MARKET_APPS);
							break;
						case 3:
							candidate_tables.add(DatabaseRevenue.TABLE_MARKET_COMMENTS);
							break;
						}
					}
					
					int deletion_count = database.clearCache(candidate_tables.toArray(new String[0]));
					Toast.makeText(MainPreferences.this, "Deleted " + deletion_count + " records.", Toast.LENGTH_LONG).show();
				}
	        })
	        .setNegativeButton(R.string.alert_dialog_cancel, null)
	        .create();
        }
        default:
        	return super.onCreateDialog(id);
        }
    }

	// ========================================================================
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.e(TAG, "Preference changed");
		if (CheckUpdateService.PREFKEY_ENABLE_PERIODIC_CHECKIN.equals(key)) {
			
			Log.e(TAG, "Will set notifcation alarm...");
			CheckUpdateService.schedule(this);
		}
	}

	// ========================================================================
	class StateObject {
		List<String> publishers_list;
	    boolean[] checked_cache_items_array;
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {
		StateObject state = new StateObject();
		state.publishers_list = this.publishers_list;
		state.checked_cache_items_array = this.checked_cache_items_array;
		return state;
	}
	
	// ========================================================================
	// This class keeps a reference to the server list.
	class PublisherPreferenceFetcherTaskExtended extends PublisherPreferenceFetcherTask {
		public PublisherPreferenceFetcherTaskExtended(Context c) {super(c);}

	    @Override
	    public void onPostExecute(List<String> publishers) {
	    	super.onPostExecute(publishers);
	    	publishers_list = publishers;
	    }
	}
}
