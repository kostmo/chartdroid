package org.crittr.shared.browser.containers;

import org.crittr.shared.browser.Constants;

public class KingdomRankIdPair {
	
	public int kingdom_id = Constants.UNKNOWN_ITIS_KINGDOM;
	public int rank_id = Constants.INVALID_RANK_ID;
	
	public boolean isInvalid() {
		
		return kingdom_id == Constants.UNKNOWN_ITIS_KINGDOM || rank_id == Constants.INVALID_RANK_ID;
	}
	
	public KingdomRankIdPair(int kingdom_id, int rank_id) {
		this.kingdom_id = kingdom_id;
		this.rank_id = rank_id;
	}
	
	
	public KingdomRankIdPair() {
	}
	
	@Override
	public String toString() {
		return "kingdom_id: " + kingdom_id + "; rank_id: " + rank_id;
	}
}