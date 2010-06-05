package org.achartengine.consumer;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.googlecode.chartdroid.core.IntentConstants;
import com.googlecode.chartdroid.core.ColumnSchema.AxisExpressionMethod;
import com.googlecode.chartdroid.core.IntentConstants.LineStyle;

import org.achartengine.activity.GraphicalActivity;
import org.achartengine.util.MathHelper.MinMax;
import org.achartengine.view.chart.PointStyle;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/* 
 * Utility functions to help extract data from a ContentProvider
 */
public class DataCollector {

	static final String TAG = "ChartDroid";
	
	// ========================================================================
	public static Comparator<Entry<?, ?>> MAP_KEY_COMPARATOR = new Comparator<Entry<?, ?>>() {
		@Override
		public int compare(Entry<?, ?> object1, Entry<?, ?> object2) {
			return ((Comparable) object1.getKey()).compareTo(object2.getKey());
		}
	};

	// ========================================================================
	public static <T> List<T> sortAndSimplify(Map<Integer, T> input_map, Comparator<Entry<?, ?>> comparator) {
		// Sort the axes by index
		ArrayList<Entry<Integer,T>> sorted_axes_series_map = new ArrayList<Entry<Integer, T>>(input_map.entrySet());
		Collections.sort(sorted_axes_series_map, comparator);

		// Simplify the sorted axes as a list
		List<T> simplified_sorted_axes_series_maps = new ArrayList<T>();
		for (Entry<Integer, T> entry : sorted_axes_series_map)
			simplified_sorted_axes_series_maps.add( entry.getValue() );

		return simplified_sorted_axes_series_maps;
	}

	// ========================================================================
	public static <T> List<T> pickAxisSeriesSingleRow(Map<Integer, Map<Integer, List<T>>> axes_series_map, Cursor cursor, int axis_index, int series_column) {
		// Pick the correct axis
		Map<Integer, List<T>> axis_map;            
		if (axes_series_map.containsKey(axis_index)) {
			
//			Log.d(TAG, "Reusing axis: " + axis_index);
			
			axis_map = axes_series_map.get(axis_index);
		} else {
			
//			Log.e(TAG, "Created new axis: " + axis_index);
			
			axis_map = new HashMap<Integer, List<T>>();
			axes_series_map.put(axis_index, axis_map);
		}

		// Pick the correct series for this axis
		int series_index = cursor.getInt(series_column);
//		Log.w(TAG, "Series index (from column " + series_column + "): " + series_index);
		List<T> series;
		if (axis_map.containsKey(series_index)) {
			
//			Log.d(TAG, "Reusing series: " + series_index);
			
			series = axis_map.get(series_index);
		} else {
			
//			Log.e(TAG, "Created new series: " + series_index);
			
			series = new ArrayList<T>();
			axis_map.put(series_index, series);
		}

		return series;
	}

	// ========================================================================
	public static <T> List<T> pickAxisSeriesMultiRow(Map<Integer, Map<Integer, List<T>>> axes_series_map, Cursor cursor, int axis_column, int series_column) {
		// Pick the correct axis
		int axis_index = cursor.getInt(axis_column);
		Map<Integer, List<T>> axis_map;            
		if (axes_series_map.containsKey(axis_index)) {
			
//			Log.d(TAG, "Reusing axis: " + axis_index);
			
			axis_map = axes_series_map.get(axis_index);
		} else {
			
//			Log.e(TAG, "Created new axis: " + axis_index);
			
			axis_map = new HashMap<Integer, List<T>>();
			axes_series_map.put(axis_index, axis_map);
		}

		// Pick the correct series for this axis
		int series_index = cursor.getInt(series_column);
//		Log.w(TAG, "Series index (from column " + series_column + "): " + series_index);
		List<T> series;
		if (axis_map.containsKey(series_index)) {
			
//			Log.d(TAG, "Reusing series: " + axis_index);
			
			series = axis_map.get(series_index);
		} else {
			
//			Log.e(TAG, "Created new series: " + axis_index);
			
			series = new ArrayList<T>();
			axis_map.put(series_index, series);
		}

		return series;
	}
	
	
	// ========================================================================
	public static class LabeledDatum {
		public String label;
		public Number datum;
	}

	// ========================================================================
	// Retrieve Series data

	// RETURN VALUE:
	// Outermost list: Axes
	// Second-outermost list: All Series for that axis
	// Third-outermost list: Data for a single series
	public static <T> List<List<List<T>>> getGenericSortedSeriesData(Uri intent_data, ContentResolver content_resolver, DatumExtractor<T> extractor) {

		Uri data_uri = intent_data.buildUpon()
			.appendQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER, ColumnSchema.DATASET_ASPECT_DATA)
			.build();

