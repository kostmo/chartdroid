package com.kostmo.flickr.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photosets.Photoset;
import com.aetrion.flickr.photosets.Photosets;
import com.aetrion.flickr.photosets.PhotosetsInterface;
import com.kostmo.flickr.activity.TabbedSearchActivity.GroupsList;
import com.kostmo.flickr.adapter.PhotosetAdapter;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.UserListClient;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tasks.UserContactsDialogFetcherTask;


public class PhotosetsListActivity extends Activity implements UserListClient {

	static final String TAG = Market.DEBUG_TAG; 

	final int DIALOG_PHOTO_TITLE_DESCRIPTION = 1;
	final int DIALOG_USERS_LIST = 2;
	final int DIALOG_CONFIRM_PHOTOSET_DELETE = 3;

    final int REQUEST_CODE_PHOTOSET_REPRESENTATIVE_CHOICE = 3;
	final int REQUEST_CODE_FLICKR_PHOTO_CHOOSER = 4;
	final int REQUEST_CODE_FLICKR_PHOTO_CHOOSER_GET_CONTENT = 5;
	
	
	public static final String PREFKEY_SHOW_TAXON_SEARCH_INSTRUCTIONS = "PREFKEY_SHOW_TAXON_SEARCH_INSTRUCTIONS";

	
	public final static String INTENT_EXTRA_MY_PHOTOSET_ID = "INTENT_EXTRA_MY_PHOTOSET_ID";
	
    ListView photosets_listview;
    

	GroupsList globally_stored_users_list;
    
