package com.kostmo.flickr.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.aetrion.flickr.tags.Tag;
import com.kostmo.flickr.activity.prefs.PrefsSearchOptions;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.colorsearch.SwatchAdapter;
import com.kostmo.flickr.containers.MachineTag;
import com.kostmo.flickr.containers.UserListClient;
import com.kostmo.flickr.data.DatabaseSearchHistory;
import com.kostmo.flickr.tasks.UserContactsDialogFetcherTask;
import com.kostmo.flickr.tasks.UserGroupsDialogFetcherTask;

public class TabbedSearchActivity extends Activity implements UserListClient {

	public static class GroupsList {
		CharSequence[] names, ids;
	}


	static final String TAG = Market.DEBUG_TAG; 


	public static final long INVALID_PHOTO_ID = -1;

	final int DIALOG_SEARCH_INSTRUCTIONS = 5;

	final int DIALOG_GROUPS_LIST = 7;
	final int DIALOG_USERS_LIST = 8;
	private static final int DIALOG_COLORPICKER_DOWNLOAD = 9;

	public static final String PREFKEY_SHOW_SEARCH_INSTRUCTIONS = "PREFKEY_SHOW_SEARCH_INSTRUCTIONS";

	public static final String PREFKEY_FLICKR_SEARCH_TEXT = "PREFKEY_FLICKR_SEARCH_TEXT";
	public static final String PREFKEY_FLICKR_SEARCH_GROUP_ID = "PREFKEY_FLICKR_SEARCH_GROUP_ID";
	public static final String PREFKEY_FLICKR_SEARCH_USER_ID = "PREFKEY_FLICKR_SEARCH_USER_ID";
	public static final String PREFKEY_FLICKR_SEARCH_GROUP_NAME = "PREFKEY_FLICKR_SEARCH_GROUP_NAME";
	public static final String PREFKEY_FLICKR_SEARCH_USER_NAME = "PREFKEY_FLICKR_SEARCH_USER_NAME";


	private static final int REQUEST_CODE_PICK_COLOR = 5;
	private static final int REQUEST_CODE_RECENT_SEARCH_CHOOSER = 6;
	private static final int REQUEST_CODE_TAG_SELECTION = 7;


	GroupsList globally_stored_groups_list, globally_stored_users_list;
	int selected_user_index = -1;
	int selected_group_index = -1;

	// globally_stored_color_change_index
	static final int INVALID_INDEX = -1;
	int color_change_index = INVALID_INDEX;
	Tag globally_stored_tag_to_change = null;
	String globally_stored_disabled_function_description;
	String globally_selected_group_id, globally_selected_user_id;
	String globally_selected_group_name, globally_selected_user_name;


	List<String> tags = new ArrayList<String>();

	// ================================================

	public static final String INTENT_EXTRA_SEARCH_TEXT = "EXTRA_SEARCH_TEXT";
	public static final String INTENT_EXTRA_MACHINE_TAGS = "EXTRA_MACHINE_TAGS";
	public static final String INTENT_EXTRA_STANDARD_TAGS = "EXTRA_STANDARD_TAGS";
	public static final String INTENT_EXTRA_MACHINE_TAG_ALL_MODE = "INTENT_EXTRA_MACHINE_TAG_ALL_MODE";

	public static final String INTENT_EXTRA_SELECTED_GROUP_ID = "INTENT_EXTRA_SELECTED_GROUP_ID";
	public static final String INTENT_EXTRA_SELECTED_USER_ID = "INTENT_EXTRA_SELECTED_USER_ID";
	public static final String INTENT_EXTRA_SELECTED_GROUP_NAME = "INTENT_EXTRA_SELECTED_GROUP_NAME";
	public static final String INTENT_EXTRA_SELECTED_USER_NAME = "INTENT_EXTRA_SELECTED_USER_NAME";


	public static final String INTENT_EXTRA_COLOR_LIST = "INTENT_EXTRA_COLOR_LIST";



	public static final String INTENT_EXTRA_DISABLE_COLOR_SEARCH = "INTENT_EXTRA_DISABLE_COLOR_SEARCH";


