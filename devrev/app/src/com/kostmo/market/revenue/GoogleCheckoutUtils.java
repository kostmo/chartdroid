package com.kostmo.market.revenue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.net.Uri;
import android.util.Log;

import com.kostmo.market.revenue.container.UsernamePassword;
import com.kostmo.tools.StreamUtils;

public class GoogleCheckoutUtils {

	static final String TAG = Market.TAG;

	public static final String GDTOKEN_COOKIE = "gdToken";
	public static final String SECURE_HTTP_SCHEME = "https";

	public static final Uri TARGET_CHECKOUT_PAGE = new Uri.Builder()
		.scheme(SECURE_HTTP_SCHEME)
		.authority("checkout.google.com")
		.path("sell/settings")
		.appendQueryParameter("section", "Integration")
		.build();
	
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
	public static class CheckoutCredentials {
		public long merchant_id;
		public String merchant_key;
		String gdToken;
	}

	// ========================================================================
	public static class MerchantCredentialsNotFoundException extends Exception {
		MerchantCredentialsNotFoundException(String error_message) {
			super(error_message);
		}
	}
	
	// ========================================================================
	public static CheckoutCredentials recoverCheckoutCredentials(UsernamePassword user_pass) throws MerchantCredentialsNotFoundException {

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

			// Scrape for this pattern:
			// Google merchant ID</font><br><b><font size="+0">416811198975823</font></b></td></tr>
//			<tr><td style="padding-bottom:4px;"><font size="-1">Google merchant key</font><br><b><font size="+0">HT0n7_SnLFw402JdpmsyLw
			
			// XXX This looks pretty fragile
			String uncategorized_template_matcher = "Google merchant ID</font><br><b><font size=\"\\+0\">(\\d+)</font></b></td></tr>\\s*<tr><td style=\"padding-bottom:4px;\"><font size=\"-1\">Google merchant key</font><br><b><font size=\"\\+0\">(.*)</font>";
			Pattern template_pattern = Pattern.compile(uncategorized_template_matcher);
			Matcher classMatcher = template_pattern.matcher(response_string);
			if (classMatcher.find()) {
				credentials.merchant_id = Long.parseLong(classMatcher.group(1));
				credentials.merchant_key = classMatcher.group(2);
			} else {
				Log.e(TAG, "Could not find the credentials pattern!");
				throw new MerchantCredentialsNotFoundException("Merchant credentials not found.");
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