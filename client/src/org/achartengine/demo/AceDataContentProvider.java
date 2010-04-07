package org.achartengine.demo;

import com.googlecode.chartdroid.core.ColumnSchema.EventData;

import org.achartengine.demo.ContentSchema.PlotData;
import org.achartengine.demo.data.DonutData;
import org.achartengine.demo.data.TemperatureData;
import org.achartengine.demo.data.TimelineData;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;



public class AceDataContentProvider extends ContentProvider {


	static final String TAG = "ChartDroid Demo";

	// This must be the same as what as specified as the Content Provider authority
	// in this app's AndroidManifest file.
	public static final String AUTHORITY = "com.googlecode.chartdroid.demo.provider.data2";


	public static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).build();


	static final String MESSAGE_UNSUPPORTED_FEATURE = "Not supported by this provider";

	// Let the appended ID represent a unique dataset, so that the Chart can come
	// back and query for the auxiliary (meta) data (e.g. axes labels, colors, etc.).
	// Alternatively,
	// maybe the meta data could be passed along with the original Intent instead.
	public static Uri constructUri(String dataset_class, long data_id) {
		return ContentUris.withAppendedId(
				Uri.withAppendedPath(BASE_URI, dataset_class),
				data_id);
	}



	public static final String CHART_DATA_SERIES_PATH = "singleseries";
	private static final int CHART_DATA_SERIES = 1;

	public static final String CHART_DATA_MULTISERIES_PATH = "multiseries";
	private static final int CHART_DATA_MULTISERIES = 2;

	public static final String CHART_DATA_UNLABELED_PATH = "unlabeled";
	private static final int CHART_DATA_LABELED_SERIES = 3;

	public static final String CHART_DATA_LABELED_PATH = "labeled";
	private static final int CHART_DATA_LABELED_MULTISERIES = 4;


	public static final String CHART_DATA_TIMELINE_PATH = "timeline";
	private static final int CHART_DATA_LABELED_TIMELINE = 5;


	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AUTHORITY, CHART_DATA_SERIES_PATH + "/" + CHART_DATA_UNLABELED_PATH, CHART_DATA_SERIES);
		sUriMatcher.addURI(AUTHORITY, CHART_DATA_MULTISERIES_PATH + "/" + CHART_DATA_UNLABELED_PATH, CHART_DATA_MULTISERIES);

		sUriMatcher.addURI(AUTHORITY, CHART_DATA_SERIES_PATH + "/" + CHART_DATA_LABELED_PATH, CHART_DATA_LABELED_SERIES);

		String labeled_multiseries_path = CHART_DATA_MULTISERIES_PATH + "/" + CHART_DATA_LABELED_PATH;
		Log.d(TAG, "UriMatcher labeled_multiseries_path: " + labeled_multiseries_path);


		sUriMatcher.addURI(AUTHORITY, CHART_DATA_TIMELINE_PATH + "/" + CHART_DATA_LABELED_PATH, CHART_DATA_LABELED_TIMELINE);

		sUriMatcher.addURI(AUTHORITY, labeled_multiseries_path, CHART_DATA_LABELED_MULTISERIES);
	}




	@Override
	public boolean onCreate() {
		return true;
	}


	@Override
	public String getType(Uri uri) {

		int match = sUriMatcher.match(uri);
		Log.d(TAG, "getType() UriMatcher match: " + match);

		switch (match)
		{
		case CHART_DATA_LABELED_TIMELINE:
			return EventData.CONTENT_TYPE_PLOT_DATA;
		default:
			return PlotData.CONTENT_TYPE_PLOT_DATA;
		}
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		int match = sUriMatcher.match(uri);
		Log.d(TAG, "query() UriMatcher match: " + match);

		switch (match)
		{
		case CHART_DATA_MULTISERIES:
		{
			if (ContentSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ContentSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_AXIS_LABEL});

				int row_index = 0;
				for (int i=0; i<TemperatureData.DEMO_AXES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( TemperatureData.DEMO_AXES_LABELS[i] );
					row_index++;
				}

				return c;
			} else if (ContentSchema.DATASET_ASPECT_META.equals( uri.getQueryParameter(ContentSchema.DATASET_ASPECT_PARAMETER) )) {

				// TODO: Define more columns for color, line style, marker shape, etc.
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_SERIES_LABEL});

				int row_index = 0;
				for (int i=0; i<TemperatureData.DEMO_TITLES.length; i++) {

					c.newRow().add( row_index ).add( TemperatureData.DEMO_TITLES[i] );
					row_index++;
				}

				return c;

			} else {
				// Fetch the actual data


				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_AXIS_INDEX,
						ContentSchema.PlotData.COLUMN_SERIES_INDEX,
						ContentSchema.PlotData.COLUMN_DATUM_VALUE,
						ContentSchema.PlotData.COLUMN_DATUM_LABEL
				});

				int row_index = 0;
				// Add x-axis data
				for (int i=0; i<TemperatureData.DEMO_X_AXIS_DATA.length; i++) {


					//                c.newRow().add( X_AXIS_INDEX ).add( i ).add( TemperatureData.DEMO_X_AXIS_DATA[i] ).add( null );
					c.newRow()
					.add( row_index )
					.add( ContentSchema.X_AXIS_INDEX )
					.add( 0 )   // Only create data for the first series.
					.add( TemperatureData.DEMO_X_AXIS_DATA[i] )
					.add( null );

					row_index++;
				}

				// Add y-axis data
				for (int i=0; i<TemperatureData.DEMO_SERIES_LIST.length; i++) {
					for (int j=0; j<TemperatureData.DEMO_SERIES_LIST[i].length; j++) {

						//                    c.newRow().add( Y_AXIS_INDEX ).add( i ).add( TemperatureData.DEMO_SERIES_LIST[i][j] ).add( null );
						c.newRow()
						.add( row_index )
						.add( ContentSchema.Y_AXIS_INDEX )
						.add( i )
						.add( TemperatureData.DEMO_SERIES_LIST[i][j] )
						.add( null );

						row_index++;
					}
				}

				return c;
			}
		}
		case CHART_DATA_LABELED_TIMELINE:
		{
			if (ContentSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ContentSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_AXIS_LABEL});

				int row_index = 0;
				for (int i=0; i<TimelineData.DEMO_AXES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( TimelineData.DEMO_AXES_LABELS[i] );
					row_index++;
				}

				return c;
			} else if (ContentSchema.DATASET_ASPECT_META.equals( uri.getQueryParameter(ContentSchema.DATASET_ASPECT_PARAMETER) )) {

				// TODO: Define more columns for color, line style, marker shape, etc.
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_SERIES_LABEL});

				int row_index = 0;
				for (int i=0; i<TimelineData.SEQUENCE_TITLES.length; i++) {

					c.newRow().add( row_index ).add( TimelineData.SEQUENCE_TITLES[i] );
					row_index++;
				}

				return c;

			} else {
				// Fetch the actual data


				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_AXIS_INDEX,
						ContentSchema.PlotData.COLUMN_SERIES_INDEX,
						ContentSchema.PlotData.COLUMN_DATUM_VALUE,
						ContentSchema.PlotData.COLUMN_DATUM_LABEL
				});

				int row_index = 0;
				// Add x-axis data

				Calendar date = new GregorianCalendar(0, 0, 0);
				//			Log.e(TAG, "Initializer date: " + date);

				for (int i=0; i<TimelineData.YEARS.length; i++) {
					date.set(TimelineData.YEARS[i], TimelineData.MONTHS[i], 0);

					//                c.newRow().add( X_AXIS_INDEX ).add( i ).add( TemperatureData.DEMO_X_AXIS_DATA[i] ).add( null );

					//				Log.d(TAG, "Date value pre-conversion: " + date);

					long date_long_value = date.getTimeInMillis();
					//				Log.d(TAG, "Long value post-conversion: " + date_long_value);

					c.newRow()
					.add( row_index )
					.add( ContentSchema.X_AXIS_INDEX )
					.add( 0 )   // Only create data for the first series.
					.add( date_long_value )
					.add( TimelineData.TITLES[i] );

					row_index++;


					// Add the y-axis data
					c.newRow()
					.add( row_index )
					.add( ContentSchema.Y_AXIS_INDEX )
					.add( 0 )   // Only create data for the first series.
					.add( 3*i/TimelineData.YEARS.length )	// I picked an arbitrary number here to be the value of the event
					.add( null );

					row_index++;
				}


				return c;
			}
		}
		case CHART_DATA_LABELED_MULTISERIES:
		{
			if (ContentSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ContentSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_AXIS_LABEL});

				int row_index = 0;
				for (int i=0; i<DonutData.DEMO_AXES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( DonutData.DEMO_AXES_LABELS[i] );
					row_index++;
				}

				return c;
			} else if (ContentSchema.DATASET_ASPECT_META.equals( uri.getQueryParameter(ContentSchema.DATASET_ASPECT_PARAMETER) )) {

				// TODO: Define more columns for color, line style, marker shape, etc.
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_SERIES_LABEL});

				int row_index = 0;
				for (int i=0; i<DonutData.DEMO_SERIES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( DonutData.DEMO_SERIES_LABELS[i] );
					row_index++;
				}

				return c;

			} else {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ContentSchema.PlotData.COLUMN_AXIS_INDEX,
						ContentSchema.PlotData.COLUMN_SERIES_INDEX,
						ContentSchema.PlotData.COLUMN_DATUM_VALUE,
						ContentSchema.PlotData.COLUMN_DATUM_LABEL});

				int row_index = 0;
				for (int i=0; i<DonutData.DEMO_SERIES_LIST.length; i++) {
					for (int j=0; j<DonutData.DEMO_SERIES_LIST[i].length; j++) {

						c.newRow()
						.add( row_index )
						.add( ContentSchema.Y_AXIS_INDEX )  // XXX Since we're only populating one axis, it probably doesn't matter whether it's the X or Y axis.
						.add( i )
						.add( DonutData.DEMO_SERIES_LIST[i][j] )
						.add( DonutData.DEMO_SERIES_LABELS_LIST[i][j] );

						row_index++;
					}
				}

				return c;
			}
		}
		}
		Log.w(TAG, "Failed all matching tests!");
		return null;

	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}
	
	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}
}
