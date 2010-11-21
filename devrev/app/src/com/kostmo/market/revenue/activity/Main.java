package com.kostmo.market.revenue.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.tools.StreamUtils;

// ============================================================================
public class Main extends Activity {

	public static final String TAG = Market.TAG;

	public static final int DIALOG_INSTALL_CHARTDROID = 1;
	final int DIALOG_RELEASE_NOTES = 2;
	
	SharedPreferences settings;

	// ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle(R.string.app_name_full);
		setContentView(R.layout.main);

		this.settings = PreferenceManager.getDefaultSharedPreferences(this);

		findViewById(R.id.button_ratings).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Main.this, AppsOverviewActivity.class));
			}
		});

		findViewById(R.id.button_revenue).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Main.this, RevenueActivity.class));
			}
		});
		
		Intent chardroid_dummy_intent = new Intent(Intent.ACTION_VIEW);
		chardroid_dummy_intent.setType(ColumnSchema.EventData.CONTENT_TYPE_PLOT_DATA);
		if (!Market.isIntentAvailable(this, chardroid_dummy_intent) ) {
			showDialog(DIALOG_INSTALL_CHARTDROID);
		}
		
		
		if (savedInstanceState == null) {
			int current_version_code = Market.getVersionCode(this, Main.class);
			if (current_version_code > settings.getInt(MainPreferences.PREFKEY_PREVIOUS_VERSION_CODE, -1)) {
				settings.edit().putInt(MainPreferences.PREFKEY_PREVIOUS_VERSION_CODE, current_version_code).commit();
				showDialog(DIALOG_RELEASE_NOTES);
			}
		}
	}

	// ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

//		LayoutInflater factory = LayoutInflater.from(this);

		switch (id) {
		case DIALOG_RELEASE_NOTES:
		{
			CharSequence release_notes = StreamUtils.readFile(this, R.raw.release_notes);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.release_notes)
			.setMessage(release_notes)
			.setPositiveButton(R.string.alert_dialog_ok, null)
			.create();
		}
		case DIALOG_INSTALL_CHARTDROID:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.chartdroid_needed_dialog_title)
			.setMessage(R.string.chartdroid_needed_dialog_message)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Market.launchMarketSearch(Main.this, Market.MARKET_CHARTDROID_DETAILS_STRING);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_main, menu);
		return true;
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case R.id.menu_about:
        {
			Uri website_destination = Uri.parse( Market.WEBSITE_URL );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, website_destination));

            return true;
        }
		case R.id.menu_more_apps:
		{
			Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
			Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
			startActivity(i);
			return true;
		}
		case R.id.menu_view_cache:
		{
			startActivity(new Intent(this, CacheActivity.class));
			return true;
		}
		case R.id.menu_preferences:
		{
			startActivity(new Intent(this, MainPreferences.class));
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}
}