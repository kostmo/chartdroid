package com.googlecode.chartdroid;

import java.util.GregorianCalendar;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.chartdroid.calendar.Calendar;

public class Demo extends Activity {


	static final String TAG = "ChartDroid"; 

	final int RETURN_CODE_CALENDAR_SELECTION = 1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.main);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon16);

        
        
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

				
				
				GregorianCalendar cal = new GregorianCalendar();
				Random r = new Random();
				int event_count = 5;
				long[] event_ids = new long[event_count];
				long[] event_times = new long[event_count];
				for (int event_id = 0; event_id < event_count; event_id++) {
					event_ids[event_id] = event_id;
					
		    		cal.add(GregorianCalendar.DATE, r.nextInt(3));
					event_times[event_id] = cal.getTimeInMillis();
				}
				
				
				Intent i = new Intent();
				i.setClass(Demo.this, Calendar.class);
				
				i.putExtra(intent.EXTRA_EVENT_IDS, event_ids);
				i.putExtra(intent.EXTRA_EVENT_TIMESTAMPS, event_times);
				
		    	startActivityForResult(i, RETURN_CODE_CALENDAR_SELECTION);
			}
        });

        
        ((TextView) findViewById(R.id.developer_note)).setMovementMethod(LinkMovementMethod.getInstance());
    }
    
    
    
    
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_main, menu);
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_about:
        {
			Uri flickr_destination = Uri.parse( "http://chartdroid.googlecode.com/" );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }
    
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        
  	   	switch (requestCode) {
   		case RETURN_CODE_CALENDAR_SELECTION:
   		{

   			long id = data.getLongExtra(Calendar.INTENT_EXTRA_CALENDAR_SELECTION_ID, -1);
   			Toast.makeText(this, "Result: " + id, Toast.LENGTH_SHORT).show();
            break;
        }
  	   	}
    }

}