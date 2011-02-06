package com.kostmo.commute.provider;



import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import com.kostmo.commute.CalendarPickerConstants;
import com.kostmo.commute.activity.RouteConfigurator.LocationIdPair;
import com.kostmo.commute.activity.RouteConfigurator.GeoAddress;
import com.kostmo.commute.activity.RouteConfigurator.LatLonDouble;
import com.kostmo.tools.DurationStrings.TimescaleTier;

public class DatabaseCommutes extends SQLiteOpenHelper {
	
	static final String TAG = "DatabaseCommutes"; 


    static final String DATABASE_NAME = "COMMUTES";
    static final int DATABASE_VERSION = 2;

    static final int INTEGER_FALSE = 0;
    static final int INTEGER_TRUE = 1;
    
    public static final String TABLE_LOCATIONS = "TABLE_LOCATIONS";
    public static final String TABLE_ROUTES = "TABLE_ROUTES";
    public static final String TABLE_TRIPS = "TABLE_TRIPS";
    public static final String TABLE_TRIP_BREADCRUMBS = "TABLE_TRIP_BREADCRUMBS";
    public static final String TABLE_WIFI_EVENTS = "TABLE_WIFI_EVENTS";
    

    public static final String KEY_LOCATION_ID = "KEY_LOCATION_ID";
    public static final String KEY_LOCATION_TITLE = "KEY_LOCATION_TITLE";
    public static final String KEY_STREET_ADDRESS = "KEY_STREET_ADDRESS";
    public static final String KEY_LATITUDE = "KEY_LATITUDE";
    public static final String KEY_LONGITUDE = "KEY_LONGITUDE";
    public static final String KEY_WIRELESS_SSID = "KEY_WIRELESS_SSID";
    
    public static final String KEY_LOCATION_USE_COUNT = "KEY_LOCATION_USE_COUNT";
    
    
    
    public static final String KEY_START_DESTINATION_ID = "KEY_START_DESTINATION_ID";
    public static final String KEY_END_DESTINATION_ID = "KEY_END_DESTINATION_ID";
    

    public static final String KEY_OUTBOUND_WINDOW_START_MINUTES = "KEY_OUTBOUND_WINDOW_START_MINUTES";
    public static final String KEY_RETURN_WINDOW_START_MINUTES = "KEY_RETURN_WINDOW_START_MINUTES";
    public static final String KEY_TRIP_START_WINDOW_WIDTH_MS = "KEY_TRIP_START_WINDOW_WIDTH_MS";

    public static final String KEY_TRIP_ID = "KEY_TRIP_ID";
    public static final String ROUTE_ID = "ROUTE_ID";
    public static final String KEY_START_TIME = "KEY_START_TIME";	// Timestamp string
    public static final String KEY_END_TIME = "KEY_END_TIME";	// ditto
    public static final String KEY_IS_RETURN_TRIP = "KEY_IS_RETURN_TRIP";
    public static final String KEY_ROUTE_MAX_MINUTES = "KEY_ROUTE_MAX_MINUTES";
    
    
    public static final String KEY_BREADCRUMB_ID = "KEY_BREADCRUMB_ID";
    
    public static final String KEY_TITLE = "KEY_TITLE";
    
    public static final String KEY_TRIP_DURATION_MS = "KEY_TRIP_DURATION_MS";

    public static final String KEY_EVENT_TYPE = "KEY_EVENT_TYPE";
    public static final String KEY_EVENT_DETAILS = "KEY_EVENT_DETAILS";
    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    
    

    public static final String KEY_MIN_START_TIME = "KEY_MIN_START_TIME";
    public static final String KEY_MAX_START_TIME = "KEY_MAX_START_TIME";
    public static final String KEY_MIN_END_TIME = "KEY_MIN_END_TIME";
    public static final String KEY_MAX_END_TIME = "KEY_MAX_END_TIME";
    public static final String KEY_TOTAL_TRIP_COUNT = "KEY_TOTAL_TRIP_COUNT";
    public static final String KEY_RETURN_TRIP_COUNT = "KEY_RETURN_TRIP_COUNT";
    
