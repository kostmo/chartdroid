package com.googlecode.chartdroid.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Bitmap;
import android.inputmethodservice.Keyboard.Row;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageFileContentProvider extends ContentProvider {

	static final String TAG = "ChartDroid";

	public static final String AUTHORITY = "com.googlecode.chartdroid.image";
	static Uri BASE_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).build();

	public static final String SAVED_IMAGE_MIME_TYPE = "image/png";
	public static final String PNG_EXTENSION = ".png";
	public static final String CHART_DISPLAY_NAME = "ChartDroid chart" + PNG_EXTENSION;

	static final String MESSAGE_UNSUPPORTED_FEATURE = "Not supported by this provider";

	// ========================================================================
	public static Uri constructUri(long screenshot_id) {
		return ContentUris.withAppendedId(BASE_URI, screenshot_id);
	}

	// ========================================================================
	private static File getTempDirectory(Context context, boolean create) {
		File temp_data_directory = new File(Environment.getExternalStorageDirectory(), context.getPackageName());
		if (create)
			temp_data_directory.mkdirs();
		return temp_data_directory;
	}

	// ========================================================================
	private static File getFileById(File temp_data_directory, long id) {
		return new File(temp_data_directory, id + PNG_EXTENSION);
	}
	
	// ========================================================================
	private static File getFileByUri(File temp_data_directory, Uri uri) {
		long image_id = ContentUris.parseId(uri);
		return new File(temp_data_directory, image_id + PNG_EXTENSION);
	}

	// ========================================================================
	public static Uri storeTemporaryImage(Context context, Bitmap bitmap) throws IOException {

		long randomly_assigned_id = 1;	// FIXME

		File temp_data_directory = getTempDirectory(context, true);
//		File temp_file = File.createTempFile("chart", PNG_EXTENSION, temp_data_directory);
//		Log.d(TAG, "Temp filename: " + temp_file.getAbsolutePath());
		File temp_file = getFileById(temp_data_directory, randomly_assigned_id);

		OutputStream os = new BufferedOutputStream( new FileOutputStream(temp_file) );
		bitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
		os.flush();
		os.close();

		return ContentUris.withAppendedId(BASE_URI, randomly_assigned_id);
	}

	// ========================================================================
	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) {

		File temp_data_directory = getTempDirectory(getContext(), false);
		File temp_file = getFileByUri(temp_data_directory, uri);

		try {
			return ParcelFileDescriptor.open(temp_file, ParcelFileDescriptor.MODE_READ_ONLY);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	// ========================================================================
	@Override
	public boolean onCreate() {
		return true;
	}

	// ========================================================================
	@Override
	public String getType(Uri uri) {
		return SAVED_IMAGE_MIME_TYPE;
	}

	// ========================================================================
	@Override
	public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {

		MatrixCursor cursor = new MatrixCursor(as);
		RowBuilder row = cursor.newRow();
		for (String column : as) {
			if ( MediaStore.MediaColumns.DISPLAY_NAME.equals(column) )
				row.add( CHART_DISPLAY_NAME );
			else if ( MediaStore.MediaColumns.SIZE.equals(column) ) {
				File temp_data_directory = getTempDirectory(getContext(), false);
				File file = getFileByUri(temp_data_directory, uri);
				row.add( file.length() );
			} else {
				row.add( null );
			}
		}
			
		return cursor;
	}

	// ========================================================================
	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	// ========================================================================
	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}

	// ========================================================================
	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		throw new UnsupportedOperationException(MESSAGE_UNSUPPORTED_FEATURE);
	}
}
