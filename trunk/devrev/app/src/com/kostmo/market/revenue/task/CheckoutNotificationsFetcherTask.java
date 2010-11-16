package com.kostmo.market.revenue.task;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.MerchantCredentialsActivity;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.activity.RevenueActivity.RecordFetchAssignment;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.container.ProgressPacket;
import com.kostmo.market.revenue.container.ProgressPacket.ProgressStage;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.ChargeAmount;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.ChargesRangeFetchResult;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.GoogleCheckoutException;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.GoogleCheckoutLoginException;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.GoogleCheckoutLoginIdException;

public abstract class CheckoutNotificationsFetcherTask extends AsyncTask<Void, ProgressPacket, Void> implements CancellableProgressNotifier, CancellableProgressIncrementor {

	static final String TAG = Market.TAG;

	// TODO This can be made adjustable in the preferences.
	// Or, better yet, make it adjust itself dynamically.
	static final int MAX_THREAD_DAY_COUNT = 14;	// Two weeks?

	static final int MINIMUM_NOTIFICATION_UPDATE_PERIOD_SECONDS = 3;
	
	
	protected Context context;
	protected UsernamePasswordCredentials credentials;
	protected DateRange full_requested_date_range;

	String error_message;
	protected DatabaseRevenue database;
	List<DateRange> uncached_date_ranges;
	
	protected PowerManager.WakeLock wl;
	protected long start_milliseconds;
    

	int retry_count = 0;
	final int max_retires;
	final int max_http_threads;
	
	// ========================================================================
	public CheckoutNotificationsFetcherTask(
			Context context,
			UsernamePasswordCredentials credentials,
			RecordFetchAssignment assignment) {
		this.context = context;
		this.credentials = credentials;
		this.full_requested_date_range = assignment.date_range;
		this.uncached_date_ranges = assignment.uncached_date_ranges;
		this.database = new DatabaseRevenue(this.context);
		
		PowerManager pm = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
		this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
		this.max_retires = settings.getInt(MainPreferences.PREFKEY_MAX_FETCH_RETRIES, MainPreferences.DEFAULT_MAX_TASK_RETRIES);
		this.max_http_threads = settings.getInt(MainPreferences.PREFKEY_MAX_HTTP_THREADS, MainPreferences.DEFAULT_MAX_HTTP_THREADS);
	}

	// ========================================================================
	@Override
	protected void onPreExecute() {
		this.wl.acquire();
		this.start_milliseconds = SystemClock.uptimeMillis();
	}

	// ========================================================================
	private static List<DateRange> chunkifyDateRange(DateRange range, int max_days_per_chunk) {
		
		List<DateRange> date_range_batches = new ArrayList<DateRange>();
		
		final Calendar tail_calendar = new GregorianCalendar();
		tail_calendar.setTime(range.start);
		
		while (true) {
			
			DateRange chunk_date_range = new DateRange();
			date_range_batches.add(chunk_date_range);

			chunk_date_range.start = tail_calendar.getTime();
			
			// Move the tail two weeks ahead
			tail_calendar.add(Calendar.DATE, max_days_per_chunk);
			
			if (range.end.after(tail_calendar.getTime())) {
				chunk_date_range.end = tail_calendar.getTime();
			} else {
				chunk_date_range.end = (Date) range.end.clone();
				break;
			}
		}
		
		return date_range_batches;
	}
	
	// ================================================================
	static class BatchFetcherThread extends Thread {

		// Input
		final DateRange uncached_date_range;
		final UsernamePasswordCredentials credentials;
		final CancellableProgressIncrementor notifier;
		
		// Output
		public ChargesRangeFetchResult fetch_result;
		
		public BatchFetcherThread(
			final DateRange uncached_date_range,
			final UsernamePasswordCredentials credentials,
			final CancellableProgressIncrementor notifier) {

			this.uncached_date_range = uncached_date_range;
			this.credentials = credentials;
			this.notifier = notifier;
		}

		@Override
		public void run() {

			this.fetch_result = CheckoutXmlUtils.fetchChargesRange(
					this.notifier,
					this.credentials,
					this.uncached_date_range);
		}
	};

	private long saved_max_millis;
	
