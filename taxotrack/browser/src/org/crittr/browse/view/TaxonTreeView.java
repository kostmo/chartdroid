package org.crittr.browse.view;

import org.crittr.browse.Market;
import org.crittr.browse.activity.TaxonNavigatorRadial.TaxonNode;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.provider.DatabaseTaxonomy.BasicTaxon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TaxonTreeView extends View {
	
	static final String TAG = Market.DEBUG_TAG; 

	static final float FRACTIONAL_ROTATION_OFFSET = 1/8f;
	
	
    Map<Long, BasicTaxon> taxon_map;
    Map<Long, TaxonNode> taxon_tree_map;

    Map<Long, Float> rotation_offsets = new HashMap<Long, Float>();
    TaxonNode root, current_root;

    int intrinsic_width = 75;
    int intrinsic_height = 400;
    boolean has_been_measured = false;
    TaxonNode queued_node_sort;

	float major_stroke_width = 10;

	Paint my_paint, text_paint;
	
	int color_cycle_depth_period = 8;
	

	long active_node_tsn = Constants.INVALID_TSN;
	
	
	
	private static final int LONGPRESS_DURATION_MS = ViewConfiguration.getLongPressTimeout();
	private long lonpress_begin_ms;
	private boolean longpressing_active = false;
	
	Context context;
	GestureDetector gesture_detector;
    // ========================================================================
    public TaxonTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        
        my_paint = new Paint();
		my_paint.setAntiAlias(true);
		

		
		text_paint = new TextPaint(this.my_paint);
		text_paint.setColor(Color.LTGRAY);
		text_paint.setStyle(Style.FILL);
		text_paint.setTextAlign(Align.CENTER);
		
		
		
		gesture_detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

			@Override
			public boolean onDown(MotionEvent e) {
				longpressing_active = true;
				lonpress_begin_ms = SystemClock.elapsedRealtime();
				selectTaxonNodeByTouch(e);
				return true;
			}


			@Override
			public void onLongPress(MotionEvent e) {
//				activateSelection();	// FIXME
		    	longpressing_active = false;
		    	showContextMenu();
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

				selectTaxonNodeByTouch(e2);
				return true;
			}
		});
    }
    
    

    // ========================================================================
    public TaxonNode getCurrentRoot() {
    	return this.current_root;
    }

    // ========================================================================
    public void newRoot(TaxonNode new_root) {
    	
    	TaxonNode old_root = this.current_root;
    	this.current_root = new_root;
    	
    	if (has_been_measured) {
    		sortTextNonMonotonically(this.current_root);
    	} else {
    		queued_node_sort = this.current_root;
    	}
    	
		if (taxon_map.containsKey(new_root.tsn)) {
			String new_title = taxon_map.get(new_root.tsn).name;
			((Activity) this.context).setTitle(new_title);
		}
		
    	if (new_root.getChildren().contains(old_root)) {
    		this.active_node_tsn = old_root.tsn;
    	} else if (new_root.getChildren().size() > 0) {
    		this.active_node_tsn = ((TaxonNode) new_root.getChildren().get(0)).tsn;
    	}
    	
    	invalidate();
    }
    
    
    

    // ========================================================================
    @Override
	protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
    	
    	int position = this.current_root.getChildIndex(active_node_tsn);
    	ContextMenu.ContextMenuInfo info = new AdapterView.AdapterContextMenuInfo(this, position, active_node_tsn);

    	return info;
    }
    
    
    // ========================================================================
    public void setTree(Map<Long, BasicTaxon> taxon_map, Map<Long, TaxonNode> taxon_tree_map, TaxonNode root) {
    	this.taxon_map = taxon_map;
    	this.taxon_tree_map = taxon_tree_map;
    	this.root = root;
    	
    	this.current_root = this.root;

    	rotation_offsets.clear();
    	for (long tsn : taxon_map.keySet())
    		rotation_offsets.put(tsn, (float) Math.random());
    	
    	invalidate();
    }

    // ========================================================================
    void drawSubTree(Canvas canvas, TaxonNode node, int depth) {
    	
//    	Log.d(TAG, "In drawSubTree() with depth: " + depth);
    	
    	
    	float level_scale = (float) Math.exp(-depth/2f);
    	float tree_scale = Math.min(getWidth(), getHeight())/4f * level_scale;
		float circle_radius = tree_scale/5;

    	
    	int color = Color.WHITE;
    	if (node.tsn != active_node_tsn) {
        	float color_frac = depth / (float) color_cycle_depth_period;
    		color = Color.HSVToColor(new float[] {color_frac*360, 1, 1});
    	}

    	Paint paint = new Paint(this.my_paint);
    	paint.setColor(color);
		paint.setAlpha(0xB0);
		paint.setStrokeWidth(major_stroke_width * level_scale);

    	
    	Iterator<? extends TreeNode> node_iterator = node.iterator();
    	int i=0;
    	
    	/*
    	Log.d(TAG, "rotation_offsets: " + rotation_offsets);
    	Log.d(TAG, "node: " + node);
    	Log.d(TAG, "node.tsn: " + node.tsn);
    	Log.d(TAG, "rotation_offsets.get(node.tsn): " + rotation_offsets.get(node.tsn));
		*/
    	
    	float random_rotation_offset = FRACTIONAL_ROTATION_OFFSET;
    	if (depth > 0 && rotation_offsets.containsKey(node.tsn))
    		random_rotation_offset = rotation_offsets.get(node.tsn);
    	
    	while (node_iterator.hasNext()) {

    		float arc_fraction = random_rotation_offset + i/(float) node.getChildCount();
        	float cx = tree_scale * (float) Math.cos(arc_fraction * 2*Math.PI);
        	float cy = tree_scale * (float) Math.sin(arc_fraction * 2*Math.PI);

        	
        	canvas.save();
        	canvas.translate(cx, cy);
    		
    		TaxonNode child_node = (TaxonNode) node_iterator.next();
    		drawSubTree(
    				canvas,
    				child_node,
    				depth+1);
    		    		
    		if (depth == 0) {
	    		canvas.rotate(arc_fraction*360);

	    		String label = taxon_map.get(child_node.tsn).name;
    			Rect text_bounds = new Rect();
    			text_paint.getTextBounds(label, 0, label.length(), text_bounds);
	    		canvas.translate(circle_radius + text_bounds.exactCenterX(), -text_paint.ascent()/2);

	    		if (arc_fraction % 1 < 0.5) {

		    		canvas.translate(0, text_paint.ascent()/2);
		    		canvas.rotate(180);
		    		canvas.translate(0, -text_paint.ascent()/2);
	    		}
	    		
	    		/*
	    		// XXX This slows down rendering considerably
	    		text_paint.setColor(Color.BLACK);
	    		text_paint.setStyle(Style.STROKE);
	    		text_paint.setStrokeWidth(2);	// FIXME
	    		canvas.drawText(label, 0, 0, text_paint);
	    		*/
	    		text_paint.setStyle(Style.FILL);
	    		int text_color = (child_node.tsn == active_node_tsn) ? Color.WHITE : Color.LTGRAY;
	    		// Fade out the "blue" component based on longpress time
	    		if (child_node.tsn == active_node_tsn && longpressing_active) {
		    		long time_depressed = SystemClock.elapsedRealtime() - lonpress_begin_ms;
		    		Log.i(TAG, "Time depressed: " + time_depressed + "(" + (time_depressed / (float) LONGPRESS_DURATION_MS) + ")");
		    		text_color &= 0xffffff00 | (0xff - Math.min(0xff, time_depressed * 0xff / LONGPRESS_DURATION_MS));
	    		}
	    		
	    		text_paint.setColor(text_color);
	    		canvas.drawText(label, 0, 0, text_paint);
    		}

        	canvas.restore();
        	
        	// Draws post-traversal
        	canvas.drawLine(0, 0, cx, cy, paint);
        	
    		i++;
    	}
    	
    	
    	
    	// Draws post-traversal
    	canvas.drawCircle(0, 0, circle_radius, paint);
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

    	has_been_measured = true;
    	
//		Log.d(TAG, "OLD - Width: " + oldw + "; Height: " + oldh);
//		Log.d(TAG, "NEW - Width: " + w + "; Height: " + h);
    	
		float window_scale_factor = Math.min(w, h);
		float target_text_size = window_scale_factor/20;
		text_paint.setTextSize(target_text_size);
		
		if (queued_node_sort != null) {
    		sortTextNonMonotonically(queued_node_sort);
			queued_node_sort = null;
		}
    }

    // ========================================================================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


		// Draw the outline
		my_paint.setStyle(Style.STROKE);
		my_paint.setStrokeCap(Cap.ROUND);
		my_paint.setStrokeWidth(major_stroke_width);


    	canvas.translate(getWidth()/2, getHeight()/2);
    	canvas.rotate(90);
		
		drawSubTree(canvas, this.current_root, 0);
