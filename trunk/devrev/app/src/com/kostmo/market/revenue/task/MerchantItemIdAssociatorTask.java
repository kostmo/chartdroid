package com.kostmo.market.revenue.task;

import java.io.IOException;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.container.ProgressPacket;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.GoogleCheckoutException;
import com.kostmo.tools.DurationStrings;

public abstract class MerchantItemIdAssociatorTask extends AsyncTask<Void, ProgressPacket, Void> implements CancellableProgressNotifier {

	static final String TAG = "MerchantItemIdAssociatorTask";

	final protected Context context;
	final UsernamePasswordCredentials credentials;

	String error_message;
	
	protected PowerManager.WakeLock wl;
	protected Long start_milliseconds;
	
	int retry_count = 0;
	final int max_retires;
	final int max_http_threads;
    
	// ========================================================================
	public MerchantItemIdAssociatorTask(
			Context context,
			UsernamePasswordCredentials credentials) {
		this.context = context;
		this.credentials = credentials;
		
		PowerManager pm = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
		
		this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
		this.max_retires = settings.getInt(MainPreferences.PREFKEY_MAX_FETCH_RETRIES, MainPreferences.DEFAULT_MAX_TASK_RETRIES);
		this.max_http_threads = settings.getInt(MainPreferences.PREFKEY_MAX_HTTP_THREADS, MainPreferences.DEFAULT_MAX_HTTP_THREADS);
	}
	
	// ========================================================================
	@Override
	protected void onPreExecute() {
		if (!this.wl.isHeld())
			this.wl.acquire();
		
		if (this.start_milliseconds == null)
			this.start_milliseconds = SystemClock.uptimeMillis();
	}

	// ========================================================================
	@Override
	protected Void doInBackground(Void... voided) {

		DatabaseRevenue database = new DatabaseRevenue(this.context);

		try {
			while (true) {
				// TODO Make sure that this catches the "SSL Socket error"
				try {

					CheckoutXmlUtils.fillInMerchantItemIds(
							this,
							this.max_http_threads,
							database,
							this.credentials);
					break;
					
				} catch (IOException e) {
					
					Log.e(TAG, "Caught error on retry " + retry_count + ": " + e.getMessage());
					e.printStackTrace();

					if (retry_count < this.max_retires) {
						retry_count++;

						

					} else {
						throw e;
					}
				}
			}

		} catch (GoogleCheckoutException e) {
			e.printStackTrace();
			this.error_message = e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			this.error_message = e.getMessage();
		}
		
		return null;
	}


	// ========================================================================
	@Override
	public void notifyProgress(int progress, int max, int layer) {
		ProgressPacket packet = new ProgressPacket(null, progress, max, null);
		this.publishProgress(packet);
	}

	// ========================================================================
	@Override
	protected void onCancelled() {
		cleanUp();
	}
	
	// ========================================================================
	@Override
	public void onPostExecute(Void voided) {

		cleanUp();

		if (this.error_message != null) {
			failTask(this.error_message);
		} else {
			completeTask();
		}
	}
	
	// ========================================================================
	abstract protected void completeTask();
	
	// ========================================================================
	protected void cleanUp() {
		this.wl.release();
	}
	
	// ========================================================================
	protected void failTask(String error_message) {
		Toast.makeText(this.context, error_message, Toast.LENGTH_LONG).show();
	}
	
	// ========================================================================
	protected String getElapsedTime() {
        long end_milliseconds = SystemClock.uptimeMillis();
        long delta_millis = end_milliseconds - this.start_milliseconds;
        return DurationStrings.printDuration(delta_millis);
	}
}