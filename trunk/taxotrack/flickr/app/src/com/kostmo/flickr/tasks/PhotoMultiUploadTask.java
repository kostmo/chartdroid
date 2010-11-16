package com.kostmo.flickr.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.uploader.UploadMetaData;
import com.aetrion.flickr.uploader.Uploader;
import com.kostmo.flickr.activity.BatchUploaderActivity;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.activity.ListActivityPhotoTags;
import com.kostmo.flickr.activity.BatchUploaderActivity.ImageUploadData;
import com.kostmo.flickr.activity.BatchUploaderActivity.UploadStatus;
import com.kostmo.flickr.activity.prefs.PrefsUpload;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.Refreshable;
import com.kostmo.flickr.containers.UploadProgressPacket;
import com.kostmo.flickr.data.DatabaseUploads;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.service.UploadService;
import com.kostmo.flickr.tools.LightChanger;
import com.kostmo.tools.DurationStrings;
import com.kostmo.tools.progress.EtaWindow;
import com.kostmo.tools.progress.EtaWindow.UninitializedException;

public class PhotoMultiUploadTask extends AsyncTask<Void, UploadProgressPacket, List<Long>> {
	
	
	public static int BLINK_OFF_TIME = 1900;
	public static int BLINK_ON_TIME = 100;
	

	static final String TAG = Market.DEBUG_TAG; 

	
	public static int flickr_tray_icon_resource = R.drawable.upload_notification;
//	int flickr_tray_icon_resource = R.drawable.icon_flickr_tiny;
	
	private static final int FLICKR_NOTIFICATION_ID = 1;
//	private static final int FLICKR_NOTIFICATION_FAILED_ID = 2;
	
	
    Uploader uploader = null;
//    PhotosInterface pint = null;
    Flickr flickr = null;
//    GeoInterface gint = null;
    
    Context context = null;
    Refreshable refresher = null;
    SharedPreferences settings;
    List<ImageUploadData> pending_uploads_list;
    DatabaseUploads database_uploads;
    Collection<String> batch_tags;

	EtaWindow eta_window = new EtaWindow();
	RemoteViews content_view;
	protected Notification cached_notification;
    protected NotificationManager mNM;
    
    public final static String UPLOAD_NOTIFICATION_TITLE_STRING = "Uploading photos...";
	
	// ========================================================================
    public PhotoMultiUploadTask(
    		Context c,
    		Refreshable refresher,
    		List<ImageUploadData> pending_uploads_list,
    		Notification n,
    		Collection<String> batch_tags) {
    	
    	this.context = c;
    	this.refresher = refresher;
		this.settings = PreferenceManager.getDefaultSharedPreferences(c);
		this.pending_uploads_list = pending_uploads_list;
		this.batch_tags = batch_tags;
		
		this.database_uploads = new DatabaseUploads(c);
		this.mNM = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		this.content_view = new RemoteViews(context.getPackageName(), R.layout.progress_notification_layout);
		this.cached_notification = n;
    }

    /*
	// ========================================================
	public Notification buildNotification(int progress, int progress_max, String progress_message) {
		
		int icon = R.drawable.stat_sys_upload;
		CharSequence tickerText = progress_message;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		this.content_view.setProgressBar(R.id.notification_progressbar, progress_max, progress, false);
		notification.contentView = this.content_view;
		
		content_view.setTextViewText(R.id.notification_title_text, UPLOAD_NOTIFICATION_TITLE_STRING);
		
        Intent notificationIntent = new Intent(context, UploadBatchActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);    	
		notification.contentIntent = contentIntent;
		
		return notification;
	}
	*/
    
	// ========================================================================
    void updateUploadStatus(ImageUploadData upload_data, UploadStatus new_status, long flickr_photo_id) {

//		upload.upload_status = new_status;
    	long upload_id = upload_data.row_id;
    	this.database_uploads.updateUploadStatus(upload_id, new_status, flickr_photo_id);
    }
    
