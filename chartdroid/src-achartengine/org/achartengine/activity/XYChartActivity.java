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
import org.achartengine.view.LabelView;
import org.achartengine.view.PredicateLayout;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.XYChart;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


abstract public class XYChartActivity extends GraphicalActivity {

	protected int getLayoutResourceId() {
		return R.layout.xy_chart_activity;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		XYChart xy_chart = (XYChart) mChart;
		((TextView) findViewById(R.id.chart_x_axis_title)).setText( xy_chart.getRenderer().getXTitle() );
		((LabelView) findViewById(R.id.chart_y_axis_title)).setText( xy_chart.getRenderer().getYTitle() );

		
		PredicateLayout predicate_layout = (PredicateLayout) findViewById(R.id.predicate_layout);
		List<DataSeriesAttributes> series_attributes_list = getSeriesAttributesList(mChart);
		populateLegend(predicate_layout, series_attributes_list);
	}


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





	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_bar_chart, menu);
//
//		menu.findItem(R.id.menu_toggle_stacked).setVisible(false);

		return true;
	}


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