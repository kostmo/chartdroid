package com.kostmo.market.revenue.task;

import android.app.Activity;
import android.app.ProgressDialog;

public interface ProgressHostActivity {

	public ProgressDialog getProgressDialog(int id);
	public Activity getActivity();
}
