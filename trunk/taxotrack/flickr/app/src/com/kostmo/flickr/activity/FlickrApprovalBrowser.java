package com.kostmo.flickr.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.RenderPriority;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kostmo.flickr.activity.prefs.PrefsWebView;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;



public class FlickrApprovalBrowser extends Activity {


	static final String TAG = Market.DEBUG_TAG; 
	public static final String INTENT_EXTRA_OBJECTIVE = "INTENT_EXTRA_OBJECTIVE";
	public static final String INTENT_EXTRA_WIPE_PASSWORDS = "INTENT_EXTRA_WIPE_PASSWORDS";
	
	
	

    final static int DIALOG_LOGIN_INFO = 1;
    final static int DIALOG_YAHOO_FALLBACK = 2;
    
	
	
	public enum BrowserObjective {
		APP_AUTHENTICATION, GROUP_JOIN, GEO_PRIVACY
	}
	
	
	final String JAVASCRIPT_INTERFACE_NAME = "KarlJavscriptOut";
	
	
	
	class MyJavaScriptInterface  
	{
	    public void showHTML(String response)  
	    {
//	    	Log.d(TAG, "Javascript response: " + response);
	    	
	    	if (response != null && response.length() > 0 && Integer.parseInt(response) >= 0) {

		        new AlertDialog.Builder(FlickrApprovalBrowser.this)  
		            .setTitle("Invalid login")  
		            .setMessage("Your username or password is incorrect.")  
		            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

		    			public void onClick(DialogInterface dialog, int which) {
		    		    	setResult(Activity.RESULT_CANCELED);
		    		    	finish();
		    			}	
		            }
	            )  
		        .setCancelable(false)  
		        .create()  
		        .show();
	    	}
	    }  
	}  

	WebView webview;
	ListView task_items_list;
	

	boolean on_geo_privacy_page = false;
	
	BrowserObjective objective;
    private Handler handler = new Handler();
	
    
    boolean doing_flickr_setup_task = true;	// FIXME - Pass this as an Intent Extra instead...
    
    void hide_for_automation() {
    	
        if (objective == BrowserObjective.APP_AUTHENTICATION
        		|| objective == BrowserObjective.GEO_PRIVACY
        		|| objective == BrowserObjective.GROUP_JOIN
        		) {
        	
    		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    		boolean keep_webview_visible = settings.getBoolean("keep_webview_visible", false);

        	webview.getSettings().setLoadsImagesAutomatically(keep_webview_visible);
    		if (keep_webview_visible) {
	        	webview.setVisibility(View.VISIBLE);

//	            if (doing_flickr_setup_task)
//	            	task_items_list.setVisibility(View.VISIBLE);
    		} else {

	        	webview.setVisibility(View.INVISIBLE);
    		}
        	
        	webview.setInitialScale(1);	// TODO: How to zoom out "all the way"?
        }
    }
    
    
    
    class StepDescription {
    	boolean complete = false;
    	String description;
    	StepDescription(String d) {
    		description = d;
    	}
    }

    private class CompletedTaskListAdapter extends BaseAdapter {
    	
    	ArrayList<StepDescription> task_list = new ArrayList<StepDescription>();

        final private LayoutInflater mInflater;

    	
        public void add_item(String new_item) {
        	
        	if (task_list.size() > 0)
        		task_list.get(task_list.size() - 1).complete = true;
        	        	
        	StepDescription sd = new StepDescription(new_item);
        	task_list.add(sd);
        	this.notifyDataSetInvalidated();
        }
        
    	
        public CompletedTaskListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public boolean isEnabled(int position) {return false;}
        
        @Override
        public boolean areAllItemsEnabled() {return false;}
        
        public int getCount() {
            return task_list.size();
        }

        public Object getItem(int position) {
            return task_list.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

        	ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_browser_task, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.checkmark = (ImageView) convertView.findViewById(R.id.checkmark_image);
                holder.textview = (TextView) convertView.findViewById(R.id.task_description);
                
                convertView.setTag(holder);

            } else {

                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }


            holder.textview.setText( ((StepDescription) getItem(position)).description );
            if (((StepDescription) getItem(position)).complete)
            	holder.checkmark.setImageResource(R.drawable.btn_check_buttonless_on);
            else
            	holder.checkmark.setImageResource(R.drawable.btn_check_buttonless_off);

            return convertView;
        }
        
        class ViewHolder {
        	
        	TextView textview;
        	ImageView checkmark;
        }
    }
    
    
    
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//    	Log.i(TAG, "onCreate() in FlickrApprovalBrowser");
//    	getWindow().setBackgroundDrawableResource(R.drawable.translucent_background);
    	

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        Intent i = getIntent();
        Uri passed_data = i.getData();
        objective = BrowserObjective.values()[ i.getIntExtra(INTENT_EXTRA_OBJECTIVE, BrowserObjective.APP_AUTHENTICATION.ordinal()) ];
        
        
        
        // Inflate our UI from its XML layout description.
        setContentView(R.layout.webview_frame);
        
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        
        task_items_list = (ListView) findViewById(R.id.task_items_list);
        if (doing_flickr_setup_task)
        	task_items_list.setVisibility(View.VISIBLE);
        
        


        webview = (WebView) findViewById(R.id.webview);

        webview.getSettings().setRenderPriority(RenderPriority.HIGH);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(new MyJavaScriptInterface(), JAVASCRIPT_INTERFACE_NAME);
        
        hide_for_automation();



        // Should never have to deal with orientation change!
        final Object a = getLastNonConfigurationInstance();
        if (a != null) {
            task_items_list.setAdapter( ((StateObject) a).ctla );
            webview.setWebViewClient(new HelloWebViewClient());
            webview.setWebChromeClient(new HelloWebChromeClient());
//            webview.loadUrl( passed_data.toString() );
        }
        else {
            task_items_list.setAdapter(new CompletedTaskListAdapter(this));
            webview.setWebViewClient(new HelloWebViewClient());
            webview.setWebChromeClient(new HelloWebChromeClient());
            webview.loadUrl( passed_data.toString() );
        }
    }
    
    
    
    
    
	


	
	void yahoo_login_injection(WebView view, String flickr_username, String flickr_password) {
		String javascript_injection = "javascript:document.login_form.login.value=\"" + flickr_username + "\";document.login_form.passwd.value=\"" + flickr_password + "\";document.login_form.submit();";
		view.loadUrl("javascript:" + javascript_injection);
		yahoo_login_submitted_mutex = true;
	}
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	
        switch (id) {
        case DIALOG_LOGIN_INFO:
        {
        	

//      	final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(FlickrApprovalBrowser.this);
	    	
	        LayoutInflater factory = LayoutInflater.from(FlickrApprovalBrowser.this);
	        final View textEntryView = factory.inflate(R.layout.dialog_login_standard, null);
	        
	        final EditText username_box = (EditText) textEntryView.findViewById(R.id.username_edit);
	        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	        String username = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USERNAME, null);
	        if (username != null)
	        	username_box.setText(username);
	        
	        final EditText password_box = (EditText) textEntryView.findViewById(R.id.password_edit);
	        ((TextView) textEntryView.findViewById(R.id.flickr_signup_link)).setMovementMethod(LinkMovementMethod.getInstance());
	
	
	
	    	return new AlertDialog.Builder(FlickrApprovalBrowser.this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.flickr_login_dialog_title)
	        .setView(textEntryView)
	        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	
	            	String username_string = username_box.getText().toString();
	            	String password_string = password_box.getText().toString();
	
	    			yahoo_login_injection(webview, username_string, password_string);
	//            			view.setVisibility(View.INVISIBLE);
	            }
	        })
	        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	
	            	// FIXME: This is not implemented yet.
