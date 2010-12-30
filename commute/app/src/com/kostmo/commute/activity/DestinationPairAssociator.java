package com.kostmo.commute.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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
import android.widget.Toast;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.provider.DatabaseCommutes;
import com.kostmo.commute.view.DestinationSelectorLayout;

public class DestinationPairAssociator extends Activity {

    public static final String TAG = Market.TAG;

    
	DatabaseCommutes database;
	private static final int DIALOG_PLACE_SELECTION_METHOD = 1;
	private static final int DIALOG_ENTER_ADDRESS = 2;
	
	public static final String EXTRA_ROUTE_ID = "EXTRA_ROUTE_ID";
	
	static int[] COMPOUND_SELECTORS = {R.id.compound_selector_origin, R.id.compound_selector_destination};
	

    private EditText titleEditText;
    int global_selector_id;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.pair_associator);

    	this.database = new DatabaseCommutes(this);

        this.titleEditText = (EditText) findViewById(R.id.field_pair_title);
    	
    	
        boolean is_editing = Intent.ACTION_EDIT.equals(getIntent().getAction());
        
    	for (final int selector_id : COMPOUND_SELECTORS) {
        	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(selector_id);
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
    	}

    	View save_button = findViewById(R.id.button_save_pair);
    	save_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {


				long[] destination_ids = new long[2];
				int i=0;
		    	for (int selector_id : COMPOUND_SELECTORS) {
		        	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(selector_id);
		        	destination_ids[i] = database.storeDestination(compound_selector.lat, compound_selector.lon, compound_selector.getAddress());
		        	i++;
		    	}

		    	String text = titleEditText.getText().toString();
		    	long pair_id = database.storePair(destination_ids[0], destination_ids[1], text);
            	Toast.makeText(DestinationPairAssociator.this, "Added pair with id: " + pair_id, Toast.LENGTH_SHORT).show();

				Intent result = new Intent();
				setResult(Activity.RESULT_OK, result);
				finish();
			}
    	});
    	

    	DestinationSelectorLayout compound_selector_origin = (DestinationSelectorLayout) findViewById(R.id.compound_selector_origin);
    	DestinationSelectorLayout compound_selector_destination = (DestinationSelectorLayout) findViewById(R.id.compound_selector_destination);
    	
    	
    	AddressPair pair = null;
		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.global_selector_id = state.selector_id;
			pair = state.address_pair;

		} else if (is_editing) {
			long pair_id = getIntent().getLongExtra(EXTRA_ROUTE_ID, -1);
			
			pair = this.database.getAddressPair(pair_id);
			this.titleEditText.setText(pair.title);
		}
		
		if (pair != null) {
			compound_selector_origin.setAddress(pair.origin);
	    	compound_selector_destination.setAddress(pair.destination);
		}
    }

	// ========================================================================
    public static class AddressPair {

		public String origin, destination, title;
		public AddressPair(String origin, String destination) {
			this.origin = origin;
			this.destination = destination;
		}
    }
    
	// ========================================================================
	class StateObject {
		int selector_id;
		AddressPair address_pair;
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		StateObject state = new StateObject();
		state.selector_id = this.global_selector_id;
		
    	DestinationSelectorLayout compound_selector_origin = (DestinationSelectorLayout) findViewById(R.id.compound_selector_origin);
    	DestinationSelectorLayout compound_selector_destination = (DestinationSelectorLayout) findViewById(R.id.compound_selector_destination);
    	state.address_pair = new AddressPair(compound_selector_origin.getAddress(), compound_selector_destination.getAddress());
    	
		
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
            View dialog_view = factory.inflate(R.layout.dialog_name_entry, null);

			final EditText name_box = (EditText) dialog_view.findViewById(R.id.name_edit);

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
			.setItems(new String[] {"Contacts", "Current location", "Type address"}, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					switch (which) {
					case 0:
						pickAddress(global_selector_id);
						break;
					case 1:
			        	addCurrentLocation();
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
    void addCurrentLocation() {

    	// FIXME

    	LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


    	// TODO Use this!
//    	lm.addProximityAlert (double latitude, double longitude, float radius, long expiration, PendingIntent intent)
    	
    	
    	Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

    	if (location != null) {
    		
        	Uri uri = Uri.parse("geo:"+location.getLatitude() + "," + location.getLongitude());

        	Intent intent = new Intent(Intent.ACTION_VIEW, uri); 
        	startActivity(intent);
    	} else {
    		Log.e(TAG, "Location was null!");
    	}    	
    }
    
    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        return true;
    }

	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
   		default:
	    	break;
  	   	}
    }
}