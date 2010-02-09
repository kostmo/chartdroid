package org.achartengine.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;


public class RotateTextView extends TextView
{

	public RotateTextView(Context context, AttributeSet attributes) {
		super(context, attributes);

	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

//		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}
	
	
	@Override
	protected
	void onDraw(Canvas canvas) {
		canvas.save();
		
		Log.d("Foo", "Canvas width: " + canvas.getWidth() + "; height: " + canvas.getHeight());
		Log.d("Foo", "View width: " + getWidth() + "; height: " + getHeight());
		
		canvas.translate(-getHeight()/2f, -getWidth()/2f);
		canvas.rotate(-90);
		
		super.onDraw(canvas);
		
		canvas.restore();
	} 
}