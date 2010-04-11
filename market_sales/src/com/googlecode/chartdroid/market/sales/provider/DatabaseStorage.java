package com.googlecode.chartdroid.market.sales.provider;

import com.googlecode.chartdroid.market.sales.Market;
import com.googlecode.chartdroid.market.sales.container.SpreadsheetRow;
import com.googlecode.chartdroid.market.sales.task.SpreadsheetFetcherTask.HistogramBin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.List;

public class DatabaseStorage extends SQLiteOpenHelper 
{
	static final String TAG = Market.TAG; 
 
    static final String DATABASE_NAME = "STORAGE";
    static final int DATABASE_VERSION = 12;

    
    
    public static final String TABLE_SALES = "TABLE_SALES";
    public static final String TABLE_HISTOGRAMMED_SALES = "TABLE_HISTOGRAMMED_SALES";
    public static final String TABLE_PLOTS = "TABLE_PLOTS";
    public static final String TABLE_BATCHES = "TABLE_BATCHES";
    

    public static final String KEY_SALE_ID = "KEY_SALE_ID";
    public static final String KEY_PRODUCT_NAME = "KEY_PRODUCT_NAME";
    public static final String KEY_INCOME = "KEY_INCOME";
    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";

    public static final String KEY_BIN_ID = "KEY_BIN_ID";
    public static final String KEY_SALE_COUNT = "KEY_SALE_COUNT";
    public static final String KEY_START_TIME = "KEY_START_TIME";
    public static final String KEY_END_TIME = "KEY_TIMESTAMP";
    

    public static final String KEY_PLOT_ID = "KEY_PLOT_ID";
    public static final String KEY_BATCH_ID = "KEY_BATCH_ID";

    final static String SQL_CREATE_BATCH_TABLE =
        "create table " + TABLE_BATCHES + " ("
        + KEY_BATCH_ID + " integer primary key autoincrement, "
        + KEY_TIMESTAMP + " TIMESTAMP default CURRENT_TIMESTAMP);";

    final static String SQL_CREATE_SALES_TABLE =
        "create table " + TABLE_SALES + " ("
        + KEY_SALE_ID + " integer, "
        + KEY_BATCH_ID + " integer, "
        + KEY_PRODUCT_NAME + " text default null, "
        + KEY_INCOME + " real, "
        + KEY_TIMESTAMP + " integer, "
    	+ "PRIMARY KEY(" + KEY_SALE_ID + ", " + KEY_BATCH_ID + ") ON CONFLICT IGNORE);";

    final static String SQL_CREATE_PLOTS_TABLE =
        "create table " + TABLE_PLOTS + " ("
        + KEY_PLOT_ID + " integer primary key autoincrement, "
        + KEY_TIMESTAMP + " TIMESTAMP default CURRENT_TIMESTAMP);";
    
    final static String SQL_HISTOGRAMMED_SALES_TABLE =
        "create table " + TABLE_HISTOGRAMMED_SALES + " ("
        + KEY_BIN_ID + " integer, "
        + KEY_PLOT_ID + " integer, "
        + KEY_SALE_COUNT + " integer, "
        + KEY_INCOME + " real, "
        + KEY_START_TIME + " integer, "
        + KEY_END_TIME + " integer, "
    	+ "PRIMARY KEY(" + KEY_BIN_ID + ", " + KEY_PLOT_ID + ") ON CONFLICT IGNORE);";
    
    
    final static String[] table_list = {
    	TABLE_SALES,
    	TABLE_HISTOGRAMMED_SALES,
    	TABLE_PLOTS,
    	TABLE_BATCHES
    };
    

    final static String[] table_creation_commands = {
    	SQL_CREATE_SALES_TABLE,
    	SQL_HISTOGRAMMED_SALES_TABLE,
    	SQL_CREATE_BATCH_TABLE,
    	SQL_CREATE_PLOTS_TABLE
    };
    
