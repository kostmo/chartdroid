package com.kostmo.commute.activity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.provider.DatabaseCommutes;

public class Map extends MapActivity {


    public static final String TAG = Market.TAG;


	public static final String EXTRA_STREET_ADDRESS = "EXTRA_STREET_ADDRESS";
	public static final String EXTRA_LONGITUDE = "EXTRA_LONGITUDE";
	public static final String EXTRA_LATITUDE = "EXTRA_LATITUDE";
	

	private static final int REQUEST_CODE_CONTACT_LOCATION_SELECTION = 1;
    
    
	DatabaseCommutes database;
	MapView mapView;
	MyLocationOverlay my_location_overlay;

    // ========================================================================
    @Override
    public void onResume() {
    	super.onResume();

    	this.my_location_overlay.enableMyLocation();
    }
	
    // ========================================================================
    @Override
    public void onPause() {
    	
    	this.my_location_overlay.disableMyLocation();
    	super.onPause();
    }
    
    // ========================================================================
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.map);
        
    	this.database = new DatabaseCommutes(this);

    	
    	this.mapView = (MapView) findViewById(R.id.mapview);
    	this.mapView.setBuiltInZoomControls(true);
    	

    	this.my_location_overlay = new MyLocationOverlay(this, this.mapView);
    	List<Overlay> mapOverlays =  this.mapView.getOverlays();
    	mapOverlays.add( this.my_location_overlay );
    	
    	
    	Button button_map_search = (Button) findViewById(R.id.btnSearch);
    	button_map_search.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				EditText txtSearch = (EditText)findViewById(R.id.txtMapSearch);
				String area=txtSearch.getText().toString();
				//Toast.makeText(GoogleMap.this, "Click-" + String.valueOf(area), Toast.LENGTH_SHORT).show();
				changeMap(area);
			}
		});
    }
    
    // ========================================================================
	public void changeMap(String area) {
		
        mapView = (MapView) findViewById(R.id.mapview);
        MapController mc=mapView.getController();
        

        GeoPoint myLocation=null;

        double lat = 0;
        double lng = 0;
        try {
       	 
       	 Geocoder g = new Geocoder(this, Locale.getDefault()); 

            java.util.List<android.location.Address> result=g.getFromLocationName(area, 1); 
            if(result.size()>0){

            	Toast.makeText(this, "country: " + String.valueOf(result.get(0).getCountryName()), Toast.LENGTH_SHORT).show();
            	lat = result.get(0).getLatitude();
            	lng = result.get(0).getLongitude();
            }             
            else{
            	Toast.makeText(this, "record not found", Toast.LENGTH_SHORT).show();
            	return;
            }
        } catch(IOException io) {
        	Toast.makeText(this, "Connection Error", Toast.LENGTH_SHORT).show();
        }
        
        myLocation = new GeoPoint(
            (int) (lat * 1E6), 
            (int) (lng * 1E6));
 
        //Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
        mc.animateTo(myLocation);
        mc.setZoom(10); 
        mapView.invalidate();
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
        inflater.inflate(R.menu.options_map, menu);
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
        case R.id.menu_pick_contact_address:
        {
			pickContactAddress();
        	return true;
        }
		case R.id.menu_show_previous_locations:
		{
			showPreviousLocations();
			break;
		}
		case R.id.menu_use_current_location:
		{
			returnWithCurrentLocation();
			break;
		}
        }

        return super.onOptionsItemSelected(item);
    }
    
    
    void insertAndReturnLocation(double lat, double lon, String address) {

    	long location_id = this.database.storeDestination(lat, lon, address, null);
    	    	
    	// TODO
    	Intent result = new Intent();
//    	result.putExtra(EXTRA_LATITUDE, lat);
//    	result.putExtra(EXTRA_LONGITUDE, lon);
    	result.putExtra(ListActivityLocations.EXTRA_LOCATION_ID, location_id);
    	result.putExtra(RouteConfigurator.EXTRA_IS_ORIGIN, getIntent().getBooleanExtra(RouteConfigurator.EXTRA_IS_ORIGIN, true));
    	setResult(Activity.RESULT_OK, result);
    	finish();
    }
    
	// ========================================================
    void returnWithCurrentLocation() {
    	
    	double lat = this.my_location_overlay.getLastFix().getLatitude();
    	double lon = this.my_location_overlay.getLastFix().getLongitude();

    	insertAndReturnLocation(lat, lon, null);
    }
    
	// ========================================================
    void showPreviousLocations() {
    	// TODO
    	
    }
	
	// ========================================================
    void pickContactAddress() {

    	Intent i = new Intent(Intent.ACTION_PICK);
    	i.setType("vnd.android.cursor.dir/postal-address_v2");
    	startActivityForResult(i, REQUEST_CODE_CONTACT_LOCATION_SELECTION);
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
   		case REQUEST_CODE_CONTACT_LOCATION_SELECTION:
   		{
   			String address = contactLocationPicked(data);


//   	    	double lat = this.my_location_overlay.getLastFix().getLatitude();
//   	    	double lon = this.my_location_overlay.getLastFix().getLongitude();

   	    	insertAndReturnLocation(0, 0, address);
   			
   			break;
   		}
   		default:
	    	break;
  	   	}
    }

    // ========================================================================
    String contactLocationPicked(Intent data) {

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

	    	return address;
    }
    
    // ========================================================================
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
}