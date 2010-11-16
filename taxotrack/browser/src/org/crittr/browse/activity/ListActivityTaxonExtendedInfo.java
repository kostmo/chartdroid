package org.crittr.browse.activity;

import org.crittr.browse.AsyncTaskModified;
import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.ThumbGridAdapter;
import org.crittr.containers.ThumbnailUrlPlusLinkContainer;
import org.crittr.flickr.BlacklistDatabase;
import org.crittr.shared.MachineTag;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.Constants.CollectionSource;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.utilities.FlickrPhotoDrawableManager;
import org.crittr.shared.browser.utilities.MediawikiSearchResponseParser;
import org.crittr.task.LargeImageGetterTask;
import org.crittr.task.NetworkUnavailableException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.net.Uri;
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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;


public class ListActivityTaxonExtendedInfo extends Activity {

	
	static final String TAG = Market.DEBUG_TAG; 

	final int APPENGINE_FETCH_RETURN_CODE = 1;
	final int APPENGINE_DELETE_RETURN_CODE = 2;
	
    public static final String PREFKEY_REFERENCE_INFO_INSTRUCTIONS = "PREFKEY_REFERENCE_INFO_INSTRUCTIONS";

	final int DIALOG_LARGE_PHOTO_VIEW = 1;
	final int DIALOG_REFERENCE_INFO_INSTRUCTIONS = 2;
	

	public static String INTENT_EXTRA_PHOTO_COLLECTION_SOURCE = "INTENT_EXTRA_PHOTO_COLLECTION_SOURCE";

	
	DatabaseTaxonomy database_helper;
	String search_taxon_name;
	
	
	GridView flickr_photo_grid;
	
	private long current_tsn;
	
	Bitmap globally_stored_dialog_bitmap;
	boolean globally_stored_dialog_showing = false;
	int globally_stored_selected_thumbnail = -1;

    // ========================================================================
	private class LargeImageGetterTaskExtended extends LargeImageGetterTask {

		LargeImageGetterTaskExtended(Context context) {
			super(context);
		}
		
		
	     @Override
	     protected void onPostExecute(Bitmap bitmap) {
	    	 super.onPostExecute(bitmap);

	    	 globally_stored_dialog_bitmap = bitmap;
	    	 showDialog(DIALOG_LARGE_PHOTO_VIEW);
	     }
	}
	 
	
	

    // ========================================================================
	void picture_cycler() {
		
		ThumbnailUrlPlusLinkContainer photo_thumbnail_pair = (ThumbnailUrlPlusLinkContainer) flickr_photo_grid.getAdapter().getItem(globally_stored_selected_thumbnail);
		new LargeImageGetterTaskExtended(this).execute(photo_thumbnail_pair);
	}
	
	
	

    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
 
        setContentView(R.layout.list_activity_taxon_extended_info);
        
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);
        
        database_helper = new DatabaseTaxonomy(ListActivityTaxonExtendedInfo.this);
        
        
        current_tsn = getIntent().getLongExtra(Constants.INTENT_EXTRA_TSN, Constants.INVALID_TSN);
        Log.d(TAG, "Inherited TSN: " + current_tsn);
        
        


        
        

        flickr_photo_grid = (GridView) findViewById(R.id.flickr_photo_grid);
