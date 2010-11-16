package com.kostmo.flickr.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.kostmo.flickr.activity.prefs.PrefsUpload;
import com.kostmo.flickr.adapter.ImageUploadListCursorAdapter;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.Refreshable;
import com.kostmo.flickr.data.DatabaseUploads;
import com.kostmo.flickr.service.UploadService;
import com.kostmo.flickr.tasks.MediaStoreImagePopulatorTask;
import com.kostmo.tools.SemaphoreHost;

public class BatchUploaderActivity extends ListActivity implements Disablable, SemaphoreHost, Refreshable {

	static final String TAG = Market.DEBUG_TAG; 

	public static final long INVALID_UPLOAD_ROW_ID = -1;
	public static final int INVALID_UPLOAD_ROW_POSITION = -1;
	
	
	public static final int DIALOG_UPLOAD_SCREEN_INSTRUCTIONS = 5;
	final int DIALOG_PHOTO_TITLE_DESCRIPTION = 6;
	
	public static final String PREFKEY_UPLOAD_SCREEN_INSTRUCTIONS = "PREFKEY_UPLOAD_SCREEN_INSTRUCTIONS";
	
	public static final String PREFKEY_BATCH_UPLOAD_TAGS = "PREFKEY_BATCH_UPLOAD_TAGS";
	

	public static String INTENT_EXTRA_TAGS = "INTENT_EXTRA_TAGS";
	
	private static final int REQUEST_CODE_INITIAL_UPLOAD_SETTINGS = 1;
    private static final int REQUEST_CODE_PHOTO_PICKED = 2;
    private static final int REQUEST_CODE_TAG_SELECTION = 3;
    


    private static final String TAGS_DELIMITER = ",";
    
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	
    boolean crop_first;

	SharedPreferences settings;
	UploadService record_fetcher_service;
    
    Button upload_button, tags_button;
	
    DatabaseUploads database_uploads;
    AsyncUploadQueuer upload_queuer_task;
    int pending_upload_row_position = INVALID_UPLOAD_ROW_POSITION;
    Collection<String> batch_tags = new ArrayList<String>();    
    // ========================================================================
	public enum UploadStatus {
		PENDING("Pending", Color.GRAY),
		UPLOADING("Uploading", Color.YELLOW),
		COMPLETE("Complete", Color.GREEN),
		FAILED("Failed", Color.RED);
		
		public String name;
		public int color;
		UploadStatus(String name, int color) {
			this.name = name;
			this.color = color;
		}
	}
	
    // ========================================================================
	public static class ImageUploadData {
		public long row_id = INVALID_UPLOAD_ROW_ID;
		public Uri image_uri;
		public String title;
		public String description;
		public Collection<String> tags;
		public UploadStatus upload_status = UploadStatus.PENDING;
    }

    // ========================================================================
	@Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if (!settings.getBoolean(PREFKEY_UPLOAD_SCREEN_INSTRUCTIONS, false)) {
			showDialog(DIALOG_UPLOAD_SCREEN_INSTRUCTIONS);
		}
	}

    // ========================================================================
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.batch_upload_screen);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

		this.settings = PreferenceManager.getDefaultSharedPreferences( getBaseContext() );
        this.database_uploads = new DatabaseUploads(this);
        

        this.upload_button = (Button) findViewById(R.id.button_upload);
        
		findViewById(R.id.button_add_file).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				add_photo_upload();
			}
		});
		
		this.tags_button = (Button) findViewById(R.id.button_batch_tags);
		this.tags_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent launch_intent = new Intent(BatchUploaderActivity.this, ListActivityTagSelection.class);
				if (batch_tags != null) {
					launch_intent.putExtra(BatchUploaderActivity.INTENT_EXTRA_TAGS, new ArrayList<String>(batch_tags));
				}
				startActivityForResult(launch_intent, REQUEST_CODE_TAG_SELECTION);
			}
		});
		
		
        
