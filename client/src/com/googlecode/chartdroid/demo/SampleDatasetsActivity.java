package com.googlecode.chartdroid.demo;

import com.googlecode.chartdroid.core.IntentConstants;

import org.achartengine.demo.data.DonutData;
import org.achartengine.demo.data.MultiTimelineData;
import org.achartengine.demo.data.TemperatureData;
import org.achartengine.demo.data.TimelineData;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

public class SampleDatasetsActivity extends Activity implements View.OnClickListener {

	static final String TAG = "ChartDroid"; 

    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.sample_datasets_activity);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

        for (int view : new int[] {
        		R.id.button_timeline_data_provider,
        		R.id.button_multi_timeline_data_provider,
        		R.id.button_multiseries_data_provider,
        		R.id.button_multiseries_data_provider_radial,
        		R.id.button_multiseries_data_provider_xy,
        		R.id.button_labeled_multiseries_data_provider,
        		R.id.button_labeled_multiseries_data_provider_radial,
        		R.id.button_labeled_multiseries_data_provider_xy,
        		}) {
        	findViewById(view).setOnClickListener(this);
        }
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
			Uri flickr_destination = Uri.parse( Demo.GOOGLE_CODE_URL );
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
		case R.id.button_timeline_data_provider:
		{
            Intent i = new Intent(Intent.ACTION_VIEW, TimelineData.uri);
            i.putExtra(Intent.EXTRA_TITLE, TimelineData.DEMO_CHART_TITLE);
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		case R.id.button_multi_timeline_data_provider:
		{
            Intent i = new Intent(Intent.ACTION_VIEW, MultiTimelineData.uri);
            i.putExtra(Intent.EXTRA_TITLE, MultiTimelineData.DEMO_CHART_TITLE);
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		case R.id.button_multiseries_data_provider:
		{
            Intent i = new Intent(Intent.ACTION_VIEW, TemperatureData.uri);
            i.putExtra(Intent.EXTRA_TITLE, TemperatureData.DEMO_CHART_TITLE);
			i.putExtra(IntentConstants.EXTRA_FORMAT_STRING_Y, "%.1f°C");
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		case R.id.button_multiseries_data_provider_radial:
		{
            Intent i = new Intent(Intent.ACTION_VIEW, TemperatureData.uri);
            i.putExtra(Intent.EXTRA_TITLE, TemperatureData.DEMO_CHART_TITLE);
			i.putExtra(IntentConstants.EXTRA_FORMAT_STRING_Y, "%.1f°C");
            i.addCategory(IntentConstants.CATEGORY_PIE_CHART);
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		case R.id.button_multiseries_data_provider_xy:
		{
            Intent i = new Intent(Intent.ACTION_VIEW, TemperatureData.uri);
            i.putExtra(Intent.EXTRA_TITLE, TemperatureData.DEMO_CHART_TITLE);
			i.putExtra(IntentConstants.EXTRA_FORMAT_STRING_Y, "%.1f°C");
            i.putExtra(IntentConstants.EXTRA_SERIES_COLORS, new int[] {Color.RED, Color.MAGENTA, Color.CYAN, Color.BLUE});
            i.addCategory(IntentConstants.CATEGORY_XY_CHART);
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		case R.id.button_labeled_multiseries_data_provider:
		{ 
			Intent i = new Intent(Intent.ACTION_VIEW, DonutData.uri);
            i.putExtra(Intent.EXTRA_TITLE, DonutData.DEMO_CHART_TITLE);
			i.putExtra(IntentConstants.EXTRA_FORMAT_STRING_X, "#%.0f");
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		case R.id.button_labeled_multiseries_data_provider_radial:
		{
			Intent i = new Intent(Intent.ACTION_VIEW, DonutData.uri);
            i.putExtra(Intent.EXTRA_TITLE, DonutData.DEMO_CHART_TITLE);
			i.putExtra(IntentConstants.EXTRA_FORMAT_STRING_X, "#%.0f");
            i.addCategory(IntentConstants.CATEGORY_PIE_CHART);
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		case R.id.button_labeled_multiseries_data_provider_xy:
		{
			Intent i = new Intent(Intent.ACTION_VIEW, DonutData.uri);
            i.putExtra(Intent.EXTRA_TITLE, DonutData.DEMO_CHART_TITLE);
			i.putExtra(IntentConstants.EXTRA_FORMAT_STRING_X, "#%.0f");
            i.addCategory(IntentConstants.CATEGORY_XY_CHART);
            Market.intentLaunchMarketFallback(SampleDatasetsActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
			break;
		}
		}
	}
    

}