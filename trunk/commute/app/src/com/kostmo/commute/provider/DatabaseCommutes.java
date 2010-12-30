package com.kostmo.commute.provider;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import com.kostmo.commute.CalendarPickerConstants;
import com.kostmo.commute.activity.DestinationPairAssociator.AddressPair;
import com.kostmo.tools.DurationStrings.TimescaleTier;

public class DatabaseCommutes extends SQLiteOpenHelper {
	
	static final String TAG = "DatabaseCommutes"; 
 

    static final String DATABASE_NAME = "COMMUTES";
    static final int DATABASE_VERSION = 3;

    public static final String TABLE_DESTINATIONS = "TABLE_DESTINATIONS";
    public static final String TABLE_DESTINATION_PAIRS = "TABLE_DESTINATION_PAIRS";
    public static final String TABLE_TRIPS = "TABLE_TRIPS";
    public static final String TABLE_TRIP_BREADCRUMBS = "TABLE_TRIP_BREADCRUMBS";
    public static final String TABLE_WIFI_EVENTS = "TABLE_WIFI_EVENTS";
    

    public static final String KEY_DESTINATION_ID = "KEY_DESTINATION_ID";
    public static final String KEY_ADDRESS = "KEY_ADDRESS";
    public static final String KEY_LATITUDE = "KEY_LATITUDE";
    public static final String KEY_LONGITUDE = "KEY_LONGITUDE";
    
    public static final String KEY_START_DESTINATION_ID = "KEY_START_DESTINATION_ID";
    public static final String KEY_END_DESTINATION_ID = "KEY_END_DESTINATION_ID";

    public static final String KEY_TRIP_ID = "KEY_TRIP_ID";
    public static final String KEY_DESTINATION_PAIR_ID = "KEY_DESTINATION_PAIR_ID";
    public static final String KEY_START_TIME = "KEY_START_TIME";	// Unix time milliseconds since epoch
    public static final String KEY_END_TIME = "KEY_END_TIME";	// ditto
    
    
    public static final String KEY_BREADCRUMB_ID = "KEY_BREADCRUMB_ID";
    
    public static final String KEY_TITLE = "KEY_TITLE";
    
    public static final String KEY_TRIP_DURATION_MS = "KEY_TRIP_DURATION_MS";

    public static final String KEY_EVENT_TYPE = "KEY_EVENT_TYPE";
    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    

    public static final String VIEW_TRIP_TIMES_TRUNCATED_DAY = "VIEW_TRIP_TIMES_TRUNCATED_DAY";
    public static final String VIEW_TRIP_TIMES = "VIEW_TRIP_TIMES";
    


    final static String SQL_CREATE_WIFI_EVENTS_TABLE =
        "create table " + TABLE_WIFI_EVENTS + " ("
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_EVENT_TYPE + " text, "
        + KEY_TIMESTAMP + " TIMESTAMP NULL default CURRENT_TIMESTAMP);";

    final static String SQL_CREATE_DESTINATIONS_TABLE =
        "create table " + TABLE_DESTINATIONS + " ("
        + KEY_DESTINATION_ID + " integer primary key autoincrement, "
        + KEY_ADDRESS + " text, "
        + KEY_LATITUDE + " float, "
        + KEY_LONGITUDE + " float);";

    final static String SQL_CREATE_DESTINATION_PAIRS_TABLE =
        "create table " + TABLE_DESTINATION_PAIRS + " ("
        + KEY_TITLE + " text, "
        + KEY_START_DESTINATION_ID + " integer, "
        + KEY_END_DESTINATION_ID + " integer, "
    	+ "PRIMARY KEY(" + KEY_START_DESTINATION_ID + ", " + KEY_END_DESTINATION_ID + ") ON CONFLICT IGNORE);";

    final static String SQL_CREATE_TRIPS_TABLE =
        "create table " + TABLE_TRIPS + " ("
        + KEY_TRIP_ID + " integer primary key autoincrement, "
        + KEY_DESTINATION_PAIR_ID + " integer, "
        + KEY_START_TIME + " integer, "
        + KEY_END_TIME + " integer);";
   

