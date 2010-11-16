/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kostmo.flickr.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.kostmo.flickr.bettr.Market;

public class CornerDecorationDrawable extends Drawable {

	static final String TAG = Market.DEBUG_TAG; 
	
	int padding_top = 50;
	int padding_bottom = 10;

	int padding_left = 20;
	int padding_right = 20;
	
	Context context;
	View view;
	int color;
	float r;
	GradientDrawable mDrawable;
	public CornerDecorationDrawable(Context c, View v, int color) {
		context = c;
		view = v;
		this.color = color;
		
		
		
        r = 5;
		mDrawable = new GradientDrawable(
				GradientDrawable.Orientation.BR_TL,
                new int[] { Color.DKGRAY, Color.TRANSPARENT, Color.TRANSPARENT });

        mDrawable.setShape(GradientDrawable.RECTANGLE);
        mDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
	}
	
	public void draw(Canvas canvas) {
		

		
		int view_w = view.getWidth();
		int view_h = view.getHeight();
//		Log.e(TAG, "View dimensions: (" + view_w + ", " + view_h + ")");
		
		
		
		// Here we fit the image into the bottom-right corner.
		/*
		int left = view_w - scaled_width - padding_right;
		int top = view_h - scaled_height - padding_bottom;
		int right = view_w - padding_right;
		int bottom = view_h - padding_bottom;
		*/

//		Log.e(TAG, "Bounds: (" +left + ", " + top + ", " + right + ", " + bottom + ")");

		
		
	


        Rect mRect = new Rect(view_w - view_h, 0, view_w, view_h);
		mDrawable.setBounds(mRect);

        mDrawable.draw(canvas);
	}

	public int getOpacity() {
//		return hosted_drawable.getOpacity();
		return 0xFF;
	}

	public void setAlpha(int alpha) {
//		hosted_drawable.setAlpha(alpha);
		
		return;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
//		hosted_drawable.setColorFilter(cf);
		
		return;
	}
	
	
/*
	@Override
	public boolean getPadding(Rect padding) {
		
		padding.bottom = 2;
		padding.top = 2;
		padding.left = 10;
		padding.right = 2;
		
		return true;
	}
*/
}
    


