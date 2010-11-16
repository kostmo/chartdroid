package org.crittr.containers;

import java.util.Collection;

import org.crittr.shared.browser.containers.TaxonInfo;

import android.util.Log;

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.tags.Tag;

public class PhotoTsnPair implements PhotoContainer{
	
	
	static final String TAG = "Crittr";
	
	
	IdentificationStatus id_status = IdentificationStatus.INVALID_STATUS;
	
	
//	public long photo_id = ListActivityUserPhotosStandalone.INVALID_PHOTO_ID;
	public Photo photo;
//	public long tsn = DatabasePersonalTaxonomy.INVALID_TSN;
	public TaxonInfo taxon_info;
	
	/*
	public PhotoTsnPair(long photo_id, long tsn) {
		this.tsn = tsn;
		this.photo_id = photo_id;
	}
	*/
	
	public Photo getPhoto() {
		return photo;
	}
	
	public void setPhoto(Photo photo) {
		this.photo = photo;
	}

	public IdentificationStatus getIdStatus() {
		
		return id_status;
	}

	public void setIdStatus(IdentificationStatus idStatus) {
		// FIXME - We ignore the method argument.
		

		id_status = MapPointFlickrPhoto.determine_id_status(photo.getTags());
	}
	
}