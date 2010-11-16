package com.kostmo.flickr.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

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
import android.text.TextUtils;
import android.util.Log;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.activity.Event;
import com.aetrion.flickr.activity.Item;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.activity.TabbedPhotoPageActivity;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.data.BetterMachineTagDatabase;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tasks.PhotoMultiUploadTask;
import com.kostmo.flickr.tools.LightChanger;

public class CheckUpdateService extends Service {

	public static final String PREFKEY_ENABLE_HOURLY_CHECKIN = "enable_hourly_checkin";
	

    static int TAGS_MOD_NOTIFICATION_ID = 1;


    
	static final String TAG = Market.DEBUG_TAG; 
    

    private CheckForUpdatesTask mTask;
    String duration_string;
    
    
    @Override
    public void onCreate() {
        super.onCreate();
//        Log.e(TAG, "CREATED mY sWeEt ApPlIcAtIoN!");
    }

    
    
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        
//		Log.e(TAG, "StArTeD mY sWeEt ApPlIcAtIoN!");
        
        (mTask = new CheckForUpdatesTask()).execute();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            mTask.cancel(true);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void schedule(Context context) {
        final Intent intent = new Intent(context, CheckUpdateService.class);
        final PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

        
//        Log.e(TAG, "I am about to schedule a Flickr checkup...");


        final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        alarm.cancel(pending);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hourly_checkin_enabled = prefs.getBoolean(PREFKEY_ENABLE_HOURLY_CHECKIN, true);

        if (hourly_checkin_enabled) {
        	
        	long checkin_interval;
        	
        	String default_rate_string = context.getResources().getString(R.string.frequency_hourly);
        	String frequency_string = prefs.getString("checkin_update_rate", default_rate_string);
        	
        	if (frequency_string.equals(default_rate_string))
        		checkin_interval = AlarmManager.INTERVAL_HOUR;
        	else
        		checkin_interval = AlarmManager.INTERVAL_DAY;
        	
        	
        	alarm.setInexactRepeating(

        		AlarmManager.ELAPSED_REALTIME_WAKEUP,
        		0,
        		checkin_interval,
//        		AlarmManager.INTERVAL_FIFTEEN_MINUTES,	// FIXME - DEBUG ONLY
        		pending);
        	
        } else {
        	
        	alarm.cancel(pending);
        }


    }

    private class CheckForUpdatesTask extends AsyncTask<Void, Long, Void> {
        private SharedPreferences mPreferences;
        private NotificationManager mManager;

        Map<String, Integer> eventHistogram = new HashMap<String, Integer>();
        Map<String, Integer> itemHistogram = new HashMap<String, Integer>();

