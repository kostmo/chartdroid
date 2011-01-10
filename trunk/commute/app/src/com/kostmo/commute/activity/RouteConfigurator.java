package com.kostmo.commute.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
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
	
	
	public static final String EXTRA_ROUTE_ID = "EXTRA_ROUTE_ID";
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
    		
    		final int selector_id = COMPOUND_SELECTORS[i];
    		
        	LocationConfiguratorLayout compound_selector = (LocationConfiguratorLayout) findViewById(selector_id);
        	this.selector_layouts[i] = compound_selector;
        	
        	if (is_editing) {
        		compound_selector.mPickButton.setEnabled(false);
        	} else {
	        	compound_selector.mPickButton.setOnClickListener(new OnClickListener() {
	    			@Override
	    			public void onClick(View v) {
	    				pickPreviousLocation(selector_id);
	    			}
	        	});	
        	}
        	
        	
        	compound_selector.mWifiButton.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				global_selector_id = selector_id;


    		    	Intent i = new Intent(Intent.ACTION_PICK);
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
		        	destination_ids[i] = database.storeDestination(
		        			compound_selector.latlon.lat,
		        			compound_selector.latlon.lon,
		        			compound_selector.getAddress(),
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
    	
    	
    	AddressPair pair = null;
		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.global_selector_id = state.selector_id;
			pair = state.address_pair;

	    	for (int i=0; i<this.selector_layouts.length; i++) {
	    		AddressReverseLookupTaskExtended task = state.reverse_geocode_tasks[i];
	    		this.selector_layouts[i].setAddressLookupTask( task );
	    	}
			
		} else if ( getIntent().hasExtra(EXTRA_ROUTE_ID) ) {
			
			long pair_id = getIntent().getLongExtra(EXTRA_ROUTE_ID, -1);
			
			pair = this.database.getAddressPair(pair_id);
			if (is_editing) {
				Log.d(TAG, "Editing route with pair ID: " + pair_id);
				this.titleEditText.setText(pair.title);
			} else {

				Log.d(TAG, "Duplicating route with pair ID: " + pair_id);
			}
		}
		
		if (pair != null) {
			selector_layouts[0].setWifiNetwork(pair.origin.ssid);
			selector_layouts[0].setAddress(pair.origin.address);

			selector_layouts[1].setWifiNetwork(pair.destination.ssid);
			selector_layouts[1].setAddress(pair.destination.address);
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
    public static class AddressPair {
    	
		public GeoAddress origin, destination;
		public String title;
		
		public AddressPair(GeoAddress origin, GeoAddress destination) {
			this.origin = origin;
			this.destination = destination;
		}
    }
    
	// ========================================================================
	class StateObject {
		int selector_id;
		AddressPair address_pair;
    	AddressReverseLookupTaskExtended[] reverse_geocode_tasks = new AddressReverseLookupTaskExtended[2];
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		StateObject state = new StateObject();
		state.selector_id = this.global_selector_id;
		
    	LocationConfiguratorLayout compound_selector_origin = (LocationConfiguratorLayout) findViewById(R.id.compound_selector_origin);
    	LocationConfiguratorLayout compound_selector_destination = (LocationConfiguratorLayout) findViewById(R.id.compound_selector_destination);
    	state.address_pair = new AddressPair(
    			new GeoAddress(compound_selector_origin.getAddress()),
    			new GeoAddress(compound_selector_destination.getAddress()));
    	
    	for (int i=0; i<this.selector_layouts.length; i++) {
    		state.reverse_geocode_tasks[i] = this.selector_layouts[i].getAddressLookupTask();
    	}
		
		return state;
	}


	// ========================================================
	void pickPreviousLocation(int request_code) {
    	Intent intent = new Intent(this, ListActivityLocations.class);
    	startActivityForResult(intent, request_code);
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
    void locationPicked(Intent data, int request_code) {
    	
    	
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
   		case R.id.compound_selector_origin:
   		case R.id.compound_selector_destination:
   		{
   			locationPicked(data, requestCode);
   			break;
   		}
   		case REQUEST_CODE_WIFI_SELECTION:
   		{
   			String ssid = data.getStringExtra(ListActivityWirelessNetworks.EXTRA_WIFI_SSID);

	    	LocationConfiguratorLayout compound_selector = (LocationConfiguratorLayout) findViewById(global_selector_id);
	    	compound_selector.setWifiNetwork(ssid);
   			Log.d(TAG, "Selected wifi network for " + global_selector_id + ": " + ssid);
   			// TODO
   			break;
   		}
   		default:
	    	break;
  	   	}
    }
}