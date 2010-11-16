package com.gc.android.market.api;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gc.android.market.api.MarketSession.Callback;
import com.gc.android.market.api.model.Market.App;
import com.gc.android.market.api.model.Market.AppsRequest;
import com.gc.android.market.api.model.Market.AppsResponse;
import com.gc.android.market.api.model.Market.Comment;
import com.gc.android.market.api.model.Market.CommentsRequest;
import com.gc.android.market.api.model.Market.CommentsResponse;
import com.gc.android.market.api.model.Market.GetImageRequest;
import com.gc.android.market.api.model.Market.GetImageResponse;
import com.gc.android.market.api.model.Market.ResponseContext;
import com.gc.android.market.api.model.Market.AppsRequest.ViewType;
import com.gc.android.market.api.model.Market.GetImageRequest.AppImageUsage;
import com.kostmo.market.revenue.Market;
import com.kostmo.market.revenue.activity.prefs.MainPreferences;
import com.kostmo.market.revenue.adapter.AppExpandableListAdapter.CommentsFetchStatus;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.DatabaseRevenue.CommentAbortEarlyData;
import com.kostmo.market.revenue.task.CancellableProgressNotifier;

public class MarketFetcher {

	static final String TAG = "MarketFetcher";

	public static final int REQUEST_ENTRY_LIMIT = 10;
	public static final int MAX_APP_FETCH_LIMIT = 50;
	public static final int DEFAULT_MAX_COMMENT_FETCH_LIMIT = 100;


	static final String AUTHTOKEN_TYPE_ANDROID = "android";
	public static final String ACCOUNT_TYPE_GOOGLE = "com.google";
	

	public static final String PREFKEY_SAVED_ANDROID_AUTHTOKEN = "PREFKEY_SAVED_ANDROID_AUTHTOKEN";
	public static final String PREFKEY_PREFER_HARDCODED_ANDROID_ID = "PREFKEY_PREFER_HARDCODED_ANDROID_ID";
	
	public static final String INVALID_ANDROID_ID_ERROR_CLUE = "Unknown format";
	
	

	// ========================================================================
	public static class AndroidAuthenticationCredentials {
		
		public final String authSubToken, android_id;
		
		public AndroidAuthenticationCredentials(String authSubToken, String android_id) {
			this.authSubToken = authSubToken;
			this.android_id = android_id;
		}
	}
	
	// ========================================================================
	public static void invalidateAuthToken(Activity activity) {
		AccountManager mgr = AccountManager.get(activity);
		
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		String saved_authtoken = preferences.getString(PREFKEY_SAVED_ANDROID_AUTHTOKEN, null);
		
		Log.e(TAG, "Invalidating authoken: " + saved_authtoken);
		
		mgr.invalidateAuthToken(ACCOUNT_TYPE_GOOGLE, saved_authtoken);
	}
	
	// ========================================================================
	public static AndroidAuthenticationCredentials obtainAndroidCredentials(Activity activity) {
		String authToken = getAndroidAuthToken(activity);
		String android_id = obtainAndroidId(activity);
		return new AndroidAuthenticationCredentials(authToken, android_id);
	}

	// ========================================================================
	public static String obtainAndroidId(Context context) {

		// XXX Sometimes (perhaps if the phone undergoes a factory
		// reset, is rooted, or if new firmware is installed)
		// the ID will be invalid.
		String android_id = android.provider.Settings.Secure.getString(
				context.getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences.getBoolean(PREFKEY_PREFER_HARDCODED_ANDROID_ID, false)) {
			Log.w(TAG, "Using hardcoded ANDROID_ID...");
			android_id = preferences.getString(MainPreferences.PREFKEY_ALTERNATE_DEVICE_ID, Long.toHexString(Market.PERSONAL_ANDROID_ID)).toLowerCase();
		} else {
			Log.i(TAG, "Using native ANDROID_ID: " + android_id);
		}

		return android_id;
	}
	
