package org.crittr.browse.activity;


import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.prefs.PrefsTaxonNavigator;
import org.crittr.browse.view.TaxonTreeView;
import org.crittr.browse.view.TreeNode;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.itis.ItisUtils;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.provider.DatabaseTaxonomy.BasicTaxon;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class TaxonNavigatorRadial extends TaxonNavigatorAbstract {


	static final String TAG = Market.DEBUG_TAG; 
	
	TaxonTreeView taxon_tree;
	
	

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
	    setContentView(R.layout.taxons_visited_activity);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);

        
        

        
        
        DatabaseTaxonomy helper = new DatabaseTaxonomy(this);
        Map<Long, BasicTaxon> taxon_map = helper.getAllCachedTaxons();

        

        TaxonNode taxon_tree_root = new TaxonNode();
        Map<Long, TaxonNode> taxon_tree_map = buildTree(taxon_map, taxon_tree_root);
//        taxon_tree_map = buildTree(taxon_map, taxon_tree_root);
        
        taxon_tree = (TaxonTreeView) findViewById(R.id.taxon_tree_view);
        taxon_tree.setTree(taxon_map, taxon_tree_map, taxon_tree_root);
        

        long target_tsn = getIntent().getLongExtra(Constants.INTENT_EXTRA_TSN, Constants.INVALID_TSN);
        if (target_tsn != Constants.INVALID_TSN)
        	taxon_tree.newRoot(taxon_tree_map.get(target_tsn));

        
        registerForContextMenu(taxon_tree);
    }

    // ========================================================================
    Map<Long, TaxonNode> buildTree(Map<Long, BasicTaxon> taxon_map, TaxonNode taxon_tree_root) {

        Log.d(TAG, "Map size: " + taxon_map.size());
        
        List<Long> toplevel_tsns = new ArrayList<Long>();
        for (long tsn : ItisUtils.KINGDOM_TSN_LIST) {
//        	Log.d(TAG, "Map has toplevel " + tsn + "? " + taxon_map.containsKey(tsn));
        	toplevel_tsns.add(tsn);
        }
        
        
        Map<Long, TaxonNode> taxon_tree_map = new HashMap<Long, TaxonNode>();
        for (Entry<Long, BasicTaxon> entry : taxon_map.entrySet()) {
        	TaxonNode node = new TaxonNode();
        	node.tsn = entry.getKey();
        	taxon_tree_map.put(node.tsn, node);
        }
        // Place the root node in the map
        taxon_tree_map.put(taxon_tree_root.tsn, taxon_tree_root);
        
        for (Entry<Long, BasicTaxon> entry : taxon_map.entrySet()) {
        	TaxonNode child_node = taxon_tree_map.get(entry.getKey());
        	long parent_tsn = entry.getValue().parent;
        	if ( taxon_tree_map.containsKey( parent_tsn ) ) {
        		TaxonNode parent_node = taxon_tree_map.get( parent_tsn );
        		parent_node.addChild(child_node);
        	}
        }
        
        return taxon_tree_map;
    }

    // ========================================================================
    public class TaxonNode extends TreeNode {
    	public long tsn = Constants.NO_PARENT_ID;
    	
    	public int getChildIndex(long tsn) {
    		int index = 0;
    		for (TreeNode node : this.getChildren()) {
    			TaxonNode taxon_node = (TaxonNode) node;
    			if (taxon_node.tsn == tsn) {
    				return index;
    			}
    			index++;
    		}
    		return -1;
    	}
    }

    
    
    
    
    
    
    
    

    // ========================================================================
    // TODO / FIXME - Android 2.0
    /*
    @Override
    public void onBackPressed() {
    	if (!taxon_tree.backOut())
    		super.onBackPressed();
    }
    */

    boolean processTrackball(int keyCode) {
    	switch (keyCode) {
    	case KeyEvent.KEYCODE_DPAD_DOWN:
    	case KeyEvent.KEYCODE_DPAD_LEFT:
    		taxon_tree.rotateSelected(false);
    		break;
    	case KeyEvent.KEYCODE_DPAD_UP:
    	case KeyEvent.KEYCODE_DPAD_RIGHT:
    		taxon_tree.rotateSelected(true);
    		break;
    	case KeyEvent.KEYCODE_DPAD_CENTER:
    		taxon_tree.activateSelection();
    		break;
    	default:
    		return false;
    	}
    	return true;
    }
    
    // ========================================================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
       	return (keyCode == KeyEvent.KEYCODE_BACK && taxon_tree.backOut()) ||
       		processTrackball(keyCode) ||
       		super.onKeyDown(keyCode, event);
    }

    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        default:
        	break;
        }
    }

    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
        switch (id) {

        }
        
        return null;
    }

    
    // ========================================================================
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_taxon_list, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Taxon action:");
        
        // We do everything that would be called in "onPrepare" right here.
        /*
        boolean picking = Intent.ACTION_PICK.equals( getIntent().getAction() ); 
        menu.findItem(R.id.menu_taxon_accept).setVisible( picking && Market.isPackageInstalled(this, Market.FULL_VERSION_PACKAGE) );
        */
	}


    // ========================================================================
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		Log.d(TAG, "Context menu item id: " + info.id);
		Log.d(TAG, "Context menu item position: " + info.position);
		
		switch (item.getItemId()) {
		/*
		case R.id.menu_taxon_photos:
		{
	    	Intent i = new Intent();
	    	i.setClass(this, TabActivityTaxonExtendedInfo.class);
   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, tsn);
	    	startActivity(i);
	    	break;
		}
		case R.id.menu_info_blurb:
		{
			new WikiBlurbGetterTask(this, taxon, tsn).execute();
	    	break;
		}
		case R.id.menu_wikipedia:
		{
			String taxon_name = TabActivityTaxonExtendedInfo.get_taxon_name_now(this, taxon, tsn);

	        String wikipedia_url = WikiBlurbGetterTask.WIKIPEDIA_MOBILE_BASE + taxon_name;
	    	Uri destination = Uri.parse( wikipedia_url );
	    	// Launches the standard browser.
	    	startActivity(new Intent(Intent.ACTION_VIEW, destination));
	    	break;
		}
		case R.id.menu_prefer_thumbnail:
		{
			
			// Prefer globally
			// TODO: Implement me
//			initiate_appengine_tsn_popularity_update(tsn);
			
			
			// Prefer locally
			DatabaseTaxonomy helper = new DatabaseTaxonomy(this);
			
			String current_thumbnail_url = ((ListAdapterTaxons) getListView().getAdapter()).getCurrentThumbnailURL(info.position);
//			Log.d(TAG, "Current thumbnail URL: " + current_thumbnail_url);
			
			if (current_thumbnail_url != null)
				helper.markPreferredThumbnail(tsn, current_thumbnail_url);
			
			break;
		}
		case R.id.menu_taxon_accept:
		{
			if ( ((ApplicationState) getApplication() ).hasPaid() ) {

	   	    	Intent i = new Intent();

	   	    	// Note: the full hierarchy can be recovered by API methods.
	   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, tsn);
		    	setResult(Activity.RESULT_OK, i);
		    	finish();
				
			} else {
				globally_stored_feature_nag = false;
				globally_stored_disabled_function_description = getResources().getString(R.string.disabled_generic);
				showDialog(DIALOG_PURCHASE_MESSAGE);
			}
			
	    	break;
		}
		case R.id.menu_taxon_log_sighting:
		{
			if ( ((ApplicationState) getApplication() ).hasPaid() ) {

				
				String taxon_name = TabActivityTaxonExtendedInfo.get_taxon_name_now(this, taxon, tsn);
				
				
	   	    	Intent i = new Intent();
	
	   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, tsn);
	   	    	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, taxon_name);
	   	    	String mime_type = "vnd.android.cursor.item/vnd.org.crittr.sighting";
	   	    	i.setType(mime_type);
	   	    	i.setAction(Intent.ACTION_INSERT_OR_EDIT);
	   	    	startActivity(i);
//		    	setResult(Activity.RESULT_OK, i);
//		    	finish();
				
			} else {
				globally_stored_feature_nag = false;
				globally_stored_disabled_function_description = getResources().getString(R.string.disabled_generic);
				showDialog(DIALOG_PURCHASE_MESSAGE);
			}
			break;
		}
		*/
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

    
    // ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_crittr, menu);
        return true;
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        return true;
    }

    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        
        case R.id.menu_about:
        {
        	
        	/*
        	Intent i = new Intent();
        	i.setClass(this, HelpAbout.class);
        	this.startActivity(i);
        	*/
        	
        	
			Uri flickr_destination = Uri.parse( Main.ABOUT_URL );
        	// Launches the standard browser.
        	startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));
        	
            return true;
        }
        case R.id.menu_glossary:
        {
        	Intent i = new Intent();
        	i.setClass(this, HelpGlossary.class);
        	this.startActivity(i);
            return true;
        }
        
        case R.id.menu_preferences:
        {
        	Intent i = new Intent();
        	i.setClass(this, PrefsTaxonNavigator.class);
        	startActivity(i);
            return true;
        }
        
        case R.id.menu_more_apps:
        {
	    	Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
	    	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
	    	startActivity(i);
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }

    // ========================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(request " + requestCode
              + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, "Action cancelled!", Toast.LENGTH_SHORT);
            t.show();
            return;
        }
 
  	   	switch (requestCode) {
   		default:
   			super.onActivityResult(requestCode, resultCode, data);
	    	break;
  	   	}
    }

    // ========================================================================
	@Override
	long getCurrentTSN() {
		return taxon_tree.getCurrentRoot().tsn;
	}
}
