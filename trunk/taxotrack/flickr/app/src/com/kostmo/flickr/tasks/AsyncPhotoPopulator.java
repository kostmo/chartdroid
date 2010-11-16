package com.kostmo.flickr.tasks;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotosInterface;
import com.kostmo.flickr.activity.FlickrAuthRetrievalActivity;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.containers.ThumbnailUrlPlusLinkContainer;
import com.kostmo.flickr.containers.ViewHolderFlickrPhoto;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.flickr.tools.FlickrFetchRoutines;
import com.kostmo.tools.SemaphoreHost;

public class AsyncPhotoPopulator implements SemaphoreHost {

	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	static final String TAG = Market.DEBUG_TAG; 
	
	public Map<String, SoftReference<Bitmap>> bitmapReferenceMap = new Hashtable<String, SoftReference<Bitmap>>();
	public Map<String, Photo> photoInfoMap = new Hashtable<String, Photo>();
	
	public ArrayList<String> missing_photos = new ArrayList<String>();

	Auth auth;
	Flickr flickr = null;
	PhotosInterface photos_interface = null;
	Context context;
	
	
	public AsyncPhotoPopulator(Context c) {
		context = c;
		
        try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		photos_interface = flickr.getPhotosInterface();
		
		auth = new Auth();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String stored_auth_token = settings.getString(FlickrAuthRetrievalActivity.PREFKEY_FLICKR_AUTH_TOKEN, null);
        auth.setToken( stored_auth_token );
		auth.setPermission(Permission.READ);
		
		RequestContext requestContext = RequestContext.getRequestContext();
		requestContext.setAuth(auth);
	}


