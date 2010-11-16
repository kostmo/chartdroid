package org.crittr.provider;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crittr.appengine.AppEngineSearchResultParser;
import org.crittr.browse.Market;
import org.crittr.provider.itis.ItisQueryGenerator;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.KingdomRankIdPair;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisObjects.CommonNameSearchResult;
import org.crittr.shared.browser.itis.ItisObjects.HierarchyResult;
import org.crittr.shared.browser.itis.ItisObjects.KingdomRankResult;
import org.crittr.shared.browser.itis.ItisObjects.ScientificNameSearchResult;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.provider.TaxonSearch;
import org.crittr.shared.browser.provider.TaxonSearch.Itis.CommonNameResultColumn;
import org.crittr.shared.browser.provider.TaxonSearch.Itis.HeirarchyColumn;
import org.crittr.shared.browser.provider.TaxonSearch.Itis.RankResultColumn;
import org.crittr.shared.browser.provider.TaxonSearch.Itis.ScientificNameResultColumn;
import org.crittr.shared.browser.utilities.AsyncTaxonInfoPopulator;
import org.crittr.task.NetworkUnavailableException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;


public class TaxonSearchProvider extends ContentProvider {

    private static final String TAG = Market.DEBUG_TAG;

        
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/scientific_name_result", TaxonSearch.Itis.TAXON_SEARCH);
        
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/vernacular/search/begins", TaxonSearch.Itis.VERNACULAR_BEGINS);
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/vernacular/search/ends", TaxonSearch.Itis.VERNACULAR_ENDS);
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/vernacular/search/contains", TaxonSearch.Itis.VERNACULAR_CONTAINS);
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/vernacular/#", TaxonSearch.Itis.VERNACULAR_TSN);
        
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/hierarchy/full", TaxonSearch.Itis.HIERACHRY_FULL);
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/hierarchy/down", TaxonSearch.Itis.HIERACHRY_DOWN);
        sUriMatcher.addURI(TaxonSearch.AUTHORITY, "itis/hierarchy/up", TaxonSearch.Itis.HIERACHRY_UP);
    }


	@Override
	public boolean onCreate() {
		
//		Log.d(TAG, "Executing onCreate() of TaxonSearchProvider");
		

		return false;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public String getType(Uri uri) {

		
		
		// TODO: FINISH ME
		
        int match = sUriMatcher.match(uri);
        switch (match)
        {
            case TaxonSearch.Itis.HIERACHRY_FULL:
            case TaxonSearch.Itis.HIERACHRY_DOWN:
            	return TaxonSearch.Itis.CONTENT_TYPE_HIERARCHY;
            case TaxonSearch.Itis.HIERACHRY_UP:
            	return TaxonSearch.Itis.CONTENT_TYPE_ITEM_HIERARCHY;

            case TaxonSearch.Itis.TAXON_SUGGEST:
            	return TaxonSearch.TaxonSuggest.CONTENT_TYPE_SUGGESTION;
            case TaxonSearch.Itis.TAXON_SEARCH:
            	return TaxonSearch.Itis.CONTENT_TYPE_ITEM_TAXON_INFO;
            	
            case TaxonSearch.Itis.TSN_PARENT:
            	return TaxonSearch.Itis.CONTENT_TYPE_ITEM_TSN;
            	
            case TaxonSearch.Itis.VERNACULAR_BEGINS:
            case TaxonSearch.Itis.VERNACULAR_ENDS:
            case TaxonSearch.Itis.VERNACULAR_CONTAINS:
            	return TaxonSearch.Itis.CONTENT_TYPE_VERNACULAR;
            	
            	
            case TaxonSearch.Itis.RANK:
            	return TaxonSearch.Itis.CONTENT_TYPE_RANK;
            case TaxonSearch.Itis.RANK_TSN:
            	return TaxonSearch.Itis.CONTENT_TYPE_ITEM_RANK;
            default:
                return null;
        }
	}


	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
	

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		
//		Log.d(TAG, "Running query in TaxonSearchProvider...");
//		Log.i(TAG, uri.toString());
		

		if (uri.equals(TaxonSearch.TaxonSuggest.VERNACULAR_AUTOCOMPLETE_URI)) {
			
//			Log.i(TAG, "Fetching vernacular...");
			
			MatrixCursor c = new MatrixCursor(new String[] {
					BaseColumns._ID,
					TaxonSearch.TaxonSuggest.COLUMN_SUGGESTION});
			
			// Assign the popularity number retrieved from AppEngine
			List<String> vernacular_tokens = new ArrayList<String>();
	
			try {
				vernacular_tokens = AppEngineSearchResultParser.parse_vernacular_search_results( selection );
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			long id_counter = 0;
			for (String token : vernacular_tokens)
				c.newRow().add(id_counter++).add( token );

			Log.w(TAG, "Result count: " + vernacular_tokens.size());
			
			return c;
			
		} else if (uri.equals(TaxonSearch.TaxonSuggest.SCIENTIFIC_AUTOCOMPLETE_URI)) {
			
			// TODO

		} else if (
				uri.equals(TaxonSearch.Itis.HIERARCHY_FULL_URI) ||
				uri.equals(TaxonSearch.Itis.HIERARCHY_DOWN_URI) ||
				uri.equals(TaxonSearch.Itis.HIERARCHY_UP_URI)
		) {

			
			long tsn = Long.parseLong(selection);
			
			MatrixCursor c = new MatrixCursor(projection);
			
			try {

				List<HierarchyResult> hierarcy = null;
				if (uri.equals(TaxonSearch.Itis.HIERARCHY_FULL_URI)) {
					
			    	// Starts at the kingdom level.
			    	// Get as many ranks from the database as you can, then resort to the network.
			    	// We know that we've found the top rank when rank_id == 10.
			    	// Alternatively, check whether TSN == NO_PARENT.
					
					DatabaseTaxonomy helper = new DatabaseTaxonomy(getContext());
					List<TaxonInfo> full_chain = helper.getParentChain(tsn);
					Collections.reverse(full_chain);
					
		    		// Check whether we queried all the way up to the root.
					if (!(full_chain.size() > 0 && full_chain.get(0).parent_tsn == Constants.NO_PARENT_ID)) {
						
						long starting_tsn = full_chain.size() > 0 ? full_chain.get(0).parent_tsn : tsn;
						
			    		List<TaxonInfo> network_addition;
						try {
							network_addition = ItisQueryGenerator.convertHierarchyResultToTaxonInfo( ItisQueryGenerator.getFullHierarchyFromTSN( starting_tsn ) );
						} catch (NetworkUnavailableException e) {
							network_addition = new ArrayList<TaxonInfo>();
						}
						
						helper.setParentChain( network_addition );
						
						
			    		// Concatenate the two chains
			    		network_addition.addAll(full_chain);
			    		
			    		full_chain = network_addition;
					}						
					
					
					// Convert the list back into a list of HeirarchyResult's
					hierarcy = ItisQueryGenerator.convertTaxonInfoToHierarchyResult(full_chain);


				}
				else if (uri.equals(TaxonSearch.Itis.HIERARCHY_DOWN_URI)) {

					DatabaseTaxonomy helper = new DatabaseTaxonomy(getContext());
					
			    	List<TaxonInfo> rank_members = helper.get_taxon_children(tsn);
					if (rank_members != null) {
						hierarcy = ItisQueryGenerator.convertTaxonInfoToHierarchyResult(
								rank_members);
					} else {
	
						hierarcy = ItisQueryGenerator.getHierarchyDownFromTSN(tsn);
						helper.stock_taxon_children(tsn, ItisQueryGenerator.convertHierarchyResultToTaxonInfo(hierarcy));
					}
				}
					
				else if (uri.equals(TaxonSearch.Itis.HIERARCHY_UP_URI))
					hierarcy = ItisQueryGenerator.getHierarchyUpFromTSN(tsn);

		 		Map<String, HeirarchyColumn> column_fix = new HashMap<String, HeirarchyColumn>();
		 		List<String> standard_colnames = Arrays.asList(TaxonSearch.Itis.HIERARCHY_RESULT_COLUMN_NAMES);
				for (String colname : projection)
					column_fix.put(colname,
						HeirarchyColumn.values()[standard_colnames.indexOf(colname)]);

		 		
				long id_counter = 0;
				for (HierarchyResult result : hierarcy) {

					MatrixCursor.RowBuilder row = c.newRow();
					for (String colname : projection) {
					
						switch (column_fix.get(colname)) {
						case _ID:
							row.add(id_counter);
							break;
						case COLUMN_PARENT_TAXON_NAME:
							row.add( result.parent_taxon_name );
							break;
						case COLUMN_RANK_NAME:
							row.add( result.rank_name );
							break;
						case COLUMN_TAXON_NAME:
							row.add( result.taxon_name );
							break;
						case COLUMN_TSN:
							row.add( result.tsn );
							break;
						case COLUMN_PARENT_TSN:
							row.add( result.parent_tsn );
							break;
						}
					}
					id_counter++;
				}
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			return c;
			
		} else if (uri.equals(TaxonSearch.Itis.PARENT_TSN_BY_TSN_URI)) {
			
			long tsn = Long.parseLong(selection);
			
			MatrixCursor c = new MatrixCursor(
					new String[] {TaxonSearch.Itis.COLUMN_PARENT_TSN});
			
			try {
				
				DatabaseTaxonomy helper = new DatabaseTaxonomy(getContext());
				
	        	TaxonInfo parent_taxon = helper.getParentTaxon(tsn);
	        	long parent_tsn = parent_taxon.tsn;
				
				if (parent_tsn == Constants.INVALID_TSN) {
					parent_tsn = ItisQueryGenerator.getParentTSNFromTSN(tsn);
					
			    	// Update the parent in the database.
					helper.setParentTSN(tsn, parent_tsn);
				}

				MatrixCursor.RowBuilder row = c.newRow();
				row.add(parent_tsn);
	
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			return c;
			
		} else if (uri.equals(TaxonSearch.Itis.SCIENTIFIC_NAME_RESULT)) {
			
			long tsn = Long.parseLong(selection);
			
			MatrixCursor c = new MatrixCursor(
					new String[] {TaxonSearch.Itis.COLUMN_COMBINED_NAME});
			
			try {

				DatabaseTaxonomy database_helper = new DatabaseTaxonomy(getContext());
				TaxonInfo taxon = database_helper.getSingleTaxon(tsn);
				String search_taxon_name = taxon.taxon_name;
				if (search_taxon_name != null && search_taxon_name.length() > 0) {
					// nothing (should invert the boolean to make this nicer)
				} else {
					search_taxon_name = ItisQueryGenerator.getScientificNameFromTSN(tsn);
					

			    	// TODO
					// Now stock the database with this information to avoid future network traffic.
			    	// Is this ever needed?
				}

				MatrixCursor.RowBuilder row = c.newRow();
				row.add(search_taxon_name);
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			return c;
			
		} else if (
				uri.equals(TaxonSearch.Itis.VERNACULAR_BEGINS_SEARCH_URI) ||
				uri.equals(TaxonSearch.Itis.VERNACULAR_ENDS_SEARCH_URI) ||
				uri.equals(TaxonSearch.Itis.VERNACULAR_CONTAINS_SEARCH_URI)
		) {

			MatrixCursor c = new MatrixCursor(projection);
			
			try {
				
				List<CommonNameSearchResult> common_name_results = null;
				if (uri.equals(TaxonSearch.Itis.VERNACULAR_BEGINS_SEARCH_URI))
					common_name_results = ItisQueryGenerator.searchByCommonNameBeginsWith(selection);
				else if (uri.equals(TaxonSearch.Itis.VERNACULAR_ENDS_SEARCH_URI))
					common_name_results = ItisQueryGenerator.searchByCommonNameEndsWith(selection);
				else if (uri.equals(TaxonSearch.Itis.VERNACULAR_CONTAINS_SEARCH_URI))
					common_name_results = ItisQueryGenerator.searchByCommonName(selection);
				
		 		Map<String, CommonNameResultColumn> column_fix = new HashMap<String, CommonNameResultColumn>();
		 		List<String> standard_colnames = Arrays.asList(TaxonSearch.Itis.COMMON_NAME_RESULT_COLUMN_NAMES);
				for (String colname : projection)
					column_fix.put(colname,
						CommonNameResultColumn.values()[standard_colnames.indexOf(colname)]);

		 		
				long id_counter = 0;
				for (CommonNameSearchResult result : common_name_results) {

					MatrixCursor.RowBuilder row = c.newRow();
					for (String colname : projection) {
					
						switch (column_fix.get(colname)) {
						case _ID:
							row.add(id_counter);
							break;
						case COLUMN_COMMON_NAME:
							row.add( result.common_name );
							break;
						case COLUMN_LANGUAGE:
							row.add( result.language );
							break;
						case COLUMN_TSN:
							row.add( result.tsn );
							break;
						}
					}
					id_counter++;
				}
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			return c;
			
			
		} else if (	uri.equals(TaxonSearch.Itis.RANK_NAMES_URI) ) {

			MatrixCursor c = new MatrixCursor(projection);
			
			try {
				
				List<KingdomRankResult> common_name_results = ItisQueryGenerator.getRankNames();

		 		Map<String, RankResultColumn> column_fix = new HashMap<String, RankResultColumn>();
		 		List<String> standard_colnames = Arrays.asList(TaxonSearch.Itis.RANK_RESULT_COLUMN_NAMES);
				for (String colname : projection)
					column_fix.put(colname,
							RankResultColumn.values()[standard_colnames.indexOf(colname)]);

		 		
				long id_counter = 0;
				for (KingdomRankResult result : common_name_results) {

					MatrixCursor.RowBuilder row = c.newRow();
					for (String colname : projection) {
					
						switch (column_fix.get(colname)) {
						case _ID:
							row.add(id_counter);
							break;
						case COLUMN_RANK_NAME:
							row.add( result.rank_name );
							break;
						case COLUMN_KINGDOM_NAME:
							row.add( result.kingdom_name );
							break;
						case COLUMN_RANK_ID:
							row.add( result.rank_id );
							break;
						case COLUMN_KINGDOM_ID:
							row.add( result.kingdom_id );
							break;
						}
					}
					id_counter++;
				}
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			Log.e(TAG, "I am pretty sure that this data is restricted...");
			
			return c;
			
		} else if (	uri.equals(TaxonSearch.Itis.RANK_NAME_BY_TSN_URI) ) {

			MatrixCursor c = new MatrixCursor(projection);

			try {
				
				long tsn = Long.parseLong(selection);

				DatabaseTaxonomy database_helper = new DatabaseTaxonomy(getContext());
				KingdomRankIdPair ikrp = database_helper.getRankId(tsn);

				KingdomRankResult rank_name_result;
				if ( ikrp.isInvalid() ) {
				
					rank_name_result = ItisQueryGenerator.getTaxonomicRankNameFromTSN(tsn);
					database_helper.setKingdomRank(tsn, rank_name_result);
				} else {
					
					rank_name_result = new KingdomRankResult();

					rank_name_result.kingdom_name = DatabaseTaxonomy.kingdomNameFromId(ikrp.kingdom_id);
					rank_name_result.rank_name = database_helper.getRankName(ikrp);
					rank_name_result.kingdom_id = ikrp.kingdom_id;
					rank_name_result.rank_id = ikrp.rank_id;
				}
				
				
		 		Map<String, RankResultColumn> column_fix = new HashMap<String, RankResultColumn>();
		 		List<String> standard_colnames = Arrays.asList(TaxonSearch.Itis.RANK_RESULT_COLUMN_NAMES);
				for (String colname : projection)
					column_fix.put(colname,
							RankResultColumn.values()[standard_colnames.indexOf(colname)]);

		 		
				long id_counter = 0;

				MatrixCursor.RowBuilder row = c.newRow();
				for (String colname : projection) {
				
					switch (column_fix.get(colname)) {
					case _ID:
						row.add(id_counter);
						break;
					case COLUMN_RANK_NAME:
						row.add( rank_name_result.rank_name );
						break;
					case COLUMN_KINGDOM_NAME:
						row.add( rank_name_result.kingdom_name );
						break;
					case COLUMN_RANK_ID:
						row.add( rank_name_result.rank_id );
						break;
					case COLUMN_KINGDOM_ID:
						row.add( rank_name_result.kingdom_id );
						break;
					}
				}
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			return c;
		} else if (	uri.equals(TaxonSearch.Itis.VERNACULAR_BY_TSN_URI) ) {

			MatrixCursor c = new MatrixCursor(projection);
			
			try {

				long tsn = Long.parseLong(selection);
				
				
				List<CommonNameSearchResult> common_name_results;

				DatabaseTaxonomy database_helper = new DatabaseTaxonomy(getContext());
	    		String vernacular = database_helper.get_vernaculars(tsn);
	    		

//	    		Log.i(TAG + "-Provider", "merged database vernac: " + vernacular);
	    		
	    		if (vernacular != null) {
	    			common_name_results = new ArrayList<CommonNameSearchResult>();
		    		for (String vern : vernacular.split(",")) {
		    			
		    			CommonNameSearchResult cnsr = new CommonNameSearchResult();
		    			cnsr.common_name = vern;
		    			
		    			common_name_results.add(cnsr);
		    		}
	    		} else {
	    			common_name_results = ItisQueryGenerator.getCommonNamesFromTSN(tsn);
	    			
	    			
	    			String combined_names = AsyncTaxonInfoPopulator.filterCombineCommonNames(common_name_results);
	    	    	

//		    		Log.i(TAG + "-Provider", "merged network vernac: " + combined_names);
	    			
	    			// Now stock the database with this information to avoid future network traffic.
	    			database_helper.set_vernaculars(tsn, combined_names);
	    		}
				
				
		 		Map<String, CommonNameResultColumn> column_fix = new HashMap<String, CommonNameResultColumn>();
		 		List<String> standard_colnames = Arrays.asList(TaxonSearch.Itis.COMMON_NAME_RESULT_COLUMN_NAMES);
				for (String colname : projection)
					column_fix.put(colname,
						CommonNameResultColumn.values()[standard_colnames.indexOf(colname)]);

		 		
				long id_counter = 0;
				for (CommonNameSearchResult result : common_name_results) {

					MatrixCursor.RowBuilder row = c.newRow();
					for (String colname : projection) {
					
						switch (column_fix.get(colname)) {
						case _ID:
							row.add(id_counter);
							break;
						case COLUMN_COMMON_NAME:
							row.add( result.common_name );
							break;
						case COLUMN_LANGUAGE:
							row.add( result.language );
							break;
						case COLUMN_TSN:
							row.add( result.tsn );
							break;
						}
					}
					id_counter++;
				}
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			return c;
			
			
			
			
		} else if (uri.equals(TaxonSearch.Itis.SCIENTIFIC_SEARCH_URI)
		) {

			MatrixCursor c = new MatrixCursor(projection);
			
			try {
				
				List<ScientificNameSearchResult> common_name_results = ItisQueryGenerator.searchByScientificName(selection);
				
		 		Map<String, ScientificNameResultColumn> column_fix = new HashMap<String, ScientificNameResultColumn>();
		 		List<String> standard_colnames = Arrays.asList(TaxonSearch.Itis.SCIENTIFIC_NAME_RESULT_COLUMN_NAMES);
				for (String colname : projection)
					column_fix.put(colname,
							ScientificNameResultColumn.values()[standard_colnames.indexOf(colname)]);

		 		
				long id_counter = 0;
				for (ScientificNameSearchResult result : common_name_results) {

					MatrixCursor.RowBuilder row = c.newRow();
					for (String colname : projection) {
					
						switch (column_fix.get(colname)) {
						case _ID:
							row.add(id_counter);
							break;
						case COLUMN_COMBINED_NAME:
							row.add( result.combined_name );
							break;
						case COLUMN_UNIT1:
							row.add( result.unit1 );
							break;
						case COLUMN_UNIT2:
							row.add( result.unit2 );
							break;
						case COLUMN_UNIT3:
							row.add( result.unit3 );
							break;
						case COLUMN_UNIT4:
							row.add( result.unit4 );
							break;
						case COLUMN_GENUS:
							row.add( result.genus );
							break;
						case COLUMN_SPECIES:
							row.add( result.species );
							break;
						case COLUMN_SUBSPECIES:
							row.add( result.subspecies );
							break;
						case COLUMN_SUBSUBSPECIES:
							row.add( result.subsubspecies );
							break;								
						case COLUMN_TSN:
							row.add( result.tsn );
							break;
						}
					}
					id_counter++;
				}
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				return null;
			}

			return c;
		}
		
		return null;
	}

}
