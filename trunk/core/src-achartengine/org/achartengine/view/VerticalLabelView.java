/*
 * Copyright (C) 2010 Karl Ostmo
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

package org.achartengine.view;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.googlecode.chartdroid.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;


public class VerticalLabelView extends View {
	private Paint mTextPaint;
	private String mText;
	private int mAscent;
	private Rect text_bounds = new Rect();

	final static int DEFAULT_TEXT_SIZE = 15;

	public VerticalLabelView(Context context) {
		super(context);
		initLabelView();
	}

	public VerticalLabelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initLabelView();

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VerticalLabelView);

		CharSequence s = a.getString(R.styleable.VerticalLabelView_text);
		if (s != null) setText(s.toString());

		setTextColor(a.getColor(R.styleable.VerticalLabelView_textColor, 0xFF000000));

		int textSize = a.getDimensionPixelOffset(R.styleable.VerticalLabelView_textSize, 0);
		if (textSize > 0) setTextSize(textSize);

		a.recycle();
	}

	private final void initLabelView() {
		mTextPaint = new TextPaint();
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(DEFAULT_TEXT_SIZE);
		mTextPaint.setColor(0xFF000000);
		mTextPaint.setTextAlign(Align.CENTER);
		setPadding(3, 3, 3, 3);
	}

	public void setText(String text) {
		mText = text;
		requestLayout();
		invalidate();
	}

	public void setTextSize(int size) {
		mTextPaint.setTextSize(size);
		requestLayout();
		invalidate();
	}

	public void setTextColor(int color) {
		mTextPaint.setColor(color);
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		mTextPaint.getTextBounds(mText, 0, mText.length(), text_bounds);
		setMeasuredDimension(
				measureWidth(widthMeasureSpec),
				measureHeight(heightMeasureSpec));
	}

	private int measureWidth(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			// We were told how big to be
			result = specSize;
		} else {
			// Measure the text
			result = text_bounds.height() + getPaddingLeft() + getPaddingRight();

			if (specMode == MeasureSpec.AT_MOST) {
				// Respect AT_MOST value if that was what is called for by measureSpec
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

	private int measureHeight(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		mAscent = (int) mTextPaint.ascent();
		if (specMode == MeasureSpec.EXACTLY) {
			// We were told how big to be
			result = specSize;
		} else {
			// Measure the text
			result = text_bounds.width() + getPaddingTop() + getPaddingBottom();

			if (specMode == MeasureSpec.AT_MOST) {
				// Respect AT_MOST value if that was what is called for by measureSpec
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		float text_horizontally_centered_origin_x = getPaddingLeft() + text_bounds.width()/2f;
		float text_horizontally_centered_origin_y = getPaddingTop() - mAscent;

		canvas.translate(text_horizontally_centered_origin_y, text_horizontally_centered_origin_x);
		canvas.rotate(-90);
		canvas.drawText(mText, 0, 0, mTextPaint);
	}
}