	public Photo fetchPhotoInfo(String photo_id) {
		if (photoInfoMap.containsKey(photo_id)) {
			return photoInfoMap.get(photo_id);
		}
		
		Photo p = null;
		try {
			
			RequestContext requestContext = RequestContext.getRequestContext();
			requestContext.setAuth(auth);
			
			p = photos_interface.getPhoto( photo_id );
			photoInfoMap.put(photo_id, p);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (FlickrException e1) {
			if ( e1.getErrorCode().equals("1") ) {
				Log.e(TAG, e1.getErrorMessage());

				missing_photos.add(photo_id);
			}
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
		
		return p;
	}
	
	

	// ====================================

    public class ImageThumbnailRetrievalTask extends AsyncTaskModified<Void, Void, Bitmap> {

    	ImageView flickr_view_holder;
    	String image_url;
    	
    	ImageThumbnailRetrievalTask(ImageView holder, String image_url) {
    		this.image_url = image_url;
    		this.flickr_view_holder = holder;
    	}
    	
	    @Override
	    public void onPreExecute() {

	    	incSemaphore();
    		
		   	if (bitmapReferenceMap.containsKey(image_url)) {

		   		Bitmap d = bitmapReferenceMap.get(image_url).get();
		   		if (d != null) {
			   		set_view_elements(d);
			   		cancel(false);
		   		}
		   	}
	    }
    	
		protected Bitmap doInBackground(Void... params) {
			try {
				return FlickrFetchRoutines.networkFetchBitmap(image_url);
			} catch (UnknownHostException e) {
				
				
			}
			return null;
		}
		
	    @Override
	    public void onPostExecute(Bitmap fetched_drawable) {


	    	if (fetched_drawable != null) {
	    		bitmapReferenceMap.put(image_url, new SoftReference<Bitmap>(fetched_drawable));
	    	}
	    	
	    	set_view_elements(fetched_drawable);

			
	    	decSemaphore();
	    }
	    
	    void set_view_elements(Bitmap d) {
	    	flickr_view_holder.setImageBitmap(d);
	    }
    }
	


	// ====================================

	// ====================================
    // Fetches the buddy icon URL from a user's NSID
    public class BuddyIconRetrievalTask extends AsyncTaskModified<Void, Void, String> {

    	ImageView flickr_view_holder;
    	String user_nsid;
    	
    	BuddyIconRetrievalTask(ImageView holder, String user_nsid) {
    		this.user_nsid = user_nsid;
    		this.flickr_view_holder = holder;
    	}
    	
	    @Override
	    public void onPreExecute() {

	    	incSemaphore();
    		
	    	/*
		   	if (bitmapReferenceMap.containsKey(image_url)) {

		   		Bitmap d = bitmapReferenceMap.get(image_url).get();
		   		if (d != null) {
			   		set_view_elements(d);
			   		cancel(false);
		   		}
		   	}
		   	*/
	    }
    	
		protected String doInBackground(Void... params) {
			try {
				return flickr.getPeopleInterface().getInfo(user_nsid).getBuddyIconUrl();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FlickrException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
	    @Override
	    public void onPostExecute(String icon_url) {

	    	fetchDrawableByUrlOnThread(icon_url, flickr_view_holder);
			
	    	decSemaphore();
	    }
    }

	// ====================================
    
	public void fetchBuddyIconByNSID(String user_nsid, final ImageView holder) {
		new BuddyIconRetrievalTask(holder, user_nsid).execute();
	}

    

	public void fetchDrawableByUrlOnThread(String urlString, final ImageView holder) {

		if (urlString == null) return;
		
		if (bitmapReferenceMap.containsKey(urlString)) {
			Bitmap bm = bitmapReferenceMap.get(urlString).get();
			if (bm != null) {
				holder.setImageBitmap(bm);
				return;
			}
		}
		// Set imageView to a "pending" image
		holder.setImageResource(FlickrFetchRoutines.unknown_pics[new Random().nextInt(FlickrFetchRoutines.unknown_pics.length)]);
		new ImageThumbnailRetrievalTask(holder, urlString).execute();
	}
	
	public void fetchDrawableOnThread(final Photo photo, final ViewHolderFlickrPhoto holder) {
		
		final String urlString = photo.getSmallSquareUrl().toString();
		
		if (bitmapReferenceMap.containsKey(urlString)) {
			
			Bitmap bm = bitmapReferenceMap.get(urlString).get();
			if (bm != null) {
				holder.thumbnail.setImageBitmap(bm);
				return;
			}
		}
		
		// Set imageView to a "pending" image
		holder.thumbnail.setImageResource(FlickrFetchRoutines.unknown_pics[new Random().nextInt(FlickrFetchRoutines.unknown_pics.length)]);
		new ImageThumbnailRetrievalTask(holder.thumbnail, urlString).execute();

	}
	
	public void fetchDrawableOnThread(final ThumbnailUrlPlusLinkContainer photo_thumb_pair, final ViewHolderFlickrPhoto holder) {
		
		final String urlString = photo_thumb_pair.getThumbnailUrl().toString();
		
		if (bitmapReferenceMap.containsKey(urlString)) {
			
			
			Bitmap bm = bitmapReferenceMap.get(urlString).get();
			if (bm != null) {
				holder.thumbnail.setImageBitmap(bm);
				return;
			}
		}
		// Set imageView to a "pending" image
		holder.thumbnail.setImageResource(FlickrFetchRoutines.unknown_pics[new Random().nextInt(FlickrFetchRoutines.unknown_pics.length)]);
		new ImageThumbnailRetrievalTask(holder.thumbnail, urlString).execute();
	}

	
	public void fetchPhotoInfoOnThread(final String photo_id, final ViewHolderFlickrPhoto view_holder) {
		
		if (view_holder.title == null)
			return;
		
		if (photoInfoMap.containsKey(photo_id)) {
			
			
			// Here we need to protect against null pointers in case the user
			// has quickly switched between grid view and list view:
			
			
			view_holder.title.setText(photoInfoMap.get(photo_id).getTitle());
			
			if (view_holder.description != null) {
				String html = photoInfoMap.get(photo_id).getDescription();
				if (html != null) {
					view_holder.description.setText(Html.fromHtml(html), TextView.BufferType.SPANNABLE);
	//				view_holder.description.setMovementMethod(LinkMovementMethod.getInstance());
				} else view_holder.description.setText(html);
			}

			return;
		} else {
			view_holder.title.setText( "loading..." );
			view_holder.description.setText( "loading..." );
		}
		
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				
				Photo p = (Photo) message.obj;
				if (p != null) {
					view_holder.title.setText(p.getTitle());
					
					
					String html = p.getDescription();
					if (html != null) {
						view_holder.description.setText(Html.fromHtml(html), TextView.BufferType.SPANNABLE);
//						view_holder.description.setMovementMethod(LinkMovementMethod.getInstance());
					} else view_holder.description.setText(html);
					
				}
				
				decSemaphore();
			}
		};
		
		incSemaphore();
		
		Thread thread = new Thread() {
			@Override
			public void run() {

				Photo photo_info = fetchPhotoInfo(photo_id);
				Message message = handler.obtainMessage(1, photo_info);
				handler.sendMessage(message);
				
				
			}
		};
		thread.start();
	}

	// ======================================
    public void incSemaphore() {

    	retrieval_tasks_semaphore.incrementAndGet();
		((Activity) context).setProgressBarIndeterminateVisibility(true);
    }
    
    public void decSemaphore() {

    	((Activity) context).setProgressBarIndeterminateVisibility(retrieval_tasks_semaphore.decrementAndGet() > 0);
    }
}
