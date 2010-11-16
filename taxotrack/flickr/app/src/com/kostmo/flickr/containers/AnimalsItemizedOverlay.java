package com.kostmo.flickr.containers;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.aetrion.flickr.photos.Photo;
import com.google.android.maps.ItemizedOverlay;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.tools.FlickrTaskUtils;

public class AnimalsItemizedOverlay extends ItemizedOverlay<OverlayItemWithPhoto> {

	static final String TAG = Market.DEBUG_TAG; 
	
	public ArrayList<OverlayItemWithPhoto> mOverlays = new ArrayList<OverlayItemWithPhoto>();
	
	
	List<? extends Photo> animal_geopoints;
	
	
	
	
	
	public AnimalsItemizedOverlay(Drawable defaultMarker) {
//		super(boundCenter(defaultMarker));
		super(boundCenterBottom(defaultMarker));
	}

	
	
	
	public void refreshItems(List<Photo> newPhotoList) {
		
		animal_geopoints = newPhotoList;

	    mOverlays.clear();
	    int i = 0;
		for (Photo p : newPhotoList) {
			

			String bubble_title = null, bubble_snippet = null;
			
			if (p != null) {
				bubble_title = p.getTitle();
				bubble_snippet = p.getDescription();
				
			} else {
				Log.e(TAG, "Null photo!");
				
			}

			
			if ( p.getGeoData() == null) {

				Log.e(TAG, "Null geodata for photo: " + p.getId());
				
			} else {
				
				OverlayItemWithPhoto taxon_overlay_item = new OverlayItemWithPhoto(
						FlickrTaskUtils.GeoData_to_GeoPoint( p.getGeoData() ),
						bubble_title,
						bubble_snippet,
						p);
				
				addOverlay( taxon_overlay_item );
	
				i++;
			}
		}
	    
	    populate();
	}

	public void refreshItems(ArrayList<OverlayItemWithPhoto> overlays) {
		
	    mOverlays.clear();
	    mOverlays.addAll(overlays);
	    
	    populate();
	}
	
	
	
	public void addOverlay(OverlayItemWithPhoto overlay) {
	    mOverlays.add(overlay);
	    
	    
	    populate();
	}
	
	
	@Override
	protected OverlayItemWithPhoto createItem(int i) {
	  return mOverlays.get(i);
	}


	@Override
	public int size() {
		return mOverlays.size();
	}
}
