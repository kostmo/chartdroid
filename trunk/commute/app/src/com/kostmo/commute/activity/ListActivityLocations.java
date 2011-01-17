package com.kostmo.commute.activity;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.provider.DatabaseCommutes;

public class ListActivityLocations extends ListActivity {

    public static final String TAG = Market.TAG;


	public static final String EXTRA_LOCATION_ID = "EXTRA_LOCATION_ID";
	public static final long INVALID_LOCATION_ID = -1;
	
	public static final int RESULT_WANTS_NEW_LOCATION = Activity.RESULT_FIRST_USER;


	DatabaseCommutes database;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.locations_list);
    	this.database = new DatabaseCommutes(this);


    	SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.list_item_location, null,
    			new String[] {
					DatabaseCommutes.KEY_LOCATION_TITLE,
    				DatabaseCommutes.KEY_WIRELESS_SSID,
    				DatabaseCommutes.KEY_STREET_ADDRESS,
    				DatabaseCommutes.KEY_LATITUDE,
    				DatabaseCommutes.KEY_LONGITUDE,
    				DatabaseCommutes.KEY_LOCATION_USE_COUNT
    			},
    			new int[] {
					R.id.location_title,
    				R.id.location_wireless,
    				R.id.location_address,
    				R.id.gps_lat,
    				R.id.gps_lon,
    				R.id.location_route_usage_count});
    	
    	this.setListAdapter( sca );
    	this.refreshCursor();
    	

		registerForContextMenu(getListView());
    }

	// ========================================================
    void refreshCursor() {
    	Cursor cursor = this.database.getLocations();
    	((CursorAdapter) this.getListAdapter()).changeCursor(cursor);
    }

    // ========================================================================
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);

    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.context_locations, menu);

    	menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
    	menu.setHeaderTitle("Location action:");
    }

    // ========================================================================
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

    	switch ( item.getItemId() ) {
    	case R.id.menu_delete_location:
    	{
    		// TODO Show a warning dialog that explains that
    		// all routes using this location will also be deleted
    		this.database.deleteLocationInTransaction(info.id);
    		refreshCursor();
    		break;
    	}
    	default:
    		break;
    	}

    	return super.onContextItemSelected(item);
    }

    // ======================================================================== 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_locations, menu);
                
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
        case R.id.menu_new_location:
        {  
        	Intent result = new Intent();
        	result.putExtra(RouteConfigurator.EXTRA_IS_ORIGIN, getIntent().getBooleanExtra(RouteConfigurator.EXTRA_IS_ORIGIN, true));
        	setResult(RESULT_WANTS_NEW_LOCATION, result);
        	
        	finish();

        	return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }

	// ========================================================================
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	Intent result = new Intent();
    	result.putExtra(EXTRA_LOCATION_ID, id);
    	setResult(Activity.RESULT_OK, result);
    	finish();
    }
    
    // ========================================================================
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(request " + requestCode
              + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
            t.show();
            return;
        }

  	   	switch (requestCode) {
   		default:
	    	break;
  	   	}
    }
}