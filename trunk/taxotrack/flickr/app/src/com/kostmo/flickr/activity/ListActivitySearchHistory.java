package com.kostmo.flickr.activity;



import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.data.DatabaseSearchHistory;


public class ListActivitySearchHistory extends ListActivity {

	public static final String INTENT_EXTRA_RECENT_SEARCH_ID = "INTENT_EXTRA_RECENT_SEARCH_ID";

	static final String TAG = Market.DEBUG_TAG;
	
	DatabaseSearchHistory helper;
	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.list_activity_bookmarks);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        
		helper = new DatabaseSearchHistory(this);
        Cursor c = helper.getSearchHistory();
        SearchHistoryAdapter adapter = new SearchHistoryAdapter(
        		this,
        		R.layout.list_item_search_summary,
                c);
        
		setListAdapter(adapter);

		registerForContextMenu(getListView());
    }
    
    
    // ======================================================
    
    public class SearchHistoryAdapter extends ResourceCursorAdapter {

		public SearchHistoryAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);

		}


		public void bindView(View view, Context context, Cursor cursor) {

			TextView user_textbox = (TextView) view.findViewById(R.id.recent_search_user);
			int user_id_column = cursor.getColumnIndex(DatabaseSearchHistory.KEY_USER_NAME);
			String user_id = cursor.getString(user_id_column);
			if (user_id == null || user_id.length() <= 0)
				user_textbox.setVisibility(View.GONE);
			else {
				user_textbox.setText( "User: " + user_id );
				user_textbox.setVisibility(View.VISIBLE);
			}
			
			TextView group_textbox = (TextView) view.findViewById(R.id.recent_search_group);
			int group_id_column = cursor.getColumnIndex(DatabaseSearchHistory.KEY_GROUP_NAME);
			String group_id = cursor.getString(group_id_column);
			if (group_id == null || group_id.length() <= 0)
				group_textbox.setVisibility(View.GONE);
			else {
				group_textbox.setText( "Group: " + group_id );
				group_textbox.setVisibility(View.VISIBLE);
			}
			

			TextView search_textbox = (TextView) view.findViewById(R.id.recent_search_text);
			int search_text_column = cursor.getColumnIndex(DatabaseSearchHistory.KEY_SEARCH_TEXT);
			String search_text = cursor.getString(search_text_column);
			if (search_text == null || search_text.length() <= 0) {

				search_textbox.setText( "-no search text-");
//				search_textbox.setVisibility(View.GONE);
			} else {
				search_textbox.setText( "Text: " + search_text );
//				search_textbox.setVisibility(View.VISIBLE);
			}

			
			
			int tag_count_column = cursor.getColumnIndex(DatabaseSearchHistory.KEY_TAG_COUNT);
			long tag_count = cursor.getLong(tag_count_column);
			((TextView) view.findViewById(R.id.recent_search_tag_count)).setText( Long.toString( tag_count ) + " tags" );
			

			int hit_count_column = cursor.getColumnIndex(DatabaseSearchHistory.KEY_HIT_COUNT);
			long hit_count = cursor.getLong(hit_count_column);
			((TextView) view.findViewById(R.id.recent_search_hits)).setText( "Hits: " + Long.toString( hit_count ) );
			
			
			int timestamp_column = cursor.getColumnIndex(DatabaseSearchHistory.KEY_TIMESTAMP);
			long timestamp = cursor.getLong(timestamp_column);
			
//			Log.d(TAG, "Timestamp value: " + timestamp);
			
			Date d = new Date(timestamp * 1000);
			String date_string = DateFormat.getDateTimeInstance().format(d);
//			Log.e(TAG, "Formatted date: " + date_string);
			((TextView) view.findViewById(R.id.recent_search_date)).setText( date_string );
		}
    }
    
    // ======================================================
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	Intent i = new Intent();
    	i.putExtra(INTENT_EXTRA_RECENT_SEARCH_ID, id);
    	setResult(Activity.RESULT_OK, i);
    	
    	finish();
    }
    
    // ================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_history, menu);

        return true;
    }
    
    
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	
        switch (item.getItemId()) {

        case R.id.menu_clear_history:
        {
        	DatabaseSearchHistory helper = new DatabaseSearchHistory(this);
        	helper.clear_history();
        	
        	Cursor c = helper.getSearchHistory();
        	((ResourceCursorAdapter) getListAdapter()).changeCursor(c);
        	
            return true;
        }



        }
        return super.onOptionsItemSelected(item);
    }
    
}