    public static final String KEY_CUMULATIVE_TRIP_DURATION_MS = "KEY_CUMULATIVE_TRIP_DURATION_MS";
    



    public static final String VIEW_TRIP_TIMES_TRUNCATED_DAY = "VIEW_TRIP_TIMES_TRUNCATED_DAY";
    public static final String VIEW_TRIP_TIMES = "VIEW_TRIP_TIMES";
    public static final String VIEW_AGGREGATED_TRIPS = "VIEW_AGGREGATED_TRIPS";
    public static final String VIEW_AGGREGATED_LABELED_TRIPS = "VIEW_AGGREGATED_LABELED_TRIPS";
    
    public static final String VIEW_AGGREGATE_LOCATIONS = "VIEW_AGGREGATE_LOCATIONS";
    
    


    final static String SQL_CREATE_WIFI_EVENTS_TABLE =
        "create table " + TABLE_WIFI_EVENTS + " ("
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_EVENT_TYPE + " text, "
        + KEY_EVENT_DETAILS + " text, "
        + KEY_TIMESTAMP + " TIMESTAMP NULL default CURRENT_TIMESTAMP);";

    final static String SQL_CREATE_LOCATIONS_TABLE =
        "create table " + TABLE_LOCATIONS + " ("
        + KEY_LOCATION_ID + " integer primary key autoincrement, "
        + KEY_LOCATION_TITLE + " text, "
        + KEY_WIRELESS_SSID + " text, "
        + KEY_STREET_ADDRESS + " text, "
        + KEY_LATITUDE + " float, "
        + KEY_LONGITUDE + " float);";

    final static String SQL_CREATE_ROUTES_TABLE =
        "create table " + TABLE_ROUTES + " ("
        + KEY_TITLE + " text, "
        + KEY_START_DESTINATION_ID + " integer, "
        + KEY_END_DESTINATION_ID + " integer, "
        + KEY_ROUTE_MAX_MINUTES + " integer, "
        + KEY_OUTBOUND_WINDOW_START_MINUTES + " integer, "
        + KEY_RETURN_WINDOW_START_MINUTES + " integer, "
        + KEY_TRIP_START_WINDOW_WIDTH_MS + " integer, "
    	+ "PRIMARY KEY(" + KEY_START_DESTINATION_ID + ", " + KEY_END_DESTINATION_ID + ") ON CONFLICT IGNORE);";

    final static String SQL_CREATE_TRIPS_TABLE =
        "create table " + TABLE_TRIPS + " ("
        + KEY_TRIP_ID + " integer primary key autoincrement, "
        + ROUTE_ID + " integer, "
        + KEY_IS_RETURN_TRIP + " integer default " + INTEGER_FALSE + ", "	// A boolean
        + KEY_START_TIME + " TIMESTAMP NULL default CURRENT_TIMESTAMP, "
        + KEY_END_TIME + " TIMESTAMP);";
   
    final static String SQL_CREATE_TRIP_BREADCRUMBS_TABLE =
        "create table " + TABLE_TRIP_BREADCRUMBS + " ("
        + KEY_BREADCRUMB_ID + " integer primary key autoincrement, "
        + KEY_TRIP_ID + " integer, "
        + KEY_LATITUDE + " float, "
        + KEY_LONGITUDE + " float);";
    
    
    
    

	final static String SQL_CREATE_TRIP_TIMES_VIEW = "create view "
		+ VIEW_TRIP_TIMES + " AS "
		+ ViewQueries.buildTripTimesQuery();

	final static String SQL_CREATE_AGGREGATED_TRIPS_VIEW = "create view "
		+ VIEW_AGGREGATED_TRIPS + " AS "
		+ ViewQueries.buildAggregatedTripsQuery();
	
