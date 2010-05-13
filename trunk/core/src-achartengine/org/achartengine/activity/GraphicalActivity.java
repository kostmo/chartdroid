/**
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
package org.achartengine.activity;

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.activity.prefs.ChartDisplayPreferences;
import com.googlecode.chartdroid.core.ColumnSchema;
import com.googlecode.chartdroid.core.IntentConstants;
import com.googlecode.chartdroid.provider.ImageFileContentProvider;

import org.achartengine.renderer.AxesManager;
import org.achartengine.util.SemaphoreHost;
import org.achartengine.view.FlowLayout;
import org.achartengine.view.PlotView;
import org.achartengine.view.chart.AbstractChart;
import org.achartengine.view.chart.PointStyle;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An activity that encapsulates a graphical view of the chart.
 */
abstract public class GraphicalActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, SemaphoreHost {

	protected static final String TAG = "ChartDroid";
	
	
	static final boolean ALLOW_JPEG_SCREENSHOTS = false;
	
	static public class AxesContainer {
		public List<List<Number>> x_axis_series, y_axis_series;
		List<List<String>> datam_labels;
		String[] titles;
	}
	
	
	
	// This is what fraction of the data span the axes limits will be padded by
	public static float HEADROOM_FOOTROOM_FRACTION = 0.1f;
	// For bar charts, however, the padding must be equal to one the equidistant
	// intervals between the points on both sides.
	

	/** The encapsulated graphical view. */
	protected PlotView mView;

	/** The chart to be drawn. */
	protected AbstractChart mChart;


	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	public static PointStyle[] DEFAULT_STYLES = {
			PointStyle.CIRCLE, PointStyle.DIAMOND,
			PointStyle.TRIANGLE, PointStyle.SQUARE };
	public static int[] DEFAULT_COLORS = {
			Color.BLUE, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.CYAN };

	

	DataQueryTask data_query_task;
	
	
	abstract protected AbstractChart generateChartFromContentProvider(Uri intent_data) throws IllegalArgumentException;
	abstract protected int getTitlebarIconResource();
	abstract protected int getLayoutResourceId();
	abstract protected void postChartPopulationCallback();
	
	// TODO: Implement for Donut chart
	abstract protected List<DataSeriesAttributes> getSeriesAttributesList(AbstractChart chart);

	
	   
	// ====================================================================
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

