package org.crittr.browse.activity;

import org.crittr.browse.R;
import org.crittr.browse.R.drawable;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class TabActivitySupplementaryTaxonLists extends TabActivity {

	static final String TAG = "Crittr"; 
	

	final static String INTENT_EXTRA_SUPPLEMENT_TYPE = "INTENT_EXTRA_SUPPLEMENT_TYPE";
	
	enum SupplementaryList {BOOKMARKS, SIGHTINGS, POPULAR}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        super.onCreate(savedInstanceState);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);
        
        final TabHost tabHost = getTabHost();

        /*
        Intent sightings_intent = new Intent(getIntent());
        sightings_intent.setClass(this, ExpandableListActivitySightings.class);
        sightings_intent.putExtra(ExpandableListActivitySightings.INTENT_EXTRA_SHOW_TITLE, false);

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("Sightings")
                .setContent(sightings_intent));
		*/
        
        Intent bookmarks_intent = new Intent(getIntent());
        bookmarks_intent.setClass(this, ListActivityBookmarks.class);
        
        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("Bookmarks")
                .setContent(bookmarks_intent));
        
        
        Intent popular_intent = new Intent(getIntent());
        popular_intent.setClass(this, ListActivityPopularTaxons.class);
        
        tabHost.addTab(tabHost.newTabSpec("tab3")
                .setIndicator("Popular")
                .setContent(popular_intent
//                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ));
        
        
        
        int active_tab = getIntent().getIntExtra(INTENT_EXTRA_SUPPLEMENT_TYPE, SupplementaryList.BOOKMARKS.ordinal() );
        tabHost.setCurrentTab(active_tab);
    }
    
    /*
    @Override
    public void  finishFromChild  (Activity child) {

    	setResult(Activity.RESULT_OK, child.getIntent());

    	finish();
    }
    */
    
    @Override
    public void finishActivityFromChild(Activity child, int requestCode) {

		setResult(requestCode, child.getIntent());

    	finish();
    }
}
