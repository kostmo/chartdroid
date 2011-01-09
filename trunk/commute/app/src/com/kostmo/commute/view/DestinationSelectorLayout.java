package com.kostmo.commute.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.activity.DestinationPairAssociator.LatLonDouble;
import com.kostmo.commute.activity.prefs.TriggerPreferences;
import com.kostmo.commute.task.AddressReverseLookupTask;


public class DestinationSelectorLayout extends LinearLayout {
	

    public static final String TAG = Market.TAG;
    
    Context context;
    public LatLonDouble latlon = new LatLonDouble();
    String address;
    String wireless_ssid;
    
    
    
    AddressReverseLookupTaskExtended address_lookup_task;
    
    ProgressBar progress_bar_gps;

    private TextView mHeader, mAddressView, mWifiView;
    public Button mMapButton, mPickButton, mWifiButton, mDepartureWindowButton;
    
	// ========================================================
    public DestinationSelectorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DestinationSelectorLayout);
        String label = a.getString(R.styleable.DestinationSelectorLayout_title);
        a.recycle();
        
        init(context, label);
    }

	// ========================================================
    public DestinationSelectorLayout(Context context, String title) {
        super(context);
        init(context, title);
    }

	// ========================================================
    void init(Context context, String title) {
    	
        this.context = context;

        LayoutInflater factory = LayoutInflater.from(context);
        View root = factory.inflate(R.layout.destination_selector, this);
        this.mPickButton = (Button) root.findViewById(R.id.button_choose_destination);
        this.mMapButton = (Button) root.findViewById(R.id.button_map_destination);
        this.mMapButton.setEnabled(false);
        this.mMapButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				viewOnMap();
			}
    	});
        this.mWifiButton = (Button) root.findViewById(R.id.button_wireless_network);
        this.mDepartureWindowButton = (Button) root.findViewById(R.id.button_departure_window);
        this.mHeader = (TextView) root.findViewById(R.id.header);
        this.mHeader.setText(title);
    
        this.progress_bar_gps = (ProgressBar) root.findViewById(R.id.progress_bar_gps);
        
        this.mAddressView = (TextView) root.findViewById(R.id.addresss_view);
        this.mWifiView = (TextView) root.findViewById(R.id.wireless_network_view);
        
    }

	// ========================================================================
    public AddressReverseLookupTaskExtended getAddressLookupTask() {
    	return this.address_lookup_task;
    }

	// ========================================================================
    public void setAddressLookupTask(AddressReverseLookupTaskExtended task) {

    	if (task != null) {
    		task.updateContext(this.context);
    	}

    	this.address_lookup_task = task;
    }
    
	// ========================================================================
    public class AddressReverseLookupTaskExtended extends AddressReverseLookupTask {

		public AddressReverseLookupTaskExtended(Context context,
				Location location) {
			super(context, location);
		}

		// ========================================================================
		@Override
		protected void onPreExecute() {
	    	progress_bar_gps.setVisibility(View.VISIBLE);
		}

		// ========================================================================		
    	@Override
    	protected void completeTask(Address address) {
        	
    		// FIXME This has a redundant geocoding
    		List<String> street_address_lines = new ArrayList<String>();
    		for (int i=0; i<address.getMaxAddressLineIndex(); i++) {
    			street_address_lines.add( address.getAddressLine(i) );
    		}
    		setAddress( TextUtils.join("\n", street_address_lines));
    	}

		// ========================================================================		
		@Override
		protected void failTask(String nonNullErrorMessage) {
			Toast.makeText(context, nonNullErrorMessage, Toast.LENGTH_LONG).show();
		}

		// ========================================================================		
		@Override
		protected void cleanUp() {
	    	progress_bar_gps.setVisibility(View.GONE);
		}
    }
    
    // ========================================================================
    public void getLocationFix() {

    	this.progress_bar_gps.setVisibility(View.VISIBLE);

    	final LocationManager lm = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    	// Register the listener with the Location Manager to receive location updates
    	

    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
    	
    	String location_provider_source = settings.getString(TriggerPreferences.PREFKEY_LOCATION_SOURCE, TriggerPreferences.DEFAULT_LOCATION_SOURCE);
    	lm.requestLocationUpdates(location_provider_source, 0, 0, new LocationListener() {
    	    public void onLocationChanged(Location location) {
      	      // Called when a new location is found by the network location provider.
    	    	storeLocationFix(location);
    	    	lm.removeUpdates(this);
      	    }

      	    public void onStatusChanged(String provider, int status, Bundle extras) {}

      	    public void onProviderEnabled(String provider) {}

      	    public void onProviderDisabled(String provider) {}
      	});
    }
    
    // ========================================================================
    void storeLocationFix(Location location) {
    

    	this.progress_bar_gps.setVisibility(View.GONE);
    	
    	Log.d(TAG, "Location found. Determining address...");

    	
    	this.address_lookup_task = new AddressReverseLookupTaskExtended(this.context, location);
    	address_lookup_task.execute();
    	
    	

//    	Uri uri = Uri.parse("geo:"+location.getLatitude() + "," + location.getLongitude());
//    	Intent intent = new Intent(Intent.ACTION_VIEW, uri); 
//    	startActivity(intent);s
    }
    
    
	// ========================================================
    void viewOnMap() {

    	Uri uri = Uri.parse("geo:0,0?q=" + this.address);
//    	Uri uri = Uri.parse("geo:"+location.getLatitude() + "," + location.getLongitude());

    	Intent intent = new Intent(Intent.ACTION_VIEW, uri); 
    	this.context.startActivity(intent);
    }

	// ========================================================
    public String getAddress() {
    	return this.address;
    }

	// ========================================================
    public void setAddress(String address) {

    	this.address = address;
		this.mAddressView.setText(address);
    	this.mMapButton.setEnabled( this.address != null && this.address.length() > 0 );
    	
	    Log.d(TAG, "Address: " + address);
	    Geocoder gc = new Geocoder(this.context);
	    try {
			List<Address> matches = gc.getFromLocationName(address, 3);
			if (matches.size() > 0) {
				Address first_match = matches.get(0);
				
				this.latlon.lat = first_match.getLatitude();
				this.latlon.lon = first_match.getLongitude();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
	// ========================================================
    public void setWifiNetwork(String ssid) {

    	this.wireless_ssid = ssid;
		this.mWifiView.setText(ssid);
    }
    

	// ========================================================
    public String getWifiNetwork() {
    	return this.wireless_ssid;
    }
    	
}