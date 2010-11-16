/*
 * 
 * This class should be instantiated and left around;
 * Whenever the application needs to interact with the Birdroid Server,
 * start the activity with an Intent for a return value.
 * 
 */

package org.crittr.track.provider.appengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.crittr.track.Market;
import org.crittr.track.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;





// Note: This activity is created for the sole purpose of collecting the cookie.
// Don't get distracted with other things!

public class AppEngineLogin extends Activity {

    // =============================================
    private boolean mIsBound;
    private AppEngineService mBoundService;
	
    
    
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((AppEngineService.LocalBinder)service).getService();
            

            
            
            // Instead of launching this in onCreate(), we must wait until our service is bound.
            Intent i = getIntent();
//          target_url = i.getExtras().getString(INTENT_EXTRA_DATA_REQUEST_URL);
            
            Uri passed_data = i.getData();
            if (passed_data != null)
	            target_url = passed_data.toString();
            else
    	      	target_url = "http://bugdroid.appspot.com/";
            


	      	Log.e(TAG, "Starting the auth/username snooper...");
	      	internal_username_snooper();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(AppEngineLogin.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    // =============================================

	
	
	static final String TAG = Market.DEBUG_TAG;

	
	
	public static final String INTENT_EXTRA_APPENGINE_AUTHORIZATION_STRING = "APPENGINE_AUTHORIZATION_STRING";
	
	
	public final static String PREFKEY_APPENGINE_USERNAME = "PREFKEY_APPENGINE_USERNAME";
	public final static String PREFKEY_APPENGINE_PASSWORD = "PREFKEY_APPENGINE_PASSWORD";
	public final static String PREFKEY_APPENGINE_AUTH = "PREFKEY_APPENGINE_AUTH";
	public final static String PREFKEY_APPENGINE_COOKIE_DOMAIN = "PREFKEY_APPENGINE_COOKIE_DOMAIN";
	public final static String PREFKEY_APPENGINE_COOKIE_PATH = "PREFKEY_APPENGINE_COOKIE_PATH";
	public final static String PREFKEY_APPENGINE_COOKIE_VALUE = "PREFKEY_APPENGINE_COOKIE_VALUE";
	public final static String PREFKEY_APPENGINE_COOKIE_NAME = "PREFKEY_APPENGINE_COOKIE_NAME";
	

	String target_url;
	

	private final int REQUEST_CODE_USERNAME_SNOOPER_CALLBACK_ID = 123;
	private final int REQUEST_CODE_AUTHTOKEN_SNOOPER_CALLBACK_ID = 7;
	
	
	// This authorization string should last for the session.

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

    	Log.d(TAG, "Executing onCreate() in AppEngineLogin");

        
        // Service is bound here in onCreate
        bindService(new Intent(this, 
                AppEngineService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
	
    // =============================================    
    
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w("Crittr", "Crittr onDestroy()");
	   

	    // Service is unbound here in onDestroy
	    if (mIsBound) {
        // Detach our existing connection.
           unbindService(mConnection);
           mIsBound = false;
        }

    }

    // =============================================  
	
    void finish_with_authorization_string(String authorization_string) {
    	
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_APPENGINE_AUTHORIZATION_STRING, authorization_string);
		setResult(Activity.RESULT_OK, intent);
		finish();
    }

	
	static void print_cookies(DefaultHttpClient client) {

		for (Cookie cook : client.getCookieStore().getCookies()) {
			Log.w(TAG, "A new cookie:");
			Log.i(TAG, "Domain: " + cook.getDomain());
			Log.i(TAG, "Name: " + cook.getDomain());
			Log.i(TAG, "Value: " + cook.getValue());
			Log.i(TAG, "Comment: " + cook.getComment());
			Log.i(TAG, "CommentURL: " + cook.getCommentURL());
			Log.i(TAG, "Path: " + cook.getPath());
			
			Log.i(TAG, "Version: " + cook.getVersion());
			Log.i(TAG, "Expiration date: " + cook.getExpiryDate());
		}
	}
	
	static Cookie storeCookie(Context context, DefaultHttpClient client) {
		
		for (Cookie cook : client.getCookieStore().getCookies()) {
			if (cook.getDomain().contains("appspot.com")) {
				
				Log.i(TAG, "Domain: " + cook.getDomain());
				Log.i(TAG, "Path: " + cook.getPath());
				Log.i(TAG, "Value: " + cook.getValue());
							
				
		        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
	
		        settings.edit().putString(PREFKEY_APPENGINE_COOKIE_DOMAIN, cook.getDomain()).commit();
		        settings.edit().putString(PREFKEY_APPENGINE_COOKIE_PATH, cook.getPath()).commit();
		        settings.edit().putString(PREFKEY_APPENGINE_COOKIE_VALUE, cook.getValue()).commit();
		        settings.edit().putString(PREFKEY_APPENGINE_COOKIE_NAME, cook.getName()).commit();
	        
		        return cook;
			}
		}
		return null;
	}
	
	public static void eraseLoginData(Context context) {
		
		Log.e(TAG, "Wiping Google Login data");
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_AUTH, null).commit();
		settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_USERNAME, null).commit();
		settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_PASSWORD, null).commit();
		
        settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_DOMAIN, null).commit();
        settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_PATH, null).commit();
        settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_VALUE, null).commit();
        settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_NAME, null).commit();
	}
	
    
	static Cookie fetch_cookie_from_authorization(Context context, String auth_string, String target_domain) {


    	InputStream is2 = null;
		
		Log.d(TAG, "Now attempting to retrieve cookie with auth string: " + auth_string);
		
		Uri full_uri = Uri.parse(target_domain);
		String host_part = full_uri.getScheme() + "://" + full_uri.getHost() + "/";
		
		
		// Note: This redirect only works with a "Get" HTTP request!!!
		// So the target URL must respond to Get, not Post.
//		String url_string = host_part + "_ah/login?auth=" + auth_string + "&continue=" + target_url;
		
		
		String url_string = host_part + "_ah/login?auth=" + auth_string;

		Log.d(TAG, "Using URL: " + url_string);
		
		// TODO: FIXME
//		return null;
		
//		/*
		
		
		

		
		
    	HttpPost httppost = new HttpPost( url_string );
    	
    	Cookie retrieved_cookie = null;
    	HttpResponse response = null;
    	DefaultHttpClient httpclient = new DefaultHttpClient();
    	
		try {
			response = httpclient.execute(httppost);


			
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			
			Toast.makeText(context, "No network connection!", Toast.LENGTH_LONG).show();
			((Activity) context).finish();
			return null;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int status_code = response.getStatusLine().getStatusCode();
		
		Log.e(TAG, "Status code for cookie retrieval: " + status_code);
		
		if (status_code == 204) {
			
			Log.d(TAG, "Got a good response (empty, though)!");

			
	    	print_cookies(httpclient);
			retrieved_cookie = storeCookie(context, httpclient);
			
		} else if (status_code == 200) {
			
			
	    	print_cookies(httpclient);
			retrieved_cookie = storeCookie(context, httpclient);
			

			Toast.makeText(context, "Login successful!", Toast.LENGTH_LONG).show();
			
			
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
					Log.e(TAG, "Response line: " + foo);
					
					// TODO: This is rather inelegant; it might be better to pass
					// the BufferedReader object back to the parent activity...
					fetched_data_string += foo;
					
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			


//	        Intent intent = new Intent();
//	        intent.putExtra(INTENT_EXTRA_DATA_STRING_RESULT_KEY, fetched_data_string);
//	        
//			setResult(Activity.RESULT_OK, intent);

			
		} else {
			Log.e(TAG, "Got a bad response: " + status_code);
			Log.e(TAG, response.getStatusLine().getReasonPhrase());
			
			// Invalidate the auth string
//			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//			settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_AUTH, null).commit();
			
			Toast.makeText(context, "AppEngine error: " + response.getStatusLine().getReasonPhrase(), Toast.LENGTH_LONG).show();
		}

		((Activity) context).finish();
		return retrieved_cookie;
		
//		*/
    }

	
	
	// =========================================================================

    // RETURNS A BOOLEAN REPRESENTING WHETHER WE NEED TO SHOW THE DIALOG
    private boolean internal_username_snooper() {
    	
    	boolean fetching_auth_token = false;
    	Log.d(TAG, "Retrieving username from undocumented API...");
    	// Consult http://www.androidjavadoc.com/m5-rc15/com/google/android/googleapps/GoogleLoginServiceHelper.html
    
    	try {
    		for (Method m : Class.forName("com.google.android.googlelogin.GoogleLoginServiceHelper").getMethods()) {
//    			Log.d(TAG, "Another class listing: " + ele.toString());
    			try {
    				if (m.getName().equals("getAccount")) {

   						m.invoke(null, this, REQUEST_CODE_USERNAME_SNOOPER_CALLBACK_ID, true);
    					

    					
    				} else if (m.getName().equals("getCredentials")) {


//    					Log.d(TAG, "Found 'getCredentials' method with " + parm_count + " parameters.");
//    					Log.d(TAG, "4th parameter type: " + ele.getParameterTypes()[3]);

    					// NOTE: The 4th parameter can either be a String or a boolean.
    					// boolean is primitive, so this check should work:
    					if (m.getParameterTypes()[3].isPrimitive()) {
    						
        					Log.d(TAG, "Executing 'getCredentials' method.");
    						
//    						Log.d(TAG, "4th parameter was primitive");
    						
	    					m.invoke(null,	// This extra argument is for the "invoke" method.
	    							this,
	    							REQUEST_CODE_AUTHTOKEN_SNOOPER_CALLBACK_ID,
	    							null,
//	    			                false,  // don't "require google"
	    			                true,  // require google
	    			                "ah",
	    			                false);
	    					
	    					
	    					fetching_auth_token = true;
	    					
    					} else {
//    						Log.e(TAG, "4th parameter was NOT primitive.");
    					}
    				}
    			} catch (IllegalArgumentException e) {
    				e.printStackTrace();
    			} catch (IllegalAccessException e) {
    				e.printStackTrace();
    			} catch (InvocationTargetException e) {
    				e.printStackTrace();
    			}
    		}
    	} catch (ClassNotFoundException e) {
    		e.printStackTrace();
    	} catch (SecurityException e) {
    		Log.e(TAG, "Raised a security exception :(");
    	} catch (Exception e) {
    		Log.e(TAG, "Raised a general exception.  Good thing we caught it!");
    		e.printStackTrace();
    	}

		return true;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

 	   String username = null;
    	
 	   switch (requestCode) {
 	   
	 	   case REQUEST_CODE_AUTHTOKEN_SNOOPER_CALLBACK_ID:
	 	   {
//	           String authToken = data.getStringExtra(GoogleLoginServiceConstants.AUTHTOKEN_KEY);
	           String authtoken = data.getStringExtra("authtoken");
	           Log.d(TAG, "\"authtoken\" obtained from internal GoogleLoginService API: " + authtoken);


   				Log.w(TAG, "Intent extras:");
	   			for (String key : data.getExtras().keySet())
	   				Log.i(TAG, key + ": "  + data.getExtras().get(key));
	   			

	   			
	   			Uri full_uri = Uri.parse(target_url);
	   			String host_part = full_uri.getScheme() + "://" + full_uri.getHost() + "/";
	   			
	   			
	   			// Note: This redirect only works with a "Get" HTTP request!!!
	   			// So the target URL must respond to Get, not Post.
//	   			String url_string = host_part + "_ah/login?auth=" + auth_string + "&continue=" + target_url;
	   			
	   			
	   			String url_string = host_part + "_ah/login?auth=" + authtoken;

	   			Log.e(TAG, "This is the cookie-fetcher URL from GoogleLoginService:");
	   			Log.e(TAG, url_string);

	   			
	   			// FIXME: Disabled for now
//				Log.w(TAG, "Now about to exchange \"Auth\" string for cookie...");
				Cookie auth_cookie = fetch_cookie_from_authorization(this, authtoken, target_url);

				return;
	 	   }
 	   
 	   		case REQUEST_CODE_USERNAME_SNOOPER_CALLBACK_ID:
 	   			// Fall through to next case
 	   		case 456:
 	   		{
				String key = "accounts";
				Log.d(TAG, key + ":" + Arrays.toString(data.getExtras().getStringArray(key)));
				
				String accounts[] = data.getExtras().getStringArray(key);
				if (accounts != null && accounts[0] != null) {
					Log.d(TAG, "Account: "  + accounts[0]);
					username = accounts[0];
				}
				else
					Log.d(TAG, "No account data, unfortunately.");
 	   			
 	   			
	            if (resultCode == RESULT_OK) {
	            	Log.d(TAG, "Username retrieval activity returned OK.");
	            }
	            
	            
	            
	            
	            
	            
	            
	            
	      	   if (username != null) {
	     		   
	               SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AppEngineLogin.this);
	               settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_USERNAME, username).commit();
	     	   }
	            
 	   		}
 		   default:
 	    	   	break;
 	   }
    }
    
    

    public static InputStream fetch(String urlString) throws MalformedURLException, IOException {
       	DefaultHttpClient httpClient = new DefaultHttpClient();
       	HttpGet request = new HttpGet(urlString);
       	HttpResponse response = httpClient.execute(request);
       	return response.getEntity().getContent();
    }
    
    
    
    
    
    
    
    final static String[] login_error_strings = {
	    "BadAuthentication",	// The login request used a username or password that is not recognized.
	    "NotVerified",	// The account email address has not been verified. The user will need to access their Google account directly to resolve the issue before logging in using a non-Google application.
	    "TermsNotAgreed",	// The user has not agreed to terms. The user will need to access their Google account directly to resolve the issue before logging in using a non-Google application.
	    "CaptchaRequired",	// A CAPTCHA is required. (A response with this error code will also contain an image URL and a CAPTCHA token.)
	    "Unknown",	// The error is unknown or unspecified; the request contained invalid input or was malformed.
	    "AccountDeleted",	// The user account has been deleted.
	    "AccountDisabled",	// The user account has been disabled.
	    "ServiceDisabled",	// The user's access to the specified service has been disabled. (The user account may still be valid.)
	    "ServiceUnavailable"	// The service is not available; try again later.
    };
}