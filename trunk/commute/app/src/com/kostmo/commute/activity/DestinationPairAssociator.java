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
import com.kostmo.commute.R.id;
import com.kostmo.commute.R.layout;
import com.kostmo.commute.R.menu;
import com.kostmo.commute.R.string;
import com.kostmo.commute.provider.DataContentProvider;
import com.kostmo.commute.provider.DatabaseCommutes;
import com.kostmo.commute.view.DestinationSelectorLayout;

public class DestinationPairAssociator extends Activity {

    public static final String TAG = Market.TAG;

    
	DatabaseCommutes database;
	private static final int DIALOG_CONFIRM_PLACE_ADDITION = 1;
	private static final int DIALOG_ROUTE_NAME = 2;
	
	
	static int[] COMPOUND_SELECTORS = {R.id.compound_selector_origin, R.id.compound_selector_destination};
	

    private EditText titleEditText;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.pair_associator);

    	this.database = new DatabaseCommutes(this);


        this.titleEditText = (EditText) findViewById(R.id.field_pair_title);
    	
    	
    	for (final int selector_id : COMPOUND_SELECTORS) {
        	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(selector_id);
        	compound_selector.mPickButton.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				pickAddress(selector_id);
    			}
        	});	
    	}

    	findViewById(R.id.button_save_pair).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {


				long[] destination_ids = new long[2];
				int i=0;
		    	for (int selector_id : COMPOUND_SELECTORS) {
		        	DestinationSelectorLayout compound_selector = (DestinationSelectorLayout) findViewById(selector_id);
		        	destination_ids[i] = database.storeDestination(compound_selector.lat, compound_selector.lon);
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
		case DIALOG_ROUTE_NAME:
		{
            View dialog_view = factory.inflate(R.layout.dialog_name_entry, null);

			final EditText name_box = (EditText) dialog_view.findViewById(R.id.name_edit);

            return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Route Name")
            .setView(dialog_view)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	String name = name_box.getText().toString().trim();
                	Toast.makeText(DestinationPairAssociator.this, name + " saved.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, null)
            .create();
		}
		case DIALOG_CONFIRM_PLACE_ADDITION:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Add current location?")
			.setMessage("Click OK to add the current location.")
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

			    	long destination_id = database.storeDestination(0, 0);
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

	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
        {
        	showDialog(DIALOG_CONFIRM_PLACE_ADDITION);
            return true;
        }
        case R.id.menu_add_here:
        {


        	// FIXME

        	LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

  
        	// TODO Use this!
//        	lm.addProximityAlert (double latitude, double longitude, float radius, long expiration, PendingIntent intent)
        	
        	
        	Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        	if (location != null) {
        		
	        	Uri uri = Uri.parse("geo:"+location.getLatitude() + "," + location.getLongitude());
	
	        	Intent intent = new Intent(Intent.ACTION_VIEW, uri); 
	        	startActivity(intent);
        	} else {
        		Log.e(TAG, "Location was null!");
        	}
            return true;
        }
        case R.id.menu_more_apps:
        {
        	
	    	Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
	    	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
	    	startActivity(i);
            return true;
        }
        case R.id.menu_plot_times:
        {
	    
        	// FIXME
        	long data_id = 0;
	    	Intent i = new Intent(Intent.ACTION_VIEW, DataContentProvider.constructUri(data_id));
	    	startActivity(i);
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
   		default:
	    	break;
  	   	}
    }
}