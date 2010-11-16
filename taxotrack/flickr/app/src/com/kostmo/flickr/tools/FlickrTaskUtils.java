package com.kostmo.flickr.tools;

import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;

import com.aetrion.flickr.photos.GeoData;
import com.google.android.maps.GeoPoint;
import com.kostmo.flickr.bettr.Market;

public class FlickrTaskUtils {

	static final String TAG = Market.DEBUG_TAG; 
	
	
	
	
	
	// Some non-flickr routines:
	
		
	
	
	// =====================================
	
	
	
	public static class ProgressPacket {
		public int download_count;
		public String last_downloaded_item;
		
		public ProgressPacket(int count, String item) {
			download_count = count;
			last_downloaded_item = item;
		}
	}
	
	
	
	public static class GeoBox {
		
		public GeoPoint bottom_left;
		public GeoPoint top_right;
		
		public GeoBox(GeoPoint bl, GeoPoint tr) {
			bottom_left = bl;
			top_right = tr;
		}
	}
	
	public static GeoPoint GeoData_to_GeoPoint(GeoData data) {

		return new GeoPoint((int) (data.getLatitude()*1E6), (int) (data.getLongitude()*1E6));
	}
	
	

    public static float squared_distance(Point p1, Point p2) {
    	float dx = p2.x - p1.x;
    	float dy = p2.y - p1.y;
    	return dx*dx + dy*dy;
    }
    
    
    static float squared_distanceF(PointF p1, PointF p2) {
    	float dx = p2.x - p1.x;
    	float dy = p2.y - p1.y;
    	return dx*dx + dy*dy;
    }
    
	
	static String LocationToString(Location loc) {
		
		return "(" + loc.getLatitude() + ", " + loc.getLongitude() + ")";
	}
	
	
	public static String GeoPointToString(GeoPoint loc) {
		
		return "(" + loc.getLatitudeE6() / 1E6f + ", " + loc.getLongitudeE6() / 1E6f + ")";
	}
	
}