//    	ImageUploadListAdapter adapter = new ImageUploadListAdapter(this, this);
		ImageUploadListCursorAdapter adapter = new ImageUploadListCursorAdapter(this, this, R.layout.list_item_pending_upload, null);
    	setListAdapter( adapter );
		registerForContextMenu(getListView());
		
        // Deal with orientation change
        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {

        	this.crop_first = a.crop_first;
        	adapter.changeCursor(a.cursor);
        	adapter.is_list_enabled = a.is_list_disabled;
        	this.pending_upload_row_position = a.pending_upload_row_position;
        	this.batch_tags = a.batch_tags;
        	
        } else {

        	String[] splitted = TextUtils.split(this.settings.getString(PREFKEY_BATCH_UPLOAD_TAGS, ""), TAGS_DELIMITER);
        	this.batch_tags = Arrays.asList(splitted);
        	
        	
        	if (Intent.ACTION_SEND.equals(getIntent().getAction())) {

                Log.d(TAG, "Queuing single upload from Intent");
                
                if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
                	Log.d(TAG, "The Intent posesses the \"EXTRA_TEXT\" field: " + getIntent().getExtras().get(Intent.EXTRA_TEXT));
                }
                
		        Uri stream_uri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
		        queueUploadUri(stream_uri);
		        
        	} else if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())) {

        		
        		Log.d(TAG, "Queuing multiple uploads from Intent");

        		List<Uri> stream_uris = (ArrayList<Uri>) getIntent().getExtras().get(Intent.EXTRA_STREAM);
        		
        		for (Uri stream_uri : stream_uris) {
        			Log.i(TAG, "URI: " + stream_uri.toString());
        			queueUploadUri(stream_uri);
        		}
        	}
        	
        	refresh();
        }
        
        
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
		if (stored_auth_token == null) {
			upload_button.setEnabled(false);
		} else {
	        upload_button.setOnClickListener(new View.OnClickListener() {
	    	    public void onClick(View v) {
	    			initiateUploads();
	    	    	settings.edit().putBoolean(Main.PREFKEY_HAVE_UPLOADED_ONCE, true).commit();
	    	    }
	    	});
		}
		
		if (settings.getBoolean(PrefsUpload.PREFKEY_IS_FIRST_UPLOAD, true)) {
	        Toast.makeText(this, R.string.toast_upload_review_settings, Toast.LENGTH_LONG).show();
        	startActivityForResult(new Intent(this, PrefsUpload.class), REQUEST_CODE_INITIAL_UPLOAD_SETTINGS);
		}
		
		updateBatchTagsButton();
    }

    // ========================================================================
	void queueUploadUri(Uri stream_uri) {
        
        ImageUploadData upload = new ImageUploadData();
        upload.image_uri = stream_uri;
//        adapter.addUpload(upload);
        queueUploadAsynchronously(upload);
	}
	
    // ========================================================================
    static private class StateRetainer {
    	
    	Collection<String> batch_tags;
    	
        boolean crop_first;
        Cursor cursor;
        boolean is_list_disabled;
        int pending_upload_row_position;
    }

    // ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	
    	StateRetainer pcw = new StateRetainer();

    	pcw.crop_first = this.crop_first;
    	
    	ImageUploadListCursorAdapter adapter = (ImageUploadListCursorAdapter) getListAdapter();
    	pcw.cursor = adapter.getCursor();
    	pcw.is_list_disabled = adapter.is_list_enabled;
    	pcw.pending_upload_row_position = this.pending_upload_row_position;
        return pcw;
    }

    // ========================================================================
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

    	Cursor cursor = (Cursor) l.getItemAtPosition(position);
    	long flickr_photo_id = cursor.getLong(cursor.getColumnIndex(DatabaseUploads.KEY_FLICKR_PHOTO_ID));
    	if (flickr_photo_id != ListActivityPhotoTags.INVALID_PHOTO_ID) {
	    	
	    	Intent i = new Intent(Intent.ACTION_VIEW);
	        i.addCategory(IntentConstants.CATEGORY_FLICKR_PHOTO);
			i.putExtra(IntentConstants.PHOTO_ID, flickr_photo_id);
	    	startActivity(i);
    	}
    }
    
    // ========================================================================
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_batch_uploads, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("File action:");
	}

    // ============================================================
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		
		Log.d(TAG, "Context menu item selected.");
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch ( item.getItemId() ) {
		case R.id.menu_remove:
		{
			this.database_uploads.removeUpload(info.id);
			refresh();
			break;
		}
		case R.id.menu_title:
		{
			this.pending_upload_row_position = info.position;
			showDialog(DIALOG_PHOTO_TITLE_DESCRIPTION);
			break;
		}
		default:
			break;
		}

		return super.onContextItemSelected(item);
	}
    
    // ========================================================================
    void add_photo_upload() {
    	
    	
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
//        intent.setType( "image/jpeg" );	// FIXME

        // Note: we could have the "crop" UI come up here by
        // default by doing this:
//        crop_first = ((CheckBox) findViewById(R.id.crop_checkbox)).isChecked();
        
        
		crop_first = settings.getBoolean("prompt_for_crop", false);

        if (crop_first)
        	intent.putExtra("crop", "true");
        // (But watch out: if you do that, the Intent that comes
        // back to onActivityResult() will have the URI (of the
        // cropped image) in the "action" field, not the "data"
        // field!)

        startActivityForResult(
//                Intent.createChooser(intent, "Obtain image from:"),
        		intent,
                REQUEST_CODE_PHOTO_PICKED);
        
        Toast.makeText(this, "Select a photo to upload.", Toast.LENGTH_LONG).show();
    }
    
    // ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_batch_upload, menu);
        return true;
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

    	ImageUploadListCursorAdapter adapter = (ImageUploadListCursorAdapter) getListAdapter();
        menu.findItem(R.id.menu_clear_finished).setVisible(adapter.getCount() > 0);
        return true;
    }

    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_clear_finished:
        {
        	this.database_uploads.hideCompleteUploads();
			refresh();
        	return true;
        }
        case R.id.menu_add_upload:
        {
			add_photo_upload();
        	return true;
        }
        case R.id.menu_preferences:
        {	
        	startActivity(new Intent(this, PrefsUpload.class));
        	return true;
        }
        default:
        	break;
        }

        return super.onOptionsItemSelected(item);
    }

    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
		case DIALOG_PHOTO_TITLE_DESCRIPTION:
		{
			final EditText title_textbox = (EditText) dialog.findViewById(R.id.textbox_photo_title);
			final EditText description_textbox = (EditText) dialog.findViewById(R.id.textbox_photo_description);

			Cursor cursor = (Cursor) ((CursorAdapter) this.getListAdapter()).getItem(pending_upload_row_position);
			String overridden_title = cursor.getString(cursor.getColumnIndex(DatabaseUploads.KEY_UPLOAD_TITLE));
			String overridden_description = cursor.getString(cursor.getColumnIndex(DatabaseUploads.KEY_UPLOAD_DESCRIPTION));
			
			// TODO - If the title/description has not yet been overridden,
			// fall back to the MediaStore values!!!
			
			title_textbox.setText( overridden_title );
			description_textbox.setText( overridden_description );

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
	    case DIALOG_UPLOAD_SCREEN_INSTRUCTIONS:
	    {
	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);
	
	        ((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_upload_batch);
	        
	        return new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_info)
	        .setTitle("Uploading")
	        .setView(tagTextEntryView)
	        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
		        		settings.edit().putBoolean(PREFKEY_UPLOAD_SCREEN_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
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
					
					
					long row_id = ((CursorAdapter) getListAdapter()).getItemId(pending_upload_row_position);
					
					database_uploads.setUploadTitleDescription(
							row_id,
							title_textbox.getText().toString(),
							description_textbox.getText().toString());
					
					refresh();
				}
			})
			.create();
		}
        }
        
        return null;
    }

    // ========================================================================
    void updateBatchTagsButton() {
    	int tag_count = 0;
    	if (this.batch_tags != null)
    		tag_count = this.batch_tags.size();
    	this.tags_button.setText(getResources().getString(R.string.upload_batch_tags) + " (" + tag_count + ")");
    }
    
    // ========================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    	if (requestCode == REQUEST_CODE_INITIAL_UPLOAD_SETTINGS)
   			settings.edit().putBoolean(PrefsUpload.PREFKEY_IS_FIRST_UPLOAD, false).commit();
    	
		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
	   		case REQUEST_CODE_TAG_SELECTION:
	   		{
	   			this.batch_tags = data.getStringArrayListExtra(INTENT_EXTRA_TAGS);
	   			this.settings.edit().putString(PREFKEY_BATCH_UPLOAD_TAGS, TextUtils.join(TAGS_DELIMITER, this.batch_tags)).commit();
	   			updateBatchTagsButton();
	   			break;
	   		}
	   		case REQUEST_CODE_PHOTO_PICKED:
	   		{
	            if (data == null) {
	                Log.w(TAG, "Null data, but RESULT_OK, from image picker!");
	                Toast t = Toast.makeText(this, "Nothing picked!",
	                                         Toast.LENGTH_SHORT);
	                t.show();
	                return;
	            }

	            Uri true_result;
	        	// This is a Gallery-specific hack; we need to fail gracefully if another
	            // image source was chosen
	            if (crop_first && data.getAction() != null)
	            	true_result = Uri.parse( data.getAction() );
	            else
	            	true_result = data.getData();
	            
	            if (true_result == null) {
	                Log.w(TAG, "'data' intent from image picker contained no data!");
	                Toast t = Toast.makeText(this, "Nothing picked!",
	                                         Toast.LENGTH_SHORT);
	                t.show();
	                return;
	            }

	            ImageUploadData upload = new ImageUploadData();
	            upload.image_uri = true_result;
//	            ((ImageUploadListAdapter) getListAdapter()).addUpload(upload);
	            queueUploadAsynchronously(upload);
	            break;
	        }
	   		default:
		    	break;
		   }
		}
    }

	// ========================================================================
    void queueUploadAsynchronously(ImageUploadData upload) {

    	AsyncUploadQueuer upload_queuer_task = new AsyncUploadQueuer(upload);
    	upload_queuer_task.execute();
    }

	// ========================================================================
    public class AsyncUploadQueuer extends AsyncTask<Void, Void, ImageUploadData> {

    	ImageUploadData unpopulated_upload;
    	AsyncUploadQueuer(ImageUploadData upload) {
    		this.unpopulated_upload = upload;
    	}
    	
		@Override
		protected ImageUploadData doInBackground(Void... params) {
			
			Cursor c = MediaStoreImagePopulatorTask.obtainMetadataCursor(this.unpopulated_upload.image_uri, getContentResolver());
			if (c != null) {
				MediaStoreImagePopulatorTask.populateTitleDescriptionFromCursor(c, this.unpopulated_upload);
				c.close();
			} else {
				Log.e(TAG, "The metadata cursor was null...");
			}
			return this.unpopulated_upload;
		}
    	
		@Override
		protected void onPostExecute(ImageUploadData upload) {
	        database_uploads.queueUpload(upload);
	        refresh();
		}
    }
    
	// ========================================================================
	void initiateUploads() {

		Log.d(TAG, "initiateUploads()");
		if (this.record_fetcher_service != null) {

			Log.d(TAG, "(for real this time)");

			ImageUploadListCursorAdapter adapter = (ImageUploadListCursorAdapter) getListAdapter();
			this.record_fetcher_service.beginUploadsFromList(this, adapter.gatherUploadsList(), this.batch_tags );
			
		} else {

			Log.d(TAG, "(must connect to Service first...)");
			this.starting_service = true;
			bindServiceOnly();
		}
	}
    
	// ========================================================================
	boolean starting_service = false;
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			Log.i(TAG, "We are now connected to the Service!");


			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			BatchUploaderActivity.this.record_fetcher_service = ((UploadService.LocalBinder) service).getService();
			BatchUploaderActivity.this.record_fetcher_service.setDisablableHost(BatchUploaderActivity.this);

			
			if (BatchUploaderActivity.this.starting_service) {
				BatchUploaderActivity.this.starting_service = false;
				initiateUploads();
				
			} else {
				
				if (BatchUploaderActivity.this.record_fetcher_service != null && BatchUploaderActivity.this.record_fetcher_service.isInProgress()) {
					disable();
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			BatchUploaderActivity.this.record_fetcher_service = null;
		}
	};
	
	// ========================================================================
	Intent bindServiceOnly() {
		Intent i = new Intent(this, UploadService.class);
		bindService(i, this.mConnection, Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND );
		return i;
	}

    // ========================================================================
	public void onDestroy() {

		Log.e(TAG, "The Activity was destroyed.");

		if (this.mConnection != null) {
			if (this.record_fetcher_service != null) {
				this.record_fetcher_service.setDisablableHost(null);
				
				Log.e(TAG, "Now unbinding service...");
				this.unbindService(this.mConnection);
			}
		}
		
		if (upload_queuer_task != null) {
			upload_queuer_task.cancel(true);
		}

		super.onDestroy();
	}

	// ========================================================================
	@Override
	public void disable() {
		this.upload_button.setText(R.string.alert_dialog_cancel);
		this.upload_button.setTextColor(Color.RED);
		
		ImageUploadListCursorAdapter adapter = (ImageUploadListCursorAdapter) getListAdapter();
		adapter.is_list_enabled = false;
		refresh();
	}

	// ========================================================================
	@Override
	public void reEnable() {
		this.upload_button.setText(R.string.upload);
		this.upload_button.setTextColor(Color.BLACK);
		
		ImageUploadListCursorAdapter adapter = (ImageUploadListCursorAdapter) getListAdapter();
		adapter.is_list_enabled = true;
		refresh();
	}
	
	// ========================================================================
	@Override
	public void incSemaphore() {
		setProgressBarIndeterminateVisibility(true);
		this.retrieval_tasks_semaphore.incrementAndGet();
	}

	// ========================================================================
	@Override
	public void decSemaphore() {
		boolean still_going = this.retrieval_tasks_semaphore.decrementAndGet() > 0;
		setProgressBarIndeterminateVisibility(still_going);
	}

	// ========================================================================
	@Override
	public void refresh() {
//    	((ImageUploadListAdapter) getListAdapter()).notifyDataSetInvalidated();
    	ImageUploadListCursorAdapter adapter = (ImageUploadListCursorAdapter) getListAdapter();
    	Cursor cursor = this.database_uploads.retrieveUploads();
    	adapter.changeCursor(cursor);
    	
    	this.upload_button.setEnabled(cursor.getCount() > 0);
	}
}