package org.crittr.browse.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.crittr.browse.ListAdapterTaxonSuggestions;
import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.TaxonNavigatorLinear.StateRetainer;
import org.crittr.browse.activity.prefs.PrefsTaxonSearch;
import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.itis.ItisUtils;
import org.crittr.shared.browser.provider.DatabaseTaxonomy;
import org.crittr.shared.browser.provider.TaxonSearch;
import org.crittr.shared.browser.utilities.ListAdapterTaxons;
import org.crittr.task.TaxonSearchTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;


public class ListActivityTextualSearch extends ListActivity {

	static final String TAG = Market.DEBUG_TAG; 


	final static int TAXON_CHOOSER_RETURN_CODE = 1;
	
	final static int DIALOG_RUNONCE_INSTRUCTIONS = 1;
	public final static int DIALOG_SEARCH_PROGRESS = 2;

	public ProgressDialog prog_dialog;
	public static final String PREFKEY_SHOW_TAXON_SEARCH_INSTRUCTIONS = "PREFKEY_SHOW_TAXON_SEARCH_INSTRUCTIONS";
	
	
    static final String INTENT_EXTRA_TITLE = "INTENT_EXTRA_TITLE";  
    
    DatabaseTaxonomy database;
    public Spinner kingdom_spinner;


	public Spinner rank_spinner;


	public Spinner rank_inequality_spinner;
    public Spinner search_mode_spinner;


	public Spinner name_type_spinner;

    public enum RankComparisonInequalities {AT_LEAST, AT_MOST, IS}
    
    public boolean rank_filtering_enabled = false;
    
    public NameType active_autocomplete_type = NameType.VERNACULAR;
    
