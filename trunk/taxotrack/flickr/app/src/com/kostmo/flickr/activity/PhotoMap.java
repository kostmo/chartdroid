package com.kostmo.flickr.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.SearchParameters;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.kostmo.flickr.activity.prefs.PrefsGlobal;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.AdapterHost;
import com.kostmo.flickr.containers.AnimalsItemizedOverlay;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.containers.OverlayItemWithPhoto;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;
import com.kostmo.flickr.tasks.AsyncPhotoPopulator;
import com.kostmo.flickr.tasks.RetrievalTaskFlickrPhotolist;
import com.kostmo.flickr.tools.FlickrTaskUtils.GeoBox;


public class PhotoMap extends MapActivity implements RefreshablePhotoListAdapter, AdapterHost
{

	public static String INTENT_EXTRA_LAUNCH_MODE = "INTENT_EXTRA_LAUNCH_MODE";

	public static final int LAUNCH_MODE_REGION = 1;
	
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();
	
    public static final String PREFKEY_MAP_INSTRUCTIONS = "PREFKEY_MAP_INSTRUCTIONS";
	final int DIALOG_MAP_INSTRUCTIONS = 1;

    int globally_stored_search_radio_button = -1;
    int initially_checked_item_id = -1;

	static final String TAG = Market.DEBUG_TAG; 

	final int REQUEST_CODE_SEARCH_BUILDER = 3;
	
	private boolean boxed_in_search_enabled = false;
	
    MapView mapView;
    MapController mc;
    GeoPoint p;
    
    View recursive_search_cancel_button, cycle_forward_button, cycle_back_button;
    
    AsyncPhotoPopulator flickr_thumbnail_manager;

