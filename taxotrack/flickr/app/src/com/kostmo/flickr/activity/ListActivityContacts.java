package com.kostmo.flickr.activity;

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

import com.aetrion.flickr.contacts.Contact;
import com.kostmo.flickr.adapter.ContactsAdapter;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.tasks.ContactsFetcherTask;

public class ListActivityContacts extends ListActivity {


	static final String TAG = Market.DEBUG_TAG;
	
	
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.list_activity_contacts);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
        

        setListAdapter(new ContactsAdapter(this));
        
//		registerForContextMenu(getListView());
        
        
        new ContactsFetcherTaskExtended(this).execute();
    }
    
    // ======================================================
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

    	Contact contact = (Contact) l.getItemAtPosition(position);
    	Intent i = new Intent();
    	i.putExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_USER_ID, contact.getId());
    	i.putExtra(TabbedSearchActivity.INTENT_EXTRA_SELECTED_USER_NAME, contact.getUsername());
    	
    	i.putExtra(PhotoListActivity.INTENT_EXTRA_MY_CONTACTS_MODE, true);
    	
    	i.setClass(getBaseContext(), PhotoListActivity.class);
    	startActivity(i);
    }
    
    // ================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        /*
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_history, menu);

        return true;
        */
        return false;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	
        switch (item.getItemId()) {
/*
        case R.id.menu_clear_history:
        {
        	DatabaseSearchHistory helper = new DatabaseSearchHistory(this);
        	helper.clear_history();
        	
        	Cursor c = helper.getSearchHistory();
        	((ResourceCursorAdapter) getListAdapter()).changeCursor(c);
        	
            return true;
        }
*/
        }
        return super.onOptionsItemSelected(item);
    }
    // ================================================
    
    class ContactsFetcherTaskExtended extends ContactsFetcherTask {


    	public ContactsFetcherTaskExtended(Context c) {
    		super(c);
    	}

        @Override
        public void onPostExecute(List<Contact> contacts) {
        	super.onPostExecute(contacts);
        	
        	if (error_message != null) {
        		String err = "Users not loaded; " + error_message;
    			Toast.makeText(context, err, Toast.LENGTH_SHORT).show();
        	} else {
        		
        		// Update the ListView
        		ContactsAdapter ad = (ContactsAdapter) getListAdapter();
        		ad.contact_list = contacts;
        		ad.notifyDataSetChanged();
        	}
        }
    }
    // ================================================
}
