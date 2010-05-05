package org.achartengine.demo.data;

import org.achartengine.demo.AceDataContentProvider;

import android.net.Uri;


public class BudgetData {

	public static Uri uri = AceDataContentProvider.BASE_URI.buildUpon()
        .appendPath(AceDataContentProvider.CHART_DATA_MULTISERIES_PATH)
        .appendPath(AceDataContentProvider.CHART_DATA_LABELED_PATH).build(); 
	
    public static String[] DEMO_AXES_LABELS = { "Datum abscissa", "Datum ordinate" };
    
    
    public static String[] DEMO_SERIES_LABELS = { "2006", "2007" };
    public static String DEMO_CHART_TITLE = "Project budget";

    public static double[] DATA_SERIES_1 = {12, 14, 11};
    public static double[] DATA_SERIES_2 = {10, 9, 14};
    
    public static String[] DATA_LABELS_SERIES_1 = {"P1", "P2", "P3"};
    public static String[] DATA_LABELS_SERIES_2 = {"P1", "P2", "P3"};
    
    /*
    public static double[] DATA_SERIES_1 = {12, 14, 11, 10, 19};
    public static double[] DATA_SERIES_2 = {10, 9, 14, 20, 11};
    
    public static String[] DATA_LABELS_SERIES_1 = {"P1", "P2", "P3", "P4", "P5"};
    public static String[] DATA_LABELS_SERIES_2 = {"P1", "P2", "P3", "P4", "P5"};
    */
    
    public static double[][] DEMO_SERIES_LIST = {DATA_SERIES_1, DATA_SERIES_2};
    public static String[][] DEMO_SERIES_LABELS_LIST = {DATA_LABELS_SERIES_1, DATA_LABELS_SERIES_2};
}
