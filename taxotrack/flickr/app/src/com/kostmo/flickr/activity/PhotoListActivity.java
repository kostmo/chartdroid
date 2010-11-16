package com.kostmo.flickr.activity;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

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
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
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
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.SearchParameters;
import com.aetrion.flickr.photosets.PhotosetsInterface;
import com.kostmo.flickr.activity.prefs.PrefsBrowsing;
import com.kostmo.flickr.adapter.FlickrPhotoAdapter;
import com.kostmo.flickr.adapter.FlickrPhotoDualAdapter;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.provider.ExperimentalFileContentProvider;
import com.kostmo.flickr.tasks.RetrievalTaskFlickrPhotolist;
import com.kostmo.flickr.tasks.RetrievalTaskFlickrPhotoset;
import com.kostmo.flickr.tasks.LargeImageGetterTask.DownloadObjective;

public class PhotoListActivity extends SearchResultsViewerActivity implements OnItemClickListener {

	final int DIALOG_PHOTOLIST_INSTRUCTIONS = 1;

	final int DIALOG_GOTO_PAGE = 100;


	public static final String PREFKEY_SHOW_PHOTOLIST_INSTRUCTIONS = "PREFKEY_SHOW_PHOTOLIST_INSTRUCTIONS";


    // Re-implemented ListActivity methods:
    // =================================================
    ListView getListView() {
    	return (ListView) findViewById(android.R.id.list);
    }
    
    // =================================================    
    ListAdapter getListAdapter() {
    	return getListView().getAdapter();
    }
    
    // =================================================    
    void setListAdapter(ListAdapter adapter) {
    	getListView().setAdapter(adapter);
    }
    
	// ===============================================
    @Override
	public ListAdapter getAdapter() {
		
		return getListAdapter();
	}
  
	// ===============================================
    void set_display_mode(boolean grid_mode_enabled) {

		FlickrPhotoDualAdapter ad = (FlickrPhotoDualAdapter) getAdapter();

        if ( grid_mode_enabled ) {

            Log.d(TAG, "Starting in GRID MODE");

               getListView().setVisibility(View.GONE);
               ad.gridModeToggle( true );
               flickr_photo_grid.setVisibility(View.VISIBLE);
//               gridview_empty_view.setVisibility(View.VISIBLE);
               
        } else {
            
            Log.d(TAG, "Starting in LIST MODE");

//               gridview_empty_view.setVisibility(View.GONE);
               flickr_photo_grid.setVisibility(View.GONE);
               ad.gridModeToggle( false );
            getListView().setVisibility(View.VISIBLE);
        }
    }
    
    // =============================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	super.onSaveInstanceState(out_bundle);

//		Log.e(TAG, "onSaveInstanceState() in PhotoListActivity.");

    	out_bundle.putBoolean("is_grid_mode", ((FlickrPhotoDualAdapter) getListAdapter()).isGridMode());
    	
    	out_bundle.putBoolean("color_search_mode_active", globally_stored_color_search_mode_active);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	super.onRestoreInstanceState(in_bundle);

//		Log.e(TAG, "onRestoreInstanceState() in PhotoListActivity.");

		globally_stored_color_search_mode_active = in_bundle.getBoolean("color_search_mode_active");
    }
    
    // =============================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
     
        
        
        
        
        
        
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
		
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        flickr_photo_grid = (GridView) findViewById(R.id.photo_grid);
        /*
        View empty_view_container = LayoutInflater.from(this).inflate(R.layout.empty_adapter_view, null);
        gridview_empty_view = empty_view_container.findViewById(R.id.empty_list_text_container);
        addContentView(gridview_empty_view, new LayoutParams(LayoutParams.FILL_PARENT,
        		LayoutParams.FILL_PARENT));
        flickr_photo_grid.setEmptyView(gridview_empty_view);
        */
        // We'll always start out in List (aka "Details") mode, so hide the grid at first.
        

        getListView().setOnItemClickListener(this);
        getListView().setEmptyView(findViewById(android.R.id.empty));
        flickr_photo_grid.setOnItemClickListener(this);
        
		registerForContextMenu(flickr_photo_grid);
		registerForContextMenu( getListView() );
        
        
        
