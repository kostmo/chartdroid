package com.kostmo.flickr.activity;




import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.data.BetterMachineTagDatabase;
import com.kostmo.flickr.tasks.TagConventionFetcherTask;
import com.kostmo.flickr.tools.FlickrFetchRoutines;

public class ListActivityTagConvention extends ListActivity {


	static final String TAG = Market.DEBUG_TAG; 
	
	public static String INTENT_EXTRA_ALLOW_DIRECT_CHOICE = "INTENT_EXTRA_ALLOW_DIRECT_CHOICE";
	public static String INTENT_EXTRA_DIRECT_CHOICE_MADE = "INTENT_EXTRA_DIRECT_CHOICE_MADE";


	// Note: When started with the PICK action, we will return
	// the 3 components of the chosen machine tag in Intent extras. 
	public static String INTENT_EXTRA_MACHINE_NAMESPACE = "INTENT_EXTRA_MACHINE_NAMESPACE";
	public static String INTENT_EXTRA_MACHINE_PREDICATE = "INTENT_EXTRA_MACHINE_PREDICATE";
	public static String INTENT_EXTRA_MACHINE_VALUE = "INTENT_EXTRA_MACHINE_VALUE";
	

	
	
	FlickrFetchRoutines image_populator;

	
	BetterMachineTagDatabase helper;
	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.list_activity_bookmarks);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        
        ((TextView) findViewById(R.id.activity_title_header)).setText("Tagging Convention");
        
		helper = new BetterMachineTagDatabase(this);
        image_populator = new FlickrFetchRoutines(this);
        

        TagPossibilityListAdapter adapter = new TagPossibilityListAdapter(
        		this,
        		R.layout.list_item_possible_tag,
                null);
        
		setListAdapter(adapter);

		registerForContextMenu(getListView());
		
		new TagConventionFetcherTaskExtended(this).execute();
    }
    
    
    
    
    // ========================================================
    public class TagConventionFetcherTaskExtended extends TagConventionFetcherTask {

    	TagConventionFetcherTaskExtended(Context context) {
    		super(context);
    	}

        @Override
        public void onPostExecute(Void nothing) {
        	if (loaded_conventions) {
        		Toast.makeText(ListActivityTagConvention.this, "Loaded tag conventions.", Toast.LENGTH_SHORT).show();
        	}
        	
            Cursor c = helper.getTagPossibilityCursor();
            Log.i(TAG, "Tagging convention has X rows: " + c.getCount());
            
            ResourceCursorAdapter rca = (ResourceCursorAdapter) ListActivityTagConvention.this.getListAdapter();
            rca.changeCursor(c);
//            rca.notifyDataSetInvalidated();
        }
    }
    
    // ======================================================
    
    public class TagPossibilityListAdapter extends ResourceCursorAdapter {

    	class ViewHolderTagPossibility {
    		
    		ImageView bullet;
    		TextView label;
    	}
    	
    	
		public TagPossibilityListAdapter(Context context, int layout, Cursor c) {
			super(context, layout, c);

		}


		public MachineTag getMachineTag(Cursor cursor) {
			
			
			String namespace = cursor.getString(2);
			String predicate = cursor.getString(3);
			String value = cursor.getString(4);
			
			return new MachineTag(namespace, predicate, value);
		}
		
		@Override
		public boolean areAllItemsEnabled() {

			return false;
		}
		
		@Override
		public boolean isEnabled(int position) {
			
			Cursor c = this.getCursor();
			c.moveToPosition(position);
			return c.getInt(1) == 2;
		}
		
		
		public void bindView(View view, Context context, Cursor cursor) {

			TextView my_label_textview = (TextView) view.findViewById(R.id.tag_possibility_label);



			ImageView img = (ImageView) view.findViewById(R.id.tag_possibility_bullet);
			
			int role = cursor.getInt(1);
			
			MachineTag mt = getMachineTag(cursor);
			
			int depth = role + 1;
			
//			Log.d(TAG, "Depth: " + depth + "; " + mt.toString());
			
			switch (role) {
			case 0:

				my_label_textview.setText( mt.namespace );
				my_label_textview.setTextAppearance(ListActivityTagConvention.this, android.R.style.TextAppearance_Small);
				my_label_textview.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

				view.setBackgroundResource(android.R.drawable.dark_header);
				break;
				
			case 1:
				view.setPadding(0, 2, 0, 2);
				
				my_label_textview.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				

				view.setBackgroundDrawable(null);
				view.setBackgroundColor( getResources().getColor(R.color.very_dark_gray) );
				
				my_label_textview.setTextAppearance(ListActivityTagConvention.this, android.R.style.TextAppearance_Small);

				my_label_textview.setTextColor( getResources().getColor(R.color.flickr_blue) );
				
				my_label_textview.setText( mt.predicate );
				break;
				
			case 2:
				view.setPadding(0, 2, 0, 2);
				my_label_textview.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				
				my_label_textview.setTextAppearance(ListActivityTagConvention.this, android.R.style.TextAppearance_Small);
			
				
				view.setBackgroundDrawable(null);
				my_label_textview.setTextAppearance(ListActivityTagConvention.this, android.R.style.TextAppearance_Small);
				my_label_textview.setText( mt.value );
				
				break;
				
			default:
				break;
			}
			((LinearLayout) view).updateViewLayout(img, new LinearLayout.LayoutParams(40*depth, ViewGroup.LayoutParams.FILL_PARENT, 0));

		}
    }
    
    // ====================================================

    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
  	
    	Intent i = new Intent();
