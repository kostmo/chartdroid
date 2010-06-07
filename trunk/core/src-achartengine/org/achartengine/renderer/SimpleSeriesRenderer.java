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
package org.achartengine.renderer;

import android.graphics.Color;

/**
 * A simple series renderer.
 */
public class SimpleSeriesRenderer implements AxesManager {
	private int mColor = Color.BLUE;

	/** The default color for text. */
	public static final int TEXT_COLOR = Color.LTGRAY;

	/** If the axes are visible. */
	protected boolean mShowAxes = true;

	/** The axes color. */
	protected int mAxesColor = TEXT_COLOR;

	/** If the labels are visible. */
	protected boolean mShowLabels = true;

	/** The labels color. */
	protected int mLabelsColor = TEXT_COLOR;

	/** If the legend is visible. */
	protected boolean mShowLegend = true;

	  /** If the grid should be displayed. */
	  protected boolean mShowGrid = false;
	  protected boolean mShowGridHorizontal = false;
	  protected boolean mShowGridVertical = false;
	  

	  protected boolean mUsesSecondaryAxis = false;
	  public boolean getUsesSecondaryAxis() {
		  return mUsesSecondaryAxis;
	  }
	  
	  public void setUsesSecondaryAxis(boolean uses) {
		  mUsesSecondaryAxis = uses;
	  }

	/** The chart title. */
	private String mChartTitle = "";
	/** The X axis title. */
	private String mXTitle = "";
	/** The Y axis title. */
	private String mYTitle = "";
	/** The secondary Y axis title. */
	private String mYSecondaryTitle = "";
	private boolean has_secondary_y_axis;

	/**
	 * Returns the series color.
	 * @return the series color
	 */
	public int getColor() {
		return mColor;
	}

	/**
	 * Sets the series color.
	 * @param color the series color
	 */
	public void setColor(int color) {
		mColor = color;
	}




	@Override
	public String getChartTitle() {
		return mChartTitle;
	}


	@Override
	public void setChartTitle(String title) {
		mChartTitle = title;
	}


	@Override
	public String getXTitle() {
		return mXTitle;
	}


	@Override
	public void setXTitle(String title) {
		mXTitle = title;
	}


	@Override
	public String getYTitle() {
		return mYTitle;
	}


	@Override
	public void setYTitle(String title) {
		mYTitle = title;
	}

	@Override
	public int getAxesColor() {
		return mAxesColor;
	}


	@Override
	public void setAxesColor(int color) {
		mAxesColor = color;
	}


	@Override
	public int getLabelsColor() {
		return mLabelsColor;
	}


	@Override
	public void setLabelsColor(int color) {
		mLabelsColor = color;
	}


	@Override
	public boolean isShowAxes() {
		return mShowAxes;
	}

	@Override
	public void setShowAxes(boolean showAxes) {
		mShowAxes = showAxes;
	}


	@Override
	public boolean isShowLabels() {
		return mShowLabels;
	}

	@Override
	public void setShowLabels(boolean showLabels) {
		mShowLabels = showLabels;
	}


	@Override
	public boolean isShowGrid() {
		return mShowGrid;
	}

	@Override
	public void setShowGrid(boolean showGrid) {
		mShowGrid = showGrid;
	}

	@Override
	public boolean isShowGridHorizontalLines() {
		return mShowGridHorizontal;
	}

	@Override
	public boolean isShowGridVerticalLines() {
		return mShowGridVertical;
	}

	@Override
	public void setShowGridHorizontalLines(boolean showGrid) {
		mShowGridHorizontal = showGrid;
	}

	@Override
	public void setShowGridVerticalLines(boolean showGrid) {
		mShowGridVertical = showGrid;
	}

	
	// FIXME These should not be present in the "SimpleSeriesRenderer", 
	// and therefore should not be part of the implemented interface.
	@Override
	public String getYSecondaryTitle() {
		return mYSecondaryTitle;
	}

	@Override
	public void setYSecondaryTitle(String title) {
		mYSecondaryTitle = title;
	}
}
