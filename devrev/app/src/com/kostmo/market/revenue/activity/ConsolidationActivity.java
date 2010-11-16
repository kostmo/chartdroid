package com.kostmo.market.revenue.activity;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.CursorTreeAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;

import com.gc.android.market.api.model.Market.App;
import com.gc.android.market.api.model.Market.AppsRequest.ViewType;
import com.googlecode.chartdroid.core.IntentConstants;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.AppsOverviewActivity.WindowEvaluatorMode;
import com.kostmo.market.revenue.activity.RevenueActivity.BinningMode;
import com.kostmo.market.revenue.activity.RevenueActivity.CorrelationMode;
import com.kostmo.market.revenue.activity.RevenueActivity.RevenueQueryOption;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.adapter.AppExpandableListAdapter;
import com.kostmo.market.revenue.container.CommentsProgressPacket;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.UriGenerator;
import com.kostmo.market.revenue.provider.DatabaseRevenue.Debug;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationAbsoluteRevenuePrice;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationAbsoluteRevenueRatings;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationTimelineRevenuePrice;
import com.kostmo.market.revenue.provider.plotmodes.CorrelationTimelineRevenueRatings;
import com.kostmo.market.revenue.provider.plotmodes.RevenueMultiTimeline;
import com.kostmo.market.revenue.provider.plotmodes.RevenueTotals;
import com.kostmo.market.revenue.task.AppSyncTask;
import com.kostmo.market.revenue.task.CommentSyncTask;
import com.kostmo.market.revenue.task.ProgressHostActivity;
import com.kostmo.tools.SemaphoreHost;


public class ConsolidationActivity extends ExpandableListActivity implements SemaphoreHost, ProgressHostActivity {

	static final String TAG = "ConsolidationActivity";

	public static final String EXTRA_START_MILLISECONDS = "EXTRA_START_MILLISECONDS";
	public static final String EXTRA_END_MILLISECONDS = "EXTRA_END_MILLISECONDS";
	public static final String EXTRA_QUERY_TYPE = "EXTRA_QUERY_TYPE";
	public static final String EXTRA_WINDOW_EVALUATOR = "EXTRA_WINDOW_EVALUATOR";
	public static final String EXTRA_WINDOW_WIDTH = "EXTRA_WINDOW_WIDTH";
	public static final String EXTRA_CORRELATION_MODE = "EXTRA_CORRELATION_MODE";

	public static final String EXTRA_BINNING_MODE = "EXTRA_BINNING_MODE";
	public static final String EXTRA_CUSTOM_HISTOGRAM_BINS = "EXTRA_CUSTOM_HISTOGRAM_BINS";
	public static final int DEFAULT_HISTOGRAM_BINCOUNT = 20;
	
	
	public static final String DOLLAR_AXIS_FORMAT = "$%.2f";
	public static final String FUDGED_DOLLAR_AXIS_FORMAT = "." + DOLLAR_AXIS_FORMAT;	// XXX Leading decimal is to work around bug in dollar sign width measuring

    public static String UNGROUPED_APPS_LABEL = "Ungrouped";

	
	DateRange date_range;
	RevenueQueryOption selected_query_type;
	TextView cached_ratings_range;
	String pending_dialog_message;
	

	public static final int INVALID_APP_ID = 0;

	private static final int DIALOG_INSTRUCTIONS = 1;
	private static final int DIALOG_MARKET_APP_SELECTION = 2;
	private static final int DIALOG_PUBLISHER_SELECTION = 3;
	private static final int DIALOG_PROGRESS_APP_SEARCH = 4;
	private static final int DIALOG_PROGRESS_COMMENT_SYNC = 5;
	private static final int DIALOG_MARKET_API_ERROR = 6;
	private static final int DIALOG_PUBLISHER_FILTER = 7;
	
	
	
	public static final String PREFKEY_SHOW_CONSOLIDATION_EDITOR_INSTRUCTIONS = "PREFKEY_SHOW_CONSOLIDATION_EDITOR_INSTRUCTIONS";
	
	DatabaseRevenue database;
	SharedPreferences settings;
	Toast error_toast;
	View button_sync_paid_apps, button_update_ratings;

	Cursor publishers_cursor;
	String active_publisher_name_filter = null;

	AppSearchTaskExtended apps_search_task;
	CommentSyncTaskExtended comment_sync_task;
	ProgressDialog progress_dialog_app_search, progress_dialog_comments_sync;
	
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();
	boolean plotting_app_selection = true;
	boolean app_synchronization_paid_only = true;
	long pending_consolidation_child = INVALID_APP_ID;

	// ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

		setContentView(R.layout.list_activity_consolidation_editor);
		
		// TODO
//		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);


		this.error_toast = Toast.makeText(this, "Error", Toast.LENGTH_LONG);
		this.database = new DatabaseRevenue(this);
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
		this.date_range = getDateRange();
		this.selected_query_type = RevenueQueryOption.values()[getIntent().getIntExtra(EXTRA_QUERY_TYPE, RevenueQueryOption.TOTAL_APP_REVENUE.ordinal())];

		
		
		this.button_sync_paid_apps = findViewById(R.id.button_sync_paid_apps);
		this.button_update_ratings = findViewById(R.id.button_update_ratings);
		
		this.cached_ratings_range = (TextView) findViewById(R.id.cached_ratings_range);
		