	// ========================================================================
    protected Notification flickr_upload_finished_notification(List<Long> photo_ids) {
    	
    	int success_count = 0;
    	for (long photo_id : photo_ids)
    		if (photo_id != ListActivityPhotoTags.INVALID_PHOTO_ID)
    			success_count++;
    	
    	
    	
    	int icon = flickr_tray_icon_resource;        // icon from resources
    	CharSequence tickerText = "Upload complete";              // ticker-text
    	long when = System.currentTimeMillis();         // notification time
//    	Context context = getApplicationContext();      // application Context
    	CharSequence contentTitle = "Upload complete";  // expanded message title
//    	CharSequence contentText = "Photo ID: " + photo_id;      // expanded message text
    	CharSequence contentText = "Click to view uploads";      // expanded message text

        Intent notificationIntent = new Intent(context, BatchUploaderActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);    	
    	
    	// the next two lines initialize the Notification, using the configurations above
    	Notification notification = new Notification(icon, tickerText, when);
    	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    	notification.defaults |= Notification.DEFAULT_SOUND;
    	
    	// This doesn't help!
//    	notification.defaults |= Notification.FLAG_ONLY_ALERT_ONCE;

    	// "Flickr pink"
//    	notification.ledARGB = 0xfffe0486;
    	
    	// "Flickr blue"
//    	notification.ledARGB = 0xff0465dc;
    	
    	// "Flickr pink"
    	new LightChanger.LightSettings(0xfffe0486, BLINK_ON_TIME, BLINK_OFF_TIME).alter_notification(notification);
    	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;

    	this.mNM.notify(FLICKR_NOTIFICATION_ID, notification);


    	// TODO: Use this technique elsewhere...
//    	LightChanger.LightSettings flickr_pink = new LightChanger.LightSettings(0xfffe0486, 200, 400);
//    	((Crittr) context).getHandler().postDelayed(new LightChanger(notification, context, flickr_pink), 3000);
    	
    	return notification;
    }

	// ========================================================================
	public void produce_updated_notification(UploadProgressPacket packet) {

    	if (packet.eta_millis > 0) {
    		this.cached_notification.contentView.setViewVisibility(R.id.notification_eta_text, View.VISIBLE);
    		this.cached_notification.contentView.setTextViewText(
				R.id.notification_eta_text,
				"ETA: " + DurationStrings.printDuration(packet.eta_millis));
    	} else {
    		this.cached_notification.contentView.setViewVisibility(R.id.notification_eta_text, View.GONE);
    	}
	

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
	}
	
	// ========================================================
	String getMessageText(int progress, int progress_max) {
		String format_string = "%d/%d";
		String message_text = String.format(format_string, progress, progress_max);
		return message_text;
	}
	
	// ========================================================
    public void updateNotificationProgress(UploadProgressPacket packet) {
    	produce_updated_notification(packet);
    	this.mNM.notify(UploadService.ONGOING_NOTIFICATION_ID, this.cached_notification);
    }

