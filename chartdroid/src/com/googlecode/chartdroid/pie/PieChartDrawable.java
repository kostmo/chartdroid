package com.googlecode.chartdroid.pie;

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

	final static String TAG = "ChartDroid";
	
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
	}
	
	@Override
	public void draw(Canvas canvas) {
		

		int view_w = view.getWidth();
		int view_h = view.getHeight();
//		Log.e(TAG, "View dimensions: (" + view_w + ", " + view_h + ")");
		
		
		
		int padded_horizontal_room = view_w - (padding_left + padding_right);
		int padded_vertical_room = view_h - (padding_top + padding_bottom);

		int size = Math.min(padded_horizontal_room, padded_vertical_room);

		boolean stretch = false;
		int left_edge = padding_left + (padded_horizontal_room - size)/2;
		int right_edge = left_edge + (stretch ? padded_horizontal_room : size);
		int top_edge = padding_top + (padded_vertical_room - size)/2;
		int bottom_edge = top_edge + (stretch ? padded_vertical_room : size);
		
		RectF arc_bounds = new RectF(
			left_edge,
			top_edge,
			right_edge,
			bottom_edge
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

	@Override
	public int getOpacity() {
		return paint.getAlpha();
	}

	@Override
	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paint.setColorFilter(cf);
	}
}
    


