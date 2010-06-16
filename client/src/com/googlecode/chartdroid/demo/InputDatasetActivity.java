package com.googlecode.chartdroid.demo;

import com.googlecode.chartdroid.demo.provider.DatabaseStoredData;
import com.googlecode.chartdroid.demo.provider.LocalStorageContentProvider;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InputDatasetActivity extends ListActivity {


	static final String TAG = "ChartDroid"; 

	List<EventDatum> event_list = new ArrayList<EventDatum>(); 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.manual_datasets_activity);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

        
        final InputMethodManager input = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

    	final EditText edit_text = (EditText) findViewById(R.id.datum_value_field);
    	final DatePicker date_picker = (DatePicker) findViewById(R.id.date_picker_widget);
        findViewById(R.id.button_add_datum).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

            	Date date = new Date(
            			date_picker.getYear() - 1900,
            			date_picker.getMonth(),
            			date_picker.getDayOfMonth());

            	EventDatum event_datum = new EventDatum();
            	event_datum.label = "Something";
            	event_datum.timestamp = date.getTime();
            	
            	String num_string = edit_text.getText().toString();
            	if (num_string.length() > 0)
            		event_datum.value = Float.parseFloat(num_string);
            	event_list.add(event_datum);
            	((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
            	
//            	input.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
        
        
        edit_text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                	input.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });
//        input.hideSoftInputFromWindow(edit_text.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY); 
        
        
        findViewById(R.id.button_clear_manual_data).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
            	event_list.clear();
            	((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
            	
            	DatabaseStoredData database = new DatabaseStoredData(InputDatasetActivity.this);
            	int deleted_count = database.deleteAllData();
            	Toast.makeText(InputDatasetActivity.this, "Deleted " + deleted_count + " old records.", Toast.LENGTH_SHORT).show();
            }
        });
        
        findViewById(R.id.button_graph_manual_data).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

            	// Procedure: Save the event list to the database,
            	// retrieve the URI for that dataset, and finally
            	// pass along the URI to an intent to launch the chart implicitly.
            	
            	
            	DatabaseStoredData database = new DatabaseStoredData(InputDatasetActivity.this);
            	long dataset_id = database.storeEvents(event_list);
            	// Derive URI from dataset_id.
            	Uri target_uri = LocalStorageContentProvider.constructUri(dataset_id);
            	
                Intent i = new Intent(Intent.ACTION_VIEW, target_uri);
                i.putExtra(Intent.EXTRA_TITLE, "Manual timeline");
                Market.intentLaunchMarketFallback(InputDatasetActivity.this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
            }
        });
        
        setListAdapter(new SimpleEventAdapter(this));
        
        final StateObject state = (StateObject) getLastNonConfigurationInstance();
        if (state != null) {

        	this.event_list = state.event_list;
        	SimpleEventAdapter sea = (SimpleEventAdapter) getListView().getAdapter();
        	sea.notifyDataSetChanged();
        }
    }

    // =============================================    
    class StateObject {
    	List<EventDatum> event_list;
    }
    
    // =============================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	
    	StateObject state = new StateObject();
    	state.event_list = this.event_list;

        return state;
    }
    
    
    public static class EventDatum {
    	public long timestamp;
    	public float value;
    	public String label;
    }
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
    	event_list.remove(position);
    	((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
    	
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
			Uri flickr_destination = Uri.parse( Demo.GOOGLE_CODE_URL );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

            return true;
        }
        case R.id.menu_more_apps:
        {
	    	Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
	    	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
	    	startActivity(i);
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }


    class SimpleEventAdapter extends BaseAdapter {

        DateFormat date_format;
    	Context context;
        private LayoutInflater mInflater;
    	SimpleEventAdapter(Context context) {
    		this.context = context;
    		this.mInflater = LayoutInflater.from(context);
    		this.date_format = new SimpleDateFormat("MMM d, yyyy");
    	}
    	
		@Override
		public int getCount() {
			return event_list.size();
		}

		@Override
		public Object getItem(int position) {
			return event_list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public class ViewHolderEvent {
		    public TextView label, value, date;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolderEvent holder;
	        if (convertView == null) {
	            convertView = mInflater.inflate(R.layout.list_item_event, null);

	            // Creates a ViewHolder and store references to the two children views
	            // we want to bind data to.
	            holder = new ViewHolderEvent();
	            holder.label = (TextView) convertView.findViewById(R.id.timeline_datum_label);
	            holder.value = (TextView) convertView.findViewById(R.id.timeline_datum_value);
	            holder.date = (TextView) convertView.findViewById(R.id.timeline_datum_date);

	            convertView.setTag(holder);

	        } else {

	            // Get the ViewHolder back to get fast access to the TextView
	            // and the ImageView.
	            holder = (ViewHolderEvent) convertView.getTag();
	        }

	        EventDatum event = (EventDatum) event_list.get(position);
	        holder.label.setText(event.label);
	        holder.value.setText( "" + event.value );
	        holder.date.setText( date_format.format(new Date(event.timestamp)) );

	        return convertView;
		}
    }
}