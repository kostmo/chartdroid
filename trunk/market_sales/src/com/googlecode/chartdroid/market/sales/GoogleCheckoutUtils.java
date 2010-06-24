package com.googlecode.chartdroid.market.sales;

import com.googlecode.chartdroid.market.sales.container.DateRange;
import com.googlecode.chartdroid.market.sales.container.HistogramBin;
import com.googlecode.chartdroid.market.sales.container.SpreadsheetRow;
import com.googlecode.chartdroid.market.sales.container.UsernamePassword;
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

import android.net.Uri;
import android.util.Log;

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

public class GoogleCheckoutUtils {

	static final String TAG = Market.TAG;

	public static final String GDTOKEN_COOKIE = "gdToken";
	public static final String SECURE_HTTP_SCHEME = "https";

	public static final Uri TARGET_CHECKOUT_PAGE = new Uri.Builder()
		.scheme(SECURE_HTTP_SCHEME)
		.authority("checkout.google.com")
		.path("sell/orders").build();
	
	public static final String GOOGLE_CHECKOUT_SERVICE_CODENAME = "sierra";

	final static int DOWNLOAD_DAYS_INCREMENT = 31;
	final static SimpleDateFormat ISO8601FORMAT_YMD = new SimpleDateFormat("yyyy-MM-dd");

	float histogram_binwidth_days;
	String error_message;
	
	// ========================================================================
	public String getErrorMessage() {
		return error_message;
	}
	
	// ========================================================================
	public float getHistogramBinwidthDays() {
		return histogram_binwidth_days;
	}
	
	// ========================================================================
	static Date decrementDate(Date old_date, long milliseconds) {
		return new Date(old_date.getTime() - milliseconds);
	}
	
	// ========================================================================
	public List<HistogramBin> generateHistogram(List<SpreadsheetRow> rows, int bin_count) {

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
	public static List<DateRange> planPiecewiseDownload(DateRange date_range) {
		
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
	public List<SpreadsheetRow> getPurchaseRecords(CheckoutCredentials checkout_credentials, DateRange date_range) {
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
			.path("cws/v2/Merchant/" + checkout_credentials.merchant_id + "/reportsForm")
			.toString();

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
					e.printStackTrace();
					this.error_message = response_string.trim();
					return null;
				} catch (Exception e) {
					e.printStackTrace();
					this.error_message = e.getMessage();
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
	public static CheckoutCredentials recoverCheckoutCredentials(UsernamePassword user_pass) {

		CheckoutCredentials credentials = new CheckoutCredentials();

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;

		String secret_galaxy = null;
		long secret_code = 0;
		
		Uri base_google_login_service_uri = new Uri.Builder()
			.scheme(SECURE_HTTP_SCHEME)
			.authority("www.google.com")
			.path("/accounts/ServiceLoginAuth")
			.appendQueryParameter("service", GOOGLE_CHECKOUT_SERVICE_CODENAME)
			.build();

		Uri uri = base_google_login_service_uri.buildUpon()
			.appendQueryParameter("continue", TARGET_CHECKOUT_PAGE.toString())
			.appendQueryParameter("ltmpl", "mobilec")
			.appendQueryParameter("rm", "hide")
			.appendQueryParameter("btmpl", "mobile")
			.build();
		String login_url_string = uri.toString();


		HttpGet httpget = new HttpGet( login_url_string );
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

		String cookie_fetcher_url_string = base_google_login_service_uri.toString();

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("continue", TARGET_CHECKOUT_PAGE.toString()));
		nameValuePairs.add(new BasicNameValuePair("service", GOOGLE_CHECKOUT_SERVICE_CODENAME));
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
