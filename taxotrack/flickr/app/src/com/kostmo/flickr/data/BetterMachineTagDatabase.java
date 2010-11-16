package com.kostmo.flickr.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.kostmo.flickr.containers.MachineTag;

public class BetterMachineTagDatabase extends SQLiteOpenHelper {
	
	static final String TAG = "BetterMachineTagDatabase"; 

    static final int DATABASE_VERSION = 4;
    static final String DATABASE_NAME = "MACHINE_TAG_DATA";


    public static final String TABLE_CHECKIN_TIMES = "TABLE_CHECKIN_TIMES";
    
    
    public static final String TABLE_CONVENTIONS = "TABLE_CONVENTIONS";
    public static final String TABLE_NAMESPACES = "TABLE_NAMESPACES";
    public static final String TABLE_PREDICATES = "TABLE_PREDICATES";
    public static final String TABLE_VALUES = "TABLE_VALUES";
    public static final String TABLE_IMAGE_BLACKLIST = "TABLE_IMAGE_BLACKLIST";
	
    public static final String VIEW_TRIPLES = "VIEW_TRIPLES";
    public static final String VIEW_DUPLES = "VIEW_DUPLES";
    public static final String VIEW_SINGLES = "VIEW_SINGLES";
    
    
    // View keys:
    public static final String KEY_ROLE = "KEY_ROLE";
    public static final String KEY_NAMESPACE = "KEY_NAMESPACE";
    public static final String KEY_PREDICATE = "KEY_PREDICATE";
    public static final String KEY_VALUE = "KEY_VALUE";

    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    public static final String KEY_LAST_EVENT_PHOTO_ID = "KEY_LAST_EVENT_PHOTO_ID";
    
    public static final String KEY_IMAGE_TITLE = "KEY_IMAGE_TITLE";


    public static final String KEY_TITLE = "KEY_TITLE";
    public static final String KEY_COLOR = "KEY_COLOR";
    public static final String KEY_URI = "KEY_URI";
    public static final String KEY_PARENT = "KEY_PARENT";
    public static final String KEY_CONVENTION_ID = "KEY_CONVENTION_ID";
//    public static final String KEY_ROLE = "KEY_ROLE";	// i.e. Namespace, Predicate, or Value
    
    
    
    
    static final String SQL_CREATE_CHECKIN_TIMES_TABLE =
        "create table " + TABLE_CHECKIN_TIMES + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_LAST_EVENT_PHOTO_ID + " text, "
        + KEY_TIMESTAMP + " TIMESTAMP default CURRENT_TIMESTAMP);";	// NOTE: This is only updated when inserting!!!
    
    
    
    final static String SQL_CREATE_IMAGE_BLACKLIST_TABLE =
        "create table " + TABLE_IMAGE_BLACKLIST + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_IMAGE_TITLE + " text not null);";
    
    final static String SQL_CREATE_CONVENTIONS_TABLE =
        "create table " + TABLE_CONVENTIONS + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_URI + " text not null);";
    
    final static String SQL_CREATE_NAMESPACES_TABLE =
        "create table " + TABLE_NAMESPACES + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_CONVENTION_ID + " integer not null, "
        + KEY_TITLE + " text not null);";
    
    final static String SQL_CREATE_PREDICATES_TABLE =
        "create table " + TABLE_PREDICATES + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_CONVENTION_ID + " integer not null, "
        + KEY_TITLE + " text not null, "
        + KEY_PARENT + " integer not null);";
    
    final static String SQL_CREATE_VALUES_TABLE =
        "create table " + TABLE_VALUES + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_CONVENTION_ID + " integer not null, "
        + KEY_TITLE + " text not null, "
        + KEY_COLOR + " text, "
        + KEY_PARENT + " integer not null);";
    
    
    final static String SQL_CREATE_SINGLES_VIEW =
        "create view " + VIEW_SINGLES + " AS SELECT "
        + "0 AS " + KEY_ROLE + ", "
        + "A1." + BetterMachineTagDatabase.KEY_TITLE
        + " AS " + KEY_NAMESPACE + ", "
        + "null AS " + KEY_PREDICATE + ", "
        + "null AS " + KEY_VALUE
        + " FROM "
        + BetterMachineTagDatabase.TABLE_NAMESPACES + " A1";
    
    
    final static String SQL_CREATE_DUPLES_VIEW =
        "create view " + VIEW_DUPLES + " AS SELECT "
        + "1 AS " + KEY_ROLE + ", "
        + "A1." + BetterMachineTagDatabase.KEY_TITLE
        + " AS " + KEY_NAMESPACE + ", "
        + "A2." + BetterMachineTagDatabase.KEY_TITLE
        + " AS " + KEY_PREDICATE + ", "
        + "null AS " + KEY_VALUE
        + " FROM "
        + BetterMachineTagDatabase.TABLE_NAMESPACES + " A1, "
        + BetterMachineTagDatabase.TABLE_PREDICATES + " A2"
        + " WHERE "
        + "A1." + BaseColumns._ID + " = "
        + "A2." + BetterMachineTagDatabase.KEY_PARENT;
    
    
    final static String SQL_CREATE_TRIPLES_VIEW =
        "create view " + VIEW_TRIPLES + " AS SELECT "
        + "2 AS " + KEY_ROLE + ", "
        + "A1." + BetterMachineTagDatabase.KEY_TITLE
        + " AS " + KEY_NAMESPACE + ", "
        + "A2." + BetterMachineTagDatabase.KEY_TITLE
        + " AS " + KEY_PREDICATE + ", "
        + "A3." + BetterMachineTagDatabase.KEY_TITLE
        + " AS " + KEY_VALUE
        + " FROM "
        + BetterMachineTagDatabase.TABLE_NAMESPACES + " A1, "
        + BetterMachineTagDatabase.TABLE_PREDICATES + " A2, "
        + BetterMachineTagDatabase.TABLE_VALUES + " A3"
        + " WHERE "
        + "A1." + BaseColumns._ID + " = "
        + "A2." + BetterMachineTagDatabase.KEY_PARENT
        + " AND "
        + "A2." + BaseColumns._ID + " = "
        + "A3." + BetterMachineTagDatabase.KEY_PARENT;
    
    
    final static String[] table_list = {TABLE_CHECKIN_TIMES, TABLE_IMAGE_BLACKLIST, TABLE_CONVENTIONS, TABLE_NAMESPACES, TABLE_PREDICATES, TABLE_VALUES};
    final static String[] view_list = {VIEW_SINGLES, VIEW_DUPLES, VIEW_TRIPLES};

