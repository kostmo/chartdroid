package com.kostmo.market.revenue.adapter;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.tools.SemaphoreHost;

public class CacheRangeListAdapter extends ResourceCursorAdapter {

	static final String TAG = "CacheRangeListAdapter"; 

    SemaphoreHost host;
    DatabaseRevenue database_helper;
    Context context;
    // ========================================================================
    public CacheRangeListAdapter(Context context, int layout, Cursor cursor) {
    	super(context, layout, cursor);
    	this.host = (SemaphoreHost) context;
    	this.context = context;

    	this.database_helper = new DatabaseRevenue(context);
    }

    // ========================================================================
	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		DateRange date_range = new DateRange(
				new Date(cursor.getLong(cursor.getColumnIndex(DatabaseRevenue.KEY_SPAN_START_MILLISECONDS))),
				new Date(cursor.getLong(cursor.getColumnIndex(DatabaseRevenue.KEY_SPAN_END_MILLISECONDS)))
		);
		TextView tv = (TextView) view.findViewById(android.R.id.text1);
		tv.setText(date_range.format(RevenueActivity.HUMAN_DATE_FORMAT));

		TextView tv2 = (TextView) view.findViewById(android.R.id.text2);
		new PurchaseCountFetcherTask(date_range, tv2).execute();
	}
	
	// ========================================================================
	class PurchaseCountFetcherTask extends AsyncTask<Void, Void, Integer> {
		
		TextView text_view;
		DateRange date_range;
//		DatabaseRevenue database_helper;
		public PurchaseCountFetcherTask(DateRange date_range, TextView image_view) {
			this.text_view = image_view;
			this.date_range = date_range;
//			database_helper = new DatabaseRevenue(context);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			host.incSemaphore();
			this.text_view.setText("...");
		}

		@Override
		protected Integer doInBackground(Void... params) {

//			DatabaseRevenue database = new DatabaseRevenue(context);
			return database_helper.getPurchaseCountInRange(this.date_range);
		}

		@Override
		protected void onPostExecute(Integer purchase_count) {
			host.decSemaphore();
			this.text_view.setText("Purchases: " + purchase_count);
		}
	}
}