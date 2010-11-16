package org.crittr.track.newstuff;

import com.kostmo.tools.SemaphoreHost;
import com.kostmo.tools.task.image.ImageViewPopulatorTask;

import org.crittr.shared.browser.containers.ViewHolderFlickrPhoto;
import org.crittr.track.Market;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class HostedPhotoDataPopulatorTask extends AsyncTask<Uri, Void, Cursor> {


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
	protected ViewHolderFlickrPhoto holder;
	protected ContentResolver content_resolver;
	
	protected Uri uri;
	
	// ========================================================================
	public HostedPhotoDataPopulatorTask(SemaphoreHost context, ContentResolver content_resolver, ViewHolderFlickrPhoto holder) {
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
	static Cursor obtainMetadataCursor(Uri uri, ContentResolver content_resolver) {

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
			
		} else if (MediaStore.AUTHORITY.equals(data_authority)) {
			projection = new String[] {
					MediaStore.Images.ImageColumns.DATE_TAKEN + " AS " + COLUMN_DATE,
//					MediaStore.Images.ImageColumns.TITLE + " AS " + COLUMN_TITLE,
					MediaStore.Images.ImageColumns.DISPLAY_NAME + " AS " + COLUMN_TITLE,
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
		
	
	void populateViewHolder(SemaphoreHost context, Cursor c, ViewHolderFlickrPhoto holder) {

		if (c == null) return;
		
		if (c.moveToFirst()) {

			int title_col = c.getColumnIndex(COLUMN_TITLE);

			String title_string = c.getString(title_col);
//			Log.d(TAG, "TITLE: " + title_string);
			holder.title.setText(title_string);

			float latitude = c.getFloat(c.getColumnIndex(COLUMN_LAT));
			Log.d(TAG, "LATITUDE: " + latitude);

			float longitude = c.getFloat(c.getColumnIndex(COLUMN_LON));
			Log.d(TAG, "LONGITUDE: " + longitude);


			int description_col = c.getColumnIndex(COLUMN_DESCRIPTION);

			String description_string = c.getString(description_col);
//			Log.d(TAG, "DESCRIPTION: " + description_string);
			holder.description.setText( description_string );


			int thumbnail_col = c.getColumnIndex(COLUMN_THUMBNAIL_URL);

//			Log.d(TAG, "Thumbnail column index: " + thumbnail_col);
			URL image_url = null;
			if (thumbnail_col >= 0) {
				String thumbnail_url = c.getString(thumbnail_col);
				try {
					image_url = new URL(thumbnail_url);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				Log.d(TAG, "Thumbnail URL: " + thumbnail_url);
			}
			

			String authority = this.uri.getAuthority();
			if (FLICKR_PHOTO_AUTHORITY.equals(authority) || COMMONS_PHOTO_AUTHORITY.equals(authority)) {
				new ImageViewPopulatorTask(context, holder.thumbnail).execute(image_url);
			} else if ( MediaStore.AUTHORITY.equals( authority ) ) {
				
				// TODO: Works only for Android 2.0
				/*
				Bitmap thumbnail_bitmap = Thumbnails.getThumbnail(
						getContentResolver(),
						ContentUris.parseId(uri),
						Thumbnails.MICRO_KIND,
						null);	// Don't need options for MICRO_KIND
				*/

				
				
				long baseImageId = ContentUris.parseId(uri);
				
				final String[] THUMB_PROJECTION = new String[] {
					BaseColumns._ID, // 0
					Images.Thumbnails.IMAGE_ID, // 1
					Images.Thumbnails.WIDTH,
					Images.Thumbnails.HEIGHT,
					Thumbnails.DATA
				};

					
				Cursor cc = Thumbnails.queryMiniThumbnail(
						this.content_resolver,
						baseImageId,
						Thumbnails.MINI_KIND,
//						Thumbnails.MICRO_KIND,
						THUMB_PROJECTION);
				

				int blob_index = cc.getColumnIndex(Thumbnails.DATA);
				
				Log.w(TAG, "Gallery thumbnail count: " + cc.getCount());
				
				if (cc != null && cc.moveToFirst()) {
//					long thumbId = cc.getLong(0); // This Id isn't the id of image reduce
//					long imageId = cc.getLong(1); // this id = baseImageId
//					Log.w(TAG, "Blarb: " + thumbId + " --- " + imageId);
					Log.w(TAG, "Dimensions: " + cc.getInt(2) + "x" + cc.getInt(3));
					
					
					/*
					byte[] img_data = cc.getBlob(blob_index);
					Bitmap thumbnail_bitmap = BitmapFactory.decodeByteArray(img_data, 0, img_data.length);
					holder.thumbnail.setImageBitmap(thumbnail_bitmap);
					*/
					
					String possible_thumbnail_img_data_uri = cc.getString(blob_index);
//					Log.e(TAG, "Possible uri: " + possible_thumbnail_img_data_uri);
					
					
					
					holder.thumbnail.setImageURI(Uri.parse(possible_thumbnail_img_data_uri));
				}
				cc.close();
			}
			

//			int date_col = c.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
			int date_col = c.getColumnIndex(COLUMN_DATE);
//			Log.d(TAG, "Date column index: " + date_col);

			long date_milliseconds = c.getLong(date_col);
			Date date_object = new Date(date_milliseconds);
//			Log.d(TAG, "Date: " + date_object);
//			Log.d(TAG, "In milliseconds: " + date_milliseconds);
			
			holder.owner.setText( date_object.toString() );

		} else {
			Log.e(TAG, "NO FILE :(");
		}
		
		c.close();
	}
}