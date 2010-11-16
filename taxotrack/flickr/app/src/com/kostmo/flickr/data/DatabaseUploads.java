package com.kostmo.flickr.data;

import java.util.ArrayList;
import java.util.Collection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import com.kostmo.flickr.activity.ListActivityPhotoTags;
import com.kostmo.flickr.activity.BatchUploaderActivity.ImageUploadData;
import com.kostmo.flickr.activity.BatchUploaderActivity.UploadStatus;
import com.kostmo.flickr.bettr.Market;

public class DatabaseUploads extends SQLiteOpenHelper  {
	
	static final String TAG = Market.DEBUG_TAG; 

    static final int DATABASE_VERSION = 9;
    static final String DATABASE_NAME = "UPLOADS";

    public static final String TABLE_UPLOADS = "TABLE_UPLOADS";

    public static final String KEY_UPLOAD_FILE_URI = "KEY_UPLOAD_FILE_URI";
    public static final String KEY_UPLOAD_TIMESTAMP = "KEY_UPLOAD_TIMESTAMP";
    public static final String KEY_UPLOAD_BATCH = "KEY_UPLOAD_BATCH";
    public static final String KEY_UPLOAD_TITLE = "KEY_UPLOAD_TITLE";
    public static final String KEY_UPLOAD_VISIBLE = "KEY_UPLOAD_VISIBLE";
    public static final String KEY_UPLOAD_DESCRIPTION = "KEY_UPLOAD_DESCRIPTION";
    public static final String KEY_UPLOAD_STATUS = "KEY_UPLOAD_STATUS";
    public static final String KEY_FLICKR_PHOTO_ID = "KEY_FLICKR_PHOTO_ID";
    

    public static final String TABLE_UNIQUE_TAGS = "TABLE_UNIQUE_TAGS";
    public static final String KEY_TAG_NAME = "KEY_TAG_NAME";

    public static final String TABLE_TAG_UPLOAD_ASSOCIATIONS = "TABLE_TAG_UPLOAD_ASSOCIATIONS";
    public static final String KEY_UPLOAD_ID = "KEY_UPLOAD_ID";
    public static final String KEY_TAG_ID = "KEY_TAG_ID";
    
    
    public static final String VIEW_NAMED_UPLOAD_TAGS = "VIEW_NAMED_UPLOAD_TAGS";
    public static final String VIEW_TAGGED_UPLOADS = "VIEW_TAGGED_UPLOADS";
    public static final String KEY_TAGS_LIST = "KEY_TAGS_LIST";
    
    
    public static final int FALSE = 0;
    public static final int TRUE = 1;
    
    public static final int DEFAULT_UPLOAD_VISIBILITY = TRUE;
    
    
    final static String SQL_CREATE_UNIQUE_TAGS_TABLE =
        "create table " + TABLE_UNIQUE_TAGS + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_TAG_NAME + " text);";
    
    final static String SQL_CREATE_TAG_UPLOAD_ASSOCIATIONS_TABLE =
        "create table " + TABLE_TAG_UPLOAD_ASSOCIATIONS + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_UPLOAD_ID + " integer, "
//      + KEY_TAG_ID + " integer);";
        + KEY_TAG_ID + " integer, "
        + "FOREIGN KEY (" + KEY_UPLOAD_ID + ") REFERENCES " + TABLE_UPLOADS + "(" + BaseColumns._ID + "), "
    	+ "FOREIGN KEY (" + KEY_TAG_ID + ") REFERENCES " + TABLE_UNIQUE_TAGS + "(" + BaseColumns._ID + ")" + ");";


    final static String SQL_CREATE_UPLOADS_TABLE =
        "create table " + TABLE_UPLOADS + " (" 
        + BaseColumns._ID + " integer primary key autoincrement, "
        + KEY_UPLOAD_TITLE + " text, "
        + KEY_UPLOAD_DESCRIPTION + " text, "
        + KEY_UPLOAD_FILE_URI + " text, "
        + KEY_UPLOAD_STATUS + " integer, "
        + KEY_UPLOAD_VISIBLE + " integer default " + DEFAULT_UPLOAD_VISIBILITY + " , "
        + KEY_FLICKR_PHOTO_ID + " integer default " + ListActivityPhotoTags.INVALID_PHOTO_ID + " , "
        + KEY_UPLOAD_BATCH + " integer, "
        + KEY_UPLOAD_TIMESTAMP + " TIMESTAMP default CURRENT_TIMESTAMP);";	// NOTE: This is only updated when inserting!!!
    
    
    final static String SQL_CREATE_NAMED_UPLOAD_TAGS_VIEW =
        "create view " + VIEW_NAMED_UPLOAD_TAGS + " AS " + buildNamedUploadTagsQuery();
    
    final static String SQL_CREATE_TAGGED_UPLOADS_VIEW =
        "create view " + VIEW_TAGGED_UPLOADS + " AS " + buildTaggedUploadsQuery();

    
    final static String[] table_list = {
    	TABLE_UPLOADS,
    	TABLE_UNIQUE_TAGS,
    	TABLE_TAG_UPLOAD_ASSOCIATIONS};
    
