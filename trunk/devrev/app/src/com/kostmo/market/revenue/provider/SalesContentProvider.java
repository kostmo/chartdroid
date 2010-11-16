package com.kostmo.market.revenue.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.provider.plotmodes.CommentsHistogram;
import com.kostmo.market.revenue.provider.plotmodes.CommentsPlot;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationAbsoluteRevenuePrice;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationAbsoluteRevenueRatings;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationTimelineRevenuePrice;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationTimelineRevenueRatings;
import com.kostmo.market.revenue.provider.plotmodes.RevenueMultiTimeline;
import com.kostmo.market.revenue.provider.plotmodes.RevenueTimelineOverall;
import com.kostmo.market.revenue.provider.plotmodes.RevenueTotals;

public class SalesContentProvider extends ContentProvider {

	static final String TAG = "SalesContentProvider";

	static final String MESSAGE_UNSUPPORTED_FEATURE = "Not supported by this provider";

	// ========================================================================
	@Override
	public boolean onCreate() {
		return true;
	}

	// ========================================================================
	@Override
	public String getType(Uri uri) {

		Log.i(TAG, "getType() Uri: " + uri);
		
		int match = UriGenerator.sUriMatcher.match(uri);
		Log.d(TAG, "getType() UriMatcher match: " + match);

		switch (match) {
		case UriGenerator.URI_CASE_REVENUE_TIMELINE_PLOT:
		case UriGenerator.URI_CASE_COMMENTS_PLOT:
		case UriGenerator.URI_CASE_CORRELATION_TIMELINE_REVENUE_PRICE:
		case UriGenerator.URI_CASE_CORRELATION_TIMELINE_REVENUE_RATINGS:
		case UriGenerator.URI_CASE_REVENUE_TIMELINE_OVERALL:
		{
			return ColumnSchema.EventData.CONTENT_TYPE_PLOT_DATA;
		}
		case UriGenerator.URI_CASE_APP_REVENUE_TOTALS:
		case UriGenerator.URI_CASE_CORRELATION_REVENUE_PRICE:
		case UriGenerator.URI_CASE_CORRELATION_REVENUE_RATINGS:
		{
			return ColumnSchema.PlotData.CONTENT_TYPE_PLOT_DATA;
		}
		default:
			Log.w(TAG, "getType(): Failed all matching tests!");
		}
		return ColumnSchema.EventData.CONTENT_TYPE_PLOT_DATA;
	}

	// ========================================================================
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		Log.i(TAG, "query() Uri: " + uri);
		
		DatabaseRevenue database = new DatabaseRevenue(getContext());
		
		int match = UriGenerator.sUriMatcher.match(uri);
		Log.d(TAG, "query() UriMatcher match: " + match);
		
		PlotMode plot_mode = null;
		switch (match) {
		case UriGenerator.URI_CASE_CORRELATION_TIMELINE_REVENUE_PRICE:
		{
			plot_mode = new CorrelationTimelineRevenuePrice(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_CORRELATION_TIMELINE_REVENUE_RATINGS:
		{
			plot_mode = new CorrelationTimelineRevenueRatings(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_CORRELATION_REVENUE_RATINGS:
		{
			plot_mode = new CorrelationAbsoluteRevenueRatings(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_CORRELATION_REVENUE_PRICE:
		{
			plot_mode = new CorrelationAbsoluteRevenuePrice(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_COMMENTS_HISTOGRAM:
		{
			plot_mode = new CommentsHistogram(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_COMMENTS_PLOT:
		{
			plot_mode = new CommentsPlot(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_REVENUE_TIMELINE_OVERALL:
		{
			plot_mode = new RevenueTimelineOverall(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_REVENUE_TIMELINE_PLOT:
		{
			Log.d(TAG, "I am in the revenue timeline plot.");
			plot_mode = new RevenueMultiTimeline(database, uri);
			break;
		}
		case UriGenerator.URI_CASE_APP_REVENUE_TOTALS:
		{
			plot_mode = new RevenueTotals(database, uri);
			break;
		}
		default:
			Log.w(TAG, "query(): Failed all matching tests!");
			return null;
		}
		
		if (plot_mode != null) return plot_mode.getPlotCursor();
		else return null;
	}

	// ========================================================================
	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	// ========================================================================
	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	// ========================================================================
	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}
}
