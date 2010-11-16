package com.kostmo.market.revenue.adapter;

import android.content.Context;
import android.widget.ImageView;

public interface BitmapPopulator {
	void populateBitmap(Context context, long merchant_item_id, ImageView icon);
}
