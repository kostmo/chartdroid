package com.kostmo.commute.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;

public class ListActivityWirelessNetworks extends ListActivity {

    public static final String TAG = Market.TAG;


	public static final String EXTRA_WIFI_SSID = "EXTRA_WIFI_SSID";
	public static final String EXTRA_WIFI_BSSID = "EXTRA_WIFI_BSSID";
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	List<ScanResult> scan_results = wm.getScanResults();
    	List<String> networks = new ArrayList<String>();
    	for (ScanResult result : scan_results) {
    		networks.add( result.SSID );
    	}

    	this.setListAdapter( new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, networks) );
    }

    // ========================================================================     
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

    	String ssid = (String) l.getAdapter().getItem(position);
		Intent result = new Intent();
		result.putExtra(EXTRA_WIFI_SSID, ssid);
		setResult(Activity.RESULT_OK, result);
		finish();
    }

    // ======================================================================== 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_pair_associator, menu);
        return true;
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        return true;
    }

    // ========================================================================	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        }

        return super.onOptionsItemSelected(item);
    }
}