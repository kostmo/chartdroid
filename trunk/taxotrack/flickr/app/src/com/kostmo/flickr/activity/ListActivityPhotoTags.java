package com.kostmo.flickr.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.tags.Tag;
import com.kostmo.flickr.activity.prefs.PrefsTaglist;
import com.kostmo.flickr.adapter.FlickrTagListAdapter;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.containers.NetworkUnavailableException;
import com.kostmo.flickr.data.BetterMachineTagDatabase;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tasks.TagConventionFetcherTask;
import com.kostmo.flickr.tools.TagConventionParser;

public class ListActivityPhotoTags extends BasicTagListActivity {

	static final String TAG = Market.DEBUG_TAG; 

    public static final String INTENT_EXTRA_REFRESH_NEEDED = "INTENT_EXTRA_REFRESH_NEEDED";  

    public static final String PREFKEY_SHOW_TAGGING_INSTRUCTIONS = "PREFKEY_TAGGING_INSTRUCTIONS";

	final int DIALOG_TAGGING_INSTRUCTIONS = 7;
	
	final int REQUEST_CODE_APPENGINE_FETCH = 1;
	final int REQUEST_CODE_APPENGINE_DELETE = 2;
    final int REQUEST_CODE_MACHINE_TAG_CHOOSER = 3;
	

	BetterMachineTagDatabase machinetag_database;
	SharedPreferences settings;

	boolean changes_made = false;
	String user_nsid;
	String error_message = null;

