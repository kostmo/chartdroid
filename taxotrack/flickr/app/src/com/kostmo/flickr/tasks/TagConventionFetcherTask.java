package com.kostmo.flickr.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.kostmo.flickr.activity.ListActivityPhotoTags;
import com.kostmo.flickr.containers.NetworkUnavailableException;
import com.kostmo.flickr.tools.TagConventionParser;

public class TagConventionFetcherTask extends AsyncTask<Void, Void, Void> {

	Context context;
	public TagConventionFetcherTask(Context context) {
		this.context = context;
	}
	
	
	protected boolean loaded_conventions = false;
	@Override
	protected Void doInBackground(Void... params) {

		if ( !ListActivityPhotoTags.test_tag_conventions_exist(context) ) {
        	try {
				new TagConventionParser(context, "tagging_convention").parse();
			} catch (NetworkUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	loaded_conventions = true;        	
		}
		
		return null;
	}
}