    final static String[] table_creation_commands = {SQL_CREATE_CHECKIN_TIMES_TABLE, SQL_CREATE_IMAGE_BLACKLIST_TABLE, SQL_CREATE_CONVENTIONS_TABLE, SQL_CREATE_NAMESPACES_TABLE, SQL_CREATE_PREDICATES_TABLE, SQL_CREATE_VALUES_TABLE, SQL_CREATE_SINGLES_VIEW, SQL_CREATE_DUPLES_VIEW, SQL_CREATE_TRIPLES_VIEW};

    // ============================================================
    
    public BetterMachineTagDatabase(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }
    
    
    // ============================================================

    public void importBlacklist(List<String> blacklist) {

	    SQLiteDatabase db = getWritableDatabase();

		db.beginTransaction();
	    for (String string : blacklist) {
	    	
            ContentValues c = new ContentValues();
            c.put(KEY_IMAGE_TITLE, string);
            db.insert(TABLE_IMAGE_BLACKLIST, null, c);
	    }


	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    Log.d(TAG, "Image blacklist imported.");
    }
    
    // ============================================================

    public void addToBlacklist(String image_title) {

	    SQLiteDatabase db = getWritableDatabase();

        ContentValues c = new ContentValues();
        c.put(KEY_IMAGE_TITLE, image_title);
        db.insert(TABLE_IMAGE_BLACKLIST, null, c);

	    db.close();
	    
	    Log.d(TAG, "Blacklisted " + image_title);
    }
    
    // ============================================================

    public List<String> getBlacklist() {

    	
    	List<String> blacklist = new ArrayList<String>();
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_IMAGE_BLACKLIST,
	    		new String[] {KEY_IMAGE_TITLE}, null, null,
	    		null, null, null);

	    while (c.moveToNext())
	    	blacklist.add( c.getString(0) );

	    c.close();
	    db.close();

