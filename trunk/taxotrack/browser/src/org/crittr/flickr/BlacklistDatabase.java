package org.crittr.flickr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BlacklistDatabase extends SQLiteOpenHelper 
{
	public static final String TAG = "BlacklistDatabase";

    static final int DATABASE_VERSION = 2;
    static final String DATABASE_NAME = "BLACKLIST_DATA";



    public static final String TABLE_IMAGE_BLACKLIST = "TABLE_IMAGE_BLACKLIST";

    
    
    // View keys:
    public static final String KEY_ROLE = "KEY_ROLE";
    public static final String KEY_NAMESPACE = "KEY_NAMESPACE";
    public static final String KEY_PREDICATE = "KEY_PREDICATE";
    public static final String KEY_VALUE = "KEY_VALUE";

    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    public static final String KEY_LAST_EVENT_PHOTO_ID = "KEY_LAST_EVENT_PHOTO_ID";
    
    public static final String KEY_IMAGE_TITLE = "KEY_IMAGE_TITLE";
    
    
    
    public static final String KEY_ROWID = BaseColumns._ID;

    public static final String KEY_TITLE = "KEY_TITLE";
    public static final String KEY_COLOR = "KEY_COLOR";
    public static final String KEY_URI = "KEY_URI";
    public static final String KEY_PARENT = "KEY_PARENT";
    public static final String KEY_CONVENTION_ID = "KEY_CONVENTION_ID";
//    public static final String KEY_ROLE = "KEY_ROLE";	// i.e. Namespace, Predicate, or Value
    
    

    
    final static String SQL_CREATE_IMAGE_BLACKLIST_TABLE =
        "create table " + TABLE_IMAGE_BLACKLIST + " (" 
        + KEY_ROWID + " integer primary key autoincrement, "
        + KEY_IMAGE_TITLE + " text not null);";


    
    
    final static String[] table_list = {TABLE_IMAGE_BLACKLIST};


    final static String[] table_creation_commands = {SQL_CREATE_IMAGE_BLACKLIST_TABLE};

    // ========================================================================
    public BlacklistDatabase(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    // ========================================================================
    public void importBlacklist(List<String> blacklist) {

	    SQLiteDatabase db = getWritableDatabase();

		db.beginTransaction();
	    for (String string : blacklist) {
	    	
            ContentValues c = new ContentValues();
            c.put(BlacklistDatabase.KEY_IMAGE_TITLE, string);
            db.insert(BlacklistDatabase.TABLE_IMAGE_BLACKLIST, null, c);
	    }


	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    Log.d(TAG, "Image blacklist imported.");
    }

    // ========================================================================
    public void addToBlacklist(String image_title) {

	    SQLiteDatabase db = getWritableDatabase();

        ContentValues c = new ContentValues();
        c.put(BlacklistDatabase.KEY_IMAGE_TITLE, image_title);
        db.insert(BlacklistDatabase.TABLE_IMAGE_BLACKLIST, null, c);

	    db.close();
	    
	    Log.d(TAG, "Blacklisted " + image_title);
    }

    // ========================================================================
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

    // ========================================================================
    public boolean isBlacklistDownloaded() {

    	SQLiteDatabase q = getReadableDatabase();

    	String sql = "SELECT " + KEY_ROWID + " FROM " + TABLE_IMAGE_BLACKLIST + " LIMIT 1";
        Cursor c = q.rawQuery(sql, null);
        c.moveToFirst();
        q.close();
        
        int exists = c.getCount();
        c.close();

        return exists > 0;
    }

    // ========================================================================
    @Override
    public void onCreate(SQLiteDatabase db) 
    {
    	for (String sql : table_creation_commands)
        	db.execSQL( sql );
    }

    // ========================================================================
    public void drop_all_tables(SQLiteDatabase db) 
    {
    	for (String table : table_list)
    		db.execSQL("DROP TABLE IF EXISTS " + table);
    	
    }

    // ========================================================================
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