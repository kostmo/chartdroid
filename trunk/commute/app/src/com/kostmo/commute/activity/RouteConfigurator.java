package com.kostmo.commute.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.activity.prefs.TriggerPreferences;
import com.kostmo.commute.provider.DatabaseCommutes;
import com.kostmo.commute.view.LocationConfiguratorLayout;
import com.kostmo.commute.view.LocationConfiguratorLayout.AddressReverseLookupTaskExtended;

public class RouteConfigurator extends TabActivity {

    public static final String TAG = Market.TAG;

    
	DatabaseCommutes database;
	

	private static final int REQUEST_CODE_WIFI_SELECTION = 1;
	private static final int REQUEST_CODE_PREVIOUS_LOCATION_SELECTION = 2;
	private static final int REQUEST_CODE_MAP_LOCATION_SELECTION = 3;
	
	
	public static final String EXTRA_ROUTE_ID = "EXTRA_ROUTE_ID";
	public static final String EXTRA_IS_ORIGIN = "EXTRA_IS_ORIGIN";
	public static final long INVALID_ROUTE_ID = -1;
	
	static int[] COMPOUND_SELECTORS = {R.id.compound_selector_origin, R.id.compound_selector_destination};
	LocationConfiguratorLayout selector_layouts[] = new LocationConfiguratorLayout[2];
	

