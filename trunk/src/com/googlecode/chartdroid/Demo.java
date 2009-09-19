package com.googlecode.chartdroid;

import com.googlecode.chartdroid.calendar.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class Demo extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        
        findViewById(R.id.button_pie_chart).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				
		        String[] chart_key_labels = new String[] {
		        		"People unimpressed by this chart",
		        		"People impressed by this chart"
		        };
		        
		        int[] data = new int[] {13, 81};
		        
		/*
		        int[] colors = new int[chart_key_labels.length];
		    	for (int j=0; j<chart_key_labels.length; j++)
					colors[j] = Color.HSVToColor(new float[] {360 * j / (float) colors.length, 0.6f, 1});
		*/    
		        
		    	Intent i = new Intent(intent.ACTION_PLOT);
		    	i.addCategory(intent.CATEGORY_PIE_CHART);
		    	i.putExtra(intent.EXTRA_TITLE, "Impressions");
		    	i.putExtra(intent.EXTRA_LABELS, chart_key_labels);
		    	i.putExtra(intent.EXTRA_DATA, data);
//		    	i.putExtra(intent.EXTRA_COLORS, colors);
		    	

		    	startActivity(i);
			}
        });
        
        
        
        
        
        
        findViewById(R.id.button_calendar).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				Intent i = new Intent();
				i.setClass(Demo.this, Calendar.class);
		    	startActivity(i);
			}
        });

    }
}