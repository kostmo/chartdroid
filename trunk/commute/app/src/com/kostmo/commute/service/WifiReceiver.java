package com.kostmo.commute.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.kostmo.commute.Market;
import com.kostmo.commute.provider.DatabaseCommutes;

public class WifiReceiver extends BroadcastReceiver {
	
	static final String TAG = Market.TAG;

	// NOTE: We also schedule the service from within an activity,
	// because we want it to run even if the user has
	// not restarted his phone yet.
	
	public static final String PREFKEY_CHECKIN_ALARM_SCHEDULED = "PREFKEY_CHECKIN_ALARM_SCHEDULED";

	@Override
    public void onReceive(Context context, Intent intent) {
        if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(intent.getAction())) {
        	
        	Log.e(TAG, "Received action: " + intent.getAction());

        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());

        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());
        	
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());
        	
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());
        	
        } else if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());
        	
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());
        	
        }
        
        
        DatabaseCommutes database = new DatabaseCommutes(context);
        database.storeWifiEvent(intent.getAction());
    }
}
