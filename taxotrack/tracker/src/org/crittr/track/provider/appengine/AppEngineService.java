package org.crittr.track.provider.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.crittr.track.Market;
import org.crittr.track.R;
import org.crittr.track.provider.appengine.ServiceCallbackContext.CallbackPayload;
import org.crittr.track.retrieval_tasks.NetworkUnavailableException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


public class AppEngineService extends Service {

    public static final int APPENGINE_FETCH_RETURN_CODE = 1;
	
	
	static final String TAG = Market.DEBUG_TAG;
	
	SharedPreferences settings;
	
    private NotificationManager mNM;

	public class AppEngineJob {
		public Uri target_uri;
		public int callback_id;
		public Context context;
		public String wait_message;
	}

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public AppEngineService getService() {
            return AppEngineService.this;
        }
    }
    
    @Override
    public void onCreate() {
    	
    	settings = PreferenceManager.getDefaultSharedPreferences(this);
    	
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
//        showNotification();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.local_service_started);

        // Tell the user we stopped.
//        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    
    
    private Cookie bake_cookie(String cookie_auth_key) {
    	
    	
		String cookie_name = settings.getString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_NAME, null);
    	String cookie_domain = settings.getString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_DOMAIN, null);
		String cookie_path = settings.getString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_PATH, null);
    	
		BasicClientCookie cookie = new BasicClientCookie(cookie_name, cookie_auth_key);
		cookie.setDomain(cookie_domain);
		cookie.setPath(cookie_path);
		
		return cookie;
    }
    
    
    
	// The Activity need not be instantiated if no user input is required.
	// The target_uri encodes its payload in the form of URL query name/value pairs.
    
    
    Stack<AppEngineJob> appengine_request_queue = new Stack<AppEngineJob>();

	public void putData(Context context, ServiceCallbackContext callback_context, int callback_id, String wait_message, Uri target_uri) {
		
		AppEngineJob job = new AppEngineJob();
		job.callback_id = callback_id;
		job.target_uri = target_uri;
		job.context = context;
		job.wait_message = wait_message;
		appengine_request_queue.push(job);
		


		
		
		// 1) Check whether we have the cookie.
		//    If we have the cookie, we process the queue immediately.
		//    If not, we rely on the cookie-fetching activity to trigger the queue processing.
		
		
//		String saved_auth_string = settings.getString(AppEngineLogin.PREFKEY_APPENGINE_AUTH, null);
		
		String cookie_value = settings.getString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_VALUE, null);

		Log.d(TAG, "Checking for existing cookie...");
        if (cookie_value != null) {

        	
        	Log.d(TAG, "We have the cookie!");
        	
        	Log.e(TAG, "Baking our cookie, using it to retrieve data...");
        	process_fetch_queue( bake_cookie(cookie_value) );
        	
        	
        	
        	

        } else {
        	
//        	finish_with_authorization_string(saved_auth_string);

        	// TODO: We may want to have a wrapper around the data acquisition,
        	// to account for the expiration of the authentication key.
        	// Then we can prompt the user to log in again.
        	
        	
        	Log.d(TAG, "Starting AppEngine Activity to retrieve cookie...");
        	
        	Intent i = new Intent();
        	i.setClass(this, AppEngineLogin.class);
        	i.setData( target_uri );
        	job.context.startActivity(i);
        	
        	// Note: We expect the AppEngineLogin activity to execute our callback once it
        	// has retrieved the cookie...
        }

		
		// 2) Upload the data.
		
		
		// 3) If we get a "failure" message, then maybe our cookie has expired.
		//		We attempt to log in again, then retransmit the data.
		
	}
    

	void process_fetch_queue( Cookie cookie ) {
		
		while ( !appengine_request_queue.empty() ) {

			AppEngineJob job = appengine_request_queue.pop();

			new AsyncAppEngineFetchTask(job, cookie).execute();
		}
	}
	
	
	
	String fetch_data_with_cookie(Uri target_uri, Cookie cookie) throws NetworkUnavailableException, AuthenticationExpiredException {
		
		
		Log.d(TAG, "Performing AppEngine action with cookie...");
		
		
    	InputStream is2 = null;
	

    	// TODO: This could just as easily be HttpGet - make it parameterizable
    	
    	Log.d(TAG, "Making HttpPost:");
    	Log.d(TAG, target_uri.toString() );
    	HttpPost httppost = new HttpPost( target_uri.toString() );
    	
    	HttpResponse response = null;
    	
    	DefaultHttpClient httpclient = new DefaultHttpClient();
    	httpclient.getCookieStore().addCookie(cookie);
    	
		try {
			response = httpclient.execute(httppost);

			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			
			throw new NetworkUnavailableException();

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int status_code = response.getStatusLine().getStatusCode();
		
		Log.d(TAG, "AppEngine action http response code: " + status_code);
		if (status_code == 204) {
			
			Log.d(TAG, "Got a good response (empty, though)!");

		} else if (status_code == 200) {

			try {
				is2 = response.getEntity().getContent();
			} catch (IllegalStateException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			String fetched_data_string = "";
			
			BufferedReader buff_read2 = new BufferedReader( new InputStreamReader(is2) );
			
			try {
				String foo;
				while (true) {
					foo = buff_read2.readLine();
					if (foo == null) break;
//					Log.e(TAG, "Response line: " + foo);
					
					// TODO: This is rather inelegant; it might be better to pass
					// the BufferedReader object back to the parent activity...
					fetched_data_string += foo;
					
				}

				
	        	// TODO: Execute asynchronous callback in the host activity...
				return fetched_data_string;


				
			} catch (IOException e) {
				e.printStackTrace();
			}
			


		} else {
			Log.e(TAG, "Got a bad response: " + status_code);
			Log.e(TAG, response.getStatusLine().getReasonPhrase());
			
			// Invalidate the auth string
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_AUTH, null).commit();
			
			
			// TODO: Uncomment me when we get the re-login logic implemented
//			settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_VALUE, null).commit();
			
			
			throw new AuthenticationExpiredException();

			// This is not needed, since we don't check the intent if we don't explicitly set RESULT_OK
//	        Intent intent = new Intent();
//			setResult(Activity.RESULT_CANCELED, intent);
		}
		return null;

	}
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.pawprint_notification, text,
                System.currentTimeMillis());

        
