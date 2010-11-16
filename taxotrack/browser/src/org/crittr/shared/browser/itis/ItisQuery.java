package org.crittr.shared.browser.itis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crittr.browse.Market;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.itis.ItisObjects.CommonNameSearchResult;
import org.crittr.shared.browser.itis.ItisObjects.HierarchyResult;
import org.crittr.shared.browser.itis.ItisObjects.KingdomRankResult;
import org.crittr.shared.browser.itis.ItisObjects.ScientificNameSearchResult;
import org.crittr.shared.browser.provider.TaxonSearch;
import org.crittr.task.NetworkUnavailableException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


public class ItisQuery  {

	static final String TAG = Market.DEBUG_TAG;

	// ==============================================================
	
	public static List<HierarchyResult> getFullHierarchyFromTSN(Context context, long tsn) throws NetworkUnavailableException {

		List<HierarchyResult> filtered_results = new ArrayList<HierarchyResult>();
		
		/*
		String[] projection = new String[] {
				TaxonSearch.Itis.COLUMN_PARENT_TAXON_NAME,
				TaxonSearch.Itis.COLUMN_RANK_NAME,
				TaxonSearch.Itis.COLUMN_TAXON_NAME,
				TaxonSearch.Itis.COLUMN_TSN,
				TaxonSearch.Itis.COLUMN_PARENT_TSN};
		*/
		String[] projection = TaxonSearch.Itis.HIERARCHY_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = TaxonSearch.Itis.HIERARCHY_FULL_URI;
 		Cursor c = ((Activity) context).managedQuery(
 				mySuggestion,
 				projection,
 				Long.toString(tsn), null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		
 		while (c.moveToNext()) {
			HierarchyResult hr = new HierarchyResult();
			hr.parent_taxon_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_PARENT_TAXON_NAME) );
			hr.rank_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_RANK_NAME) );
			hr.taxon_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_TAXON_NAME) );
			hr.tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_TSN) );
			hr.parent_tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_PARENT_TSN) );
			filtered_results.add(hr);
 		}
		c.close();
		return filtered_results;
	}
	
	// ==============================================================
	
	public static List<HierarchyResult> getHierarchyDownFromTSN(Context context, long tsn) throws NetworkUnavailableException {

		List<HierarchyResult> filtered_results = new ArrayList<HierarchyResult>();
		
		/*
		String[] projection = new String[] {
				TaxonSearch.Itis.COLUMN_PARENT_TAXON_NAME,
				TaxonSearch.Itis.COLUMN_RANK_NAME,
				TaxonSearch.Itis.COLUMN_TAXON_NAME,
				TaxonSearch.Itis.COLUMN_TSN,
				TaxonSearch.Itis.COLUMN_PARENT_TSN};
		*/
		String[] projection = TaxonSearch.Itis.HIERARCHY_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = TaxonSearch.Itis.HIERARCHY_DOWN_URI;
 		Cursor c = ((Activity) context).managedQuery(
 				mySuggestion,
 				projection,
 				Long.toString(tsn), null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		
 		while (c.moveToNext()) {
			HierarchyResult hr = new HierarchyResult();
			hr.parent_taxon_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_PARENT_TAXON_NAME) );
			hr.rank_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_RANK_NAME) );
			hr.taxon_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_TAXON_NAME) );
			hr.tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_TSN) );
			hr.parent_tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_PARENT_TSN) );
			filtered_results.add(hr);
 		}
		c.close();
		return filtered_results;
	}
	
	// ==============================================================
	
	public static List<HierarchyResult> getHierarchyUpFromTSN(Context context, long tsn) throws NetworkUnavailableException {

		List<HierarchyResult> filtered_results = new ArrayList<HierarchyResult>();
		
		/*
		String[] projection = new String[] {
				TaxonSearch.Itis.COLUMN_PARENT_TAXON_NAME,
				TaxonSearch.Itis.COLUMN_RANK_NAME,
				TaxonSearch.Itis.COLUMN_TAXON_NAME,
				TaxonSearch.Itis.COLUMN_TSN,
				TaxonSearch.Itis.COLUMN_PARENT_TSN};
		*/
		String[] projection = TaxonSearch.Itis.HIERARCHY_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = TaxonSearch.Itis.HIERARCHY_UP_URI;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				Long.toString(tsn), null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		
 		while (c.moveToNext()) {
			HierarchyResult hr = new HierarchyResult();
			hr.parent_taxon_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_PARENT_TAXON_NAME) );
			hr.rank_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_RANK_NAME) );
			hr.taxon_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_TAXON_NAME) );
			hr.tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_TSN) );
			hr.parent_tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_PARENT_TSN) );
			filtered_results.add(hr);
 		}
		c.close();
		return filtered_results;
	}
	
	// ==============================================================
	
	public static long getParentTSNFromTSN(Context context, long tsn) throws NetworkUnavailableException {

		String[] projection = new String[] {TaxonSearch.Itis.COLUMN_PARENT_TSN};
		
 		Uri mySuggestion = TaxonSearch.Itis.PARENT_TSN_BY_TSN_URI;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				Long.toString(tsn), null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
 		long parent_tsn = Constants.INVALID_TSN;
 		if (c.moveToFirst())
 			parent_tsn = c.getLong(0);

 		c.close();
 		
		return parent_tsn;
	}
	
	
	// ==============================================================
	
	public static String getScientificNameFromTSN(Context context, long tsn) throws NetworkUnavailableException {

		String[] projection = new String[] {TaxonSearch.Itis.COLUMN_COMBINED_NAME};
		
 		Uri mySuggestion = TaxonSearch.Itis.SCIENTIFIC_NAME_RESULT;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				Long.toString(tsn), null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
 		String name = null;
 		if (c.moveToFirst())
 			name = c.getString(0);

 		c.close();
 		
		return name;
	}
	
	

	// ==============================================================
	
	public static KingdomRankResult getTaxonomicRankNameFromTSN(Context context, long tsn) throws NetworkUnavailableException {

		String[] projection = TaxonSearch.Itis.RANK_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = TaxonSearch.Itis.RANK_NAME_BY_TSN_URI;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				Long.toString(tsn), null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		

		KingdomRankResult kr = null;
 		
 		while (c.moveToNext()) {
 			
 			kr = new KingdomRankResult();
 			
 			kr.rank_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_RANK_NAME) );
 			kr.rank_id = c.getInt( column_number.get(TaxonSearch.Itis.COLUMN_RANK_ID) );
			kr.kingdom_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_KINGDOM_NAME) );
			kr.kingdom_id = c.getInt( column_number.get(TaxonSearch.Itis.COLUMN_KINGDOM_ID) );
 		}
		c.close();
		return kr;
	}
	
	
	// ==============================================================
	
	public static List<KingdomRankResult> getRankNames(Context context) throws NetworkUnavailableException {

		List<KingdomRankResult> filtered_results = new ArrayList<KingdomRankResult>();

		String[] projection = TaxonSearch.Itis.RANK_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = TaxonSearch.Itis.RANK_NAMES_URI;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				null, null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		
 		while (c.moveToNext()) {
 			KingdomRankResult kr = new KingdomRankResult();
 			kr.rank_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_RANK_NAME) );
 			kr.rank_id = c.getInt( column_number.get(TaxonSearch.Itis.COLUMN_RANK_ID) );
 			kr.kingdom_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_KINGDOM_NAME) );
			kr.kingdom_id = c.getInt( column_number.get(TaxonSearch.Itis.COLUMN_KINGDOM_ID) );

			filtered_results.add(kr);
 		}
		c.close();
		return filtered_results;
	}
	// ==============================================================
	
	public static List<CommonNameSearchResult> getCommonNamesFromTSN(Context context, long tsn) throws NetworkUnavailableException {

		List<CommonNameSearchResult> filtered_results = new ArrayList<CommonNameSearchResult>();

		String[] projection = TaxonSearch.Itis.COMMON_NAME_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = TaxonSearch.Itis.VERNACULAR_BY_TSN_URI;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				Long.toString(tsn), null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		
 		while (c.moveToNext()) {
 			CommonNameSearchResult hr = new CommonNameSearchResult();
 			hr.language = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_LANGUAGE) );
 			hr.common_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_COMMON_NAME) );
			hr.tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_TSN) );


