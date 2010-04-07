package com.googlecode.chartdroid.demo.provider;

import com.googlecode.chartdroid.demo.InputDatasetActivity.EventDatum;

import org.achartengine.demo.ContentSchema;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

public class DatabaseStoredData extends SQLiteOpenHelper 
{
	static final String TAG = "ChartDroid"; 
 

    static final String DATABASE_NAME = "PLOTTABLE_DATA";
    static final int DATABASE_VERSION = 1;

    public static final String TABLE_DATASETS = "TABLE_DATASETS";
    public static final String TABLE_DATA = "TABLE_DATA";
    
    

    public static final String KEY_DATASET_INDEX = "KEY_DATASET_INDEX";
    public static final String KEY_DATASET_LABEL = "KEY_DATASET_LABEL";
    
    
    public static final String KEY_DATUM_INDEX = "KEY_DATUM_INDEX";
    public static final String KEY_AXIS_X = "AXIS_X";
    public static final String KEY_AXIS_Y = "AXIS_Y";
    

    final static String SQL_CREATE_DATASETS_TABLE =
        "create table " + TABLE_DATASETS + " ("
        + KEY_DATASET_INDEX + " integer primary key autoincrement, "
        + KEY_DATASET_LABEL + " text);";

    final static String SQL_CREATE_DATA_TABLE =
        "create table " + TABLE_DATA + " ("
        + KEY_DATASET_INDEX + " integer, "
        + KEY_DATUM_INDEX + " integer, "
        + ContentSchema.PlotData.COLUMN_SERIES_INDEX + " integer, "
        + KEY_AXIS_X + " integer, "
        + KEY_AXIS_Y + " integer, "
        + ContentSchema.PlotData.COLUMN_DATUM_LABEL + " text,"
    	+ "PRIMARY KEY(" + KEY_DATASET_INDEX + ", " + KEY_DATUM_INDEX + ") ON CONFLICT IGNORE);";

    
    final static String[] table_list = {
    	TABLE_DATASETS,
    	TABLE_DATA,
    };

    final static String[] table_creation_commands = {
    	SQL_CREATE_DATASETS_TABLE,
    	SQL_CREATE_DATA_TABLE
    };

    // ============================================================
    public DatabaseStoredData(Context context)
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
    public int deleteAllData() {

    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
		int deletion_count = db.delete(TABLE_DATA, null, null);
		int deleted_dataset_count = db.delete(TABLE_DATASETS, null, null);

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return deletion_count;
    }
    
    // ============================================================
    public long storeEvents(List<EventDatum> event_list) {
    	
    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
    	ContentValues cv = new ContentValues();
    	
    	cv.put(KEY_DATASET_LABEL, "random dataset");
    	long dataset_id = db.insert(TABLE_DATASETS, null, cv);
    	cv.clear();

    	cv.put(KEY_DATASET_INDEX, dataset_id);
    	int datum_index = 0;
        for (EventDatum datum : event_list) {
            
        	cv.put(ContentSchema.PlotData.COLUMN_SERIES_INDEX, 0);	// XXX
        	cv.put(KEY_DATUM_INDEX, datum_index);
        	cv.put(KEY_AXIS_X, datum.timestamp);
        	cv.put(KEY_AXIS_Y, datum.value);
//        	cv.put(COLUMN_DATUM_LABEL, null);

        	db.insert(TABLE_DATA, null, cv);
        	datum_index++;
        }

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return dataset_id;
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