    // XXX The nullity of this variable may serve as
    // a flag for a running task or not.
    TaxonSearchTask taxon_search_task = null;
    
    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.list_activity_textual_search);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);
        
        setup_rank_selector();


		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        
        
        
        ListAdapterTaxonSuggestions autocomplete_adapter = new ListAdapterTaxonSuggestions(
        		this,
        		android.R.layout.simple_dropdown_item_1line,
        		null,
        		new String[] {TaxonSearch.TaxonSuggest.COLUMN_SUGGESTION},
        		new int[] {android.R.id.text1});
        autocomplete_adapter.setStringConversionColumn(1);
        
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.taxon_searchbox);
        textView.setAdapter(autocomplete_adapter);
        
        
        
        
        
        findViewById(R.id.button_rank_filter_disable).setOnClickListener(cb_hide_rank_filter);
        
        
        
        
        
        
        View empty_area = (View) findViewById(android.R.id.empty);
        /*
        Drawable empty_search_drawable = new NonScalingBackgroundDrawable(this, getListView(), -1);
        empty_search_drawable.setAlpha(0x20);	// mostly transparent
    	empty_area.setBackgroundDrawable(empty_search_drawable);
		*/

        search_mode_spinner = (Spinner) findViewById(R.id.spinner);
        name_type_spinner = (Spinner) findViewById(R.id.spinner2);
        
        name_type_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				
				active_autocomplete_type = NameType.values()[ position ];
				
				switch (active_autocomplete_type) {
				case VERNACULAR:
					search_mode_spinner.setEnabled(true);
					break;
				case SCIENTIFIC:
					search_mode_spinner.setSelection(SearchMode.CONTAINS.ordinal());
					search_mode_spinner.setEnabled(false);
					break;
				}
			}
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        });

        findViewById(R.id.button_initiate_search).setOnClickListener(cb_initiate_search);

    	registerForContextMenu(getListView());
    	
    	
    	
    	
    	
    	
    	ListAdapterTaxons list_adapter = new ListAdapterTaxons(this, false);
    	setListAdapter( list_adapter );
    	
        // Deal with orientation change
        final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	list_adapter.taxon_list = a.taxon_list;
        	
        	list_adapter.drawable_manager.drawableMap = a.drawableMap;
        	list_adapter.drawable_manager.taxon_to_thumbnail_url_map = a.taxon_to_thumbnail_url_map;
        	list_adapter.notifyDataSetInvalidated();
        	
        	this.taxon_search_task = a.taxon_search_task;
        	// Important: we update the activity of the task
        	if (this.taxon_search_task != null)
        		this.taxon_search_task.updateActivity(this);
        	
        } else {
        	
        	Intent i = getIntent();
        	if ( Intent.ACTION_SEARCH.equals( i.getAction() ) ) {
        		String search_string = i.getStringExtra(SearchManager.QUERY);
        		((AutoCompleteTextView) findViewById(R.id.taxon_searchbox)).setText(search_string);
        		
        		this.taxon_search_task = new TaxonSearchTask(search_string, settings, this, database);
        		this.taxon_search_task.execute();
        	}
        }
    	
    	
		if (!settings.getBoolean(PREFKEY_SHOW_TAXON_SEARCH_INSTRUCTIONS, false)) {
			showDialog(DIALOG_RUNONCE_INSTRUCTIONS);
		}
    }

    // ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	
    	ListAdapterTaxons list_adapter = (ListAdapterTaxons) getListAdapter();
    	
    	StateRetainer sr = new StateRetainer();
    	sr.taxon_list = list_adapter.taxon_list;

    	sr.drawableMap = list_adapter.drawable_manager.drawableMap;
    	sr.taxon_to_thumbnail_url_map = list_adapter.drawable_manager.taxon_to_thumbnail_url_map;
    	
    	sr.taxon_search_task = taxon_search_task;
    	
        return sr;
    }
    
    // ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	super.onSaveInstanceState(out_bundle);
    	
    	Log.i(TAG, "onSaveInstanceState");
    	out_bundle.putBoolean("rank_filter_visible", rank_filtering_enabled);
    	

    	TextView searchbar = (TextView) findViewById(R.id.search_result_count);
    	out_bundle.putBoolean("search_bar_visible", searchbar.getVisibility() == View.VISIBLE);
    	out_bundle.putString("search_bar_text", searchbar.getText().toString());
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	super.onRestoreInstanceState(in_bundle);
    	
    	Log.i(TAG, "onRestoreInstanceState");
    	
    	rank_filtering_enabled = in_bundle.getBoolean("rank_filter_visible");
    	if (rank_filtering_enabled)
    	    findViewById(R.id.map_search_criteria_group).setVisibility(View.VISIBLE);

    	TextView searchbar = (TextView) findViewById(R.id.search_result_count);
    	boolean search_bar_visible = in_bundle.getBoolean("search_bar_visible");
    	if (search_bar_visible)
    		searchbar.setVisibility(View.VISIBLE);
   
    	searchbar.setText( in_bundle.getString("search_bar_text") );
    }
    
    // ========================================================================
    @Override
    protected Dialog onCreateDialog(int id) {
    	
        switch (id) {
        case DIALOG_SEARCH_PROGRESS:
        {
        	
        	Log.d(TAG, "Creating search dialog in onCreateDialog(" + id + ")");
        	
        	prog_dialog = new ProgressDialog(this);
        	prog_dialog.setProgressStyle(
                    ProgressDialog.STYLE_HORIZONTAL);

        	prog_dialog.setIndeterminate(true);
        	prog_dialog.setTitle("Searching...");
        	prog_dialog.setMessage("initializing...");
        	prog_dialog.setCancelable(true);

        	return prog_dialog;
        }
        case DIALOG_RUNONCE_INSTRUCTIONS:
        {
	        LayoutInflater factory = LayoutInflater.from(this);

	        final CheckBox reminder_checkbox;
	        View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
	        reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);
	        
	        ((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_taxon_search);
	        

            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Taxon searching")
            .setView(tagTextEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

            		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ListActivityTextualSearch.this);
            		settings.edit().putBoolean(PREFKEY_SHOW_TAXON_SEARCH_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
                }
            })
            .create();
        }
        }
        
        return null;
    }

    // ========================================================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
        case DIALOG_SEARCH_PROGRESS:
        {
        	
        	Log.d(TAG, "Preparing search dialog in onPrepareDialog(" + id + ")");
        	
	        break;
        }
        default:
        	break;
        }
    }
    
    // ========================================================================
	private View.OnClickListener cb_hide_rank_filter = new View.OnClickListener() {
	    public void onClick(View v) {

            rank_filtering_enabled = false;
	        findViewById(R.id.map_search_criteria_group).setVisibility(View.GONE);
	    }
	};
    
	private View.OnClickListener cb_initiate_search = new View.OnClickListener() {
	    public void onClick(View v) {
	    	
	    	String search_string = ((AutoCompleteTextView) findViewById(R.id.taxon_searchbox)).getText().toString();
        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ListActivityTextualSearch.this);
        	
        	taxon_search_task = new TaxonSearchTask(search_string, settings, ListActivityTextualSearch.this, database);
        	taxon_search_task.execute();
	    }
	};

    // ========================================================================
    void setup_rank_selector() {


    	database = new DatabaseTaxonomy(this);
        
        kingdom_spinner = (Spinner) findViewById(R.id.spinner_kingdoms);
        rank_spinner = (Spinner) findViewById(R.id.spinner_ranks);
        rank_inequality_spinner = (Spinner) findViewById(R.id.spinner_rank_inequality);
        

        ArrayAdapter<String> kingdomArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                ItisUtils.KINGDOM_NAME_LIST);
        kingdomArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        kingdom_spinner.setAdapter(kingdomArrayAdapter);
        kingdom_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				int true_kingdom_id = ItisUtils.KINGDOM_ID_LIST[position];
				
		        List<Map.Entry<Integer, String>> these_kingdom_ranks = new ArrayList<Map.Entry<Integer, String>>(
		        		database.rank_name_key.get(true_kingdom_id).entrySet()
		        		);
		        
		        Collections.sort(these_kingdom_ranks, new Comparator<Map.Entry<Integer, String>>() {
					public int compare(Entry<Integer, String> object1, Entry<Integer, String> object2) {
						return object1.getKey().compareTo( object2.getKey() );
					}
		        });

		        String[] ranks = new String[these_kingdom_ranks.size()];
		        for (int i=0; i<ranks.length; i++)
		        	ranks[i] = these_kingdom_ranks.get(i).getValue();
		        
		        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(ListActivityTextualSearch.this,
		                android.R.layout.simple_spinner_item,
		                android.R.id.text1,
		                ranks);
		        
		        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		        
		        rank_spinner.setAdapter(spinnerArrayAdapter);

			}
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        });
    }
	
    // ========================================================================
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	

    	

    	TaxonInfo ti = (TaxonInfo) l.getAdapter().getItem(position);
    	
    	Intent i = new Intent();

    	// Note: the full hierarchy can be recovered by API methods.
    	i.putExtra(Constants.INTENT_EXTRA_TSN, ti.tsn);
    	i.putExtra(Constants.INTENT_EXTRA_RANK_NAME, ti.rank_name);
    	i.putExtra(Constants.INTENT_EXTRA_TAXON_NAME, ti.taxon_name);

    	i.setAction( getIntent().getAction() );
    	
    	// NOTE: We make the intent explicit, rather than implicit.
    	i.setClass(this, TaxonNavigatorLinear.class);
