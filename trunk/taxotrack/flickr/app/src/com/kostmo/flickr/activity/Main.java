package com.kostmo.flickr.activity;


import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.TextView;
import android.widget.Toast;

import com.kostmo.flickr.activity.prefs.PrefsGlobal;
import com.kostmo.flickr.bettr.BootReceiver;
import com.kostmo.flickr.bettr.Eula;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.TaskHostActivity;
import com.kostmo.flickr.service.CheckUpdateService;
import com.kostmo.flickr.tasks.CheckAuthTokenTask;
import com.kostmo.flickr.tasks.TagConventionFetcherTask;
import com.kostmo.tools.StreamUtils;

public class Main extends Activity implements OnClickListener, TaskHostActivity {
	
    // =============================================

	static final String TAG = Market.DEBUG_TAG; 
	
	public static String PREFERENCE_FIRST_STARTUP = "first_startup";
//	public static String PREFERENCE_MIGRATE_TO_NEW_LOGIN = "PREFERENCE_MIGRATE_TO_NEW_LOGIN";


    public static final int REQUEST_CODE_APPENGINE_FETCH = 1;
    public static final int REQUEST_CODE_FLICKR_AUTH_FETCH = 2;
    
    public static final String NEWS_URL = "http://sites.google.com/site/bettrflickr/news";
    public static final String ABOUT_URL = "http://sites.google.com/site/bettrflickr/about";
    public static final String INSTRUCTIONS_URL = "http://sites.google.com/site/bettrflickr/user-guide";
    
    
    public static final String PREFKEY_SHOW_UPLOAD_INSTRUCTIONS = "PREFKEY_SHOW_UPLOAD_INSTRUCTIONS";
    
	final int DIALOG_PURCHASE_MESSAGE = 1;
	final int DIALOG_RELEASE_NOTES = 2;
	final int DIALOG_LOGIN_ERROR = 3;
	
	
	
	public static final String PREFKEY_HAVE_UPLOADED_ONCE = "PREFKEY_HAVE_UPLOADED_ONCE";

	
    public Handler mHandler = new Handler();

	String globally_stored_disabled_function_description;
    boolean crop_first;
    SharedPreferences settings;
    String persisted_error_message = null;
	CheckAuthTokenTask check_authtoken_task = null;

    // ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	super.onSaveInstanceState(out_bundle);
    	Log.i(TAG, "onSaveInstanceState");
    	
    	out_bundle.putString("disabled_function_description", globally_stored_disabled_function_description);
    }
    
    // ========================================================================
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	super.onRestoreInstanceState(in_bundle);
    	Log.i(TAG, "onRestoreInstanceState");
    	
    	globally_stored_disabled_function_description = in_bundle.getString("disabled_function_description");
    }
	
    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

	    setContentView(R.layout.main);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        for (int button : new int[] {R.id.button_photo_upload, R.id.button_photo_stream, R.id.button_photo_tabs, R.id.button_crittr_map, R.id.button_photo_sets, R.id.button_contacts})
        	findViewById(button).setOnClickListener(this);   

        Eula.showEula(this);

	

        /*
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	
		boolean have_uploaded_once = settings.getBoolean(PREFKEY_HAVE_UPLOADED_ONCE, false);
    	if (!have_uploaded_once)
    		findViewById(R.id.get_started_nagger).setVisibility(View.VISIBLE);

		boolean first_startup = settings.getBoolean(PREFERENCE_FIRST_STARTUP, true);
		*/
        

        
        
//        Animation logo_fade = AnimationUtils.loadAnimation(this, R.anim.title_slide_left);
//        findViewById(R.id.flickr_logo).startAnimation(logo_fade);

