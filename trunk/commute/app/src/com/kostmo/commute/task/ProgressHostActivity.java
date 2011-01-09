package com.kostmo.commute.task;

import android.app.Activity;
import android.app.ProgressDialog;

public interface ProgressHostActivity {

	public ProgressDialog getProgressDialog(int id);
	public Activity getActivity();
}
