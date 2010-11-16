package org.crittr.browse.activity;

import org.crittr.browse.ApplicationState;
import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.prefs.PrefsTaxonNavigator;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;


abstract public class TaxonNavigatorAbstract extends Activity {
	
	static final String TAG = Market.DEBUG_TAG;
	
	boolean globally_stored_feature_nag = false;
	String globally_stored_disabled_function_description;

    public static final String PREFKEY_SHOW_BACK_BUTTON_INSTRUCTIONS = "PREFKEY_SHOW_BACK_BUTTON_INSTRUCTIONS";
    

    
    public static final String INTENT_EXTRA_LAUNCHED_FROM_NAVIGATOR = "INTENT_EXTRA_LAUNCHED_FROM_NAVIGATOR";
    

    // =============================================

	final int DIALOG_BACK_KEY_INSTRUCTIONS = 1;
	final int DIALOG_PURCHASE_MESSAGE = 2;
	

	final static int APPENGINE_FETCH_RETURN_CODE = 1;
	final static int TAXON_CHOOSER_RETURN_CODE = 2;
	final static int TEXTUAL_SEARCH_RETURN_CODE = 3;
	final static int BOOKMARK_OR_SIGHTINGS_RETURN_CODE = 4;
    
    
    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	
    	
        switch (id) {
        case DIALOG_PURCHASE_MESSAGE:
        {
	        TextView feature_overview_blurb = (TextView) dialog.findViewById(R.id.disabled_function_description);
	        feature_overview_blurb.setText(Html.fromHtml(globally_stored_disabled_function_description), TextView.BufferType.SPANNABLE);
	        feature_overview_blurb.setMovementMethod(LinkMovementMethod.getInstance());

	        break;
        }
        default:
        	break;
        }
    }

    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        switch (id) {

        case DIALOG_PURCHASE_MESSAGE:
        {
        	
        	// NOTE: This dialog is customized differently from the others.
        	
	        View tagTextEntryView = factory.inflate(R.layout.dialog_purchase_nagger, null);

	        TextView feature_overview_blurb = (TextView) tagTextEntryView.findViewById(R.id.disabled_function_description);
	        feature_overview_blurb.setText(Html.fromHtml(globally_stored_disabled_function_description), TextView.BufferType.SPANNABLE);
	        feature_overview_blurb.setMovementMethod(LinkMovementMethod.getInstance());
	        
	        tagTextEntryView.findViewById(R.id.purchase_nag_secondary_text).setVisibility(View.GONE);
	        
	        
	        

            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.purchase_main_dialog_title)
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.purchase_button_message, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
	
                	// Launch market intent
                	Uri market_uri = Uri.parse(Market.MARKET_PACKAGE_SEARCH);
                	Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
                	startActivity(i);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
	
                }
            })
            .create();
        }
        case DIALOG_BACK_KEY_INSTRUCTIONS:
        {

	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

	        ((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_taxonav);
	        
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Taxon Selection")
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

            		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(TaxonNavigatorAbstract.this);
            		settings.edit().putBoolean(PREFKEY_SHOW_BACK_BUTTON_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
                }
            })
            .create();
        }
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
        
        boolean picking = Intent.ACTION_PICK.equals( getIntent().getAction() );
        menu.findItem(R.id.menu_taxon_accept).setVisible( picking && Market.isPackageInstalled(this, Market.FULL_VERSION_PACKAGE) );
	}

    // ========================================================================
