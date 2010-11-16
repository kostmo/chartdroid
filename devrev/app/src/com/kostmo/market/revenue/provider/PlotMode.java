package com.kostmo.market.revenue.provider;

import android.database.Cursor;
import android.net.Uri;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.Market;

public abstract class PlotMode {

	protected static final String TAG = Market.TAG;
	
	protected final DatabaseRevenue database;
	protected final Uri uri;
	
	public PlotMode(DatabaseRevenue database, Uri uri) {
		this.database = database;
		this.uri = uri;
	}
	
	protected abstract Cursor getAxesCursor();
	protected abstract Cursor getSeriesCursor();
	protected abstract Cursor getDataCursor();
	
	public Cursor getPlotCursor() {
		if (ColumnSchema.Aspect.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) ))
			return getAxesCursor();
		else if (ColumnSchema.Aspect.DATASET_ASPECT_SERIES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) ))
			return getSeriesCursor();
		else
			return getDataCursor();
	}
}
