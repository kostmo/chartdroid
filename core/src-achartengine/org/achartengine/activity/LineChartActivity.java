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
import com.googlecode.chartdroid.core.ColumnSchema;
import com.googlecode.chartdroid.core.IntentConstants;

import org.achartengine.ChartFactory;
import org.achartengine.activity.GraphicalActivity.AxesContainer;
import org.achartengine.consumer.DataCollector;
import org.achartengine.consumer.DoubleDatumExtractor;
import org.achartengine.consumer.DataCollector.SeriesMetaData;
import org.achartengine.model.XYMultiSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper.MinMax;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.LineChart;
import org.achartengine.view.chart.XYChart;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class LineChartActivity extends XYSpatialChartActivity {

	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typepointline;
	}

	// ========================================================================
	@Override
	protected AbstractChart generateChartFromContentProvider(Uri intent_data) {


		RenderingAxesContainer axes_container = getAxesSets(intent_data);
		
		int length = axes_container.renderer.getSeriesRendererCount();

		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) axes_container.renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}



		String chart_title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		String x_label = axes_container.axis_labels.get( ColumnSchema.X_AXIS_INDEX );
		String y_label = axes_container.axis_labels.get( ColumnSchema.Y_AXIS_INDEX );
		Log.d(TAG, "X LABEL: " + x_label);
		Log.d(TAG, "X LABEL: " + y_label);
		Log.d(TAG, "chart_title: " + chart_title);




		org.achartengine.ChartGenHelper.setChartSettings(axes_container.renderer, chart_title, x_label, y_label,
				Color.LTGRAY, Color.GRAY);
		axes_container.renderer.setXLabels(12);
		axes_container.renderer.setYLabels(10);

		// FIXME: Generate dynamically
//		org.achartengine.ChartGenHelper.setAxesExtents(renderer, 0.5, 12.5, 0, 32);


		XYMultiSeries dataset = org.achartengine.ChartGenHelper.buildDataset(axes_container.titles, axes_container.x_axis_series, axes_container.y_axis_series);

		ChartFactory.checkParameters(dataset, axes_container.renderer);

		XYChart chart = new LineChart(dataset, axes_container.renderer);

		String x_format = getIntent().getStringExtra(IntentConstants.EXTRA_FORMAT_STRING_X);
		if (x_format != null) chart.setXFormat(x_format);

		String y_format = getIntent().getStringExtra(IntentConstants.EXTRA_FORMAT_STRING_Y);
		if (y_format != null) chart.setYFormat(y_format);

		return chart;
	}
}