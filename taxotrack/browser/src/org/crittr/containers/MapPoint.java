package org.crittr.containers;

import org.crittr.browse.R;

import com.google.android.maps.GeoPoint;

abstract public class MapPoint {

	public abstract GeoPoint getGeoPoint();
	
	public int getDrawableId() {

		return R.drawable.ic_delete;
	}
}