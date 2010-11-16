package com.kostmo.flickr.activity;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.AuthInterface;
import com.aetrion.flickr.auth.Permission;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tools.FlickrFetchRunnable;





class FlickrFetchRunnableInstance implements FlickrFetchRunnable {

	Context ctx;
	FlickrFetchRunnableInstance(Context c, String f) {
		ctx = c;
	}

	@Override
	public void fetch() throws FlickrException {
		
	}
}




public class FlickrAuthRetrievalActivity extends Activity {
	
	
	boolean started_browser_authentication = false;
	
	
    boolean dialog_has_started = false;

	public final static String PREFKEY_FLICKR_AUTH_TOKEN = "PREFKEY_FLICKR_AUTH_TOKEN";
	public final static String PREFKEY_FLICKR_USER_NSID = "PREFKEY_FLICKR_USER_NSID";
	public final static String PREFKEY_FLICKR_USERNAME = "PREFKEY_FLICKR_USERNAME";
	final static String PREFKEY_FLICKR_PASSWORD = "PREFKEY_FLICKR_PASSWORD";

	final static String PREFKEY_FLICKR_TEMPORARY_FROB = "PREFKEY_FLICKR_TEMPORARY_FROB";
	
	public final static String ACTIVITY_EXTRA_FLICKR_AUTH_TOKEN = "ACTIVITY_EXTRA_FLICKR_AUTH_TOKEN";
	
	
    RequestContext requestContext;

    final static int DIALOG_INITIAL_INFORMATION = 1;
    final static int DIALOG_BROWSER_INSTRUCTIONS = 2;
    
    
	
	final static int REQUEST_CODE_EMBEDDED_BROWSER = 1;
//	final static int STANDARD_BROWSER_RETURN_CODE = 2;
	
	
	public final static int FLICKR_CODE_INVALID_AUTH_TOKEN = 98;
	public final static int FLICKR_CODE_INVALID_SIGNATURE = 96;
	

    private AuthenticationFetchTask mTask;


	static final String TAG = Market.DEBUG_TAG; 
    
    
    public Handler mHandler = new Handler();

    // ========================================================================
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	if (started_browser_authentication) {

			Toast.makeText(this, "Completing the authentication...", Toast.LENGTH_SHORT).show();
			
			Log.d(TAG, "Authentication web page already visited.  Now retrieving token.");
			(mTask = new AuthenticationFetchTask(new FlickrFetchRunnableInstance(this, null))).execute();
    	}
    }

    
    // ========================================================================
    @Override
    protected void  onRestart() {
    	super.onRestart();
    	
//    	Log.i(TAG, "onRestart");
    }

    // ========================================================================
    @Override
    protected void onPause() {
    	super.onPause();

//    	Log.i(TAG, "onPause");
    }

    // ========================================================================
    @Override
    protected void onStop() {
    	super.onStop();

//    	Log.i(TAG, "onStop");
    }

    // ========================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
      
//    	Log.i(TAG, "onDestroy");
    	
        if (mTask != null && mTask.getStatus() == AuthenticationFetchTask.Status.RUNNING) {
            mTask.cancel(true);
        }
    }
    
    // ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
//    	Log.i(TAG, "onSaveInstanceState");
    	
    	

    	// This boolean is restored in onCreate()
    	out_bundle.putBoolean("dialog_started", dialog_has_started);

    	out_bundle.putBoolean("started_browser_authentication", started_browser_authentication);
    	

//        Log.d(TAG, "boolean 'dialog_started' stored in out_bundle: " + out_bundle.getBoolean("dialog_started", false));
    }


    // ========================================================================
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
//    	Log.i(TAG, "onRestoreInstanceState");

    	started_browser_authentication = in_bundle.getBoolean("started_browser_authentication");
    	

//        Log.d(TAG, "in_bundle in onRestoreInstanceState(): " + in_bundle);
//        Log.d(TAG, "boolean 'dialog_started' stored in in_bundle: " + in_bundle.getBoolean("dialog_started", false));
    }
    
    
    // ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
//    	getIntent().putExtra(INTENT_EXTRA_TSN, current_tsn);

//    	Log.i(TAG, "onRetainNonConfigurationInstance");
    	
    	
        return null;
    }
    

    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//    	Log.i(TAG, "onCreate");
    	
    	setContentView(R.layout.authentication_failsafe_screen);
    	
    	findViewById(R.id.button_resume_failsafe).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				(mTask = new AuthenticationFetchTask(new FlickrFetchRunnableInstance(FlickrAuthRetrievalActivity.this, null))).execute();
			}
    	});
    	
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        
        
