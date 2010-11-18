package org.crittr.track.activity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.crittr.shared.browser.Constants;
import org.crittr.shared.browser.utilities.AsyncTaxonInfoPopulatorModified;
import org.crittr.track.CalendarPickerConstants;
import org.crittr.track.DatabaseSightings;
import org.crittr.track.Market;
import org.crittr.track.R;
import org.crittr.track.SightingsExpandableListAdapter;
import org.crittr.track.provider.SightingEventContentProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

public class SightingsList extends ExpandableListActivity {


	static final String TAG = Market.DEBUG_TAG;

	
	
	
	public static final String CONTENT_TYPE_BASE_SINGLE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/";

	public static final String CONTENT_TYPE_TAXON_SIGHTING = CONTENT_TYPE_BASE_SINGLE + "vnd.org.crittr.sighting";
	
	
	
	public static final String PREFKEY_SHOW_SIGHTINGS_INSTRUCTIONS = "PREFKEY_SHOW_SIGHTINGS_INSTRUCTIONS";
	final int DIALOG_SIGHTINGS_INSTRUCTIONS = 1;
	final int DIALOG_CONFIRM_DELETE = 2;
	final int DIALOG_IMPORT_NO_FILE_MANAGER = 3;
	final int DIALOG_CONFIRM_SIGHTING_ADD = 4;
	final int DIALOG_CONFIRM_PHOTO_ATTACHMENT = 5;
	static final int DIALOG_CALENDARPICKER_DOWNLOAD = 6;




	static final String INTENT_EXTRA_SIGHTING_ID = "INTENT_EXTRA_SIGHTING_ID";
	public static final long INVALID_SIGHTING_ID = -1;






	final int REQUEST_CODE_TAXON_CHOOSER = 1;
	final int REQUEST_CODE_SELECT_IMAGE = 2;
	final int REQUEST_CODE_SELECT_IMAGE_INTERMEDIARY = 200;
	final int REQUEST_CODE_IMPORT_FILE_CHOOSER = 3;
	final int REQUEST_CODE_EVENT_SELECTION = 4;

	long pending_sighting_id = -1;

	ResourceCursorTreeAdapter mAdapter;
	DatabaseSightings database_ref;
	AsyncTaxonInfoPopulatorModified taxon_populator;

	static final String INTENT_EXTRA_SHOW_TITLE = "INTENT_EXTRA_SHOW_TITLE";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

		setContentView(R.layout.expandable_list_activity_sightings);

		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ladybug16);


		taxon_populator = new AsyncTaxonInfoPopulatorModified(this);
		
		
