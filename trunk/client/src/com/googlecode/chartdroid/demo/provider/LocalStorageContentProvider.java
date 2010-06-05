package com.googlecode.chartdroid.demo.provider;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.googlecode.chartdroid.core.ColumnSchema.EventData;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class LocalStorageContentProvider extends ContentProvider {

	static final String TAG = "ChartDroid Demo";

	// This must be the same as what as specified as the Content Provider authority
	// in the manifest file.
	public static final String AUTHORITY = "com.googlecode.chartdroid.demo.provider.data3";


	public static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).build();

	static final String MESSAGE_UNSUPPORTED_FEATURE = "Not supported by this provider";

	public static Uri constructUri(long data_id) {
		return ContentUris.withAppendedId(BASE_URI, data_id);
	}

	private static final int CHART_DATA_SERIES = 1;



	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
//		sUriMatcher.addURI(AUTHORITY, labeled_multiseries_path, CHART_DATA_SERIES);
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
//		return PlotData.CONTENT_TYPE_PLOT_DATA;
		return EventData.CONTENT_TYPE_PLOT_DATA;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		// TODO: Re-implement UriMatcher
		int match = sUriMatcher.match(uri);
		Log.d(TAG, "UriMatcher match: " + match);

		
		
		
		switch (match)
		{
		default:
		{

			long dataset_id = ContentUris.parseId(uri);
			Log.d(TAG, "Dataset ID: " + dataset_id);
			
			if (ColumnSchema.Aspect.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				/*
				DatabaseStoredData database = new DatabaseStoredData(getContext());
				SQLiteDatabase db = database.getReadableDatabase();
				Cursor cursor = db.query(DatabaseStoredData.TABLE_DATASETS,
						new String[] {DatabaseStoredData.KEY_DATASET_LABEL},
						DatabaseStoredData.KEY_DATASET_INDEX + "=?",
						new String[] { Long.toString(dataset_id) }, null, null, null);

				Log.d(TAG, "Axes meta row count: " + cursor.getCount());
				
				return cursor;
				*/
				
				return null;
				
			} else if (ColumnSchema.Aspect.DATASET_ASPECT_SERIES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.Aspect.Series.COLUMN_SERIES_LABEL});

				int row_index = 0;
				for (int i=0; i<1; i++) {
					
					c.newRow().add( row_index ).add( "Manually entered series" );
					row_index++;
				}

				Log.d(TAG, "Axes meta row count: " + c.getCount());
				
				return c;

			} else {

				// Fetch the actual data
				DatabaseStoredData database = new DatabaseStoredData(getContext());
				SQLiteDatabase db = database.getReadableDatabase();
				Cursor cursor = db.query(DatabaseStoredData.TABLE_DATA,
					new String[] {
						DatabaseStoredData.KEY_DATUM_INDEX +" AS " + BaseColumns._ID,
				        ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				        DatabaseStoredData.KEY_AXIS_X,
				        DatabaseStoredData.KEY_AXIS_Y,
				        ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
					},
					DatabaseStoredData.KEY_DATASET_INDEX + "=?",
					new String[] { Long.toString(dataset_id) }, null, null, null);

				
				Log.d(TAG, "Data row count: " + cursor.getCount());
				
				return cursor;
			}
		}
		}
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}
}
