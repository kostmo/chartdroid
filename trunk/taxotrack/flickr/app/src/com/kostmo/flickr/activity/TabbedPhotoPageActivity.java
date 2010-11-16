package com.kostmo.flickr.activity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotosInterface;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.containers.ProgressHostActivity;
import com.kostmo.flickr.containers.ThumbnailUrlPlusLinkContainer;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tasks.LargeImageGetterTask;
import com.kostmo.flickr.tasks.PhotoGetterTask;
import com.kostmo.flickr.tools.FlickrFetchRoutines;
import com.kostmo.tools.SemaphoreHost;

public class TabbedPhotoPageActivity extends TabActivity implements SemaphoreHost, ProgressHostActivity {

	static final String TAG = "TabbedPhotoPageActivity"; 

	Bitmap globally_stored_dialog_bitmap;

	final int DIALOG_LARGE_PHOTO_VIEW = 5;
	final int DIALOG_PHOTO_TITLE_DESCRIPTION = 6;
	final int DIALOG_PROGRESS = 7;

	TextView flickr_main_blurb;
	TextView flickr_main_title;
	
//	TextView embedded_description_textview;
	
	ImageView flickr_main_photo;
	Bitmap globally_stored_thumbnail_bitmap;

	public ProgressDialog prog_dialog;
	
	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	Photo globally_stored_photo;
	
	LargeImageGetterTaskExtended large_image_getter_task;
	
	// ========================================================================
	Bitmap getBigPhotoBitmap() {
		return globally_stored_dialog_bitmap;
	}

