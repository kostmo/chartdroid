package org.crittr.track;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class DatabaseSightings extends SQLiteOpenHelper {
	
	public static final String TAG = Market.DEBUG_TAG;
	
	static final String DATABASE_NAME = "SIGHTINGS_DATA";
    static final int DATABASE_VERSION = 5;
    
    
	public static final long INVALID_TSN = -1;
	public static final long NO_PARENT_ID = 0;
	public static final long ORPHAN_PARENT_ID = NO_PARENT_ID;
	public static final int INVALID_RANK_ID = -1;


    
    public static final String TABLE_SIGHTINGS = "TABLE_SIGHTINGS";
    public static final String TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS = "TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS";

	public static final String VIEW_EVENTS_PROVIDER = "VIEW_EVENTS_PROVIDER";

    

    public static final String IMPLICIT_ROWID = "ROWID";
    public static final String KEY_ROWID = BaseColumns._ID;
    public static final String KEY_TSN = "KEY_TSN";
    public static final String KEY_USE_COUNT = "KEY_USE_COUNT";


    public static final String KEY_LAT = "KEY_LAT";
    public static final String KEY_LON = "KEY_LON";
    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    public static final String KEY_ACCURACY = "KEY_ACCURACY";
    
    public static final String KEY_SIGHTING_ID = "KEY_SIGHTING_ID";
    public static final String KEY_SIGHTING_TITLE = "KEY_SIGHTING_TITLE";
    public static final String KEY_IMAGE_URI = "KEY_FLICKR_PHOTO_ID";
    

    public static final String KEY_THUMBNAIL_URL = "KEY_THUMBNAIL_URL";
    public static final String KEY_LOCAL_THUMBNAIL_PATH = "KEY_LOCAL_THUMBNAIL_PATH";
    
    
  
    static final String SQL_CREATE_SIGHTINGS_TABLE =
        "create table " + TABLE_SIGHTINGS + " (" 
        + KEY_ROWID + " integer primary key autoincrement, "
        + KEY_TSN + " integer default " + INVALID_TSN + ", "
        + KEY_SIGHTING_TITLE + " text, "
        + KEY_LAT + " float, "
        + KEY_LON + " float, " 
        + KEY_ACCURACY + " float,"
        + KEY_TIMESTAMP + " TIMESTAMP NULL default CURRENT_TIMESTAMP);";
    
    static final String SQL_CREATE_PHOTOGRAPH_ASSOCIATIONS_TABLE =
        "create table " + TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS + " ("
        + KEY_SIGHTING_ID + " integer, "
        + KEY_IMAGE_URI + " text,"
        + "PRIMARY KEY(" + KEY_SIGHTING_ID + ", " + KEY_IMAGE_URI + ") ON CONFLICT IGNORE);";
   
    
	final static String SQL_CREATE_EVENTS_PROVIDER_VIEW = "create view "
		+ VIEW_EVENTS_PROVIDER + " AS "
		+ buildEventsProviderViewQuery();
    
    

    final static String[] table_list = {
    	TABLE_SIGHTINGS,
    	TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS};
    
	final static String[] view_list = {
		VIEW_EVENTS_PROVIDER
	};
    
    final static String[] table_creation_commands = {
    	SQL_CREATE_SIGHTINGS_TABLE,
    	SQL_CREATE_PHOTOGRAPH_ASSOCIATIONS_TABLE,
    	
    	SQL_CREATE_EVENTS_PROVIDER_VIEW
	};
    
    
    
    Context context;
    // ==================================================================
    public DatabaseSightings(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    
	// ========================================================================
	private static String buildEventsProviderViewQuery() {

		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
		query_builder.setTables(TABLE_SIGHTINGS);

		// Here we have an extra column for aggregates
		return query_builder.buildQuery(
				new String[] {
				KEY_ROWID,
				KEY_SIGHTING_TITLE + " AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TITLE,
				"CAST((CAST(strftime('%s', " + KEY_TIMESTAMP + ") AS INTEGER)*1000) AS INTEGER) AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP},
				null, null, null, null, null, null);
	}
    
    // ==================================================================
    public void wipe() {

    	SQLiteDatabase db = getWritableDatabase();
    	db.beginTransaction();

    	drop_all_tables(db);

    	try {
    		db.setTransactionSuccessful();
    	} finally {
    		db.endTransaction();
    	}

    	db.close();
    }

    // ==================================================================
    public void dumpSightingsView() {


	    SQLiteDatabase db = getReadableDatabase();
	    Cursor cursor = db.query(VIEW_EVENTS_PROVIDER, null, null, null, null, null, null);
	    
	    Log.d(TAG, "Available columns:");
	    Log.i(TAG, TextUtils.join(", ", cursor.getColumnNames()));
	    
	    while (cursor.moveToNext()) {
	    	Log.w(TAG, "Row " + cursor.getPosition());
	    	for (int i=0; i<cursor.getColumnCount(); i++) {
	    		Log.d(TAG, cursor.getString(i));
	    	}
	    }
	    
	    cursor.close();
	    db.close();
    }

    // ==================================================================
    public void dumpSightingsTable() {
    	
    	String[] projection = new String[] {
	    		KEY_ROWID,
	            KEY_TSN,
	            KEY_LAT,
	            KEY_LON,
	            KEY_ACCURACY,
	            KEY_TIMESTAMP
	    };
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor cursor = db.query(TABLE_SIGHTINGS, null, null, null, null, null, null);
	    
	    Log.d(TAG, "Available columns:");
	    Log.i(TAG, TextUtils.join(", ", cursor.getColumnNames()));
	    
	    while (cursor.moveToNext()) {
	    	Log.w(TAG, "Row " + cursor.getPosition());
	    	for (int i=0; i<cursor.getColumnCount(); i++) {
	    		Log.d(TAG, cursor.getString(i));
	    	}
	    }
	    
	    cursor.close();
	    db.close();
    }

    
    // ==================================================================
    public void recordSighting(long tsn, String title) {
    	
	    SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(KEY_TSN, tsn);
        cv.put(KEY_SIGHTING_TITLE, title);
		
    	LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    	Location last_location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	if (last_location != null) {
    		cv.put(KEY_LAT, last_location.getLatitude());
            cv.put(KEY_LON, last_location.getLongitude());
            cv.put(KEY_ACCURACY, last_location.getAccuracy());
    	}	
    	else {
    		Log.e(TAG, "Needs at least one location update!");
    	}

        db.insert(TABLE_SIGHTINGS, null, cv);
	    db.close();
    }

    // ==================================================================
    public int updateSightingTaxon(long sighting_id, long tsn, String title) {

	    SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(KEY_TSN, tsn);
        cv.put(KEY_SIGHTING_TITLE, title);
	    int updates = db.update(TABLE_SIGHTINGS, cv, KEY_ROWID + " = ?", new String[] {Long.toString(sighting_id)});

	    db.close();
	    
	    return updates;
    }

    // ==================================================================
    public int removeSighting(long sighting_id) {

	    SQLiteDatabase db = getWritableDatabase();
	    int deletions = db.delete(TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS, KEY_SIGHTING_ID + " = ?", new String[] {Long.toString(sighting_id)});
	    Log.d(TAG, "Removed " + deletions + " photos.");
	    deletions = db.delete(TABLE_SIGHTINGS, KEY_ROWID + " = ?", new String[] {Long.toString(sighting_id)});
	    Log.d(TAG, "Removed " + deletions + " sightings.");
	    db.close();
	    return deletions;
    }
    
    // ==================================================================
    public Date getEarliestSightingDate() {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_SIGHTINGS,
	    		new String[] {"strftime('%s', " + KEY_TIMESTAMP + ") AS " + KEY_TIMESTAMP},
	    		null,
	    		null,
	    		null, null, KEY_TIMESTAMP + " ASC LIMIT 1");
	    
	    Date earliest = null;
	    
	    if (c.moveToFirst()) {
		    long last_timestamp = c.getLong(0);
		    earliest = new Date(last_timestamp * 1000);
	    }
	    
		c.close();
	    db.close();
	    
	    return earliest;
    }

    // ==================================================================
    public int count_photo_associations() {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS,
	    		new String[] {"COUNT(DISTINCT " + KEY_SIGHTING_ID + ")"},
	    		null,
	    		null,
	    		null, null, null);
	    
	    c.moveToFirst();
	    int unique_count = c.getInt(0);
	    
	    c.close();
	    db.close();

	    return unique_count;
    }
    
    // ==================================================================
    public Cursor sightingsProvider(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(VIEW_EVENTS_PROVIDER,
	    		projection,
	    		selection,
	    		selectionArgs,
	    		null, null,
	    		sortOrder);
	    
	    Log.d(TAG,"Number of rows retrieved: " + c.getCount());

	    return c;
    }
    
    // ==================================================================
    public Cursor list_sightings() {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_SIGHTINGS,
	    		new String[] {KEY_ROWID, KEY_TSN, KEY_LAT, KEY_LON, KEY_ACCURACY, "strftime('%s', " + KEY_TIMESTAMP + ") AS " + KEY_TIMESTAMP},
	    		null,
	    		null,
	    		null, null, KEY_TIMESTAMP + " DESC");
	    
	    Log.d(TAG, "Sighting ids:");
	    while (c.moveToNext())
	    	Log.i(TAG, "sighting id: " + c.getLong(0));
	    
	    c.moveToFirst();
	    
	    db.close();
	    
	    return c;
    }
    
    // ==================================================================
    public Cursor getSightingPhotos(long sighting_id) {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS,
	    		new String[] {IMPLICIT_ROWID + " AS " + KEY_ROWID, KEY_IMAGE_URI},
	    		KEY_SIGHTING_ID + " = ?",
	    		new String[] {Long.toString(sighting_id)},
	    		null, null, null);
	    
	    Log.d(TAG, "Photos for sighting " + sighting_id + ":");
	    while (c.moveToNext())
	    	Log.i(TAG, "photo_id: " + c.getLong(1));
	    
	    c.moveToFirst();
	    
	    db.close();
	    
	    return c;
    }
    

    // ==================================================================
    public int unassociateSightingPhoto(long rowid) {

//    	Log.d(TAG, "Trying to disassociate photo " + photo_id + " from sighting " + sighting_id);
    	
	    SQLiteDatabase db = getWritableDatabase();
	    int deletion_count = db.delete(TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS, IMPLICIT_ROWID + " = ?", new String[] {Long.toString(rowid)});
	    db.close();
	    return deletion_count;
    }
    

    // ==================================================================
    public long associateSightingPhoto(long sighting_id, Uri photo_uri) {

	    SQLiteDatabase db = getWritableDatabase();
	    
	    ContentValues cv = new ContentValues();
    	cv.put(KEY_SIGHTING_ID, sighting_id);
	    cv.put(KEY_IMAGE_URI, photo_uri.toString());

    	long result = db.insert(TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS, null, cv);

	    db.close();
	    return result;
    }
    

    // ==================================================================
    @Override
    public void onCreate(SQLiteDatabase db)  {
    	for (String sql : table_creation_commands)
        	db.execSQL( sql );
    }

    // ==================================================================
    public void drop_all_tables(SQLiteDatabase db) 
    {
		for (String view : view_list)
			db.execSQL("DROP VIEW IF EXISTS " + view);

		for (String table : table_list)
			db.execSQL("DROP TABLE IF EXISTS " + table);
    }

    // ==================================================================
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, 
    int newVersion) 
    {
        Log.w(TAG, "Upgrading database from version " + oldVersion 
                + " to "
                + newVersion + ", which will destroy all old data");

        drop_all_tables(db);
        
        onCreate(db);
    }
}

