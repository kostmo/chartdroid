package com.googlecode.chartdroid.calendar.activity;

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.calendar.CalendarDaysAdapter;
import com.googlecode.chartdroid.calendar.MiniMonthDrawable;
import com.googlecode.chartdroid.calendar.container.CalendarDay;
import com.googlecode.chartdroid.calendar.container.SimpleEvent;
import com.googlecode.chartdroid.core.ContentSchemaOld;
import com.googlecode.chartdroid.core.IntentConstants;
import com.googlecode.chartdroid.core.ContentSchemaOld.CalendarEvent;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class OldCalendarActivity extends Activity {

    final static public String TAG = "Calendar";

	static final int REQUEST_CODE_EVENT_SELECTION = 1;


    GridView mGrid, weekday_labels_grid;
    private LayoutInflater mInflater;
    
    ImageView mini_calendar_prev, mini_calendar_curr, mini_calendar_next;
    
    
    List<SimpleEvent> events = new ArrayList<SimpleEvent>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.calendar_month);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);


        
        
        Uri intent_data = getIntent().getData();
//    	Log.d(TAG, "Intent data: " + intent_data);
//    	Log.d(TAG, "Intent type: " + getIntent().getType());
//
//    	Log.d(TAG, "Intent action: " + getIntent().getAction());
//    	
    	
        // Zip the events
        if (intent_data != null) {
        	// We have been passed a cursor to the data via a content provider.
        	
        	Log.d(TAG, "Querying content provider for: " + intent_data);

        	String KEY_EVENT_TITLE = ContentSchemaOld.CalendarEvent.COLUMN_EVENT_TITLE;
 			Cursor cursor = managedQuery(intent_data,
 					new String[] {BaseColumns._ID, CalendarEvent.COLUMN_EVENT_TIMESTAMP, CalendarEvent.COLUMN_EVENT_TITLE},
 					null, null, null);

 			int id_column = cursor.getColumnIndex(BaseColumns._ID);
 			int timestamp_column = cursor.getColumnIndex(ContentSchemaOld.CalendarEvent.COLUMN_EVENT_TIMESTAMP);
 			
// 			Log.e(TAG, "In calendar - rowcount: " + cursor.getCount());
// 			Log.e(TAG, "In calendar - colcount: " + cursor.getColumnCount());
 			
 			if (cursor.moveToFirst()) {
	 			do {
	 				long timestamp = cursor.getLong(timestamp_column)*1000;
	 				Log.d(TAG, "Adding event with timestamp: " + timestamp);
	 				Log.d(TAG, "Timestamp date is: " + new Date(timestamp));
	
		        	events.add(
		        		new SimpleEvent(
		        			cursor.getLong(id_column),
		        			timestamp) );
		        	
	 			} while (cursor.moveToNext());
 			}
        } else {
        	// We have been passed the data directly.

//        	Log.d(TAG, "We have been passed the data directly.");
        	
	        long[] event_ids = getIntent().getLongArrayExtra(IntentConstants.EXTRA_EVENT_IDS);
	        long[] event_timestamps = getIntent().getLongArrayExtra(IntentConstants.EXTRA_EVENT_TIMESTAMPS);
	        for (int i=0; i<event_timestamps.length; i++)
	        	events.add( new SimpleEvent(event_ids[i], event_timestamps[i]) );
	        
//	        Log.d(TAG, "Added " + event_timestamps.length + " timestamps.");
        }
        Collections.sort(events);
        
        
        
        mInflater = LayoutInflater.from(this);
        

        
        mGrid = (GridView) findViewById(R.id.full_month);

        final CalendarDaysAdapter cda = new CalendarDaysAdapter(this, mInflater, events);
        mGrid.setAdapter(cda);
        
        mGrid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				
				CalendarDay day = (CalendarDay) cda.getItem(position);
				
//				Toast.makeText(Calendar.this, "Choice: " + day.d.getDate(), Toast.LENGTH_SHORT).show();
				
				Uri data = getIntent().getData();
				if (data != null) {
					Intent i = new Intent();
					i.setData(data);
					
//					Log.d(TAG, "Hours: " + day.d.getHours() + "; Minutes: " + day.d.getMinutes());
					
					i.putExtra(IntentConstants.INTENT_EXTRA_DATE, day.date.getTime());
					i.setClass(OldCalendarActivity.this, EventListActivity.class);
					startActivityForResult(i, REQUEST_CODE_EVENT_SELECTION);
				}
			}
        });


        mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
        	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {

				CalendarDay day = (CalendarDay) cda.getItem(position);

//				Log.d(TAG, "Hours: " + day.d.getHours() + "; Minutes: " + day.d.getMinutes());

				Intent i = new Intent();
				i.putExtra(IntentConstants.INTENT_EXTRA_DATE, day.date.getTime());
		        setResult(Activity.RESULT_OK, i);
				finish();
				
				return true;
			}
        });

        
        /*
        weekday_labels_grid = (GridView) findViewById(R.id.weekday_labels);
        WeekdayLabelsAdapter wda = new WeekdayLabelsAdapter(mInflater, this);
        weekday_labels_grid.setAdapter(wda);
		*/
        
        String month_string = new DateFormatSymbols().getMonths()[ cda.cal.getTime().getMonth() ];
        ((TextView) findViewById(R.id.chart_title_placeholder)).setText( month_string );
        
        
        mini_calendar_prev = (ImageView) findViewById(R.id.mini_calendar_prev);
        mini_calendar_curr = (ImageView) findViewById(R.id.mini_calendar_curr);
        mini_calendar_next = (ImageView) findViewById(R.id.mini_calendar_next);

        

		GregorianCalendar cal_prev = new GregorianCalendar();
		cal_prev.add(GregorianCalendar.MONTH, -1);
		
		GregorianCalendar cal_curr = new GregorianCalendar();
		
		GregorianCalendar cal_next = new GregorianCalendar();
		cal_next.add(GregorianCalendar.MONTH, 1);
		
//		Log.d(TAG, "Previous month...");
        mini_calendar_prev.setImageDrawable(new MiniMonthDrawable(this, mini_calendar_prev, cal_prev));
        
//        Log.d(TAG, "Current month...");
        mini_calendar_curr.setImageDrawable(new MiniMonthDrawable(this, mini_calendar_curr, cal_curr));
        
//        Log.d(TAG, "Next month...");
        mini_calendar_next.setImageDrawable(new MiniMonthDrawable(this, mini_calendar_next, cal_next));
    }


    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
//            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
//            t.show();
            return;
        }
        
  	   	switch (requestCode) {
   		case REQUEST_CODE_EVENT_SELECTION:
   		{
   			
   			long id = data.getLongExtra(IntentConstants.INTENT_EXTRA_CALENDAR_EVENT_ID, -1);

			Intent i = new Intent();
			i.putExtra(IntentConstants.INTENT_EXTRA_CALENDAR_EVENT_ID, id);
	        setResult(Activity.RESULT_OK, i);
			finish();

//   			Toast.makeText(this, "Result: " + id, Toast.LENGTH_SHORT).show();
            break;
        }
  	   	}
    }
    
}
