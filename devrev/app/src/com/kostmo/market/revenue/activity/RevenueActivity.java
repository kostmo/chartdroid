package com.kostmo.market.revenue.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.AppsOverviewActivity.WindowEvaluatorMode;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.service.DownloadRecordsService;
import com.kostmo.tools.DurationStrings;
import com.kostmo.tools.DurationStrings.TimescaleTier;

// ============================================================================
public class RevenueActivity extends Activity implements Disablable {

	public static final String TAG = Market.TAG;

	public static final String PREFKEY_SAVED_START_DATE = "PREFKEY_SAVED_START_DATE";
	public static final String PREFKEY_SAVED_DAY_COUNT = "PREFKEY_SAVED_DAY_COUNT";
	public static final String PREFKEY_SAVED_QUERY_TYPE = "PREFKEY_SAVED_QUERY_TYPE";
	public static final String PREFKEY_SAVED_WINDOW_EVALUATOR = "PREFKEY_SAVED_WINDOW_EVALUATOR";
	public static final String PREFKEY_SAVED_WINDOW_WIDTH = "PREFKEY_SAVED_WINDOW_WIDTH";
	

	public static final String EXTRA_NOTIFICATION_RETURNING = "EXTRA_NOTIFICATION_RETURNING";
	
	
	private static final int REQUEST_CODE_MERCHANT_CREDENTIALS = 1;

	
	private static final int DIALOG_INSTRUCTIONS = 1;
	public static final int DIALOG_ERROR = 2;
	public static final int DIALOG_3G_WARNING = 3;
	public static final int DIALOG_NO_CONNECTION = 4;

	public static final String PREFKEY_SHOW_REVENUE_ANALYSIS_INSTRUCTIONS = "PREFKEY_SHOW_REVENUE_ANALYSIS_INSTRUCTIONS";


	public static final int DAYS_PER_WEEK = DurationStrings.DAYS_PER_WEEK;
	public static final int DEFAULT_SPAN_DAYS = DAYS_PER_WEEK;
	public static final int INVALID_MERCHANT_ID = 0;
	public static final long MILLIS_PER_DAY = DurationStrings.MILLIS_TO_DAYS;
	public static final SimpleDateFormat HUMAN_DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
	public static final SimpleDateFormat HUMAN_FULL_MONTH_DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy");
	public static final SimpleDateFormat MINI_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");

	EditText bins_field, width_textbox;
	DatePickerDialog date_picker_dialog;
	TextView day_count, button_calendar_date, ending_date;
	SeekBar seek_bar;
	Button button_execute_plot;
	Spinner revenue_query_option, window_evaluator_mode, correlation_modes, bin_modes;

	View bincount_wrapper, comment_window_wrapper, correlation_mode_wrapper;
	
	Calendar start_date_calendar;
	boolean only_saving_credentials = false;
	String dialog_error_message = null;
	
	RecordFetchAssignment record_fetch_assignment = null;
	PendingAssignmentAndCredentialsForServiceBinding pending_assignment_and_credentials = null;

	DownloadRecordsService record_fetcher_service;
	SharedPreferences settings;
	DatabaseRevenue database;

	// ========================================================================
	public enum RecordFetcherTaskStage {
		GET_RECORD_GAPS, GET_INCOMPLETE_ITEM_IDS, MATCH_ITEM_NAMES
	}

	// ========================================================================
	public enum CorrelationMode {
		TIMELINE, CORRELATION
	}

	// ========================================================================
	enum RevenueQueryOption {
		REVENUE_TIMELINE, OVERALL_REVENUE, TOTAL_APP_REVENUE, REVENUE_RATING_RELATION, REVENUE_PRICE_RELATION
	}

	// ========================================================================
	public enum BinningMode {
		CUSTOM(null), HOURLY(TimescaleTier.HOURS), DAILY(TimescaleTier.DAYS), WEEKLY(TimescaleTier.WEEKS), MONTHLY(TimescaleTier.MONTHS);

