package com.kostmo.tools.progress;

import java.util.ArrayList;
import java.util.List;

public class EtaWindow {

	final static int WINDOW_SIZE = 3;

	// ========================================================================
	static class CountedDuration {
		long duration_millis;
		int count;
	}
	
	// ========================================================================
	public static class UninitializedException extends Exception {
		private static final long serialVersionUID = 8905182040902858881L;
	}
	
	// ========================================================================
	boolean initialized = false;
	long last_increment_millis;
	int last_progress_count;
	List<CountedDuration> spans = new ArrayList<CountedDuration>();

	// ========================================================================
	/**
	 * This must be called before the first span is being "worked on".
	 * @param timestamp
	 */
	void initialize(long timestamp) {
		initialized = true;
		last_increment_millis = timestamp;
		last_progress_count = 0;
	}

	// ========================================================================
	public void addSpan(int count, long timestamp) throws UninitializedException {

		if (count == 0) {
			initialize(timestamp);
			return;
		}
		
		if (!initialized) throw new UninitializedException();
		
		CountedDuration span = new CountedDuration();
		span.count = count - last_progress_count;
		last_progress_count = count;
		span.duration_millis = timestamp - last_increment_millis;
		last_increment_millis = timestamp;
		spans.add(span);
		if (spans.size() > WINDOW_SIZE)
			spans.remove(0);
	}

	// ========================================================================
	public long getEtaMillis(int remaining_count) {
		long summed_millis = 0;
		int summed_counts = 0;
		
		for (CountedDuration span : spans) {
			summed_millis += span.duration_millis;
			summed_counts += span.count;
		}
		
		return summed_counts == 0 ? 0 : summed_millis * remaining_count / summed_counts; 
	}
}