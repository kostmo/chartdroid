package com.kostmo.flickr.tasks;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;
import android.view.View;

import com.kostmo.flickr.activity.BatchUploaderActivity.ImageUploadData;
import com.kostmo.flickr.adapter.ImageUploadListCursorAdapter.UploadViewHolder;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.tools.SemaphoreHost;

public class MediaStoreImagePopulatorTask extends AsyncTask<Uri, Void, Cursor> {


	static final String TAG = Market.DEBUG_TAG;

	
	static final String COLUMN_DATE = "COLUMN_DATE";
	static final String COLUMN_TITLE = "COLUMN_TITLE";
	static final String COLUMN_DESCRIPTION = "COLUMN_DESCRIPTION";
	static final String COLUMN_LAT = "COLUMN_LAT";
	static final String COLUMN_LON = "COLUMN_LON";
	static final String COLUMN_THUMBNAIL_URL = "COLUMN_THUMBNAIL_URL";

	static final String FLICKR_PHOTO_AUTHORITY = "com.kostmo.flickr.bettr.provider.experimental";
	static final String COMMONS_PHOTO_AUTHORITY = "com.kostmo.labeler.commons.provider.experimental";
	

	protected SemaphoreHost semaphore_host;
	protected UploadViewHolder holder;
	protected ContentResolver content_resolver;
	
	protected Uri uri;
	
	// ========================================================================
	public MediaStoreImagePopulatorTask(SemaphoreHost context, ContentResolver content_resolver, UploadViewHolder holder) {
		this.semaphore_host = context;
		this.holder = holder;
		this.content_resolver = content_resolver;
	}

	// ========================================================================
	@Override
	public void onPreExecute() {
		semaphore_host.incSemaphore();
	}

	// ========================================================================
	@Override
	protected Cursor doInBackground(Uri... params) {
		Uri uri = params[0];
		this.uri = uri;
		return obtainMetadataCursor(this.uri, this.content_resolver);
	}

	// ========================================================================
	@Override
	protected void onPostExecute(Cursor cursor) {

		populateViewHolder(semaphore_host, cursor, holder);
		semaphore_host.decSemaphore();
	}

	// ========================================================================
	public static Cursor obtainMetadataCursor(Uri uri, ContentResolver content_resolver) {

		String data_authority = uri.getAuthority();
		Log.i(TAG, "Authority of returned data: " + data_authority);
		String[] projection = null;

		if (FLICKR_PHOTO_AUTHORITY.equals(data_authority) || COMMONS_PHOTO_AUTHORITY.equals(data_authority)) {
			projection = new String[] {
					COLUMN_DATE,
					COLUMN_TITLE,
					COLUMN_DESCRIPTION,
					COLUMN_LAT,
					COLUMN_LON,
					COLUMN_THUMBNAIL_URL};
			
			Log.i(TAG, "Using \"Flickr photo\" authority...");
			
		} else if (MediaStore.AUTHORITY.equals(data_authority)) {
			projection = new String[] {
					MediaStore.Images.ImageColumns.DATE_TAKEN + " AS " + COLUMN_DATE,
					MediaStore.Images.ImageColumns.TITLE + " AS " + COLUMN_TITLE,
//					MediaStore.Images.ImageColumns.DISPLAY_NAME + " AS " + COLUMN_TITLE,
					MediaStore.Images.ImageColumns.DESCRIPTION + " AS " + COLUMN_DESCRIPTION,
					MediaStore.Images.ImageColumns.LATITUDE + " AS " + COLUMN_LAT,
					MediaStore.Images.ImageColumns.LONGITUDE + " AS " + COLUMN_LON,
			};
		}

		Cursor c = content_resolver.query(
				uri,
				projection,
				null, null, null
		);

		return c;
	}

	
	public static void populateTitleDescriptionFromCursor(Cursor c, ImageUploadData upload) {
		
		if (c.moveToFirst()) {
			int title_col = c.getColumnIndex(COLUMN_TITLE);
			if (title_col > 0)
				upload.title = c.getString(title_col);
			else {
				title_col = c.getColumnIndex(MediaStore.Images.ImageColumns.TITLE);
				if (title_col > 0)
					upload.title = c.getString(title_col);
			}
			
			int description_col = c.getColumnIndex(COLUMN_DESCRIPTION);
			if (description_col > 0)
				upload.description = c.getString(description_col);
			else {
				description_col = c.getColumnIndex(MediaStore.Images.ImageColumns.DESCRIPTION);
				if (description_col > 0)
					upload.description = c.getString(description_col);
			}
		}
	}
	
	
	void populateViewHolder(SemaphoreHost context, Cursor c, UploadViewHolder holder) {

		if (c == null) return;
		
		if (c.moveToFirst()) {

			
			
			// XXX The title and description will be populated from my personal database
			/*
			int title_col = c.getColumnIndex(COLUMN_TITLE);

			String title_string = c.getString(title_col);
//			Log.d(TAG, "TITLE: " + title_string);
			holder.title.setText(title_string);
			
			
			int description_col = c.getColumnIndex(COLUMN_DESCRIPTION);
			String description_string = c.getString(description_col);
			if (description_string == null || description_string.length() == 0) {
				holder.description.setVisibility(View.GONE);
			} else {
				holder.description.setText( description_string );
				holder.description.setVisibility(View.VISIBLE);
			}
			*/
			
			

			int lat_column_index = c.getColumnIndex(COLUMN_LAT);
			int lon_column_index = c.getColumnIndex(COLUMN_LON);
			if (!c.isNull(lat_column_index) && !c.isNull(lon_column_index)) {
				float latitude = c.getFloat(lat_column_index);
				float longitude = c.getFloat(c.getColumnIndex(COLUMN_LON));
				holder.geo_coords.setText(String.format("Geo: (%.2f, %.2f)", latitude, longitude));
				holder.geo_icon.setVisibility(View.VISIBLE);
				
			} else {
				holder.geo_coords.setText("No geo");
				holder.geo_icon.setVisibility(View.GONE);
			}



			String authority = this.uri.getAuthority();
			if ( MediaStore.AUTHORITY.equals( authority ) ) {
				
				// Works only for Android 2.0
				Bitmap thumbnail_bitmap = Thumbnails.getThumbnail(
						this.content_resolver,
						ContentUris.parseId(uri),
						Thumbnails.MICRO_KIND,
						null);	// Don't need options for MICRO_KIND
				
				holder.icon.setImageBitmap(thumbnail_bitmap);
//				holder.icon.setVisibility(View.VISIBLE);	// XXX
			} else {
//				holder.icon.setVisibility(View.GONE);	// XXX
				holder.icon.setImageResource(R.drawable.picture_unknown_1);
			}
			
//			int date_col = c.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
			int date_col = c.getColumnIndex(COLUMN_DATE);
			if (date_col < 0)
				date_col = c.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
//			Log.d(TAG, "Date column index: " + date_col);

			if (date_col >= 0) {
				long date_milliseconds = c.getLong(date_col);
				Date date_object = new Date(date_milliseconds);
			}

		} else {
			Log.e(TAG, "NO FILE :(");
		}
		
		c.close();
	}
}