		TimescaleTier timescale_tier;
		BinningMode(TimescaleTier tier) {
			this.timescale_tier = tier;
		}
		
		public int getBinCount(Uri uri, long millis) {

			if (this == CUSTOM) {
				return Integer.parseInt(uri.getQueryParameter(UriGenerator.QUERY_PARAMETER_HISTOGRAM_BINS));
			}

			return (int) (millis / this.timescale_tier.millis);
		}
		

		public String getDurationString(int bin_count, long millis) {
			String window_duration_string = "Revenue per ";
			
			if (this == CUSTOM) {

				long millisecond_bin_duration = millis / bin_count;
				float histogram_binwidth_days = millisecond_bin_duration / (float) RevenueActivity.MILLIS_PER_DAY;

				window_duration_string += String.format("%.1f days", histogram_binwidth_days);
				return window_duration_string;
			}
			
			window_duration_string += this.timescale_tier.getName();
			return window_duration_string;
		}
	}
	
	// ========================================================================
	public static class RecordFetchAssignment {
		public DateRange date_range;
		public RecordFetcherTaskStage fetcher_stage;

		public RecordFetchAssignment() {}

		public Long task_start_milliseconds = null;
		
		public List<DateRange> uncached_date_ranges;		
		public List<Long> incomplete_order_numbers;
	}

	// ========================================================================
	public static class PendingAssignmentAndCredentialsForServiceBinding {
		public RecordFetchAssignment assignment;
		public UsernamePasswordCredentials cred;
		PendingAssignmentAndCredentialsForServiceBinding(RecordFetchAssignment assignment, UsernamePasswordCredentials cred) {
			this.assignment = assignment;
			this.cred = cred;
		}
	}

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
		setContentView(R.layout.revenue);

		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
		this.database = new DatabaseRevenue(this);

		final Calendar now = new GregorianCalendar();

		this.day_count = (TextView) findViewById(R.id.day_count);
		this.ending_date = (TextView) findViewById(R.id.textview_ending_date);
		

		this.bincount_wrapper = findViewById(R.id.bincount_wrapper);
		this.comment_window_wrapper = findViewById(R.id.comment_window_wrapper);
		this.correlation_mode_wrapper = findViewById(R.id.correlation_mode_wrapper);
		
		
		/*
		findViewById(R.id.button_bins_days).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bins_field.setText( Integer.toString(seek_bar.getProgress()) );
			}
		});
		
		findViewById(R.id.button_bins_weeks).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				int weeks_count = (int) FloatMath.ceil(seek_bar.getProgress() / (float) DAYS_PER_WEEK);
				int minimum_days = DAYS_PER_WEEK*weeks_count;
				if (seek_bar.getMax() < minimum_days) {
					Calendar week_beginning = (Calendar) now.clone();
					week_beginning.add(Calendar.DAY_OF_MONTH, -minimum_days);
					start_date_calendar.setTime(week_beginning.getTime());
				}
				seek_bar.setProgress(minimum_days);
				bins_field.setText( Integer.toString(weeks_count) );

				updateDate(now);
			}
		});
		*/
		
		
		
		this.bin_modes = (Spinner) findViewById(R.id.bin_modes);
		this.bin_modes.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
				
				BinningMode binning_mode = BinningMode.values()[position];
				bins_field.setVisibility(BinningMode.CUSTOM.equals(binning_mode) ? View.VISIBLE : View.GONE);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		
		this.revenue_query_option = (Spinner) findViewById(R.id.revenue_query_option);
		this.revenue_query_option.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
				
				RevenueQueryOption query_option = RevenueQueryOption.values()[position];
				
				boolean is_comment_correlator = RevenueQueryOption.REVENUE_RATING_RELATION.equals( query_option );

