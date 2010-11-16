package org.crittr.track.provider.appengine;


public interface ServiceCallbackContext {

	public class CallbackPayload {
		public String payload;
		public int callback_id;
	}
	
	public void serviceCallback(CallbackPayload result);
}