//    abstract String getSelectedTaxonName(int position);

    // ========================================================================
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
//		TaxonInfo taxon = ((ListAdapterTaxons) getListView().getAdapter()).taxon_list.get(info.position);
//		String taxon_name = getSelectedTaxonName(info.position);
		long tsn = info.id;

		
		switch (item.getItemId()) {
		case R.id.menu_taxon_photos:
		{
	    	Intent i = new Intent();
	    	i.setClass(this, TabActivityTaxonExtendedInfo.class);
   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, tsn);
	    	startActivity(i);
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
        inflater.inflate(R.menu.options_taxon_list, menu);
        return true;
    }

    Menu stashed_options_menu;

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        stashed_options_menu = menu;
        
        boolean picking = Intent.ACTION_PICK.equals( getIntent().getAction() ); 
        boolean not_kingdom = getCurrentTSN() != Constants.INVALID_TSN;
        menu.findItem(R.id.menu_option_taxon_accept).setVisible( picking && not_kingdom );

		menu.findItem(R.id.menu_bookmark).setVisible( getCurrentTSN() != Constants.INVALID_TSN );

        return true;
    }
    

    // ========================================================================
    void handle_radio_selection(MenuItem item) {
    	item.setChecked(true);
    	
    	MenuItem base = stashed_options_menu.findItem(R.id.menu_option_search);
    	base.setTitle(item.getTitle());
    	base.setIcon(item.getIcon());
    	base.setTitleCondensed(item.getTitleCondensed());
    }

    // ========================================================================
    abstract long getCurrentTSN();
    
    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	

        boolean picking = Intent.ACTION_PICK.equals( getIntent().getAction() );
    	
        switch (item.getItemId()) {
        case R.id.menu_help:
        {
        	showDialog(DIALOG_BACK_KEY_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_popular_taxons:
        {

   	    	Intent i = new Intent();

   	    	i.putExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, picking);

   	    	// This doesn't work, because we can't passed "finish()" results from embedded activities in the ActivityGroup.
//   	    	i.putExtra(TabActivitySupplementaryTaxonLists.INTENT_EXTRA_SUPPLEMENT_TYPE, TabActivitySupplementaryTaxonLists.SupplementaryList.POPULAR.ordinal());
//   	    	i.setClass(this, TabActivitySupplementaryTaxonLists.class);
   	    	
   	    	i.setClass(this, ListActivityPopularTaxons.class);
   	    	
   	    	startActivityForResult(i, BOOKMARK_OR_SIGHTINGS_RETURN_CODE);
        	return true;
        }	
        	
        
        case R.id.menu_bookmark:
        {
        	// This check should be redundant if the "OnPrepareOptionsMenu" works correctly
        	if (getCurrentTSN() != Constants.INVALID_TSN) {
				DatabaseTaxonomy helper = new DatabaseTaxonomy(this);
				helper.add_or_update_tsn_bookmark(getCurrentTSN());
				
	        	Toast.makeText(this, "Bookmarked.", Toast.LENGTH_SHORT).show();
	            return true;
        	}
        }	
        case R.id.menu_view_bookmarks:
        {	
   	    	Intent i = new Intent();
//   	    	i.putExtra(TabActivitySupplementaryTaxonLists.INTENT_EXTRA_SUPPLEMENT_TYPE, TabActivitySupplementaryTaxonLists.SupplementaryList.BOOKMARKS.ordinal());
//   	    	i.setClass(this, TabActivitySupplementaryTaxonLists.class);
   	    	
   	    	i.putExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, picking);

   	    	i.setClass(this, ListActivityBookmarks.class);

   	    	startActivityForResult(i, BOOKMARK_OR_SIGHTINGS_RETURN_CODE);
        	return true;
        }
        
        case R.id.menu_finished:
        {
        	finish();
        	return true;
        }	
        case R.id.menu_option_taxon_info:
        {
	    	Intent i = new Intent();
	    	i.setClass(this, TabActivityTaxonExtendedInfo.class);
   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, getCurrentTSN());
	    	startActivity(i);

            return true;
        }
        case R.id.menu_option_taxon_accept:
        {
   	    	Intent i = new Intent();
   	    	i.putExtra(Constants.INTENT_EXTRA_TSN, getCurrentTSN());
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
            return true;
        }
        case R.id.menu_option_search:
        {
	    	Intent i = new Intent();
	    	i.setClass(this, ListActivityTextualSearch.class);

   	    	i.putExtra(Constants.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, picking);
	    	startActivityForResult(i, TEXTUAL_SEARCH_RETURN_CODE);
        	
            return true;
        }
        case R.id.menu_preferences:
        {	
        	Intent i = new Intent();
        	i.setClass(this, PrefsTaxonNavigator.class);
        	startActivity(i);
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }
}
