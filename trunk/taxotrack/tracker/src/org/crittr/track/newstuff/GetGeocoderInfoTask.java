package org.crittr.track.newstuff;

import org.crittr.track.AsyncTaskModified;
import org.crittr.track.activity.SightingsList.LatLonFloat;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GetGeocoderInfoTask extends AsyncTaskModified<Void, Integer, Address> {

	LatLonFloat latlon;
	TextView view_holder;
	Context context;
	public GetGeocoderInfoTask(Context context, LatLonFloat latlon, TextView view_holder) {
		this.context = context;
		this.latlon = latlon;
		this.view_holder = view_holder;

	}

	protected Address doInBackground(Void... urls) {
		Geocoder geoCoder = new Geocoder(this.context, Locale.getDefault());

		List<Address> addresses = null;
		try {
			addresses = geoCoder.getFromLocation(latlon.lat, latlon.lon, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (addresses != null && addresses.size() > 0)
			return addresses.get(0);
		else return null;
	}



	protected void onPostExecute(Address result) {
		if (result != null) {
			view_holder.setText( result.getLocality() + ", " + result.getAdminArea() );
		}
	}
}