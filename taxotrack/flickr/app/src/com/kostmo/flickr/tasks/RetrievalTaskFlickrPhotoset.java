package com.kostmo.flickr.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.widget.TextView;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;
import com.kostmo.flickr.tools.FlickrFetchRoutines;


public class RetrievalTaskFlickrPhotoset extends RetrievalTaskFlickrPhotosAbstract {

	String photoset_id;
	
	public RetrievalTaskFlickrPhotoset(
			Context context,
			RefreshablePhotoListAdapter adapter,
			TextView grid_title,
			String photoset_id,
			int current_page,
			int photos_per_page,
			AtomicInteger task_semaphore) {
		
		
		super(context,
				adapter,
				grid_title,
				current_page,
				photos_per_page,
				task_semaphore);
		
		
		this.photoset_id = photoset_id;
	}


	@Override
	protected void getFlickrPhotoMatches() throws FlickrException {

        Set<String> extras = new HashSet<String>();
        
        extras.add("owner_name");
        // Valid extras:
        // license, date_upload, date_taken, owner_name, icon_server, original_format, last_update, geo, tags, machine_tags, o_dims, views, media, path_alias, url_sq, url_t, url_s, url_m, url_o
//		int privacy_filter = Integer.parseInt( settings.getString("search_privacy_level", Integer.toString( Flickr.PRIVACY_LEVEL_NO_FILTER ) ) );
        int privacy_filter = Flickr.PRIVACY_LEVEL_NO_FILTER;


		
		PhotoList flickr_search_resultset = FlickrFetchRoutines.getSingleFlickrPhotosetPage(this, context, photoset_id, extras, privacy_filter, photos_per_page, current_page);

		photo_count = flickr_search_resultset.size();
		total_matches = flickr_search_resultset.getTotal();
		
		this.publishProgress( (List<Photo>) flickr_search_resultset );
    }
}