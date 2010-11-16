package com.kostmo.tools;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class DurationStrings {
	
	public static final int MAX_UNIT_DEPTH = 2;
	

	public static final int MILLIS_PER_SECOND = 1000;
	public static final int SECONDS_PER_MINUTE = 60;
	public static final int MINUTES_PER_HOUR = 60;
	public static final int HOURS_PER_DAY = 24;
	public static final int DAYS_PER_WEEK = 7;
	public static final int DAYS_PER_MONTH = 30;

	public static final long MILLIS_TO_SECONDS = MILLIS_PER_SECOND;
	public static final long MILLIS_TO_MINUTES = MILLIS_TO_SECONDS*SECONDS_PER_MINUTE;
	public static final long MILLIS_TO_HOURS = MILLIS_TO_MINUTES*MINUTES_PER_HOUR;
	public static final long MILLIS_TO_DAYS = MILLIS_TO_HOURS*HOURS_PER_DAY;
	public static final long MILLIS_TO_WEEKS = MILLIS_TO_DAYS*DAYS_PER_WEEK;
	public static final long MILLIS_TO_MONTHS = MILLIS_TO_DAYS*DAYS_PER_MONTH;
	

	// ========================================================================
	public enum TimescaleTier {
		MONTHS(MILLIS_TO_MONTHS, "month"),
		WEEKS(MILLIS_TO_WEEKS, "week"),
		DAYS(MILLIS_TO_DAYS, "day"),
		HOURS(MILLIS_TO_HOURS, "hour"),
		MINUTES(MILLIS_TO_MINUTES, "minute"),
		SECONDS(MILLIS_TO_SECONDS, "second");
		
		public final long millis;
		private final String label;
		TimescaleTier(long millis, String label) {
			this.millis = millis;
			this.label = label;
		}
		
	    public String getName() {
	    	return this.label;
	    }
		
	    public String getLabel(float count) {
	    	return this.label + pluralize(count);
	    }
	}

	// ========================================================================
    public static String pluralize(float count) {
    	return (count != 1 ? "s" : "");
    }
	
	// ========================================================================
	public static String printDuration(final long milliseconds) {
		
		List<String> timescale_values = new ArrayList<String>();
		long remaining_milliseconds = milliseconds;
		for (TimescaleTier timescale : TimescaleTier.values()) {
			int unit_count = (int) (remaining_milliseconds / timescale.millis);
			if (unit_count > 0) {
				String labeled_unit = unit_count + " " + timescale.getLabel(unit_count);
				timescale_values.add(labeled_unit);
				
				if (timescale_values.size() >= MAX_UNIT_DEPTH) break;
			}
			remaining_milliseconds = remaining_milliseconds % timescale.millis;
		}
		
		// XXX What if the array is empty?
		return TextUtils.join(", ", timescale_values);
	}
}