/*
        flickr_photo_grid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
			    
				ThumbnailUrlPlusLinkContainer photo_thumbnail_pair = (ThumbnailUrlPlusLinkContainer) flickr_photo_grid.getAdapter().getItem(pos);
				
				new LargeImageGetterTask().execute(photo_thumbnail_pair);

			}
        });
*/
        
        flickr_photo_grid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
				
				globally_stored_selected_thumbnail = pos;
				
				picture_cycler();
			}
        });
        
        
        
        
		registerForContextMenu(flickr_photo_grid);
        
        
        // Deal with orientation change
        final PreserveConfigurationWrapper a = (PreserveConfigurationWrapper) getLastNonConfigurationInstance();
        if (a != null) {
        	 flickr_photo_grid.setAdapter( (ThumbGridAdapter) a.gridview_adapter );
        	 globally_stored_dialog_bitmap = a.dialog_bitmap;
        	 globally_stored_selected_thumbnail = a.selected_thumbnail;
        	 
        	 Log.d(TAG, "Should the dialog be showing? " + a.dialog_showing);
        	 
	    	 if (a.dialog_showing)
	    		 showDialog(DIALOG_LARGE_PHOTO_VIEW);
        }
        else {
        	flickr_photo_grid.setAdapter(new ThumbGridAdapter(this));
            if (current_tsn != Constants.INVALID_TSN) // Why would it be invalid?
            	new TaxonImageQueryTask(current_tsn).execute();
        }
        
        
        // NOTE: This listener will override the context menu...
        /*
        flickr_photo_grid.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
        	public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {

//    	    	Toast.makeText(ListActivityTaxonExtendedInfo.this, "Longpress at " + pos, Toast.LENGTH_SHORT).show();
    	    	
    	    	String url = ((PhotoThumbnailUrlPair) av.getAdapter().getItem(pos)).thumbnail_url.toString();
    	    	Log.d(TAG, "Thumbnail URL: " + url);
    			return true;
        	}
        });
        */
        

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (!settings.getBoolean(PREFKEY_REFERENCE_INFO_INSTRUCTIONS, false)) {
			showDialog(DIALOG_REFERENCE_INFO_INSTRUCTIONS);
		}
    }

    

    // ========================================================================
    class PreserveConfigurationWrapper {
    	ListAdapter gridview_adapter;
    	Bitmap dialog_bitmap;
    	boolean dialog_showing;
    	int selected_thumbnail;
    }
    

    // ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	
    	PreserveConfigurationWrapper pcw = new PreserveConfigurationWrapper();
    	pcw.gridview_adapter = flickr_photo_grid.getAdapter();
    	pcw.dialog_bitmap = globally_stored_dialog_bitmap;
    	pcw.dialog_showing = globally_stored_dialog_showing;
    	pcw.selected_thumbnail = globally_stored_selected_thumbnail;
    	
        return pcw;
    }

    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        
        switch (id) {

        case DIALOG_LARGE_PHOTO_VIEW:
        {
			
			ImageView i = new ImageView(ListActivityTaxonExtendedInfo.this);
			i.setImageBitmap( globally_stored_dialog_bitmap );
			i.setAdjustViewBounds(true);
//			i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			i.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					
					int current_selection = globally_stored_selected_thumbnail;
					int next_selection = (current_selection + 1) % flickr_photo_grid.getAdapter().getCount();
					
//					Log.d(TAG, "Current: " + current_selection + "; Next: " + next_selection);
					
					globally_stored_selected_thumbnail = next_selection;
					picture_cycler();
				}
			});
			
			dialog.setContentView(i);
			globally_stored_dialog_showing = true;
        }

        }
    }


    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {
        
        switch (id) {
        case DIALOG_REFERENCE_INFO_INSTRUCTIONS:
        {
	        LayoutInflater factory = LayoutInflater.from(this);

	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

	        ((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_reference_info);
	        
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Reference info")
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

            		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ListActivityTaxonExtendedInfo.this);
            		settings.edit().putBoolean(PREFKEY_REFERENCE_INFO_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
                }
            })
            .create();
        }
        case DIALOG_LARGE_PHOTO_VIEW:
        {
			Dialog d = new Dialog(ListActivityTaxonExtendedInfo.this);
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);
			
			d.setOnCancelListener( new OnCancelListener() {

				public void onCancel(DialogInterface dialog) {
					globally_stored_dialog_showing = false;
				}
			});
			return d;
        }

        }
        
        return null;
    }
    

    // ========================================================================
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_taxon_photos, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Photo action:");
        
        
        Log.d(TAG, "Creating ExtendedInfo context menu.");
	}

    // ========================================================================
	public boolean onContextItemSelected(MenuItem item) {
		
		Log.d(TAG, "Context menu item selected.");
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		ThumbnailUrlPlusLinkContainer photo_thumbnail_pair = (ThumbnailUrlPlusLinkContainer) flickr_photo_grid.getAdapter().getItem(info.position);
		
		switch ( item.getItemId() ) {
		case R.id.menu_context_prefer:
		{
			// Prefer locally
			DatabaseTaxonomy helper = new DatabaseTaxonomy(this);
			
			String current_thumbnail_url = photo_thumbnail_pair.getThumbnailUrl().toString();
			Log.d(TAG, "Current thumbnail URL: " + current_thumbnail_url);
			
			if (current_thumbnail_url != null)
				helper.markPreferredThumbnail(current_tsn, current_thumbnail_url);
			
			break;
		}
		case R.id.menu_context_blacklist:
		{
			// Blacklist locally
			BlacklistDatabase blacklist_helper = new BlacklistDatabase(this);
			blacklist_helper.addToBlacklist(photo_thumbnail_pair.getIdentifier());

			break;
		}

		case R.id.menu_view_on_flickr:

			Uri flickr_destination = Uri.parse( photo_thumbnail_pair.getLink() );

        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

			break;
		default:
			break;
		}

		return super.onContextItemSelected(item);
	}


    // ========================================================================
	// Also see "SearchTaskTaxonBased.java"
    public class TaxonImageQueryTask extends AsyncTaskModified<Void, List<ThumbnailUrlPlusLinkContainer>, Void> {
    	
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ListActivityTaxonExtendedInfo.this);
		int MAX_PHOTOS = settings.getInt("max_photos", 40);
    	
    	long tsn;
    	
    	int total_matches = 0;
		int photo_count = 0;

		int photo_source;

    	
    	TaxonImageQueryTask(long tsn) {
    		this.tsn = tsn;
    		
    		photo_source = getIntent().getIntExtra(INTENT_EXTRA_PHOTO_COLLECTION_SOURCE, CollectionSource.FLICKR.ordinal());
    	}

    	
    	
	    @Override
	    public void onPreExecute() {
    		Log.d(TAG, "Beginnging Flickr search for taxon " + tsn);
    		

    		// Try to get the rank name of this taxon locally.
    		// TODO: If we don't have it, we should fetch from the network and cache the result.
    		TaxonInfo taxon_target = database_helper.getSingleTaxon(tsn);
    		search_taxon_name = taxon_target.taxon_name;
	    }
	    
	    


	    
	    private List<ThumbnailUrlPlusLinkContainer> getMediawikiResults(String search_taxon_name, boolean use_wikipedia) {
	    	MediawikiSearchResponseParser mwsrp = new MediawikiSearchResponseParser(ListActivityTaxonExtendedInfo.this);
	    	
	    	List<ThumbnailUrlPlusLinkContainer> foo;

	    	try {
				foo = mwsrp.parse_category_thumbnails( (use_wikipedia ? "" : "Category:") + search_taxon_name, use_wikipedia);
				
				total_matches = foo.size();
				photo_count = Math.min(total_matches, MAX_PHOTOS);
				foo = foo.subList(0, photo_count);
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
				foo = new ArrayList<ThumbnailUrlPlusLinkContainer>();
			}

			return foo;
	    }
	    
		protected Void doInBackground(Void... params) {

			
			if (search_taxon_name == null || search_taxon_name.length() == 0) {
				try {
					search_taxon_name = ItisQuery.getScientificNameFromTSN(ListActivityTaxonExtendedInfo.this, tsn);
		    		
				} catch (NetworkUnavailableException e) {
					e.printStackTrace();
				}
			}
			
			MachineTag flickr_searchtag = new MachineTag("taxonomy", null, search_taxon_name.toLowerCase());
			
			List<ThumbnailUrlPlusLinkContainer> foo;
			switch ( CollectionSource.values()[photo_source] ) {
			case FLICKR:
				foo = FlickrPhotoDrawableManager.getFlickrPhotoMatches(ListActivityTaxonExtendedInfo.this, this, flickr_searchtag, 20);
				break;
				
			case COMMONS:
				foo = getMediawikiResults(search_taxon_name, false);
				break;
				
			case WIKIPEDIA:
				foo = getMediawikiResults(search_taxon_name, true);
				break;
				
			default:
				foo = new ArrayList<ThumbnailUrlPlusLinkContainer>();
				break;
			}
			
			this.publishProgress( foo );

			return null;
		}
		

	    @Override
	    public void onProgressUpdate(List<ThumbnailUrlPlusLinkContainer>... progress_packet) {
	    	
//	    	String base_title = ListActivityTaxonExtendedInfo.this.getResources().getString(R.string.flickr_photolist_title);
	    	
	    	String base_title = TabActivityTaxonExtendedInfo.photoset_labels[photo_source] + " images";
	    	
//	    	String combined_title = base_title + " (fetched " + photo_count + " of " +  + ", " + total_matches + " total)";
	    	String combined_title = base_title + " (" + photo_count + " fetched, " + total_matches + " total)";
	        ((TextView) findViewById(R.id.flickr_grid_title)).setText( combined_title );
				
			
			Log.w(TAG, "Added photopairs to GridView");
			((ThumbGridAdapter) flickr_photo_grid.getAdapter()).add_photopairs(progress_packet[0]);
	    }
		

	    @Override
	    public void onPostExecute(Void foo) {
	    	Log.d(TAG, "Done with photo search.");
	    }
    }

    

    // ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);


        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_taxon_extended_info, menu);
        return true;
    }


    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
        {
        	showDialog(DIALOG_REFERENCE_INFO_INSTRUCTIONS);
            return true;
        }
        }
        return super.onOptionsItemSelected(item);
    }
}
