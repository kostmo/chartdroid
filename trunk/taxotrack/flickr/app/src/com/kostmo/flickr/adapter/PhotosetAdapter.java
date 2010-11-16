package com.kostmo.flickr.adapter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photosets.Photoset;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.ViewHolderFlickrPhoto;
import com.kostmo.flickr.graphics.CornerDecorationDrawable;
import com.kostmo.flickr.tasks.AsyncPhotoPopulator;

public class PhotosetAdapter extends BaseAdapter {

	public long total_result_count;
	
	public AsyncPhotoPopulator photo_populator;

    final private LayoutInflater mInflater;

	static final String TAG = Market.DEBUG_TAG; 
	
	public List<Photoset> photo_list = new ArrayList<Photoset>();

	
	public Map<String, SoftReference<Bitmap>> getCachedBitmaps() {
		return photo_populator.bitmapReferenceMap;
	}
	


	public void restoreCachedBitmaps(Map<String, SoftReference<Bitmap>> cache) {
		photo_populator.bitmapReferenceMap = cache;
	}
	
	public void restoreCachedPhotoInfo(Map<String, Photo> cache) {
		photo_populator.photoInfoMap = cache;
	}
	
	
	
	
	Context context;
    public PhotosetAdapter(Context context) {
    	this.context = context;
    	photo_populator = new AsyncPhotoPopulator(context);
    	

        mInflater = LayoutInflater.from(context);
    }

    public int getCount() { return photo_list.size(); }
    public Object getItem(int position) { return photo_list.get(position); }
    public long getItemId(int position) { return position; }

    // ==========================================


	@Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolderFlickrPhoto holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_flickr_photoset, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolderFlickrPhoto();
            holder.title = (TextView) convertView.findViewById(R.id.flickr_photo_title);
            holder.description = (TextView) convertView.findViewById(R.id.flickr_photo_description);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.flickr_photo_thumbnail);

            convertView.setTag(holder);

        } else {

            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolderFlickrPhoto) convertView.getTag();
        }

        
        Photoset set = (Photoset) photo_list.get(position);
        Photo photo = set.getPrimaryPhoto();

        holder.title.setText( set.getTitle() );
        holder.description.setText( set.getDescription() );
        
        
        // FIXME: This is all convoluted -- it fetches the Photo container back from Flickr,
        // but we already have that!  It needs to be simplified!
        photo_populator.fetchDrawableOnThread(photo, holder);
        
        convertView.setBackgroundDrawable(new CornerDecorationDrawable(context, convertView, Color.BLACK));

        return convertView;
    }
}