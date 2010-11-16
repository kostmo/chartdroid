package org.crittr.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.ListActivityTextualSearch;
import org.crittr.browse.activity.ListActivityTextualSearch.NameType;
import org.crittr.browse.activity.ListActivityTextualSearch.RankComparisonInequalities;
import org.crittr.browse.activity.ListActivityTextualSearch.SearchMode;
import org.crittr.containers.SearchProgressPacket;
import org.crittr.containers.SearchProgressPacket.SearchStage;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.KingdomRankIdPair;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.shared.browser.itis.ItisUtils;
import org.crittr.shared.browser.itis.ItisObjects.CommonNameSearchResult;
import org.crittr.shared.browser.itis.ItisObjects.KingdomRankResult;
import org.crittr.shared.browser.itis.ItisObjects.ScientificNameSearchResult;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.utilities.AsyncTaxonInfoPopulator;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

// ========================================================================
public class TaxonSearchTask extends AsyncTask<Void, SearchProgressPacket, List<TaxonInfo>> {

	static final String TAG = Market.DEBUG_TAG; 

	List<TaxonInfo> aborted_results;

	String search_string;
	SharedPreferences settings;

	ListActivityTextualSearch activity;

    DatabaseTaxonomy database;
    SearchProgressPacket last_progress_stage = null;
	
	public TaxonSearchTask(String s, SharedPreferences settings, ListActivityTextualSearch activity, DatabaseTaxonomy database) {
		this.search_string = s;
		this.settings = settings;
		this.database = database;

		updateActivity(activity);
	}

	// Used for orientation changes
	public void updateActivity(ListActivityTextualSearch activity) {
		this.activity = activity;
		if (last_progress_stage != null)
			publishProgress(last_progress_stage);
	}

