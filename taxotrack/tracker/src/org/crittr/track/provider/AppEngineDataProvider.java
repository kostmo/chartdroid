package org.crittr.track.provider;


import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import org.crittr.shared.tracker.provider.AppEngineData;
import org.crittr.track.Market;
import org.crittr.track.provider.appengine.AppEngineResponseParser;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;


public class AppEngineDataProvider extends ContentProvider {

    private static final String TAG = Market.DEBUG_TAG;

        
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    }


	@Override
	public boolean onCreate() {
		
//		Log.d(TAG, "Executing onCreate() of TaxonSearchProvider");
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public String getType(Uri uri) {

		
		
		// TODO: FINISH ME
		
        int match = sUriMatcher.match(uri);
        switch (match)
        {

            default:
                return null;
        }
	}


	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
	

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		
//		Log.d(TAG, "Running query in TaxonSearchProvider...");
//		Log.i(TAG, uri.toString());
		

		if (uri.equals(AppEngineData.TaxoData.TAXON_POPULARITY_URI)) {

			MatrixCursor c = new MatrixCursor(projection);
			try {
				Map<Long, Integer> popularity_hash = AppEngineResponseParser.parse_taxon_popularity_response(selection);

				for (Entry<Long, Integer> key : popularity_hash.entrySet()) {
					MatrixCursor.RowBuilder row = c.newRow();
					row.add(key.getKey().longValue()).add(key.getValue().intValue());
				}

			} catch (UnknownHostException e) {
				
				e.printStackTrace();
				return null;
			}

			return c;
		}
		
		return null;
	}

}
