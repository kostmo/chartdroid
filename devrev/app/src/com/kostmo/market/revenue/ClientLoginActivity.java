/*
 * 
 * This class should be instantiated and left around;
 * Whenever the application needs to interact with the Birdroid Server,
 * start the activity with an Intent for a return value.
 * 
 */

package com.kostmo.market.revenue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// Note: This activity is created for the sole purpose of collecting the cookie.
// Don't get distracted with other things!

public class ClientLoginActivity extends Activity {

    public static final String TAG = Market.TAG;

    // ========================================================================
	private final int DIALOG_LOGIN_PROMPT = 1;
	
    public static final String application_identifier = "kostmo-Crittr-1.0";

	public static final String INTENT_EXTRA_APPENGINE_AUTHORIZATION_STRING = "APPENGINE_AUTHORIZATION_STRING";
	
	
	public final static String PREFKEY_APPENGINE_USERNAME = "PREFKEY_APPENGINE_USERNAME";
	public final static String PREFKEY_APPENGINE_PASSWORD = "PREFKEY_APPENGINE_PASSWORD";
	public final static String PREFKEY_APPENGINE_AUTH = "PREFKEY_APPENGINE_AUTH";
	public final static String PREFKEY_APPENGINE_COOKIE_DOMAIN = "PREFKEY_APPENGINE_COOKIE_DOMAIN";
	public final static String PREFKEY_APPENGINE_COOKIE_PATH = "PREFKEY_APPENGINE_COOKIE_PATH";
	public final static String PREFKEY_APPENGINE_COOKIE_VALUE = "PREFKEY_APPENGINE_COOKIE_VALUE";
	public final static String PREFKEY_APPENGINE_COOKIE_NAME = "PREFKEY_APPENGINE_COOKIE_NAME";

//	public final static String cookie_target_url = "http://bugdroid.appspot.com/";
	public final static String cookie_target_url = new Uri.Builder()
		.scheme("http")
		.authority("android.clients.google.com")
		.path("market/api/ApiRequest").build().toString();
	
	public final static boolean SHOULD_FETCH_COOKIE = true;
	

    // ========================================================================
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

    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.app_engine_login);

		findViewById(R.id.button_login_app_engine).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_LOGIN_PROMPT);
			}
		});
    }
	
    // ========================================================================
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w("Crittr", "Crittr onDestroy()");
    }

    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
        switch (id) {

        case DIALOG_LOGIN_PROMPT:
        {
        	final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        	
            final View textEntryView = factory.inflate(R.layout.dialog_login_appengine, null);

            final EditText username_box = (EditText) textEntryView.findViewById(R.id.username_edit);
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
    						fetch(globally_stored_captcha_bundle.captcha_url), "src");

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
                	
                	
                    settings.edit().putString(PREFKEY_APPENGINE_USERNAME, username_string).commit();
                    settings.edit().putString(PREFKEY_APPENGINE_PASSWORD, password_string).commit();
                	
                	
                	if (globally_stored_captcha_bundle != null) {
                		globally_stored_captcha_bundle.assign_response( captcha_response_field.getText().toString() );
                	}
                	
                    String recovered_auth_string = transactClientLogin(
                    		ClientLoginActivity.this,
                    		username_string, password_string,
                    		globally_stored_captcha_bundle,
                    		cookie_target_url);
                    settings.edit().putString(PREFKEY_APPENGINE_AUTH, recovered_auth_string).commit();
                    
                    Log.d(TAG, "Success.");
                    Toast.makeText(ClientLoginActivity.this, "Success.", Toast.LENGTH_SHORT).show();
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
    
	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {
		case DIALOG_LOGIN_PROMPT:
		{
            final EditText username_box = (EditText) dialog.findViewById(R.id.username_edit);
            final EditText password_box = (EditText) dialog.findViewById(R.id.password_edit);
            
        	final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            username_box.setText(settings.getString(PREFKEY_APPENGINE_USERNAME, null));
            password_box.setText(settings.getString(PREFKEY_APPENGINE_PASSWORD, null));
			break;
		}
		default:
			break;
		}
	}
    
    // ========================================================================
    void finish_with_authorization_string(String authorization_string) {
    	
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_APPENGINE_AUTHORIZATION_STRING, authorization_string);
		setResult(Activity.RESULT_OK, intent);
		finish();
    }
    
    // ========================================================================
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

    // ========================================================================
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
    
    // ========================================================================
	String transactClientLogin(
			Context context,
			String user_email,
			String user_password,
			CaptchaBundle captcha_bundle,
			String target_domain) {
    	
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
		
		
		if (SHOULD_FETCH_COOKIE) {
			if (auth_string != null) {
				Log.w(TAG, "Now about to exchange \"Auth\" from ClientLogin for cookie...");
				Cookie auth_cookie = fetch_cookie_from_authorization(
						context,
						auth_string,
						target_domain);
			}
		}

		return auth_string;
    }

    // ========================================================================
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

    // ========================================================================
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

    // ========================================================================
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
	
    // ========================================================================
    public class AsyncAppEngineFetchTask extends AsyncTask<Void, String, String> {

    	ProgressDialog wait_dialog;
    	Uri target_uri;
    	Cookie cookie;

		Context activity_context;
    	String wait_message;
    	AsyncAppEngineFetchTask(Cookie cookie) {

    		this.cookie = cookie;
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
			}
			return null;
		}
		
	    @Override
	    public void onProgressUpdate(String... error_message) {

	    	Toast.makeText(ClientLoginActivity.this, error_message[0], Toast.LENGTH_LONG).show();
	    }
		
	    @Override
	    public void onPostExecute(String result) {
			wait_dialog.dismiss();
	    }
    }

    // ========================================================================
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
			settings.edit().putString(PREFKEY_APPENGINE_AUTH, null).commit();
			
			
			// TODO: Uncomment me when we get the re-login logic implemented
//			settings.edit().putString(AppEngineLogin.PREFKEY_APPENGINE_COOKIE_VALUE, null).commit();
			
			
			throw new AuthenticationExpiredException();

			// This is not needed, since we don't check the intent if we don't explicitly set RESULT_OK
//	        Intent intent = new Intent();
//			setResult(Activity.RESULT_CANCELED, intent);
		}
		return null;

	}


    // ========================================================================
    public static InputStream fetch(String urlString) throws MalformedURLException, IOException {
       	DefaultHttpClient httpClient = new DefaultHttpClient();
       	HttpGet request = new HttpGet(urlString);
       	HttpResponse response = httpClient.execute(request);
       	return response.getEntity().getContent();
    }
	
    // ========================================================================
	String globally_stored_suggested_username = null;
    CaptchaBundle globally_stored_captcha_bundle = null;
    
    private void create_login_dialog(String suggested_username, final CaptchaBundle captcha_bundle) {
    	
    	globally_stored_suggested_username = suggested_username;
    	globally_stored_captcha_bundle = captcha_bundle;
        
    	showDialog(DIALOG_LOGIN_PROMPT);
    }

    // ========================================================================
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