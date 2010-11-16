package org.crittr.browse.activity;



import java.text.DateFormat;
import java.util.Date;

import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.R.drawable;
import org.crittr.browse.R.id;
import org.crittr.browse.R.layout;
import org.crittr.browse.R.menu;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.containers.ViewHolderTaxon;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.utilities.AsyncTaxonInfoPopulator;
import org.crittr.shared.browser.utilities.FlickrPhotoDrawableManager;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;


// Note: For now, this is just a list of bookmarks.
public class ListActivityBookmarks extends ListActivity {

	AsyncTaxonInfoPopulator taxon_populator;
	FlickrPhotoDrawableManager image_populator;
//	final int TAXON_CHOOSER_RETURN_CODE = 1;
	
	static final String TAG = Market.DEBUG_TAG;
	
	DatabaseTaxonomy helper;
	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.list_activity_bookmarks);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);
        
		helper = new DatabaseTaxonomy(this);
        taxon_populator = new AsyncTaxonInfoPopulator(this);
        image_populator = new FlickrPhotoDrawableManager(this);
        
        Cursor c = helper.get_recent_bookmarks();
        BookmarksListAdapter adapter = new BookmarksListAdapter(
        		this,
        		R.layout.list_item_taxon_bookmark,
                c);
        
		setListAdapter(adapter);
	
		
		
		registerForContextMenu(getListView());
    }
    
    
    // ======================================================
    
    public class BookmarksListAdapter extends ResourceCursorAdapter {

		public BookmarksListAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);

		}


		public void bindView(View view, Context context, Cursor cursor) {
			int tsn_column = 0;
			long my_tsn = cursor.getLong(tsn_column);
//			((TextView) view.findViewById(R.id.taxon_tsn)).setText( Long.toString( my_tsn ) );
			


			int count_column = 1;
			long my_counts = cursor.getLong(count_column);

			((TextView) view.findViewById(R.id.count_holder)).setText( Long.toString( my_counts ) );
			
			
			
			int timestamp_column = 2;
			long timestamp = cursor.getLong(timestamp_column);
			Date d = new Date(timestamp * 1000);
			String date_string = DateFormat.getDateTimeInstance().format(d);
			Log.e(TAG, "Formatted date: " + date_string);
			((TextView) view.findViewById(R.id.date_modified_holder)).setText( date_string );
			
			
			
			


	        ViewHolderTaxon holder;
            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolderTaxon();
            holder.taxon_name_textview = (TextView) view.findViewById(R.id.taxon_name);
            holder.taxon_rank_textview = (TextView) view.findViewById(R.id.taxon_rank_name);
            
//            holder.tsn_textview = (TextView) view.findViewById(R.id.taxon_tsn);
            holder.thumbnail = (ImageView) view.findViewById(R.id.flickr_photo_thumbnail);
            
            holder.vernacular_name_textview = (TextView) view.findViewById(R.id.taxon_vernacular_name);
            holder.orphan_textview = (TextView) view.findViewById(R.id.orphan_textview);
            holder.full_view = view.findViewById(R.id.taxon_enclosure);
            holder.rating_bar = (RatingBar) view.findViewById(R.id.small_ratingbar);

			
            TaxonInfo ti = new TaxonInfo();
            ti.tsn = my_tsn;
            ListAdapterTaxons.populate_taxon_box(
            		ListActivityBookmarks.this,
            		helper,
            		taxon_populator,
            		image_populator,
            		holder,
            		ti,
            		false,
            		1);
		}

    }
    
    
    
    void taxon_selection(long id) {
    	
    	Intent i = new Intent();
    	i.putExtra(Constants.INTENT_EXTRA_TSN, id);
    	
    	// TODO: An alternate way to determine this would be to check whether
    	// getCallingActivity() returns "null".
    	boolean should_return = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
    	if (should_return) {
	    	i.putExtra(Constants.INTENT_EXTRA_TSN, id);

	    	setResult(Activity.RESULT_OK, i);

	    	finish();
	    	
    	} else {
    		i.setAction(Intent.ACTION_VIEW);
    		i.addCategory(Constants.CATEGORY_TAXON);
    		startActivity(i);
    	}
    	
    }
    
    // ==========================================
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

    	taxon_selection(id);
    }
    
    // ======================================================

	
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_search_result_list, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Taxon action:");
        
        
        // We do everything that would be called in "onPrepare" right here.
        boolean picking = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
        menu.findItem(R.id.menu_taxon_accept).setVisible( picking );
        
        menu.findItem(R.id.menu_goto).setVisible( true );
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch (item.getItemId()) {
		case R.id.menu_usage_stats:
		{
			Log.e(TAG, "Not implemented.");
			return true;
		}
		case R.id.menu_goto:
        {
//	    	Intent i = new Intent(Intent.ACTION_PICK);
        	
	    	taxon_selection(info.id);
	    	
	    	
//	    	i.addCategory(ListActivityTaxonNavigator.CATEGORY_TAXON);
	//    	i.setClass(PhotoTagsListActivity.this, TaxonNavigatorListActivity.class);
	//    	startActivityForResult(i, TAXON_CHOOSER_RETURN_CODE);
//	    	startActivity(i);
	    	
	    	
	    	

			return true;
        }
        case R.id.menu_taxon_accept:
        {
   	    	Intent i = new Intent();

   	    	// Note: the full hierarchy can be recovered by API methods.
   	    	i.putExtra(Constants.INTENT_EXTRA_DIRECT_CHOICE_MADE, true);

	    	i.putExtra(Constants.INTENT_EXTRA_TSN, info.id);
	    	
	    	Log.d(TAG, "TSN from direct choice in bookmark/sightings list: " + info.id);
	    	
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
            return true;
        }
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	
	
	// =================================
	
    
    // ================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_bookmarks, menu);
        
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {


        case R.id.menu_clear_bookmarks:
        {	

        	Log.e(TAG, "Not implemented.");
            return true;
        }
        case R.id.menu_help:
        {	
        	// TODO
//        	showDialog();
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }
    
}
