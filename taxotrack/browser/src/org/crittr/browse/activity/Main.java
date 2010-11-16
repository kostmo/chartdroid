package org.crittr.browse.activity;


import java.net.UnknownHostException;

import org.crittr.appengine.BlacklistParser;
import org.crittr.browse.ApplicationState;
import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.prefs.PrefsTaxonNavigator;
import org.crittr.flickr.BlacklistDatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.kostmo.tools.StreamUtils;


public class Main extends Activity implements OnClickListener {

	// =============================================

	class WikimediaBlacklistFetcherTask extends AsyncTask<Void, Void, Void> {

		BlacklistDatabase db_helper;
		WikimediaBlacklistFetcherTask() {

			db_helper = new BlacklistDatabase(Main.this);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if ( !db_helper.isBlacklistDownloaded() ) {

				try {
					BlacklistParser.import_blacklist(db_helper, "image_blacklist");
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}

			}
			return null;
		}
	}


	static final String TAG = Market.DEBUG_TAG; 

	public static String PREFERENCE_FIRST_STARTUP = "first_startup";
	public static final String PREFKEY_PREVIOUS_VERSION_CODE = "PREFKEY_PREVIOUS_VERSION_CODE";



	String globally_stored_disabled_function_description;


	//	public static String INTENT_EXTRA_PHOTO_UPLOAD_TITLE = "INTENT_EXTRA_PHOTO_UPLOAD_TITLE";
	//	public static String INTENT_EXTRA_PHOTO_UPLOAD_DESCRIPTION = "INTENT_EXTRA_PHOTO_UPLOAD_DESCRIPTION";

	private static final int PHOTO_PICKED = 1;
	public static final int APPENGINE_FETCH_RETURN_CODE = 2;
	public static final int FLICKR_AUTH_FETCH_RETURN_CODE = 3;

	public static final String NEWS_URL = "http://bugdroid.appspot.com/news";
	public static final String ABOUT_URL = "http://bugdroid.appspot.com/about";
	public static final String INSTRUCTIONS_URL = "http://bugdroid.appspot.com/instructions";

	final int DIALOG_PURCHASE_MESSAGE = 1;
	final int DIALOG_PHOTO_TITLE_DESCRIPTION = 2;
	private static final int DIALOG_RELEASE_NOTES = 3;


	Uri globally_stored_photo_picked_uri;
	boolean crop_first;

	private class IntentLauncherCallback implements View.OnClickListener {

		private Context ctx;
		private Class<?> cls;
		IntentLauncherCallback(Context cx, Class<?> cs) {
			ctx = cx;
			cls = cs;
		}

		public void onClick(View v) {
			Intent i = new Intent();
			i.setClass(ctx, cls);
			ctx.startActivity(i);
		}
	}





	@Override
	protected void onSaveInstanceState(Bundle out_bundle) {
		Log.i(TAG, "onSaveInstanceState");

		if (globally_stored_photo_picked_uri != null)
			out_bundle.putString("photo_choice_uri", globally_stored_photo_picked_uri.toString());
	}

	@Override
	protected void onRestoreInstanceState(Bundle in_bundle) {
		Log.i(TAG, "onRestoreInstanceState");

		String restored_uri_string = in_bundle.getString("photo_choice_uri");
		if (restored_uri_string != null)
			globally_stored_photo_picked_uri = Uri.parse( restored_uri_string );
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);




