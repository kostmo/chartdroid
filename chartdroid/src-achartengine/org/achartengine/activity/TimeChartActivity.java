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

import org.achartengine.ChartFactory;
import org.achartengine.consumer.LabeledDatumExtractor;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper.MinMax;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.PointStyle;
import org.achartengine.view.chart.TimeChart;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class TimeChartActivity extends XYChartActivity {

	// This is what fraction of the data span the axes limits will be padded by
	public static double HEADROOM_FOOTROOM_FRACTION = 0.1;
	
	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typepointline;
	}

	
	List<List<Date>> convertNumberToDateSeries(List<List<Number>> number_series) {
		List<List<Date>> output = new ArrayList<List<Date>>();
		for (List<Number> individual_series : number_series) {
			List<Date> converted_series = new ArrayList<Date>();
			output.add(converted_series);
			for (Number number : individual_series) {
				
				long long_value = number.longValue();
//				Log.d(TAG, "Long value pre-conversion: " + long_value);
				
				Date date = new Date( long_value );
//				Log.d(TAG, "Date value post-conversion: " + date);
				converted_series.add( date );
			}
		}
		return output;
	}

	
	// ---------------------------------------------
	@Override
	protected AbstractChart generateChartFromContentProvider(Uri intent_data) throws IllegalArgumentException {

		List<List<List<LabeledDatum>>> sorted_series_list = getGenericSortedSeriesData(intent_data, new LabeledDatumExtractor());


		if (! (sorted_series_list.size() >= 1) ) {
			throw new IllegalArgumentException("There are no series!");
		}


		List<List<String>> datam_labels = new ArrayList<List<String>>();


		List<List<Number>> x_axis_series;
		List<List<Number>> y_axis_series = null;
		if (sorted_series_list.size() == 1) {
			// XXX - Let the X-axis carry the only data.  This is different from all the other classes.
			x_axis_series = unzipSeriesDatumLabels( sorted_series_list.get( 0 ), datam_labels );
			y_axis_series = new ArrayList<List<Number>>();

		} else {
			x_axis_series = unzipSeriesDatumLabels( sorted_series_list.get( ColumnSchema.X_AXIS_INDEX ), datam_labels );
			y_axis_series = unzipSeriesDatumLabels( sorted_series_list.get( ColumnSchema.Y_AXIS_INDEX ), datam_labels );
		}


		String[] titles = getSortedSeriesTitles();
		if (titles.length != x_axis_series.size()) {
			throw new IllegalArgumentException("Titles count must match series count (X)!");
		} else if (titles.length != y_axis_series.size()) {
			throw new IllegalArgumentException("Titles count must match series count (Y)!");
		}


		// TODO: If there is no y-axis data, we probably want a histogram of the events.
		// Otherwise we could draw a timeline that consists of labeled vertical event bars
		// a la Firebug, or we could nix the bars and just draw labels at 45 degrees
		// like a historical timeline.

		int[] colors = new int[titles.length];
		PointStyle[] styles =  new PointStyle[titles.length];
		for (int i=0; i<titles.length; i++) {
			colors[i] = DEFAULT_COLORS[i % DEFAULT_COLORS.length];
			styles[i] = DEFAULT_STYLES[i % DEFAULT_STYLES.length];
		}



		List<String> axis_labels = getAxisTitles();





		XYMultipleSeriesRenderer renderer = org.achartengine.ChartGenHelper.buildRenderer(colors, styles);
		int length = renderer.getSeriesRendererCount();

		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}


		String chart_title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		String x_label = axis_labels.get( ColumnSchema.X_AXIS_INDEX );
		String y_label = axis_labels.get( ColumnSchema.Y_AXIS_INDEX );
		Log.d(TAG, "X LABEL: " + x_label);
		Log.d(TAG, "Y LABEL: " + y_label);
		Log.d(TAG, "chart_title: " + chart_title);

		org.achartengine.ChartGenHelper.setChartSettings(renderer, chart_title, x_label, y_label, Color.LTGRAY, Color.GRAY);


//		Log.i(TAG, "About to convert numbers to date series...");
		List<List<Date>> x_axis_date_series = convertNumberToDateSeries(x_axis_series);

//		Log.i(TAG, "Getting the axis limits...");
		
		MinMax x_axis_limits = getTimeAxisLimits(x_axis_series);
		MinMax y_axis_limits = getYAxisLimits(y_axis_series);
		Log.d(TAG, "Y axis bottom: " + y_axis_limits.min.doubleValue());
		Log.d(TAG, "Y axis top: " + y_axis_limits.max.doubleValue());
		
		
		org.achartengine.ChartGenHelper.setAxesExtents(
				renderer,
				x_axis_limits.min.longValue(),
				x_axis_limits.max.longValue(),
				y_axis_limits.min.doubleValue(),
				y_axis_limits.max.doubleValue());


//		Log.i(TAG, "About to build date dataset...");
		XYMultipleSeriesDataset dataset = org.achartengine.ChartGenHelper.buildDateDataset(titles, x_axis_date_series, y_axis_series);

//		Log.i(TAG, "Checking parameters...");
		ChartFactory.checkParameters(dataset, renderer);


//		Log.i(TAG, "Instantiating TimeChart...");
		TimeChart chart = new TimeChart(dataset, renderer);
		chart.setDateFormat("MM/dd/yyyy");
		chart.setYFormat("%.1f%%");
		
		return chart;
	}

	// =========================
	MinMax getTimeAxisLimits(List<List<Number>> time_axis_series) {
		
		MinMax time_minmax = new MinMax(time_axis_series);
		long time_values_span = (long) time_minmax.getSpan();
		long padding;
		if (time_values_span > 0) {
			padding = (long) (time_values_span*HEADROOM_FOOTROOM_FRACTION);
		} else {
			// Pad by one day.
			long day_ms = 1000*60*60*24;
			padding = day_ms;
		}
		
		long time_axis_lower_limit = time_minmax.min.longValue() - padding;
		long time_axis_upper_limit = time_minmax.max.longValue() + padding;
		return new MinMax(time_axis_lower_limit, time_axis_upper_limit);
	}
	// =========================
	MinMax getYAxisLimits(List<List<Number>> y_axis_series) {
		
		MinMax y_minmax = new MinMax(y_axis_series);
		double y_values_span = y_minmax.getSpan();
		double padding;
		if (y_values_span > 0) {
			padding = y_values_span*HEADROOM_FOOTROOM_FRACTION;
		} else {
			padding = y_minmax.min.doubleValue()*HEADROOM_FOOTROOM_FRACTION;
		}
		
		double y_axis_lower_limit = y_minmax.min.doubleValue() - padding;
		double y_axis_upper_limit = y_minmax.max.doubleValue() + padding;
		return new MinMax(y_axis_lower_limit, y_axis_upper_limit);
	}
}