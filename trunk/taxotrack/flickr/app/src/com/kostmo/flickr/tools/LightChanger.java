package com.kostmo.flickr.tools;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;


public class LightChanger implements Runnable {

	
	public static class LightSettings {
		
		int ledARGB, ledOnMS, ledOffMS;

		public LightSettings(int color, int on, int off) {
			ledARGB = color;
			ledOnMS = on;
			ledOffMS = off;	
		}
		
		public void alter_notification(Notification notification) {
	    	notification.ledARGB = ledARGB;
	    	notification.ledOnMS = ledOnMS;
	    	notification.ledOffMS = ledOffMS;
		}
	}

	int notification_id;

	
	LightSettings new_light_settings;
	Notification notification;
	Context context;
	LightChanger(Notification n, int notification_id, Context c, LightSettings l) {
		notification = n;
		
		this.notification_id = notification_id;
		context = c;
		new_light_settings = l;
	}
	
	
	public void run() {

		Log.d("FlickrAPI", "Running light changer...");
		
		new_light_settings.alter_notification(notification);
		
		// Disable the sound for this "lights" transition
    	notification.defaults &= ~Notification.DEFAULT_SOUND;
    	
		
		

    	NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	mNotificationManager.notify(notification_id, notification);
	 }
};