package org.achartengine.renderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Typeface;

/**
 * An abstract renderer to be extended by the multiple series classes.
 */
public interface LabelsManager {


	  /**
	   * Returns the labels color.
	   * 
	   * @return the labels color
	   */
	  public int getLabelsColor();

	  /**
	   * Sets the labels color.
	   * 
	   * @param color the labels color
	   */
	  public void setLabelsColor(int color);


	  public boolean isShowLabels();

	  /**
	   * Sets if the labels should be visible.
	   * 
	   * @param showLabels the visibility flag for the labels
	   */
	  public void setShowLabels(boolean showLabels);
}