//    	TextView grid_title = (TextView) findViewById(R.id.photo_list_heading);

    	FlickrPhotoAdapter adapter = new FlickrPhotoDualAdapter(this);
    	flickr_photo_grid.setAdapter( adapter );
    	setListAdapter( adapter );
    	if (savedInstanceState == null) {
    		set_display_mode( !settings.getBoolean("start_details_view", true) );
    	} else {
    		set_display_mode( savedInstanceState.getBoolean("is_grid_mode") );
    		globally_stored_current_search_page = savedInstanceState.getInt("current_search_page");
    	}
    	
    	
    	runInitialSearch(adapter);

		
		
        Log.e(TAG, "Finished onCreate() in PhotoListActivity.");
		
        findViewById(R.id.new_search_empty_button).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				launch_search_dialog();
			}
        });
        

        
        
        
		if (savedInstanceState == null) {
			if (!settings.getBoolean(PREFKEY_SHOW_PHOTOLIST_INSTRUCTIONS, false)) {
				showDialog(DIALOG_PHOTOLIST_INSTRUCTIONS);
			}
		}
        
        findViewById(R.id.picking_indicator).setVisibility(
        		Intent.ACTION_PICK.equals( getIntent().getAction() ) ? View.VISIBLE : View.GONE);
    }

	
    // =============================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
        case DIALOG_LARGE_PHOTO_VIEW:
        {
			dialog.setContentView( createImageDialogContent() );
			break;
        }
        case DIALOG_FLICKR_AVAILABLE_SIZES:
        {
			break;
        }
        case DIALOG_GOTO_PAGE:
        {
	        final EditText number_edit = (EditText) dialog.findViewById(R.id.number_edit);
	        number_edit.setText( Integer.toString( globally_stored_current_search_page ) );
	        break;
        }
        default:
        	break;
        }
        
        super.onPrepareDialog(id, dialog);
    }
	
	// =============================================
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
        switch (id) {
        case DIALOG_GOTO_PAGE:
        {
	        View tagTextEntryView = factory.inflate(R.layout.dialog_numeric_input, null);
	        final EditText number_edit = (EditText) tagTextEntryView.findViewById(R.id.number_edit);
	        
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.menu_go_to_page)
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	try {
	                    globally_stored_current_search_page = Integer.parseInt( number_edit.getText().toString() );
	                    updatePage();
                	} catch (NumberFormatException e) {
                        Toast.makeText(PhotoListActivity.this, "Invalid number.", Toast.LENGTH_SHORT).show();
                	}
                }
            })
            .create();
        }
        case DIALOG_LARGE_PHOTO_VIEW:
        {
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			
			dialog.setContentView( createImageDialogContent() );
			
			dialog.setOnCancelListener( new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {}
			});
			return dialog;
        }
        case DIALOG_PHOTOLIST_INSTRUCTIONS:
        {
	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

	        ((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_photo_list);
	        
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Photo lists")
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

            		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            		settings.edit().putBoolean(PREFKEY_SHOW_PHOTOLIST_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
            		
                }
            })
            .create();
        }
        }
        return super.onCreateDialog(id);
    }

    // ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_photo_list, menu);

        return true;
    }
	
    // ========================================================================    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        FlickrPhotoDualAdapter ad = (FlickrPhotoDualAdapter) getAdapter();
        handle_radio_selection( menu, ad.isGridMode() );

		int photos_per_page = settings.getInt("photos_per_page", Market.DEFAULT_PHOTOS_PER_PAGE);
        boolean more_available = globally_stored_current_search_page*photos_per_page < ad.total_result_count;
        
        boolean has_next_page = globally_stored_color_search_mode_active || more_available;
        boolean has_prev_page = globally_stored_current_search_page > 1;
        menu.findItem(R.id.menu_next_page).setVisible(has_next_page);
        menu.findItem(R.id.menu_prev_page).setVisible(has_prev_page);
        
        boolean multiple_pages = photos_per_page < ad.total_result_count;
        menu.findItem(R.id.menu_goto_page).setVisible(multiple_pages);

        menu.findItem(R.id.menu_page_navigation_group).setVisible(
        		has_next_page || has_prev_page || multiple_pages);
        
        return true;
    }
    
    // ========================================================================
    private String[] view_mode_options = {"Grid", "Details"};
    void handle_radio_selection(Menu menu, boolean grid_view_enabled) {

    	MenuItem base = menu.findItem(R.id.menu_display_mode_base);
    	
    	String button_title = view_mode_options[grid_view_enabled ? 1 : 0] + " View";
    	if (grid_view_enabled) {
    		
	    	base.setTitle(button_title);
	    	base.setTitleCondensed(button_title);
	    	base.setIcon( android.R.drawable.ic_menu_info_details );
	    	
    	} else {
    		base.setTitle(button_title);
	    	base.setTitleCondensed(button_title);
	    	base.setIcon( android.R.drawable.ic_menu_gallery );
    	}
    }

    // ========================================
    
    void updatePage() {
    	if (globally_stored_color_search_mode_active) {
   			int[] int_colors = previous_search_intent.getIntArrayExtra(TabbedSearchActivity.INTENT_EXTRA_COLOR_LIST);
   			if (int_colors != null)
   				fetch_color_images(int_colors);
    	}
    	else {
    		
    		if ( getIntent().getBooleanExtra(INTENT_EXTRA_MY_PHOTOSET_MODE, false) ) {
    			String photoset_id = getIntent().getStringExtra(PhotosetsListActivity.INTENT_EXTRA_MY_PHOTOSET_ID);
    			invokePhotosetSearch( photoset_id );
    			
    		} else {
        			
        		
        		invokeSearch( globally_stored_current_search_parameters,
    				globally_stored_current_search_page,
    				getIntent().getBooleanExtra(INTENT_EXTRA_MY_PHOTOSTREAM_MODE, false)
				);
    		}
    	}
    }
    
    // ========================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
