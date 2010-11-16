package org.crittr.shared.browser.utilities;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.containers.OrphanLabeler;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.containers.ViewHolderTaxon;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.tracker.provider.AppEngineDataFrontend;
import org.crittr.task.NetworkUnavailableException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;






public class ListAdapterTaxons extends BaseAdapter {
	
	
	static final String TAG = Market.DEBUG_TAG;
	
	public List<TaxonInfo> taxon_list = new ArrayList<TaxonInfo>();

	
	public FlickrPhotoDrawableManager drawable_manager;
	
	
	public final AsyncTaxonInfoPopulator taxon_populator;
    final private LayoutInflater mInflater;
    int baseline_frequency = 1;
    
    private boolean orphan_check;
    private boolean do_orphan_check; 
    
    DatabaseTaxonomy helper;
    Context context;
    
    // ========================================================================
    public ListAdapterTaxons(Context context, boolean orphan_check) {
    	this.context = context;
    	
    	drawable_manager = new FlickrPhotoDrawableManager(context);
    	
    	
        mInflater = LayoutInflater.from(context);
        taxon_populator = new AsyncTaxonInfoPopulator(context);
        
        this.orphan_check = orphan_check;
        
        
        // FIXME - We might want to make this more dynamic, e.g. - 
        // check whether we're at the "top level" (current_tsn = -1).
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean label_orphans_pref = settings.getBoolean("label_orphans", false);
		do_orphan_check = orphan_check && label_orphans_pref;
     
		
    	helper = new DatabaseTaxonomy(context);
    }

    // ========================================================================
    class AlphaTaxonComparator implements Comparator<TaxonInfo> {
		public int compare(TaxonInfo object1, TaxonInfo object2) {
			return object1.taxon_name.compareTo(object2.taxon_name);
		}
    }
    
    

    // ========================================================================
    public void sort_by_popularity() {
		Collections.sort(taxon_list);
		this.notifyDataSetInvalidated();
    }

    // ========================================================================
    public void sort_alphabetically() {
		Collections.sort(taxon_list, new AlphaTaxonComparator());
		this.notifyDataSetInvalidated();
    }
    

    // ========================================================================
    public void clear_list() {
    	refresh_list( new ArrayList<TaxonInfo>() );
    }
    

    // ========================================================================
	public void refresh_list(List<TaxonInfo> new_taxon_list) {
		taxon_list.clear();
		taxon_list.addAll(new_taxon_list);
		

		baseline_frequency = update_max_frequency();
		
		notifyDataSetInvalidated();
		
		Log.e(TAG, "I notified invalidated, should refresh!");
	}

    // ========================================================================
	public int update_max_frequency() {
		int max_frequency = 1;	// This way, we avoid ever dividing by zero.
		for (TaxonInfo ti : taxon_list)
			max_frequency = Math.max(max_frequency, ti.frequency);
		return max_frequency;
	}
		


    // ========================================================================
    @Override
    public int getCount() {
        return taxon_list.size();
    }

    // ========================================================================
    @Override
    public Object getItem(int position) {
        return taxon_list.get(position);
    }

    // ========================================================================
    @Override    
    public long getItemId(int position) {
        return taxon_list.get(position).tsn;
    }

    // ========================================================================
    public long getItemTSN(int position) {
        return getItemId(position);
    }


    // ========================================================================
    public String getCurrentThumbnailURL(int position) {
    	
    	View taxon_view = getView(position, null, null);
    	ViewHolderTaxon holder = (ViewHolderTaxon) taxon_view.getTag();
        TaxonInfo ti = ((TaxonInfo) getItem(position));
        
        String current_url = null;
        
		if ( ti != null && ti.taxon_name != null && drawable_manager.taxon_to_thumbnail_url_map.containsKey(ti.taxon_name) ) {
			List<String> my_thumbnail_list = drawable_manager.taxon_to_thumbnail_url_map.get(ti.taxon_name);
			if (my_thumbnail_list.size() > 0) {
				
				Log.d(TAG, "Current thumbnail index: " + ti.preferred_thumbnail_index);
				
				current_url = my_thumbnail_list.get( ti.preferred_thumbnail_index );
			}
		}
		
		Log.d(TAG, "Result of getCurrentThumbnailURL(): " + current_url);
		
		return current_url;
    }
    

    // ========================================================================
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolderTaxon holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_taxon, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolderTaxon();
            holder.taxon_name_textview = (TextView) convertView.findViewById(R.id.taxon_name);
            holder.taxon_rank_textview = (TextView) convertView.findViewById(R.id.taxon_rank_name);
            
//			holder.tsn_textview = (TextView) convertView.findViewById(R.id.taxon_tsn);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.flickr_photo_thumbnail);
            
            holder.vernacular_name_textview = (TextView) convertView.findViewById(R.id.taxon_vernacular_name);
            holder.orphan_textview = (TextView) convertView.findViewById(R.id.orphan_textview);
            holder.full_view = convertView.findViewById(R.id.main_view);
            holder.rating_bar = (RatingBar) convertView.findViewById(R.id.small_ratingbar);

