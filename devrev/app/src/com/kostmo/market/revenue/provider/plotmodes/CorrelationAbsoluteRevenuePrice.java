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
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.provider.DatabaseRevenue.PriceSalesSpan;

public class CorrelationAbsoluteRevenuePrice extends PlotMode {

	DateRange date_range;
	long app_id;

	// ========================================================================
	public CorrelationAbsoluteRevenuePrice(DatabaseRevenue database, Uri uri) {
		super(database, uri);

		// Strategy:
		// Obtain both the Comments plot and the Revenue plot, then
		// plot them with a primary and secondary Y-axis.

		this.date_range = new DateRange(
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_START_MILLISECONDS))),
				new Date(Long.parseLong(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_END_MILLISECONDS)))
		);
		
		this.app_id = ContentUris.parseId(uri);
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
		
		List<PriceSalesSpan> spans = database.getRevenuePriceCorrelation(app_id, date_range);
		Log.d(TAG, "Found " + spans.size() + " separate price spans.");
		
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				DatabaseRevenue.AXIS_X,
				DatabaseRevenue.AXIS_Y
			});

		
		int row_index = 0;
		for (PriceSalesSpan span : spans) {
			if (span.purchase_count > 0) {
				c.newRow().add( row_index ).add( 0 ).add( null )
					.add( (span.price_cents / 100f) ).add( span.getDailyRevenueDollars() );

				row_index++;
			}
		}
		
		return c;
	}
	
	
	// ========================================================================
	public static Uri constructUri(long app_id, DateRange date_range) {

		return ContentUris.withAppendedId(
				UriGenerator.appendDateRange(
					UriGenerator.constructRevenuePriceCorrelationUri(),
					date_range),
			app_id);
	}
}