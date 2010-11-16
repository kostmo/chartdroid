package org.crittr.shared.browser.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.crittr.browse.AsyncTaskModified;
import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.containers.ThumbnailUrlPlusLinkContainer;
import org.crittr.keys.ApiKeys;
import org.crittr.shared.MachineTag;
import org.crittr.shared.browser.Constants.CollectionSource;
import org.crittr.shared.browser.containers.ViewHolderFlickrPhoto;
import org.crittr.task.NetworkUnavailableException;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageView;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.SearchParameters;

public class FlickrPhotoDrawableManager {

	static final String TAG = Market.DEBUG_TAG;
	
	final static int[] unknown_pics = {R.drawable.picture_unknown_1, R.drawable.picture_unknown_2, R.drawable.picture_unknown_3};

	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();
	public Map<String, SoftReference<Bitmap>> drawableMap = new Hashtable<String, SoftReference<Bitmap>>();

	public Map<String, List<String>> taxon_to_thumbnail_url_map = new Hashtable<String, List<String>>();
	
	
	
	Context context;
	public FlickrPhotoDrawableManager(Context c) {
		context = c;
	}
	
	// =============================================================

	static void copy(InputStream is, OutputStream os) {
		int datum;
		try {
			while ((datum = is.read()) != -1) os.write(datum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static Bitmap networkFetchThumbnail(String urlString) throws UnknownHostException {

//	   	Log.d(this.getClass().getSimpleName(), "image url:" + urlString);
	   	try {
	   		InputStream is = fetch(urlString);
//	   		Drawable drawable = Drawable.createFromStream(is, "src");
//	    	Log.w(TAG, "Drawable " + image_counter++ + " in networkFetchThumbnail(): " + drawable);
			
			
	   		// FIXME: THIS IS A WORKAROUND FOR A KNOWN BUG:
	   		// See http://groups.google.com/group/android-developers/browse_thread/thread/4ed17d7e48899b26/
	   		final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
	   		BufferedOutputStream out = new BufferedOutputStream(dataStream, 4096);
			copy(is, out);
			out.flush();
			final byte[] data = dataStream.toByteArray();
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

	   		is.close();

	   		return bitmap;
	   	} catch (UnknownHostException e) {
	   		Log.e(TAG, "fetchDrawable failed", e);
	   		throw e;
	   	} catch (MalformedURLException e) {
	   		Log.e(TAG, "fetchDrawable failed", e);

	   	} catch (IOException e) {
	   		Log.e(TAG, "fetchDrawable failed", e);
	   	}
	   	
	   	return null;
	}

	

	
	// =============================================================

	
	public static List<ThumbnailUrlPlusLinkContainer> extractFlickrThumbnailData(Context context, List<Photo> flickr_search_resultset) {

    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	boolean square_thumbnails = settings.getBoolean("square_thumbnails", true);

    	List<ThumbnailUrlPlusLinkContainer> photo_url_pair_builder = new ArrayList<ThumbnailUrlPlusLinkContainer>(); 
		for (Photo p : flickr_search_resultset) {
			ThumbnailUrlPlusLinkContainer photo_url_pair = new ThumbnailUrlPlusLinkContainer();
			photo_url_pair.setSource(CollectionSource.FLICKR);
			
			photo_url_pair.setLink( p.getUrl() );
			try {
				photo_url_pair.setThumbnailUrl( new URL( square_thumbnails ? p.getSmallSquareUrl() : p.getThumbnailUrl()) );
				
				photo_url_pair.setLargeUrl( new URL( p.getMediumUrl() ) );
				
				photo_url_pair.setIdentifier( p.getId() );
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			photo_url_pair_builder.add(photo_url_pair);
		}
		
		return photo_url_pair_builder;
	}
	
	// =============================================================
	
    public static List<ThumbnailUrlPlusLinkContainer> getFlickrPhotoMatches(Context context, AsyncTaskModified task, MachineTag search_machinetag, int search_quota) {

		SearchParameters search_params = new SearchParameters();
//		search_params.setExtrasMachineTags(true);
		search_params.setSafeSearch("1");
		search_params.setMachineTagMode("all");
		search_params.setSort( SearchParameters.INTERESTINGNESS_DESC );
		search_params.setMachineTags(new String[] {search_machinetag.getQueryString()});

		List<Photo> flickr_search_resultset = aggregateFlickrResultPages(task, context, search_params, search_quota);

		List<ThumbnailUrlPlusLinkContainer> photo_url_pair_builder = extractFlickrThumbnailData(context, flickr_search_resultset);

		return photo_url_pair_builder;
    }

	// =============================================================
	
    public static PhotoList aggregateFlickrResultPages( AsyncTaskModified task, Context context, SearchParameters search_params, int search_quota) {

    	Flickr flickr = null;
		String api_key = ApiKeys.FLICKR_API_KEY;
		String api_secret = ApiKeys.FLICKR_API_SECRET;
        try {
			flickr = new Flickr(
				api_key,	// My API key
				api_secret,	// My API secret
		        new REST()
		    );
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		PhotosInterface photoInt = flickr.getPhotosInterface();
    	
    	
    	
    	
    	
		PhotoList flickr_search_resultset = new PhotoList();
    	
    	int perPage = 20;
    	int current_page = 1;
    	
		while (flickr_search_resultset.size() < search_quota) {

			if (task.isCancelled())
				break;
			
			
			
			PhotoList pl = null;
			
			try {
				pl = photoInt.search(search_params, perPage, current_page);
				
//				Log.d(TAG, "There are " + pl.getTotal() + " total hits across " + pl.getPages() + " pages with " + perPage + " per page.");
				if (current_page == 1)
					flickr_search_resultset.setTotal( pl.getTotal() );
//					Toast.makeText(ListActivityTaxonExtendedInfo.this, "Found " + pl.getTotal() + " matches.", Toast.LENGTH_SHORT).show();

				flickr_search_resultset.addAll( (List<Photo>) pl );
				
				if (pl.size() < perPage) break;
				
				current_page++;
				
				
			} catch (IOException e1) {
				e1.printStackTrace();
				break;
			} catch (SAXException e1) {
				e1.printStackTrace();
				break;
			} catch (FlickrException e1) {
				e1.printStackTrace();
				break;
			}

		}
		
		return flickr_search_resultset;
    }
    
    
    
    // =============================================================

    // TODO: Is this duplicated in the recursive search for Maps?
    public class TaxonThumbnailUrlsRetrievalTask extends AsyncTaskModified<Void, Void, List<ThumbnailUrlPlusLinkContainer>> {

    	ImageView image_view_holder;
    	String taxon_name;
    	
    	// TODO: Make this a Preference
    	int thumbnail_search_quota = 1;
    	
    	TaxonThumbnailUrlsRetrievalTask(String taxon_name, final ImageView holder) {
    		this.taxon_name = taxon_name;
    		this.image_view_holder = holder;
    	}
    	
	    @Override
	    public void onPreExecute() {

	    	retrieval_tasks_semaphore.incrementAndGet();
    		((Activity) context).setProgressBarIndeterminateVisibility(true);

	    }


	    
	    
	    
	    
	    
	    
	    
	    

	    



	    
	    
	    List<ThumbnailUrlPlusLinkContainer> networkGetTaxonThumbnailUrls(String initial_taxon_name, int search_quota) {

	    	List<ThumbnailUrlPlusLinkContainer> thumbnail_results = new ArrayList<ThumbnailUrlPlusLinkContainer>();

			boolean quota_unmet = true;
	    	
	    	
			
			// NOTE: We're omitting the rank name, since we probably don't have it!
			MachineTag flickr_searchtag = new MachineTag("taxonomy", null, initial_taxon_name.toLowerCase());
			
			
			List<ThumbnailUrlPlusLinkContainer> flickr_thumbnail_results = getFlickrPhotoMatches(context, this, flickr_searchtag, search_quota);
//			Log.d(TAG, "Got " + flickr_thumbnail_results.size() + " thumbnails from Flickr.");
			
			thumbnail_results.addAll(flickr_thumbnail_results);
	    	
			
	    	if (thumbnail_results.size() >= search_quota) quota_unmet = false;

	    	if (quota_unmet) {
		    	List<ThumbnailUrlPlusLinkContainer> commons_thumbnail_results = MediawikiSearchResponseParser.getCommonsPhotoMatchesRecursive(context, this, initial_taxon_name, search_quota);
//		    	Log.d(TAG, "Got " + commons_thumbnail_results.size() + " thumbnails from Commons.");
		    	thumbnail_results.addAll(commons_thumbnail_results);
	    	}
	    	
	    	if (thumbnail_results.size() >= search_quota) quota_unmet = false;
						
						
						
			// If Commons didn't do the job, we resort to Wikipedia.
			if (quota_unmet) {
				List<ThumbnailUrlPlusLinkContainer> new_results;
				try {
					MediawikiSearchResponseParser mwsrp = new MediawikiSearchResponseParser(context);
					new_results = mwsrp.parse_category_thumbnails(initial_taxon_name, true);
				} catch (NetworkUnavailableException e) {
					new_results = new ArrayList<ThumbnailUrlPlusLinkContainer>();
				}

//				Log.d(TAG, "Found " + new_results.size() + " " + initial_taxon_name + " thumbnail results on Wikipedia.");
				
				thumbnail_results.addAll(new_results);
			}

	    	
	    	return thumbnail_results;
	    }
    	
		protected List<ThumbnailUrlPlusLinkContainer> doInBackground(Void... params) {
			
			return networkGetTaxonThumbnailUrls(taxon_name, thumbnail_search_quota);
		}
		
	    @Override
	    public void onPostExecute(List<ThumbnailUrlPlusLinkContainer> thumbnail_urls) {

	    	
	    	// We remember to populate the hash map.
	    	ArrayList<String> thumbnail_list = new ArrayList<String>(); 
	    	for (ThumbnailUrlPlusLinkContainer tuplc : thumbnail_urls)
	    		thumbnail_list.add( tuplc.getThumbnailUrl().toString() );

	    	taxon_to_thumbnail_url_map.put(taxon_name, thumbnail_list);
	    	
	    	if (thumbnail_urls.size() > 0) {
	    		
	    		String obtained_url_string = thumbnail_urls.get(0).getThumbnailUrl().toString();
	    		
				fetchDrawableByUrlOnThread(obtained_url_string, image_view_holder);
	    	}
			else {

				// Respond with the empty string.
				// To handle this, populate with one of the Unknown images downstream
	    		image_view_holder.setImageResource(R.drawable.leafs);
			}
	    	
	    	((Activity) context).setProgressBarIndeterminateVisibility(retrieval_tasks_semaphore.decrementAndGet() > 0);
	    }
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

	    	retrieval_tasks_semaphore.incrementAndGet();
    		((Activity) context).setProgressBarIndeterminateVisibility(true);
    		
		   	if (drawableMap.containsKey(image_url)) {
		   		Bitmap d = drawableMap.get(image_url).get();
		   		if (d != null) {
			   		set_view_elements(d);
			   		cancel(false);
		   		}
		   	}
	    }
    	
		protected Bitmap doInBackground(Void... params) {
			try {
				return networkFetchThumbnail(image_url);
			} catch (UnknownHostException e) {
				
				
			}
			return null;
		}
		
	    @Override
	    public void onPostExecute(Bitmap fetched_drawable) {

	    	// FIXME: This should never happen, but it does.  It's an Android bug.
	    	if (fetched_drawable != null)
	    		drawableMap.put(image_url, new SoftReference<Bitmap>(fetched_drawable));
	    	
	    	set_view_elements(fetched_drawable);

			
    		((Activity) context).setProgressBarIndeterminateVisibility(retrieval_tasks_semaphore.decrementAndGet() > 0);
	    }
	    
	    void set_view_elements(Bitmap d) {
	    	flickr_view_holder.setImageBitmap(d);
	    }
    }
	
	
    
    
	public void fetchThumbnailImageOnThread(String taxon_name, final ImageView holder, int preferred_thumbnail_index) {

		if (taxon_to_thumbnail_url_map.containsKey(taxon_name)) {

	    	List<String> thumbnail_list = taxon_to_thumbnail_url_map.get(taxon_name);

			if (thumbnail_list.size() > 0) {
				String obtained_url_string = thumbnail_list.get(preferred_thumbnail_index % thumbnail_list.size());
				

				
				fetchDrawableByUrlOnThread(obtained_url_string, holder);
			}
			else {
				holder.setImageResource(R.drawable.leafs);
				
			}

		} else {
			// Set imageView to a "pending" image
			holder.setImageResource(unknown_pics[new Random().nextInt(unknown_pics.length)]);

			new TaxonThumbnailUrlsRetrievalTask(taxon_name, holder).execute();
		}
	}
    
    
	
	public void fetchDrawableByUrlOnThread(String urlString, final ImageView holder) {

		if (drawableMap.containsKey(urlString)) {
			Bitmap bm = drawableMap.get(urlString).get();
			if (bm != null) {
				holder.setImageBitmap(bm);
				return;
			}
		}
		// Set imageView to a "pending" image
		holder.setImageResource(unknown_pics[new Random().nextInt(unknown_pics.length)]);
		new ImageThumbnailRetrievalTask(holder, urlString).execute();
	}
	
	
	public void fetchDrawableOnThread(final ThumbnailUrlPlusLinkContainer photo_thumb_pair, final ViewHolderFlickrPhoto holder) {
		
		final String urlString = photo_thumb_pair.getThumbnailUrl().toString();
		
		if (drawableMap.containsKey(urlString)) {
			
			Bitmap bm = drawableMap.get(urlString).get();
			if (bm != null) {
			
				holder.thumbnail.setImageBitmap(bm);
				return;
			}
		}
		// Set imageView to a "pending" image
		holder.thumbnail.setImageResource(unknown_pics[new Random().nextInt(unknown_pics.length)]);
		new ImageThumbnailRetrievalTask(holder.thumbnail, urlString).execute();
	}

	private static InputStream fetch(String url_string) throws UnknownHostException, MalformedURLException, IOException {
		
		/*
	   	DefaultHttpClient httpClient = new DefaultHttpClient();
	   	HttpGet request = new HttpGet(urlString);
	   	HttpResponse response = httpClient.execute(request);
	   	return response.getEntity().getContent();
	   	*/
		
		
		URL image_url = new URL(url_string);
		HttpURLConnection conn = (HttpURLConnection) image_url.openConnection();
		conn.setDoInput(true);
		conn.connect();

		BufferedInputStream buf_is = new BufferedInputStream(conn.getInputStream(), 4096);
		return buf_is;
	}
}
