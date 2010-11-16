package com.kostmo.flickr.tasks;

import java.net.UnknownHostException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.tools.FlickrFetchRoutines;


public class GenericBitmapDownloadTask extends AsyncTask<String, Void, Bitmap> {

	static final String TAG = Market.DEBUG_TAG;
	
	protected Context context;
	protected GenericBitmapDownloadTask(Context context) {
		this.context = context;
	}
	
	protected ProgressDialog wait_dialog;
	
	void instantiate_latent_wait_dialog() {

		wait_dialog = new ProgressDialog(context);
		wait_dialog.setMessage("Downloading...");
		wait_dialog.setIndeterminate(true);
		wait_dialog.setCancelable(true);
		wait_dialog.show();
		
		wait_dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				GenericBitmapDownloadTask.this.cancel(true);
			}
		});
	}

    @Override
    public void onPreExecute() {
		instantiate_latent_wait_dialog();
    }
	
	@Override
    protected Bitmap doInBackground(String... photos) {
		String url = photos[0];

		try {
			return FlickrFetchRoutines.networkFetchBitmap(url);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

    @Override
    protected void onPostExecute(Bitmap bitmap) {
    	wait_dialog.dismiss();
    }
 }