//        Log.d(TAG, "savedInstanceState: " + savedInstanceState);
        if (savedInstanceState != null) {
        	dialog_has_started = savedInstanceState.getBoolean("dialog_started", false); 
        }
    }

    // ========================================================================
    @Override
    protected void onStart() {
    	super.onStart();
//    	Log.i(TAG, "onStart");
    	
    	
    	
//        Log.d(TAG, "dialog_has_started: " + dialog_has_started);
        
        if (!dialog_has_started) {

        	run_activity_example();
        	
        	
//        	Log.i(TAG, "FlickrAuthRetrievalActivity is doing the standard initialization.");
        }
    }

    // ========================================================================
    DialogInterface.OnClickListener foo_canceller = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        	Log.e(TAG, "User cancelled the initial dialog.");
        	setResult(Activity.RESULT_CANCELED);
        	finish();
        }
    };

    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {
    	
        switch (id) {
        case DIALOG_INITIAL_INFORMATION:
        {
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.flickr_authorization_heading)
            .setMessage(R.string.flickr_authorization_instructions)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
            		showDialog(DIALOG_BROWSER_INSTRUCTIONS);
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, foo_canceller)
            .create();
        }
        case DIALOG_BROWSER_INSTRUCTIONS:
        {
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Approve app in browser")
            .setMessage(R.string.flickr_browser_instructions)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	getFrobAndLaunchBrowser();
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, foo_canceller)
            .create();
        }
        }
        
        return null;
    }

    // ========================================================================
    void getFrobAndLaunchBrowser() {
    	UrlAndFrob uaf = null;
    	
    	
		try {
			uaf = get_authentication_url();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
    	
    	
    	
    	if (uaf != null) {
        	Uri foobar = Uri.parse( uaf.url.toString() );


        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(FlickrAuthRetrievalActivity.this);
        	settings.edit().putString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_TEMPORARY_FROB, uaf.frob).commit();
        	
			Intent i = new Intent(Intent.ACTION_VIEW, foobar);
//            startActivityForResult(i, STANDARD_BROWSER_RETURN_CODE);
			
			
			
            startActivity(i);
            
            started_browser_authentication = true;
            
            // XXX Apparently not needed...
//            findViewById(R.id.button_resume_failsafe).setVisibility(View.VISIBLE);
            
    	} else {
    		
    		String fail_message = "Failed getting frob and authorization URL.";
    		Log.e(TAG, fail_message);

		    Toast.makeText(FlickrAuthRetrievalActivity.this, fail_message, Toast.LENGTH_LONG).show();
    		
    	}
    }
    

    
    // ========================================================================
    // This catches revocation of the Authentication key and responds appropriately.
    // Accepts a FlickrFetchRunnable.
    public static void fetchDataAndCatchAuthRevocation(Context c, FlickrFetchRunnable runnable) {

//    	Log.d(TAG, "Now in fetchDataAndCatchAuthRevocation()");
    	
		try {

			runnable.fetch();
			
		} catch (FlickrException e) {
			
			e.printStackTrace();
			
			Log.e(FlickrAuthRetrievalActivity.TAG, "ERROR CODE: " + e.getErrorCode());
			Log.e(FlickrAuthRetrievalActivity.TAG, "ERROR MESSAGE: " + e.getErrorMessage());

			if (Integer.parseInt( e.getErrorCode() ) == FLICKR_CODE_INVALID_AUTH_TOKEN) {
				
				// The user has revoked our key.  Reset authentication process...
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
				settings.edit().putString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null).commit();
				settings.edit().putString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_TEMPORARY_FROB, null).commit();
				
				Toast.makeText(c, "Authorization key revoked!", Toast.LENGTH_SHORT).show();
				
				// TODO: I don't want to cause an infinite loop...
//				((FlickrAuthRetrievalActivity) c).run_activity_example();
			}
		}
    }

    // ========================================================================
    void finish_with_authorization_string(String authorization_string) {
    	
        Intent intent = new Intent();
        intent.putExtra(ACTIVITY_EXTRA_FLICKR_AUTH_TOKEN, authorization_string);
		setResult(Activity.RESULT_OK, intent);

		finish();
    }
    
    // ========================================================================
    public class UrlAndFrob {
    	URL url;
    	String frob;
    	
    	UrlAndFrob(URL u, String f) {
    		
        	url = u;
        	frob = f;	
    	}
    }

    // ========================================================================
    void run_activity_example() {

//    	Log.d(TAG, "Now in run_activity_example()");
    	
	    	
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
		
		
		if (stored_auth_token != null) {

			finish_with_authorization_string(stored_auth_token);
			
		} else {
			
			Log.e(TAG, "We don't have an authentication token!!!");
				
			if (!dialog_has_started) {
				showDialog(DIALOG_INITIAL_INFORMATION);
				dialog_has_started = true;
			}
		}
    }
    
    // ========================================================================
    UrlAndFrob get_authentication_url() throws SAXException, IOException  {

        Flickr flickr = null;
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
			    new REST()
			);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
 

    	
    	
    	String frob = null;
    	
        Flickr.debugStream = false;

        AuthInterface authInterface = flickr.getAuthInterface();

        try {
            frob = authInterface.getFrob();
        } catch (FlickrException e) {
            e.printStackTrace();
        }
