package org.crittr.track.containers;

import java.net.URL;

import org.crittr.shared.browser.Constants.CollectionSource;

public class ThumbnailUrlPlusLinkContainer {
	
	private CollectionSource source;
	
	public ThumbnailUrlPlusLinkContainer() {
		
	}
	
	public ThumbnailUrlPlusLinkContainer(String l, URL t) {
		link = l;
		thumbnail_url = t;
	}
	
	private String link, identifier;
	private URL thumbnail_url, large_url;
	public String getLink() {
		return link;
	}
	
	
	public void setSource(CollectionSource s) {
		source = s;
	}
	
	public URL getThumbnailUrl() {
		return thumbnail_url;
	}
	
	public URL getLargeUrl() {
		
		return large_url;
	}
	
	public void setLink(String l) {
		link = l;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String i) {
		identifier = i;
	}
	
	public void setThumbnailUrl(URL t) {
		thumbnail_url = t;
	}
	
	public void setLargeUrl(URL t) {
		large_url = t;
	}
}