//    		Log.i(TAG + "-ItisQuery", "vernac: " + hr.common_name);
			
			filtered_results.add(hr);
 		}
		c.close();
		return filtered_results;
	}

	
	
	// ==============================================================
	
	public static List<CommonNameSearchResult> searchByCommonNameBeginsWith(Context context, String search_phrase) throws NetworkUnavailableException {

 		Uri mySuggestion = TaxonSearch.Itis.VERNACULAR_BEGINS_SEARCH_URI;
		return searchByCommon(context, mySuggestion, search_phrase);
	}
	// ==============================================================
	
	public static List<CommonNameSearchResult> searchByCommonNameEndsWith(Context context, String search_phrase) throws NetworkUnavailableException {

 		Uri mySuggestion = TaxonSearch.Itis.VERNACULAR_ENDS_SEARCH_URI;
		return searchByCommon(context, mySuggestion, search_phrase);
	}
	// ==============================================================
	
	public static List<CommonNameSearchResult> searchByCommonName(Context context, String search_phrase) throws NetworkUnavailableException {

 		Uri mySuggestion = TaxonSearch.Itis.VERNACULAR_CONTAINS_SEARCH_URI;
		return searchByCommon(context, mySuggestion, search_phrase);
	}
	// ==============================================================
	public static List<CommonNameSearchResult> searchByCommon(Context context, Uri type, String search_phrase) throws NetworkUnavailableException {

		List<CommonNameSearchResult> filtered_results = new ArrayList<CommonNameSearchResult>();

		String[] projection = TaxonSearch.Itis.COMMON_NAME_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = type;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				search_phrase, null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		
 		while (c.moveToNext()) {
 			CommonNameSearchResult hr = new CommonNameSearchResult();
 			hr.language = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_LANGUAGE) );
 			hr.common_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_COMMON_NAME) );
			hr.tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_TSN) );

			filtered_results.add(hr);
 		}
		c.close();
		return filtered_results;
	}
	

	// ==============================================================
	public static List<ScientificNameSearchResult> searchByScientificName(Context context, String search_phrase) throws NetworkUnavailableException {

		List<ScientificNameSearchResult> filtered_results = new ArrayList<ScientificNameSearchResult>();

		String[] projection = TaxonSearch.Itis.SCIENTIFIC_NAME_RESULT_COLUMN_NAMES;
		
 		Uri mySuggestion = TaxonSearch.Itis.SCIENTIFIC_SEARCH_URI;
 		Cursor c = context.getContentResolver().query(
 				mySuggestion,
 				projection,
 				search_phrase, null, null);

 		if (c == null) throw new NetworkUnavailableException();
 		
		// Use a "Map" to remember the column number.
 		Map<String, Integer> column_number = new HashMap<String, Integer>();
 		for (String col : projection)
 			column_number.put(col, c.getColumnIndex(col));
 		
 		while (c.moveToNext()) {
 			ScientificNameSearchResult hr = new ScientificNameSearchResult();
 			hr.combined_name = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_COMBINED_NAME) );
 			hr.unit1 = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_UNIT1) );
 			hr.unit2 = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_UNIT2) );
 			hr.unit3 = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_UNIT3) );
 			hr.unit4 = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_UNIT4) );
 			hr.genus = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_GENUS) );
 			hr.species = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_SPECIES) );
 			hr.subspecies = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_SUBSPECIES) );
 			hr.subsubspecies = c.getString( column_number.get(TaxonSearch.Itis.COLUMN_SUBSUBSPECIES) );
			hr.tsn = c.getLong( column_number.get(TaxonSearch.Itis.COLUMN_TSN) );

			filtered_results.add(hr);
 		}
		c.close();
		return filtered_results;
	}
	
}