	// ========================================================================
	public static String getAndroidAuthToken(Activity activity) {
		AccountManager mgr = AccountManager.get(activity);
		for (Account acct : mgr.getAccountsByType(ACCOUNT_TYPE_GOOGLE)) {

			AccountManagerFuture<Bundle> accountManagerFuture = mgr.getAuthToken(acct, AUTHTOKEN_TYPE_ANDROID, null, activity, null, null);
			Bundle authTokenBundle = null;

			try {
				authTokenBundle = accountManagerFuture.getResult();
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			String authtoken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
			// Save the token in case we need to access it from outside an activity
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
			preferences.edit().putString(PREFKEY_SAVED_ANDROID_AUTHTOKEN, authtoken).commit();

			
			return authtoken;
		}
		return null;
	}

	// ========================================================================
	static class EntryCountWrapper {
		int entry_count;
	}

	// ========================================================================
	static class TerminationWrapper {
		boolean terminate = false;
		CommentsFetchStatus comment_status = null;
	}
	
	// ========================================================================
	public class CommentAlreadyStoredException extends Exception {}

	// ========================================================================
	public class CommentsNotFoundException extends Exception {}

	// ========================================================================
	public static void updateDatabaseCommentsStatus(DatabaseRevenue database, long app_id, CommentsFetchStatus status) {
		
		if (status != null) {
			switch (status) {
			case GOT_ALL:
				database.setTrueAppBoolean(DatabaseRevenue.KEY_GOT_EARLIEST_COMMENT, app_id);
				break;
			case GOT_MAX:
				break;
			default:
			case INCOMPLETE:
				database.setTrueAppBoolean(DatabaseRevenue.KEY_GOT_MAX_COMMENTS, app_id);
				break;
			}
		}
	}
	
	
	public static class CommentRetrievalContainer {
		public CommentsFetchStatus comment_status = null;
		public List<Comment> comments;
	}
	
	// ========================================================================
	/**
	 * We download the comments in batches of 10. We can save lots of time
	 * by aborting if we detect that any of the comments in our current batch
	 * is already stored in the database.  We go ahead and store the rest of
	 * the batch, then terminate.
	 */
	public static CommentRetrievalContainer getAppComments(
			final CancellableProgressNotifier cancellable_notifier,
			final int max_comments,
			final CommentAbortEarlyData abort_early_comment_data,
			AndroidAuthenticationCredentials android_credentials,
			final long app_id) throws IOException {

		MarketSession session = new MarketSession(android_credentials.android_id);
		session.setAuthSubToken(android_credentials.authSubToken);


		final List<Comment> full_comments_list = new ArrayList<Comment>();


		CommentsRequest.Builder comment_request_builder = CommentsRequest.newBuilder()
			.setAppId(Long.toString(app_id));

		final EntryCountWrapper entry_count_wrapper = new EntryCountWrapper();
		final TerminationWrapper termination_wrapper = new TerminationWrapper();
		do {

			CommentsRequest.Builder cloned_builder = (CommentsRequest.Builder) comment_request_builder.clone();
			CommentsRequest commentsRequest = cloned_builder
			.setStartIndex(full_comments_list.size())
			.setEntriesCount(REQUEST_ENTRY_LIMIT)	// This field is mandatory!
			.build();

			MarketSession.Callback<CommentsResponse> callback = new MarketSession.Callback<CommentsResponse>() {


				
				@Override
				public void onResult(ResponseContext context, CommentsResponse response) {

					entry_count_wrapper.entry_count = response.getEntriesCount();

					List<Comment> partial_list = response.getCommentsList();
					full_comments_list.addAll(partial_list);
					
					
					if (cancellable_notifier != null)
						cancellable_notifier.notifyProgress(full_comments_list.size(), entry_count_wrapper.entry_count, 0);
					
					
					// Check whether we have any of these comments already. If so,
					// than we can terminate the fetching process.
					
					if (abort_early_comment_data != null) {
						
						if (full_comments_list.size() >= entry_count_wrapper.entry_count) {
							termination_wrapper.terminate = true;
							termination_wrapper.comment_status = CommentsFetchStatus.GOT_ALL;
							
							
						} else if (full_comments_list.size() >= max_comments) {
							termination_wrapper.terminate = true;
							termination_wrapper.comment_status = CommentsFetchStatus.GOT_MAX;
							
						} else if (abort_early_comment_data.shouldAbortEarly() || abort_early_comment_data.checkAlreadyHasCommentInBatch(partial_list)) {
//							Log.e(TAG, "We already have a comment with the same App and Author ID!");
							termination_wrapper.terminate = true;
						}
					}
					
					if ((cancellable_notifier != null && cancellable_notifier.isCancelled()) || response.getCommentsCount() == 0) {
						termination_wrapper.terminate = true;
					}
				}
			};
			session.append(commentsRequest, callback);
			session.flush();	// Blocks here until the callback finishes executing

		} while (!termination_wrapper.terminate);
		

		CommentRetrievalContainer container = new CommentRetrievalContainer();
		container.comment_status = termination_wrapper.comment_status;
		container.comments = full_comments_list;
		return container;
	}

	// ========================================================================
	static class BitmapContainer {
		Bitmap bitmap;
	}

	// ========================================================================
	public static Bitmap getAppIcon(AndroidAuthenticationCredentials android_credentials, long app_id) throws IOException {

		// This is only capable of fetching "published" apps.

		MarketSession session = new MarketSession(android_credentials.android_id);
		session.setAuthSubToken(android_credentials.authSubToken);

		final BitmapContainer container = new BitmapContainer();

		GetImageRequest imgReq = GetImageRequest.newBuilder().setAppId( Long.toString(app_id))
		.setImageUsage(AppImageUsage.ICON)
//		.setImageId("1")
		.build();

		session.append(imgReq, new Callback<GetImageResponse>() {

			@Override
			public void onResult(ResponseContext context, GetImageResponse response) {
				try {
					byte[] data = response.getImageData().toByteArray();
//					FileOutputStream fos = new FileOutputStream("icon.png");
//					fos.write(response.getImageData().toByteArray());
//					fos.close();

					container.bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		session.flush();
		return container.bitmap;
	}

	// ========================================================================
	public static List<App> getPublisherApps(AndroidAuthenticationCredentials android_credentials, String publisher, ViewType view_type) throws UnknownHostException, IOException {

		// Note: This is only capable of fetching "published" apps.

		MarketSession session = new MarketSession(android_credentials.android_id);
		session.setAuthSubToken(android_credentials.authSubToken);

		String query = "pub:\"" + publisher + "\"";
		AppsRequest.Builder apps_request_builder = AppsRequest.newBuilder()
		.setQuery(query)
		.setViewType(view_type)
		//		.setOrderType(OrderType.POPULAR)
		//		.setAppType(AppType.GAME)
		//		.setCategoryId(value)
		.setWithExtendedInfo(true);



		final List<App> full_app_list = new ArrayList<App>();

		final EntryCountWrapper entry_count_wrapper = new EntryCountWrapper();
		do {
			AppsRequest.Builder cloned_builder = (AppsRequest.Builder) apps_request_builder.clone();
			AppsRequest appsRequest = cloned_builder
			.setStartIndex(full_app_list.size())
			.setEntriesCount(REQUEST_ENTRY_LIMIT)	// This field is mandatory!
			.build();

			session.append(appsRequest, new Callback<AppsResponse>() {
				@Override
				public void onResult(ResponseContext context, AppsResponse response) {

					Log.d(TAG, "App count: " + response.getAppCount());
					Log.d(TAG, "Fetched list size: " + response.getAppList().size());
					Log.d(TAG, "Entries count: " + response.getEntriesCount());
					entry_count_wrapper.entry_count = response.getEntriesCount();

					full_app_list.addAll(response.getAppList());
					Log.e(TAG, "Size of full apps list: " + full_app_list.size());
				}
			});
			
			try {
				session.flush();
			} catch (SocketException e) {
				throw new IOException(e.getMessage());
			}
		} while (full_app_list.size() < entry_count_wrapper.entry_count && full_app_list.size() < MAX_APP_FETCH_LIMIT);

		return full_app_list;
	}
}
