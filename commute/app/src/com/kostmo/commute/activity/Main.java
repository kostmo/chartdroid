package com.kostmo.commute.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.kostmo.commute.CalendarPickerConstants;
import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.activity.prefs.TriggerPreferences;
import com.kostmo.commute.provider.DataContentProvider;
import com.kostmo.commute.provider.DatabaseCommutes;
import com.kostmo.commute.provider.EventContentProvider;
import com.kostmo.commute.service.RouteTrackerService;

public class Main extends ListActivity implements Disablable {


    public static final String TAG = Market.TAG;

    
	private static final int DIALOG_PLOT_METHOD = 1;
	private static final int DIALOG_CALENDARPICKER_DOWNLOAD = 2;


	private static final int REQUEST_CODE_NEW_PAIR = 1;


	DatabaseCommutes database;
	SharedPreferences settings;

    long global_route_id;
    
    TextView service_connection_status;
	Button button_cancel_tracker;
	
    // ========================================================================
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.main);
    	this.database = new DatabaseCommutes(this);
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        
        this.service_connection_status = (TextView) findViewById(R.id.service_connection_status);
        this.button_cancel_tracker = (Button) findViewById(R.id.button_cancel_tracker);
        this.button_cancel_tracker.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {


				if (Main.this.record_fetcher_service != null) {
					// We check for nullity again, just in case the service died while the menu was idly showing.
					Main.this.record_fetcher_service.cancelAllProximityAlerts();
				}
			}
        });
    	
        SimpleCursorAdapter sca = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null,
        		new String[] {
		    		DatabaseCommutes.KEY_TITLE,
		    		DatabaseCommutes.KEY_TOTAL_TRIP_COUNT},
        		new int[] {android.R.id.text1, android.R.id.text2});
        this.setListAdapter(sca);
        
        this.refreshCursor();
    
		registerForContextMenu(getListView());
		
		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.global_route_id = state.global_route_id;
		}
		
		
		bindServiceOnly();
    }
    
	
	// ========================================================================
	void bindServiceOnly() {
		
		this.service_connection_status.setText(R.string.status_connecting);
		Main.this.button_cancel_tracker.setEnabled(false);
		
		Intent i = new Intent(this, RouteTrackerService.class);
		bindService(i, this.mConnection, Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND );
	}


	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		StateObject state = new StateObject();
		state.global_route_id = this.global_route_id;		
		return state;
	}
	
	// ========================================================================
	class StateObject {
		long global_route_id;
	}

	// ========================================================================
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	this.global_route_id = id;
    	showDialog(DIALOG_PLOT_METHOD);
    }
    
	// ========================================================
    void refreshCursor() {
    	Cursor cursor = this.database.getDestinationPairs();
    	((CursorAdapter) this.getListAdapter()).changeCursor(cursor);
    }

	// ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
		case DIALOG_CALENDARPICKER_DOWNLOAD:
		{
			boolean has_android_market = CalendarPickerConstants.DownloadInfo.isIntentAvailable(this,
					CalendarPickerConstants.DownloadInfo.getMarketDownloadIntent(CalendarPickerConstants.DownloadInfo.PACKAGE_NAME_CALENDAR_PICKER));

			Log.d(TAG, "has_android_market? " + has_android_market);

			dialog.findViewById(android.R.id.button1).setVisibility(
					has_android_market ? View.VISIBLE : View.GONE);
			break;
		}
        default:
        	break;
        }
    }
    
	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {
        
		switch (id) {
		case DIALOG_CALENDARPICKER_DOWNLOAD:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.download_calendar_picker)
			.setMessage(R.string.calendar_picker_modularization_explanation)
			.setPositiveButton(R.string.download_calendar_picker_market, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(CalendarPickerConstants.DownloadInfo.getMarketDownloadIntent(CalendarPickerConstants.DownloadInfo.PACKAGE_NAME_CALENDAR_PICKER));
				}
			})
			.setNeutralButton(R.string.download_calendar_picker_web, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(new Intent(Intent.ACTION_VIEW, CalendarPickerConstants.DownloadInfo.APK_DOWNLOAD_URI));
				}
			})
			.create();
		}
		case DIALOG_PLOT_METHOD:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Plot method:")
			.setItems(new String[] {"Chart", "Calendar"}, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						plotChart();
						break;
					case 1:
						plotCalendar();
						break;
					}
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, null)
			.create();
		}
		}
		return null;
	}

    

	// ========================================================================
	void downloadLaunchCheck(Intent intent, int request_code) {
		if (CalendarPickerConstants.DownloadInfo.isIntentAvailable(this, intent))
			if (request_code >= 0)
				startActivityForResult(intent, request_code);
			else
				startActivity(intent);
		else
			showDialog(DIALOG_CALENDARPICKER_DOWNLOAD);
	}
	
    // ========================================================================	
    void plotCalendar() {
    

		long calendar_id = 1;	// XXX Irrelevant
		Uri u = EventContentProvider.constructUri(calendar_id);
		Intent i = new Intent(Intent.ACTION_VIEW, u);
		
		
		String extra_name = CalendarPickerConstants.CalendarEventPicker.IntentExtras.EXTRA_QUANTITY_COLUMN_NAMES[0];
		i.putExtra(extra_name, EventContentProvider.COLUMN_QUANTITY0);
		i.putExtra(CalendarPickerConstants.CalendarEventPicker.IntentExtras.EXTRA_QUANTITY_FORMATS[0], "%.0f minutes");
		
		i.putExtra(CalendarPickerConstants.CalendarEventPicker.IntentExtras.EXTRA_VISUALIZE_QUANTITIES, true);
//		i.putExtra(CalendarPickerConstants.CalendarEventPicker.IntentExtras.EXTRA_BACKGROUND_COLORMAP_COLORS, COLORMAP_COLORS);
		i.putExtra(CalendarPickerConstants.CalendarEventPicker.IntentExtras.EXTRA_SHOW_EVENT_COUNT, false);

		
		downloadLaunchCheck(i, -1);
    	
    }

    // ========================================================================	
    void plotChart() {
    
    	// FIXME
    	long data_id = 0;
    	Intent i = new Intent(Intent.ACTION_VIEW, DataContentProvider.constructUri(data_id));
    	startActivity(i);
    }

	// ========================================================================
    void startTripInService(long trip_id, boolean returning) {
    	
		if (this.record_fetcher_service != null) {
			// We check for nullity again, just in case the service died while the menu was idly showing.
			this.record_fetcher_service.startTrip(trip_id, returning);
			
			refreshCursor();
		}
    }

	// ========================================================================
    void openRoute(String action, long route_id) {
    	
    	Intent intent = new Intent(action);
    	intent.setClass(this, DestinationPairAssociator.class);
    	intent.putExtra(DestinationPairAssociator.EXTRA_ROUTE_ID, route_id);
    	startActivityForResult(intent, REQUEST_CODE_NEW_PAIR);
    }
    
	// ========================================================================
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_routes, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Route action:");
        
        
        
        boolean service_connected = this.record_fetcher_service != null;
        menu.findItem(R.id.menu_start_logging_outbound).setVisible(service_connected);
        menu.findItem(R.id.menu_start_logging_return).setVisible(service_connected);
	}
    
	// ========================================================================
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch ( item.getItemId() ) {
		
		// NOTE: The logging option won't be visible until the service is connected.
		
		case R.id.menu_list_trips:
		{
	    	Intent intent = new Intent(this, ListActivityTrips.class);
	    	intent.putExtra(DestinationPairAssociator.EXTRA_ROUTE_ID, info.id);
	    	startActivity(intent);
			break;
		}
		case R.id.menu_start_logging_outbound:
		{
			startTripInService(info.id, false);
			break;
		}
		case R.id.menu_start_logging_return:
		{
			startTripInService(info.id, true);
			break;
		}
		case R.id.menu_edit_route:
		{
			openRoute(Intent.ACTION_EDIT, info.id);
			break;
		}
		case R.id.menu_duplicate_route:
		{
			openRoute(Intent.ACTION_VIEW, info.id);
			break;
		}
        case R.id.menu_plot_times:
        {
        	this.global_route_id = info.id;
        	showDialog(DIALOG_PLOT_METHOD);
            return true;
        }
		default:
			break;
		}

		return super.onContextItemSelected(item);
	}

    // ======================================================================== 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_main, menu);
        return true;
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        return true;
    }

    
    
    
    // ========================================================================	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
        {
        	Intent intent = new Intent(this, TriggerPreferences.class);
        	startActivity(intent);
        	return true;
        }
        case R.id.menu_dump_wifi:
        {
        	this.database.dumpWifiTable();
        	return true;
        }
        case R.id.new_pair:
        {
        	Intent intent = new Intent(this, DestinationPairAssociator.class);
        	startActivityForResult(intent, REQUEST_CODE_NEW_PAIR);
        	return true;
        }
        case R.id.menu_about:
        {

        	Uri flickr_destination = Uri.parse( Market.WEBSITE_URL );
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

    // ========================================================================
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(request " + requestCode
              + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
        
  	   	switch (requestCode) {
  	   	case REQUEST_CODE_NEW_PAIR:

  	        this.refreshCursor();
  	   		break;
   		default:
	    	break;
  	   	}
    }
    
    
    

    RouteTrackerService record_fetcher_service;
	// ========================================================================
	@Override
	public void onDestroy() {

		
		Log.e(TAG, "The Activity was destroyed.");

		if (this.mConnection != null) {
			if (this.record_fetcher_service != null) {
				this.record_fetcher_service.setDisablableHost(null);
				
				Log.e(TAG, "Now unbinding service...");
				this.unbindService(this.mConnection);
			}
		}

		super.onDestroy();
	}

	// ========================================================================
	boolean starting_service = false;
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			Log.d(TAG, "The service has connected.");
			Main.this.service_connection_status.setText(R.string.status_connected);
			Main.this.button_cancel_tracker.setEnabled(true);
			
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			Main.this.record_fetcher_service = ((RouteTrackerService.LocalBinder) service).getService();
			Main.this.record_fetcher_service.setDisablableHost(Main.this);

			
			if (Main.this.starting_service) {
				Main.this.starting_service = false;


			   	
			} else {
				
				if (Main.this.record_fetcher_service != null && Main.this.record_fetcher_service.isInProgress()) {
					disable();
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			Main.this.record_fetcher_service = null;
		}
	};

	@Override
	public void disable() {
		Log.e(TAG, "Host disabled.");	// FIXME
	}

	@Override
	public void reEnable() {
		Log.e(TAG, "Host re-enabled.");	// FIXME
	}
}