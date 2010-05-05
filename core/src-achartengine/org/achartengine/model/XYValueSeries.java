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
package org.achartengine.model;

import org.achartengine.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension of the XY series which adds a third dimension. It is used for XY
 * charts like bubble.
 */
public class XYValueSeries extends XYSeries {
  /** A list to contain the series values. */
  private List<Number> mValue = new ArrayList<Number>();
  /** The minimum value. */
  private double mMinValue = MathHelper.NULL_VALUE;
  /** The maximum value. */
  private double mMaxValue = -MathHelper.NULL_VALUE;

  /**
   * Builds a new XY value series.
   * 
   * @param title the series title.
   */
  public XYValueSeries(String title) {
    super(title);
  }

  /**
   * Adds a new value to the series.
   * 
   * @param number the value for the X axis
   * @param number2 the value for the Y axis
   * @param number3 the value
   */
  public void add(Number number, Number number2, Number number3) {
    super.add(number, number2);
    mValue.add(number3);
    updateRange(number3);
  }
  
  /**
   * Initializes the values range.
   */
  private void initRange() {
    mMinValue = MathHelper.NULL_VALUE;
    mMaxValue = MathHelper.NULL_VALUE;
    int length = getItemCount();
    for (int k = 0; k < length; k++) {
      updateRange(getValue(k));
    }
  }
  
  /**
   * Updates the values range.
   * @param value the new value
   */
  private void updateRange(Number value) {
    mMinValue = Math.min(mMinValue, value.doubleValue());
    mMaxValue = Math.max(mMaxValue, value.doubleValue());
  }

  /**
   * Adds a new value to the series.
   * 
   * @param x the value for the X axis
   * @param y the value for the Y axis
   */
  public void add(double x, double y) {
    add(x, y, 0d);
  }

  /**
   * Removes an existing value from the series.
   * @param index the index in the series of the value to remove
   */
  public void remove(int index) {
    super.remove(index);
    Number removedValue = mValue.remove(index);
    if (removedValue.equals(mMinValue) || removedValue.equals(mMaxValue)) {
      initRange();
    }
  }
  
  /**
   * Removes all the values from the series.
   */
  public void clear() {
    super.clear();
    mValue.clear();
    initRange();
  }
  
  /**
   * Returns the value at the specified index.
   * 
   * @param index the index
   * @return the value
   */
  public Number getValue(int index) {
    return mValue.get(index);
  }

  /**
   * Returns the minimum value.
   * 
   * @return the minimum value
   */
  public double getMinValue() {
    return mMinValue;
  }

  /**
   * Returns the maximum value.
   * 
   * @return the maximum value
   */
  public double getMaxValue() {
    return mMaxValue;
  }

}