	// ========================================================================
	@Override
	protected void onCreate(Bundle savedInstanceState) {

        getWindow().setFlags(
        		WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN );

		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.tab_activity_photo_page);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);


		final TabHost tabHost = getTabHost();
		long photo_id = getIntent().getLongExtra(IntentConstants.PHOTO_ID, ListActivityPhotoTags.INVALID_PHOTO_ID);


		// TODO
		/*
		this.embedded_description_textview = new TextView(this);
		tabHost.addTab(tabHost.newTabSpec("tab0")
				.setIndicator("Description")
				.setContent( new TabHost.TabContentFactory() {
					@Override
					public View createTabContent(String tag) {
						embedded_description_textview.setText("pending...");
						return null;
					}
				}));
		*/
		
		Intent tags_intent = new Intent();
		tags_intent.setAction(IntentConstants.ACTION_TAG);
		tags_intent.putExtra(IntentConstants.PHOTO_ID, photo_id);
		tags_intent.addCategory(IntentConstants.CATEGORY_FLICKR_PHOTO);
		tabHost.addTab(tabHost.newTabSpec("tab1")
				.setIndicator("Tags")
				.setContent(tags_intent));

		Intent comments_intent = new Intent(this, ListActivityPhotoComments.class);
		comments_intent.putExtra(IntentConstants.PHOTO_ID, photo_id);
		tabHost.addTab(tabHost.newTabSpec("tab2")
				.setIndicator("Comments")
				.setContent(comments_intent));

		flickr_main_blurb = (TextView) findViewById(R.id.flickr_main_blurb);
		flickr_main_title = (TextView) findViewById(R.id.flickr_main_title);
		flickr_main_photo = (ImageView) findViewById(R.id.flickr_main_photo);

		flickr_main_photo.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new LargeImageGetterFromScratch(TabbedPhotoPageActivity.this).execute( getIntent().getLongExtra(IntentConstants.PHOTO_ID, ListActivityPhotoTags.INVALID_PHOTO_ID ) );
			}
		});
		
    	new PhotoInfoFetcherTask(this).execute(photo_id);


		// Deal with orientation change
		final PreserveConfigurationWrapper a = (PreserveConfigurationWrapper) getLastNonConfigurationInstance();
		if (a != null) {
			this.globally_stored_dialog_bitmap = a.dialog_bitmap;
			this.large_image_getter_task = a.large_image_getter_task;
			if (this.large_image_getter_task != null)
				this.large_image_getter_task.updateActivity(this);
		}
	}

	// ========================================================================
	class PreserveConfigurationWrapper {
		Bitmap dialog_bitmap;
		LargeImageGetterTaskExtended large_image_getter_task;
	}

	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {
		PreserveConfigurationWrapper pcw = new PreserveConfigurationWrapper();
		pcw.dialog_bitmap = this.globally_stored_dialog_bitmap;
		pcw.large_image_getter_task = this.large_image_getter_task;
		return pcw;
	}

	// ========================================================================
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		Log.d(TAG, "Executing onPrepareDialog()");

		switch (id) {
        case DIALOG_PROGRESS:
        {
        	Log.d(TAG, "Preparing progress dialog in onPrepareDialog(" + id + ")");
        	
	        break;
        }
		case DIALOG_PHOTO_TITLE_DESCRIPTION:
		{
			final EditText title_textbox = (EditText) dialog.findViewById(R.id.textbox_photo_title);
			final EditText description_textbox = (EditText) dialog.findViewById(R.id.textbox_photo_description);

			title_textbox.setText( flickr_main_title.getText() );
			description_textbox.setText( flickr_main_blurb.getText() );

			break;
		}
		case DIALOG_LARGE_PHOTO_VIEW:
		{
			Log.d(TAG, "Since this doesn't get called on orientation change, we do nothing.");
		}
		default:
			break;
		}
	}

	// ========================================================================
	void updatePhotoTitleDescription(long photo_id, String title, String description) {

		Flickr flickr = null;
		try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
					new REST()
			);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		RequestContext requestContext = RequestContext.getRequestContext();
		Auth auth = new Auth();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
		auth.setToken( stored_auth_token );
		auth.setPermission(Permission.WRITE);
		requestContext.setAuth(auth);

		PhotosInterface photos_interface = flickr.getPhotosInterface();

		try {
			photos_interface.setMeta(Long.toString(photo_id), title, description);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (FlickrException e) {
			e.printStackTrace();
		}

		updatePageTitleDescription(title, description);
	}

	// ========================================================================
	void updatePageTitleDescription(String title, String description) {
		if (description == null || description.length() == 0)
			flickr_main_blurb.setVisibility(View.GONE);
		else {

			flickr_main_blurb.setText(Html.fromHtml(description), TextView.BufferType.SPANNABLE);
			flickr_main_blurb.setMovementMethod(LinkMovementMethod.getInstance());

			flickr_main_blurb.setVisibility(View.VISIBLE);
		}

		if (title == null || title.length() == 0)
			flickr_main_title.setVisibility(View.GONE);
		else {
			flickr_main_title.setText(title);
			flickr_main_title.setVisibility(View.VISIBLE);
		}
	}
	
	// ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

		LayoutInflater factory = LayoutInflater.from(this);
		Log.d(TAG, "Executing onCreateDialog()");

		switch (id) {
        case DIALOG_PROGRESS:
        {
        	Log.d(TAG, "Creating progress dialog in onCreateDialog(" + id + ")");

        	prog_dialog = new ProgressDialog(this);
        	prog_dialog.setMessage("Fetching image...");
    		prog_dialog.setIndeterminate(true);
    		prog_dialog.setCancelable(false);

        	return prog_dialog;
        }
		case DIALOG_PHOTO_TITLE_DESCRIPTION:
		{

			View dialog_photo_description = factory.inflate(R.layout.dialog_photo_description, null);


			final EditText title_textbox = (EditText) dialog_photo_description.findViewById(R.id.textbox_photo_title);
			final EditText description_textbox = (EditText) dialog_photo_description.findViewById(R.id.textbox_photo_description);


			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Title / Description")
			.setView(dialog_photo_description)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					updatePhotoTitleDescription(
							getIntent().getLongExtra(IntentConstants.PHOTO_ID, ListActivityPhotoTags.INVALID_PHOTO_ID),
							title_textbox.getText().toString(),
							description_textbox.getText().toString()
					);
				}
			})
			.create();
		}
		case DIALOG_LARGE_PHOTO_VIEW:
		{
			Dialog d = new Dialog(this);
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);


			ImageView i = new ImageView(this);
			i.setImageBitmap( getBigPhotoBitmap() );
			i.setAdjustViewBounds(true);
