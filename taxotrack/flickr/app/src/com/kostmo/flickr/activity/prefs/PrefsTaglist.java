package com.kostmo.flickr.activity.prefs;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;

import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.graphics.NonScalingBackgroundDrawable;
public class PrefsTaglist extends PreferenceActivity {

	   @Override
	   public void onCreate(Bundle savedInstanceState) {
		   getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		   super.onCreate(savedInstanceState);
		   getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);
	       addPreferencesFromResource( R.xml.prefs_taglist );

	       
	       getListView().setCacheColorHint(0);	// Required for custom background images
	       Drawable d = new NonScalingBackgroundDrawable(this, getListView(), -1);
	       d.setAlpha(0x20);	// mostly transparent
//	       d.setAlpha(0xFF);	// not transparent
//	       d.setColorFilter(new PorterDuffColorFilter(Color.rgb(0xFF, 0xA0, 0xFF), PorterDuff.Mode.SRC_ATOP));
	       getListView().setBackgroundDrawable(d);
	       


	   }
	
}