            convertView.setTag(holder);

        } else {

            // Get the ViewHolder back to get fast access to the View elements.
            holder = (ViewHolderTaxon) convertView.getTag();
        }

        final TaxonInfo ti = ((TaxonInfo) getItem(position));

        
        
        final ViewHolderTaxon holder_holder = holder;
        holder.thumbnail.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
//				Log.d(TAG, "New thumbnail index: " + holder_holder.incrementPreferredThumbnailIndex());
				
				if ( ti != null && ti.taxon_name != null && drawable_manager.taxon_to_thumbnail_url_map.containsKey(ti.taxon_name) ) {
					
					List<String> my_thumbnail_list = drawable_manager.taxon_to_thumbnail_url_map.get(ti.taxon_name);
					
					if (my_thumbnail_list.size() > 0) {
						String obtained_url_string = my_thumbnail_list.get( ti.incrementPreferredThumbnailIndex() % my_thumbnail_list.size());
						

						drawable_manager.fetchDrawableByUrlOnThread(obtained_url_string, holder_holder.thumbnail );
					}
				}
			}
        });
        
        
        


        populate_taxon_box(context, helper, taxon_populator, drawable_manager, holder, ti, do_orphan_check, baseline_frequency);
        
        return convertView;
    }
    

    // ========================================================================
    public static void populate_taxon_box(Context context, DatabaseTaxonomy helper, AsyncTaxonInfoPopulator taxon_populator, FlickrPhotoDrawableManager drawable_manager, ViewHolderTaxon holder, TaxonInfo ti, boolean check_orphan, int baseline_frequency) {


    	
    	// FIXME - This slows down the UI too much!
		boolean thumbnail_needed = true;

		String preferred_thumbnail_url = helper.getPreferredThumbnail(ti.tsn);
		if (preferred_thumbnail_url != null) {
			drawable_manager.fetchDrawableByUrlOnThread(preferred_thumbnail_url, holder.thumbnail);
			thumbnail_needed = false;
		}

    	
        
        if (ti.taxon_name == null || ti.taxon_name.length() == 0) {
        	
            taxon_populator.fetchTaxonNameOnThread( ti.tsn, holder, drawable_manager, ti.getPreferredThumbnailIndex(), thumbnail_needed );
        } else {
        	holder.taxon_name_textview.setText( ti.taxon_name );
        	
        	if (thumbnail_needed)
        		drawable_manager.fetchThumbnailImageOnThread( ti.taxon_name, holder.thumbnail, ti.getPreferredThumbnailIndex() );
        }

//        holder.tsn_textview.setText( Long.toString( ti.tsn ) );
        float normalized_rating = ti.frequency / (float) baseline_frequency;
        holder.rating_bar.setRating(normalized_rating * holder.rating_bar.getNumStars()); 

        if (ti.vernacular_name == null || ti.vernacular_name.length() == 0)
        	taxon_populator.fetchTaxonVernacularOnThread( ti.tsn, holder );
        else
        	holder.vernacular_name_textview.setText(ti.vernacular_name);
        
        taxon_populator.fetchTaxonRankOnThread( ti, holder );


        if (check_orphan)
        	taxon_populator.fetchParentTSNOnThread( ti.tsn, new OrphanLabeler(holder) );
    }
    

    // ========================================================================
    public static void conditionally_sort_by_popularity(Context context, List<TaxonInfo> taxon_list) throws UnknownHostException {
    	
		List<Long> tsn_list = new ArrayList<Long>();
		for (TaxonInfo taxinfo: taxon_list) tsn_list.add( taxinfo.tsn );

		
    	// Grab frequency for each TSN from AppEngine, then sort by frequency.
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean sort_by_popularity = settings.getBoolean("enable_sort_taxon_by_popularity", true);
		if (sort_by_popularity) {
			
			// Fetch and parse AppEngine result:
			String concatenated_tsn_list = TextUtils.join(",", tsn_list);
			String query_string = "get_tsn_popularity?tsn_list=" + concatenated_tsn_list;

			// Assign the popularity number retrieved from AppEngine
			
			try {
				
				Log.i(TAG, "About to fetch popularity data via ContentProvider...");
				Map<Long, Integer> popularity_hash = AppEngineDataFrontend.taxon_popularity(context, query_string);
				Log.i(TAG, "Fetched popularity data via ContentProvider: " + popularity_hash.size());
				
				for (long tsn : popularity_hash.keySet()) {
	    			for (TaxonInfo taxinfo: taxon_list)  {
	    				if (taxinfo.tsn == tsn) {
	    					taxinfo.frequency = popularity_hash.get(tsn);
	    					break;
	    				}
	    			}
				}
				
				// Sort by popularity...
	    		Collections.sort(taxon_list);
				
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}
		}
    }
}
