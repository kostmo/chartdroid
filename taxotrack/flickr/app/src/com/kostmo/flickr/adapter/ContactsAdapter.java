package com.kostmo.flickr.adapter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

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
import com.aetrion.flickr.REST;
import com.aetrion.flickr.contacts.Contact;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.ViewHolderFlickrPhoto;
import com.kostmo.flickr.graphics.CornerDecorationDrawable;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tasks.AsyncPhotoPopulator;

public class ContactsAdapter extends BaseAdapter {

	static final String TAG = Market.DEBUG_TAG; 
	
	public long total_result_count;
	
	public AsyncPhotoPopulator photo_populator;
    private LayoutInflater mInflater;

    
    Flickr flickr;
	
	Context context;
    public ContactsAdapter(Context context) {
    	this.context = context;
    	photo_populator = new AsyncPhotoPopulator(context);
        mInflater = LayoutInflater.from(context);

        try {
    		flickr = new Flickr(
    				ApiKeys.FLICKR_API_KEY,	// My API key
    				ApiKeys.FLICKR_API_SECRET,	// My API secret
    	        new REST()
    	    );
    	} catch (ParserConfigurationException e) {
    		e.printStackTrace();
    	}
    }
    
    
	
	public List<Contact> contact_list = new ArrayList<Contact>();

	
	public Map<String, SoftReference<Bitmap>> getCachedBitmaps() {
		return photo_populator.bitmapReferenceMap;
	}

	public void restoreCachedBitmaps(Map<String, SoftReference<Bitmap>> cache) {
		photo_populator.bitmapReferenceMap = cache;
	}
	


    public int getCount() { return contact_list.size(); }
    public Object getItem(int position) { return contact_list.get(position); }
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

            convertView.setTag(holder);

        } else {

            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolderFlickrPhoto) convertView.getTag();
        }

        
        Contact contact = (Contact) contact_list.get(position);
//        Photo photo = comment.;

        holder.title.setText( contact.getRealName() );
        holder.description.setText( contact.getUsername() );

        photo_populator.fetchDrawableByUrlOnThread(contact.getBuddyIconUrl(), holder.thumbnail);

        convertView.setBackgroundDrawable(new CornerDecorationDrawable(context, convertView, Color.BLACK));

        return convertView;
    }
}