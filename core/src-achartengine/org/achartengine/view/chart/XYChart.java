/**
 * Copyright (C) 2009 SC 4ViewSoft SRL
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
package org.achartengine.view.chart;

import org.achartengine.model.XYMultiSeries;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;
import org.achartengine.util.MathHelper;
import org.achartengine.util.MathHelper.MinMax;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.text.TextPaint;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The XY chart rendering class.
 * Tick marks and tick labels, and chart data
 */
public abstract class XYChart extends AbstractChart {

	public final static String TAG = "XYChart";

	public final static float DEFAULT_HASH_MARK_TEXT_SIZE = 9;
	public final static float DEFAULT_HORIZONTAL_AXIS_HASH_MARK_LENGTH = 4;

    /** Separation of tick label from tick line */
	public final static float VERTICAL_AXIS_LABEL_HASH_MARK_CLEARANCE = 2;

    /** Separation of tick label from vertical axis line */
	public final static int VERTICAL_AXIS_LINE_LABEL_CLEARANCE = 2;

	public XYMultiSeries getDataset() {
		return mDataset;
	}

	public XYMultipleSeriesRenderer getRenderer() {
		return mRenderer;
	}
	
	

	enum Axis {
		X_AXIS, Y_AXIS, Y_AXIS_SECONDARY
	}

	/** The multiple series dataset. */
	protected XYMultiSeries mDataset;
	/** The multiple series renderer. */
	protected XYMultipleSeriesRenderer mRenderer;
	/** The current scale value. */
	private float mScale;
	/** The current translate value. */
	private float mTranslate;
	/** The canvas center point. */
	private PointF mCenter;
	
	/** The grid color. */
	protected static final int GRID_COLOR = Color.GRAY;
	protected static final int GRID_COLOR_SECONDARY_AXIS = Color.CYAN;
	
	protected static final Align[] label_alignments = new Align[] {Align.RIGHT, Align.LEFT};
	protected static final Axis[] vertical_axis_enums = new Axis[] {Axis.Y_AXIS, Axis.Y_AXIS_SECONDARY};
	protected static final int[] grid_line_colors = new int[] {GRID_COLOR, GRID_COLOR_SECONDARY_AXIS};
	protected static final PathEffect[] grid_line_path_effects = new PathEffect[] {
		null,
		new DashPathEffect(new float[] {
				2*VERTICAL_AXIS_LINE_LABEL_CLEARANCE,
				2*VERTICAL_AXIS_LINE_LABEL_CLEARANCE
			}, 0)};
	
	private String y_format, x_format, y_secondary_format;
	public void setYFormat(String format_string) {
		this.y_format = format_string;
	}

	public String getYFormat() {
		return this.y_format;
	}
	
	public String getYSecondaryFormat() {
		return this.y_secondary_format;
	}

	public void setYSecondaryFormat(String format_string) {
		this.y_secondary_format = format_string;
	}
	
	public void setXFormat(String format_string) {
		this.x_format = format_string;
	}

	public String getXFormat() {
		return this.x_format;
	}


	/**
	 * Builds a new XY chart instance.
	 * 
	 * @param dataset the multiple series dataset
	 * @param renderer the multiple series renderer
	 */
	public XYChart(XYMultiSeries dataset, XYMultipleSeriesRenderer renderer) {
		mDataset = dataset;
		mRenderer = renderer;
	}

	double getPixelsPerUnit(int frame_span, MinMax span) {
		if (span.getSpan() != 0) {
			return frame_span / span.getSpan();
		}
		return 0;
	}

