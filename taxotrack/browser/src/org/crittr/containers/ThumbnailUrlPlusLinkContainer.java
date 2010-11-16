package org.crittr.containers;

import java.net.URL;

import org.crittr.shared.browser.Constants.CollectionSource;
import org.crittr.shared.browser.utilities.MediawikiSearchResponseParser;
import org.crittr.shared.browser.utilities.MediawikiSearchResponseParser.URLholder;
import org.crittr.task.NetworkUnavailableException;

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
		
		if (large_url == null) {
			if (source != CollectionSource.FLICKR) {
				
				// Look up the url from MediaWiki API
				try {
					int width = 480;
					int height = width;
					URLholder url_holder = MediawikiSearchResponseParser.getSingleThumbnailUrl(
							identifier,
							width,
							height,
							source == CollectionSource.WIKIPEDIA);
					
					return url_holder.url;
				} catch (NetworkUnavailableException e) {
					e.printStackTrace();
				}
			}
		}
			
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