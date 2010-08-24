package com.googlecode.chartdroid.market.sales;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.googlecode.chartdroid.core.ColumnSchema;
import com.googlecode.chartdroid.market.sales.container.DateRange;
import com.googlecode.chartdroid.market.sales.container.UsernamePassword;
import com.googlecode.chartdroid.market.sales.task.SpreadsheetFetcherTask;

// ============================================================================
public class Main extends Activity {

	public static final String TAG = Market.TAG;

	public static final String PREFKEY_SAVED_USERNAME = "PREFKEY_SAVED_USERNAME";
	public static final String PREFKEY_SAVED_PASSWORD = "PREFKEY_SAVED_PASSWORD";
	
	public static final int DIALOG_LOGIN_INFO = 1;
	public static final int DIALOG_INSTALL_CHARTDROID = 2;
	public static final int DIALOG_DEVREV_ADVERTISEMENT = 3;
	

    static final String GOOGLE_CODE_URL = "http://code.google.com/p/chartdroid/wiki/MarketSalesPlotter";
    

	public static final long MILLIS_PER_DAY = 1000*60*60*24;


	EditText bins_field;
	DatePicker date_picker_widget;
	TextView day_count;
	SeekBar seek_bar;

	// ========================================================================
	public static Calendar getCalendarFromDatePicker(DatePicker dp) {
		return new GregorianCalendar(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
	}

	// ========================================================================
	public static void setDatePickerFromCalendar(DatePicker dp, Calendar calendar) {
		dp.updateDate(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH)
		);
	}
	
	// ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		day_count = (TextView) findViewById(R.id.day_count);
		
