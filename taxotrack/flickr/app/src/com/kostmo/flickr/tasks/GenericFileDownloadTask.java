package com.kostmo.flickr.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.tools.FlickrFetchRoutines;

public class GenericFileDownloadTask extends AsyncTask<Void, Float, Boolean> {

	static final String TAG = Market.DEBUG_TAG;

	MediaScannerConnection mMediaScannerConnection;
	
	Context context;
	String url;
	String file_path;
	String error_message;
	public GenericFileDownloadTask(Context context, String url, String file_path) {
		this.context = context;
		this.url = url;
		this.file_path = file_path;
	}
	
	protected ProgressDialog wait_dialog;
	
	void instantiate_latent_wait_dialog() {

		wait_dialog = new ProgressDialog(this.context);
		wait_dialog.setMessage("Downloading...");
		wait_dialog.setIndeterminate(true);
		wait_dialog.setCancelable(true);
		wait_dialog.show();
		
		wait_dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				GenericFileDownloadTask.this.cancel(true);
			}
		});
	}

	
	
    @Override
    public void onPreExecute() {
		instantiate_latent_wait_dialog();
    }
	
	@Override
    protected Boolean doInBackground(Void... voided) {

		try {
			InputStream is = FlickrFetchRoutines.fetch(this.url);
			OutputStream os = new FileOutputStream(this.file_path);
			
			int datum;
			while (
					(datum = is.read()) != -1
					&& !this.isCancelled())
				os.write(datum);
			
			// TODO: Make this step optional.
//            publishProgress(-1f);
    		scanMedia();
    		
			return true;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			error_message = e.getLocalizedMessage();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			error_message = e.getLocalizedMessage();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			error_message = e.getLocalizedMessage();
		} catch (IOException e) {
			e.printStackTrace();
			error_message = e.getLocalizedMessage();
		}
		return false;
	}

	
    protected void onProgressUpdate(Float... percentage) {
    	wait_dialog.setMessage("Adding to gallery...");
    }

	
	
    @Override
    protected void onPostExecute(Boolean success) {
    	wait_dialog.dismiss();
    	
    	if (!success) {
            Toast.makeText(context, error_message, Toast.LENGTH_SHORT).show();
    	} else {
    	}
    }
    
    

    @Override
    protected void onCancelled() {

        Toast.makeText(context, "Cancelled download!", Toast.LENGTH_SHORT).show();

        // Try to delete partial file...
        boolean success = new File(this.file_path).delete();
        Log.d(TAG, "Partial file deleted? " + success);
    }

    
    
    
    
    
    
    void scanMedia() {
   		MediaScannerConnection.MediaScannerConnectionClient mMediaScanConnClient =
	        new MediaScannerConnection.MediaScannerConnectionClient() {
	            /**
	             * Called when a connection to the MediaScanner service has been established.
	             */
	    		@Override
	            public void onMediaScannerConnected() {
	                Log.d(TAG, "I just connected, so says the MediaScannerConnectionClient callback...");

	                
	                mMediaScannerConnection.scanFile(file_path,
                            null /* mimeType */);
	            }

				@Override
				public void onScanCompleted(String path, Uri uri) {

					mMediaScannerConnection.disconnect();
				}
		};
		
		mMediaScannerConnection = new MediaScannerConnection(context, mMediaScanConnClient);
		mMediaScannerConnection.connect();
    }
 }