package com.gc.android.market.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;

import android.net.Uri;

import com.gc.android.market.api.model.Market.AppsRequest;
import com.gc.android.market.api.model.Market.AppsResponse;
import com.gc.android.market.api.model.Market.CategoriesRequest;
import com.gc.android.market.api.model.Market.CategoriesResponse;
import com.gc.android.market.api.model.Market.CommentsRequest;
import com.gc.android.market.api.model.Market.CommentsResponse;
import com.gc.android.market.api.model.Market.GetImageRequest;
import com.gc.android.market.api.model.Market.GetImageResponse;
import com.gc.android.market.api.model.Market.Request;
import com.gc.android.market.api.model.Market.RequestContext;
import com.gc.android.market.api.model.Market.Response;
import com.gc.android.market.api.model.Market.ResponseContext;
import com.gc.android.market.api.model.Market.Request.RequestGroup;
import com.gc.android.market.api.model.Market.Response.ResponseGroup;
import com.kostmo.tools.StreamUtils;

/**
 * MarketSession session = new MarketSession();
 * session.login(login,password);
 * session.append(xxx,yyy);
 * session.append(xxx,yyy);
 * ...
 * session.flush();
 */
public class MarketSession {


	static final String TAG = "MarketSession";

	public static interface Callback<T> {

		public void onResult(ResponseContext context, T response);
	}

	public static final int PROTOCOL_VERSION = 2;
	Request.Builder request = Request.newBuilder();
	RequestContext.Builder context = RequestContext.newBuilder();
	public RequestContext.Builder getContext() {
		return context;
	}

	List<Callback<?>> callbacks = new Vector<Callback<?>>(); 
	String authSubToken = null;

	public String getAuthSubToken() {
		return authSubToken;
	}

	public MarketSession(String android_id) {
		context.setUnknown1(0);
		context.setVersion(1002);
		context.setAndroidId( android_id );
		setLocale(Locale.getDefault());
		context.setDeviceAndSdkVersion("sapphire:7");
		setOperatorTMobile();
	}

	public void setLocale(Locale locale) {
		context.setUserLanguage(locale.getLanguage().toLowerCase());
		context.setUserCountry(locale.getCountry().toLowerCase());
	}

	public void setOperator(String alpha, String numeric) {
		setOperator(alpha, alpha, numeric, numeric);
	}

	public void setOperatorTMobile() {
		setOperator("T-Mobile", "310260");
	}

	public void setOperatorSFR() {
		setOperator("F SFR", "20810");
	}

	public void setOperatorO2() {
		setOperator("o2 - de", "26207");
	}

	public void setOperatorSimyo() {
		setOperator("E-Plus", "simyo", "26203", "26203");
	}

	public void setOperatorSunrise() {
		setOperator("sunrise", "22802");
	}

	/**
	 * http://www.2030.tk/wiki/Android_market_switch
	 */
	public void setOperator(String alpha, String simAlpha, String numeric, String simNumeric) {
		context.setOperatorAlpha(alpha);
		context.setSimOperatorAlpha(simAlpha);
		context.setOperatorNumeric(numeric);
		context.setSimOperatorNumeric(simNumeric);
	}

	public void setAuthSubToken(String authSubToken) {
		context.setAuthSubToken(authSubToken);
		this.authSubToken = authSubToken; 
	}

	public void append(AppsRequest requestGroup, Callback<AppsResponse> responseCallback) {
		request.addRequestGroup(RequestGroup.newBuilder().setAppsRequest(requestGroup));
		callbacks.add(responseCallback);
	}

	public void append(GetImageRequest requestGroup, Callback<GetImageResponse> responseCallback) {
		request.addRequestGroup(RequestGroup.newBuilder().setImageRequest(requestGroup));
		callbacks.add(responseCallback);
	}

	public void append(CommentsRequest requestGroup, Callback<CommentsResponse> responseCallback) {
		request.addRequestGroup(RequestGroup.newBuilder().setCommentsRequest(requestGroup));
		callbacks.add(responseCallback);
	}

	public void append(CategoriesRequest requestGroup, Callback<CategoriesResponse> responseCallback) {
		request.addRequestGroup(RequestGroup.newBuilder().setCategoriesRequest(requestGroup));
		callbacks.add(responseCallback);
	}

	@SuppressWarnings("unchecked")
	public void flush() throws IOException {
		RequestContext ctxt = context.build();
		context = RequestContext.newBuilder(ctxt);
		request.setContext(ctxt);
		Response resp = executeProtobuf(request.build());
		int i = 0;
		for(ResponseGroup grp : resp.getResponseGroupList()) {
			Object val = null;
			if(grp.hasAppsResponse())
				val = grp.getAppsResponse();
			if(grp.hasCategoriesResponse())
				val = grp.getCategoriesResponse();
			if(grp.hasCommentsResponse())
				val = grp.getCommentsResponse();
			if(grp.hasImageResponse())
				val = grp.getImageResponse();
			((Callback)callbacks.get(i)).onResult(grp.getContext(), val);
			i++;
		}
		request = Request.newBuilder();
		callbacks.clear();
	}

	public ResponseGroup execute(RequestGroup requestGroup) throws IOException {
		RequestContext ctxt = context.build();
		context = RequestContext.newBuilder(ctxt);
		request.setContext(ctxt);
		Response resp = executeProtobuf(request.addRequestGroup(requestGroup).setContext(ctxt).build());
		return resp.getResponseGroup(0);
	}

	private Response executeProtobuf(Request request) throws IOException {
		byte[] requestBytes = request.toByteArray();
		byte[] responseBytes = executeRawHttpQuery(requestBytes);
		try {
			return Response.parseFrom(responseBytes);
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private byte[] executeRawHttpQuery(byte[] request) throws IOException {

		Uri request_uri = new Uri.Builder()
			.scheme("http")
			.authority("android.clients.google.com")
			.path("market/api/ApiRequest").build();

		HttpPost httppost = new HttpPost( request_uri.toString() );
		httppost.setHeader("User-Agent", "Android-Market/2 (sapphire PLAT-RC33); gzip");


		List<NameValuePair> name_values = new ArrayList<NameValuePair>();
		name_values.add(new BasicNameValuePair("version", Integer.toString(PROTOCOL_VERSION)));
		name_values.add(new BasicNameValuePair("request", Base64.encodeBytes(request)));
		try {
			httppost.setEntity(new UrlEncodedFormEntity(name_values));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}


		DefaultHttpClient httpclient = new DefaultHttpClient();
		BasicClientCookie cookie = new BasicClientCookie("ANDROID", this.getAuthSubToken());
		cookie.setDomain( request_uri.getHost() );
		httpclient.getCookieStore().addCookie(cookie);

		HttpResponse response = null;
		try {
			response = httpclient.execute(httppost);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		}

//		Log.d(TAG, "Status code: " + response.getStatusLine().getStatusCode());
//		Log.d(TAG, "Reason: " + response.getStatusLine().getReasonPhrase());

		InputStream is = null;
		try {
			is = response.getEntity().getContent();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}


		GZIPInputStream gzIs = new GZIPInputStream(is);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream bis = new BufferedInputStream(gzIs);
		OutputStream os = new BufferedOutputStream(bos);
		StreamUtils.copy(bis, os);

		bis.close();
		os.close();

		return bos.toByteArray();
	}
}
