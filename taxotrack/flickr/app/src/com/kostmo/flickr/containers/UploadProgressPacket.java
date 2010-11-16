package com.kostmo.flickr.containers;

public class UploadProgressPacket {
	public int progress_value, max_value;
	public String error_message;
	public long eta_millis;
	
	public UploadProgressPacket(int finished, int total) {
		this.progress_value = finished;
		this.max_value = total;
	}

	public UploadProgressPacket(String err) {
		this(-1, -1);
		this.error_message = err;
	}
}
