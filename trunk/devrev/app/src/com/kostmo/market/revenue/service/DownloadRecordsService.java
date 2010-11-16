package com.kostmo.market.revenue.service;

import java.util.Date;
import java.util.List;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.Disablable;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.activity.RevenueActivity.RecordFetchAssignment;
import com.kostmo.market.revenue.activity.RevenueActivity.RecordFetcherTaskStage;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.container.ProgressPacket;
import com.kostmo.market.revenue.container.ProgressPacket.DoneType;
import com.kostmo.market.revenue.container.ProgressPacket.ProgressStage;
import com.kostmo.market.revenue.task.CheckoutNotificationsFetcherTask;
import com.kostmo.market.revenue.task.MerchantItemIdAssociatorTask;
import com.kostmo.tools.DurationStrings;
import com.kostmo.tools.progress.EtaWindow;
import com.kostmo.tools.progress.EtaWindow.UninitializedException;

public class DownloadRecordsService extends Service {

	static final String TAG = "DownloadRecordsService";

	final static int MAX_DISCRETE_PROGRESS_INCREMENTS = 50;

	static final String PREFKEY_BACKUP_ASSIGNMENT_START = "PREFKEY_BACKUP_ASSIGNMENT_START";
	static final String PREFKEY_BACKUP_ASSIGNMENT_END = "PREFKEY_BACKUP_ASSIGNMENT_END";
	static final String PREFKEY_BACKUP_ASSIGNMENT_MODE = "PREFKEY_BACKUP_ASSIGNMENT_MODE";
	
	// XXX These are redundant copies, intended only for use by this Service
	static final String PREFKEY_BACKUP_MERCHANT_ID = "PREFKEY_BACKUP_MERCHANT_ID";
	static final String PREFKEY_BACKUP_MERCHANT_KEY = "PREFKEY_BACKUP_MERCHANT_KEY";

	SharedPreferences settings;
    protected NotificationManager mNM;
	CheckoutNotificationsFetcherTaskExtended record_fetcher_task = null;
	MerchantItemIdAssociatorTaskExtended merchant_item_id_task = null;
	RecordFetchAssignment record_fetch_assignment = null;
	
	Disablable disablable_host = null;

	// ========================================================================
	public void setDisablableHost(Disablable host) {
		this.disablable_host = host;
	}

	// ========================================================================
	public void cancelEverything() {
		
		Log.d(TAG, "Cancelling all tasks in the Service.");
		
		// Set notification status message to "Cancelled"
		if (this.record_fetcher_task != null)
			this.record_fetcher_task.cancel(true);
		
		if (merchant_item_id_task != null)
			merchant_item_id_task.cancel(true);
	}
	
