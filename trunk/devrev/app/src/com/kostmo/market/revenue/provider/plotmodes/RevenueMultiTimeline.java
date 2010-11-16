package com.kostmo.market.revenue.provider.plotmodes;

import java.util.Date;
import java.util.List;

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
import com.kostmo.market.revenue.provider.DatabaseRevenue.SeriesValue;

public class RevenueMultiTimeline extends PlotMode {
	
	DateRange truncated_date_range;
	BinningMode binning_mode;
	int bin_count;
	long app_id;
	long publisher_id;

	// ========================================================================
	public RevenueMultiTimeline(DatabaseRevenue database, Uri uri) {
		super(database, uri);

		this.truncated_date_range = database.getTruncatedSalesDateRange(new DateRange(
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_START_MILLISECONDS))),
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_END_MILLISECONDS)))
		));

		this.binning_mode = BinningMode.values()[Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_BINNING_MODE))];

		long millis_delta = this.truncated_date_range.getMillisDelta();
		this.bin_count = binning_mode.getBinCount(uri, millis_delta);
		this.publisher_id = Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_PUBLISHER_ID));
	}

	// ========================================================================
	@Override
	public Cursor getAxesCursor() {

		Log.d(TAG, "Getting axes cursor...");
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_LABEL});
		
		String[] axis_labels = new String[] {
			"Date",
			this.binning_mode.getDurationString(this.bin_count, this.truncated_date_range.getMillisDelta())
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

		Log.d(TAG, "Getting series cursor...");
		
		Cursor cursor = this.database.getSeriesLabelsForRevenueByItem(this.publisher_id, this.truncated_date_range);
		
		Log.e(TAG, "Series labels cursor row count: " + cursor.getCount());
		
		return cursor;
	}

	// ========================================================================
	@Override
	public Cursor getDataCursor() {

		Log.d(TAG, "Getting data cursor...");
		
		List<DatedMultiSeriesValues> bins = this.database.generateHistogram(this.publisher_id, this.truncated_date_range, this.bin_count);
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				DatabaseRevenue.AXIS_X,
				DatabaseRevenue.AXIS_Y});

		int row_index = 0;
		for (DatedMultiSeriesValues bin : bins) {

			for (SeriesValue series_value : bin.multi_series) {
				c.newRow().add( row_index )
					.add( series_value.series )
					.add( null )
					.add( bin.date.getTime() )
					.add( series_value.value.floatValue() );
				
				row_index++;
			}
		}
		return c;
	}

	// ========================================================================
	public static Uri constructUri(DateRange date_range, BinningMode binning_mode, int bins, long publisher_id) {
		return UriGenerator.appendDateRange(Uri.withAppendedPath(UriGenerator.BASE_URI, UriGenerator.URI_PATH_HISTOGRAM_PLOT), date_range)
		.buildUpon()
		.appendQueryParameter(UriGenerator.QUERY_PARAMETER_BINNING_MODE, Integer.toString(binning_mode.ordinal()))
		.appendQueryParameter(UriGenerator.QUERY_PARAMETER_HISTOGRAM_BINS, Integer.toString(bins))
		.appendQueryParameter(UriGenerator.QUERY_PARAMETER_PUBLISHER_ID, Long.toString(publisher_id))
		.build();
	}
}