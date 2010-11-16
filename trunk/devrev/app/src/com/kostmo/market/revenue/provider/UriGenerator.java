package com.kostmo.market.revenue.provider;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;

import com.kostmo.market.revenue.container.DateRange;

public class UriGenerator {

	// This must be the same as what as specified as the Content Provider authority
	// in the manifest file.
	private static final String AUTHORITY = "com.kostmo.market.revenue.provider";
	public static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).build();
	
	
	public static final String QUERY_PARAMETER_START_MILLISECONDS = "start";
	public static final String QUERY_PARAMETER_END_MILLISECONDS = "end";
	public static final String QUERY_PARAMETER_PUBLISHER_ID = "pubid";
	

	public static final String QUERY_PARAMETER_HISTOGRAM_BINS = "bins";
	public static final String QUERY_PARAMETER_BINNING_MODE = "binmode";
	public static final String QUERY_PARAMETER_WINDOW_SIZE = "window";
	public static final String QUERY_PARAMETER_WINDOW_EVALUATOR = "evaluator";

	// ========================================================================
	public static final String URI_PATH_HISTOGRAM_PLOT = "histogram";
	static final int URI_CASE_REVENUE_TIMELINE_PLOT = 1;
	
	public static final String URI_PATH_REVENUE_TOTALS = "revenue_totals";
	static final int URI_CASE_APP_REVENUE_TOTALS = 2;

	public static final String URI_PATH_COMMENTS_PLOT = "comments";
	static final int URI_CASE_COMMENTS_PLOT = 3;
	
	public static final String URI_PATH_COMMENTS_HISTOGRAM = "comment_histogram";
	static final int URI_CASE_COMMENTS_HISTOGRAM = 4;

	public static final String URI_PATH_CORRELATION_REVENUE_PRICE = "revenue_price_correlation";
	static final int URI_CASE_CORRELATION_REVENUE_PRICE = 5;

	public static final String URI_PATH_CORRELATION_REVENUE_RATINGS = "revenue_ratings_correlation";
	static final int URI_CASE_CORRELATION_REVENUE_RATINGS = 6;
	
	public static final String URI_PATH_TIMELINE_QUALIFIER = "timeline";
	static final int URI_CASE_CORRELATION_TIMELINE_REVENUE_PRICE = 7;
	static final int URI_CASE_CORRELATION_TIMELINE_REVENUE_RATINGS = 8;

	
	// New
	public static final String URI_PATH_REVENUE_TIMELINE_OVERALL = "overall";
	static final int URI_CASE_REVENUE_TIMELINE_OVERALL = 9;
	
	
	// ========================================================================
	public static Uri constructRevenueTotalsUri(DateRange date_range) {
		return appendDateRange(Uri.withAppendedPath(BASE_URI, URI_PATH_REVENUE_TOTALS), date_range);
	}
	
	public static Uri constructRevenuePriceCorrelationUri() {
		return Uri.withAppendedPath(BASE_URI, URI_PATH_CORRELATION_REVENUE_PRICE);
	}
	
	public static Uri constructRevenueRatingsCorrelationUri() {
		return Uri.withAppendedPath(BASE_URI, URI_PATH_CORRELATION_REVENUE_RATINGS);
	}
	
	public static Uri appendDateRange(Uri uri, DateRange date_range) {
		return uri.buildUpon()
			.appendQueryParameter(QUERY_PARAMETER_START_MILLISECONDS, Long.toString( date_range.start.getTime() ))
			.appendQueryParameter(QUERY_PARAMETER_END_MILLISECONDS, Long.toString( date_range.end.getTime() ))
			.build();
	}

	// ========================================================================
	static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, URI_PATH_HISTOGRAM_PLOT, URI_CASE_REVENUE_TIMELINE_PLOT);
		sUriMatcher.addURI(AUTHORITY, URI_PATH_REVENUE_TOTALS, URI_CASE_APP_REVENUE_TOTALS);

		sUriMatcher.addURI(AUTHORITY, URI_PATH_REVENUE_TIMELINE_OVERALL, URI_CASE_REVENUE_TIMELINE_OVERALL);
		
		// XXX Hash symbol does not work for negative numbers
//		sUriMatcher.addURI(AUTHORITY, URI_PATH_COMMENTS_PLOT + "/#", URI_CASE_COMMENTS_PLOT);
		sUriMatcher.addURI(AUTHORITY, URI_PATH_COMMENTS_PLOT + "/*", URI_CASE_COMMENTS_PLOT);
		
		sUriMatcher.addURI(AUTHORITY, URI_PATH_COMMENTS_HISTOGRAM + "/*", URI_CASE_COMMENTS_HISTOGRAM);

		sUriMatcher.addURI(AUTHORITY, URI_PATH_CORRELATION_REVENUE_PRICE + "/" + URI_PATH_TIMELINE_QUALIFIER + "/*", URI_CASE_CORRELATION_TIMELINE_REVENUE_PRICE);
		sUriMatcher.addURI(AUTHORITY, URI_PATH_CORRELATION_REVENUE_RATINGS + "/" + URI_PATH_TIMELINE_QUALIFIER + "/*", URI_CASE_CORRELATION_TIMELINE_REVENUE_RATINGS);
		
		sUriMatcher.addURI(AUTHORITY, URI_PATH_CORRELATION_REVENUE_PRICE + "/*", URI_CASE_CORRELATION_REVENUE_PRICE);
		sUriMatcher.addURI(AUTHORITY, URI_PATH_CORRELATION_REVENUE_RATINGS + "/*", URI_CASE_CORRELATION_REVENUE_RATINGS);
	}
}
