package com.kostmo.commute.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.kostmo.commute.CalendarPickerConstants;
import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.provider.DataContentProvider;
import com.kostmo.commute.provider.DatabaseCommutes;
import com.kostmo.commute.provider.EventContentProvider;

public class Main extends ListActivity {


    public static final String TAG = Market.TAG;

    
	DatabaseCommutes database;
	
	private static final int DIALOG_PLOT_METHOD = 1;
	private static final int DIALOG_CALENDARPICKER_DOWNLOAD = 8;


	private static final int REQUEST_CODE_NEW_PAIR = 1;

    // ========================================================================
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.main);
    	this.database = new DatabaseCommutes(this);
    	
        SimpleCursorAdapter sca = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null,
        		new String[] {
		    		DatabaseCommutes.KEY_TITLE,
		    		DatabaseCommutes.KEY_START_DESTINATION_ID},
        		new int[] {android.R.id.text1, android.R.id.text2});
        this.setListAdapter(sca);
        
        this.refreshCursor();
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
			.setTitle("Add current location?")
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.new_pair:
        {
        	startActivityForResult(new Intent(this, DestinationPairAssociator.class), REQUEST_CODE_NEW_PAIR);


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
        case R.id.menu_plot_times:
        {
        	
        	showDialog(DIALOG_PLOT_METHOD);
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

}