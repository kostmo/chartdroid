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

import org.achartengine.renderer.DefaultRenderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;

import java.io.Serializable;

/**
 * An abstract class to be implemented by the chart rendering classes.
 */
public abstract class AbstractChart implements Serializable {
  
  /**
   * The graphical representation of the chart.
   * @param canvas the canvas to paint to
   * @param x the top left x value of the view to draw to
   * @param y the top left y value of the view to draw to
   * @param width the width of the view to draw to
   * @param height the height of the view to draw to
   */
  public abstract void draw(Canvas canvas, int width, int height);
  
  /**
   * Draws the chart background.
   * @param renderer the chart renderer
   * @param canvas the canvas to paint to
   * @param x the top left x value of the view to draw to
   * @param y the top left y value of the view to draw to
   * @param width the width of the view to draw to
   * @param height the height of the view to draw to
   * @param paint the paint used for drawing
   */
  protected void drawBackground(DefaultRenderer renderer, Canvas canvas, int x, int y, int width, int height, Paint paint) {
    if (renderer.isApplyBackgroundColor()) {
      paint.setColor(renderer.getBackgroundColor());
      paint.setStyle(Style.FILL);
      canvas.drawRect(x, y, x + width, y + height, paint);
    }
  }


  
  /**
   * The graphical representation of a path.
   * @param canvas the canvas to paint to
   * @param points the points that are contained in the path to paint
   * @param paint the paint to be used for painting
   * @param circular if the path ends with the start point
   */
  protected void drawPath(Canvas canvas, float[] points, Paint paint, boolean circular) {
    Path path = new Path();
    path.moveTo(points[0], points[1]);
    for (int i = 2; i < points.length; i += 2) {
      path.lineTo(points[i], points[i + 1]);
    }
    if (circular) {
      path.lineTo(points[0], points[1]);
    }
    canvas.drawPath(path, paint);
  }

  private boolean is_anti_aliased = true;
  protected boolean getAntiAliased() {
    return this.is_anti_aliased;
  }
  
  protected void setAntiAliased(boolean is_anti_aliased) {
    this.is_anti_aliased = is_anti_aliased;
  }
}