	/**
	 * The graphical representation of the XY chart.
	 * 
	 * @param canvas the canvas to paint to
	 * @param x the top left x value of the view to draw to
	 * @param y the top left y value of the view to draw to
	 * @param width the width of the view to draw to
	 * @param height the height of the view to draw to
	 */
	@Override
	public void draw(Canvas canvas, int width, int height) {

		TextPaint hash_mark_label_paint = new TextPaint();
		hash_mark_label_paint.setTextSize(DEFAULT_HASH_MARK_TEXT_SIZE);
		hash_mark_label_paint.setTypeface(DefaultRenderer.REGULAR_TEXT_FONT);
		hash_mark_label_paint.setAntiAlias(getAntiAliased());



		boolean rotate = Orientation.VERTICAL.equals(mRenderer.getOrientation());
		mScale = (float) (height) / width;
		mTranslate = Math.abs(width - height) / 2;
		if (mScale < 1) {
			mTranslate *= -1;
		}
		mCenter = new PointF(width / 2, height / 2);
		if (rotate) {
			transform(canvas, mRenderer.getOrientation().getAngle(), false);
		}

		MinMax x_span = mRenderer.getXAxisSpan();




		float vertical_axis_hash_mark_width = 0;
		float vertical_secondary_axis_hash_mark_width = 0;
		float horizontal_axis_label_height = 0;
		
		// Measure all y-axis label widths to determine the axis line position
		if (mRenderer.isShowLabels() && mRenderer.getShowYAxis()) {
			vertical_axis_hash_mark_width = measurePrimaryYaxisTickLabels(hash_mark_label_paint);
			
			if (mRenderer.hasSecondaryYAxis()) {
				Log.e(TAG, "Has secondary axis!!!");
				vertical_secondary_axis_hash_mark_width = measureSecondaryYaxisTickLabels(hash_mark_label_paint);
			}
		}

		if (mRenderer.isShowLabels() && mRenderer.getShowXAxis()) {
			horizontal_axis_label_height = measureXaxisTickLabels(hash_mark_label_paint, x_span);
		}


		Rect frame = new Rect(
			(int) Math.ceil(vertical_axis_hash_mark_width),
			0,
			width - (int) Math.ceil(vertical_secondary_axis_hash_mark_width),
			height - (int) horizontal_axis_label_height
		);

		double xPixelsPerUnit = getPixelsPerUnit(frame.width(), x_span);
		double yPixelsPerUnit = getPixelsPerUnit(frame.height(), mRenderer.getYPrimaryAxisSpan());
		double ySecondaryPixelsPerUnit = 0;
		if (mRenderer.hasSecondaryYAxis())
			ySecondaryPixelsPerUnit = getPixelsPerUnit(frame.height(), mRenderer.getYSecondaryAxisSpan());


		if (mRenderer.isShowLabels() || mRenderer.isShowGrid()) {

			List<Double> xLabels = MathHelper.getLabels(x_span, mRenderer.getXLabels());
			if (mRenderer.isShowLabels()) {
				hash_mark_label_paint.setColor(mRenderer.getLabelsColor());
				hash_mark_label_paint.setTextAlign(Align.CENTER);
			}

			drawXLabels(
					xLabels,
					mRenderer.getXTextLabelLocations(),
					canvas,
					hash_mark_label_paint,
					frame.left, frame.top, frame.bottom,
					xPixelsPerUnit,
					x_span.min.doubleValue(),
					DEFAULT_HORIZONTAL_AXIS_HASH_MARK_LENGTH,
					horizontal_axis_label_height - DEFAULT_HORIZONTAL_AXIS_HASH_MARK_LENGTH
					);

			
			drawVerticalAxisLabels(
					canvas,
					frame,
					hash_mark_label_paint,
					yPixelsPerUnit,
					ySecondaryPixelsPerUnit,
					vertical_axis_hash_mark_width,
					vertical_secondary_axis_hash_mark_width);
		}


		// This draws the plot boundaries.
		drawPlotBoundaries(canvas, frame, hash_mark_label_paint);

		// This draws the plot boundaries.
		drawAllDataSets(
				canvas,
				frame,
				hash_mark_label_paint,
				xPixelsPerUnit, yPixelsPerUnit, ySecondaryPixelsPerUnit
		);

		if (rotate) {
			transform(canvas, mRenderer.getOrientation().getAngle(), true);
		}
	}
	
	
	// ========================================================================
	float measureXaxisTickLabels(Paint hash_mark_label_paint, MinMax x_span) {

		
		Rect text_bounds_holder = new Rect();
		
		List<Integer> x_label_heights = new ArrayList<Integer>();
		for (Double xlabel : MathHelper.getLabels(x_span, mRenderer.getXLabels())) {
			String x_label = getLabel( xlabel, Axis.X_AXIS );
			hash_mark_label_paint.getTextBounds(x_label, 0, x_label.length(), text_bounds_holder);
			
			int label_height = text_bounds_holder.height();
			x_label_heights.add(label_height);
		}
		float max_x_label_height = Collections.max(x_label_heights);
		return max_x_label_height + VERTICAL_AXIS_LINE_LABEL_CLEARANCE + DEFAULT_HORIZONTAL_AXIS_HASH_MARK_LENGTH;
	}
	
