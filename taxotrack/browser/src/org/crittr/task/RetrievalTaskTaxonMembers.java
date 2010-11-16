package org.crittr.task;

import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.TaxonNavigatorLinear;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.shared.browser.itis.ItisObjects.HierarchyResult;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class RetrievalTaskTaxonMembers extends AsyncTask<Void, Void, List<TaxonInfo>> {
	
	static final String TAG = Market.DEBUG_TAG;
	
	long tsn;

	DatabaseTaxonomy database_helper;
	Activity context;
	boolean network_unavailable = false;
	public RetrievalTaskTaxonMembers(long tsn, Context c) {
		this.tsn = tsn;
		context = (Activity) c;
		database_helper = new DatabaseTaxonomy(context);
	}

	public List<TaxonInfo> networkFetchRankMembers(long tsn) {
    	List<TaxonInfo> rank_members_list = new ArrayList<TaxonInfo>();

		try {
			for (HierarchyResult hr : ItisQuery.getHierarchyDownFromTSN(context, tsn)) {

				TaxonInfo ti = new TaxonInfo();
				ti.taxon_name = hr.taxon_name;
				ti.tsn = hr.tsn;
				rank_members_list.add(ti);

			}
		} catch (NetworkUnavailableException e) {
			e.printStackTrace();
		}
		return rank_members_list;
	}
	
	
    @Override
    public void onPreExecute() {

		((TextView) context.findViewById(R.id.empty_list_text)).setText( R.string.working_message);
		context.findViewById(R.id.empty_list_progress).setVisibility(View.VISIBLE);
		context.findViewById(R.id.leaf_button_bar).setVisibility(View.GONE);
    }
    
	protected List<TaxonInfo> doInBackground(Void... params) {

		return networkFetchRankMembers(tsn);
	}

    @Override
    public void onPostExecute(List<TaxonInfo> rank_members) {
    	set_view_elements(rank_members);
    }

    
    void set_view_elements(List<TaxonInfo> rank_member_list) {
    	TextView rank_member_count = (TextView) context.findViewById(R.id.rank_member_count);
    	rank_member_count.setText( Integer.toString( rank_member_list.size() ) );
    	rank_member_count.requestLayout();

    	if (rank_member_list.size() > 0) {
    		
    		try {
    			ListAdapterTaxons.conditionally_sort_by_popularity(context, rank_member_list);
			} catch (UnknownHostException e) {

			    Toast.makeText(context, "No network connection!", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
    		((ListAdapterTaxons) ((TaxonNavigatorLinear) context).getListAdapter()).refresh_list( rank_member_list );

    	} else {
    		
//    		((TextView) findViewById(android.R.id.empty)).setText( R.string.max_depth_reached );
    		((TextView) context.findViewById(R.id.empty_list_text)).setText( R.string.max_depth_reached );
    		context.findViewById(R.id.empty_list_progress).setVisibility(View.GONE);
    		context.findViewById(R.id.leaf_button_bar).setVisibility(View.VISIBLE);
	   	}
    }
}