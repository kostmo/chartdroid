package org.crittr.browse.activity;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.containers.IntentDependentRunnable;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.containers.ViewHolderTaxon;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.utilities.AsyncTaxonInfoPopulator;
import org.crittr.shared.browser.utilities.FlickrPhotoDrawableManager;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;
import org.crittr.task.RetrievalTaskDomainMembers;
import org.crittr.task.RetrievalTaskTaxonMembers;
import org.crittr.task.TaxonSearchTask;
import org.crittr.task.WikiBlurbGetterTask;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;


public class TaxonNavigatorLinear extends TaxonNavigatorAbstract implements OnItemClickListener {

    
    static final String INTENT_EXTRA_CURRENT_HIERARCHY = "INTENT_EXTRA_CURRENT_HIERARCHY";

	
	
    private long current_tsn = Constants.INVALID_TSN;
    TaxonInfo parent_taxon = new TaxonInfo();

	
	List<String> hierarchy_key;

	
	
	
    // ========================================================================
	public class TaxonLauncherRunnable extends IntentDependentRunnable {

		TaxonLauncherRunnable(Intent i) {
			super(i);
			enable_dialog = true;
		}
		public void run() {
			
//			Log.e(TAG, "TSN passed via intent: " + getIntent().getLongExtra(ListActivityTaxonNavigator.INTENT_EXTRA_TSN, -2));
			initialize_with_intent( getIntent() );
		}
	}



	
    // Re-implemented ListActivity methods:
    // ========================================================================
    ListView getListView() {
    	return (ListView) findViewById(android.R.id.list);
    }

    // ========================================================================  
    public ListAdapter getListAdapter() {
    	return getListView().getAdapter();
    }

    // ========================================================================
    void setListAdapter(ListAdapter adapter) {
    	getListView().setAdapter(adapter);
    }
	
    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.list_activity_taxons);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);
        
        
        View chooser_button = findViewById(R.id.button_choose_taxon);
        	
    	boolean picking = Intent.ACTION_PICK.equals( getIntent().getAction() );
    	chooser_button.setVisibility(
    			picking && Market.isPackageInstalled(this, Market.FULL_VERSION_PACKAGE)
    			? View.VISIBLE : View.GONE );
    
        chooser_button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Intent i = new Intent();
	   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, getCurrentTSN());
		    	setResult(Activity.RESULT_OK, i);
		    	finish();
			}
        });

        findViewById(R.id.button_view_images).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Intent i = new Intent();
		    	i.setClass(TaxonNavigatorLinear.this, TabActivityTaxonExtendedInfo.class);
	   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, getCurrentTSN());
		    	startActivity(i);
			}
        });
        

        View empty_area = findViewById(android.R.id.empty);
        getListView().setEmptyView(empty_area);
        getListView().setOnItemClickListener(this);
        /*
        Drawable empty_search_drawable = new NonScalingBackgroundDrawable(this, getListView(), -1);
    	empty_search_drawable.setAlpha(0x20);	// mostly transparent
    	empty_area.setBackgroundDrawable(empty_search_drawable);
        */
        
    	ListAdapterTaxons list_adapter = new ListAdapterTaxons(this, false);
    	
        // Deal with orientation change
        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	list_adapter.taxon_list = a.taxon_list;
        	
        	list_adapter.drawable_manager.drawableMap = a.drawableMap;
        	list_adapter.drawable_manager.taxon_to_thumbnail_url_map = a.taxon_to_thumbnail_url_map;
        	
        } else {
        	
        }
    	setListAdapter( list_adapter );
    	
    	

		registerForContextMenu(getListView());
		
    	initialize_with_intent( getIntent() );
    	
    	
    	if (Intent.ACTION_PICK.equals( getIntent().getAction() )) {

			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			if (!settings.getBoolean(PREFKEY_SHOW_BACK_BUTTON_INSTRUCTIONS, false)) {
				showDialog(DIALOG_BACK_KEY_INSTRUCTIONS);
			} else {
				
				Toast.makeText(this, "Choose a taxon...", Toast.LENGTH_LONG).show();
			}
    	}
    }

    // ========================================================================
    public static class StateRetainer {
    	List<TaxonInfo> taxon_list;
    	Map<String, SoftReference<Bitmap>> drawableMap;
    	Map<String, List<String>> taxon_to_thumbnail_url_map;
    	
    	TaxonSearchTask taxon_search_task;
    }

    // ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	getIntent().putExtra(Constants.INTENT_EXTRA_TSN, getCurrentTSN());
    	
    	ListAdapterTaxons list_adapter = (ListAdapterTaxons) getListAdapter();
    	
    	StateRetainer sr = new StateRetainer();
    	sr.taxon_list = list_adapter.taxon_list;

    	sr.drawableMap = list_adapter.drawable_manager.drawableMap;
    	sr.taxon_to_thumbnail_url_map = list_adapter.drawable_manager.taxon_to_thumbnail_url_map;
    	
        return sr;
    }


    // ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	Log.i(TAG, "onSaveInstanceState");

    	out_bundle.putString("disabled_function_description", globally_stored_disabled_function_description);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	Log.i(TAG, "onRestoreInstanceState");

    	globally_stored_disabled_function_description = in_bundle.getString("disabled_function_description");
    }



    // ========================================================================
    // TODO / FIXME - Android 2.0
    /*
    @Override
    public void onBackPressed() {
    	
    	if (getCurrentTSN() > 0) {
    		
        	// Gets parent TSN, creates new intent, and pops current Taxon from the stack.
        	final Intent i = new Intent();
        	i.setAction( getIntent().getAction() );
        	i.addCategory(Constants.CATEGORY_TAXON);

        	
        	
        	ArrayList<String> new_hierarchy_key = new ArrayList<String>();
        	if (parent_taxon.tsn != Constants.INVALID_TSN) {
	        	new_hierarchy_key.addAll( hierarchy_key );
	        	try {
	        		new_hierarchy_key.remove( new_hierarchy_key.size() - 1 );	// "Pop"
	        	} catch (IndexOutOfBoundsException e) {
	        		Log.e(TAG, "Tried to pop from an empty heirarchy stack...");
	        	}
        	}
        	i.putExtra(INTENT_EXTRA_CURRENT_HIERARCHY, new_hierarchy_key);
        	i.putExtra(Constants.INTENT_EXTRA_RANK_NAME, parent_taxon.rank_name);
        	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, parent_taxon.taxon_name);


//        	Log.e(TAG, "Current Constants.TSN before parent fetcher: " + getCurrentTSN());
	    	((ListAdapterTaxons) getListAdapter()).taxon_populator.fetchParentTSNOnThread( getCurrentTSN(), new TaxonLauncherRunnable(i));
	    	
    	}
    }
    */

    // ========================================================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

    	// Note: this takes care of both DatabasePersonalTaxonomy.INVALID_TSN
    	// and the case where the parent tsn is valid (0).
    	boolean can_move_up = getCurrentTSN() > 0;
