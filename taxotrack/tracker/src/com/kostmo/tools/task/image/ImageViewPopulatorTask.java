package com.kostmo.tools.task.image;

import com.kostmo.tools.SemaphoreHost;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class ImageViewPopulatorTask extends BitmapDownloaderTask {

	protected SemaphoreHost context;
	protected ImageView image_view;
	public ImageViewPopulatorTask(SemaphoreHost context, ImageView image_view) {
		this.context = context;
		this.image_view = image_view;
	}

    @Override
    public void onPreExecute() {
    	context.incSemaphore();
    }

     @Override
     protected void onPostExecute(Bitmap bitmap) {
    	 image_view.setImageBitmap(bitmap);
    	 context.decSemaphore();
     }
 }