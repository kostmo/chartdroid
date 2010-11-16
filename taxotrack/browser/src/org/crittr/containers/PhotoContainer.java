package org.crittr.containers;


import android.graphics.Color;

import com.aetrion.flickr.photos.Photo;

public interface PhotoContainer {

	// White, Flickr Blue, Flickr Pink
	public static String[] IdLabelMap = {"unidentified", "identified", "contested", "invalid label"};
	public static int[] IdColorMap = {Color.WHITE, 0xff0465dc, 0xfffe0486, Color.DKGRAY};
	
	public enum IdentificationStatus {
		UNIDENTIFIED, IDENTIFIED, CONTESTED, INVALID_STATUS
	}
	
	

	public void setIdStatus(IdentificationStatus id_status);

	public IdentificationStatus getIdStatus();
	
	public Photo getPhoto();
	
	public void setPhoto(Photo photo);
}