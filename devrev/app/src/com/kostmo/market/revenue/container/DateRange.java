package com.kostmo.market.revenue.container;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;

import com.kostmo.market.revenue.R;

public class DateRange implements Comparable<DateRange> {
	// The natural sort order shall be by "start date".
	
	public Date start, end;
	
	public DateRange() {};
	
	public DateRange(Date start, Date end) {
		this.start = start;
		this.end = end;
	};		
	
	@Override
	public int compareTo(DateRange another) {
		return this.start.compareTo(another.start);
	}
	
	@Override
	public DateRange clone() {
		return new DateRange(
				(Date) this.start.clone(),
				(Date) this.end.clone());
	}

	@Override
	public String toString() {
		return "from " + this.start + " to " + this.end;
	}
	
	public long getMillisDelta() {
		return this.end.getTime() - this.start.getTime();
	}
	
	public String format(SimpleDateFormat format) {
		return format.format(this.start) + " to " + format.format(this.end);
	}
	
	public String formatLayout(SimpleDateFormat format, Context context) {
		return context.getResources().getString(R.string.plot_date_range, format.format(this.start), format.format(this.end));
	}
	
	public String[] getRangeAsStringArray() {
    	return new String[] {
    			Long.toString(this.start.getTime()),
    			Long.toString(this.end.getTime())
	    	};
	}
	
}