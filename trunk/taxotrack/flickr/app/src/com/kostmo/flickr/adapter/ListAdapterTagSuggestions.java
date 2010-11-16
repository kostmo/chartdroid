package com.kostmo.flickr.adapter;

import android.content.Context;
import android.database.Cursor;
import android.widget.Filterable;
import android.widget.SimpleCursorAdapter;

import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.containers.MachineTag.TagPart;
import com.kostmo.flickr.data.DatabaseSearchHistory;


// XXX compiler bug in javac 1.5.0_07-164, we need to implement Filterable
// to make compilation work
public class ListAdapterTagSuggestions extends SimpleCursorAdapter implements Filterable {

	final static String TAG = Market.DEBUG_TAG;
	
	Context context;
	DatabaseSearchHistory database;
	TagPart chosen_tag_part;
	boolean for_machine_tags;
	boolean uploads;
	public ListAdapterTagSuggestions(
			Context context,
			int layout,
			String[] from,
			int[] to,
			DatabaseSearchHistory database,
			TagPart chosen_tag_part,
			boolean for_machine_tags,
			boolean uploads) {
    	super(context, layout, null, from, to);

        this.context = context;
        this.database = database;
        this.chosen_tag_part = chosen_tag_part;
        this.for_machine_tags = for_machine_tags;
        this.uploads = uploads;
    }


	// Can be KEY_NAMESPACE, KEY_PREDICATE, or KEY_VALUE
	String getColumnName(TagPart tag_part) {
		switch (tag_part) {
		case NAMESPACE:
			return DatabaseSearchHistory.KEY_NAMESPACE;
		case PREDICATE:
			return DatabaseSearchHistory.KEY_PREDICATE;
		case VALUE:
			return DatabaseSearchHistory.KEY_VALUE;
		}
		return null;
	}
	
    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }
        
        Cursor cur = null;
        if (constraint != null) {
        	
        	String like_pattern = (String) constraint + '%';
        	
        	if (for_machine_tags)
	        	cur = database.retrieveMachineTags(
        			like_pattern,
        			getColumnName(this.chosen_tag_part),
        			this.uploads);
        	else
	        	cur = database.retrieveStandardTags(like_pattern, this.uploads);
        }
		
		return cur;
    }
}