	// ========================================================================
	@Override
	protected Void doInBackground(Void... voided) {
		
		try {

			while (true) {
				// TODO Make sure that this catches the "SSL Socket error"
				
				try {
					
					this.saved_max_millis = 0;
					
					
					
					Stack<DateRange> chunkified_date_ranges = new Stack<DateRange>();
					for (DateRange uncached_date_range : this.uncached_date_ranges) {
						
						this.saved_max_millis += uncached_date_range.getMillisDelta();
						
						List<DateRange> chunk_date_ranges = chunkifyDateRange(uncached_date_range, MAX_THREAD_DAY_COUNT);
						chunkified_date_ranges.addAll(chunk_date_ranges);
					}
					

					this.notifyIncrementalProgress(0);
					

					while (!chunkified_date_ranges.isEmpty()) {

						if (isCancelled()) return null;
						
						// These are the ranges that will be fetched in parallel.
						List<DateRange> date_range_batch = new ArrayList<DateRange>();
						for (int j=0; j<this.max_http_threads; j++) {
							
							date_range_batch.add( chunkified_date_ranges.pop() );
							
							if (chunkified_date_ranges.isEmpty())
								break;
						}
						
		            	Collection<BatchFetcherThread> fetcher_threads = new ArrayList<BatchFetcherThread>();
						for (DateRange uncached_date_range : date_range_batch) {
								
							BatchFetcherThread fetcher_thread = new BatchFetcherThread(
									uncached_date_range,
									this.credentials,
									this);
		    				fetcher_threads.add(fetcher_thread);
		    				fetcher_thread.start();
						}
						
		    			// Rejoin all of the threads			
		    			for (BatchFetcherThread thread : fetcher_threads) {
		    				try {
		    					thread.join();

		    				} catch (InterruptedException e) {
		    					e.printStackTrace();
		    				}
		    			}
		    			
		    			
		    			// Check for errors and store results
		    			for (BatchFetcherThread thread : fetcher_threads) {

	    					List<ChargeAmount> charges = thread.fetch_result.charges_list;
		    				
	    					if (charges.size() > 0) {
			    				// Assume we are fully cached...
			    				DateRange cached_range = thread.uncached_date_range;
	
		    					// ...but if there was an error, we can mark the date range up to the last fetched record as cached.
			    				if (thread.fetch_result.io_exception != null || thread.fetch_result.google_exception != null) {
			    					Date last_record_date = CheckoutXmlUtils.getLast(charges).date;
			    					cached_range = new DateRange(cached_range.start, last_record_date);
			    				}
			    				
		    					database.storePurchasesInTransaction(charges, cached_range);
	    					}
	    					
		    				
		    				if (thread.fetch_result.google_exception != null) {
		    					throw thread.fetch_result.google_exception;
		    				} else if (thread.fetch_result.io_exception != null) {
		    					throw thread.fetch_result.io_exception;
		    				}
		    			}
					}

					// If we successfully complete fetching our ranges, break from the while loop.
					break;
				} catch (UnknownHostException e) {
					
					Log.e(TAG, "Got unknown host...");
					
					throw new UnknownHostException("Unknown host: " + e.getMessage());
				} catch (IOException e) {
					
					Log.e(TAG, "Caught error on retry " + retry_count + ": " + e.getMessage());
					e.printStackTrace();
					
					if (retry_count < this.max_retires) {
						retry_count++;

						
						this.uncached_date_ranges = this.database.getUncachedDateRanges(this.full_requested_date_range);
						Log.d(TAG, "Updated uncached ranges: " + this.uncached_date_ranges.size());
					
					} else {
						throw e;
					}
				}
			}
		

		} catch (GoogleCheckoutLoginIdException e) {
			// Set login ID to invalid.
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
			settings.edit().putLong(MerchantCredentialsActivity.PREFKEY_SAVED_MERCHANT_ID, RevenueActivity.INVALID_MERCHANT_ID).commit();
			e.printStackTrace();
			this.error_message = e.getMessage();
		} catch (GoogleCheckoutLoginException e) {
			// Set login password to invalid.
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
			settings.edit().putString(MerchantCredentialsActivity.PREFKEY_SAVED_MERCHANT_KEY, null).commit();
			e.printStackTrace();
			this.error_message = e.getMessage();
		} catch (GoogleCheckoutException e) {
			e.printStackTrace();
			this.error_message = e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			this.error_message = e.getMessage();
		}

		return null;
	}

	AtomicLong progress_millis = new AtomicLong();
	static final int MILLIS_PER_HOUR = 1000*60*60;

	@Override
	public void notifyIncrementalProgress(long incremental_progress) {

		long progress = progress_millis.addAndGet(incremental_progress);
		
		// Divide by hours
		int progress_hours = (int) (progress / MILLIS_PER_HOUR);
		int max_hours = (int) (this.saved_max_millis / MILLIS_PER_HOUR);
		notifyProgress(progress_hours, max_hours, 0);
	}
	
	
	Long last_progress_update_time = null;
	
	// ========================================================================
	@Override
	public void notifyProgress(int progress, int max, int layer) {
		
		long current_millis = SystemClock.uptimeMillis();
		if (last_progress_update_time == null) {
			last_progress_update_time = current_millis;
		} else {
			// Don't update more frequently than thrice per second
			if ((current_millis - last_progress_update_time)/1000 < MINIMUM_NOTIFICATION_UPDATE_PERIOD_SECONDS) {
				return;
			} else {
				last_progress_update_time = current_millis;
			}
		}
		
		ProgressPacket packet = null;
		if (layer > 0) {
			String message = this.context.getResources().getString(R.string.task_fetching_batches, progress, max);
			packet = new ProgressPacket(ProgressStage.RECORD_FETCHING, -1, -1, message);
		} else {

			packet = new ProgressPacket(null, progress, max, null);
		}
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
			completeTask(this.full_requested_date_range);
		}
	}
	
	// ========================================================================
	abstract protected void completeTask(DateRange date_range);
	
	// ========================================================================
	protected void cleanUp() {
		this.wl.release();
	}
	
	// ========================================================================
	protected void failTask(String error_message) {
		Toast.makeText(this.context, error_message, Toast.LENGTH_LONG).show();
	}
	
	// ========================================================================
	protected float getElapsedSeconds() {
        long end_milliseconds = SystemClock.uptimeMillis();
        float seconds = (end_milliseconds - this.start_milliseconds)/1000f;
        return seconds;
	}
}