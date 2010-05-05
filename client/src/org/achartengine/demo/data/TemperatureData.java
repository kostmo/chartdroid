package org.achartengine.demo.data;

import org.achartengine.demo.AceDataContentProvider;

import android.net.Uri;

public class TemperatureData {
	
	
    public static Uri uri = AceDataContentProvider.BASE_URI.buildUpon()
    .appendPath(AceDataContentProvider.CHART_DATA_MULTISERIES_PATH)
    .appendPath(AceDataContentProvider.CHART_DATA_UNLABELED_PATH).build();  
	
	
    public static String[] DEMO_AXES_LABELS = { "Month", "Temperature (F)" };
    public static String DEMO_CHART_TITLE = "Average temp";
   
    
    
    public static double[] DEMO_X_AXIS_DATA = { 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    

    public static String[] DEMO_TITLES = { "Crete", "Corfu", "Thassos", "Skiathos" };
    
    
    public static double[] DEMO_SERIES_1 = { 12.3, 12.5, 13.8, 16.8, 20.4, 24.4, 26.4, 26.1, 23.6, 20.3, 17.2,
        13.9 };
    public static double[] DEMO_SERIES_2 = { 10, 10, 12, 15, 20, 24, 26, 26, 23, 18, 14, 11 };
    public static double[] DEMO_SERIES_3 = { 5, 5.3, 8, 12, 17, 22, 24.2, 24, 19, 15, 9, 6 };

    public static double[] DEMO_SERIES_4 = { 9, 10, 11, 15, 19, 23, 26, 25, 22, 18, 13, 10 };   
    
    public static double[][] DEMO_SERIES_LIST = {DEMO_SERIES_1, DEMO_SERIES_2, DEMO_SERIES_3, DEMO_SERIES_4};
}
