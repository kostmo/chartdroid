package org.crittr.browse;

import java.util.ArrayList;
import java.util.List;

import org.crittr.containers.ThumbnailUrlPlusLinkContainer;
import org.crittr.shared.browser.containers.ViewHolderFlickrPhoto;
import org.crittr.shared.browser.utilities.FlickrPhotoDrawableManager;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ThumbGridAdapter extends BaseAdapter {

	FlickrPhotoDrawableManager drawable_manager;
	
	List<ThumbnailUrlPlusLinkContainer> photo_list = new ArrayList<ThumbnailUrlPlusLinkContainer>();
	
	Context context;
    public ThumbGridAdapter(Context context) {
    	this.context = context;
    	drawable_manager = new FlickrPhotoDrawableManager(context);
    }
    
    

    public void add_photopairs(List<ThumbnailUrlPlusLinkContainer> new_pairs) {

    	photo_list.addAll(new_pairs);
    	this.notifyDataSetChanged();
    }
    
    
    public int getCount() { return photo_list.size(); }

    
    public Object getItem(int position) { return photo_list.get(position); }
    public long getItemId(int position) { return position; }

    public View getView(int position, View convertView, ViewGroup parent) {

    	
    	ViewHolderFlickrPhoto holder;
        if (convertView == null) {
//            convertView = mInflater.inflate(R.layout.list_item_taxon, null);
        	
        	convertView = new ImageView(context);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolderFlickrPhoto();
            holder.thumbnail = (ImageView) convertView;
            holder.thumbnail.setLayoutParams(new GridView.LayoutParams(90, 90));
//            holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
//            holder.thumbnail.setPadding(8, 8, 8, 8);

            holder.thumbnail.setBackgroundResource(R.drawable.picture_frame);
            
            // This is a bit convoluted, since our view holder right now only contains a single view.
            convertView.setTag(holder);

        } else {

            // Get the ViewHolder back to get fast access to the View elements.
            holder = (ViewHolderFlickrPhoto) convertView.getTag();
        }
    	
    	


	    drawable_manager.fetchDrawableOnThread( (ThumbnailUrlPlusLinkContainer) photo_list.get(position), holder);

        return convertView;
    }
}