		setContentView(R.layout.main);

		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);



		for (int button : new int[] {R.id.button_kingdom_list, R.id.button_taxon_search, R.id.button_taxon_explored})
			findViewById(button).setOnClickListener(this);


		if ( Market.SHOULD_ADVERTISE_PAID_VERSION && !((ApplicationState) getApplication() ).hasPaid() ) {

			View purchase_button = findViewById(R.id.button_purchase);
			purchase_button.setVisibility(View.VISIBLE);

			purchase_button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {


					globally_stored_disabled_function_description = "Features available in <b>full version</b>:<br/>" + Eula.readFile(Main.this, R.raw.full_version_features).toString();
					//					globally_stored_disabled_function_description = getResources().getString(R.string.purchase_features_overview);

					showDialog(DIALOG_PURCHASE_MESSAGE);
				}
			});
		}



		Eula.showEula(this);



		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);


		boolean first_startup = settings.getBoolean(PREFERENCE_FIRST_STARTUP, true);
		if (first_startup) {

		} else {
			// Run on startup
		}


		if (savedInstanceState == null) {
			// We avoid redoing these things if it's just an orientation change...

			//	        new StartupChoresTask(this).execute();
			new WikimediaBlacklistFetcherTask().execute();
			
			
			
			int current_version_code = Market.getVersionCode(this, Main.class);
			if (current_version_code > settings.getInt(PREFKEY_PREVIOUS_VERSION_CODE, -1)) {
				showDialog(DIALOG_RELEASE_NOTES);
			}
		}




		Animation logo_fade2 = AnimationUtils.loadAnimation(this, R.anim.bug_spinner);
		//		logo_fade2.setStartOffset(1000);
		findViewById(R.id.backround_image).startAnimation(logo_fade2);
	}

	// =============================================

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {


		switch (id) {
		case DIALOG_PURCHASE_MESSAGE:
		{
			TextView feature_overview_blurb = (TextView) dialog.findViewById(R.id.disabled_function_description);
			feature_overview_blurb.setText(Html.fromHtml(globally_stored_disabled_function_description), TextView.BufferType.SPANNABLE);
			feature_overview_blurb.setMovementMethod(LinkMovementMethod.getInstance());

			break;
		}
		default:
			break;
		}
	}
	// =============================================


	@Override
	protected Dialog onCreateDialog(int id) {

		LayoutInflater factory = LayoutInflater.from(this);
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		switch (id) {
		case DIALOG_RELEASE_NOTES:
		{
			CharSequence release_notes = StreamUtils.readFile(this, R.raw.release_notes);

			final int current_version_code = Market.getVersionCode(this, Main.class);
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.release_notes)
			.setMessage(release_notes)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					settings.edit().putInt(PREFKEY_PREVIOUS_VERSION_CODE, current_version_code).commit();
				}
			})
			.create();
		}
		case DIALOG_PURCHASE_MESSAGE:
		{

			// NOTE: This dialog is customized differently from the others.

			View tagTextEntryView = factory.inflate(R.layout.dialog_purchase_nagger, null);

			TextView feature_overview_blurb = (TextView) tagTextEntryView.findViewById(R.id.disabled_function_description);
			feature_overview_blurb.setText(Html.fromHtml(globally_stored_disabled_function_description), TextView.BufferType.SPANNABLE);
			feature_overview_blurb.setMovementMethod(LinkMovementMethod.getInstance());

			tagTextEntryView.findViewById(R.id.purchase_nag_secondary_text).setVisibility(View.GONE);


			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.purchase_main_dialog_title)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.purchase_button_message, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					// Launch market intent
					Uri market_uri = Uri.parse(Market.MARKET_PACKAGE_SEARCH);
					Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
					startActivity(i);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			})
			.create();
		}
		}

		return null;
	}



	// =============================================    
	public Handler mHandler = new Handler();


	public Handler getHandler() {
		return mHandler;
	}

	// =============================================

	void do_photo_upload() {


		Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
		//        intent.setType("image/*");
		intent.setType( "image/jpeg" );	// FIXME

		// Note: we could have the "crop" UI come up here by
		// default by doing this:
		//        crop_first = ((CheckBox) findViewById(R.id.crop_checkbox)).isChecked();


		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Main.this);
		crop_first = settings.getBoolean("prompt_for_crop", false);

		if (crop_first)
			intent.putExtra("crop", "true");
		// (But watch out: if you do that, the Intent that comes
		// back to onActivityResult() will have the URI (of the
		// cropped image) in the "action" field, not the "data"
		// field!)

		startActivityForResult(intent, PHOTO_PICKED);

		Toast.makeText(Main.this, "Select a photo to upload.", Toast.LENGTH_SHORT).show();
	}

	// =============================================

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_crittr, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {


		case R.id.menu_about:
		{

			/*
        	Intent i = new Intent();
        	i.setClass(this, HelpAbout.class);
        	this.startActivity(i);
			 */


			Uri flickr_destination = Uri.parse( ABOUT_URL );
			// Launches the standard browser.
			startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

			return true;
		}
		case R.id.menu_glossary:
		{
			Intent i = new Intent();
			i.setClass(this, HelpGlossary.class);
			this.startActivity(i);
			return true;
		}

		case R.id.menu_preferences:
		{
			Intent i = new Intent();
			i.setClass(this, PrefsTaxonNavigator.class);
			startActivity(i);
			return true;
		}

		case R.id.menu_more_apps:
		{
			Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
			Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
			startActivity(i);
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}


	// ==========================================



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
		case PHOTO_PICKED:
		{
			if (data == null) {
				Log.w(TAG, "Null data, but RESULT_OK, from image picker!");
				Toast t = Toast.makeText(this, "Nothing picked!",
						Toast.LENGTH_SHORT);
				t.show();
				return;
			}

			Uri true_result;
			if (crop_first)
				true_result = Uri.parse( data.getAction() );	// FIXME - Experimental!
			else
				true_result = data.getData();

			if (true_result == null) {
				Log.w(TAG, "'data' intent from image picker contained no data!");
				Toast t = Toast.makeText(this, "Nothing picked!",
						Toast.LENGTH_SHORT);
				t.show();
				return;
			}

			globally_stored_photo_picked_uri = true_result;
			showDialog(DIALOG_PHOTO_TITLE_DESCRIPTION);

			break;
		}
		default:
			break;
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button_kingdom_list:
		{
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setClass(this, TaxonNavigatorLinear.class);
			startActivity(i);
			break;
		}
		case R.id.button_taxon_search:
		{
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setClass(this, ListActivityTextualSearch.class);
			startActivity(i);
			break;
		}
		case R.id.button_taxon_explored:
		{
			Intent i = new Intent(this, TaxonNavigatorRadial.class);
			startActivity(i);
			break;
		}
		}
	}
}
