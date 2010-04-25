package com.googlecode.chartdroid.market.sales.container;

import java.util.Date;

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
}