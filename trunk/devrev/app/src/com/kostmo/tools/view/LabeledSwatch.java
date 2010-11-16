package com.kostmo.tools.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Button;

import com.kostmo.market.revenue.R;


public class LabeledSwatch extends Button {


	static final String TAG = "LabeledSwatch"; 

	final static int DEFAULT_SIDE_LENGTH = 32;	
	final static int DEFAULT_COLOR = Color.RED;
	
	
	int color;
	
    // ========================================================================
	public LabeledSwatch(Context context, AttributeSet attrs) {
		super(context, attrs);

		initialize(context);
		
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LabeledSwatch);
        int color = a.getColor(R.styleable.LabeledSwatch_swatchColor, Color.MAGENTA);
        a.recycle();
        
		setColor(color);
	}
	
    // ========================================================================
	public LabeledSwatch(Context context) {
		super(context);

		initialize(context);
		setColor(DEFAULT_COLOR);
		setText("Foo");
	}

    // ========================================================================
	public void initialize(Context context) {

		setTextColor(Color.WHITE);
		setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		
		setCompoundDrawablePadding(5);
		setBackgroundDrawable(null);
		setPadding(0, 0, 0, 0);
	}
    
    // ========================================================================
	public void setColor(int color) {
		PaintDrawable d = new PaintDrawable(color);
		d.setIntrinsicWidth(DEFAULT_SIDE_LENGTH);
		d.setIntrinsicHeight(DEFAULT_SIDE_LENGTH);
		d.setCornerRadius(DEFAULT_SIDE_LENGTH/4f);
		
		setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
	}
}