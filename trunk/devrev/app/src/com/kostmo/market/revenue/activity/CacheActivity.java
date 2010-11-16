package com.kostmo.market.revenue.activity;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.adapter.CacheRangeListAdapter;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.tools.SemaphoreHost;


public class CacheActivity extends ListActivity implements SemaphoreHost {

	static final String TAG = "CacheActivity";

	DatabaseRevenue database;
	SharedPreferences settings;

	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	// ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list_activity_cache_explorer);
		
		
		this.database = new DatabaseRevenue(this);
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);

		Cursor cursor = this.database.getCacheRanges();
		CacheRangeListAdapter adapter = new CacheRangeListAdapter(
				this,
				R.layout.list_item_cache_range,
//				android.R.layout.simple_list_item_1,
				cursor);
		
		this.setListAdapter(adapter);
	}

	// ========================================================================
	class StateObject {
	}
	
	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {
		StateObject state = new StateObject();
		return state;
	}

	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);

		switch (id) {

		}
		return null;
	}

	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {
		default:
			break;
		}
	}

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
        }

        return super.onOptionsItemSelected(item);
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
		// TODO Auto-generated method stub
		
	}
}