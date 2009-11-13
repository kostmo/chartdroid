package com.googlecode.chartdroid.demo;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
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

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.IntentConstants;
import com.googlecode.chartdroid.calendar.Calendar;
import com.googlecode.chartdroid.demo.provider.DataContentProvider;
import com.googlecode.chartdroid.demo.provider.EventContentProvider;

public class Demo extends Activity {


	static final String TAG = "ChartDroid"; 

	final int RETURN_CODE_CALENDAR_SELECTION = 1;
	
	
    public static final String[] demo_pie_labels = new String[] {
    		"People unimpressed by this chart",
    		"People impressed by this chart"
        };
        
    public static final int[] demo_pie_data = new int[] {13, 81};
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.main);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon16);

        
        
        findViewById(R.id.button_pie_chart).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

		/*
		        int[] colors = new int[chart_key_labels.length];
		    	for (int j=0; j<chart_key_labels.length; j++)
					colors[j] = Color.HSVToColor(new float[] {360 * j / (float) colors.length, 0.6f, 1});
		*/    
		        
		    	Intent i = new Intent(IntentConstants.ACTION_PLOT);
		    	i.addCategory(IntentConstants.CATEGORY_PIE_CHART);
		    	i.putExtra(Intent.EXTRA_TITLE, "Impressions");
		    	i.putExtra(IntentConstants.EXTRA_LABELS, demo_pie_labels);
		    	i.putExtra(IntentConstants.EXTRA_DATA, demo_pie_data);
//		    	i.putExtra(intent.EXTRA_COLORS, colors);
		    	

		    	startActivity(i);
			}
        });
        

        
        findViewById(R.id.button_calendar).setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				List<EventWrapper> generated_events = generateRandomEvents(5);
				int event_count = generated_events.size();
				long[] event_ids = new long[event_count];
				long[] event_times = new long[event_count];
				for (int i = 0; i < event_count; i++) {
					EventWrapper event = generated_events.get(i);
					event_ids[i] = event.id;
					event_times[i] = event.timestamp;
				}
				
				
				Intent i = new Intent();
				i.setClass(Demo.this, Calendar.class);
				
				i.putExtra(IntentConstants.EXTRA_EVENT_IDS, event_ids);
				i.putExtra(IntentConstants.EXTRA_EVENT_TIMESTAMPS, event_times);
				
		    	startActivityForResult(i, RETURN_CODE_CALENDAR_SELECTION);
			}
        });

        
        findViewById(R.id.button_pie_chart_provider).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Uri u = DataContentProvider.constructUri(12345);
				Intent i = new Intent(Intent.ACTION_VIEW, u);
				i.putExtra(Intent.EXTRA_TITLE, "This is a really long title, isn't it?");
		    	startActivity(i);
			}
        });
        
        
        findViewById(R.id.button_calendar_provider).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	Uri u = EventContentProvider.constructUri(12345);
				Intent i = new Intent(Intent.ACTION_VIEW, u);
		    	startActivityForResult(i, RETURN_CODE_CALENDAR_SELECTION);
			}
        });
        

        
        ((TextView) findViewById(R.id.developer_note)).setMovementMethod(LinkMovementMethod.getInstance());
    }
    
    
    
    public static class EventWrapper {
    	public long id, timestamp;
    	public String title;
    }
    
    public static List<EventWrapper> generateRandomEvents(int event_count) {
    	
    	List<EventWrapper> events = new ArrayList<EventWrapper>();
		GregorianCalendar cal = new GregorianCalendar();
		Random r = new Random();

		for (int event_id = 0; event_id < event_count; event_id++) {
			EventWrapper event = new EventWrapper();
    		cal.roll(GregorianCalendar.DATE, r.nextInt(3));
    		event.timestamp = cal.getTimeInMillis();
			event.id = event_id;
    		events.add(event);
		}
		
		return events;
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
//            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
//            t.show();
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