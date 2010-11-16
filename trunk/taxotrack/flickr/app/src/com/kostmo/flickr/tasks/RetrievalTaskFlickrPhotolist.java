package com.kostmo.flickr.tasks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.widget.TextView;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.SearchParameters;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;
import com.kostmo.flickr.tools.FlickrFetchRoutines;


public class RetrievalTaskFlickrPhotolist extends RetrievalTaskFlickrPhotosAbstract {

	
	SearchParameters task_search_params;
	
	public RetrievalTaskFlickrPhotolist(
			Context context,
			RefreshablePhotoListAdapter adapter,
			TextView grid_title,
			SearchParameters task_search_params,
			int current_page,
			int photos_per_page,
			AtomicInteger task_semaphore) {
		
		super(context,
			adapter,
			grid_title,
			current_page,
			photos_per_page,
			task_semaphore);
		

		this.task_search_params = task_search_params;
	}
    
	@Override
	protected void getFlickrPhotoMatches() throws FlickrException {

		this.task_search_params.setPrivacyFilter( settings.getString("search_privacy_level", Integer.toString( Flickr.PRIVACY_LEVEL_NO_FILTER ) ) );
    	
		this.task_search_params.setIsCommons( settings.getBoolean("commons_only", false) );
        

        // See bug http://code.google.com/p/android/issues/detail?id=2096
//		search_params.setContentType( Integer.toString( settings.getInt("content_type", Integer.parseInt(Flickr.CONTENTTYPE_PHOTO)) ) );
		this.task_search_params.setContentType( settings.getString("content_type", Flickr.CONTENTTYPE_PHOTO) );

//		search_params.setGeoContext( Integer.toString( settings.getInt("geo_context", Integer.parseInt(Flickr.GEOCONTEXT_NOT_DEFINED)) ) );
		this.task_search_params.setGeoContext( settings.getString("geo_context", Flickr.GEOCONTEXT_NOT_DEFINED) );
        
//		search_params.setSafeSearch( Integer.toString( settings.getInt("safe_search", Integer.parseInt(Flickr.SAFETYLEVEL_SAFE)) ) );
		this.task_search_params.setSafeSearch( settings.getString("safe_search", Flickr.SAFETYLEVEL_SAFE) );
        
        

		
		this.task_search_params.setExtrasOwnerName(true);

		
		PhotoList flickr_search_resultset = FlickrFetchRoutines.getSingleFlickrResultPage(this, context, task_search_params, photos_per_page, current_page);

		this.photo_count = flickr_search_resultset.size();
		this.total_matches = flickr_search_resultset.getTotal();
		
		this.publishProgress( (List<Photo>) flickr_search_resultset );
    }
}