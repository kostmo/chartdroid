package com.kostmo.flickr.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.aetrion.flickr.photos.Photo;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.ViewHolderFlickrPhoto;
import com.kostmo.flickr.graphics.CornerDecorationDrawable;
import com.kostmo.flickr.tasks.AsyncPhotoPopulator;

public class FlickrPhotoDualAdapter extends FlickrPhotoAdapter {

	private boolean grid_mode = false;
	

	static final String TAG = Market.DEBUG_TAG; 
	
	
    final private LayoutInflater mInflater;
	
    public FlickrPhotoDualAdapter(Context context) {
    	super(context);

        mInflater = LayoutInflater.from(context);
    }
    
    
    
    
    // Apparently this works?
	public void gridModeToggle(boolean new_sate) {

		if (grid_mode && !new_sate) {
			
			// Refresh the view
			List<Photo> backup_list = new ArrayList<Photo>();
			backup_list.addAll( photo_list );
			
			photo_list.clear();
			this.notifyDataSetInvalidated();
			
			photo_list.addAll( backup_list );
			
			this.notifyDataSetInvalidated();
			
			Log.e("Flickr", "Did some kind of refreshing trick.  Did it work?");
		}
		
		grid_mode = new_sate;
	}
    
    

	public boolean isGridMode() {
		return grid_mode;
	}
    
    
    

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {

    	if (grid_mode) {
    	
	    	
	    	ViewHolderFlickrPhoto holder;
	        if (convertView == null) {
	//            convertView = mInflater.inflate(R.layout.list_item_taxon, null);
	        	
	        	convertView = new ImageView(context);
	
	            // Creates a ViewHolder and store references to the two children views
	            // we want to bind data to.
	            holder = new ViewHolderFlickrPhoto();
	            holder.thumbnail = (ImageView) convertView;
	            holder.thumbnail.setLayoutParams(new GridView.LayoutParams(100, 100));
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
	    	
	    	
	
	
	        photo_populator.fetchDrawableOnThread( (Photo) photo_list.get(position), holder);
	
	        return convertView;

	        
    	} else {
    		
        	return getViewFlickrItem(photo_list.get(position), convertView, mInflater, photo_populator, context);
        	
    	}
    }
    

    
    // ===============================
    
    public static View getViewFlickrItem(Photo pc, View convertView, LayoutInflater inflator, AsyncPhotoPopulator photopopulator, Context context) {

    	
        ViewHolderFlickrPhoto holder;
        if (convertView == null) {
            convertView = inflator.inflate(R.layout.list_item_flickr_photo, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolderFlickrPhoto();
            holder.title = (TextView) convertView.findViewById(R.id.flickr_photo_title);
            holder.description = (TextView) convertView.findViewById(R.id.flickr_photo_description);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.flickr_photo_thumbnail);
            holder.owner = (TextView) convertView.findViewById(R.id.flickr_photo_owner);

            convertView.setTag(holder);

        } else {

            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolderFlickrPhoto) convertView.getTag();
        }

        prepareViewFlickrItem(pc, holder, photopopulator, context);

        
        convertView.setBackgroundDrawable(new CornerDecorationDrawable(context, convertView, Color.BLACK));

        return convertView;
    }


    // ===============================
	
    public static void prepareViewFlickrItem(Photo photo, ViewHolderFlickrPhoto holder, AsyncPhotoPopulator photopopulator, Context context) {
        
        if (holder.owner != null)
        	holder.owner.setText(photo.getOwner().getUsername());
//        Log.d(TAG, "I requested the username, it should be here: " + photo.getOwner().getUsername());
        
        String photo_id = photo.getId();
        
        
        if (holder.title == null)
        	Log.d(TAG, "Uh-oh, I'm about to call fetchPhotoInfoOnThread() with a null 'title' TextView");
        
        photopopulator.fetchPhotoInfoOnThread(photo_id, holder);
        

        
        // FIXME: This is all convoluted -- it fetches the Photo container back from Flickr,
        // but we already have that!  It needs to be simplified!
        photopopulator.fetchDrawableOnThread(photo, holder);
        
    }
}