package com.kostmo.market.revenue.activity;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.gc.android.market.api.model.Market.App;
import com.gc.android.market.api.model.Market.AppsRequest.ViewType;
import com.googlecode.chartdroid.core.IntentConstants;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.adapter.ListAdapterPublisherSuggestions;
import com.kostmo.market.revenue.adapter.RatedAppListAdapter;
import com.kostmo.market.revenue.container.CommentsProgressPacket;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.plotmodes.CommentsHistogram;
import com.kostmo.market.revenue.provider.plotmodes.CommentsPlot;
import com.kostmo.market.revenue.service.BootReceiver;
import com.kostmo.market.revenue.service.CheckUpdateService;
import com.kostmo.market.revenue.task.AppSyncTask;
import com.kostmo.market.revenue.task.CommentSyncTask;
import com.kostmo.market.revenue.task.ProgressHostActivity;
import com.kostmo.tools.SemaphoreHost;

public class AppsOverviewActivity extends ListActivity implements SemaphoreHost, OnClickListener, ProgressHostActivity {

	static final String TAG = "AppsOverviewActivity";
	
	public static final String PREFKEY_SHOW_APPS_OVERVIEW_INSTRUCTIONS = "PREFKEY_SHOW_APPS_OVERVIEW_INSTRUCTIONS";

	
	public static final String PREFKEY_SAVED_PUBLISHER_NAME = "PREFKEY_SAVED_PUBLISHER_NAME";
	
	public static final int DEFAULT_RATING_ALERT_THRESHOLD = 3;
	public static final int DEFAULT_WINDOW_WIDTH = 3;
	public static final int INVALID_RATING = 0;
	
	private static final int DIALOG_INSTRUCTIONS = 1;
	private static final int DIALOG_SET_RATING_THRESHOLD = 2;
	private static final int DIALOG_COMMENT_WINDOW_LENGTH = 3;
	private static final int DIALOG_PROGRESS_APP_SYNC = 4;
	private static final int DIALOG_PROGRESS_COMMENT_SYNC = 5;
	

	Toast error_toast;
	DatabaseRevenue database;
	SharedPreferences settings;
	AppSearchTaskExtended apps_sync_task;
	CommentSyncTaskExtended comment_sync_task;
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	AutoCompleteTextView publisher_name;
	ProgressDialog progress_dialog_app_search, progress_dialog_comments_sync;
	
	public enum WindowEvaluatorMode {
		AVERAGE("Average", "Avg"), MINIMUM("Minimum", "Min");
		
		public String name, shortname;
		WindowEvaluatorMode(String name, String shortname) {
			this.name = name;
			this.shortname = shortname;
		}
	}

	long chosen_app_id = RevenueActivity.INVALID_MERCHANT_ID;
	ViewType app_filter_mode = ViewType.ALL;
    boolean setting_all_thresholds = false;
	
    
    void updateSyncEnabled(AutoCompleteTextView textview) {
    	// FIXME - This is not being updated on every keystroke;
    	// instead, it is only updated when DONE is pressed on the OSK.
//		findViewById(R.id.button_sync_apps).setEnabled(textview.getText().length() > 0);
    }
    
	// ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.list_activity_apps_overview);
		
		// TODO
//		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);


