package org.crittr.provider.itis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.shared.browser.itis.ItisUtils;
import org.crittr.shared.browser.itis.ItisObjects.CommonNameSearchResult;
import org.crittr.shared.browser.itis.ItisObjects.HierarchyResult;
import org.crittr.shared.browser.itis.ItisObjects.KingdomRankResult;
import org.crittr.shared.browser.itis.ItisObjects.ScientificNameSearchResult;
import org.crittr.task.NetworkUnavailableException;

import android.util.Log;


public class ItisQueryGenerator  {

	static final String TAG = "ITIS";

	public static List<ScientificNameSearchResult> searchByScientificName(String query_argument) throws NetworkUnavailableException {

		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"searchByScientificName",
				"scientificNames",
				query_argument,
				"srchKey",
				ItisUtils.scientific_name_tags)
		).parse();
		
		List<ScientificNameSearchResult> filtered_results = new ArrayList<ScientificNameSearchResult>();
		for (Map<String, String> hash : intermediate_result)
			if (hash.size() > 0)
				filtered_results.add(new ScientificNameSearchResult(hash));
		
		return filtered_results;
	}
	

	public static List<CommonNameSearchResult> searchByCommonName(String query_argument) throws NetworkUnavailableException {

		
		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
			"searchByCommonName",
			"commonNames",
			query_argument,
			"srchKey",
			ItisUtils.common_name_tags)
		).parse();
		
		List<CommonNameSearchResult> filtered_results = new ArrayList<CommonNameSearchResult>();
		for (Map<String, String> hash : intermediate_result)
			if (hash.size() > 0)
				filtered_results.add(new CommonNameSearchResult(hash));
		
		return filtered_results;
	}
	
	
	public static List<CommonNameSearchResult> searchByCommonNameBeginsWith(String query_argument) throws NetworkUnavailableException {

		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"searchByCommonNameBeginsWith",
				"commonNames",
				query_argument,
				"srchKey",
				ItisUtils.common_name_tags)
		).parse();
		
		
		
		List<CommonNameSearchResult> filtered_results = new ArrayList<CommonNameSearchResult>();
		for (Map<String, String> hash : intermediate_result)
			if (hash.size() > 0)
				filtered_results.add(new CommonNameSearchResult(hash));
		
		return filtered_results;
	}
	
	
	public static List<CommonNameSearchResult> searchByCommonNameEndsWith(String query_argument) throws NetworkUnavailableException {

		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"searchByCommonNameEndsWith",
				"commonNames",
				query_argument,
				"srchKey",
				ItisUtils.common_name_tags)
		).parse();

		List<CommonNameSearchResult> filtered_results = new ArrayList<CommonNameSearchResult>();
		for (Map<String, String> hash : intermediate_result)
			if (hash.size() > 0)
				filtered_results.add(new CommonNameSearchResult(hash));
		
		return filtered_results;
	}
	
	
	
	
	

	
	
	
	
	
	// New
	public static List<Map<String, String>> getITISTermsFromScientificName(String query_argument) throws NetworkUnavailableException {

		return (new QueryResponseParser(
				"getITISTermsFromScientificName",
				"itisTerms",
				query_argument,
				"srchKey",
				ItisUtils.itis_terms_tags)
		).parse();
	}
	
	// New
	public static List<Map<String, String>> getITISTermsFromCommonName(String query_argument) throws NetworkUnavailableException {

		return (new QueryResponseParser(
				"getITISTermsFromCommonName",
				"itisTerms",
				query_argument,
				"srchKey",
				ItisUtils.itis_terms_tags)
		).parse();
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	// New
	public static long getParentTSNFromTSN(long tsn) throws NetworkUnavailableException {

		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"getParentTSNFromTSN",
				null,
				Long.toString(tsn),
				"tsn",
				ItisUtils.heirarchy_tags)
		).parse(); 