//   	    Log.d(TAG, "GridView visibile before click? " + (flickr_photo_grid.getVisibility() == View.VISIBLE));
//   	    Log.d(TAG, "ListView visibile before click? " + (getListView().getVisibility() == View.VISIBLE));
    	
        switch (item.getItemId()) {
        case R.id.menu_preferences:
        {	
        	Intent i = new Intent();
        	i.setClass(this, PrefsBrowsing.class);
        	this.startActivity(i);
            return true;
        }
        case R.id.menu_goto_page:
        {
        	showDialog(DIALOG_GOTO_PAGE);
            return true;
        }
        case R.id.menu_prev_page:
        case R.id.menu_next_page:
        {	

			if (item.getItemId() == R.id.menu_next_page)
	        	globally_stored_current_search_page++;
			else
				globally_stored_current_search_page--;
			
        	
			updatePage();


            return true;
        }
        case R.id.menu_display_mode_base:
        {
        	set_display_mode( !((FlickrPhotoDualAdapter) getAdapter()).isGridMode() );
            return true;
        }
        case R.id.menu_help:
        {	
            showDialog(DIALOG_PHOTOLIST_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_slideshow:
        {	
        	Intent slideshow_intent = new Intent(getIntent());
        	slideshow_intent.setClass(this, LiveSlideshowActivity.class);
            startActivity(slideshow_intent);
            return true;
        }
        }
        return super.onOptionsItemSelected(item);
    }

    // ========================================
	@Override
	int getMainLayoutResource() {
		return R.layout.list_activity_user_photos;
	}

    // ========================================================
    void remove_from_photoset(String photoset_id, long photo_id) {

    	Auth auth;
    	Flickr flickr = null;
    	PhotosetsInterface photosets_interface = null;
    	
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		photosets_interface = flickr.getPhotosetsInterface();
		
		auth = new Auth();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
		auth.setPermission(Permission.WRITE);
		
		RequestContext requestContext = RequestContext.getRequestContext();
		requestContext.setAuth(auth);
		
		try {
			photosets_interface.removePhoto(photoset_id, Long.toString(photo_id));

            Toast.makeText(this, "Removed from set.", Toast.LENGTH_SHORT).show();
            
            
            FlickrPhotoAdapter ad = (FlickrPhotoAdapter) getAdapter();
            List<Photo> list = ad.photo_list;
            for (Photo p : list) {
            	if (Long.parseLong( p.getId() ) == photo_id) {
            		list.remove(p);
            		break;
            	}
            }
            ad.notifyDataSetInvalidated();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {

            Toast.makeText(this, e.getErrorMessage(), Toast.LENGTH_LONG).show();
			
			e.printStackTrace();
		}
    }
    
    // ========================================================
    void add_photo_to_set(String photoset_id, long photo_id) {

    	Auth auth;
    	Flickr flickr = null;
    	PhotosetsInterface photosets_interface = null;
    	
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		photosets_interface = flickr.getPhotosetsInterface();
		
		auth = new Auth();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
		auth.setPermission(Permission.WRITE);
		
		RequestContext requestContext = RequestContext.getRequestContext();
		requestContext.setAuth(auth);
		
		try {
			photosets_interface.addPhoto(photoset_id, Long.toString(photo_id));

            Toast.makeText(this, "Added to set.", Toast.LENGTH_SHORT).show();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {

            Toast.makeText(this, e.getErrorMessage(), Toast.LENGTH_LONG).show();
			
			e.printStackTrace();
		}
    }
    // =============================================

    final int SWIPE_MAX_OFF_PATH = 250;
    final int SWIPE_MIN_DISTANCE = 120;
    final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;

				int current_selection = globally_stored_selected_thumbnail_index;
				int next_selection = current_selection;

                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {

                	next_selection = (getAdapter().getCount() + current_selection - 1) % getAdapter().getCount();
                    Toast.makeText(PhotoListActivity.this, "Previous (" + next_selection + ")", Toast.LENGTH_SHORT).show();
                	
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {

                	next_selection = (current_selection + 1) % getAdapter().getCount();
                    Toast.makeText(PhotoListActivity.this, "Next (" + next_selection + ")", Toast.LENGTH_SHORT).show();
                	
                } else return false;
                
//				Log.d(TAG, "Current: " + current_selection + "; Next: " + next_selection);
				
                flickr_photo_grid.setSelection(next_selection);
                getListView().setSelection(next_selection);
                
				picture_cycler(next_selection);
                
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }
    
    // =============================================
	void picture_cycler(int pos) {
		
		globally_stored_selected_thumbnail_index = pos;
		
		Photo photo = (Photo) getAdapter().getItem(globally_stored_selected_thumbnail_index);
		large_image_getter_task = new LargeImageGetterTaskExtended(true, DownloadObjective.SHOW_IN_DIALOG);
		large_image_getter_task.execute(photo);
	}
    
    // =============================================
    View createImageDialogContent() {
		ImageView i = new ImageView(this);
		i.setImageBitmap( globally_stored_dialog_bitmap );
		i.setAdjustViewBounds(true);
//		i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		
		
		i.setOnTouchListener(gestureListener);

/*
		i.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Photo photo = (Photo) getAdapter().getItem(globally_stored_selected_thumbnail);
				launchZoom(photo, globally_stored_dialog_bitmap);
			}
		});
*/
		
		i.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View v) {
				
				Photo photo = (Photo) getAdapter().getItem(globally_stored_selected_thumbnail_index);
				launchCustomZoom(PhotoListActivity.this, photo, globally_stored_dialog_bitmap);
				
				return true;
			}
			
		});
		
/*
		i.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View v) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(PhotoListActivity.this);
				builder.setMessage("Set as wallpaper?")
				       .setCancelable(false)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				    	   
				           public void onClick(DialogInterface dialog, int id) {

					   			if (Meta.FULL_VERSION) {
									
					   				new SetWallpaperTask().execute(globally_stored_dialog_bitmap);
	//								new WallpaperGetterTask(true).execute(photo);
									
								} else {
									globally_stored_feature_nag = false;
									globally_stored_disabled_function_description = getResources().getString(R.string.disabled_generic);
									showDialog(DIALOG_PURCHASE_MESSAGE);
								}
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();

				
				
				return true;
			}
			
		});
*/
		return i;
    }

    // =============================================
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
    	
