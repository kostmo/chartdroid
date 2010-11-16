package com.kostmo.tools.task.image;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;

public class BitmapDialogDownloaderTask extends BitmapDownloaderTask {


	public ProgressDialog wait_dialog;
	
	protected Context context;
	public BitmapDialogDownloaderTask(Context context) {
		this.context = context;
	}
	
	
	void instantiate_latent_wait_dialog() {

		if (wait_dialog == null)
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
     protected void onPostExecute(Bitmap bitmap) {
		 wait_dialog.dismiss();
     }
 }