		this.button_sync_paid_apps.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				app_synchronization_paid_only = true;
				showDialog(DIALOG_PUBLISHER_SELECTION);
			}
		});

		this.button_update_ratings.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				List<Long> app_ids = database.getPublishedAppIds();
				comment_sync_task = new CommentSyncTaskExtended();
				comment_sync_task.execute(app_ids.toArray(new Long[0]));
			}
		});

		AppExpandableListAdapter adapter = new AppExpandableListAdapter(
				this,
				null,
				R.layout.list_item_app_parent,
				R.layout.list_item_merchant_item_child,
				this.database);

		setListAdapter(adapter);
		registerForContextMenu(getExpandableListView());
		

		TextView date_range_textview = (TextView) findViewById(R.id.date_range_textview);
		date_range_textview.setText(this.date_range.formatLayout(RevenueActivity.HUMAN_DATE_FORMAT, this));
		
		Button button_execute_plot = (Button) findViewById(R.id.button_execute_plot);
		button_execute_plot.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (	RevenueQueryOption.REVENUE_RATING_RELATION.equals(selected_query_type)
						|| RevenueQueryOption.REVENUE_PRICE_RELATION.equals(selected_query_type)) {
					plotting_app_selection = true;
					showDialog(DIALOG_MARKET_APP_SELECTION);
				} else {
					launchPlot();
				}
			}
		});


		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			
			this.publishers_cursor = state.publishers_cursor;
			this.active_publisher_name_filter = state.active_publisher_name_filter;
			
			this.plotting_app_selection = state.plotting_app_selection;
			this.pending_consolidation_child = state.pending_consolidation_child;
			this.pending_dialog_message = state.pending_dialog_message;
			this.app_synchronization_paid_only = state.app_synchronization_paid_only;
			adapter.setCachedImageMap(state.image_cache);
			

			this.apps_search_task = state.apps_search_task;
			if (this.apps_search_task != null)
				this.apps_search_task.updateActivity(this);

			this.comment_sync_task = state.comment_sync_task;
			if (this.comment_sync_task != null)
				this.comment_sync_task.updateActivity(this);
			
		}

		if (savedInstanceState == null) {
			if (!this.settings.getBoolean(PREFKEY_SHOW_CONSOLIDATION_EDITOR_INSTRUCTIONS, false)) {
				showDialog(DIALOG_INSTRUCTIONS);
			}
		}
	}
	
	// ========================================================================
	class StateObject {
		boolean plotting_app_selection;
		boolean app_synchronization_paid_only;
		
		long pending_consolidation_child;
		String pending_dialog_message;
		Map<Long, SoftReference<Bitmap>> image_cache;

		AppSearchTaskExtended apps_search_task;
		CommentSyncTaskExtended comment_sync_task;
		
		Cursor publishers_cursor;
		String active_publisher_name_filter;
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		StateObject state = new StateObject();
		state.plotting_app_selection = this.plotting_app_selection;
		state.app_synchronization_paid_only = this.app_synchronization_paid_only;
		
		state.pending_consolidation_child = this.pending_consolidation_child;
		state.image_cache = ((AppExpandableListAdapter) getExpandableListAdapter()).getCachedImageMap();

		state.apps_search_task = this.apps_search_task;
		state.comment_sync_task = this.comment_sync_task;
		state.publishers_cursor = this.publishers_cursor;
		
		state.active_publisher_name_filter = this.active_publisher_name_filter;
		return state;
	}

	// ========================================================================
	@Override
	protected void onResume() {
		super.onResume();
		
		updateList();
	}
	
	// ========================================================================
	DateRange getDateRange() {
		return new DateRange(
				new Date(getIntent().getLongExtra(EXTRA_START_MILLISECONDS, 0)),
				new Date(getIntent().getLongExtra(EXTRA_END_MILLISECONDS, 0))
			);
	}

	// ========================================================================
	void launchPriceRevenueCorrelation(long selected_app_id, String app_title) {
		Log.d(TAG, "launchPriceRevenueCorrelation()");

		Log.d(TAG, "I am in revenue/price mode with selected app ID: " + selected_app_id);

		int correlation_mode_index = getIntent().getIntExtra(EXTRA_CORRELATION_MODE, CorrelationMode.CORRELATION.ordinal());
		CorrelationMode correlation_mode = CorrelationMode.values()[correlation_mode_index];

		String partial_title = null;
		
		Intent i = new Intent(Intent.ACTION_VIEW);
		switch (correlation_mode) {
		case CORRELATION:
			i.setData(
					CorrelationAbsoluteRevenuePrice.constructUri(
					selected_app_id,
					this.date_range));
//			i.setClassName("com.googlecode.chartdroid", "org.achartengine.activity.ScatterChartActivity");
			i.setClassName("com.googlecode.chartdroid", "org.achartengine.activity.LineChartActivity");

			partial_title = "Revenue-Price correlation";

			ArrayList<String> axis_titles = new ArrayList<String>();
			axis_titles.add("Price");
			axis_titles.add("Daily Revenue");
			i.putExtra(IntentConstants.Meta.Axes.EXTRA_AXIS_TITLES, axis_titles);
			
			i.putExtra(IntentConstants.Meta.Series.EXTRA_SERIES_LABELS, new String[] {app_title});
			i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_X, DOLLAR_AXIS_FORMAT);
			break;
		case TIMELINE:
			i.setData(
					CorrelationTimelineRevenuePrice.constructUri(
					selected_app_id,
					this.date_range,
					BinningMode.values()[getIntent().getIntExtra(EXTRA_BINNING_MODE, BinningMode.CUSTOM.ordinal())],
					getIntent().getIntExtra(EXTRA_CUSTOM_HISTOGRAM_BINS, DEFAULT_HISTOGRAM_BINCOUNT)));
//			i.setClassName("com.googlecode.chartdroid", "org.achartengine.activity.BubbleChartActivity");

			partial_title = "Revenue and Price,";
			
//			i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y_SECONDARY, FUDGED_DOLLAR_AXIS_FORMAT);
			i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y_SECONDARY, DOLLAR_AXIS_FORMAT);
			break;
		}
		
		i.putExtra(Intent.EXTRA_TITLE, partial_title + " " + this.date_range.format(RevenueActivity.MINI_DATE_FORMAT));
		
		
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, FUDGED_DOLLAR_AXIS_FORMAT);
		i.putExtra(IntentConstants.Meta.Series.EXTRA_RAINBOW_COLORS, true);
		
		Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
	}

	// ========================================================================
	void launchRatingsRevenueCorrelation(long selected_app_id, String app_title) {
		
		int window_width = getIntent().getIntExtra(EXTRA_WINDOW_WIDTH, AppsOverviewActivity.DEFAULT_WINDOW_WIDTH);

		int window_evaluator_type_index = getIntent().getIntExtra(EXTRA_WINDOW_EVALUATOR, WindowEvaluatorMode.AVERAGE.ordinal());
		WindowEvaluatorMode window_evaluator_type = WindowEvaluatorMode.values()[window_evaluator_type_index];
		
		int correlation_mode_index = getIntent().getIntExtra(EXTRA_CORRELATION_MODE, CorrelationMode.CORRELATION.ordinal());
		CorrelationMode correlation_mode = CorrelationMode.values()[correlation_mode_index];


		String partial_title = null;
		Intent i = new Intent(Intent.ACTION_VIEW);
		switch (correlation_mode) {
		case CORRELATION:
			i.setData(CorrelationAbsoluteRevenueRatings.constructUri(
					selected_app_id,
					window_width,
					this.date_range,
					window_evaluator_type));
			i.setClassName("com.googlecode.chartdroid", "org.achartengine.activity.LineChartActivity");
			
			partial_title = "Revenue-Rating correlation";
			
			ArrayList<String> axis_titles = new ArrayList<String>();
			axis_titles.add("Rating");
			axis_titles.add("Daily Revenue");
			i.putExtra(IntentConstants.Meta.Axes.EXTRA_AXIS_TITLES, axis_titles);
			i.putExtra(IntentConstants.Meta.Series.EXTRA_SERIES_LABELS, new String[] {app_title});

			i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_X, "%.1f");
			break;
		case TIMELINE:
			i.setData(CorrelationTimelineRevenueRatings.constructUri(
					selected_app_id,
					window_width,
					this.date_range,
					window_evaluator_type,
					BinningMode.values()[getIntent().getIntExtra(EXTRA_BINNING_MODE, BinningMode.CUSTOM.ordinal())],
					getIntent().getIntExtra(EXTRA_CUSTOM_HISTOGRAM_BINS, DEFAULT_HISTOGRAM_BINCOUNT)));
//			i.setClassName("com.googlecode.chartdroid", "org.achartengine.activity.BubbleChartActivity");

			partial_title = "Revenue and Ratings,";
			
			i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y_SECONDARY, "%.1f");
			break;
		}

		Log.d(TAG, "I am in here with URI: " + i.getData());
		
		i.putExtra(Intent.EXTRA_TITLE, partial_title + " " + this.date_range.format(RevenueActivity.MINI_DATE_FORMAT));
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, FUDGED_DOLLAR_AXIS_FORMAT);
		i.putExtra(IntentConstants.Meta.Series.EXTRA_RAINBOW_COLORS, true);
		
		Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
	}
	
	// ========================================================================
	void launchRevenueTotalsBarPlot() {
		
		Log.d(TAG, "launchRevenueTotalsBarPlot()");
		Uri revenue_totals_uri = UriGenerator.constructRevenueTotalsUri(this.date_range);

		Intent i = new Intent(Intent.ACTION_VIEW, revenue_totals_uri);
		// XXX Explicitly launches the Bar chart
		i.setClassName("com.googlecode.chartdroid", "org.achartengine.activity.BarChartActivity");
		
		i.putExtra(Intent.EXTRA_TITLE, "Revenue " + this.date_range.format(RevenueActivity.MINI_DATE_FORMAT));
		
		ArrayList<String> axis_titles = new ArrayList<String>();
		axis_titles.add("Application");
		axis_titles.add("Total Revenue");
		
		i.putExtra(IntentConstants.Meta.Series.EXTRA_RAINBOW_COLORS, true);
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_AXIS_TITLES, axis_titles);
		
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, FUDGED_DOLLAR_AXIS_FORMAT);
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_AXIS_VISIBLE_X, false);
		
		Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
	}
	
	// ========================================================================
	void launchOverallRevenueTimeline() {
		Log.d(TAG, "launchOverallRevenueTimeline()");
		
		// FIXME
		long publisher_id = database.getPublisherId(this.active_publisher_name_filter);
		
		Uri histogram_uri = RevenueTotals.constructUri(this.date_range,
				BinningMode.values()[getIntent().getIntExtra(EXTRA_BINNING_MODE, BinningMode.CUSTOM.ordinal())],
				getIntent().getIntExtra(EXTRA_CUSTOM_HISTOGRAM_BINS, DEFAULT_HISTOGRAM_BINCOUNT),
				publisher_id);

		Intent i = new Intent(Intent.ACTION_VIEW, histogram_uri);
		i.putExtra(Intent.EXTRA_TITLE, "Revenue " + this.date_range.format(RevenueActivity.MINI_DATE_FORMAT));

		i.putExtra(IntentConstants.Meta.Series.EXTRA_RAINBOW_COLORS, true);
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, FUDGED_DOLLAR_AXIS_FORMAT);

		Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
	}
	
	// ========================================================================
	void launchRevenueTimeline() {
		Log.d(TAG, "launchRevenueTimeline()");
		
		// FIXME
		long publisher_id = database.getPublisherId(this.active_publisher_name_filter);
		
		Uri histogram_uri = RevenueMultiTimeline.constructUri(this.date_range,
				BinningMode.values()[getIntent().getIntExtra(EXTRA_BINNING_MODE, BinningMode.CUSTOM.ordinal())],
				getIntent().getIntExtra(EXTRA_CUSTOM_HISTOGRAM_BINS, DEFAULT_HISTOGRAM_BINCOUNT),
				publisher_id);

		Intent i = new Intent(Intent.ACTION_VIEW, histogram_uri);
		i.putExtra(Intent.EXTRA_TITLE, "Revenue " + this.date_range.format(RevenueActivity.MINI_DATE_FORMAT));
		
//		ArrayList<String> axis_titles = new ArrayList<String>();
//		axis_titles.add("Date");
//		axis_titles.add("Revenue");
//		i.putExtra(IntentConstants.EXTRA_AXIS_TITLES, axis_titles);
		i.putExtra(IntentConstants.Meta.Series.EXTRA_RAINBOW_COLORS, true);
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, FUDGED_DOLLAR_AXIS_FORMAT);
		
		Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
	}

	// ========================================================================
	void launchPlot() {

		switch (this.selected_query_type) {
		case REVENUE_TIMELINE:
			launchRevenueTimeline();
			break;
		case OVERALL_REVENUE:
			launchOverallRevenueTimeline();
			break;
		case TOTAL_APP_REVENUE:
			launchRevenueTotalsBarPlot();
			break;
		}
	}
	
	// ========================================================================
	void launchPlot(long app_id, String app_title) {

		switch (this.selected_query_type) {
		case REVENUE_PRICE_RELATION:
			launchPriceRevenueCorrelation(app_id, app_title);
			break;
		case REVENUE_RATING_RELATION:
			launchRatingsRevenueCorrelation(app_id, app_title);
			break;
		}
	}

	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
		switch (id) {
		case DIALOG_MARKET_API_ERROR:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.error_title_market_data_retrieval)
			.setMessage(R.string.error_instructions_bad_device_id)
			.setPositiveButton("View Settings", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(new Intent(ConsolidationActivity.this, MainPreferences.class));
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, null)
			.create();
		}
		case DIALOG_PROGRESS_COMMENT_SYNC:
		{
			this.progress_dialog_comments_sync = new ProgressDialog(this);
			String message = getResources().getString(R.string.comments_syncing);
			this.progress_dialog_comments_sync.setTitle(message);
			this.progress_dialog_comments_sync.setMessage(message);	// Needs to be initialized with a String to reserve space		
			this.progress_dialog_comments_sync.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			this.progress_dialog_comments_sync.setCancelable(true);

			return this.progress_dialog_comments_sync;
		}
		case DIALOG_PROGRESS_APP_SEARCH:
		{
			this.progress_dialog_app_search = new ProgressDialog(this);
			String message = getResources().getString(R.string.market_syncing_apps);
			this.progress_dialog_app_search.setMessage(message);	// Needs to be initialized with a String to reserve space		
			this.progress_dialog_app_search.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			this.progress_dialog_app_search.setCancelable(false);
			return this.progress_dialog_app_search;
		}
		case DIALOG_INSTRUCTIONS:
		{
			final CheckBox reminder_checkbox;
			View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions_swatched, null);
			reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

			((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_consolidation_editor);
			
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.instructions_title_consolidation_editor)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					settings.edit().putBoolean(PREFKEY_SHOW_CONSOLIDATION_EDITOR_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
				}
			})
			.create();
		}
		case DIALOG_PUBLISHER_SELECTION:
		{
			View tagTextEntryView = factory.inflate(R.layout.dialog_publisher_input, null);
			
			final EditText publisher_input = (EditText) tagTextEntryView.findViewById(R.id.publisher_input);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.publisher)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String publisher_name = publisher_input.getText().toString();
					startSyncPaidApps(publisher_name, app_synchronization_paid_only);
				}
			})
			.create();
		}
		case DIALOG_PUBLISHER_FILTER:
		{
			int selected_index = -1;
			final int name_column = publishers_cursor.getColumnIndex(DatabaseRevenue.KEY_PUBLISHER_NAME);
			
			if (this.active_publisher_name_filter != null && this.publishers_cursor.moveToFirst()) {
				do {
					String publisher_name = publishers_cursor.getString( name_column );
					if ( this.active_publisher_name_filter.equals( publisher_name ) ) {
						selected_index = this.publishers_cursor.getPosition();
						break;
					}
				} while (this.publishers_cursor.moveToNext());
			}
			
			return new AlertDialog.Builder(this)
			.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					// This will force onCreateDialog() to be called again
					removeDialog(DIALOG_PUBLISHER_FILTER);
				}
			})
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.publisher)
			.setSingleChoiceItems(this.publishers_cursor, selected_index, DatabaseRevenue.KEY_PUBLISHER_NAME, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					publishers_cursor.moveToPosition(which);
					String publisher_name = publishers_cursor.getString( name_column );
					Log.d(TAG, "Clicked on item: " + which + "; Publisher: " + publisher_name);
					
					active_publisher_name_filter = publisher_name;
				}
			})
			.setNeutralButton("Ignore", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					active_publisher_name_filter = null;
					removeDialog(DIALOG_PUBLISHER_FILTER);
					updateList();				
				}
			})
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					removeDialog(DIALOG_PUBLISHER_FILTER);
					updateList();
				}
			})
			.create();
		}
		case DIALOG_MARKET_APP_SELECTION:
		{
			View tagTextEntryView = factory.inflate(R.layout.dialog_app_selection, null);
			

			final Spinner app_chooser = (Spinner) tagTextEntryView.findViewById(R.id.app_chooser);
			
	        SimpleCursorAdapter sca = new SimpleCursorAdapter(
	        		this, android.R.layout.simple_spinner_item,
	        		null,
	        		new String[] {DatabaseRevenue.KEY_APP_TITLE},
	        		new int[] {android.R.id.text1});
	        sca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        sca.setCursorToStringConverter(new CursorToStringConverter() {
				@Override
				public CharSequence convertToString(Cursor cursor) {
					return cursor.getString(cursor.getColumnIndex(DatabaseRevenue.KEY_APP_TITLE));
				}
	        });
	        app_chooser.setAdapter(sca);
			

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.dialog_title_assign_market_app)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					long app_id = app_chooser.getSelectedItemId();
					if (plotting_app_selection) {
						SimpleCursorAdapter sca = (SimpleCursorAdapter) app_chooser.getAdapter();
						String app_title = (String) sca.getCursorToStringConverter().convertToString((Cursor) app_chooser.getSelectedItem());
						launchPlot(app_id, app_title);
					} else if (pending_consolidation_child != INVALID_APP_ID) {

						List<Long> checked_children = new ArrayList<Long>();
						checked_children.add(pending_consolidation_child);
						database.consolidateItems(app_id, checked_children);
						updateList();
					}
				}
			})
			.create();
		}
		}
		return null;
	}

	// ========================================================================
	public class PublisherFetcherTask extends AsyncTask<Void, Void, Cursor> {
		
		public PublisherFetcherTask(Context c) {
		}
		
		@Override
	    public void onPreExecute() {
		}
		
		@Override
		protected Cursor doInBackground(Void... voided) {
			return database.getPublisherNamesCursor();
		}
		
	    @Override
	    public void onPostExecute(Cursor cursor) {
			publishers_cursor = cursor;
			
			Log.d(TAG, "Row count: " + publishers_cursor.getCount());
			
        	showDialog(DIALOG_PUBLISHER_FILTER);
	    }
	}
	
	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {
        case DIALOG_PROGRESS_APP_SEARCH:
        {
			// Customize the message
        	((TextView) dialog.findViewById(android.R.id.message)).setText(R.string.market_syncing_apps);

	        break;
        }
        case DIALOG_PROGRESS_COMMENT_SYNC:
        {
			// Customize the message
        	((TextView) dialog.findViewById(android.R.id.message)).setText(R.string.comments_syncing);
        	dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					comment_sync_task.cancel(true);
				}
			});
	        break;
        }
		case DIALOG_MARKET_APP_SELECTION:
		{
			dialog.setTitle(this.plotting_app_selection ? R.string.dialog_title_choose_market_app : R.string.dialog_title_assign_market_app);
			
			final Spinner app_chooser = (Spinner) dialog.findViewById(R.id.app_chooser);
			
			// TODO Make free apps available here?
			Cursor new_cursor = this.database.getLinkedPaidMarketApps();
			dialog.findViewById(android.R.id.button1).setEnabled(new_cursor.getCount() > 0);
			((CursorAdapter) app_chooser.getAdapter()).changeCursor(new_cursor);
			break;
		}
		case DIALOG_PUBLISHER_SELECTION:
		{
			final EditText publisher_input = (EditText) dialog.findViewById(R.id.publisher_input);
			String pub = this.settings.getString(AppsOverviewActivity.PREFKEY_SAVED_PUBLISHER_NAME, Market.MARKET_AUTHOR_NAME);
			publisher_input.setText(pub);
			break;
		}		
		default:
			break;
		}
	}
	
	// ========================================================================
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		
		((AppExpandableListAdapter) getExpandableListAdapter()).toggleCheckmark(id);
		refreshForCheckmark();
		return true;
	}

	// ========================================================================
	void refreshForCheckmark() {

		// Remember the open groups
		ExpandableListView elv = getExpandableListView();
		List<Integer> expanded_group_positions = new ArrayList<Integer>();
		// Iterate through group positions
		for (int flat_position=0; flat_position<elv.getCount(); flat_position++) {
			long packedPosition = elv.getExpandableListPosition(flat_position);
			if (ExpandableListView.PACKED_POSITION_TYPE_GROUP == ExpandableListView.getPackedPositionType(packedPosition)) {
				int group_position = ExpandableListView.getPackedPositionGroup(packedPosition);
				if (elv.isGroupExpanded(group_position))
					expanded_group_positions.add(group_position);
			}
		}
		
		onContentChanged();	// This has the side-effect of closing all groups.
		
		// Re-expand the previously open groups
		for (int group_position : expanded_group_positions)
			elv.expandGroup(group_position);
	}

	// ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_consolidation_editor, menu);
        return true;
    }
    
	// ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	boolean temp = super.onPrepareOptionsMenu(menu);
    	menu.findItem(R.id.menu_sync_comments).setVisible(this.database.countLinkedPaidMarketApps() > 0);
    	
