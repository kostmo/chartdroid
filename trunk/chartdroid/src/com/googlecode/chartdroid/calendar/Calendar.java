package com.googlecode.chartdroid.calendar;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.core.ContentSchema;
import com.googlecode.chartdroid.core.IntentConstants;
import com.googlecode.chartdroid.core.ContentSchema.CalendarEvent;

public class Calendar extends Activity {

	public static String INTENT_EXTRA_CALENDAR_SELECTION_ID = "INTENT_EXTRA_CALENDAR_SELECTION_ID";
	public static String INTENT_EXTRA_DATE = "INTENT_EXTRA_DATE";

	static final int RETURN_CODE_EVENT_SELECTION = 1;
	
	public static class SimpleEvent implements Comparable<SimpleEvent> {

		long id;
		Date timestamp;

		SimpleEvent(long id, long timestamp) {
			this.id = id;
			this.timestamp = new Date(timestamp);
			
//			Log.i(TAG, "Added Date: " + this.timestamp);
		}
		
		SimpleEvent(long id, Date timestamp) {
			this.id = id;
			this.timestamp = timestamp;
		}
		
		public int compareTo(SimpleEvent another) {
			return timestamp.compareTo(another.timestamp);
		}
	}
	
	
	
	public static class CalendarDay {
		Date d;
		String content;
		List<SimpleEvent> day_events;
	}
	
	
	final static public String TAG = "Calendar";
	

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

        	String KEY_EVENT_TITLE = ContentSchema.CalendarEvent.COLUMN_EVENT_TITLE;
 			Cursor cursor = managedQuery(intent_data,
 					new String[] {BaseColumns._ID, CalendarEvent.COLUMN_EVENT_TIMESTAMP, CalendarEvent.COLUMN_EVENT_TITLE},
 					null, null, null);

 			int id_column = cursor.getColumnIndex(BaseColumns._ID);
 			int timestamp_column = cursor.getColumnIndex(ContentSchema.CalendarEvent.COLUMN_EVENT_TIMESTAMP);
 			
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
					
					i.putExtra(INTENT_EXTRA_DATE, day.d.getTime());
					i.setClass(Calendar.this, EventListActivity.class);
					startActivityForResult(i, RETURN_CODE_EVENT_SELECTION);
				}
			}
        });


        mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
        	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {

				CalendarDay day = (CalendarDay) cda.getItem(position);

//				Log.d(TAG, "Hours: " + day.d.getHours() + "; Minutes: " + day.d.getMinutes());

				Intent i = new Intent();
				i.putExtra(INTENT_EXTRA_DATE, day.d.getTime());
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


    static class ViewHolderCalendarDay {
    	
    	public TextView title, datum;
    	public ImageView thumb;
    }
    
    public static int generate_days(GregorianCalendar cal, List<CalendarDay> day_list, List<SimpleEvent> events) {

    	final int active_month = cal.get(GregorianCalendar.MONTH);

    	cal.set(GregorianCalendar.DAY_OF_MONTH, 1);

    	
    	
    	
		GregorianCalendar cal_min_upper_limit = (GregorianCalendar) cal.clone();
		cal_min_upper_limit.add(GregorianCalendar.MONTH, 1);
//		Log.i(TAG, "Minimum upper limit: " + cal_min_upper_limit.getTime());

//		GregorianCalendar cal_max_upper_limit = (GregorianCalendar) cal_min_upper_limit.clone();
//		cal_max_upper_limit.add(GregorianCalendar.DATE, 7);
//    	Log.i(TAG, "Maximum upper limit: " + cal_max_upper_limit.getTime());
    	
    	



    	int first_day_of_week = cal.getFirstDayOfWeek();
//    	Log.e(TAG, "first day of week: " + first_day_of_week);
    	
    	int daydiff = cal.get(GregorianCalendar.DAY_OF_WEEK) - first_day_of_week;
		cal.add(GregorianCalendar.DATE, -daydiff);
    	
//    	Log.i(TAG, "Month of starting day in calendar: " + cal.get(GregorianCalendar.MONTH));
		
//		int maximum_possible_day_of_month = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
//		Log.d(TAG, "Days in this month: " + maximum_possible_day_of_month);
    	
		

//		Log.d(TAG, "Size of day_list: " + day_list.size());
//		Log.d(TAG, "Size of events: " + events.size());
//		Log.d(TAG, "active_month: " + active_month);
		
		int event_index = 0;
    	while ( cal.before(cal_min_upper_limit)
    			|| cal.get(GregorianCalendar.DAY_OF_WEEK) > first_day_of_week) {

//    		Log.i(TAG, "Cal date: " + cal.getTime());
//    		Log.d(TAG, "Before min end date? " + cal.before(cal_min_upper_limit));
//    		Log.w(TAG, "After first day of week? " + (cal.get(GregorianCalendar.DAY_OF_WEEK) > first_day_of_week));
//    		Log.e(TAG, "Before uppper limit? " + cal.before(cal_max_upper_limit));
    		
    		
    		CalendarDay cd = new CalendarDay();
    		cd.d = cal.getTime();
    		cd.day_events = new ArrayList<SimpleEvent>();
    		
    		
    		// Catch up the event list with the current date
    		while ( event_index < events.size() && events.get(event_index).timestamp.compareTo( cal.getTime() ) <= 0 ) {
    			event_index++;
    		}

    		
    		// Advance calendar to the next day
    		cal.add(GregorianCalendar.DATE, 1);

    		
    		// Add all the events that occur before the next day
    		if (event_index < events.size()) {
	    		SimpleEvent scan_event = events.get(event_index);
				while ( scan_event.timestamp.compareTo( cal.getTime() ) <= 0 ) {
					
					cd.day_events.add( scan_event );

	    			event_index++;
	    			if (event_index < events.size())
	    				scan_event = events.get(event_index);
	    			else
	    				break;
				}
    		}
    		
    		day_list.add(cd);
    	}
    	
    	// Reset the moth so we can access it later...
    	cal.set(GregorianCalendar.MONTH, active_month);
    	
    	return active_month;
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
   		case RETURN_CODE_EVENT_SELECTION:
   		{
   			
   			long id = data.getLongExtra(Calendar.INTENT_EXTRA_CALENDAR_SELECTION_ID, -1);

			Intent i = new Intent();
			i.putExtra(INTENT_EXTRA_CALENDAR_SELECTION_ID, id);
	        setResult(Activity.RESULT_OK, i);
			finish();

//   			Toast.makeText(this, "Result: " + id, Toast.LENGTH_SHORT).show();
            break;
        }
  	   	}
    }
    
}
