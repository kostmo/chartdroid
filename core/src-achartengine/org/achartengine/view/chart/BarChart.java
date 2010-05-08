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
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.util.Log;

import java.util.List;

/**
 * The bar chart rendering class.
 */
public class BarChart extends XYChart {

	/** The chart type. */
	private Type mType = Type.DEFAULT;

	// ========================================================================
	/**
	 * The bar chart type enum.
	 */
	public enum Type {
		DEFAULT, STACKED;
	}

	// ========================================================================
	/**
	 * Builds a new bar chart instance.
	 * 
	 * @param dataset the multiple series dataset
	 * @param renderer the multiple series renderer
	 * @param type the bar chart type
	 */
	public BarChart(XYMultiSeries dataset, XYMultipleSeriesRenderer renderer, Type type) {
		super(dataset, renderer);
		mType = type;
	}

	// ========================================================================
	public Type getType() {
		return mType;
	}

	// ========================================================================
	public void setType(Type type) {
		mType = type;
	}

	// ========================================================================
	/**
	 * The graphical representation of a series.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param points the array of points to be used for drawing the series
	 * @param seriesRenderer the series renderer
	 * @param yAxisValue the minimum value of the y axis
	 * @param seriesIndex the index of the series currently being drawn
	 */
	public void drawSeries(Canvas canvas,
			Paint paint,
			List<PointF> points,
			SimpleSeriesRenderer seriesRenderer,
			float xScale, float yScale,
			float yAxisValue,
			int seriesIndex) {
		
		int series_count = mDataset.getSeriesCount();

		paint.setColor(seriesRenderer.getColor());
		paint.setStyle(Style.FILL);
		float bar_width = getBarWidth(points, series_count);
		
		boolean zero_bar_width = bar_width == 0;
		if (zero_bar_width) bar_width = 2*xScale/series_count;

		for (PointF point : points) {
			if (mType == Type.STACKED) {
				canvas.drawRect(point.x - bar_width/2, point.y, point.x + bar_width/2, yAxisValue, paint);
			} else {
				float startX = point.x - (series_count/2f - seriesIndex) * bar_width;
				canvas.drawRect(startX, point.y, startX + bar_width, yAxisValue, paint);
			}
		}
	}

	// ========================================================================
	/**
	 * The graphical representation of the series values as text.
	 * 
	 * @param canvas the canvas to paint to
	 * @param series the series to be painted
	 * @param paint the paint to be used for drawing
	 * @param points the array of points to be used for drawing the series
	 * @param seriesIndex the index of the series currently being drawn
	 */
	protected void drawChartValuesText(Canvas canvas, XYSeries series, Paint paint, List<PointF> points,
			int seriesIndex) {
		int seriesNr = mDataset.getSeriesCount();
		float halfDiffX = getBarWidth(points, seriesNr);
		
		int k=0;
		for (PointF point : points) {
			float x = point.x;
			if (mType == Type.DEFAULT) {
				x += seriesIndex * 2 * halfDiffX - (seriesNr - 1.5f) * halfDiffX;
			}
			drawText(canvas, getLabel(series.getY(k)),
					x,
					point.y - 3.5f,	// FIXME Magic number
					paint, 0);
			
			k++;
		}
	}

	// ========================================================================
	private float getBarWidth(List<PointF> points, int series_count) {
		float end_x = (points.get(points.size() - 1)).x;
		float start_x = points.get(0).x;
		float x_delta = end_x - start_x;
		float halfDiffX = x_delta / points.size();

		if (mType != Type.STACKED) {
			halfDiffX /= series_count;
//			Log.e(TAG, "series count: " + series_count + "; final bin width: " + halfDiffX);
		}
		return halfDiffX;
	}
}
