package com.googlecode.chartdroid.market.sales.provider;

import com.googlecode.chartdroid.market.sales.Market;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class SalesContentProvider extends ContentProvider {

	static final String TAG = Market.TAG;

	// This must be the same as what as specified as the Content Provider authority
	// in the manifest file.
	public static final String AUTHORITY = "com.kostmo.market.provider.sales";

	static final String MESSAGE_UNSUPPORTED_FEATURE = "Not supported by this provider";


	static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path("events").build();




	public static String[] DEMO_AXES_LABELS = { "Date", "Sales" };
	public static String[] DEMO_SERIES_LABELS = { "Everything" };




	// the appended ID is actually not used in this demo.
	public static Uri constructUri(long data_id) {
		return ContentUris.withAppendedId(BASE_URI, data_id);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	@Override
	public String getType(Uri uri) {
		return ColumnSchema.EventData.CONTENT_TYPE_PLOT_DATA;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {



		if (ColumnSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

			MatrixCursor c = new MatrixCursor(new String[] {
					BaseColumns._ID,
					ColumnSchema.COLUMN_AXIS_LABEL});

			int row_index = 0;
			for (int i=0; i<DEMO_AXES_LABELS.length; i++) {

				c.newRow().add( row_index ).add( DEMO_AXES_LABELS[i] );
				row_index++;
			}

			return c;
		} else if (ColumnSchema.DATASET_ASPECT_META.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

			// Get the series labels
			MatrixCursor c = new MatrixCursor(new String[] {
					BaseColumns._ID,
					ColumnSchema.COLUMN_SERIES_LABEL});

			int row_index = 0;
			for (int i=0; i<DEMO_SERIES_LABELS.length; i++) {

				c.newRow().add( row_index ).add( DEMO_SERIES_LABELS[i] );
				row_index++;
			}
			return c;

		} else {
			// Fetch the actual data

			DatabaseStorage database = new DatabaseStorage(getContext());
//			Cursor c = database.getRawSalesForPlotting( ContentUris.parseId(uri) );
			Cursor c = database.getHistogrammedSalesForPlotting( ContentUris.parseId(uri) );

			return c;
		}

	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}
}
