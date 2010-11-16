package com.kostmo.tools.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

import com.kostmo.market.revenue.R;


public class CheckedDualTextView extends LinearLayout implements Checkable {

	static final String TAG = "CheckedDualTextView";
	
	CheckBox checkbox;
	
	public CheckedDualTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
        LayoutInflater factory = LayoutInflater.from(context);
    	View view = factory.inflate(R.layout.checkable_dual_textview, this);
    	
    	this.checkbox = (CheckBox) view.findViewById(R.id.checkbox);
	}

	@Override
	public boolean isChecked() {
		return this.checkbox.isChecked();
	}

	@Override
	public void setChecked(boolean checked) {
		this.checkbox.setChecked(checked);
	}

	@Override
	public void toggle() {
		this.checkbox.toggle();
	}
}