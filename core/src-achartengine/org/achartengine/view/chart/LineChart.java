/**
 * Copyright (C) 2009 SC 4ViewSoft SRL
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
package org.achartengine.view.chart;

import org.achartengine.model.XYMultiSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Style;

import java.util.ArrayList;
import java.util.List;


/**
 * The line chart rendering class.
 */
public class LineChart extends XYChart {

	/**
	 * Builds a new line chart instance.
	 * @param dataset the multiple series dataset
	 * @param renderer the multiple series renderer
	 */
	public LineChart(XYMultiSeries dataset, XYMultipleSeriesRenderer renderer) {
		super(dataset, renderer);
	}

	/**
	 * The graphical representation of a series.
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param points the array of points to be used for drawing the series
	 * @param seriesRenderer the series renderer
	 * @param yAxisValue the minimum value of the y axis
	 * @param seriesIndex the index of the series currently being drawn
	 */
	public void drawSeries(Canvas canvas, Paint paint, List<PointF> points,
			SimpleSeriesRenderer seriesRenderer,
			float xScale, float yScale,
			float yAxisValue, int seriesIndex) {

		XYSeriesRenderer renderer = (XYSeriesRenderer) seriesRenderer;
		if (renderer.isFillBelowLine()) {
			paint.setColor(renderer.getFillBelowLineColor());

			List<PointF> fill_points = new ArrayList<PointF>(points);
			fill_points.add(new PointF(points.get(points.size() - 1).x, yAxisValue));
			fill_points.add(new PointF(points.get(0).x, yAxisValue));

			paint.setStyle(Style.FILL);
			drawPath(canvas, fill_points, paint, true);
		}
		paint.setColor(seriesRenderer.getColor());
		paint.setStyle(Style.STROKE);
		drawPath(canvas, points, paint, false);
	}

	/**
	 * Returns if the chart should display the points as a certain shape.
	 * @param renderer the series renderer
	 */
	public boolean isRenderPoints(SimpleSeriesRenderer renderer) {
		return ((XYSeriesRenderer) renderer).getPointStyle() != PointStyle.POINT;
	}
}
