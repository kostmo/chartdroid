package com.kostmo.commute.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.activity.Disablable;
import com.kostmo.commute.activity.Main;
import com.kostmo.commute.activity.TripSummaryActivity;
import com.kostmo.commute.activity.DestinationPairAssociator.AddressPair;
import com.kostmo.commute.activity.prefs.TriggerPreferences;
import com.kostmo.commute.provider.DatabaseCommutes;

/** This service should be started as "Foreground" with a notification.
 * It can either actively track a route (with breadcrumbs), or simply hold a
 * reference to a the PendingIntent for a ProximityAlert, in case the user
 * wants to cancel it.
 * 
 * @author kostmo
 *
 */
public class RouteTrackerService extends Service {

	static final String TAG = Market.TAG;

	public final static int ONGOING_NOTIFICATION_ID = 1;

	

	DatabaseCommutes database;
	SharedPreferences settings;
	LocationManager location_manager;

	Disablable disablable_host = null;
    Map<Long, PendingIntent> arrival_pending_intents = new HashMap<Long, PendingIntent>();

	// ========================================================================
	public void setDisablableHost(Disablable host) {
		this.disablable_host = host;
	}


	// ========================================================================
    @Override
    public void onCreate() {

    	this.database = new DatabaseCommutes(this);
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);

    	this.location_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

	// ========================================================================
    public PendingIntent makePendingIntent(long trip_id) {

    	if ( this.arrival_pending_intents.containsKey(trip_id) ) {
    		return this.arrival_pending_intents.get(trip_id);
    		
    	} else {

        	Intent activity_intent = new Intent(this, TripSummaryActivity.class);
        	activity_intent.putExtra(TripSummaryActivity.EXTRA_TRIP_ID, trip_id);
        	
        	
        	PendingIntent pending_intent = PendingIntent.getActivity(this, 0, activity_intent, PendingIntent.FLAG_UPDATE_CURRENT);
        	this.arrival_pending_intents.put(trip_id, pending_intent);
        	
        	return pending_intent;	
    	}
    }

	// ========================================================================
    public void startTrip(long route_id, boolean return_trip) {
    	
    	// NOTE:
    	// Whether or not you want to track breadcrumbs during the trip or just get a notification at the end
    	// with a ProximityAlert, we must still use a service (with Foreground notification).  This is because
    	// we must cancel ProximityAlerts using the original object, and the lifetime of an Activity may be
    	// too short to preserve a reference to that object.

		
    	

    	Notification notification_message = buildNotificationMessageOnly("foo bar");
    	

		startService(new Intent(this, this.getClass()));
    	startForeground(ONGOING_NOTIFICATION_ID, notification_message);

    	
    	
		// TODO Register a location listener for the destination
		
		if (this.settings.getBoolean(TriggerPreferences.PREFKEY_ENABLE_RECORD_BREADCRUMBS, TriggerPreferences.DEFAULT_ENABLE_RECORD_BREADCRUMBS)) {
			startService(new Intent(this, RouteTrackerService.class));
			
		} else {
			long trip_id = this.database.startTrip(route_id);
			
	    	
	    	float radius = this.settings.getFloat(TriggerPreferences.PREFKEY_TRIP_COMPLETION_RADIUS, TriggerPreferences.DEFAULT_TRIP_COMPLETION_RADIUS);
	    	long expiration = this.settings.getLong(TriggerPreferences.PREFKEY_TRIP_EXPIRATION_MS, TriggerPreferences.DEFAULT_TRIP_EXPIRATION_MS);
	    	
	    	
			AddressPair pair = this.database.getAddressPair(route_id);
			
			

	        PendingIntent arrival_pending_intent = makePendingIntent(trip_id);
	    	this.location_manager.addProximityAlert(pair.destination.latlon.lat, pair.destination.latlon.lon, radius, expiration, arrival_pending_intent);
		}
    }

	// ========================================================
    public void cancelAllProximityAlerts() {
    	
    	Log.d(TAG, "Cancelling all proximity alerts...");
    	for (Entry<Long, PendingIntent> entry : this.arrival_pending_intents.entrySet()) {
    		this.location_manager.removeProximityAlert( entry.getValue() );
    	}
    	
    	Log.d(TAG, "Stopping service...");
		stopForeground(true);	// Is this necessary?
		stopSelf();
    }
    
	// ========================================================
    // TODO Use me
    public void cancelActiveProximityAlert(long trip_id) {
    
    	
    	if (this.arrival_pending_intents.containsKey(trip_id)) {
    		location_manager.removeProximityAlert( this.arrival_pending_intents.get(trip_id) );
    	} else {
    		Log.e(TAG, "Could not cancel proximity alert for trip " + trip_id);
    	}
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

		Log.d(TAG, "Called onStart() in RouteTrackerService.");
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
        return this.mBinder;
	}
	

	// ========================================================================
    @Override
    public void onDestroy() {

        // Tell the user we stopped.
        Toast.makeText(this, "The service was destroyed.", Toast.LENGTH_SHORT).show();
    }
}