	// ========================================================================
	float measureSecondaryYaxisTickLabels(Paint hash_mark_label_paint) {

		MinMax y_secondary_span = mRenderer.getYSecondaryAxisSpan();
		
		Rect text_bounds_holder = new Rect();
		
		List<Integer> y_secondary_label_widths = new ArrayList<Integer>();
		for (Double ylabel : MathHelper.getLabels(y_secondary_span, mRenderer.getSecondaryYLabels())) {
			String y_label = getLabel( ylabel, Axis.Y_AXIS_SECONDARY );
			hash_mark_label_paint.getTextBounds(y_label, 0, y_label.length(), text_bounds_holder);
			
			int label_width = text_bounds_holder.width();
			y_secondary_label_widths.add(label_width);
		}
		float max_label_width = Collections.max(y_secondary_label_widths);
		
		return max_label_width + VERTICAL_AXIS_LINE_LABEL_CLEARANCE;
	}
	
	// ========================================================================
	float measurePrimaryYaxisTickLabels(Paint hash_mark_label_paint) {
		MinMax y_span = mRenderer.getYPrimaryAxisSpan();
		Rect text_bounds_holder = new Rect();
		
		List<Integer> y_label_widths = new ArrayList<Integer>();
		for (Double ylabel : MathHelper.getLabels(y_span, mRenderer.getYLabels())) {
			String y_label = getLabel( ylabel, Axis.Y_AXIS );
			hash_mark_label_paint.getTextBounds(y_label, 0, y_label.length(), text_bounds_holder);
			
			int label_width = text_bounds_holder.width();
			y_label_widths.add(label_width);
		}
		float max_label_width = Collections.max(y_label_widths);
		return max_label_width + VERTICAL_AXIS_LINE_LABEL_CLEARANCE;
	}
	
