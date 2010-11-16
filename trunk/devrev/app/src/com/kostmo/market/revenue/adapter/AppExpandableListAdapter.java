package com.kostmo.market.revenue.adapter;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.BaseColumns;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;

import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.ConsolidationActivity;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.task.AppIconAdapterFetcherTask;
import com.kostmo.tools.SemaphoreHost;

public class AppExpandableListAdapter extends ResourceCursorTreeAdapter implements BitmapPopulator {

	static final String TAG = "MerchantItemExpandableListAdapter";

	private Map<Long, SoftReference<Bitmap>> bitmapMap = new Hashtable<Long, SoftReference<Bitmap>>();
    
    SemaphoreHost host;
	DatabaseRevenue database;

	// Maps the (unique) merchant item ids to checkmarks.
	// This map should be cleared every time the Cursor is requeried.
	private Map<Long,Boolean> checkmarks = new HashMap<Long,Boolean>();
	
    
	public enum CommentsFetchStatus {
		GOT_ALL(R.color.light_green), GOT_MAX(R.color.light_yellow), INCOMPLETE(R.color.light_red);

		public final int color_resource_id;
		CommentsFetchStatus(int color_resource) {
			this.color_resource_id = color_resource;
		}
	}
	
    // ========================================================================
    public Map<Long, SoftReference<Bitmap>> getCachedImageMap() {
    	return this.bitmapMap;
    }
    
    // ========================================================================
    public void setCachedImageMap(Map<Long, SoftReference<Bitmap>> map) {
    	this.bitmapMap = map;
    }
	
	// ============================================================================
	public AppExpandableListAdapter(
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
	public void dumpCheckedIds() {
		List<Long> checked = getCheckedIds();
		Log.d(TAG, "Checked IDs: (" + checked.size() + ")");		
		for (long id : checked)
			Log.d(TAG, "" + id);
	}
	
	// ============================================================================
	public List<Long> getCheckedIds() {

		List<Long> checked_ids = new ArrayList<Long>();
		for (Entry<Long, Boolean> entry : this.checkmarks.entrySet())
			if (entry.getValue())
				checked_ids.add(entry.getKey());

		return checked_ids;
	}
	
	// ============================================================================
	public void assignCheckmark(long id, boolean checked) {
		this.checkmarks.put(id, checked);
	}
	
	// ============================================================================
	public void toggleCheckmark(long merchant_item_id) {
		this.checkmarks.put(merchant_item_id, !isChecked(merchant_item_id));
	}
	
	// ============================================================================
	public void clearCheckmarks() {
		this.checkmarks.clear();
	}

	// ============================================================================
	private boolean isChecked(long merchant_item_id) {
		boolean checked = false;
		if (this.checkmarks.containsKey(merchant_item_id))
			checked = this.checkmarks.get(merchant_item_id);
		
		return checked;
	}
	
	// ============================================================================
	void populateViewCheckmark(View view, Cursor cursor) {
		long merchant_item_id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
		CheckBox cbox = (CheckBox) view.findViewById(R.id.checkbox);
		cbox.setChecked(isChecked(merchant_item_id));
	}
	
	// ============================================================================
	@Override
	protected void bindChildView(View view, Context context, Cursor cursor,
			boolean isLastChild) {

		int text_column = cursor.getColumnIndex(DatabaseRevenue.KEY_ITEM_NAME);

		TextView series_name = (TextView) view.findViewById(android.R.id.text1);
		if (text_column >= 0) {
			String label = cursor.getString(text_column);
			series_name.setText(label);
		}
		
		int color = Color.HSVToColor(new float[] {360 * cursor.getPosition() / (float) cursor.getCount(), 0.6f, 1});
		series_name.setTextColor(color);


		
		
		populateViewCheckmark(view, cursor);
		
		

		int app_price_column = cursor.getColumnIndex(DatabaseRevenue.KEY_REVENUE_CENTS);
		int aggregate_revenue_column = cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_REVENUE);
		int sale_count_column = cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_SALE_COUNT);
		int first_sale_date_column = cursor.getColumnIndex(DatabaseRevenue.KEY_EARLIEST_SALE_DATE);
		int last_sale_date_column = cursor.getColumnIndex(DatabaseRevenue.KEY_LATEST_SALE_DATE);

