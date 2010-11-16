package com.kostmo.flickr.provider;

import android.content.ContentResolver;
import android.provider.BaseColumns;


public final class FlickrPhoto {
	
    public static final String AUTHORITY = "com.kostmo.flickr.bettr.provider.experimental";


    public static final class PhotoRetrieval implements BaseColumns {
    	
    	public static final String VND_TYPE_DECLARATION = "vnd.com.kostmo.image.hosted";
    	
        // ==== CONTENT TYPES ====
        
        public static final String CONTENT_TYPE_HOSTED_IMAGE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
        public static final String CONTENT_TYPE_ITEM_HOSTED_IMAGE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
    	
    	
        // ==== COLUMNS ====
    	
    	public static final String COLUMN_DATE = "COLUMN_DATE";
    	public static final String COLUMN_OWNER = "COLUMN_OWNER";
    	public static final String COLUMN_TITLE = "COLUMN_TITLE";
    	public static final String COLUMN_DESCRIPTION = "COLUMN_DESCRIPTION";
    	public static final String COLUMN_LAT = "COLUMN_LAT";
    	public static final String COLUMN_LON = "COLUMN_LON";
    	public static final String COLUMN_THUMBNAIL_URL = "COLUMN_THUMBNAIL_URL";
    	
        // ==== CONTENT TYPE IDs ====
    	
        public static final int NO_MATCH = 0;
        
    	public static final int FLICKR_PHOTO_INFO_SINGLE = 1;
    	public static final int FLICKR_PHOTO_INFO_MULTIPLE = 2;
        public static final int FLICKR_PHOTO_DATA = 3;

    }
}
