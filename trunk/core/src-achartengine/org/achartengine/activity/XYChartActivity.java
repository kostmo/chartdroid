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

import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.util.MathHelper.MinMax;
import org.achartengine.view.VerticalLabelView;
import org.achartengine.view.FlowLayout;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.XYChart;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


abstract public class XYChartActivity extends GraphicalActivity {

	protected int getLayoutResourceId() {
		return R.layout.xy_chart_activity;
	}

	// ========================================================================
	MinMax getAxisLimits(List<List<Number>> multi_series) {
		
		MinMax y_minmax = new MinMax(multi_series);
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

	// ========================================================================
	abstract MinMax getXAxisLimits(List<List<Number>> multi_series);
	abstract MinMax getYAxisLimits(List<List<Number>> multi_series);
	
	// ========================================================================	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(
        		WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN );
	}

	// ========================================================================	
	void assignAxesExtents(XYMultipleSeriesRenderer renderer, List<List<Number>> x_axis_series, List<List<Number>> y_axis_series) {
		MinMax x_axis_limits = getXAxisLimits(x_axis_series);
		MinMax y_axis_limits = getYAxisLimits(y_axis_series);
		Log.d(TAG, "Y axis bottom: " + y_axis_limits.min.doubleValue());
		Log.d(TAG, "Y axis top: " + y_axis_limits.max.doubleValue());

		org.achartengine.ChartGenHelper.setAxesExtents(
				renderer,
				x_axis_limits.min.longValue(),
				x_axis_limits.max.longValue(),
				y_axis_limits.min.doubleValue(),
				y_axis_limits.max.doubleValue());
	}

	// ========================================================================	
	@Override
	protected void postChartPopulationCallback() {
		XYChart xy_chart = (XYChart) mChart;
		
		if (xy_chart == null) {
			
			Log.e(TAG, "Chart is null; finishing activity.");
			
			finish();
			return;
		}
		
		
		((TextView) findViewById(R.id.chart_x_axis_title)).setText( xy_chart.getRenderer().getXTitle() );
		((VerticalLabelView) findViewById(R.id.chart_y_axis_title)).setText( xy_chart.getRenderer().getYTitle() );
		
		FlowLayout predicate_layout = (FlowLayout) findViewById(R.id.predicate_layout);
		List<DataSeriesAttributes> series_attributes_list = getSeriesAttributesList(mChart);
		populateLegend(predicate_layout, series_attributes_list);
	}

	// ========================================================================	
	@Override
	protected List<DataSeriesAttributes> getSeriesAttributesList(AbstractChart chart) {
		

		XYChart xy_chart = (XYChart) chart;
		
		// Zip the series attributes
		List<DataSeriesAttributes> series_attributes_list = new ArrayList<DataSeriesAttributes>();
		int i=0;
		for (SimpleSeriesRenderer renderer : xy_chart.getRenderer().getSeriesRenderers()) {
			DataSeriesAttributes series = new DataSeriesAttributes();
			series.color = renderer.getColor();
			series.title = xy_chart.getDataset().getSeriesAt(i).getTitle();
				
			series_attributes_list.add( series );
			i++;
		}
		
		return series_attributes_list;
	}

	// ========================================================================	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_bar_chart, menu);
//
//		menu.findItem(R.id.menu_toggle_stacked).setVisible(false);

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