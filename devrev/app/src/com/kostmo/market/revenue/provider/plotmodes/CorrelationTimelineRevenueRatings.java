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
import com.kostmo.market.revenue.activity.RevenueActivity.BinningMode;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.provider.DatabaseRevenue.DatedMultiSeriesValues;
import com.kostmo.market.revenue.provider.DatabaseRevenue.DatedRating;
import com.kostmo.market.revenue.provider.DatabaseRevenue.DatedRatingsWindow;
import com.kostmo.market.revenue.provider.DatabaseRevenue.SeriesValue;

public class CorrelationTimelineRevenueRatings extends PlotMode {

	
	DateRange truncated_date_range;
	BinningMode binning_mode;
	int bin_count;
	long app_id;
	
	int window_size;
	WindowEvaluatorMode evaluator;

	// ========================================================================
	public CorrelationTimelineRevenueRatings(DatabaseRevenue database, Uri uri) {
		super(database, uri);

		// Strategy:
		// Obtain both the Comments plot and the Revenue plot, then
		// plot them with a primary and secondary Y-axis.

		this.truncated_date_range = database.getTruncatedSalesDateRange(new DateRange(
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_START_MILLISECONDS))),
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_END_MILLISECONDS)))
		));

		this.binning_mode = BinningMode.values()[Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_BINNING_MODE))];
		this.bin_count = binning_mode.getBinCount(uri, this.truncated_date_range.getMillisDelta());
		this.app_id = ContentUris.parseId(uri);

		this.window_size = Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_SIZE));
		this.evaluator = WindowEvaluatorMode.values()[Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_EVALUATOR))];
	}

	// ========================================================================
	@Override
	public Cursor getAxesCursor() {
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_LABEL,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_ROLE,
				});

		String[] axis_labels = new String[] {
			"Date",
			binning_mode.getDurationString(bin_count, truncated_date_range.getMillisDelta()),
			String.format(evaluator.shortname + " Rating, %d-comment window", window_size)
		};

		
	
		int[] axis_expressions = new int[] {
				ColumnSchema.Aspect.Axes.AxisExpressionMethod.HORIZONTAL_AXIS.ordinal(),
				ColumnSchema.Aspect.Axes.AxisExpressionMethod.VERTICAL_AXIS.ordinal(),
				ColumnSchema.Aspect.Axes.AxisExpressionMethod.VERTICAL_AXIS.ordinal()
		};
		
		int row_index = 0;
		for (int i=0; i<axis_labels.length; i++) {
			c.newRow().add( row_index ).add( axis_labels[i] ).add( axis_expressions[i] );
			row_index++;
		}

		return c;
	}

	// ========================================================================
	@Override
	public Cursor getSeriesCursor() {
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Series.COLUMN_SERIES_LABEL,
				ColumnSchema.Aspect.Series.COLUMN_SERIES_AXIS_SELECT});

		String[] series_labels = new String[] {
			database.getAppTitle(app_id),
			"Ratings"
		};
		
		int row_index = 0;
		for (int i=0; i<series_labels.length; i++) {
			c.newRow().add( row_index ).add( series_labels[i] ).add( row_index );
			row_index++;
		}

		return c;
	}
	

	// ========================================================================
	@Override
	public Cursor getDataCursor() {

		// Here we must interleave the Comments and Revenue series into the Cursor.
		Log.e(TAG, "I am in the right spot with requested bincount: " + bin_count);
		
		List<DatedMultiSeriesValues> bins = database.generateSingleAppHistogram(truncated_date_range, bin_count, app_id);

		Log.e(TAG, "Actual bincount: " + bins.size());
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				DatabaseRevenue.AXIS_X,
				DatabaseRevenue.AXIS_Y});

		
		int revenue_series_index = 0;
		int ratings_series_index = 1;
		
		
		int row_index = 0;
		for (DatedMultiSeriesValues bin : bins) {

			for (SeriesValue series_value : bin.multi_series) {
				c.newRow().add( row_index )
					.add( revenue_series_index )
					.add( null )
					.add( bin.date.getTime() )
					.add( series_value.value.floatValue() );
				
				row_index++;
			}
		}

		
		
		
		
		
		// Add a data point to the beginning of the date range.
		DatedRating last_rating = database.getLastRatingBefore(app_id, truncated_date_range.start);

		float last_value = 0;
		if (last_rating != null) {
			
			last_value = last_rating.rating;
			
			c.newRow().add( row_index )
			.add( ratings_series_index )
			.add( null )
			.add( truncated_date_range.start.getTime() )
			.add( last_value );
			
			row_index++;
		}
		
		// Retrieve the actual rated/dated comments from our cached database
		List<DatedRatingsWindow> windows = database.genWindowedRatingAverages(app_id, window_size, truncated_date_range);

		for (DatedRatingsWindow window : windows) {

			last_value = window.getValue(evaluator);
			
			c.newRow().add( row_index )
				.add( ratings_series_index )
				.add( null )
				.add( window.getDate().getTime() )
				.add( last_value );
		
			row_index++;
		}
		
		// Add a data point to the end of the date range.
		if (last_value != 0)
			c.newRow().add( row_index )
				.add( ratings_series_index )
				.add( null )
				.add( truncated_date_range.end.getTime() )
				.add( last_value );
		
		return c;
	}
	
	// ========================================================================
	public static Uri constructUri(
			long app_id,
			int window,
			DateRange date_range,
			WindowEvaluatorMode window_evaluator,
			BinningMode binning_mode,
			int bins) {

		Uri base_uri = UriGenerator.appendDateRange(UriGenerator.constructRevenueRatingsCorrelationUri(), date_range).buildUpon()
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_EVALUATOR, Integer.toString(window_evaluator.ordinal()))
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_SIZE, Integer.toString(window))
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_BINNING_MODE, Integer.toString(binning_mode.ordinal()))
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_HISTOGRAM_BINS, Integer.toString(bins))
			.build();

		return ContentUris.withAppendedId(Uri.withAppendedPath(base_uri, UriGenerator.URI_PATH_TIMELINE_QUALIFIER), app_id);
	}
}