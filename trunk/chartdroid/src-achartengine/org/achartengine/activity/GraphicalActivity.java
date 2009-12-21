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
import com.googlecode.chartdroid.core.ContentSchema;
import com.googlecode.chartdroid.core.IntentConstants;
import com.googlecode.chartdroid.core.ContentSchema.PlotData;

import org.achartengine.consumer.DatumExtractor;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
abstract public class GraphicalActivity extends Activity {


	protected static final String TAG = "ChartDroid"; 

	/** The encapsulated graphical view. */
	protected GraphicalView mView;

	/** The chart to be drawn. */
	protected AbstractChart mChart;



	protected PointStyle[] DEFAULT_STYLES = new PointStyle[] { PointStyle.CIRCLE, PointStyle.DIAMOND,
			PointStyle.TRIANGLE, PointStyle.SQUARE };
	protected int[] DEFAULT_COLORS = new int[] { Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.CYAN };

	
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


	public void populateLegend(PredicateLayout predicate_layout, List<DataSeriesAttributes> series_attributes_list) {
		populateLegend(predicate_layout, series_attributes_list, false);
	}
	
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

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

	    getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		if (getIntent().getBooleanExtra(IntentConstants.EXTRA_FULLSCREEN, false))
			getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		
	    
	    
		super.onCreate(savedInstanceState);


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

		mChart = generateChartFromContentProvider(intent_data);
		

		mView = (GraphicalView) findViewById(R.id.chart_view);
		
		((TextView) findViewById(R.id.chart_title_placeholder)).setText(title);
		
		
		mView.setChart(mChart);

	}




	// ---------------------------------------------
	Comparator<Entry<Integer, ?>> integer_keyed_entry_comparator = new Comparator<Entry<Integer, ?>>() {

		@Override
		public int compare(Entry<Integer, ?> object1, Entry<Integer, ?> object2) {
			return object1.getKey().compareTo(object2.getKey());
		}
	};

	// ---------------------------------------------
	<T> List<T> sortAndSimplify(Map<Integer, T> input_map) {
		// Sort the axes by index
		ArrayList<Entry<Integer,T>> sorted_axes_series_map = new ArrayList<Entry<Integer, T>>(input_map.entrySet());
		Collections.sort(sorted_axes_series_map, integer_keyed_entry_comparator);

		// Simplify the sorted axes as a list
		List<T> simplified_sorted_axes_series_maps = new ArrayList<T>();
		for (Entry<Integer, T> entry : sorted_axes_series_map)
			simplified_sorted_axes_series_maps.add( entry.getValue() );

		return simplified_sorted_axes_series_maps;
	}    

	// ---------------------------------------------

	<T> List<T> pickAxisSeries(Map<Integer, Map<Integer, List<T>>> axes_series_map, Cursor cursor, int axis_column, int series_column) {
		// Pick the correct axis
		int axis_index = cursor.getInt(axis_column);
		Map<Integer, List<T>> series_map;            
		if (axes_series_map.containsKey(axis_index)) {
			series_map = axes_series_map.get(axis_index);
		} else {
			series_map = new HashMap<Integer, List<T>>();
			axes_series_map.put(axis_index, series_map);
		}

		// Pick the correct series for this axis
		int series_index = cursor.getInt(series_column);
		List<T> series_axis_data;
		if (series_map.containsKey(series_index)) {
			series_axis_data = series_map.get(series_index);
		} else {
			series_axis_data = new ArrayList<T>();
			series_map.put(series_index, series_axis_data);
		}

		return series_axis_data;
	}

	// ---------------------------------------------
	//  Retrieve Axes data
	protected List<String> getAxisTitles() {

		Intent intent = getIntent();
		List<String> extra_series_titles = intent.getStringArrayListExtra(IntentConstants.EXTRA_AXIS_TITLES);
		if (extra_series_titles != null)
			return extra_series_titles;
		
		

		Uri intent_data = intent.getData();
		Uri axes_uri = intent_data.buildUpon().appendEncodedPath( ContentSchema.DATASET_ASPECT_AXES ).build();
		Log.d(TAG, "Querying content provider for: " + axes_uri);

		List<String> axis_labels = new ArrayList<String>();
		{

			Cursor meta_cursor = managedQuery(axes_uri,
					new String[] {BaseColumns._ID, PlotData.COLUMN_AXIS_LABEL},
					null, null, null);

			int axis_column = meta_cursor.getColumnIndex(BaseColumns._ID);
			int label_column = meta_cursor.getColumnIndex(PlotData.COLUMN_AXIS_LABEL);

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
		
		Uri meta_uri = intent_data.buildUpon().appendEncodedPath( ContentSchema.DATASET_ASPECT_META ).build();
		Log.d(TAG, "Querying content provider for: " + meta_uri);

		Map<Integer, String> series_label_map = new HashMap<Integer, String>();
		{

			Cursor meta_cursor = managedQuery(meta_uri,
					new String[] {BaseColumns._ID, PlotData.COLUMN_SERIES_LABEL},
					null, null, null);

			int series_column = meta_cursor.getColumnIndex(BaseColumns._ID);
			int label_column = meta_cursor.getColumnIndex(PlotData.COLUMN_SERIES_LABEL);

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
		List<String> sorted_series_labels = sortAndSimplify(series_label_map);

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

	// Outermost list: Axes
	// Second-outermost list: All Series
	// Third-outermost list: Data for a single series
	protected <T> List<List<List<T>>> getGenericSortedSeriesData(Uri intent_data, DatumExtractor<T> extractor) {

		Uri data_uri = intent_data.buildUpon().appendEncodedPath( ContentSchema.DATASET_ASPECT_DATA ).build();
		Log.d(TAG, "Querying content provider for: " + data_uri);

		Map<Integer, Map<Integer, List<T>>> axes_series_map = new HashMap<Integer, Map<Integer, List<T>>>();

		Cursor cursor = managedQuery(data_uri,
				new String[] {
				BaseColumns._ID,
				PlotData.COLUMN_AXIS_INDEX,
				PlotData.COLUMN_SERIES_INDEX,
				PlotData.COLUMN_DATUM_VALUE,
				PlotData.COLUMN_DATUM_LABEL},
				null, null, null);

		int id_column = cursor.getColumnIndex(BaseColumns._ID);
		int axis_column = cursor.getColumnIndex(PlotData.COLUMN_AXIS_INDEX);
		int series_column = cursor.getColumnIndex(PlotData.COLUMN_SERIES_INDEX);
		int data_column = cursor.getColumnIndex(PlotData.COLUMN_DATUM_VALUE);
		int label_column = cursor.getColumnIndex(PlotData.COLUMN_DATUM_LABEL);


		int i=0;
		if (cursor.moveToFirst()) {
			do {

				List<T> series_axis_data = pickAxisSeries(axes_series_map, cursor, axis_column, series_column);

				T datum = extractor.getDatum(cursor, data_column, label_column);
				series_axis_data.add(datum);

				i++;
			} while (cursor.moveToNext());
		}


		// Sort each axis map by key; that is, sort by the series index - then add it to the simplified axis list
		List<List<List<T>>> simplified_sorted_axes_series = new ArrayList<List<List<T>>>();
		for (Map<Integer, List<T>> series_map : sortAndSimplify(axes_series_map))
			simplified_sorted_axes_series.add( sortAndSimplify(series_map) );

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


	abstract protected AbstractChart generateChartFromContentProvider(Uri intent_data);

	
	
	
	
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
}