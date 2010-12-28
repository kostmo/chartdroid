package com.kostmo.commute.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.R.id;
import com.kostmo.commute.R.menu;
import com.kostmo.commute.provider.DataContentProvider;
import com.kostmo.commute.provider.DatabaseCommutes;

public class Map extends ListActivity {


    public static final String TAG = Market.TAG;

    
    
	DatabaseCommutes database;
	
	
	private static final int DIALOG_CONFIRM_PLACE_ADDITION = 1;

	private static final int REQUEST_CODE_PICK_LOCATION = 1;

	
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    	this.database = new DatabaseCommutes(this);
    	
        SimpleCursorAdapter sca = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null,
        		new String[] {
		    		DatabaseCommutes.KEY_TITLE,
		    		DatabaseCommutes.KEY_START_DESTINATION_ID},
        		new int[] {android.R.id.text1, android.R.id.text2});
        this.setListAdapter(sca);
        
        this.refreshCursor();
    }

	// ========================================================
    void refreshCursor() {

    	Cursor cursor = this.database.getDestinationPairs();
    	((CursorAdapter) this.getListAdapter()).changeCursor(cursor);
    }

	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
		switch (id) {
		case DIALOG_CONFIRM_PLACE_ADDITION:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Add current location?")
			.setMessage("Click OK to add the current location.")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

			    	long pair_id = database.storeDestination(0, 0);
				}
			})
			.setNegativeButton("Cancel", null)
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
        inflater.inflate(R.menu.options_main, menu);
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
        case R.id.pick_address:
        {
        	// TODO Where is the result?

        	Intent i = new Intent(Intent.ACTION_PICK);
        	i.setType("vnd.android.cursor.dir/postal-address_v2");
        	startActivityForResult(i, REQUEST_CODE_PICK_LOCATION);
//        	cmp=com.android.contacts/.ContactsListActivity }

        	// FIXME
/*
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
 */
        	return true;
        }
        case R.id.menu_add:
        {
        	showDialog(DIALOG_CONFIRM_PLACE_ADDITION);
            return true;
        }
        case R.id.menu_about:
        {

        	Uri flickr_destination = Uri.parse( Market.WEBSITE_URL );
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
   		case REQUEST_CODE_PICK_LOCATION:
   		{
   			Log.w(TAG, "Picked location Intent extras:");
   			for (String key : data.getExtras().keySet()) {
   				Log.d(TAG, "Key: " + key);
   				Log.i(TAG, "Data: " + data.getExtras().get(key).toString());
   			}
   			
   			
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
   			

		    Log.d(TAG, "Address: " + address);
   			
   			break;
   		}
   		default:
	    	break;
  	   	}
    }

}