package com.googlecode.chartdroid.market.sales.provider;

import android.content.ContentResolver;
import android.provider.BaseColumns;


public final class ColumnSchema {


	
	public static final String EXTRA_AXIS_TITLES = "com.googlecode.chartdroid.intent.extra.AXIS_TITLES";
	public static final String EXTRA_FORMAT_STRING_Y = "com.googlecode.chartdroid.intent.extra.FORMAT_STRING_Y";

	public static final int X_AXIS_INDEX = 0;
	public static final int Y_AXIS_INDEX = 1;

	public static final String DATASET_ASPECT_PARAMETER = "aspect";
	public static final String DATASET_ASPECT_DATA = "data";
	public static final String DATASET_ASPECT_AXES = "axes";
	public static final String DATASET_ASPECT_META = "meta";


	public static final String CONTENT_TYPE_BASE_SINGLE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/";
	public static final String CONTENT_TYPE_BASE_MULTIPLE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/";




	// ==== COLUMNS ====

	public static final String COLUMN_SERIES_INDEX = "COLUMN_SERIES_INDEX";
	public static final String COLUMN_SERIES_LABEL = "COLUMN_SERIES_LABEL";

	public static final String COLUMN_AXIS_INDEX = "COLUMN_AXIS_INDEX";
	public static final String COLUMN_AXIS_LABEL = "COLUMN_AXIS_LABEL";

	public static final String COLUMN_DATUM_VALUE = "COLUMN_DATUM_VALUE";
	public static final String COLUMN_DATUM_LABEL = "COLUMN_DATUM_LABEL";

	public static final class PlotData implements BaseColumns {

		public static final String VND_TYPE_DECLARATION = "vnd.com.googlecode.chartdroid.graphable";

		// ==== CONTENT TYPES ====

		public static final String CONTENT_TYPE_PLOT_DATA = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
		public static final String CONTENT_TYPE_ITEM_PLOT_DATA = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
	}


	public static final class EventData implements BaseColumns {

		public static final String VND_TYPE_DECLARATION = "vnd.com.googlecode.chartdroid.timeline";

		// ==== CONTENT TYPES ====

		public static final String CONTENT_TYPE_PLOT_DATA = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
		public static final String CONTENT_TYPE_ITEM_PLOT_DATA = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
	}
}
