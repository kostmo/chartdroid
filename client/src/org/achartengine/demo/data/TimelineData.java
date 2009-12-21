package org.achartengine.demo.data;

import org.achartengine.demo.AceDataContentProvider;

import android.net.Uri;

public class TimelineData {
	
    public static Uri uri = AceDataContentProvider.BASE_URI.buildUpon()
    .appendPath(AceDataContentProvider.CHART_DATA_TIMELINE_PATH)
    .appendPath(AceDataContentProvider.CHART_DATA_LABELED_PATH).build(); 
	
    public static String[] SEQUENCE_TITLES = { "Open Source" };
	
    public static String[] DEMO_AXES_LABELS = { "Date", "Events" };
    public static String DEMO_CHART_TITLE = "OSS Timeline";
	
    public static String[] TITLES = {
    	"Emacs",
    	"TeX",
    	"X window system",
    	"GCC",
    	"Perl",
    	"Linux Kernel",
    	"Python",
    	"386BSD",
    	"Samba",
    	"NetBSD",
    	"FreeBSD",
    	"Wine",
    	"PHP",
    	"GIMP",
    	"Apache",
    	"KDE",
    	"GNOME",
    	"OpenOffice.org",
    	"MediaWiki",
    	"Firefox",
    	};

    public static int[] YEARS = {
     		1976,
     		1982,
     		1984,
     		1985,
     		1987,
     		1991,
     		1991,
     		1992,
     		1992,
     		1993,
     		1993,
     		1993,
     		1995,
     		1995,
     		1996,
     		1996,
     		1997,
     		1999,
     		2002,
     		2003
     	};
 		
 	    public static int[] MONTHS = {
     		0,
     		0,
     		0,
     		0,
     		0,
     		0,
     		0,
     		0,
     		0,
     		2,
     		11,
     		0,
     		5,
     		0,
     		0,
     		0,
     		7,
     		7,
     		0,
     		3
 	    };
     		
}
