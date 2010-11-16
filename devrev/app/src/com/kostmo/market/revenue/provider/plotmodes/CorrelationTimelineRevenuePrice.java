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
import com.kostmo.market.revenue.activity.RevenueActivity.BinningMode;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.provider.DatabaseRevenue.DatedMultiSeriesValues;
import com.kostmo.market.revenue.provider.DatabaseRevenue.PriceDate;
import com.kostmo.market.revenue.provider.DatabaseRevenue.SeriesValue;

public class CorrelationTimelineRevenuePrice extends PlotMode {

	
	DateRange truncated_date_range;
	BinningMode binning_mode;
	int bin_count;
	long app_id;

	// ========================================================================
	public CorrelationTimelineRevenuePrice(DatabaseRevenue database, Uri uri) {
		super(database, uri);

		// Strategy:
		// Obtain both the Comments plot and the Revenue plot, then
		// plot them with a primary and secondary Y-axis.

		this.truncated_date_range = database.getTruncatedSalesDateRange(new DateRange(
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_START_MILLISECONDS))),
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_END_MILLISECONDS)))
		));

		// TODO Apply binning mode to all cases!!!
		this.binning_mode = BinningMode.values()[Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_BINNING_MODE))];
		this.bin_count = binning_mode.getBinCount(uri, this.truncated_date_range.getMillisDelta());
		this.app_id = ContentUris.parseId(uri);
	}

	// ========================================================================
	@Override
	public Cursor getAxesCursor() {
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_LABEL,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_ROLE,
				});

		String window_duration_string = this.binning_mode.getDurationString(bin_count, truncated_date_range.getMillisDelta());
		
		String[] axis_labels = new String[] {
			"Date",
			window_duration_string,
			"Price"
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
			"Price"
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
		// Here we must interleave the Price and Revenue series into the Cursor.
		Log.e(TAG, "I am in the right spot with bincount: " + bin_count);
		
		List<DatedMultiSeriesValues> bins = database.generateSingleAppHistogram(truncated_date_range, bin_count, app_id);

		Log.e(TAG, "Actual bincount: " + bins.size());
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				DatabaseRevenue.AXIS_X,
				DatabaseRevenue.AXIS_Y});

		
		int revenue_series_index = 0;
		int price_transitions_series_index = 1;
		
		
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

		
		// Graph price transitions
		List<PriceDate> price_dates = database.getPriceTransitions(app_id, truncated_date_range);
		
		int last_value_cents = 0;
		for (PriceDate price_date : price_dates) {
			last_value_cents = price_date.price_cents;
			c.newRow().add( row_index )
				.add( price_transitions_series_index )
				.add( null )
				.add( price_date.date.getTime() )
				.add( last_value_cents / (float) 100 );
		
			row_index++;
		}
		
		// Add a data point to the end of the date range.
		if (last_value_cents != 0)
			c.newRow().add( row_index )
				.add( price_transitions_series_index )
				.add( null )
				.add( truncated_date_range.end.getTime() )
				.add( last_value_cents / (float) 100 );
		
		return c;
	}
	
	
	// ========================================================================
	public static Uri constructUri(long app_id, DateRange date_range, BinningMode binning_mode, int bins) {
		return ContentUris.withAppendedId(Uri.withAppendedPath(
				UriGenerator.appendDateRange(
						UriGenerator.constructRevenuePriceCorrelationUri(),
						date_range), UriGenerator.URI_PATH_TIMELINE_QUALIFIER),
					app_id)
			.buildUpon()
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_BINNING_MODE, Integer.toString(binning_mode.ordinal()))
			.appendQueryParameter(UriGenerator.QUERY_PARAMETER_HISTOGRAM_BINS, Integer.toString(bins))
			.build();
	}
}