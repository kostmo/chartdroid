package com.kostmo.commute.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.kostmo.commute.Market;
import com.kostmo.commute.R;
import com.kostmo.commute.provider.DatabaseCommutes;

public class TripSummaryActivity extends Activity {

    public static final String TAG = Market.TAG;

    
	DatabaseCommutes database;
	
	public static final String EXTRA_TRIP_ID = "EXTRA_TRIP_ID";
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.trip_summary);

    	this.database = new DatabaseCommutes(this);
    	
    	
    	// TODO This should all be done in a Service!
    	long trip_id = getIntent().getLongExtra(EXTRA_TRIP_ID, -1);
    	this.database.finishTrip(trip_id);
    	
    	TextView textview_trip_summary = (TextView) findViewById(R.id.textview_trip_summary);
    	textview_trip_summary.setText("foo bar");
    }


    // ======================================================================== 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_pair_associator, menu);
        return true;
    }

    // ========================================================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        return true;
    }

	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        }

        return super.onOptionsItemSelected(item);
    }
    
    

    // ========================================================================
    @Override
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
	    	break;
  	   	}
    }
}