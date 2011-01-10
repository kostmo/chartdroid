package com.kostmo.commute.activity;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import com.kostmo.commute.Market;
import com.kostmo.commute.provider.DatabaseCommutes;

public class ListActivityLocations extends ListActivity {

    public static final String TAG = Market.TAG;


	DatabaseCommutes database;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    	this.database = new DatabaseCommutes(this);
    	
    	long route_id = getIntent().getLongExtra(DestinationPairAssociator.EXTRA_ROUTE_ID, DestinationPairAssociator.INVALID_ROUTE_ID);
	    	
    	
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

        /*
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_pair_associator, menu);
        
        
        return true;
    	*/
        return false;
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