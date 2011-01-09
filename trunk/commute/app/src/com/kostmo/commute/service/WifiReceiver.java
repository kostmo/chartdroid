package com.kostmo.commute.service;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
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
		
		
		String details = "";
    	WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		
        if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(intent.getAction())) {
        	
        	Log.e(TAG, "Received action: " + intent.getAction());

        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());

        	
        	details = Integer.toString( intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0) );
        	
        	
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());

        	NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        	if (ni.isConnected()) {
            	details = intent.getStringExtra(WifiManager.EXTRA_BSSID);
        	}
        	
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());
        	
        	List<ScanResult> scan_results = wm.getScanResults();
        	
        	String concatenated = "";
        	for (ScanResult result : scan_results) {
        		Log.d(TAG, "Network: " + result.SSID);
        		concatenated += "," + result.SSID;
        	}
        	
        	details = concatenated;getResultCode();

        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {

        	details = "Current: " + Integer.toString(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1));
        	details += "; Previous: " + Integer.toString(intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, -1));

        	/*
        	WifiManager.WIFI_STATE_DISABLED
        	WifiManager.WIFI_STATE_DISABLING
        	WifiManager.WIFI_STATE_ENABLED
        	WifiManager.WIFI_STATE_ENABLING
        	WifiManager.WIFI_STATE_UNKNOWN
        	*/
        	
        	Log.e(TAG, "Received action: " + intent.getAction());
        	
        } else if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());

        	boolean connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);

        	details = "" + connected;
        
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())) {

        	Log.e(TAG, "Received action: " + intent.getAction());

        	SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
        	details = "State: " + state.name();

        	details += "; Error: " + intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
        }
        
        DatabaseCommutes database = new DatabaseCommutes(context);
        database.storeWifiEvent(intent.getAction(), details);
    }
}