//    	boolean can_move_up = getCurrentTSN() != DatabasePersonalTaxonomy.INVALID_TSN;

        if ((keyCode == KeyEvent.KEYCODE_BACK) && can_move_up) {
        	
        	// Gets parent TSN, creates new intent, and pops current Taxon from the stack.
        	final Intent i = new Intent();
        	i.setAction( getIntent().getAction() );
        	i.addCategory(Constants.CATEGORY_TAXON);

        	
        	
        	ArrayList<String> new_hierarchy_key = new ArrayList<String>();
        	if (parent_taxon.tsn != Constants.INVALID_TSN) {
	        	new_hierarchy_key.addAll( hierarchy_key );
	        	try {
	        		new_hierarchy_key.remove( new_hierarchy_key.size() - 1 );	// "Pop"
	        	} catch (IndexOutOfBoundsException e) {
	        		Log.e(TAG, "Tried to pop from an empty heirarchy stack...");
	        	}
        	}
        	i.putExtra(INTENT_EXTRA_CURRENT_HIERARCHY, new_hierarchy_key);
        	i.putExtra(Constants.INTENT_EXTRA_RANK_NAME, parent_taxon.rank_name);
        	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, parent_taxon.taxon_name);


//        	Log.e(TAG, "Current Constants.TSN before parent fetcher: " + getCurrentTSN());
	    	((ListAdapterTaxons) getListAdapter()).taxon_populator.fetchParentTSNOnThread( getCurrentTSN(), new TaxonLauncherRunnable(i));
	    	
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ========================================================================
    @Override protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	
    	Log.d(TAG,"New intent received.");
    	initialize_with_intent( intent );
    }

    // ========================================================================
    @Override
	protected void onStart() {
    	super.onStart();
        
    	Log.d(TAG,"Start was called.");
    }
    

    // ========================================================================
    void initialize_with_intent(Intent activity_intent) {

        hierarchy_key = activity_intent.getStringArrayListExtra(INTENT_EXTRA_CURRENT_HIERARCHY);
        if (hierarchy_key == null) {
        	
        	// Later, we fetch from local DB or Network
        	hierarchy_key = new ArrayList<String>();
        }
        
        
        current_tsn = activity_intent.getLongExtra(Constants.INTENT_EXTRA_TSN, Constants.INVALID_TSN);
        
//        Log.e(TAG, "TSN after being received in initialize_with_intent(): " + getCurrentTSN());
        
        if (getCurrentTSN() == Constants.INVALID_TSN) {
        	// Check to see if the TSN was specified in the URI instead.
        	Uri data = activity_intent.getData(); 
        	if (data != null) {
        		String host = data.getHost();
        		if (host != null) {
        			current_tsn = Long.parseLong(host);
        		}
        	}
        }

        // NOTE: THE VARIABLE current_tsn IS ONLY VALID BELOW THIS POINT
//        Log.d(TAG, "Current TSN: " + getCurrentTSN());
  
        

        ((ListAdapterTaxons) getListAdapter()).clear_list();
        
        

        if (getCurrentTSN() > 0) {
        	
        	/*
        	
        // ==== SLIDING DRAWER STUFF ====
        	final ImageView drawer_handle = (ImageView) findViewById(R.id.handle);
        	final ImageView handle_bar_chunk = (ImageView) findViewById(R.id.handle_bar_chunk); 
        	final SlidingDrawer sliding_drawer = (SlidingDrawer) findViewById(R.id.sliding_drawer);
        	final Drawable normal_handle = getResources().getDrawable(R.drawable.top_chunk);
        	normal_handle.setAlpha(0x80); // translucent
        	drawer_handle.setBackgroundDrawable(normal_handle);
        	
        	final Drawable bar_piece = getResources().getDrawable(R.drawable.tray_base);
        	bar_piece.setAlpha(0x80); // translucent
        	handle_bar_chunk.setBackgroundDrawable(bar_piece);

        	final Drawable pressed_handle = getResources().getDrawable(R.drawable.tray_handle_pressed);
        	pressed_handle.setAlpha(0xB0); // translucent
        	
        	
        	sliding_drawer.setVisibility(View.VISIBLE);

        	
        	// NOTE: Setting the DrawerScrollListener messes up the sliding animation!!!

        	drawer_handle.setOnTouchListener(new OnTouchListener() {

				public boolean onTouch(View v, MotionEvent event) {
					
					
					switch ( event.getAction() ) {
					case MotionEvent.ACTION_DOWN:
						Log.d(TAG, "Caught mouse down.");
						drawer_handle.setBackgroundDrawable(pressed_handle);
						break;
					case MotionEvent.ACTION_UP:
//						drawer_handle.setBackgroundResource(R.drawable.tray_handle_normal);
						drawer_handle.setBackgroundDrawable(normal_handle);
//						sliding_drawer.animateToggle();
						break;
					default:
						break;
					}

					// NOTE: We should disable dragging, because the sliding animation is all screwed up.

					// Returning true here will intercept all clicks for the other views, too?
					return false;
				}
        	});
        	
*/

        	((TextView) findViewById(R.id.rank_name_subheading)).setVisibility(View.VISIBLE);
        	((TextView) findViewById(R.id.header_title_taxon_name)).setText( activity_intent.getStringExtra(Constants.INTENT_EXTRA_TAXON_NAME) );
        	
        	
	        // =============== BEGIN HEADER TAXON INFO POPULATION ===============

	        // We re-fetch the rank of the current Taxon, in case it hadn't been retrieved in the previous activity.
	        ViewHolderTaxon holder = new ViewHolderTaxon();
//	        holder.full_view = (LinearLayout) findViewById(R.id.taxon_header_view);
            holder.taxon_name_textview = (TextView) findViewById(R.id.header_title_taxon_name);
            holder.taxon_rank_textview = (TextView) findViewById(R.id.rank_name_subheading);

            
//            holder.vernacular_name_textview = (TextView) findViewById(R.id.taxon_vernacular_name);


//            ((TextView) findViewById(R.id.rank_name_subheading)).setText( "-" );
            AsyncTaxonInfoPopulator taxon_populator = new AsyncTaxonInfoPopulator(this);
            FlickrPhotoDrawableManager image_populator = new FlickrPhotoDrawableManager(this);
            
        	TaxonInfo ti = new TaxonInfo();
        	ti.tsn = getCurrentTSN();
            taxon_populator.fetchTaxonRankOnThread( ti, holder );
            
            
            taxon_populator.fetchTaxonNameOnThread( getCurrentTSN(), holder, image_populator, -1, false );

            // =============== END HEADER TAXON INFO POPULATION ===============
        	
            TextView hierarchy_key_textview = (TextView) findViewById(R.id.hierarchy_key);
            taxon_populator.fetchHierarchyOnThread( getCurrentTSN(), hierarchy_key_textview );

        	
        	
            /*
            holder.taxon_name_textview.setText( ti.taxon_name );
            holder.tsn_textview.setText( Long.toString( getItemTSN(position) ) );

            if (ti.vernacular_name == null || ti.vernacular_name.length() == 0)
            	taxon_populator.fetchTaxonVernacularOnThread( getItemTSN(position), holder );
            else
            	holder.vernacular_name_textview.setText(ti.vernacular_name);
            */

            

        	((TextView) findViewById(R.id.rank_member_count)).setText( "-" );
	        new RetrievalTaskTaxonMembers( getCurrentTSN(), this ).execute();

        }
        else {
        	hierarchy_key = new ArrayList<String>();
        	Log.i(TAG, "Launching kingdoms!");
        	
        	((TextView) findViewById(R.id.hierarchy_key)).setVisibility(View.GONE);
        	((TextView) findViewById(R.id.header_title_taxon_name)).setText( "Eukaryota" );
        	((TextView) findViewById(R.id.rank_name_subheading)).setText( "Domain" );
//        	((TextView) findViewById(R.id.rank_name_subheading)).setVisibility(View.GONE);
        	
        	
        	new RetrievalTaskDomainMembers( getCurrentTSN(), this ).execute();
        }
    }
    
    // ========================================================================
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		TaxonInfo taxon = ((ListAdapterTaxons) getListView().getAdapter()).taxon_list.get(info.position);
//		String taxon_name = getSelectedTaxonName(info.position);
		long tsn = info.id;

		switch (item.getItemId()) {
		case R.id.menu_info_blurb:
		{
			new WikiBlurbGetterTask(this, taxon, tsn).execute();
	    	break;
		}
		case R.id.menu_wikipedia:
		{
			String taxon_name = TabActivityTaxonExtendedInfo.get_taxon_name_now(this, taxon, tsn);

	        String wikipedia_url = WikiBlurbGetterTask.WIKIPEDIA_MOBILE_BASE + taxon_name;
	    	Uri destination = Uri.parse( wikipedia_url );
	    	// Launches the standard browser.
	    	startActivity(new Intent(Intent.ACTION_VIEW, destination));
	    	break;
		}
		case R.id.menu_prefer_thumbnail:
		{
			
			// Prefer globally
			// TODO: Implement me
//			initiate_appengine_tsn_popularity_update(tsn);
			
			
			// Prefer locally
			DatabaseTaxonomy helper = new DatabaseTaxonomy(this);
			
			String current_thumbnail_url = ((ListAdapterTaxons) getListView().getAdapter()).getCurrentThumbnailURL(info.position);
//			Log.d(TAG, "Current thumbnail URL: " + current_thumbnail_url);
			
			if (current_thumbnail_url != null)
				helper.markPreferredThumbnail(tsn, current_thumbnail_url);
			
			break;
		}
		case R.id.menu_taxon_accept:
		{

//			String taxon_name = getSelectedTaxonName(info.position);
			
			Intent i = new Intent();
   	    	
   	    	// Note: the full hierarchy can be recovered by API methods.
   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, tsn);
   	    	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, taxon.taxon_name);
   	    	
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
	    	break;
		}
		case R.id.menu_taxon_log_sighting:
		{

			String taxon_name = TabActivityTaxonExtendedInfo.get_taxon_name_now(this, taxon, tsn);
			
			
   	    	Intent i = new Intent();

   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, tsn);
   	    	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, taxon_name);
   	    	String mime_type = "vnd.android.cursor.item/vnd.org.crittr.sighting";
   	    	i.setType(mime_type);
   	    	i.setAction(Intent.ACTION_INSERT_OR_EDIT);
   	    	startActivity(i);