    private EditText titleEditText;
    int global_selector_id;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.route_configurator);

        
        TabHost tabHost = getTabHost();
        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("Origin", getResources().getDrawable(R.drawable.ic_menu_home))
                .setContent(R.id.compound_selector_origin));
        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("Destination", getResources().getDrawable(R.drawable.ic_menu_myplaces))
                .setContent(R.id.compound_selector_destination));

    	this.database = new DatabaseCommutes(this);
        this.titleEditText = (EditText) findViewById(R.id.field_pair_title);
        boolean is_editing = Intent.ACTION_EDIT.equals(getIntent().getAction());
        
    	for (int i=0; i<COMPOUND_SELECTORS.length; i++) {
    		
    		final boolean is_origin = i == 0;
        	this.selector_layouts[i] = (LocationConfiguratorLayout) findViewById(COMPOUND_SELECTORS[i]);
        	
        	if (is_editing) {
        		this.selector_layouts[i].mPickButton.setEnabled(false);
        	} else {
        		this.selector_layouts[i].mPickButton.setOnClickListener(new OnClickListener() {
	    			@Override
	    			public void onClick(View v) {
	    				pickPreviousLocation(is_origin);
	    			}
	        	});	
        	}
        	
        	
        	this.selector_layouts[i].mWifiButton.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    		    	Intent i = new Intent(Intent.ACTION_PICK);
    		    	i.putExtra(EXTRA_IS_ORIGIN, is_origin);
    		    	i.setClass(RouteConfigurator.this, ListActivityWirelessNetworks.class);
    		    	startActivityForResult(i, REQUEST_CODE_WIFI_SELECTION);	
    			}
        	});	
    	}


    	View save_button = findViewById(R.id.button_save_pair);
    	save_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
		    	String title = titleEditText.getText().toString();
		    	if (title.length() == 0) {
	            	Toast.makeText(RouteConfigurator.this, "You must enter a title.", Toast.LENGTH_SHORT).show();
	            	return;
		    	} else if (database.hasRouteTitle(title)) {
		    		
		    		Toast.makeText(RouteConfigurator.this, "Title is already taken.", Toast.LENGTH_SHORT).show();
		    		return;
		    	}

				long[] destination_ids = new long[2];
				int i=0;
		    	for (int selector_id : COMPOUND_SELECTORS) {
		        	LocationConfiguratorLayout compound_selector = (LocationConfiguratorLayout) findViewById(selector_id);
		        	
		        	
		        	destination_ids[i] = compound_selector.getLocationId();
		        	database.updateDestinationWireless(
		        			destination_ids[i],
		        			compound_selector.getWifiNetwork());
		        	i++;
		    	}

		    	long pair_id = database.storePair(
		    			destination_ids[0],
		    			destination_ids[1],
		    			title
    			);
            	Toast.makeText(RouteConfigurator.this, "Added pair with id: " + pair_id, Toast.LENGTH_SHORT).show();

				Intent result = new Intent();
				setResult(Activity.RESULT_OK, result);
				finish();
			}
    	});
    	
    	
    	LocationIdPair pair = null;
		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.global_selector_id = state.selector_id;
			
			
			
			
			pair = state.location_pair;

	    	for (int i=0; i<this.selector_layouts.length; i++) {
	    		AddressReverseLookupTaskExtended task = state.reverse_geocode_tasks[i];
	    		this.selector_layouts[i].setAddressLookupTask( task );
	    	}
			
		} else if ( getIntent().hasExtra(EXTRA_ROUTE_ID) ) {
			
			long pair_id = getIntent().getLongExtra(EXTRA_ROUTE_ID, -1);
			
			pair = this.database.getLocationPair(pair_id);
			if (is_editing) {
				Log.d(TAG, "Editing route with pair ID: " + pair_id);
//				this.titleEditText.setText(pair.title);	// THIS SHOULD BE FROZEN TEXT; DON'T NEED TO SAVE
			} else {

				Log.d(TAG, "Duplicating route with pair ID: " + pair_id);
			}
		}
		
		if (pair != null) {
			
			
			selector_layouts[0].setLocation(pair.origin);
			selector_layouts[1].setLocation(pair.destination);
		}
    }

	// ========================================================================
    public static class LatLonDouble {
    	public double lat, lon;
    }

	// ========================================================================
    public static class GeoAddress {

    	public String address, ssid;
    	public GeoAddress(String address) {
    		this.address = address;
    	}
    	public LatLonDouble latlon = new LatLonDouble();
    }
    
	// ========================================================================
    public static class LocationIdPair {
    	
		public long origin, destination;
		public String title;
		
		
		public LocationIdPair(long origin, long destination) {
			this.origin = origin;
			this.destination = destination;
		}
    }
    
	// ========================================================================
	class StateObject {
		int selector_id;
		LocationIdPair location_pair;
    	AddressReverseLookupTaskExtended[] reverse_geocode_tasks = new AddressReverseLookupTaskExtended[2];
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		StateObject state = new StateObject();
		state.selector_id = this.global_selector_id;
		
		
    	state.location_pair = new LocationIdPair(
    			this.selector_layouts[0].getLocationId(),
    			this.selector_layouts[1].getLocationId());
    	
    	for (int i=0; i<this.selector_layouts.length; i++) {
    		state.reverse_geocode_tasks[i] = this.selector_layouts[i].getAddressLookupTask();
    	}
		
		return state;
	}

	// ========================================================
	void pickPreviousLocation(boolean is_origin) {
    	Intent intent = new Intent(this, ListActivityLocations.class);
    	intent.putExtra(EXTRA_IS_ORIGIN, is_origin);
    	startActivityForResult(intent, REQUEST_CODE_PREVIOUS_LOCATION_SELECTION);
	}

	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
		switch (id) {
		}
		return null;
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
        case R.id.menu_settings:
        {
        	Intent intent = new Intent(this, TriggerPreferences.class);
        	startActivity(intent);
        	return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }
    
    // ========================================================================
    /** The Map activity will abstract away all address/location selection to return simply a database record id.
     * The request_code indicates which tab (origin/destination) was targeted. */
    void locationPicked(long location_id, boolean is_origin) {
    	
    	
    }
    
    // ========================================================================
	void pickMapLocation(boolean is_origin) {

    	Intent intent = new Intent(Intent.ACTION_PICK);
    	intent.setClass(this, Map.class);
    	intent.putExtra(EXTRA_IS_ORIGIN, is_origin);
    	startActivityForResult(intent, REQUEST_CODE_MAP_LOCATION_SELECTION);
	}
	
    // ========================================================================
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(request " + requestCode
              + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode == ListActivityLocations.RESULT_WANTS_NEW_LOCATION) {
        	
   			boolean is_origin = data.getBooleanExtra(EXTRA_IS_ORIGIN, true);
        	pickMapLocation(is_origin);
        	
        } else if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        
        

  	   	switch (requestCode) {
   		case REQUEST_CODE_PREVIOUS_LOCATION_SELECTION:
   		{

   			locationPicked(
   					data.getLongExtra(ListActivityLocations.EXTRA_LOCATION_ID, ListActivityLocations.INVALID_LOCATION_ID),
   					data.getBooleanExtra(EXTRA_IS_ORIGIN, true));
   			break;
   		}
   		case REQUEST_CODE_MAP_LOCATION_SELECTION:
   		{

//   			double lat = data.getDoubleExtra(Map.EXTRA_LATITUDE, 0);
//   			double lon = data.getDoubleExtra(Map.EXTRA_LONGITUDE, 0);

   			locationPicked(
   					data.getLongExtra(ListActivityLocations.EXTRA_LOCATION_ID, ListActivityLocations.INVALID_LOCATION_ID),
   					data.getBooleanExtra(EXTRA_IS_ORIGIN, true));
   			break;
   		}
   		case REQUEST_CODE_WIFI_SELECTION:
   		{
   			boolean is_origin = data.getBooleanExtra(EXTRA_IS_ORIGIN, true);
   			
   			String ssid = data.getStringExtra(ListActivityWirelessNetworks.EXTRA_WIFI_SSID);
	    	this.selector_layouts[is_origin ? 0 : 1].setWifiNetwork(ssid);
   			Log.d(TAG, "Selected wifi network: " + ssid);
   			
   			// TODO
   			break;
   		}
   		default:
	    	break;
  	   	}
    }
}