//			i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);


			d.setContentView(i);

			return d;
		}
		}  
		return null;
	}

	// ========================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_photo, menu);
		return true;
	}

	// ========================================================================
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.menu_zoom_view).setVisible(
			globally_stored_photo != null && globally_stored_thumbnail_bitmap != null
		);
			
		return true;
	}

	// ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_edit_title_description:
		{
			showDialog(DIALOG_PHOTO_TITLE_DESCRIPTION);
			break;
		}
		case R.id.menu_zoom_view:
		{
			SearchResultsViewerActivity.launchCustomZoom(
					this,
					globally_stored_photo,
					globally_stored_thumbnail_bitmap);
			break;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	// ========================================================================
	enum PhotoInfoStage {
		DESCRIPTION, TAGS, THUMBNAIL
	};
	
	// ========================================================================
	class PhotoInfoFetcherTask extends PhotoGetterTask  {

		protected PhotoInfoFetcherTask(Context context) {
			super(context);
		}

	    @Override
	    protected void onPostExecute(Photo photo) {
	    	
	    	globally_stored_photo = photo;
	    	
	    	new PhotoInfoFetcherSubTask().execute(photo);
	    	super.onPostExecute(photo);
	    }
		
		class PhotoInfoFetcherSubTask extends AsyncTask<Photo, PhotoInfoStage, Void>  {
			
			Bitmap bitmap = null;
			String description = null;
			String photo_title = null;
	
	
			@Override
			public void onPreExecute() {
				incSemaphore();
			}
	
			@Override
			protected Void doInBackground(Photo... photos) {
	
				Photo photo = photos[0];
	

		    	Flickr flickr = null;
		        try {
					flickr = new Flickr(
							ApiKeys.FLICKR_API_KEY,	// My API key
							ApiKeys.FLICKR_API_SECRET,	// My API secret
				        new REST()
				    );
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}
				
				
				RequestContext requestContext = RequestContext.getRequestContext();
		        Auth auth = new Auth();

				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(TabbedPhotoPageActivity.this);
				String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
		        auth.setToken( stored_auth_token );
		        auth.setPermission(Permission.READ);
		        requestContext.setAuth(auth);

				description = photo.getDescription();
				photo_title = photo.getTitle();
				publishProgress(PhotoInfoStage.DESCRIPTION);


    			String thumbnail_url = photo.getThumbnailUrl();

				try {
					bitmap = FlickrFetchRoutines.networkFetchBitmap(thumbnail_url);
				} catch (IOException e) {
					Log.e(this.getClass().getSimpleName(), "fetchDrawable failed", e);
				}

				publishProgress(PhotoInfoStage.THUMBNAIL);

				return null;
			}
	
	
			@Override
			protected void onProgressUpdate(PhotoInfoStage... progress) {
	
				switch (progress[0]) {
				case DESCRIPTION:

					updatePageTitleDescription(photo_title, description);

	
					break;
	
				case THUMBNAIL:
					flickr_main_photo.setImageBitmap(bitmap);
					globally_stored_thumbnail_bitmap = bitmap;
					break;
				}
			}
	
			@Override
			public void onPostExecute(Void nothing) {
				decSemaphore();
			}
		}
	}

	// ========================================================================
	public class LargeImageGetterFromScratch extends PhotoGetterTask {

		protected LargeImageGetterFromScratch(Context context) {
			super(context);
		}

		@Override
		protected void onPostExecute(Photo photo) {
			ThumbnailUrlPlusLinkContainer photo_thumbnail_pair = new ThumbnailUrlPlusLinkContainer();
			try {
				photo_thumbnail_pair.setLargeUrl( new URL(photo.getMediumUrl()) );
				large_image_getter_task = new LargeImageGetterTaskExtended();
				large_image_getter_task.execute(photo);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
			super.onPostExecute(photo);
		}

	}

	// ========================================================================
	public class LargeImageGetterTaskExtended extends LargeImageGetterTask {

		LargeImageGetterTaskExtended() {
			super(TabbedPhotoPageActivity.this, true, DownloadObjective.SHOW_IN_DIALOG);
		}

		@Override
		public void onPreExecute() {
			super.onPreExecute();
			incSemaphore();
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);

			globally_stored_dialog_bitmap = bitmap;
			showDialog(DIALOG_LARGE_PHOTO_VIEW);
			
			decSemaphore();
		}
	}
	
	// ========================================================================
	@Override
    public void incSemaphore() {
    	retrieval_tasks_semaphore.incrementAndGet();
		setProgressBarIndeterminateVisibility(true);
    }

	// ========================================================================
	@Override
    public void decSemaphore() {
    	setProgressBarIndeterminateVisibility(retrieval_tasks_semaphore.decrementAndGet() > 0);
    }
	
    // ========================================================================
	@Override
	public void showProgressDialog() {
		showDialog(DIALOG_PROGRESS);
	}

	@Override
	public ProgressDialog getProgressDialog() {
		return prog_dialog;
	}

	@Override
	public void dismissProgressDialog() {
		dismissDialog(DIALOG_PROGRESS);
	}
}
