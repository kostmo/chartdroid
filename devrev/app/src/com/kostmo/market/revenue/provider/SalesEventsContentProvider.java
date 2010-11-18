package com.kostmo.market.revenue.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.kostmo.market.revenue.CalendarPickerConstants;

public class SalesEventsContentProvider extends ContentProvider {

	public static final String TAG = "SalesEventsContentProvider";

	public static final String AUTHORITY = "com.kostmo.market.revenue.events.provider";


	private static final String URI_PREFIX = "content://" + AUTHORITY;

	public static final String URI_PATH_AGGREGATE_EVENTS = "aggregate";
	public static final String URI_PATH_INDIVIDUAL_EVENTS = "events";
	
	
	public static final String COLUMN_QUANTITY0 = "REVENUE";
	public static final String COLUMN_QUANTITY1 = "SALES_COUNT";
	
	public static Uri constructUri() {
		Uri uri = Uri.parse(URI_PREFIX);
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
		DatabaseRevenue helper = new DatabaseRevenue(getContext());
		
		if (uri.getPathSegments().contains(URI_PATH_AGGREGATE_EVENTS))
			return helper.getCalendarSalesEventsGrouped(projection, selection, selectionArgs, sortOrder);
		else
			return helper.getCalendarSalesEvents(projection, selection, selectionArgs, sortOrder);
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
