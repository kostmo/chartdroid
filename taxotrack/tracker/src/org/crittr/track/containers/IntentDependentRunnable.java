package org.crittr.track.containers;

import android.content.Intent;

public abstract class IntentDependentRunnable implements Runnable {

	private Intent intent;
	public boolean enable_dialog = false;
	protected IntentDependentRunnable(Intent i) {
		intent = i;
	}
	public Intent getIntent() {
		return intent;
	}
}