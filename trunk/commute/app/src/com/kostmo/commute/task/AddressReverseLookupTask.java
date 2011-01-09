package com.kostmo.commute.task;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

import com.kostmo.commute.Market;

public abstract class AddressReverseLookupTask extends AsyncTask<Void, Void, Address> {

	static final String TAG = Market.TAG;

	String error_message;
	Context context;
	Location location;

	// ========================================================================
	/** "location" will contain GPS coordinates. */
	public AddressReverseLookupTask(Context context, Location location) {
		
		updateContext(context);
		this.location = location;
	}

	// ====================================================================
	public void updateContext(Context context) {
		this.context = context;
	}

	// ========================================================================
	@Override
	protected void onPreExecute() {

	}

	// ========================================================================
	@Override
	protected Address doInBackground(Void... voided) {

		Geocoder gc = new Geocoder(this.context);		
		try {
			List<Address> matches = gc.getFromLocation(this.location.getLatitude(), this.location.getLongitude(), 1);

			if (matches.size() > 0) {
				Address first_match = matches.get(0);
				return first_match;
			} else {
				
				this.error_message = "No address matches.";
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			
			this.error_message = e.getMessage();
		}

		return null;
	}

	// ========================================================================
	@Override
	protected void onCancelled() {
		cleanUp();
	}
	
	// ========================================================================
	@Override
	public void onPostExecute(Address address) {

		cleanUp();

		if (this.error_message != null) {
			failTask(this.error_message);
		} else {
			completeTask(address);			
		}
	}

	// ========================================================================
	protected abstract void cleanUp();

	// ========================================================================
	protected abstract void failTask(String non_null_error_message);

	// ========================================================================
	protected abstract void completeTask(Address address);
}