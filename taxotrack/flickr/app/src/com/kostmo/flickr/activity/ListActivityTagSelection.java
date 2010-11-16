package com.kostmo.flickr.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.aetrion.flickr.tags.Tag;
import com.kostmo.flickr.adapter.FlickrTagListAdapter;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.containers.NetworkUnavailableException;
import com.kostmo.flickr.data.BetterMachineTagDatabase;
import com.kostmo.flickr.data.DatabaseSearchHistory;
import com.kostmo.flickr.tasks.TagConventionFetcherTask;
import com.kostmo.flickr.tools.TagConventionParser;

public class ListActivityTagSelection extends BasicTagListActivity {

	static final String TAG = "ListActivityTagSelection"; 

	
    public static final String INTENT_EXTRA_REFRESH_NEEDED = "INTENT_EXTRA_REFRESH_NEEDED";  

    public static final String PREFKEY_SHOW_TAGGING_INSTRUCTIONS = "PREFKEY_TAGGING_INSTRUCTIONS";
	
	
	final int REQUEST_CODE_APPENGINE_FETCH = 1;
	final int REQUEST_CODE_APPENGINE_DELETE = 2;
    final int REQUEST_CODE_MACHINE_TAG_CHOOSER = 3;
    final int REQUEST_CODE_MACHINE_TAG_EDITOR = 4;
	

    int pending_tag_edit_position;
    
	BetterMachineTagDatabase machinetag_database;
	String error_message = null;

	TextView tag_list_header;

