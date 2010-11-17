package org.crittr.track.activity;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.crittr.shared.browser.Constants;
import org.crittr.track.Market;
import org.crittr.track.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.kostmo.tools.StreamUtils;


public class Main extends Activity {

	// =============================================

	public static boolean check_tutorial_completion(Context context) {

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

		boolean seen_all = true;
		for (String prefkey : PrefsGlobal.dialog_prefkey_list)
			seen_all = seen_all && settings.getBoolean(prefkey, false);

		if (seen_all) {

			AlertDialog d = new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Tutorial complete")
			.setMessage("You have found all of the tutorial boxes.")
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			})
			.create();
			d.show();
		}

		return seen_all;
	}


	static final String TAG = Market.DEBUG_TAG; 

	public static String PREFERENCE_FIRST_STARTUP = "first_startup";



	//	public static String INTENT_EXTRA_PHOTO_UPLOAD_TITLE = "INTENT_EXTRA_PHOTO_UPLOAD_TITLE";
	//	public static String INTENT_EXTRA_PHOTO_UPLOAD_DESCRIPTION = "INTENT_EXTRA_PHOTO_UPLOAD_DESCRIPTION";





	public static final String NEWS_URL = "http://bugdroid.appspot.com/news";
	public static final String ABOUT_URL = "http://bugdroid.appspot.com/about";
	public static final String INSTRUCTIONS_URL = "http://bugdroid.appspot.com/instructions";


	public static final String PREFKEY_SHOW_UPLOAD_INSTRUCTIONS = "PREFKEY_SHOW_UPLOAD_INSTRUCTIONS";
	final int DIALOG_UPLOAD_INSTRUCTIONS = 1;


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


		findViewById(R.id.button_list_sightings).setOnClickListener(new IntentLauncherCallback(this, SightingsList.class));
		findViewById(R.id.button_kingdom_list).setOnClickListener(cb_taxon_intent);

//        findViewById(R.id.button_utilities).setOnClickListener(new IntentLauncherCallback(this, Utilities.class));


		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		boolean first_startup = settings.getBoolean(PREFERENCE_FIRST_STARTUP, true);

		Animation logo_fade2 = AnimationUtils.loadAnimation(this, R.anim.bug_spinner);
//		logo_fade2.setStartOffset(1000);
		findViewById(R.id.backround_image).startAnimation(logo_fade2);



//      testContentProvider();	// XXX DEBUG ONLY
	}

	// ============================================= 

	private View.OnClickListener cb_taxon_intent = new View.OnClickListener() {
		public void onClick(View v) {

			Intent intent = new Intent(Intent.ACTION_VIEW);
			//	    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tsn://202423"));	// Animalia

			intent.addCategory(Constants.CATEGORY_TAXON);
			//	    	intent.putExtra(TaxonNavigatorListActivity.INTENT_EXTRA_TSN, 202423);

			/*
	    	try {
	    		Crittr.this.startActivity(intent);
	    	} catch (ActivityNotFoundException e) {

	    	}
			 */


			if (Constants.isIntentAvailable(Main.this, intent))
				startActivity(intent);
			else {
				// Launch market intent
				Uri market_uri = Uri.parse(Market.MARKET_CRITTR_BROWSER_PACKAGE_SEARCH);
				Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
				startActivity(i);
			}
		}
	};    


	// =============================================    
	public Handler mHandler = new Handler();


	public Handler getHandler() {
		return mHandler;
	}

	// =============================================

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_main, menu);
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
		case R.id.menu_news:
		{
			Uri flickr_destination = Uri.parse( NEWS_URL );
			// Launches the standard browser.
			startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

			return true;
		}


		case R.id.menu_about:
		{
			Uri flickr_destination = Uri.parse( ABOUT_URL );
			// Launches the standard browser.
			startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

			return true;
		}
		case R.id.menu_instructions:
		{
			Uri flickr_destination = Uri.parse( INSTRUCTIONS_URL );
			// Launches the standard browser.
			startActivity(new Intent(Intent.ACTION_VIEW, flickr_destination));

			return true;
		}
		case R.id.menu_preferences:
		{
			Intent i = new Intent();
			i.setClass(this, PrefsGlobal.class);
			this.startActivity(i);

			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}











	// ========================================================
	// XXX Used to test streaming image downloads through Content Providers
	void testContentProvider() {

		final String CONTENT_PROVIDER_AUTHORITY = "com.kostmo.flickr.bettr.provider.experimental";
		//    	final String CONTENT_PROVIDER_AUTHORITY = CommonsPhoto.AUTHORITY;

		// FLICKR PHOTO IDs
		//      long photo_id = 4018945887L;	// A private photo
		//    	long photo_id = 4094507168L;	// Another public photo
		long photo_id = 4094505566L;	// Another public photo


		// COMMONS PHOTO IDs
		//    	long photo_id = 6222567L;
		//    	long photo_id = 440629L;


		Uri base_uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(CONTENT_PROVIDER_AUTHORITY).path("images").build();
		Uri my_uri = ContentUris.withAppendedId(base_uri, photo_id);
		Log.d(TAG, "Content URI: " + my_uri);
		try {

			//      	AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(my_uri, "r");
			ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(my_uri, "r");
			InputStream is = new ParcelFileDescriptor.AutoCloseInputStream( fd );
			save_file_to_disk(is, "foobar1");
			is.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Cursor cursor = managedQuery(my_uri, null, null, null, null);
		String date_column_string = "COLUMN_DATE";
		int date_col = cursor.getColumnIndex(date_column_string);

		if (cursor.moveToFirst()) {
			Date date_uploaded = new Date(cursor.getLong(date_col));
			Log.d(TAG, "Photo date: " + date_uploaded);
		}
	}
	// ========================================================

	void save_file_to_disk(InputStream is, String filename) {

		File root = Environment.getExternalStorageDirectory();
		File crittr_directory = new File(root, "experimental");
		File xml_file = new File(crittr_directory, filename);
		crittr_directory.mkdirs();
		FileOutputStream fos;
		try {

			fos = new FileOutputStream(xml_file);

			StreamUtils.copy(is, fos);

			fos.flush();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
