package com.kostmo.flickr.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.favorites.FavoritesInterface;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.SearchParameters;
import com.aetrion.flickr.photos.Size;
import com.kostmo.flickr.adapter.FlickrPhotoAdapter;
import com.kostmo.flickr.adapter.FlickrPhotoDualAdapter;
import com.kostmo.flickr.bettr.ApplicationState;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.colorsearch.ColorMatcherPhotoIdRetrievalTask;
import com.kostmo.flickr.containers.AdapterHost;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.containers.ProgressHostActivity;
import com.kostmo.flickr.containers.RefreshablePhotoListAdapter;
import com.kostmo.flickr.data.DatabaseSearchHistory;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.provider.ExperimentalFileContentProvider;
import com.kostmo.flickr.provider.LocalFileContentProvider;
import com.kostmo.flickr.tasks.GenericBitmapDownloadTask;
import com.kostmo.flickr.tasks.GenericFileDownloadTask;
import com.kostmo.flickr.tasks.ImageSizesGetterTask;
import com.kostmo.flickr.tasks.LargeImageGetterTask;

public abstract class SearchResultsViewerActivity extends Activity implements AdapterHost, ProgressHostActivity {

	static final String TAG = "SearchResultsViewerActivity";
    // ========================================================================

	public enum PhotolistViewMode {GRID, LIST}


	static final String ACTION_GET_SOCKET_CONTENT = "com.kostmo.intent.GET_SOCKET_CONTENT";
	
	

	final int DIALOG_LARGE_PHOTO_VIEW = 2;	// Only used from PhotoListActivity
	final int DIALOG_FLICKR_AVAILABLE_SIZES = 4;
	final int DIALOG_PROGRESS = 5;
	
	
	
	
	final int REQUEST_CODE_TAGGING_ACTIVITY_REFRESH = 1;
	final int REQUEST_CODE_SEARCH_BUILDER = 2;
	final int REQUEST_CODE_PHOTOSET_CHOOSER = 3;
	final int REQUEST_CODE_DIRECTORY_CHOOSER = 4;

	
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	
	final static String INTENT_EXTRA_PHOTOLIST_TYPE = "INTENT_EXTRA_PHOTOLIST_TYPE";
	final static String INTENT_EXTRA_GROUP_ID = "INTENT_EXTRA_GROUP_ID";

	final static String INTENT_EXTRA_PHOTO_ID = "INTENT_EXTRA_PHOTO_ID";
	
	final static String INTENT_EXTRA_PHOTOLIST_VIEW_MODE = "INTENT_EXTRA_PHOTOLIST_VIEW_MODE";
	final static String INTENT_EXTRA_MY_PHOTOSTREAM_MODE = "INTENT_EXTRA_MY_PHOTOSTREAM_MODE";
	final static String INTENT_EXTRA_MY_PHOTOSET_MODE = "INTENT_EXTRA_MY_PHOTOSET_MODE";
	final static String INTENT_EXTRA_MY_CONTACTS_MODE = "INTENT_EXTRA_MY_CONTACTS_MODE";
	
	
	
	GridView flickr_photo_grid;
	Bitmap globally_stored_dialog_bitmap;
	SharedPreferences settings;

	int globally_stored_selected_thumbnail_index = -1;
	int globally_stored_current_search_page = 1;
	boolean globally_stored_color_search_mode_active = false;
	String globally_stored_disabled_function_description;
	SearchParameters globally_stored_current_search_parameters;
	long globally_stored_selected_photo_id = -1;
    Collection<Size> globally_stored_collected_sizes;
	
	String pending_download_url;
	String pending_suggested_filename;
    boolean downloading_image_only = false;
    boolean sharing_saved_image = false;
    
    boolean globally_stored_selecting_for_content_uri_flag = false;

    Intent previous_search_intent;


	public ProgressDialog prog_dialog;
	LargeImageGetterTaskExtended large_image_getter_task = null;

    // ========================================================================
    abstract int getMainLayoutResource();
    
