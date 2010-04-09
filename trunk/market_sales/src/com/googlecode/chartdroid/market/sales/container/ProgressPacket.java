package com.googlecode.chartdroid.market.sales.container;

public class ProgressPacket {
	
	public enum ProgressStage {
		AUTHENTICATING, DOWNLOADING, STORING
	};
	
	public ProgressStage stage;
	public int progress_value;
	
	public ProgressPacket(int progress_value, ProgressStage stage) {
		this.stage = stage;
		this.progress_value = progress_value;
	}
}