	// ========================================================================
	void drawAllDataSets(
			Canvas canvas,
			Rect frame,
			Paint hash_mark_label_paint,
			double xPixelsPerUnit, double yPixelsPerUnit, double ySecondaryPixelsPerUnit) {
//			double xPixelsPerUnit, double ySecondaryPixelsPerUnit) {

		MinMax x_span = mRenderer.getXAxisSpan();
		
		for (int i=0; i < mDataset.getSeriesCount(); i++) {
			XYSeries series = mDataset.getSeriesAt(i);
			if (series.getItemCount() == 0) {
				continue;
			}

			SimpleSeriesRenderer seriesRenderer = mRenderer.getSeriesRendererAt(i);

			MinMax vertical_series_span;
			double vertical_pixels_per_unit;
			if (mRenderer.hasSecondaryYAxis() && seriesRenderer.getUsesSecondaryAxis()) {
				vertical_series_span = mRenderer.getYSecondaryAxisSpan();
				vertical_pixels_per_unit = ySecondaryPixelsPerUnit;
			} else {
				vertical_series_span = mRenderer.getYPrimaryAxisSpan();
				vertical_pixels_per_unit = yPixelsPerUnit;
			}
			
			List<PointF> points = new ArrayList<PointF>();
			for (int j=0; j < series.getItemCount(); j++) {
				PointF point = new PointF(
						(float) (frame.left + xPixelsPerUnit * (series.getX(j).doubleValue() - x_span.min.doubleValue())),
						(float) (frame.bottom - vertical_pixels_per_unit * (series.getY(j).doubleValue() - vertical_series_span.min.doubleValue()))	
					);
				points.add( point );
			}
			
			drawSeries(canvas,
					hash_mark_label_paint,
					points,
					seriesRenderer,
					(float) xPixelsPerUnit,
					(float) vertical_pixels_per_unit,
					Math.min(
						frame.bottom,
						(float) (frame.bottom + vertical_pixels_per_unit * vertical_series_span.min.doubleValue())),
					i);
			
			// Render glyphs atop the chart if necessary
			if (isRenderPoints(seriesRenderer)) {
				ScatterChart pointsChart = new ScatterChart(mDataset, mRenderer);
				pointsChart.drawSeries(canvas, hash_mark_label_paint, points, seriesRenderer,
						(float) xPixelsPerUnit,
						(float) vertical_pixels_per_unit,
						0, i);
			}
			
			hash_mark_label_paint.setTextSize( DEFAULT_HASH_MARK_TEXT_SIZE );
			if (Orientation.HORIZONTAL.equals(mRenderer.getOrientation())) {
				hash_mark_label_paint.setTextAlign(Align.CENTER);
			} else {
				hash_mark_label_paint.setTextAlign(Align.LEFT);
			}
			if (mRenderer.isDisplayChartValues()) {
				drawChartValuesText(canvas, series, hash_mark_label_paint, points, i);
			}
		}
	}
	
	// ========================================================================
	void drawPlotBoundaries(Canvas canvas, Rect frame, Paint hash_mark_label_paint) {
		if (mRenderer.isShowAxes()) {
			hash_mark_label_paint.setColor(mRenderer.getAxesColor());
			
			// Draw the bottom (horizontal) line
			canvas.drawLine(frame.left, frame.bottom, frame.right, frame.bottom, hash_mark_label_paint);

			// Draw the left (vertical) line
			canvas.drawLine(frame.left, frame.top, frame.left, frame.bottom, hash_mark_label_paint);

			// Optionally draw the right (vertical) line
			if (mRenderer.hasSecondaryYAxis())
				canvas.drawLine(frame.right, frame.top, frame.right, frame.bottom, hash_mark_label_paint);
		}
	}

	// ========================================================================
	
	
	/**
	 * The graphical representation of the series values as text.
	 * 
	 * @param canvas the canvas to paint to
	 * @param series the series to be painted
	 * @param paint the paint to be used for drawing
	 * @param points the array of points to be used for drawing the series
	 * @param seriesIndex the index of the series currently being drawn
	 */
	protected void drawChartValuesText(Canvas canvas, XYSeries series, Paint paint, List<PointF> points,
			int seriesIndex) {
		int k=0;
		for (PointF point : points) {
			drawText(
				canvas,
				getLabel(k, Axis.Y_AXIS),
				point.x,
				point.y - 3.5f,	// FIXME Magic number
				paint,
				0);
			k++;
		}
	}

	/**
	 * The graphical representation of a text, to handle both HORIZONTAL and
	 * VERTICAL orientations and extra rotation angles.
	 * 
	 * @param canvas the canvas to paint to
	 * @param text the text to be rendered
	 * @param x the X axis location of the text
	 * @param y the Y axis location of the text
	 * @param paint the paint to be used for drawing
	 * @param extraAngle the array of points to be used for drawing the series
	 */
	protected void drawText(Canvas canvas, String text, float x, float y, Paint paint, int extraAngle) {
		int angle = -mRenderer.getOrientation().getAngle() + extraAngle;
		if (angle != 0) {
			// canvas.scale(1 / mScale, mScale);
			canvas.rotate(angle, x, y);
		}
		canvas.drawText(text, x, y, paint);
		if (angle != 0) {
			canvas.rotate(-angle, x, y);
			// canvas.scale(mScale, 1 / mScale);
		}
	}

