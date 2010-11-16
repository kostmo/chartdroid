package com.kostmo.flickr.activity.prefs;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Window;
import android.widget.Toast;

import com.kostmo.flickr.activity.PhotosetsListActivity;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.graphics.NonScalingBackgroundDrawable;
import com.kostmo.flickr.tasks.UserGroupsPreferenceFetcherTask;
public class PrefsUpload extends PreferenceActivity {

	static final String TAG = Market.DEBUG_TAG; 
	

	public static final String PREFKEY_UPLOAD_ENABLE_GEOTAGGING = "geotag_uploads";
	
	public static final String PREFKEY_UPLOAD_PUBLIC = "upload_public";
	public static final String PREFKEY_UPLOAD_CONTENT_TYPE = "upload_content_type";
	public static final String PREFKEY_UPLOAD_SAFETY_LEVEL = "upload_safety_level";

	public static final String PREFKEY_UPLOAD_GROUP_AUTO_ADD = "group_auto_add";
	public static final String PREFKEY_UPLOAD_DEFAULT_GROUP = "default_upload_group";

	public static final String PREFKEY_UPLOAD_SET_AUTO_ADD = "set_auto_add";
	public static final String PREFKEY_DEFAULT_UPLOAD_SET = "default_upload_set";
	
	public static final String PREFKEY_IS_FIRST_UPLOAD = "PREFKEY_IS_FIRST_UPLOAD";

	final int REQUEST_CODE_PHOTOSET_CHOOSER = 1;


	Preference default_upload_set;
	
   @Override
   public void onCreate(Bundle savedInstanceState) {
	   
	   getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
	   super.onCreate(savedInstanceState);
	   getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

	   
       addPreferencesFromResource( R.xml.prefs_upload );

       getListView().setCacheColorHint(0);	// Required for custom background images

       
       // Approach 2: Use a custom drawable

       getListView().setCacheColorHint(0);	// Required for custom background images


       Drawable d = new NonScalingBackgroundDrawable(this, getListView(), -1);
       d.setAlpha(0x20);	// mostly transparent
       getListView().setBackgroundDrawable(d);

       
       
       default_upload_set = findPreference(PREFKEY_DEFAULT_UPLOAD_SET);
       default_upload_set.setOnPreferenceClickListener(new OnPreferenceClickListener() {

		public boolean onPreferenceClick(Preference preference) {
			
			Intent i = new Intent();
			i.setAction(Intent.ACTION_PICK);
			i.setClass(PrefsUpload.this, PhotosetsListActivity.class);
			startActivityForResult(i, REQUEST_CODE_PHOTOSET_CHOOSER);
			
			return true;
		}
       });

       
       new UserGroupsPreferenceFetcherTask(this).execute();
   }

    // ========================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {

	   		case REQUEST_CODE_PHOTOSET_CHOOSER:
	   		{
	   			String photoset_id = data.getStringExtra(PhotosetsListActivity.INTENT_EXTRA_MY_PHOTOSET_ID);

	   			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences( getBaseContext() );
	   			settings.edit().putString(PREFKEY_DEFAULT_UPLOAD_SET, photoset_id).commit();
	   			
	   			Toast.makeText(this, "Assigned default set.", Toast.LENGTH_SHORT).show();
	   			
	   			break;
	   		}
	   		default:
		    	break;
		   }
		}
    }
}