/*
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LocalServiceController.class), 0);
*/
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                       text, null);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.local_service_started, notification);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    // ========================================================    
    
    public class AsyncAppEngineFetchTask extends AsyncTask<Void, String, String> {

    	ProgressDialog wait_dialog;
    	
	    CallbackPayload callback_payload;
    	
    	Uri target_uri;
    	Cookie cookie;
    	
    	AppEngineJob job;

		Context activity_context;
    	String wait_message;
    	AsyncAppEngineFetchTask(AppEngineJob job, Cookie cookie) {
    		
    		this.job = job;
    		this.target_uri = job.target_uri;
    		this.cookie = cookie;
    		this.activity_context = job.context;
    		this.wait_message = job.wait_message;
    		callback_payload = new CallbackPayload();
			callback_payload.callback_id = job.callback_id;
    	}

    	
		void instantiate_latent_wait_dialog() {

			wait_dialog = new ProgressDialog(activity_context);
			wait_dialog.setMessage("Recording photo in tracker...");
			wait_dialog.setIndeterminate(true);
			wait_dialog.setCancelable(false);
			wait_dialog.show();
		}

    	
    	
	    @Override
	    public void onPreExecute() {

			instantiate_latent_wait_dialog();
	    }
	    
		protected String doInBackground(Void... params) {

			
			
	    	try {
				return fetch_data_with_cookie(target_uri, cookie);
			} catch (NetworkUnavailableException e) {
				
				
				
				e.printStackTrace();
				

	    		publishProgress("No network connection!");
	    		
			} catch (AuthenticationExpiredException e) {

	    		publishProgress("AppEngine authenticaion may be expired.");

				e.printStackTrace();
				
				// FIXME - If there is *actually* a server error, this will continue
				// in an infinite loop.
				if (false) {
		        	Log.d(TAG, "Starting AppEngine Activity to REVALIDATE cookie...");
		        	
		        	Intent i = new Intent();
		        	i.setClass(job.context, AppEngineLogin.class);
		        	i.setData( target_uri );
		        	job.context.startActivity(i);
		        	
		        	// Since we failed, we need to add the job back onto the stack.
		        	appengine_request_queue.push(job);
				}
			}
			return null;
		}
		
	    @Override
	    public void onProgressUpdate(String... error_message) {

	    	Toast.makeText(AppEngineService.this, error_message[0], Toast.LENGTH_LONG).show();
	    }
		
	    @Override
	    public void onPostExecute(String result) {

	    	if (result != null) {
				callback_payload.payload = result;
				((ServiceCallbackContext) activity_context).serviceCallback(callback_payload);
	    	}

			wait_dialog.dismiss();
	    }
    }
}

