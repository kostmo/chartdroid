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
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.activity.prefs.TriggerPreferences;
import com.kostmo.commute.provider.DatabaseCommutes;
import com.kostmo.commute.view.DestinationSelectorLayout;
import com.kostmo.commute.view.DestinationSelectorLayout.AddressReverseLookupTaskExtended;

public class DestinationPairAssociator extends TabActivity {

    public static final String TAG = Market.TAG;

    
	DatabaseCommutes database;
	private static final int DIALOG_PLACE_SELECTION_METHOD = 1;
	private static final int DIALOG_ENTER_ADDRESS = 2;
	

	private static final int REQUEST_CODE_WIFI_SELECTION = 1;
	
	
	public static final String EXTRA_ROUTE_ID = "EXTRA_ROUTE_ID";
	
	static int[] COMPOUND_SELECTORS = {R.id.compound_selector_origin, R.id.compound_selector_destination};
	DestinationSelectorLayout selector_layouts[] = new DestinationSelectorLayout[2];
	

    private EditText titleEditText;
    int global_selector_id;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.pair_associator);

        
        TabHost tabHost = getTabHost();


        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("Origin")
                .setContent(R.id.compound_selector_origin));
        tabHost.addTab(tabHost.newTabSpec("tab3")
                .setIndicator("Destination")
                .setContent(R.id.compound_selector_destination));

    	this.database = new DatabaseCommutes(this);

        this.titleEditText = (EditText) findViewById(R.id.field_pair_title);
    	
    	
        boolean is_editing = Intent.ACTION_EDIT.equals(getIntent().getAction());
        

        
    	for (int i=0; i<COMPOUND_SELECTORS.length; i++) {
    		
    		final int selector_id = COMPOUND_SELECTORS[i];
    		
        	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(selector_id);
        	this.selector_layouts[i] = compound_selector;
        	
        	if (is_editing) {
        		compound_selector.mPickButton.setEnabled(false);
        	} else {
	        	compound_selector.mPickButton.setOnClickListener(new OnClickListener() {
	    			@Override
	    			public void onClick(View v) {
	    				global_selector_id = selector_id;
	    				showDialog(DIALOG_PLACE_SELECTION_METHOD);
	    			}
	        	});	
        	}
        	
        	
        	compound_selector.mWifiButton.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				global_selector_id = selector_id;


    		    	Intent i = new Intent(Intent.ACTION_PICK);
    		    	i.setClass(DestinationPairAssociator.this, ListActivityWirelessNetworks.class);
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
	            	Toast.makeText(DestinationPairAssociator.this, "You must enter a title.", Toast.LENGTH_SHORT).show();
	            	return;
		    	}

				long[] destination_ids = new long[2];
				int i=0;
		    	for (int selector_id : COMPOUND_SELECTORS) {
		        	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(selector_id);
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
            	Toast.makeText(DestinationPairAssociator.this, "Added pair with id: " + pair_id, Toast.LENGTH_SHORT).show();

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
			
		} else if (is_editing) {
			
			long pair_id = getIntent().getLongExtra(EXTRA_ROUTE_ID, -1);
			Log.d(TAG, "Editing location with pair ID: " + pair_id);
			
			pair = this.database.getAddressPair(pair_id);
			this.titleEditText.setText(pair.title);
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
		
    	DestinationSelectorLayout compound_selector_origin = (DestinationSelectorLayout) findViewById(R.id.compound_selector_origin);
    	DestinationSelectorLayout compound_selector_destination = (DestinationSelectorLayout) findViewById(R.id.compound_selector_destination);
    	state.address_pair = new AddressPair(
    			new GeoAddress(compound_selector_origin.getAddress()),
    			new GeoAddress(compound_selector_destination.getAddress()));
    	
    	for (int i=0; i<this.selector_layouts.length; i++) {
    		state.reverse_geocode_tasks[i] = this.selector_layouts[i].getAddressLookupTask();
    	}
		
		return state;
	}

	// ========================================================
    void pickAddress(int request_code) {

    	Intent i = new Intent(Intent.ACTION_PICK);
    	i.setType("vnd.android.cursor.dir/postal-address_v2");
    	startActivityForResult(i, request_code);
    }

	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
		switch (id) {
		case DIALOG_ENTER_ADDRESS:
		{
            View dialog_view = factory.inflate(R.layout.dialog_address_entry, null);

			final EditText name_box = (EditText) dialog_view.findViewById(R.id.address_edit);

            return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Address:")
            .setView(dialog_view)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	String address = name_box.getText().toString().trim();
                	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(global_selector_id);
                	compound_selector.setAddress(address);
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, null)
            .create();
		}
		case DIALOG_PLACE_SELECTION_METHOD:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Select location:")
			.setItems(new String[] {"Contacts", "Current location", "New address"}, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					switch (which) {
					case 0:
						pickAddress(global_selector_id);
						break;
					case 1:

				    	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(global_selector_id);
				    	compound_selector.getLocationFix();
						break;
					case 2:
			        	showDialog(DIALOG_ENTER_ADDRESS);
						break;
					}					
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, null)
			.create();
		}
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
    void resultLocationPicked(Intent data, int view_id) {

			String address = null;
			Cursor c = getContentResolver().query(data.getData(),
					new String[]{ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS},
					null,
					null,
					null
			);
			try {
				if (c.moveToFirst()) {
			    address = c.getString(0);
				} else {
					Log.e(TAG, "Address not found!");
				}
			} finally {
			    c.close();
			}

	    	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(view_id);
	    	compound_selector.setAddress(address);
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
   			resultLocationPicked(data, requestCode);
   			break;
   		}
   		case REQUEST_CODE_WIFI_SELECTION:
   		{
   			String ssid = data.getStringExtra(ListActivityWirelessNetworks.EXTRA_WIFI_SSID);
   			

	    	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(global_selector_id);
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