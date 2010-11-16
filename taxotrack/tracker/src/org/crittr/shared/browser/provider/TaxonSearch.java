package org.crittr.shared.browser.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


public final class TaxonSearch {
	
    public static final String AUTHORITY = "org.crittr.provider.TaxonSearch";

    public static final String _ID = "_id";


    public static final String CONTENT_TYPE_BASE_SINGLE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/";
    public static final String CONTENT_TYPE_BASE_MULTIPLE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/";
    
    
    public static final class TaxonSuggest implements BaseColumns {

        public static final Uri SCIENTIFIC_AUTOCOMPLETE_URI = Uri.parse("content://" + AUTHORITY + "/suggestions/scientific");
        public static final Uri VERNACULAR_AUTOCOMPLETE_URI = Uri.parse("content://" + AUTHORITY + "/suggestions/vernacular");
        

        
        
        public static final String COLUMN_SUGGESTION = "COLUMN_SUGGESTION";



        public static final String CONTENT_TYPE_SUGGESTION = CONTENT_TYPE_BASE_MULTIPLE + "vnd.org.crittr.suggestion";

        
    }
    
    public static final class Itis implements BaseColumns {
    	
    	

    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
        // ==== STOCK URIs ====    	

    	public static final Uri SCIENTIFIC_NAME_RESULT = Uri.parse("content://" + AUTHORITY + "/itis/scientific_name_result");
//        public static final Uri COMMON_NAME_RESULT = Uri.parse("content://" + AUTHORITY + "/itis/common_name_result");
        public static final Uri HIERARCHY_FULL_URI = Uri.parse("content://" + AUTHORITY + "/itis/hierarchy/full");
        public static final Uri HIERARCHY_DOWN_URI = Uri.parse("content://" + AUTHORITY + "/itis/hierarchy/down");
        public static final Uri HIERARCHY_UP_URI = Uri.parse("content://" + AUTHORITY + "/itis/hierarchy/up");
        
        public static final Uri SCIENTIFIC_SEARCH_URI = Uri.parse("content://" + AUTHORITY + "/itis/scientific/search");
        public static final Uri VERNACULAR_BEGINS_SEARCH_URI = Uri.parse("content://" + AUTHORITY + "/itis/vernacular/search/begins");
        public static final Uri VERNACULAR_ENDS_SEARCH_URI = Uri.parse("content://" + AUTHORITY + "/itis/vernacular/search/ends");
        public static final Uri VERNACULAR_CONTAINS_SEARCH_URI = Uri.parse("content://" + AUTHORITY + "/itis/vernacular/search/contains");
        
        public static final Uri VERNACULAR_BY_TSN_URI = Uri.parse("content://" + AUTHORITY + "/itis/vernacular/tsn");
        public static final Uri PARENT_TSN_BY_TSN_URI = Uri.parse("content://" + AUTHORITY + "/itis/tsn/parent");
        public static final Uri RANK_NAME_BY_TSN_URI = Uri.parse("content://" + AUTHORITY + "/itis/ranks/tsn");
        
        public static final Uri RANK_NAMES_URI = Uri.parse("content://" + AUTHORITY + "/itis/ranks");
        

		// TODO: FINISH ME
        // ==== CONTENT TYPE IDs ====
    	
        public static final int NO_MATCH = 0;
        
    	public static final int HIERACHRY_FULL = 1;
        public static final int HIERACHRY_UP = 2;
        public static final int HIERACHRY_DOWN = 3;
        public static final int TAXON_SUGGEST = 4;
        public static final int TAXON_SEARCH = 5;
        public static final int TSN_PARENT = 7;
        public static final int RANK = 8;
        public static final int RANK_TSN = 9;
        public static final int VERNACULAR_BEGINS = 10;
        public static final int VERNACULAR_ENDS = 11;
        public static final int VERNACULAR_CONTAINS = 12;
        public static final int VERNACULAR_TSN = 13;

        
        // ==== CONTENT TYPES ====
        
        public static final String CONTENT_TYPE_HIERARCHY = CONTENT_TYPE_BASE_MULTIPLE + "vnd.org.crittr.hierarchy";
        public static final String CONTENT_TYPE_ITEM_HIERARCHY = CONTENT_TYPE_BASE_SINGLE + "vnd.org.crittr.hierarchy";
        
        public static final String CONTENT_TYPE_RANK = CONTENT_TYPE_BASE_MULTIPLE + "vnd.org.crittr.rank";
        public static final String CONTENT_TYPE_ITEM_RANK = CONTENT_TYPE_BASE_SINGLE + "vnd.org.crittr.rank";
 
        public static final String CONTENT_TYPE_VERNACULAR = CONTENT_TYPE_BASE_MULTIPLE + "vnd.org.crittr.vernacular";
        public static final String CONTENT_TYPE_ITEM_VERNACULAR = CONTENT_TYPE_BASE_SINGLE + "vnd.org.crittr.vernacular";
    
