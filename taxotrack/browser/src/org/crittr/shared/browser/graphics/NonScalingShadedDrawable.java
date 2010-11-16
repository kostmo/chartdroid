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

package org.crittr.shared.browser.graphics;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

public class NonScalingShadedDrawable extends Drawable {

	final static String TAG = "Crittr";
	
	int padding_top = 50;
	int padding_bottom = 10;

	int padding_left = 20;
	int padding_right = 20;
	
	Context context;
	View view;
	
	Bitmap hosted_bitmap;
	Paint paint;
	
	public NonScalingShadedDrawable(Context c, View v, int resource) {
		context = c;
		view = v;

        InputStream is = context.getResources().openRawResource(resource);
        Bitmap original_bitmap = BitmapFactory.decodeStream(is);
        hosted_bitmap = original_bitmap.extractAlpha();
        

        paint = new Paint();

	}
	
	public void draw(Canvas canvas) {
		

		int w = hosted_bitmap.getWidth();
		int h = hosted_bitmap.getHeight();
/*
		int w = bitmap_drawable.getIntrinsicWidth();
		int h = bitmap_drawable.getIntrinsicHeight();
*/
		int view_w = view.getWidth();
		int view_h = view.getHeight();
//		Log.e(TAG, "View dimensions: (" + view_w + ", " + view_h + ")");
		
		
		int padded_horizontal_room = view_w - (padding_left + padding_right);
		int padded_vertical_room = view_h - (padding_top + padding_bottom);
		
		float scale;
		float intrinsic_aspect_ratio = w / (float) h;
		float padded_canvas_aspect_ratio = padded_horizontal_room / (float) padded_vertical_room;
		if (intrinsic_aspect_ratio > padded_canvas_aspect_ratio)
			// Our source image is wider than the canvas, so we scale by width.
			scale = padded_horizontal_room / (float) w;
		else
			scale = padded_vertical_room / (float) h;
		
		int scaled_width = (int) (scale*w);
		int scaled_height = (int) (scale*h);
		

//		Log.e(TAG, "Scaled dimensions: (" +scaled_width + ", " + scaled_height + ")");
		
		// Here we fit the image into the bottom-right corner.
		int left = view_w - scaled_width - padding_right;
		int top = view_h - scaled_height - padding_bottom;
		int right = view_w - padding_right;
		int bottom = view_h - padding_bottom;
		

//		Log.e(TAG, "Bounds: (" +left + ", " + top + ", " + right + ", " + bottom + ")");
		
		Rect bounds = new Rect(
				left,
				top,
				right,
				bottom
				);

        

        Shader mShader = new RadialGradient(w/2f, h/2f, Math.max(w, h)/2, Color.RED, Color.BLUE, Shader.TileMode.CLAMP);
        paint.setShader(mShader);
        
        canvas.drawBitmap(hosted_bitmap, null, bounds, paint);

	}

	public int getOpacity() {
		return paint.getAlpha();
	}

	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
	}

	public void setColorFilter(ColorFilter cf) {
		paint.setColorFilter(cf);
	}
}
    