//	            	showDialog(YAHOO_FALLBACK_DIALOG);
	            }
	        })
	        .create();

        	
        	
        	
        }
        case DIALOG_YAHOO_FALLBACK:
        {
        	return new AlertDialog.Builder(FlickrApprovalBrowser.this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.flickr_abort_authentication_title)
            .setMessage(R.string.flickr_fallback_explanation)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	// Launches the standard browser.
                	startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
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
    
    
    
    
    
    
    
    
    class StateObject {
    	HelloWebViewClient hwvc;
    	HelloWebChromeClient hwcc;
    	CompletedTaskListAdapter ctla;
    }
    
    
    @Override
    protected void onResume() {
    	super.onResume();
//    	Log.i(TAG, "onResume");
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
//    	Log.i(TAG, "onStart");
    }

    @Override
    protected void onPause() {
    	super.onPause();

//    	Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
    	super.onStop();

//    	Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//    	Log.i(TAG, "onDestroy");
    }
    
    
    
    
    
    
    @Override
    public Object onRetainNonConfigurationInstance() {
//    	getIntent().putExtra(INTENT_EXTRA_TSN, current_tsn);
    	
    	StateObject so = new StateObject();
    	
    	so.ctla = (CompletedTaskListAdapter) task_items_list.getAdapter();
    	
        return so;
    }
    
    
    
    boolean started_phase_1a = false;
    boolean started_phase_1b = false;
    
    
    boolean started_phase_2 = false;
    boolean completed_second_authenticiation_page = false;
    
    private void clean_up_and_quit() {
    	
		webview.stopLoading();
//        Toast.makeText(FlickrApprovalBrowser.this, "Success!", Toast.LENGTH_SHORT).show();
        
        // TODO: Experimental
        if (doing_flickr_setup_task) {
        	objective = BrowserObjective.GEO_PRIVACY;
        	webview.loadUrl( "http://www.flickr.com/account/geo/privacy/" );
        } else {
	    	setResult(Activity.RESULT_OK);
	    	finish();
        }
    }
    
	private class HelloWebChromeClient extends WebChromeClient {
		
		
		public void onProgressChanged(WebView view, int newProgress) {

			setProgress(newProgress*100);	// Goes on a scale of 10,000.
		}
		

		public void onReceivedTitle(WebView view, String title) {
			// This was redundant.
			/*
	        if (objective == BrowserObjective.APP_AUTHENTICATION) {
	        	if (title.equals("Flickr Services")) {
	        		completed_second_authenticiation_page = true;
	        	}
	        }
	        */
		}
		
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {

//			Log.i(TAG, "Alert message: " + message);
			return false;
		}
	}
    
	void make_single_click() {
		
//		Log.w(TAG, "WebView scale: " + webview.getScale());
		MotionEvent ev1 = MotionEvent.obtain(0, SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0);
		MotionEvent ev2 = MotionEvent.obtain(ev1);
		ev2.setAction(MotionEvent.ACTION_UP);
		webview.dispatchTouchEvent(ev1);
		webview.dispatchTouchEvent(ev2);
	}
	
	private ClickAndGo global_click_and_go;
	private class ClickAndGo implements Runnable {
		public void run() {

			make_single_click();
			
			String win = "javascript:var correct_form;for(var formid in document.forms){var doc_form=document.forms[formid];if(doc_form.getAttribute('action')=='/services/auth/'){for(var el in doc_form.elements){var form_element=doc_form.elements[el];if(form_element.type=='submit'){correct_form=doc_form;break;}}break;}}correct_form.submit();";
//			Log.w(TAG, "Attempting injection...");
			webview.loadUrl(win);

			if (completed_second_authenticiation_page)
				clean_up_and_quit();
			else
				handler.postDelayed(this, 1000);
		}
	};
	
	
	private class HelloWebViewClient extends WebViewClient {

		public void onScaleChanged(WebView view, float oldScale, float newScale) {
			
//			Log.d(TAG, "New WebView scale: " + newScale + " (old: " + oldScale + ")");
		}
		
		
		public void onLoadResource(WebView view, String url) {
//			Log.d(TAG, "Loading resource: " + url);
		}
		
		public void  onPageStarted(WebView view, String url, Bitmap favicon) {
//			Log.d(TAG, "Starting page: " + url);
			setProgressBarVisibility(true);
			setProgressBarIndeterminateVisibility(true);
			

			if (objective == BrowserObjective.APP_AUTHENTICATION) {
				if (started_phase_2) {
					if (url.equals("http://www.flickr.com/services/auth/")) {
						
	//					Log.d(TAG, "I have completed second auth page...");
//						Log.d(TAG, "URL: " + url);
						
						completed_second_authenticiation_page = true;
					}
				}
			}
			
			if (objective == BrowserObjective.GEO_PRIVACY) {
				if (url.equals("http://www.flickr.com/account/?donegeopriv=1")) {
					webview.stopLoading();
					
					if (doing_flickr_setup_task) {
						
			        	objective = BrowserObjective.GROUP_JOIN;
	        	
	
						String group_id = "1144642@N25";	// Open Field Guide
						
						
						String group_join_url_string = "http://www.flickr.com/groups_join.gne?id=" + group_id;
//						Uri group_join_uri = Uri.parse( group_join_url_string );


			        	webview.loadUrl( group_join_url_string );
	
	
					} else {
						Toast.makeText(FlickrApprovalBrowser.this, "Success!", Toast.LENGTH_SHORT).show();
						
				    	setResult(Activity.RESULT_OK);
				    	finish();
					}
					
					

				}
			} else if (objective == BrowserObjective.GROUP_JOIN) {
				if (url.contains("flickr.com/groups/")) {
					webview.stopLoading();
					
					if (doing_flickr_setup_task) {

					} else {
						Toast.makeText(FlickrApprovalBrowser.this, "Success!", Toast.LENGTH_SHORT).show();
					}
			    	setResult(Activity.RESULT_OK);
			    	finish();
				}
			}
		}
		
		public void onPageFinished  (WebView view, String url) {

//			Log.d(TAG, "Page finished URL: " + url);
			
    		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(FlickrApprovalBrowser.this);
    		boolean automate_authentication = settings.getBoolean("automate_authentication", true);
    		if (automate_authentication) {
    			
	
				switch ( objective ) {
	
				case APP_AUTHENTICATION:
					if (url.contains("www.flickr.com/services/auth/?api_key")) {
						
						if (!started_phase_1a) {
				        	started_phase_1a = true;
				        	
				        	
							String message = "Submitting Flickr authentication...";
//							Log.e(TAG, message);
						    ((CompletedTaskListAdapter) task_items_list.getAdapter()).add_item(message);
				        	
						} else if (!started_phase_1b) {
				        	started_phase_1b = true;
							String message = "Relaying application approval...";
//							Log.e(TAG, message);
						    ((CompletedTaskListAdapter) task_items_list.getAdapter()).add_item(message);
		
				    		while (webview.zoomOut());	// This will zoom all the way out.
//				    		hide_for_automation();
							
							
							String script = FlickrApprovalBrowser.this.getResources().getString(R.string.script_submit_authentication_part_1);
							view.loadUrl("javascript:" + script);
//							Log.d(TAG, "phase_1b has been injected...");
						}
					
					} else if (url.contains("www.flickr.com/services/auth/")) {
	
						
						String message = "Retrieving approval result...";
//						Log.e(TAG, message);
						
					    ((CompletedTaskListAdapter) task_items_list.getAdapter()).add_item(message);
						
						
			    		while (webview.zoomOut());	// This will zoom all the way out.
//			    		hide_for_automation();
			        	
			        	started_phase_2 = true;
			        	
			        	clean_up_and_quit();
			        	
			        	/*
			        	if (!completed_second_authenticiation_page) {

				        	global_click_and_go = new ClickAndGo(); 
				        	global_click_and_go.run();
			        	}
			        	*/
					}
					break;
					
				case GROUP_JOIN:
					if (url.contains("flickr.com/groups_join.gne")) {
						String script = FlickrApprovalBrowser.this.getResources().getString(R.string.script_submit_join_group);
						view.loadUrl("javascript:" + script);
						
						String message = "Joining identification group...";
//						Log.e(TAG, message);
					    ((CompletedTaskListAdapter) task_items_list.getAdapter()).add_item(message);
						
						
					}
					break;
					
				case GEO_PRIVACY:
					if (url.contains("flickr.com/account/geo/privacy/")) {
						String script = FlickrApprovalBrowser.this.getResources().getString(R.string.script_submit_geo_privacy);
						view.loadUrl("javascript:" + script);
						
						String message = "Setting default geotagging privacy...";
//						Log.e(TAG, message);
					    ((CompletedTaskListAdapter) task_items_list.getAdapter()).add_item(message);
						
					}	
					break;
				}
    		}
//			Log.i(TAG, "url: " + url);
			

			setProgressBarIndeterminateVisibility(false);

			if ( Uri.parse(url).getHost().equals("login.yahoo.com") ) {
				if (!yahoo_login_prompt_mutex) {
					yahoo_login_prompt_mutex = true;

		        	showDialog(DIALOG_LOGIN_INFO);
				} else if (yahoo_login_submitted_mutex) {
					
					String script = "var h=document.body.innerHTML;window." + JAVASCRIPT_INTERFACE_NAME + ".showHTML(Math.max(h.indexOf(\"Invalid ID or password\"),h.indexOf(\"This ID is not yet taken\")))";
					view.loadUrl("javascript:" + script);
				}
//				view.invokeZoomPicker();
			}
		}
		
		
		
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	
	    	if ( Uri.parse(url).getHost().equals("login.yahoo.com") ) {
	    		view.getSettings().setBuiltInZoomControls(true);

	    		webview.setInitialScale(39);
	    	} else {
	    		while (webview.zoomOut());	// This will zoom all the way out.
	    		hide_for_automation();
	    	}

//	    	Log.d(TAG, "New URL: " + url);
	    	
	        view.loadUrl(url);
	        return true;
	    }
	}
	
	boolean yahoo_login_prompt_mutex, yahoo_login_submitted_mutex;
	
	// ================================================
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_webview, menu);
        
        yahoo_login_prompt_mutex = false;
        yahoo_login_submitted_mutex = false;
        
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            
        case R.id.menu_preferences:
        	
        	Intent i = new Intent();
        	i.setClass(this, PrefsWebView.class);
        	this.startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}