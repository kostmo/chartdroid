package org.crittr.containers;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.tags.Tag;
import com.google.android.maps.GeoPoint;

import org.crittr.browse.R;
import org.crittr.flickr.FlickrTaskUtils;
import org.crittr.shared.MachineTag;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MapPointFlickrPhoto extends MapPoint implements PhotoContainer {

	Date date;
	long tsn;
	
	
	static final String TAG = "Crittr";
	
	GeoPoint overriding_geopoint = null;
	
	IdentificationStatus id_status = IdentificationStatus.INVALID_STATUS;
	

	private Photo photo;
	
	public Photo getPhoto() {
		return photo;
	}
	
	public void setPhoto(Photo photo) {
		this.photo = photo;
	}
	
	@Override
	public int getDrawableId() {

		return R.drawable.ic_delete;
	}


	public void setGeoPoint(GeoPoint g) {
		this.overriding_geopoint = g;
	}
	
	
	
	public GeoPoint getGeoPoint() {
		
		// FIXME This is an ugly kludge
		if (overriding_geopoint != null)
			return overriding_geopoint;
		
		return FlickrTaskUtils.GeoData_to_GeoPoint( photo.getGeoData() );
	}

	public IdentificationStatus getIdStatus() {
		
		return id_status;
	}

	
	
	public static IdentificationStatus determine_id_status(Collection<Tag> taglist) {
		// ID status states in order of decreasing precedence:
		// "contested", "identified", "unidentified"
		Map<String, Integer> precedence = new HashMap<String, Integer>();
		for (int i=0; i<3; i++) {
			String status = IdLabelMap[i];
			precedence.put(status, i);
		}
		
		String value_string = null;
		int highest_precedence = 0; 
		for (Tag t : taglist) {

//			Log.e(TAG, "Machine tag value: " + t.getValue());

			MachineTag mt = new MachineTag(t.getValue());

			if (mt.namespace.equals("identification") && mt.predicate.equals("status")) {
				
				value_string = mt.value.toLowerCase();
				
//				Log.i(TAG, "This tag is identification status.");

				if (precedence.containsKey(value_string)) {
					int this_precedence = precedence.get(value_string);
					highest_precedence = Math.max(this_precedence, highest_precedence);
				}
			}
		}
		
		return IdentificationStatus.values()[highest_precedence];
	}
	
	
	public void setIdStatus(IdentificationStatus idStatus) {
		// FIXME - We ignore the method argument.
		
		id_status = determine_id_status(photo.getTags());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public long getTSN() {
		return tsn;
	}

	
	public void setTSN(long tsn) {
		this.tsn = tsn;
	}
	
	
	public void setDate(long timestamp) {
		
		date = new Date( timestamp * 1000 );
	}
	
	
	public Date getDate() {
		return date;
	}
	
	
	
	public void setGeo(float lat, float lon) {
		overriding_geopoint = new GeoPoint( (int) (lat * 1E6), (int) (lon * 1E6) );
	}
}