	// ========================================================================
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.taglist);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        
        
  	   
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);
        this.machinetag_database = new BetterMachineTagDatabase(this);
        
        this.user_nsid = this.settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USER_NSID, null);
    	
    	
        
        
        
        

    	setListAdapter(new FlickrTagListAdapter(this, this.user_nsid));

        // Deal with orientation change
	    final PreserveConfigurationWrapper a = (PreserveConfigurationWrapper) getLastNonConfigurationInstance();
        if (a != null) {
        	initiate_tag_retrieval( getIntent().getLongExtra(IntentConstants.PHOTO_ID, INVALID_PHOTO_ID), false);
        } else {
            initiate_tag_retrieval( getIntent().getLongExtra(IntentConstants.PHOTO_ID, INVALID_PHOTO_ID), true);
        }


		registerForContextMenu(getListView());
        
		// TODO: Should this be done regardless?
        new TagConventionFetcherTaskExtended(this).execute();

		if (!this.settings.getBoolean(PREFKEY_SHOW_TAGGING_INSTRUCTIONS, false)) {
			showDialog(DIALOG_TAGGING_INSTRUCTIONS);
		}
    }

	// ========================================================================
    private class PreserveConfigurationWrapper {
    	
    }

	// ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	PreserveConfigurationWrapper pcw = new PreserveConfigurationWrapper();
        return pcw;
    }

	// ========================================================================
	static String update_tags(Context context, Flickr flickr, long photo_id, Tag old_tag, String[] new_tags) {
		
		RequestContext requestContext = RequestContext.getRequestContext();
        Auth auth = new Auth();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
        auth.setPermission(Permission.WRITE);
        requestContext.setAuth(auth);
        
        
        
        
		try {
			if (old_tag != null)
				flickr.getPhotosInterface().removeTag(old_tag.getId());
			if (new_tags != null) {

	    		Log.w(TAG, "About to add " + new_tags.length + " tags.");
				flickr.getPhotosInterface().addMachineTags(Long.toString(photo_id), new_tags);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
			
			if (e.getErrorCode() == "99")
				return e.getErrorMessage();
			else e.printStackTrace();
			
		}
		
		
		return null;
	}

	// ========================================================================
	// Override the BACK button so we can return a result.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        if (keyCode == KeyEvent.KEYCODE_BACK) {

        	Intent i = new Intent();
   	    	i.putExtra(INTENT_EXTRA_REFRESH_NEEDED, changes_made);
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
	    	
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

	// ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	
    	Log.d(TAG, "Executing onPrepareDialog()");
    	
        switch (id) {
        case DIALOG_STANDARD_TAG_CREATION:
        {
        	final Tag tag = getCurrentManipulatingTag();
        	dialog.setTitle(tag == null ? R.string.flickr_add_tag_title : R.string.flickr_change_tag_title);

            final EditText tag_box = (EditText) dialog.findViewById(R.id.tag_edit);
            if (tag != null)
            	tag_box.setText(tag.getRaw());
        	
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
        case DIALOG_TAGGING_INSTRUCTIONS:
        {

	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

	        ((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_tagging);
	        
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Tagging")
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
            		settings.edit().putBoolean(PREFKEY_SHOW_TAGGING_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
                }
            })
            .create();
        }
        case DIALOG_STANDARD_TAG_CREATION:
        {


            View tagTextEntryView;
            tagTextEntryView = factory.inflate(R.layout.dialog_plain_tag, null);
            
            final EditText tag_box = (EditText) tagTextEntryView.findViewById(R.id.tag_edit);

            
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	new TagUpdateTask(
                			getIntent().getLongExtra(IntentConstants.PHOTO_ID, INVALID_PHOTO_ID ),
                			getCurrentManipulatingTag()).execute(new String[] { tag_box.getText().toString()});

                }
            };
            
            
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getCurrentManipulatingTag() == null ? R.string.flickr_add_tag_title : R.string.flickr_change_tag_title)
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, listener)
            .setNegativeButton(R.string.alert_dialog_cancel, null)
            .create();
        	
        }
        	
        case DIALOG_NEW_TAG_PROMPT:
        	
        	return new AlertDialog.Builder( ListActivityPhotoTags.this )
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.add_tag_heading)
            .setItems(R.array.flickr_tag_types, new DialogInterface.OnClickListener() {

    			public void onClick(DialogInterface dialog, int which) {
    				
    				switch (TagAdditionActions.values()[which]) {
    				
    				case PLAIN_TAG: // Standard tag chosen
    				{
    					// Set the dialog parameter with this global variable:
    					globally_stored_tag_to_change = null;
    					showDialog(DIALOG_STANDARD_TAG_CREATION);
    					break;
    				}
    				case MACHINE_TAG_CUSTOM:	// Taxonomy tag chosen
    				{
    			    	Intent i = new Intent(Intent.ACTION_PICK);
    			    	i.setClass(ListActivityPhotoTags.this, ListActivityTagConvention.class);
    			    	startActivityForResult(i, REQUEST_CODE_MACHINE_TAG_CHOOSER);
    					
    			    	
    			    	
//    					showDialog(MACHINE_TAG_CREATION_DIALOG);
    					break;
    				}	
    				default:
    					/*
    	            	new TagUpdateTask(
                			getIntent().getLongExtra(PHOTO_ID),
                			null).execute(new String[] {"bogus_tag"});
                		*/
    				}
    			}
            })
            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	Log.d(TAG, "Dialog cancelled.");
                }
            })
            .create();
        }
        
        return null;
    }

	// ========================================================================
    class TagUpdateTask extends AsyncTask<String, Void, String> {

    	long photo_id;
    	Tag old_tag;
    	String new_tag;

    	Flickr flickr = null;
    	
    	TagUpdateTask(long photo_id, Tag old_tag) {
    		
    		this.photo_id = photo_id;
    		this.old_tag = old_tag;
    	}
    	
    	@Override
        public void onPreExecute() {
			
    		Log.w(TAG, "Began tagging process.");
			
            try {
    			flickr = new Flickr(
    					ApiKeys.FLICKR_API_KEY,	// My API key
    					ApiKeys.FLICKR_API_SECRET,	// My API secret
    		        new REST()
    		    );
    		} catch (ParserConfigurationException e) {
    			e.printStackTrace();
    		}
    	}
    	
		@Override
		protected String doInBackground(String... new_tags) {

			changes_made = true;
			return update_tags(ListActivityPhotoTags.this, flickr, photo_id, old_tag, new_tags);
		}
    	
	    @Override
	    public void onPostExecute(String error_message) {
	    	if (error_message == null)
	    		new TagFetcherTask(flickr).execute(photo_id);
	    	else
	    		Toast.makeText(ListActivityPhotoTags.this, error_message, Toast.LENGTH_LONG).show();
	    }
    }
	
	// ========================================================================
	void prompt_change_machine_tag(final Tag tag) {


		final MachineTag parts = new MachineTag(tag.getRaw());
		final String[] tag_value_options = machinetag_database.getTagValueOptions(parts);

		
        AlertDialog d = new AlertDialog.Builder(ListActivityPhotoTags.this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle("New value for \"" + parts.predicate + "\"")
        .setItems(tag_value_options, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				
            	Log.d(TAG, "Chosen item: " + which);
            	
            	parts.value = tag_value_options[which];
            	
            	new TagUpdateTask(
            			getIntent().getLongExtra(IntentConstants.PHOTO_ID, INVALID_PHOTO_ID ),
            			tag).execute(new String[] {parts.toString()});
			}
        })
        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            	Log.d(TAG, "Dialog cancelled.");
            }
        })
        .create();
    
        d.show();
	}

	// ========================================================================
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_tag_list, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Tag action:");
	}

	// ========================================================================
    void tag_changer(Tag tag) {
    	
		Log.d(TAG, "Tag id: " + tag.getId());
		
		if (tag.isMachineTag()) {
			prompt_change_machine_tag(tag);
		} else {
			
			globally_stored_tag_to_change = tag;
			showDialog(DIALOG_STANDARD_TAG_CREATION);
		}
    }

	// ========================================================================
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();


		Tag tag = (Tag) getListView().getAdapter().getItem(info.position);
		
		switch ( item.getItemId() ) {
		case R.id.menu_tag_remove:
		{
			new TagUpdateTask(
        			getIntent().getLongExtra(IntentConstants.PHOTO_ID, INVALID_PHOTO_ID ),
        			tag).execute((String[]) null);
			break;
		}	
		case R.id.menu_tag_change_value:
		{	
			tag_changer(tag);
			break;
		}
		default:
			break;
		}

		return super.onContextItemSelected(item);
	}

	// ========================================================================
    public static boolean test_tag_conventions_exist(Context context) {

        BetterMachineTagDatabase DBHelper = new BetterMachineTagDatabase(context);
        return DBHelper.isTagConventionDownloaded();
    }

	// ========================================================================
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Tag tag = (Tag) getListView().getAdapter().getItem(position);
    	tag_changer(tag);