	// ========================================================================
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.tag_selection);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

        
		this.machinetag_database = new BetterMachineTagDatabase(this);


		this.tag_list_header = (TextView) findViewById(R.id.tag_list_header);

		findViewById(R.id.button_tag_selection_finished).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finishWithTags();
			}
		});
		
		findViewById(R.id.button_add_tag).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	        	showDialog(DIALOG_NEW_TAG_PROMPT);
			}
		});
		
		
		FlickrTagListAdapter adapter = new FlickrTagListAdapter(this);
    	setListAdapter(adapter);
		

        // Deal with orientation change
	    final PreserveConfigurationWrapper a = (PreserveConfigurationWrapper) getLastNonConfigurationInstance();
        if (a != null) {
        	this.pending_tag_edit_position = a.pending_tag_edit_position;
        	adapter.filtered_tags_collection.addAll( a.tags );
        	
        } else {

    		Intent passed_intent = getIntent();
    		if (passed_intent != null && passed_intent.hasExtra(BatchUploaderActivity.INTENT_EXTRA_TAGS)) {
    			for (String tag_string : passed_intent.getStringArrayListExtra(BatchUploaderActivity.INTENT_EXTRA_TAGS)) {
    				
    				Tag parsed_tag;

    				if (MachineTag.checkIsParseable(tag_string)) {
    					parsed_tag = new MachineTag( tag_string );
    				} else {
    					parsed_tag = new Tag();
    					parsed_tag.setValue( tag_string );
    					parsed_tag.setRaw( tag_string );
    				}
    				adapter.filtered_tags_collection.add( parsed_tag );
    			}
    		}
        }

		registerForContextMenu(getListView());

		// TODO: Should this be done regardless?
        new TagConventionFetcherTaskExtended(this).execute();
        
        refresh(adapter);
    }

	// ========================================================================
    private class PreserveConfigurationWrapper {
    	int pending_tag_edit_position;
    	List<Tag> tags;
    }

	// ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	PreserveConfigurationWrapper pcw = new PreserveConfigurationWrapper();
    	pcw.pending_tag_edit_position = this.pending_tag_edit_position;

    	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
    	pcw.tags = adapter.filtered_tags_collection;
        return pcw;
    }

	// ========================================================================
    void finishWithTags() {
    		
    	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
    	Log.d(TAG, "There were " + adapter.getCount() + " tags.");
    	
		List<Tag> saveable_standard_tags = new ArrayList<Tag>();
		List<MachineTag> saveable_machine_tags = new ArrayList<MachineTag>();

    	for (Tag tag : adapter.filtered_tags_collection) {
    		if (tag.isMachineTag()) {
    			MachineTag mt = (MachineTag) tag;
    			saveable_machine_tags.add( mt );
    		}
    		else {
    			saveable_standard_tags.add(tag);
    		}
    	}

    	DatabaseSearchHistory search_database = new DatabaseSearchHistory(this);
    	search_database.save_last_tags(saveable_standard_tags, saveable_machine_tags);

    	ArrayList<String> tags = new ArrayList<String>();
    	for (Tag tag : adapter.getFilteredTags())
    		tags.add( tag.getRaw() );

    	Intent i = new Intent();
	    i.putExtra(BatchUploaderActivity.INTENT_EXTRA_TAGS, tags);
    	setResult(Activity.RESULT_OK, i);
    	finish();
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
        case DIALOG_STANDARD_TAG_CREATION:
        {
            View tagTextEntryView = factory.inflate(R.layout.dialog_plain_tag, null);
            
            final EditText tag_box = (EditText) tagTextEntryView.findViewById(R.id.tag_edit);

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
                	String tag_string = tag_box.getText().toString();
                	Tag tag = new Tag();
                	tag.setValue(tag_string);
                	tag.setRaw(tag_string);
                	adapter.addTag( tag );
                	
                	refresh(adapter);
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
        	
        	return new AlertDialog.Builder( ListActivityTagSelection.this )
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
    				case MACHINE_TAG_CUSTOM:
    				{
    			    	Intent i = new Intent(Intent.ACTION_PICK);
    			    	i.setClass(ListActivityTagSelection.this, MachineTagActivity.class);
    			    	startActivityForResult(i, REQUEST_CODE_MACHINE_TAG_CHOOSER);
    					break;
    				}
    				case MACHINE_TAG_PREDEFINED:
    				{
    			    	Intent i = new Intent(Intent.ACTION_PICK);
    			    	i.setClass(ListActivityTagSelection.this, ListActivityTagConvention.class);
    			    	startActivityForResult(i, REQUEST_CODE_MACHINE_TAG_CHOOSER);
    				}
    				default:
    					break;
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
	void prompt_change_machine_tag(final Tag tag) {


		final MachineTag parts = new MachineTag(tag.getRaw());
		final String[] tag_value_options = machinetag_database.getTagValueOptions(parts);

		if (tag_value_options.length == 0) {
			// TODO
			Log.e(TAG, "TODO");
			return;
		}
		
        AlertDialog d = new AlertDialog.Builder(ListActivityTagSelection.this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle("New value for \"" + parts.predicate + "\"")
        .setItems(tag_value_options, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				
            	Log.d(TAG, "Chosen item: " + which);
            	
            	parts.value = tag_value_options[which];
            	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
            	refresh(adapter);
			}
        })
        .setNegativeButton(R.string.alert_dialog_cancel, null)
        .create();
    
        d.show();
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
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_tag_list, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Tag action:");
        
        menu.findItem(R.id.menu_edit_machine_tag).setVisible(true);
        menu.findItem(R.id.menu_tag_change_value).setVisible(true);
	}

	// ========================================================================
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();


		Tag tag = (Tag) getListView().getAdapter().getItem(info.position);
		
		switch ( item.getItemId() ) {
		case R.id.menu_tag_remove:
		{
	    	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
	    	adapter.filtered_tags_collection.remove(info.position);
	    	adapter.raw_tags_collection.remove(tag);
	    	refresh(adapter);
			break;
		}
		case R.id.menu_edit_machine_tag:
		{
	    	if (tag.isMachineTag()) {

				this.pending_tag_edit_position = info.position;
				MachineTag machine_tag = (MachineTag) tag;
				
		    	Intent i = new Intent(Intent.ACTION_EDIT);
	   			i.putExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_NAMESPACE, machine_tag.namespace);
	   			i.putExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_PREDICATE, machine_tag.predicate);
	   			i.putExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_VALUE, machine_tag.value);

		    	i.setClass(ListActivityTagSelection.this, MachineTagActivity.class);
		    	startActivityForResult(i, REQUEST_CODE_MACHINE_TAG_EDITOR);
		    	
	    	} else {

				tag_changer(tag);
	    	}

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
    }

	// ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_tag_selection, menu);

        return true;
    }

	// ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

		boolean already_downloaded = test_tag_conventions_exist(this);


    	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
    	menu.findItem(R.id.menu_clear_tags).setVisible( adapter.getCount() > 0 );
		
		menu.findItem(R.id.menu_load_convention).setVisible( !already_downloaded );
		menu.findItem(R.id.menu_purge_convention).setVisible( already_downloaded );
        return true;
    }

	// ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_clear_tags:
        {
        	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
        	adapter.filtered_tags_collection.clear();
        	adapter.raw_tags_collection.clear();
        	refresh(adapter);
            return true;
        }
        case R.id.menu_add_tag:
        	
        	showDialog(DIALOG_NEW_TAG_PROMPT);
        	
            return true;
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
				new TagConventionParser(ListActivityTagSelection.this, "tagging_convention2").parse();
			} catch (NetworkUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
    	
	    @Override
	    public void onPostExecute(Void nothing) {

	    	Toast.makeText(ListActivityTagSelection.this, "Updated tag conventions.", Toast.LENGTH_SHORT).show();
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

	    	Toast.makeText(ListActivityTagSelection.this, "Purged tag conventions.", Toast.LENGTH_SHORT).show();
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
	    		Toast.makeText(ListActivityTagSelection.this, "Loaded tag conventions.", Toast.LENGTH_SHORT).show();
	    		
				FlickrTagListAdapter adapter = ((FlickrTagListAdapter) getListAdapter());
				adapter.sort_taglist();
//		   		adapter.notifyDataSetInvalidated();
	    	}
	    }
    }

	// ========================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
	
	   		case REQUEST_CODE_APPENGINE_FETCH:
	   		{
				Log.d(TAG, "Foo.");
	   			break;
	   		}	
	   		case REQUEST_CODE_APPENGINE_DELETE:
	   		{
				Log.d(TAG, "Bar.");
	   			break;
	   		}
	   		case REQUEST_CODE_MACHINE_TAG_EDITOR:
	   		{
	   			String machine_namespace = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_NAMESPACE);
	   			String machine_predicate = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_PREDICATE);
	   			String machine_value = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_VALUE);

            	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
            	MachineTag mt = (MachineTag) adapter.getItem(this.pending_tag_edit_position);
            	mt.namespace = machine_namespace;
            	mt.predicate = machine_predicate;
            	mt.value = machine_value;
            	refresh(adapter);
	   			break;
	   		}
	   		case REQUEST_CODE_MACHINE_TAG_CHOOSER:
	   		{
	   			String machine_namespace = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_NAMESPACE);
	   			String machine_predicate = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_PREDICATE);
	   			String machine_value = data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_VALUE);

	   			MachineTag mt = new MachineTag(machine_namespace, machine_predicate, machine_value);
	   			
            	FlickrTagListAdapter adapter = (FlickrTagListAdapter) getListAdapter();
            	adapter.addTag( mt );
            	refresh(adapter);
	   			break;
	   		}
	   		default:
		    	break;
		   }
		}
    }

	// ========================================================================
    void refresh(BaseAdapter adapter) {
    	adapter.notifyDataSetChanged();
    	int count = adapter.getCount();
    	this.tag_list_header.setText( getResources().getQuantityString(R.plurals.tag_count, count, count));
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