//    	int ungrouped_depoloyments = this.database.countUngroupedAppDeployments();
//    	Log.d(TAG, "Ungrouped deployments: " + ungrouped_depoloyments);
    	
    	boolean has_ungrouped_deploymenrs = this.database.hasUngroupedAppDeployments();
    	Log.d(TAG, "Has ungrouped deployments: " + has_ungrouped_deploymenrs);
    	
    	int available_paid_apps = this.database.countLinkedPaidMarketApps();
//    	Log.d(TAG, "Available paid apps: " + available_paid_apps);

    	boolean has_available_paid_apps = this.database.hasLinkedPaidMarketApps();
    	Log.d(TAG, "Has available paid apps: " + has_available_paid_apps);
    	
//    	menu.findItem(R.id.menu_autogroup).setVisible(ungrouped_depoloyments > 0 && available_paid_apps > 0);
    	menu.findItem(R.id.menu_autogroup).setVisible(has_ungrouped_deploymenrs && has_available_paid_apps);
    	
    	
    	return temp;
    }
    
	// ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_autogroup:
        {
        	this.database.autoGroupMerchantItems();
        	updateList();
        	Toast.makeText(this, "Autogroup complete.", Toast.LENGTH_SHORT).show();
            return true;
        }
        case R.id.menu_publishers:
        {
			new PublisherFetcherTask(this).execute();
            return true;
        }
        case R.id.menu_instructions:
        {
        	showDialog(DIALOG_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_sync_all_apps:
        {
			app_synchronization_paid_only = false;
			showDialog(DIALOG_PUBLISHER_SELECTION);
            return true;
        }
        case R.id.menu_sync_comments:
        {
			List<Long> app_ids = database.getPublishedAppIds();
			this.comment_sync_task = new CommentSyncTaskExtended();
			this.comment_sync_task.execute(app_ids.toArray(new Long[0]));
            return true;
        }
		case R.id.menu_preferences:
		{
			startActivity(new Intent(this, MainPreferences.class));
			return true;
		}
		case R.id.menu_dump_consolidation_details:
		{
			Debug d = database.new Debug();
			d.dumpConsolidatedItemsView();
			return true;
		}
		case R.id.menu_dump_market_apps:
		{
			Debug d = database.new Debug();
			d.dumpMarketAppsTable();
			return true;
		}
		case R.id.menu_dump_unique_raw_merchant_item_ids:
		{
			Debug d = database.new Debug();
			d.dumpUniqueRawMerchantItemIds();
			return true;
		}
		case R.id.menu_dump_parents_children_joined:
		{
			Debug d = database.new Debug();
			d.dumpParentsAndChildrenJoined();
			return true;
		}
		case R.id.menu_dump_parents_children_raw:
		{
			Debug d = database.new Debug();
			d.dumpParentsAndChildrenRaw();
			return true;
		}
		case R.id.menu_dump_checkout_items:
		{
			Debug d = database.new Debug();
			d.dumpCheckoutItemsTable();
			return true;
		}
        }

        return super.onOptionsItemSelected(item);
    }
	
	// ========================================================================
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		MenuInflater inflater = getMenuInflater();

		List<Long> checked_items = ((AppExpandableListAdapter) getExpandableListAdapter()).getCheckedIds();
		boolean has_checkmarks = checked_items.size() > 0;
		
		int type = ExpandableListView.getPackedPositionType( ((ExpandableListContextMenuInfo)  menuInfo).packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

			inflater.inflate(R.menu.context_consolidator_child, menu);
			menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
			menu.setHeaderTitle(R.string.menu_context_child_title);
			
			long child_id = ((ExpandableListContextMenuInfo) menuInfo).id;
			int groupPos = ExpandableListView.getPackedPositionGroup(((ExpandableListContextMenuInfo) menuInfo).packedPosition);
			long group_id = getExpandableListAdapter().getGroupId(groupPos);

			boolean natural_match = child_id == group_id;
			menu.findItem(R.id.menu_ungroup_item).setVisible(group_id != 0 && !natural_match);
			menu.findItem(R.id.menu_set_market_app).setVisible(!natural_match);
			
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {

			long selected_id = ((ExpandableListContextMenuInfo) menuInfo).id;
			if (selected_id == 0) return;

			inflater.inflate(R.menu.context_consolidator_parent, menu);
			menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
			menu.setHeaderTitle(R.string.menu_context_parent_title);
			
			menu.findItem(R.id.menu_group_items_parent).setVisible(
					has_checkmarks &&
					!(checked_items.size() == 1 && checked_items.contains(selected_id))	// I'm the only one checked
			);

			menu.findItem(R.id.menu_plot_single_app).setVisible(
					RevenueQueryOption.REVENUE_RATING_RELATION.equals(this.selected_query_type)
					|| RevenueQueryOption.REVENUE_PRICE_RELATION.equals(this.selected_query_type)		
			);
		}
	}

	// ========================================================================
    public int consolidateCheckedItemsExpandable(long parent_id) {
    	List<Long> checked_children = ((AppExpandableListAdapter) getExpandableListAdapter()).getCheckedIds();
		return this.database.consolidateItems(parent_id, checked_children);
    }	
	
	// ========================================================================
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

		int groupPos = 0, childPos = 0;

		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
			childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

