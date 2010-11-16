package com.kostmo.flickr.activity;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;

import com.aetrion.flickr.tags.Tag;
import com.kostmo.flickr.bettr.Market;


public class BasicTagListActivity extends ListActivity {

	static final String TAG = Market.DEBUG_TAG; 

	public static final long INVALID_PHOTO_ID = -1;

	public static final int DIALOG_STANDARD_TAG_CREATION = 2;
	public static final int DIALOG_NEW_TAG_PROMPT = 3;
	public static final int DIALOG_CUSTOM_MACHINE_TAG_CREATION = 4;
    


	public static enum TagAdditionActions {PLAIN_TAG, MACHINE_TAG_CUSTOM, MACHINE_TAG_PREDEFINED};

	Tag globally_stored_tag_to_change = null;
	// ================================================
	
	Tag getCurrentManipulatingTag() {
		
		return globally_stored_tag_to_change;
	}
	
	// ================================================
	

	

    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	super.onSaveInstanceState(out_bundle);
    	Log.i(TAG, "onSaveInstanceState");
//      out_bundle.putString("tag_header_text", (String) ((TextView) findViewById(R.id.tag_list_header)).getText());
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	super.onRestoreInstanceState(in_bundle);
    	Log.i(TAG, "onRestoreInstanceState");
//    	((TextView) findViewById(R.id.tag_list_header)).setText( in_bundle.getString("tag_header_text") ) ;
    }
}
