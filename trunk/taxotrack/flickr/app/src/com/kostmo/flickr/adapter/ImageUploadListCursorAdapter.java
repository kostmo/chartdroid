package com.kostmo.flickr.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.kostmo.flickr.activity.BatchUploaderActivity.ImageUploadData;
import com.kostmo.flickr.activity.BatchUploaderActivity.UploadStatus;
import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.bettr.R;
import com.kostmo.flickr.data.DatabaseUploads;
import com.kostmo.flickr.tasks.MediaStoreImagePopulatorTask;
import com.kostmo.tools.SemaphoreHost;

public class ImageUploadListCursorAdapter extends ResourceCursorAdapter {

    Context context;
    SemaphoreHost host;
    public boolean is_list_enabled = true;
    DatabaseUploads database;
    
    // ========================================================================
    public ImageUploadListCursorAdapter(Context context, SemaphoreHost host, int layout_resource, Cursor cursor) {
    	super(context, layout_resource, cursor);
    	
        this.context = context;
        this.host = host;
		this.database = new DatabaseUploads(this.context);
    }

    // ========================================================================
    public static class UploadViewHolder {
    	public TextView title, description, geo_coords, upload_index, upload_status;
    	public ImageView icon, geo_icon;
    }
    
    // ========================================================================
    @Override
	public boolean areAllItemsEnabled() {
    	return this.is_list_enabled;
    }
    
    // ========================================================================
    @Override
	public boolean isEnabled(int position) {
    	return this.is_list_enabled;
    }


    // ========================================================================
	public List<ImageUploadData> gatherUploadsList() {
		List<ImageUploadData> upload_list = new ArrayList<ImageUploadData>();
		
		
		Cursor cursor = this.getCursor();
		if (cursor.moveToFirst()) {
			do {
				ImageUploadData upload_data = wrapCursorRow(this.database, cursor);
				
				// Don't upload the ones that have completed successfully.
				if (UploadStatus.PENDING.equals( upload_data.upload_status ))
					upload_list.add(upload_data);
				
			} while (cursor.moveToNext());
		}
		
		return upload_list;
	}

    // ========================================================================
	void populateMediaStoreImageData(Uri uri, UploadViewHolder holder) {
		new MediaStoreImagePopulatorTask(host, context.getContentResolver(), holder).execute(uri);
	}

    // ========================================================================
	public static ImageUploadData wrapCursorRow(DatabaseUploads database, Cursor cursor) {
		ImageUploadData upload_data = new ImageUploadData();
		upload_data.description = cursor.getString(cursor.getColumnIndex(DatabaseUploads.KEY_UPLOAD_DESCRIPTION));
		upload_data.title = cursor.getString(cursor.getColumnIndex(DatabaseUploads.KEY_UPLOAD_TITLE));
		
		upload_data.image_uri = Uri.parse(cursor.getString(cursor.getColumnIndex(DatabaseUploads.KEY_UPLOAD_FILE_URI)));
		upload_data.upload_status = UploadStatus.values()[cursor.getInt(cursor.getColumnIndex(DatabaseUploads.KEY_UPLOAD_STATUS))];
		upload_data.row_id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
		
		
		upload_data.tags = database.getTagsForUpload(upload_data.row_id);
		
		
		return upload_data;
	}

    // ========================================================================
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		
        UploadViewHolder holder = new UploadViewHolder();

        holder.title = (TextView) view.findViewById(android.R.id.text1);
        holder.description = (TextView) view.findViewById(android.R.id.text2);
        holder.geo_coords = (TextView) view.findViewById(R.id.geo_coords);
        holder.upload_index = (TextView) view.findViewById(R.id.upload_index);
        holder.upload_status = (TextView) view.findViewById(R.id.upload_status);
        
        holder.icon = (ImageView) view.findViewById(android.R.id.icon);
        holder.geo_icon = (ImageView) view.findViewById(R.id.geo_icon);
        
        ImageUploadData upload_data = wrapCursorRow(this.database, cursor);
        holder.upload_index.setText( "(" + (cursor.getPosition() + 1) + ")");
        holder.upload_status.setText(upload_data.upload_status.name);
        holder.upload_status.setTextColor(upload_data.upload_status.color);
        
        
        // At the time this view is bound, the title and description should be
        // stored in my personal upload database.
		holder.title.setText(upload_data.title);		
		if (upload_data.description == null || upload_data.description.length() == 0) {
			holder.description.setVisibility(View.GONE);
		} else {
			holder.description.setText( upload_data.description );
			holder.description.setVisibility(View.VISIBLE);
		}
        
        populateMediaStoreImageData(upload_data.image_uri, holder);
	}
}