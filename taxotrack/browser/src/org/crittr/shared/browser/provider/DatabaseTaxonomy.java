package org.crittr.shared.browser.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crittr.browse.Market;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.KingdomRankIdPair;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.shared.browser.itis.ItisUtils;
import org.crittr.shared.browser.itis.ItisObjects.KingdomRankResult;
import org.crittr.task.NetworkUnavailableException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseTaxonomy extends SQLiteOpenHelper 
{
	public static final String TAG = Market.DEBUG_TAG;
	
	static final String DATABASE_NAME = "TAXONOMY_DATA";
    static final int DATABASE_VERSION = 13;


    public static final String TABLE_CACHED_TAXONS = "TABLE_CACHED_TAXONS";
    public static final String TABLE_PREFERRED_THUMBNAILS = "TABLE_PREFERRED_THUMBNAILS";
    
    public static final String TABLE_BOOKMARKED_TAXONS = "TABLE_BOOKMARKED_TAXONS";
    public static final String TABLE_RANK_KEYS = "TABLE_RANK_KEYS";

    public static final String KEY_ROWID = BaseColumns._ID;
    public static final String KEY_TSN = "KEY_TSN";
    public static final String KEY_USE_COUNT = "KEY_USE_COUNT";

    public static final String KEY_ITIS_KINGDOM_ID = "KEY_ITIS_KINGDOM_ID";
    public static final String KEY_RANK_NAME = "KEY_RANK_NAME";
    public static final String KEY_RANK_ID = "KEY_RANK_ID";
    public static final String KEY_TAXON_NAME = "KEY_TAXON_NAME";
    public static final String KEY_VERNACULAR_NAMES = "KEY_VERNACULAR_NAMES";
    public static final String KEY_PARENT_TSN = "KEY_PARENT_TSN";
    public static final String KEY_CACHED_CHILDREN = "KEY_CACHED_CHILDREN";

    public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";

    

    public static final String KEY_THUMBNAIL_URL = "KEY_THUMBNAIL_URL";
    public static final String KEY_LOCAL_THUMBNAIL_PATH = "KEY_LOCAL_THUMBNAIL_PATH";
    
    
    // Note: The bookmarks table can be used to learn local usage patterns and backtrack...
    final static String SQL_CREATE_TSN_BOOKMARKS_TABLE =
        "create table " + TABLE_BOOKMARKED_TAXONS + " (" 
        + KEY_TSN + " integer primary key ON CONFLICT IGNORE, "
        + KEY_USE_COUNT + " integer default 1, "
//        + KEY_TIMESTAMP + " TIMESTAMP(8));";	// In MySQL, this would act as a "last modified" field
        + KEY_TIMESTAMP + " TIMESTAMP NULL default CURRENT_TIMESTAMP);";

    final static String SQL_CREATE_RANK_KEYS_TABLE =
        "create table " + TABLE_RANK_KEYS + " (" 
//        + KEY_ROWID + " integer primary key autoincrement, "
        + KEY_ITIS_KINGDOM_ID + " integer not null, "
        + KEY_RANK_NAME + " text not null, "
        + KEY_RANK_ID + " integer not null,"
        + "PRIMARY KEY(" + KEY_ITIS_KINGDOM_ID + ", " + KEY_RANK_ID + ") ON CONFLICT IGNORE);";
    
    final static String SQL_CREATE_CACHED_TAXONS_TABLE =
        "create table " + TABLE_CACHED_TAXONS + " (" 
        + KEY_TSN + " integer primary key, "
        + KEY_PARENT_TSN + " integer null default " + Constants.UNKNOWN_PARENT_ID + ", "
        + KEY_ITIS_KINGDOM_ID + " integer null default " + Constants.UNKNOWN_ITIS_KINGDOM + ", "
        + KEY_RANK_ID + " integer null default " + Constants.INVALID_RANK_ID + ", "
        + KEY_TAXON_NAME + " text, "
        + KEY_CACHED_CHILDREN + " integer null default 0, "
        + KEY_VERNACULAR_NAMES + " text);";

    final static String SQL_CREATE_PREFERRED_THUMBNAILS_TABLE =
        "create table " + TABLE_PREFERRED_THUMBNAILS + " (" 
        + KEY_TSN + " integer primary key, "
        + KEY_THUMBNAIL_URL + " text, "
        + KEY_LOCAL_THUMBNAIL_PATH + " text);";
    
    final static String[] table_list = {
    	TABLE_BOOKMARKED_TAXONS,
    	TABLE_PREFERRED_THUMBNAILS,
    	TABLE_RANK_KEYS,
    	TABLE_CACHED_TAXONS};
    
    final static String[] table_creation_commands = {
    	SQL_CREATE_TSN_BOOKMARKS_TABLE,
    	SQL_CREATE_PREFERRED_THUMBNAILS_TABLE,
    	SQL_CREATE_RANK_KEYS_TABLE,
    	SQL_CREATE_CACHED_TAXONS_TABLE};
    
	public final Map<Integer, Map<Integer, String>> rank_name_key;
	
    Context context;
    public DatabaseTaxonomy(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        
        this.context = context;
        
		if (!check_has_ranks()) slurp_itis_ranks_table();
		rank_name_key = generate_rank_hash();
    }

    // ========================================================================
    public String getPreferredThumbnail(long tsn) {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    
	    Cursor c = db.query(TABLE_PREFERRED_THUMBNAILS,
	    		new String[] {KEY_THUMBNAIL_URL},
	    		KEY_TSN + "=?", new String[] {Long.toString(tsn)},
	    		null, null, null);

	    String preferred_thumbnail_url = null;
	    if (c.moveToFirst())
	    	preferred_thumbnail_url = c.getString(0);
	    c.close();
	    db.close();

//	    Log.d(TAG, "Getting preferred thumbnail for " + tsn + ": " + preferred_thumbnail_url);
	    return preferred_thumbnail_url;
    }

    // ========================================================================
    public void markPreferredThumbnail(long tsn, String thumbnail_url) {
    	
	    SQLiteDatabase db = getWritableDatabase();

	    ContentValues cv = new ContentValues();
        cv.put(KEY_THUMBNAIL_URL, thumbnail_url);
	    int updates = db.update(TABLE_PREFERRED_THUMBNAILS, cv, KEY_TSN + " = ?", new String[] {Long.toString(tsn)});
	    if (updates == 0) {
	    	cv.put(KEY_TSN, tsn);
	    	db.insert(TABLE_PREFERRED_THUMBNAILS, null, cv);
	    }

        Log.d(TAG, "Set preferred thumbnail for " + tsn + ": " + thumbnail_url);
        
	    db.close();
    }

    // ========================================================================
    // TODO: Maybe this method should accept a TaxonInfo object instead?
    public void add_or_update_tsn_bookmark(long tsn) {
    	
	    SQLiteDatabase db = getWritableDatabase();
 
	    
//	    The following doesn't work, because it doesn't return the number of rows affected.
//	    db.execSQL("UPDATE " + TABLE_BOOKMARKED_TAXONS + " SET " + KEY_USE_COUNT + "=" + KEY_USE_COUNT + " + 1 WHERE KEY_TSN = ?", new Object[] {tsn});

	    ContentValues cv = new ContentValues();
        cv.put(KEY_TSN, tsn);
        long result = db.insert(TABLE_BOOKMARKED_TAXONS, null, cv);
        
//        Log.d(TAG, "Insertion results: " + result);
        // BEWARE: This relies on the fact that 0 is not a valid TSN (I checked)...
        // http://www.itis.gov/ITISWebService/services/ITISService/getScientificNameFromTSN?tsn=0
        if (result == 0) {
        	Log.w(TAG, "Incremented bookmark: " + tsn);
    	    db.execSQL("UPDATE " + TABLE_BOOKMARKED_TAXONS + " SET " + KEY_USE_COUNT + "=" + KEY_USE_COUNT + " + 1, " + KEY_TIMESTAMP + " = CURRENT_TIMESTAMP WHERE KEY_TSN = ?", new Object[] {tsn});
        } else {
        	Log.d(TAG, "Added bookmark: " + tsn);
        }
        
	    // NOTE: The builtin update() command is unsuitable, because it employs the ContentValues structure,
        // which doesn't allow you to reference other variables.  If this were not the case,
        // then the update/count affected/insert idiom would work.
//	    int affected_rows = db.update(TABLE_BOOKMARKED_TAXONS, values, "KEY_TSN = ?", new String[] {tsn});

	    db.close();
    }
    
    
    public Cursor get_recent_bookmarks() {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    
		// NOTE: This "AS" trick actually works (and is quite necessary)!
	    Cursor c = db.query(TABLE_BOOKMARKED_TAXONS,
	    		new String[] {KEY_TSN + " AS " + KEY_ROWID, KEY_USE_COUNT, "strftime('%s', " + KEY_TIMESTAMP + ") AS " + KEY_TIMESTAMP},
	    		null, null,
	    		null, null, KEY_TIMESTAMP + " DESC");

	    Log.d(TAG, "Number of bookmarks: " + c.getCount());
	    
	    c.moveToFirst();
	    db.close();
	    
	    return c;
    }

    // ========================================================================
    public void clear_cached_taxons() {

	    SQLiteDatabase db = getWritableDatabase();
	    
	    // TODO: Don't actually drop the whole table;
	    // instead remove the bottom 80% of taxons in terms of usage/views
	    
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHED_TAXONS);
    	db.execSQL( SQL_CREATE_CACHED_TAXONS_TABLE );
    	db.close();
    	
    	Log.d(TAG, "Cleared cached taxons.");

    	// Repopulate the kingdom list, at least
    	slurp_itis_ranks_table();
//    	slurp_kingdoms();
    }

    // ========================================================================
    // Methods for initial setup:
    public boolean check_has_ranks() {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_RANK_KEYS,
	    		null, null, null,
	    		null, null, null, "1");

	    int count = c.getCount();
	    c.close();
	    db.close();
	    return count > 0;
    }

    // ========================================================================
    public void slurp_itis_ranks_table() {

	    slurp_kingdoms();

	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
    	
		try {
			List<KingdomRankResult> ranks = ItisQuery.getRankNames(context);
			for (KingdomRankResult rank : ranks) {

	            ContentValues c = new ContentValues();

//	           	Log.i(TAG, "Storing rank name for kingdom_id " + kingdom_id + " and rank_id " + rank_id + ": " + rank_name);
				
	            c.put(KEY_ITIS_KINGDOM_ID,  rank.kingdom_id );
	            c.put(KEY_RANK_ID, rank.rank_id );
	            c.put(KEY_RANK_NAME, rank.rank_name );

	            db.insert(TABLE_RANK_KEYS, null, c);
			}
		} catch (NetworkUnavailableException e) {
			e.printStackTrace();
		}


	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }

	    db.close();
	    Log.d(TAG, "Acquired taxonomic ranks list.");
    }

    // ========================================================================
	public static String kingdomNameFromId(int kingdom_id) {
		for (int i=0; i<ItisUtils.KINGDOM_ID_LIST.length; i++)
			if (kingdom_id == ItisUtils.KINGDOM_ID_LIST[i])
				return ItisUtils.KINGDOM_NAME_LIST[i];

		return null;
	}

    // ========================================================================
    public void slurp_kingdoms() {
    	
	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		for (int i=0; i<ItisUtils.KINGDOM_ID_LIST.length; i++) {

            ContentValues cv = new ContentValues();

		    cv.put(KEY_ITIS_KINGDOM_ID, ItisUtils.KINGDOM_ID_LIST[i]);
		    cv.put(KEY_TAXON_NAME, ItisUtils.KINGDOM_NAME_LIST[i]);
		    cv.put(KEY_PARENT_TSN, Constants.NO_PARENT_ID);
		    cv.put(KEY_RANK_ID, ItisUtils.KINGDOM_RANK);
		    cv.put(KEY_VERNACULAR_NAMES, ItisUtils.KINGDOM_VERNACULAR_LIST[i]);
		    
		    int updates = db.update(TABLE_CACHED_TAXONS, cv, KEY_TSN + " = ?", new String[] {Long.toString(ItisUtils.KINGDOM_TSN_LIST[i])});
		    if (updates == 0) {
		    	cv.put(KEY_TSN, ItisUtils.KINGDOM_TSN_LIST[i]);
            	db.insert(TABLE_CACHED_TAXONS, null, cv);
		    }
		}

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
	    
	    Log.d(TAG, "Imported kingdom list.");
    }
    
    // =================================
    /*
    public String get_rank_name_from_id(int rank_id, int kingdom_id) {
    	
    	String where_clause;
    	String[] where_args;
    	if (kingdom_id == UNKNOWN_ITIS_KINGDOM) {	// Kingdom is unknown - this might work anyway
    		where_clause = KEY_RANK_ID + "= ?";
    		where_args = new String[] {Integer.toString(rank_id)};
    	} else {
    		where_clause = KEY_RANK_ID + "= ? AND " + KEY_ITIS_KINGDOM_ID + "= ?";
    		where_args = new String[] {Integer.toString(rank_id), Integer.toString(kingdom_id)};
    	}
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_RANK_KEYS,
	    		new String[] {KEY_RANK_NAME},
	    		where_clause,
	    		where_args,
	    		null, null, null);

	    String result = null;
	    if (c.getCount() > 0) {
		    c.moveToFirst();
		    result = c.getString(0);
	    }
	    c.close();
	    db.close();
	    return result;
    }
    */

    // ========================================================================
	public Map<Integer, Map<Integer, String>> generate_rank_hash() {
		Map<Integer, Map<Integer, String>> rank_hash = new HashMap<Integer, Map<Integer, String>>();

	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_RANK_KEYS,
	    		new String[] {KEY_ITIS_KINGDOM_ID, KEY_RANK_ID, KEY_RANK_NAME},
	    		null,
	    		null,
	    		null, null, null);

	    while( c.moveToNext() ) {
	    	int kingdom_id = c.getInt(0);
	    	
	    	Map<Integer, String> subhash;
	    	if (rank_hash.containsKey(kingdom_id))
	    		subhash = (Map<Integer, String>) rank_hash.get(kingdom_id);
	    	else {
	    		subhash = new HashMap<Integer, String>();
	    		rank_hash.put(kingdom_id, subhash);
	    	}

	    	subhash.put(c.getInt(1), c.getString(2));
	    }

	    c.close();
	    db.close();

		return rank_hash;
	}

    // ========================================================================
	// This has been known to raise a "LOCK" exception.
    public void set_vernaculars(long tsn, String vernaculars) {

    	try {
		    SQLiteDatabase db = getWritableDatabase();
		    
		    
//		    Log.e(TAG, "Storing vernacular for TSN " + tsn + ": " + vernaculars);
		    
		    // We use this nice "UPDATE OR INSERT" idiom.
		    
		    ContentValues cv = new ContentValues();
		    cv.put(KEY_VERNACULAR_NAMES, vernaculars);
		    int updates = db.update(TABLE_CACHED_TAXONS, cv, KEY_TSN + " = ?", new String[] {Long.toString(tsn)});
		    if (updates == 0) {
		    	cv.put(KEY_TSN, tsn);
		    	db.insert(TABLE_CACHED_TAXONS, null, cv);
		    }
		    db.close();
    	} catch (SQLiteException e) {
    		Log.e(TAG, "set_vernaculars LOCK: " + e.getMessage());
    	}
    }


    // ========================================================================
    public String get_vernaculars(long tsn) {
    	
    	String vernaculars = null;
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_CACHED_TAXONS,
	    		new String[] {KEY_VERNACULAR_NAMES}, KEY_TSN + " = ?",
	    		new String[] {Long.toString(tsn)},
	    		null, null, null);

	    if (c.moveToFirst()) {
	    	vernaculars = c.getString(0);
	    }
	    c.close();
	    db.close();
	    
	    return vernaculars;
    }

    // ========================================================================
	// This has been known to rase a "LOCK" exception.
    public void setKingdomRank(long tsn, KingdomRankResult ikrp) {	

    	try {
		    SQLiteDatabase db = getWritableDatabase();
		    
		    // We use this nice "UPDATE OR INSERT" idiom.
		    ContentValues cv = new ContentValues();
		    cv.put(KEY_ITIS_KINGDOM_ID, ikrp.kingdom_id);
		    cv.put(KEY_RANK_ID, ikrp.rank_id);
		    int updates = db.update(TABLE_CACHED_TAXONS, cv, KEY_TSN + " = ?", new String[] {Long.toString(tsn)});
		    if (updates == 0) {
		    	cv.put(KEY_TSN, tsn);
		    	db.insert(TABLE_CACHED_TAXONS, null, cv);
		    }
		    db.close();
    	} catch (SQLiteException e) {
    		Log.e(TAG, "setKingdomRank LOCK: " + e.getMessage());
    	}
    }

    // Not to be confused with "get_rank_name_from_id()"
    public KingdomRankIdPair getRankId(long tsn) {
    	
    	KingdomRankIdPair ikrp = new KingdomRankIdPair();
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_CACHED_TAXONS,
	    		new String[] {KEY_ITIS_KINGDOM_ID, KEY_RANK_ID}, KEY_TSN + " = ?",
	    		new String[] {Long.toString(tsn)},
	    		null, null, null);

	    if (c.moveToFirst()) {
	    	ikrp.kingdom_id = c.getInt(0);
	    	ikrp.rank_id = c.getInt(1);
	    }

	    c.close();
	    db.close();
	    
	    return ikrp;
    }

    // ========================================================================
    public void stock_taxon_children(long tsn, List<TaxonInfo> taxon_children) {

	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
	    
	    for (TaxonInfo ti : taxon_children) {
		    ContentValues cv = new ContentValues();
		    cv.put(KEY_TAXON_NAME, ti.taxon_name);
		    cv.put(KEY_PARENT_TSN, tsn);
		    int updates = db.update(TABLE_CACHED_TAXONS, cv, KEY_TSN + " = ?", new String[] {Long.toString(ti.tsn)});
		    if (updates == 0) {
		    	cv.put(KEY_TSN, ti.tsn);
		    	db.insert(TABLE_CACHED_TAXONS, null, cv);
		    }
	    }
	    
	    // Set the "children cached" flag to 1
	    ContentValues cv = new ContentValues();
	    cv.put(KEY_CACHED_CHILDREN, 1);
	    int updates = db.update(TABLE_CACHED_TAXONS, cv, KEY_TSN + " = ?", new String[] {Long.toString(tsn)});
	    if (updates == 0) {
	    	cv.put(KEY_TSN, tsn);
	    	db.insert(TABLE_CACHED_TAXONS, null, cv);
	    }
	    
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();
    }

    // ========================================================================
    public List<TaxonInfo> get_taxon_children(long tsn) {
    	
    	// First, check the "cached children" flag of the parent.
    	// If this is not true, we must return null.

	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_CACHED_TAXONS,
	    		new String[] {KEY_CACHED_CHILDREN}, KEY_TSN + " = ?",
	    		new String[] {Long.toString(tsn)},
	    		null, null, null);
	    

	    if ( !(c.moveToFirst() && c.getInt(0) == 1) ) {
		    c.close();
		    db.close();
	    	return null;
	    }
	    	
	    // Only retrieve the taxon name and the TSN.
	    // TODO: Eventually, maybe we can check if there's more info to grab,
	    // and then populate more of the TaxonInfo data structure at the outset.

	    List<TaxonInfo> taxon_members = new ArrayList<TaxonInfo>(); 
	    c = db.query(TABLE_CACHED_TAXONS,
	    		new String[] {KEY_TSN, KEY_TAXON_NAME}, KEY_PARENT_TSN + " = ?",
	    		new String[] {Long.toString(tsn)},
	    		null, null, null);

	    while (c.moveToNext()) {
	    	TaxonInfo ti = new TaxonInfo();
	    	ti.tsn = c.getLong(0);
	    	ti.taxon_name = c.getString(1);
	    	taxon_members.add(ti);
	    }
	    c.close();
	    db.close();
	    
	    
	    return taxon_members;
    }
    

    // ========================================================================
    public class BasicTaxon {
    	public long parent = Constants.UNKNOWN_PARENT_ID;
    	public int kingdom = Constants.UNKNOWN_ITIS_KINGDOM;
    	public int rank = Constants.INVALID_RANK_ID;
    	public String name;
    	public boolean children_cached = false;
    }
    
    // ========================================================================
    public Map<Long, BasicTaxon> getAllCachedTaxons() {
    	
    	SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_CACHED_TAXONS,
    		new String[] {
	    		KEY_TSN,
	    		KEY_PARENT_TSN,
	            KEY_ITIS_KINGDOM_ID,
	            KEY_RANK_ID,
	            KEY_TAXON_NAME,
	            KEY_CACHED_CHILDREN},
            null, null, null, null, null);
	    
	    Map<Long, BasicTaxon> taxon_map = new HashMap<Long, BasicTaxon>();
	    while (c.moveToNext()) {
	    	BasicTaxon taxon = new BasicTaxon();
	    	long tsn = c.getLong(0);
	    	taxon_map.put(tsn, taxon);
	    	
	    	taxon.parent = c.getLong(1);
	    	taxon.kingdom = c.getInt(2);
	    	taxon.rank = c.getInt(3);
	    	taxon.name = c.getString(4);
	    	taxon.children_cached = c.getInt(5) != 0;
	    }
	    
	    c.close();
	    db.close();
    	
    	return taxon_map;
    }

    // ========================================================================
    public long getParentTSN(long tsn) {
    	
	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_CACHED_TAXONS,
	    		new String[] {KEY_PARENT_TSN},
	    		KEY_TSN + " = ?",
	    		new String[] {Long.toString(tsn)},
	    		null, null, null);
	    
	    long parent_tsn = Constants.INVALID_TSN;
	    if (c.moveToFirst()) {
	    	
	    	parent_tsn = c.getLong(0);
//	    	Log.d(TAG, "Parent retrieved for " + tsn + ": " + parent_tsn);
	    	
	    }
	    c.close();
	    db.close();
	    
	    return parent_tsn;
    }

    // ========================================================================
    public void setParentTSN(long tsn, long parent_tsn) {
	
	    SQLiteDatabase db = getWritableDatabase();
	    
	    // We use this nice "UPDATE OR INSERT" idiom.
	    ContentValues cv = new ContentValues();
	    cv.put(KEY_PARENT_TSN, parent_tsn);
	    int updates = db.update(TABLE_CACHED_TAXONS, cv, KEY_TSN + " = ?", new String[] {Long.toString(tsn)});
	    if (updates == 0) {
	    	cv.put(KEY_TSN, tsn);
	    	db.insert(TABLE_CACHED_TAXONS, null, cv);
	    }
	    db.close();
 
//	    Log.i(TAG, "Set parent for TSN " + tsn + ": " + parent_tsn);
    }

    // ==================================================================
    public List<TaxonInfo> getParentChain(final long tsn) {
    	
	    SQLiteDatabase db = getReadableDatabase();

	    List<TaxonInfo> parent_taxon_chain = new ArrayList<TaxonInfo>();
	    
//	    Log.w(TAG, "Getting database-stored parent chain for: " + tsn);
	    
	    Cursor c;
	    long active_tsn = tsn;
	    while (true) {
		    c = db.query(TABLE_CACHED_TAXONS,
		    		new String[] {KEY_PARENT_TSN, KEY_RANK_ID, KEY_TAXON_NAME},
		    		KEY_TSN + " = ?",
		    		new String[] {Long.toString(active_tsn)},
		    		null, null, null);
	    

//		    Log.w(TAG, "Any data at all?");
		    
		    if (c.moveToFirst()) {
		    	
//		    	Log.i(TAG, "Yes.");
		    	
			    long parent_tsn = c.getLong(0);
			    int rank_id = c.getInt(1);
			    String taxon_name = c.getString(2);
			    
//		    	Log.i(TAG, "parent_tsn: " + parent_tsn + "; rank_id: " + rank_id + "; taxon_name: " + taxon_name);
			    
			    
//			    if (parent_tsn != Constants.INVALID_TSN && taxon_name != null && rank_id != INVALID_RANK_ID) {
			    if (parent_tsn != Constants.INVALID_TSN && taxon_name != null) {

		    		TaxonInfo ti = new TaxonInfo();
		    		ti.taxon_name = taxon_name;
		    		ti.parent_tsn = parent_tsn;
		    		ti.rank_id = rank_id;
		    		parent_taxon_chain.add(ti);
		    		
		    		if (parent_tsn == Constants.NO_PARENT_ID)
		    			break;
		    		else active_tsn = parent_tsn;
		    		
			    } else break;
		    } else break;
	    }
	    c.close();
	    db.close();
	    
	    return parent_taxon_chain;
    }

    // ========================================================================
    public void setParentChain(List<TaxonInfo> parent_taxon_chain) {
    	
	    SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

	    for (TaxonInfo ti : parent_taxon_chain) {
		    // We use this nice "UPDATE OR INSERT" idiom.
		    ContentValues cv = new ContentValues();
		    cv.put(KEY_PARENT_TSN, ti.parent_tsn);
		    if (ti.taxon_name != null && ti.taxon_name.length() > 0)
			    cv.put(KEY_TAXON_NAME, ti.taxon_name);
		    
		    int updates = db.update(TABLE_CACHED_TAXONS, cv, KEY_TSN + " = ?", new String[] {Long.toString(ti.tsn)});
		    if (updates == 0) {
		    	cv.put(KEY_TSN, ti.tsn);
		    	db.insert(TABLE_CACHED_TAXONS, null, cv);
		    }
		    
//		    Log.i(TAG, "Set parent for TSN " + ti.tsn + ": " + ti.parent_tsn);
	    }
	    
	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }
	    db.close();

    }

    // ========================================================================
    public TaxonInfo getParentTaxon(long tsn) {
    	return getSingleTaxon( getParentTSN(tsn) );
    }

    // ========================================================================
	public String getRankName(KingdomRankIdPair ikrp) {
		if ( !ikrp.isInvalid() ) {
			
//			Log.d(TAG, "Is rank_name_key null? " + rank_name_key);
//			Log.d(TAG, "Is ikrp null? " + ikrp);
			
			Map<Integer, String> kingdom_rank_set = rank_name_key.get(ikrp.kingdom_id);
			
//			Log.d(TAG, "Is kingdom_rank_set null? " + kingdom_rank_set);
//			Log.e(TAG, "Rank name for kingdom_id " + ikrp.kingdom_id + " and rank_id " + ikrp.rank_id + ": " + kingdom_rank_set.get(ikrp.rank_id));
			
			return kingdom_rank_set.get(ikrp.rank_id);
			
		}
		Log.e(TAG, "Unknown rank name.");
		return "";
	}
	
    // ========================================================================
    public TaxonInfo getSingleTaxon(long tsn) {
	    	
	    // Only retrieve the taxon name and the TSN.
	    // TODO: Eventually, maybe we can check if there's more info to grab,
	    // and then populate more of the TaxonInfo data structure at the outset.

	    TaxonInfo taxon_info = new TaxonInfo();
	    taxon_info.tsn = tsn;

	    SQLiteDatabase db = getReadableDatabase();
	    Cursor c = db.query(TABLE_CACHED_TAXONS,
	    		new String[] {KEY_TAXON_NAME, KEY_RANK_ID, KEY_ITIS_KINGDOM_ID}, KEY_TSN + " = ?",
	    		new String[] {Long.toString(tsn)},
	    		null, null, null);

//	    int rank_id = INVALID_RANK_ID;
	    if (c.moveToFirst()) {
	    	taxon_info.taxon_name = c.getString(0);
	    	

	    	taxon_info.rank_id = c.getInt(1);
	    	taxon_info.itis_kingdom_id = c.getInt(2);
	    	
	    	KingdomRankIdPair ikrp = new KingdomRankIdPair(taxon_info.itis_kingdom_id, taxon_info.rank_id);
	    	
//	    	Log.w(TAG, "About to get rank name: Kingdom " + taxon_info.itis_kingdom_id + ", Rank " + taxon_info.rank_id);
	    	taxon_info.rank_name = getRankName(ikrp);

	    }
	    c.close();
	    db.close();
	    
//	    taxon_info.rank_name = get_rank_name_from_id(rank_id, UNKNOWN_KINGDOM);
	    
	    
	    return taxon_info;
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