//        Log.d("FLICKR", "frob: " + frob);

        if (frob != null) {
//			URL url = authInterface.buildAuthenticationUrl(Permission.DELETE, frob);
        	URL url = authInterface.buildAuthenticationUrl(Permission.WRITE, frob);	// Edited by Karl

//        	Log.d(Meta.DEBUG_TAG, url.toExternalForm());
        

//        	Log.d(Meta.DEBUG_TAG, "Be sure to visit the provided URL now!!!");
        	return new UrlAndFrob(url, frob);
        	
        } else {
        	
        	Log.e(Market.DEBUG_TAG, "I could not generate a frob.");
        	return null;
        }
    }
    
    // ========================================================================
    String continue_frobbery(String passed_frob) {
    	
//    	Log.d("FLICKR", "Proceeding with FROB retrieval.");
    	
        Flickr flickr = null;
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
			    new REST()
			);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
		
		
		AuthInterface authInterface = flickr.getAuthInterface();
    	
    	
    	
    	
    	
    	Auth auth = null;
        try {
            auth = authInterface.getToken(passed_frob);
            Log.i("FLICKR", "Authentication success");
            // This token can be used until the user revokes it.
//            Log.d(TAG, "Token: " + auth.getToken());
            
            Editor settings_edit = PreferenceManager.getDefaultSharedPreferences(FlickrAuthRetrievalActivity.this).edit();
            settings_edit.putString(PREFKEY_FLICKR_USER_NSID, auth.getUser().getId());
            settings_edit.putString(PREFKEY_FLICKR_USERNAME, auth.getUser().getUsername());
            settings_edit.commit();
            
            Log.d(TAG, "nsid: " + auth.getUser().getId());
            Log.d(TAG, "Realname: " + auth.getUser().getRealName());
            Log.d(TAG, "Username: " + auth.getUser().getUsername());
            Log.d(TAG, "Permission: " + auth.getPermission().getType());
            
        } catch (FlickrException e) {
            Log.e(TAG, "Authentication failed");
            Log.w(TAG, e.getErrorMessage());
            return null;
        } catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}    	

    	return auth.getToken();
    }

    

    // ========================================================================
    private class AuthenticationFetchTask extends AsyncTask<Void, Void, String> {
    	
    	

    	
		FlickrFetchRunnableInstance delayed_executable;
    	AuthenticationFetchTask(FlickrFetchRunnableInstance r) {
    		delayed_executable = r;
    	}
    	
    	
        private SharedPreferences mPreferences;


        @Override
        public void onPreExecute() {
        	mPreferences = PreferenceManager.getDefaultSharedPreferences(FlickrAuthRetrievalActivity.this);
        }

        public String doInBackground(Void... params) {

        	String stored_frob = mPreferences.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_TEMPORARY_FROB, null);
    		return continue_frobbery(stored_frob);
        }



        @Override
        public void onPostExecute(String auth_token) {
 
        	if (auth_token != null) {
        		
        		mPreferences.edit().putString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, auth_token).commit();

//    			fetchDataAndCatchAuthRevocation(delayed_executable.ctx, delayed_executable);
        		Toast.makeText(FlickrAuthRetrievalActivity.this, R.string.auth_complete_message, Toast.LENGTH_LONG).show();
    			finish_with_authorization_string(auth_token);
    			
        	} else {

            	// If we're in here, the authentication has failed.
            	mPreferences.edit().putString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_TEMPORARY_FROB, null).commit();
            	
                AlertDialog d = new AlertDialog.Builder(FlickrAuthRetrievalActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Authentication unsuccessful")
                .setMessage("Did you complete the steps in the browser?")
                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

//                    	run_activity_example();
                    	getFrobAndLaunchBrowser();
                    }
                })
                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    	setResult(Activity.RESULT_CANCELED);
                    	finish();
                    }
                })
                .create();
            
                d.show();
        	}
        }
    }

    
    // ========================================================================
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.i(TAG, "onActivityResult(request " + requestCode + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode != RESULT_OK) {
//            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, "Failed - Please retry login.", Toast.LENGTH_LONG);
            t.show();
            
        	setResult(Activity.RESULT_CANCELED);
        	finish();
            
            return;
        }

        
        
  	   	switch (requestCode) {
   		case REQUEST_CODE_EMBEDDED_BROWSER:
   		{
			Log.d(TAG, "Authentication web page already visited.  Now retrieving token.");
			(mTask = new AuthenticationFetchTask(new FlickrFetchRunnableInstance(this, null))).execute();
			
	    	break;
   		}
   		default:

	    	break;
  	   	}
  	   	
    }
}