package com.googlecode.chartdroid;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.chartdroid.ColorSwatchKeyAdapter.PieDataElement;


public class ChartPanelActivity extends ListActivity {
	
	static final String TAG = "Crittr"; 
    
    
    public ChartPanelActivity() {
    }

    String[] data_labels;
    int[] data_values;
    int[] color_values;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
 	    setContentView(R.layout.panel_statistics);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon16);


        
        // Blurring is not a good idea when we animate the bird
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        
        // Inflate our UI from its XML layout description.
        
        
       color_values = getIntent().getIntArrayExtra(intent.EXTRA_COLORS);
       if (color_values == null) {
    	   color_values = getResources().getIntArray(R.array.colors_watermelon);
       }

        
        
        TextView title_holder = (TextView) findViewById(R.id.chart_title_placeholder);
        title_holder.setText(getIntent().getStringExtra(intent.EXTRA_TITLE));
        
        ImageView img = (ImageView) findViewById(R.id.image_placeholder);
        
        
        data_values = getIntent().getIntArrayExtra(intent.EXTRA_DATA);
        data_labels = getIntent().getStringArrayExtra(intent.EXTRA_LABELS);

        
        
        // Zip the data.
        List<PieDataElement> list = new ArrayList<PieDataElement>(); 
		int i = 0;
		for (int datum : data_values) {
        	PieDataElement slice = new PieDataElement();
        	slice.color = color_values[i % color_values.length];
        	slice.label = data_labels[i];
        	
//        	Log.d(TAG, "Data label: " + slice.label);
        	
        	slice.datum = datum;
        	list.add(slice);
        	
        	i++;
        }
        
        
        
        // Set up our adapter
        ColorSwatchKeyAdapter adapter = new ColorSwatchKeyAdapter(this);
        adapter.setData(list);
        
        
        setListAdapter(adapter);

        
        PieChartDrawable pie = new PieChartDrawable(this, img, data_values, color_values);

        
        img.setImageDrawable( pie );
    }
}