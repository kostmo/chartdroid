package org.achartengine.renderer;


/**
 * An abstract renderer to be extended by the multiple series classes.
 */
public interface LabelsManager {

	  public int getLabelsColor();
	  public void setLabelsColor(int color);

	  public boolean isShowLabels();
	  public void setShowLabels(boolean showLabels);
}
