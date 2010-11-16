package org.crittr.flickr;

import java.util.List;

import android.graphics.PointF;
import android.location.Location;

import com.aetrion.flickr.photos.GeoData;
import com.google.android.maps.GeoPoint;
import org.crittr.containers.MapPoint;
import org.crittr.containers.PhotoContainer;

import android.graphics.Point;

public class FlickrTaskUtils {
	
	static final String TAG = "FlickrAPI";
	
	
	
	
	
	
	
	
	
	// Some non-flickr routines:
	
	
	
	public static GeoBox growBoundingBox(List<? extends MapPoint> photo_list) {
		
		float min_lat = Float.POSITIVE_INFINITY;
		float max_lat = Float.NEGATIVE_INFINITY;
		
		float min_lon = Float.POSITIVE_INFINITY;
		float max_lon = Float.NEGATIVE_INFINITY;
		
		// Grow the bounding box.
		for (MapPoint ptp : photo_list) {
			
			float lat = ptp.getGeoPoint().getLatitudeE6();
			min_lat = Math.min(min_lat, lat);
			max_lat = Math.max(max_lat, lat);

			float lon = ptp.getGeoPoint().getLongitudeE6();
			min_lon = Math.min(min_lon, lon);
			max_lon = Math.max(max_lat, lon);
		}
		
		GeoPoint bottom_left = new GeoPoint( (int) (min_lat * 1E6), (int) (min_lon * 1E6) );
		GeoPoint top_right = new GeoPoint( (int) (max_lat * 1E6), (int) (max_lon * 1E6) );
		
		return new GeoBox(bottom_left, top_right);
		
	}
	
	
	
	
	
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