		// Outermost map: Axes
		// Second-outermost map: All Series for that axis
		// Innermost list: Data for a single series
		Map<Integer, Map<Integer, List<T>>> axes_series_map = new HashMap<Integer, Map<Integer, List<T>>>();

		
		

		Cursor cursor = content_resolver.query(data_uri,
//		Cursor cursor = managedQuery(data_uri,
				null,	// Note that the author of the ContentProvider should specify their own projection,
						// so it is irrelevant what we specify here.
				/*
				new String[] {
				BaseColumns._ID,
				ColumnSchema.COLUMN_AXIS_INDEX,
				ColumnSchema.COLUMN_SERIES_INDEX,
				ColumnSchema.COLUMN_DATUM_VALUE,
				ColumnSchema.COLUMN_DATUM_LABEL},
				*/
				null, null, null);

		List<String> column_names = Arrays.asList(cursor.getColumnNames());
//		Log.d(TAG, "Available columns: " + TextUtils.join(", ", column_names));

		List<String> axis_column_names = new ArrayList<String>();
		for (String column : column_names) {
			if (column.startsWith(ColumnSchema.AXIS_PREFIX)) {
				axis_column_names.add(column);
			}
		}
		Collections.sort(axis_column_names);
		boolean is_primary_mode = axis_column_names.size() > 0;

