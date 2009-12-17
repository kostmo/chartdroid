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

import org.achartengine.ChartFactory;
import org.achartengine.consumer.LabeledDoubleDatumExtractor;
import org.achartengine.intent.ContentSchema;
import org.achartengine.model.MultipleCategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.view.PredicateLayout;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.DoughnutChart;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class DonutChartActivity extends GraphicalActivity {

	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typepie;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PredicateLayout predicate_layout = (PredicateLayout) findViewById(R.id.predicate_layout);
		List<DataSeriesAttributes> series_attributes_list = getSeriesAttributesList(mChart);
		populateLegend(predicate_layout, series_attributes_list, true);
	}

	// ---------------------------------------------
	@Override
	protected AbstractChart generateChartFromContentProvider(Uri intent_data) {


		List<List<List<LabeledDatum>>> sorted_series_list = getGenericSortedSeriesData(intent_data, new LabeledDoubleDatumExtractor());



		assert( sorted_series_list.size() >= 1 );


		List<List<String>> datam_labels = new ArrayList<List<String>>();


		List<List<Number>> x_axis_series, y_axis_series = null;
		if (sorted_series_list.size() == 1) {
			// Let the Y-axis carry the only data.
			x_axis_series = new ArrayList<List<Number>>();
			y_axis_series = unzipSeriesDatumLabels( sorted_series_list.get( 0 ), datam_labels );

		} else {
			x_axis_series = unzipSeriesDatumLabels( sorted_series_list.get( ContentSchema.X_AXIS_INDEX ), datam_labels );
			y_axis_series = unzipSeriesDatumLabels( sorted_series_list.get( ContentSchema.Y_AXIS_INDEX ), datam_labels );
		}





		assert (x_axis_series.size() == y_axis_series.size()
				|| x_axis_series.size() == 1
				|| x_axis_series.size() == 0);

		String[] titles = getSortedSeriesTitles();

		assert (titles.length == y_axis_series.size());


		assert (titles.length == y_axis_series.get(0).size());


		// If there is no x-axis data, just number the y-elements.
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



		// Use the first series as the representative series
		int series_length = y_axis_series.get(0).size();
		// TODO: Assert that all series are the same length?

		// There should be the same number of colors as the number of elements
		// in the series, NOT the number of series.

		int[] colors = new int[series_length];
		for (int i=0; i<series_length; i++) {
			colors[i] = DEFAULT_COLORS[i % DEFAULT_COLORS.length];
		}



		List<String> axis_labels = getAxisTitles();








		String chart_title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		// NOTE: Axes labels are not applicable to the donut chart. 
		Log.d(TAG, "chart_title: " + chart_title);



		MultipleCategorySeries dataset = org.achartengine.ChartGenHelper.buildMultipleCategoryDataset(chart_title, titles, datam_labels, y_axis_series);

		DefaultRenderer renderer = org.achartengine.ChartGenHelper.buildCategoryRenderer(colors);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.BLACK);

		DoughnutChart chart = new DoughnutChart(dataset, renderer);


		ChartFactory.checkParameters(dataset, renderer);

		return chart;

	}



	@Override
	protected List<DataSeriesAttributes> getSeriesAttributesList(AbstractChart chart) {

		DoughnutChart donut_chart = (DoughnutChart) chart;
		
		// Zip the series attributes
		List<DataSeriesAttributes> series_attributes_list = new ArrayList<DataSeriesAttributes>();

		DefaultRenderer renderer = donut_chart.getRenderer();
		
		for (int i=0; i<donut_chart.getDataset().getCategoriesCount(); i++) {
			String category_title = donut_chart.getDataset().getCategory(i);
			DataSeriesAttributes series = new DataSeriesAttributes();
			
			
			 
			series.color = renderer.getSeriesRendererAt(i).getColor();
			series.title = category_title;
			
			Log.d(TAG, "Series: " + i + "; Title: " + series.title + "; Color: " + series.color);
			
			series_attributes_list.add( series );
		}
		
		return series_attributes_list;
		
	}
}