    // ========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        super.onCreate(savedInstanceState);
        setContentView( getMainLayoutResource() );
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);



        this.mMediaScannerConnection = new MediaScannerConnection(this, this.mMediaScanConnClient);
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);


        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	this.large_image_getter_task = a.large_image_getter_task;
        	if (this.large_image_getter_task != null)
        		this.large_image_getter_task.updateActivity(this);
        }
    }

    // ========================================================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_SEARCH) {
    		launch_search_dialog();
    		return true;
    	}
    	return super.onKeyDown(keyCode, event);
    }
    
    // ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	super.onSaveInstanceState(out_bundle);

    	out_bundle.putInt("current_search_page", globally_stored_current_search_page);

    	out_bundle.putString("disabled_function_description", globally_stored_disabled_function_description);

    	out_bundle.putBoolean("sharing_saved_image", sharing_saved_image);
    	out_bundle.putBoolean("downloading_image_only", downloading_image_only);
    	out_bundle.putBoolean("globally_stored_selecting_for_content_uri_flag", globally_stored_selecting_for_content_uri_flag);

    	out_bundle.putString("pending_download_url", pending_download_url);
    	out_bundle.putString("pending_suggested_filename", pending_suggested_filename);
    	
    	out_bundle.putLong("selected_photo_id", globally_stored_selected_photo_id);
    }
    
    // ========================================================================
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	super.onRestoreInstanceState(in_bundle);

		globally_stored_disabled_function_description = in_bundle.getString("disabled_function_description");
		
		pending_download_url = in_bundle.getString("pending_download_url");
		pending_suggested_filename = in_bundle.getString("pending_suggested_filename");
		
		sharing_saved_image = in_bundle.getBoolean("sharing_saved_image");
		downloading_image_only = in_bundle.getBoolean("downloading_image_only");
		globally_stored_selecting_for_content_uri_flag = in_bundle.getBoolean("globally_stored_selecting_for_content_uri_flag");
		
		
		globally_stored_selected_photo_id = in_bundle.getLong("selected_photo_id");
    }
    
    // ========================================================================
    public static class StateRetainer {

    	Map<String, SoftReference<Bitmap>> bitmap_cache;
    	Map<String, Photo> photo_cache;
		List<Photo> photo_list;
    	long total_result_count;
    	
//    	String list_header;

    	Bitmap dialog_bitmap;
    	int selected_thumbnail;
    	Intent prev_search_intent;
    	
    	SearchParameters globally_stored_current_search_parameters;
		Collection<Size> stored_dialog_photosizes;
		
		LargeImageGetterTaskExtended large_image_getter_task;
    }
    
    // ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {

    	StateRetainer a = new StateRetainer();

