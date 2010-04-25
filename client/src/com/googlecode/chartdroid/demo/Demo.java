package com.googlecode.chartdroid.demo;

import com.googlecode.chartdroid.core.IntentConstants;
import com.googlecode.chartdroid.demo.provider.EventContentProvider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class Demo extends Activity implements View.OnClickListener {
    
    static final String GOOGLE_CODE_URL = "http://chartdroid.googlecode.com/";


	static final String TAG = "ChartDroid";
	
	
	// XXX Experimental
	final int REQUEST_CODE_CALENDAR_SELECTION = 1;
	
	// ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.main);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

        
        for (int view : new int[] {
        		R.id.button_manual_entry,
        		R.id.button_sample_datasets,
        		R.id.button_old_style,
        		R.id.button_experimental_calendar,
        		}) {
        	findViewById(view).setOnClickListener(this);
        }

        
        ((TextView) findViewById(R.id.developer_note)).setMovementMethod(LinkMovementMethod.getInstance());
    }
    
	// ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_main, menu);
        return true;
    }
    
	// ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_about:
        {
			Uri flickr_destination = Uri.parse( GOOGLE_CODE_URL );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

            return true;
        }
        case R.id.menu_more_apps:
        {
	    	Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
	    	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
	    	startActivity(i);
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }
    
	// ========================================================================
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_manual_entry:
		{
            startActivity(new Intent(Demo.this, InputDatasetActivity.class));
			break;
		}
		case R.id.button_sample_datasets:
		{
            startActivity(new Intent(Demo.this, SampleDatasetsActivity.class));
			break;
		}
		case R.id.button_old_style:
		{
            startActivity(new Intent(Demo.this, OldChartsActivity.class));
			break;
		}
		
		case R.id.button_experimental_calendar:
		{

			Intent i = new Intent(Intent.ACTION_VIEW, EventContentProvider.constructUri(123));
			Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, REQUEST_CODE_CALENDAR_SELECTION);
			break;
		}
		}
	}
	
	// ========================================================================
	// XXX Experimental	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
//            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
//            t.show();
            return;
        }
        
  	   	switch (requestCode) {
   		case REQUEST_CODE_CALENDAR_SELECTION:
   		{
   			long id = data.getLongExtra(IntentConstants.INTENT_EXTRA_CALENDAR_SELECTION_ID, -1);
   			Toast.makeText(this, "Result: " + id, Toast.LENGTH_SHORT).show();
            break;
        }
  	   	}
    }
}