    final static String SQL_CREATE_TRIP_BREADCRUMBS_TABLE =
        "create table " + TABLE_TRIP_BREADCRUMBS + " ("
        + KEY_BREADCRUMB_ID + " integer primary key autoincrement, "
        + KEY_TRIP_ID + " integer, "
        + KEY_LATITUDE + " float, "
        + KEY_LONGITUDE + " float);";
    
    
    
    

	final static String SQL_CREATE_TRIP_TIMES_VIEW = "create view "
		+ VIEW_TRIP_TIMES + " AS "
		+ buildTripTimesQuery();

	
	final static String SQL_CREATE_TRIP_TIMES_TRUNCATED_DAY_VIEW = "create view "
		+ VIEW_TRIP_TIMES_TRUNCATED_DAY + " AS "
		+ buildTripTimesTruncatedDayQuery();
    
    final static String[] table_list = {
    	TABLE_DESTINATIONS,
    	TABLE_DESTINATION_PAIRS,
    	TABLE_TRIPS,
    	TABLE_TRIP_BREADCRUMBS,
    	TABLE_WIFI_EVENTS
    };


    final static String[] view_list = {
    	VIEW_TRIP_TIMES,
    	VIEW_TRIP_TIMES_TRUNCATED_DAY
    };
    
    
    final static String[] table_creation_commands = {
    	SQL_CREATE_DESTINATIONS_TABLE,
    	SQL_CREATE_DESTINATION_PAIRS_TABLE,
    	SQL_CREATE_TRIPS_TABLE,
    	SQL_CREATE_TRIP_BREADCRUMBS_TABLE,
    	SQL_CREATE_WIFI_EVENTS_TABLE,
    	
    	SQL_CREATE_TRIP_TIMES_VIEW,
    	SQL_CREATE_TRIP_TIMES_TRUNCATED_DAY_VIEW,
    };

    // ============================================================
    public DatabaseCommutes(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ============================================================
    
    @Override
    public void onCreate(SQLiteDatabase db) 
    {
    	for (String sql : table_creation_commands) {
    		Log.d(TAG, sql);
        	db.execSQL( sql );
    	}
    }

    // ============================================================
	static String buildTripTimesQuery() {

		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
		query_builder.setTables(TABLE_TRIPS);
		
		return query_builder.buildQuery(
				new String[] {
						KEY_TRIP_ID,
						KEY_DESTINATION_PAIR_ID,
						KEY_START_TIME,
						 KEY_END_TIME, 
						 "(" + KEY_END_TIME + "-" + KEY_START_TIME + ")" + " AS " + KEY_TRIP_DURATION_MS},
				null, null, null, null, null, null);
	}


    // ============================================================
	static String buildTripTimesTruncatedDayQuery() {

		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
		query_builder.setTables(VIEW_TRIP_TIMES);
		
		return query_builder.buildQuery(
				new String[] {
				KEY_TRIP_ID + " AS " + BaseColumns._ID,
				KEY_TRIP_DURATION_MS + " AS " + EventContentProvider.COLUMN_QUANTITY0,
				"CAST((CAST(" + KEY_START_TIME + "*1000/" + TimescaleTier.DAYS.millis + " AS INTEGER)*" + TimescaleTier.DAYS.millis + ") AS INTEGER) " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP},
				null, null, null, null, null, null);
	}

    
    // ============================================================
    /** Kind of a pointless function */
    public int deleteWithCount() {

    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
		int total_deletions = 0;
		for (String table : table_list)
			total_deletions += db.delete(table, null, null);

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return total_deletions;
    }

    

    
    
    

    // ============================================================
    public AddressPair getAddressPair(long pair_id) {
    	
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(TABLE_DESTINATION_PAIRS,
    			new String[] {
    	        KEY_START_DESTINATION_ID,
    	        KEY_END_DESTINATION_ID,
    			KEY_TITLE},
    			"ROWID=?", new String[] {Long.toString(pair_id)}, null, null, null);
    	cursor.moveToFirst();
    	

	    long[] place_ids = new long[2];
	    for (int i=0; i<2; i++) {
		    place_ids[i] = cursor.getLong(i);
	    }
    	String title = cursor.getString(2);
	    cursor.close();
	    
	    String[] places = new String[2];
	    int i=0;
	    for (long place_id : place_ids) {
	    	
	    	Cursor cursor2 = db.query(TABLE_DESTINATIONS,
	    			new String[] {
	    			KEY_ADDRESS,
	    			KEY_LATITUDE,
	    			KEY_LONGITUDE},
	    			"KEY_DESTINATION_ID=?", new String[] {Long.toString(place_id)}, null, null, null);
	    	cursor2.moveToFirst();
	    	places[i] = cursor2.getString(0);
		    cursor2.close();
	    	
		    i++;
	    }
	    
	    
	    AddressPair pair = new AddressPair(places[0], places[1]);
	    pair.title = title;
	    

	    db.close();
	    
	    return pair;
    }

    
    // ============================================================
    public Cursor getDestinationPairs() {
    	
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(TABLE_DESTINATION_PAIRS,
    			new String[] {
    			"ROWID AS " + BaseColumns._ID,
    			KEY_TITLE,
    	        KEY_START_DESTINATION_ID,
    	        KEY_END_DESTINATION_ID},
    			null, null, null, null, null);
    	cursor.moveToFirst();
	    db.close();
	    
	    return cursor;
    }

    // ============================================================
    public void dumpWifiTable() {
    	
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(TABLE_WIFI_EVENTS,
    			new String[] {
    			BaseColumns._ID,
    			KEY_TIMESTAMP,
    	        KEY_EVENT_TYPE},
    			null, null, null, null, null);
    	
    	while (cursor.moveToNext()) {
    		Log.d(TAG, "EVENT " + cursor.getLong(0) + " (" + cursor.getString(1) + "): " + cursor.getString(2));
    	}
    	
    	cursor.close();
	    db.close();
    }
    
    // ============================================================
    public long storeDestination(double lat, double lon, String address) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(KEY_LATITUDE, lat);
    	cv.put(KEY_LONGITUDE, lon);
    	cv.put(KEY_ADDRESS, address);

    	long destination_id = db.insert(TABLE_DESTINATIONS, null, cv);

	    db.close();
	    
	    return destination_id;
    }

