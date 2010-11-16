package com.kostmo.flickr.colorsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.kostmo.flickr.bettr.R;
import com.kostmo.tools.view.SwatchView;

public class SwatchAdapter extends BaseAdapter {
	
	public static final int MAX_COLORS = 10;
	static final int INITIAL_COLOR_COUNT = 3;

	public final List<Integer> color_list = new ArrayList<Integer>();

	Context context;
	LayoutInflater factory;
	public SwatchAdapter(Context context) {
		this.context = context;
		
        Random r = new Random();
        for (int i=0; i<INITIAL_COLOR_COUNT; i++)
        	this.color_list.add( r.nextInt() | 0xFF000000 );

        this.factory = LayoutInflater.from(this.context);
	}
	
	
	@Override
	public int getCount() {
		return color_list.size();
	}

    /* Use the array-Positions as unique IDs */
	@Override
    public Object getItem(int position) { return position; }

	@Override
    public long getItemId(int position) { return position; }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
        	convertView = factory.inflate(R.layout.color_swatch, null);
        }

	    int color = color_list.get(position);
	    
	    SwatchView icon = (SwatchView) convertView.findViewById(android.R.id.icon);
	    icon.setColor(color);
	    icon.setGrow(true);

        return convertView;
	}
}