		this.error_toast = Toast.makeText(this, "Error", Toast.LENGTH_LONG);
		this.database = new DatabaseRevenue(this);
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);

		
		this.publisher_name = (AutoCompleteTextView) findViewById(R.id.publisher_name);
		this.publisher_name.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				AutoCompleteTextView textview = (AutoCompleteTextView) v;
				updateSyncEnabled(textview);
				return false;
			}
		});
		
        ListAdapterPublisherSuggestions autocomplete_adapter = new ListAdapterPublisherSuggestions(
        		this,
        		android.R.layout.simple_dropdown_item_1line,
        		null,
        		new String[] {DatabaseRevenue.KEY_PUBLISHER_NAME},
        		new int[] {android.R.id.text1});
        
        autocomplete_adapter.setStringConversionColumn(1);
        
        /*
        this.publisher_name.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {


				return true;
			}
        });
        */
        this.publisher_name.setAdapter(autocomplete_adapter);

		
		String saved_publisher_name = this.settings.getString(PREFKEY_SAVED_PUBLISHER_NAME, null);
		if (saved_publisher_name != null)
			this.publisher_name.setText(saved_publisher_name);

		
		int[] button_ids = new int[] {
				R.id.button_sync_apps,
				R.id.button_sync_comments
		};
		
		for (int button_id : button_ids)
			findViewById(button_id).setOnClickListener(this);


		RatedAppListAdapter adapter = new RatedAppListAdapter(this, R.layout.list_item_app_threshold, null);
		setListAdapter(adapter);

		registerForContextMenu(getListView());

		
		final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
		if (a != null) {
			adapter.changeCursor(a.cursor);
			adapter.setCachedImageMap(a.image_cache);
			this.setting_all_thresholds = a.setting_all_thresholds;
			
			this.chosen_app_id = a.chosen_app_id;
			this.app_filter_mode = a.app_filter_mode;	// TODO Set menu selected radio item
			
			this.apps_sync_task = a.apps_search_task;
			if (this.apps_sync_task != null)
				this.apps_sync_task.updateActivity(this);

			this.comment_sync_task = a.comment_sync_task;
			if (this.comment_sync_task != null)
				this.comment_sync_task.updateActivity(this);
			
		} else {
			if (!this.settings.getBoolean(PREFKEY_SHOW_APPS_OVERVIEW_INSTRUCTIONS, false)) {
				showDialog(DIALOG_INSTRUCTIONS);
			}
		}
		

		updateSyncEnabled(this.publisher_name);
	}
	
	// ========================================================================
	@Override
	protected void onResume() {
		super.onResume();
		
		updateList();
	}

	// ========================================================================
	/*
	 * This should not be used in response to changed settings, since there
	 * is that extra check
	 */
	@Deprecated
	public static void setNotificationsAlarm(Context context) {
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.getBoolean(BootReceiver.PREFKEY_CHECKIN_ALARM_SCHEDULED, false))
        	CheckUpdateService.schedule(context);
	}
	
	// ========================================================================
	class AppSearchTaskExtended extends AppSyncTask {

		public AppSearchTaskExtended(String publisher, ViewType view_type) {
			super(new DatabaseRevenue(getBaseContext()), publisher, view_type);
			updateActivity(AppsOverviewActivity.this);
		}

		// ====================================================================
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			incSemaphore();
			
			this.host.getActivity().showDialog(DIALOG_PROGRESS_APP_SYNC);
		}

		// ====================================================================
		@Override
		protected void cleanUp() {
			super.cleanUp();

			decSemaphore();
			this.host.getActivity().dismissDialog(DIALOG_PROGRESS_APP_SYNC);
		}
		
		// ====================================================================
		@Override
		protected void completeTask(List<App> apps) {
			super.completeTask(apps);
			((AppsOverviewActivity) this.host.getActivity()).updateList();
		}
	}
	
	// ========================================================================
	class CommentSyncTaskExtended extends CommentSyncTask {
		
		public CommentSyncTaskExtended() {
			super(
					new DatabaseRevenue(getBaseContext()),
					PreferenceManager.getDefaultSharedPreferences(getBaseContext()));
			updateActivity(AppsOverviewActivity.this);
		}

		// ====================================================================
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			incSemaphore();
			
			this.host.getActivity().showDialog(DIALOG_PROGRESS_COMMENT_SYNC);
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
			((AppsOverviewActivity) this.host.getActivity()).updateList();
		}

		// ====================================================================
		@Override
		protected void cleanUp() {
			this.host.getActivity().dismissDialog(DIALOG_PROGRESS_COMMENT_SYNC);
			decSemaphore();
		}
	}

	// ========================================================================
	void plotComments(int width, WindowEvaluatorMode evaluator_mode) {
		
		Uri uri = CommentsPlot.constructUri(this.chosen_app_id, width, evaluator_mode);
		this.chosen_app_id = RevenueActivity.INVALID_MERCHANT_ID;
		
		Log.d(TAG, "plotComments()");
		Log.d(TAG, "Uri is: " + uri);
		
		Intent i = new Intent(Intent.ACTION_VIEW, uri);
		i.putExtra(Intent.EXTRA_TITLE, "App ratings");
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, "%.0f");
		Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
	}
	
	// ========================================================================
	void launchRatingHistogram(long app_id, String app_title) {
		
		Log.d(TAG, "launchRatingHistogram()");
		Uri uri = CommentsHistogram.constructUri(app_id);
		
		Intent i = new Intent(Intent.ACTION_VIEW, uri);
		// XXX Explicitly launches the Bar chart
		i.setClassName("com.googlecode.chartdroid", "org.achartengine.activity.BarChartActivity");
		
		i.putExtra(Intent.EXTRA_TITLE, "Rated Comments Histogram");
		
		ArrayList<String> axis_titles = new ArrayList<String>();
		axis_titles.add("Rating");
		axis_titles.add("Count");
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_AXIS_TITLES, axis_titles);
		i.putExtra(IntentConstants.Meta.Axes.EXTRA_FORMAT_STRING_Y, "%.0f");

		i.putExtra(IntentConstants.Meta.Series.EXTRA_SERIES_LABELS, new String[] {app_title});
		
		Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_DETAILS_STRING, i, Market.NO_RESULT);
	}
	
	// ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
		switch (id) {
		// Note: These dialogs are differentiated in onPrepareDialog()
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
		case DIALOG_PROGRESS_APP_SYNC:
		{
			this.progress_dialog_app_search = new ProgressDialog(this);
			String message = getResources().getString(R.string.market_syncing_apps);
			this.progress_dialog_app_search.setMessage(message);	// Needs to be initialized with a String to reserve space		
			this.progress_dialog_app_search.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			this.progress_dialog_app_search.setCancelable(false);
			return this.progress_dialog_app_search;
		}
		case DIALOG_COMMENT_WINDOW_LENGTH:
		{
			View tagTextEntryView = factory.inflate(R.layout.dialog_comment_window_width, null);
			final EditText width_textbox = (EditText) tagTextEntryView.findViewById(R.id.width_textbox);
			final Spinner window_evaluator_mode = (Spinner) tagTextEntryView.findViewById(R.id.window_evaluator_mode);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.dialog_comment_window_length_title)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String window_width_text = width_textbox.getText().toString();
					int window_width = window_width_text.length() > 0 ? Integer.parseInt(window_width_text) : DEFAULT_WINDOW_WIDTH;
					Log.e(TAG, "Width: " + window_width);
					plotComments(window_width, WindowEvaluatorMode.values()[window_evaluator_mode.getSelectedItemPosition()]);
				}
			})
			.create();
		}
		case DIALOG_SET_RATING_THRESHOLD:
		{
			View tagTextEntryView = factory.inflate(R.layout.dialog_rating, null);
			final RatingBar rating_bar = (RatingBar) tagTextEntryView.findViewById(R.id.rating_threshold);
			
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.dialog_rating_alerts_title)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					int update_count = 0;
					if (setting_all_thresholds) {
						update_count = database.setAllRatingAlertThresholds((int) rating_bar.getRating(), app_filter_mode);
					} else {
						update_count = database.setRatingAlertThreshold(chosen_app_id, (int) rating_bar.getRating());
					}
					Toast.makeText(AppsOverviewActivity.this, "Updated " + update_count + " threshold.", Toast.LENGTH_SHORT);
					updateList();
				}
			})
			.create();
		}
		case DIALOG_INSTRUCTIONS:
		{
			View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions_swatched, null);
			final CheckBox reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

			((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_comment_alerts);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.instructions_comment_alerts_title)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					settings.edit().putBoolean(PREFKEY_SHOW_APPS_OVERVIEW_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
				}
			})
			.create();
		}
		}

		return null;
	}

	// ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
        case DIALOG_PROGRESS_APP_SYNC:
        {
			// Customize the message
        	((TextView) dialog.findViewById(android.R.id.message)).setText(R.string.market_syncing_apps);
        	dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					apps_sync_task.cancel(true);
				}
			});
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
        case DIALOG_SET_RATING_THRESHOLD:
        {
			final RatingBar rating_bar = (RatingBar) dialog.findViewById(R.id.rating_threshold);
			int previous_threshold = DEFAULT_RATING_ALERT_THRESHOLD;

			TextView app_text = (TextView) dialog.findViewById(R.id.rating_threshold_app_text);
			if (this.setting_all_thresholds) {
				app_text.setText( "For " + this.app_filter_mode.name() + " apps");
			} else {
				this.database.getRatingAlertThreshold(this.chosen_app_id);
				app_text.setText( this.database.getAppTitle(this.chosen_app_id) );
				previous_threshold = this.database.getRatingAlertThreshold(this.chosen_app_id);
			}

			rating_bar.setRating(previous_threshold);

	        break;
        }
        default:
        	break;
        }
    }
    
	// ========================================================================
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		this.chosen_app_id = id;
		this.setting_all_thresholds = false;
		showDialog(DIALOG_SET_RATING_THRESHOLD);
	}

	// ========================================================================
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_market_apps, menu);
		
		menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
		menu.setHeaderTitle(R.string.menu_context_applist_action_title);
	}

	// ========================================================================
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.menu_plot_windowed_comments:
		{
			this.chosen_app_id = info.id;
			showDialog(DIALOG_COMMENT_WINDOW_LENGTH);
			return true;
		}
		case R.id.menu_plot_rating_histogram:
		{
			CursorAdapter cursor_adapter = (CursorAdapter) getListAdapter();
			Cursor cursor = (Cursor) cursor_adapter.getItem(info.position);
			String app_title = cursor.getString(cursor.getColumnIndex(DatabaseRevenue.KEY_APP_TITLE));
			launchRatingHistogram(info.id, app_title);
			return true;
		}
		case R.id.menu_context_app_rating_alert_threshold:
		{
			this.chosen_app_id = info.id;
			this.setting_all_thresholds = false;
			showDialog(DIALOG_SET_RATING_THRESHOLD);

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
		}

		return true;
	}
	
	// ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_app_list, menu);
        
        // Default
        menu.findItem(R.id.menu_filter_all).setChecked(true);
        
        return true;
    }
    
    // ========================================================
    void handle_radio_selection(MenuItem item) {

    	item.setChecked(true);
    	
    	updateList();

    	if (false) {
	    	MenuItem base = this.stashed_options_menu.findItem(R.id.menu_app_filter);
	    	base.setTitle(item.getTitle());
	    	base.setIcon(item.getIcon());
	    	base.setTitleCondensed(item.getTitleCondensed());
    	}
    }
    
    // ========================================================
    Menu stashed_options_menu;
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);

    	this.stashed_options_menu = menu;
		return true;
    }

    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_filter_all:
        case R.id.menu_filter_free:
        case R.id.menu_filter_paid:
        {
        	switch (item.getItemId()) {
            case R.id.menu_filter_all:
            	this.app_filter_mode = ViewType.ALL;
            	break;
            case R.id.menu_filter_free:
            	this.app_filter_mode = ViewType.FREE;
            	break;
            case R.id.menu_filter_paid:
            	this.app_filter_mode = ViewType.PAID;
            	break;
        	}
        	
        	handle_radio_selection(item);
            return true;
        }
        case R.id.menu_set_all_thresholds:
        {
    		this.setting_all_thresholds = true;
    		showDialog(DIALOG_SET_RATING_THRESHOLD);
    		return true;
        }
		case R.id.menu_unread_comments:
		{
			startActivity(new Intent(this, NewCommentsActivity.class));
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
	class StateRetainer {
		Cursor cursor;
		Map<Long, SoftReference<Bitmap>> image_cache;
		boolean setting_all_thresholds;
		long chosen_app_id;
		ViewType app_filter_mode;
		AppSearchTaskExtended apps_search_task;
		CommentSyncTaskExtended comment_sync_task;
	}

	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {
		StateRetainer state = new StateRetainer();
		state.cursor = ((CursorAdapter) getListAdapter()).getCursor();
		state.image_cache = ((RatedAppListAdapter) getListAdapter()).getCachedImageMap();
		state.setting_all_thresholds = this.setting_all_thresholds;
		state.chosen_app_id = this.chosen_app_id;
		state.app_filter_mode = this.app_filter_mode;
		state.apps_search_task = this.apps_sync_task;
		state.comment_sync_task = this.comment_sync_task;
		return state;
	}

	// ========================================================================
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_sync_apps:
		{
			String pub = publisher_name.getText().toString();
			this.settings.edit().putString(PREFKEY_SAVED_PUBLISHER_NAME, pub).commit();
			
			this.apps_sync_task = new AppSearchTaskExtended(pub, this.app_filter_mode);
			this.apps_sync_task.execute();
			
			break;
		}	
		case R.id.button_sync_comments:
		{
			// Get list of unique app IDs:
//			List<Long> app_ids = database.getRawCheckoutItemIds();
			List<Long> app_ids = this.database.getPublishedAppIds();
			
			this.comment_sync_task = new CommentSyncTaskExtended();
			this.comment_sync_task.execute(app_ids.toArray(new Long[0]));

			break;
		}
		}
	}

	// ========================================================================
	void updateList() {
		Cursor cursor = this.database.getCachedLatestMarketApps(this.app_filter_mode);
		startManagingCursor(cursor);
		
		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		adapter.changeCursor(cursor);

		// Maybe we must do this *after* changeCursor is applied?
		findViewById(R.id.button_sync_comments).setVisibility(
				cursor.getCount() > 0
				? View.VISIBLE : View.GONE);
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
		case DIALOG_PROGRESS_APP_SYNC:
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