    // ============================================================
    public long storeWifiEvent(String event_type) {

        Log.i(TAG, "Storing action to database: " + event_type);
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(KEY_EVENT_TYPE, event_type);
    	long event_id = db.insert(TABLE_WIFI_EVENTS, null, cv);

	    db.close();
	    
	    return event_id;
    }
    
    // ============================================================
    public long storePair(long from_id, long to_id, String title) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	

    	cv.put(KEY_START_DESTINATION_ID, from_id);
    	cv.put(KEY_END_DESTINATION_ID, to_id);
    	cv.put(KEY_TITLE, title);

    	long pair_id = db.insert(TABLE_DESTINATION_PAIRS, null, cv);

	    db.close();
	    
	    return pair_id;
    }
    

	// ========================================================================
	public Cursor getCalendarPlayTimesGrouped(String[] projection,
			String selection, String[] selection_args, String order_by) {
		
		SQLiteDatabase db = getReadableDatabase();
		
		
		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
		query_builder.setTables(VIEW_TRIP_TIMES_TRUNCATED_DAY);

		Cursor cursor = query_builder.query(
				db,
				new String[] {
				BaseColumns._ID,
				0 + " AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.CALENDAR_ID,
				"SUM(" + EventContentProvider.COLUMN_QUANTITY0 + ")/" + TimescaleTier.MINUTES.millis + " AS " + EventContentProvider.COLUMN_QUANTITY0,
				"COUNT(" + BaseColumns._ID + ") AS " + EventContentProvider.COLUMN_QUANTITY1,
				"'Total play time' AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TITLE,
				CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP},
				selection, selection_args,
				// Group by:
				CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP,
				null, order_by, null);
		
		return cursor;
	}
    
    // ============================================================
    public void drop_all_tables(SQLiteDatabase db) {

    	for (String table : table_list)
    		db.execSQL("DROP TABLE IF EXISTS " + table);

    	for (String table : view_list)
    		db.execSQL("DROP VIEW IF EXISTS " + table);
    }
    
    // ============================================================
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
    {
        Log.w(TAG, "Upgrading database from version " + oldVersion 
                + " to "
                + newVersion + ", which will destroy all old data");

        drop_all_tables(db);
        
        onCreate(db);
    }
}