        public static final String CONTENT_TYPE_TAXON_INFO = CONTENT_TYPE_BASE_MULTIPLE + "vnd.org.crittr.taxon";
        public static final String CONTENT_TYPE_ITEM_TAXON_INFO = CONTENT_TYPE_BASE_SINGLE + "vnd.org.crittr.taxon";
        
        public static final String CONTENT_TYPE_ITEM_TSN = CONTENT_TYPE_BASE_SINGLE + "vnd.org.crittr.tsn";


        
        
        
        
        
        public static final String COLUMN_COMMON_NAME = "COLUMN_COMMON_NAME";
        public static final String COLUMN_LANGUAGE = "COLUMN_LANGUAGE";
        public static final String COLUMN_TSN = "COLUMN_TSN";
        

        public static final String COLUMN_COMBINED_NAME = "COLUMN_COMBINED_NAME";
        public static final String COLUMN_UNIT1 = "COLUMN_UNIT1";
        public static final String COLUMN_UNIT2 = "COLUMN_UNIT2";
        public static final String COLUMN_UNIT3 = "COLUMN_UNIT3";
        public static final String COLUMN_UNIT4 = "COLUMN_UNIT4";
        public static final String COLUMN_GENUS = "COLUMN_GENUS";
        public static final String COLUMN_SPECIES = "COLUMN_SPECIES";
        public static final String COLUMN_SUBSPECIES = "COLUMN_SUBSPECIES";
        public static final String COLUMN_SUBSUBSPECIES = "COLUMN_SUBSUBSPECIES";

        public static final String COLUMN_RANK_NAME = "COLUMN_RANK_NAME";
        public static final String COLUMN_RANK_ID = "COLUMN_RANK_ID";
        public static final String COLUMN_KINGDOM_NAME = "COLUMN_KINGDOM_NAME";
        public static final String COLUMN_KINGDOM_ID = "COLUMN_KINGDOM_ID";

        public static final String[] HIERARCHY_RESULT_COLUMN_NAMES = {
    			TaxonSearch._ID,
    			TaxonSearch.Itis.COLUMN_PARENT_TAXON_NAME,
    			TaxonSearch.Itis.COLUMN_RANK_NAME,
    			TaxonSearch.Itis.COLUMN_TAXON_NAME,
    			TaxonSearch.Itis.COLUMN_TSN,
    			TaxonSearch.Itis.COLUMN_PARENT_TSN};

        public enum HeirarchyColumn {
        	_ID,
        	COLUMN_PARENT_TAXON_NAME,
        	COLUMN_RANK_NAME,
        	COLUMN_TAXON_NAME,
        	COLUMN_TSN,
        	COLUMN_PARENT_TSN
        }

        public static final String[] RANK_RESULT_COLUMN_NAMES = {
			_ID,
			COLUMN_RANK_NAME,
			COLUMN_RANK_ID,
			COLUMN_KINGDOM_NAME,
			COLUMN_KINGDOM_ID};

	    public enum RankResultColumn {
	    	_ID,
			COLUMN_RANK_NAME,
			COLUMN_RANK_ID,
			COLUMN_KINGDOM_NAME,
			COLUMN_KINGDOM_ID
	    }
	    
        public static final String[] COMMON_NAME_RESULT_COLUMN_NAMES = {
			_ID,
			COLUMN_COMMON_NAME,
			COLUMN_LANGUAGE,
			COLUMN_TSN};

	    public enum CommonNameResultColumn {
	    	_ID,
	    	COLUMN_COMMON_NAME,
	    	COLUMN_LANGUAGE,
	    	COLUMN_TSN
	    }
	    
        public static final String[] SCIENTIFIC_NAME_RESULT_COLUMN_NAMES = {
			_ID,
			COLUMN_COMBINED_NAME,
	        COLUMN_UNIT1,
	        COLUMN_UNIT2,
	        COLUMN_UNIT3,
	        COLUMN_UNIT4,
	        COLUMN_GENUS,
	        COLUMN_SPECIES,
	        COLUMN_SUBSPECIES,
	        COLUMN_SUBSUBSPECIES,
	        COLUMN_TSN};

	    public enum ScientificNameResultColumn {
			_ID,
			COLUMN_COMBINED_NAME,
	        COLUMN_UNIT1,
	        COLUMN_UNIT2,
	        COLUMN_UNIT3,
	        COLUMN_UNIT4,
	        COLUMN_GENUS,
	        COLUMN_SPECIES,
	        COLUMN_SUBSPECIES,
	        COLUMN_SUBSUBSPECIES,
	        COLUMN_TSN
	    }
	    
	    
	    
        
        public static final String COLUMN_PARENT_TAXON_NAME = "COLUMN_PARENT_TAXON_NAME";
        public static final String COLUMN_TAXON_NAME = "COLUMN_TAXON_NAME";
        public static final String COLUMN_PARENT_TSN = "COLUMN_PARENT_TSN";

        
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.kostmo.suggestion";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.kostmo.suggestion";
    }
}