//			String msg = "Child " + childPos + " clicked in group " + groupPos;
//			Log.d(TAG, msg);

		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 

//			String msg = "Group " + groupPos + " clicked";
//			Log.d(TAG, msg);
		}

		
		switch (item.getItemId()) {
		case R.id.menu_ungroup_item:	// child
		{
			long child_id = getExpandableListAdapter().getChildId(groupPos, childPos);
			this.database.unConsolidateItem(child_id);
			updateList();
			return true;
		}
		case R.id.menu_set_market_app:	// child
		{
			this.plotting_app_selection = false;
			this.pending_consolidation_child = getExpandableListAdapter().getChildId(groupPos, childPos);
			showDialog(DIALOG_MARKET_APP_SELECTION);
			return true;
		}
		case R.id.menu_plot_single_app:	// parent
		{
			launchPlot(info.id, this.database.getAppTitle(info.id));
			return true;
		}
		case R.id.menu_view_on_market:	// parent
		{
			String package_name = this.database.getMarketAppPackageName(info.id);
			Market.launchMarketAppDetails(this, package_name);
			return true;
		}
		case R.id.menu_update_ratings:	// parent
		{
			this.comment_sync_task = new CommentSyncTaskExtended();
			this.comment_sync_task.execute(info.id);
			return true;
		}
		case R.id.menu_group_items_parent:
		{
			Cursor cursor = (Cursor) getExpandableListAdapter().getGroup(groupPos);
			String parent_name = cursor.getString(cursor.getColumnIndex(DatabaseRevenue.KEY_APP_TITLE));
			
			Log.d(TAG, "Starting process");
			long group_id = getExpandableListAdapter().getGroupId(groupPos);
			int update_count = consolidateCheckedItemsExpandable(group_id);
			Log.e(TAG, "Finished process");
			
			updateList();
			
			String count = getResources().getQuantityString(R.plurals.item_count, update_count, update_count);
			Toast.makeText(this, "Consolidated " + count + " under \"" + parent_name + "\"", Toast.LENGTH_LONG).show();
			return true;
		}
		}
		
