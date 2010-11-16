package org.crittr.track.activity;


import org.crittr.track.Market;
import org.crittr.track.R;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PhotoSelectionIntermediary extends ListActivity {


	static final String TAG = Market.DEBUG_TAG;

	final int REQUEST_CODE_SELECT_IMAGE_INTERMEDIARY = 1;
	
	List<Map<String, ?>> application_map_list = new ArrayList<Map<String, ?>>();
	List<ResolveInfo> list;
	
	static final String KEY_APP_ICON = "KEY_APP_ICON";
	static final String KEY_APP_LABEL = "KEY_APP_LABEL";
	static final String KEY_APP_DESCRIPTION = "KEY_APP_DESCRIPTION";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);



		setContentView(R.layout.list_activity_application_intermediary);


		Intent intent = intentGenerator();

		final PackageManager packageManager = getPackageManager();
		list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);


		for (ResolveInfo ri : list) {
			Log.d(TAG, "POSSIBLE MATCH" + ri.loadLabel(packageManager));
			Map<String, Object> my_map = new HashMap<String, Object>();
			application_map_list.add(my_map);
			

			my_map.put(KEY_APP_ICON, ri.loadIcon(packageManager));
			my_map.put(KEY_APP_LABEL, ri.loadLabel(packageManager));
			my_map.put(KEY_APP_DESCRIPTION, "Foo Barbecue");
		}

		
		SimpleAdapter adapter = new SimpleAdapter(
				this,
				application_map_list,
				R.layout.list_item_application,
				new String[] {KEY_APP_ICON, KEY_APP_LABEL, KEY_APP_DESCRIPTION},
				new int[] {R.id.application_icon, R.id.application_label, R.id.application_description});
		
			
		adapter.setViewBinder(new SimpleAdapter.ViewBinder() {

			@Override
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				if (data instanceof Drawable) {
					ImageView image_view = (ImageView) view;
					image_view.setImageDrawable( (Drawable) data );
					return true;
				}
				return false;
			}
		});
			
		setListAdapter(adapter);
		getListView().setTextFilterEnabled(true);
	}

	// ========================================================================
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ResolveInfo ri = list.get(position);

		Intent i = intentGenerator();
		i.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
		
		startActivityForResult(i, REQUEST_CODE_SELECT_IMAGE_INTERMEDIARY);
	}

	// ========================================================================
	static Intent intentGenerator() {

		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		return intent;
	}

	// ========================================================================
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {


		if (resultCode != Activity.RESULT_CANCELED) {
			switch (requestCode) {
			case REQUEST_CODE_SELECT_IMAGE_INTERMEDIARY:
			{
				setResult(Activity.RESULT_OK, data);
				finish();
				
				break;
			}
			default:
				break;
			}
		}
	}

}
