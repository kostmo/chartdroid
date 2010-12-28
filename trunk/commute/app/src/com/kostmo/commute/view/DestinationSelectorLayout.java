package com.kostmo.commute.view;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;


public class DestinationSelectorLayout extends LinearLayout {
	

    public static final String TAG = Market.TAG;
    
    Context context;
    public double lat, lon;
    String address;

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
        mMapButton = (Button) root.findViewById(R.id.button_map_destination);
        mPickButton = (Button) root.findViewById(R.id.button_choose_destination);
        
        
        mHeader = (TextView) root.findViewById(R.id.header);
        mHeader.setText(title);
        
        mAddressView = (TextView) root.findViewById(R.id.addresss_view);
         
        mMapButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				viewOnMap();
			}
    	});
    }

	// ========================================================
    void viewOnMap() {

		
    	
    	Uri uri = Uri.parse("geo:0,0?q=" + this.address);
//    	Uri uri = Uri.parse("geo:"+location.getLatitude() + "," + location.getLongitude());

    	Intent intent = new Intent(Intent.ACTION_VIEW, uri); 
    	this.context.startActivity(intent);
    }

	// ========================================================
    public void setAddress(String address) {

    	this.address = address;
    	
	    Log.d(TAG, "Address: " + address);
	    Geocoder gc = new Geocoder(this.context);
	    try {
			List<Address> matches = gc.getFromLocationName(address, 3);
			if (matches.size() > 0) {
				Address first_match = matches.get(0);
				
				this.mAddressView.setText(address);
				
				this.lat = first_match.getLatitude();
				this.lon = first_match.getLongitude();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Convenience method to set the title of a SpeechView
     */
    public void setTitle(String title) {
        mHeader.setText(title);
    }

    private TextView mHeader, mAddressView;
    public Button mMapButton, mPickButton;
}