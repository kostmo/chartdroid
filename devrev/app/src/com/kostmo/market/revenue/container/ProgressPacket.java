package com.kostmo.market.revenue.container;

public class ProgressPacket {
	
	public enum DoneType {
		READY, FAILED, CANCELLED
	}
	
	public enum ProgressStage {
		RECORD_FETCHING, ITEM_ID_MATCHING, DONE
	};
	
	public DoneType done_type = null;
	
	public ProgressStage stage;
	public int progress_value;
	public int max_value;
	public long eta_millis;
	public String message;
	public ProgressPacket(ProgressStage stage, int progress_value, int max_value, String message) {
		this.stage = stage;
		this.progress_value = progress_value;
		this.max_value = max_value;
		this.message = message;
	}
}
