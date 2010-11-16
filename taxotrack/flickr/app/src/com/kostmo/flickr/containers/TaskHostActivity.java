package com.kostmo.flickr.containers;

import android.app.Activity;

public interface TaskHostActivity {
	void showToast(String message);
	void showErrorDialog(String message);
	
	void launchAuthenticationActivity();
	Activity getContext();
}