        public void onPreExecute() {
        	
//        	Log.d(TAG, "Doing update ChEcKeR tAsK!");

        	mPreferences = PreferenceManager.getDefaultSharedPreferences(CheckUpdateService.this);
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public Void doInBackground(Void... params) {

//        	Log.i(TAG, "RUnnING mY KeWL THREad In tHE bACkGrOuND!");


        	
        	BetterMachineTagDatabase bmtd = new BetterMachineTagDatabase(CheckUpdateService.this);
        	Date last_checkin_time = bmtd.getLastCheckinTime();
        	
//        	Log.d(TAG, "Last checkin time: " + last_checkin_time);
//        	Log.d(TAG, "Last checkin ms: " + last_checkin_time.getTime());
        	
        	Date current_date = new Date();
        	
//        	Log.d(TAG, "Curent time: " + current_date);
//        	Log.d(TAG, "Curent ms: " + current_date.getTime());
        	
        	long millisecond_difference = current_date.getTime() - last_checkin_time.getTime();

//        	Log.d(TAG, "Millisecond difference: " + millisecond_difference);
        	
        	// Round up to the nearest hour:
        	float milliseconds_per_hour = 1000 * 60 * 60;
        	
        	float partial_answer = millisecond_difference / milliseconds_per_hour;
//        	Log.d(TAG, "Float division: " + partial_answer);
        	int hour_count = (int) Math.ceil(partial_answer);
        	
        	
//        	String duration_string = "1h";
//        	String duration_string = "48h";
        	
        	Log.d(TAG, "Milliseconds since last update: " + millisecond_difference);
        	duration_string = hour_count + "h";
        	
        	Log.d(TAG, "Querying for photo activity over the last " + duration_string);

    		Flickr flickr = null;
            try {
    			flickr = new Flickr(
    					ApiKeys.FLICKR_API_KEY,	// My API key
    					ApiKeys.FLICKR_API_SECRET,	// My API secret
    		        new REST()
    		    );
    		} catch (ParserConfigurationException e) {
    			e.printStackTrace();
    		}
    		
    		RequestContext requestContext = RequestContext.getRequestContext();
            Auth auth = new Auth();

    		String stored_auth_token = mPreferences.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
            auth.setToken( stored_auth_token );
            auth.setPermission(Permission.READ);
            requestContext.setAuth(auth);

            Item final_item = null;
    		try {
    			
    			Log.d(TAG, "Checking items.");
    			Collection<Item> recent_activity = (ArrayList<Item>) flickr.getActivityInterface().userPhotos(10, 1, duration_string);
    			
    			
    			for (Item item : recent_activity) {
    				Collection<Event> events = (ArrayList<Event>) item.getEvents();

					int newcount1 = 1;
					if (itemHistogram.containsKey(item.getType())) {
						newcount1 = itemHistogram.get(item.getType()) + 1; 
					}
					itemHistogram.put(item.getType(), newcount1);
    				
    					

//        			Log.i(TAG, "Found an item.");
    				
    				for (Event event : events) {

    					int newcount = 1;
    					if (eventHistogram.containsKey(event.getType())) {
    						newcount = eventHistogram.get(event.getType()) + 1; 
    					}
						eventHistogram.put(event.getType(), newcount);
    					
    					
    					Log.e(TAG, item.getType() + " " + item.getId() + " had " + event.getType() + " event: Value: " + event.getValue() + "; By: " + event.getUsername());
    					
    					final_item = item;
    				}
    			}
    			
    			
    			
    			
    			
        		long photo_id = -1;
        		if (final_item != null) {
        			
        			photo_id = Long.parseLong( final_item.getId() );
    	    		publishProgress( photo_id );
        	
        		}
    			bmtd.updateLastCheckinTime( photo_id );

    			
//    			Log.d(TAG, "Now the timestamp should have been updated...");
            	Date last_checkin_time_updated = bmtd.getLastCheckinTime();

//            	Log.d(TAG, "Last checkin time (updated): " + last_checkin_time_updated);
    			
    			
    		} catch (IOException e) {
    			e.printStackTrace();
    		} catch (SAXException e) {
    			e.printStackTrace();
    		} catch (FlickrException e) {
    			e.printStackTrace();
    		}

            return null;
        }

        
        public void onProgressUpdate(Long... values) {

//			Log.d(TAG, "tRiGgErEd Progress UPDATE");
        	do_notify(values[0]);
        }

        
        public void onPostExecute(Void aVoid) {
            stopSelf();
        }
        
        
        

        
        void do_notify(long photo_id) {

//        	int icon = PhotoUploadTask.flickr_tray_icon_resource;        // icon from resources
        	int icon = R.drawable.flickr_notification;
        	
        	CharSequence tickerText = "New photo activity from past " + duration_string;	// ticker-text
        	long when = System.currentTimeMillis();         // notification time
//        	Context context = getApplicationContext();      // application Context
        	
        	
        	

        	
        	
        	
        	
//        	String contentTitle = "Photos have activity";  // expanded message title
			String contentTitle;
        	List<String> info_el1 = new ArrayList<String>();
        	for (Entry<String, Integer> entry : itemHistogram.entrySet()) {
        		String elstring = entry.getValue() + " " + entry.getKey() + (entry.getValue() != 1 ? "s" : "");
        		info_el1.add( elstring );
        	}
        	contentTitle = TextUtils.join(", ", info_el1) + " with activity";
				
				
				
//        	CharSequence contentText = "Photo ID: " + photo_id;      // expanded message text
//        	CharSequence contentText = "There is activity your photos!";      // expanded message text

        	String contentText;
        	List<String> info_el = new ArrayList<String>();
        	for (Entry<String, Integer> entry : eventHistogram.entrySet()) {
        		String elstring = entry.getValue() + " " + entry.getKey() + (entry.getValue() != 1 ? "s" : "");
        		info_el.add( elstring );
        	}
        	contentText = TextUtils.join(", ", info_el);
        	
        	
        	// Bring the user to the Tags page when clicked.
        	Intent notificationIntent = new Intent(CheckUpdateService.this, TabbedPhotoPageActivity.class);
	        notificationIntent.putExtra(IntentConstants.PHOTO_ID, photo_id);


        	PendingIntent contentIntent = PendingIntent.getActivity(CheckUpdateService.this, 0, notificationIntent, 0);

        	// the next two lines initialize the Notification, using the configurations above
        	Notification notification = new Notification(icon, tickerText, when);
        	notification.setLatestEventInfo(CheckUpdateService.this, contentTitle, contentText, contentIntent);
        	notification.defaults |= Notification.DEFAULT_SOUND;
        	
        	// This doesn't help!
//        	notification.defaults |= Notification.FLAG_ONLY_ALERT_ONCE;

        	// "Flickr pink"
//        	notification.ledARGB = 0xfffe0486;
        	
        	// "Flickr blue"
//        	notification.ledARGB = 0xff0465dc;
        	
        	// "Flickr pink"
        	new LightChanger.LightSettings(0xfffe0486, PhotoMultiUploadTask.BLINK_ON_TIME, PhotoMultiUploadTask.BLINK_OFF_TIME).alter_notification(notification);
        	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        	notification.flags |= Notification.FLAG_AUTO_CANCEL;


        	mManager.notify(TAGS_MOD_NOTIFICATION_ID, notification);
        }
    }
}