		TextView sold_since = (TextView) view.findViewById(android.R.id.text2);

		int price_cents = cursor.getInt(app_price_column);
		int total_revenue_cents = cursor.getInt(aggregate_revenue_column);
		int sale_count = cursor.getInt(sale_count_column);
		
		DateRange sales_span = new DateRange(
			new Date(cursor.getLong(first_sale_date_column)),
			new Date(cursor.getLong(last_sale_date_column))
		);
		
		


		String complete_string = "";
		
		String total_revenue_string = centsAsDollarString(total_revenue_cents);
		int total_revenue_start_position = complete_string.length();
		complete_string += total_revenue_string;
		int total_revenue_end_position = complete_string.length();
		
		complete_string += " from ";
		String sale_count_string = "" + sale_count;
		int sale_count_start_position = complete_string.length();
		complete_string += sale_count_string;
		int sale_count_end_position = complete_string.length();
		

		String sale_word = context.getResources().getQuantityString(R.plurals.sale_count_word, sale_count);
		complete_string += " cached " + sale_word + " at ";
		
		String price_string = centsAsDollarString(price_cents);
		int price_string_start_position = complete_string.length();
		complete_string += price_string;
		int price_string_end_position = complete_string.length();
		
		complete_string += " from " + sales_span.format(RevenueActivity.HUMAN_DATE_FORMAT);

		SpannableString complete_span = new SpannableString(complete_string);
		
		complete_span.setSpan(new StyleSpan(Typeface.BOLD), total_revenue_start_position, total_revenue_end_position, 0);
		complete_span.setSpan(new ForegroundColorSpan(Color.WHITE), total_revenue_start_position, total_revenue_end_position, 0);
	
		complete_span.setSpan(new StyleSpan(Typeface.BOLD), sale_count_start_position, sale_count_end_position, 0);
		complete_span.setSpan(new ForegroundColorSpan(Color.WHITE), sale_count_start_position, sale_count_end_position, 0);
	
		complete_span.setSpan(new StyleSpan(Typeface.BOLD), price_string_start_position, price_string_end_position, 0);
		complete_span.setSpan(new ForegroundColorSpan(Color.WHITE), price_string_start_position, price_string_end_position, 0);
		
