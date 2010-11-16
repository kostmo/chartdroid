package com.kostmo.market.revenue.task;

import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.SystemClock;

import com.gc.android.market.api.MarketFetcher;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.provider.DatabaseRevenue;

public abstract class AppIconAdapterFetcherTask extends AsyncTask<Void, Void, Bitmap> {

	static final String TAG = Market.TAG;

	protected Context context;

	String error_message;
	protected DatabaseRevenue database;

	long start_milliseconds;
	protected long app_id;
	
	// ========================================================================
	public AppIconAdapterFetcherTask(Context context, long app_id) {
		this.context = context;
		this.database = new DatabaseRevenue(this.context);
		this.app_id = app_id;
	}

	// ========================================================================
	@Override
	protected void onPreExecute() {
		this.start_milliseconds = SystemClock.uptimeMillis();
	}

	// ========================================================================
	@Override
	protected Bitmap doInBackground(Void... voided) {
		
		try {
			return MarketFetcher.getAppIcon(MarketFetcher.obtainAndroidCredentials((Activity) this.context), this.app_id);

		} catch (UnknownHostException e) {

			e.printStackTrace();
			this.error_message = "Unknown host: " + e.getMessage();

		} catch (IOException e) {
			e.printStackTrace();
			this.error_message = e.getMessage();
		}

		return null;
	}

	// ========================================================================
	@Override
	protected void onCancelled() {
		cleanUp();
	}

	// ========================================================================
	@Override
	public void onPostExecute(Bitmap bitmap) {

		cleanUp();

		if (this.error_message != null) {
			failTask(this.error_message);
		} else {
			completeTask(bitmap);
		}
	}

	// ========================================================================
	abstract protected void completeTask(Bitmap bitmap);

	// ========================================================================
	protected void cleanUp() {
	}

	// ========================================================================
	abstract protected void failTask(String error_message);

	// ========================================================================
	protected float getElapsedSeconds() {
		long end_milliseconds = SystemClock.uptimeMillis();
		float seconds = (end_milliseconds - this.start_milliseconds)/1000f;
		return seconds;
	}
}