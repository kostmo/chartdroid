package org.crittr.track.provider;

import org.crittr.track.CalendarPickerConstants;
import org.crittr.track.DatabaseSightings;
import org.crittr.track.Market;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SightingEventContentProvider extends ContentProvider {

	public static final String TAG = Market.DEBUG_TAG;

	public static final String AUTHORITY = "org.crittr.sighting";


	private static final String URI_PREFIX = "content://" + AUTHORITY;

	public static Uri constructUri(String absolute_file_path) {
		Uri uri = Uri.parse(URI_PREFIX + absolute_file_path);
		return uri;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return CalendarPickerConstants.CalendarEventPicker.CONTENT_TYPE_CALENDAR_EVENT;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		DatabaseSightings helper = new DatabaseSightings(getContext());
		return helper.sightingsProvider(projection, selection, selectionArgs, sortOrder);
	}
	

	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}
}
