package com.kostmo.market.revenue.adapter;

import java.lang.ref.SoftReference;
import java.util.Hashtable;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.activity.ConsolidationActivity;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.task.AppIconAdapterFetcherTask;
import com.kostmo.tools.SemaphoreHost;

public abstract class AppListAdapter extends ResourceCursorAdapter implements BitmapPopulator {

	static final String TAG = "AppListAdapter"; 

	private Map<Long, SoftReference<Bitmap>> bitmapMap = new Hashtable<Long, SoftReference<Bitmap>>();
    
    SemaphoreHost host;
    
    // ========================================================================
    public AppListAdapter(Context context, int layout, Cursor cursor) {
    	super(context, layout, cursor);
    	this.host = (SemaphoreHost) context;
    }
    
    // ========================================================================
    public Map<Long, SoftReference<Bitmap>> getCachedImageMap() {
    	return this.bitmapMap;
    }
    
    // ========================================================================
    public void setCachedImageMap(Map<Long, SoftReference<Bitmap>> map) {
    	this.bitmapMap = map;
    }
    
    // ========================================================================
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		bindAppView(view, cursor, context, this);
	}

	// ========================================================================
	public static void bindAppView(View view, Cursor cursor, Context context, BitmapPopulator bitmap_populator) {
		int merchant_item_id_column = cursor.getColumnIndex(BaseColumns._ID);
		int app_title_column = cursor.getColumnIndex(DatabaseRevenue.KEY_APP_TITLE);
		int app_price_column = cursor.getColumnIndex(DatabaseRevenue.KEY_APP_PRICE_MICROS);

		TextView series_name = (TextView) view.findViewById(android.R.id.text1);
		if (app_title_column >= 0) {
			String label = cursor.getString(app_title_column);
			series_name.setText(label);
		}
		
		TextView sold_since = (TextView) view.findViewById(android.R.id.text2);
		if (app_price_column >= 0) {
			int micros = cursor.getInt(app_price_column);

			boolean free = micros == 0;
			String price = free ? "Free" : String.format(ConsolidationActivity.DOLLAR_AXIS_FORMAT, micros / 1E6f);
			sold_since.setText( price );
			sold_since.setTextColor( context.getResources().getColor( free ? android.R.color.secondary_text_dark : R.color.lemon_custard ) );
		}
	
		ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
		if (app_title_column >= 0) {
			long merchant_item_id = cursor.getLong(merchant_item_id_column);
			bitmap_populator.populateBitmap(context, merchant_item_id, icon);
		}
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

		if (bitmapMap.containsKey(merchant_item_id) && bitmapMap.get(merchant_item_id).get() != null) {
			icon.setImageBitmap(bitmapMap.get(merchant_item_id).get());
		} else {
			new AppIconAdapterFetcherTaskExtended(context, merchant_item_id, icon).execute();
		}
	}
}