	final static String SQL_CREATE_AGGREGATED_LABELED_TRIPS_VIEW = "create view "
		+ VIEW_AGGREGATED_LABELED_TRIPS + " AS "
		+ ViewQueries.buildAggregatedLabeledTripsQuery();
	
	final static String SQL_CREATE_TRIP_TIMES_TRUNCATED_DAY_VIEW = "create view "
		+ VIEW_TRIP_TIMES_TRUNCATED_DAY + " AS "
		+ ViewQueries.buildTripTimesTruncatedDayQuery();
	
	final static String SQL_CREATE_AGGREGATE_LOCATIONS_VIEW = "create view "
		+ VIEW_AGGREGATE_LOCATIONS + " AS "
		+ ViewQueries.buildAggregateLocationsQuery();
	
	
	
	
    
    final static String[] table_list = {
    	TABLE_LOCATIONS,
    	TABLE_ROUTES,
    	TABLE_TRIPS,
    	TABLE_TRIP_BREADCRUMBS,
    	TABLE_WIFI_EVENTS
    };

    final static String[] view_list = {
    	VIEW_TRIP_TIMES,
    	VIEW_AGGREGATED_TRIPS,
    	VIEW_AGGREGATED_LABELED_TRIPS,
    	VIEW_TRIP_TIMES_TRUNCATED_DAY
    };
    
    final static String[] table_creation_commands = {
    	SQL_CREATE_LOCATIONS_TABLE,
    	SQL_CREATE_ROUTES_TABLE,
    	SQL_CREATE_TRIPS_TABLE,
    	SQL_CREATE_TRIP_BREADCRUMBS_TABLE,
    	SQL_CREATE_WIFI_EVENTS_TABLE,

    	SQL_CREATE_TRIP_TIMES_VIEW,
    	SQL_CREATE_AGGREGATED_TRIPS_VIEW,
    	SQL_CREATE_AGGREGATED_LABELED_TRIPS_VIEW,
    	SQL_CREATE_TRIP_TIMES_TRUNCATED_DAY_VIEW,
    	SQL_CREATE_AGGREGATE_LOCATIONS_VIEW,
    };

