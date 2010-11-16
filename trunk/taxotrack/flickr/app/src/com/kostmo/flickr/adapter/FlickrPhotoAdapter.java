package com.kostmo.flickr.adapter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.BaseAdapter;

import com.aetrion.flickr.photos.Photo;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;
import com.kostmo.flickr.tasks.AsyncPhotoPopulator;

public abstract class FlickrPhotoAdapter extends BaseAdapter implements RefreshablePhotoListAdapter {

	public long total_result_count;
	
	public AsyncPhotoPopulator photo_populator;

	public List<Photo> photo_list = new ArrayList<Photo>();

	
	public Map<String, SoftReference<Bitmap>> getCachedBitmaps() {
		return photo_populator.bitmapReferenceMap;
	}
	
	public Map<String, Photo> getCachedPhotoInfo() {
		return photo_populator.photoInfoMap;
	}
	

	public void restoreCachedBitmaps(Map<String, SoftReference<Bitmap>> cache) {
		photo_populator.bitmapReferenceMap = cache;
	}
	
	public void restoreCachedPhotoInfo(Map<String, Photo> cache) {
		photo_populator.photoInfoMap = cache;
	}
	
	
	
	
	Context context;
    public FlickrPhotoAdapter(Context context) {
    	this.context = context;
    	photo_populator = new AsyncPhotoPopulator(context);
    }
    

	public void setTotalResults(long totalResults) {
		total_result_count = totalResults;
		
	}
    
//	@Override
	public void refresh_list(List<Photo> new_photo_list) {

		photo_list.addAll( new_photo_list );

		this.notifyDataSetInvalidated();
		
		Log.e("Flickr", "Refreshed and invalidated.  New size: " + photo_list.size());
	}
    
    
    public void clear_list() {

    	photo_list.clear();
    	this.notifyDataSetChanged();
    }

    
    public int getCount() { return photo_list.size(); }

    
    public Object getItem(int position) { return photo_list.get(position); }
    public long getItemId(int position) { return position; }
}