package org.crittr.track.containers;

import org.crittr.track.DatabaseSightings;

public class RankNameIdPair {
	
	public RankNameIdPair(String rank_name, int rank_id) {
		this.rank_name = rank_name;
		this.rank_id = rank_id;
	}
	
	public boolean fully_populated() {
		return rank_name != null && rank_id != DatabaseSightings.INVALID_RANK_ID;
	}
	
	public RankNameIdPair() {
	}
	
	public String rank_name = null;
	public int rank_id = DatabaseSightings.INVALID_RANK_ID;
}