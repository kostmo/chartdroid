package com.kostmo.market.revenue.adapter;

import android.content.Context;
import android.database.Cursor;
import android.widget.Filterable;
import android.widget.SimpleCursorAdapter;

import com.kostmo.market.revenue.provider.DatabaseRevenue;


// XXX compiler bug in javac 1.5.0_07-164, we need to implement Filterable
// to make compilation work
public class ListAdapterPublisherSuggestions extends SimpleCursorAdapter implements Filterable {

	final static String TAG = "ListAdapterPublisherSuggestions";
	
	Context context;
	DatabaseRevenue database;
	public ListAdapterPublisherSuggestions(Context context, int layout, Cursor c, String[] from, int[] to) {
    	super(context, layout, c, from, to);

        this.context = context;
        this.database = new DatabaseRevenue(context);
    }

    /*
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final TextView view = (TextView) inflater.inflate(
                android.R.layout.simple_dropdown_item_1line, parent, false);
        
        view.setText( convertToString(cursor)  );
        return view;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	
    	((TextView) view).setText( convertToString(cursor) );
    	
    }

    @Override
    public String convertToString(Cursor cursor) {
    	
    	
//    	return Integer.toString( cursor.getInt( 2 ) );
    	
        return cursor.getString( 1 );
    }
    */

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }
        
        Cursor cur = null;
        if (constraint != null) {
        	cur =  this.database.getPrefixedPublisherNames((String) constraint);
        }
		
		return cur;
    }
}