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
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Paint.Style;

import java.util.List;

/**
 * The scatter chart rendering class.
 */
public class ScatterChart extends XYChart {
	/** The point shape size. */
	private static final float SIZE = 3;
	/** The legend shape width. */
	private static final int SHAPE_WIDTH = 10;

	/**
	 * Builds a new scatter chart instance.
	 * 
	 * @param dataset the multiple series dataset
	 * @param renderer the multiple series renderer
	 */
	
	Path triangle, diamond;
	public ScatterChart(XYMultiSeries dataset, XYMultipleSeriesRenderer renderer) {
		super(dataset, renderer);
		
		triangle = new Path();
		triangle.moveTo(0, -SIZE - SIZE/2);
		triangle.lineTo(-SIZE,  SIZE);
		triangle.lineTo(SIZE,  SIZE);
		triangle.close();

		diamond = new Path();
		diamond.moveTo(0, -SIZE);
		diamond.lineTo(-SIZE, 0);
		diamond.lineTo(0, SIZE);
		diamond.lineTo(SIZE, 0);
		diamond.close();
	}

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
	public void drawSeries(Canvas canvas, Paint paint, List<PointF> points,
			SimpleSeriesRenderer seriesRenderer,
			float xScale, float yScale,
			float yAxisValue, int seriesIndex) {
		
		XYSeriesRenderer renderer = (XYSeriesRenderer) seriesRenderer;
		paint.setColor(renderer.getColor());
		if (renderer.isFillPoints()) {
			paint.setStyle(Style.FILL);
		} else {
			paint.setStyle(Style.STROKE);
		}
		int length = points.size();
		switch (renderer.getPointStyle()) {
		case X:
			for (PointF point : points) {
				drawX(canvas, paint, point);
			}
			break;
		case CIRCLE:
			for (PointF point : points) {
				drawCircle(canvas, paint, point);
			}
			break;
		case TRIANGLE:
			for (PointF point : points) {
				drawTriangle(canvas, paint, point);
			}
			break;
		case SQUARE:
			for (PointF point : points) {
				drawSquare(canvas, paint, point);
			}
			break;
		case DIAMOND:
			for (PointF point : points) {
				drawDiamond(canvas, paint, point);
			}
			break;
		case POINT:

			for (PointF point : points) {
				canvas.drawPoint(point.x, point.y, paint);
			}

			break;
		}
	}


	/**
	 * The graphical representation of an X point shape.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 */
	private void drawX(Canvas canvas, Paint paint, PointF point) {
		canvas.drawLine(point.x - SIZE, point.y - SIZE, point.x + SIZE, point.y + SIZE, paint);
		canvas.drawLine(point.x + SIZE, point.y - SIZE, point.x - SIZE, point.y + SIZE, paint);
	}

	/**
	 * The graphical representation of a circle point shape.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 */
	private void drawCircle(Canvas canvas, Paint paint, PointF point) {
		canvas.drawCircle(point.x, point.y, SIZE, paint);
	}

	

	private void drawSomeShape(Canvas canvas, Paint paint, Path path, PointF point) {
		canvas.save();
		canvas.translate(point.x, point.y);
	    canvas.drawPath(path, paint);
		canvas.restore();
	}
	
	/**
	 * The graphical representation of a triangle point shape.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param path the triangle path
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 */
	private void drawTriangle(Canvas canvas, Paint paint, PointF point) {
		
		drawSomeShape(canvas, paint, triangle, point);
	}

	/**
	 * The graphical representation of a square point shape.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 */
	private void drawSquare(Canvas canvas, Paint paint, PointF point) {
		canvas.drawRect(point.x - SIZE, point.y - SIZE, point.x + SIZE, point.y + SIZE, paint);
	}

	/**
	 * The graphical representation of a diamond point shape.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param path the diamond path
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 */
	private void drawDiamond(Canvas canvas, Paint paint, PointF point) {
		
		drawSomeShape(canvas, paint, diamond, point);
		
	}

}