	String globally_stored_disabled_function_description;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        flickr_thumbnail_manager = new AsyncPhotoPopulator(this);
        
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.map_panel);
        
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

        cycle_forward_button = findViewById(R.id.right_nav_button);
        cycle_forward_button.setOnClickListener(cb_nav_forward);
        
        cycle_back_button = findViewById(R.id.left_nav_button);
        cycle_back_button.setOnClickListener(cb_nav_back);
        
        
        findViewById(R.id.map_popup_photo_thumbnail).setOnClickListener(cb_view_photo);

        // ************* MAP CONTENT ************* 
        
        mapView = (MapView) findViewById(R.id.mapView);

        
        LinearLayout zoomLayout = (LinearLayout) findViewById(R.id.zoom);  
        View zoomView = mapView.getZoomControls();
 
        zoomLayout.addView(zoomView, 
            new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, 
                LayoutParams.WRAP_CONTENT)); 
        mapView.displayZoomControls(true);
        
        
        
        mc = mapView.getController();
        



		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	TextView grid_title = (TextView) findViewById(R.id.photo_list_heading);
        
        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	if (a.overlay != null) {
        		
        		/*
                List<Overlay> listOfOverlays = mapView.getOverlays();
                listOfOverlays.add(a.overlay);
                
                Log.d(TAG, "Just restored map overlay.  Supposedly.");
                */
        		
        		
        		// FIXME: Crappy hack
        		
                Log.d(TAG, "I'm redoing the search instead of retrieving from memory.");
       			
   				SearchParameters sp = PhotoListActivity.searchParmsFromPrefs(settings, this);
	   		    invokeSearch( sp );
        		
        		
        	} else {
        		

                Log.e(TAG, "Failed to restore map overlay.");
        	}
        	
        	/*
    		globally_stored_dialog_bitmap = a.dialog_bitmap;
    		globally_stored_selected_thumbnail = a.selected_thumbnail;
    		*/
        	
    		grid_title.setText( a.list_header );
    		
        } else {
        	
//        	initial_display_mode = getIntent().getIntExtra(PhotoListActivity.INTENT_EXTRA_PHOTOLIST_VIEW_MODE, PhotolistViewMode.LIST.ordinal());

    		if ( settings.getBoolean("launch_with_last_search", true) ) {
    			

                Log.d(TAG, "I'm supposed to automatically redo the search now!");
   			
   				SearchParameters sp = PhotoListActivity.searchParmsFromPrefs(settings, this);
	   		    invokeSearch( sp );
	   		    
    		} else {
    			
    			Log.d(TAG, "Launching search dialog in onCreate().");

            	// Launch the search window if we are just arriving here
    			launch_search_dialog();
    			
    			
    			
    			
    			
    			
    			
    			
    			
    			
    			
    	        double lat, lng;
    	        
    	    	LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	    	Location last_location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	    	if (last_location != null) {

    	    		lat = last_location.getLatitude();
    	    		lng = last_location.getLongitude();
    	    	}	
    	    	else {
    	    		Log.e(TAG, "Needs at least one location update!");
    	    		String[] coordinates = new String[] {"1.352566007", "103.78921587"};
    	            lat = Double.parseDouble(coordinates[0]);
    	            lng = Double.parseDouble(coordinates[1]);
    	    	}
    	 
    	        p = new GeoPoint(
    	            (int) (lat * 1E6), 
    	            (int) (lng * 1E6));
    	 
    	        mc.animateTo(p);
    	        mc.setZoom(17);
    		}
        }
        
        
        
        
        
        
        
        
        

        mapView.invalidate();
        
		if (!settings.getBoolean(PREFKEY_MAP_INSTRUCTIONS, false)) {
			showDialog(DIALOG_MAP_INSTRUCTIONS);
		}
    }


    // ===================================================
    
    class StateRetainer {
    	String list_header;

    	Bitmap dialog_bitmap;
    	
    	AnimalsItemizedOverlay overlay;

    	int selected_thumbnail;
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {

    	StateRetainer a = new StateRetainer();

		TextView subtitle_holder = (TextView) findViewById(R.id.photo_list_heading);
		String subtitle_text = subtitle_holder.getText().toString();
		a.list_header = subtitle_text;
		
    	if ( mapView.getOverlays().size() > 0) {

    		AnimalsItemizedOverlay aio = (AnimalsItemizedOverlay) mapView.getOverlays().get(0);
    	
    		a.overlay = aio;
    	}
    		
		/*
		a.dialog_bitmap = globally_stored_dialog_bitmap;
		a.selected_thumbnail = globally_stored_selected_thumbnail;
		*/
        return a;
    }

    // =============================================
	
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	
        switch (id) {
        default:
        	break;
        }
    }
    // ===================================================
    
    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
        case DIALOG_MAP_INSTRUCTIONS:
        {
	        LayoutInflater factory = LayoutInflater.from(this);

	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

	        TextView instructions_text = (TextView) tagTextEntryView.findViewById(R.id.instructions_textview);
	        instructions_text.setText(R.string.instructions_map);
	        instructions_text.setMovementMethod(LinkMovementMethod.getInstance());
	        
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.instructions_map_title)
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

            		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PhotoMap.this);
            		settings.edit().putBoolean(PREFKEY_MAP_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
            		
                }
            })
            .create();
        }
        }
        
        return null;
    }

    // ===================================================
    
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	Log.i(TAG, "onSaveInstanceState");
    	
        out_bundle.putInt("globally_stored_search_radio_button", globally_stored_search_radio_button);
        
    	out_bundle.putString("disabled_function_description", globally_stored_disabled_function_description);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	Log.i(TAG, "onRestoreInstanceState");

    	globally_stored_search_radio_button = in_bundle.getInt("globally_stored_search_radio_button");
    	
		
		globally_stored_disabled_function_description = in_bundle.getString("disabled_function_description");
    }
    
    
    // ===================================================
	private View.OnClickListener cb_nav_forward = new View.OnClickListener() {
	    public void onClick(View v) {
	    	
	    	if ( mapView.getOverlays().size() > 0) {

	    		AnimalsItemizedOverlay aio = (AnimalsItemizedOverlay) mapView.getOverlays().get(0);
	    		
	    		OverlayItemWithPhoto next_focus = aio.nextFocus(true);

//	    		Log.d(TAG, "NAV FORWARD: next_focus: " + next_focus);
	    		if (next_focus == null) {
	    			
	    			OverlayItemWithPhoto first_item = aio.getItem(0);
	    			

//		    		Log.i(TAG, "Next is null, setting focus to first item: " + first_item);
	    			
	    			aio.setFocus( first_item );
	    		}
	    		else {
	    			aio.setFocus( next_focus );
	    			
	    		}
	    		
	    	}
	    }
	};
	
    // ===================================================
	private View.OnClickListener cb_nav_back = new View.OnClickListener() {
	    public void onClick(View v) {

	    	if ( mapView.getOverlays().size() > 0) {

	    		AnimalsItemizedOverlay aio = (AnimalsItemizedOverlay) mapView.getOverlays().get(0);
	    		

	    		OverlayItemWithPhoto prev_focus = aio.nextFocus(false);
	    		Log.d(TAG, "NAV BACK: prev_focus: " + prev_focus);
	    		if (prev_focus == null)
	    			aio.setFocus( aio.getItem( aio.size() - 1 ) );
	    		else
	    			aio.setFocus( prev_focus );
	    	}
	    }
	};
    

    // ===================================================
	private View.OnClickListener cb_view_photo = new View.OnClickListener() {
	    public void onClick(View v) {
	    	

    		AnimalsItemizedOverlay aio = (AnimalsItemizedOverlay) mapView.getOverlays().get(0);
    		String photo_id_string = aio.getFocus().photo.getId();

    		// If we're viewing "Sightings", then there won't be a Photo ID.
    		try {
    			long photo_id = Long.parseLong(photo_id_string);
    			
            	Intent i = new Intent();
	        	i.setAction(Intent.ACTION_VIEW);
	        	i.addCategory(IntentConstants.CATEGORY_FLICKR_TAGS);
            	
            	// FIXME: Actually, the receiver expects a String.
//            	i.putExtra(ListActivityPhotoTags.INTENT_EXTRA_PHOTO_ID, Long.toString(photo_id) );
            	i.putExtra(IntentConstants.PHOTO_ID, photo_id );
            	startActivity(i);
            	
    		} catch (NumberFormatException e) {
    			e.printStackTrace();
    		}
	    }
	};
	


    
    public void move_to_geobox(GeoBox gb) {
    	
    	int lat_center = (gb.bottom_left.getLatitudeE6() + gb.top_right.getLatitudeE6()) / 2;
    	int lon_center = (gb.bottom_left.getLongitudeE6() + gb.top_right.getLongitudeE6()) / 2;
    	
        mc.setCenter(new GeoPoint(lat_center, lon_center));
        
        int lat_span = gb.top_right.getLatitudeE6() - gb.bottom_left.getLatitudeE6();
        int lon_span = gb.top_right.getLongitudeE6() - gb.bottom_left.getLongitudeE6();
        mc.zoomToSpan(lat_span, lon_span);
    }
    

    

    private GeoBox getCurrentGeoBox() {
    	
    	GeoPoint map_center = mapView.getMapCenter();
    	int lat_span = mapView.getLatitudeSpan();
    	int long_span = mapView.getLongitudeSpan();
    	
    	GeoPoint lower_left_geo = new GeoPoint(map_center.getLatitudeE6() - lat_span/2, map_center.getLongitudeE6() - long_span/2);
    	GeoPoint upper_right_geo = new GeoPoint(map_center.getLatitudeE6() + lat_span/2, map_center.getLongitudeE6() + long_span/2);
    	
    	GeoBox b = new GeoBox(lower_left_geo, upper_right_geo);
    	
    	return b;

    }
    
    


    public void clear_list() {
    	AnimalsItemizedOverlay ao = new AnimalsItemizedOverlay( this.getResources().getDrawable(R.drawable.pushpin_pink) );
    	ao.refreshItems( new ArrayList<Photo>() );

        List<Overlay> listOfOverlays = mapView.getOverlays();
        listOfOverlays.clear();
        listOfOverlays.add(ao);
    }
    
    public void refresh_list( List<Photo> new_photo_list ) {
    	
    	AnimalsItemizedOverlay ao = new AnimalsItemizedOverlay( this.getResources().getDrawable(R.drawable.pushpin_pink) );
    	ao.setOnFocusChangeListener(new ItemizedOverlay.OnFocusChangeListener () {

//    		@Override
			public void onFocusChanged(ItemizedOverlay overlay, OverlayItem newFocus) {
				
				
    			
				if (newFocus != null) {

					OverlayItemWithPhoto casted_item = (OverlayItemWithPhoto) newFocus;
					
					
					mc.setCenter( newFocus.getPoint() );
					
					View marker_popup_cell = findViewById(R.id.marker_popup_cell);
					marker_popup_cell.setVisibility(View.VISIBLE);
					
					
					
					
			        
			        TextView header = (TextView) findViewById(R.id.blurb_header);
			        header.setText( newFocus.getTitle() );
			        
			        TextView content = (TextView) findViewById(R.id.blurb_content);
			        content.setText( newFocus.getSnippet() );
			        
			        
			        ImageView thumbnail_view = (ImageView) findViewById(R.id.map_popup_photo_thumbnail);

	                View icon_goto_photopage = findViewById(R.id.icon_goto_photopage);
	                
	                
			        final Photo photo = casted_item.photo;
			        
			        // TODO: Fetch the Flickr photo on a thread!
			        if (photo != null) {
			        	
			        	marker_popup_cell.setBackgroundResource(R.drawable.popup_full_bright);
			        	
			        	thumbnail_view.setVisibility(View.VISIBLE);
			        	icon_goto_photopage.setVisibility(View.VISIBLE);

				        
			        	flickr_thumbnail_manager.fetchDrawableByUrlOnThread(photo.getSmallSquareUrl(), thumbnail_view);
			        	
			        	
			        	icon_goto_photopage.setOnClickListener(new OnClickListener() {

							public void onClick(View v) {

								Uri flickr_destination = Uri.parse( photo.getUrl() );
					        	// Launches the standard browser.
					        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));
							}
			        	});
			        	
			        } else {
			        	marker_popup_cell.setBackgroundResource(R.drawable.panel_background);
			        	
			        	thumbnail_view.setVisibility(View.GONE);
			        	icon_goto_photopage.setVisibility(View.GONE);
			        	
			        	Log.e(TAG, "Has no associated photo!");
			        }
			        
			        
				} else {
					
			        findViewById(R.id.marker_popup_cell).setVisibility(View.GONE);
				}
			
			}
    	});
    	
    	
    	
    	ao.refreshItems(new_photo_list);
    	
        List<Overlay> listOfOverlays = mapView.getOverlays();
        listOfOverlays.clear();
        listOfOverlays.add(ao);
        
        
        /*
        GeoBox location_bounds = FlickrTaskUtils.growBoundingBox(new_photo_list);
    	move_to_geobox(location_bounds);
    	*/
    	
        
        mc.zoomToSpan(ao.getLatSpanE6(), ao.getLonSpanE6());
   	
    	
        if (new_photo_list.size() == 0) {
        	cycle_forward_button.setVisibility(View.GONE);
            cycle_back_button.setVisibility(View.GONE);
        } else {

    		ao.setFocus( ao.getItem(0) );
    		
        	cycle_forward_button.setVisibility(View.VISIBLE);
            cycle_back_button.setVisibility(View.VISIBLE);
        }
    }
    
    
    public void populate_map_points( List<Photo> animal_geopoints ) {

        refresh_list(animal_geopoints);
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
    	PhotoMap.this.mc = mapView.getController(); 
        switch (keyCode) 
        {
            case KeyEvent.KEYCODE_3:
                mc.zoomIn();
                break;
            case KeyEvent.KEYCODE_1:
                mc.zoomOut();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }    


    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    
    // =============================================
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_map, menu);

        if (initially_checked_item_id >= 0)
        	menu.findItem( initially_checked_item_id ).setChecked(true);
        
        return true;
    }


    Menu stashed_options_menu;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (globally_stored_search_radio_button != -1)
        	menu.findItem( globally_stored_search_radio_button ).setChecked(true);

        stashed_options_menu = menu;

        return true;
    }
    
    
    void handle_radio_selection(MenuItem item) {


    	item.setChecked(true);
    	
    	handle_radio_check( item.getItemId() );
    	
    	/*
    	MenuItem base = stashed_options_menu.findItem(R.id.menu_sort_mode);
    	base.setTitle(item.getTitle());
    	base.setIcon(item.getIcon());
    	base.setTitleCondensed(item.getTitleCondensed());
    	*/
    }

    
    
    void handle_radio_check(int id) {
    	
        findViewById(R.id.marker_popup_cell).setVisibility(View.GONE);
    }
    
    
    private void launch_search_dialog() {
    	Intent i = new Intent();
    	i.setClass(getBaseContext(), TabbedSearchActivity.class);
//    	i.setClass(getBaseContext(), FlickrTabbedSearchActivity.class);
    	
    	i.putExtra(TabbedSearchActivity.INTENT_EXTRA_DISABLE_COLOR_SEARCH, true);
    	
    	startActivityForResult(i, REQUEST_CODE_SEARCH_BUILDER);
    }
    
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	
    	
        switch (item.getItemId()) {
        case R.id.menu_help:
        {
        	showDialog(DIALOG_MAP_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_option_search_here:
        {
			boxed_in_search_enabled = true;
        	launch_search_dialog();

            return true;
        }
        case R.id.menu_option_new_search:
        {
        	

        	boxed_in_search_enabled = false;
        	launch_search_dialog();

    	

            return true;
        }
        
        case R.id.menu_preferences:
        {
        	Intent i = new Intent();
        	i.setClass(this, PrefsGlobal.class);
        	this.startActivity(i);
        	
        	return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }

    // =============================================    

    private void invokeSearch(SearchParameters sp) {

    	Log.d(TAG, "Invoking search...");
    	
    	TextView grid_title = (TextView) findViewById(R.id.photo_list_heading);

		// We must both require that the photo is geotagged,
		// and return the geo info. 
		sp.setExtrasGeo(true);
		sp.setHasGeo(true);
		
		
		if (boxed_in_search_enabled) {
	
		    GeoBox geo_box = getCurrentGeoBox();

			String min_lon = Float.toString( geo_box.bottom_left.getLongitudeE6()/1E6f );
			String max_lon = Float.toString( geo_box.top_right.getLongitudeE6()/1E6f );
			String min_lat = Float.toString( geo_box.bottom_left.getLatitudeE6()/1E6f );
			String max_lat = Float.toString( geo_box.top_right.getLatitudeE6()/1E6f );
			
			sp.setBBox(min_lon, min_lat, max_lon, max_lat);
			
			Log.d(TAG, "Bounding box: (" + min_lon + ", " + min_lat + ", " + max_lon + ", " + max_lat + ")");
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		sp.setSort( Integer.parseInt( settings.getString("sort_order", Integer.toString( SearchParameters.INTERESTINGNESS_DESC) ) ) );


		int per_page = settings.getInt("photos_per_page", Market.DEFAULT_PHOTOS_PER_PAGE);
    	RetrievalTaskFlickrPhotolist rtfp = new RetrievalTaskFlickrPhotolist(
    			this,
    			this,
    			grid_title,
    			sp,
    			1,
    			per_page,
    			retrieval_tasks_semaphore);
    	
    	rtfp.execute();
    }

    
    // =============================================
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
	   		case REQUEST_CODE_SEARCH_BUILDER:
	   		{
   				SearchParameters sp = PhotoListActivity.searchParmsFromIntent(data);
	   		    invokeSearch( sp );
	   			break;
	   		}
	   		default:
		    	break;
		   }
		}
    }
    
    
    // =======================================
    
    @Override
    protected void  onStop  () {
    	super.onStop();

    }


	public ListAdapter getAdapter() {
		// TODO Auto-generated method stub
		return null;
	}







	long total_results;
	public void setTotalResults(long totalResults) {
		total_results = totalResults;
	}
}

