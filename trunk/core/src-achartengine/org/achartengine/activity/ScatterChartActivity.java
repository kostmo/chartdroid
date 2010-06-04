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
import org.achartengine.activity.XYChartActivity.AxesException;
import org.achartengine.model.XYMultiSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.ScatterChart;
import org.achartengine.view.chart.XYChart;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class ScatterChartActivity extends XYSpatialChartActivity {

	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typepointline;
	}

	// ========================================================================
	@Override
	protected AbstractChart generateChartFromContentProvider(Uri intent_data) throws AxesException {


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

		XYMultiSeries dataset = org.achartengine.ChartGenHelper.buildDataset(axes_container.titles, axes_container.x_axis_series, axes_container.y_axis_series);

		ChartFactory.checkParameters(dataset, axes_container.renderer);

		XYChart chart = new ScatterChart(dataset, axes_container.renderer);

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

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_bar_chart, menu);

		menu.findItem(R.id.menu_toggle_stacked).setVisible(false);

		return true;
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_toggle_orientation:
		{

			XYMultipleSeriesRenderer renderer = ((XYChart) mChart).getRenderer();
			renderer.setOrientation( renderer.getOrientation().equals(
					XYMultipleSeriesRenderer.Orientation.HORIZONTAL)
					? XYMultipleSeriesRenderer.Orientation.VERTICAL
							: XYMultipleSeriesRenderer.Orientation.HORIZONTAL);

			mView.repaint();
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}
}