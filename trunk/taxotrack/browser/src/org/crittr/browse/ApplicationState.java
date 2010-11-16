package org.crittr.browse;

import org.crittr.shared.browser.Constants;

import android.app.Application;

public class ApplicationState extends Application {
	
	@Override
	public void  onCreate() {
		super.onCreate();

	}
	
	
	public boolean hasPaid() {
		
		return Market.isPackageInstalled(this, Market.FULL_VERSION_PACKAGE);
	}
}
