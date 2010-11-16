package com.kostmo.flickr.containers;



import com.aetrion.flickr.photos.Photo;

public interface PhotoContainer {

	public Photo getPhoto();
	
	public void setPhoto(Photo photo);
}