package org.crittr.track.provider;

import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.track.DatabaseSightings;
import org.crittr.track.Market;
import org.crittr.track.retrieval_tasks.NetworkUnavailableException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class SightingEventContentProvider extends ContentProvider {

	public static final String TAG = Market.DEBUG_TAG;
	
		public static final String AUTHORITY = "org.crittr.sighting";
		
	    public static final String CONTENT_TYPE_BASE_SINGLE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/";
	    public static final String CONTENT_TYPE_BASE_MULTIPLE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/";
	    
	    // FIXME - EXPERIMENTAL
		public static final String CONTENT_TYPE_CHARTDROID_EVENT = CONTENT_TYPE_BASE_MULTIPLE + "vnd.com.googlecode.chartdroid.event";
//	    public static final String CONTENT_TYPE_CHARTDROID_EVENT = CONTENT_TYPE_BASE_MULTIPLE + "vnd.com.googlecode.chartdroid.eventx";	// NOT VALID

		
	   private static final String URI_PREFIX = "content://" + AUTHORITY;

	   public static Uri constructUri(String absolute_file_path) {

	       Uri uri = Uri.parse(URI_PREFIX + absolute_file_path);
	       return uri;
	   }

	   /*
	   @Override
	   public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
	       File file = new File(uri.getPath());
	       ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
	       return parcel;
	   }
		*/
	   
	   @Override
	   public boolean onCreate() {
	       return true;
	   }

	   @Override
	   public int delete(Uri uri, String s, String[] as) {
	       throw new UnsupportedOperationException("Not supported by this provider");
	   }

	   @Override
	   public String getType(Uri uri) {
		   return CONTENT_TYPE_CHARTDROID_EVENT;
	   }

	   @Override
	   public Uri insert(Uri uri, ContentValues contentvalues) {
	       throw new UnsupportedOperationException("Not supported by this provider");
	   }

	   @Override
	   public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		   String[] provider_override_projection = {
				   DatabaseSightings.KEY_ROWID,
				   DatabaseSightings.KEY_TSN,
				   "strftime('%s', " + DatabaseSightings.KEY_TIMESTAMP + ") AS " + DatabaseSightings.KEY_TIMESTAMP};
		   DatabaseSightings helper = new DatabaseSightings(getContext());
		   Cursor c0 = helper.list_sightings_2(provider_override_projection, selection, selectionArgs, sortOrder);
		   
		   
		   
		   
		   
		   
	        final String COLUMN_EVENT_TITLE = "COLUMN_EVENT_TITLE";
	        final String COLUMN_EVENT_TIMESTAMP = "COLUMN_EVENT_TIMESTAMP";
		    String[] matrix_override_projection = {BaseColumns._ID, COLUMN_EVENT_TITLE, COLUMN_EVENT_TIMESTAMP};
		    
			MatrixCursor c = new MatrixCursor(matrix_override_projection);
			if (c0 != null && c0.moveToFirst()) {

				int rowid_column = c0.getColumnIndex(DatabaseSightings.KEY_ROWID);
				int tsn_column = c0.getColumnIndex(DatabaseSightings.KEY_TSN);
				int timestamp_column = c0.getColumnIndex(DatabaseSightings.KEY_TIMESTAMP);
				
				do {

					try {
						
						long tsn = c0.getLong(tsn_column);
						Log.d(TAG, "Fetching name for TSN: " + tsn);
						
						String taxon_name = ItisQuery.getScientificNameFromTSN( getContext(), tsn );
						

						Log.d(TAG, "Got it: " + taxon_name);
						
						MatrixCursor.RowBuilder row = c.newRow();
						row.add( c0.getLong(rowid_column) )
							.add( taxon_name )
							.add( c0.getString(timestamp_column) );
						
					} catch (NetworkUnavailableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} while (c0.moveToNext());
			}

			return c;
	   }

	   @Override
	   public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
	       throw new UnsupportedOperationException("Not supported by this provider");
	   }

	}
