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

public class RoundedRectBackgroundDrawable extends Drawable {

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
	public RoundedRectBackgroundDrawable(Context c, View v, int color) {
		context = c;
		view = v;
		this.color = color;
		
		
		
        r = 5;
		mDrawable = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { color, Color.TRANSPARENT });

        mDrawable.setShape(GradientDrawable.RECTANGLE);
        mDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        setCornerRadii(mDrawable, 0, r, r, 0);
	}
	
	
	
    static void setCornerRadii(GradientDrawable drawable, float r0,
            float r1, float r2, float r3) {
		drawable.setCornerRadii(new float[] { r0, r0, r1, r1,
		                           r2, r2, r3, r3 });
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

		
		
		
		/*
        float[] outerR = new float[] { 12, 12, 12, 12, 12, 12, 12, 12 };
        RectF   inset = new RectF(6, 6, 6, 6);
        float[] innerR = new float[] { 12, 12, 12, 12, 12, 12, 12, 12 };
        ShapeDrawable rounded_rect = new ShapeDrawable(new RoundRectShape(outerR, inset,
                innerR));
		
        
        
        rounded_rect.getPaint().setColor(color);
        rounded_rect.getPaint().setStrokeWidth(4);

        rounded_rect.setBounds(
						0,
						0,
						view_w,
						view_h);

        rounded_rect.draw(canvas);
        */
		


        Rect mRect = new Rect(0, 2, (int) (r*2), view_h - 2);
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
	
	

	@Override
	public boolean getPadding(Rect padding) {
		
		padding.bottom = 2;
		padding.top = 2;
		padding.left = (int) r*3;
		padding.right = 2;
		
		return true;
	}
}
    


