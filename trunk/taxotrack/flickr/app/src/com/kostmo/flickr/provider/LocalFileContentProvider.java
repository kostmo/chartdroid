package com.kostmo.flickr.provider;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.kostmo.flickr.bettr.Market;

public class LocalFileContentProvider extends ContentProvider {
	// This class can be used for launching the Image Zoom Activity in the Camera app
	
	
	   private static final String URI_PREFIX = "content://com.kostmo.flickr.bettr.provider.localfile";


		static final String TAG = Market.DEBUG_TAG; 
	   
	   public static Uri constructUri(String absolute_file_path) {
	       Uri uri = Uri.parse(URI_PREFIX + absolute_file_path);
	       return uri;
	   }

	   @Override
	   public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		   
		   Log.e(TAG, "Using file descriptor!!!!");
		   
	       File file = new File(uri.getPath());
	       ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
	       return parcel;
	   }

	   @Override
	   public boolean onCreate() {
	       return true;
	   }

	   @Override
	   public int delete(Uri uri, String s, String[] as) {
	       throw new UnsupportedOperationException("Not supported by this provider");
	   }

	   @Override
	   public String getType(Uri uri) {
		   return "image/*";
	   }

	   @Override
	   public Uri insert(Uri uri, ContentValues contentvalues) {
	       throw new UnsupportedOperationException("Not supported by this provider");
	   }

	   @Override
	   public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
	       throw new UnsupportedOperationException("Not supported by this provider");
	   }

	   @Override
	   public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
	       throw new UnsupportedOperationException("Not supported by this provider");
	   }

	}
