package org.achartengine.renderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Typeface;

/**
 * An abstract renderer to be extended by the multiple series classes.
 */
public interface AxesLabelsManager extends LabelsManager {
	  /**
	   * Returns the axes color.
	   * 
	   * @return the axes color
	   */
	  public int getAxesColor();

	  /**
	   * Sets the axes color.
	   * 
	   * @param color the axes color
	   */
	  public void setAxesColor(int color);

	  public boolean isShowAxes();

	  /**
	   * Sets if the axes should be visible.
	   * 
	   * @param showAxes the visibility flag for the axes
	   */
	  public void setShowAxes(boolean showAxes);

	  /**
	   * Returns if the grid should be visible.
	   * 
	   * @return the visibility flag for the grid
	   */
	  public boolean isShowGrid();

	  /**
	   * Sets if the grid should be visible.
	   * 
	   * @param showGrid the visibility flag for the grid
	   */
	  public void setShowGrid(boolean showGrid);
}
