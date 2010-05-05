/**
 * Copyright (C) 2009 Karl Ostmo
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

import org.achartengine.ChartFactory;
import org.achartengine.consumer.DataCollector;
import org.achartengine.consumer.DoubleDatumExtractor;
import org.achartengine.consumer.DataCollector.SeriesMetaData;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.util.MathHelper.MinMax;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.BarChart;
import org.achartengine.view.chart.PointStyle;
import org.achartengine.view.chart.BarChart.Type;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class BarChartActivity extends XYChartActivity {

	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typebar;
	}

	// ========================================================================
	@Override
	protected AbstractChart generateChartFromContentProvider(Uri intent_data) {


		List<? extends List<? extends List<? extends Number>>> sorted_series_list = DataCollector.getGenericSortedSeriesData(intent_data, getContentResolver(), new DoubleDatumExtractor());

		assert( sorted_series_list.size() >= 1 );

		List<List<Number>> x_axis_series, y_axis_series = null;
		if (sorted_series_list.size() == 1) {
			// Let the Y-axis carry the only data.
			x_axis_series = new ArrayList<List<Number>>();
			y_axis_series = (List<List<Number>>) sorted_series_list.get( 0 );

		} else {
			x_axis_series = (List<List<Number>>) sorted_series_list.get( ColumnSchema.X_AXIS_INDEX );
			y_axis_series = (List<List<Number>>) sorted_series_list.get( ColumnSchema.Y_AXIS_INDEX );    
		}


		assert (x_axis_series.size() == y_axis_series.size()
				|| x_axis_series.size() == 1
				|| x_axis_series.size() == 0);

		List<SeriesMetaData> series_meta_data = DataCollector.getSeriesMetaData( getIntent(), getContentResolver() );
		String[] titles = new String[series_meta_data.size()];
		for (int i=0; i<series_meta_data.size(); i++)
			titles[i] = series_meta_data.get(i).title;
		
		
		assert (titles.length == y_axis_series.size());


		assert (titles.length == y_axis_series.get(0).size());


		// If there is no x-axis data, just fill it in by numbering the y-elements.
		List<Number> prototypical_x_values; 
		if (x_axis_series.size() == 0) {
			for (int i=0; i < y_axis_series.size(); i++) {
				prototypical_x_values = new ArrayList<Number>();
				x_axis_series.add( prototypical_x_values );
				for (int j=0; j < y_axis_series.get(i).size(); j++)
					prototypical_x_values.add(j);
			}
		}


		// Replicate the X-axis data for each series if necessary
		if (x_axis_series.size() == 1) {
			Log.i(TAG, "Replicating x-axis series...");
			prototypical_x_values = x_axis_series.get(0);
			Log.d(TAG, "Size of prototypical x-set: " + prototypical_x_values.size());
			while (x_axis_series.size() < titles.length)
				x_axis_series.add( prototypical_x_values );
		}


		int[] colors = new int[titles.length];
		PointStyle[] styles =  new PointStyle[titles.length];
		for (int i=0; i<titles.length; i++) {
			colors[i] = DEFAULT_COLORS[i % DEFAULT_COLORS.length];
			styles[i] = DEFAULT_STYLES[i % DEFAULT_STYLES.length];
		}


		List<String> axis_labels = DataCollector.getAxisTitles(getIntent(), getContentResolver());





		XYMultipleSeriesRenderer renderer = org.achartengine.ChartGenHelper.buildRenderer(series_meta_data);



		String chart_title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		String x_label = axis_labels.get( ColumnSchema.X_AXIS_INDEX );
		String y_label = axis_labels.get( ColumnSchema.Y_AXIS_INDEX );
		Log.d(TAG, "X LABEL: " + x_label);
		Log.d(TAG, "X LABEL: " + y_label);
		Log.d(TAG, "chart_title: " + chart_title);

		org.achartengine.ChartGenHelper.setChartSettings(renderer, chart_title, x_label, y_label,
				Color.LTGRAY, Color.GRAY);
		renderer.setXLabels(12);

		// FIXME: Generate dynamically
		org.achartengine.ChartGenHelper.setAxesExtents(renderer, 0.5, 12.5, 0, 32);


		XYMultipleSeriesDataset dataset = org.achartengine.ChartGenHelper.buildBarDataset2(titles, y_axis_series);

		ChartFactory.checkParameters(dataset, renderer);

		BarChart chart = new BarChart(dataset, renderer, Type.DEFAULT);

		String x_format = getIntent().getStringExtra(IntentConstants.EXTRA_FORMAT_STRING_X);
		if (x_format != null) chart.setXFormat(x_format);

		String y_format = getIntent().getStringExtra(IntentConstants.EXTRA_FORMAT_STRING_Y);
		if (y_format != null) chart.setYFormat(y_format);

		return chart;
	}

	// ========================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);


		menu.findItem(R.id.menu_toggle_stacked).setVisible(true);
		return true;
	}


	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_toggle_stacked:
		{

			BarChart bc = (BarChart) mChart;
			bc.setType( bc.getType().equals(Type.DEFAULT) ? Type.STACKED : Type.DEFAULT);

			mView.repaint();
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}
	
	
	// ========================================================================
	@Override
	MinMax getYAxisLimits(List<List<Number>> multi_series) {
		return getAxisLimits(multi_series);
	}

	// ========================================================================
	@Override
	MinMax getXAxisLimits(List<List<Number>> multi_series) {
		return getAxisLimits(multi_series);
	}
}