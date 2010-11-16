/*
 * Copyright (c) 2005 Aetrion LLC.
 */
package com.aetrion.flickr.photos;

import com.kostmo.flickr.bettr.Market;

import android.util.Log;

/**
 * This class descibes a Size of a Photo.<p>
 *
 * @author Anthony Eden
 * @version $Id: Size.java,v 1.6 2009/07/12 22:43:07 x-mago Exp $
 */
public class Size {
	private static final long serialVersionUID = 12L;

	public enum SizeType {
		THUMB,	// Thumbnail, 100 on longest side.
		SQUARE,	// Small square 75x75.
		SMALL,	// Small, 240 on longest side.
		MEDIUM500,	// Medium, 500 on longest side.
		MEDIUM640,	// Medium, 640 on longest side.
		LARGE,	// Large, 1024 on longest side (only exists for very large original images).
		ORIGINAL}


    private int label;
    private int width;
    private int height;
    private String source;
    private String url;
    private String description;

    public Size() {

    }

    /**
     * Size of the Photo.
     *
     * @return label
     * @see com.aetrion.flickr.photos.Size#THUMB
     * @see com.aetrion.flickr.photos.Size#SQUARE
     * @see com.aetrion.flickr.photos.Size#SMALL
     * @see com.aetrion.flickr.photos.Size#MEDIUM500
     * @see com.aetrion.flickr.photos.Size#MEDIUM640
     * @see com.aetrion.flickr.photos.Size#LARGE
     * @see com.aetrion.flickr.photos.Size#ORIGINAL
     */
    public int getLabel() {
        return label;
    }

    public String getDescription() {
        return this.description;
    }
    
    public void setLabel(String label) {
    	
    	this.description = label;
    	
        if (label.equals("Square")) {
            setLabel(SizeType.SQUARE.ordinal());
        } else if (label.equals("Thumbnail")) {
            setLabel(SizeType.THUMB.ordinal());
        } else if (label.equals("Small")) {
            setLabel(SizeType.SMALL.ordinal());
        } else if (label.equals("Medium")) {
            setLabel(SizeType.MEDIUM500.ordinal());
        } else if (label.equals("Medium 640")) {
            setLabel(SizeType.MEDIUM640.ordinal());
        } else if (label.equals("Large")) {
            setLabel(SizeType.LARGE.ordinal());
        } else if (label.equals("Original")) {
            setLabel(SizeType.ORIGINAL.ordinal());
        }
    }

    /**
     * Size of the Photo.
     *
     * @param label The integer-representation of a size
     * @see com.aetrion.flickr.photos.Size#THUMB
     * @see com.aetrion.flickr.photos.Size#SQUARE
     * @see com.aetrion.flickr.photos.Size#SMALL
     * @see com.aetrion.flickr.photos.Size#MEDIUM
     * @see com.aetrion.flickr.photos.Size#LARGE
     * @see com.aetrion.flickr.photos.Size#ORIGINAL
     */
    public void setLabel(int label) {
        this.label = label;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setWidth(String width) {
        if (width != null) {
            setWidth(Integer.parseInt(width));
        }
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setHeight(String height) {
        if (height != null) {
            setHeight(Integer.parseInt(height));
        }
    }

    /**
     * URL of the image.
     *
     * @return Image-URL
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * URL of the photopage.
     *
     * @return Page-URL
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