    	// In the general case, do nothing.
    }
	
	
	// ========================================================================
	public static class DataSeriesAttributes {
		public String title;
		public int color;
	}

	// ========================================================================
	public class DataQueryTask extends AsyncTask<Void, Void, AbstractChart> {

		private Uri chart_data_uri;
		private String error_message;
		DataQueryTask(Uri chart_data_uri) {
			this.chart_data_uri = chart_data_uri;
		}

		@Override
		protected void onPreExecute() {
			incSemaphore();
		}

		@Override
		protected AbstractChart doInBackground(Void... params) {
			try {
				return generateChartFromContentProvider(this.chart_data_uri);
			} catch (IllegalArgumentException e) {
				this.error_message = e.getLocalizedMessage();
			}
			return null;
		}

		@Override
		protected void onPostExecute(AbstractChart chart) {

			decSemaphore();
			
			if (error_message != null) {
				Toast.makeText(GraphicalActivity.this, "There are no series!", Toast.LENGTH_LONG).show();
				Log.e(TAG, "Error in chart; finishing activity. Chart: " + chart);
				finish();
				return;
			}
			
			mChart = chart;
			mView.setChart(mChart);
			
			postChartPopulationCallback();
		}
	}

	// ========================================================================
	public void populateLegend(FlowLayout predicate_layout, List<DataSeriesAttributes> series_attributes_list) {
		populateLegend(predicate_layout, series_attributes_list, false);
	}

	// ========================================================================
	public void populateLegend(FlowLayout flow_layout, List<DataSeriesAttributes> series_attributes_list, boolean donut_style) {
		
		FlowLayout.LayoutParams lp = new FlowLayout.LayoutParams(5, 1);
		flow_layout.setFlowLayoutParams(lp);

		int i=0;
		for (DataSeriesAttributes series : series_attributes_list) {

			Button b = new Button(this);
			b.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			b.setGravity(Gravity.CENTER_VERTICAL);
			b.setText( series.title );
			b.setBackgroundDrawable(null);
			b.setPadding(0, 0, 0, 0);

			
			int icon_width = 16;
			Drawable icon;
			int color;
			if (donut_style) {
				color = Color.WHITE;
				PaintDrawable swatch = new PaintDrawable( color );
				int width = icon_width * (series_attributes_list.size() - i)/series_attributes_list.size();
				swatch.setIntrinsicWidth(width);
				swatch.setIntrinsicHeight(width);
				swatch.setCornerRadius(width/2f);
				icon = swatch;
			} else {
				color = series.color;
				PaintDrawable swatch = new PaintDrawable( color );
				swatch.setIntrinsicWidth(icon_width);
				swatch.setIntrinsicHeight(icon_width);
				swatch.setCornerRadius(icon_width/4);
				icon = swatch;
			}
			
			b.setTextColor( color );

			b.setCompoundDrawablePadding(3);
			b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			flow_layout.addView(b);
			
			i++;
		}
	}

	// ========================================================================
	@Override
	protected void onCreate(Bundle savedInstanceState) {

	    getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		if (getIntent().getBooleanExtra(IntentConstants.EXTRA_FULLSCREEN, false))
			getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		

		String title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		if (title == null) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		} else if (title.length() > 0) {
			setTitle(title);
		}

		setContentView( getLayoutResourceId() );
	    getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, getTitlebarIconResource());


		this.mView = (PlotView) findViewById(R.id.chart_view);
		
		((TextView) findViewById(R.id.chart_title_placeholder)).setText(title);

		this.data_query_task = new DataQueryTask( getIntent().getData() );
		this.data_query_task.execute();
	}

	// ========================================================================
	protected void assignChartLabels(List<String> axis_labels, AxesManager renderer) {
		
		String chart_title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
		
		String x_label = "X-Axis";
		String y_label = "Y-Axis";
		if (axis_labels != null) {
			if (axis_labels.size() - 1 >= ColumnSchema.X_AXIS_INDEX)
				x_label = axis_labels.get( ColumnSchema.X_AXIS_INDEX );
			
			if (axis_labels.size() - 1 >= ColumnSchema.Y_AXIS_INDEX)
				y_label = axis_labels.get( ColumnSchema.Y_AXIS_INDEX );
		}
 
		Log.d(TAG, "X LABEL: " + x_label);
		Log.d(TAG, "Y LABEL: " + y_label);
		Log.d(TAG, "chart_title: " + chart_title);

		org.achartengine.ChartGenHelper.setChartSettings(renderer, chart_title, x_label, y_label, Color.LTGRAY, Color.GRAY);
	}

	// ========================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_chart, menu);

		return true;
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_share_chart:
		{
			View view = findViewById(R.id.full_chart_view);
			
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			boolean allow_transparency = settings.getBoolean(ChartDisplayPreferences.PREFKEY_SCREENSHOT_TRANSPARENCY, false);

			Point preferred_dimensions = new Point(view.getWidth(), view.getHeight());
			if (settings.getBoolean(ChartDisplayPreferences.PREFKEY_SCREENSHOT_ALLOW_CUSTOM_SIZE, false)) {
				int preferred_width = Integer.parseInt(settings.getString(ChartDisplayPreferences.PREFKEY_SCREENSHOT_WIDTH, "800"));
				int preferred_height = preferred_width * 2 / 3;
				preferred_dimensions.set(preferred_width, preferred_height);
			}

			Bitmap image = Bitmap.createBitmap(
					preferred_dimensions.x,
					preferred_dimensions.y,
					allow_transparency ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

			
			// Next create a canvas with the bitmap and pass that into the draw() method of the view. This will ask the view to draw it's contents onto the canvas and therefore the associated bitmap.
			view.draw(new Canvas(image));

			Uri uri = null;
			if (ALLOW_JPEG_SCREENSHOTS) {
				// Next insert the image into the Media library. This returns a URI that refers to the stored image and can be reused across applications. In our case we could pass this URL to the MMS application to have it embed the image in a message.  I'll post some sample code of how to send this image via MMS shortly.
				uri = Uri.parse(Images.Media.insertImage(getContentResolver(), image, getIntent().getStringExtra(Intent.EXTRA_TITLE), null));
			} else {
				try {
					uri = ImageFileContentProvider.storeTemporaryImage(this, image);
				} catch (IOException e) {
					uri = Uri.parse(Images.Media.insertImage(getContentResolver(), image, getIntent().getStringExtra(Intent.EXTRA_TITLE), null));
				}
			}
			
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("image/*");
			i.putExtra(Intent.EXTRA_STREAM, uri);
			startActivity(i);
			
			return true;
		}
		case R.id.menu_fullscreen:
		{
			Intent i = getIntent();
			boolean fullscreen = getIntent().getBooleanExtra(IntentConstants.EXTRA_FULLSCREEN, false);
			i.putExtra(IntentConstants.EXTRA_FULLSCREEN, !fullscreen);
			startActivity(i);
			finish();
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}
    
	// ========================================================================
	@Override
	public void incSemaphore() {

		setProgressBarIndeterminateVisibility(true);
		retrieval_tasks_semaphore.incrementAndGet();
	}

	// ========================================================================
	@Override
	public void decSemaphore() {

		boolean still_going = retrieval_tasks_semaphore.decrementAndGet() > 0;
		setProgressBarIndeterminateVisibility(still_going);
	}

	// ========================================================================
	@Override
	protected void onDestroy() {

		if (data_query_task != null)
			data_query_task.cancel(true);
		
		super.onDestroy();
	}
}