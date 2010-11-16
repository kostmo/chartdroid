package com.kostmo.market.revenue.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gc.android.market.api.MarketFetcher;
import com.gc.android.market.api.MarketFetcher.AndroidAuthenticationCredentials;
import com.gc.android.market.api.MarketFetcher.CommentRetrievalContainer;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.NewCommentsActivity;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.DatabaseRevenue.CommentAbortEarlyData;
import com.kostmo.tools.DurationStrings;

public class CheckUpdateService extends Service {

	public static final String PREFKEY_ENABLE_PERIODIC_CHECKIN = "enable_periodic_checkin";
	public static final String PREFKEY_UPDATE_INTERVAL_MILLIS = "checkin_update_rate";

	public static final boolean DEFAULT_CHECKIN_ENABLED = false;
	
	
	
    static int COMMENTS_NOTIFICATION_ID = 2;

	static final String TAG = "CheckUpdateService"; 
    

    private CheckForUpdatesTask mTask;
    String duration_string;

	DatabaseRevenue database;

	// ========================================================================
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate() in CheckUpdateService");
		this.database = new DatabaseRevenue(getBaseContext());
    }

	// ========================================================================
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        
        (mTask = new CheckForUpdatesTask()).execute();
    }

	// ========================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            mTask.cancel(true);
        }
    }

	// ========================================================================
    public IBinder onBind(Intent intent) {
        return null;
    }

	// ========================================================================
	/*
	 * This is able to both "set" and "unset" the alarm.
	 */
    public static void schedule(Context context) {
        final Intent intent = new Intent(context, CheckUpdateService.class);
        final PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

        
        Log.e(TAG, "I am about to schedule a Market Revenue checkup...");


        final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        alarm.cancel(pending);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hourly_checkin_enabled = prefs.getBoolean(PREFKEY_ENABLE_PERIODIC_CHECKIN, DEFAULT_CHECKIN_ENABLED);

        if (hourly_checkin_enabled) {

        	long checkin_interval_millis = Long.parseLong(prefs.getString(PREFKEY_UPDATE_INTERVAL_MILLIS, Long.toString(AlarmManager.INTERVAL_DAY)));
        	// Just for fun, check whether it is one of the predefined increments.
        	List<Long> predefined_alarm_intervals = Arrays.asList(new Long[] {
        			AlarmManager.INTERVAL_DAY,
        			AlarmManager.INTERVAL_HALF_DAY,
        			AlarmManager.INTERVAL_HOUR,
        			AlarmManager.INTERVAL_HALF_HOUR,
        			AlarmManager.INTERVAL_FIFTEEN_MINUTES
        	});
        	int interval_index = predefined_alarm_intervals.indexOf(checkin_interval_millis);
        	Log.d(TAG, "Chose predefined interval index: " + interval_index);

        	alarm.setInexactRepeating(
        		AlarmManager.ELAPSED_REALTIME_WAKEUP,
        		0,
        		checkin_interval_millis,
//        		AlarmManager.INTERVAL_FIFTEEN_MINUTES,	// FIXME - DEBUG ONLY
        		pending);
        	
        } else {
        	
        	alarm.cancel(pending);
        }
    }

    // ========================================================================
    static class NewTriggeredCommentStats {
    	List<Date> last_sync_dates = new ArrayList<Date>();
    	int new_bad_comment_count = 0;
    	Map<Long, List<Integer>> new_bad_ratings_map = new HashMap<Long, List<Integer>>();
    }

	// ================================================================
	static class CommentFetcherThread extends Thread {

		// Input
		final long app_id;
		final int max_comments;
		final AndroidAuthenticationCredentials credentials;
		final CommentAbortEarlyData abort_early_comment_data;
		
		// Output
		CommentRetrievalContainer container = null;
		IOException exception = null;
		
		public CommentFetcherThread(
			long app_id,
			final int max_comments,
			final CommentAbortEarlyData abort_early_comment_data,
			final AndroidAuthenticationCredentials android_credentials) {
			
			this.app_id = app_id;
			
			this.max_comments = max_comments;
			this.abort_early_comment_data = abort_early_comment_data;
			this.credentials = android_credentials;
		}

		@Override
		public void run() {
			try {
				this.container = MarketFetcher.getAppComments(
						null,
						this.max_comments,
						this.abort_early_comment_data,
						this.credentials,
						this.app_id);
			} catch (IOException e) {
				e.printStackTrace();
				this.exception = e;
			}
		}
	};
    
    // ========================================================================
    private class CheckForUpdatesTask extends AsyncTask<Void, Long, NewTriggeredCommentStats> {
        private SharedPreferences mPreferences;
        private NotificationManager mManager;
        private DatabaseRevenue database;

        // ====================================================================
        public void onPreExecute() {
        	this.mPreferences = PreferenceManager.getDefaultSharedPreferences(CheckUpdateService.this);
        	this.mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        	this.database = new DatabaseRevenue(getBaseContext());
        }
        
        // ====================================================================
        public NewTriggeredCommentStats doInBackground(Void... params) {

    		String authToken = this.mPreferences.getString(MarketFetcher.PREFKEY_SAVED_ANDROID_AUTHTOKEN, null);
    		if (authToken == null) {
    			Log.e(TAG, "Error: null authToken");
    			this.cancel(true);
    			return null;
    		}

    		String android_id = MarketFetcher.obtainAndroidId(getBaseContext());
    		AndroidAuthenticationCredentials credentials = new AndroidAuthenticationCredentials(authToken, android_id);
    		final int max_comments = this.mPreferences.getInt(MainPreferences.PREFKEY_MAX_COMMENTS, MarketFetcher.DEFAULT_MAX_COMMENT_FETCH_LIMIT);
        	
    		NewTriggeredCommentStats stats = new NewTriggeredCommentStats();
        	

        	// Sync comments, then list all comments made after last sync time for certain apps.
        	// We ignore the apps that have a "zero" rating threshold.
        	
        	Collection<Long> full_app_ids_list = this.database.getAppsWithNonzeroRatingThreshold();
			Log.i(TAG, "There are " + full_app_ids_list.size() + " Google Checkout apps that need comments checked.");
			
        	// Since we must not access the database from multiple threads,
        	// we just obtain the sync dates all up front.
        	Map<Long, Date> last_app_sync_dates = Collections.unmodifiableMap(this.database.getAllLastCommentSyncDates());
        	
		
        	
			// We want to fetch all of the apps in parallel.
        	// However, we absolutely must not *write* to the database from
			// multiple threads; therefore, we write *after* the threads rejoin.
        	
        	Stack<Long> batch_app_ids = new Stack<Long>();
        	batch_app_ids.addAll(full_app_ids_list);
        	

        	final int max_http_threads = this.mPreferences.getInt(MainPreferences.PREFKEY_MAX_HTTP_THREADS, MainPreferences.DEFAULT_MAX_HTTP_THREADS);
        	
        	// We limit the number of concurrent threads in batches.
        	int batch_count = 0;
        	while (!batch_app_ids.isEmpty()) {
        		Collection<Long> app_ids = new ArrayList<Long>();
        		for (int i=0; i<max_http_threads; i++) {
        			if (batch_app_ids.isEmpty())
        				break;
        			
        			app_ids.add( batch_app_ids.pop() );
        		}
        		
        		Log.d(TAG, "Comment thread batch " + batch_count + " has " + app_ids.size() + " apps.");
        		
            	Collection<CommentFetcherThread> fetcher_threads = new ArrayList<CommentFetcherThread>();

    			for (long app_id : app_ids) {

    				if (!last_app_sync_dates.containsKey(app_id))
    					continue;
    				
    				Date last_sync_date = last_app_sync_dates.get(app_id);
    				stats.last_sync_dates.add(last_sync_date);
    				
    				CommentAbortEarlyData abort_early_comment_data = this.database.getCommentAbortEarlyData(app_id);
    				
    				CommentFetcherThread fetcher_thread = new CommentFetcherThread(app_id, max_comments, abort_early_comment_data, credentials);
    				fetcher_threads.add(fetcher_thread);
    				fetcher_thread.start();
    			}
    			
    			// Rejoin all of the threads			
    			for (CommentFetcherThread thread : fetcher_threads) {

    				try {
    					thread.join();
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
    			}
    			
    			// We must wait for *all* of the threads to join(), since some may be
    			// reading from the database.
    			for (CommentFetcherThread thread : fetcher_threads) {
    				if (thread.exception != null) {
    					// TODO
    				} else {

    					long app_id = thread.app_id;
    					CommentRetrievalContainer container = thread.container;
    					
    					MarketFetcher.updateDatabaseCommentsStatus(this.database, app_id, container.comment_status);
    					this.database.populateComments(app_id, container.comments);
    					
    					List<Integer> new_bad_ratings = this.database.getUnreadCommentsBelowThreshold(app_id);
    					Log.e(TAG, "Found " + new_bad_ratings.size() + " new ratings for app " + app_id + " at or below their threshold");
    					stats.new_bad_comment_count += new_bad_ratings.size();
    					
    					int total_count = this.database.countAllComments(app_id);
    					int new_count = this.database.countUnreadComments(app_id);
    					Log.i(TAG, new_count + " new, " + total_count + " total");
    					
    					if (new_bad_ratings.size() > 0) {
    						stats.new_bad_ratings_map.put(app_id, new_bad_ratings);
    					}
    				}
    			}
    			
    			
    			batch_count++;
        	}


            return stats;
        }

        // ====================================================================
        public void onPostExecute(NewTriggeredCommentStats stats) {
        	
        	Log.d(TAG, "All done with checkup!");

        	if (stats != null)
        		do_notify(stats);
        	
            stopSelf();
        }
        
        // ====================================================================
        void do_notify(NewTriggeredCommentStats stats) {

        	// Note: If you comment out this next line, notifications with 0 comments
        	// will show a red icon instead of green. 
        	if (stats.new_bad_comment_count <= 0) return;

        	int icon = R.drawable.notify;
        	String tickerText = stats.new_bad_comment_count + " new comments across " + stats.new_bad_ratings_map.size() + " apps";
        	if (stats.new_bad_comment_count <= 0) {
            	icon = R.drawable.notify_dummy;
            	tickerText = "Dummy";
        	}
        	
        	String full_notification_message = stats.new_bad_comment_count + " comments below rating thresholds across " + stats.new_bad_ratings_map.size() + " apps";
        	String elaborated_notification_message = null;
        	Date since_date = null;
        	if (stats.last_sync_dates.size() > 0) {
        		since_date = Collections.min(stats.last_sync_dates);
        		full_notification_message += " since " + since_date;
        		elaborated_notification_message = "From " + stats.new_bad_ratings_map.size() + " app" + DurationStrings.pluralize(stats.new_bad_ratings_map.size()) + " since " + RevenueActivity.HUMAN_DATE_FORMAT.format(Collections.min(stats.last_sync_dates));
        	}
        	Log.d(TAG, full_notification_message);

        	
        	long when = System.currentTimeMillis();         // notification time
        	Context context = getApplicationContext();      // application Context

    		Notification notification = new Notification(icon, tickerText, when);
    		notification.flags |= Notification.FLAG_AUTO_CANCEL;
    		Intent notificationIntent = new Intent(context, NewCommentsActivity.class);
    		if (since_date != null)
    			notificationIntent.putExtra(NewCommentsActivity.EXTRA_CUTOFF_DATE, since_date.getTime());
    		
    		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    		
    		
    		notification.setLatestEventInfo(context,
    				stats.new_bad_comment_count + " new subthreshold comment" + DurationStrings.pluralize(stats.new_bad_comment_count),
    				elaborated_notification_message, contentIntent);
        	
        	mManager.notify(COMMENTS_NOTIFICATION_ID, notification);
        	
//        	database.dumpCommentsTable();
        }
    }
}
