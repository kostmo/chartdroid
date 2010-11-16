package com.kostmo.market.revenue.task;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

import com.kostmo.market.revenue.provider.DatabaseRevenue;

public abstract class PublishersFetcherTask extends AsyncTask<Void, Void, List<String>> {
	
	protected Context context;
	protected DatabaseRevenue database;
	public PublishersFetcherTask(Context c) {
		this.context = c;
		this.database = new DatabaseRevenue(c);
	}
	
	@Override
    public void onPreExecute() {
	}
	
	@Override
	protected List<String> doInBackground(Void... voided) {
		
		return this.database.getPublisherNames();
	}
	
    @Override
    public void onPostExecute(List<String> publishers) {
    }
}