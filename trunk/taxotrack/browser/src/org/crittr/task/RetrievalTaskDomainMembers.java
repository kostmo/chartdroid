package org.crittr.task;

import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.TaxonNavigatorLinear;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisUtils;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class RetrievalTaskDomainMembers extends AsyncTask<Void, String, List<TaxonInfo>> {

	static final String TAG = Market.DEBUG_TAG; 
	
	long tsn;
	Context context;
	List<TaxonInfo> taxon_list = new ArrayList<TaxonInfo>();
	

    // ========================================================================
	public RetrievalTaskDomainMembers(long tsn, Context c) {
		this.tsn = tsn;
		context = c;
	}

    // ========================================================================
    @Override
    public void onPreExecute() {
		
    	for (int i=0; i<ItisUtils.KINGDOM_NAME_LIST.length; i++) {
    		TaxonInfo ti = new TaxonInfo();
    		ti.rank_name = "Kingdom";
    		ti.taxon_name = ItisUtils.KINGDOM_NAME_LIST[i];
    		ti.tsn = ItisUtils.KINGDOM_TSN_LIST[i];
    		ti.parent_tsn = Constants.NO_PARENT_ID;
    		ti.vernacular_name = ItisUtils.KINGDOM_VERNACULAR_LIST[i];
    		taxon_list.add( ti );
    	}
    }

    // ========================================================================
	protected List<TaxonInfo> doInBackground(Void... params) {

		try {
			
			Log.d(TAG, "About to sort domains by popularity...");
			
			ListAdapterTaxons.conditionally_sort_by_popularity(context, taxon_list);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			publishProgress( "No network connection!" );
		}
    	
		return taxon_list;
	}
	

    // ========================================================================
    @Override
    public void onProgressUpdate(String... error_message) {

    	Toast.makeText(context, error_message[0], Toast.LENGTH_SHORT).show();
    }

	

    // ========================================================================
    @Override
    public void onPostExecute(List<TaxonInfo> rank_members) {

    	((TextView) ((Activity) context).findViewById(R.id.rank_member_count)).setText( Integer.toString( taxon_list.size() ) );
    	((ListAdapterTaxons) ((TaxonNavigatorLinear) context).getListAdapter()).refresh_list( rank_members );
    }
}