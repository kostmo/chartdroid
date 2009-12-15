package com.googlecode.chartdroid.demo.provider;

import com.googlecode.chartdroid.core.ContentSchema;
import com.googlecode.chartdroid.demo.Demo;
import com.googlecode.chartdroid.demo.Demo.EventWrapper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.List;

public class EventContentProvider extends ContentProvider {
	

	static final String TAG = "Chartdroid Demo";
	
	// This must be the same as what as specified as the Content Provider authority
	// in the manifest file.
	public static final String AUTHORITY = "com.googlecode.chartdroid.demo.provider.events";
	
	
	static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path("events").build();



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
       throw new UnsupportedOperationException("Not supported by this provider");
   }

   @Override
   public String getType(Uri uri) {
	   return ContentSchema.CalendarEvent.CONTENT_TYPE_CALENDAR_EVENT;
   }

   @Override
   public Uri insert(Uri uri, ContentValues contentvalues) {
       throw new UnsupportedOperationException("Not supported by this provider");
   }

   @Override
   public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ContentSchema.CalendarEvent.COLUMN_EVENT_TIMESTAMP,
				ContentSchema.CalendarEvent.COLUMN_EVENT_TITLE});

		List<EventWrapper> generated_events = Demo.generateRandomEvents(5);
//		Log.i(TAG, "Generated " + generated_events.size() + " events.");
		
		// The "EVENT_TITLE" field is omitted (it will be NULL) for this demo.
		for (EventWrapper event : generated_events)
			c.newRow().add(event.id).add( event.timestamp/1000 );

//		Log.i(TAG, "Generated cursor with " + c.getCount() + " rows.");
		return c;
   }

   @Override
   public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
       throw new UnsupportedOperationException("Not supported by this provider");
   }
}