	GridView swatch_view;
	View color_adder_button, go_button;
	TextView search_phrase_textbox, user_name_holder, group_name_holder;
	SharedPreferences settings;
	DatabaseSearchHistory helper;


	Button add_tags_button;

	boolean disable_color_search = false;
	boolean search_mode_color_active = false;

	View tab1, tab2;

	void updateTabState(boolean color_tab_selected) {

		tab1.setSelected(!search_mode_color_active);
		tab2.setSelected(search_mode_color_active);

		findViewById(R.id.text_search_tab).setVisibility(!color_tab_selected ? View.VISIBLE : View.GONE);
		findViewById(R.id.color_search_tab).setVisibility(color_tab_selected ? View.VISIBLE : View.GONE);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {

		//		Log.e(TAG, "Starting onCreate() in TabSearchActivity.");

		super.onCreate(savedInstanceState);


		settings = PreferenceManager.getDefaultSharedPreferences( getBaseContext() );



		getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		//        setContentView(R.layout.dialog_search_fields);



		helper = new DatabaseSearchHistory(this);
		disable_color_search = getIntent().getBooleanExtra(INTENT_EXTRA_DISABLE_COLOR_SEARCH, false);

		if (disable_color_search) {

			setContentView(R.layout.dialog_search_fields);

		} else {
			setContentView(R.layout.tabroast_search);



			if (savedInstanceState != null)
				search_mode_color_active = savedInstanceState.getBoolean("search_mode_color_active");

			tab1 = findViewById(R.id.tab1);
			tab2 = findViewById(R.id.tab2);

			updateTabState(search_mode_color_active);


			tab1.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					search_mode_color_active = false;
					updateTabState(search_mode_color_active);
				}
			});

