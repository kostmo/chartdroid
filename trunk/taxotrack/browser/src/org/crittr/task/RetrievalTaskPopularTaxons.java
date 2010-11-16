package org.crittr.task;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.crittr.appengine.AppEngineResponseParser;
import org.crittr.browse.R;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RetrievalTaskPopularTaxons extends AsyncTask<Void, Void, List<TaxonInfo>> {

	Context context;
	public RetrievalTaskPopularTaxons(Context c) {
		context = c;
	}


	
    @Override
    public void onPreExecute() {
		
		((TextView) ((Activity) context).findViewById(R.id.empty_list_text)).setText( R.string.working_message);
//		((ProgressBar) ((Activity) context).findViewById(R.id.empty_list_progress)).setVisibility(View.VISIBLE);

    }
    
	protected List<TaxonInfo> doInBackground(Void... params) {

		return get_popular_taxon_list(context);
	}

    @Override
    public void onPostExecute(List<TaxonInfo> rank_members) {
    	set_view_elements(rank_members);
    	
    }
    

    
    void set_view_elements(List<TaxonInfo> fetched_taxon_list) {
    	
    	/*
    	((TextView) ((Activity) context).findViewById(R.id.rank_member_count)).setText( Integer.toString( fetched_taxon_list.size() ) );
    	((TextView) ((Activity) context).findViewById(R.id.rank_member_count)).requestLayout();
    	 */
    	
    	if (fetched_taxon_list.size() <= 0) {

    		((TextView) ((Activity) context).findViewById(R.id.empty_list_text)).setText( R.string.empty_no_taxons );
    		((ProgressBar) ((Activity) context).findViewById(R.id.empty_list_progress)).setVisibility(View.GONE);

    		
    	} else {
    		
    		((ListAdapterTaxons) ((ListActivity) context).getListAdapter()).refresh_list( fetched_taxon_list );
    	}
    }
    
    
  
    List<TaxonInfo> get_popular_taxon_list(Context context) {
    	
    	List<TaxonInfo> fetched_taxon_list = new ArrayList<TaxonInfo>();

		// FIXME - Get limit from Settings?
		String query_string = "get_popular_taxons?limit=" + 25;

		// Assign the popularity number retrieved from AppEngine
		Map<Long, Integer> popularity_hash = null;
		try {
			popularity_hash = AppEngineResponseParser.parse_taxon_popularity_response(query_string);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (long tsn : popularity_hash.keySet()) {

			TaxonInfo ti = new TaxonInfo();
			ti.tsn = tsn;
			ti.frequency = popularity_hash.get(tsn);
			
			
			fetched_taxon_list.add( ti );
		}

		Collections.sort(fetched_taxon_list);
		return fetched_taxon_list;
    }
}