//		Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPos, childPos);
//		item.getItemId();
//		info.id;
//		getExpandableListAdapter().getGroupId(groupPos);
		
		return true;
	}

	// ========================================================================
	void startSyncPaidApps(String pub, boolean paid_only) {

		this.settings.edit().putString(AppsOverviewActivity.PREFKEY_SAVED_PUBLISHER_NAME, pub).commit();
		
		this.apps_search_task = new AppSearchTaskExtended(pub, paid_only ? ViewType.PAID : ViewType.ALL);
		this.apps_search_task.execute(); 
	}
	
	// ========================================================================
	class AppSearchTaskExtended extends AppSyncTask {

		public AppSearchTaskExtended(String publisher, ViewType view_type) {
			super(new DatabaseRevenue(getBaseContext()), publisher, view_type);
			updateActivity(ConsolidationActivity.this);
		}

		// ====================================================================
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			incSemaphore();
			
			this.host.getActivity().showDialog(DIALOG_PROGRESS_APP_SEARCH);
		}

		// ====================================================================
		@Override
		protected void cleanUp() {
			super.cleanUp();

			decSemaphore();
			this.host.getActivity().dismissDialog(DIALOG_PROGRESS_APP_SEARCH);
		}
		
		// ====================================================================
		@Override
		protected void completeTask(List<App> apps) {
			super.completeTask(apps);
			
			try {
				database.autoGroupMerchantItems();
			} catch (SQLiteDatabaseCorruptException e) {
				error_toast.setText(e.getMessage());
				error_toast.show();
			}

			((ConsolidationActivity) this.host.getActivity()).updateList();
		}
		
		// ========================================================================
		protected void failTask(String non_null_error_message) {

			if (non_null_error_message.contains("Unknown format")) {
				Log.e(TAG, "Retrieval error message: " + non_null_error_message);
				this.host.getActivity().showDialog(DIALOG_MARKET_API_ERROR);
			} else {
				super.failTask(non_null_error_message);
			}
		}
	}

	// ========================================================================
	class CommentSyncTaskExtended extends CommentSyncTask {
		
		public CommentSyncTaskExtended() {
			super(
					new DatabaseRevenue(getBaseContext()),
					PreferenceManager.getDefaultSharedPreferences(getBaseContext()));
			updateActivity(ConsolidationActivity.this);
		}

		// ====================================================================
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			incSemaphore();

			host.getActivity().showDialog(DIALOG_PROGRESS_COMMENT_SYNC);
		}

		// ====================================================================
		@Override
	    protected void onProgressUpdate(CommentsProgressPacket... packets) {
			CommentsProgressPacket packet = packets[0];
			ProgressDialog progress_dialog = host.getProgressDialog(DIALOG_PROGRESS_COMMENT_SYNC);
			if (packet.max_steps != 0) {
				String app_iterator = getResources().getString(R.string.app_iterator, packet.current_step, packet.max_steps);
				progress_dialog.setMessage(app_iterator);
			} else {
				progress_dialog.setProgress(packet.progress_value);
				progress_dialog.setMax(packet.max_value);
			}
		}

		// ====================================================================
		@Override
		protected void completeTask() {
			((ConsolidationActivity) host.getActivity()).updateList();
		}

		// ====================================================================
		@Override
		protected void cleanUp() {
			host.getActivity().dismissDialog(DIALOG_PROGRESS_COMMENT_SYNC);
			decSemaphore();
		}
	}
	
	// ========================================================================
	void updateList() {
		((AppExpandableListAdapter) getExpandableListAdapter()).clearCheckmarks();
		new CursorUpdateTask().execute();
	}

	// ========================================================================
	public class CursorUpdateTask extends AsyncTask<Void, Void, Cursor> {
		
		// ========================================================================
		@Override
		protected void onPreExecute() {
			incSemaphore();

			TextView empty_text = (TextView) findViewById(R.id.empty_text_consolidation);
			View progress_throbber = findViewById(R.id.empty_list_progress);
			
			empty_text.setText(R.string.empty_building_apps_list);
			progress_throbber.setVisibility(View.VISIBLE);
		}
		
		// ========================================================================
		@Override
		protected Cursor doInBackground(Void... voided) {

			// FIXME
			long publisher_id = database.getPublisherId(active_publisher_name_filter);
			return database.getMarketAppsAndUnparentedRow(publisher_id);
		}

		// ========================================================================
		@Override
		public void onPostExecute(Cursor cursor) {
			
			decSemaphore();

			// Total app statistics for summary.
			if (cursor != null && cursor.moveToFirst()) {
				TextView summary_textview = (TextView) findViewById(R.id.revenue_summary_textview);
				
				int sale_count = 0;
				int app_count = 0;
				int versions_count = 0;
				int revenue_total = 0;
				int ratings_count = 0;
				
				do {
					app_count++;
					versions_count += cursor.getInt(cursor.getColumnIndex(DatabaseRevenue.KEY_CONSOLIDATED_APP_COUNT));
					revenue_total += cursor.getInt(cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_REVENUE));
					sale_count += cursor.getInt(cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_SALE_COUNT));
					ratings_count += cursor.getInt(cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_RATING_COUNT));
//					ratings_count += cursor.getInt(cursor.getColumnIndex(DatabaseRevenue.KEY_APP_RATING_COUNT));

					
				} while (cursor.moveToNext());

				Resources resources = ConsolidationActivity.this.getResources();

				String dollars = String.format("$%01.2f", revenue_total / 100f);
				Log.d(TAG, dollars);
				
				String pluralized_sale_count = resources.getQuantityString(R.plurals.sale_count, sale_count, sale_count);
				Log.d(TAG, pluralized_sale_count);
				
				String pluralized_version_count = resources.getQuantityString(R.plurals.version_count, versions_count, versions_count);
				Log.d(TAG, pluralized_version_count);

				String pluralized_app_count = resources.getQuantityString(R.plurals.app_count, app_count, app_count);
				Log.d(TAG, pluralized_app_count);
				
				String html = "Total revenue: <b>" + dollars + "</b> across <b>" + pluralized_sale_count + "</b> from <b>" + pluralized_app_count + "</b> and <b>" + pluralized_version_count + "</b>";
				summary_textview.setText( Html.fromHtml(html) );
			}
			

			TextView empty_text = (TextView) findViewById(R.id.empty_text_consolidation);
			View progress_throbber = findViewById(R.id.empty_list_progress);
			
			empty_text.setText(R.string.empty_apps_list);
			progress_throbber.setVisibility(View.GONE);
			
