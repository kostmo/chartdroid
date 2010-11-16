package com.kostmo.market.revenue.provider.plotmodes;

import java.util.Date;
import java.util.List;

import android.content.ContentUris;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.activity.AppsOverviewActivity.WindowEvaluatorMode;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.provider.DatabaseRevenue.DatedRatingsWindow;
import com.kostmo.market.revenue.provider.DatabaseRevenue.RatedRevenueDuration;

public class CorrelationAbsoluteRevenueRatings extends PlotMode {

	DateRange date_range;
	long app_id;
	
	int window_size;
	WindowEvaluatorMode evaluator;

	// ========================================================================
	public CorrelationAbsoluteRevenueRatings(DatabaseRevenue database, Uri uri) {
		super(database, uri);

		// Strategy:
		// Obtain both the Comments plot and the Revenue plot, then
		// plot them with a primary and secondary Y-axis.

		date_range = new DateRange(
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_START_MILLISECONDS))),
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_END_MILLISECONDS)))
		);
		
		this.app_id = ContentUris.parseId(uri);

		this.window_size = Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_SIZE));
		this.evaluator = WindowEvaluatorMode.values()[Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_EVALUATOR))];
	}

	// ========================================================================
	@Override
	public Cursor getAxesCursor() {
		// Don't need this; I supply the axis labels through the Intent.
		return null;
	}

	// ========================================================================
	@Override
	public Cursor getSeriesCursor() {
		// Don't need this; I supply the series label through the Intent.
		return null;
	}

	// ========================================================================
	@Override
	public Cursor getDataCursor() {
		
		List<DatedRatingsWindow> windows = database.genWindowedRatingAverages(app_id, window_size, date_range);
		
		List<RatedRevenueDuration> spans = database.getRevenueRatingsCorrelation(app_id, date_range, windows, evaluator);
		Log.d(TAG, "Found " + spans.size() + " separate comment value spans.");
		
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				DatabaseRevenue.AXIS_X,
				DatabaseRevenue.AXIS_Y
			});


		int row_index = 0;
		for (RatedRevenueDuration span : spans) {
			c.newRow().add( row_index ).add( 0 ).add( null )
				.add( span.getRating() ).add( span.getDailyRevenueDollars() );

			row_index++;
		}
		
		return c;
	}
	

	// ========================================================================
	public static Uri constructUri(
			long app_id,
			int window,
			DateRange date_range,
			WindowEvaluatorMode window_evaluator) {
		
		Uri base_uri = ContentUris.withAppendedId(UriGenerator.appendDateRange(UriGenerator.constructRevenueRatingsCorrelationUri(), date_range), app_id);
		return base_uri.buildUpon()
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_EVALUATOR, Integer.toString(window_evaluator.ordinal()))
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_SIZE, Integer.toString(window))
			.build();
	}
}