package org.crittr.track;

import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.containers.ViewHolderFlickrPhoto;
import org.crittr.shared.browser.containers.ViewHolderTaxon;
import org.crittr.shared.browser.utilities.AsyncTaxonInfoPopulatorModified;
import org.crittr.track.activity.SightingsList.LatLonFloat;
import org.crittr.track.newstuff.GetGeocoderInfoTask;
import org.crittr.track.newstuff.HostedPhotoDataPopulatorTask;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SightingsExpandableListAdapter extends ResourceCursorTreeAdapter {

	static final String TAG = Market.DEBUG_TAG;

	AsyncTaxonInfoPopulatorModified taxon_populator;
	Context context;
	DatabaseSightings database_ref;
	public SightingsExpandableListAdapter(
			Context context,
			Cursor cursor,
			int groupLayout,
			int childLayout, 
			AsyncTaxonInfoPopulatorModified taxon_populator,
			DatabaseSightings database_ref) {
		
		super(context, cursor, groupLayout, childLayout);

		this.context = context;
		this.taxon_populator = taxon_populator;
		this.database_ref = database_ref;
	}

	Map<Long, String> geocoded_location_map = new HashMap<Long, String>();

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor,
			boolean isLastChild) {

		int flickr_photo_id_column = 1;
		long photo_id = cursor.getLong(flickr_photo_id_column);
		((TextView) view.findViewById(R.id.flickr_photo_title)).setText( "Photo ID: " + photo_id );


		
		
		
		ViewHolderFlickrPhoto holder = new ViewHolderFlickrPhoto();
		holder.title = (TextView) view.findViewById(R.id.flickr_photo_title);
		holder.description = (TextView) view.findViewById(R.id.flickr_photo_description);
		holder.thumbnail = (ImageView) view.findViewById(R.id.flickr_photo_thumbnail);
		holder.owner = (TextView) view.findViewById(R.id.flickr_photo_owner);
		holder.id_status = (TextView) view.findViewById(R.id.flickr_photo_id_status);

		
		int meta_uri_column_index = cursor.getColumnIndex(DatabaseSightings.KEY_IMAGE_URI);
		Uri meta_uri = Uri.parse( cursor.getString(meta_uri_column_index) );
		
		getHostedImageDataTest(
				meta_uri,
				holder,
				taxon_populator);
	}

	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor,
			boolean isExpanded) {

		int tsn_column = 1;
		long my_tsn = cursor.getLong(tsn_column);
//		((TextView) view.findViewById(R.id.taxon_tsn)).setText( Long.toString( my_tsn ) );


		long rowid = cursor.getLong(0);
		
		
		
		int col_index_lat = cursor.getColumnIndex(DatabaseSightings.KEY_LAT);
		int col_index_lon = cursor.getColumnIndex(DatabaseSightings.KEY_LON);
		if (!(cursor.isNull(col_index_lat) || cursor.isNull(col_index_lon))) {
			
			float lat = cursor.getFloat(col_index_lat);
			float lon = cursor.getFloat(col_index_lon);
			
			float accuracy = cursor.getFloat(cursor.getColumnIndex(DatabaseSightings.KEY_ACCURACY));
			
			TextView place_string_holder = (TextView) view.findViewById(R.id.info_place);
			if (geocoded_location_map.containsKey(rowid)) {
				String txt = geocoded_location_map.get(rowid);
				place_string_holder.setText( txt );
			} else {
				new GetGeocoderInfoTask(context, new LatLonFloat(lat, lon), place_string_holder).execute();
			}
		}
		
		


		long timestamp = cursor.getLong( cursor.getColumnIndex( DatabaseSightings.KEY_TIMESTAMP ) );
//		Log.e(TAG, "Numeric date: " + timestamp);
		Date d = new Date(timestamp * 1000);
		String date_string = DateFormat.getDateTimeInstance().format(d);

//		Log.e(TAG, "Formatted date: " + date_string);

		((TextView) view.findViewById(R.id.info_time)).setText( date_string );






		// Creates a ViewHolder and store references to the two children views
		// we want to bind data to.
		ViewHolderTaxon holder = new ViewHolderTaxon();
		holder.taxon_name_textview = (TextView) view.findViewById(R.id.taxon_name);
		holder.taxon_rank_textview = (TextView) view.findViewById(R.id.taxon_rank_name);

//		holder.tsn_textview = (TextView) view.findViewById(R.id.taxon_tsn);
		holder.thumbnail = (ImageView) view.findViewById(R.id.flickr_photo_thumbnail);

		holder.vernacular_name_textview = (TextView) view.findViewById(R.id.taxon_vernacular_name);
		holder.orphan_textview = (TextView) view.findViewById(R.id.orphan_textview);
		holder.full_view = view.findViewById(R.id.taxon_enclosure);
		holder.rating_bar = (RatingBar) view.findViewById(R.id.small_ratingbar);


		TaxonInfo ti = new TaxonInfo();
		ti.tsn = my_tsn;


		taxon_populator.fetchTaxonRankOnThread(ti, holder);
		taxon_populator.fetchTaxonNameOnThread(my_tsn, holder, 0);
		taxon_populator.fetchTaxonVernacularOnThread(my_tsn, holder);
	}

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		int rowid_index = groupCursor.getColumnIndex(DatabaseSightings.KEY_ROWID);
		long sighting_id = groupCursor.getLong(rowid_index);

		return database_ref.getSightingPhotos(sighting_id);
	}
	

	
	// ========================================================================
	void getHostedImageDataTest(Uri uri, ViewHolderFlickrPhoto holder, AsyncTaxonInfoPopulatorModified taxon_populator) {
		
		Log.e(TAG, "BLAAAARG!!! URI: " + uri);
		
		new HostedPhotoDataPopulatorTask(taxon_populator, context.getContentResolver(), holder).execute(uri);
	}
}
