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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.crittr.track.Market;
import org.crittr.track.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// Note: This activity is created for the sole purpose of collecting the cookie.
// Don't get distracted with other things!

public class ClientLoginActivity extends Activity {

//  application_identifier = i.getExtras().getString(INTENT_EXTRA_APPLICATION_IDENTIFIER);
    public static final String application_identifier = "kostmo-Crittr-1.0";
	
	
	
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
            
//          application_identifier = i.getExtras().getString(INTENT_EXTRA_APPLICATION_IDENTIFIER);

	      	Log.e(TAG, "Starting the auth/username snooper...");

			create_login_dialog("", null);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(ClientLoginActivity.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    // =============================================
	
	
	
	static class CaptchaBundle {
		String captcha_url, captcha_token, captcha_response;
		
		CaptchaBundle(String url, String token) {
			captcha_url = url;
			captcha_token = token;
		}
		
		public void assign_response(String response) {
			captcha_response = response;
		}
	}
	
	
	
	private final int DIALOG_LOGIN_PROMPT = 1;
	
	
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
	

	private final int USERNAME_SNOOPER_CALLBACK_ID = 123;
	private final int AUTHTOKEN_SNOOPER_CALLBACK_ID = 7;
	
	
	// This authorization string should last for the session.

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

    	Log.d(TAG, "Executing onCreate() in ClientLoginActivity");
        
        
 
        
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
	
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
        switch (id) {

        case DIALOG_LOGIN_PROMPT:
        {
        	final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        	
            final View textEntryView = factory.inflate(R.layout.dialog_login_appengine, null);

            final EditText username_box = (EditText) textEntryView.findViewById(R.id.username_edit);
            username_box.setText(globally_stored_suggested_username);
            
            final EditText password_box = (EditText) textEntryView.findViewById(R.id.password_edit);
            final TextView google_signup_link = (TextView) textEntryView.findViewById(R.id.google_signup_link);
            google_signup_link.setMovementMethod(LinkMovementMethod.getInstance());

            final EditText captcha_response_field = (EditText) textEntryView.findViewById(R.id.captcha_text_edit);;
            if (globally_stored_captcha_bundle != null) {
            	LinearLayout captcha_box = (LinearLayout) textEntryView.findViewById(R.id.captcha_box);
            	captcha_box.setVisibility(View.VISIBLE);
            	
            	
            	ImageView captcha_image_view = (ImageView) textEntryView.findViewById(R.id.captcha_image);

            	
            	Drawable drawable;
    			try {
    				drawable = Drawable.createFromStream(
    						AppEngineLogin.fetch(globally_stored_captcha_bundle.captcha_url), "src");

    	        	captcha_image_view.setImageDrawable(drawable);
    			} catch (MalformedURLException e) {
    				e.printStackTrace();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
            }
            
            
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.google_login_dialog_title)
            .setView(textEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	String username_string = username_box.getText().toString();
                	String password_string = password_box.getText().toString();
                	
                	if (globally_stored_captcha_bundle != null) {
                		globally_stored_captcha_bundle.assign_response( captcha_response_field.getText().toString() );
                	}
                	
                    String recovered_auth_string = transactClientLogin(
                    		ClientLoginActivity.this,
                    		username_string, password_string,
                    		globally_stored_captcha_bundle,
                    		target_url,
                    		mBoundService);
                    settings.edit().putString(ClientLoginActivity.PREFKEY_APPENGINE_AUTH, recovered_auth_string).commit();
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	setResult(Activity.RESULT_CANCELED);
                	finish();
                }
            })
            .create();

        }
        }
        
        return null;
    }
    // =============================================  
	
    void finish_with_authorization_string(String authorization_string) {
    	
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_APPENGINE_AUTHORIZATION_STRING, authorization_string);
		setResult(Activity.RESULT_OK, intent);
		finish();
    }
    
    
    
    static HashMap<String, String> dump_response_to_log(InputStream is) {
		BufferedReader buff_read = new BufferedReader(new InputStreamReader(is));
		
		HashMap<String, String> response_hash = new HashMap<String, String>(); 

		try {
			String line;
			while (true) {
				line = buff_read.readLine();
				if (line == null) break;
//				Log.d(TAG, "Response line: " + line);
				
				int equals_index = line.indexOf('=');
				if (equals_index >= 0)
					response_hash.put(line.substring(0, equals_index), line.substring(equals_index+1));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response_hash;
	}

    
    
	static String parse_auth_string_from_response(InputStream is) {
		BufferedReader buff_read = new BufferedReader(new InputStreamReader(is));
		
		String Auth = null;
		String LSID = null;
		String SID = null;
		try {
			String foo;
			while (true) {
				foo = buff_read.readLine();
				if (foo == null) break;
//				Log.d(TAG, "Response line: " + foo);
				
				String[] split = foo.split("=");
				if (split.length > 0) {
					if (split[0].equals("SID"))
						SID = split[1];
					else if (split[0].equals("LSID"))
						LSID = split[1];
					else if (split[0].equals("Auth"))
						Auth = split[1];
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		Log.d(TAG, "ClientLogin response:");
		Log.i(TAG, "Auth: " + Auth);
		Log.i(TAG, "LSID: " + LSID);
		Log.i(TAG, "SID: " + SID);
		
		return Auth;
	}
    

	
    
	String transactClientLogin(
			Context context,
			String user_email,
			String user_password,
			CaptchaBundle captcha_bundle,
			String target_domain,
			AppEngineService bound_service) {
    	
		Log.d(TAG, "Obtaining credentials via ClientLogin...");
		
    	UrlEncodedFormEntity entity = null;
    	
    	List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
    	pairs.add(new BasicNameValuePair("accountType", "HOSTED_OR_GOOGLE"));
    	pairs.add(new BasicNameValuePair("Email", user_email));
    	pairs.add(new BasicNameValuePair("Passwd", user_password));
    	pairs.add(new BasicNameValuePair("service", "ah"));
    	pairs.add(new BasicNameValuePair("source", application_identifier));

    	if (captcha_bundle != null) {
    		pairs.add(new BasicNameValuePair("logintoken", captcha_bundle.captcha_token));
    		pairs.add(new BasicNameValuePair("logincaptcha", captcha_bundle.captcha_response));
    	}
    	
    	
    	
    	try {
			entity = new UrlEncodedFormEntity(pairs);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		


    	HttpPost httppost = new HttpPost("https://www.google.com/accounts/ClientLogin");
    	httppost.setEntity(entity);
    	

    	DefaultHttpClient httpclient = new DefaultHttpClient();


    	HttpResponse response;
    	InputStream is = null;
		try {
			
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			
			response = httpclient.execute(httppost);
			int status_code = response.getStatusLine().getStatusCode();
			is = response.getEntity().getContent();
			
			if (status_code == 200) {
				Log.d(TAG, "Got a good response!");
				
            	// If the password was successful, store login info in Preferences

                settings.edit().putString(ClientLoginActivity.PREFKEY_APPENGINE_USERNAME, user_email).commit();
                settings.edit().putString(ClientLoginActivity.PREFKEY_APPENGINE_PASSWORD, user_password).commit();
				
			} else {
				Log.e(TAG, "Got a bad response: " + status_code);
				
				if (status_code == 403) {


	                HashMap<String, String> response_hash = dump_response_to_log(is);
	                
	        		String error_type = response_hash.get("Error");

					Toast.makeText(context, "Login error: " + error_type, Toast.LENGTH_LONG).show();
					if (error_type != null) {
						if (error_type.equalsIgnoreCase("BadAuthentication")) {

							// Don't clear the password necessarily; we may have had a Captcha challenge.
		                	settings.edit().putString(ClientLoginActivity.PREFKEY_APPENGINE_PASSWORD, null).commit();
		                	
		                	// FIXME
//							username_retrieved_callback(user_email);
							
						} else if (error_type.equalsIgnoreCase("CaptchaRequired")) {

							String captcha_url_suffix = response_hash.get("CaptchaUrl");
							String full_captcha_url = "http://www.google.com/accounts/" + captcha_url_suffix;
							Log.e(TAG, "Captcha URL suffix: " + full_captcha_url);


							String captcha_token = response_hash.get("CaptchaToken");
							Log.e(TAG, "Captcha token: " + captcha_token);
							
							
							
							create_login_dialog(user_email, new CaptchaBundle(full_captcha_url, captcha_token));
						}
					}

					
					return null;
				} else {
					
					
					Log.w(TAG, "Got a bad response, but not error code 403.");
					
					// We probably need to do the Captcha challenge.
					dump_response_to_log(is);
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		
		
		String auth_string = parse_auth_string_from_response(is);
		Log.d(TAG, "\"Auth\" string obtained from ClientLogin: " + auth_string);
		
		

		if (auth_string != null) {
			Log.w(TAG, "Now about to exchange \"Auth\" from ClientLogin for cookie...");
			Cookie auth_cookie = AppEngineLogin.fetch_cookie_from_authorization(
					context,
					auth_string,
					target_domain);
			
			bound_service.process_fetch_queue( auth_cookie );
		}

		return auth_string;
    }




	String globally_stored_suggested_username = null;
    CaptchaBundle globally_stored_captcha_bundle = null;
    
    private void create_login_dialog(String suggested_username, final CaptchaBundle captcha_bundle) {
    	
    	globally_stored_suggested_username = suggested_username;
    	globally_stored_captcha_bundle = captcha_bundle;
        
    	showDialog(DIALOG_LOGIN_PROMPT);
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