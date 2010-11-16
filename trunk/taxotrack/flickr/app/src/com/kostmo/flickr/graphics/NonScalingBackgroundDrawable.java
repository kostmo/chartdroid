package com.kostmo.flickr.graphics;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;

public class NonScalingBackgroundDrawable extends Drawable {

	static final String TAG = Market.DEBUG_TAG; 
	
	int padding_top = 30;
	int padding_bottom = 10;

	int padding_left = 20;
	int padding_right = 10;
	
	Context context;
	View view;
	Drawable hosted_drawable; 
	public NonScalingBackgroundDrawable(Context c, View v, int resource) {
//	public NonScalingBackgroundDrawable(Context c, View v) {
		context = c;
		view = v;
		

	    int drawable_resource;
	    if (resource < 0) {
		   switch (context.getResources().getConfiguration().orientation) {
		   case Configuration.ORIENTATION_PORTRAIT:
			   drawable_resource = R.drawable.flickr_wordmark;
			   break;
		   case Configuration.ORIENTATION_LANDSCAPE:
			   drawable_resource = R.drawable.flickr_wordmark;
			   break;
		   default:
			   drawable_resource = R.drawable.flickr_wordmark;
		   }
	    }
	    else drawable_resource = resource;
		

		hosted_drawable = context.getResources().getDrawable(drawable_resource);
	}
	
	public void draw(Canvas canvas) {
		

		int w = hosted_drawable.getIntrinsicWidth();
		int h = hosted_drawable.getIntrinsicHeight();
//		Log.e(TAG, "Intrinsic dimensions: (" + w + ", " + h + ")");

		/*
		int canvas_w = canvas.getWidth();
		int canvas_h = canvas.getHeight();
		*/
//		Log.e(TAG, "Canvas dimensions: (" + canvas_w + ", " + canvas_h + ")");
		
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
		hosted_drawable.setBounds(
//				new Rect(
						left,
						top,
						right,
						bottom
//						)
				);
		
		hosted_drawable.draw(canvas);
	}

	public int getOpacity() {
		return hosted_drawable.getOpacity();
	}

	public void setAlpha(int alpha) {
		hosted_drawable.setAlpha(alpha);
	}

	public void setColorFilter(ColorFilter cf) {
		hosted_drawable.setColorFilter(cf);
	}
}
    


