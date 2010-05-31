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
package org.achartengine;

import org.achartengine.model.CategoryMultiSeries;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultiSeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.view.PlotView;
import org.achartengine.view.chart.BarChart;
import org.achartengine.view.chart.BubbleChart;
import org.achartengine.view.chart.DoughnutChart;
import org.achartengine.view.chart.LineChart;
import org.achartengine.view.chart.PieChart;
import org.achartengine.view.chart.ScatterChart;
import org.achartengine.view.chart.TimeChart;
import org.achartengine.view.chart.XYChart;
import org.achartengine.view.chart.BarChart.Type;

import android.content.Context;
import android.util.Log;

/**
 * Utility methods for creating chart views or intents.
 */
public class ChartFactory {


	protected static final String TAG = "AChartEngine"; 


	/** The key for the chart data. */
	public static final String CHART = "chart";

	/** The key for the chart graphical activity title. */
	public static final String TITLE = "title";

	private ChartFactory() {
		// empty for now
	}

	/**
	 * Creates a line chart view.
	 * 
	 * @param context the context
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @return a line chart graphical view
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static final PlotView getLineChartView(Context context,
			XYMultiSeries dataset, XYMultipleSeriesRenderer renderer) {
		checkParameters(dataset, renderer);
		XYChart chart = new LineChart(dataset, renderer);
		return new PlotView(context, chart);
	}

	/**
	 * Creates a scatter chart view.
	 * 
	 * @param context the context
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @return a scatter chart graphical view
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static final PlotView getScatterChartView(Context context,
			XYMultiSeries dataset, XYMultipleSeriesRenderer renderer) {
		checkParameters(dataset, renderer);
		XYChart chart = new ScatterChart(dataset, renderer);
		return new PlotView(context, chart);
	}

	/**
	 * Creates a bubble chart view.
	 * 
	 * @param context the context
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @return a scatter chart graphical view
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static final PlotView getBubbleChartView(Context context,
			XYMultiSeries dataset, XYMultipleSeriesRenderer renderer) {
		checkParameters(dataset, renderer);
		XYChart chart = new BubbleChart(dataset, renderer);
		return new PlotView(context, chart);
	}

	/**
	 * Creates a time chart view.
	 * 
	 * @param context the context
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @param format the date format pattern to be used for displaying the X axis
	 *          date labels. If null, a default appropriate format will be used.
	 * @return a time chart graphical view
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static final PlotView getTimeChartView(Context context,
			XYMultiSeries dataset, XYMultipleSeriesRenderer renderer, String format) {
		checkParameters(dataset, renderer);
		TimeChart chart = new TimeChart(dataset, renderer);
		chart.setDateFormat(format);
		return new PlotView(context, chart);
	}

	/**
	 * Creates a bar chart view.
	 * 
	 * @param context the context
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @param type the bar chart type
	 * @return a bar chart graphical view
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static final PlotView getBarChartView(Context context,
			XYMultiSeries dataset, XYMultipleSeriesRenderer renderer, Type type) {
		checkParameters(dataset, renderer);
		XYChart chart = new BarChart(dataset, renderer, type);
		return new PlotView(context, chart);
	}

	/**
	 * Creates a pie chart intent that can be used to start the graphical view
	 * activity.
	 * 
	 * @param context the context
	 * @param dataset the category series dataset (cannot be null)
	 * @param renderer the series renderer (cannot be null)
	 * @return a pie chart view
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset number of items is different than the number of
	 *           series renderers
	 */
	public static final PlotView getPieChartView(Context context, CategorySeries dataset,
			DefaultRenderer renderer) {
		checkParameters(dataset, renderer);
		PieChart chart = new PieChart(dataset, renderer);
		return new PlotView(context, chart);
	}

	/**
	 * Creates a doughnut chart intent that can be used to start the graphical
	 * view activity.
	 * 
	 * @param context the context
	 * @param dataset the multiple category series dataset (cannot be null)
	 * @param renderer the series renderer (cannot be null)
	 * @return a pie chart view
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset number of items is different than the number of
	 *           series renderers
	 */
	public static final PlotView getDoughnutChartView(Context context,
			CategoryMultiSeries dataset, DefaultRenderer renderer) {
		checkParameters(dataset, renderer);
		DoughnutChart chart = new DoughnutChart(dataset, renderer);
		return new PlotView(context, chart);
	}



	/**
	 * Checks the validity of the dataset and renderer parameters.
	 * 
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static void checkParameters(XYMultiSeries dataset,
			XYMultipleSeriesRenderer renderer) {
		if (dataset == null || renderer == null
				|| dataset.getSeriesCount() != renderer.getSeriesRendererCount()) {

			Log.e(TAG, "dataset: " + dataset);
			Log.e(TAG, "renderer: " + renderer);

			Log.e(TAG, "Dataset series count: " + dataset.getSeriesCount() + "; Series renderer count: " + renderer.getSeriesRendererCount());

			throw new IllegalArgumentException(
			"Dataset and renderer should be not null and should have the same number of series");
		}
	}

	/**
	 * Checks the validity of the dataset and renderer parameters.
	 * 
	 * @param dataset the category series dataset (cannot be null)
	 * @param renderer the series renderer (cannot be null)
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset number of items is different than the number of
	 *           series renderers
	 */
	public static void checkParameters(CategorySeries dataset, DefaultRenderer renderer) {
		if (dataset == null || renderer == null
				|| dataset.getItemCount() != renderer.getSeriesRendererCount()) {
			throw new IllegalArgumentException(
					"Dataset and renderer should be not null and the dataset number of items should be equal to the number of series renderers");
		}
	}

	/**
	 * Checks the validity of the dataset and renderer parameters.
	 * 
	 * @param dataset the category series dataset (cannot be null)
	 * @param renderer the series renderer (cannot be null)
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset number of items is different than the number of
	 *           series renderers
	 */
	public static void checkParameters(CategoryMultiSeries dataset, DefaultRenderer renderer) {
		if (dataset == null || renderer == null
				|| !checkMultipleSeriesItems(dataset, renderer.getSeriesRendererCount())) {
			throw new IllegalArgumentException(
					"Titles and values should be not null and the dataset number of items should be equal to the number of series renderers");
		}
	}

	private static boolean checkMultipleSeriesItems(CategoryMultiSeries dataset, int value) {
		int count = dataset.getCategoriesCount();
		boolean equal = true;
		for (int k = 0; k < count && equal; k++) {
			equal = dataset.getValues(k).size() == dataset.getTitles(k).size();
		}
		return equal;
	}

}
