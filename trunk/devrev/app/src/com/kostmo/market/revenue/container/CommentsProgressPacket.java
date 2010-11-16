package com.kostmo.market.revenue.container;

public class CommentsProgressPacket {
	
	public enum CommentProgressStage {
		COMMENT_FETCHING, READY, FAIL, CANCEL
	};
	
	public CommentProgressStage stage;
	public int progress_value;
	public int max_value;
	public int current_step;
	public int max_steps;
	public String message;
	public CommentsProgressPacket(CommentProgressStage stage, int progress_value, int max_value, String message) {
		this.stage = stage;
		this.progress_value = progress_value;
		this.max_value = max_value;
		this.message = message;
	}
	public CommentsProgressPacket() {
		// TODO Auto-generated constructor stub
	}
}