//	        startManagingCursor(cursor);	// XXX This has caused a crash, I think.
			((CursorTreeAdapter) getExpandableListAdapter()).changeCursor(cursor);

			// Maybe we must do this *after* changeCursor is applied?
			button_update_ratings.setVisibility(
					database.countLinkedPaidMarketApps() > 0
					? View.VISIBLE : View.GONE);
		}
	}
	
	// ========================================================================
	@Override
	public void incSemaphore() {

		setProgressBarIndeterminateVisibility(true);
		this.retrieval_tasks_semaphore.incrementAndGet();
	}

	// ========================================================================
	@Override
	public void decSemaphore() {

		boolean still_going = this.retrieval_tasks_semaphore.decrementAndGet() > 0;
		setProgressBarIndeterminateVisibility(still_going);
	}

	// ========================================================================
	@Override
	public void showError(String error) {
		error_toast.setText(error);
		error_toast.show();
	}

	// ========================================================================
	@Override
	public Activity getActivity() {
		return this;
	}

	// ========================================================================
	@Override
	public ProgressDialog getProgressDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS_APP_SEARCH:
		{
			return this.progress_dialog_app_search;
		}
		case DIALOG_PROGRESS_COMMENT_SYNC:
		{
			return this.progress_dialog_comments_sync;
		}
		}
		
		return null;
	}
}