    // ============================================================
    public DatabaseCommutes(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ============================================================
    @Override
    public void onCreate(SQLiteDatabase db) {
    	
    	for (String sql : table_creation_commands) {
    		Log.d(TAG, sql);
        	db.execSQL( sql );
    	}
    }


    // ============================================================
    static class ViewQueries {
        
        // ============================================================
    	static String buildTripTimesQuery() {

    		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
    		query_builder.setTables(TABLE_TRIPS);
    		
    		return query_builder.buildQuery(
    				new String[] {
    						KEY_TRIP_ID,
    						ROUTE_ID,
    						KEY_IS_RETURN_TRIP,
    						KEY_START_TIME,
    						KEY_END_TIME, 
    						"(" + KEY_END_TIME + "-" + KEY_START_TIME + ")" + " AS " + KEY_TRIP_DURATION_MS
    				},
    				null, null, null, null, null, null);
    	}

    	
        // ============================================================
    	static String buildAggregatedTripsQuery() {

    		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
    		query_builder.setTables(VIEW_TRIP_TIMES);
    		
    		return query_builder.buildQuery(
    				new String[] {
    						ROUTE_ID,
    						"COUNT(" + ROUTE_ID + ") AS " + KEY_TOTAL_TRIP_COUNT,
    						"SUM(" + KEY_IS_RETURN_TRIP + ") AS " + KEY_RETURN_TRIP_COUNT,
    						"MIN(" + KEY_START_TIME + ") AS " + KEY_MIN_START_TIME,
    						"MAX(" + KEY_START_TIME + ") AS " + KEY_MAX_START_TIME,
    						"MIN(" + KEY_END_TIME + ") AS " + KEY_MIN_END_TIME,
    						"MAX(" + KEY_END_TIME + ") AS " + KEY_MAX_END_TIME,
    						"SUM(" + KEY_TRIP_DURATION_MS + ") AS " + KEY_CUMULATIVE_TRIP_DURATION_MS},
    				null, null, ROUTE_ID, null, null, null);
    	}
    	
        // ============================================================
    	static String buildAggregatedLabeledTripsQuery() {

    		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_ROUTES + " AS A2"
					+ " LEFT JOIN " + VIEW_AGGREGATED_TRIPS + " AS A1"
					+ " ON (A1." + ROUTE_ID + " = " + "A2." + "ROWID"
					+ ")");
    		
    		return query_builder.buildQuery(
    				new String[] {
    						"A2." + "ROWID" + " AS " + ROUTE_ID,
    						"A2." + KEY_TITLE + " AS " + KEY_TITLE,

    						"A1." + KEY_TOTAL_TRIP_COUNT + " AS " + KEY_TOTAL_TRIP_COUNT,
    						"A1." + KEY_MIN_START_TIME + " AS " + KEY_MIN_START_TIME,
    						"A1." + KEY_MAX_START_TIME + " AS " + KEY_MAX_START_TIME,
    						"A1." + KEY_MIN_END_TIME + " AS " + KEY_MIN_END_TIME,
    						"A1." + KEY_MAX_END_TIME + " AS " + KEY_MAX_END_TIME,
    						"A1." + KEY_CUMULATIVE_TRIP_DURATION_MS + " AS " + KEY_CUMULATIVE_TRIP_DURATION_MS},
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
    	static String buildAggregateLocationsQuery() {

    		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_LOCATIONS + " AS A1"
					+ " LEFT JOIN " + TABLE_ROUTES + " AS A2"
					+ " ON (A1." + KEY_LOCATION_ID + " = " + "A2." + KEY_START_DESTINATION_ID
					+ " OR A1." + KEY_LOCATION_ID + " = " + "A2." + KEY_END_DESTINATION_ID + ")");
			
    		return query_builder.buildQuery(
    				new String[] {
    				KEY_LOCATION_ID,
    				KEY_LOCATION_TITLE,
    				KEY_WIRELESS_SSID,
    				KEY_STREET_ADDRESS,
    				KEY_LATITUDE,
    				KEY_LONGITUDE,
    				"COUNT(" + "A2.ROWID" + ") AS " + KEY_LOCATION_USE_COUNT},
    				null, null,
    				KEY_LOCATION_ID,
    				null, null, null);
    	}        	
    }

    
    // ============================================================
    public int deleteRoute(SQLiteDatabase db, long route_id) {

		int total_deletions = 0;
    	Cursor cursor = db.query(TABLE_TRIPS,
    			new String[] {
    				KEY_TRIP_ID},
				ROUTE_ID + "=?",
				new String[] {Long.toString(route_id)},
				null, null, null);
    	
    	while (cursor.moveToNext())
    		total_deletions += db.delete(TABLE_TRIP_BREADCRUMBS, KEY_TRIP_ID + "=?", new String[] {Long.toString(cursor.getLong(0))});
	    cursor.close();
		
	    total_deletions += db.delete(TABLE_TRIPS, KEY_TRIP_ID + "=?", new String[] {Long.toString(route_id)});
	    total_deletions += db.delete(TABLE_ROUTES, "ROWID=?", new String[] {Long.toString(route_id)});
		
	    return total_deletions;
    }
    
    // ============================================================
    public int deleteRouteInTransaction(long route_id) {

    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		int total_deletions = deleteRoute(db, route_id);
		
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return total_deletions;
    }

    // ============================================================
    /** all routes using this location will also be deleted */
    public int deleteLocationInTransaction(long location_id) {

    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
		int total_deletions = 0;
    	Cursor cursor = db.query(TABLE_ROUTES,
    			new String[] {
    				"ROWID"},
    			KEY_START_DESTINATION_ID + " =? OR " + KEY_END_DESTINATION_ID + " =?",
				new String[] {Long.toString(location_id), Long.toString(location_id)},
				null, null, null);
    	
    	while (cursor.moveToNext())
    		total_deletions += deleteRoute(db, cursor.getLong(0));
	    cursor.close();

	    total_deletions += db.delete(TABLE_LOCATIONS, KEY_LOCATION_ID + "=?", new String[] {Long.toString(location_id)});
		
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return total_deletions;
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
    public GeoAddress getLocationInfo(long location_id) {
    	

    	SQLiteDatabase db = getReadableDatabase();
    	
    	GeoAddress place = null;
    	
    	Cursor cursor = db.query(TABLE_LOCATIONS,
    			new String[] {
	    			KEY_STREET_ADDRESS,
	    			KEY_LATITUDE,
	    			KEY_LONGITUDE,
	    			KEY_WIRELESS_SSID,
	    			KEY_LOCATION_TITLE,
	    		},
	    		KEY_LOCATION_ID + "=?",
    			new String[] {Long.toString(location_id)},
    			null, null, null);
    	
    	if (cursor.moveToFirst()) {
	    	place = new GeoAddress( cursor.getString(0) );
	    	place.latlon = new LatLonDouble();
	    	place.latlon.lat = cursor.getDouble(1);
	    	place.latlon.lon = cursor.getDouble(2);
	    	place.ssid = cursor.getString(3);
	    	
	    	Log.d(TAG, "Lat/lon retrieved: " + place.latlon);
    	}
	    cursor.close();
	       
	    db.close();
	    return place;
    }
    
    
    // ============================================================
    public LocationIdPair getLocationPair(long pair_id) {
    	
    	LocationIdPair pair = null;
    	
    	Log.d(TAG, "About to get address pair with ID: " + pair_id);
    	
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(TABLE_ROUTES,
    			new String[] {
    	        KEY_START_DESTINATION_ID,
    	        KEY_END_DESTINATION_ID,
    			KEY_TITLE},
    			"ROWID=?", new String[] {Long.toString(pair_id)}, null, null, null);
    	
    	if (cursor.moveToFirst()) {
		    pair = new LocationIdPair(cursor.getLong(0), cursor.getLong(1));
		    pair.title = cursor.getString(2);;
    	}

	    cursor.close();
	    db.close();
	    
	    return pair;
    }
    
    // ============================================================
    public Cursor getLocations() {

    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(VIEW_AGGREGATE_LOCATIONS,
    			new String[] {
    			KEY_LOCATION_ID + " AS " + BaseColumns._ID,
    			KEY_LOCATION_TITLE,
    	        KEY_WIRELESS_SSID,
    	        KEY_STREET_ADDRESS,
    	        KEY_LATITUDE,
    	        KEY_LONGITUDE,
    	        KEY_LOCATION_USE_COUNT},
			null, null, null, null, null);
	
    	
    	if (cursor.moveToFirst()) {
	    	// EXPERIMENTAL / DEBUG
	    	do {
	    		
	    		Log.d(TAG, "LOCATION ADDRESS: " + cursor.getString(2));
	    	} while (cursor.moveToNext());
    	}
	    db.close();
	    
	    return cursor;
    }
    
    // ============================================================
    public Cursor getTrips(long route_id) {

    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(VIEW_TRIP_TIMES,
    			new String[] {
    			KEY_TRIP_ID + " AS " + BaseColumns._ID,
				KEY_IS_RETURN_TRIP,
				KEY_START_TIME,
				KEY_END_TIME, 
				KEY_TRIP_DURATION_MS},
			ROUTE_ID + "=?",
			new String[] {Long.toString(route_id)},
			null, null, null);
	
    	
    	if (cursor.moveToFirst()) {
	    	// EXPERIMENTAL / DEBUG
	    	do {
	    		
	    		Log.d(TAG, "TRIP MILLISECONDS: " + cursor.getLong(4));
	    	} while (cursor.moveToNext());
    	}
	    db.close();
	    
	    return cursor;
    }
    // ============================================================
    public Cursor getDestinationPairs() {
    	
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(VIEW_AGGREGATED_LABELED_TRIPS,
    			new String[] {
    			ROUTE_ID + " AS " + BaseColumns._ID,
    			KEY_TITLE,

				KEY_TOTAL_TRIP_COUNT,
				KEY_MIN_START_TIME,
				KEY_MAX_START_TIME,
				KEY_MIN_END_TIME,
				KEY_MAX_END_TIME,
				KEY_CUMULATIVE_TRIP_DURATION_MS},
    			null, null, null, null, null);
    	
    	
    	if (cursor.moveToFirst()) {
	    	// EXPERIMENTAL / DEBUG
	    	do {
	    		
	    		Log.d(TAG, "ROUTE: " + cursor.getString(1));
	    	} while (cursor.moveToNext());
    	}
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
    public long startTrip(long pair_id) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(ROUTE_ID, pair_id);
    	long trip_id = db.insert(TABLE_TRIPS, null, cv);
	    db.close();
	    
	    return trip_id;
    }

    // ============================================================
    public int finishTrip(long trip_id) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(KEY_END_TIME, new Date().toGMTString());
    	int update_count = db.update(TABLE_TRIPS, cv, ROUTE_ID + "=?", new String[] {Long.toString(trip_id)});
	    db.close();
	    
	    return update_count;
    }
    
    
    
    

    
    // ============================================================
    public int updateDestinationWireless(long location_id, String ssid) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	
    	ContentValues cv = new ContentValues();
    	cv.put(KEY_WIRELESS_SSID, ssid);
    	int update_count = db.update(TABLE_LOCATIONS, cv, KEY_LOCATION_ID + "=?", new String[] {Long.toString(location_id)});

	    db.close();
	    
	    return update_count;
    }
    

    
    // ============================================================
    public int updateDestinationGeo(long location_id, LatLonDouble geo) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	
    	ContentValues cv = new ContentValues();
    	cv.put(KEY_LATITUDE, geo.lat);
    	cv.put(KEY_LONGITUDE, geo.lon);
    	int update_count = db.update(TABLE_LOCATIONS, cv, KEY_LOCATION_ID + "=?", new String[] {Long.toString(location_id)});

