package com.kostmo.market.revenue.task;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.gc.android.market.api.MarketFetcher;
import com.gc.android.market.api.model.Market.App;
import com.gc.android.market.api.model.Market.AppsRequest.ViewType;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.container.ProgressPacket;
import com.kostmo.market.revenue.provider.DatabaseRevenue;

public abstract class AppSyncTask extends AsyncTask<Void, ProgressPacket, List<App>> {

	static final String TAG = Market.TAG;


	protected ProgressHostActivity host;
	String error_message;
	protected DatabaseRevenue database;

	long start_milliseconds;
	String publisher;
	ViewType view_type;
	
	// ========================================================================
	public AppSyncTask(DatabaseRevenue database, String publisher, ViewType view_type) {
		this.database = database;
		this.publisher = publisher;
		this.view_type = view_type;
	}

	// ====================================================================
	public void updateActivity(ProgressHostActivity activity) {
		this.host = activity;
	}
	
	// ========================================================================
	@Override
	protected void onPreExecute() {
		this.start_milliseconds = SystemClock.uptimeMillis();
	}

	// ========================================================================
	@Override
	protected List<App> doInBackground(Void... voided) {
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(host.getActivity());
		
		// Tries again exactly once with the hardcoded Android ID, if it didn't work the first time.
		while (true) {
			try {
	
				// Upon success, we break out of the loop.
				return MarketFetcher.getPublisherApps(MarketFetcher.obtainAndroidCredentials(host.getActivity()), this.publisher, this.view_type);

			} catch (UnknownHostException e) {

				e.printStackTrace();
				this.error_message = "Unknown host: " + e.getMessage();
				break;

			} catch (IOException e) {
				
				e.printStackTrace();

				String preliminary_error_message = e.getMessage();
				
				if (
					!preferences.getBoolean(MarketFetcher.PREFKEY_PREFER_HARDCODED_ANDROID_ID, false)
					&& preliminary_error_message != null
					&& preliminary_error_message.contains(MarketFetcher.INVALID_ANDROID_ID_ERROR_CLUE)) {
					
						preferences.edit().putBoolean(MarketFetcher.PREFKEY_PREFER_HARDCODED_ANDROID_ID, true).commit();
						
				} else {

					this.error_message = preliminary_error_message;
					break;
				}
			}
		};
		
		return null;
	}

	// ========================================================================
	@Override
	protected void onCancelled() {
		cleanUp();
	}

	// ========================================================================
	@Override
	public void onPostExecute(List<App> apps) {
		
		cleanUp();

		if (this.error_message != null) {
			failTask(this.error_message);
		} else {
			completeTask(apps);
		}
	}

	// ========================================================================
	protected void completeTask(List<App> apps) {
		this.database.populateMarketApps(apps);
	}

	// ========================================================================
	protected void cleanUp() {}

	// ========================================================================
	protected void failTask(String non_null_error_message) {
		Toast.makeText(this.host.getActivity(), non_null_error_message, Toast.LENGTH_LONG).show();
	}

	// ========================================================================
	protected float getElapsedSeconds() {
		long end_milliseconds = SystemClock.uptimeMillis();
		float seconds = (end_milliseconds - this.start_milliseconds)/1000f;
		return seconds;
	}
}