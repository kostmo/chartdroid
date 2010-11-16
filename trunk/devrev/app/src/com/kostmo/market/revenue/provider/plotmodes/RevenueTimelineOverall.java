package com.kostmo.market.revenue.provider.plotmodes;

import java.util.Date;
import java.util.List;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.activity.RevenueActivity.BinningMode;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.provider.DatabaseRevenue.DatedValue;

public class RevenueTimelineOverall extends PlotMode {
	
	DateRange truncated_date_range;
	BinningMode binning_mode;
	int bin_count;
	long app_id;

	// ========================================================================
	public RevenueTimelineOverall(DatabaseRevenue database, Uri uri) {
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
	}

	// ========================================================================
	@Override
	public Cursor getAxesCursor() {

		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_LABEL});
		
		String[] axis_labels = new String[] {
			"Date",
			binning_mode.getDurationString(bin_count, truncated_date_range.getMillisDelta())
		};
		
		int row_index = 0;
		for (int i=0; i<axis_labels.length; i++) {
			c.newRow().add( row_index ).add( axis_labels[i] );
			row_index++;
		}

		return c;
	}

	// ========================================================================
	@Override
	public Cursor getSeriesCursor() {

		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Series.COLUMN_SERIES_LABEL});
		
		c.newRow().add( 0 ).add( "All Apps" );
		return c;
	}

	// ========================================================================
	@Override
	public Cursor getDataCursor() {

		List<DatedValue> bins = database.generateOverallHistogram(truncated_date_range, bin_count);
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				DatabaseRevenue.AXIS_X,
				DatabaseRevenue.AXIS_Y});

		int row_index = 0;
	
		for (DatedValue series_value : bins) {
			c.newRow().add( row_index )
				.add( 0 )
				.add( null )
				.add( series_value.date.getTime() )
				.add( series_value.value.floatValue() );

			row_index++;
		}

		return c;
	}
}