	    db.close();
	    
	    return update_count;
    }
    
    
    // ============================================================
    public long storeDestination(double lat, double lon, String address, String ssid) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(KEY_LATITUDE, lat);
    	cv.put(KEY_LONGITUDE, lon);

    	if (address != null)
        	cv.put(KEY_STREET_ADDRESS, address);

    	if (ssid != null)
    		cv.put(KEY_WIRELESS_SSID, ssid);

    	long destination_id = db.insert(TABLE_LOCATIONS, null, cv);

	    db.close();
	    
	    return destination_id;
    }

    // ============================================================
    public long storeWifiEvent(String event_type, String details) {

        Log.i(TAG, "Storing action to database: " + event_type);
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(KEY_EVENT_TYPE, event_type);
    	cv.put(KEY_EVENT_DETAILS, details);
    	
    	long event_id = db.insert(TABLE_WIFI_EVENTS, null, cv);

	    db.close();
	    
	    return event_id;
    }

    // ============================================================
    public boolean hasRouteTitle(String title) {
    
		SQLiteDatabase db = getReadableDatabase();
		
		// Limit 1
		Cursor cursor = db.query(TABLE_ROUTES,
				new String[] {KEY_TITLE},
				KEY_TITLE + " LIKE ?",
				new String[] {title},
				null, null, null, "1");
		
		boolean has_record = false;
		if (cursor.moveToFirst()) {
			has_record = true;
		}
		
		cursor.close();
		db.close();
		
		return has_record;
    }
    
    // ============================================================
    public long storePair(long from_id, long to_id, String title) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	

    	cv.put(KEY_START_DESTINATION_ID, from_id);
    	cv.put(KEY_END_DESTINATION_ID, to_id);
    	cv.put(KEY_TITLE, title);

    	long pair_id = db.insert(TABLE_ROUTES, null, cv);

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