			tab2.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					search_mode_color_active = true;
					updateTabState(search_mode_color_active);
				}
			});
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if (!disable_color_search) {
			swatch_view = (GridView) findViewById(R.id.color_grid);
			swatch_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

					SwatchAdapter swatch_adapter = (SwatchAdapter) swatch_view.getAdapter();

					swatch_adapter.color_list.remove(position);
					swatch_adapter.notifyDataSetChanged();

					if (swatch_adapter.getCount() < SwatchAdapter.MAX_COLORS)
						color_adder_button.setEnabled(true);

					if (swatch_adapter.getCount() < 1)
						go_button.setEnabled(false);

					return true;
				}
			});
			swatch_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					color_change_index = position;
					pickColor( ((SwatchAdapter) swatch_view.getAdapter()).color_list.get(position) );

				}
			});

			color_adder_button = findViewById(R.id.color_picker_button);
			go_button = findViewById(R.id.button_execute_color_search);
			color_adder_button.setOnClickListener(cb_color_picker);
		}


		search_phrase_textbox = (TextView) findViewById(R.id.search_phrase_textbox);
		user_name_holder = (TextView) findViewById(R.id.selected_user_name_field);
		group_name_holder = (TextView) findViewById(R.id.selected_group_name_field);




		// Deal with orientation change
		final PreserveConfigurationWrapper a = (PreserveConfigurationWrapper) getLastNonConfigurationInstance();
		if (a != null) {



			this.tags = a.tags;

			if (!disable_color_search)
				swatch_view.setAdapter( (SwatchAdapter) a.swatch_adapter );

			globally_stored_users_list = a.users;
			globally_stored_groups_list = a.groups;

			selected_user_index = a.user_index;
			selected_group_index = a.group_index;


			if (selected_user_index >= 0) {
				globally_selected_user_id = (String) globally_stored_users_list.ids[selected_user_index];
				globally_selected_user_name = (String) globally_stored_users_list.names[selected_user_index];
				user_name_holder.setText( globally_stored_users_list.names[selected_user_index] );
				user_name_holder.setTextColor(Color.WHITE);
			} else {
				user_name_holder.setText( R.string.no_user_search );
				user_name_holder.setTextColor(Color.GRAY);
				globally_selected_user_id = null;
				globally_selected_user_name = null;
			}


			if (selected_group_index >= 0) {
				globally_selected_group_id = (String) globally_stored_groups_list.ids[selected_group_index];
				globally_selected_group_name = (String) globally_stored_groups_list.names[selected_group_index];
				group_name_holder.setText( globally_stored_groups_list.names[selected_group_index] );
				group_name_holder.setTextColor(Color.WHITE);
			} else {
				group_name_holder.setText( R.string.no_group_search );
				group_name_holder.setTextColor(Color.GRAY);
				globally_selected_group_id = null;
				globally_selected_group_name = null;
			}


		}
		else {
			if (!disable_color_search)
				swatch_view.setAdapter(new SwatchAdapter(this));



			// Restore the "last search" from prefs+database

			SQLiteDatabase db = helper.getReadableDatabase();
			populateTagsFromDatabase(db, -1);
			db.close();

			search_phrase_textbox.setText( settings.getString(PREFKEY_FLICKR_SEARCH_TEXT, "") );



			String group_id = settings.getString(PREFKEY_FLICKR_SEARCH_GROUP_ID, null);
			globally_selected_group_id = group_id;
			if (group_id == null) {
				group_name_holder.setText( R.string.no_group_search );
				group_name_holder.setTextColor(Color.GRAY);
			} else {
				globally_selected_group_name = settings.getString(PREFKEY_FLICKR_SEARCH_GROUP_NAME, "");
				group_name_holder.setText( globally_selected_group_name );
				group_name_holder.setTextColor(Color.WHITE);
			}

			String user_id = settings.getString(PREFKEY_FLICKR_SEARCH_USER_ID, null);
			globally_selected_user_id = user_id;
			if (user_id == null) {
				user_name_holder.setText( R.string.no_user_search );
				user_name_holder.setTextColor(Color.GRAY);
			} else {
				globally_selected_user_name = settings.getString(PREFKEY_FLICKR_SEARCH_USER_NAME, "");
				user_name_holder.setText( globally_selected_user_name );
				user_name_holder.setTextColor(Color.WHITE);
			}
		}

		findViewById(R.id.button_execute_search).setOnClickListener(cb_execute_search);

		if (!disable_color_search) {
			findViewById(R.id.button_execute_color_search).setOnClickListener(cb_execute_color_search);
		}

		this.add_tags_button = (Button) findViewById(R.id.button_add_tags);
		this.add_tags_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				Intent launch_intent = new Intent(TabbedSearchActivity.this, ListActivityTagSelection.class);
				if (tags != null) {
					launch_intent.putExtra(BatchUploaderActivity.INTENT_EXTRA_TAGS, new ArrayList<String>(tags));
				}
				startActivityForResult(launch_intent, REQUEST_CODE_TAG_SELECTION);
			}
		});


		user_name_holder.setOnClickListener(cb_user_selection);
		findViewById(R.id.button_user_search_selector).setOnClickListener(cb_user_selection);
		group_name_holder.setOnClickListener(cb_group_selection);
		findViewById(R.id.button_group_search_selector).setOnClickListener(cb_group_selection);


		if (!settings.getBoolean(PREFKEY_SHOW_SEARCH_INSTRUCTIONS, false)) {
			showDialog(DIALOG_SEARCH_INSTRUCTIONS);
		}

		refreshTagCount();
	}


	// =============================================

	@Override
	protected void onSaveInstanceState(Bundle out_bundle) {
		super.onSaveInstanceState(out_bundle);

		out_bundle.putBoolean("search_mode_color_active", search_mode_color_active);
		out_bundle.putString("disabled_function_description", globally_stored_disabled_function_description);
	}

	@Override
	protected void onRestoreInstanceState(Bundle in_bundle) {
		super.onRestoreInstanceState(in_bundle);

		// This is handled in onCreate() instead.
//		search_mode_color_active = in_bundle.getBoolean("search_mode_color_active");

		globally_stored_disabled_function_description = in_bundle.getString("disabled_function_description");
	}


	// =============================================

	class PreserveConfigurationWrapper {
		ListAdapter swatch_adapter;
		GroupsList users, groups;
		int user_index, group_index;


		List<String> tags;
	}

	// =============================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		PreserveConfigurationWrapper pcw = new PreserveConfigurationWrapper();

		pcw.tags = this.tags;

		if (!this.disable_color_search)
			pcw.swatch_adapter = (SwatchAdapter) this.swatch_view.getAdapter();

		pcw.users = this.globally_stored_users_list;
		pcw.groups = this.globally_stored_groups_list;

		pcw.user_index = this.selected_user_index;
		pcw.group_index = this.selected_user_index;

		return pcw;
	}

	// ============================================= 

	private View.OnClickListener cb_color_picker = new View.OnClickListener() {
		public void onClick(View v) {

			if (((SwatchAdapter) swatch_view.getAdapter()).getCount() < SwatchAdapter.MAX_COLORS)
				pickColor( (new Random()).nextInt() | 0xFF000000 );

		}
	};

	// =============================================    
	private View.OnClickListener cb_user_selection = new View.OnClickListener() {
		public void onClick(View v) {

			// Fetches asynchronously from Contacts, like groups.
			if (globally_stored_users_list != null)
				showDialog(DIALOG_USERS_LIST);
			else
				new UserContactsDialogFetcherTask(TabbedSearchActivity.this).execute();
		}
	};

	// =============================================    
	private View.OnClickListener cb_group_selection = new View.OnClickListener() {
		public void onClick(View v) {

			if (globally_stored_groups_list != null)
				showDialog(DIALOG_GROUPS_LIST);
			else
				new UserGroupsDialogFetcherTask(TabbedSearchActivity.this).execute();
		}
	};

	// =============================================
	private void pickColor(int passed_color) {
		Intent i = new Intent();
		i.setAction(Market.ACTION_PICK_COLOR);
		i.putExtra(Market.EXTRA_COLOR, passed_color);

		if (Market.isIntentAvailable(this, i)) {
			startActivityForResult(i, REQUEST_CODE_PICK_COLOR);
		} else {
			showDialog(DIALOG_COLORPICKER_DOWNLOAD);
		}
	}

	// =============================================    
	private View.OnClickListener cb_execute_color_search = new View.OnClickListener() {
		public void onClick(View v) {

			List<Integer> color_list = ((SwatchAdapter) swatch_view.getAdapter()).color_list;
			int[] int_colors = new int[color_list.size()];
			for (int i=0; i<int_colors.length; i++)
				int_colors[i] = color_list.get(i);

			Intent i = new Intent();
			i.putExtra(INTENT_EXTRA_COLOR_LIST, int_colors);

			setResult(Activity.RESULT_OK, i);

			finish();
		}
	};

	// =============================================    
	private View.OnClickListener cb_execute_search = new View.OnClickListener() {
		public void onClick(View v) {


			String search_string = search_phrase_textbox.getText().toString();


			// Populate these lists from the Tag ListAdapter
			ArrayList<String> searchable_standard_tags = new ArrayList<String>();
			ArrayList<String> searchable_machine_tags = new ArrayList<String>();

			for (int i=0; i<tags.size(); i++) {
				String tag_string = tags.get(i);
				if (MachineTag.checkIsParseable(tag_string))
					searchable_machine_tags.add( tag_string );
				else
					searchable_standard_tags.add( tag_string );
			}

			Intent i = new Intent();
			i.putExtra(INTENT_EXTRA_SEARCH_TEXT, search_string);
			i.putExtra(INTENT_EXTRA_STANDARD_TAGS, searchable_standard_tags);
			i.putExtra(INTENT_EXTRA_MACHINE_TAGS, searchable_machine_tags);


//	    	CheckBox all_mt_mode_checkbox = (CheckBox) findViewById(R.id.all_machine_tags_checkmark);
//	    	i.putExtra(INTENT_EXTRA_MACHINE_TAG_ALL_MODE, all_mt_mode_checkbox.isChecked() );

			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences( getBaseContext() );
			i.putExtra(INTENT_EXTRA_MACHINE_TAG_ALL_MODE, settings.getBoolean("match_all_tags", false) );

			i.putExtra(INTENT_EXTRA_SELECTED_GROUP_ID, globally_selected_group_id);
			i.putExtra(INTENT_EXTRA_SELECTED_USER_ID, globally_selected_user_id);
			i.putExtra(INTENT_EXTRA_SELECTED_GROUP_NAME, globally_selected_group_name);
			i.putExtra(INTENT_EXTRA_SELECTED_USER_NAME, globally_selected_user_name);

			setResult(Activity.RESULT_OK, i);


			// Save the "last search" to preferences
			Editor editor = settings.edit();
			editor.putString(PREFKEY_FLICKR_SEARCH_TEXT, search_string);
			editor.putString(PREFKEY_FLICKR_SEARCH_GROUP_ID, globally_selected_group_id);
			if (globally_selected_group_id != null) {
				editor.putString(PREFKEY_FLICKR_SEARCH_GROUP_NAME, group_name_holder.getText().toString());
			}
			editor.putString(PREFKEY_FLICKR_SEARCH_USER_ID, globally_selected_user_id);
			if (globally_selected_user_id != null) {
				editor.putString(PREFKEY_FLICKR_SEARCH_USER_NAME, user_name_holder.getText().toString());
			}
			editor.commit();


			finish();
		}
	};

	// =============================================

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_search, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		return !search_mode_color_active;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_help:
		{	

			showDialog(DIALOG_SEARCH_INSTRUCTIONS);
			return true;
		}
		case R.id.menu_clear_search:
		{
			user_name_holder.setText( R.string.no_user_search );
			user_name_holder.setTextColor(Color.GRAY);
			group_name_holder.setText( R.string.no_group_search );
			group_name_holder.setTextColor(Color.GRAY);
			search_phrase_textbox.setText( "" );

			globally_selected_group_id = null;
			globally_selected_user_id = null;
			globally_selected_group_name = null;
			globally_selected_user_name = null;
			this.tags.clear();

			refreshTagCount();

			return true;
		}
		case R.id.menu_preferences:
		{
			Intent i = new Intent();
			i.setClass(TabbedSearchActivity.this, PrefsSearchOptions.class);
			startActivity(i);
			return true;
		}
		case R.id.menu_search_history:
		{
			Intent i = new Intent(Intent.ACTION_PICK);
			i.setClass(this, ListActivitySearchHistory.class);
			startActivityForResult(i, REQUEST_CODE_RECENT_SEARCH_CHOOSER);

			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	// ========================================================================
	void refreshTagCount() {
		this.add_tags_button.setText(getResources().getString(R.string.tags) + " (" + this.tags.size() + ")");
	}

	// ========================================================================	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		switch (id) {
		case DIALOG_COLORPICKER_DOWNLOAD:
		{
			boolean has_android_market = Market.isIntentAvailable(this,
					Market.getMarketDownloadIntent(Market.PACKAGE_NAME_COLOR_PICKER));

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

		LayoutInflater factory = LayoutInflater.from(this);
		Log.d(TAG, "Executing onCreateDialog()");

		switch (id) {
		case DIALOG_COLORPICKER_DOWNLOAD:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.download_color_picker)
			.setMessage(R.string.color_picker_modularization_explanation)
			.setPositiveButton(R.string.download_color_picker_market, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(Market.getMarketDownloadIntent(Market.PACKAGE_NAME_COLOR_PICKER));
				}
			})
			.setNeutralButton(R.string.download_color_picker_web, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(new Intent(Intent.ACTION_VIEW, Market.APK_DOWNLOAD_URI_COLOR_PICKER));
				}
			})
			.create();
		}
		case DIALOG_USERS_LIST:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Select user")
			.setNegativeButton(R.string.no_user_search_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					selected_user_index = -1;

					user_name_holder.setText( R.string.no_user_search );
					user_name_holder.setTextColor(Color.GRAY);
					globally_selected_user_id = null;
					globally_selected_user_name = null;
				}
			})
			.setItems(globally_stored_users_list.names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					/*
	    	    	Intent i = new Intent(Intent.ACTION_VIEW);
	    	    	i.setClass(getBaseContext(), PhotoListActivity.class);


	            	i.putExtra(PhotoListActivity.INTENT_EXTRA_GROUP_ID, globally_stored_groups_list.ids[whichButton]);

	            	i.putExtra(PhotoListActivity.INTENT_EXTRA_PHOTOLIST_VIEW_MODE, PhotolistViewMode.LIST.ordinal());

	            	startActivity(i);
					 */

					selected_user_index = whichButton;

					globally_selected_user_id = (String) globally_stored_users_list.ids[whichButton];
					globally_selected_user_name = (String) globally_stored_users_list.names[whichButton];

					user_name_holder.setText( globally_selected_user_name );
					user_name_holder.setTextColor(Color.WHITE);
				}
			})
			.create();
		}
		case DIALOG_GROUPS_LIST:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Select group")
			.setNegativeButton(R.string.no_group_search_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					selected_group_index = -1;

					group_name_holder.setText( R.string.no_group_search );
					group_name_holder.setTextColor(Color.GRAY);
					globally_selected_group_id = null;
					globally_selected_group_name = null;

				}
			})

			/*
	        .setNeutralButton("Join new group", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {

	            	Log.e(TAG, "Not implemented.");
	            	// TODO
	            	Log.e(TAG, "Launch an intent that allows 'group search' here.");
	            }
	        })
			 */
			.setItems(globally_stored_groups_list.names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					/*
	    	    	Intent i = new Intent(Intent.ACTION_VIEW);
	    	    	i.setClass(getBaseContext(), PhotoListActivity.class);


	            	i.putExtra(PhotoListActivity.INTENT_EXTRA_GROUP_ID, globally_stored_groups_list.ids[whichButton]);

	            	i.putExtra(PhotoListActivity.INTENT_EXTRA_PHOTOLIST_VIEW_MODE, PhotolistViewMode.LIST.ordinal());

	            	startActivity(i);
					 */

					selected_group_index = whichButton;

					globally_selected_group_id = (String) globally_stored_groups_list.ids[whichButton];
					globally_selected_group_name = (String) globally_stored_groups_list.names[whichButton];
					group_name_holder.setText( globally_selected_group_name );
					group_name_holder.setTextColor(Color.WHITE);
				}
			})
			.create();
		}
		case DIALOG_SEARCH_INSTRUCTIONS:
		{

			final CheckBox reminder_checkbox;
			View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
			reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

			((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_color_search);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.instructions_color_search_title)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences( TabbedSearchActivity.this );
					settings.edit().putBoolean(PREFKEY_SHOW_SEARCH_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();

				}
			})
			.create();
		}
		}

		return null;
	}

	// ========================================================

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
			switch (requestCode) {
			case REQUEST_CODE_TAG_SELECTION:
			{
				this.tags = data.getStringArrayListExtra(BatchUploaderActivity.INTENT_EXTRA_TAGS);
				refreshTagCount();
				break;
			}
			case REQUEST_CODE_PICK_COLOR:
			{
				if (resultCode == RESULT_OK) {
					int color = data.getIntExtra(Market.EXTRA_COLOR, Color.BLUE);

					Log.d(TAG, "Selected color: " + color + "; Hex: " + Integer.toHexString(color).substring(2));

					if (color_change_index < 0)
						((SwatchAdapter) swatch_view.getAdapter()).color_list.add( color );
					else {
						((SwatchAdapter) swatch_view.getAdapter()).color_list.set(color_change_index, color);
						color_change_index = INVALID_INDEX;					
					}
					((SwatchAdapter) swatch_view.getAdapter()).notifyDataSetChanged();

					if (((SwatchAdapter) swatch_view.getAdapter()).getCount() >= SwatchAdapter.MAX_COLORS)
						color_adder_button.setEnabled(false);

					if (((SwatchAdapter) swatch_view.getAdapter()).getCount() > 0)
						go_button.setEnabled(true);
				}
				break;
			}
			case REQUEST_CODE_RECENT_SEARCH_CHOOSER:
			{
				long searchid = data.getLongExtra(ListActivitySearchHistory.INTENT_EXTRA_RECENT_SEARCH_ID, -1);
				if (searchid >= 0)
					populateSearchFromDatabase(searchid);

				break;
			}
			default:
				break;
			}
		}
	}

	// =============================================
	void populateTagsFromDatabase(SQLiteDatabase db, long search_id) {

		Cursor c = db.query(DatabaseSearchHistory.TABLE_STANDARD_TAGS,
				new String[] {DatabaseSearchHistory.KEY_VALUE},
				DatabaseSearchHistory.KEY_SEARCH_TAGSET + "=?",
				new String[] {Long.toString(search_id)},
				null, null, null);
		while (c.moveToNext()) {
			this.tags.add( c.getString(0) );
		}
		c.close();

		c = db.query(DatabaseSearchHistory.TABLE_MACHINE_TAG_TRIPLES,
				new String[] {
				DatabaseSearchHistory.KEY_NAMESPACE,
				DatabaseSearchHistory.KEY_PREDICATE,
				DatabaseSearchHistory.KEY_VALUE},
				DatabaseSearchHistory.KEY_SEARCH_TAGSET + "=?",
						new String[] {Long.toString(search_id)},
						null, null, null);
		while (c.moveToNext()) {
			MachineTag mt = new MachineTag(c.getString(0), c.getString(1), c.getString(2));
			this.tags.add( mt.getRaw() );
		}
		c.close();
	}

	// =============================================
	void populateSearchFromDatabase(long search_id) {

		SQLiteDatabase db = helper.getReadableDatabase();

		Cursor c = db.query(DatabaseSearchHistory.TABLE_SEARCH_HISTORY,
				new String[] {
				DatabaseSearchHistory.KEY_SEARCH_TEXT,
				DatabaseSearchHistory.KEY_GROUP_NAME,
				DatabaseSearchHistory.KEY_USER_NAME,
				DatabaseSearchHistory.KEY_GROUP_ID,
				DatabaseSearchHistory.KEY_USER_ID},
				BaseColumns._ID + "=?",
						new String[] {Long.toString(search_id)},
						null, null, null);
		if (c.moveToFirst()) {

			// Populate search fields
			search_phrase_textbox.setText(c.getString(0));

			user_name_holder.setText(c.getString(2));
			globally_selected_group_id = c.getString(3);
			globally_selected_user_id = c.getString(4);

			if (globally_selected_group_id != null) {
				globally_selected_group_name = c.getString(1);
				group_name_holder.setText( globally_selected_group_name );
				group_name_holder.setTextColor(Color.WHITE);
			} else {
				group_name_holder.setText( R.string.no_group_search );
				group_name_holder.setTextColor(Color.GRAY);
				globally_selected_group_name = null;
			}

			if (globally_selected_user_id != null) {
				globally_selected_user_name = c.getString(1);
				user_name_holder.setText( globally_selected_user_name );
				user_name_holder.setTextColor(Color.WHITE);
			} else {
				user_name_holder.setText( R.string.no_user_search );
				user_name_holder.setTextColor(Color.GRAY);
				globally_selected_user_name = null;
			}


		}
		c.close();



		populateTagsFromDatabase(db, search_id);




		db.close();
	}

	// =============================================

	@Override
	public void generateManagedUsersDialog(CharSequence[] group_names, CharSequence[] group_ids) {

		globally_stored_users_list = new GroupsList();
		globally_stored_users_list.names = group_names;
		globally_stored_users_list.ids = group_ids;

		showDialog(DIALOG_USERS_LIST);
	}

	public void generateManagedGroupsDialog(CharSequence[] group_names, CharSequence[] group_ids) {

		globally_stored_groups_list = new GroupsList();
		globally_stored_groups_list.names = group_names;
		globally_stored_groups_list.ids = group_ids;

		showDialog(DIALOG_GROUPS_LIST);

	}
}