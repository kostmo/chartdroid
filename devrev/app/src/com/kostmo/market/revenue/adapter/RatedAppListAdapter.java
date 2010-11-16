package com.kostmo.market.revenue.adapter;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.provider.DatabaseRevenue;

public class RatedAppListAdapter extends AppListAdapter {

	static final String TAG = "RatedAppListAdapter"; 
    
    // ========================================================================
    public RatedAppListAdapter(Context context, int layout, Cursor cursor) {
    	super(context, layout, cursor);
    }
    
    // ========================================================================
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);
		
		AppExpandableListAdapter.bindColoredCommentsCountView(view, context, cursor);
		
		
		TextView last_comment_date = (TextView) view.findViewById(R.id.latest_comment_date);
		int last_comment_date_column = cursor.getColumnIndex(DatabaseRevenue.KEY_LATEST_RATING);
		String last_comment_text = "Last: ";
		if (cursor.isNull(last_comment_date_column)) {
			last_comment_text += "Never";
		} else {
			long latest_comment_date_millis = cursor.getLong(last_comment_date_column);
			last_comment_text += RevenueActivity.HUMAN_DATE_FORMAT.format(new Date(latest_comment_date_millis));
		}
		last_comment_date.setText( last_comment_text );
		

		int rating_threshold_column = cursor.getColumnIndex(DatabaseRevenue.KEY_RATING_ALERT_THRESHOLD);
		RatingBar rating_bar = (RatingBar) view.findViewById(R.id.rating_threshold);
		if (rating_threshold_column >= 0) {
			int rating = cursor.getInt(rating_threshold_column);
			rating_bar.setRating(rating);
		}
	}
}