        return blacklist;
    }
    // ============================================================

    public boolean isBlacklistDownloaded() {

    	SQLiteDatabase q = getReadableDatabase();

    	String sql = "SELECT " + BaseColumns._ID + " FROM " + TABLE_IMAGE_BLACKLIST + " LIMIT 1";
        Cursor c = q.rawQuery(sql, null);
        c.moveToFirst();
        q.close();
        
        int exists = c.getCount();
        c.close();

        return exists > 0;
    }

    // ============================================================

    public boolean isTagConventionDownloaded() {

    	SQLiteDatabase q = getReadableDatabase();



    	String sql = "SELECT " + BaseColumns._ID + " FROM " + BetterMachineTagDatabase.TABLE_CONVENTIONS + " LIMIT 1";
        Cursor c = q.rawQuery(sql, null);
        c.moveToFirst();
        q.close();

        int exists = c.getCount();
        c.close();

        return exists > 0;
    }
    
    // ============================================================
    
    public String getAppropriateTagColor(MachineTag parts) {
    	
	    SQLiteDatabase db = getReadableDatabase();


        String sql = "SELECT "
            + "A1." + BetterMachineTagDatabase.KEY_COLOR
            + " AS " + BetterMachineTagDatabase.KEY_COLOR
            + " FROM "
            + BetterMachineTagDatabase.TABLE_VALUES + " A1, "
            + BetterMachineTagDatabase.TABLE_PREDICATES + " A2"
            + " WHERE "
            + "A2." + BaseColumns._ID + " = "
            + "A1." + BetterMachineTagDatabase.KEY_PARENT
            + " AND "
            + "A1." + BetterMachineTagDatabase.KEY_TITLE + " = ?"
            + " AND "
            + "A2." + BetterMachineTagDatabase.KEY_TITLE + " = ?";

        Cursor c = db.rawQuery(sql, new String[] {parts.value, parts.predicate});
        String color = null;
        if (c.getCount() > 0) {
        	c.moveToFirst();
        	color = c.getString(0);
        }
        
        db.close();
        c.close();
        
        return color;
    }
    
    // ============================================================

    public String[] getTagValueOptions(MachineTag parts) {
    	
	    SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT "
            + "A1." + BetterMachineTagDatabase.KEY_TITLE
            + " AS " + BetterMachineTagDatabase.KEY_TITLE
            + " FROM "
            + BetterMachineTagDatabase.TABLE_VALUES + " A1, "
            + BetterMachineTagDatabase.TABLE_PREDICATES + " A2"
            + " WHERE "
            + "A2." + BaseColumns._ID + " = "
            + "A1." + BetterMachineTagDatabase.KEY_PARENT
            + " AND "
            + "A2." + BetterMachineTagDatabase.KEY_TITLE + " = ?";

        Cursor c = db.rawQuery(sql, new String[] {parts.predicate});
		
        String[] tag_value_options = new String[c.getCount()];
        int i=0;
        while (c.moveToNext()) {
        	tag_value_options[i] = c.getString(0);
        	i++;
        }
        db.close();
        c.close();
        
        return tag_value_options;
    }

    // ============================================================
    
    public Date getLastCheckinTime() {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    
		// NOTE: This "AS" trick actually works (and is quite necessary)!
	    Cursor c = db.query(TABLE_CHECKIN_TIMES,
	    		new String[] {"strftime('%s', " + KEY_TIMESTAMP + ") AS " + KEY_TIMESTAMP},
	    		null, null,
	    		null, null, KEY_TIMESTAMP + " DESC LIMIT 1");	// TODO: Does the LIMIT clause work here?

	    Date last_checkin;
	    if (c.getCount() > 0) {
		    c.moveToFirst();
		    long last_timestamp = c.getLong(0);
		    last_checkin = new Date(last_timestamp * 1000);
		    

	    	Log.e(TAG, "Found a checkin time in the database!");
	    	
	    } else {
	    	
	    	Log.e(TAG, "There were no checkin times in the database.");
	    	
	    	last_checkin = new Date();
	    }
	    c.close();
	    db.close();
	    
	    return last_checkin;
    }
    
    // ============================================================
    
    public void updateLastCheckinTime(long last_event_photo_id) {
    	
	    SQLiteDatabase db = getWritableDatabase();


	    db.execSQL("REPLACE INTO " + TABLE_CHECKIN_TIMES + " VALUES(1, " + last_event_photo_id + ", datetime()) ");

	    
	    /*	    
	    // We use this nice "UPDATE OR INSERT" idiom.
    
	    ContentValues cv = new ContentValues();
	    cv.put(KEY_LAST_EVENT_PHOTO_ID, last_event_photo_id);
	    
	    // NOTE: It seems that the timestamp is not updated automatically
	    // by the database.  Therefore, we must generate the new timestamp manually.
	    cv.put(KEY_TIMESTAMP, new Date().getTime());
//	    cv.putNull(KEY_TIMESTAMP);	// This fails...
	    
	    int updates = db.update(TABLE_CHECKIN_TIMES, cv, null, null);
	    
	    Log.d(TAG, "Timestamp update count: " + updates);
	    if (updates == 0) {
	    	Log.e(TAG, "Did not update, need to insert a new checkin time.");
	    	db.insert(TABLE_CHECKIN_TIMES, null, cv);
	    }
	    */
	    
	    db.close();
    }
    
    
    // ============================================================
    
    public Cursor getTagPossibilityCursor() {
    	
	    SQLiteDatabase db = getReadableDatabase();

//	    String sql = "SELECT 1 as " + KEY_ROWID + ", * FROM " + VIEW_TRIPLES + " JOIN " + VIEW_DUPLES;
	    String sql = "SELECT 1 as " + BaseColumns._ID + ", * FROM " + VIEW_TRIPLES
	    + " UNION "
	    + "SELECT 1 as " + BaseColumns._ID + ", * FROM " + VIEW_DUPLES
	    + " UNION "
	    + "SELECT 1 as " + BaseColumns._ID + ", * FROM " + VIEW_SINGLES
	    + " ORDER BY KEY_NAMESPACE, KEY_PREDICATE, KEY_ROLE, KEY_VALUE";
//        Log.d(TAG, sql);
        
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        
//        Log.d(TAG, "Row count: " + c.getCount());
        
        db.close();
        
        return c;
    }
    
    // ============================================================
    
    @Override
    public void onCreate(SQLiteDatabase db) 
    {
    	for (String sql : table_creation_commands)
        	db.execSQL( sql );
    }
    
    // ============================================================
    
    public void drop_all_tables(SQLiteDatabase db) 
    {
    	for (String table : table_list)
    		db.execSQL("DROP TABLE IF EXISTS " + table);
    	
    	for (String view : view_list)
    		db.execSQL("DROP VIEW IF EXISTS " + view);
    	
    }
    
    // ============================================================
    
    
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