//    	i.addCategory(ListActivityTaxonNavigator.CATEGORY_TAXON);
    	
    	
    	
    	// NOTE: Since we may have entered Search directly for a result,
    	// the Taxonomy Navigator should be our slave.
    	startActivityForResult(i, TAXON_CHOOSER_RETURN_CODE);

    	/*
    	setResult(Activity.RESULT_OK, i);
    	finish();
    	*/
    }

    // ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_taxon_search, menu);

        return true;
    }
    
    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.menu_rank_filtering).setVisible( !rank_filtering_enabled );
        return true;
    }

    // ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
        {
        	showDialog(DIALOG_RUNONCE_INSTRUCTIONS);
            return true;
        }
        case R.id.menu_rank_filtering:
        {
        	rank_filtering_enabled = true;
        	
        	// Make rank filtering menu visible.

            findViewById(R.id.map_search_criteria_group).setVisibility(View.VISIBLE);
            

            break;
        }
        
        case R.id.menu_preferences:
        {
        	Intent i = new Intent();
        	i.setClass(this, PrefsTaxonSearch.class);
        	this.startActivity(i);
        	
        	return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }

    // ========================================================================
    public enum SearchMode {ENDS_WITH, BEGINS_WITH, CONTAINS}
    public enum NameType {VERNACULAR, SCIENTIFIC}

    


    
    // ========================================================================
	public void onCreateContextMenu(ContextMenu menu, View v,
	        ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.context_search_result_list, menu);
	    
	    menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
	    menu.setHeaderTitle("Taxon action:");
	    
	    
	    // We do everything that would be called in "onPrepare" right here.

//	    boolean picking = getIntent().getBooleanExtra(ListActivityBookmarks.INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
	    
	    boolean picking = getIntent().getAction().equals(Intent.ACTION_PICK);

	    menu.findItem(R.id.menu_taxon_accept).setVisible( picking && Market.isPackageInstalled(this, Market.FULL_VERSION_PACKAGE));
	}

    // ========================================================================
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
    	long chosen_tsn = ((TaxonInfo) getListAdapter().getItem(info.position)).tsn;

    	Log.d(TAG, "Selected context menu item " + item.getItemId() + " with TSN " + chosen_tsn);
		
		switch (item.getItemId()) {
		case R.id.menu_usage_stats:
		{
			/*
        	Intent i = new Intent().setClass(this, ChartPanelActivity.class);
        	this.startActivity(i);
			*/
			Log.e(TAG, "Not implemented.");
			return true;
		}
		case R.id.menu_goto:
		{
	    	Intent i = new Intent();
	    	i.putExtra(Constants.INTENT_EXTRA_TSN, chosen_tsn);
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
	
			return true;
		}
        case R.id.menu_taxon_accept:
        {
   	    	Intent i = new Intent();

   	    	// Note: the full hierarchy can be recovered by API methods.
   	    	i.putExtra(Constants.INTENT_EXTRA_DIRECT_CHOICE_MADE, true);


	    	i.putExtra(Constants.INTENT_EXTRA_TSN, chosen_tsn);
	    	Log.d(TAG, "TSN from direct choice in search results list: " + chosen_tsn);
	    	
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
            return true;
        }
		default:
			return super.onContextItemSelected(item);
		}
	}
   
    // ========================================================================
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {

	   		case TAXON_CHOOSER_RETURN_CODE:

		    	setResult(Activity.RESULT_OK, data);
		    	finish();
	   			break;
	  	   	
	   		default:
		    	break;
		   }
		}
    }
}
