package com.kostmo.flickr.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;

import com.kostmo.flickr.adapter.ListAdapterTagSuggestions;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.MachineTag.TagPart;
import com.kostmo.flickr.data.DatabaseSearchHistory;

public class MachineTagActivity extends Activity {

	static final String TAG = "MachineTagActivity"; 
	
	
	AutoCompleteTextView namespace_box, predicate_box, value_box;
	CheckBox namespace_wildcard_checkmark, predicate_wildcard_checkmark, value_wildcard_checkmark;
	
    // ========================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
	    setContentView(R.layout.machine_tag_activity);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        

    	DatabaseSearchHistory helper = new DatabaseSearchHistory(this);
 
        
        this.namespace_box = (AutoCompleteTextView) findViewById(R.id.namespace_edit);
        ListAdapterTagSuggestions sca = new ListAdapterTagSuggestions(
        		this,
        		android.R.layout.simple_dropdown_item_1line,
        		new String[] {DatabaseSearchHistory.KEY_NAMESPACE},
        		new int[] {android.R.id.text1},
        		helper,
        		TagPart.NAMESPACE,
        		true,
        		false);
        sca.setStringConversionColumn(1);
        this.namespace_box.setAdapter(sca);
        
        this.predicate_box = (AutoCompleteTextView) findViewById(R.id.predicate_edit);
        ListAdapterTagSuggestions sca2 = new ListAdapterTagSuggestions(
        		this,
        		android.R.layout.simple_dropdown_item_1line,
        		new String[] {DatabaseSearchHistory.KEY_PREDICATE},
        		new int[] {android.R.id.text1},
        		helper,
        		TagPart.PREDICATE,
        		true,
        		false);
        sca2.setStringConversionColumn(1);
        this.predicate_box.setAdapter(sca2);
        
        this.value_box = (AutoCompleteTextView) findViewById(R.id.value_edit);
        ListAdapterTagSuggestions sca3 = new ListAdapterTagSuggestions(
        		this,
        		android.R.layout.simple_dropdown_item_1line,
        		new String[] {DatabaseSearchHistory.KEY_VALUE},
        		new int[] {android.R.id.text1},
        		helper,
        		TagPart.VALUE,
        		true,
        		false);
        sca3.setStringConversionColumn(1);
        this.value_box.setAdapter(sca3);

        // TODO Use these
        this.namespace_wildcard_checkmark = (CheckBox) findViewById(R.id.namespace_wildcard_checkmark);
        this.predicate_wildcard_checkmark = (CheckBox) findViewById(R.id.predicate_wildcard_checkmark);
        this.value_wildcard_checkmark = (CheckBox) findViewById(R.id.value_wildcard_checkmark);
        

        
        Intent data = getIntent();
        if (data != null) {
        	this.namespace_box.setText( data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_NAMESPACE) );
			this.predicate_box.setText( data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_PREDICATE) );
			this.value_box.setText( data.getStringExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_VALUE) );
        }

        
        findViewById(R.id.button_confirm_machine_tag).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finishWithSelection();
			}
        });
    }

    // ======================================================================== 
    void finishWithSelection() {
        Intent result_intent = new Intent();
        
        result_intent.putExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_NAMESPACE, this.namespace_box.getText().toString());
        result_intent.putExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_PREDICATE, this.predicate_box.getText().toString());
        result_intent.putExtra(ListActivityTagConvention.INTENT_EXTRA_MACHINE_VALUE, this.value_box.getText().toString());
        
		this.setResult(RESULT_OK, result_intent);
		finish();
    }
    
    // ======================================================================== 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);

//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.options_main, menu);
//        return true;
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
        default:
        	break;
        }

        return super.onOptionsItemSelected(item);
    }
}