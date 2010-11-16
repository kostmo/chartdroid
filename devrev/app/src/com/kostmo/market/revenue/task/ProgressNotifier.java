package com.kostmo.market.revenue.task;

public interface ProgressNotifier {
	/*
	 * If the "layer" argument is greater than zero, then the "progress"
	 * and "max" values will be treated as a "supertask"; the
	 * progress bar value will not change, but some other indicator
	 * (e.g. a fractional number like "Step 3/4") will be modified.
	 */
	public void notifyProgress(int progress, int max, int layer);
}
