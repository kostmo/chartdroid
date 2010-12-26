package com.kostmo.commute.provider;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.googlecode.chartdroid.core.ContentSchemaOld;
import com.googlecode.chartdroid.demo.OldChartsActivity;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class DataContentProvider extends ContentProvider {
	

	static final String TAG = "Chartdroid Demo";
	
	// This must be the same as what as specified as the Content Provider authority
	// in the manifest file.
	public static final String AUTHORITY = "com.googlecode.chartdroid.demo.provider.data";
	
	
	static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path("data").build();



	// Let the appended ID represent a unique dataset, so that the Chart can come
	// back and query for the auxiliary (meta) data (e.g. axes labels, colors, etc.).
	// Alternatively,
	// maybe the meta data could be passed along with the original Intent instead.
   public static Uri constructUri(long data_id) {
       return ContentUris.withAppendedId(BASE_URI, data_id);
   }

   
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
	   return ContentSchemaOld.PlotData.CONTENT_TYPE_PLOT_DATA;
   }

   @Override
   public Uri insert(Uri uri, ContentValues contentvalues) {
       throw new UnsupportedOperationException("Not supported by this provider");
   }

   @Override
   public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_AXIS_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_VALUE,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL});

		for (int i=0; i<OldChartsActivity.demo_pie_data.length; i++)
			c.newRow().add( i ).add( 0 ).add( OldChartsActivity.demo_pie_data[i] ).add( OldChartsActivity.demo_pie_labels[i] );

//		Log.i(TAG, "Generated cursor with " + c.getCount() + " rows.");
		return c;
   }

   @Override
   public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
       throw new UnsupportedOperationException("Not supported by this provider");
   }
}
