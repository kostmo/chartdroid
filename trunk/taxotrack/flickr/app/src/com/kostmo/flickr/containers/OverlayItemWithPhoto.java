package com.kostmo.flickr.containers;

import com.aetrion.flickr.photos.Photo;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class OverlayItemWithPhoto extends OverlayItem {
	
	public Photo photo;
	
	public OverlayItemWithPhoto(GeoPoint point, String title, String snippet, Photo p) {
		
		super(point, title, snippet);

		photo = p;
		
	}

}
