package com.kostmo.flickr.containers;

import java.util.List;

import com.aetrion.flickr.photos.Photo;


public interface RefreshablePhotoListAdapter {

	public void setTotalResults(long total_results);
	
	public void refresh_list(List<Photo> new_photo_list);
	public void clear_list();
	
}