	@Override
	public void onPreExecute() {

		// This is unnecessary now that we have a progress dialog.
		//    		ListActivityTextualSearch.this.setProgressBarIndeterminateVisibility(true);

		ListAdapterTaxons adapter = ((ListAdapterTaxons) activity.getListAdapter());

		adapter.refresh_list(new ArrayList<TaxonInfo>());
		adapter.notifyDataSetInvalidated();

		((TextView) activity.findViewById(R.id.search_result_count)).setVisibility(View.GONE);

		activity.showDialog(ListActivityTextualSearch.DIALOG_SEARCH_PROGRESS);
		activity.prog_dialog.setOnCancelListener(
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						TaxonSearchTask.this.cancel(true);
					}
				});
	}

	// ====================================================================
	List<TaxonInfo> do_network_search() throws NetworkUnavailableException {
		List<TaxonInfo> taxon_search_results =  new ArrayList<TaxonInfo>();

		switch (NameType.values()[ activity.name_type_spinner.getSelectedItemPosition() ]) {
		case VERNACULAR:

			List<CommonNameSearchResult> raw_result_list = null;

			switch (SearchMode.values()[ activity.search_mode_spinner.getSelectedItemPosition() ]) {
			case BEGINS_WITH:
				raw_result_list = ItisQuery.searchByCommonNameBeginsWith(activity, search_string);
				break;
			case ENDS_WITH:
				raw_result_list = ItisQuery.searchByCommonNameEndsWith(activity, search_string);
				break;
			case CONTAINS:
				raw_result_list = ItisQuery.searchByCommonName(activity, search_string);
				break;
			}

			List<String> acceptable_languages_list = Arrays.asList(AsyncTaxonInfoPopulator.acceptable_languages);
			for (CommonNameSearchResult cnsr : raw_result_list) {
				if ( acceptable_languages_list.contains( cnsr.language ) ) {

					TaxonInfo ti = new TaxonInfo();
					ti.vernacular_name = cnsr.common_name;
					ti.tsn = cnsr.tsn;
					taxon_search_results.add(ti);
				}
			}

			break;
		case SCIENTIFIC:

			for (ScientificNameSearchResult snsr : ItisQuery.searchByScientificName(activity, search_string)) {

				TaxonInfo ti = new TaxonInfo();
				ti.tsn = snsr.tsn;
				ti.taxon_name = snsr.combined_name;
				taxon_search_results.add(ti);
			}
		}

		return taxon_search_results;
	}

	// ====================================================================    	
	@Override
	protected List<TaxonInfo> doInBackground(Void... params) {



		publishProgress( new SearchProgressPacket( SearchStage.SEARCHING ) );

		List<TaxonInfo> taxon_search_results;
		try {
			taxon_search_results = do_network_search();
		} catch (NetworkUnavailableException e) {
			taxon_search_results = new ArrayList<TaxonInfo>();
		}

		if (!activity.rank_filtering_enabled) return taxon_search_results;




		// RANK FILTERING STUFF BELOW
		List<TaxonInfo> filtered_taxon_search_results = new ArrayList<TaxonInfo>();


		// Next, filter out non-conforming ranks.
		int true_kingdom_id = ItisUtils.KINGDOM_ID_LIST[activity.kingdom_spinner.getSelectedItemPosition()];

		// Ick, we need to re-sort the list to get the proper id...
		ArrayList<Map.Entry<Integer, String>> these_kingdom_ranks = new ArrayList<Map.Entry<Integer, String>>(
				database.rank_name_key.get(true_kingdom_id).entrySet()
		);

		Collections.sort(these_kingdom_ranks, new Comparator<Map.Entry<Integer, String>>() {
			public int compare(Entry<Integer, String> object1, Entry<Integer, String> object2) {
				return object1.getKey().compareTo( object2.getKey() );
			}
		});


		int comparison_rank_id = these_kingdom_ranks.get( activity.rank_spinner.getSelectedItemPosition() ).getKey();
		Log.e(TAG, "Spinner position: " + activity.rank_spinner.getSelectedItemPosition() + "; Rankd ID: " + comparison_rank_id + "; Rank: " + (String) activity.rank_spinner.getSelectedItem());


		int max_rank_comparisons = settings.getInt("max_rank_comparisons", 60);

		SearchProgressPacket packet = new SearchProgressPacket(SearchStage.RANK_FILTERING);
		packet.stage_progress_max = Math.min(max_rank_comparisons, taxon_search_results.size());
		publishProgress( packet );


		int count = 0;
		for (TaxonInfo ti : taxon_search_results) {

			// Check whether thread cancelled...

			if (this.isCancelled() || count >= max_rank_comparisons) {

				// We'll get at least a partially-filtered set of results.
				aborted_results = filtered_taxon_search_results;
				publishProgress( new SearchProgressPacket(SearchStage.DIALOG_CANCELLATION) );
				return aborted_results;

				// We'll get the full set of results instead.
				//					return taxon_search_results;
			}				

			// Fetch the rank (synchronously).
			KingdomRankIdPair ikrp = database.getRankId(ti.tsn);
			if (ikrp.rank_id == Constants.INVALID_RANK_ID) {
				try {


					KingdomRankResult krr = ItisQuery.getTaxonomicRankNameFromTSN(activity, ti.tsn);

					ti.rank_id = krr.rank_id;
					ti.itis_kingdom_id = krr.kingdom_id;

					// TODO: ADD Taxon to queue for rank id updates in a transaction!

				} catch (NetworkUnavailableException e) {
					e.printStackTrace();
				}
			}



			// Now make our comparison.
			// NOTE: The sense of this comparison is inverted, because ITIS numbers
			// ranks in increasing order from Kingdom to species.
			if (true_kingdom_id == ti.itis_kingdom_id) {

				boolean criteria_met = false;

				switch (RankComparisonInequalities.values()[activity.rank_inequality_spinner.getSelectedItemPosition()]) {
				case AT_LEAST:
					criteria_met = ti.rank_id <= comparison_rank_id;
					Log.i(TAG, "Checking " + ti.rank_id + "<=" +  comparison_rank_id + " for " + ti.taxon_name + ": " + criteria_met);
					break;
				case AT_MOST:

					criteria_met = ti.rank_id >= comparison_rank_id;
					Log.i(TAG, "Checking " + ti.rank_id + ">=" +  comparison_rank_id + " for " + ti.taxon_name + ": " + criteria_met);
					break;

				case IS:

					criteria_met = ti.rank_id == comparison_rank_id;
					Log.i(TAG, "Checking " + ti.rank_id + "==" +  comparison_rank_id + " for " + ti.taxon_name + ": " + criteria_met);
					break;
				}

				if (criteria_met)
					filtered_taxon_search_results.add(ti);
			}

			count++;

			packet.stage_current_progress = count;
			publishProgress( packet );
		}


		// NOTE: Once we've fetched the rank here, don't let the AsyncPopulator re-fetch it!!!
		return filtered_taxon_search_results;
	}

	// ====================================================================
	@Override
	protected void onProgressUpdate(SearchProgressPacket... progress) {
		SearchProgressPacket progress_stage = progress[0];
		last_progress_stage = progress_stage;

		// XXX Might this grab the old reference?
		ProgressDialog progress_dialog = activity.prog_dialog;
		progress_dialog.setIndeterminate(progress_stage.stage != SearchStage.RANK_FILTERING);

		String message = null;
		switch (progress_stage.stage) {
		case SEARCHING:

			message = "Searching";
			break;

		case RANK_FILTERING:

			message = "Filtering by rank";
			progress_dialog.setMax(progress_stage.stage_progress_max);
			progress_dialog.setProgress(progress_stage.stage_current_progress);
			break;

		case DIALOG_CANCELLATION:
			refresh_result_list(aborted_results);
			return;

		default:
			break;
		}

		Log.d(TAG, message);
		progress_dialog.setMessage(message);
	}

	// ====================================================================
	@Override
	public void onPostExecute(List<TaxonInfo> taxon_search_results) {

		refresh_result_list(taxon_search_results);

		Log.d(TAG, "Removing dialog in onPostExecute()...");
//		removeDialog(ListActivityTextualSearch.DIALOG_SEARCH_PROGRESS);
		activity.dismissDialog(ListActivityTextualSearch.DIALOG_SEARCH_PROGRESS);
	}

	// ====================================================================
	void refresh_result_list(List<TaxonInfo> taxon_search_results) {
		// Finally, limit the result set to maximum count.    		

		boolean limit_results = settings.getBoolean("limit_taxon_search_results", true);
		int max_results = settings.getInt("max_taxon_search_results", 30);

		List<TaxonInfo> result_subset;
		if (limit_results)
			result_subset = taxon_search_results.subList( 0, Math.min(max_results, taxon_search_results.size()) );
		else
			result_subset = taxon_search_results;

		ListAdapterTaxons adapter = ((ListAdapterTaxons) activity.getListAdapter());
		adapter.refresh_list( result_subset );
		adapter.notifyDataSetInvalidated();

		TextView result_count = (TextView) activity.findViewById(R.id.search_result_count);
		result_count.setVisibility(View.VISIBLE);
		result_count.setText("Search returned " + result_subset.size() + " result(s).");
	}
}