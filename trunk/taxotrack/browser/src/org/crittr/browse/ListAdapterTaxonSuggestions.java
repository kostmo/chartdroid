package org.crittr.browse;

import org.crittr.browse.activity.ListActivityTextualSearch;
import org.crittr.browse.activity.ListActivityTextualSearch.NameType;
import org.crittr.shared.browser.provider.TaxonSearch;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Filterable;
import android.widget.SimpleCursorAdapter;


// XXX compiler bug in javac 1.5.0_07-164, we need to implement Filterable
// to make compilation work
public class ListAdapterTaxonSuggestions extends SimpleCursorAdapter implements Filterable {
	
	

	final static String TAG = Market.DEBUG_TAG;
	
	Context context;
	public ListAdapterTaxonSuggestions(Context context, int layout, Cursor c, String[] from, int[] to) {
    	super(context, layout, c, from, to);

        this.context = context;
    }

	
    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }
        
        Cursor cur = null;
        if (constraint != null) {
        	
        	if (((ListActivityTextualSearch) context).active_autocomplete_type.equals(NameType.VERNACULAR)) {
	        	
				Uri mySuggestion = TaxonSearch.TaxonSuggest.VERNACULAR_AUTOCOMPLETE_URI;
				cur = ((Activity) context).managedQuery(mySuggestion, new String[] {TaxonSearch.TaxonSuggest.COLUMN_SUGGESTION}, constraint.toString(), null, null);

//				Log.d(TAG, "I got the cursor, itemcount: " + cur.getCount());
        	}
        }
		
		return cur;
    }
}