//        Animation logo_fade2 = AnimationUtils.loadAnimation(this, R.anim.title_slide_right);
//        findViewById(R.id.bettr_logo).startAnimation(logo_fade2);
        
        
        
        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	this.persisted_error_message = a.persisted_error_message;
        	this.check_authtoken_task = a.check_authtoken_task;
        	if (this.check_authtoken_task != null)
        		this.check_authtoken_task.updateActivity(this);
        } else {
        	
        	// We avoid redoing these things if it's just an orientation change...

        	check_authtoken_task = new CheckAuthTokenTask(this);
        	check_authtoken_task.execute();
	        
	        new TagConventionFetcherTask(this).execute();
	        
			int current_version_code = Market.getVersionCode(this, Main.class);
			if (current_version_code > settings.getInt(PrefsGlobal.PREFKEY_PREVIOUS_VERSION_CODE, -1)) {
				settings.edit().putInt(PrefsGlobal.PREFKEY_PREVIOUS_VERSION_CODE, current_version_code).commit();
				showDialog(DIALOG_RELEASE_NOTES);
			}
        }
    }

    
    // =============================================

    class StateRetainer {
		String persisted_error_message;
		CheckAuthTokenTask check_authtoken_task;
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {

    	StateRetainer a = new StateRetainer();
		a.persisted_error_message = this.persisted_error_message;
		a.check_authtoken_task = this.check_authtoken_task;
        return a;
    }

    
    
    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
        switch (id) {
        case DIALOG_PURCHASE_MESSAGE:
        {
	        TextView feature_overview_blurb = (TextView) dialog.findViewById(R.id.disabled_function_description);
	        feature_overview_blurb.setText(Html.fromHtml(globally_stored_disabled_function_description), TextView.BufferType.SPANNABLE);
	        feature_overview_blurb.setMovementMethod(LinkMovementMethod.getInstance());

	        break;
        }
        case DIALOG_LOGIN_ERROR:
        {
        	((TextView) dialog.findViewById(android.R.id.message)).setText(persisted_error_message);
        	break;
        }
        default:
        	break;
        }
    }

    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
        switch (id) {
		case DIALOG_LOGIN_ERROR:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.dialog_title_login_trouble)
			.setMessage("Couldn't log in.")
			.setPositiveButton(R.string.alert_dialog_ok, null)
			.create();
		}
		case DIALOG_RELEASE_NOTES:
		{
			CharSequence release_notes = StreamUtils.readFile(this, R.raw.release_notes);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.release_notes)
			.setMessage(release_notes)
			.setPositiveButton(R.string.alert_dialog_ok, null)
			.create();
		}
        case DIALOG_PURCHASE_MESSAGE:
        {
        	// NOTE: This dialog is customized differently from the others.
        	
	        View tagTextEntryView = factory.inflate(R.layout.dialog_purchase_nagger, null);

	        TextView feature_overview_blurb = (TextView) tagTextEntryView.findViewById(R.id.disabled_function_description);
	        feature_overview_blurb.setText(Html.fromHtml(globally_stored_disabled_function_description), TextView.BufferType.SPANNABLE);
	        feature_overview_blurb.setMovementMethod(LinkMovementMethod.getInstance());
	        
	        tagTextEntryView.findViewById(R.id.purchase_nag_secondary_text).setVisibility(View.GONE);
	        

            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.purchase_main_dialog_title)
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.purchase_button_message, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
	
                	// Launch market intent
                	Uri market_uri = Uri.parse(Market.MARKET_FLICKR_PACKAGE_SEARCH);
                	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
                	startActivity(i);
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, null)
            .create();
        }
        }
        
        return super.onCreateDialog(id);
    }

    // ========================================================================
    public Handler getHandler() {
    	return mHandler;
    }

    // ======================================================================== 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_main, menu);
        return true;
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);

		menu.findItem(R.id.menu_flickr_logout).setVisible( !(stored_auth_token == null) );
		menu.findItem(R.id.menu_flickr_login).setVisible( stored_auth_token == null );
		
        return true;
    }

    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_news:
        {
			Uri flickr_destination = Uri.parse( NEWS_URL );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

            return true;
        }
        case R.id.menu_about:
        {
        	
        	/*
        	Intent i = new Intent();
        	i.setClass(this, HelpAbout.class);
        	this.startActivity(i);
        	*/
        	
        	
			Uri flickr_destination = Uri.parse( ABOUT_URL );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));
        	
            return true;
        }
        
        case R.id.menu_instructions:
        {
        	/*
        	Intent i = new Intent();
        	i.setClass(this, HelpGlossary.class);
        	this.startActivity(i);
        	*/
        	
			Uri flickr_destination = Uri.parse( INSTRUCTIONS_URL );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));
        	
            return true;
        }
        case R.id.menu_flickr_login:

            get_auth_key(null, null, null, new ArrayList<String>());
            return true;
        	
        case R.id.menu_flickr_logout:
        {
			// The user has revoked our key.  Reset authentication process...
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			settings.edit().putString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null).commit();
			
			try {
				Log.d(TAG, "Creating CookieSyncManager instance...");
				CookieSyncManager.createInstance(this);
				Log.d(TAG, "Created.");
				CookieManager.getInstance().removeAllCookie();
			} catch (IllegalStateException e) {
				Log.e(TAG, "Caught CookieManager error...");
				e.printStackTrace();
			}
            return true;
        }    
            
        case R.id.menu_preferences:
        {
        	Intent i = new Intent();
        	i.setClass(this, PrefsGlobal.class);
        	this.startActivity(i);
        	
        	return true;
        }
        case R.id.menu_more_apps:
        {
	    	Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
	    	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
			if (Market.isIntentAvailable(this, i))
				startActivity(i);
			else
				Toast.makeText(this, "Android Market not available.", Toast.LENGTH_SHORT).show();
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }

    // ========================================================================
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(request " + requestCode
              + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        
  	   	switch (requestCode) {
   		case REQUEST_CODE_FLICKR_AUTH_FETCH:
   		{
   			// This is not needed
//			String auth_token = data.getStringExtra(FlickrAuthRetrievalActivity.ACTIVITY_EXTRA_FLICKR_AUTH_TOKEN);

   			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
   	        if (!settings.getBoolean(BootReceiver.PREFKEY_CHECKIN_ALARM_SCHEDULED, false))
   	        	CheckUpdateService.schedule(this);
   	        
   			break;
   		}
   		default:
	    	break;
  	   	}
    }

    // ========================================================================
    void get_auth_key(Uri true_result, String title, String description, ArrayList<String> tags) {

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);

		
		if (stored_auth_token == null) {
			
			// Launch authorization Activity and catch result.
	    	Intent i = new Intent();
	    	i.setClass(Main.this, FlickrAuthRetrievalActivity.class);
            startActivityForResult(i, REQUEST_CODE_FLICKR_AUTH_FETCH);
		}
    }

    // ========================================================================
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_photo_upload:
		{
			startActivity(new Intent(Main.this, BatchUploaderActivity.class));
			break;
		}
    	case R.id.button_photo_stream:
    	{
	    	Intent i = new Intent(Intent.ACTION_VIEW);
	    	i.setClass(Main.this, PhotoListActivity.class);
	//    	i.putExtra(PhotoListActivity.INTENT_EXTRA_PHOTOLIST_VIEW_MODE, PhotolistViewMode.LIST.ordinal());
	    	
	    	i.putExtra(PhotoListActivity.INTENT_EXTRA_MY_PHOTOSTREAM_MODE, true);
	    	
	    	startActivity(i);
			break;
    	}
    	case R.id.button_photo_tabs:
    	{
	    	Intent i = new Intent(Intent.ACTION_VIEW);
	    	i.setClass(Main.this, PhotoListActivity.class);
	//    	i.putExtra(PhotoListActivity.INTENT_EXTRA_PHOTOLIST_VIEW_MODE, PhotolistViewMode.LIST.ordinal());
	    	startActivity(i);
			break;
    	}
    	case R.id.button_crittr_map:
    	{
	    	Intent i = new Intent(Intent.ACTION_VIEW);
	    	i.setClass(Main.this, PhotoMap.class);
	
	    	startActivity(i);
			break;
    	}
    	case R.id.button_photo_sets:
    	{
	    	Intent i = new Intent(Intent.ACTION_VIEW);
	    	i.setClass(Main.this, PhotosetsListActivity.class);
	
	    	startActivity(i);
			break;
    	}
    	case R.id.button_contacts:
    	{
	    	Intent i = new Intent();
	    	i.setClass(Main.this, ListActivityContacts.class);
	
	    	startActivity(i);
			break;
    	}
		}
	}

    // ========================================================================
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w(Market.DEBUG_TAG, "onDestroy()");
    }

    // ========================================================================
	@Override
	public void showErrorDialog(String message) {
		this.persisted_error_message = message;
		showDialog(DIALOG_LOGIN_ERROR);
	}

	@Override
	public void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}


	@Override
	public void launchAuthenticationActivity() {

		Intent i = new Intent(this, FlickrAuthRetrievalActivity.class);
	    startActivityForResult(i, REQUEST_CODE_FLICKR_AUTH_FETCH);
	}

	@Override
	public Activity getContext() {
		return this;
	}
}
