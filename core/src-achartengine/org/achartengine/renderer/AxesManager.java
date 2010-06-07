package org.achartengine.renderer;


/**
 * An abstract renderer to be extended by the multiple series classes.
 */
public interface AxesManager extends AxesLabelsManager {

	  public String getChartTitle();

	  public void setChartTitle(String title);

	  public String getXTitle();
	  public void setXTitle(String title);

	  public String getYTitle();
	  public void setYTitle(String title);
	  
	  public String getYSecondaryTitle();
	  public void setYSecondaryTitle(String title);
}
