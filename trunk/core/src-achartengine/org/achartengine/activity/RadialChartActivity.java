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

import org.achartengine.consumer.DataCollector;
import org.achartengine.consumer.LabeledDatumExtractor;
import org.achartengine.consumer.DataCollector.LabeledDatum;
import org.achartengine.consumer.DataCollector.SeriesMetaData;
import org.achartengine.view.FlowLayout;

import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * An activity that encapsulates a graphical view of the chart.
 */
public abstract class RadialChartActivity extends GraphicalActivity {
	
	// ========================================================================
	@Override
	protected int getLayoutResourceId() {
		return R.layout.simple_chart_activity;
	}

	// ========================================================================
	@Override
	protected void postChartPopulationCallback() {

		FlowLayout predicate_layout = (FlowLayout) findViewById(R.id.predicate_layout);
		List<DataSeriesAttributes> series_attributes_list = getSeriesAttributesList(mChart);
		populateLegend(predicate_layout, series_attributes_list);
	}
	

	// ========================================================================
	AxesContainer getAxesSets(Uri intent_data) {
		AxesContainer axes_container = new AxesContainer();
		
		List<List<List<LabeledDatum>>> sorted_series_list = DataCollector.getGenericSortedSeriesData(intent_data, getContentResolver(), new LabeledDatumExtractor());



		assert( sorted_series_list.size() >= 1 );


		axes_container.datam_labels = new ArrayList<List<String>>();


		if (sorted_series_list.size() == 1) {
			// Let the Y-axis carry the only data.
			axes_container.x_axis_series = new ArrayList<List<Number>>();
			axes_container.y_axis_series = DataCollector.unzipSeriesDatumLabels( sorted_series_list.get( 0 ), axes_container.datam_labels);

		} else {
			axes_container.x_axis_series = DataCollector.unzipSeriesDatumLabels( sorted_series_list.get( ColumnSchema.X_AXIS_INDEX ), axes_container.datam_labels );
			axes_container.y_axis_series = DataCollector.unzipSeriesDatumLabels( sorted_series_list.get( ColumnSchema.Y_AXIS_INDEX ), axes_container.datam_labels );
		}





		assert (axes_container.x_axis_series.size() == axes_container.y_axis_series.size()
				|| axes_container.x_axis_series.size() == 1
				|| axes_container.x_axis_series.size() == 0);

		List<SeriesMetaData> series_meta_data = DataCollector.getSeriesMetaData( getIntent(), getContentResolver() );
		axes_container.titles = new String[series_meta_data.size()];
		for (int i=0; i<series_meta_data.size(); i++)
			axes_container.titles[i] = series_meta_data.get(i).title;
		
		
		assert (axes_container.titles.length == axes_container.y_axis_series.size());
		assert (axes_container.titles.length == axes_container.y_axis_series.get(0).size());


		// If there is no x-axis data, just number the y-elements.
		List<Number> prototypical_x_values; 
		if (axes_container.x_axis_series.size() == 0) {
			for (int i=0; i < axes_container.y_axis_series.size(); i++) {
				prototypical_x_values = new ArrayList<Number>();
				axes_container.x_axis_series.add( prototypical_x_values );
				for (int j=0; j < axes_container.y_axis_series.get(i).size(); j++)
					prototypical_x_values.add(j);
			}
		}


		// Replicate the X-axis data for each series if necessary
		if (axes_container.x_axis_series.size() == 1) {
			Log.i(TAG, "Replicating x-axis series...");
			prototypical_x_values = axes_container.x_axis_series.get(0);
			Log.d(TAG, "Size of prototypical x-set: " + prototypical_x_values.size());
			while (axes_container.x_axis_series.size() < axes_container.titles.length)
				axes_container.x_axis_series.add( prototypical_x_values );
		}

		return axes_container;
	}
}