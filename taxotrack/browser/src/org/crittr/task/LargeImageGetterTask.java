package org.crittr.task;

import java.net.URL;
import java.net.UnknownHostException;

import org.crittr.containers.ThumbnailUrlPlusLinkContainer;
import org.crittr.shared.browser.utilities.FlickrPhotoDrawableManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

public class LargeImageGetterTask extends AsyncTask<ThumbnailUrlPlusLinkContainer, Void, Bitmap> {
	 

	ProgressDialog wait_dialog;
	
	Context context;
	protected LargeImageGetterTask(Context context) {
		this.context = context;
	}
	
	
	void instantiate_latent_wait_dialog() {

		wait_dialog = new ProgressDialog(context);
		wait_dialog.setMessage("Fetching image...");
		wait_dialog.setIndeterminate(true);
		wait_dialog.setCancelable(false);
		wait_dialog.show();
	}

    @Override
    public void onPreExecute() {
		instantiate_latent_wait_dialog();
    }
	
	@Override
    protected Bitmap doInBackground(ThumbnailUrlPlusLinkContainer... photo_thumbnail_pairs) {
    	ThumbnailUrlPlusLinkContainer tuplc = photo_thumbnail_pairs[0];

		Bitmap bitmap = null;
		
		URL url = tuplc.getLargeUrl();
		
		if (url != null) {
			String large_url = url.toString();
	
			try {
				bitmap = FlickrPhotoDrawableManager.networkFetchThumbnail(large_url);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}


     @Override
     protected void onPostExecute(Bitmap bitmap) {


		 wait_dialog.dismiss();

     }
 }