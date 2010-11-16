package com.kostmo.flickr.service;

import java.util.Collection;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.kostmo.flickr.activity.Disablable;
import com.kostmo.flickr.activity.Main;
import com.kostmo.flickr.activity.BatchUploaderActivity.ImageUploadData;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.Refreshable;
import com.kostmo.flickr.tasks.PhotoMultiUploadTask;

public class UploadService extends Service {

	static final String TAG = Market.DEBUG_TAG;

	public final static int ONGOING_NOTIFICATION_ID = 2;

	Disablable disablable_host = null;

	// ========================================================================
	public void setDisablableHost(Disablable host) {
		this.disablable_host = host;
	}

	// ========================================================================
    @Override
    public void onCreate() {

    }

	// ========================================================
	public Notification buildNotificationMessageOnly(String progress_message) {
		
//		int icon = R.drawable.upload_notification;
		int icon = R.drawable.stat_sys_upload;
		CharSequence tickerText = progress_message;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		RemoteViews content_view = new RemoteViews(getPackageName(), R.layout.progress_notification_layout);

		String initial_progress_text = PhotoMultiUploadTask.UPLOAD_NOTIFICATION_TITLE_STRING;
		content_view.setTextViewText(R.id.notification_title_text, initial_progress_text);
		
		notification.contentView = content_view;
		
		Intent notificationIntent = new Intent(this, Main.class);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.contentIntent = contentIntent;
		
		return notification;
	}

	// ========================================================================
    public void beginUploadsFromList(Refreshable refreshable, List<ImageUploadData> pending_uploads_list, Collection<String> tags) {

		startService(new Intent(this, getClass()));
		
		this.is_in_progress = true;
		Notification n = buildNotificationMessageOnly( "Uploading " + pending_uploads_list.size() + " items." );
		startForeground(ONGOING_NOTIFICATION_ID, n);

    	Log.d(TAG, "About to upload " + pending_uploads_list.size() + " items.");
    	
		if (disablable_host != null)
			disablable_host.disable();
    	
		PhotoMultiUploadTaskExtended pu = new PhotoMultiUploadTaskExtended(getBaseContext(), refreshable, pending_uploads_list, n, tags);
		pu.execute();
    }
    
	// ========================================================================
    class PhotoMultiUploadTaskExtended extends PhotoMultiUploadTask {

    	public PhotoMultiUploadTaskExtended(Context c, Refreshable refresher,
				List<ImageUploadData> pending_uploads_list, Notification n, Collection<String> tags) {
			super(c, refresher, pending_uploads_list, n, tags);
		}

    	// ====================================================================
        @Override
        public void onPostExecute(List<Long> photo_ids) {
			stopForeground(true);
    		flickr_upload_finished_notification(photo_ids);
    		is_in_progress = false;
    		
    		if (disablable_host != null)
    			disablable_host.reEnable();
    		
        	stopSelf();
        }
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
        public UploadService getService() {
            return UploadService.this;
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