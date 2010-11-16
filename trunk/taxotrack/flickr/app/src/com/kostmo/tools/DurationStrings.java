package com.kostmo.tools;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class DurationStrings {
	
	public static final int MAX_UNIT_DEPTH = 2;
	

	public static final int MILLIS_PER_SECOND = 1000;
	public static final int SECONDS_PER_MINUTE = 60;
	public static final int MINUTES_PER_HOUR = 60;
	public static final int HOURS_PER_DAY = 24;

	public static final int MILLIS_TO_SECONDS = MILLIS_PER_SECOND;
	public static final int MILLIS_TO_MINUTES = MILLIS_TO_SECONDS*SECONDS_PER_MINUTE;
	public static final int MILLIS_TO_HOURS = MILLIS_TO_MINUTES*MINUTES_PER_HOUR;
	public static final int MILLIS_TO_DAYS = MILLIS_TO_HOURS*HOURS_PER_DAY;

	// ========================================================================
	public enum TimescaleTier {
		DAYS(MILLIS_TO_DAYS, "day"),
		HOURS(MILLIS_TO_HOURS, "hour"),
		MINUTES(MILLIS_TO_MINUTES, "minute"),
		SECONDS(MILLIS_TO_SECONDS, "second");
		
		int millis;
		String label;
		TimescaleTier(int millis, String label) {
			this.millis = millis;
			this.label = label;
		}
	}

	// ========================================================================
    public static String pluralize(int count) {
    	return (count != 1 ? "s" : "");
    }
	
	// ========================================================================
	public static String printDuration(final long milliseconds) {
		
		List<String> timescale_values = new ArrayList<String>();
		long remaining_milliseconds = milliseconds;
		for (TimescaleTier timescale : TimescaleTier.values()) {
			int unit_count = (int) (remaining_milliseconds / timescale.millis);
			if (unit_count > 0) {
				String labeled_unit = unit_count + " " + timescale.label + pluralize(unit_count);
				timescale_values.add(labeled_unit);
				
				if (timescale_values.size() >= MAX_UNIT_DEPTH) break;
			}
			remaining_milliseconds = remaining_milliseconds % timescale.millis;
		}
		
		// XXX What if the array is empty?
		return TextUtils.join(", ", timescale_values);
	}
}