	/**
	 * Transform the canvas such as it can handle both HORIZONTAL and VERTICAL
	 * orientations.
	 * 
	 * @param canvas the canvas to paint to
	 * @param angle the angle of rotation
	 * @param inverse if the inverse transform needs to be applied
	 */
	private void transform(Canvas canvas, float angle, boolean inverse) {
		if (inverse) {
			canvas.scale(1 / mScale, mScale);
			canvas.translate(mTranslate, -mTranslate);
			canvas.rotate(-angle, mCenter.x, mCenter.y);
		} else {
			canvas.rotate(angle, mCenter.x, mCenter.y);
			canvas.translate(-mTranslate, mTranslate);
			canvas.scale(mScale, 1 / mScale);
		}
	}

	protected String getLabel(Number label, Axis axis) {
		String format_string = null;
		
		switch (axis) {
		case Y_AXIS:
			format_string = getYFormat();
			break;
		case X_AXIS:
			format_string = getXFormat();
			break;
		case Y_AXIS_SECONDARY:
			format_string = getYSecondaryFormat();
			break;
		}
		
		if (format_string != null)
			return String.format(format_string, label);

		String text = "";
		if (label.intValue() == label.doubleValue()) {
			text = label.intValue() + "";
		} else {
			text = label + "";
		}
		return text;
	}

	
	
	
	protected void drawVerticalAxisLabels(
			Canvas canvas,
			Rect frame,
			Paint hash_mark_label_paint,
			double yPixelsPerUnit,
			double ySecondaryPixelsPerUnit,
			float vertical_axis_hash_mark_width,
			float vertical_secondary_axis_hash_mark_width) {
		
		

		MinMax y_span = mRenderer.getYPrimaryAxisSpan();
		
		List<Double> yLabels = MathHelper.getLabels(y_span, mRenderer.getYLabels());

		List<List<Double>> vertical_axis_datasets = new ArrayList<List<Double>>();
		List<MinMax> vertical_axis_spans = new ArrayList<MinMax>();
		vertical_axis_datasets.add(yLabels);
		vertical_axis_spans.add( y_span );
		if (mRenderer.hasSecondaryYAxis()) {
			MinMax secondary_span = mRenderer.getYSecondaryAxisSpan();
			vertical_axis_datasets.add( MathHelper.getLabels(secondary_span, mRenderer.getSecondaryYLabels()) );
			vertical_axis_spans.add( secondary_span );
		}

		double[] vertical_pixels_per_unit = new double[] {yPixelsPerUnit, ySecondaryPixelsPerUnit};
		for (int vertical_axis_index=0; vertical_axis_index<vertical_axis_datasets.size(); vertical_axis_index++) {
//			List<Double> vertical_labels = vertical_axis_datasets.get(0);
			List<Double> vertical_labels = vertical_axis_datasets.get(vertical_axis_index);
			
			MinMax label_span = vertical_axis_spans.get(vertical_axis_index);
			
			hash_mark_label_paint.setTextAlign( label_alignments[vertical_axis_index] );
			for (int i = 0; i < vertical_labels.size(); i++) {
				double label = vertical_labels.get(i);
				float label_value = (float) (frame.bottom - vertical_pixels_per_unit[vertical_axis_index] * (label - label_span.min.doubleValue()));


				float grid_line_startx, grid_line_stopx, hash_mark_startx, hash_mark_stopx;
				float label_x_offset;

				if (vertical_axis_index == 0) {
					
					grid_line_startx = frame.left;
					grid_line_stopx = frame.right;
					hash_mark_startx = grid_line_startx - vertical_axis_hash_mark_width;
					hash_mark_stopx = grid_line_startx;

					label_x_offset = frame.left - VERTICAL_AXIS_LINE_LABEL_CLEARANCE;

				} else {

					grid_line_startx = frame.left;
					grid_line_stopx = frame.right;
					hash_mark_startx = grid_line_stopx;
					hash_mark_stopx = grid_line_stopx + vertical_secondary_axis_hash_mark_width;

					label_x_offset = frame.right + VERTICAL_AXIS_LINE_LABEL_CLEARANCE;
				}

				if (mRenderer.isShowLabels()) {
					hash_mark_label_paint.setColor( grid_line_colors[vertical_axis_index] );
//					hash_mark_label_paint.setColor(mRenderer.getLabelsColor());	// XXX
					canvas.drawLine(hash_mark_startx, label_value, hash_mark_stopx, label_value, hash_mark_label_paint);

					String label_string = getLabel( label, vertical_axis_enums[vertical_axis_index] );
					hash_mark_label_paint.measureText(label_string);
					drawText(
						canvas,
						getLabel(label, vertical_axis_enums[vertical_axis_index]),
						label_x_offset,
						label_value - VERTICAL_AXIS_LABEL_HASH_MARK_CLEARANCE,
						hash_mark_label_paint, 0);
				}

				if (mRenderer.isShowGrid() && mRenderer.isShowGridHorizontalLines()) {
					hash_mark_label_paint.setColor( grid_line_colors[vertical_axis_index] );
					hash_mark_label_paint.setPathEffect( grid_line_path_effects[vertical_axis_index] );
					canvas.drawLine(grid_line_startx,
							label_value,
							grid_line_stopx,
							label_value,
							hash_mark_label_paint);

					hash_mark_label_paint.setPathEffect(null);
				}
			}
		}
	}
	
	
	
	
	/**
	 * The graphical representation of the labels on the X axis.
	 * 
	 * @param xLabels the X labels values
	 * @param xTextLabelLocations the X text label locations
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param left the left value of the labels area
	 * @param top the top value of the labels area
	 * @param bottom the bottom value of the labels area
	 * @param xPixelsPerUnit the amount of pixels per one unit in the chart labels
	 * @param minX the minimum value on the X axis in the chart
	 */
	protected void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
			Paint paint, int left, int top, int bottom, double xPixelsPerUnit, double minX, float hash_mark_height, float max_text_height) {
		int length = xLabels.size();
		boolean showLabels = mRenderer.isShowLabels();
		for (int i = 0; i < length; i++) {
			double label = xLabels.get(i);
			float xLabel = (float) (left + xPixelsPerUnit * (label - minX));
			if (showLabels) {
				paint.setColor(mRenderer.getLabelsColor());
				canvas.drawLine(xLabel, bottom, xLabel, bottom + hash_mark_height, paint);	// FIXME Magic numbers
				drawText(canvas, getLabel(label, Axis.X_AXIS), xLabel, bottom + hash_mark_height + max_text_height, paint, 0);
			}
			if (mRenderer.isShowGrid() && mRenderer.isShowGridVerticalLines()) {
				paint.setColor(GRID_COLOR);
				canvas.drawLine(xLabel, bottom, xLabel, top, paint);
			}
		}
		if (showLabels) {
			paint.setColor(mRenderer.getLabelsColor());
			for (Double location : xTextLabelLocations) {
				float xLabel = (float) (left + xPixelsPerUnit * (location.doubleValue() - minX));
				canvas.drawLine(xLabel, bottom, xLabel, bottom + 4, paint);	// FIXME Magic numbers
				drawText(canvas, mRenderer.getXTextLabel(location), xLabel, bottom + 12, paint, 0);
			}
		}
	}

	/**
	 * The graphical representation of a series.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param points the array of points to be used for drawing the series
	 * @param seriesRenderer the series renderer
	 * @param yAxisValue the minimum value of the y axis
	 * @param seriesIndex the index of the series currently being drawn
	 */
	public abstract void drawSeries(Canvas canvas, Paint paint, List<PointF> points,
			SimpleSeriesRenderer seriesRenderer,
			float xScale, float yScale,	// FIXME These probably aren't necessary
			float yAxisValue, int seriesIndex);

	/**
	 * Returns if the chart should display the points as a certain shape.
	 * 
	 * @param renderer the series renderer
	 */
	public boolean isRenderPoints(SimpleSeriesRenderer renderer) {
		return false;
	}
}