//		Log.v(TAG, "getParentTSNFromTSN(" + tsn + ")");
//		print_results( intermediate_result );
		if (intermediate_result.size() > 0) {
			Map<String, String> first = intermediate_result.get(0);
			if (first.size() > 0) {
				String parent_tsn = first.get(ItisUtils.PARENT_TSN_TAG);
				if (parent_tsn != null && parent_tsn.length() > 0)
					return Long.parseLong( parent_tsn );
			}
		}
		return Constants.INVALID_TSN;
	}
	
	public static List<CommonNameSearchResult> getCommonNamesFromTSN(long tsn) throws NetworkUnavailableException {

		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"getCommonNamesFromTSN",
				"commonNames",
				Long.toString(tsn),
				"tsn",
				ItisUtils.common_name_tags)
		).parse();

		List<CommonNameSearchResult> filtered_results = new ArrayList<CommonNameSearchResult>();
		for (Map<String, String> hash : intermediate_result)
			if (hash.size() > 0)
				filtered_results.add(new CommonNameSearchResult(hash));
		
		return filtered_results;
	}
	
	
	
	
	
	
	
	
	// ===================================
	// Helper function:
	
	public static List<TaxonInfo> convertHierarchyResultToTaxonInfo(List<HierarchyResult> result_set) {

		List<TaxonInfo> taxon_ladder = new ArrayList<TaxonInfo>();
		for (HierarchyResult hr : result_set) {
			
			if (hr.parent_tsn != Constants.INVALID_TSN) {
				TaxonInfo ti = new TaxonInfo();

    			ti.tsn = hr.tsn;
    			ti.taxon_name = hr.taxon_name;
    			ti.parent_tsn = hr.parent_tsn;
    			taxon_ladder.add(ti);
			}
		}

		return taxon_ladder;
	}

	// ===================================
	// Helper function:
	
	public static List<HierarchyResult> convertTaxonInfoToHierarchyResult(List<TaxonInfo> full_chain) {
	
		// Convert the list back into a list of HeirarchyResult's
		List<HierarchyResult> hierarcy = new ArrayList<HierarchyResult>();
		for (TaxonInfo ti : full_chain) {
			
			HierarchyResult hr = new HierarchyResult();
			hr.parent_tsn = ti.parent_tsn;
			hr.tsn = ti.tsn;
			hr.taxon_name = ti.taxon_name;
			hr.rank_name = ti.rank_name;	// This one might not be available
			
			hierarcy.add(hr);
		}
		
		return hierarcy;
	}
	


	public static List<HierarchyResult> getFullHierarchyFromTSN(long tsn) throws NetworkUnavailableException {

		List<HierarchyResult> filtered_results = new ArrayList<HierarchyResult>();

		// Bypass an idiosyncrasy with ITIS's Kingdoms handling.
		int kingdom_place = -1;
		for (int i=0; i<ItisUtils.KINGDOM_TSN_LIST.length; i++)
			if (tsn == ItisUtils.KINGDOM_TSN_LIST[i]) {
				kingdom_place = i;
				break;
			}

		if ( kingdom_place >= 0 ) {

			HierarchyResult hr = new HierarchyResult();
			hr.taxon_name = ItisUtils.KINGDOM_NAME_LIST[kingdom_place];
			hr.tsn = tsn;
			hr.rank_name = "Kingdom";
			hr.parent_tsn = Constants.NO_PARENT_ID;
			filtered_results.add(hr);

		} else {
			List<Map<String, String>> intermediate_result = (new QueryResponseParser(
					"getFullHierarchyFromTSN",
					"hierarchyList",
					Long.toString(tsn),
					"tsn",
					ItisUtils.heirarchy_tags)
			).parse();
			
			
			for (Map<String, String> hash : intermediate_result)
				if (hash.size() > 0)
					filtered_results.add( new HierarchyResult(hash) );
		}
		
		return filtered_results;
	}
	
	
	
	
	
	
	// Note: Does not return the "rankName" (ITIS returns "NIL" for that field)
	public static List<HierarchyResult> getHierarchyDownFromTSN(long tsn) throws NetworkUnavailableException {



		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"getHierarchyDownFromTSN",
				"hierarchyList",
				Long.toString(tsn),
				"tsn",
				ItisUtils.heirarchy_tags)
		).parse();
		

		List<HierarchyResult> filtered_results = new ArrayList<HierarchyResult>();
		for (Map<String, String> hash : intermediate_result)
			if (hash.size() > 0)
				filtered_results.add( new HierarchyResult(hash) );


