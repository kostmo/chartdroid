package com.kostmo.flickr.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aetrion.flickr.tags.Tag;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.containers.TagsViewHolder;
import com.kostmo.flickr.data.BetterMachineTagDatabase;
import com.kostmo.flickr.graphics.RoundedRectBackgroundDrawable;

public class FlickrTagListAdapter extends BaseAdapter {
	
	// FIXME Parallel lists is a bad way to do this.
	public ArrayList<Tag> filtered_tags_collection = new ArrayList<Tag>();
	public Collection<Tag> raw_tags_collection = new ArrayList<Tag>();

    final private LayoutInflater mInflater;
    Context context;
    String user_nsid;

	BetterMachineTagDatabase machinetag_database;
	
    public FlickrTagListAdapter(Context context, String user_nsid) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.user_nsid = user_nsid;
        this.machinetag_database = new BetterMachineTagDatabase(context);
    }
    
    public FlickrTagListAdapter(Context context) {
    	this(context, null);
    }
    
    // Sorts by "machine tag" status
    class TagComparator implements Comparator<Tag> {
		public int compare(Tag object1, Tag object2) {
			return new Boolean(object1.isMachineTag()).compareTo(object2.isMachineTag());
		}
    }
    
    public Collection<Tag> getFilteredTags() {
    	return Collections.unmodifiableCollection(this.filtered_tags_collection);
    }
    
    public void addTag(Tag tag) {
    	this.filtered_tags_collection.add( tag );
    	this.raw_tags_collection.add( tag );
    }
    
   
    public void sort_taglist() {
		Collections.sort(this.filtered_tags_collection, new TagComparator());
//		Collections.sort(raw_tags_collection, new TagComparator());
		this.notifyDataSetInvalidated();
    }
    
    

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public boolean isEnabled(int position) {
		if (this.user_nsid == null)
			return true;
		return ((Tag) getItem(position)).getAuthor().equals(this.user_nsid);
	}


    @Override
    public int getCount() {
        return this.filtered_tags_collection.size();
    }

    @Override
    public Object getItem(int position) {
        return this.filtered_tags_collection.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TagsViewHolder holder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.list_item_flickr_tag, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new TagsViewHolder();
            
            holder.machine_container = (LinearLayout) convertView.findViewById(R.id.machine_tag_triple_view);
            holder.machine_namespace = (TextView) convertView.findViewById(R.id.machine_tag_namespace);
            holder.machine_predicate = (TextView) convertView.findViewById(R.id.machine_tag_predicate);
            holder.machine_value = (TextView) convertView.findViewById(R.id.machine_tag_value);
            
            holder.raw_tag = (TextView) convertView.findViewById(R.id.flickr_tag_raw);
            holder.tag_author = (TextView) convertView.findViewById(R.id.flickr_tag_author);
            holder.icon = (ImageView) convertView.findViewById(R.id.tag_type_icon);

            convertView.setTag(holder);

        } else {

            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (TagsViewHolder) convertView.getTag();
        }


        Tag t = this.filtered_tags_collection.get(position);
        

//    	holder.description.setText(  t.getAuthor() + " (" + t.getAuthorName() + ")");
        holder.tag_author.setText( t.getAuthorName() );
        
        
        int tint_color = Color.TRANSPARENT;
        convertView.setPadding(0, 2, 0, 2);
        if (t.isMachineTag()) {
        	
        	holder.icon.setImageResource(R.drawable.gears);
        	
        	holder.machine_container.setVisibility(View.VISIBLE);
        	holder.raw_tag.setVisibility(View.GONE);
        	
//        	convertView.setBackgroundColor(R.color.dark_red_bg);

//        	convertView.setBackgroundResource(R.color.dark_red_bg);
        	String color_string = get_appropriate_color(t);
//        	convertView.setBackgroundColor( Color.parseColor("#55ff0000") );

        	if (color_string != null)
        		tint_color = Color.parseColor( color_string );
        	
        	
        	
        	// Use try/catch for Class Cast
        	MachineTag parts;
        	try {
        		parts = (MachineTag) t;
            	
        	} catch (ClassCastException e) {
//        		Log.w(TAG, "Coerced MachineTag type");
        		parts = new MachineTag( t.getRaw() );
        	}
        	
            holder.machine_namespace.setText( parts.namespace );
            holder.machine_predicate.setText( parts.predicate );
            holder.machine_value.setText( parts.value );
        }
        else {
        	holder.icon.setImageResource(R.drawable.tag);
        	holder.machine_container.setVisibility(View.GONE);
        	holder.raw_tag.setVisibility(View.VISIBLE);
        	
            holder.raw_tag.setText( t.getRaw() );
//        	convertView.setBackgroundColor(R.color.black);
        	convertView.setBackgroundDrawable(null);
        }
        
        Drawable d = new RoundedRectBackgroundDrawable(context, convertView, tint_color); 
		convertView.setBackgroundDrawable(d);
        
        return convertView;
    }

    private String get_appropriate_color(Tag t) {
    	
    	MachineTag parts = new MachineTag( t.getRaw() );
    	String color = machinetag_database.getAppropriateTagColor(parts);

        return color;
    }
}