package org.crittr.shared.browser;

import java.util.List;

import org.crittr.browse.Market;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class Constants {
	

	static final String TAG = Market.DEBUG_TAG;
	
	public static final String CATEGORY_TAXON = "org.crittr.category.TAXON";
    
    // Startup values
	public static final String INTENT_EXTRA_PARENT_TSN = "INTENT_EXTRA_PARENT_TSN";
    public static final String INTENT_EXTRA_TAXON_NAME = "INTENT_EXTRA_TAXON_NAME";
    public static final String INTENT_EXTRA_RANK_NAME = "INTENT_EXTRA_RANK_NAME";
    
    public static final String INTENT_EXTRA_TSN = "TSN";
    
	public static String INTENT_EXTRA_ALLOW_DIRECT_CHOICE = "INTENT_EXTRA_ALLOW_DIRECT_CHOICE";
	public static String INTENT_EXTRA_DIRECT_CHOICE_MADE = "INTENT_EXTRA_DIRECT_CHOICE_MADE";

    
	public static final long INVALID_TSN = -1;
	public static final long UNKNOWN_PARENT_ID = -1;
	public static final long NO_PARENT_ID = 0;
	public static final long ORPHAN_PARENT_ID = NO_PARENT_ID;
	public static final int INVALID_RANK_ID = -1;
	public static final int UNKNOWN_ITIS_KINGDOM = -1;
    

	public enum CollectionSource {FLICKR, COMMONS, WIKIPEDIA}
}
