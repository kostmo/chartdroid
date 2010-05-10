package org.achartengine.demo.data;

import org.achartengine.demo.AceDataContentProvider;

import android.graphics.Color;
import android.net.Uri;

public class MultiTimelineData {
	
    public static Uri uri = AceDataContentProvider.BASE_URI.buildUpon()
	    .appendPath(AceDataContentProvider.CHART_DATA_MULTI_TIMELINE_PATH)
	    .appendPath(AceDataContentProvider.CHART_DATA_LABELED_PATH).build(); 
	
    public static String[] SEQUENCE_TITLES = {"Births", "Deaths", "Mutations"};
    public static int[] SEQUENCE_COLORS = {Color.WHITE, Color.RED, Color.BLUE };
	
    public static String[] DEMO_AXES_LABELS = { "Date", "Events" };
    public static String DEMO_CHART_TITLE = "Multi Timeline";
}
