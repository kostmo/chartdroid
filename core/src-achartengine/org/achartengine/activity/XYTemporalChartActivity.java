/**
 * Copyright (C) 2009 Karl Ostmo
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

package org.achartengine.activity;

import org.achartengine.util.MathHelper.MinMax;

import java.util.List;


/**
 * An activity that encapsulates a graphical view of the chart.
 */
public abstract class XYTemporalChartActivity extends XYChartActivity {

	MinMax getTimeAxisLimits(List<List<Number>> time_axis_series) {
		
		MinMax time_minmax = new MinMax(time_axis_series);
		long time_values_span = (long) time_minmax.getSpan();
		long padding;
		if (time_values_span > 0) {
			padding = (long) (time_values_span*HEADROOM_FOOTROOM_FRACTION);
		} else {
			// Pad by one day.
			long day_ms = 1000*60*60*24;
			padding = day_ms;
		}
		
		long time_axis_lower_limit = time_minmax.min.longValue() - padding;
		long time_axis_upper_limit = time_minmax.max.longValue() + padding;
		return new MinMax(time_axis_lower_limit, time_axis_upper_limit);
	}

	// ========================================================================
	@Override
	MinMax getYAxisLimits(List<List<Number>> multi_series) {
		return getAxisLimits(multi_series);
	}

	// ========================================================================
	@Override
	MinMax getXAxisLimits(List<List<Number>> multi_series) {
		return getTimeAxisLimits(multi_series);
	}
}