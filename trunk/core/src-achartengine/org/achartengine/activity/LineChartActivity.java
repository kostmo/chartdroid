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

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultiSeries;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.LineChart;
import org.achartengine.view.chart.XYChart;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;

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
		XYMultiSeries dataset = org.achartengine.ChartGenHelper.buildDataset(
				axes_container.titles,
				axes_container.x_axis_series,
				axes_container.y_axis_series);

		org.achartengine.ChartGenHelper.setChartSettings(
				axes_container.renderer,
				getIntent().getStringExtra(Intent.EXTRA_TITLE),
				axes_container.axis_properties.get( ColumnSchema.X_AXIS_INDEX ).title,
				axes_container.axis_properties.get( ColumnSchema.Y_AXIS_INDEX ).title,
				Color.LTGRAY, Color.GRAY);

		ChartFactory.checkParameters(dataset, axes_container.renderer);

		XYChart chart = new LineChart(dataset, axes_container.renderer);
		for (int i = 0; i < axes_container.renderer.getSeriesRendererCount(); i++)
			((XYSeriesRenderer) axes_container.renderer.getSeriesRendererAt(i)).setFillPoints(true);

		setAxisFormats(getIntent(), chart);
		return chart;
	}
}