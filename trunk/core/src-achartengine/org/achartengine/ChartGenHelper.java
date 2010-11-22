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
package org.achartengine;

import org.achartengine.consumer.DataCollector.SeriesMetaData;
import org.achartengine.model.CategoryMultiSeries;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultiSeries;
import org.achartengine.model.XYSeries;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.AxesManager;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.util.Log;

import java.util.Date;
import java.util.List;

/**
 * An abstract class for the demo charts to extend.
 */
public abstract class ChartGenHelper {

	static final String TAG = "ChartDroid";

	// ========================================================================
	/**
	 * Builds an XY multiple time dataset using the provided values.
	 * @param titles the series titles
	 * @param xValues the values for the X axis
	 * @param yValues the values for the Y axis
	 * @return the XY multiple time dataset
	 */
	public static XYMultiSeries buildDateDataset(
			String[] titles,
			List<List<Date>> xValues,
			List<List<Number>> yValues) {
		
		XYMultiSeries dataset = new XYMultiSeries();

		for (int i = 0; i < titles.length; i++) {
			TimeSeries series = new TimeSeries(titles[i]);
			List<Date> xV = xValues.get(i);
			List<Number> yV = yValues.get(i);
			int seriesLength = xV.size();
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV.get(k), yV.get(k));
			}
			dataset.addSeries(series);
		}
		return dataset;
	}

	// ========================================================================
	public static XYMultiSeries buildValueDataset(
			String[] titles,
			List<? extends List<? extends Number>> xValues,
			List<? extends List<? extends Number>> yValues,
			List<? extends List<? extends Number>> zValues) {
		
		XYMultiSeries dataset = new XYMultiSeries();

		for (int i = 0; i < titles.length; i++) {
			// Zip the coordinates together for each series
			XYValueSeries series = new XYValueSeries(titles[i]);
			List<? extends Number> xV = xValues.get(i);
			List<? extends Number> yV = yValues.get(i);
			List<? extends Number> zV = zValues.get(i);
			
			int seriesLength = xV.size();
			int corroboratedYSeriesLength = yV.size();
			int corroboratedZSeriesLength = yV.size();
//			Log.d(TAG, "Series " + i + " axes set sizes: X: " + seriesLength + "; Y: " + corroboratedYSeriesLength + "; Z: " + corroboratedZSeriesLength); 

			for (int k = 0; k < seriesLength; k++) {
				series.add(xV.get(k), yV.get(k), zV.get(k));
			}
			dataset.addSeries(series);
		}
		return dataset;
	}

	// ========================================================================
	public static XYMultiSeries buildDataset(
			String[] titles,
			List<? extends List<? extends Number>> xValues,
			List<? extends List<? extends Number>> yValues) {
		
		XYMultiSeries dataset = new XYMultiSeries();

		for (int i = 0; i < titles.length; i++) {
			// Zip the coordinates together for each series
			XYSeries series = new XYSeries(titles[i]);
			List<? extends Number> xV = xValues.get(i);
			List<? extends Number> yV = yValues.get(i);
			int seriesLength = xV.size();
			int corroboratedSeriesLength = yV.size();

			for (int k = 0; k < seriesLength; k++) {
				series.add(xV.get(k), yV.get(k));
			}
			dataset.addSeries(series);
		}
		return dataset;
	}

	// ========================================================================
	/**
	 * Builds an XY multiple series renderer.
	 * @param colors the series rendering colors
	 * @param styles the series point styles
	 * @return the XY multiple series renderers
	 */
	public static XYMultipleSeriesRenderer buildRenderer(List<SeriesMetaData> series_meta_data) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

		for (SeriesMetaData meta_data : series_meta_data) {
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor( meta_data.color );
			r.setPointStyle( meta_data.marker_style );
			r.setUsesSecondaryAxis( meta_data.y_axis_index != 0 );
			renderer.addSeriesRenderer(r);
		}
		return renderer;
	}

	// ========================================================================
	/**
	 * Sets a few of the series renderer settings.
	 * @param renderer the renderer to set the properties to
	 * @param title the chart title
	 * @param xTitle the title for the X axis
	 * @param yTitle the title for the Y axis
	 * @param xMin the minimum value on the X axis
	 * @param xMax the maximum value on the X axis
	 * @param yMin the minimum value on the Y axis
	 * @param yMax the maximum value on the Y axis
	 * @param axesColor the axes color
	 * @param labelsColor the labels color
	 */
	public static void setChartSettings(AxesManager renderer, String title, String xTitle,
			String yTitle, int axesColor, int labelsColor) {
		
		renderer.setChartTitle(title);
		
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
	}

	// ========================================================================
	public static CategorySeries buildCategoryDataset(String title, List<Number> values, List<String> datum_labels) {
		CategorySeries series = new CategorySeries(title);

		for (int i=0; i<values.size(); i++) {
			String label = datum_labels.get(i);

			if (label == null) {
				label = "Datum " + i;
			}
			
			series.add(label, values.get(i));
		}

		return series;
	}

	// ========================================================================
	/**
	 * Builds a multiple category series using the provided values.
	 * @param datum_labels the series titles
	 * @param series_set the values
	 * @return the category series
	 */
	public static CategoryMultiSeries buildMultiCategoryDataset(
			String title,
			String[] series_labels,
			List<List<String>> datum_labels,
			List<List<Number>> series_set) {
		
		CategoryMultiSeries series = new CategoryMultiSeries(title);
		int k = 0;
		for (List<Number> series_values : series_set) {
			series.add(series_labels[k], datum_labels.get(k), series_values);
			k++;
		}
		return series;
	}

	// ========================================================================
	/**
	 * Builds a category renderer to use the provided colors.
	 * @param colors the colors
	 * @return the category renderer
	 */
	public static DefaultRenderer buildCategoryRenderer(int[] colors) {
		DefaultRenderer renderer = new DefaultRenderer();
		for (int color : colors) {
			SimpleSeriesRenderer r = new SimpleSeriesRenderer();
			r.setColor(color);
			renderer.addSeriesRenderer(r);
		}
		return renderer;
	}

	// ========================================================================
	/**
	 * Builds a bar multiple series renderer to use the provided colors.
	 * @param colors the series renderers colors
	 * @return the bar multiple series renderer
	 */
	protected XYMultipleSeriesRenderer buildBarRenderer(int[] colors) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		int length = colors.length;
		for (int i = 0; i < length; i++) {
			SimpleSeriesRenderer r = new SimpleSeriesRenderer();
			r.setColor(colors[i]);
			renderer.addSeriesRenderer(r);
		}
		return renderer;
	}
}
