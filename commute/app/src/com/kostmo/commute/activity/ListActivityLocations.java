package com.kostmo.commute.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.provider.DatabaseCommutes;

public class ListActivityLocations extends ListActivity {

    public static final String TAG = Market.TAG;


	public static final String EXTRA_LOCATION_ID = "EXTRA_LOCATION_ID";
	public static final long INVALID_LOCATION_ID = -1;
	

	private static final int REQUEST_CODE_MAP_LOCATION_SELECTION = 1;

	DatabaseCommutes database;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    	this.database = new DatabaseCommutes(this);
    	
    	long route_id = getIntent().getLongExtra(RouteConfigurator.EXTRA_ROUTE_ID, RouteConfigurator.INVALID_ROUTE_ID);
	    	
    	
    	Cursor cursor = this.database.getTrips(route_id);

    	SimpleCursorAdapter sca = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, cursor,
    			new String[] {DatabaseCommutes.KEY_WIRELESS_SSID, DatabaseCommutes.KEY_STREET_ADDRESS},
    			new int[] {android.R.id.text1, android.R.id.text2});
    	this.setListAdapter( sca );
    }

    // ======================================================================== 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_locations, menu);
        
        
        return true;
    }
    

	// ========================================================
	void pickMapLocation() {

    	Intent intent = new Intent(Intent.ACTION_PICK);
    	intent.setClass(this, Map.class);
    	startActivityForResult(intent, REQUEST_CODE_MAP_LOCATION_SELECTION);
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
        case R.id.menu_new_location:
        	
        	pickMapLocation();
        	return true;
        }

        return super.onOptionsItemSelected(item);
    }
}