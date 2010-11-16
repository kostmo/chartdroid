package org.crittr.track;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

public class DatabaseSightings extends SQLiteOpenHelper 
{
	public static final String TAG = Market.DEBUG_TAG;
	
	static final String DATABASE_NAME = "SIGHTINGS_DATA";
    static final int DATABASE_VERSION = 5;
    
    
	public static final long INVALID_TSN = -1;
	public static final long UNKNOWN_PARENT_ID = -1;
	public static final long NO_PARENT_ID = 0;
	public static final long ORPHAN_PARENT_ID = NO_PARENT_ID;
	public static final int INVALID_RANK_ID = -1;
	public static final int UNKNOWN_ITIS_KINGDOM = -1;

    public static final String IMPLICIT_ROWID = "ROWID";

    public static final String TABLE_SIGHTINGS = "TABLE_SIGHTINGS";
    public static final String TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS = "TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS";

    public static final String KEY_ROWID = "_id";
    public static final String KEY_TSN = "KEY_TSN";
    public static final String KEY_USE_COUNT = "KEY_USE_COUNT";


    public static final String KEY_LAT = "KEY_LAT";
    public static final String KEY_LON = "KEY_LON";
    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    public static final String KEY_ACCURACY = "KEY_ACCURACY";
    
    public static final String KEY_SIGHTING_ID = "KEY_SIGHTING_ID";
    public static final String KEY_IMAGE_URI = "KEY_FLICKR_PHOTO_ID";
    

    public static final String KEY_THUMBNAIL_URL = "KEY_THUMBNAIL_URL";
    public static final String KEY_LOCAL_THUMBNAIL_PATH = "KEY_LOCAL_THUMBNAIL_PATH";
    
    
  
    static final String SQL_CREATE_SIGHTINGS_TABLE =
        "create table " + TABLE_SIGHTINGS + " (" 
        + KEY_ROWID + " integer primary key autoincrement, "
        + KEY_TSN + " integer default " + INVALID_TSN + ", "
        + KEY_LAT + " float, "
        + KEY_LON + " float, " 
        + KEY_ACCURACY + " float,"
        + KEY_TIMESTAMP + " TIMESTAMP NULL default CURRENT_TIMESTAMP);";
    
    static final String SQL_CREATE_PHOTOGRAPH_ASSOCIATIONS_TABLE =
        "create table " + TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS + " ("
        + KEY_SIGHTING_ID + " integer, "
        + KEY_IMAGE_URI + " text,"
        + "PRIMARY KEY(" + KEY_SIGHTING_ID + ", " + KEY_IMAGE_URI + ") ON CONFLICT IGNORE);";
   
    
    
    
    final static String[] table_list = {
    	TABLE_SIGHTINGS,
    	TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS};
    
    final static String[] table_creation_commands = {
    	SQL_CREATE_SIGHTINGS_TABLE,
    	SQL_CREATE_PHOTOGRAPH_ASSOCIATIONS_TABLE};
    
    

    Context context;
    
    // ==================================================================
    public void wipe_sightings_table() {

    	SQLiteDatabase db = getWritableDatabase();
	    
		db.beginTransaction();
		
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SIGHTINGS);
    	db.execSQL( SQL_CREATE_SIGHTINGS_TABLE );
    	
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS);
    	db.execSQL( SQL_CREATE_PHOTOGRAPH_ASSOCIATIONS_TABLE );
    	
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    
    	db.close();
    }
    

    // ==================================================================
    public DatabaseSightings(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        
        this.context = context;
    }
    
    // ==================================================================
    public void record_sighting(long tsn) {
    	
	    SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(KEY_TSN, tsn);
		
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
    public int update_sighting(long sighting_id, long tsn) {

	    SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(KEY_TSN, tsn);

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
    public Cursor list_sightings_2(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_SIGHTINGS,
	    		projection,
//	    		new String[] {KEY_ROWID, KEY_TSN, KEY_LAT, KEY_LON, KEY_ACCURACY, "strftime('%s', " + KEY_TIMESTAMP + ") AS " + KEY_TIMESTAMP},
	    		selection,
	    		selectionArgs,
	    		null, null,
	    		sortOrder);
//	    		KEY_TIMESTAMP + " DESC");

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
    public void onCreate(SQLiteDatabase db) 
    {
    	for (String sql : table_creation_commands)
        	db.execSQL( sql );
    }
    


    // ==================================================================
    public void drop_all_tables(SQLiteDatabase db) 
    {
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

