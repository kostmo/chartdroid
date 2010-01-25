/**
 * Copyright (C) 2009 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.achartengine.activity;

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.core.ColumnSchema;
import com.googlecode.chartdroid.core.IntentConstants;

import org.achartengine.consumer.DatumExtractor;
import org.achartengine.util.SemaphoreHost;
import org.achartengine.view.GraphicalView;
import org.achartengine.view.PredicateLayout;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.PointStyle;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
abstract public class GraphicalActivity extends Activity implements SemaphoreHost {


	protected static final String TAG = "ChartDroid"; 

	/** The encapsulated graphical view. */
	protected GraphicalView mView;

	/** The chart to be drawn. */
	protected AbstractChart mChart;


	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	protected PointStyle[] DEFAULT_STYLES = new PointStyle[] { PointStyle.CIRCLE, PointStyle.DIAMOND,
			PointStyle.TRIANGLE, PointStyle.SQUARE };
	protected int[] DEFAULT_COLORS = new int[] { Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.CYAN };

	
	
	
	

	DataQueryTask data_query_task;
	
	
	
	protected abstract int getTitlebarIconResource();
	
	// TODO: Implement for Donut chart
	abstract protected List<DataSeriesAttributes> getSeriesAttributesList(AbstractChart chart);
	

	protected int getLayoutResourceId() {
		return R.layout.simple_chart_activity;
	}
	
	public static class DataSeriesAttributes {
		public String title;
		int color;
	}


	// =============================================
	public class DataQueryTask extends AsyncTask<Void, Void, AbstractChart> {

		private Uri chart_data_uri;
		private String error_message;
		DataQueryTask(Uri chart_data_uri) {
			this.chart_data_uri = chart_data_uri;
		}

		@Override
		protected void  onPreExecute  () {
			incSemaphore();
		}

		@Override
		protected AbstractChart doInBackground(Void... params) {
			try {
				return generateChartFromContentProvider(this.chart_data_uri);
			} catch (IllegalArgumentException e) {
				this.error_message = e.getLocalizedMessage();
			}
			return null;
		}

		@Override
		protected void onPostExecute(AbstractChart chart) {

			decSemaphore();
			
			if (error_message != null) {
				Toast.makeText(GraphicalActivity.this, "There are no series!", Toast.LENGTH_LONG).show();
				
				
				Log.e(TAG, "Error in chart; finishing activity. Chart: " + chart);
				
				finish();
				
				return;
			}
			
			mChart = chart;
			mView.setChart(mChart);
			
			
			postChartPopulationCallback();
			
		}
	}

	// =============================================
	
	protected abstract void postChartPopulationCallback();
	
	// =============================================
	public void populateLegend(PredicateLayout predicate_layout, List<DataSeriesAttributes> series_attributes_list) {
		populateLegend(predicate_layout, series_attributes_list, false);
	}

	// =============================================
	public void populateLegend(PredicateLayout predicate_layout, List<DataSeriesAttributes> series_attributes_list, boolean donut_style) {
		
		PredicateLayout.LayoutParams lp = new PredicateLayout.LayoutParams(5, 1);
		predicate_layout.setPredicateLayoutParams(lp);

		int i=0;
		for (DataSeriesAttributes series : series_attributes_list) {

			Button b = new Button(this);
			b.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			b.setGravity(Gravity.CENTER_VERTICAL);
			b.setText( series.title );
			b.setBackgroundDrawable(null);
			b.setPadding(0, 0, 0, 0);

			
			int icon_width = 16;
			Drawable icon;
			int color;
			if (donut_style) {
				color = Color.WHITE;
				PaintDrawable swatch = new PaintDrawable( color );
				int width = icon_width * (series_attributes_list.size() - i)/series_attributes_list.size();
				swatch.setIntrinsicWidth(width);
				swatch.setIntrinsicHeight(width);
				swatch.setCornerRadius(width/2f);
				icon = swatch;
			} else {
				color = series.color;
				PaintDrawable swatch = new PaintDrawable( color );
				swatch.setIntrinsicWidth(icon_width);
				swatch.setIntrinsicHeight(icon_width);
				swatch.setCornerRadius(icon_width/4);
				icon = swatch;
			}
			

			b.setTextColor( color );
			
			
			
			b.setCompoundDrawablePadding(3);
			b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			predicate_layout.addView(b);
			
			i++;
		}
	}


	// ---------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {

	    getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		if (getIntent().getBooleanExtra(IntentConstants.EXTRA_FULLSCREEN, false))
			getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		
	    
	    
		super.onCreate(savedInstanceState);


		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		

		Uri intent_data = getIntent().getData();

		// We should have been passed a cursor to the data via a content provider.

		String title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		if (title == null) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		} else if (title.length() > 0) {
			setTitle(title);
		}


		setContentView( getLayoutResourceId() );
	    getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, getTitlebarIconResource());

		

		mView = (GraphicalView) findViewById(R.id.chart_view);
		
		((TextView) findViewById(R.id.chart_title_placeholder)).setText(title);
		
		
		data_query_task = new DataQueryTask(intent_data);
		data_query_task.execute();
	}

	// ---------------------------------------------
	Comparator<Entry<?, ?>> map_key_comparator = new Comparator<Entry<?, ?>>() {
		@Override
		public int compare(Entry<?, ?> object1, Entry<?, ?> object2) {
			return ((Comparable) object1.getKey()).compareTo(object2.getKey());
		}
	};

	// ---------------------------------------------
	<T> List<T> sortAndSimplify(Map<Integer, T> input_map, Comparator<Entry<?, ?>> comparator) {
		// Sort the axes by index
		ArrayList<Entry<Integer,T>> sorted_axes_series_map = new ArrayList<Entry<Integer, T>>(input_map.entrySet());
		Collections.sort(sorted_axes_series_map, comparator);

		// Simplify the sorted axes as a list
		List<T> simplified_sorted_axes_series_maps = new ArrayList<T>();
		for (Entry<Integer, T> entry : sorted_axes_series_map)
			simplified_sorted_axes_series_maps.add( entry.getValue() );

		return simplified_sorted_axes_series_maps;
	}

	// ---------------------------------------------

	<T> List<T> pickAxisSeriesMethod1(Map<Integer, Map<Integer, List<T>>> axes_series_map, Cursor cursor, int axis_index, int series_column) {
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
	
	// ---------------------------------------------

	<T> List<T> pickAxisSeries(Map<Integer, Map<Integer, List<T>>> axes_series_map, Cursor cursor, int axis_column, int series_column) {
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

	// ---------------------------------------------
	//  Retrieve Axes data
	protected List<String> getAxisTitles() {

		Intent intent = getIntent();
		List<String> extra_series_titles = intent.getStringArrayListExtra(IntentConstants.EXTRA_AXIS_TITLES);
		if (extra_series_titles != null)
			return extra_series_titles;
		
		

		Uri intent_data = intent.getData();
		Uri axes_uri = intent_data.buildUpon().appendEncodedPath( ColumnSchema.DATASET_ASPECT_AXES ).build();
//		Log.d(TAG, "Querying content provider for: " + axes_uri);

		List<String> axis_labels = new ArrayList<String>();
		{

			Cursor meta_cursor = managedQuery(axes_uri,
					new String[] {BaseColumns._ID, ColumnSchema.COLUMN_AXIS_LABEL},
					null, null, null);

			int axis_column = meta_cursor.getColumnIndex(BaseColumns._ID);
			int label_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_AXIS_LABEL);

			int i=0;
			if (meta_cursor.moveToFirst()) {
				// TODO: This could also be used to set color, line style, marker shape, etc.
				do {
					//            int axis_index = meta_cursor.getInt(axis_column);
					String axis_label = meta_cursor.getString(label_column);


					axis_labels.add(axis_label);


					i++;
				} while (meta_cursor.moveToNext());
			}
		}

		return axis_labels;
	}

	// ---------------------------------------------
	protected String[] getSortedSeriesTitles() {

		Intent intent = getIntent();
		String[] extra_series_titles = intent.getStringArrayExtra(IntentConstants.EXTRA_SERIES_LABELS);
		if (extra_series_titles != null)
			return extra_series_titles;
		
		Uri intent_data = intent.getData();
		
		Uri meta_uri = intent_data.buildUpon().appendEncodedPath( ColumnSchema.DATASET_ASPECT_META ).build();
//		Log.d(TAG, "Querying content provider for: " + meta_uri);

		Map<Integer, String> series_label_map = new HashMap<Integer, String>();
		{

			Cursor meta_cursor = managedQuery(meta_uri,
					new String[] {BaseColumns._ID, ColumnSchema.COLUMN_SERIES_LABEL},
					null, null, null);

			int series_column = meta_cursor.getColumnIndex(BaseColumns._ID);
			int label_column = meta_cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_LABEL);

			int i=0;
			if (meta_cursor.moveToFirst()) {
				// TODO: This could also be used to set color, line style, marker shape, etc.
				do {
					int series_index = meta_cursor.getInt(series_column);
					String series_label = meta_cursor.getString(label_column);


					series_label_map.put(series_index, series_label);


					i++;
				} while (meta_cursor.moveToNext());
			}
		}

		// Sort the map by key; that is, sort by the series index
		List<String> sorted_series_labels = sortAndSimplify(series_label_map, map_key_comparator);

		String[] titles = sorted_series_labels.toArray(new String[] {});
		return titles;
	}

	// ---------------------------------------------
	public static class LabeledDatum {
		public String label;
		public Number datum;
	}

	// ---------------------------------------------
	// Retrieve Series data

	// RETURN VALUE:
	// Outermost list: Axes
	// Second-outermost list: All Series for that axis
	// Third-outermost list: Data for a single series
	protected <T> List<List<List<T>>> getGenericSortedSeriesData(Uri intent_data, DatumExtractor<T> extractor) {

		Uri data_uri = intent_data.buildUpon().appendEncodedPath( ColumnSchema.DATASET_ASPECT_DATA ).build();
//		Log.d(TAG, "Querying content provider for: " + data_uri);

		// Outermost map: Axes
		// Second-outermost map: All Series for that axis
		// Innermost list: Data for a single series
		Map<Integer, Map<Integer, List<T>>> axes_series_map = new HashMap<Integer, Map<Integer, List<T>>>();

		
		

		Cursor cursor = getContentResolver().query(data_uri,
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
			if (column.startsWith("AXIS_")) {
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
						
						List<T> series_axis_data = pickAxisSeriesMethod1(axes_series_map, cursor, axis_index, series_column);
						
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


			int i=0;
			if (cursor.moveToFirst()) {
				do {

					List<T> series_axis_data = pickAxisSeries(axes_series_map, cursor, axis_column, series_column);

					T datum = extractor.getDatum(cursor, data_column, label_column);
					series_axis_data.add(datum);

					i++;
				} while (cursor.moveToNext());
			}
		}
		
		cursor.close();

		// Sort each axis map by key; that is, sort by the series index - then add it to the simplified axis list
		for (Map<Integer, List<T>> series_map : sortAndSimplify(axes_series_map, map_key_comparator))
			simplified_sorted_axes_series.add( sortAndSimplify(series_map, map_key_comparator) );

		
		return simplified_sorted_axes_series;		
	}

	// ---------------------------------------------

	// Outermost list: all series
	// Inner list: individual series
	protected List<List<Number>> unzipSeriesDatumLabels(List<List<LabeledDatum>> sorted_labeled_series_list, List<List<String>> datum_labels) {

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

				if (individual_series_labels.size() < labeled_series.size()) {
					individual_series_labels.add(labeled_datum.label);
				} else {
					if (labeled_datum.label != null)
						individual_series_labels.set(j, labeled_datum.label);
				}

				j++;
			}

			i++;
		}
		return sorted_series_list;
	}


	abstract protected AbstractChart generateChartFromContentProvider(Uri intent_data) throws IllegalArgumentException;

	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_chart, menu);

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_share_chart:
		{
			View view = findViewById(R.id.full_chart_view);
			Bitmap image = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
			
			// Next create a canvas with the bitmap and pass that into the draw() method of the view. This will ask the view to draw it's contents onto the canvas and therefore the associated bitmap.
			view.draw(new Canvas(image));

			// Next insert the image into the Media library. This returns a URI that refers to the stored image and can be reused across applications. In our case we could pass this URL to the MMS application to have it embed the image in a message.  I'll post some sample code of how to send this image via MMS shortly.
			String url = Images.Media.insertImage(getContentResolver(), image, getIntent().getStringExtra(Intent.EXTRA_TITLE), null);
			Uri uri = Uri.parse(url);
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("image/*");
			i.putExtra(Intent.EXTRA_STREAM, uri);
			startActivity(i);
			
			return true;
		}
		case R.id.menu_fullscreen:
		{
			Intent i = getIntent();
			boolean fullscreen = getIntent().getBooleanExtra(IntentConstants.EXTRA_FULLSCREEN, false);
			i.putExtra(IntentConstants.EXTRA_FULLSCREEN, !fullscreen);
			startActivity(i);
			finish();
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}
	
	
    
	// ========================================================================
	@Override
	public void incSemaphore() {

		setProgressBarIndeterminateVisibility(true);
		retrieval_tasks_semaphore.incrementAndGet();
	}

	@Override
	public void decSemaphore() {

		boolean still_going = retrieval_tasks_semaphore.decrementAndGet() > 0;
		setProgressBarIndeterminateVisibility(still_going);
	}
	
	
	
	// =============================================

	@Override
	protected void onDestroy() {

		if (data_query_task != null)
			data_query_task.cancel(true);
		
		super.onDestroy();
	}
}