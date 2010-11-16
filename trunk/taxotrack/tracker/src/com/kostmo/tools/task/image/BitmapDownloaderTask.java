package com.kostmo.tools.task.image;

import org.crittr.track.Market;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;

public class BitmapDownloaderTask extends AsyncTask<URL, Void, Bitmap> {

	static final String TAG = Market.DEBUG_TAG;
	
	@Override
    protected Bitmap doInBackground(URL... urls) {

		if (urls.length > 0) {
			URL url = urls[0];
			if (url != null) {
				try {
					return BitmapFactory.decodeStream(urls[0].openStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Log.e(TAG, "Provided URL was null!");
			}
		} else {
			Log.e(TAG, "No URLs provided!");
		}
		return null;
	}
 }