package com.kostmo.flickr.adapter;

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.comments.Comment;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.ViewHolderFlickrPhoto;
import com.kostmo.flickr.graphics.CornerDecorationDrawable;
import com.kostmo.flickr.tasks.AsyncPhotoPopulator;

public class CommentsAdapter extends BaseAdapter {

	static final String TAG = Market.DEBUG_TAG; 
	
	public long total_result_count;
	
	public AsyncPhotoPopulator photo_populator;
    private LayoutInflater mInflater;

    
    Flickr flickr;
    DateFormat date_format;
	Context context;
    public CommentsAdapter(Context context, Flickr flickr) {
    	this.context = context;
    	photo_populator = new AsyncPhotoPopulator(context);
        mInflater = LayoutInflater.from(context);
        this.flickr = flickr;
        
        date_format = new SimpleDateFormat("h:mm a EE\nMMM d, yyyy");
    }
    
    
	
	public List<Comment> comment_list = new ArrayList<Comment>();

	
	public Map<String, SoftReference<Bitmap>> getCachedBitmaps() {
		return photo_populator.bitmapReferenceMap;
	}
	


	public void restoreCachedBitmaps(Map<String, SoftReference<Bitmap>> cache) {
		photo_populator.bitmapReferenceMap = cache;
	}
	
	public void restoreCachedPhotoInfo(Map<String, Photo> cache) {
		photo_populator.photoInfoMap = cache;
	}
	
	
	


    public int getCount() { return comment_list.size(); }
    public Object getItem(int position) { return comment_list.get(position); }
    public long getItemId(int position) { return position; }

    // ==========================================


	@Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolderFlickrPhoto holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_flickr_comment, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolderFlickrPhoto();
            holder.title = (TextView) convertView.findViewById(R.id.flickr_photo_title);
            holder.description = (TextView) convertView.findViewById(R.id.flickr_photo_description);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.flickr_photo_thumbnail);
            holder.owner = (TextView) convertView.findViewById(R.id.flickr_comment_date);

            convertView.setTag(holder);

        } else {

            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolderFlickrPhoto) convertView.getTag();
        }

        
        Comment comment = (Comment) comment_list.get(position);

        holder.title.setText( comment.getAuthorName() + " says:" );
        holder.description.setText( comment.getText() );
        holder.owner.setText( date_format.format(comment.getDateCreate()) );
        

		photo_populator.fetchBuddyIconByNSID(comment.getAuthor(), holder.thumbnail);
		


        convertView.setBackgroundDrawable(new CornerDecorationDrawable(context, convertView, Color.BLACK));

        return convertView;
    }
}