package com.googlecode.chartdroid.adapter;

import com.googlecode.chartdroid.core.ColumnSchema;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class PlotSeriesListAdapter extends ResourceCursorAdapter {

	static final String TAG = "PlotSeriesListAdapter"; 
	
    public PlotSeriesListAdapter(Context context, int layout, Cursor cursor) {
    	super(context, layout, cursor);
    }


    final int[] FIXME_COLOR_WHEEL = {Color.BLUE, Color.YELLOW, Color.CYAN};
	public void bindView(View view, Context context, Cursor cursor) {

		int text_column = cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_LABEL);
		int potential_color_column = cursor.getColumnIndex(ColumnSchema.COLUMN_SERIES_COLOR);

		TextView series_name = (TextView) view.findViewById(android.R.id.text1);
		if (potential_color_column >= 0) {
			int potential_color = cursor.getInt(potential_color_column);
			Log.d(TAG, "Potential color: 0x" + Integer.toHexString(potential_color));
			
			series_name.setTextColor(potential_color);
		} else {
			series_name.setTextColor(FIXME_COLOR_WHEEL[cursor.getPosition() % FIXME_COLOR_WHEEL.length]);
		}
		
		if (text_column >= 0) {
			String label = cursor.getString(text_column);
			series_name.setText(label);
		}
	}
}