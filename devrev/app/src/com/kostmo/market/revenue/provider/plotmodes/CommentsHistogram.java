package com.kostmo.market.revenue.provider.plotmodes;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.PlotMode;
import com.kostmo.market.revenue.provider.UriGenerator;

public class CommentsHistogram extends PlotMode {

	// ========================================================================
	public CommentsHistogram(DatabaseRevenue database, Uri uri) {
		super(database, uri);
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
		long app_id = ContentUris.parseId(this.uri);
		return this.database.getRatingHistogram(app_id);
	}

	// ========================================================================
	public static Uri constructUri(long app_id) {
		return ContentUris.withAppendedId(
				Uri.withAppendedPath(UriGenerator.BASE_URI, UriGenerator.URI_PATH_COMMENTS_HISTOGRAM), app_id);
	}
}