    // ============================================================
    public DatabaseStorage(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ============================================================
    public int deleteOldPlots() {

    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
		int deletion_count = db.delete(TABLE_HISTOGRAMMED_SALES, null, null);
		int deleted_dataset_count = db.delete(TABLE_PLOTS, null, null);

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return deletion_count;
    }
    
    // ============================================================
    public long aggregateForPlot(List<HistogramBin> bins) {
    	
    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
    	ContentValues cv = new ContentValues();

		long plot_id = db.insert(TABLE_PLOTS, KEY_TIMESTAMP, cv);
//		cv.clear();	// Not necessary, since it's already empty


		cv.put(KEY_PLOT_ID, plot_id);
		
		int bin_id = 0;
    	for (HistogramBin bin : bins) {

    		cv.put(KEY_BIN_ID, bin_id);

    		cv.put(KEY_SALE_COUNT, bin.rows.size());
    		cv.put(KEY_INCOME, bin.getTotalIncome());
    		
    		long start_time_seconds = bin.start.getTime()/1000L;
    		cv.put(KEY_START_TIME, start_time_seconds);
    		
    		long end_time_seconds = bin.end.getTime()/1000L;
    		cv.put(KEY_END_TIME, end_time_seconds);
    		
    		long rowid = db.insert(TABLE_HISTOGRAMMED_SALES, null, cv);
    		
    		bin_id++;
    	}
    	
    	
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return plot_id;
    }

    // ============================================================
    public long storeRecords(List<SpreadsheetRow> rows) {
    	
    	SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
    	ContentValues cv = new ContentValues();

		long batch_id = db.insert(TABLE_BATCHES, KEY_TIMESTAMP, cv);
    	
    	for (SpreadsheetRow row : rows) {
    		cv.clear();

    		cv.put(KEY_BATCH_ID, batch_id);
    		
    		cv.put(KEY_SALE_ID, row.getOrderNumber());
    		cv.put(KEY_INCOME, row.getIncome());
    		
    		long inserted_seconds = row.getOrderDate().getTime()/1000L;
    		cv.put(KEY_TIMESTAMP, inserted_seconds);
    		
    		long rowid = db.insert(TABLE_SALES, null, cv);
    	}
    	
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    return batch_id;
    }
    
    
    // ============================================================
    public Cursor getHistogrammedSalesForPlotting(long plot_id) {
	    
    	final String COLUMN_AXIS_X = "AXIS_X";
    	final String COLUMN_AXIS_Y = "AXIS_Y";
    	final String COLUMN_AXIS_Z = "AXIS_Z";

	    SQLiteDatabase db = getReadableDatabase();

	    Cursor c = db.query(TABLE_HISTOGRAMMED_SALES,
    		new String[] {
	    		KEY_BIN_ID + " AS " + BaseColumns._ID,
    			0 + " AS " + ColumnSchema.COLUMN_SERIES_INDEX,	// TODO: Different series for different apps
    			KEY_START_TIME + "*1000 AS " + COLUMN_AXIS_X,
    			KEY_INCOME + " AS " + COLUMN_AXIS_Y,
    			KEY_SALE_COUNT + " AS " + COLUMN_AXIS_Z	// TODO: Add handler in ChartDroid
    		},
    		KEY_PLOT_ID + "=?",
    		new String[] {Long.toString(plot_id)},
    		null, null,
    		KEY_START_TIME + " ASC");
    
	    int row_count = c.getCount();
	    Log.d(TAG, "Row count: " + row_count);
//        db.close();

        return c;
    }

    // ============================================================
    public Cursor getRawSalesForPlotting(long batch_id) {
    	
    	final String COLUMN_AXIS_X = "AXIS_X";
    	final String COLUMN_AXIS_Y = "AXIS_Y";

	    SQLiteDatabase db = getReadableDatabase();

	    Cursor c = db.query(TABLE_SALES,
    		new String[] {
	    		KEY_SALE_ID + " AS " + BaseColumns._ID,
    			0 + " AS " + ColumnSchema.COLUMN_SERIES_INDEX,
    			KEY_TIMESTAMP + "*1000 AS " + COLUMN_AXIS_X,
    			KEY_INCOME + " AS " + COLUMN_AXIS_Y,
    			KEY_PRODUCT_NAME + " AS " + ColumnSchema.COLUMN_DATUM_LABEL
    		},
    		KEY_BATCH_ID + "=?",
    		new String[] {Long.toString(batch_id)},
    		null, null,
		    KEY_TIMESTAMP + " ASC");
    
	    int row_count = c.getCount();
	    Log.d(TAG, "Row count: " + row_count);
//        db.close();
        
        return c;
    }
    
    // ============================================================
    public int countRawRecords(long batch_id) {
    	
	    SQLiteDatabase db = getReadableDatabase();

	    Cursor c = db.query(TABLE_SALES,
	    		new String[] {
	    		KEY_SALE_ID,
	    		KEY_TIMESTAMP + "*1000 AS " + KEY_TIMESTAMP,
    		},
    		KEY_BATCH_ID + "=?",
    		new String[] {Long.toString(batch_id)},
    		null, null, null);

	    /*
	    int date_column = c.getColumnIndex(KEY_TIMESTAMP);
	    int sale_id_column = c.getColumnIndex(KEY_SALE_ID);
	    while (c.moveToNext()) {
	    	Log.i(TAG, c.getLong(sale_id_column) + ": " + c.getLong(date_column) + " : " + new Date(c.getLong(date_column)));
	    }
	    */
	    
	    int row_count = c.getCount();
	    c.close();
        db.close();
        
        return row_count;
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
