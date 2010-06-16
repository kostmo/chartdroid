package com.googlecode.chartdroid.example.minimal;

import com.googlecode.chartdroid.core.IntentConstants;
import com.googlecode.chartdroid.example.minimal.provider.DataContentProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class Demo extends Activity implements View.OnClickListener {
    
	static final String TAG = Market.TAG;

	final int DIALOG_CHARTDROID_DOWNLOAD = 1;
	
	// ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
    	findViewById(R.id.button_sample_datasets).setOnClickListener(this);
    }

	// ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

		Log.d(TAG, "Called onCreateDialog()");
		
		switch (id) {
		case DIALOG_CHARTDROID_DOWNLOAD:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Download ChartDroid")
			.setMessage("You need to download ChartDroid to display this data.")
			.setPositiveButton("Market download", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(Market.getMarketDownloadIntent(Market.CHARTDROID_PACKAGE_NAME));
				}
			})
			.setNeutralButton("Web download", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(new Intent(Intent.ACTION_VIEW, Market.APK_DOWNLOAD_URI_CHARTDROID));
				}
			})
			.create();
		}
		return null;
	}
	
	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		Log.d(TAG, "Called onPrepareDialog()");
	
		switch (id) {
		case DIALOG_CHARTDROID_DOWNLOAD:
		{
			boolean has_android_market = Market.isIntentAvailable(this,
					Market.getMarketDownloadIntent(Market.CHARTDROID_PACKAGE_NAME));

			Log.d(TAG, "has android market? " + has_android_market);
			
			dialog.findViewById(android.R.id.button1).setVisibility(
					has_android_market ? View.VISIBLE : View.GONE);
			break;
		}
		default:
			break;
		}
	}
    
	// ========================================================================
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_sample_datasets:
		{
            Intent i = new Intent(Intent.ACTION_VIEW, DataContentProvider.PROVIDER_URI);
            i.putExtra(Intent.EXTRA_TITLE, TemperatureData.DEMO_CHART_TITLE);
			i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, "%.1f°C");

			if (Market.isIntentAvailable(this, i)) {
				startActivity(i);
			} else {
				showDialog(DIALOG_CHARTDROID_DOWNLOAD);
			}

			break;
		}
		}
	}
}