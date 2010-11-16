package com.kostmo.flickr.data;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.aetrion.flickr.tags.Tag;
import com.kostmo.flickr.activity.TabbedSearchActivity;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.containers.MachineTag;

public class DatabaseSearchHistory extends SQLiteOpenHelper {

	static final String TAG = Market.DEBUG_TAG; 

    static final int DATABASE_VERSION = 4;
    static final String DATABASE_NAME = "SEARCH_HISTORY";

    public static final String TABLE_SEARCH_HISTORY = "TABLE_SEARCH_HISTORY";
    public static final String TABLE_MACHINE_TAG_TRIPLES = "TABLE_MACHINE_TAG_TRIPLES";
    public static final String TABLE_STANDARD_TAGS = "TABLE_STANDARD_TAGS";


    public static final String KEY_NAMESPACE = "KEY_NAMESPACE";
    public static final String KEY_PREDICATE = "KEY_PREDICATE";
    public static final String KEY_VALUE = "KEY_VALUE";
    
    public static final String KEY_SEARCH_TAGSET = "KEY_SEARCH_TAGSET";
    public static final String KEY_SEARCH_TEXT = "KEY_SEARCH_TEXT";
    public static final String KEY_GROUP_ID = "KEY_GROUP_ID";
    public static final String KEY_GROUP_NAME = "KEY_GROUP_NAME";
    public static final String KEY_USER_ID = "KEY_USER_ID";
    public static final String KEY_USER_NAME = "KEY_USER_NAME";
    public static final String KEY_TAG_COUNT = "KEY_TAG_COUNT";
    
    
    public static final String KEY_HIT_COUNT = "KEY_HIT_COUNT";
    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    

    public static final long UPLOADED_TAG_KEY = -2;
    public static final long PREVIOUS_SEARCH_ID = -1;



    final static String SQL_CREATE_STANDARD_TAG_TABLE =
        "create table " + TABLE_STANDARD_TAGS + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_SEARCH_TAGSET + " integer not null, "
        + KEY_VALUE + " text);";
    
    final static String SQL_CREATE_MACHINE_TAG_TRIPLE_TABLE =
        "create table " + TABLE_MACHINE_TAG_TRIPLES + " (" 
        + KEY_SEARCH_TAGSET + " integer not null, "
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_NAMESPACE + " text, "
        + KEY_PREDICATE + " text, "
        + KEY_VALUE + " text);";
    
    final static String SQL_CREATE_SEARCH_HISTORY_TABLE =
        "create table " + TABLE_SEARCH_HISTORY + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_SEARCH_TEXT + " text, "
        + KEY_GROUP_ID + " text, "
        + KEY_GROUP_NAME + " text, "
        + KEY_USER_ID + " text, "
        + KEY_USER_NAME + " text, "
        + KEY_TAG_COUNT + " integer, "
        + KEY_HIT_COUNT + " integer, "
        + KEY_TIMESTAMP + " TIMESTAMP default CURRENT_TIMESTAMP);";	// NOTE: This is only updated when inserting!!!
    
    final static String[] table_list = {TABLE_SEARCH_HISTORY, TABLE_MACHINE_TAG_TRIPLES, TABLE_STANDARD_TAGS};
    final static String[] table_creation_commands = {SQL_CREATE_MACHINE_TAG_TRIPLE_TABLE, SQL_CREATE_STANDARD_TAG_TABLE, SQL_CREATE_SEARCH_HISTORY_TABLE};

    // ============================================================
    
    public DatabaseSearchHistory(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }
    
    // ============================================================

    public void clear_history() {

	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
	    for (String table : table_list)
	    	db.delete(table, null, null);

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    Log.d(TAG, "Hisotry wiped.");
    }
    // ============================================================

    public Cursor retrieveStandardTags(String like_pattern, boolean uploads) {

	    SQLiteDatabase db = getReadableDatabase();

	    String component = KEY_VALUE;
	    Cursor c = db.query(TABLE_STANDARD_TAGS,
	    		new String[] {BaseColumns._ID, component},
	    		KEY_SEARCH_TAGSET + (uploads ? "" : "!") + "=? AND " + component + " LIKE ?",
	    		new String[] {Long.toString(UPLOADED_TAG_KEY), like_pattern},
	    		component, null, null);
        
        Log.d(TAG, "Row count: " + c.getCount());
        
        db.close();

        return c;
    }
    // ============================================================

    public Cursor retrieveMachineTags(String like_pattern, String component, boolean uploads) {

	    SQLiteDatabase db = getReadableDatabase();

	    Cursor c = db.query(TABLE_MACHINE_TAG_TRIPLES,
	    		new String[] {BaseColumns._ID, component},
	    		KEY_SEARCH_TAGSET + (uploads ? "" : "!") + "=? AND " + component + " LIKE ?",
	    		new String[] {Long.toString(UPLOADED_TAG_KEY), like_pattern},
	    		component, null, null);
        
        Log.d(TAG, "Row count: " + c.getCount());
        
        db.close();
        
        return c;
    }
    // ============================================================

    public void saveUploadTags(
    		List<Tag> standard_tags,
    		List<MachineTag> machine_tags) {

	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		save_tags(db, UPLOADED_TAG_KEY, standard_tags, machine_tags);

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
    }
    
