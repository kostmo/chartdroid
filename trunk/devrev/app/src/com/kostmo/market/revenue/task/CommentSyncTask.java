package com.kostmo.market.revenue.task;

import java.io.IOException;
import java.util.List;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.gc.android.market.api.MarketFetcher;
import com.gc.android.market.api.MarketFetcher.CommentRetrievalContainer;
import com.gc.android.market.api.model.Market.Comment;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.container.CommentsProgressPacket;
import com.kostmo.market.revenue.provider.DatabaseRevenue;

public abstract class CommentSyncTask extends AsyncTask<Long, CommentsProgressPacket, Void> implements CancellableProgressNotifier {

	protected ProgressHostActivity host;
	
	static final String TAG = Market.TAG;

	String error_message;
	protected DatabaseRevenue database;
	protected SharedPreferences settings;

	long start_milliseconds;
	
	// ========================================================================
	public CommentSyncTask(DatabaseRevenue database, SharedPreferences settings) {
		this.database = database;
		this.settings = settings;
	}

	// ====================================================================
	public void updateActivity(ProgressHostActivity activity) {
		this.host = activity;
	}
	
	// ========================================================================
	@Override
	protected void onPreExecute() {
		this.start_milliseconds = SystemClock.uptimeMillis();
	}

	// ========================================================================
	@Override
	protected Void doInBackground(Long... app_ids) {

		try {
			
			Log.i(TAG, "There are " + app_ids.length + " Google Checkout apps that need comments.");
			int app_count = 0;
			for (long app_id : app_ids) {
				
				if (isCancelled()) break;
				
				CommentsProgressPacket packet = new CommentsProgressPacket();
				packet.max_steps = app_ids.length;
				packet.current_step = app_count;
				publishProgress(packet);
				
				// FIXME
				// For the manual sync, we don't want to abort fetching early when we encounter
				// a previously stored comment, just in case there was an author who
				// changed or removed his comment.
				CommentRetrievalContainer container = MarketFetcher.getAppComments(
					this,
					this.settings.getInt(MainPreferences.PREFKEY_MAX_COMMENTS, MarketFetcher.DEFAULT_MAX_COMMENT_FETCH_LIMIT),
					this.database.getCommentAbortEarlyData(app_id),
					MarketFetcher.obtainAndroidCredentials(host.getActivity()),
					app_id);
			
				MarketFetcher.updateDatabaseCommentsStatus(this.database, app_id, container.comment_status);

				List<Comment> comments = container.comments;
				this.database.populateComments(app_id, comments);
				
				app_count++;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			this.error_message = e.getMessage();
		}

		return null;
	}

	// ========================================================================
	@Override
	protected void onCancelled() {
		cleanUp();
	}
	
	// ========================================================================
	@Override
	public void notifyProgress(int progress, int max, int layer) {

		CommentsProgressPacket packet = new CommentsProgressPacket();
		packet.max_value = max;
		packet.progress_value = progress;
		this.publishProgress(packet);
	}
	
	// ========================================================================
	@Override
	public void onPostExecute(Void voided) {

		cleanUp();

		if (this.error_message != null) {
			failTask(this.error_message);
		} else {
			completeTask();
		}
	}

	// ========================================================================
	abstract protected void completeTask();

	// ========================================================================
	abstract protected void cleanUp();

	// ========================================================================
	protected void failTask(String error_message) {
		Toast.makeText(host.getActivity(), error_message, Toast.LENGTH_LONG).show();
	}

	// ========================================================================
	protected float getElapsedSeconds() {
		long end_milliseconds = SystemClock.uptimeMillis();
		float seconds = (end_milliseconds - this.start_milliseconds)/1000f;
		return seconds;
	}
}