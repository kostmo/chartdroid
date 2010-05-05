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
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Style;

import java.util.List;

/**
 * The bubble chart rendering class.
 */
public class BubbleChart extends XYChart {

  /** The minimum bubble size. */
  private static final int MIN_BUBBLE_SIZE = 2;

  /** The maximum bubble size. */
  private static final int MAX_BUBBLE_SIZE = 20;

  /**
   * Builds a new bubble chart instance.
   * 
   * @param dataset the multiple series dataset
   * @param renderer the multiple series renderer
   */
  public BubbleChart(XYMultiSeries dataset, XYMultipleSeriesRenderer renderer) {
    super(dataset, renderer);
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
    paint.setStyle(Style.FILL);
    XYValueSeries series = (XYValueSeries) mDataset.getSeriesAt(seriesIndex);
    double max = series.getMaxValue();

    double coef = MAX_BUBBLE_SIZE / max;
    
    int i = 0;
    for (PointF point : points) {
      double size = series.getValue(i).doubleValue() * coef + MIN_BUBBLE_SIZE;
      drawCircle(canvas, paint, point.x, point.y, (float) size);
      
      i++;
    }
  }

  /**
   * The graphical representation of a circle point shape.
   * 
   * @param canvas the canvas to paint to
   * @param paint the paint to be used for drawing
   * @param x the x value of the point the shape should be drawn at
   * @param y the y value of the point the shape should be drawn at
   * @param radius the bubble radius
   */
  private void drawCircle(Canvas canvas, Paint paint, float x, float y, float radius) {
    canvas.drawCircle(x, y, radius, paint);
  }
}