    // ============================================================

    public void save_last_tags(
    		List<Tag> standard_tags,
    		List<MachineTag> machine_tags) {

	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();


        
        // Purge the old tags first
        for (String table : new String[] {TABLE_STANDARD_TAGS, TABLE_MACHINE_TAG_TRIPLES})
        	db.delete(table, KEY_SEARCH_TAGSET + "=?", new String[] {Long.toString(PREVIOUS_SEARCH_ID)});
		
        save_tags(db, PREVIOUS_SEARCH_ID, standard_tags, machine_tags);

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
    }

    // ============================================================

    public static void save_tags(
    		SQLiteDatabase db,
    		long search_id,
    		List<Tag> standard_tags,
    		List<MachineTag> machine_tags) {
    	
	    for (Tag tag : standard_tags) {
            ContentValues c1 = new ContentValues();
            c1.put(KEY_SEARCH_TAGSET, search_id);
            c1.put(KEY_VALUE, tag.getRaw());
            db.insert(TABLE_STANDARD_TAGS, null, c1);
	    }

	    for (MachineTag mt : machine_tags) {
            ContentValues c1 = new ContentValues();
            c1.put(KEY_SEARCH_TAGSET, search_id);
            c1.put(KEY_NAMESPACE, mt.namespace);
            c1.put(KEY_PREDICATE, mt.predicate);
            c1.put(KEY_VALUE, mt.value);
            db.insert(TABLE_MACHINE_TAG_TRIPLES, null, c1);
	    }
    }
    // ============================================================

    public static void save_stringified_tags(
    		SQLiteDatabase db,
    		long search_id,
    		List<String> standard_tags,
    		List<String> machine_tags) {
    	
	    for (String string : standard_tags) {
            ContentValues c1 = new ContentValues();
            c1.put(KEY_SEARCH_TAGSET, search_id);
            c1.put(KEY_VALUE, string);
            db.insert(TABLE_STANDARD_TAGS, null, c1);
	    }

	    for (String string : machine_tags) {

			Log.e(TAG, "Saving machine tag to database: " + string);
	    	
	    	
	    	MachineTag mt = new MachineTag(string);
            ContentValues c1 = new ContentValues();
            c1.put(KEY_SEARCH_TAGSET, search_id);
            c1.put(KEY_NAMESPACE, mt.namespace);
            c1.put(KEY_PREDICATE, mt.predicate);
            c1.put(KEY_VALUE, mt.value);
            db.insert(TABLE_MACHINE_TAG_TRIPLES, null, c1);
	    }
    }

    // ============================================================

    public void save_search(Intent search_intent, long hits) {

	    SQLiteDatabase db = getWritableDatabase();
	    
    	
		String search_text = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SEARCH_TEXT);
		

		
		
		List<String> standard_tags = search_intent.getStringArrayListExtra(TabbedSearchActivity.INTENT_EXTRA_STANDARD_TAGS);
//		boolean machine_tag_all_mode = search_intent.getBooleanExtra(TabSearchActivity.INTENT_EXTRA_MACHINE_TAG_ALL_MODE, true);
		List<String> machine_tags = search_intent.getStringArrayListExtra(TabbedSearchActivity.INTENT_EXTRA_MACHINE_TAGS);

		String user_id = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_USER_ID);
		String group_id = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_GROUP_ID);

		String user_name = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_USER_NAME);
		String group_name = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_GROUP_NAME);

		db.beginTransaction();
		
        ContentValues c = new ContentValues();
        c.put(KEY_SEARCH_TEXT, search_text);
        c.put(KEY_GROUP_NAME, group_name);
        c.put(KEY_GROUP_ID, group_id);
        c.put(KEY_USER_NAME, user_name);
        c.put(KEY_USER_ID, user_id);
        c.put(KEY_HIT_COUNT, hits);
        c.put(KEY_TAG_COUNT, standard_tags.size() + machine_tags.size());
        
        
        long search_id = db.insert(TABLE_SEARCH_HISTORY, null, c);
		
        save_stringified_tags(db, search_id, standard_tags, machine_tags);

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    Log.d(TAG, "Search params saved.");
    }

    // ============================================================
    
    public Cursor getSearchHistory() {
    	
	    SQLiteDatabase db = getReadableDatabase();

	    Cursor c = db.query(TABLE_SEARCH_HISTORY,
	    		new String[] {BaseColumns._ID, KEY_SEARCH_TEXT, KEY_GROUP_NAME, KEY_USER_NAME, KEY_TAG_COUNT, KEY_HIT_COUNT,
	    			"strftime('%s', " + KEY_TIMESTAMP + ") AS " + KEY_TIMESTAMP},
	    		null, null, null, null, KEY_TIMESTAMP + " DESC");
        
        Log.d(TAG, "Row count: " + c.getCount());
        
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
    }
    
    // ============================================================
    
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, 
    int newVersion) 
    {
        Log.w(TAG, "Upgrading database from version " + oldVersion 
                + " to "
                + newVersion + ", which will destroy all old data");

        if (false && oldVersion == 4 && newVersion == 5) {
        	// Save taglist here?
        	// Restore taglist here?
        } else {
	        
	        drop_all_tables(db);
	        
	        onCreate(db);
        }
    }
    
}
