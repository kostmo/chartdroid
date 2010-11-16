package com.kostmo.market.revenue.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.kostmo.market.revenue.GoogleCheckoutUtils;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.GoogleCheckoutUtils.CheckoutCredentials;
import com.kostmo.market.revenue.GoogleCheckoutUtils.MerchantCredentialsNotFoundException;
import com.kostmo.market.revenue.container.UsernamePassword;

abstract public class CredentialsFetcherTask extends AsyncTask<Void, Void, CheckoutCredentials> {

	static final String TAG = Market.TAG;

	protected ProgressDialog wait_dialog;
	protected Context context;
	UsernamePassword user_pass;

	String current_progress_message;
	String error_message;
	
	
	// ========================================================================
	public CredentialsFetcherTask(Context context, UsernamePassword user_pass) {
		this.context = context;
		this.user_pass = user_pass;
	}

	// ========================================================================
	@Override
	public void onPreExecute() {

		this.wait_dialog = new ProgressDialog(this.context);
		String message = this.context.getResources().getString(R.string.merchant_fetching_credentials);
		this.wait_dialog.setMessage(message);	// Needs to be initialized with a String to reserve space		
		this.wait_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.wait_dialog.setCancelable(false);
		this.wait_dialog.show();
	}

	// ========================================================================
	@Override
	protected CheckoutCredentials doInBackground(Void... voided) {

		CheckoutCredentials checkout_credentials = null;
		try {
			checkout_credentials = GoogleCheckoutUtils.recoverCheckoutCredentials(this.user_pass);
		} catch (MerchantCredentialsNotFoundException e) {
			this.error_message = e.getMessage();
		}
		return checkout_credentials;
	}
	
	// ========================================================================
	@Override
	public void onPostExecute(CheckoutCredentials checkout_credentials) {

		this.wait_dialog.dismiss();

		if (this.error_message != null) {
			Toast.makeText(this.context, error_message, Toast.LENGTH_LONG).show();
			completeTask(checkout_credentials);
		} else {
			completeTask(checkout_credentials);
		}
	}

	// ========================================================================
	public abstract void completeTask(CheckoutCredentials checkout_credentials);
}