//		TextView subtitle_holder = (TextView) findViewById(R.id.photo_list_heading);
//		String subtitle_text = subtitle_holder.getText().toString();
//		a.list_header = subtitle_text;
		
		FlickrPhotoAdapter ad = (FlickrPhotoAdapter) getAdapter();
		a.bitmap_cache = ad.getCachedBitmaps();
		a.photo_cache = ad.getCachedPhotoInfo();
		a.photo_list = ad.photo_list;
		a.total_result_count = ad.total_result_count;

		a.dialog_bitmap = this.globally_stored_dialog_bitmap;
		a.selected_thumbnail = this.globally_stored_selected_thumbnail_index;
		
		a.prev_search_intent = this.previous_search_intent;
		
		a.stored_dialog_photosizes = this.globally_stored_collected_sizes;
		a.globally_stored_current_search_parameters = this.globally_stored_current_search_parameters;
		
		a.large_image_getter_task = this.large_image_getter_task;
        return a;
    }
    
    // ========================================================================
    void runInitialSearch(FlickrPhotoAdapter flickr_adapter) {
    	
        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	if (a.bitmap_cache != null && a.photo_cache != null) {

        		flickr_adapter.restoreCachedBitmaps(a.bitmap_cache);
        		flickr_adapter.restoreCachedPhotoInfo(a.photo_cache);
        		flickr_adapter.refresh_list(a.photo_list);
        	}
        	
        	this.globally_stored_dialog_bitmap = a.dialog_bitmap;
    		this.globally_stored_selected_thumbnail_index = a.selected_thumbnail;
    		this.previous_search_intent = a.prev_search_intent;
    		this.globally_stored_current_search_parameters = a.globally_stored_current_search_parameters;
    		flickr_adapter.setTotalResults(a.total_result_count);

//    		grid_title.setText( a.list_header );
    		
    		
    		this.globally_stored_collected_sizes = a.stored_dialog_photosizes;
    		
        } else {
        	
//        	initial_display_mode = getIntent().getIntExtra(PhotoListActivity.INTENT_EXTRA_PHOTOLIST_VIEW_MODE, PhotolistViewMode.LIST.ordinal());

        	
    		if ( getIntent().getBooleanExtra(INTENT_EXTRA_MY_PHOTOSTREAM_MODE, false) ) {
    			
    			this.globally_stored_current_search_page = 1;
    			
   				SearchParameters sp = searchParmsMyPhotostream(this.settings);
	   		    invokeSearch( sp, this.globally_stored_current_search_page, true );

    		} else if ( getIntent().getBooleanExtra(INTENT_EXTRA_MY_PHOTOSET_MODE, false) ) {

    			String photoset_id = getIntent().getStringExtra(PhotosetsListActivity.INTENT_EXTRA_MY_PHOTOSET_ID);
    			invokePhotosetSearch( photoset_id );
    			
    		} else if ( getIntent().getBooleanExtra(INTENT_EXTRA_MY_CONTACTS_MODE, false) ) {
    			
    			String user_nsid = getIntent().getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_USER_ID);
    			
    			SearchParameters sp = searchParmsByUser(user_nsid);
	   		    invokeSearch( sp, this.globally_stored_current_search_page, true );
    			
    		} else {
    			if ( this.settings.getBoolean("launch_with_last_search", true) ) {
        			
    				this.globally_stored_current_search_page = 1;
        			
       				SearchParameters sp = searchParmsFromPrefs(this.settings, this);
    	   		    invokeSearch( sp, this.globally_stored_current_search_page, false );
    	   		    
        		} else {
                	// Launch the search window if we are just arriving here
        			launch_search_dialog();
        		}
    		}
        }
    }
    
    // ========================================================================
 	class ImageSizesGetterExtended extends ImageSizesGetterTask {

	    protected ImageSizesGetterExtended(Context context) {
			super(context);
		}

		@Override
	    protected void onPostExecute(Collection<Size> sizes) {
	    	super.onPostExecute(sizes);

	    	globally_stored_collected_sizes = sizes;
	    	showDialog(DIALOG_FLICKR_AVAILABLE_SIZES);
	    }
	}
 	
    // ========================================================================
 	public class LargeImageGetterTaskExtended extends LargeImageGetterTask {

		LargeImageGetterTaskExtended(boolean use_medium_size, DownloadObjective download_objective) {
			super(SearchResultsViewerActivity.this, use_medium_size, download_objective);
		}
		
	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	super.onPostExecute(bitmap);

	    	globally_stored_dialog_bitmap = bitmap;
	    	
	    	if (DownloadObjective.SHOW_IN_DIALOG.equals(download_objective))
	    		showDialog(DIALOG_LARGE_PHOTO_VIEW);
	    }
	}

    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
        case DIALOG_PROGRESS:
        {
        	Log.d(TAG, "Preparing progress dialog in onPrepareDialog(" + id + ")");
        	
	        break;
        }
        default:
        	break;
        }
    }

    // ========================================================================

    protected void invokePhotosetSearch(String photoset_id) {
    	Log.d(TAG, "invokePhotosetSearch(" + photoset_id + ")");
    	
    	int current_page = globally_stored_current_search_page;
    	
    	Log.d(TAG, "Search photoset: " + photoset_id);
    	TextView grid_title = (TextView) findViewById(R.id.photo_list_heading);
    	
    	FlickrPhotoAdapter ad = (FlickrPhotoAdapter) getAdapter();

    	
    	executePhotosetSearchTask(this,
    			ad,
    			grid_title,
    			photoset_id,
    			current_page,
    			retrieval_tasks_semaphore);
    }

    // =============================================
    abstract void executePhotosetSearchTask(
    		Context context,
			RefreshablePhotoListAdapter adapter,
			TextView grid_title,
			String photoset_id,
			int current_page,
			AtomicInteger task_semaphore);
    
    
    // =============================================    
    AsyncTask<Void, List<Photo>, String> photo_search_task;
    protected void invokeSearch(SearchParameters sp, int current_page, boolean my_photostream) {
    	
    	this.globally_stored_current_search_parameters = sp;
    	
    	this.globally_stored_color_search_mode_active = false;
    	Log.d(TAG, "Invoking search...");
    	
    	TextView grid_title = (TextView) findViewById(R.id.photo_list_heading);
    	
		if (my_photostream)
			sp.setSort( SearchParameters.DATE_POSTED_DESC );
		else {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

			String default_sort_string;	// XXX One time "settings" was null.  Why was that?
			if (settings != null)
				default_sort_string = settings.getString("sort_order", Integer.toString( SearchParameters.INTERESTINGNESS_DESC ));
			else {
				default_sort_string = Integer.toString( SearchParameters.INTERESTINGNESS_DESC );
				Log.e(TAG, "SETTINGS WAS NULL!!!!!");
//				throw new NullPointerException();
			}
			
			Log.e(TAG, "default_sort_string: " + default_sort_string);
			
			sp.setSort( Integer.parseInt( default_sort_string ) );
		}


    	FlickrPhotoAdapter ad = (FlickrPhotoAdapter) getAdapter();
    	
    	executeSearchTask(
    			this,
    			ad,
    			grid_title,
    			sp,
    			current_page,
    			retrieval_tasks_semaphore);
    }

    // ========================================================================
    abstract void executeSearchTask(
    		Context context,
			RefreshablePhotoListAdapter adapter,
			TextView grid_title,
			SearchParameters task_search_params,
			int current_page,
			AtomicInteger task_semaphore);
	
    // ========================================================================
	abstract void picture_cycler(int pos); 
	
	// =============================================
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
        switch (id) {
        case DIALOG_PROGRESS:
        {
        	Log.d(TAG, "Creating progress dialog in onCreateDialog(" + id + ")");

        	prog_dialog = new ProgressDialog(this);
        	prog_dialog.setMessage("Fetching image...");
    		prog_dialog.setIndeterminate(true);
    		prog_dialog.setCancelable(false);

        	return prog_dialog;
        }
        case DIALOG_FLICKR_AVAILABLE_SIZES:
        {

        	final List<Size> sizes_array = new ArrayList<Size>();
        	if (globally_stored_collected_sizes != null)
        		sizes_array.addAll(globally_stored_collected_sizes);
        	
        	CharSequence[] items = new CharSequence[sizes_array.size()];
        	int i=0;
        	for (Size s : sizes_array) {
        		items[i++] = s.getDescription() + " ("+ s.getWidth() + "x" + s.getHeight() + ")";
        	}

        	return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Available sizes:")
            .setItems(items, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					
					Size size = sizes_array.get(which);
						
					String size_abbreviation = size.getDescription();
					String suggested_filename = Long.toString(globally_stored_selected_photo_id) + size_abbreviation;
					String download_url = size.getSource();
					if (globally_stored_selecting_for_content_uri_flag) {
						
						finishWithContentUri(download_url);
						
					} else if (downloading_image_only) {
						downloading_image_only = false; // Reset for next use
						
						get_download_path(suggested_filename, download_url);
						
					} else {
						shareSavedImage(suggested_filename, download_url);
					}

					removeDialog(DIALOG_FLICKR_AVAILABLE_SIZES);
				}
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	removeDialog(DIALOG_FLICKR_AVAILABLE_SIZES);
                }
            })
            .setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					removeDialog(DIALOG_FLICKR_AVAILABLE_SIZES);
				}
            	
            })
            .create();
        }
        }
        return null;
    }


    // ============================================================
    void makeFlickrFavorite(Photo photo) {
    	
    	Auth auth;
    	Flickr flickr = null;
    	FavoritesInterface fav_interface = null;
    	
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		fav_interface = flickr.getFavoritesInterface();
		
		auth = new Auth();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
		auth.setPermission(Permission.WRITE);
		
		RequestContext requestContext = RequestContext.getRequestContext();
		requestContext.setAuth(auth);
		
		
		try {
			fav_interface.add( photo.getId() );
			Toast.makeText(this, "Added to favorites.", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
			e.printStackTrace();
		}
    }

    // ============================================================

    
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_photo_list, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Photo action:");
        
        
        

        Photo p = (Photo) getAdapter().getItem( ((AdapterView.AdapterContextMenuInfo) menuInfo).position );
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String logged_in_user_id = settings.getString("PREFKEY_FLICKR_USER_NSID", null);
		String owner_user_id = p.getOwner().getId();
        boolean vis = owner_user_id.equals(logged_in_user_id);
        
//        Log.e(TAG, "Logged-in user: " + logged_in_user_id);
//        Log.e(TAG, "Photo owner: " + owner_user_id);

        menu.findItem(R.id.menu_add_to_photoset).setVisible(vis);
        
        


        menu.findItem(R.id.menu_remove_from_photoset).setVisible(
        		getIntent().getBooleanExtra(INTENT_EXTRA_MY_PHOTOSET_MODE, false)
        );
	}
    
    // ============================================================
    
    void launchBuiltinImageViewerZoom(Photo photo) {
    	
    	String filename = "my_temp.jpg";
    	try {
			FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);

	    	// Write to file here.
			String thumbnail_url = photo.getSmallSquareUrl().toString();
			Bitmap bitmap = (Bitmap) ((FlickrPhotoDualAdapter) getAdapter()).photo_populator.bitmapReferenceMap.get(thumbnail_url).get();
			bitmap.compress(CompressFormat.JPEG, 80, fos);

			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	File mypath = new File(getFilesDir(), filename);
    	String input_abolute_path = mypath.getAbsolutePath();
    	Uri x = LocalFileContentProvider.constructUri(input_abolute_path);
		Intent i = new Intent(Intent.ACTION_VIEW, x);

    	startActivity(i);
    }
    
    // ============================================================
    
    void launchBuiltinBrowserZoom(Photo photo) {

    	// NOTE: This will launch the Browser:
    	Log.d(TAG, "Launching built-in zoomer");
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse( photo.getMediumUrl() ));
    	startActivity(i);
    }
    
    // ============================================================
    public static void launchCustomZoom(Activity context, Photo photo, Bitmap bitmap) {
    	
    	Log.d(TAG, "Launching home-made zoomer");
    	
		ApplicationState my_app = (ApplicationState) context.getApplication();
		my_app.active_thumbnail_bitmap = bitmap;
		my_app.active_flickr_photo = photo;
		
		Intent i = new Intent();
		i.setClass(context, ImageZoomActivity.class);
		context.startActivity(i);
    }

	// ===============================================
