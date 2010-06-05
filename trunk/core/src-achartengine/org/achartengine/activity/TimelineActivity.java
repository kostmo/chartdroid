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
import org.achartengine.consumer.LabeledDatumExtractor;
import org.achartengine.consumer.DataCollector.AxesMetaData;
import org.achartengine.consumer.DataCollector.LabeledDatum;
import org.achartengine.consumer.DataCollector.SeriesMetaData;
import org.achartengine.model.XYMultiSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.TimeChart;

import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class TimelineActivity extends XYTemporalChartActivity {

	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typepointline;
	}

	// ========================================================================
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

	// ========================================================================
	@Override
	protected AbstractChart generateChartFromContentProvider(Uri intent_data) throws IllegalArgumentException {

		List<List<List<LabeledDatum>>> sorted_series_list = DataCollector.getGenericSortedSeriesData(intent_data, getContentResolver(), new LabeledDatumExtractor());


		if ( !(sorted_series_list.size() > 0) ) {
			throw new IllegalArgumentException("There are no series!");
		}


		List<List<String>> datam_labels = new ArrayList<List<String>>();


		List<List<Number>> x_axis_series;
		List<List<Number>> y_axis_series = null;
		if (sorted_series_list.size() == 1) {
			// XXX - Let the X-axis carry the only data.  This is different from all the other classes.
			x_axis_series = DataCollector.unzipSeriesDatumLabels( sorted_series_list.get( 0 ), datam_labels );
			y_axis_series = new ArrayList<List<Number>>();

		} else {
			x_axis_series = DataCollector.unzipSeriesDatumLabels( sorted_series_list.get( ColumnSchema.X_AXIS_INDEX ), datam_labels );
			y_axis_series = DataCollector.unzipSeriesDatumLabels( sorted_series_list.get( ColumnSchema.Y_AXIS_INDEX ), datam_labels );
		}

		List<SeriesMetaData> series_meta_data = DataCollector.getSeriesMetaData( getIntent(), getContentResolver() );
		String[] titles = new String[series_meta_data.size()];
		for (int i=0; i<series_meta_data.size(); i++)
			titles[i] = series_meta_data.get(i).title;
			
		if (titles.length != x_axis_series.size()) {
			throw new IllegalArgumentException("Titles count must match series count (X)!");
		} else if (titles.length != y_axis_series.size()) {
			throw new IllegalArgumentException("Titles count must match series count (Y)!");
		}


		// TODO: If there is no y-axis data, we probably want a histogram of the events.
		// Otherwise we could draw a timeline that consists of labeled vertical event bars
		// a la Firebug, or we could nix the bars and just draw labels at 45 degrees
		// like a historical timeline.



		List<AxesMetaData> axis_labels = DataCollector.getAxisTitles(getIntent(), getContentResolver());


		XYMultipleSeriesRenderer renderer = org.achartengine.ChartGenHelper.buildRenderer(series_meta_data);
		int length = renderer.getSeriesRendererCount();

		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		assignChartLabels(axis_labels, renderer);



		Log.i(TAG, "Getting the axis limits...");
		assignAxesExtents(renderer, x_axis_series, y_axis_series);

		
		

//		Log.i(TAG, "About to convert numbers to date series...");
		List<List<Date>> x_axis_date_series = convertNumberToDateSeries(x_axis_series);




//		Log.i(TAG, "About to build date dataset...");
		XYMultiSeries dataset = org.achartengine.ChartGenHelper.buildDateDataset(titles, x_axis_date_series, y_axis_series);

//		Log.i(TAG, "Checking parameters...");
		ChartFactory.checkParameters(dataset, renderer);


//		Log.i(TAG, "Instantiating TimeChart...");
		TimeChart chart = new TimeChart(dataset, renderer);
		chart.setDateFormat("MM/dd/yyyy");
		
		String passed_format_string = getIntent().getStringExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y);
		String y_format = passed_format_string != null ? passed_format_string : "%.1f%%";
		chart.setYFormat(y_format);
		
		String passed_secondary_format_string = getIntent().getStringExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y_SECONDARY);
		String y_secondary_format = passed_secondary_format_string != null ? passed_secondary_format_string : "%.1f%%";
		chart.setYSecondaryFormat(y_secondary_format);
		
		return chart;
	}
}