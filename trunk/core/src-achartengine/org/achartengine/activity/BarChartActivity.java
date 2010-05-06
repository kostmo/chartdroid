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
import org.achartengine.model.XYMultiSeries;
import org.achartengine.util.MathHelper.MinMax;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.BarChart;
import org.achartengine.view.chart.BarChart.Type;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
public class BarChartActivity extends XYSpatialChartActivity {

	@Override
	protected int getTitlebarIconResource() {
		return R.drawable.typebar;
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
				axes_container.axis_labels.get( ColumnSchema.X_AXIS_INDEX ),
				axes_container.axis_labels.get( ColumnSchema.Y_AXIS_INDEX ),
				Color.LTGRAY, Color.GRAY);

		ChartFactory.checkParameters(dataset, axes_container.renderer);

		BarChart chart = new BarChart(dataset, axes_container.renderer, Type.DEFAULT);

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

		menu.findItem(R.id.menu_toggle_stacked).setVisible(true);
		return true;
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_toggle_stacked:
		{
			BarChart bc = (BarChart) mChart;
			bc.setType( bc.getType().equals(Type.DEFAULT) ? Type.STACKED : Type.DEFAULT);

			mView.repaint();
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	// ========================================================================
	@Override
	MinMax getXAxisLimits(List<List<Number>> multi_series) {
		
		// Get most dense count:
		List<Integer> counts = new ArrayList<Integer>();
		for (List<Number> series : multi_series) counts.add(series.size());
		int biggest_size = Collections.max(counts);
		
		MinMax minmax = new MinMax(multi_series);
		double values_span = minmax.getSpan();
		double padding;
		if (values_span > 0) {
			padding = values_span/biggest_size;
		} else {
			padding = 1;
		}
		
		double lower_limit = minmax.min.doubleValue() - padding;
		double upper_limit = minmax.max.doubleValue() + padding;
		return new MinMax(lower_limit, upper_limit);
	}
}