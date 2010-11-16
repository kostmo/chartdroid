package org.crittr.browse.activity;



import org.crittr.browse.R;
import org.crittr.browse.R.drawable;
import org.crittr.browse.R.id;
import org.crittr.browse.R.layout;
import org.crittr.browse.R.menu;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;
import org.crittr.task.RetrievalTaskPopularTaxons;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;


// Note: For now, this is just a list of bookmarks.
public class ListActivityPopularTaxons extends ListActivity {

	
	static final String TAG = "Crittr";
	

	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.list_activity_bookmarks);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);
        
        ((TextView) findViewById(R.id.activity_title_header)).setText("Popular Taxons");

    	setListAdapter(new ListAdapterTaxons(this, false));
		registerForContextMenu(getListView());

        new RetrievalTaskPopularTaxons( this ).execute();
    }
    
    
    // ==========================================
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
    	
//    	long tsn = ((ListAdapterTaxons) l.getAdapter()).getItemTSN(position);
    	TaxonInfo ti = (TaxonInfo) l.getAdapter().getItem(position);
    	
    	
    	

    	
    	
    	Intent i = new Intent();
    	i.putExtra(Constants.INTENT_EXTRA_TSN, ti.tsn);
    	
    	
    	// TODO: An alternate way to determine this would be to check whether
    	// getCallingActivity() returns "null".
    	boolean should_return = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
    	if (should_return) {

        	i.putExtra(Constants.INTENT_EXTRA_RANK_NAME, ti.rank_name);
        	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, ti.taxon_name);
        	
        	setResult(Activity.RESULT_OK, i);
        	finish();
	    	
    	} else {
    		i.setAction(Intent.ACTION_VIEW);
    		i.addCategory(Constants.CATEGORY_TAXON);
    		startActivity(i);
    	}
    }


    // ==========================================
    
	
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_search_result_list, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Taxon action:");
        
        
        // We do everything that would be called in "onPrepare" right here.
        boolean picking = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
        menu.findItem(R.id.menu_taxon_accept).setVisible( picking );
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

    	long chosen_tsn = ((TaxonInfo) getListAdapter().getItem(info.position)).tsn;
    	
		switch (item.getItemId()) {
		case R.id.menu_usage_stats:
			Log.e(TAG, "Not implemented.");
			return true;
		case R.id.menu_goto:
        {
	    	Intent i = new Intent();
	    	i.putExtra(Constants.INTENT_EXTRA_TSN, chosen_tsn);
	    	
	    	// TODO: An alternate way to determine this would be to check whether
	    	// getCallingActivity() returns "null".
	    	boolean should_return = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
        	if (should_return) {
		    	i.putExtra(Constants.INTENT_EXTRA_TSN, chosen_tsn);

		    	setResult(Activity.RESULT_OK, i);

		    	finish();
		    	
        	} else {
        		i.setAction(Intent.ACTION_VIEW);
        		i.addCategory(Constants.CATEGORY_TAXON);
        		startActivity(i);
        	}

			return true;
        }
        case R.id.menu_taxon_accept:
        {
   	    	Intent i = new Intent();

   	    	i.putExtra(Constants.INTENT_EXTRA_DIRECT_CHOICE_MADE, true);
	    	i.putExtra(Constants.INTENT_EXTRA_TSN, chosen_tsn);
	    	Log.d(TAG, "TSN from direct choice in bookmark/sightings list: " + chosen_tsn);
	    	
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
            return true;
        }
		default:
			return super.onContextItemSelected(item);
		}
	}
}