		int id_column = cursor.getColumnIndex(BaseColumns._ID);	// XXX Not used
		int series_column = cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_INDEX);
		int label_column = cursor.getColumnIndex(ColumnSchema.COLUMN_DATUM_LABEL);

		List<List<List<T>>> simplified_sorted_axes_series = new ArrayList<List<List<T>>>();
		
		if (is_primary_mode) {
			// Primary mode (Column Scheme #1 in the Interface Specification docs)
			
//			Log.d(TAG, "Building series in Primary Mode");

			int i=0;
			if (cursor.moveToFirst()) {
				do {
					
//					Log.d(TAG, "Row: " + i);
					
					
					int axis_index = 0;
					for (String axis_column_label : axis_column_names) {

						int data_column = cursor.getColumnIndex(axis_column_label);
						
//						Log.i(TAG, "Axis column: " + axis_column_label + " (Index " + axis_index + ", Column " + data_column + ")");
						
						List<T> series_axis_data = DataCollector.pickAxisSeriesSingleRow(axes_series_map, cursor, axis_index, series_column);
						
						T datum = extractor.getDatum(cursor, data_column, label_column);
						series_axis_data.add(datum);
//						Log.e(TAG, "Series axis datum count: " + series_axis_data.size());
						
						axis_index++;
					}

					i++;
				} while (cursor.moveToNext());
			}
			
		} else {
			// Secondary mode (Column Scheme #2 in the Interface Specification docs)
			// One might use this scheme if the number of axes is unknown at compile time
			
//			Log.d(TAG, "Building series in Secondary Mode");
			
			// This method takes a little more work to extract the data
			
			int axis_column = cursor.getColumnIndex(ColumnSchema.COLUMN_AXIS_INDEX);
			int data_column = cursor.getColumnIndex(ColumnSchema.COLUMN_DATUM_VALUE);


			if (cursor.moveToFirst()) {
				do {
					List<T> series_axis_data = DataCollector.pickAxisSeriesMultiRow(axes_series_map, cursor, axis_column, series_column);

					T datum = extractor.getDatum(cursor, data_column, label_column);
					series_axis_data.add(datum);

				} while (cursor.moveToNext());
			}
		}
		
		cursor.close();

		// Sort each axis map by key; that is, sort by the series index - then add it to the simplified axis list
		for (Map<Integer, List<T>> series_map : DataCollector.sortAndSimplify(axes_series_map, DataCollector.MAP_KEY_COMPARATOR))
			simplified_sorted_axes_series.add( DataCollector.sortAndSimplify(series_map, DataCollector.MAP_KEY_COMPARATOR) );

		return simplified_sorted_axes_series;		
	}
	
	// ========================================================================
	// Outermost list: all series
	// Inner list: individual series
	public static List<List<Number>> unzipSeriesDatumLabels(List<List<LabeledDatum>> sorted_labeled_series_list, List<List<String>> datum_labels) {

		// Don't just discard the datum labels; store them in an auxilliary array.
		// Since we haven't specified which axis the labels should be stored with,
		// we have to check each axis, taking care to preserve labels from previous
		// axes.

		List<List<Number>> sorted_series_list = new ArrayList<List<Number>>();

		int i=0;
		for (List<LabeledDatum> labeled_series : sorted_labeled_series_list) {
			List<Number> series = new ArrayList<Number>();
			sorted_series_list.add( series );


			List<String> individual_series_labels;
			// Grow the datum labels series list if need be
			if (datum_labels.size() < sorted_series_list.size()) {
				individual_series_labels = new ArrayList<String>();
				datum_labels.add(individual_series_labels);
			} else {
				individual_series_labels = datum_labels.get(i);
			}

			int j=0;
			for (LabeledDatum labeled_datum : labeled_series) {
				
				series.add( labeled_datum.datum );

				if (individual_series_labels.size() < labeled_series.size())
					individual_series_labels.add(labeled_datum.label);
				else if (labeled_datum.label != null)
					individual_series_labels.set(j, labeled_datum.label);

				j++;
			}

			i++;
		}
		return sorted_series_list;
	}

	// ========================================================================
	public static class AxesMetaData {
		public String title = "Untitled";
		public AxisExpressionMethod expression_method = null;
		public MinMax limits_override;
	}
	
	// ========================================================================
	/**
	 * Retrieve Axes properties
	 */
	public static List<AxesMetaData> getAxisTitles(Intent intent, ContentResolver content_resolver) {

		List<AxesMetaData> axes_meta_data_list = new ArrayList<AxesMetaData>();
		
		List<String> extra_series_titles = intent.getStringArrayListExtra(IntentConstants.EXTRA_AXIS_TITLES);
		if (extra_series_titles != null) {
			for (String title : extra_series_titles) {
				AxesMetaData meta_data = new AxesMetaData();
				meta_data.title = title;
				axes_meta_data_list.add(meta_data);
			}
			
			// FIXME Include the other axis properties, too!
			return axes_meta_data_list;
		}

		Uri intent_data = intent.getData();
		Uri axes_uri = intent_data.buildUpon()
			.appendQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER, ColumnSchema.DATASET_ASPECT_AXES)
			.build();

		Cursor meta_cursor = content_resolver.query(axes_uri,
				new String[] {
					BaseColumns._ID,
					ColumnSchema.COLUMN_AXIS_LABEL,
					ColumnSchema.COLUMN_AXIS_EXPRESSION,
					ColumnSchema.COLUMN_AXIS_MIN,
					ColumnSchema.COLUMN_AXIS_MAX},
				null, null, null);

		if (meta_cursor == null) return axes_meta_data_list;
		
		int axis_column = meta_cursor.getColumnIndex(BaseColumns._ID);
		int label_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_AXIS_LABEL);
		int expression_method_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_AXIS_EXPRESSION);
		int axis_min_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_AXIS_MIN);
		int axis_max_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_AXIS_MAX);

		if (meta_cursor.moveToFirst()) {

			do {

				AxesMetaData meta_data = new AxesMetaData();
				if (axis_column >= 0)
					meta_data.title = meta_cursor.getString(label_column);
				
				if (expression_method_column >= 0 && !meta_cursor.isNull(expression_method_column))
					meta_data.expression_method = AxisExpressionMethod.values()[meta_cursor.getInt(expression_method_column)];

				if (axis_min_column >= 0 && !meta_cursor.isNull(axis_min_column)
						&& axis_max_column >= 0 && !meta_cursor.isNull(axis_max_column)) {

					meta_data.limits_override = new MinMax(
							meta_cursor.getDouble(axis_min_column),
							meta_cursor.getDouble(axis_max_column)
					);
				}
				
				axes_meta_data_list.add(meta_data);

			} while (meta_cursor.moveToNext());
		}

		return axes_meta_data_list;
	}

	// ========================================================================
	public static class SeriesMetaData {
		public String title = "Untitled";
		public Integer color = null;
		public PointStyle marker_style = PointStyle.CIRCLE;
		public LineStyle line_style = LineStyle.SOLID;
		public float line_thickness = 2f;
	}

	// ========================================================================
	public static List<SeriesMetaData> supplementIntentSeriesMetaData(Intent intent, List<SeriesMetaData> meta_data_list) {
		List<SeriesMetaData> integrated_meta_data = new ArrayList<SeriesMetaData>();

		String[] extra_series_titles = intent.getStringArrayExtra(IntentConstants.EXTRA_SERIES_LABELS);
		
		int[] extra_series_colors = intent.getIntArrayExtra(IntentConstants.EXTRA_SERIES_COLORS);
		
		
		int[] extra_series_markers = intent.getIntArrayExtra(IntentConstants.EXTRA_SERIES_MARKERS);
		int[] extra_series_line_styles = intent.getIntArrayExtra(IntentConstants.EXTRA_SERIES_LINE_STYLES);
		float[] extra_series_line_thicknesses = intent.getFloatArrayExtra(IntentConstants.EXTRA_SERIES_LINE_THICKNESSES);

		List<Integer> sizes = new ArrayList<Integer>();
		if (meta_data_list != null)
			sizes.add(meta_data_list.size());
		
		if (extra_series_titles != null)
			sizes.add(extra_series_titles.length);
		
		if (extra_series_colors != null)
			sizes.add(extra_series_colors.length);
		
		if (extra_series_markers != null)
			sizes.add(extra_series_markers.length);
		
		if (extra_series_line_styles != null)
			sizes.add(extra_series_line_styles.length);
		
		if (extra_series_line_thicknesses != null)
			sizes.add(extra_series_line_thicknesses.length);
		
		int series_size = Collections.max(sizes);
		
		for (int i=0; i<series_size; i++) {
			SeriesMetaData meta_data = new SeriesMetaData();
			integrated_meta_data.add(meta_data);
			
			if (extra_series_titles != null && i < extra_series_titles.length) {
				meta_data.title = extra_series_titles[i];
			} else if (meta_data_list != null && i < meta_data_list.size()) {
				meta_data.title = meta_data_list.get(i).title;
			}
			
			if (intent.getBooleanExtra(IntentConstants.EXTRA_RAINBOW_COLORS, false)) {
				
				meta_data.color = Color.HSVToColor(new float[] {360 * i / (float) series_size, 0.6f, 1});
				
			} else if (extra_series_colors != null && i < extra_series_colors.length) {
				meta_data.color = extra_series_colors[i];
			} else if (meta_data_list != null && i < meta_data_list.size() && meta_data_list.get(i).color != null) {
				meta_data.color = meta_data_list.get(i).color;
			} else {
				// XXX Here we want to at least provide a differentiated default color
				meta_data.color = GraphicalActivity.DEFAULT_COLORS[i % GraphicalActivity.DEFAULT_COLORS.length];
			}
			
			if (extra_series_markers != null && i < extra_series_markers.length) {
				meta_data.marker_style = PointStyle.values()[extra_series_markers[i]];
			} else if (meta_data_list != null && i < meta_data_list.size()) {
				meta_data.marker_style = meta_data_list.get(i).marker_style;
			}
			
			if (extra_series_line_styles != null && i < extra_series_line_styles.length) {
				meta_data.line_style = LineStyle.values()[extra_series_line_styles[i]];
			} else if (meta_data_list != null && i < meta_data_list.size()) {
				meta_data.line_style = meta_data_list.get(i).line_style;
			}
			
			if (extra_series_line_thicknesses != null && i < extra_series_line_thicknesses.length) {
				meta_data.line_thickness = extra_series_line_thicknesses[i];
			} else if (meta_data_list != null && i < meta_data_list.size()) {
				meta_data.line_thickness = meta_data_list.get(i).line_thickness;
			}
		}
		
		return integrated_meta_data;
	}

	// ========================================================================
	public static List<SeriesMetaData> getSeriesMetaData(Intent intent, ContentResolver content_resolver) {

		List<SeriesMetaData> sorted_series_metadata = null;
		Uri series_meta_uri = intent.getData().buildUpon().appendQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER, ColumnSchema.DATASET_ASPECT_SERIES).build();
		Cursor meta_cursor = content_resolver.query(series_meta_uri,
				new String[] {BaseColumns._ID, ColumnSchema.COLUMN_SERIES_LABEL},
				null, null, null);

		if (meta_cursor != null) {

			int series_column = meta_cursor.getColumnIndex(BaseColumns._ID);
			int label_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_LABEL);
			int color_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_COLOR);
			int marker_style_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_MARKER);
			int line_style_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_LINE_STYLE);
			int line_thickness_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_LINE_THICKNESS);
	
			Map<Integer, SeriesMetaData> series_metadata_map = new HashMap<Integer, SeriesMetaData>();
			if (meta_cursor.moveToFirst()) {
	
				do {
					SeriesMetaData series_meta_data = new SeriesMetaData();
					if (label_column >= 0)
						series_meta_data.title = meta_cursor.getString(label_column);
					if (color_column >= 0)
						series_meta_data.color = meta_cursor.getInt(color_column);
					if (marker_style_column >= 0)
						series_meta_data.marker_style = PointStyle.values()[meta_cursor.getInt(marker_style_column)];
					if (line_style_column >= 0)
						series_meta_data.line_style = LineStyle.values()[meta_cursor.getInt(line_style_column)];
					if (line_thickness_column >= 0)
						series_meta_data.line_thickness = meta_cursor.getFloat(line_thickness_column);
	
	
					int series_index = meta_cursor.getInt(series_column);
					series_metadata_map.put(series_index, series_meta_data);
	
				} while (meta_cursor.moveToNext());
			}
	
			// Sort the map by key; that is, sort by the series index
			sorted_series_metadata = DataCollector.sortAndSimplify(series_metadata_map, DataCollector.MAP_KEY_COMPARATOR);
		}

		// Supplement with the intent data
		return supplementIntentSeriesMetaData(intent, sorted_series_metadata);
	}
}