		seek_bar = (SeekBar) findViewById(R.id.seek_bar);
		seek_bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				day_count.setText( Integer.toString(progress) );
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		
		
		
		final Button button_execute_plot = (Button) findViewById(R.id.button_execute_plot);
		button_execute_plot.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(DIALOG_LOGIN_INFO);
			}
		});
		
		bins_field = (EditText) findViewById(R.id.bins_field);
		date_picker_widget = (DatePicker) findViewById(R.id.date_picker_widget);
		
		
		
		
		Calendar yesterday = null;
		Calendar now = new GregorianCalendar();
		
        final StateObject state = (StateObject) getLastNonConfigurationInstance();
        if (state != null) {
        	yesterday = state.calendar;
        } else {
    		yesterday = (Calendar) now.clone();
    		yesterday.add(Calendar.DAY_OF_MONTH, -1);
        }

		
		updateSeekBarMax(yesterday, now);
		
		date_picker_widget.init(
				yesterday.get(Calendar.YEAR),
				yesterday.get(Calendar.MONTH),
				yesterday.get(Calendar.DAY_OF_MONTH),
				new DatePicker.OnDateChangedListener() {
			
			@Override
			public void onDateChanged(DatePicker dp, int year, int monthOfYear, int dayOfMonth) {
				
				Log.d(TAG, "Old year: " + dp.getYear() + "; New year: " + year); 
				
				Calendar start = new GregorianCalendar(year, monthOfYear, dayOfMonth);
				Calendar now = new GregorianCalendar();
				Calendar yesterday = (Calendar) now.clone();
				yesterday.add(Calendar.DAY_OF_MONTH, -1);
				button_execute_plot.setEnabled( !start.after(yesterday) );

				updateSeekBarMax(start, now);
			}
		});
		

		
		Intent chardroid_dummy_intent = new Intent(Intent.ACTION_VIEW);
		chardroid_dummy_intent.setType(ColumnSchema.EventData.CONTENT_TYPE_PLOT_DATA);
		if (!Market.isIntentAvailable(this, chardroid_dummy_intent) ) {
			showDialog(DIALOG_INSTALL_CHARTDROID);
		}
		
		
		if (savedInstanceState == null) {
			if (android.os.Build.VERSION.SDK_INT >= 8) {
				Log.d(TAG, "Device is running Froyo");
				showDialog(DIALOG_DEVREV_ADVERTISEMENT);
			}
		}
	}


	// ========================================================================
    class StateObject {
    	Calendar calendar;
    }

	// ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	
    	StateObject state = new StateObject();
    	state.calendar = getCalendarFromDatePicker(date_picker_widget);

        return state;
    }
    
	// ========================================================================
	void updateSeekBarMax(Calendar start, Calendar now) {

		long millis_diff = now.getTimeInMillis() - start.getTimeInMillis();
		long days = Math.max(1, millis_diff / MILLIS_PER_DAY);
		seek_bar.setMax((int) days);
		
		int old_progress = seek_bar.getProgress();
		seek_bar.setProgress(0);
		seek_bar.setProgress(old_progress);
	}

	// ========================================================================
	void executePlot(UsernamePassword user_pass) {
		
		Calendar start_date = getCalendarFromDatePicker(date_picker_widget);

		Calendar end_date = (Calendar) start_date.clone();
		end_date.add(Calendar.DATE, seek_bar.getProgress());
	
		int bin_count = Integer.parseInt(bins_field.getText().toString());
		new SpreadsheetFetcherTask(
				this,
				user_pass,
				new DateRange(start_date.getTime(), end_date.getTime()),
				bin_count
			).execute();
	}

	// ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {
    	
        switch (id) {
        case DIALOG_LOGIN_INFO:
        {
	        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        	
	        LayoutInflater factory = LayoutInflater.from(Main.this);
	        final View textEntryView = factory.inflate(R.layout.dialog_login_standard, null);
	        
	        final EditText username_box = (EditText) textEntryView.findViewById(R.id.username_edit);
	        String username = settings.getString(PREFKEY_SAVED_USERNAME, null);
	        if (username != null)
	        	username_box.setText(username);
	        
	        final EditText password_box = (EditText) textEntryView.findViewById(R.id.password_edit);
	        String password = settings.getString(PREFKEY_SAVED_PASSWORD, null);
	        if (password != null)
	        	password_box.setText(password);

	    	return new AlertDialog.Builder(Main.this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.login_dialog_title)
	        .setView(textEntryView)
	        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	
	            	String username = username_box.getText().toString();
	            	settings.edit().putString(PREFKEY_SAVED_USERNAME, username).commit();
	            	
	            	String password = password_box.getText().toString();
	            	CheckBox checkbox_save_password = (CheckBox) textEntryView.findViewById(R.id.checkbox_save_password);
	            	if (checkbox_save_password.isChecked())
	            		settings.edit().putString(PREFKEY_SAVED_PASSWORD, password).commit();
	            	
	            	UsernamePassword user_pass = new UsernamePassword(username, password);
	            	executePlot(user_pass);
	            }
	        })
	        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {

	            }
	        })
	        .create();
        }
        case DIALOG_INSTALL_CHARTDROID:
        {
	    	return new AlertDialog.Builder(Main.this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle("ChartDroid Needed")
	        .setMessage("Download ChartDroid application from the Market?")
	        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	Market.launchMarketSearch(Main.this, Market.MARKET_CHARTDROID_DETAILS_STRING);
	            }
	        })
	        .setNegativeButton(R.string.alert_dialog_cancel, null)
	        .create();
        }
        case DIALOG_DEVREV_ADVERTISEMENT:
        {
	    	return new AlertDialog.Builder(Main.this)
	        .setIcon(android.R.drawable.ic_dialog_info)
	        .setTitle(R.string.advertisement_devrev_title)
	        .setMessage(R.string.advertisement_devrev_blurb)
	        .setPositiveButton(R.string.view_on_market, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	Market.launchMarketSearch(Main.this, Market.MARKET_DEVREV_DETAILS_STRING);
	            }
	        })
	        .setNegativeButton(R.string.alert_dialog_cancel, null)
	        .create();
        }
        }
		return null;
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
			Uri flickr_destination = Uri.parse( GOOGLE_CODE_URL );
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
}