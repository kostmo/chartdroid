package com.kostmo.commute;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseCommutes extends SQLiteOpenHelper {
	
	static final String TAG = "DatabaseCommutes"; 
 

    static final String DATABASE_NAME = "COMMUTES";
    static final int DATABASE_VERSION = 1;

    public static final String TABLE_DESTINATIONS = "TABLE_DESTINATION_PAIRS";
    public static final String TABLE_DESTINATION_PAIRS = "TABLE_DESTINATION_PAIRS";
    public static final String TABLE_TRIPS = "TABLE_TRIPS";
    public static final String TABLE_TRIP_BREADCRUMBS = "TABLE_TRIP_BREADCRUMBS";
    

    public static final String KEY_DESTINATION_ID = "KEY_DESTINATION_ID";
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
    
    

    final static String SQL_CREATE_DESTINATIONS_TABLE =
        "create table " + TABLE_DESTINATIONS + " ("
        + KEY_DESTINATION_ID + " integer primary key autoincrement, "
        + KEY_LATITUDE + " float, "
        + KEY_LONGITUDE + " float);";

    final static String SQL_CREATE_DESTINATION_PAIRS_TABLE =
        "create table " + TABLE_DESTINATION_PAIRS + " ("
        + KEY_DESTINATION_PAIR_ID + " integer autoincrement, "
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
   

    final static String SQL_CREATE_TRIP_BREADCRUBS_TABLE =
        "create table " + TABLE_TRIP_BREADCRUMBS + " ("
        + KEY_BREADCRUMB_ID + " integer primary key autoincrement, "
        + KEY_TRIP_ID + " integer, "
        + KEY_LATITUDE + " float, "
        + KEY_LONGITUDE + " float);";
    
    final static String[] table_list = {
    	TABLE_DESTINATIONS,
    	TABLE_DESTINATION_PAIRS,
    	TABLE_TRIPS,
    	TABLE_TRIP_BREADCRUMBS
    };

    final static String[] table_creation_commands = {
    	SQL_CREATE_DESTINATIONS_TABLE,
    	SQL_CREATE_DESTINATION_PAIRS_TABLE,
    	SQL_CREATE_TRIPS_TABLE,
    	SQL_CREATE_TRIP_BREADCRUBS_TABLE
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
    public Cursor getDestinationPairs() {
    	
    	SQLiteDatabase db = getReadableDatabase();
    	Cursor cursor = db.query(TABLE_DESTINATION_PAIRS, null, null, null, null, null, null);
    	cursor.moveToFirst();
	    db.close();
	    
	    return cursor;
    }

    
    // ============================================================
    public long storeDestination(float lat, float lon) {
    	
    	SQLiteDatabase db = getWritableDatabase();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(KEY_LATITUDE, lat);
    	cv.put(KEY_LONGITUDE, lon);

    	long destination_id = db.insert(TABLE_DESTINATIONS, null, cv);

	    db.close();
	    
	    return destination_id;
    }
    
    // ============================================================
    
    public void drop_all_tables(SQLiteDatabase db) 
    {
    	for (String table : table_list)
    		db.execSQL("DROP TABLE IF EXISTS " + table);
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
