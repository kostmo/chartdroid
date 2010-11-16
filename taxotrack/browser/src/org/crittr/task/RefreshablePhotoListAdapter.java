package org.crittr.task;

import java.util.List;

import org.crittr.containers.MapPointFlickrPhoto;

public interface RefreshablePhotoListAdapter {

	
	public void refresh_list(List<MapPointFlickrPhoto> new_photo_list);
	
}