//    	boolean show_title = getIntent().getBooleanExtra(INTENT_EXTRA_SHOW_TITLE, true);
//		findViewById(R.id.title_header).setVisibility(show_title ? View.VISIBLE : View.GONE);


		this.getExpandableListView().setOnChildClickListener(new OnChildClickListener() {

			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {

				Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
				int col_idx = c.getColumnIndex(DatabaseSightings.KEY_IMAGE_URI);
				long photo_id = c.getLong(col_idx);

				return true;
			}
		});


		// Set up our adapter
		//        mAdapter = new MyExpandableListAdapter();


		database_ref = new DatabaseSightings(this);

		mAdapter = new SightingsExpandableListAdapter(
				this,
				null,
				R.layout.list_item_sighting,
				R.layout.list_item_sighting_photo,
				taxon_populator,
				database_ref);
		refereshSightings();

		setListAdapter(mAdapter);
		registerForContextMenu(getExpandableListView());

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if (!settings.getBoolean(PREFKEY_SHOW_SIGHTINGS_INSTRUCTIONS, false)) {
			showDialog(DIALOG_SIGHTINGS_INSTRUCTIONS);
		}
		
		
		
		if (Intent.ACTION_INSERT_OR_EDIT.equals(getIntent().getAction())) {
			showDialog(DIALOG_CONFIRM_SIGHTING_ADD);
		}
		
		if (Intent.ACTION_ATTACH_DATA.equals(getIntent().getAction())) {
			Log.e(TAG, "Got ATTACH_DATA action with uri: " + getIntent().getData());
			showDialog(DIALOG_CONFIRM_PHOTO_ATTACHMENT);
		}		
	}




	@Override
	protected void onSaveInstanceState(Bundle out_bundle) {
		Log.i(TAG, "onSaveInstanceState");


		out_bundle.putLong("pending_sighting_id", pending_sighting_id);
	}

	@Override
	protected void onRestoreInstanceState(Bundle in_bundle) {
		Log.i(TAG, "onRestoreInstanceState");

		pending_sighting_id = in_bundle.getLong("pending_sighting_id");
	}





	void refereshSightings() {


		Cursor c = database_ref.list_sightings();
		mAdapter.changeCursor(c);


		TextView stats_header = (TextView) findViewById(R.id.sighting_list_statistics);
		int sighting_count = c.getCount();


		Date earliest = database_ref.getEarliestSightingDate();
		String stats_string = sighting_count + " sighting" + (sighting_count == 1 ? "" : "s");
		if (earliest != null) {
			String date_string = DateFormat.getDateInstance().format(earliest);
			stats_string += " starting from " + date_string  + ".";
		} else
			stats_string += ".";

		stats_header.setText(stats_string);
	}


	// ========================================================================
	void downloadLaunchCheck(Intent intent, int request_code) {
		if (CalendarPickerConstants.DownloadInfo.isIntentAvailable(this, intent))
			startActivityForResult(intent, request_code);
		else
			showDialog(DIALOG_CALENDARPICKER_DOWNLOAD);
	}

	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		switch (id) {
		case DIALOG_CALENDARPICKER_DOWNLOAD:
		{
			boolean has_android_market = Market.isIntentAvailable(this,
					CalendarPickerConstants.DownloadInfo.getMarketDownloadIntent(CalendarPickerConstants.DownloadInfo.PACKAGE_NAME_CALENDAR_PICKER));

			Log.d(TAG, "has_android_market? " + has_android_market);

			dialog.findViewById(android.R.id.button1).setVisibility(
					has_android_market ? View.VISIBLE : View.GONE);
			break;
		}
		default:
			break;
		}
	}

	// ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_CALENDARPICKER_DOWNLOAD:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.download_calendar_picker)
			.setMessage(R.string.calendar_picker_modularization_explanation)
			.setPositiveButton(R.string.download_calendar_picker_market, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(CalendarPickerConstants.DownloadInfo.getMarketDownloadIntent(CalendarPickerConstants.DownloadInfo.PACKAGE_NAME_CALENDAR_PICKER));
				}
			})
			.setNeutralButton(R.string.download_calendar_picker_web, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(new Intent(Intent.ACTION_VIEW, CalendarPickerConstants.DownloadInfo.APK_DOWNLOAD_URI));
				}
			})
			.create();
		}
		case DIALOG_CONFIRM_PHOTO_ATTACHMENT:
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Attach photo")
			.setMessage("Longpress a sighting to attach this photo to.")
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					Toast.makeText(SightingsList.this, "Longpress a sighting...", Toast.LENGTH_SHORT).show();
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					Toast.makeText(SightingsList.this, "Cancelled.", Toast.LENGTH_SHORT).show();
					finish();
				}
			});
			return builder.create();
		}
		case DIALOG_CONFIRM_SIGHTING_ADD:
		{
			final long chosen_tsn = getIntent().getLongExtra(Constants.INTENT_EXTRA_TSN, Constants.INVALID_TSN);
			final String taxon_name = getIntent().getStringExtra(Constants.INTENT_EXTRA_TAXON_NAME);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Add sighting")
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					DatabaseSightings helper = new DatabaseSightings(SightingsList.this);
					helper.recordSighting(chosen_tsn, taxon_name);

					// Now, we re-query the database.
					refereshSightings();
					Toast.makeText(SightingsList.this, "Sighting logged.", Toast.LENGTH_SHORT).show();
				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					Toast.makeText(SightingsList.this, "Cancelled.", Toast.LENGTH_SHORT).show();
					finish();
				}
			});

			if (taxon_name != null) {
				builder.setMessage("Add sighting of \"" + taxon_name + "\"?");
			}

			return builder.create();
		}
		case DIALOG_CONFIRM_DELETE:
		{
			LayoutInflater factory = LayoutInflater.from(this);

			View foo = factory.inflate(R.layout.dialog_checkable_instructions, null);

			((TextView) foo.findViewById(R.id.instructions_textview)).setText(R.string.sightings_deletion_prompt);
			final CheckBox backup_checkbox = (CheckBox) foo.findViewById(R.id.reminder_checkmark);
			backup_checkbox.setText(R.string.sightings_backup_checkbox_label);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Delete checklist")
			.setView(foo)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					boolean backup_ok = true;
					if (backup_checkbox.isChecked()) {
						backup_ok = false;
						if (export_sightings_to_sdcard("sightings_backup.xml")) {
							Log.d(TAG, "Exported!");
							backup_ok = true;
						}
					}

					if (backup_ok) {

						database_ref.wipe();

						// Refresh the list
						refereshSightings();

					} else {
						Toast.makeText(SightingsList.this, "Backup failed, deletion aborted.", Toast.LENGTH_LONG).show();
					}

				}
			})
			.setNegativeButton(R.string.alert_dialog_cancel, null)
			.create();
		}
		case DIALOG_IMPORT_NO_FILE_MANAGER:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("No file manager")
			.setMessage("File will be imported directly from \"/sdcard/sightings.xml\", unless you would like to download a file manager.")
			.setPositiveButton("Get file manager", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					// Launch market intent
					Uri market_uri = Uri.parse(Market.MARKET_FILE_MANAGER_PACKAGE_SEARCH);
					Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
					startActivity(i);
				}
			})
			.setNegativeButton("Import directly", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					if ( import_sightings_from_sdcard() ) {
						Toast.makeText(SightingsList.this, "Import successful!", Toast.LENGTH_SHORT).show();

						// Now, we re-query the database.
						refereshSightings();
					}

				}
			})
			.create();
		}

		case DIALOG_SIGHTINGS_INSTRUCTIONS:
		{
			LayoutInflater factory = LayoutInflater.from(this);

			final CheckBox reminder_checkbox;
			View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
			reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

			TextView instructions_text = (TextView) tagTextEntryView.findViewById(R.id.instructions_textview);
			instructions_text.setText(R.string.instructions_sightings);
			instructions_text.setMovementMethod(LinkMovementMethod.getInstance());

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Sightings checklist")
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(SightingsList.this);
					settings.edit().putBoolean(PREFKEY_SHOW_SIGHTINGS_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();


					Main.check_tutorial_completion(SightingsList.this);
				}
			})
			.create();
		}
		}

		return null;
	}





	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {


		MenuInflater inflater = getMenuInflater();

		int type = ExpandableListView.getPackedPositionType( ((ExpandableListContextMenuInfo)  menuInfo).packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {


			inflater.inflate(R.menu.context_sighting_photos, menu);


			menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
			menu.setHeaderTitle("Photo menu");

		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {

			inflater.inflate(R.menu.context_sighting_list, menu);

			menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
			menu.setHeaderTitle("Sighting action:");
			
			
			
			
			
			if (Intent.ACTION_ATTACH_DATA.equals(getIntent().getAction())) {
				Log.e(TAG, "Got ATTACH_DATA action with uri: " + getIntent().getData());
				
				menu.findItem(R.id.menu_attach_sighting_photo).setVisible(false);
				menu.findItem(R.id.menu_confirm_attach_photo).setVisible(true);
			}
		}
	}








	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

		int groupPos = 0, childPos = 0;

		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
			childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);



			String msg = "Child " + childPos + " clicked in group " + groupPos;
			Log.d(TAG, msg);
			//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 

			String msg = "Group " + groupPos + " clicked";
			Log.d(TAG, msg);
			//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		}


		switch (item.getItemId()) {
		case R.id.menu_confirm_attach_photo:
		{
			long sighting_id = getExpandableListAdapter().getGroupId(groupPos);
			sightingPhotoAttach(sighting_id, getIntent().getData());

			return true;
		}
		case R.id.menu_remove_sighting_photo:
		{
			//            String msg = "Child " + childPos + " clicked in group " + groupPos;

			Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPos, childPos);
			int col_idx = c.getColumnIndex(DatabaseSightings.KEY_IMAGE_URI);
			long photo_id = c.getLong(col_idx);

			long sighting_id = getExpandableListAdapter().getGroupId(groupPos);


			//   	    	int num_deletions = database_ref.unassociateSightingPhoto(sighting_id, photo_id);
			int num_deletions = database_ref.unassociateSightingPhoto(info.id);


			Log.d(TAG, "Deleted " + num_deletions + " rows.");
			// Now, we re-query the database.
			refereshSightings();


			return true;
		}
		case R.id.menu_assign_taxon:
		{
			pending_sighting_id = info.id;

			int tsn_column = 1;
			Cursor c = (Cursor) this.getExpandableListAdapter().getGroup(groupPos);
			long current_tsn = c.getLong(tsn_column);



			Intent i = new Intent(Intent.ACTION_PICK);
			i.putExtra(Constants.INTENT_EXTRA_TSN, current_tsn);
			i.addCategory(Constants.CATEGORY_TAXON);
			Constants.intentLaunchMarketFallback(this, Market.MARKET_CRITTR_BROWSER_PACKAGE_SEARCH, i, REQUEST_CODE_TAXON_CHOOSER);



			return true;
		}

		case R.id.menu_delete_sighting:
		{
			database_ref.removeSighting(info.id);

			// Now, we re-query the database.
			refereshSightings();
			return true;
		}

		case R.id.menu_propagate_taxon:
		{
			Log.e(TAG, "Not implemented");
			return true;
		}

		case R.id.menu_attach_sighting_photo:
		{



			// The ID is either of a Child or Group.
			// Since this context menu item is only visible when a
			// Group is longpressed, it must be for a Group!
			pending_sighting_id = info.id;


			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_PICK);

		    final PackageManager packageManager = getPackageManager();
			List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
			
			
			for (ResolveInfo ri : list) {
				Log.d(TAG, "POSSIBLE MATCH" + ri.loadLabel(packageManager));
			}
			
			
//			startActivityForResult(Intent.createChooser(intent, "Choose a Source"), REQUEST_CODE_SELECT_IMAGE);

			startActivityForResult(new Intent(this, PhotoSelectionIntermediary.class), REQUEST_CODE_SELECT_IMAGE_INTERMEDIARY);
			
			return true;
		}
		}
		return false;
	}






	// ========================================================================
	public static class LatLonFloat {
		public float lat, lon;

		public LatLonFloat(float lat, float lon) {
			this.lat = lat;
			this.lon = lon;
		}
	}












	// ========================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_sightings, menu);

		return true;
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_calendar:
		{
			Log.d(TAG, "Viewing events in calendar format...");
			Uri x = SightingEventContentProvider.constructUri("/x");

			Intent i = new Intent(Intent.ACTION_VIEW, x);
	    	downloadLaunchCheck(i, REQUEST_CODE_EVENT_SELECTION);
			return true;
		}
		case R.id.menu_help:
		{
			showDialog(DIALOG_SIGHTINGS_INSTRUCTIONS);
			return true;
		}
		case R.id.menu_plot_photo_coverage:
		{
			ExpandableListAdapter ad = getExpandableListAdapter();
			/*
        	for (int j=0; j<ad.getGroupCount(); j++) {
        		Cursor c = (Cursor) ad.getGroup(j);
        	}
			 */
			int total = ad.getGroupCount();
			int with_photos = database_ref.count_photo_associations();
			int without_photos = total - with_photos;


			String[] labels = new String[] {"With photos", "Without photos"};
			int[] values = new int[] {with_photos, without_photos};


			Intent i = new Intent("com.googlecode.chartdroid.intent.action.PLOT");
			i.putExtra(Intent.EXTRA_TITLE, "Photo coverage");
			i.putExtra("com.googlecode.chartdroid.intent.extra.LABELS", labels);
			i.putExtra("com.googlecode.chartdroid.intent.extra.DATA", values);


			Market.intentLaunchMarketFallback(this, Market.MARKET_CHARTDROID_PACKAGE_SEARCH, i, -1);
			return true;
		}
		case R.id.menu_preferences:
		{	
			Intent i = new Intent();
			i.setClass(this, PrefsGlobal.class);
			this.startActivity(i);
			return true;
		}
		case R.id.menu_map_sightings:
		{	
			return true;
		}
		case R.id.menu_log_sighting:
		{
			Intent i = new Intent(Intent.ACTION_PICK);
			i.addCategory(Constants.CATEGORY_TAXON);
			Constants.intentLaunchMarketFallback(this, Market.MARKET_CRITTR_BROWSER_PACKAGE_SEARCH, i, REQUEST_CODE_TAXON_CHOOSER);

			return true;
		}
		case R.id.menu_import_sightings:
		{
			import_from_uri();

			return true;
		}
		case R.id.menu_clear_sightings:
		{
			showDialog(DIALOG_CONFIRM_DELETE);

			return true;
		}        
		case R.id.menu_upload_sightings:
		{
			upload_sightings();
			return true;
		}                
		case R.id.menu_export_sightings:
		{
			if (export_sightings_to_sdcard("sightings.xml"))
				Log.d(TAG, "Exported!");
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}


	// ========================================================================
	void sightingPhotoAttach(long sighting_id, Uri photo_uri) {
		
		
		//	   			Log.d(TAG, "Selected photo id: " + photo_id);
		database_ref.associateSightingPhoto(sighting_id, photo_uri);

		// Now, we re-query the database.
		refereshSightings();

		// Expands the sighting of the photo we added
		for (int i=0; i<mAdapter.getGroupCount(); i++) {
			if (pending_sighting_id == mAdapter.getGroupId(i)) {
				getExpandableListView().expandGroup(i);
				break;
			}
		}

//		SightingsExpandableListAdapter.getHostedImageDataTest(this, data.getData());

		Toast.makeText(this, "Attached photo.", Toast.LENGTH_SHORT).show();
	}
	
	// ========================================================================
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {


		if (resultCode != Activity.RESULT_CANCELED) {
			switch (requestCode) {

			case REQUEST_CODE_IMPORT_FILE_CHOOSER:
			{

				Uri selected_uri = data.getData();
				//	  	   		Log.d(TAG, "Full URI string: " + selected_uri.toString());
				String filepath = selected_uri.getPath(); 
				//	  	   		Log.d(TAG, "URI file path: " + filepath);
				File xml_file = new File( filepath );

				if (import_sightings_from_file(xml_file)) {
					Toast.makeText(this, "Import successful!", Toast.LENGTH_SHORT).show();

					// Now, we re-query the database.
					refereshSightings();
				}

				break;
			}

			case REQUEST_CODE_TAXON_CHOOSER:
			{
				long chosen_tsn = data.getLongExtra(Constants.INTENT_EXTRA_TSN, DatabaseSightings.INVALID_TSN);
				String taxon_name = data.getStringExtra(Constants.INTENT_EXTRA_TAXON_NAME);

				DatabaseSightings helper = new DatabaseSightings(this);
				if (pending_sighting_id >= 0)
					helper.updateSightingTaxon(pending_sighting_id, chosen_tsn, taxon_name);
				else
					helper.recordSighting(chosen_tsn, taxon_name);

				// Now, we re-query the database.
				refereshSightings();
				break;
			}
			case REQUEST_CODE_SELECT_IMAGE_INTERMEDIARY:
			case REQUEST_CODE_SELECT_IMAGE:
			{

				sightingPhotoAttach(pending_sighting_id, data.getData());
				break;
			}
			case REQUEST_CODE_EVENT_SELECTION:
			{
				long id = data.getLongExtra("INTENT_EXTRA_CALENDAR_SELECTION_ID", -1);

				String message = "Selected event: " + id;
				Log.d(TAG, message);
				//	   			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
				break;
			}


			default:
				break;
			}
		}

		if (requestCode == REQUEST_CODE_TAXON_CHOOSER)
			pending_sighting_id = -1;
	}


	// ========================================================================
	boolean import_from_uri() {

		Intent intent = new Intent("org.openintents.action.PICK_FILE");
		if (Constants.isIntentAvailable(this, intent))
			startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE_CHOOSER);
		else {
			Log.e(TAG, "Intent not available.");
			showDialog(DIALOG_IMPORT_NO_FILE_MANAGER);

			return false;
		}
		return true;
	}

	// ========================================================================
	boolean import_sightings_from_file(File xml_file) {

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(xml_file);
		} catch (FileNotFoundException e) {

			Toast.makeText(this, xml_file.getAbsolutePath() + " not found!", Toast.LENGTH_SHORT).show();
			return false;
		}




		SQLiteDatabase db = database_ref.getWritableDatabase();
		db.beginTransaction();



		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document dom = null;
		try {
			builder = factory.newDocumentBuilder();
			dom = builder.parse(fis);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Element document_root = dom.getDocumentElement();






		NodeList sighting_nodes = document_root.getElementsByTagName( "sighting" );

		for (int i=0; i<sighting_nodes.getLength(); i++) {

			//			Log.d(TAG, "Sighting "+ i);

			Node sighting_node = sighting_nodes.item(i);


			ContentValues cv = new ContentValues();

			long tsn = Long.parseLong( sighting_node.getAttributes().getNamedItem("tsn").getNodeValue() );
			//	        long timestamp = Long.parseLong( sighting_node.getAttributes().getNamedItem("timestamp").getNodeValue() );
			String timestamp = sighting_node.getAttributes().getNamedItem("timestamp").getNodeValue();
			float lat = Float.parseFloat( sighting_node.getAttributes().getNamedItem("lat").getNodeValue() );
			float lon = Float.parseFloat( sighting_node.getAttributes().getNamedItem("lon").getNodeValue() );
			float accuracy = Float.parseFloat( sighting_node.getAttributes().getNamedItem("accuracy").getNodeValue() );

			cv.put(DatabaseSightings.KEY_TSN, tsn);
			cv.put(DatabaseSightings.KEY_TIMESTAMP, timestamp);	// FIXME - Kludge?
			cv.put(DatabaseSightings.KEY_LAT, lat);
			cv.put(DatabaseSightings.KEY_LON, lon);
			cv.put(DatabaseSightings.KEY_ACCURACY, accuracy);

			long sighting_id = db.insert(DatabaseSightings.TABLE_SIGHTINGS, null, cv);


			NodeList photo_nodes = ((Element) sighting_node).getElementsByTagName( "photo" );
			for (int j=0; j<photo_nodes.getLength(); j++) {

				//				Log.i(TAG, "Photo " + j);

				Node photo_node = photo_nodes.item(j);

				ContentValues cv2 = new ContentValues();
				cv2.put(DatabaseSightings.KEY_SIGHTING_ID, sighting_id);

				long photo_id = Long.parseLong( photo_node.getAttributes().getNamedItem("id").getNodeValue() );
				cv2.put(DatabaseSightings.KEY_IMAGE_URI, photo_id);

				db.insert(DatabaseSightings.TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS, null, cv2);
			}
		}

		boolean success = false;
		try {
			db.setTransactionSuccessful();
			success = true;
		} finally {
			db.endTransaction();
		}

		db.close();


		return success;

	}

	// ========================================================================
	boolean import_sightings_from_sdcard() {

		String storage_state = Environment.getExternalStorageState();
		if (!storage_state.contains("mounted")) {

			Toast.makeText(this, "SD Card not mounted!", Toast.LENGTH_SHORT).show();
			return false;
		}


		File root = Environment.getExternalStorageDirectory();
		File crittr_directory = new File(root, "crittr");
		File xml_file = new File(crittr_directory, "sightings.xml");


		return import_sightings_from_file(xml_file);
	}

	// ===================================
	StringWriter serialize_sighting_list() throws IllegalArgumentException, IllegalStateException, IOException {

		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		serializer.setOutput(writer);
		serializer.startDocument(null, null);
		XmlSerializer main_tag = serializer.startTag(null, "sightings");

		SQLiteDatabase db = database_ref.getReadableDatabase();
		Cursor c = db.query(DatabaseSightings.TABLE_SIGHTINGS,
				new String[] {
				DatabaseSightings.KEY_ROWID,
				DatabaseSightings.KEY_TSN,
				DatabaseSightings.KEY_LAT,
				DatabaseSightings.KEY_LON,
				DatabaseSightings.KEY_ACCURACY,
				DatabaseSightings.KEY_TIMESTAMP},
				null,
				null,
				null, null, null);

		int i=0;
		while (c.moveToNext()) {


//			Log.d(TAG, "Sighting " + i++);

			// KEY_ROWID, KEY_TSN, KEY_LAT, KEY_LON, KEY_ACCURACY, KEY_TIMESTAMP
			long sighting_id = c.getLong(0);
			long tsn = c.getLong(1);
			float lat = c.getFloat(2);
			float lon = c.getFloat(3);
			float accuracy = c.getFloat(4);
			String timestamp = c.getString(5);
			//    		long timestamp = c.getLong(5);



			XmlSerializer sighting_tag = main_tag.startTag(null, "sighting");

			// TODO: Do we want to save the ID?
			sighting_tag.attribute(null, "id", Long.toString(sighting_id));
			sighting_tag.attribute(null, "tsn", Long.toString(tsn));
			sighting_tag.attribute(null, "timestamp", timestamp);
			//    		sighting_tag.attribute(null, "timestamp", Long.toString(timestamp));
			sighting_tag.attribute(null, "lat", Float.toString(lat));
			sighting_tag.attribute(null, "lon", Float.toString(lon));
			sighting_tag.attribute(null, "accuracy", Float.toString(accuracy));


			Cursor d = db.query(DatabaseSightings.TABLE_PHOTOGRAPH_SIGHTING_ASSOCIATIONS,
					new String[] {
					DatabaseSightings.KEY_IMAGE_URI},
					DatabaseSightings.KEY_SIGHTING_ID + " = ?",
					new String[] {Long.toString(sighting_id)},
					null, null, null);


			int j = 0;
			while (d.moveToNext()) {

//				Log.i(TAG, "Photo " + j++);

				long photo_id = d.getLong(0);

				XmlSerializer photo_tag = sighting_tag.startTag(null, "photo");
				photo_tag.attribute(null, "id", Long.toString(photo_id));
				photo_tag.endTag(null, "photo");
			}
			sighting_tag.endTag(null, "sighting");

			d.close();
		}
		c.close();
		db.close();

		main_tag.endTag(null, "sightings");
		serializer.endDocument();
		serializer.flush();

		return writer;

	}

	// ========================================================================
	boolean upload_sightings() {

		try {
			StringWriter writer = serialize_sighting_list();


		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		Toast.makeText(this, "Uploaded.", Toast.LENGTH_LONG).show();

		return true;
	}

	// ========================================================================
	boolean export_sightings_to_sdcard(String filename) {

		String storage_state = Environment.getExternalStorageState();
		if (!storage_state.contains("mounted")) {

			Toast.makeText(this, "SD Card not mounted!", Toast.LENGTH_SHORT).show();
			return false;
		}

		File root = Environment.getExternalStorageDirectory();
		File crittr_directory = new File(root, "crittr");
		File xml_file = new File(crittr_directory, filename);
		crittr_directory.mkdirs();

		try {
			xml_file.createNewFile();
			BufferedWriter temp_output_file = new BufferedWriter(new FileWriter(xml_file), 4096);

			StringWriter writer = serialize_sighting_list();
			temp_output_file.write( writer.toString() );
			temp_output_file.flush();
			temp_output_file.close();
			writer.close();

		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}


		Toast.makeText(this, "Exported to " + xml_file.getAbsolutePath(), Toast.LENGTH_LONG).show();

		return true;

	}
}