//		    setResult(Activity.RESULT_OK, i);
//		    finish();

			break;
		}
		default:
			return super.onContextItemSelected(item);
		}
		
		return true;
	}
    // ========================================================================
    @Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
  	
    	Intent i = new Intent();
//    	i.setClass(this, ListActivityTaxonNavigator.class);
    	
    	i.setAction( getIntent().getAction() );
//    	i.addCategory(Constants.CATEGORY_TAXON);
    	

    	i.putExtra(Constants.INTENT_EXTRA_TSN, ((ListAdapterTaxons) l.getAdapter()).getItemTSN(position));
    	String taxon_name = ((TaxonInfo) l.getAdapter().getItem(position)).taxon_name;
    	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, taxon_name);
    	
    	ArrayList<String> new_hierarchy_key = new ArrayList<String>();
    	new_hierarchy_key.addAll( hierarchy_key );
    	new_hierarchy_key.add( taxon_name );
    	i.putExtra(INTENT_EXTRA_CURRENT_HIERARCHY, new_hierarchy_key);
    	i.putExtra(Constants.INTENT_EXTRA_RANK_NAME, ((TaxonInfo) l.getAdapter().getItem(position)).rank_name);
    	
    	

    	initialize_with_intent(i);
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

		boolean has_children = getListAdapter().getCount() > 0;
		menu.findItem(R.id.menu_sort_alpha).setVisible( has_children );
		menu.findItem(R.id.menu_sort_popularity).setVisible( has_children );
		
        return true;
    }
    
    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	

        boolean picking = Intent.ACTION_PICK.equals( getIntent().getAction() );
    	
        switch (item.getItemId()) {
        case R.id.menu_sort_popularity:
        	handle_radio_selection(item);
        	

        	((ListAdapterTaxons) TaxonNavigatorLinear.this.getListAdapter()).sort_by_popularity();
//        	Toast.makeText(ListActivityTaxonNavigator.this, "Sorted.", Toast.LENGTH_SHORT).show();
            return true;

        case R.id.menu_sort_alpha:
        	handle_radio_selection(item);

        	((ListAdapterTaxons) TaxonNavigatorLinear.this.getListAdapter()).sort_alphabetically();
//        	Toast.makeText(ListActivityTaxonNavigator.this, "Sorted.", Toast.LENGTH_SHORT).show();
            return true;
            
            
        case R.id.menu_radial_view:
        {
        	Intent i = new Intent();

        	i.putExtra(Constants.INTENT_EXTRA_TSN, getCurrentTSN());
        	i.setClass(this, TaxonNavigatorRadial.class);
        	startActivity(i);
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }


    // ========================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
	   			
	   		case TAXON_CHOOSER_RETURN_CODE:

		    	setResult(Activity.RESULT_OK, data);
		    	finish();
	   			break;
	   			
	   		case TEXTUAL_SEARCH_RETURN_CODE:
	   			Log.d(TAG, "Returned from search.");
	   		case BOOKMARK_OR_SIGHTINGS_RETURN_CODE:
	   		{
//			    Toast.makeText(this, "Returned from search.", Toast.LENGTH_SHORT).show();

	   			long target_tsn = data.getLongExtra(Constants.INTENT_EXTRA_TSN, Constants.INVALID_TSN);

	   			boolean direct_choice_made = data.getBooleanExtra(Constants.INTENT_EXTRA_DIRECT_CHOICE_MADE, false);
	   			if (direct_choice_made) {

	   	   	    	Intent i = new Intent();
	   	   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, target_tsn);
	   		    	setResult(Activity.RESULT_OK, i);
	   		    	finish();
	   			} else {
		   			
		        	Intent i = getIntent();
		        	i.putExtra(Constants.INTENT_EXTRA_TSN, target_tsn);
		        	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, data.getStringExtra(Constants.INTENT_EXTRA_TAXON_NAME));
		        	i.putExtra(Constants.INTENT_EXTRA_RANK_NAME, data.getStringExtra(Constants.INTENT_EXTRA_RANK_NAME));
	
		        	initialize_with_intent(i);
	   			}
	        	break;
	   		}
	   		default:
	   			super.onActivityResult(requestCode, resultCode, data);
		    	break;
		   }
		}
    }

    // ========================================================================
	@Override
	long getCurrentTSN() {
		return current_tsn;
	}
}
