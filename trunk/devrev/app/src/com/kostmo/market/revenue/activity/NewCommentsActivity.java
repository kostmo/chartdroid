package com.kostmo.market.revenue.activity;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.CheckBox;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.adapter.CommentsExpandableListAdapter;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.tools.SemaphoreHost;


public class NewCommentsActivity extends ExpandableListActivity implements SemaphoreHost {

	static final String TAG = "NewCommentsActivity";

	/**
	 * Timestamp (milliseconds) to check for comments newer than.
	 */
	public static final String EXTRA_CUTOFF_DATE = "EXTRA_CUTOFF_DATE";


	private static final int DIALOG_INSTRUCTIONS = 1;
	private static final int DIALOG_FULL_COMMENT = 2;
	
	public static final String PREFKEY_SHOW_NEW_COMMENTS_INSTRUCTIONS = "PREFKEY_SHOW_NEW_COMMENTS_INSTRUCTIONS";
	
	DatabaseRevenue database;
	SharedPreferences settings;
	Toast error_toast;
	Date comments_since;
	String pending_dialog_message;

	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	// ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.list_activity_new_comments);
		
		// TODO
//		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

		TextView list_header = (TextView) this.findViewById(R.id.list_header);
		if (getIntent().hasExtra(EXTRA_CUTOFF_DATE)) {
			this.comments_since = new Date(getIntent().getLongExtra(EXTRA_CUTOFF_DATE, 0));
			list_header.setText( "Comments since " + RevenueActivity.HUMAN_DATE_FORMAT.format(this.comments_since));
		}
		
		this.error_toast = Toast.makeText(this, "Error", Toast.LENGTH_LONG);
		this.database = new DatabaseRevenue(this);
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);

		this.findViewById(R.id.button_mark_all_read).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int update_count = database.markAllCommentsAsRead(true);
				Log.e(TAG, "Marked " + update_count + " comments as read.");
				updateList();
			}
		});

		CommentsExpandableListAdapter adapter = new CommentsExpandableListAdapter(
				this,
				null,
				R.layout.list_item_app_comments_summary,
				R.layout.list_item_app_comment_child,
				this.database);

		setListAdapter(adapter);
		registerForContextMenu(getExpandableListView());

		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.pending_dialog_message = state.pending_dialog_message;

			adapter.setCachedImageMap(state.image_cache);
		}
		
		
		if (savedInstanceState == null) {
			if (!this.settings.getBoolean(PREFKEY_SHOW_NEW_COMMENTS_INSTRUCTIONS, false)) {
				showDialog(DIALOG_INSTRUCTIONS);
			}
		}
	}
	
	// ========================================================================
	class StateObject {
		String pending_dialog_message;
		Map<Long, SoftReference<Bitmap>> image_cache;
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {
		StateObject state = new StateObject();
		state.pending_dialog_message = this.pending_dialog_message;
		state.image_cache = ((CommentsExpandableListAdapter) getExpandableListAdapter()).getCachedImageMap();
		return state;
	}

	// ========================================================================
	@Override
	protected void onResume() {
		super.onResume();
		
		updateList();
	}

	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);

		switch (id) {
		case DIALOG_INSTRUCTIONS:
		{
			final CheckBox reminder_checkbox;
			View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
			reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

			((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_new_comments);
			
			return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.instructions_title_new_comments)
				.setView(tagTextEntryView)
				.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						settings.edit().putBoolean(PREFKEY_SHOW_NEW_COMMENTS_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
					}
				})
				.create();
		}
		case DIALOG_FULL_COMMENT:
		{
			return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle("Full comment")
				.setMessage("...")
				.setPositiveButton(R.string.alert_dialog_ok, null)
				.create();
		}
		}
		return null;
	}

	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {
		case DIALOG_FULL_COMMENT:
		{
			TextView tv = (TextView) dialog.findViewById(android.R.id.message);
			tv.setText( this.pending_dialog_message );
		}
		default:
			break;
		}
	}

	// ========================================================================
	/*
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		
		// Longpress can do this:
		int modified_count = this.database.markCommentAsRead(id);
		Log.e(TAG, "Modified " + modified_count + " comments.");
		updateList();

		return true;
	}
	*/

	// ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_new_comments, menu);
        return true;
    }
    
	// ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
		case R.id.menu_mark_all_unread:
		{
			int update_count = database.markAllCommentsAsRead(false);
			Log.e(TAG, "Marked " + update_count + " comments as read.");
			updateList();
			return true;
		}
		case R.id.menu_view_thresholds:
		{
			startActivity(new Intent(this, AppsOverviewActivity.class));
			finish();
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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		MenuInflater inflater = getMenuInflater();
		
		int type = ExpandableListView.getPackedPositionType( ((ExpandableListContextMenuInfo)  menuInfo).packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

			inflater.inflate(R.menu.context_comments_child, menu);
			menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
			menu.setHeaderTitle(R.string.menu_context_child_title);
			
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {

			inflater.inflate(R.menu.context_comments_parent, menu);
			menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
			menu.setHeaderTitle(R.string.menu_context_parent_title);
		}
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
		case R.id.menu_view_on_market:	// parent
		{
			String package_name = this.database.getMarketAppPackageName(info.id);
			Market.launchMarketAppDetails(this, package_name);
			return true;
		}
		case R.id.menu_mark_children_read:	// parent
		{
			int update_count = database.markAllAppCommentsAsRead(info.id);
			Log.e(TAG, "Marked " + update_count + " comments as read.");

			updateList();
			return true;
		}
		case R.id.menu_mark_read:	// child
		{
			long child_id = getExpandableListAdapter().getChildId(groupPos, childPos);
			
			int modified_count = this.database.markCommentAsRead(child_id);
			Log.e(TAG, "Modified " + modified_count + " comments.");
			updateList();
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
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		
		Cursor cursor = ((CursorTreeAdapter) parent.getExpandableListAdapter()).getChild(groupPosition, childPosition);
		this.pending_dialog_message = cursor.getString(cursor.getColumnIndex(DatabaseRevenue.KEY_COMMENT_TEXT));
		showDialog(DIALOG_FULL_COMMENT);
		return true;
	}
	
	// ========================================================================
	void updateList() {
		
		new CursorUpdateTask().execute();
	}

	// ========================================================================
	public class CursorUpdateTask extends AsyncTask<Void, Void, Cursor> {

		// ========================================================================
		@Override
		protected void onPreExecute() {
			incSemaphore();
		}
		
		// ========================================================================
		@Override
		protected Cursor doInBackground(Void... voided) {
			return database.getMarketAppsForUnreadComments();
		}

		// ========================================================================
		@Override
		public void onPostExecute(Cursor cursor) {
			decSemaphore();
			startManagingCursor(cursor);
			((CursorTreeAdapter) getExpandableListAdapter()).changeCursor(cursor);
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
}