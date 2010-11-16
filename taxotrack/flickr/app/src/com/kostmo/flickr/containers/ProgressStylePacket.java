package com.kostmo.flickr.containers;

public class ProgressStylePacket {
	
	public enum ProgressStyle {
	    LARGE_IMAGE_LOADING
	};
	
	public ProgressStyle style;
	
	
	public ProgressStylePacket(ProgressStyle s) {
		style = s;
	}
}