	// ========================================================================
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public DownloadRecordsService getService() {
            return DownloadRecordsService.this;
        }
    }

    boolean is_in_progress = false;
    public boolean isInProgress() {
    	return this.is_in_progress;
    }
    
	// ========================================================================
    boolean revived = false;
    
    @Override
    public void onCreate() {
    	super.onCreate();

		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
		this.mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
	// ========================================================================
    // TODO
    /* ENABLE ON ANDROID 2.0
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    */
    
	// ========================================================================
    UsernamePasswordCredentials reconstituteCredentials() {
    	UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
			Long.toString(this.settings.getLong(PREFKEY_BACKUP_MERCHANT_ID, RevenueActivity.INVALID_MERCHANT_ID)),
			this.settings.getString(PREFKEY_BACKUP_MERCHANT_KEY, null)
    	);

    	return credentials;
    }
    
	// ========================================================================
    void backupCredentials(UsernamePasswordCredentials credentials) {

		Editor settings_editor = this.settings.edit();
		settings_editor.putLong(PREFKEY_BACKUP_MERCHANT_ID, Long.parseLong(credentials.getUserName()));
		settings_editor.putString(PREFKEY_BACKUP_MERCHANT_KEY, credentials.getPassword());
		settings_editor.commit();
    }

	// ========================================================================
    RecordFetchAssignment reconstituteAssignmentFromPreferences() {
    	
    	RecordFetchAssignment assignment = new RecordFetchAssignment();
    	assignment.date_range = new DateRange(
    			new Date(settings.getLong(PREFKEY_BACKUP_ASSIGNMENT_START, 0)),
    			new Date(settings.getLong(PREFKEY_BACKUP_ASSIGNMENT_END, 0))
    	);
    	assignment.fetcher_stage = RecordFetcherTaskStage.values()[this.settings.getInt(PREFKEY_BACKUP_ASSIGNMENT_MODE, RecordFetcherTaskStage.GET_RECORD_GAPS.ordinal())];
    	
    	return assignment;
    }

	// ========================================================================
    void backupAssignmentInPreferences(RecordFetchAssignment assignment) {
    	
    	Log.e(TAG, "Is assignment null? " + assignment);
    	
		Editor settings_editor = this.settings.edit();
		settings_editor.putInt(PREFKEY_BACKUP_ASSIGNMENT_MODE, assignment.fetcher_stage.ordinal());
		settings_editor.putLong(PREFKEY_BACKUP_ASSIGNMENT_START, assignment.date_range.start.getTime());
		settings_editor.putLong(PREFKEY_BACKUP_ASSIGNMENT_END, assignment.date_range.end.getTime());
		settings_editor.commit();
    }
    
	// ========================================================================
	@Override
	public void onStart(Intent intent, int startId) {
		Log.e(TAG, "Called DownloadImagesService onStart()");
	}
    
	// ========================================================================
	public void carryOutAssignment(RecordFetchAssignment assignment, UsernamePasswordCredentials cred) {
	
		backupAssignmentInPreferences(assignment);
		backupCredentials(cred);
		
		this.is_in_progress = true;

		switch (assignment.fetcher_stage) {
		case GET_RECORD_GAPS:
        	executeServiceTaskRecordFetcher(cred, assignment);
        	break;
		case GET_INCOMPLETE_ITEM_IDS:
		case MATCH_ITEM_NAMES:
			executeServiceTaskItemIdAssociationFetcher(null, cred, assignment);
			break;
		}
	}
	
	// ========================================================================
	void executeServiceTaskRecordFetcher(UsernamePasswordCredentials cred, RecordFetchAssignment assignment) {

		this.record_fetcher_task = new CheckoutNotificationsFetcherTaskExtended(
			this,
			cred,
			assignment);
		this.record_fetcher_task.execute();
	}

	// ========================================================================
	void executeServiceTaskItemIdAssociationFetcher(WakeLock wl, UsernamePasswordCredentials credentials, RecordFetchAssignment assignment) {
		MerchantItemIdAssociatorTaskExtended task = new MerchantItemIdAssociatorTaskExtended(
				this,
				wl,
				credentials,
				assignment.task_start_milliseconds);
		this.merchant_item_id_task = task;
		task.execute();
	}

	// ========================================================================
    @Override
    public void onDestroy() {

    	Log.e(TAG, "The Service was destroyed.");
    	
    	cancelEverything();
		
    	super.onDestroy();
    }

	// ========================================================================
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    

	public static final int NOTIFICATION_RECORD_FETCHER = 1;
	RemoteViews content_view;
	protected Notification cached_notification;

	// ========================================================
	public Notification buildNotification(int progress, int progress_max, String progress_message) {
		
		// FIXME Change to a static icon when we've reached the status of "DONE".
		int icon = R.drawable.stat_sys_download;
		CharSequence tickerText = progress_message;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		this.content_view.setProgressBar(R.id.notification_progressbar, progress_max, progress, false);
		notification.contentView = this.content_view;
		
		Intent notificationIntent = new Intent(this, RevenueActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.contentIntent = contentIntent;
		
		return notification;
	}
	
	// ========================================================
	public Notification buildNotificationMessageOnly(RemoteViews view, String progress_message, boolean launch_plot) {
		
		int icon = R.drawable.notify;
		CharSequence tickerText = progress_message;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		notification.contentView = view;
		
		Intent notificationIntent = new Intent(this, RevenueActivity.class);
		if (launch_plot) {
			notificationIntent.putExtra(RevenueActivity.EXTRA_NOTIFICATION_RETURNING, true);
		}
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.contentIntent = contentIntent;
		
		return notification;
	}

	// ========================================================
	public void produce_updated_notification(ProgressPacket packet) {

    	if (packet.message != null)
    		this.cached_notification.contentView.setTextViewText(R.id.notification_title_text, packet.message);

    	if (packet.eta_millis > 0) {
    		this.cached_notification.contentView.setViewVisibility(R.id.notification_eta_text, View.VISIBLE);
    		this.cached_notification.contentView.setTextViewText(
				R.id.notification_eta_text,
				"ETA: " + DurationStrings.printDuration(packet.eta_millis));
    	} else {
    		this.cached_notification.contentView.setViewVisibility(R.id.notification_eta_text, View.GONE);
    	}
    	
    	if (packet.stage == null) {

    		this.cached_notification.contentView.setViewVisibility(R.id.notification_progressbar_wrapper, View.VISIBLE);
    		
    		boolean nonnegative_progress = packet.progress_value >= 0;

    		this.cached_notification.contentView.setViewVisibility(
    				R.id.notification_progress_text,
    				nonnegative_progress ? View.VISIBLE : View.GONE);
    		
    		if (nonnegative_progress) {

    			boolean indeterminate = packet.max_value <= 0;

	    		this.cached_notification.contentView.setProgressBar(
		    			R.id.notification_progressbar,
		    			packet.max_value,
		    			packet.progress_value,
		    			indeterminate);

	    		this.cached_notification.contentView.setTextViewText(
	    				R.id.notification_progress_text,
	    				indeterminate ?
    						Integer.toString(packet.progress_value) :
							getMessageText(packet.progress_value, packet.max_value));

    		}
    		
    	} else if (!ProgressStage.DONE.equals(packet.stage)) {

    		String format_string = "Step %d/%d";
    		String message_text = String.format(
    				format_string,
    				packet.stage.ordinal() + 1,
    				ProgressStage.DONE.ordinal());
    		this.cached_notification.contentView.setTextViewText(R.id.notification_steps_text, message_text);
    		
		} else {

    		this.cached_notification.contentView.setViewVisibility(
    				R.id.notification_steps_text, View.GONE);
			
    		this.cached_notification.contentView.setViewVisibility(R.id.notification_progressbar_wrapper, View.GONE);
    		
    		if (DoneType.FAILED.equals(packet.done_type)) {
    			String failure_title = getResources().getString(R.string.task_failiure_title);
	    		this.cached_notification.contentView.setTextViewText(R.id.notification_title_text, failure_title);
	    		this.cached_notification.contentView.setTextViewText(R.id.notification_progress_text, packet.message);

    		} else {
	    		this.cached_notification.contentView.setTextViewText(R.id.notification_title_text, packet.message);
	    		
	    		String subtitle_message = getResources().getString(R.string.task_finished_subtitle);
	    		this.cached_notification.contentView.setTextViewText(R.id.notification_progress_text, subtitle_message);
    		}
    		
    		this.cached_notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	}
	}
	
	// ========================================================
    public void updateNotificationProgress(ProgressPacket packet) {
    	produce_updated_notification(packet);
    	this.mNM.notify(NOTIFICATION_RECORD_FETCHER, this.cached_notification);
    }
	
	// ========================================================
	String getMessageText(int progress, int progress_max) {
		String format_string = "%d/%d";
		String message_text = String.format(format_string, progress, progress_max);
		return message_text;
	}
	
	// ========================================================================
	class CheckoutNotificationsFetcherTaskExtended extends CheckoutNotificationsFetcherTask {

		EtaWindow eta_window = new EtaWindow();
		
		// ====================================================================
		public CheckoutNotificationsFetcherTaskExtended(
				Context context,
				UsernamePasswordCredentials credentials,
				RecordFetchAssignment assignment) {
			super(context, credentials, assignment);
			
			content_view = new RemoteViews(context.getPackageName(), R.layout.progress_notification_layout);

			String initial_progress_text = getResources().getString(R.string.task_notifications_fetcher);
			content_view.setTextViewText(R.id.notification_title_text, initial_progress_text);
			
			String initial_notification_message = getResources().getString(R.string.merchant_syncing_records);
			cached_notification = buildNotification(0, -1, initial_notification_message);
		}

		// ========================================================================
		@Override
		protected void onCancelled() {

			preCleanUpBad();
			
			String failure_notification_message = "Cancelled.";
			String failure_notification_message2 = "Retrieval cancelled.";
			cached_notification = buildNotificationMessageOnly(content_view, failure_notification_message, false);
			ProgressPacket packet = new ProgressPacket(ProgressStage.DONE, 0, 1, failure_notification_message2);
			packet.done_type = DoneType.CANCELLED;
			updateNotificationProgress(packet);
			
			super.onCancelled();
			
			stopSelf();
		}

		// ====================================================================
		void preCleanUpBad() {
			super.cleanUp();
			stopForeground(true);
			is_in_progress = false;
		}
		
		// ====================================================================
		@Override
		public void onPreExecute() {
			super.onPreExecute();

			startService(new Intent(DownloadRecordsService.this, DownloadRecordsService.this.getClass()));
			startForeground(NOTIFICATION_RECORD_FETCHER, cached_notification);
			
//			ProgressPacket packet = new ProgressPacket(ProgressStage.RECORD_FETCHING, 0, 1, null);
//			updateNotificationProgress(packet);
			
			if (disablable_host != null)
				disablable_host.disable();
		}

		// ====================================================================
		@Override
	    protected void onProgressUpdate(ProgressPacket... packets) {
			ProgressPacket packet = packets[0];
			
			try {
				this.eta_window.addSpan(packet.progress_value, SystemClock.uptimeMillis());
				packet.eta_millis = this.eta_window.getEtaMillis(packet.max_value - packet.progress_value);
			} catch (UninitializedException e) {
				e.printStackTrace();
			}
			
			updateNotificationProgress(packet);
		}
		
		// ====================================================================
		@Override
		protected void cleanUp() {
			// XXX We do not call through to the parent here, since we
			// DO NOT want to release the wake lock; the wake lock
			// will be released in the next Task.
//			super.cleanUp();

			if (disablable_host != null)
				disablable_host.reEnable();
			else {
				Log.e(TAG, "Reference to host is null; can't re-enable.");
			}
		}
		
		// ====================================================================
		@Override
		protected void failTask(String error_message) {

			preCleanUpBad();
			
			String failure_notification_message = getResources().getString(R.string.task_failed_notification);
			cached_notification = buildNotificationMessageOnly(content_view, failure_notification_message, false);

			ProgressPacket packet = new ProgressPacket(ProgressStage.DONE, 0, 1, error_message);
			packet.done_type = DoneType.FAILED;
			updateNotificationProgress(packet);
		}
		
		// ====================================================================
		@Override
		protected void completeTask(DateRange date_range) {

			List<Long> incomplete_order_numbers = this.database.queryOrdersLackingItemIds(date_range);
			Log.i(TAG, "Orders lacking merchant IDs: " + incomplete_order_numbers.size());
			
			record_fetch_assignment = new RecordFetchAssignment();
			record_fetch_assignment.task_start_milliseconds = this.start_milliseconds;
			
			record_fetch_assignment.date_range = this.full_requested_date_range;
			record_fetch_assignment.fetcher_stage = RecordFetcherTaskStage.GET_INCOMPLETE_ITEM_IDS;
			record_fetch_assignment.incomplete_order_numbers = incomplete_order_numbers;
			
			executeServiceTaskItemIdAssociationFetcher(this.wl, this.credentials, record_fetch_assignment);
		}
	}

	// ========================================================================
	class MerchantItemIdAssociatorTaskExtended extends MerchantItemIdAssociatorTask {

		EtaWindow eta_window = new EtaWindow();
		
		public MerchantItemIdAssociatorTaskExtended(
				Context context,
				WakeLock wl,
				UsernamePasswordCredentials credentials,
				Long previous_start_millis) {
			super(context, credentials);

			if (wl != null)
				this.wl = wl;
			
			content_view = new RemoteViews(context.getPackageName(), R.layout.progress_notification_layout);
			
			String initial_notification_message = getResources().getString(R.string.merchant_completing_records);
			cached_notification = buildNotification(0, -1, initial_notification_message);
			content_view.setTextViewText(R.id.notification_title_text, initial_notification_message);
			
			this.start_milliseconds = previous_start_millis;
		}

		// ========================================================================
		@Override
		protected void onCancelled() {
			
			String failure_notification_message = "Cancelled.";
			String failure_notification_message2 = "Retrieval cancelled.";
			cached_notification = buildNotificationMessageOnly(content_view, failure_notification_message, false);
			ProgressPacket packet = new ProgressPacket(ProgressStage.DONE, 0, 1, failure_notification_message2);
			packet.done_type = DoneType.CANCELLED;
			updateNotificationProgress(packet);
			
			super.onCancelled();
		}
		
		// ====================================================================
		@Override
		public void onPreExecute() {
			super.onPreExecute();
			
			ProgressPacket packet = new ProgressPacket(ProgressStage.ITEM_ID_MATCHING, 0, 1, null);
	    	produce_updated_notification(packet);
	    	
			startService(new Intent(DownloadRecordsService.this, DownloadRecordsService.this.getClass()));
		    startForeground(NOTIFICATION_RECORD_FETCHER, cached_notification);
			
			if (disablable_host != null)
				disablable_host.disable();
		}

		// ====================================================================
		@Override
	    protected void onProgressUpdate(ProgressPacket... packets) {
			ProgressPacket packet = packets[0];
			
			try {
				this.eta_window.addSpan(packet.progress_value, SystemClock.uptimeMillis());
				packet.eta_millis = this.eta_window.getEtaMillis(packet.max_value - packet.progress_value);
			} catch (UninitializedException e) {
				e.printStackTrace();
			}
			
			updateNotificationProgress(packet);
		}
		
		// ====================================================================
		@Override
		protected void cleanUp() {
			super.cleanUp();
			
			if (disablable_host != null) {
				disablable_host.reEnable();
			} else {
				Log.e(TAG, "Reference to host is null; can't re-enable.");
			}
			
			is_in_progress = false;
			stopForeground(true);	// Is this necessary?
			stopSelf();	// With stopForegroundCompat(), this removes the notification.
		}

		// ====================================================================
		@Override
		protected void failTask(String error_message) {
			
			String failure_notification_message = getResources().getString(R.string.task_failed_notification);
			cached_notification = buildNotificationMessageOnly(content_view, failure_notification_message, false);
			ProgressPacket packet = new ProgressPacket(ProgressStage.DONE, 0, 1, error_message);
			packet.done_type = DoneType.FAILED;
			updateNotificationProgress(packet);
		}
		
		// ====================================================================
		@Override
		protected void completeTask() {

			String complete_notification_message = getResources().getString(R.string.task_complete_notification);
			cached_notification = buildNotificationMessageOnly(content_view, complete_notification_message, true);
			
    		String master_message = getResources().getString(R.string.task_timed_finish_title, getElapsedTime());
			ProgressPacket packet = new ProgressPacket(ProgressStage.DONE, 0, 1, master_message);
			updateNotificationProgress(packet);
		}
	}
}
