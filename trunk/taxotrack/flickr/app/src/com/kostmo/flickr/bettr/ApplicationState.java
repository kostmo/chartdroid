package com.kostmo.flickr.bettr;

import android.app.Application;
import android.graphics.Bitmap;

import com.aetrion.flickr.photos.Photo;

public class ApplicationState extends Application {

	public static String active_filename;
	
	public Photo active_flickr_photo;
	public Bitmap active_thumbnail_bitmap;
	
	@Override
	public void  onCreate() {
		super.onCreate();
	}
}