/*
 	private class WallpaperGetterTask extends LargeImageGetterTask {
		
	    WallpaperGetterTask(Context context, boolean use_medium_size) {
			super(context, use_medium_size);
		}

		@Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	wait_dialog.dismiss();

	    	Log.d(TAG, "Image fetched.  Now assign wallpaper.");
	    	
			new SetWallpaperTask().execute(bitmap);
	    }
	}
*/

    // ========================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.menu_save_search).setVisible(previous_search_intent != null && !globally_stored_color_search_mode_active);

        
        return true;
    }
    
    // ========================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case R.id.menu_search:
        {	
        	launch_search_dialog();
	    	
            return true;
        }
        case R.id.menu_save_search:
        {	
    		DatabaseSearchHistory search_database = new DatabaseSearchHistory(this);
    		long total_results = ((FlickrPhotoAdapter) getAdapter()).total_result_count;
    		search_database.save_search(previous_search_intent, total_results);
	    	
			Toast.makeText(this, "Search saved.", Toast.LENGTH_SHORT).show();

            return true;
        }
        }
        return super.onOptionsItemSelected(item);
    }

    // ========================================================
    void launch_search_dialog() {
    	Intent i = new Intent();
    	i.setClass(getBaseContext(), TabbedSearchActivity.class);
    	startActivityForResult(i, REQUEST_CODE_SEARCH_BUILDER);
    }

    // ========================================================
    void fetch_color_images(int[] int_colors) {
    	
    	String[] hex_colors = new String[int_colors.length];
    	for (int i=0; i<int_colors.length; i++)
    		hex_colors[i] = Integer.toHexString(int_colors[i]).substring(2);
    	
    	fetch_images_with_colors( hex_colors );
	};

    // ========================================================
    private void fetch_images_with_colors(String[] hex_colors) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		int photos_per_page = settings.getInt("photos_per_page", Market.DEFAULT_PHOTOS_PER_PAGE);

		globally_stored_color_search_mode_active = true;
		
		((FlickrPhotoAdapter) getAdapter()).clear_list();
		
    	ColorMatcherPhotoIdRetrievalTask task = new ColorMatcherPhotoIdRetrievalTask(this, hex_colors, photos_per_page, globally_stored_current_search_page);
    	task.execute();
    }

    // ========================================================
    public static SearchParameters searchParmsByUser(String user_id) {
    	
		SearchParameters sp = new SearchParameters();
//		sp.setUserId( "me" );
		sp.setUserId( user_id );
		sp.setForceAuthentication(true);
		return sp;
    }

    // ========================================================
    public static SearchParameters searchParmsMyPhotostream(SharedPreferences settings) {
    	
		String user_id = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_USER_NSID, null);
		return searchParmsByUser(user_id);
    }
    
    // ========================================================
    public static SearchParameters searchParmsFromPrefs(SharedPreferences settings, Context context) {
    	
		String group_id = settings.getString(TabbedSearchActivity.PREFKEY_FLICKR_SEARCH_GROUP_ID, null);
		String user_id = settings.getString(TabbedSearchActivity.PREFKEY_FLICKR_SEARCH_USER_ID, null);
		String search_text = settings.getString(TabbedSearchActivity.PREFKEY_FLICKR_SEARCH_TEXT, null);
		boolean machine_tag_all_mode = settings.getBoolean("match_all_tags", false);
		
		List<String> standard_tags = new ArrayList<String>();
		List<String> machine_tags = new ArrayList<String>();
		
		
		// Add last used tags from database:
    	DatabaseSearchHistory helper = new DatabaseSearchHistory( context );
	    SQLiteDatabase db = helper.getReadableDatabase();
    	
    	long search_id = -1;
    	
	    Cursor c = db.query(DatabaseSearchHistory.TABLE_STANDARD_TAGS,
	    		new String[] {DatabaseSearchHistory.KEY_VALUE},
	    		DatabaseSearchHistory.KEY_SEARCH_TAGSET + "=?",
	    		new String[] {Long.toString(search_id)},
	    		null, null, null);
        while (c.moveToNext()) {
        	/*
   	    	Tag t = new Tag();
   	    	t.setRaw(c.getString(0));
   	    	standard_tags.add( t.getRaw() );
   	    	*/
        	
        	standard_tags.add( c.getString(0) );
        }
        c.close();
        
	    c = db.query(DatabaseSearchHistory.TABLE_MACHINE_TAG_TRIPLES,
	    		new String[] {
		    		DatabaseSearchHistory.KEY_NAMESPACE,
		    		DatabaseSearchHistory.KEY_PREDICATE,
	    			DatabaseSearchHistory.KEY_VALUE},
	    		DatabaseSearchHistory.KEY_SEARCH_TAGSET + "=?",
	    		new String[] {Long.toString(search_id)},
	    		null, null, null);
        while (c.moveToNext()) {
   	    	MachineTag mt = new MachineTag(c.getString(0), c.getString(1), c.getString(2));
   	    	machine_tags.add( mt.toString() );
        }
        c.close();
        
        db.close();
		

		SearchParameters sp = buildSearchParms(
				user_id,
				group_id,
				search_text,
				standard_tags,
				machine_tags,
				machine_tag_all_mode);
		
		return sp;
    }
    
    // ========================================================
    public static SearchParameters searchParmsFromIntent(Intent search_intent) {
    	
			String search_text = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SEARCH_TEXT);
   			List<String> standard_tags = search_intent.getStringArrayListExtra(TabbedSearchActivity.INTENT_EXTRA_STANDARD_TAGS);
   			boolean machine_tag_all_mode = search_intent.getBooleanExtra(TabbedSearchActivity.INTENT_EXTRA_MACHINE_TAG_ALL_MODE, true);
   			List<String> machine_tags = search_intent.getStringArrayListExtra(TabbedSearchActivity.INTENT_EXTRA_MACHINE_TAGS);

   			String user_id = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_USER_ID);
   			String group_id = search_intent.getStringExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_GROUP_ID);
   			
   				
   			
   			// DEBUG
   			Log.d(TAG, "Tag search list: " + standard_tags.size());
   			for (String tag_raw : standard_tags)
   				Log.i(TAG, tag_raw);
   			
   			// Build search params, then execute search here!
   			SearchParameters sp = buildSearchParms(
   					user_id,
   					group_id,
   					search_text,
   					standard_tags,
   					machine_tags,
   					machine_tag_all_mode);

   			return sp;
    }

    // ========================================================
    public static SearchParameters buildSearchParms(
    		String user_id,
    		String group_id,
    		String search_text,
    		List<String> standard_tags,
    		List<String> machine_tag_strings,
    		boolean machine_tag_all_mode) {
    	
    	
		SearchParameters sp = new SearchParameters();
		if (search_text != null && search_text.length() > 0)
			sp.setText( search_text );
		
	
		if (user_id != null && user_id.length() > 0)
			sp.setUserId(user_id);
		if (group_id != null && group_id.length() > 0)
			sp.setGroupId(group_id);
	
	
		if (standard_tags.size() > 0) {
   			String[] standard_tag_array = new String[standard_tags.size()];
   			for (int i=0; i<standard_tag_array.length; i++)
   				standard_tag_array[i] = standard_tags.get(i);

   			sp.setTagMode(machine_tag_all_mode ? "all" : "any");	// TODO
   			sp.setTags( standard_tag_array );
		}
		

		if (machine_tag_strings.size() > 0) {
   			String[] machine_tag_array = new String[machine_tag_strings.size()];
   			for (int i=0; i<machine_tag_array.length; i++) {
   				// XXX Important: Only here do we construct the machine tag query
   				// after reconstructing the machine tag.
   				
   				MachineTag mt = new MachineTag(machine_tag_strings.get(i));
   				machine_tag_array[i] = mt.getQueryString();
   			}

   			sp.setMachineTagMode(machine_tag_all_mode ? "all" : "any");
   			
   			sp.setMachineTags( machine_tag_array );
		}
		
		
		if (sp.getMachineTags() != null) {

			Log.i(TAG, "Machine tags for search:");
			for (String machine_tag : sp.getMachineTags())
				Log.d(TAG, "Machine Tag: " + machine_tag);
		}
		
		return sp;
    }
    	
	// =======================================================================
	// =======================================================================
	// =======================================================================
	
	
	
    //
    // MediaScanner-related code
    //

    private String mSavedImageFilename;

    Handler mHandler = new Handler();

    
    /**
     * android.media.MediaScannerConnection.MediaScannerConnectionClient implementation.
     */
    private MediaScannerConnection.MediaScannerConnectionClient mMediaScanConnClient =
        new MediaScannerConnection.MediaScannerConnectionClient() {
            /**
             * Called when a connection to the MediaScanner service has been established.
             */
    		@Override
            public void onMediaScannerConnected() {
                Log.d(TAG, "I just connected, so says the MediaScannerConnectionClient callback...");
                // The next step happens in the UI thread:
                mHandler.post(new Runnable() {
                        public void run() {
                        	SearchResultsViewerActivity.this.onMediaScannerConnected();
                        }
                    });
            }

            /**
             * Called when the media scanner has finished scanning a file.
             * @param path the path to the file that has been scanned.
             * @param uri the Uri for the file if the scanning operation succeeded
             *        and the file was added to the media database, or null if scanning failed.
             */
    		@Override
            public void onScanCompleted(final String path, final Uri uri) {
                Log.i(TAG, "MediaScannerConnectionClient.onScanCompleted: path "
                      + path + ", uri " + uri);
                // Just run the "real" onScanCompleted() method in the UI thread:
                mHandler.post(new Runnable() {
                        public void run() {
                        	SearchResultsViewerActivity.this.onScanCompleted(path, uri);
                        }
                    });
            }
    };
    

        
    // =============================================

 	private class GenericBitmapDownloadTaskExtended extends GenericBitmapDownloadTask {

 		String filename;
	    protected GenericBitmapDownloadTaskExtended(Context context, String filename) {
			super(context);
			this.filename = filename;
		}

		@Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	super.onPostExecute(bitmap);
	    	
	    	if (bitmap != null)
	    		save_file_to_disk(bitmap, filename);
	    	else
	            Toast.makeText(SearchResultsViewerActivity.this, "Could not download image!", Toast.LENGTH_SHORT).show();
	    }
	}
        
    // =============================================
 	
 	void save_file_to_disk(Bitmap bitmap, String filename) {
 		
    	File root = Environment.getExternalStorageDirectory();
    	File crittr_directory = new File(root, "flickr");
    	File xml_file = new File(crittr_directory, filename + ".jpg");
    	crittr_directory.mkdirs();
	    FileOutputStream fos;
		try {
			fos = new FileOutputStream(xml_file);
		    bitmap.compress(CompressFormat.JPEG, 80, fos);
	        
		    fos.flush();	           
		    fos.close();	
			
			xml_file.createNewFile();
			
	    	mSavedImageFilename = xml_file.getAbsolutePath();

	        mMediaScannerConnection.connect();

	    	Log.d(TAG, "We just called the 'connect()' method of the MediaScannerConnection.");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
 	}
    // =============================================
 	
 	private void get_download_path(String filename, String photo_url) {

 		pending_download_url = photo_url;
 		pending_suggested_filename = filename;
 		
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_PICK);
		Uri startDir = Uri.fromFile(new File("/sdcard"));
		// Files and directories
//		intent.setType("vnd.android.cursor.dir/lysesoft.andexplorer.directory");
		intent.setDataAndType(startDir, "vnd.android.cursor.dir/lysesoft.andexplorer.directory");
//		intent.setDataAndType(startDir, "vnd.android.cursor.dir/lysesoft.andexplorer.file");
		// Optional filtering on file extension.
//		intent.putExtra("browser_filter_extension_whitelist", "*.txt,*.mp3");
		// Title
		intent.putExtra("explorer_title", "Select a folder:");
		
		Market.intentLaunchMarketFallback(this, Market.FILE_NAVIGATOR_PACKAGE_SEARCH, intent, REQUEST_CODE_DIRECTORY_CHOOSER);
//		startActivityForResult(intent, RETURN_CODE_DIRECTORY_CHOOSER);
 	}

    // =============================================
 	