				setBincountVisibility(position, correlation_modes.getSelectedItemPosition());
				comment_window_wrapper.setVisibility(is_comment_correlator ? View.VISIBLE : View.GONE);
				
				correlation_mode_wrapper.setVisibility(
						RevenueQueryOption.REVENUE_PRICE_RELATION.equals( query_option )
						|| RevenueQueryOption.REVENUE_RATING_RELATION.equals( query_option )
							? View.VISIBLE : View.GONE);

				
				((TextView) findViewById(R.id.analysis_explanation_textview)).setText(getResources().getStringArray(R.array.analysis_type_explanations)[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		findViewById(R.id.today_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seek_bar.setProgress(seek_bar.getMax());
			}
		});

		this.seek_bar = (SeekBar) findViewById(R.id.seek_bar);
		this.seek_bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				
				String count = getResources().getQuantityString(R.plurals.day_count, progress, progress);
				day_count.setText( count );
				ending_date.setText( HUMAN_DATE_FORMAT.format( getPlotDateRange().end ));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});


		this.button_execute_plot = (Button) findViewById(R.id.button_execute_plot);
		this.button_execute_plot.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (RevenueActivity.this.record_fetcher_service != null && RevenueActivity.this.record_fetcher_service.isInProgress()) {
					
					Log.d(TAG, "Record Fetcher Service is present and in progress, so I will cancel it.");
					
					RevenueActivity.this.record_fetcher_service.cancelEverything();
					
				} else {

					Log.d(TAG, "Saving form values to prefs and preparing record fetcher.");
					
					Editor editor = settings.edit();
					editor.putLong(PREFKEY_SAVED_START_DATE, start_date_calendar.getTimeInMillis());
					editor.putInt(PREFKEY_SAVED_DAY_COUNT, seek_bar.getProgress());
					editor.putInt(PREFKEY_SAVED_QUERY_TYPE, revenue_query_option.getSelectedItemPosition());
					editor.putInt(PREFKEY_SAVED_WINDOW_EVALUATOR, window_evaluator_mode.getSelectedItemPosition());
					editor.putInt(PREFKEY_SAVED_WINDOW_WIDTH, Integer.parseInt(width_textbox.getText().toString()));
					editor.commit();

			    	prepareRecordFetcher( getPlotDateRange() );
				}
			}
		});

		this.bins_field = (EditText) findViewById(R.id.bins_field);
		this.width_textbox = (EditText) findViewById(R.id.width_textbox);
		

		this.window_evaluator_mode = (Spinner) findViewById(R.id.window_evaluator_mode);
		this.correlation_modes = (Spinner) findViewById(R.id.correlation_modes);
		this.correlation_modes.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
				setBincountVisibility(revenue_query_option.getSelectedItemPosition(), position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		this.button_calendar_date = (TextView) findViewById(R.id.button_calendar_date);
		this.button_calendar_date.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				date_picker_dialog.show();
			}
		});


		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.start_date_calendar = state.calendar;
			this.only_saving_credentials = state.only_saving_credentials;
			this.record_fetch_assignment = state.record_fetch_assignment;
			this.pending_assignment_and_credentials = state.pending_assignment_and_credentials;
		} else {

			Calendar yesterday = (Calendar) now.clone();
			yesterday.add(Calendar.DAY_OF_MONTH, -DEFAULT_SPAN_DAYS);  

			if (this.settings.contains(PREFKEY_SAVED_START_DATE)) {

				this.start_date_calendar = new GregorianCalendar();
				this.start_date_calendar.setTimeInMillis(this.settings.getLong(PREFKEY_SAVED_START_DATE, yesterday.getTimeInMillis()));

			} else {
				this.start_date_calendar = yesterday;       		
			}


			this.revenue_query_option.setSelection(this.settings.getInt(PREFKEY_SAVED_QUERY_TYPE, RevenueQueryOption.REVENUE_TIMELINE.ordinal()));
			this.window_evaluator_mode.setSelection(this.settings.getInt(PREFKEY_SAVED_WINDOW_EVALUATOR, WindowEvaluatorMode.AVERAGE.ordinal()));
			this.width_textbox.setText(Integer.toString(this.settings.getInt(PREFKEY_SAVED_WINDOW_WIDTH, AppsOverviewActivity.DEFAULT_WINDOW_WIDTH)));
		}

		updateDate(now);

		if (state == null) {
			if (this.settings.contains(PREFKEY_SAVED_START_DATE)) {
				int days = this.settings.getInt(PREFKEY_SAVED_DAY_COUNT, 1);
				this.seek_bar.setProgress(days);
			}
		}

		this.date_picker_dialog = new DatePickerDialog(
			this,
			new DatePickerDialog.OnDateSetListener() {

				@Override
				public void onDateSet(DatePicker dp, int year, int monthOfYear, int dayOfMonth) {
					Log.d(TAG, "Old year: " + dp.getYear() + "; New year: " + year); 
	
					start_date_calendar.set(year, monthOfYear, dayOfMonth);
					Calendar now = new GregorianCalendar();
					Calendar yesterday = (Calendar) now.clone();
					yesterday.add(Calendar.DAY_OF_MONTH, -1);
					button_execute_plot.setEnabled( !start_date_calendar.after(yesterday) );
	
					updateDate(now);
				}
			},
			this.start_date_calendar.get(Calendar.YEAR),
			this.start_date_calendar.get(Calendar.MONTH),
			this.start_date_calendar.get(Calendar.DAY_OF_MONTH)
		);


		if (getIntent().getBooleanExtra(EXTRA_NOTIFICATION_RETURNING, false)) {
			Log.d(TAG, "Am I returning from a notification?");
			prepareRecordFetcher( getPlotDateRange() );
		}
		
		// We need to check whether to disable the Plot button
		bindServiceOnly();
		
		
		if (savedInstanceState == null) {
			if (!this.settings.getBoolean(PREFKEY_SHOW_REVENUE_ANALYSIS_INSTRUCTIONS, false)) {
				showDialog(DIALOG_INSTRUCTIONS);
			}
		}
	}

	// ========================================================================
	void wrap3gCheck() {
		
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
	    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

	    if (mWifi.isAvailable()) {

	    	promptCredentialsOrExecute();
	    	
	    } else if (mMobile.isAvailable()) {
	    	showDialog(DIALOG_3G_WARNING);
	    } else {
	    	showDialog(DIALOG_NO_CONNECTION);
	    }
	}

	// ========================================================================
	void setBincountVisibility(int revenue_analysis_mode_index, int correlation_mode_index) {

		RevenueQueryOption query_option = RevenueQueryOption.values()[revenue_analysis_mode_index];
		CorrelationMode correlation_mode = CorrelationMode.values()[correlation_mode_index];
		bincount_wrapper.setVisibility(
				RevenueQueryOption.REVENUE_TIMELINE.equals( query_option )
				|| RevenueQueryOption.OVERALL_REVENUE.equals( query_option )
				|| (
						(RevenueQueryOption.REVENUE_RATING_RELATION.equals( query_option )
						|| RevenueQueryOption.REVENUE_PRICE_RELATION.equals( query_option )
					) && CorrelationMode.TIMELINE.equals(correlation_mode))
				? View.VISIBLE : View.GONE);
	}
	
	// ========================================================================
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "In onNewIntent()");

		if (intent.getBooleanExtra(EXTRA_NOTIFICATION_RETURNING, false)) {
			Log.d(TAG, "Returning from a notification...");
			prepareRecordFetcher( getPlotDateRange() );
		}
	}
	
	// ========================================================================
	class StateObject {
		Calendar calendar;
		boolean only_saving_credentials;
		RecordFetchAssignment record_fetch_assignment;
		PendingAssignmentAndCredentialsForServiceBinding pending_assignment_and_credentials;
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		StateObject state = new StateObject();
		state.calendar = this.start_date_calendar;
		state.only_saving_credentials = this.only_saving_credentials;
		state.record_fetch_assignment = this.record_fetch_assignment;
		state.pending_assignment_and_credentials = this.pending_assignment_and_credentials;
		return state;
	}


	// ========================================================================
	void carryOutAssignment(RecordFetchAssignment assignment, UsernamePasswordCredentials cred) {

		Log.d(TAG, "Carrying out assignment...");

		if (this.record_fetcher_service != null) {

			Log.d(TAG, "(for real this time)");

			this.record_fetcher_service.carryOutAssignment(assignment, cred);
		} else {

			Log.d(TAG, "(must connect to Service first...)");

			this.pending_assignment_and_credentials = new PendingAssignmentAndCredentialsForServiceBinding(
					assignment,
					cred
			);
			
			this.starting_service = true;
			bindServiceOnly();
		}

	}
	
	// ========================================================================
	void promptCredentialsOrExecute() {

		long stored_merchant_id = this.settings.getLong(MerchantCredentialsActivity.PREFKEY_SAVED_MERCHANT_ID, INVALID_MERCHANT_ID);
		String stored_merchant_key = this.settings.getString(MerchantCredentialsActivity.PREFKEY_SAVED_MERCHANT_KEY, null);
		if (stored_merchant_id != INVALID_MERCHANT_ID && stored_merchant_key != null) {

			UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
					Long.toString(stored_merchant_id),
					stored_merchant_key);

			carryOutAssignment(this.record_fetch_assignment, cred);

		} else {
			only_saving_credentials = false;	// FIXME
			startActivityForResult(
					new Intent(this, MerchantCredentialsActivity.class),
					REQUEST_CODE_MERCHANT_CREDENTIALS);
		}
	}

	// ========================================================================
	void prepareRecordFetcher(DateRange date_range) {
		Log.d(TAG, "prepareRecordFetcher()");

		// Examines the date range to only fetch uncached records
		List<DateRange> uncached_date_ranges = this.database.getUncachedDateRanges(date_range);
		Log.d(TAG, "There are " + uncached_date_ranges.size() + " gaps in the cache.");

		if (uncached_date_ranges.size() > 0) {

			this.record_fetch_assignment = new RecordFetchAssignment();
			this.record_fetch_assignment.date_range = date_range;
			this.record_fetch_assignment.uncached_date_ranges = uncached_date_ranges;
			this.record_fetch_assignment.fetcher_stage = RecordFetcherTaskStage.GET_RECORD_GAPS;

			wrap3gCheck();

		} else {
			// We might still need to fetch Merchant Item ID associations.

			List<Long> incomplete_order_numbers = this.database.queryOrdersLackingItemIds(date_range);
			Log.i(TAG, "Number of orders lacking merchant IDs: " + incomplete_order_numbers.size());

			if (incomplete_order_numbers.size() > 0) {

				this.record_fetch_assignment = new RecordFetchAssignment();
				this.record_fetch_assignment.date_range = date_range;
				this.record_fetch_assignment.incomplete_order_numbers = incomplete_order_numbers;
				this.record_fetch_assignment.fetcher_stage = RecordFetcherTaskStage.GET_INCOMPLETE_ITEM_IDS;

				wrap3gCheck();

			} else {
				
				List<Long> unlabeled_merchant_item_representatives = this.database.getRepresentativeUnlabeledMerchantItemIds(date_range);
				Log.d(TAG, "Count of unlabeled representatives: " + unlabeled_merchant_item_representatives.size());
				
				if (unlabeled_merchant_item_representatives.size() > 0) {
					this.record_fetch_assignment = new RecordFetchAssignment();
					this.record_fetch_assignment.date_range = date_range;
					this.record_fetch_assignment.incomplete_order_numbers = new ArrayList<Long>();
					this.record_fetch_assignment.fetcher_stage = RecordFetcherTaskStage.MATCH_ITEM_NAMES;

					wrap3gCheck();
					
				} else {
					// Looks like we have absolutely everything cached. Plot away!
					launchPlot(date_range);
				}
			}
		}
	}

	// ========================================================================
	void launchPlot(DateRange date_range) {

		Intent i = new Intent(this, ConsolidationActivity.class);


		BinningMode binning_mode = BinningMode.values()[this.bin_modes.getSelectedItemPosition()];
		i.putExtra(ConsolidationActivity.EXTRA_BINNING_MODE, binning_mode.ordinal());
		if (BinningMode.CUSTOM.equals(binning_mode)) {
			// Some analysis modes don't require the bins extra, but we supply it anyway.
			String bincount_text = this.bins_field.getText().toString();
			if (bincount_text.length() > 0) {
				int bin_count = Integer.parseInt(bincount_text);
				i.putExtra(ConsolidationActivity.EXTRA_CUSTOM_HISTOGRAM_BINS, bin_count);
			}
		}
		i.putExtra(ConsolidationActivity.EXTRA_QUERY_TYPE, this.revenue_query_option.getSelectedItemPosition());
		
		RevenueQueryOption query_mode = RevenueQueryOption.values()[this.revenue_query_option.getSelectedItemPosition()];
		if (RevenueQueryOption.REVENUE_RATING_RELATION.equals(query_mode)
				|| RevenueQueryOption.REVENUE_PRICE_RELATION.equals(query_mode)) {
			
			CorrelationMode correlation_mode = CorrelationMode.values()[this.correlation_modes.getSelectedItemPosition()];
			i.putExtra(ConsolidationActivity.EXTRA_CORRELATION_MODE, correlation_mode.ordinal());
		}

		if (RevenueQueryOption.REVENUE_RATING_RELATION.equals(query_mode)) {
			WindowEvaluatorMode window_evaluator = WindowEvaluatorMode.values()[window_evaluator_mode.getSelectedItemPosition()];
			i.putExtra(ConsolidationActivity.EXTRA_WINDOW_EVALUATOR, window_evaluator.ordinal());
			
			int window_width = Integer.parseInt(width_textbox.getText().toString());
			i.putExtra(ConsolidationActivity.EXTRA_WINDOW_WIDTH, window_width);
		}

		i.putExtra(ConsolidationActivity.EXTRA_START_MILLISECONDS, date_range.start.getTime());
		i.putExtra(ConsolidationActivity.EXTRA_END_MILLISECONDS, date_range.end.getTime());
		startActivity(i);
	}

	// ========================================================================
	/** Must supply the current date as an argument. */
	void updateDate(Calendar now) {
		updateSeekBarMax(this.start_date_calendar, now);
		this.button_calendar_date.setText( HUMAN_DATE_FORMAT.format( this.start_date_calendar.getTime() ) );
	}

	// ========================================================================
	void updateSeekBarMax(Calendar start, Calendar now) {

		long millis_diff = now.getTimeInMillis() - start.getTimeInMillis();
		int days = (int) Math.max(1, millis_diff / MILLIS_PER_DAY);
//		Log.d(TAG, "Set seek bar max to " + days + " days.");
		this.seek_bar.setMax(days);

		int old_progress = this.seek_bar.getProgress();
		this.seek_bar.setProgress(0);
		this.seek_bar.setProgress(old_progress);
	}

	// ========================================================================
	DateRange getPlotDateRange() {
		DateRange date_range = new DateRange();
		date_range.start = new Date(this.start_date_calendar.getTimeInMillis());

		Calendar end_date = (Calendar) this.start_date_calendar.clone();
		end_date.add(Calendar.DATE, this.seek_bar.getProgress());
		date_range.end = end_date.getTime();

		return date_range;
	}

	// ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

		LayoutInflater factory = LayoutInflater.from(this);

		switch (id) {
		case DIALOG_INSTRUCTIONS:
		{
			final CheckBox reminder_checkbox;
			View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
			reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

			((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_revenue_analysis);
			
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.instructions_title_revenue_analysis)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					settings.edit().putBoolean(PREFKEY_SHOW_REVENUE_ANALYSIS_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
				}
			})
			.create();
		}
		case DIALOG_ERROR:
		{
			return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.merchant_retrieval_error_title)
				.setMessage(this.dialog_error_message)
				.setPositiveButton(R.string.alert_dialog_cancel, null)
				.create();
		}
		case DIALOG_3G_WARNING:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("3G Has Issues")
			.setMessage("You are connected to a mobile network instead of a Wi-Fi network. Sometimes there is a problem connecting to HTTPS on 3G.  Switch to Wi-Fi if downloads get stuck.")
			.setPositiveButton("Proceed Anyway", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
			    	promptCredentialsOrExecute();
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, null)
			.create();
		}
		case DIALOG_NO_CONNECTION:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("No Connection")
			.setMessage("You lack network connectivity. This probably won't work.")
			.setPositiveButton("Proceed Anyway", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
			    	promptCredentialsOrExecute();
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, null)
			.create();
		}
		default:
			return super.onCreateDialog(id);
		}
	}



	// ========================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_revenue, menu);
		return true;
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case R.id.menu_merchant_credentials:
        {
        	Intent i = new Intent(this, MerchantCredentialsActivity.class);
        	i.putExtra(MerchantCredentialsActivity.EXTRA_ONLY_SAVING_CREDENTIALS, true);
        	startActivity(i);
            return true;
        }
		case R.id.menu_preferences:
		{
			startActivity(new Intent(this, MainPreferences.class));
			return true;
		}
        case R.id.menu_instructions:
        {
        	showDialog(DIALOG_INSTRUCTIONS);
            return true;
        }
		}

		return super.onOptionsItemSelected(item);
	}

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

			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			RevenueActivity.this.record_fetcher_service = ((DownloadRecordsService.LocalBinder) service).getService();
			RevenueActivity.this.record_fetcher_service.setDisablableHost(RevenueActivity.this);

			
			if (RevenueActivity.this.starting_service) {
				RevenueActivity.this.starting_service = false;
				carryOutAssignment(
						RevenueActivity.this.pending_assignment_and_credentials.assignment,
						RevenueActivity.this.pending_assignment_and_credentials.cred);
			} else {
				
				if (RevenueActivity.this.record_fetcher_service != null && RevenueActivity.this.record_fetcher_service.isInProgress()) {
					disable();
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			RevenueActivity.this.record_fetcher_service = null;
		}
	};
	
	

	// ========================================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
			case REQUEST_CODE_MERCHANT_CREDENTIALS:
			{
				if (resultCode == RESULT_OK) {
					if (!only_saving_credentials) {

						UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
								Long.toString(data.getLongExtra(MerchantCredentialsActivity.EXTRA_MERCHANT_ID, INVALID_MERCHANT_ID)),
								data.getStringExtra(MerchantCredentialsActivity.EXTRA_MERCHANT_KEY));

						carryOutAssignment(record_fetch_assignment, cred);
					}
				}
				break;
			}
	   		default:
		    	break;
		   }
		}
    }
	
	// ========================================================================
	Intent bindServiceOnly() {
		Intent i = new Intent(this, DownloadRecordsService.class);
		bindService(i, this.mConnection, Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND );
		return i;
	}

	// ========================================================================
	@Override
	public void disable() {
		this.button_execute_plot.setText(R.string.alert_dialog_cancel);
		this.button_execute_plot.setTextColor(Color.RED);
	}

	// ========================================================================
	@Override
	public void reEnable() {
		this.button_execute_plot.setText(R.string.prepare_plot);
		this.button_execute_plot.setTextColor(Color.BLACK);
	}
}