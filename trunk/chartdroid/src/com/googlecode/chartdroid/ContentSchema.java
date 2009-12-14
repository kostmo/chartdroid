package com.googlecode.chartdroid;

import android.content.ContentResolver;
import android.provider.BaseColumns;


public final class ContentSchema {
	
//    public static final String AUTHORITY = "org.crittr.provider.TaxonSearch";



    public static final String CONTENT_TYPE_BASE_SINGLE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/";
    public static final String CONTENT_TYPE_BASE_MULTIPLE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/";
    
    
    public static final class CalendarEvent implements BaseColumns {

    	public static final String VND_TYPE_DECLARATION = "vnd.com.googlecode.chartdroid.event";
    	
        // ==== CONTENT TYPES ====
        
        public static final String CONTENT_TYPE_CALENDAR_EVENT = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
        public static final String CONTENT_TYPE_ITEM_CALENDAR_EVENT = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
    	
        // ==== COLUMNS ====

        public static final String COLUMN_EVENT_TITLE = "COLUMN_EVENT_TITLE";
        public static final String COLUMN_EVENT_TIMESTAMP = "COLUMN_EVENT_TIMESTAMP";
    }
    
    
    
    public static final class PlotData implements BaseColumns {

    	public static final String VND_TYPE_DECLARATION = "vnd.com.googlecode.chartdroid.data";
    	
        // ==== CONTENT TYPES ====
        
        public static final String CONTENT_TYPE_PLOT_DATA = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
        public static final String CONTENT_TYPE_ITEM_PLOT_DATA = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_TYPE_DECLARATION;
    	
        // ==== COLUMNS ====

        public static final String COLUMN_AXIS_INDEX = "COLUMN_AXIS_INDEX";
        public static final String COLUMN_DATUM_VALUE = "COLUMN_DATUM_VALUE";
        public static final String COLUMN_DATUM_LABEL = "COLUMN_DATUM_LABEL";
    }
}
