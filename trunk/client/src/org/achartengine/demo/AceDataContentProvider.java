package org.achartengine.demo;

import com.googlecode.chartdroid.core.ColumnSchema;

import org.achartengine.demo.data.BudgetData;
import org.achartengine.demo.data.MultiTimelineData;
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
import java.util.Random;



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
	public static final String CHART_DATA_MULTI_TIMELINE_PATH = "multitimeline";
	
	
	private static final int CHART_DATA_LABELED_TIMELINE = 5;
	private static final int CHART_DATA_MULTI_TIMELINE = 6;
	

	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AUTHORITY, CHART_DATA_SERIES_PATH + "/" + CHART_DATA_UNLABELED_PATH, CHART_DATA_SERIES);
		sUriMatcher.addURI(AUTHORITY, CHART_DATA_MULTISERIES_PATH + "/" + CHART_DATA_UNLABELED_PATH, CHART_DATA_MULTISERIES);

		sUriMatcher.addURI(AUTHORITY, CHART_DATA_SERIES_PATH + "/" + CHART_DATA_LABELED_PATH, CHART_DATA_LABELED_SERIES);

		String labeled_multiseries_path = CHART_DATA_MULTISERIES_PATH + "/" + CHART_DATA_LABELED_PATH;
		Log.d(TAG, "UriMatcher labeled_multiseries_path: " + labeled_multiseries_path);


		sUriMatcher.addURI(AUTHORITY, CHART_DATA_TIMELINE_PATH + "/" + CHART_DATA_LABELED_PATH, CHART_DATA_LABELED_TIMELINE);
		
		sUriMatcher.addURI(AUTHORITY, CHART_DATA_MULTI_TIMELINE_PATH + "/" + CHART_DATA_LABELED_PATH, CHART_DATA_MULTI_TIMELINE);


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

		switch (match) {
		case CHART_DATA_LABELED_TIMELINE:
		case CHART_DATA_MULTI_TIMELINE:
			return ColumnSchema.EventData.CONTENT_TYPE_PLOT_DATA;
		default:
			return ColumnSchema.PlotData.CONTENT_TYPE_PLOT_DATA;
		}
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		int match = sUriMatcher.match(uri);
		Log.d(TAG, "query() UriMatcher match: " + match);

		switch (match) {
		// --------------------------------------------------------------------
		case CHART_DATA_MULTISERIES:
		{
			if (ColumnSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_AXIS_LABEL});

				int row_index = 0;
				for (int i=0; i<TemperatureData.DEMO_AXES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( TemperatureData.DEMO_AXES_LABELS[i] );
					row_index++;
				}

				return c;
			} else if (ColumnSchema.DATASET_ASPECT_SERIES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				// TODO: Define more columns for color, line style, marker shape, etc.
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_SERIES_LABEL});

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
						ColumnSchema.COLUMN_AXIS_INDEX,
						ColumnSchema.COLUMN_SERIES_INDEX,
						ColumnSchema.COLUMN_DATUM_VALUE,
						ColumnSchema.COLUMN_DATUM_LABEL
				});

				int row_index = 0;
				// Add x-axis data
				for (int i=0; i<TemperatureData.DEMO_X_AXIS_DATA.length; i++) {


					//                c.newRow().add( X_AXIS_INDEX ).add( i ).add( TemperatureData.DEMO_X_AXIS_DATA[i] ).add( null );
					c.newRow()
					.add( row_index )
					.add( ColumnSchema.X_AXIS_INDEX )
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
						.add( ColumnSchema.Y_AXIS_INDEX )
						.add( i )
						.add( TemperatureData.DEMO_SERIES_LIST[i][j] )
						.add( null );

						row_index++;
					}
				}

				return c;
			}
		}
		// --------------------------------------------------------------------
		case CHART_DATA_MULTI_TIMELINE:
		{
			if (ColumnSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_AXIS_LABEL});

				for (int i=0; i<MultiTimelineData.DEMO_AXES_LABELS.length; i++) {
					c.newRow().add( i ).add( MultiTimelineData.DEMO_AXES_LABELS[i] );
				}

				return c;
			} else if (ColumnSchema.DATASET_ASPECT_SERIES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				// TODO: Define more columns for color, line style, marker shape, etc.
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_SERIES_LABEL,
						ColumnSchema.COLUMN_SERIES_COLOR});

				for (int i=0; i<MultiTimelineData.SEQUENCE_TITLES.length; i++) {
					c.newRow().add( i )
						.add( MultiTimelineData.SEQUENCE_TITLES[i] )
						.add( MultiTimelineData.SEQUENCE_COLORS[i] );
				}

				return c;

			} else {
				// Fetch the actual data

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_SERIES_INDEX,
						ColumnSchema.COLUMN_DATUM_LABEL,
						"AXIS_X",
						"AXIS_Y",
				});


				int row_index = 0;
				Random random = new Random();
				for (int series_index=0; series_index<MultiTimelineData.SEQUENCE_TITLES.length; series_index++) {

					double y_offset = random.nextDouble()*10; 
					Calendar date = new GregorianCalendar();
					
					for (int i=0; i<12; i++) {
						c.newRow()
						.add( row_index )
						.add( series_index )
						.add( null )
						.add( date.getTime().getTime() )   // Only create data for the first series.
						.add( y_offset + Math.pow(i, 1.3) );	// I picked an arbitrary number here to be the value of the event

						date.add(Calendar.DATE, 1);
						
						row_index++;
					}
				}

				return c;
			}
		}
		// --------------------------------------------------------------------
		case CHART_DATA_LABELED_TIMELINE:
		{
			if (ColumnSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_AXIS_LABEL});

				int row_index = 0;
				for (int i=0; i<TimelineData.DEMO_AXES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( TimelineData.DEMO_AXES_LABELS[i] );
					row_index++;
				}

				return c;
			} else if (ColumnSchema.DATASET_ASPECT_SERIES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				// TODO: Define more columns for color, line style, marker shape, etc.
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_SERIES_LABEL});

				int row_index = 0;
				for (int i=0; i<TimelineData.SEQUENCE_TITLES.length; i++) {

					c.newRow().add( row_index ).add( TimelineData.SEQUENCE_TITLES[i] );
					row_index++;
				}

				return c;

			} else {
				// Fetch the actual data

				// Note: This uses the "alternate" column schema
				// (http://code.google.com/p/chartdroid/wiki/AlternateColumnScheme)
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_AXIS_INDEX,
						ColumnSchema.COLUMN_SERIES_INDEX,
						ColumnSchema.COLUMN_DATUM_VALUE,
						ColumnSchema.COLUMN_DATUM_LABEL
				});

				int row_index = 0;
				// Add x-axis data

				Calendar date = new GregorianCalendar();

				for (int i=0; i<TimelineData.YEARS.length; i++) {
					date.set(TimelineData.YEARS[i], TimelineData.MONTHS[i], 0);

					long date_long_value = date.getTimeInMillis();

					c.newRow()
					.add( row_index )
					.add( ColumnSchema.X_AXIS_INDEX )
					.add( 0 )   // Only create data for the first series.
					.add( date_long_value )
					.add( TimelineData.TITLES[i] );

					row_index++;


					// Add the y-axis data
					c.newRow()
					.add( row_index )
					.add( ColumnSchema.Y_AXIS_INDEX )
					.add( 0 )   // Only create data for the first series.
					.add( 3*i/TimelineData.YEARS.length )	// I picked an arbitrary number here to be the value of the event
					.add( null );

					row_index++;
				}

				return c;
			}
		}
		// --------------------------------------------------------------------
		case CHART_DATA_LABELED_MULTISERIES:
		{
			if (ColumnSchema.DATASET_ASPECT_AXES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_AXIS_LABEL});

				int row_index = 0;
				for (int i=0; i<BudgetData.DEMO_AXES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( BudgetData.DEMO_AXES_LABELS[i] );
					row_index++;
				}

				return c;
			} else if (ColumnSchema.DATASET_ASPECT_SERIES.equals( uri.getQueryParameter(ColumnSchema.DATASET_ASPECT_PARAMETER) )) {

				// TODO: Define more columns for color, line style, marker shape, etc.
				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_SERIES_LABEL});

				int row_index = 0;
				for (int i=0; i<BudgetData.DEMO_SERIES_LABELS.length; i++) {

					c.newRow().add( row_index ).add( BudgetData.DEMO_SERIES_LABELS[i] );
					row_index++;
				}

				return c;

			} else {

				MatrixCursor c = new MatrixCursor(new String[] {
						BaseColumns._ID,
						ColumnSchema.COLUMN_AXIS_INDEX,
						ColumnSchema.COLUMN_SERIES_INDEX,
						ColumnSchema.COLUMN_DATUM_VALUE,
						ColumnSchema.COLUMN_DATUM_LABEL});

				int row_index = 0;
				for (int i=0; i<BudgetData.DEMO_SERIES_LIST.length; i++) {
					for (int j=0; j<BudgetData.DEMO_SERIES_LIST[i].length; j++) {

						c.newRow()
						.add( row_index )
						.add( ColumnSchema.Y_AXIS_INDEX )  // XXX Since we're only populating one axis, it probably doesn't matter whether it's the X or Y axis.
						.add( i )
						.add( BudgetData.DEMO_SERIES_LIST[i][j] )
						.add( BudgetData.DEMO_SERIES_LABELS_LIST[i][j] );

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
