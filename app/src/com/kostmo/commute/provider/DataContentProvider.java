package com.kostmo.commute.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.commute.DatabaseCommutes;

public class DataContentProvider extends ContentProvider {
	

	static final String TAG = "DataContentProvider";
	
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
	   return ColumnSchema.EventData.CONTENT_TYPE_PLOT_DATA;
   }

   @Override
   public Uri insert(Uri uri, ContentValues contentvalues) {
       throw new UnsupportedOperationException("Not supported by this provider");
   }

   @Override
   public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

	   DatabaseCommutes database = new DatabaseCommutes(getContext());
	   Cursor cursor = database.getDestinationPairs();
   
	   return cursor;
   }

   @Override
   public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
       throw new UnsupportedOperationException("Not supported by this provider");
   }
}
