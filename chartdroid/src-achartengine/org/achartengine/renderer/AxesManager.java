package org.achartengine.renderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Typeface;

/**
 * An abstract renderer to be extended by the multiple series classes.
 */
public interface AxesManager extends AxesLabelsManager {

	
	/**
	   * Returns the chart title.
	   * 
	   * @return the chart title
	   */
	  public String getChartTitle();

	  /**
	   * Sets the chart title.
	   * 
	   * @param title the chart title
	   */
	  public void setChartTitle(String title);

	  /**
	   * Returns the title for the X axis.
	   * 
	   * @return the X axis title
	   */
	  public String getXTitle();

	  /**
	   * Sets the title for the X axis.
	   * 
	   * @param title the X axis title
	   */
	  public void setXTitle(String title);

	  /**
	   * Returns the title for the Y axis.
	   * 
	   * @return the Y axis title
	   */
	  public String getYTitle();

	  /**
	   * Sets the title for the Y axis.
	   * 
	   * @param title the Y axis title
	   */
	  public void setYTitle(String title);
	
}