//    	Log.i(TAG, "What was the intent action? " + getIntent().getAction());

		Photo photo = (Photo) getAdapter().getItem(position);
		
    	if ( Intent.ACTION_PICK.equals( getIntent().getAction() ) ) {

    		long photo_id = Long.parseLong(photo.getId());
    		Log.d(TAG, "Selected photo_id for ACTION_PICK: " + photo_id);

    		Intent result_intent = new Intent();
    		result_intent.putExtra(INTENT_EXTRA_PHOTO_ID, photo_id);
    		
    		if (!getIntent().hasCategory(IntentConstants.CATEGORY_FLICKR_PHOTO)) {
    			// This is for Crittr compatibility.
    			// We must provide a callback URI for image meta-data.
    			
        		Uri uri = ExperimentalFileContentProvider.constructMetaInfoUri(photo_id);
        		Log.i(TAG, "Constructed Socket URI as: " + uri);
        		result_intent.setData( uri );
    		}
    		
	    	setResult(Activity.RESULT_OK, result_intent);
	    	finish();
	    	
    	} else if ( Intent.ACTION_GET_CONTENT.equals( getIntent().getAction() ) ) {

    		globally_stored_selecting_for_content_uri_flag = true;
 
    		globally_stored_selected_photo_id = Long.parseLong( photo.getId() );
    		new ImageSizesGetterExtended(this).execute(photo);

	    	
    	} else if ( ACTION_GET_SOCKET_CONTENT.equals( getIntent().getAction() ) ) {

    		// Here we only want the thumbnail, anyway - we leave it to the ContentProvider
    		// to retrieve the proper URL.
    		
    		long photo_id = Long.parseLong(photo.getId());
    		Log.d(TAG, "Selected photo_id for ACTION_GET_SOCKET_CONTENT: " + photo_id);

    		Intent result_intent = new Intent();
    		Uri uri = ExperimentalFileContentProvider.constructSocketUri(photo_id);
    		Log.i(TAG, "Constructed URI as: " + uri);
    		result_intent.setData( uri );
	    	setResult(Activity.RESULT_OK, result_intent);
	    	finish();
    	}
    	else {
    		if (Integer.parseInt(settings.getString("photo_list_click_action", "0")) == 0)
    			viewPhotoInfoPage(photo);
    		else
    			picture_cycler(position);
    	}
    }
    
    // ============================================================
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		
		Log.d(TAG, "Context menu item selected.");
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Photo photo = (Photo) getAdapter().getItem(info.position);
		
		switch ( item.getItemId() ) {
		case R.id.menu_start_slideshow_here:
		{
        	Intent slideshow_intent = new Intent(getIntent());
        	slideshow_intent.setClass(this, LiveSlideshowActivity.class);
        	slideshow_intent.putExtra(LiveSlideshowActivity.EXTRA_STARTING_SLIDE, info.position);
            startActivity(slideshow_intent);
			break;
		}
		case R.id.menu_zoom:
		{
			
			String thumbnail_url = photo.getSmallSquareUrl().toString();
			Bitmap bm = (Bitmap) ((FlickrPhotoDualAdapter) getAdapter()).photo_populator.bitmapReferenceMap.get(thumbnail_url).get();
			
			launchCustomZoom(PhotoListActivity.this, photo, bm);
			break;
		}
		case R.id.menu_goto_flickr:
		{
			Uri flickr_destination = Uri.parse( photo.getUrl() );

        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

			break;
		}
		case R.id.menu_flickr_favorite:
		{
			makeFlickrFavorite(photo);
			break;
		}
		
		case R.id.menu_add_to_photoset:
		{
			Intent i = new Intent();
			i.setAction(Intent.ACTION_PICK);
			i.putExtra(INTENT_EXTRA_PHOTO_ID, Long.parseLong( photo.getId() ));
			i.setClass(this, PhotosetsListActivity.class);
			startActivityForResult(i, REQUEST_CODE_PHOTOSET_CHOOSER);

			break;
		}
		case R.id.menu_remove_from_photoset:
		{
			String photoset_id = getIntent().getStringExtra(PhotosetsListActivity.INTENT_EXTRA_MY_PHOTOSET_ID);
			remove_from_photoset(photoset_id, Long.parseLong( photo.getId() ));
			break;
		}
		case R.id.menu_set_for_contact:
		{

			sharing_saved_image = false;
			globally_stored_selected_photo_id = Long.parseLong( photo.getId() );
//			new GenericBitmapDownloadTaskExtended(this).execute(photo.getLargeUrl());
			new ImageSizesGetterExtended(this).execute(photo);
		
			break;
		}
		case R.id.menu_download_photo:
		{

			downloading_image_only = true;
			globally_stored_selected_photo_id = Long.parseLong( photo.getId() );
			new ImageSizesGetterExtended(this).execute(photo);
		
			break;
		}
		case R.id.menu_share_photo:
		{
				
			sharing_saved_image = true;
			globally_stored_selected_photo_id = Long.parseLong( photo.getId() );
			new ImageSizesGetterExtended(this).execute(photo);

			break;
		}
		/*
		case R.id.menu_set_as_wallpaper:
		{
			if (Meta.FULL_VERSION) {
				
				new WallpaperGetterTask(this, false).execute(photo);
//				new WallpaperGetterTask(true).execute(photo);
				
			} else {
				globally_stored_feature_nag = false;
				globally_stored_disabled_function_description = getResources().getString(R.string.disabled_generic);
				showDialog(DIALOG_PURCHASE_MESSAGE);
			}
		
			break;
		}
		*/
		case R.id.menu_view_info:
		{
			viewPhotoInfoPage(photo);
			break;
		}	
		default:
			break;
		}

		return super.onContextItemSelected(item);
	}

    // ========================================================
    void viewPhotoInfoPage(Photo photo) {
    	long photo_id = Long.parseLong( photo.getId() );
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.addCategory(IntentConstants.CATEGORY_FLICKR_PHOTO);
    	i.putExtra(IntentConstants.PHOTO_ID, photo_id);
    	startActivityForResult(i, REQUEST_CODE_TAGGING_ACTIVITY_REFRESH);
    }
    
    // ========================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
	   		case REQUEST_CODE_PHOTOSET_CHOOSER:
	   		{
	   			long photo_id = data.getLongExtra(INTENT_EXTRA_PHOTO_ID, -1);
	   			String photoset_id = data.getStringExtra(PhotosetsListActivity.INTENT_EXTRA_MY_PHOTOSET_ID);
	   			
	   			add_photo_to_set(photoset_id, photo_id);
	   			break;
	   		}
	  	   	}

		}
	  	super.onActivityResult(requestCode, resultCode, data);
    }

    // ========================================================================
	@Override
	void executeSearchTask(Context context, RefreshablePhotoListAdapter adapter,
			TextView grid_title, SearchParameters sp, int current_page,
			AtomicInteger retrieval_tasks_semaphore) {

		int per_page = settings.getInt("photos_per_page", Market.DEFAULT_PHOTOS_PER_PAGE);
    	photo_search_task = new RetrievalTaskFlickrPhotolist(
    			this,
    			adapter,
    			grid_title,
    			sp,
    			current_page,
    			per_page,
    			retrieval_tasks_semaphore);
    	
    	photo_search_task.execute();
	}

    // ========================================================================
	@Override
	void executePhotosetSearchTask(Context context, RefreshablePhotoListAdapter adapter,
			TextView gridTitle, String photosetId, int currentPage, AtomicInteger taskSemaphore) {

		int per_page = settings.getInt("photos_per_page", Market.DEFAULT_PHOTOS_PER_PAGE);
    	
    	photo_search_task = new RetrievalTaskFlickrPhotoset(
    			this,
    			adapter,
    			gridTitle,
    			photosetId,
    			currentPage,
    			per_page,
    			taskSemaphore);
    	
    	photo_search_task.execute();
	}
}
