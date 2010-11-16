/**
 * This is the most updated version of EditIntegerPreference.
 * It automatically saves the preference to Float or Decimal
 * format depending upon the android:numeric="integer" or
 * android:numeric="decimal" attribute.
 * 
 * June 12, 2010
 */

package com.kostmo.tools.view.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;

import com.kostmo.market.revenue.R;

public class EditIntegerPreference extends EditTextPreference {
	
	public EditIntegerPreference(Context context) {
		super(context);
	}

	int min_value, max_value;
	boolean has_min, has_max;
	
	void initAttrs(Context context, AttributeSet attrs) {
		
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.EditIntegerPreference);
        
        has_min = a.hasValue(R.styleable.EditIntegerPreference_minValue);
        if (has_min)
        	min_value = a.getInt(R.styleable.EditIntegerPreference_minValue, 0);
        
        has_max = a.hasValue(R.styleable.EditIntegerPreference_maxValue);
        if (has_max)
        	max_value = a.getInt(R.styleable.EditIntegerPreference_maxValue, 10);

        a.recycle();
	}
	
	
	public EditIntegerPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initAttrs(context, attrs);
	}

	
	public EditIntegerPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initAttrs(context, attrs);
	}

	@Override
	public String getText()
	{
		// Test the mask to see if we are dealing with Floats or Integers
		if ((this.getEditText().getInputType() & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0) {
			return String.valueOf( getPersistedFloat(0) );
		} else {
			return String.valueOf( getPersistedInt(0) );
		}
	}

	@Override
	public void setText(String text)
	{
		if (text.length() <= 0) return;
		
		// Test the mask to see if we are dealing with Floats or Integers
		if ((this.getEditText().getInputType() & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0) {
			// We are dealing with floats
			
			float typed_value = Float.parseFloat(text);
//			Log.e("Prefs", "typed_value: " + typed_value);
			if (has_max)
				typed_value = Math.min(max_value, typed_value);
//			Log.e("Prefs", "upper_clip: " + upper_clip);
			if (has_min)
				typed_value = Math.max(min_value, typed_value);
//			Log.e("Prefs", "About to save value: " + int_value);
			getSharedPreferences().edit().putFloat(getKey(), typed_value).commit();
		} else {
			// We are dealing with integers
			
			int typed_value = Integer.parseInt(text);
//			Log.e("Prefs", "typed_value: " + typed_value);
			if (has_max)
				typed_value = Math.min(max_value, typed_value);
//			Log.e("Prefs", "upper_clip: " + upper_clip);
			if (has_min)
				typed_value = Math.max(min_value, typed_value);
//			Log.e("Prefs", "About to save value: " + int_value);
			getSharedPreferences().edit().putInt(getKey(), typed_value).commit();
		}
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		if (restoreValue)
			getEditText().setText(getText());
		else
			super.onSetInitialValue(restoreValue, defaultValue);
	}
} 