//    	drawSubTree(canvas, this.root.iterator().next(), 0, 0);
		
		if (longpressing_active) invalidate();
    }
    
    

    // ========================================================================
    // TODO: Increase this value depending on the time held down
    int touch_detection_depth = 0;
    
    class MyPointF extends PointF {
    	public MyPointF(float x, float y) {
    		super(x, y);
    	}
    	
    	public PointF delta(PointF point) {
    		PointF result = new PointF();
    		result.set(this);
    		result.negate();
    		result.offset(point.x, point.y);
    		return result;
    	}
    }
    
    // ========================================================================
    public boolean selectTaxonNodeByTouch(MotionEvent event) {
    	
    	MyPointF level_center = new MyPointF(getWidth()/2f, getHeight()/2f);
    	MyPointF absolute_touch_point = new MyPointF(event.getX(), event.getY());
    	PointF touch_delta = level_center.delta(absolute_touch_point);
//    	Log.d(TAG, "Touched at: (" + touch_delta.x + ", " + touch_delta.y + ")");
    	
    	// XXX The node-drawing arc goes clockwise (with 0degress=straight down),
    	// so we invert the x-axis (since arctangent uses a counter-clockwise convention)
    	float radians = (float) Math.atan2(-touch_delta.x, touch_delta.y);
    	
		// XXX The 90-degree view rotation seems to be already accounted for...
    	float touch_arc_fraction = radians / (float) (2*Math.PI);
//    	Log.d(TAG, "Touch arc position: " + touch_arc_fraction);
    	
    	long closest_tsn = Constants.INVALID_TSN;
    	float min_arc_distance = Float.POSITIVE_INFINITY;
    	
    	TaxonNode node = this.current_root;
    	Iterator<? extends TreeNode> node_iterator = node.iterator();

    	float random_rotation_offset = FRACTIONAL_ROTATION_OFFSET;
    	// XXX We don't randomly rotate the active node.
    	/*
    	if (rotation_offsets.containsKey(node.tsn))
    		random_rotation_offset = rotation_offsets.get(node.tsn);
    	*/
    	int i=0;
    	while (node_iterator.hasNext()) {

    		TaxonNode child_node = (TaxonNode) node_iterator.next();

    		float node_arc_fractional_position = random_rotation_offset + i/(float) node.getChildCount();
        	float raw_sweep_delta = node_arc_fractional_position - touch_arc_fraction;
        	float modded_sweep_delta = (raw_sweep_delta + 1) % 1;
        	float shifted_sweep_delta = modded_sweep_delta;
        	if (shifted_sweep_delta > 0.5) shifted_sweep_delta -= 1;
    		float absolute_shifted_sweep_delta = Math.abs( shifted_sweep_delta );
    		if (absolute_shifted_sweep_delta < min_arc_distance) {
    			closest_tsn = child_node.tsn;
    			min_arc_distance = absolute_shifted_sweep_delta;
    		}
    		
    		i++;
    	}

    	if (this.active_node_tsn == closest_tsn)
    		return false;
    	
    	this.active_node_tsn = closest_tsn;
    	invalidate();
    	return true;
    }

    // ========================================================================
    void sortTextNonMonotonically(TaxonNode node) {
    	
    	List<? extends TreeNode> list_uncast = node.getChildren();
    	List<TaxonNode> list = (List<TaxonNode>) list_uncast;
    	Collections.sort( list, new Comparator<TaxonNode>() {
			@Override
			public int compare(TaxonNode object1, TaxonNode object2) {
				String name1 = taxon_map.get(object1.tsn).name;
				String name2 = taxon_map.get(object2.tsn).name;
//				return new Integer(name1.length()).compareTo(name2.length());
				
				Rect bounds = new Rect();
				text_paint.getTextBounds(name1, 0, name1.length(), bounds);
				Integer width1 = bounds.width();
				text_paint.getTextBounds(name2, 0, name2.length(), bounds);
				Integer width2 = bounds.width();
				return width1.compareTo(width2);
			}
    	});
    	
    	class FloatCarrier implements Comparable<FloatCarrier> {
    		float height;
    		int index;
    		FloatCarrier(float height, int index) {
    			this.height = height;
    			this.index = index;
    		}
    		
			@Override
			public int compareTo(FloatCarrier another) {
				return new Float(this.height).compareTo(another.height);
			}
    	}
    	
    	List<FloatCarrier> sinusoid_heights = new ArrayList<FloatCarrier>();
    	for (int i=0; i<list.size(); i++) {
    		// XXX Offset by 90 degrees and another 45 for optimally placing square corners
    		double radians = 2*Math.PI * ( i / (float) list.size() + 1/4f + FRACTIONAL_ROTATION_OFFSET );
    		double height = rectSideDistance(radians, getWidth(), getHeight());
    		sinusoid_heights.add( new FloatCarrier((float) height, i) );
    	}
    	Collections.sort(sinusoid_heights);
    	
    	TaxonNode[] final_list = new TaxonNode[list.size()];
    	for (int i=0; i<list.size(); i++) {
    		final_list[sinusoid_heights.get(i).index] = (TaxonNode) list.get(i);
    	}
    	
    	list.clear();
    	list.addAll(Arrays.asList(final_list));
    }

    // ========================================================================
    double rectSideDistance(double radians, int w, int h) {
    	double boundary_radians = Math.atan(h/(float) w);
    	double value = Math.abs(Math.atan(Math.tan(radians))) < boundary_radians ? w/Math.cos(radians) : h/Math.sin(radians);
    	
    	double absolute_value = Math.abs(value);
//    	Log.e(TAG, "" + absolute_value);
    	return absolute_value;
    }
    
    // ========================================================================
    public boolean backOut() {
    	
    	if (taxon_map.containsKey(this.current_root.tsn)) {
    		long parent_tsn = taxon_map.get(this.current_root.tsn).parent;
//    	if (taxon_tree_map.containsKey(parent_tsn)) {
    		newRoot(taxon_tree_map.get(parent_tsn));
    		return true;
    	}
    	return false;
    }

    // ========================================================================
    public void rotateSelected(boolean forward) {
    	
//    	Log.d(TAG, "Rotating selection.");
    	
    	if (!taxon_map.containsKey(active_node_tsn)) return;
    	
    	long parent_tsn = taxon_map.get(active_node_tsn).parent;
    	if (taxon_tree_map.containsKey(parent_tsn)) {
    		
        	List<? extends TreeNode> list_uncast = taxon_tree_map.get(parent_tsn).getChildren();
        	List<TaxonNode> children = (List<TaxonNode>) list_uncast;
    		int idx = children.indexOf( taxon_tree_map.get(active_node_tsn) );
    		if (idx >= 0) {
    			idx = (idx + (forward ? 1 : -1) + children.size()) % children.size();
    			active_node_tsn = children.get(idx).tsn;
    			
    			invalidate();
    		}
    	}
    }

    // ========================================================================
    public boolean activateSelection() {
    	
    	if (taxon_tree_map.containsKey(this.active_node_tsn)) {
    		newRoot( taxon_tree_map.get(this.active_node_tsn) );

    		return true;
    	}
    	return false;
    }
    
    // ========================================================================
    // TODO
    public boolean onTrackballEvent(MotionEvent event) {
    	return false;
    }
    
    
    // ========================================================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {

    	Log.d(TAG, "Processing onTouchEvent() at " + event.getEventTime());
    	
    	if (event.getAction() != MotionEvent.ACTION_DOWN) longpressing_active = false;
    	return gesture_detector.onTouchEvent(event);
    	
    }
}