//    	i.setClass(this, ListActivityTaxonNavigator.class);
    	
    	i.setAction( getIntent().getAction() );
    	
    	TagPossibilityListAdapter a = (TagPossibilityListAdapter) l.getAdapter();
    	MachineTag m = a.getMachineTag( (Cursor) a.getItem(position) );
    	

    	i.putExtra(INTENT_EXTRA_MACHINE_NAMESPACE, m.namespace);
    	i.putExtra(INTENT_EXTRA_MACHINE_PREDICATE, m.predicate);
    	i.putExtra(INTENT_EXTRA_MACHINE_VALUE, m.value);
    	
    	setResult(Activity.RESULT_OK, i);
    	finish();
    }
    // ======================================================

/*
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_search_result_list, menu);
        
        menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
        menu.setHeaderTitle("Taxon action:");
        
        
        // We do everything that would be called in "onPrepare" right here.
        boolean picking = getIntent().getBooleanExtra(INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
        menu.findItem(R.id.menu_taxon_accept).setVisible( picking );
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch (item.getItemId()) {
		case R.id.menu_usage_stats:
		{
        	Intent i = new Intent().setClass(this, PanelStatistics.class);
        	this.startActivity(i);
        	
			return true;
		}
		case R.id.menu_goto:
        {
//	    	Intent i = new Intent(Intent.ACTION_PICK);
        	

	    	Intent i = new Intent();
	    	i.putExtra(ListActivityTaxonNavigator.INTENT_EXTRA_TSN, info.id);
	    	
	    	// TODO: An alternate way to determine this would be to check whether
	    	// getCallingActivity() returns "null".
	    	boolean should_return = getIntent().getBooleanExtra(INTENT_EXTRA_ALLOW_DIRECT_CHOICE, false);
        	if (should_return) {
		    	i.putExtra(ListActivityTaxonNavigator.INTENT_EXTRA_TSN, info.id);

		    	setResult(Activity.RESULT_OK, i);

		    	finish();
		    	
        	} else {
        		i.setAction(Intent.ACTION_VIEW);
        		i.addCategory(ListActivityTaxonNavigator.CATEGORY_TAXON);
        		startActivity(i);
        	}
	    	
	    	
//	    	i.addCategory(ListActivityTaxonNavigator.CATEGORY_TAXON);
	//    	i.setClass(PhotoTagsListActivity.this, TaxonNavigatorListActivity.class);
	//    	startActivityForResult(i, TAXON_CHOOSER_RETURN_CODE);
//	    	startActivity(i);
	    	
	    	
	    	

			return true;
        }
        case R.id.menu_taxon_accept:
        {
   	    	Intent i = new Intent();

   	    	// Note: the full hierarchy can be recovered by API methods.
   	    	i.putExtra(INTENT_EXTRA_DIRECT_CHOICE_MADE, true);

	    	i.putExtra(ListActivityTaxonNavigator.INTENT_EXTRA_TSN, info.id);
	    	
	    	Log.d(TAG, "TSN from direct choice in bookmark/sightings list: " + info.id);
	    	
	    	setResult(Activity.RESULT_OK, i);
	    	finish();
            return true;
        }
		default:
			return super.onContextItemSelected(item);
		}
	}
	
*/
	
	// =================================
	
    
    // ================================================
    
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_bookmarks, menu);
        
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {


        case R.id.menu_clear_bookmarks:
        {	

        	Log.e(TAG, "Not implemented.");
            return true;
        }

        }

        return super.onOptionsItemSelected(item);
    }
    */
    
}