    String globally_stored_selected_photoset_id;
    int globally_stored_selected_position;
    long globally_stored_photo_id = -1;
    String globally_stored_current_user_id;
    boolean globally_stored_creating_new_photoset = false;

    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.list_activity_photosets);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

        photosets_listview = (ListView) findViewById(R.id.category_list);
        photosets_listview.setAdapter(new PhotosetAdapter(this));
        photosets_listview.setOnItemClickListener(photoset_choice_listener);
        photosets_listview.setEmptyView(findViewById(R.id.empty_categories));

		registerForContextMenu(photosets_listview);


		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if (!settings.getBoolean(PREFKEY_SHOW_TAXON_SEARCH_INSTRUCTIONS, false)) {
//			showDialog(DIALOG_RUNONCE_INSTRUCTIONS);
		}
		
		if (savedInstanceState != null) {
//			autocomplete_textview.setText( savedInstanceState.getString("search_text") );
		}
		
		
        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	
        	PhotosetAdapter psad = (PhotosetAdapter) photosets_listview.getAdapter();
        	psad.photo_list = a.photoset_list;
	    	psad.notifyDataSetInvalidated();
	    	
        } else {
        	String user_id = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USER_NSID, null);
        	globally_stored_current_user_id = user_id;
        	new PhotosetQueryTask().execute(globally_stored_current_user_id);
        }
        
        
        findViewById(R.id.picking_indicator).setVisibility(
        		Intent.ACTION_PICK.equals( getIntent().getAction() ) ? View.VISIBLE : View.GONE);
    }

    // ========================================================================
    OnItemClickListener photoset_choice_listener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> adapter_view, View arg1, int position, long id) {
			
			Photoset photoset = (Photoset) ((PhotosetAdapter) adapter_view.getAdapter()).getItem(position);
			
			if (Intent.ACTION_PICK.equals( getIntent().getAction() )) {
				
				if (getIntent().hasCategory(IntentConstants.CATEGORY_FLICKR_PHOTO)) {
					
			    	Intent i = new Intent();
			    	i.setAction(Intent.ACTION_PICK);
			    	i.setClass(PhotosetsListActivity.this, PhotoListActivity.class);
		
			    	i.putExtra(PhotoListActivity.INTENT_EXTRA_MY_PHOTOSET_MODE, true);
			    	i.putExtra(INTENT_EXTRA_MY_PHOTOSET_ID, photoset.getId());
			    	
			    	startActivityForResult(i, REQUEST_CODE_FLICKR_PHOTO_CHOOSER);
			    	return;
				} 
				
				
				Intent result_intent = new Intent();
				result_intent.putExtra(INTENT_EXTRA_MY_PHOTOSET_ID, photoset.getId());
				
				result_intent.putExtra(
						PhotoListActivity.INTENT_EXTRA_PHOTO_ID, getIntent().getLongExtra(
								PhotoListActivity.INTENT_EXTRA_PHOTO_ID, -1));
				
			    setResult(Activity.RESULT_OK, result_intent);
		    	finish();
			} else if (getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)) {
				
		    	Intent i = new Intent();
		    	i.setAction(Intent.ACTION_GET_CONTENT);
		    	i.setClass(PhotosetsListActivity.this, PhotoListActivity.class);
	
		    	i.putExtra(PhotoListActivity.INTENT_EXTRA_MY_PHOTOSET_MODE, true);
		    	i.putExtra(INTENT_EXTRA_MY_PHOTOSET_ID, photoset.getId());
		    	
		    	startActivityForResult(i, REQUEST_CODE_FLICKR_PHOTO_CHOOSER_GET_CONTENT);
		    	return;
				
			} else {
		    	Intent i = new Intent();
		    	i.setClass(PhotosetsListActivity.this, PhotoListActivity.class);
	
		    	i.putExtra(PhotoListActivity.INTENT_EXTRA_MY_PHOTOSET_MODE, true);
		    	i.putExtra(INTENT_EXTRA_MY_PHOTOSET_ID, photoset.getId());
	
		    	startActivity(i);
			}
		}
    };    

    // ========================================================================
    class StateRetainer {
    	List<Photoset> photoset_list;
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	
    	StateRetainer state = new StateRetainer();
    	state.photoset_list = ((PhotosetAdapter) photosets_listview.getAdapter()).photo_list;
        return state;
    }

    // ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	Log.i(TAG, "onSaveInstanceState");

    	out_bundle.putString("list_header_text", (String) ((TextView) findViewById(R.id.search_result_count)).getText());
    	
    	out_bundle.putString("selected_photoset_id", globally_stored_selected_photoset_id);
    	out_bundle.putInt("selected_position", globally_stored_selected_position);
    	

    	out_bundle.putBoolean("creating_new_photoset", globally_stored_creating_new_photoset);

    	out_bundle.putLong("photo_id", globally_stored_photo_id);
    	
    	out_bundle.putString("current_user_id", globally_stored_current_user_id);
    	
    }

    // ========================================================================
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	Log.i(TAG, "onRestoreInstanceState");

    	((TextView) findViewById(R.id.search_result_count)).setText( in_bundle.getString("list_header_text") );
    	
    	globally_stored_selected_photoset_id = in_bundle.getString("selected_photoset_id");
    	globally_stored_selected_position = in_bundle.getInt("selected_position");
    	
    	globally_stored_creating_new_photoset = in_bundle.getBoolean("creating_new_photoset");
    	

    	globally_stored_photo_id = in_bundle.getLong("photo_id");
    	

    	globally_stored_current_user_id = in_bundle.getString("current_user_id");
    }

    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	
    	Log.d(TAG, "Executing onPrepareDialog()");
    	
        switch (id) {
        case DIALOG_PHOTO_TITLE_DESCRIPTION:
        {
        	
        	if (!globally_stored_creating_new_photoset) {
	        	
	        	final EditText title_textbox = (EditText) dialog.findViewById(R.id.textbox_photo_title);
	        	final EditText description_textbox = (EditText) dialog.findViewById(R.id.textbox_photo_description);
		        
	        	Photoset photoset = (Photoset) ((PhotosetAdapter) photosets_listview.getAdapter()).getItem( globally_stored_selected_position );
	    		
	        	title_textbox.setText( photoset.getTitle() );
	        	description_textbox.setText( photoset.getDescription() );
        	}
        	
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
    	Log.d(TAG, "Executing onCreateDialog()");
    	
        switch (id) {
        case DIALOG_USERS_LIST:
        {
        	return new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_info)
	        .setTitle("Select user")
	        .setItems(globally_stored_users_list.names, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {

	            	globally_stored_current_user_id = (String) globally_stored_users_list.ids[whichButton];
	            	new PhotosetQueryTask().execute(globally_stored_current_user_id);
	            }
	        })
	        .create();
        }
        case DIALOG_PHOTO_TITLE_DESCRIPTION:
        {
        	
        	View dialog_photo_description = factory.inflate(R.layout.dialog_photo_description, null);
        	
        	
        	final EditText title_textbox = (EditText) dialog_photo_description.findViewById(R.id.textbox_photo_title);
        	final EditText description_textbox = (EditText) dialog_photo_description.findViewById(R.id.textbox_photo_description);
	        
        	
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Title / Description")
            .setView(dialog_photo_description)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	if (globally_stored_creating_new_photoset) {
                		createPhotoset(
                			title_textbox.getText().toString(),
	            			description_textbox.getText().toString()
	            		);
                	} else {
	                	updatePhotosetTitleDescription(
	                		globally_stored_selected_photoset_id,
	            			title_textbox.getText().toString(),
	            			description_textbox.getText().toString()
	           			);
                	}
                }
            })
            .create();
        }
        case DIALOG_CONFIRM_PHOTOSET_DELETE:
        {

            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Delete photoset")
            .setMessage("Are you sure?")
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	deletePhotoset(globally_stored_selected_photoset_id);

                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, null)
            .create();
        }
        }

        return null;
    }
    

    // ========================================================================
    void deletePhotoset(String photoset_id) {
    	
		Flickr flickr = null;
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.WRITE);
        requestContext.setAuth(auth);
		
		PhotosetsInterface photos_interface = flickr.getPhotosetsInterface();

		try {
			photos_interface.delete(photoset_id);
			new PhotosetQueryTask().execute(globally_stored_current_user_id);
	    	
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
			

            Toast.makeText(this, e.getErrorMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		

    }
    
    

    // ========================================================================
    void createPhotoset(String title, String description) {
    	
		Flickr flickr = null;
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.WRITE);
        requestContext.setAuth(auth);
		
		PhotosetsInterface photos_interface = flickr.getPhotosetsInterface();
		try {
			
			photos_interface.create(title, description, Long.toString( globally_stored_photo_id ) );

			new PhotosetQueryTask().execute(globally_stored_current_user_id);
	    	
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
            Toast.makeText(this, e.getErrorMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
    }
    

    // ========================================================================
    void updatePhotosetTitleDescription(String photoset_id, String title, String description) {
    	
		Flickr flickr = null;
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.WRITE);
        requestContext.setAuth(auth);
		
		PhotosetsInterface photos_interface = flickr.getPhotosetsInterface();

		try {
			
			photos_interface.editMeta(photoset_id, title, description);
			
			new PhotosetQueryTask().execute(globally_stored_current_user_id);;
	    	
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {

            Toast.makeText(this, e.getErrorMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
    }
    

    // ========================================================================
	private class PhotosetQueryTask extends AsyncTask<String, Void, Collection<Photoset>> {

		ProgressDialog wait_dialog;
		
		void instantiate_latent_wait_dialog() {

			wait_dialog = new ProgressDialog(PhotosetsListActivity.this);
			wait_dialog.setMessage("Fetching photosets...");
			wait_dialog.setIndeterminate(true);
			wait_dialog.setCancelable(false);
			wait_dialog.show();
		}
		
		
	     @Override
	     protected void onPreExecute() {
	 		 instantiate_latent_wait_dialog();
	     }

		@Override
	    protected Collection<Photoset> doInBackground(String... user_ids) {
	    	Flickr flickr = null;
	        try {
				flickr = new Flickr(
						ApiKeys.FLICKR_API_KEY,	// My API key
						ApiKeys.FLICKR_API_SECRET,	// My API secret
			        new REST()
			    );
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			
			RequestContext requestContext = RequestContext.getRequestContext();
	        Auth auth = new Auth();

	        // Get token from saved Settings
	        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PhotosetsListActivity.this);
			String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
	        auth.setToken( stored_auth_token );
	        auth.setPermission(Permission.READ);
	        requestContext.setAuth(auth);
	        
	        
	        String user_id = user_ids[0];
	        
	        
	        try {
	        	Photosets photosets = (Photosets) flickr.getPhotosetsInterface().getList(user_id);

				return photosets.getPhotosets();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (FlickrException e) {
				e.printStackTrace();
			}

	         return new ArrayList<Photoset>();
	     }

	     @Override
	     protected void onProgressUpdate(Void... cis) {
  			
	     }

	     @Override
	     protected void onPostExecute(Collection<Photoset> list) {

	    	 PhotosetAdapter psad = (PhotosetAdapter) photosets_listview.getAdapter();
	    	 psad.photo_list.clear();
	    	 psad.photo_list.addAll(list);

	    	 psad.notifyDataSetInvalidated();
	    	 
	    	 ((TextView) findViewById(R.id.search_result_count)).setText("Listing " + psad.photo_list.size() + " photosets.");
	    	 
     		 wait_dialog.dismiss();
	     }
	 }


    // ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_photoset_list, menu);
        
        return true;
    }
    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

	    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    String user_id = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USER_NSID, null);
	    boolean is_me = user_id != null && user_id.equals( globally_stored_current_user_id );
	    
	    menu.findItem(R.id.menu_new_photoset).setVisible(is_me);
        
        return true;
    }

    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        case R.id.menu_select_user:
        {
	    	if (globally_stored_users_list != null)
	        	showDialog(DIALOG_USERS_LIST);
	    	else
	    		new UserContactsDialogFetcherTask(this).execute();
			break;
        }
        
        case R.id.menu_new_photoset:
        {
	    	Intent i = new Intent(Intent.ACTION_PICK);
	    	i.setClass(this, PhotoListActivity.class);
	    	i.putExtra(PhotoListActivity.INTENT_EXTRA_MY_PHOTOSTREAM_MODE, true);
	    	
        	startActivityForResult(i, REQUEST_CODE_PHOTOSET_REPRESENTATIVE_CHOICE);
        	

            Toast.makeText(this, "Choose primary photo...", Toast.LENGTH_LONG).show();
        	
			break;
        }        
        
        
        case R.id.menu_help:
        {
//        	showDialog(DIALOG_RUNONCE_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_preferences:
        {

        	return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }


    // ========================================================================
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.context_photoset_list, menu);
	    
	    menu.setHeaderTitle("Photoset action:");
	    
	    
	    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    String user_id = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USER_NSID, null);	    
	    boolean is_me = user_id.equals( globally_stored_current_user_id );
	    menu.findItem(R.id.menu_edit_title_description).setVisible(is_me);
	    menu.findItem(R.id.menu_disband_photoset).setVisible(is_me);
	}

    // ========================================================================
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		Photoset photoset = (Photoset) ((PhotosetAdapter) photosets_listview.getAdapter()).getItem(info.position);

		switch (item.getItemId()) {
        case R.id.menu_edit_title_description:
        {

    		globally_stored_selected_position = info.position;
        	globally_stored_selected_photoset_id = photoset.getId();
        	
        	globally_stored_creating_new_photoset = false;
        	showDialog(DIALOG_PHOTO_TITLE_DESCRIPTION);
			return true;
        }
        case R.id.menu_disband_photoset:
        {
    		globally_stored_selected_position = info.position;
        	globally_stored_selected_photoset_id = photoset.getId();
        	showDialog(DIALOG_CONFIRM_PHOTOSET_DELETE);
			return true;
        }
		
		default:
			return super.onContextItemSelected(item);
		}
	}
	

    // ========================================================================
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {

	   		case REQUEST_CODE_PHOTOSET_REPRESENTATIVE_CHOICE:
	   		{
	   			globally_stored_photo_id = data.getLongExtra(PhotoListActivity.INTENT_EXTRA_PHOTO_ID, -1);
	   			Log.d(TAG, "Selected cover photo: " + globally_stored_photo_id);
	   			
	   			
	        	globally_stored_creating_new_photoset = true;
	        	showDialog(DIALOG_PHOTO_TITLE_DESCRIPTION);
	        	
	   			break;
	   		}
	   		case REQUEST_CODE_FLICKR_PHOTO_CHOOSER:
	   		{
	   			long photo_id = data.getLongExtra(PhotoListActivity.INTENT_EXTRA_PHOTO_ID, -1);
	   			Log.e(TAG, "Selected photo id: " + photo_id);
	   			
	    		Intent result_intent = new Intent();

	    		result_intent.putExtra(PhotoListActivity.INTENT_EXTRA_PHOTO_ID, photo_id);
	    		

		    	setResult(Activity.RESULT_OK, result_intent);
		    	finish();
		    	
	   			break;
	   		}
	   		case REQUEST_CODE_FLICKR_PHOTO_CHOOSER_GET_CONTENT:
	   		{
	   			/*
	   			long photo_id = data.getLongExtra(PhotoListActivity.INTENT_EXTRA_PHOTO_ID, -1);
	   			Log.e(TAG, "Selected photo id: " + photo_id);
	   			
	    		Intent result_intent = new Intent();

	    		result_intent.putExtra(PhotoListActivity.INTENT_EXTRA_PHOTO_ID, photo_id);
	    		

		    	setResult(Activity.RESULT_OK, result_intent);
		    	finish();
		    	*/
	   			Intent i = new Intent();
	   			i.setData(data.getData());
	   			setResult(Activity.RESULT_OK, i);
	   			finish();
	   			
	   			break;
	   		}
	   		default:
		    	break;
		   }
		}
    }

    // ========================================================================
    public void generateManagedUsersDialog(CharSequence[] group_names, CharSequence[] group_ids) {
    	
    	globally_stored_users_list = new GroupsList();
    	globally_stored_users_list.names = group_names;
    	globally_stored_users_list.ids = group_ids;
    	    	
    	showDialog(DIALOG_USERS_LIST);
    }
}

