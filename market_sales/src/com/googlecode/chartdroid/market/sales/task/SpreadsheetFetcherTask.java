package com.googlecode.chartdroid.market.sales.task;

import com.googlecode.chartdroid.market.sales.GoogleCheckoutUtils;
import com.googlecode.chartdroid.market.sales.Market;
import com.googlecode.chartdroid.market.sales.GoogleCheckoutUtils.CheckoutCredentials;
import com.googlecode.chartdroid.market.sales.container.DateRange;
import com.googlecode.chartdroid.market.sales.container.HistogramBin;
import com.googlecode.chartdroid.market.sales.container.ProgressPacket;
import com.googlecode.chartdroid.market.sales.container.SpreadsheetRow;
import com.googlecode.chartdroid.market.sales.container.UsernamePassword;
import com.googlecode.chartdroid.market.sales.container.ProgressPacket.ProgressStage;
import com.googlecode.chartdroid.market.sales.provider.ColumnSchema;
import com.googlecode.chartdroid.market.sales.provider.DatabaseStorage;
import com.googlecode.chartdroid.market.sales.provider.SalesContentProvider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SpreadsheetFetcherTask extends AsyncTask<Void, ProgressPacket, Long> {

	static final String TAG = Market.TAG;

	protected ProgressDialog wait_dialog;
	protected Context context;
	UsernamePassword user_pass;
	DateRange date_range;
	int histogram_bincount;

	DatabaseStorage database;
	
	ProgressStage current_progress_stage;
	int current_progress_max = -1;
	String current_progress_message;
	
	GoogleCheckoutUtils google_checkout_utils = new GoogleCheckoutUtils();
	
	// ========================================================================
	public SpreadsheetFetcherTask(Context context, UsernamePassword user_pass, DateRange date_range, int histogram_bincount) {
		this.context = context;
		this.user_pass = user_pass;
		this.date_range = date_range;
		this.histogram_bincount = histogram_bincount;
		this.database = new DatabaseStorage(context);
	}

	// ========================================================================
	@Override
	public void onPreExecute() {

		this.wait_dialog = new ProgressDialog(this.context);
		this.wait_dialog.setMessage("");	// Needs to be initialized with a String to reserve space		
		this.wait_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		this.wait_dialog.setCancelable(false);
		this.wait_dialog.show();
	}

	// ========================================================================
	@Override
	protected Long doInBackground(Void... voided) {

		this.current_progress_message = "Authenticating...";
		publishProgress(new ProgressPacket(0, ProgressStage.AUTHENTICATING));
		CheckoutCredentials checkout_credentials = GoogleCheckoutUtils.recoverCheckoutCredentials(this.user_pass);

		List<DateRange> download_pieces = GoogleCheckoutUtils.planPiecewiseDownload(this.date_range);
		this.current_progress_message = "Downloading in chunks...";
		this.current_progress_max = download_pieces.size();
		
		List<SpreadsheetRow> aggregated_rows = new ArrayList<SpreadsheetRow>();
		int i=0;		
		for (DateRange partial_range : download_pieces) {
			publishProgress(new ProgressPacket(i, ProgressStage.DOWNLOADING));
			List<SpreadsheetRow> parsed_rows = this.google_checkout_utils.getPurchaseRecords(checkout_credentials, partial_range);
			aggregated_rows.addAll(parsed_rows);
			i++;
		}
		
		this.current_progress_message = "Storing records...";
		this.current_progress_max = -1;
		publishProgress(new ProgressPacket(0, ProgressStage.STORING));
//		long batch_id = this.database.storeRecords(aggregated_rows);

		List<HistogramBin> bins = this.google_checkout_utils.generateHistogram(aggregated_rows, this.histogram_bincount);
		Log.d(TAG, "Generated histrogram with bin count: " + bins.size());
		this.database.deleteOldPlots();	// Wipe old data
		long plot_id = this.database.aggregateForPlot(bins);
		
		return plot_id;
	}

	// ========================================================================
	@Override
    protected void onProgressUpdate(ProgressPacket... packets) {
		ProgressPacket packet = packets[0];
		if (packet.stage != this.current_progress_stage) {
			this.current_progress_stage = packet.stage;
			
			// Perform stage updates
			this.wait_dialog.setMessage(this.current_progress_message);			
			this.wait_dialog.setIndeterminate( this.current_progress_max < 0 );
			this.wait_dialog.setMax(this.current_progress_max);
		}
		
		this.wait_dialog.setProgress(packet.progress_value);
    }
	
	// ========================================================================
	@Override
	public void onPostExecute(Long batch_id) {

		this.wait_dialog.dismiss();

		if (this.google_checkout_utils.getErrorMessage() != null) {
			Toast.makeText(this.context, this.google_checkout_utils.getErrorMessage(), Toast.LENGTH_LONG).show();
		} else {

//			int record_count = this.database.countRawRecords(batch_id);
//			Log.e(TAG, "Stored record count: " + record_count);
			
			Uri chartdroid_uri = SalesContentProvider.constructUri(batch_id);
			
			Intent i = new Intent(Intent.ACTION_VIEW, chartdroid_uri);
			SimpleDateFormat mini_format = new SimpleDateFormat("MM/dd/yy");
			i.putExtra(Intent.EXTRA_TITLE, "Sales " + mini_format.format(this.date_range.start) + " to " + mini_format.format(this.date_range.end));
			
			
			ArrayList<String> axis_titles = new ArrayList<String>();
			axis_titles.add("Date");
			axis_titles.add(String.format("Sales per %.1f days", this.google_checkout_utils.getHistogramBinwidthDays()));
			i.putExtra(ColumnSchema.EXTRA_AXIS_TITLES, axis_titles);
			i.putExtra(ColumnSchema.EXTRA_FORMAT_STRING_Y, "$%.2f");
			
			Market.intentLaunchMarketFallback((Activity) this.context, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
		}
	}
}