    final static String[] view_list = {
    	VIEW_NAMED_UPLOAD_TAGS,
    	VIEW_TAGGED_UPLOADS};
    
    final static String[] table_creation_commands = {
    	SQL_CREATE_UPLOADS_TABLE,
    	SQL_CREATE_UNIQUE_TAGS_TABLE,
    	SQL_CREATE_TAG_UPLOAD_ASSOCIATIONS_TABLE,
    	
    	SQL_CREATE_NAMED_UPLOAD_TAGS_VIEW,
    	SQL_CREATE_TAGGED_UPLOADS_VIEW};

    // ============================================================
    public DatabaseUploads(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    // ========================================================================
    private static String buildNamedUploadTagsQuery() {

    	SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
	    query_builder.setTables(
	    		TABLE_TAG_UPLOAD_ASSOCIATIONS + " AS A1" +
	    		" LEFT JOIN " +
	    		TABLE_UNIQUE_TAGS + " AS A2" +
	    		" ON (A1." + KEY_TAG_ID + " = " +
	    		"A2." + BaseColumns._ID + ")");
	    
	    return query_builder.buildQuery(
				new String[] {
			    		"A1." + KEY_UPLOAD_ID + " AS " + KEY_UPLOAD_ID,
			    		"A1." + KEY_TAG_ID + " AS " + KEY_TAG_ID,
			    		"A2." + KEY_TAG_NAME + " AS " + KEY_TAG_NAME
	    		}, 
	    		null, null, null, null, null, null);
    }
    
    // ========================================================================
    private static String buildTaggedUploadsQuery() {

    	SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
	    query_builder.setTables(
	    		TABLE_UPLOADS + " AS A1" +
	    		" LEFT JOIN " +
	    		VIEW_NAMED_UPLOAD_TAGS + " AS A2" +
	    		" ON (A1." + BaseColumns._ID + " = " +
	    		"A2." + KEY_UPLOAD_ID + ")");

	    return query_builder.buildQuery(
				new String[] {
						"A1." + BaseColumns._ID + " AS " + BaseColumns._ID,
				        KEY_UPLOAD_TITLE,
				        KEY_UPLOAD_DESCRIPTION,
				        KEY_UPLOAD_FILE_URI,
				        KEY_UPLOAD_STATUS,
				        KEY_UPLOAD_VISIBLE,
				        KEY_UPLOAD_BATCH,
				        KEY_UPLOAD_TIMESTAMP,
				        KEY_FLICKR_PHOTO_ID,
			    		"GROUP_CONCAT(" + "A2." + KEY_TAG_NAME + ", ', ') AS " + KEY_TAGS_LIST
	    		}, 
	    		null, null, "A1." + BaseColumns._ID, null, null, null);
    }
    
    // ========================================================================
    public Collection<String> getTagsForUpload(long upload_rowid) {

	    SQLiteDatabase db = getReadableDatabase();
	    Cursor cursor = db.query(
	    		VIEW_NAMED_UPLOAD_TAGS,
	    		new String[] {KEY_TAG_NAME},
	    		KEY_UPLOAD_ID + "=?",
	    		new String[] {Long.toString(upload_rowid)},
	    		null, null, null);

    	Collection<String> tags = new ArrayList<String>();
	    while (cursor.moveToNext())
	    	tags.add(cursor.getString(0));

	    cursor.close();
	    db.close();

	    return tags;
    }

    // ============================================================
    public int removeUpload(long upload_id) {
	    SQLiteDatabase db = getWritableDatabase();
	    
	    String selection = BaseColumns._ID + "=?";
	    String[] selectionArgs = new String[] {Long.toString(upload_id)};
	    
	    // First get the upload status.
	    Cursor cursor = db.query(TABLE_UPLOADS,
	    		new String[] {KEY_UPLOAD_STATUS},
	    		selection, selectionArgs, null, null, null);
	    
	    UploadStatus upload_status = UploadStatus.PENDING;
	    if (cursor.moveToFirst()) {
	    	upload_status = UploadStatus.values()[cursor.getInt(0)];
	    }
	    
	    int updated_count;
	    if (UploadStatus.PENDING.equals(upload_status)) {
	    	updated_count = db.delete(TABLE_UPLOADS, selection, selectionArgs);
	    } else {
		    ContentValues cv = new ContentValues();
		    cv.put(KEY_UPLOAD_VISIBLE, FALSE);
		    updated_count = db.update(TABLE_UPLOADS, cv,
		    		selection, selectionArgs);
	    }
		    
	    db.close();
	    
	    return updated_count;
    }
    
    // ============================================================
    public int hideCompleteUploads() {
	    SQLiteDatabase db = getWritableDatabase();
	    ContentValues cv = new ContentValues();
	    cv.put(KEY_UPLOAD_VISIBLE, FALSE);
	    int updated_count = db.update(TABLE_UPLOADS, cv,
	    		KEY_UPLOAD_STATUS + "=?", new String[] {Integer.toString(UploadStatus.COMPLETE.ordinal())});
	    db.close();
	    
	    return updated_count;
    }

    // ============================================================
    public int updateUploadStatus(long upload_id, UploadStatus new_status) {
	    return updateUploadStatus(upload_id, new_status, ListActivityPhotoTags.INVALID_PHOTO_ID);
    }

    // ============================================================
    public int setUploadTitleDescription(long upload_id, String title, String description) {
	    SQLiteDatabase db = getWritableDatabase();
	    ContentValues cv = new ContentValues();
	    cv.put(KEY_UPLOAD_TITLE, title);
	    cv.put(KEY_UPLOAD_DESCRIPTION, description);
	    
	    int updated_count = db.update(TABLE_UPLOADS, cv,
	    		BaseColumns._ID + "=?", new String[] {Long.toString(upload_id)});
	    db.close();
	    
	    return updated_count;
    }
    
    // ============================================================
    public int updateUploadStatus(long upload_id, UploadStatus new_status, long flickr_photo_id) {
	    SQLiteDatabase db = getWritableDatabase();
	    ContentValues cv = new ContentValues();
	    cv.put(KEY_UPLOAD_STATUS, new_status.ordinal());
	    
	    if (UploadStatus.COMPLETE.equals(new_status) && flickr_photo_id != ListActivityPhotoTags.INVALID_PHOTO_ID)
	    	cv.put(KEY_FLICKR_PHOTO_ID, flickr_photo_id);
	    
	    int updated_count = db.update(TABLE_UPLOADS, cv,
	    		BaseColumns._ID + "=?", new String[] {Long.toString(upload_id)});
	    db.close();
	    
	    return updated_count;
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
	    
	    Log.d(TAG, "History wiped.");
    }
    
    // ============================================================
    public void queueUpload(ImageUploadData upload) {

	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
		ContentValues cv = new ContentValues();
		cv.put(KEY_UPLOAD_FILE_URI, upload.image_uri.toString());
		cv.put(KEY_UPLOAD_TITLE, upload.title);
		cv.put(KEY_UPLOAD_DESCRIPTION, upload.description);
		cv.put(KEY_UPLOAD_STATUS, upload.upload_status.ordinal());
	    long upload_id = db.insert(TABLE_UPLOADS, null, cv);
	    Log.e(TAG, "Row id of inserted upload: " + upload_id);
	    
	    if (upload.tags != null)
	    	insertUploadTags(db, upload_id, upload.tags);
	    
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
    }
    
    // ============================================================
    /** Should have a writable Database handle and be called from within a transaction. */
    public void insertUploadTags(SQLiteDatabase db, long upload_id, Collection<String> tags) {
	    // populate and get IDs for unique tags
	    for (String tag : tags) {
		    Cursor cursor = db.query(TABLE_UNIQUE_TAGS,
		    		new String[] {BaseColumns._ID},
		    		KEY_TAG_NAME + " LIKE ?",
		    		new String[] {tag},
		    		null, null, KEY_TAG_NAME + " ASC");
		    
		    Long tag_id = null;
		    if (cursor.moveToFirst())
		    	tag_id = cursor.getLong(0);

		    cursor.close();
		    
		    if (tag_id == null) {
		    	ContentValues tag_cv = new ContentValues();
		    	tag_id = db.insert(TABLE_UNIQUE_TAGS, null, tag_cv);
		    }

	    	ContentValues tag_upload_association_cv = new ContentValues();
	    	tag_upload_association_cv.put(KEY_UPLOAD_ID, upload_id);
	    	tag_upload_association_cv.put(KEY_TAG_ID, tag_id);
	    	long tag_upload_association_id = db.insert(TABLE_TAG_UPLOAD_ASSOCIATIONS, null, tag_upload_association_cv);
	    }
    }
    
    // ============================================================
    public Cursor retrieveUploads() {

	    SQLiteDatabase db = getReadableDatabase();

	    Cursor c = db.query(
	    		VIEW_TAGGED_UPLOADS,
	    		
	    		new String[] {
		    		BaseColumns._ID,
		            KEY_UPLOAD_FILE_URI,
		            KEY_UPLOAD_STATUS,
		            KEY_UPLOAD_TITLE,
			        KEY_UPLOAD_DESCRIPTION,
		            KEY_UPLOAD_TIMESTAMP,
			        KEY_TAGS_LIST,
			        KEY_FLICKR_PHOTO_ID
			        },
	            KEY_UPLOAD_VISIBLE,
			        
	    		null,
	    		null, null, null);
        
        Log.d(TAG, "Row count: " + c.getCount());
        
        db.close();

        return c;
    }
    
    // ============================================================
    @Override
    public void onCreate(SQLiteDatabase db) {
    	for (String sql : table_creation_commands)
        	db.execSQL( sql );
    }
    
    // ============================================================
    public void drop_all_tables(SQLiteDatabase db) {
    	for (String table : table_list)
    		db.execSQL("DROP TABLE IF EXISTS " + table);
    	
    	for (String view : view_list)
    		db.execSQL("DROP VIEW IF EXISTS " + view);
    }
    
    // ============================================================
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion 
                + " to "
                + newVersion + ", which will destroy all old data");

	        
        drop_all_tables(db);
        
        onCreate(db);
    }
}
