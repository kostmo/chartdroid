// NOTE: Found in modified form at this URL:
// http://staticfree.info/clip/2009-10-20T132442

package com.googlecode.chartdroid.calendar.view;


import com.googlecode.chartdroid.calendar.CalendarUtils;
import com.googlecode.chartdroid.calendar.container.CalendarDay;
import com.googlecode.chartdroid.calendar.container.SimpleEvent;

import org.achartengine.activity.GraphicalActivity;
import org.achartengine.activity.GraphicalActivity.DataSeriesAttributes;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * ViewGroup that arranges child views in a similar way to text, with them laid
 * out one line at a time and "wrapping" to the next line as needed.
 */
public class MonthLayout extends ViewGroup {

	static final String TAG = "MonthLayout";
	
	final int DAYS_PER_WEEK = 7;

    // ========================================================================
    public static class LayoutParams extends ViewGroup.LayoutParams {
        public final int horizontal_spacing;
        public final int vertical_spacing;
        
        /**
         * @param horizontal_spacing Pixels between items, horizontally
         * @param vertical_spacing Pixels between items, vertically
         */
        public LayoutParams(int horizontal_spacing, int vertical_spacing) {
            super(0, 0);
            this.horizontal_spacing = horizontal_spacing;
            this.vertical_spacing = vertical_spacing;     
        }
    }

    LayoutParams reflow_layout_params;
    Context context;
    Calendar month;
    // ========================================================================
    public MonthLayout(Context context, Calendar month) {
        super(context);

    	init(context);
    	setMonth(month);
    }
    
    // ========================================================================
    public MonthLayout(Context context, AttributeSet attrs){
    	super(context, attrs);
    	
    	reflow_layout_params = (LayoutParams) generateDefaultLayoutParams();
    	
    	init(context);
    }

    // ========================================================================
    public void setMonth(Calendar month) {
    	this.month = month;
    }
    
    // ========================================================================
    void init(Context context) {
    	this.context = context;
    }

    // ========================================================================
    void generateChildren(GregorianCalendar month) {
    	
    	List<CalendarDay> day_list = new ArrayList<CalendarDay>();
    	CalendarUtils.generate_days(month, day_list, new ArrayList<SimpleEvent>());
    	
        List<DataSeriesAttributes> series_attributes_list = new ArrayList<DataSeriesAttributes>();
        for (int i=0; i<4; i++) {
	        for (int color : GraphicalActivity.DEFAULT_COLORS) {
	        	DataSeriesAttributes dsa = new DataSeriesAttributes();
	        	dsa.color = color;
	        	dsa.title = "0x" + Integer.toHexString(color);
	        	series_attributes_list.add( dsa );
	        }
        }

		populateCalendarView(this.context, month, day_list);
    }
    
    // ========================================================================
    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();

        
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
//                final ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) child.getLayoutParams();
                child.measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
                
                final int childw = child.getMeasuredWidth();
                line_height = Math.max(line_height, child.getMeasuredHeight() + reflow_layout_params.vertical_spacing);
                
                if (xpos + childw > width) {
                    xpos = getPaddingLeft();
                    ypos += line_height;
                }
                
                xpos += childw + reflow_layout_params.horizontal_spacing;
            }
        }
        this.line_height = line_height;
        
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED){
        	height = ypos + line_height;
        } else {
        	// XXX
        	height = ypos + line_height;
        }
        setMeasuredDimension(width, height);
    }
     */
    // ========================================================================
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(1, 1); // default of 1px spacing
    }

    // ========================================================================
    public void setFlowLayoutParams(LayoutParams lp) {
    	reflow_layout_params = lp;
    }

    // ========================================================================
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams)
            return true;
        return false;
    }

    // ========================================================================
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    	if (this.month != null) {
    		generateChildren((GregorianCalendar) this.month);
    	}
    	
        final int count = getChildCount();
        int usable_width = getWidth() - getPaddingLeft() - getPaddingRight();
        int usable_height = getHeight() - getPaddingTop() - getPaddingBottom();
        
        int inter_day_horizontal_padding = reflow_layout_params.horizontal_spacing;
        float width_per_day = (usable_width - (DAYS_PER_WEEK - 1)*inter_day_horizontal_padding) / DAYS_PER_WEEK;
        
        
        int weeks_per_month = (int) Math.ceil(getChildCount() / (float) DAYS_PER_WEEK);
        
        int inter_day_vertical_padding = reflow_layout_params.vertical_spacing;
        float height_per_day = (usable_height - (weeks_per_month - 1)*inter_day_vertical_padding) / weeks_per_month;
        
        Log.d(TAG, "Spacing: " + inter_day_horizontal_padding + ", " + inter_day_vertical_padding);
        
        
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);

            if (child instanceof DayView) {
            	DayView day_view = (DayView) child;
            }
            
            int left = getPaddingLeft() + (int) ((width_per_day + inter_day_horizontal_padding) * (i % DAYS_PER_WEEK));
            int top = getPaddingTop() + (int) ((height_per_day + inter_day_vertical_padding) * (i / DAYS_PER_WEEK));
                
            child.layout(left, top, left + (int) width_per_day, top + (int) height_per_day);
        }
    }

	// ========================================================================
	public void populateCalendarView(Context context, GregorianCalendar calendar, List<CalendarDay> day_list) {
		
		int month = calendar.get(GregorianCalendar.MONTH);
		
		MonthLayout.LayoutParams lp = new MonthLayout.LayoutParams(15, 10);
		this.setFlowLayoutParams(lp);

		for (CalendarDay day : day_list) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(day.date);
			boolean dim = month == cal.get(GregorianCalendar.MONTH);
			
			DayView b = new DayView(context, day, dim);
			b.setPadding(0, 0, 0, 0);
			this.addView(b);
		}
	}
}
