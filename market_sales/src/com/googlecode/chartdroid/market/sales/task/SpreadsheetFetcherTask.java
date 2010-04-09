package com.googlecode.chartdroid.market.sales.task;

import com.googlecode.chartdroid.market.sales.Main;
import com.googlecode.chartdroid.market.sales.Market;
import com.googlecode.chartdroid.market.sales.container.ProgressPacket;
import com.googlecode.chartdroid.market.sales.container.SpreadsheetRow;
import com.googlecode.chartdroid.market.sales.container.ProgressPacket.ProgressStage;
import com.googlecode.chartdroid.market.sales.provider.ColumnSchema;
import com.googlecode.chartdroid.market.sales.provider.DatabaseStorage;
import com.googlecode.chartdroid.market.sales.provider.SalesContentProvider;
import com.kostmo.tools.StreamUtils;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import au.com.bytecode.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpreadsheetFetcherTask extends AsyncTask<Void, ProgressPacket, Long> {

	static final String TAG = Market.TAG;


	final static int DOWNLOAD_DAYS_INCREMENT = 30;
	
	public static final String GDTOKEN_COOKIE = "gdToken";
	public static final String SECURE_HTTP_SCHEME = "https";

	public static final Uri TARGET_CHECKOUT_PAGE = new Uri.Builder()
	.scheme(SECURE_HTTP_SCHEME)
	.authority("checkout.google.com")
	.path("sell/orders").build();

	static SimpleDateFormat ISO8601FORMAT_YMD = new SimpleDateFormat("yyyy-MM-dd");


	protected ProgressDialog wait_dialog;
	protected Context context;
	UsernamePassword user_pass;
	DateRange date_range;
	int histogram_bincount;

	DatabaseStorage database;
	String error_message;
	ProgressStage current_progress_stage;
	int current_progress_max = -1;
	String current_progress_message;
	
	float histogram_binwidth_days;
	
	// ========================================================================
	public SpreadsheetFetcherTask(Context context, UsernamePassword user_pass, DateRange date_range, int histogram_bincount) {
		this.context = context;
		this.user_pass = user_pass;
		this.date_range = date_range;
		this.histogram_bincount = histogram_bincount;
		this.database = new DatabaseStorage(context);
	}

	// ========================================================================
	public static class DateRange implements Comparable<DateRange> {
		// The natural sort order shall be by "start date".
		
		public Date start, end;
		
		public DateRange() {};
		
		public DateRange(Date start, Date end) {
			this.start = start;
			this.end = end;
		};		
		
		@Override
		public int compareTo(DateRange another) {
			return this.start.compareTo(another.start);
		}
	}

	// ========================================================================
	public static class HistogramBin extends DateRange {
		public List<SpreadsheetRow> rows;
		
		HistogramBin() {
			super();
			this.rows = new ArrayList<SpreadsheetRow>();
		}
		
		HistogramBin(Date start, Date end, List<SpreadsheetRow> rows) {
			super(start, end);
			this.rows = rows;
		}
		
		public double getTotalIncome() {
			double total = 0;
			for (SpreadsheetRow row : rows)
				total += row.getIncome();
			return total;
		}
	}
	
	// ========================================================================
	public static class UsernamePassword {
		private String username, password;

		public UsernamePassword(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUserName() {
			return username;
		}

		public String getPassword() {
			return password;
		}
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
		CheckoutCredentials checkout_credentials = recoverCheckoutCredentials(user_pass);

		List<DateRange> download_pieces = planPiecewiseDownload(this.date_range);
		this.current_progress_message = "Downloading in chunks...";
		this.current_progress_max = download_pieces.size();
		
		List<SpreadsheetRow> aggregated_rows = new ArrayList<SpreadsheetRow>();
		int i=0;		
		for (DateRange partial_range : download_pieces) {
			publishProgress(new ProgressPacket(i, ProgressStage.DOWNLOADING));
			List<SpreadsheetRow> parsed_rows = getPurchaseRecords(checkout_credentials, partial_range);
			aggregated_rows.addAll(parsed_rows);
			i++;
		}
		
		current_progress_message = "Storing records...";
		this.current_progress_max = -1;
		publishProgress(new ProgressPacket(0, ProgressStage.STORING));
//		long batch_id = this.database.storeRecords(aggregated_rows);

		List<HistogramBin> bins = generateHistogram(aggregated_rows, this.histogram_bincount);
		Log.d(TAG, "Generated histrogram with bin count: " + bins.size());
		long plot_id = this.database.aggregateForPlot(bins);
		
		return plot_id;
	}

	// ========================================================================
	@Override
    protected void onProgressUpdate(ProgressPacket... packets) {
		ProgressPacket packet = packets[0];
		if (packet.stage != current_progress_stage) {
			current_progress_stage = packet.stage;
			
			// Perform stage updates
			this.wait_dialog.setMessage(current_progress_message);			
			this.wait_dialog.setIndeterminate( current_progress_max < 0 );
			this.wait_dialog.setMax(current_progress_max);
		}
		
		this.wait_dialog.setProgress(packet.progress_value);
    }
	
	// ========================================================================
	@Override
	public void onPostExecute(Long batch_id) {

		this.wait_dialog.dismiss();

		if (error_message != null) {
			Toast.makeText(context, error_message, Toast.LENGTH_LONG).show();
		} else {

//			int record_count = this.database.countRawRecords(batch_id);
//			Log.e(TAG, "Stored record count: " + record_count);
			
			Uri chartdroid_uri = SalesContentProvider.constructUri(batch_id);
			
			Intent i = new Intent(Intent.ACTION_VIEW, chartdroid_uri);
			SimpleDateFormat mini_format = new SimpleDateFormat("MM/dd/yy");
			i.putExtra(Intent.EXTRA_TITLE, "Sales " + mini_format.format(this.date_range.start) + " to " + mini_format.format(this.date_range.end));
			
			
			ArrayList<String> axis_titles = new ArrayList<String>();
			axis_titles.add("Date");
			axis_titles.add(String.format("Sales per %.1f days", this.histogram_binwidth_days));
			i.putExtra(ColumnSchema.EXTRA_AXIS_TITLES, axis_titles);
			
			i.putExtra(ColumnSchema.EXTRA_FORMAT_STRING_Y, "$%.2f");
			
			Market.intentLaunchMarketFallback((Activity) context, Market.MARKET_PACKAGE_SEARCH_STRING, i, Market.NO_RESULT);
		}
	}

	// ========================================================================
	static Date decrementDate(Date old_date, long milliseconds) {
		return new Date(old_date.getTime() - milliseconds);
	}
	
	// ========================================================================
	List<HistogramBin> generateHistogram(List<SpreadsheetRow> rows, int bin_count) {

		// It may be safe to assume that the list is already sorted, but we don't take any chances.
		Collections.sort(rows);
		
		// Given a fixed bin duration, there will usually be one bin that is smaller than the rest.
		// I prefer to have this bin come first, so as not to suggest a sudden slump in sales
		// in the most recent bin.  Therefore, we reverse the list and start adding the latest dates
		// to bins first.
		Collections.reverse(rows);
		
		// Have a moving calendar date that starts at the most recent entry and advances
		// backwards in time towards the oldest entry.
		Date max_bin_age = new Date();
		long millisecond_bin_duration = 0;
		if (rows.size() > 0) {
			max_bin_age = rows.get(0).getOrderDate();
			Date oldest = rows.get(rows.size() - 1).getOrderDate();
			
			millisecond_bin_duration = (max_bin_age.getTime() - oldest.getTime()) / bin_count;
			this.histogram_binwidth_days = millisecond_bin_duration / (float) Main.MILLIS_PER_DAY;
		}


		List<HistogramBin> bins = new ArrayList<HistogramBin>();
		HistogramBin bin = null;
		for (SpreadsheetRow row : rows) {
			if ( !row.getOrderDate().after(max_bin_age) ) {
				// The first iteration of the loop should enter this branch

				// If we've filled our quota of bins, the only data left
				// is destined for a partially filled bin, so we ignore it.
				if (bins.size() >= bin_count) break;
				
				bin = new HistogramBin();
				bin.end = max_bin_age;
				max_bin_age = decrementDate(max_bin_age, millisecond_bin_duration);
				bin.start = max_bin_age;
				
				bins.add(bin);
			}

			bin.rows.add(row);
		}
		
		// Although the bins will be sorted in increasing chronological order automatically by
		// the SQLite database, this function may be used elsewhere, so we ensure increasing
		// order here.
		Collections.reverse(bins);
		
		return bins;
	}

	// ========================================================================
	List<DateRange> planPiecewiseDownload(DateRange date_range) {
		
		// The date range must be split into segments of no longer than 30 days.
		List<DateRange> download_pieces = new ArrayList<DateRange>();

		Calendar moving_front = new GregorianCalendar();
		moving_front.setTime(date_range.start);
		
		Calendar end_date = new GregorianCalendar();
		end_date.setTime(date_range.end);

		while (moving_front.before(end_date)) {

			Date segment_start = moving_front.getTime();
			moving_front.add(Calendar.DATE, DOWNLOAD_DAYS_INCREMENT);
			
			Date segment_end = moving_front.before(end_date) ? moving_front.getTime() : end_date.getTime();
			download_pieces.add(new DateRange(segment_start, segment_end));
		}

		Log.d(TAG, "Splitting download into " + download_pieces.size() + " chunks.");
		
		return download_pieces;
	}
	
	// ========================================================================
	List<SpreadsheetRow> getPurchaseRecords(CheckoutCredentials checkout_credentials, DateRange date_range) {
		// Example row:
		//		Google Order Number,Merchant Order Number,Order Creation Date,Currency of Transaction,Order Amount,Amount Charged,Financial Status,Fulfillment Status
		//		345850876342638,345850876342638,"Dec 11, 2009 3:38:08 AM",USD,1.49,1.49,CHARGED,DELIVERED

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("start-date", ISO8601FORMAT_YMD.format(date_range.start)));
		nameValuePairs.add(new BasicNameValuePair("end-date", ISO8601FORMAT_YMD.format(date_range.end)));
		nameValuePairs.add(new BasicNameValuePair("financial-state", "CHARGED"));
		nameValuePairs.add(new BasicNameValuePair("_type", "order-list-request"));

		String url_string = new Uri.Builder()
		.scheme(SECURE_HTTP_SCHEME)
		.authority(TARGET_CHECKOUT_PAGE.getAuthority())
		.path("cws/v2/Merchant/" + checkout_credentials.merchant_id + "/reportsForm").toString();

		HttpPost httppost = new HttpPost( url_string );
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		List<SpreadsheetRow> parsed_rows = new ArrayList<SpreadsheetRow>();
		try {

			DefaultHttpClient httpclient = new DefaultHttpClient();

			BasicClientCookie cookie = new BasicClientCookie(GDTOKEN_COOKIE, checkout_credentials.gdToken);
			cookie.setDomain( Uri.parse(url_string).getHost() );
			httpclient.getCookieStore().addCookie(cookie);

			HttpResponse response = httpclient.execute(httppost);

			Log.d(TAG, "Status code: " + response.getStatusLine().getStatusCode());
			Log.d(TAG, "Reason: " + response.getStatusLine().getReasonPhrase());
			InputStream content_stream = response.getEntity().getContent();

			boolean parse = true;
			if (parse) {
				String response_string = StreamUtils.convertStreamToString(content_stream);

				CSVReader reader = new CSVReader(new StringReader(response_string), ',', '"', 1);
				List<String[]> myEntries = reader.readAll();
				reader.close();
				try {

					for (String[] row : myEntries)
						parsed_rows.add(new SpreadsheetRow(row));

				} catch (NumberFormatException e) {
					error_message = response_string.trim();
					return null;
				} catch (Exception e) {
					error_message = e.getMessage();
					return null;
				}

			} else {
				String result = StreamUtils.convertStreamToString(content_stream);
				Log.i(TAG, result);
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		return parsed_rows;
	}

	// ========================================================================
	public static class CheckoutCredentials {
		long merchant_id;
		String gdToken;
	}

	// ========================================================================
	CheckoutCredentials recoverCheckoutCredentials(UsernamePassword user_pass) {

		CheckoutCredentials credentials = new CheckoutCredentials();

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;

		String secret_galaxy = null;
		long secret_code = 0;
		{

			Uri uri = new Uri.Builder().scheme(SECURE_HTTP_SCHEME).authority("www.google.com")
			.path("/accounts/ServiceLoginAuth")
			.appendQueryParameter("service", "sierra")
			.appendQueryParameter("continue", TARGET_CHECKOUT_PAGE.toString())
			.appendQueryParameter("ltmpl", "mobilec")
			.appendQueryParameter("rm", "hide")
			.appendQueryParameter("btmpl", "mobile")
			.build();
			String cookie_fetcher_url_string = uri.toString();


			HttpGet httpget = new HttpGet( cookie_fetcher_url_string );
			try {
				response = httpclient.execute(httpget);
				String response_string = StreamUtils.convertStreamToString(response.getEntity().getContent());


				{
					String uncategorized_template_matcher = "input\\s+type=\"hidden\"\\s+name=\"dsh\"\\s+id=\"dsh\"\\s+value=\"(-?\\d+)\"";
					Pattern template_pattern = Pattern.compile(uncategorized_template_matcher, Pattern.CASE_INSENSITIVE);
					Matcher classMatcher = template_pattern.matcher(response_string);
					if (classMatcher.find()) {
						secret_code = Long.parseLong(classMatcher.group(1));
					}
				}

				{
					String uncategorized_template_matcher = "input\\s+type=\"hidden\"\\s+name=\"GALX\"\\s+value=\"([^\"]+)\"";
					Pattern template_pattern = Pattern.compile(uncategorized_template_matcher, Pattern.CASE_INSENSITIVE);
					Matcher classMatcher = template_pattern.matcher(response_string);
					if (classMatcher.find()) {
						secret_galaxy = classMatcher.group(1);
					}
				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}







		Uri uri = new Uri.Builder().scheme(SECURE_HTTP_SCHEME).authority("www.google.com").path("/accounts/ServiceLoginAuth").appendQueryParameter("service", "sierra").build();
		String cookie_fetcher_url_string = uri.toString();

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("continue", TARGET_CHECKOUT_PAGE.toString()));
		nameValuePairs.add(new BasicNameValuePair("service", "sierra"));
		nameValuePairs.add(new BasicNameValuePair("dsh", Long.toString(secret_code)));
		nameValuePairs.add(new BasicNameValuePair("ltmpl", "mobilec"));
		nameValuePairs.add(new BasicNameValuePair("btmpl", "mobile"));
		nameValuePairs.add(new BasicNameValuePair("GALX", secret_galaxy));
		nameValuePairs.add(new BasicNameValuePair("Email", user_pass.getUserName()));
		nameValuePairs.add(new BasicNameValuePair("PersistentCookie", "yes"));
		nameValuePairs.add(new BasicNameValuePair("Passwd", user_pass.getPassword()));


		HttpPost httppost = new HttpPost( cookie_fetcher_url_string );
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}



		try {
			response = httpclient.execute(httppost);

			String response_string = StreamUtils.convertStreamToString(response.getEntity().getContent());

			String uncategorized_template_matcher = "Merchant ID:\\s+(\\d+)";
			Pattern template_pattern = Pattern.compile(uncategorized_template_matcher);
			Matcher classMatcher = template_pattern.matcher(response_string);
			if (classMatcher.find()) {
				credentials.merchant_id = Long.parseLong(classMatcher.group(1));
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		credentials.gdToken = get_gdToken_from_cookies(httpclient);
		return credentials;
	}

	// ========================================================================
	static String get_gdToken_from_cookies(DefaultHttpClient client) {
		for (Cookie cook : client.getCookieStore().getCookies())
			if (cook.getName().equals(GDTOKEN_COOKIE))
				return cook.getValue();

		return null;
	}
}