/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.googlecode.chartdroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

public class PieChartDrawable extends Drawable {

	final static String TAG = "Crittr";
	
	int padding_top = 10;
	int padding_bottom = 10;

	int padding_left = 10;
	int padding_right = 10;
	
	Context context;
	View view;

	Paint paint;
	int[] data_values;
	int[] color_values;
	public PieChartDrawable(Context c, View v, int[] data_values, int[] color_values) {
		context = c;
		view = v;
		this.data_values = data_values;
		this.color_values = color_values;
		
		paint = new Paint();

		
		
		
		/*
		Rect bounds = new Rect(
				0,
				0,
				150,
				150
				);
		this.setBounds(bounds);
		*/
	}
	
	public void draw(Canvas canvas) {
		

		int view_w = view.getWidth();
		int view_h = view.getHeight();
//		Log.e(TAG, "View dimensions: (" + view_w + ", " + view_h + ")");
		
		
//		int size = Math.min(view_w, view_h);
		
//		int size = 150;
		
		int padded_horizontal_room = view_w - (padding_left + padding_right);
		int padded_vertical_room = view_h - (padding_top + padding_bottom);
		

		RectF arc_bounds = new RectF(
				padding_left,
				padding_top,
				padded_horizontal_room + padding_left,
				padded_vertical_room + padding_top
				);
		
		
		
		int value_sum = 0;
		for (int datum : data_values)
			value_sum += datum;
		
		float current_arc_position_degrees = 0;
		int i = 0;
		for (int datum : data_values) {
			if (datum == 0) continue;
			
			float arc_sweep = value_sum == 0 ? 0 : 360 * datum / (float) value_sum;
			
			float new_arc_position_degrees = current_arc_position_degrees + arc_sweep;
			

			
			int flickr_pink = color_values[i % color_values.length];
			paint.setColor(flickr_pink);
			paint.setAntiAlias(true);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawArc(arc_bounds, current_arc_position_degrees, arc_sweep, true, paint);
			
			
//			Log.d(TAG, "Drawing arc from " + current_arc_position_degrees + " to " + new_arc_position_degrees);

	
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeJoin(Join.ROUND);
			paint.setStrokeCap(Cap.ROUND);
			paint.setStrokeWidth(4);
			paint.setColor(Color.WHITE);
			canvas.drawArc(arc_bounds, current_arc_position_degrees, arc_sweep, true, paint);
			
			current_arc_position_degrees = new_arc_position_degrees;
			i++;
		}
	}

	public int getOpacity() {
		return paint.getAlpha();
	}

	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
	}

	public void setColorFilter(ColorFilter cf) {
		paint.setColorFilter(cf);
	}
}
    


