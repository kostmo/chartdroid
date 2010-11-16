package org.crittr.shared.browser.containers;

import java.util.ArrayList;

import org.crittr.shared.browser.Constants;

import android.net.Uri;

public class TaxonInfo implements Comparable<TaxonInfo> {
	public String taxon_name;
    public String rank_name;
    public String vernacular_name;
    public int rank_id = Constants.INVALID_RANK_ID;
    public int itis_kingdom_id = Constants.UNKNOWN_ITIS_KINGDOM; 
    public long tsn = Constants.INVALID_TSN;
    public long parent_tsn = Constants.UNKNOWN_PARENT_ID;
    
    public int frequency = 0;

    ArrayList<Uri> thumnail_uri_list;

    // Facilitates sort by descending popularity,
    // otherwise by descending rank,
    // otherwise by the presence of vernacular name.
    
    @Override
	public int compareTo(TaxonInfo another) {
		int by_popularity = new Integer(another.frequency).compareTo(this.frequency);
		
//		Log.d("foo", "Comparison result of TSN " + this.tsn + " (pop " + this.frequency + ") vs. TSN " + another.tsn + " (pop " + another.frequency + "): " + by_popularity);
		
		if (by_popularity != 0) return by_popularity;
		int by_rank = new Integer(another.rank_id).compareTo(this.rank_id);
		if (by_rank != 0) return by_rank;
		
		if (another.vernacular_name != null && this.vernacular_name != null)
		return another.vernacular_name.compareTo(this.vernacular_name);
		
		return 0;
	}
    
    public KingdomRankIdPair getIkrp() {
    	return new KingdomRankIdPair(itis_kingdom_id, rank_id);
    }
    
    
    
    
    
    
    

    public int preferred_thumbnail_index = 0;
    
    public int incrementPreferredThumbnailIndex() {

    	preferred_thumbnail_index++;
    	return preferred_thumbnail_index;
    }
    
    public int getPreferredThumbnailIndex() {
    	return preferred_thumbnail_index;
    }
}