package com.kostmo.flickr.provider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotosInterface;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.tools.StreamUtils;

public class ExperimentalFileContentProvider extends ContentProvider {

	static final String TAG = "ExperimentalFileContentProvider";

	static final String SOCKET_IMAGES_PATH_SEGMENT = "socket_images";
	static final String IMAGES_PATH_SEGMENT = "images";
	
	static final String QUERY_PARAMETER_KEY_DOWNLOAD_URL = "u";
	
	static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(FlickrPhoto.AUTHORITY).path(IMAGES_PATH_SEGMENT).build();
	static Uri SOCKET_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(FlickrPhoto.AUTHORITY).path(SOCKET_IMAGES_PATH_SEGMENT).build();

	public static Uri constructMetaInfoUri(long photo_id) {
		return ContentUris.withAppendedId(BASE_URI, photo_id);
	}
	
	public static Uri constructDownloaderUri(long photo_id, String url) {
		return constructMetaInfoUri(photo_id).buildUpon().appendQueryParameter(QUERY_PARAMETER_KEY_DOWNLOAD_URL, url).build();
	}

	public static Uri constructSocketUri(long photo_id) {
		return ContentUris.withAppendedId(SOCKET_URI, photo_id);
	}

	
	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode) {

		// This way avoids saving the file to disk (download stream is used directly)
		if (uri.getPathSegments().contains(SOCKET_IMAGES_PATH_SEGMENT)) {
			
			long photo_id = ContentUris.parseId(uri);
			Log.d(TAG, "Fetching photo_id: " + photo_id);

			Photo photo = getPhotoInfo(photo_id);
			String small_square_url = photo.getSmallSquareUrl();


			URL u = null;
			try {
				u = new URL(small_square_url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
			
			Log.e(TAG, "Returning special socket stream.");
			
			Socket s = convertURLtoSocket(u);
	
			/*
				HttpParams basic_params = new BasicHttpParams();
				SessionInputBuffer buffer;
				try {
					buffer = new SocketInputBuffer(s, 1024, basic_params);
					HttpResponseParser hrp = new HttpResponseParser(buffer, BasicLineParser.DEFAULT , new DefaultHttpResponseFactory(), basic_params);
					HttpMessage hm = hrp.parse();
					String content_length = hm.getFirstHeader("Content-Length").getValue();
	
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (HttpException e1) {
					e1.printStackTrace();
				}
			 */
	
	
			String header_string = advance_past_header(s);
			//			Log.d(TAG, "HEADER: " + header_string);
			long content_length = parseContentLength(header_string);
	
			ParcelFileDescriptor x = ParcelFileDescriptor.fromSocket(s);
			return new AssetFileDescriptor(x, 0, content_length);
			//	        return x;
			

		// This is the normal way to do it.
		} else {

			String download_url = uri.getQueryParameter(QUERY_PARAMETER_KEY_DOWNLOAD_URL);
			
			Log.e(TAG, "Returning normal file stream.");
			
			try {
				File temp_file = File.createTempFile("flickr", ".jpg");
				OutputStream os = new BufferedOutputStream( new FileOutputStream(temp_file) );
				InputStream is = new BufferedInputStream(new URL(download_url).openStream());
				int length = StreamUtils.copy(is, os);
				os.flush();
				os.close();
				is.close();
				
				ParcelFileDescriptor x = ParcelFileDescriptor.open(temp_file, ParcelFileDescriptor.MODE_READ_ONLY);
				return new AssetFileDescriptor(x, 0, length);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private long parseContentLength(String header_string) {
		// Use a regular expression to determine the content length.
		long content_length = AssetFileDescriptor.UNKNOWN_LENGTH;
		String matcher_string = "Content-Length:\\s*(\\d+)";
		Pattern matcher_pattern = Pattern.compile(matcher_string, Pattern.CASE_INSENSITIVE);
		Matcher classMatcher = matcher_pattern.matcher(header_string);
		if (classMatcher.find()) {
			//	        	Log.e(TAG, "Entire match: " + classMatcher.group());
			String content_length_string = classMatcher.group(1);
			content_length = Long.parseLong(content_length_string);
			Log.e(TAG, "Found content length: " + content_length);

		} else {
			Log.e(TAG, "Could not find content length!!!");
		}
		return content_length;
	}


	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}
	/*
	   @Override
	   public String getType(Uri uri) {
		   return "image/*";
	   }
	 */


	private static final UriMatcher sUriMatcher;
	static {

		// TODO
		/*

 FLICKR_PHOTO_AUTHORITY.equals(data_authority) || COMMONS_PHOTO_AUTHORITY.equals(data_authority)
		 */
		
		
		
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(FlickrPhoto.AUTHORITY, IMAGES_PATH_SEGMENT + "/#", FlickrPhoto.PhotoRetrieval.FLICKR_PHOTO_INFO_SINGLE);
		sUriMatcher.addURI(FlickrPhoto.AUTHORITY, IMAGES_PATH_SEGMENT + "/by_owner/*", FlickrPhoto.PhotoRetrieval.FLICKR_PHOTO_INFO_MULTIPLE);
		

	}

	@Override
	public String getType(Uri uri) {

		switch ( sUriMatcher.match(uri) )
		{
		case FlickrPhoto.PhotoRetrieval.FLICKR_PHOTO_INFO_SINGLE:
			return FlickrPhoto.PhotoRetrieval.CONTENT_TYPE_ITEM_HOSTED_IMAGE;
		case FlickrPhoto.PhotoRetrieval.FLICKR_PHOTO_INFO_MULTIPLE:
			return FlickrPhoto.PhotoRetrieval.CONTENT_TYPE_HOSTED_IMAGE;

		default:
			return null;
		}
	}


	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}

	@Override
	public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
		
		Log.d(TAG, "Passed URI: " + uri);

		int match = sUriMatcher.match(uri);
		Log.e(TAG, "UriMatcher match index: " + match);
		switch (match) {

		case FlickrPhoto.PhotoRetrieval.FLICKR_PHOTO_INFO_SINGLE:
		{
			MatrixCursor c = new MatrixCursor(new String[] {
					BaseColumns._ID,
					FlickrPhoto.PhotoRetrieval.COLUMN_DATE,
					FlickrPhoto.PhotoRetrieval.COLUMN_OWNER,
					FlickrPhoto.PhotoRetrieval.COLUMN_TITLE,
					FlickrPhoto.PhotoRetrieval.COLUMN_DESCRIPTION,
					FlickrPhoto.PhotoRetrieval.COLUMN_LAT,
					FlickrPhoto.PhotoRetrieval.COLUMN_LON,
					FlickrPhoto.PhotoRetrieval.COLUMN_THUMBNAIL_URL});

			long photo_id = ContentUris.parseId(uri);
			Photo photo = getPhotoInfo(photo_id);

			RowBuilder row_builder = c.newRow().add(photo_id)
			.add( photo.getDatePosted().getTime() )
			.add( photo.getOwner().getId() )
			.add( photo.getTitle() )
			.add( photo.getDescription() );
			
			GeoData flickr_geo_data = photo.getGeoData();
			if (flickr_geo_data != null) {
				row_builder.add( flickr_geo_data.getLatitude() )
				.add( flickr_geo_data.getLongitude() );
			} else {
				row_builder.add( null )
				.add( null );
			}
			
			row_builder.add( photo.getThumbnailUrl() );

			return c;
		}
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}

	// ====================================

	Socket convertURLtoSocket(URL u) {
		try {
			if (!u.getProtocol().equalsIgnoreCase("http")) {
				System.err.println("Sorry, " + u.getProtocol() + 
				" is not yet supported.");
				return null;
			}

			String host = u.getHost();
			int port = u.getPort();
			String file = u.getFile();
			// default port
			if (port <= 0) port = 80;

			Socket s = new Socket(host, port);

			String user_agent_string = "Android";
			String accept_types_string = "image/*";

			// NOTE: HTTP 1.0 headers MUST be used to properly pass an EOF!
			// If HTTTP 1.1 headers are used, it doesn't know when the file ends!!!
			String request = "GET " + file + " HTTP/1.0\r\n"
			+ "Host: " + host + "\r\n"	// XXX - This is mandatory on wikimedia.org...
			+ "User-Agent: " + user_agent_string + "\r\n"
			+ "Accept: " + accept_types_string + "\r\n"
			+ "\r\n";

			byte[] b = request.getBytes();
			OutputStream out = s.getOutputStream();
			out.write(b);
			out.flush();




			//		        in.close();
			//		        out.close();
			//		        s.close();

			return s;
		}
		catch (IOException e) {System.err.println(e);} 
		return null;
	}


	// ====================================
	String advance_past_header(Socket s) {

		StringBuilder builder = new StringBuilder();
		try {
			InputStream in = s.getInputStream();

			Log.d(TAG, "ContentProvider InputStream: " + in);


			int position = 0;
			int consecutive_n_count = 0;
			boolean r_last = false;
			boolean n_last = false;
			while (true) {

				int current_char = in.read();
				position++;

				boolean r_current = current_char == '\r';
				boolean n_current = current_char == '\n';

				if (r_current != n_current && r_current != r_last) {
					consecutive_n_count++;
				} else consecutive_n_count = 0;
				builder.append((char) current_char);

				if (consecutive_n_count >= 4) return builder.toString();

				r_last = r_current;
				n_last = n_current;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}
	// ====================================
	Photo getPhotoInfo(long photo_id) {

		Flickr flickr = null;
		try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
					new REST()
			);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		Auth auth = new Auth();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
		auth.setToken( stored_auth_token );
		auth.setPermission(Permission.READ);

		RequestContext requestContext = RequestContext.getRequestContext();
		requestContext.setAuth(auth);

		PhotosInterface photoInt = flickr.getPhotosInterface();
		Photo photo = null;
		try {
			photo = photoInt.getPhoto( Long.toString(photo_id) );
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (FlickrException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
		return photo;
	}
}
