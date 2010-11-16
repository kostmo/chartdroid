package org.crittr.containers;

public class SearchProgressPacket {
	
	public enum SearchStage {
	    SEARCHING, RANK_FILTERING, DIALOG_CANCELLATION
	};
	
	public SearchStage stage;
	public int stage_progress_max;
	public int stage_current_progress = 0;
	
	
	public SearchProgressPacket(SearchStage s) {
		stage = s;
	}
}