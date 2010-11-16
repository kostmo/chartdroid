package org.crittr.shared.browser.utilities;

import com.kostmo.tools.SemaphoreHost;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.KingdomRankIdPair;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.containers.ViewHolderTaxon;
import org.crittr.shared.browser.graphics.RoundedRectBackgroundDrawable;
import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.shared.browser.itis.ItisObjects.CommonNameSearchResult;
import org.crittr.shared.browser.itis.ItisObjects.HierarchyResult;
import org.crittr.shared.browser.itis.ItisObjects.KingdomRankResult;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.track.AsyncTaskModified;
import org.crittr.track.Market;
import org.crittr.track.R;
import org.crittr.track.containers.IntentDependentRunnable;
import org.crittr.track.retrieval_tasks.NetworkUnavailableException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncTaxonInfoPopulatorModified implements SemaphoreHost {
	
	// TODO: Retrieve this dynamically?
	public final static int MAX_RANK_INDEX = 260;
	
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	static final String TAG = Market.DEBUG_TAG;
	
	final int CURRENT_KINGDOM_FIXME = 1;

	
	public static String[] acceptable_languages = {"English", "unspecified"};
	
	// Note: We should have a different HashMap for each datasource.
	private final Map<Long, String> vernacular_map = new HashMap<Long, String>();
	private final Map<Long, KingdomRankResult> rank_id_map = new HashMap<Long, KingdomRankResult>();

	private final Map<Long, Long> parent_map = new HashMap<Long, Long>();
	private final Map<Long, String> taxon_name_map = new HashMap<Long, String>();
	
	
	
	DatabaseTaxonomy database_helper;
	
	
	public List<String> missing_photos = new ArrayList<String>();

	Context context;
	public AsyncTaxonInfoPopulatorModified(Context c) {
		context = c;
		database_helper = new DatabaseTaxonomy(context);
	}

	
	// ============================================================================

	public class RankDepRun2 extends RankDependentRunnable {

		Spinner rank_spinner;
		RankDepRun2(Spinner s) {
			rank_spinner = s;
		}


		public void run() {
			
			
			int true_kingdom_id = this.ikrp.kingdom_id;
			
	        List<Map.Entry<Integer, String>> these_kingdom_ranks = new ArrayList<Map.Entry<Integer, String>>(
	        		database_helper.rank_name_key.get(true_kingdom_id).entrySet()
	        		);
	        
	        Collections.sort(these_kingdom_ranks, new Comparator<Map.Entry<Integer, String>>() {
				public int compare(Entry<Integer, String> object1, Entry<Integer, String> object2) {
					return object1.getKey().compareTo( object2.getKey() );
				}
	        });

	        String[] ranks = new String[these_kingdom_ranks.size()];
	        for (int i=0; i<ranks.length; i++)
	        	ranks[i] = these_kingdom_ranks.get(i).getValue();
	        
	        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context,
	                android.R.layout.simple_spinner_item,
	                android.R.id.text1,
	                ranks);
	        
	        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        
	        rank_spinner.setAdapter(spinnerArrayAdapter);
		}
	}
	

	
    public class TaxonRankRetrievalTask extends AsyncTaskModified<Void, Void, KingdomRankResult> {

    	
    	ViewHolderTaxon taxon_info_holder;
    	long tsn;
    	

    	RankDependentRunnable rdr;
    	TaxonRankRetrievalTask(ViewHolderTaxon taxon_info_holder, long tsn) {
    		this.tsn = tsn;
    		this.taxon_info_holder = taxon_info_holder;
    	}
    	
    	void assignRunnable(RankDependentRunnable rdr) {
        	this.rdr = rdr;
    	}
    	
    	
	    @Override
	    public void onPreExecute() {
	    	incSemaphore();
	    }
    	
		protected KingdomRankResult doInBackground(Void... params) {

			try {
				
				if (rank_id_map.containsKey(tsn))
					return rank_id_map.get(tsn);
				
				KingdomRankResult krr = ItisQuery.getTaxonomicRankNameFromTSN(context, tsn);
				
				// FIXME
				if (krr.rank_id == 0 || krr.kingdom_id == 0)
					Log.e(TAG, "BAD! tsn: " + tsn + "; kingdom_id: " + krr.kingdom_id + "; rank_id: " + krr.rank_id);
				
				// Stock the hashmap like a responsible minion! :)
				rank_id_map.put(tsn, krr);
				
				return krr;
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}
			return new KingdomRankResult();
		}

		
	    @Override
	    public void onPostExecute(KingdomRankResult ikrp) {
	    	
	    	set_view_elements(ikrp);
	    	decSemaphore();
	    }
	    
	    
	    void set_view_elements(KingdomRankResult ikrp) {
	    	
	    	if (taxon_info_holder != null) {
				taxon_info_holder.taxon_rank_textview.setText( ikrp.rank_name );
				set_bg_color(taxon_info_holder.full_view, ikrp.rank_id);
	    	}
			
			if (rdr != null) {
				rdr.setKingdomRank(ikrp);
				rdr.run();
			}
	    }
    }
    
	
	void set_bg_color(View view, int rank_id) {
		// Maybe we should use just 5/6 of the color wheel?
		float hue = 360f * rank_id / MAX_RANK_INDEX;
		if (view != null) {
			
			int color = Color.HSVToColor(0xFF, new float[] {hue, 0.6f, 0.8f});
//			view.setBackgroundColor( color );
						
			Drawable d = new RoundedRectBackgroundDrawable(context, view, color); 
			view.setBackgroundDrawable(d);
		}
	}
	
	
	public void fetchTaxonRankOnThread(final TaxonInfo ti, final ViewHolderTaxon taxon_info_holder) {
		
		KingdomRankIdPair previously_populated_ikrp = ti.getIkrp();
		
		if (!previously_populated_ikrp.isInvalid()) {
			taxon_info_holder.taxon_rank_textview.setText( database_helper.getRankName( previously_populated_ikrp ) );
			set_bg_color(taxon_info_holder.full_view, ti.rank_id);
			
		} else if (rank_id_map.containsKey(ti.tsn)) {
			
			KingdomRankIdPair ikrp = new KingdomRankIdPair();
			KingdomRankResult krr = rank_id_map.get(ti.tsn);
			
//			Log.d(TAG, "TSN: " + ti.tsn + "; rank_id: " + krr.rank_id + "; kingdom_id: " + krr.kingdom_id);
			ikrp.rank_id = krr.rank_id;
			ikrp.kingdom_id = krr.kingdom_id;
			
			taxon_info_holder.taxon_rank_textview.setText( database_helper.getRankName( ikrp ) );
			set_bg_color(taxon_info_holder.full_view, rank_id_map.get(ti.tsn).rank_id);
			
		} else {
			taxon_info_holder.taxon_rank_textview.setText( "" );
			new TaxonRankRetrievalTask(taxon_info_holder, ti.tsn).execute();
		}
	}
	
	
	
	
	public void fetchTaxonRankOnThread(final long tsn, final Spinner kingdom_spinner) {

		TaxonRankRetrievalTask trrt = new TaxonRankRetrievalTask(null, tsn);
		
		if (kingdom_spinner != null)
			trrt.assignRunnable(new RankDepRun2(kingdom_spinner));
		
		trrt.execute();
	}
	
	// ======================================================================================
	// ======================================================================================

    public class TaxonNameRetrievalTask extends AsyncTaskModified<Void, Void, String> {

    	ViewHolderTaxon taxon_view_holder;
    	long tsn;
    	int preferred_thumbnail_index;
    	boolean thumbnail_needed;
    	TaxonNameRetrievalTask(ViewHolderTaxon taxon_info_holder, long tsn, int preferred_thumbnail_index ) {
    		this.tsn = tsn;
    		this.taxon_view_holder = taxon_info_holder;
    		this.preferred_thumbnail_index = preferred_thumbnail_index;
    	}
    	
    	public String networkFetchTaxonName(long tsn) throws NetworkUnavailableException {

    		// Network retrieval
    		String scientific_name = ItisQuery.getScientificNameFromTSN(context, tsn);

    		if (scientific_name != null && scientific_name.length() > 0)
    			taxon_name_map.put(tsn, scientific_name);
    		
    		return scientific_name;
    	}
    	
	    @Override
	    public void onPreExecute() {
    		
    		// The very FIRST thing we must do is check the HashMap!
    		if (taxon_name_map.containsKey(tsn)) {

//				Log.d(TAG, "Taxon name was cached: " + taxon_name);
				
				// We already have it in the database.
				set_view_elements( taxon_name_map.get(tsn) );
				this.cancel(false);
				return;
			}
	    	
    		incSemaphore();
	    }
    	
		protected String doInBackground(Void... params) {
			
			// If we're here, we must not have found it locally.
			// Fetch it from the network instead.
			try {
				
				return networkFetchTaxonName(tsn);
				
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}
			return "";
		}
		
	    @Override
	    public void onPostExecute(String taxon_name) {
	    	
	    	set_view_elements(taxon_name);
	    	decSemaphore();
	    }
	    
	    
	    void set_view_elements(String taxon_name) {
	    	

	    	
	    	taxon_view_holder.taxon_name_textview.setText( taxon_name );
	    }
    }
	
	
    
    
	public void fetchTaxonNameOnThread(final long tsn, final ViewHolderTaxon taxon_info_holder, int preferred_thumbnail_index ) {
		if (taxon_name_map.containsKey(tsn)) {
			
			String taxon_name = taxon_name_map.get(tsn);
			taxon_info_holder.taxon_name_textview.setText( taxon_name );
			
			if (taxon_info_holder.thumbnail != null) {

			}
		} else {

			taxon_info_holder.taxon_name_textview.setText( "loading..." );
			new TaxonNameRetrievalTask(taxon_info_holder, tsn, preferred_thumbnail_index ).execute();
		}
	}
	
	// ======================================================================================

	public static String filterCombineCommonNames(List<CommonNameSearchResult> common_name_hashes) {
		
		// Accept "English" and "unspecified".
		List<String> acceptable_languages_list = Arrays.asList(acceptable_languages);
		List<String> acceptable_vernacular_names = new ArrayList<String>();
		for (CommonNameSearchResult hash : common_name_hashes)
			if ( hash.language == null || acceptable_languages_list.contains( hash.language ) ) {
				String common_name = hash.common_name;
				// Avoid duplicates
				if ( ! acceptable_vernacular_names.contains(common_name))
					acceptable_vernacular_names.add( common_name );
			}
		
		String concatenated_vernaculars = TextUtils.join(", ", acceptable_vernacular_names);
		return concatenated_vernaculars;
	}
	
	
	

    public class VernacularRetrievalTask extends AsyncTaskModified<Void, Void, String> {

    	ViewHolderTaxon taxon_info_holder;
    	long tsn;
    	boolean fetched_locally = false;
    	VernacularRetrievalTask(ViewHolderTaxon taxon_info_holder, long tsn) {
    		this.tsn = tsn;
    		this.taxon_info_holder = taxon_info_holder;
    	}


    	
    	public String networkFetchTaxonVernacular(long tsn) throws NetworkUnavailableException {

    		// Content provider retrieval
    		List<CommonNameSearchResult> common_name_hashes = ItisQuery.getCommonNamesFromTSN(context, tsn);

    		String concatenated_vernaculars = filterCombineCommonNames(common_name_hashes);
    		
    		Log.d(TAG, "concatenated_vernaculars: " + concatenated_vernaculars);
    		
    		vernacular_map.put(tsn, concatenated_vernaculars);
    		return concatenated_vernaculars;
    	}

    	
	    @Override
	    public void onPreExecute() {
    		
    		if (vernacular_map.containsKey(tsn)) {
				
				// We already have it in the database.
    			String cached_vernacular = vernacular_map.get(tsn);
    			
    			Log.d(TAG, "VERNACULAR IN MEMORY: " + cached_vernacular);
    			
				set_view_elements( cached_vernacular );
				this.cancel(false);
				return;
			}

			incSemaphore();
	    }
    	
		protected String doInBackground(Void... params) {
			
			try {
				return networkFetchTaxonVernacular(tsn);
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}
			return "";
		}
		
	    @Override
	    public void onPostExecute(String vernacular) {
	    	
	    	set_view_elements(vernacular);
			
			decSemaphore();
	    }
	    
	    void set_view_elements(String vernacular_name) {
	    	taxon_info_holder.vernacular_name_textview.setText( vernacular_name );
	    }
    }
	
	
    
    
	public void fetchTaxonVernacularOnThread(final long tsn, final ViewHolderTaxon taxon_info_holder) {
		if (vernacular_map.containsKey(tsn)) {
			taxon_info_holder.vernacular_name_textview.setText( vernacular_map.get(tsn) );
		} else {
			taxon_info_holder.vernacular_name_textview.setText( "loading..." );
			new VernacularRetrievalTask(taxon_info_holder, tsn).execute();
		}
	}


	// ============================================================================

    public class ParentRetrievalTask extends AsyncTaskModified<Void, Void, Long> {

    	long tsn;

    	DatabaseTaxonomy database_helper;
    	ProgressDialog wait_dialog;
    	Intent latent_intent = null;
    	IntentDependentRunnable runnable_operation;
    	
    	ParentRetrievalTask(long tsn, IntentDependentRunnable run_op) {
    		
    		runnable_operation = run_op;
    		
    		this.tsn = tsn;
    		database_helper = new DatabaseTaxonomy(context);
    	}

		void instantiate_latent_wait_dialog() {
			wait_dialog = new ProgressDialog(context);
			wait_dialog.setMessage("Retrieving parent...");
			wait_dialog.setIndeterminate(true);
			wait_dialog.setCancelable(false);
			if (runnable_operation.enable_dialog)  {
//				Log.e(TAG, "The wait dialog should be shown...");
				wait_dialog.show();
			}
		}


    	
	    @Override
	    public void onPreExecute() {

//	    	Log.e(TAG, "Starting execution of ParentRetrievalTask.");
	    	
    		long retrieved_parent_tsn = Constants.INVALID_TSN;
    		if (parent_map.containsKey(tsn)) {
    			retrieved_parent_tsn = parent_map.get(tsn);
//    			Log.i(TAG, "parent_map has the key: " + retrieved_parent_tsn);
    		}

			if (retrieved_parent_tsn != Constants.INVALID_TSN) {
				
				// We already have them in the database.
//				Log.d(TAG, "We already have the parent in the database: " + retrieved_parent_tsn);

		    	update_map_and_finish(retrieved_parent_tsn);
			
				this.cancel(false);
				return;
			}
			
			instantiate_latent_wait_dialog();
	    }
	    
		protected Long doInBackground(Void... params) {
			try {
				return ItisQuery.getParentTSNFromTSN(context, tsn);
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}
			return Constants.INVALID_TSN;
		}

		
	    @Override
	    public void onPostExecute(Long parent_tsn) {
	    	
	    	// If, even after fetching from the netowrk, the
	    	// tsn is still invalid, then we must have an orphan.
	    	// Label it as such.
	    	if (parent_tsn == Constants.INVALID_TSN) {
	    		parent_tsn = Constants.ORPHAN_PARENT_ID;
	    		Log.e(TAG, "Moving up from an orphaned taxon: " + tsn);
	    	}
	    	
	    	update_map_and_finish(parent_tsn);

			wait_dialog.dismiss();
	    }
	    
	    void update_map_and_finish(long parent_tsn) {

	    	parent_map.put(tsn, parent_tsn);
			runnable_operation.getIntent().putExtra(Constants.INTENT_EXTRA_TSN, parent_tsn);
			runnable_operation.run();
	    }
    }
	
	
	public void fetchParentTSNOnThread(final long tsn, IntentDependentRunnable run_op) {

		new ParentRetrievalTask(tsn, run_op).execute();
		
//    	Log.e(TAG, "fetchParentTSNOnThread: " + tsn);
	}
	

	// ============================================================================
	
    public class HierarchyRetrievalTask extends AsyncTask<Void, Void, List<TaxonInfo>> {

    	long tsn;

    	DatabaseTaxonomy database_helper;
    	
    	final TextView taxon_info_holder;
    	
    	HierarchyRetrievalTask(long tsn, final TextView taxon_info_holder) {
    		this.taxon_info_holder = taxon_info_holder;
    		this.tsn = tsn;
    		database_helper = new DatabaseTaxonomy(context);

//			Log.i(TAG, "We were instantiated.");
    	}


    	public List<TaxonInfo> networkFetchFullHierarchy(long tsn) throws NetworkUnavailableException {

    		List<TaxonInfo> taxon_ladder = new ArrayList<TaxonInfo>();

			for (HierarchyResult hr : ItisQuery.getFullHierarchyFromTSN(context, tsn)) {
    			
    			if (hr.parent_tsn != Constants.INVALID_TSN) {
    				TaxonInfo ti = new TaxonInfo();

        			ti.tsn = hr.tsn;
        			ti.taxon_name = hr.taxon_name;
        			ti.parent_tsn = hr.parent_tsn;
	    			taxon_ladder.add(ti);
    			}
			}

			return taxon_ladder;
    	}
    	
    	
	    @Override
	    public void onPreExecute() {

//	    	Log.i(TAG, "We were executed.");
//    		taxon_info_holder.setVisibility(View.GONE);
	    	taxon_info_holder.setVisibility(View.INVISIBLE);
	    	
	    	incSemaphore();
	    }
	    
		protected List<TaxonInfo> doInBackground(Void... params) {

			try {
				return networkFetchFullHierarchy(tsn);
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}
			return new ArrayList<TaxonInfo>();
		}

	    @Override
	    public void onPostExecute(List<TaxonInfo> parent_taxon_chain) {
	    	int size = parent_taxon_chain.size();

	    	// Warn on orphaned taxon
	    	if (size == 0)
	    		Toast.makeText(context, R.string.orphaned_taxon, Toast.LENGTH_LONG).show();

	    	// Set the view
    		set_view_element(parent_taxon_chain);
    		
    		decSemaphore();
	    }


	    
	    void set_view_element(List<TaxonInfo> parent_taxon_chain) {
	    	
	    	// Make sure we have each of the taxon names?
	    	List<String> taxon_name_list = new ArrayList<String>();
    		for (TaxonInfo ti : parent_taxon_chain) taxon_name_list.add(ti.taxon_name);
   		
	    	
    		taxon_info_holder.setVisibility(View.VISIBLE);
    		String joined_hierarchy_string = TextUtils.join(" >> ", taxon_name_list);
//    		Log.d(TAG, "Joined string: " + joined_hierarchy_string);
            taxon_info_holder.setText( joined_hierarchy_string );
	    }
    }
	
    
    
	// ======================================
    
    public void incSemaphore() {

    	((Activity) context).setProgressBarIndeterminateVisibility(true);
    	retrieval_tasks_semaphore.incrementAndGet();
    }
    
    public void decSemaphore() {

    	boolean still_going = retrieval_tasks_semaphore.decrementAndGet() > 0;
    	((Activity) context).setProgressBarIndeterminateVisibility(still_going);
        
    }
    
	// ==============================================
	public void fetchHierarchyOnThread(final long tsn, final TextView parent_holder) {

		HierarchyRetrievalTask h = new HierarchyRetrievalTask(tsn, parent_holder); 
		h.execute();
	}
	
	// ======================================
	public static InputStream fetch(String urlString) throws MalformedURLException, IOException {
	   	DefaultHttpClient httpClient = new DefaultHttpClient();
	   	HttpGet request = new HttpGet(urlString);
	   	HttpResponse response = httpClient.execute(request);
	   	return response.getEntity().getContent();
	}
}
