package com.googlecode.chartdroid.calendar.view;


import com.googlecode.chartdroid.calendar.container.CalendarDay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DayView extends View {

	static final String TAG = "DayView"; 
	
    int intrinsic_width = 50;
    int intrinsic_height = 50;
  
    CalendarDay calendar_day;
    boolean dim;
	Paint my_paint;

    // ========================================================================
    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public DayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(new CalendarDay(), false);
    }

    // ========================================================================
    public DayView(Context context, CalendarDay calendar, boolean dim) {
        super(context);
        init(calendar, dim);
    }

    // ========================================================================
    void init(CalendarDay calendar_day, boolean dim) {
    	this.calendar_day = calendar_day;
    	this.dim = dim;
    	
        my_paint = new Paint();
		my_paint.setAntiAlias(true);
		my_paint.setColor(Color.WHITE);
//		my_paint.setStyle(Style.STROKE);
//		my_paint.setStrokeJoin(Join.MITER);
//		my_paint.setStrokeWidth(2);
		my_paint.setTextAlign(Align.CENTER);
    }

    // ========================================================================
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    // ========================================================================
    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = intrinsic_width + getPaddingLeft()
                    + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }
    
    // ========================================================================
    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = intrinsic_height + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    // ========================================================================
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

		Log.d(TAG, "OLD - Width: " + oldw + "; Height: " + oldh);
		Log.d(TAG, "NEW - Width: " + w + "; Height: " + h);
    }

    // ========================================================================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

		// Draw the background
		canvas.drawColor(this.dim ? Color.BLUE : Color.YELLOW);
		
        canvas.translate(getWidth()/2, getHeight()/2);

        int size = Math.min(getWidth(), getHeight());
        Log.e(TAG, "Size: " + size);
		
        my_paint.setColor(Color.RED);
        my_paint.setStyle(Style.FILL);
		canvas.drawCircle(0, 0, size/3, my_paint);
		
		
		// Draw the outline
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(calendar_day.date);
		String text = Integer.toString( cal.get(Calendar.DAY_OF_MONTH) );
//		Rect text_bounds = new Rect();
//		my_paint.getTextBounds(text, 0, text.length(), text_bounds);
		my_paint.setTextSize(size/2f);
		float text_height = my_paint.getFontMetrics().ascent;
		

		
        my_paint.setColor(Color.WHITE);
		canvas.drawText(text, 0, -text_height/2, my_paint);
    }

    // ========================================================================
    public Date getDate() {
    	return calendar_day.date;
    }
}