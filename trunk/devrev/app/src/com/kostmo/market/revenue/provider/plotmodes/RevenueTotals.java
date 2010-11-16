package com.kostmo.market.revenue.provider.plotmodes;

import java.util.Date;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.activity.RevenueActivity.BinningMode;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;

public class RevenueTotals extends PlotMode {

	public static String[] DEFAULT_AXES_LABELS = { "Date", "Sales" };
	public static String[] DEFAULT_SERIES_LABELS = { "Everything" };

	DateRange date_range;
	long publisher_id;

	// ========================================================================
	public RevenueTotals(DatabaseRevenue database, Uri uri) {
		super(database, uri);

		// Strategy:
		// Obtain both the Comments plot and the Revenue plot, then
		// plot them with a primary and secondary Y-axis.

		this.date_range = new DateRange(
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_START_MILLISECONDS))),
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_END_MILLISECONDS)))
		);
		
		this.publisher_id = Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_PUBLISHER_ID));
	}

	// ========================================================================
	@Override
	public Cursor getAxesCursor() {
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_LABEL});

		int row_index = 0;
		for (int i=0; i<DEFAULT_AXES_LABELS.length; i++) {

			c.newRow().add( row_index ).add( DEFAULT_AXES_LABELS[i] );
			row_index++;
		}

		return c;
	}

	// ========================================================================
	@Override
	public Cursor getSeriesCursor() {

		Log.i(TAG, "Date range of interest: " + this.date_range);
		
		Cursor cursor = database.getSeriesLabelsForRevenueByItem(this.publisher_id, this.date_range);
		Log.i(TAG, "Series rowcount: " + cursor.getCount());
		return cursor;

	}

	// ========================================================================
	@Override
	public Cursor getDataCursor() {
		// Fetch the actual data
	    SQLiteDatabase db = this.database.getReadableDatabase();

		Cursor cursor = this.database.getRevenueByItemPlottableCursor(db, this.publisher_id, this.date_range);
	    Log.e(TAG, "Data cursor row count: " + cursor.getCount());
	    
		return cursor;
	}
	
	
	// ========================================================================
	public static Uri constructUri(DateRange date_range, BinningMode binning_mode, int bins, long publisher_id) {
		return UriGenerator.appendDateRange(Uri.withAppendedPath(UriGenerator.BASE_URI, UriGenerator.URI_PATH_REVENUE_TIMELINE_OVERALL), date_range)
		.buildUpon()
		.appendQueryParameter(UriGenerator.QUERY_PARAMETER_BINNING_MODE, Integer.toString(binning_mode.ordinal()))
		.appendQueryParameter(UriGenerator.QUERY_PARAMETER_HISTOGRAM_BINS, Integer.toString(bins))
		.appendQueryParameter(UriGenerator.QUERY_PARAMETER_PUBLISHER_ID, Long.toString(publisher_id))
		.build();
	}
}