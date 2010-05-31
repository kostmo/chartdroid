package org.achartengine.renderer;


/**
 * An abstract renderer to be extended by the multiple series classes.
 */
public interface AxesLabelsManager extends LabelsManager {

	  public int getAxesColor();
	  public void setAxesColor(int color);

	  public boolean isShowAxes();
	  public void setShowAxes(boolean showAxes);

	  public boolean isShowGrid();
	  public void setShowGrid(boolean showGrid);
	  
	  public void setShowGridHorizontalLines(boolean showGrid);
	  public void setShowGridVerticalLines(boolean showGrid);

	  public boolean isShowGridHorizontalLines();
	  public boolean isShowGridVerticalLines();
}
