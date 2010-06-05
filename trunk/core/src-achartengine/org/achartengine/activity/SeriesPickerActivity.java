package org.achartengine.activity;

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.adapter.PlotSeriesListAdapter;
import com.googlecode.chartdroid.core.ColumnSchema;

import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;


public class SeriesPickerActivity extends ListActivity {

	static final String TAG = "SeriesPickerActivity"; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.list_activity_series_picker);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

		
		Uri series_meta_uri = getIntent().getData();
		Cursor meta_cursor = managedQuery(series_meta_uri,
				new String[] {BaseColumns._ID, ColumnSchema.Aspect.Series.COLUMN_SERIES_LABEL},
				null, null, null);
		
		setListAdapter(new PlotSeriesListAdapter(
				this,
				R.layout.list_item_series_picker,
				meta_cursor));

		
		
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		
		getListView().setOnItemClickListener(category_choice_listener);
//      category_listview.setEmptyView(findViewById(R.id.empty_categories));

//    	registerForContextMenu( category_listview );

		if (savedInstanceState != null) {
//			autocomplete_textview.setText( savedInstanceState.getString("search_text") );
		}


		final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
		if (a != null) {

		} else {

		}
	}

	// =============================================

	OnItemClickListener category_choice_listener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> adapter_view, View arg1, int position, long id) {

		}
	};    

	// =============================================

	class StateRetainer {
		Cursor cursor;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		StateRetainer state = new StateRetainer();
		state.cursor = ((CursorAdapter) getListAdapter()).getCursor();
		return state;
	}
}

