package com.kostmo.flickr.containers;

import android.app.ProgressDialog;

public interface ProgressHostActivity {
	void showProgressDialog();
	void dismissProgressDialog();
	
	ProgressDialog getProgressDialog();
}
