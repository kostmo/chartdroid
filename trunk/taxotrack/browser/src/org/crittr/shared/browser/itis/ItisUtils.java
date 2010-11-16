package org.crittr.shared.browser.itis;

public class ItisUtils {

	public static String QUERY_RETURN_CONTAINER = "return";

	public static String PARENT_NAME_TAG = "parentName";
	public static String PARENT_TSN_TAG = "parentTsn";
	public static String RANK_NAME_TAG = "rankName";
	public static String TAXON_NAME_TAG = "taxonName";
	public static String TSN_TAG = "tsn";
	
	public static String COMBINED_NAME_TAG = "combinedName";
	public static String UNIT_1_TAG = "unitInd1";
	public static String UNIT_2_TAG = "unitInd2";
	public static String UNIT_3_TAG = "unitInd3";
	public static String UNIT_4_TAG = "unitInd4";
	public static String GENUS_TAG = "unitName1";
	public static String SPECIES_TAG = "unitName2";
	public static String SUBSPECIES_TAG = "unitName3";
	public static String SUBSUBSPECIES_TAG = "unitName4";
	
	public static String GEOGRAPHIC_VALUE_TAG = "geographicValue";
	public static String UPDATE_DATE_TAG = "updateDate";
	
	public static String COMMON_NAME_TAG = "commonName";
	public static String LANGUAGE_TAG = "language";
	
	public static String KINGDOM_ID_TAG = "kingdomId";
	public static String KINGDOM_NAME_TAG = "kingdomName";
	public static String RANK_ID_TAG = "rankId";
	
	public static String GEOGRAPHIC_VALUES_TAG = "geographicValues";
	public static String LANGUAGE_NAMES_TAG = "languageNames";
	
	public static String COMMON_NAMES_TAG = "commonNames";
	public static String NAME_USAGE_TAG = "nameUsage";
	public static String SCIENTIFIC_NAME_TAG = "scientificName";
	
	
	// FUNCTION TABLE:
	public static String[] heirarchy_tags = {PARENT_NAME_TAG, PARENT_TSN_TAG, RANK_NAME_TAG, TAXON_NAME_TAG, TSN_TAG};
	public static String[] common_name_tags = {COMMON_NAME_TAG, LANGUAGE_TAG, TSN_TAG};
	public static String[] scientific_name_tags = {COMBINED_NAME_TAG, UNIT_1_TAG, UNIT_2_TAG, UNIT_3_TAG, UNIT_4_TAG, GENUS_TAG, SPECIES_TAG, SUBSPECIES_TAG, SUBSUBSPECIES_TAG, TSN_TAG};

	public static String[] getGeographicDivisionsFromTSN_tags = {GEOGRAPHIC_VALUE_TAG, UPDATE_DATE_TAG};
	public static String[] getTaxonomicRankNameFromTSN_tags = {KINGDOM_ID_TAG, KINGDOM_NAME_TAG, RANK_ID_TAG, RANK_NAME_TAG, TSN_TAG};

	public static String[] kingdom_names_tags = {KINGDOM_ID_TAG, KINGDOM_NAME_TAG};
	public static String[] rank_names_tags = {KINGDOM_ID_TAG, KINGDOM_NAME_TAG, RANK_ID_TAG, RANK_NAME_TAG};
	public static String[] itis_terms_tags = {COMMON_NAMES_TAG, NAME_USAGE_TAG, SCIENTIFIC_NAME_TAG, TSN_TAG};

	public static int[] FULL_KINGDOM_ID_LIST = {1, 2, 3, 4, 5, 6};
	public static String[] FULL_KINGDOM_NAME_LIST = {"Monera", "Protozoa", "Plantae", "Fungi", "Animalia", "Chromista"};
	public static String[] FULL_KINGDOM_VERNACULAR_LIST = {"monerans", "", "plants", "fungi", "animals", ""};
	public static long[] FULL_KINGDOM_TSN_LIST = {202420, 630577, 202422, 555705, 202423, 630578};

	
	public static int[] KINGDOM_ID_LIST = {5, 3, 4};
	public static String[] KINGDOM_NAME_LIST = {"Animalia", "Plantae", "Fungi"};
	public static String[] KINGDOM_VERNACULAR_LIST = {"animals", "plants", "fungi"};
	public static long[] KINGDOM_TSN_LIST = {202423, 202422, 555705};
	
	public static int KINGDOM_RANK = 10;
}
