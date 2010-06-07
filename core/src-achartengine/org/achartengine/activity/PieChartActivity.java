/**
 * Copyright (C) 2010 Karl Ostmo
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
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.PieChart;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class PieChartActivity extends RadialChartActivity {

	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typepie;
	}

	// ========================================================================
	// NOTE: This chart type will ignore all but the first series on the first axis.
	@Override
	protected AbstractChart generateChartFromContentProvider(Uri intent_data) {


		AxesContainer axes_container = getAxesSets(intent_data);

		// Use the first series as the representative series
		int series_length = axes_container.y_axis_series.get(0).size();
		// TODO: Assert that all series are the same length?

		// There should be the same number of colors as the number of elements
		// in the series, NOT the number of series.

		int[] colors = new int[series_length];
		for (int i=0; i<series_length; i++) {
			colors[i] = DEFAULT_COLORS[i % DEFAULT_COLORS.length];
		}


		String chart_title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		// NOTE: Axes labels are not applicable to the donut chart. 
		Log.d(TAG, "chart_title: " + chart_title);


		List<Number> first_series = axes_container.y_axis_series.get(0);
		CategorySeries dataset = org.achartengine.ChartGenHelper.buildCategoryDataset(chart_title, first_series);

		DefaultRenderer renderer = org.achartengine.ChartGenHelper.buildCategoryRenderer(colors);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.BLACK);



		ChartFactory.checkParameters(dataset, renderer);

		return new PieChart(dataset, renderer);
	}


	// ========================================================================
	@Override
	protected List<DataSeriesAttributes> getSeriesAttributesList(AbstractChart chart) {

		PieChart pie_chart = (PieChart) chart;

		// Zip the series attributes
		List<DataSeriesAttributes> series_attributes_list = new ArrayList<DataSeriesAttributes>();

		DefaultRenderer renderer = pie_chart.getRenderer();

		for (int i=0; i<pie_chart.getDataset().getItemCount(); i++) {
			String category_title = pie_chart.getDataset().getCategory(i);
			DataSeriesAttributes series = new DataSeriesAttributes();



			series.color = renderer.getSeriesRendererAt(i).getColor();
			series.title = category_title;

			Log.d(TAG, "Series: " + i + "; Title: " + series.title + "; Color: " + series.color);

			series_attributes_list.add( series );
		}

		return series_attributes_list;
	}
}