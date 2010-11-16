package com.kostmo.market.revenue.provider.plotmodes;

import java.util.List;

import android.content.ContentUris;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.activity.AppsOverviewActivity.WindowEvaluatorMode;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.provider.DatabaseRevenue.DatedRatingsWindow;

public class CommentsPlot extends PlotMode {
	
	private int window_size;
	private WindowEvaluatorMode evaluator;

	// ========================================================================
	public CommentsPlot(DatabaseRevenue database, Uri uri) {
		super(database, uri);

		this.window_size = Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_SIZE));
		this.evaluator = WindowEvaluatorMode.values()[Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_EVALUATOR))];
	}

	// ========================================================================
	@Override
	public Cursor getAxesCursor() {

		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Axes.COLUMN_AXIS_LABEL});

		String[] axis_labels = new String[] {
			"Date",
			String.format(evaluator.shortname + " Rating, %d-comment window", window_size)
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
		
		long app_id = ContentUris.parseId(this.uri);
		Log.d(TAG, "Parsed App ID: " + app_id);
		Cursor cursor = this.database.getSeriesLabelsForComments(app_id);
		
		Log.e(TAG, "Series labels cursor row count: " + cursor.getCount());
		
		return cursor;
	}

	// ========================================================================
	@Override
	public Cursor getDataCursor() {

		long app_id = ContentUris.parseId(this.uri);
		List<DatedRatingsWindow> windows = this.database.genWindowedRatingAverages(app_id, this.window_size, null);
		 
		Log.e(TAG, "DatedRatings window count: " + windows.size());
		
		MatrixCursor c = new MatrixCursor(new String[] {
				BaseColumns._ID,
				ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				DatabaseRevenue.AXIS_X,
				DatabaseRevenue.AXIS_Y});

		int row_index = 0;
		for (DatedRatingsWindow window : windows) {

			c.newRow().add( row_index )
			.add( 0 )
			.add( null )
			.add( window.getDate().getTime() )
			.add( window.getValue( this.evaluator ) );
		
			row_index++;
		}
		return c;
	}
	

	// ========================================================================
	public static Uri constructUri(long app_id, int window, WindowEvaluatorMode evaluator_mode) {
		return ContentUris.appendId(Uri.withAppendedPath(UriGenerator.BASE_URI, UriGenerator.URI_PATH_COMMENTS_PLOT)
			.buildUpon()
				.appendQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_EVALUATOR, Integer.toString(evaluator_mode.ordinal()))
				.appendQueryParameter(UriGenerator.QUERY_PARAMETER_WINDOW_SIZE, Integer.toString(window))
				, app_id)
			.build();
	}
}