//		Log.d(TAG, "Size of result: " + filtered_results.size());
		
		return filtered_results;
	}
	
	
	public static List<HierarchyResult> getHierarchyUpFromTSN(long tsn) throws NetworkUnavailableException {

		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"getHierarchyUpFromTSN",
				null,
				Long.toString(tsn),
				"tsn",
				ItisUtils.heirarchy_tags)
		).parse();
		

		List<HierarchyResult> filtered_results = new ArrayList<HierarchyResult>();
		for (Map<String, String> hash : intermediate_result)
			if (hash.size() > 0)
				filtered_results.add( new HierarchyResult(hash) );


		return filtered_results;
	}	
	
	
	public static List<Map<String, String>> getGeographicDivisionsFromTSN(long tsn) throws NetworkUnavailableException {

		return (new QueryResponseParser(
				"getGeographicDivisionsFromTSN",
				"geoDivisions",
				Long.toString(tsn),
				"tsn",
				ItisUtils.getGeographicDivisionsFromTSN_tags)
		).parse();
	}

	
	public static KingdomRankResult getTaxonomicRankNameFromTSN(long tsn) throws NetworkUnavailableException {

		
		List<Map<String, String>> rank_name_hashes = (new QueryResponseParser(
				"getTaxonomicRankNameFromTSN",
				null,
				Long.toString(tsn),
				"tsn",
				ItisUtils.getTaxonomicRankNameFromTSN_tags)
		).parse();

//		Log.v(TAG, "getTaxonomicRankNameFromTSN(" + tsn + ")");
//		print_results( rank_name_hashes );
		

		KingdomRankResult kingdom_rank = new KingdomRankResult();
		
		if (rank_name_hashes.size() > 0) {

			String supposed_kingdom_id_string = rank_name_hashes.get(0).get(ItisUtils.KINGDOM_ID_TAG);
			if (supposed_kingdom_id_string != null && supposed_kingdom_id_string.length() > 0)
				kingdom_rank.kingdom_id = Integer.parseInt( supposed_kingdom_id_string );

			String supposed_rank_id_string = rank_name_hashes.get(0).get(ItisUtils.RANK_ID_TAG);
			if (supposed_rank_id_string != null && supposed_rank_id_string.length() > 0)
				kingdom_rank.rank_id = Integer.parseInt( supposed_rank_id_string );
			
			kingdom_rank.kingdom_name = rank_name_hashes.get(0).get(ItisUtils.KINGDOM_NAME_TAG);
			kingdom_rank.rank_name = rank_name_hashes.get(0).get(ItisUtils.RANK_NAME_TAG);
		}

		return kingdom_rank;
	}

	
	public static String getScientificNameFromTSN(long tsn) throws NetworkUnavailableException {

		List<Map<String, String>> intermediate_result = (new QueryResponseParser(
				"getScientificNameFromTSN",
				null,
				Long.toString(tsn),
				"tsn",
				ItisUtils.scientific_name_tags)
		).parse();

//		Log.v(TAG, "getScientificNameFromTSN(" + tsn + ")");
//		print_results( intermediate_result );
		
		if (intermediate_result.size() > 0) {
			String taxon_name_from_network = intermediate_result.get(0).get(ItisUtils.COMBINED_NAME_TAG); 
//			Log.d(TAG, "Taxon name from network: " + taxon_name_from_network);
			return taxon_name_from_network;
		}
		
		return null;
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	

	public static List<Map<String, String>> getGeographicValues() throws NetworkUnavailableException {

		return (new QueryResponseParser(
				"getGeographicValues",
				ItisUtils.GEOGRAPHIC_VALUES_TAG,
				null,
				null,
				null)
		).parse();
	}

	
	public static List<Map<String, String>> getVernacularLanguages() throws NetworkUnavailableException {

		return (new QueryResponseParser(
				"getVernacularLanguages",
				ItisUtils.LANGUAGE_NAMES_TAG,
				null,
				null,
				null)
		).parse();
	}
	

	public static List<KingdomRankResult> getRankNames() throws NetworkUnavailableException {

		List<Map<String, String>> rank_names_hashes = (new QueryResponseParser(
				"getRankNames",
				"rankNames",
				null,
				null,
				ItisUtils.rank_names_tags)
		).parse();
		
		List<KingdomRankResult> ranks = new ArrayList<KingdomRankResult>();
		for (Map<String, String> hash : rank_names_hashes)
			ranks.add( new KingdomRankResult(hash) );
			
		return ranks;
	}
	
	
	public static List<Map<String, String>> getKingdomNames() throws NetworkUnavailableException {

		return (new QueryResponseParser(
				"getKingdomNames",
				"kingdomNames",
				null,
				null,
				ItisUtils.kingdom_names_tags)
		).parse();
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Support functions:
	
	public static String get_vernacular_from_scientific(String search_term) throws NetworkUnavailableException {
		
		List<Map<String, String>> result_set = getITISTermsFromScientificName(search_term);
		// Get only exact matches:
		for (Map<String, String> dic : result_set) {
			
			String sci_name = dic.get(ItisUtils.SCIENTIFIC_NAME_TAG);
			
			if (sci_name != null && sci_name.equalsIgnoreCase(search_term)) {
//			if (sci_name != null && sci_name.trim().equals(search_term)) {
				
//				Log.i(TAG, "We have a match: " + sci_name);
				
				String common_name = dic.get(ItisUtils.COMMON_NAMES_TAG);	
				if (common_name != null && common_name.trim().length() > 0)
					return common_name;
			}
		}
		
		return null;
	}				           
	
	public static void print_results(List<Map<String, String>> results) {
		
		Log.w(TAG, "Item count: " + results.size());
		for (Map<String, String> result_group : results) {
			Log.d(TAG, "============ Item ============ field count: " + result_group.size());
			for (String key : result_group.keySet()) {
				Log.i(TAG, key + ": " + result_group.get(key));
			}
		}
	}
	
	public static void test_functions() {


		/*
		Log.v(TAG, "getHierarchyUpFromTSN()");
		print_results( getHierarchyUpFromTSN(1378) );
		
		Log.v(TAG, "getHierarchyDownFromTSN()");
		print_results( getHierarchyDownFromTSN(202420) );
		
		Log.v(TAG, "getFullHierarchyFromTSN()");
		print_results( getFullHierarchyFromTSN(180543) );

		Log.v(TAG, "getGeographicDivisionsFromTSN()");
		print_results( getGeographicDivisionsFromTSN(180543) );
		
		Log.v(TAG, "getCommonNamesFromTSN()");
		print_results( getCommonNamesFromTSN(552479) );	// Cougar - a good example
		
		Log.v(TAG, "getTaxonomicRankNameFromTSN()");
		print_results( getTaxonomicRankNameFromTSN(531894) );
		
		Log.v(TAG, "getScientificNameFromTSN()");
		print_results( getScientificNameFromTSN(202385) );

		

		Log.v(TAG, "getKingdomNames()");
		print_results( getKingdomNames() );
		
		Log.v(TAG, "getRankNames()");
		print_results( getRankNames() );
		
		Log.v(TAG, "getVernacularLanguages()");
		print_results( getVernacularLanguages() );
		
		Log.v(TAG, "getGeographicValues()");
		print_results( getGeographicValues() );
		*/
	}
}

