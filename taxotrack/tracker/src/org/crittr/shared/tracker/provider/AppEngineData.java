package org.crittr.shared.tracker.provider;

import android.net.Uri;
import android.provider.BaseColumns;


public final class AppEngineData {
	
    public static final String AUTHORITY = "org.crittr.provider.AppEngineData";

    public static final String _ID = "_id";

    
    public static final class TaxoData implements BaseColumns {

        public static final Uri TAXON_POPULARITY_URI = Uri.parse("content://" + AUTHORITY + "/popularity");

        public static final String COLUMN_TAXON_HITS = "COLUMN_TAXON_HITS";
        public static final String COLUMN_TSN = "COLUMN_TSN";
        


    }
}
