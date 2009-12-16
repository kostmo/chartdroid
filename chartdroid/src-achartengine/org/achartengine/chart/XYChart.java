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
package org.achartengine.chart;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;
import org.achartengine.util.MathHelper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.util.Log;

import java.util.List;




/**
 * The XY chart rendering class.
 */
public abstract class XYChart extends AbstractChart {
    

    final static public String TAG = "XYChart";
    
    
  public XYMultipleSeriesRenderer getRenderer() {
      return mRenderer;
  }
    
  /** The multiple series dataset. */
  protected XYMultipleSeriesDataset mDataset;
  /** The multiple series renderer. */
  protected XYMultipleSeriesRenderer mRenderer;
  /** The current scale value. */
  private float mScale;
  /** The current translate value. */
  private float mTranslate;
  /** The canvas center point. */
  private PointF mCenter;
  /** The grid color. */
  protected static final int GRID_COLOR = Color.argb(75, 200, 200, 200);

  /**
   * Builds a new XY chart instance.
   * 
   * @param dataset the multiple series dataset
   * @param renderer the multiple series renderer
   */
  public XYChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
    mDataset = dataset;
    mRenderer = renderer;
  }

  /**
   * The graphical representation of the XY chart.
   * 
   * @param canvas the canvas to paint to
   * @param x the top left x value of the view to draw to
   * @param y the top left y value of the view to draw to
   * @param width the width of the view to draw to
   * @param height the height of the view to draw to
   */
  @Override
  public void draw(Canvas canvas, int x, int y, int width, int height) {
    Paint paint = new Paint();
    paint.setAntiAlias(getAntiAliased());
    int legendSize = 30;
    if (mRenderer.isShowLegend()) {
      legendSize = height / 5;
    }

    drawBackground(mRenderer, canvas, x, y, width, height, paint);

    

    
    Orientation or = mRenderer.getOrientation();
    int angle = or.getAngle();
    boolean rotate = angle == 90;
    mScale = (float) (height) / width;
    mTranslate = Math.abs(width - height) / 2;
    if (mScale < 1) {
      mTranslate *= -1;
    }
    mCenter = new PointF((x + width) / 2, (y + height) / 2);
    if (rotate) {
      transform(canvas, angle, false);
    }
    double minX = mRenderer.getXAxisMin();
    double maxX = mRenderer.getXAxisMax();
    double minY = mRenderer.getYAxisMin();
    double maxY = mRenderer.getYAxisMax();
    boolean isMinXSet = mRenderer.isMinXSet();
    boolean isMaxXSet = mRenderer.isMaxXSet();
    boolean isMinYSet = mRenderer.isMinYSet();
    boolean isMaxYSet = mRenderer.isMaxYSet();
    double xPixelsPerUnit = 0;
    double yPixelsPerUnit = 0;
    int sLength = mDataset.getSeriesCount();
    String[] titles = new String[sLength];
    for (int i = 0; i < sLength; i++) {
      XYSeries series = mDataset.getSeriesAt(i);
      titles[i] = series.getTitle();
      if (series.getItemCount() == 0) {
        continue;
      }
      if (!isMinXSet) {
        Number minimumX = series.getMinX();
        minX = Math.min(minX, minimumX.doubleValue());
      }
      if (!isMaxXSet) {
        Number maximumX = series.getMaxX();
        maxX = Math.max(maxX, maximumX.doubleValue());
      }
      if (!isMinYSet) {
        Number minimumY = series.getMinY();
        minY = Math.min(minY, minimumY.doubleValue());
      }
      if (!isMaxYSet) {
        Number maximumY = series.getMaxY();
        maxY = Math.max(maxY, maximumY.doubleValue());
      }
    }
    
    


//  boolean showLabels = mRenderer.isShowLabels() && hasValues;
    boolean showLabels = mRenderer.isShowLabels();
    boolean showGrid = mRenderer.isShowGrid();
    

    Rect y_title_bounds = new Rect();
    if (showLabels || showGrid) {

        if (showLabels) {
          paint.setColor(mRenderer.getLabelsColor());
          paint.setTextSize(12);
          paint.setTextAlign(Align.CENTER);
          
          String y_title = mRenderer.getYTitle();
          paint.getTextBounds(y_title, 0, y_title.length(), y_title_bounds);
        }
    }
    
    
    
    Log.d(TAG, "Y-title rect top: " + y_title_bounds.top);
    Log.d(TAG, "Y-title rect bottom: " + y_title_bounds.bottom);
    Log.d(TAG, "Y-title rect left: " + y_title_bounds.left);
    Log.d(TAG, "Y-title rect right: " + y_title_bounds.right);

    int y_label_height = y_title_bounds.bottom - y_title_bounds.top;

    float hash_mark_width = 0;
    float label_clearance = 2;
    // Measure all y-axis label widths to determine the axis line position
    if (showLabels) {

        List<Double> yLabels = MathHelper.getLabels(minY, maxY, mRenderer.getYLabels());
        int length = yLabels.size();
        
        float[] label_widths = new float[length];
        float max_label_width = 0;
        for (int i = 0; i < length; i++) {
            float label_width = paint.measureText( getLabel( yLabels.get(i) ) );
            label_widths[i] = width;
            max_label_width = Math.max(label_width, max_label_width);
        }
    
        
        hash_mark_width = max_label_width + label_clearance;
    }
    
    int left = x + y_label_height + (int) Math.ceil(hash_mark_width);
//    int left = x + 20;
    int top = y + 10;
    int right = x + width;
    int bottom = y + height - legendSize;
    if (or == Orientation.VERTICAL) {
        right -= legendSize;
        bottom += legendSize - 20;
    }
    
    
    if (maxX - minX != 0) {
        xPixelsPerUnit = (right - left) / (maxX - minX);
      }
      if (maxY - minY != 0) {
        yPixelsPerUnit = (float) ((bottom - top) / (maxY - minY));
      }
    
    if (showLabels || showGrid) {
        
        

        if (showLabels) {
            

            String y_title = mRenderer.getYTitle();
            
          if (or == Orientation.HORIZONTAL) {
            drawText(canvas, mRenderer.getXTitle(), x + width / 2, bottom + 24, paint, 0);
            drawText(canvas, y_title, x + y_label_height, y + height / 2, paint, -90);
            paint.setTextSize(15);
            drawText(canvas, mRenderer.getChartTitle(), x + width / 2, top + 10, paint, 0);
            
          } else if (or == Orientation.VERTICAL) {
            drawText(canvas, mRenderer.getXTitle(), x + width / 2, y + height - 10, paint, -90);
            drawText(canvas, y_title, right + y_label_height, y + height / 2, paint, 0);
            paint.setTextSize(15);
            drawText(canvas, mRenderer.getChartTitle(), x + 14, top + height / 2, paint, 0);
          }
        }
        
        
        
        
        
        
        
      List<Double> xLabels = MathHelper.getLabels(minX, maxX, mRenderer.getXLabels());
      List<Double> yLabels = MathHelper.getLabels(minY, maxY, mRenderer.getYLabels());
      if (showLabels) {
        paint.setColor(mRenderer.getLabelsColor());
        paint.setTextSize(9);
        paint.setTypeface(DefaultRenderer.REGULAR_TEXT_FONT);
        paint.setTextAlign(Align.CENTER);
      }
      drawXLabels(xLabels, mRenderer.getXTextLabelLocations(), canvas, paint, left, top, bottom,
          xPixelsPerUnit, minX);
      int length = yLabels.size();
      
      

      
      
      paint.setTextAlign(Align.RIGHT);
      for (int i = 0; i < length; i++) {
        double label = yLabels.get(i);
        float yLabel = (float) (bottom - yPixelsPerUnit * (label - minY));

        
        float grid_line_startx, grid_line_stopx, hash_mark_startx, hash_mark_stopx;
        float label_x_offset;

        if (or == Orientation.HORIZONTAL) {
            grid_line_startx = left;
            grid_line_stopx = right;
            hash_mark_startx = grid_line_startx - hash_mark_width;
            hash_mark_stopx = grid_line_startx;
            
            label_x_offset = left - label_clearance;

//      } else if (or == Orientation.VERTICAL) {
        } else { 
            grid_line_startx = right;
            grid_line_stopx = left;
            hash_mark_startx = grid_line_startx + hash_mark_width;
            hash_mark_stopx = grid_line_startx;

//            label_x_offset = right + 10;
            label_x_offset = right - label_clearance;
        }
        
        if (showLabels) {
            paint.setColor(mRenderer.getLabelsColor());
//            paint.setColor(Color.MAGENTA);    // FIXME
            canvas.drawLine(hash_mark_startx, yLabel, hash_mark_stopx, yLabel, paint);
            
            String label_string = getLabel(label);
            paint.measureText(label_string);
            drawText(canvas, getLabel(label), label_x_offset, yLabel - 2, paint, 0);
          }
        
        if (showGrid) {
            paint.setColor(GRID_COLOR);
//            paint.setColor(Color.GREEN);    // FIXME
            canvas.drawLine(grid_line_startx, yLabel, grid_line_stopx, yLabel, paint);
        }
      }

      

      
      
      
    }

    if (or == Orientation.HORIZONTAL) {
      drawLegend(canvas, mRenderer, titles, left, right, y, width, height, legendSize, paint);
    } else if (or == Orientation.VERTICAL) {
      transform(canvas, angle, true);
      drawLegend(canvas, mRenderer, titles, left, right, y, width, height, legendSize, paint);
      transform(canvas, angle, false);
    }
    if (mRenderer.isShowAxes()) {
      paint.setColor(mRenderer.getAxesColor());
      canvas.drawLine(left, bottom, right, bottom, paint);
      if (or == Orientation.HORIZONTAL) {
        canvas.drawLine(left, top, left, bottom, paint);
      } else if (or == Orientation.VERTICAL) {
        canvas.drawLine(right, top, right, bottom, paint);
      }
    }
    
    
    
    
    
    
    
    

    


      boolean hasValues = false;
      for (int i = 0; i < sLength; i++) {
        XYSeries series = mDataset.getSeriesAt(i);
        if (series.getItemCount() == 0) {
          continue;
        }
        hasValues = true;
        SimpleSeriesRenderer seriesRenderer = mRenderer.getSeriesRendererAt(i);
        int originalValuesLength = series.getItemCount();
        float[] points = null;
        int valuesLength = originalValuesLength;
        int length = valuesLength * 2;
        points = new float[length];
        for (int j = 0; j < length; j += 2) {
          int index = j / 2;
          points[j] = (float) (left + xPixelsPerUnit * (series.getX(index).doubleValue() - minX));
          points[j + 1] = (float) (bottom - yPixelsPerUnit * (series.getY(index).doubleValue() - minY));
        }
        drawSeries(canvas, paint, points, seriesRenderer, Math.min(bottom,
            (float) (bottom + yPixelsPerUnit * minY)), i);
        if (isRenderPoints(seriesRenderer)) {
          ScatterChart pointsChart = new ScatterChart(mDataset, mRenderer);
          pointsChart.drawSeries(canvas, paint, points, seriesRenderer, 0, i);
        }
        paint.setTextSize(9);
        if (or == Orientation.HORIZONTAL) {
          paint.setTextAlign(Align.CENTER);
        } else {
          paint.setTextAlign(Align.LEFT);
        }
        if (mRenderer.isDisplayChartValues()) {
          drawChartValuesText(canvas, series, paint, points, i);
        }
      }
    
    
    
    
    
    
    
    
    
    
    
    if (rotate) {
      transform(canvas, angle, true);
    }
  }

  /**
   * The graphical representation of the series values as text.
   * 
   * @param canvas the canvas to paint to
   * @param series the series to be painted
   * @param paint the paint to be used for drawing
   * @param points the array of points to be used for drawing the series
   * @param seriesIndex the index of the series currently being drawn
   */
  protected void drawChartValuesText(Canvas canvas, XYSeries series, Paint paint, float[] points,
      int seriesIndex) {
    for (int k = 0; k < points.length; k += 2) {
      drawText(canvas, getLabel(series.getY(k / 2)), points[k], points[k + 1] - 3.5f, paint, 0);
    }
  }

  /**
   * The graphical representation of a text, to handle both HORIZONTAL and
   * VERTICAL orientations and extra rotation angles.
   * 
   * @param canvas the canvas to paint to
   * @param text the text to be rendered
   * @param x the X axis location of the text
   * @param y the Y axis location of the text
   * @param paint the paint to be used for drawing
   * @param extraAngle the array of points to be used for drawing the series
   */
  protected void drawText(Canvas canvas, String text, float x, float y, Paint paint, int extraAngle) {
    int angle = -mRenderer.getOrientation().getAngle() + extraAngle;
    if (angle != 0) {
      // canvas.scale(1 / mScale, mScale);
      canvas.rotate(angle, x, y);
    }
    canvas.drawText(text, x, y, paint);
    if (angle != 0) {
      canvas.rotate(-angle, x, y);
      // canvas.scale(mScale, 1 / mScale);
    }
  }

  /**
   * Transform the canvas such as it can handle both HORIZONTAL and VERTICAL
   * orientations.
   * 
   * @param canvas the canvas to paint to
   * @param angle the angle of rotation
   * @param inverse if the inverse transform needs to be applied
   */
  private void transform(Canvas canvas, float angle, boolean inverse) {
    if (inverse) {
      canvas.scale(1 / mScale, mScale);
      canvas.translate(mTranslate, -mTranslate);
      canvas.rotate(-angle, mCenter.x, mCenter.y);
    } else {
      canvas.rotate(angle, mCenter.x, mCenter.y);
      canvas.translate(-mTranslate, mTranslate);
      canvas.scale(mScale, 1 / mScale);
    }
  }

  /**
   * Makes sure the fraction digit is not displayed, if not needed.
   * 
   * @param label the input label value
   * @return the label without the useless fraction digit
   */
  protected String getLabel(Number label) {
    String text = "";
    if (label.intValue() == label.doubleValue()) {
      text = label.intValue() + "";
    } else {
      text = label + "";
    }
    return text;
  }

  /**
   * The graphical representation of the labels on the X axis.
   * 
   * @param xLabels the X labels values
   * @param xTextLabelLocations the X text label locations
   * @param canvas the canvas to paint to
   * @param paint the paint to be used for drawing
   * @param left the left value of the labels area
   * @param top the top value of the labels area
   * @param bottom the bottom value of the labels area
   * @param xPixelsPerUnit the amount of pixels per one unit in the chart labels
   * @param minX the minimum value on the X axis in the chart
   */
  protected void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
      Paint paint, int left, int top, int bottom, double xPixelsPerUnit, double minX) {
    int length = xLabels.size();
    boolean showLabels = mRenderer.isShowLabels();
    boolean showGrid = mRenderer.isShowGrid();
    for (int i = 0; i < length; i++) {
      double label = xLabels.get(i);
      float xLabel = (float) (left + xPixelsPerUnit * (label - minX));
      if (showLabels) {
        paint.setColor(mRenderer.getLabelsColor());
        canvas.drawLine(xLabel, bottom, xLabel, bottom + 4, paint);
        drawText(canvas, getLabel(label), xLabel, bottom + 12, paint, 0);
      }
      if (showGrid) {
        paint.setColor(GRID_COLOR);
        canvas.drawLine(xLabel, bottom, xLabel, top, paint);
      }
    }
    if (showLabels) {
      paint.setColor(mRenderer.getLabelsColor());
      for (Double location : xTextLabelLocations) {
        float xLabel = (float) (left + xPixelsPerUnit * (location.doubleValue() - minX));
        canvas.drawLine(xLabel, bottom, xLabel, bottom + 4, paint);
        drawText(canvas, mRenderer.getXTextLabel(location), xLabel, bottom + 12, paint, 0);
      }
    }
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
  public abstract void drawSeries(Canvas canvas, Paint paint, float[] points,
      SimpleSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex);

  /**
   * Returns if the chart should display the points as a certain shape.
   * 
   * @param renderer the series renderer
   */
  public boolean isRenderPoints(SimpleSeriesRenderer renderer) {
    return false;
  }
}
