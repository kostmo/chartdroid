package com.kostmo.flickr.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.comments.Comment;
import com.kostmo.flickr.adapter.CommentsAdapter;
import com.kostmo.flickr.bettr.IntentConstants;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.keys.ApiKeys;
import com.kostmo.tools.SemaphoreHost;

public class ListActivityPhotoComments extends ListActivity implements SemaphoreHost {


	static final String TAG = Market.DEBUG_TAG;
	

	private AtomicInteger retrieval_tasks_semaphore = new AtomicInteger();

	// ========================================================================
    @Override
    protected void onSaveInstanceState(Bundle out_bundle) {
    	super.onSaveInstanceState(out_bundle);
    	Log.i(TAG, "onSaveInstanceState");
    }

	// ========================================================================
    @Override
    protected void onRestoreInstanceState(Bundle in_bundle) {
    	super.onRestoreInstanceState(in_bundle);
    	Log.i(TAG, "onRestoreInstanceState");
    }

	// ========================================================================
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.list_activity_comments);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.titlebar_icon);

        Flickr flickr = null;
		try {
			flickr = new Flickr(
					ApiKeys.FLICKR_API_KEY,	// My API key
					ApiKeys.FLICKR_API_SECRET,	// My API secret
			        new REST()
			    );
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		CommentsAdapter adapter = new CommentsAdapter(this, flickr);
        setListAdapter(adapter);
        
        // Deal with orientation change
	    final StateRetainer a = (StateRetainer) getLastNonConfigurationInstance();
        if (a != null) {
        	adapter.comment_list = a.comments_list;
        	adapter.notifyDataSetChanged();
        	
        } else {
        	long photo_id = getIntent().getLongExtra(IntentConstants.PHOTO_ID, ListActivityPhotoTags.INVALID_PHOTO_ID);
            
            new CommentsFetcherTask(flickr).execute(photo_id);
        }
    }

	// ========================================================================
    private class StateRetainer {
    	List<Comment> comments_list;
    }

	// ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
    	StateRetainer pcw = new StateRetainer();
    	
    	CommentsAdapter adapter = ((CommentsAdapter) getListAdapter());
    	pcw.comments_list = adapter.comment_list;
        return pcw;
    }
    

	// ========================================================================
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	/*
    	Intent i = new Intent();
    	i.putExtra(INTENT_EXTRA_RECENT_SEARCH_ID, id);
    	setResult(Activity.RESULT_OK, i);
    	
    	finish();
    	*/
    }

	// ========================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.options_photo, menu);
        return true;
    }

	// ========================================================================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

	// ========================================================================
    class CommentsFetcherTask extends AsyncTask<Long, Void, List<Comment>> {

		Flickr flickr;
		CommentsFetcherTask(Flickr f) {
			flickr = f;
		}
		
    	@Override
        public void onPreExecute() {
    		incSemaphore();
    	}
    	
		@Override
		protected List<Comment> doInBackground(Long... photo_ids) {
			
			String photo_id = Long.toString( photo_ids[0] );

    		try {
    			return flickr.getCommentsInterface().getList(photo_id);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (FlickrException e) {
				e.printStackTrace();
			}
			
			return new ArrayList<Comment>();
		}
    	


	    @Override
	    public void onPostExecute(List<Comment> comments) {
	    	
	    	decSemaphore();

	    	if (comments.size() == 0) {
		    	TextView empty_textview = (TextView) findViewById(R.id.empty_list_text);
		    	empty_textview.setText(R.string.empty_no_items);
		    	
		    	findViewById(R.id.empty_list_progress).setVisibility(View.GONE);
	    	}

			
			
			Map<String, Integer> users = new HashMap<String, Integer>();
			for (Comment comment : comments) {
				
				int count = 1;
				if (users.containsKey(comment.getAuthor())) {
					count = users.get(comment.getAuthor());
					count++;
				}
				users.put(comment.getAuthor(), count);
			}
			


	    	CommentsAdapter adapter = ((CommentsAdapter) getListAdapter());
	    	adapter.comment_list = comments;
	   		adapter.notifyDataSetInvalidated();
	   		
	   		
			TextView activity_title_header = (TextView) findViewById(R.id.activity_title_header);
			activity_title_header.setText("Showing " + adapter.getCount() + " comments by " + users.size() + " author(s)");
	    }
    }

	// ========================================================================
    public void initiate_tag_retrieval(final long photo_id, boolean refetch_tags) {

    	// Even though we knew it before, we have to re-fetch the description
    	// and thumbnail, because now we're in a different activity.
    	
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

    	if (refetch_tags)
    		new CommentsFetcherTask(flickr).execute(photo_id);
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
		boolean spinner_visible = retrieval_tasks_semaphore.decrementAndGet() > 0;
    	setProgressBarIndeterminateVisibility(spinner_visible);
    }
}
