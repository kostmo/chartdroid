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
import com.googlecode.chartdroid.activity.prefs.ChartDisplayPreferences;
import com.googlecode.chartdroid.core.IntentConstants;

import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.util.MathHelper.MinMax;
import org.achartengine.view.FlowLayout;
import org.achartengine.view.VerticalLabelView;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.XYChart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


abstract public class XYChartActivity extends GraphicalActivity {

	/**
	 * Thrown when there is not at least one axis present
	 */
	public static class AxesException extends Exception {

		private static final long serialVersionUID = -1667855840214135417L;

		public AxesException(String message) {
			super(message);
		}
	}

	// ====================================================================
	protected int getLayoutResourceId() {
		return R.layout.xy_chart_activity;
	}


	// ====================================================================
	void setGridLinesFromPrefs(SharedPreferences prefs) {
    	boolean enable_grid_lines = prefs.getBoolean(ChartDisplayPreferences.PREFKEY_ENABLE_GRID_LINES, false);
    	boolean enable_grid_lines_horizontal = prefs.getBoolean(ChartDisplayPreferences.PREFKEY_ENABLE_HORIZONTAL_GRID_LINES, true);
    	boolean enable_grid_lines_vertical = prefs.getBoolean(ChartDisplayPreferences.PREFKEY_ENABLE_VERTICAL_GRID_LINES, true);

		XYChart xy_chart = (XYChart) mChart;
		xy_chart.getRenderer().setShowGrid(enable_grid_lines);
		xy_chart.getRenderer().setShowGridHorizontalLines(enable_grid_lines_horizontal);
		xy_chart.getRenderer().setShowGridVerticalLines(enable_grid_lines_vertical);
	}
	
	// ====================================================================
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	
        if (mChart != null) {

        	setGridLinesFromPrefs(prefs);
        	
        	if (mView != null) {
        		mView.invalidate();
        	}
        }
    }
	
	// ========================================================================
	MinMax getAxisLimits(List<List<Number>> multi_series, float fractional_span_margin) {
		
		MinMax minmax = new MinMax(multi_series);
		double values_span = minmax.getSpan();
		double padding;
		if (values_span > 0) {
			padding = values_span*fractional_span_margin;
		} else {
			if (minmax.min.doubleValue() != 0)
				padding = minmax.min.doubleValue()*fractional_span_margin;
			else
				padding = 1;
		}
		
		double lower_limit = minmax.min.doubleValue() - padding;
		double upper_limit = minmax.max.doubleValue() + padding;
		return new MinMax(lower_limit, upper_limit);
	}
	
	// ========================================================================
	MinMax getAxisLimits(List<List<Number>> multi_series) {
		return getAxisLimits(multi_series, HEADROOM_FOOTROOM_FRACTION);
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

		renderer.setXAxisSpan( getXAxisLimits(x_axis_series) );
		renderer.setYAxisSpan( getYAxisLimits(y_axis_series) );
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
		
		XYMultipleSeriesRenderer renderer = xy_chart.getRenderer();
		renderer.setShowXAxis(getIntent().getBooleanExtra(IntentConstants.Meta.Axes.EXTRA_AXIS_VISIBLE_X, true));
		renderer.setShowYAxis(getIntent().getBooleanExtra(IntentConstants.Meta.Axes.EXTRA_AXIS_VISIBLE_Y, true));		
		
		((TextView) findViewById(R.id.chart_x_axis_title)).setText( xy_chart.getRenderer().getXTitle() );
		((VerticalLabelView) findViewById(R.id.chart_y_axis_title)).setText( xy_chart.getRenderer().getYTitle() );
		
		FlowLayout predicate_layout = (FlowLayout) findViewById(R.id.predicate_layout);
		List<DataSeriesAttributes> series_attributes_list = getSeriesAttributesList(mChart);
		populateLegend(predicate_layout, series_attributes_list);
		
		setGridLinesFromPrefs(PreferenceManager.getDefaultSharedPreferences(this));
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