//    private static final String SAVED_IMAGE_MIME_TYPE = "image/png";
    private static final String SAVED_IMAGE_MIME_TYPE = "image/jpg";
    private MediaScannerConnection mMediaScannerConnection;

    private void shareSavedImage(String filename, String photo_url) {

    	// We must save the file to the SD card before scanning.
    	
		String storage_state = Environment.getExternalStorageState();
		if (!storage_state.contains("mounted")) {

            Toast.makeText(this, "SD Card not mounted!", Toast.LENGTH_SHORT).show();
			return;
		}

		new GenericBitmapDownloadTaskExtended(this, filename).execute(photo_url);
    }

    
    /**
     * This method is called when our MediaScannerConnection successfully
     * connects to the MediaScanner service.  At that point we fire off a
     * request to scan the lolcat image we just saved.
     *
     * This needs to run in the UI thread, so it's called from
     * mMediaScanConnClient's onMediaScannerConnected() method via our Handler.
     */
    private void onMediaScannerConnected() {

        Log.d(TAG, "As I said before, we've connected.  Now starting to scan file...");

        // Update the message in the progress dialog...
//        mSaveProgressDialog.setMessage(getResources().getString(R.string.lolcat_scanning));

        
        // Fire off a request to the MediaScanner service to scan this
        // file; we'll get notified when the scan completes.
        Log.d(TAG, "- Requesting scan for file: " + mSavedImageFilename);
        mMediaScannerConnection.scanFile(mSavedImageFilename,
                                         null /* mimeType */);


        // Next step: mMediaScanConnClient will get an onScanCompleted() callback,
        // which calls our own onScanCompleted() method via our Handler.
    }
    
    
    
    
    /**
     * Updates the UI after the media scanner finishes the scanFile()
     * request we issued from onMediaScannerConnected().
     *
     * This needs to run in the UI thread, so it's called from
     * mMediaScanConnClient's onScanCompleted() method via our Handler.
     */
    private void onScanCompleted(String path, final Uri uri) {
        Log.d(TAG, "onScanCompleted: path " + path + ", uri " + uri);
        mMediaScannerConnection.disconnect();

        if (uri == null) {
            Log.w(TAG, "onScanCompleted: scan failed.");
//            onSaveFailed(R.string.lolcat_scan_failed);
            return;
        }

        // Success!
        
        if (sharing_saved_image)
        	shareSavedImage(uri);
        else {
	//		Intent intent = new Intent(Contacts.Intents.ATTACH_IMAGE);
			Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);	// From AttachImage.java in Contacts app
			intent.setData(uri);
			startActivity(intent);
        }
    }
    
    // ===================================

    private void shareSavedImage(Uri uri) {
        Log.i(TAG, "shareSavedImage(" + uri + ")...");

        if (uri == null) {
            Log.w(TAG, "shareSavedImage: null uri!");
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(SAVED_IMAGE_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            startActivity(
                    Intent.createChooser(
                            intent,
                            "Share picture via"));
        } catch (android.content.ActivityNotFoundException ex) {
            Log.w(TAG, "shareSavedImage: startActivity failed", ex);
            Toast.makeText(this, "Sharing failed", Toast.LENGTH_SHORT).show();
        }
    }

    // =============================================
    void finishWithContentUri(String download_url) {

    	// Clear the flag.
    	globally_stored_selecting_for_content_uri_flag = false;
    	
		Intent result_intent = new Intent();
		Uri uri = ExperimentalFileContentProvider.constructDownloaderUri(globally_stored_selected_photo_id, download_url);
		Log.i(TAG, "Constructed URI as: " + uri);
		result_intent.setData( uri );
    	setResult(Activity.RESULT_OK, result_intent);
    	finish();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    

    // ========================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
	   		case REQUEST_CODE_DIRECTORY_CHOOSER:
	   		{
	   			if (data != null) {
		   			Uri selected_file_uri = data.getData();
	   				
	   				if (selected_file_uri != null) {
	   					
	   					Uri extended_uri = selected_file_uri.buildUpon().appendEncodedPath(pending_suggested_filename + ".jpg").build();
//	   					File f = new File();
	   					String extended_path = extended_uri.getPath();
	   					
	   		   			Log.d(TAG, "Selected path: " + extended_path);
	   		   			Log.d(TAG, "Download URL: " + pending_download_url);
	   		   			
	   		   			new GenericFileDownloadTask(this, pending_download_url, extended_path).execute();
	   					
	   					return;
	   				}
	   			}


	            Toast.makeText(this, "No folder selected.", Toast.LENGTH_SHORT).show();
	   			
	   			break;
	   		}
	   		case REQUEST_CODE_SEARCH_BUILDER:
	   		{

   				previous_search_intent = data;
	   			
   				getIntent().putExtra(INTENT_EXTRA_MY_PHOTOSTREAM_MODE, false);
	   			
	   			int[] int_colors = data.getIntArrayExtra(TabbedSearchActivity.INTENT_EXTRA_COLOR_LIST);
	   			if (int_colors != null) {
	   				fetch_color_images(int_colors);
	   				
	   			} else {
	    			globally_stored_current_search_page = 1;
	   				SearchParameters sp = searchParmsFromIntent(data);
		   		    invokeSearch( sp, globally_stored_current_search_page, false );
	   			}
	   			break;
	   		}
	   		case REQUEST_CODE_TAGGING_ACTIVITY_REFRESH:
	   			
	   			boolean need_refresh = data.getBooleanExtra(ListActivityPhotoTags.INTENT_EXTRA_REFRESH_NEEDED, false);
	   			
	   			// FIXME: For now we're going to ignore this, because it's not working right.

	   			
	   			break;

	   		default:
		    	break;
		   }
		}
    }

    // ========================================================================
	@Override
	public void showProgressDialog() {
		showDialog(DIALOG_PROGRESS);
	}

	@Override
	public ProgressDialog getProgressDialog() {
		return prog_dialog;
	}

	@Override
	public void dismissProgressDialog() {
		dismissDialog(DIALOG_PROGRESS);
	}
	
    // ========================================================================
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w(TAG, "onDestroy()");
		
		if (photo_search_task != null)
			photo_search_task.cancel(true);
    }
}
