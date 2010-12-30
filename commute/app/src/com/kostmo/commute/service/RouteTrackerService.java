package com.kostmo.commute.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.activity.Main;

public class RouteTrackerService extends Service {

	static final String TAG = Market.TAG;

	public final static int ONGOING_NOTIFICATION_ID = 1;



	// ========================================================================
    @Override
    public void onCreate() {

    }

	// ========================================================
	public Notification buildNotificationMessageOnly(String progress_message) {
		
		
//		int icon = R.drawable.upload_notification;
		int icon = R.drawable.notification_trip_timing;
		CharSequence tickerText = progress_message;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		RemoteViews content_view = new RemoteViews(getPackageName(), R.layout.progress_notification_layout);

		String initial_progress_text = "Route in progress";
		content_view.setTextViewText(R.id.notification_title_text, initial_progress_text);
		
		notification.contentView = content_view;
		
		Intent notificationIntent = new Intent(this, Main.class);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.contentIntent = contentIntent;
		
		return notification;
	}

	// ========================================================================
    @Override
    public void onStart(Intent intent, int startId) {

		Log.d(TAG, "Called onStart() for a Batch upload.");
    }

	// ========================================================================
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public RouteTrackerService getService() {
            return RouteTrackerService.this;
        }
    }

    boolean is_in_progress = false;
    public boolean isInProgress() {
    	return this.is_in_progress;
    }
    

	// ========================================================================
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

	// ========================================================================
	@Override
	public IBinder onBind(Intent intent) {
        return mBinder;
	}
}