//    	Toast.makeText(ListActivityPhotoTags.this, "Vote for tag here", Toast.LENGTH_SHORT).show();
    }

	// ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_tag_list, menu);

        return true;
    }

	// ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

		boolean already_downloaded = test_tag_conventions_exist(this);


		menu.findItem(R.id.menu_load_convention).setVisible( !already_downloaded );
		menu.findItem(R.id.menu_purge_convention).setVisible( already_downloaded );
        return true;
    }

	// ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
        {	

            showDialog(DIALOG_TAGGING_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_preferences:
        {
        	Intent i = new Intent();
        	i.setClass(this, PrefsTaglist.class);
        	this.startActivity(i);
        	
        	return true;
        }
		
		
		case R.id.menu_plot_tag_authors:
		{
			

        	
        	Map<String, Integer> author_bins = new LinkedHashMap<String, Integer>();
        	Map<String, String> author_translator = new HashMap<String, String>();
         	
            FlickrTagListAdapter ad = (FlickrTagListAdapter) getListAdapter();
    		for (Tag t : ad.raw_tags_collection) {
 
    			String author = t.getAuthor();
    			author_translator.put(author, t.getAuthorName());
    			
    			int current_count = 1;
    			
				if (author_bins.containsKey(author)) {
					current_count = author_bins.get(author);
					current_count++;
				}

				author_bins.put(author, current_count);
    		}

        	int[] colors = new int[author_bins.size()];
        	String[] chart_key_labels = new String[author_bins.size()];
        	int[] author_frequencies = new int[author_bins.size()];
        	int j=0;
        	for (String author : author_bins.keySet()) {
        		
        		chart_key_labels[j] = author_translator.get(author);
        		author_frequencies[j] = author_bins.get(author);
        		colors[j] = Color.HSVToColor(new float[] {360 * j / (float) author_bins.size(), 0.6f, 1});
    			j++;
        	}


        	Intent i = new Intent("com.googlecode.chartdroid.intent.action.PLOT");
        	i.putExtra(Intent.EXTRA_TITLE, "Tag Authors");
        	i.putExtra("com.googlecode.chartdroid.intent.extra.LABELS", chart_key_labels);
        	i.putExtra("com.googlecode.chartdroid.intent.extra.DATA", author_frequencies);
        	i.putExtra("com.googlecode.chartdroid.intent.extra.COLORS", colors);
        	
        	
        	Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_PACKAGE_SEARCH, i, -1);

        	
            return true;
        }
		case R.id.menu_plot_tag_usage:
		{
			

        	Intent i = new Intent("com.googlecode.chartdroid.intent.action.PLOT");



        	String[] chart_key_labels = {"Machine tag", "Standard tag"};
        	int[] tag_counters = new int[2]; 
        	for (int j=0; j<tag_counters.length; j++)
        		tag_counters[j] = 0;

         	
            FlickrTagListAdapter ad = (FlickrTagListAdapter) getListAdapter();


    		for (Tag t : ad.raw_tags_collection) {
    			if (t.isMachineTag()) {

    				tag_counters[0]++;
    				/*
    				MachineTag mt = new MachineTag(t);
    			
	    			if (mt.namespace.equals("identification") && mt.predicate.equals("status")) {
	    				
	
	    			}
	    			*/
    			} else {
    				

    				tag_counters[1]++;
    			}
    		}

        	i.putExtra(Intent.EXTRA_TITLE, "Tag Types");
        	i.putExtra("com.googlecode.chartdroid.intent.extra.LABELS", chart_key_labels);
        	i.putExtra("com.googlecode.chartdroid.intent.extra.DATA", tag_counters);
        	
        	Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_PACKAGE_SEARCH, i, -1);
            return true;
        }
        case R.id.menu_add_tag:
        	
        	showDialog(DIALOG_NEW_TAG_PROMPT);
        	
            return true;
        case R.id.menu_view_on_flickr:
        case R.id.menu_add_comment:
        {

        	// TODO: Combine all usages of this idiom into a static library function:
        	
        	String photo_id = Long.toString( getIntent().getLongExtra(IntentConstants.PHOTO_ID, INVALID_PHOTO_ID) );
        	
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
			
			PhotosInterface photos_interface = flickr.getPhotosInterface();
			

			try {
				String url = photos_interface.getPhoto(photo_id).getUrl();

				Log.d(TAG, "photo url: " + url);
				

				Uri flickr_destination = Uri.parse( url );

	        	// Launches the standard browser.
	        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FlickrException e) {
				
				Toast.makeText(this, e.getErrorMessage(), Toast.LENGTH_LONG).show();
				
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}

	        return true;
        }
        case R.id.menu_browse_conventions:
        {
	    	Intent i = new Intent(this, ListActivityTagConvention.class);
	    	startActivity(i);

        	Log.e(TAG, "Not implemented?");
            return true;           
        }    
        case R.id.menu_purge_convention:
 
        	new TagConventionDeletionTask().execute();
            return true;
            
        case R.id.menu_update_convention:
        	
        	new TagConventionUpdateTask().execute();
            return true;
            

        case R.id.menu_load_convention:
        	
        	new TagConventionFetcherTaskExtended(this).execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

	// ========================================================================
    private void purge_tag_convention_database() {

		SQLiteDatabase db = machinetag_database.getWritableDatabase();
		machinetag_database.onUpgrade(db, 0, 0);
		db.close();
    }

	// ========================================================================
    class TagConventionUpdateTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			
			purge_tag_convention_database();
			
			try {
				new TagConventionParser(ListActivityPhotoTags.this, "tagging_convention2").parse();
			} catch (NetworkUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
    	
	    @Override
	    public void onPostExecute(Void nothing) {

	    	Toast.makeText(ListActivityPhotoTags.this, "Updated tag conventions.", Toast.LENGTH_SHORT).show();
	    }
    }

	// ========================================================================
    class TagConventionDeletionTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			purge_tag_convention_database();
			return null;
		}
    	
	    @Override
	    public void onPostExecute(Void nothing) {

	    	Toast.makeText(ListActivityPhotoTags.this, "Purged tag conventions.", Toast.LENGTH_SHORT).show();
	    }
    }

	// ========================================================================
    public class TagConventionFetcherTaskExtended extends TagConventionFetcherTask {

    	TagConventionFetcherTaskExtended(Context context) {
    		super(context);
    	}

	    @Override
	    public void onPostExecute(Void nothing) {
	    	if (loaded_conventions) {
	    		Toast.makeText(ListActivityPhotoTags.this, "Loaded tag conventions.", Toast.LENGTH_SHORT).show();
	    		
				FlickrTagListAdapter adapter = ((FlickrTagListAdapter) getListAdapter());

				adapter.sort_taglist();
//		   		adapter.notifyDataSetInvalidated();
	    	}
	    }
    }

	// ========================================================================
    class TagFetcherTask extends AsyncTask<Long, Void, Collection<Tag>> {

		Flickr flickr;

		Collection<Tag> raw_collection = new ArrayList<Tag>();
		TagFetcherTask(Flickr f) {
			this.flickr = f;
		}
		
    	@Override
        public void onPreExecute() {
    	}
    	
		@Override
		protected Collection<Tag> doInBackground(Long... photo_ids) {
			
			String photo_id = Long.toString( photo_ids[0] );

    		try {
    			raw_collection = flickr.getTagsInterface().getListPhoto(photo_id).getTags();

    			// Look for the "tsn" tag:
    			for (Tag t : raw_collection) {
    				if (t.isMachineTag()) {
	    				MachineTag mt = new MachineTag(t);
    				}
    			}

    			boolean suppress_machine_tags = settings.getBoolean("suppress_machine_tags", false);
    			boolean suppress_plain_tags = settings.getBoolean("suppress_plain_tags", false);

    			List<Tag> filtered_collection = new ArrayList<Tag>();
    			

    			
    			for (Tag tag : raw_collection) {
    				if ( tag.isMachineTag() ) {
    					if ( !suppress_machine_tags )
	        				filtered_collection.add(tag);
    				} else if ( !suppress_plain_tags )
    					filtered_collection.add(tag);
    			}
				
    			Log.d(TAG, "Reduced taglist from " + raw_collection.size() + " to " + filtered_collection.size());
    			return filtered_collection;

			} catch (IOException e) {
				e.printStackTrace();
				error_message = e.getLocalizedMessage();
			} catch (SAXException e) {
				e.printStackTrace();
				error_message = e.getLocalizedMessage();
			} catch (FlickrException e) {
				e.printStackTrace();
				error_message = e.getLocalizedMessage();
			}
			
			return new ArrayList<Tag>();
		}
    	


	    @Override
	    public void onPostExecute(Collection<Tag> tags) {
	    	
	    	if (error_message != null) {
	    		Toast.makeText(ListActivityPhotoTags.this, error_message, Toast.LENGTH_LONG).show();
	    	}
	    	
	    	TextView empty_textview = (TextView) findViewById(android.R.id.empty);
	    	if (tags.size() == 0) {
	    		empty_textview.setText("No tags to display (hiding " + raw_collection.size() + ").");
	    	}
	    	
	    	

			FlickrTagListAdapter adapter = ((FlickrTagListAdapter) getListAdapter());
			
			adapter.raw_tags_collection = raw_collection;
			
			
			Map<String, Integer> users = new HashMap<String, Integer>();
			for (Tag tag : adapter.raw_tags_collection) {
				
				int count = 1;
				if (users.containsKey(tag.getAuthor())) {
					count = users.get(tag.getAuthor());
					count++;
				}
				users.put(tag.getAuthor(), count);
			}
			

			adapter.filtered_tags_collection.clear();
			adapter.filtered_tags_collection.addAll( tags );
	   		adapter.notifyDataSetInvalidated();
	   		
	   		
			TextView taglist_header = (TextView) findViewById(R.id.tag_list_header);
			taglist_header.setText("Showing " + adapter.filtered_tags_collection.size() + " of " + adapter.raw_tags_collection.size() + " tags by " + users.size() + " author(s)");
	    }
    }

	// ========================================================================
    public void initiate_tag_retrieval(final long photo_id, boolean refetch_tags) {

    	// Even though we knew it before, we have to re-fetch the description
    	// and thumbnail, because now we're in a different activity.
    	
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

    	if (refetch_tags)
    		new TagFetcherTask(flickr).execute(photo_id);
    }

	// ========================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
	
	   		case REQUEST_CODE_APPENGINE_FETCH:
	   			
				Log.d(TAG, "Foo.");
	   			break;
	   			
	   		case REQUEST_CODE_APPENGINE_DELETE:

				Log.d(TAG, "Bar.");

	   			break;
	   			
	   			
	   		case REQUEST_CODE_MACHINE_TAG_CHOOSER:
	   		{
	   			String machine_namespace = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_NAMESPACE);
	   			String machine_predicate = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_PREDICATE);
	   			String machine_value = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_VALUE);

	   			MachineTag mt = new MachineTag(machine_namespace, machine_predicate, machine_value);
	   			
            	new TagUpdateTask(
            			getIntent().getLongExtra(IntentConstants.PHOTO_ID, INVALID_PHOTO_ID ),
            			null).execute(new String[] { mt.toString() });
	   			break;
	   		}
	   		default:
		    	break;
		   }
		}
    }

	// ========================================================================
    enum TaxonomyTaggingStage {
    	RETRIEVING_HIERARCHY, REMOVING_TAGS, ADDING_TAGS, REFRESHING_LIST
    }

	// ========================================================================
	class CustomProgressPacket {
		TaxonomyTaggingStage stage;
		int stage_progress_max;
		int stage_current_progress = 0;
		
		CustomProgressPacket(TaxonomyTaggingStage s) {
			stage = s;
		}
	}
}