	// ========================================================================
	@Override
    public void onPreExecute() {
    	
        try {
			this.flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
			        new REST()
			    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		this.uploader = this.flickr.getUploader();
        
        UploadProgressPacket packet = new UploadProgressPacket(0, 1);
    	produce_updated_notification(packet);
	}

	// ========================================================================
    public long uploadPhoto(ImageUploadData upload) throws IOException, FlickrException, SAXException {
    	

		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();
        

		String stored_auth_token = this.settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        
//        Log.d(TAG, "Stored token: " + stored_auth_token);
        
        auth.setPermission(Permission.WRITE);
        Log.w(TAG, "Just requested write permission...");
        requestContext.setAuth(auth);
    	
    	
    	
        String photoId = null;

        UploadMetaData metaData = new UploadMetaData();
        // TODO: check correct handling of escaped values
        metaData.setTitle(upload.title);
        

		Log.e(Market.TAG, "Setting metaData title as: " + upload.title);
        
        metaData.setDescription(upload.description);

        // Note: the HashSet removes duplicates.
        Collection<String> aggregated_tags = null;
        if (this.batch_tags != null)
        	aggregated_tags = new HashSet<String>(this.batch_tags);
        else
        	aggregated_tags = new HashSet<String>();
        	
        aggregated_tags.addAll(upload.tags);
        metaData.setTags(aggregated_tags);
        
        /*
        String default_tag = settings.getString("default_tag", null);
        
        if (default_tag != null && default_tag.length() > 0) {
        	ArrayList<String> initial_tags = new ArrayList<String>();
            initial_tags.add(default_tag);
            metaData.setTags( initial_tags );
        }
		*/


        boolean upload_public = settings.getBoolean(PrefsUpload.PREFKEY_UPLOAD_PUBLIC, false);
        metaData.setPublicFlag(upload_public);

        String upload_safety_level = settings.getString(PrefsUpload.PREFKEY_UPLOAD_SAFETY_LEVEL, Flickr.SAFETYLEVEL_MODERATE);
        metaData.setSafetyLevel(upload_safety_level);
        
        String upload_content_type = settings.getString(PrefsUpload.PREFKEY_UPLOAD_CONTENT_TYPE, Flickr.CONTENTTYPE_PHOTO);
        metaData.setContentType(upload_content_type);
        
        photoId = uploader.upload(
        		context.getContentResolver().openInputStream( upload.image_uri ),
        		metaData);
    
        return Long.parseLong( photoId );
    }

	// ========================================================================
    void geotagPhoto(long photo_id) throws IOException, SAXException, FlickrException {
		Log.i(TAG, "Now geotagging photo...");
    	
    	LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    	Location last_location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	if (last_location != null) {

        	GeoData geo_loc = new GeoData();
        	geo_loc.setLatitude( (float) last_location.getLatitude() );
        	geo_loc.setLongitude( (float) last_location.getLongitude() );
       		flickr.getGeoInterface().setLocation( Long.toString( photo_id ), geo_loc );

    	} else {
    		String err = "Could not geotag photo!";
    		
    		UploadProgressPacket packet = new UploadProgressPacket(err);
    		Log.e(TAG, err);
    		publishProgress(packet);
    	}
    }

	// ========================================================================
    void addPhotoToGroup(long photo_id) throws IOException, SAXException, FlickrException {
		String default_group_id = settings.getString(PrefsUpload.PREFKEY_UPLOAD_DEFAULT_GROUP, null);
		Log.i(TAG, "Now adding to default group: " + default_group_id);
		
		if (default_group_id != null)
			flickr.getPoolsInterface().add( Long.toString( photo_id ), default_group_id);
    }

	// ========================================================================
    void addPhotoToSet(long photo_id) throws IOException, SAXException, FlickrException {

		String default_set_id = settings.getString(PrefsUpload.PREFKEY_DEFAULT_UPLOAD_SET, null);
		Log.i(TAG, "Now adding to default set: " + default_set_id);
		
		if (default_set_id != null)
			flickr.getPhotosetsInterface().addPhoto(default_set_id, Long.toString( photo_id ));
    }
    
	// ========================================================================
	@Override
	protected List<Long> doInBackground(Void... params) {

		List<Long> uploaded_photo_ids = new ArrayList<Long>();
		int uploaded_count = 0;
		for (ImageUploadData upload : this.pending_uploads_list) {

			updateUploadStatus(upload, UploadStatus.UPLOADING, ListActivityPhotoTags.INVALID_PHOTO_ID);
			publishProgress(new UploadProgressPacket(uploaded_count, this.pending_uploads_list.size()));
			
			long photo_id = ListActivityPhotoTags.INVALID_PHOTO_ID;
			try {
				photo_id = uploadPhoto(upload);
				
				boolean geotag_uploads = settings.getBoolean(PrefsUpload.PREFKEY_UPLOAD_ENABLE_GEOTAGGING, false);
				if (geotag_uploads)
					geotagPhoto(photo_id);
				
				boolean group_auto_add = settings.getBoolean(PrefsUpload.PREFKEY_UPLOAD_GROUP_AUTO_ADD, false);
				if (group_auto_add)
					addPhotoToGroup(photo_id);
				
				boolean set_auto_add = settings.getBoolean(PrefsUpload.PREFKEY_UPLOAD_SET_AUTO_ADD, false);
				if (set_auto_add)
					addPhotoToSet(photo_id);

			} catch (FlickrException e) {

				e.printStackTrace();
				updateUploadStatus(upload, UploadStatus.FAILED, ListActivityPhotoTags.INVALID_PHOTO_ID);
				publishProgress();
				continue;

			} catch (SAXException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				
				e.printStackTrace();
				updateUploadStatus(upload, UploadStatus.FAILED, ListActivityPhotoTags.INVALID_PHOTO_ID);
				publishProgress();
			}

			
			uploaded_photo_ids.add(photo_id);
			updateUploadStatus(upload, UploadStatus.COMPLETE, photo_id);
			
			uploaded_count++;
		}
		
		publishProgress(new UploadProgressPacket(uploaded_count, this.pending_uploads_list.size()));
		
		return uploaded_photo_ids;
	}

	// ========================================================================
	void failTask(String error_message) {
		Toast.makeText(context, error_message, Toast.LENGTH_LONG).show();
	}
	
	// ========================================================================
    @Override
    public void onProgressUpdate(UploadProgressPacket... packets) {

    	if (packets.length > 0) {
    		UploadProgressPacket packet = packets[0];
    		
    		if (packet.error_message != null && packet.error_message.length() > 0) {
    			
    			failTask(packet.error_message);
        	} else {
        		
        		if (packet.progress_value > 0 && packet.max_value > 0) {
            		
       			
        			try {
        				this.eta_window.addSpan(packet.progress_value, SystemClock.uptimeMillis());
        				packet.eta_millis = this.eta_window.getEtaMillis(packet.max_value - packet.progress_value);
        			} catch (UninitializedException e) {
        				e.printStackTrace();
        			}
        			
        			updateNotificationProgress(packet);
        		}
        		
        		if (refresher != null)
        			refresher.refresh();
        	}
    	}
    }

	// ========================================================================
    @Override
    public void onPostExecute(List<Long> photo_ids) {

    }
}
