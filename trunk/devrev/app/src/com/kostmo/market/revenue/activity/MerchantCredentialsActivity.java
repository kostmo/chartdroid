package com.kostmo.market.revenue.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.gc.android.market.api.MarketFetcher;
import com.kostmo.market.revenue.R;
import com.kostmo.market.revenue.GoogleCheckoutUtils.CheckoutCredentials;
import com.kostmo.market.revenue.container.UsernamePassword;
import com.kostmo.market.revenue.task.CredentialsFetcherTask;

// ============================================================================
public class MerchantCredentialsActivity extends Activity {

	public static final String TAG = "MerchantCredentialsActivity";

	// Preference keys
	public static final String PREFKEY_SAVED_USERNAME = "PREFKEY_SAVED_USERNAME";
	public static final String PREFKEY_SAVED_PASSWORD = "PREFKEY_SAVED_PASSWORD";

	public static final String PREFKEY_SAVED_MERCHANT_ID = "PREFKEY_SAVED_MERCHANT_ID";
	public static final String PREFKEY_SAVED_MERCHANT_KEY = "PREFKEY_SAVED_MERCHANT_KEY";

	
	// Intent extras
	public static final String EXTRA_MERCHANT_ID = "EXTRA_MERCHANT_ID";	// Long
	public static final String EXTRA_MERCHANT_KEY = "EXTRA_MERCHANT_KEY";	// String
	public static final String EXTRA_ONLY_SAVING_CREDENTIALS = "EXTRA_ONLY_SAVING_CREDENTIALS";	// String
	

	public static final int DIALOG_LOGIN_INFO = 2;
	public static final int DIALOG_MERCHANT_CREDENTIALS = 3;
	
	
	
	SharedPreferences settings;
	EditText merchant_id_box, merchant_key_box;
	
	// ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_merchant_credentials);

		this.settings = PreferenceManager.getDefaultSharedPreferences(this);

		
		
		
		
		final TextView merchant_info_fetcher_blurb = (TextView) findViewById(R.id.merchant_info_fetcher_note);
		merchant_info_fetcher_blurb.setMovementMethod(LinkMovementMethod.getInstance());


		this.merchant_id_box = (EditText) findViewById(R.id.merchant_id_edit);
		this.merchant_key_box = (EditText) findViewById(R.id.merchant_key_edit);

		findViewById(R.id.button_credentials_fetcher).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_LOGIN_INFO);
			}
		});
		
		findViewById(R.id.button_obtain_records).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String merchant_id_text = merchant_id_box.getText().toString();
				long merchant_id = RevenueActivity.INVALID_MERCHANT_ID;
				if (merchant_id_text.length() > 0)
					merchant_id = Long.parseLong( merchant_id_text );

				Editor settings_editor = settings.edit();
				settings_editor.putLong(PREFKEY_SAVED_MERCHANT_ID, merchant_id);

				String merchant_key = merchant_key_box.getText().toString();
				CheckBox checkbox_save_password = (CheckBox) findViewById(R.id.checkbox_save_password);
				settings_editor.putString(PREFKEY_SAVED_MERCHANT_KEY, checkbox_save_password.isChecked() ? merchant_key : null);
				settings_editor.commit();

				
				
				Intent result = new Intent();
				result.putExtra(EXTRA_MERCHANT_ID, merchant_id);
				result.putExtra(EXTRA_MERCHANT_KEY, merchant_key);
				setResult(Activity.RESULT_OK, result);
				finish();
			}
		});
		

		boolean only_saving_credentials = getIntent().getBooleanExtra(EXTRA_ONLY_SAVING_CREDENTIALS, false);
		((Button) findViewById(R.id.button_obtain_records)).setText(only_saving_credentials ? R.string.alert_dialog_save : R.string.merchant_obtain_records);
		if (this.settings.contains(PREFKEY_SAVED_MERCHANT_ID)) {
			long merchant_id = this.settings.getLong(PREFKEY_SAVED_MERCHANT_ID, RevenueActivity.INVALID_MERCHANT_ID);
			this.merchant_id_box.setText( Long.toString(merchant_id) );
		}

		String merchant_key = this.settings.getString(PREFKEY_SAVED_MERCHANT_KEY, null);
		if (merchant_key != null)
			this.merchant_key_box.setText(merchant_key);

		findViewById(R.id.button_credentials_fetcher).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_LOGIN_INFO);
			}
		});
	}
	
	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {
		case DIALOG_LOGIN_INFO:
		{
			final EditText username_box = (EditText) dialog.findViewById(R.id.username_edit);
			String username = this.settings.getString(PREFKEY_SAVED_USERNAME, null);
			if (username == null)
				username = accountNameGrabber();
			if (username != null)
				username_box.setText(username);

			final EditText password_box = (EditText) dialog.findViewById(R.id.password_edit);
			String password = this.settings.getString(PREFKEY_SAVED_PASSWORD, null);
			if (password != null)
				password_box.setText(password);

			break;
		}
		default:
			break;
		}
	}
	

	// ========================================================================
	String accountNameGrabber() {
		AccountManager mgr = AccountManager.get(this);
		Account[] accts = mgr.getAccountsByType( MarketFetcher.ACCOUNT_TYPE_GOOGLE );
		if (accts.length > 0) {
			Account acct = accts[0];
//	        Log.d(TAG, "Name: " + acct.name + "; Type: " + acct.type);
			return acct.name;
		}
		return null;
	}
	
	// ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

		LayoutInflater factory = LayoutInflater.from(this);

		switch (id) {
		case DIALOG_LOGIN_INFO:
		{
			final View textEntryView = factory.inflate(R.layout.dialog_login_standard, null);
			final EditText username_box = (EditText) textEntryView.findViewById(R.id.username_edit);
			final EditText password_box = (EditText) textEntryView.findViewById(R.id.password_edit);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.login_dialog_title)
			.setView(textEntryView)
			.setPositiveButton(R.string.login_dialog_fetch, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					String username = username_box.getText().toString();

					Editor settings_editor = settings.edit();
					settings_editor.putString(PREFKEY_SAVED_USERNAME, username);

					String password = password_box.getText().toString();
					CheckBox checkbox_save_password = (CheckBox) textEntryView.findViewById(R.id.checkbox_save_password);
					if (checkbox_save_password.isChecked())
						settings_editor.putString(PREFKEY_SAVED_PASSWORD, password);

					settings_editor.commit();

					UsernamePassword user_pass = new UsernamePassword(username, password);
					new CredentialsFetcherTaskExtended(MerchantCredentialsActivity.this, user_pass).execute();
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, null)
			.create();
		}
		default:
			return super.onCreateDialog(id);
		}
	}

	
	// ========================================================================
	private class CredentialsFetcherTaskExtended extends CredentialsFetcherTask {

		public CredentialsFetcherTaskExtended(Context context, UsernamePassword userPass) {
			super(context, userPass);
		}

		@Override
		public void completeTask(CheckoutCredentials checkout_credentials) {

			if (checkout_credentials != null) {
				Editor editor = settings.edit();
				editor.putLong(MerchantCredentialsActivity.PREFKEY_SAVED_MERCHANT_ID, checkout_credentials.merchant_id);
				editor.putString(MerchantCredentialsActivity.PREFKEY_SAVED_MERCHANT_KEY, checkout_credentials.merchant_key);
				editor.commit();
				
				merchant_id_box.setText( Long.toString(checkout_credentials.merchant_id) );
				merchant_key_box.setText( checkout_credentials.merchant_key );
			}
		}
	}
	
	// ========================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
}