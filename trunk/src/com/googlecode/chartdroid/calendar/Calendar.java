package com.googlecode.chartdroid.calendar;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.intent;


public class Calendar extends Activity {

	public static class SimpleEvent implements Comparable<SimpleEvent> {

		long id;
		Date timestamp;

		SimpleEvent(long id, long timestamp) {
			this.id = id;
			this.timestamp = new Date(timestamp);
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

        
        
        // Zip the events
        long[] event_ids = getIntent().getLongArrayExtra(intent.EXTRA_EVENT_IDS);
        long[] event_timestamps = getIntent().getLongArrayExtra(intent.EXTRA_EVENT_TIMESTAMPS);
        for (int i=0; i<event_timestamps.length; i++)
        	events.add( new SimpleEvent(event_ids[i], event_timestamps[i]) );
        Collections.sort(events);
        
        
        
        mInflater = LayoutInflater.from(this);
        

        setContentView(R.layout.calendar_month);
        
        mGrid = (GridView) findViewById(R.id.full_month);
        CalendarDaysAdapter cda = new CalendarDaysAdapter(this, mInflater, events);
        mGrid.setAdapter(cda);
        
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
		
		GregorianCalendar cal_next = new GregorianCalendar();
		cal_next.add(GregorianCalendar.MONTH, 1);
		
        mini_calendar_prev.setImageDrawable(new MiniMonthDrawable(this, mini_calendar_prev, cal_prev));
        mini_calendar_curr.setImageDrawable(new MiniMonthDrawable(this, mini_calendar_curr, cda.cal));
        mini_calendar_next.setImageDrawable(new MiniMonthDrawable(this, mini_calendar_next, cal_next));
    }



    
    static class ViewHolderCalendarDay {
    	
    	public TextView title, datum;
    	public ImageView thumb;
    }
    
    public static int generate_days(GregorianCalendar cal, List<CalendarDay> day_list, List<SimpleEvent> events, int active_month) {

    	
    	
    	int first_day_of_week = cal.getFirstDayOfWeek();
    	
//    	cal.get(GregorianCalendar.MONTH);


    	
    	cal.set(GregorianCalendar.DAY_OF_MONTH, 1);
    	
    	int daydiff = cal.get(GregorianCalendar.DAY_OF_WEEK) - first_day_of_week;
		cal.add(GregorianCalendar.DATE, -daydiff);
    	

//    	int days_in_month = cal.get(cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
//		Log.d(TAG, "Days in this month: " + days_in_month);
    	
		
		int event_index = 0;
    	while (cal.get(GregorianCalendar.MONTH) <= active_month || cal.get(GregorianCalendar.DAY_OF_WEEK) > first_day_of_week) {
    		
    		CalendarDay cd = new CalendarDay();
    		cd.d = cal.getTime();
    		cd.day_events = new ArrayList<SimpleEvent>();
    		
    		
    		// Catch up the event list with the current date
    		while ( event_index < events.size() && events.get(event_index).timestamp.compareTo( cal.getTime() ) <= 0 )
    			event_index++;

    		
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

}
