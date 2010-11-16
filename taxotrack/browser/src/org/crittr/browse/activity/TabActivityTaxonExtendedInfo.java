package org.crittr.browse.activity;

import java.util.List;

import org.crittr.browse.AsyncTaskModified;
import org.crittr.browse.R;
import org.crittr.browse.R.drawable;
import org.crittr.browse.R.id;
import org.crittr.browse.R.layout;
import org.crittr.containers.ThumbnailUrlPlusLinkContainer;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisQuery;
import org.crittr.task.NetworkUnavailableException;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TabHost;

public class TabActivityTaxonExtendedInfo extends TabActivity {

	static final String TAG = "Crittr"; 

	TaxonInfo taxon_target = new TaxonInfo();

	long current_tsn;

	public static String[] photoset_labels = new String[] { "Flickr",  "Commons", "Wikipedia" };
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tabs_extended_info);

        
        
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);
        
        
        current_tsn = getIntent().getLongExtra(Constants.INTENT_EXTRA_TSN, Constants.INVALID_TSN);
        
        new TaxonGetterTask(current_tsn).execute();
        
        
        findViewById(R.id.button_launch_wikipedia).setOnClickListener(cb_launch_wikipedia);
        findViewById(R.id.button_launch_eol).setOnClickListener(cb_launch_eol);
        findViewById(R.id.button_launch_commons).setOnClickListener(cb_launch_commons);
        findViewById(R.id.button_launch_wikispecies).setOnClickListener(cb_launch_wikispecies);
        
        
        
        
        
        
        
        final TabHost tabHost = getTabHost();

        int[] icons = new int[] {R.drawable.flickr48, R.drawable.commons48, R.drawable.wikipedia48};
        
        
        
        for (int j=0; j<icons.length; j++) {
	        Intent i = new Intent(getIntent()).setClass(this, ListActivityTaxonExtendedInfo.class);
	        tabHost.addTab(tabHost.newTabSpec( "tab" + j )
	                .setIndicator(photoset_labels[j], this.getResources().getDrawable(icons[j]))
	                .setContent( i.putExtra(ListActivityTaxonExtendedInfo.INTENT_EXTRA_PHOTO_COLLECTION_SOURCE, j)));
        }
        tabHost.setCurrentTab(0);
    }

    

	// ======================================================================
    
    private View.OnClickListener cb_launch_wikispecies = new View.OnClickListener() {
	    public void onClick(View v) {

	    	String taxon_name = get_taxon_name_now(TabActivityTaxonExtendedInfo.this, taxon_target, current_tsn);

	        String wikipedia_url = "http://species.wikimedia.org/wiki/" + taxon_name;
	    	Uri flickr_destination = Uri.parse( wikipedia_url );
	    	// Launches the standard browser.
	    	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

	    }
    }; 
    
    private View.OnClickListener cb_launch_commons = new View.OnClickListener() {
	    public void onClick(View v) {
	    	String taxon_name = get_taxon_name_now(TabActivityTaxonExtendedInfo.this, taxon_target, current_tsn);

	        String wikipedia_url = "http://commons.wikimedia.org/wiki/Category:" + taxon_name;
	    	Uri flickr_destination = Uri.parse( wikipedia_url );
	    	// Launches the standard browser.
	    	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

	    }
    };  

    private View.OnClickListener cb_launch_eol = new View.OnClickListener() {
	    public void onClick(View v) {
	    	String taxon_name = get_taxon_name_now(TabActivityTaxonExtendedInfo.this, taxon_target, current_tsn);

	        String wikipedia_url = "http://www.eol.org/search?q=" + taxon_name;
	    	Uri flickr_destination = Uri.parse( wikipedia_url );
	    	// Launches the standard browser.
	    	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

	    }
    };  
    
    
    private View.OnClickListener cb_launch_wikipedia = new View.OnClickListener() {
	    public void onClick(View v) {
	    	String taxon_name = get_taxon_name_now(TabActivityTaxonExtendedInfo.this, taxon_target, current_tsn);

	        String wikipedia_url = "http://en.wikipedia.org/wiki/" + taxon_name;
	    	Uri flickr_destination = Uri.parse( wikipedia_url );
	    	// Launches the standard browser.
	    	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

	    }
    };    
    
    // ============================================= 
 
    public static String get_taxon_name_now(Context context, TaxonInfo taxon_target, long tsn) {
		if (taxon_target.taxon_name == null || taxon_target.taxon_name.length() == 0) {
			try {
				taxon_target.taxon_name = ItisQuery.getScientificNameFromTSN(context, tsn);
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}
		}
		return taxon_target.taxon_name;
    }
    
    
    // ===============================================
    
    
	// ===================================
	// Also see "SearchTaskTaxonBased.java"
    public class TaxonGetterTask extends AsyncTaskModified<Void, List<ThumbnailUrlPlusLinkContainer>, Void> {

    	long tsn;
    	
    	TaxonGetterTask(long tsn) {
    		this.tsn = tsn;
    		

    	}

		protected Void doInBackground(Void... params) {

			try {
	    		taxon_target.taxon_name = ItisQuery.getScientificNameFromTSN(TabActivityTaxonExtendedInfo.this, tsn);
			} catch (NetworkUnavailableException e) {
				e.printStackTrace();
			}

			return null;
		}
		

	    @Override
	    public void onProgressUpdate(List<ThumbnailUrlPlusLinkContainer>... progress_packet) {

	    }
		

	    @Override
	    public void onPostExecute(Void foo) {
	    	Log.d(TAG, "Done with taxon fetch.");
	    }
    }
}