		sold_since.setText( complete_span );
	}

	// ============================================================================
	public static String centsAsDollarString(int cents) {
		return String.format(ConsolidationActivity.DOLLAR_AXIS_FORMAT, cents / 100f);
	}

	// ============================================================================
	static void bindColoredCommentsCountView(View view, Context context, Cursor cursor) {

		TextView ratings_count_textbox = (TextView) view.findViewById(R.id.ratings_count);
		if (cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)) == 0) {
			ratings_count_textbox.setVisibility(View.GONE);
		} else {
			ratings_count_textbox.setVisibility(View.VISIBLE);
			
			int cached_ratings_count_column = cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_RATING_COUNT);	
			int cached_ratings_count = cursor.getInt( cached_ratings_count_column );
			int ratings_count_column = cursor.getColumnIndex(DatabaseRevenue.KEY_APP_RATING_COUNT);	
			int ratings_count = cursor.getInt( ratings_count_column );
	
			ratings_count_textbox.setText("Comments: " + cached_ratings_count + " (" + ratings_count + ")");
			
			int ratings_color_resource = 
				cursor.getInt(cursor.getColumnIndex(DatabaseRevenue.KEY_GOT_EARLIEST_COMMENT)) != 0 ? CommentsFetchStatus.GOT_ALL.color_resource_id :
					cursor.getInt(cursor.getColumnIndex(DatabaseRevenue.KEY_GOT_MAX_COMMENTS)) != 0 ? CommentsFetchStatus.INCOMPLETE.color_resource_id : CommentsFetchStatus.INCOMPLETE.color_resource_id;
			
			ratings_count_textbox.setTextColor(context.getResources().getColor(ratings_color_resource));
		}
	}
	
	// ============================================================================
	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor,
			boolean isExpanded) {

		AppListAdapter.bindAppView(view, cursor, context, this);
		
		bindColoredCommentsCountView(view, context, cursor);

		
		
		
		int current_price_column = cursor.getColumnIndex(DatabaseRevenue.KEY_APP_CURRENT_PRICE);
		TextView app_title = (TextView) view.findViewById(android.R.id.text1);
		app_title.setTextColor(context.getResources().getColor(cursor.getInt(current_price_column) > 0 ? android.R.color.white : R.color.aquamarine));
		
		
		int child_count_column = cursor.getColumnIndex(DatabaseRevenue.KEY_CONSOLIDATED_APP_COUNT);
		int child_count = cursor.getInt( child_count_column );
		String child_plural_string = context.getResources().getQuantityString(R.plurals.version_count, child_count, child_count);
		TextView deployments_count_textbox = (TextView) view.findViewById(R.id.deployments_count);
		deployments_count_textbox.setText(child_plural_string);
		
		
		
		
		TextView sold_since = (TextView) view.findViewById(android.R.id.text2);
		
		
		
		int aggregate_revenue_column = cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_REVENUE);
		int aggregate_sales_count_column = cursor.getColumnIndex(DatabaseRevenue.KEY_AGGREGATE_SALE_COUNT);

		int aggregate_revenue = cursor.getInt( aggregate_revenue_column );
		int aggregate_sales_count = cursor.getInt( aggregate_sales_count_column );
		
		

		String sale_plural_string = context.getResources().getQuantityString(R.plurals.sale_count_word, aggregate_sales_count);
		
		
		
		
		
		



		String complete_string = "";
		
		String total_revenue_string = centsAsDollarString(aggregate_revenue);
		int total_revenue_start_position = complete_string.length();
		complete_string += total_revenue_string;
		int total_revenue_end_position = complete_string.length();

		complete_string += " from ";
		String sale_count_string = "" + aggregate_sales_count;
		int sale_count_start_position = complete_string.length();
		complete_string += sale_count_string;
		int sale_count_end_position = complete_string.length();

		complete_string += " cached " + sale_plural_string;
		 
		
		
		SpannableString complete_span = new SpannableString(complete_string);
		
		complete_span.setSpan(new StyleSpan(Typeface.BOLD), total_revenue_start_position, total_revenue_end_position, 0);
		complete_span.setSpan(new ForegroundColorSpan(Color.WHITE), total_revenue_start_position, total_revenue_end_position, 0);
	
		complete_span.setSpan(new StyleSpan(Typeface.BOLD), sale_count_start_position, sale_count_end_position, 0);
		complete_span.setSpan(new ForegroundColorSpan(Color.WHITE), sale_count_start_position, sale_count_end_position, 0);
	
		sold_since.setText( complete_span );
	}

	// ============================================================================
	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		int rowid_index = groupCursor.getColumnIndex(BaseColumns._ID);
		long app_id = groupCursor.getLong(rowid_index);

		return database.getMarketAppsChildren(app_id);
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

		// XXX How could this happen?
		if (icon == null) {
			Log.e(TAG, "Somehow, the ImageView reference in populateBitmap() was null...");
			return;
		}
		
		if (merchant_item_id == 0) {
			icon.setImageResource(android.R.drawable.ic_menu_help);
			return;
		}
		
		if (bitmapMap.containsKey(merchant_item_id)) {
			SoftReference<Bitmap> softref = bitmapMap.get(merchant_item_id);
			if (softref != null) {
				Bitmap bitmap = softref.get();
				if (bitmap != null) {
					icon.setImageBitmap(bitmap);
					return;
				}
			}
		}

		// If the cached bitmap was unavailable, we fetch it now.
		new AppIconAdapterFetcherTaskExtended(context, merchant_item_id, icon).execute();
	}
}
