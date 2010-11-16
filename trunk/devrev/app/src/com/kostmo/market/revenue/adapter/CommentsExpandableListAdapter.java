package com.kostmo.market.revenue.adapter;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;

import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.task.AppIconAdapterFetcherTask;
import com.kostmo.tools.SemaphoreHost;

public class CommentsExpandableListAdapter extends ResourceCursorTreeAdapter implements BitmapPopulator {

	static final String TAG = "CommentsExpandableListAdapter";

	private Map<Long, SoftReference<Bitmap>> bitmapMap = new Hashtable<Long, SoftReference<Bitmap>>();
    
    SemaphoreHost host;
	DatabaseRevenue database;
	
    
    // ========================================================================
    public Map<Long, SoftReference<Bitmap>> getCachedImageMap() {
    	return this.bitmapMap;
    }
    
    // ========================================================================
    public void setCachedImageMap(Map<Long, SoftReference<Bitmap>> map) {
    	this.bitmapMap = map;
    }
	
	// ============================================================================
	public CommentsExpandableListAdapter(
			Context context,
			Cursor cursor,
			int groupLayout,
			int childLayout,
			DatabaseRevenue database) {
		
		super(context, cursor, groupLayout, childLayout);
		this.database = database;
		this.host = (SemaphoreHost) context;
	}

	// ============================================================================
	@Override
	protected void bindChildView(View view, Context context, Cursor cursor,
			boolean isLastChild) {

		int author_name_column = cursor.getColumnIndex(DatabaseRevenue.KEY_COMMENT_AUTHOR_NAME);	
		String author_name = cursor.getString( author_name_column );
		
		int rating_value_column = cursor.getColumnIndex(DatabaseRevenue.KEY_RATING_VALUE);	
		int rating_value = cursor.getInt( rating_value_column );
		
//		int comment_length_column = cursor.getColumnIndex(DatabaseRevenue.KEY_COMMENT_LENGTH);	
//		int comment_length = cursor.getInt( comment_length_column );

		int comment_text_column = cursor.getColumnIndex(DatabaseRevenue.KEY_COMMENT_TEXT);	
		String comment_text = cursor.getString( comment_text_column );
		
		int rating_timetamp_column = cursor.getColumnIndex(DatabaseRevenue.KEY_RATING_TIMESTAMP);	
		long rating_timetamp = cursor.getLong( rating_timetamp_column );
		
		
		RatingBar rating_bar = (RatingBar) view.findViewById(R.id.rating);
		rating_bar.setRating(rating_value);
		
		TextView author_textview = (TextView) view.findViewById(android.R.id.text1);
		author_textview.setText(author_name);
		
		TextView comment_textview = (TextView) view.findViewById(android.R.id.text2);
		comment_textview.setText(comment_text);
		
		
		TextView most_recent_rating_date = (TextView) view.findViewById(R.id.comment_date);
		most_recent_rating_date.setText(
			RevenueActivity.HUMAN_DATE_FORMAT.format( new Date(rating_timetamp) )
		);
	}

	// ============================================================================
	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor,
			boolean isExpanded) {

		AppListAdapter.bindAppView(view, cursor, context, this);
		
		AppExpandableListAdapter.bindColoredCommentsCountView(view, context, cursor);

		
		// Most recent:
		int most_recent_rating_timetamp_column = cursor.getColumnIndex(DatabaseRevenue.KEY_LATEST_SUBTHRESHOLD_RATING);	
		long most_recent_rating_timetamp = cursor.getLong( most_recent_rating_timetamp_column );

		int oldest_rating_timetamp_column = cursor.getColumnIndex(DatabaseRevenue.KEY_EARLIEST_SUBTHRESHOLD_RATING);	
		long oldest_rating_timetamp = cursor.getLong( oldest_rating_timetamp_column );

		DateRange subthreshold_date_range = new DateRange(
				new Date(oldest_rating_timetamp), new Date(most_recent_rating_timetamp));

		TextView most_recent_rating_date = (TextView) view.findViewById(android.R.id.text2);
		most_recent_rating_date.setText(
				subthreshold_date_range.format(RevenueActivity.HUMAN_DATE_FORMAT));
		
		
		int unread_count_column = cursor.getColumnIndex(DatabaseRevenue.KEY_UNREAD_SUBTHRESHOLD_COMMENT_COUNT);	
		int unread_count = cursor.getInt( unread_count_column );

		TextView new_count_textview = (TextView) view.findViewById(R.id.new_count);
		new_count_textview.setText( unread_count + " new");
	}

	// ============================================================================
	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		int rowid_index = groupCursor.getColumnIndex(BaseColumns._ID);
		long app_id = groupCursor.getLong(rowid_index);

		return database.getNewAppComments(app_id);
	}

	// ========================================================================
	class AppIconAdapterFetcherTaskExtended extends AppIconAdapterFetcherTask {
		
		ImageView image_view;
		public AppIconAdapterFetcherTaskExtended(Context context, long appId, ImageView image_view) {
			super(context, appId);
			this.image_view = image_view;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			host.incSemaphore();
			this.image_view.setImageResource(R.drawable.sym_def_app_icon);
		}

		@Override
		protected void completeTask(Bitmap bitmap) {
			host.decSemaphore();
			this.image_view.setImageBitmap(bitmap);
			bitmapMap.put(this.app_id, new SoftReference<Bitmap>(bitmap));
		}

		@Override
		protected void failTask(String errorMessage) {
			host.showError(errorMessage);
		}
	}
	
	// ========================================================================
	@Override
	public void populateBitmap(Context context, long merchant_item_id, ImageView icon) {

		if (merchant_item_id == 0) {
			icon.setImageResource(android.R.drawable.ic_menu_help);
			return;
		}
		
		if (bitmapMap.containsKey(merchant_item_id) && bitmapMap.get(merchant_item_id).get() != null) {
			icon.setImageBitmap(bitmapMap.get(merchant_item_id).get());
		} else {
			new AppIconAdapterFetcherTaskExtended(context, merchant_item_id, icon).execute();
		}
	}
}
