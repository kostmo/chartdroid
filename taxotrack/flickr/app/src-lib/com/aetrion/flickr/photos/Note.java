/*
 * Copyright (c) 2005 Aetrion LLC.
 */
package com.aetrion.flickr.photos;

//import java.awt.Rectangle;
import android.graphics.RectF;

/**
 * @author Anthony Eden
 */
public class Note {
	private static final long serialVersionUID = 12L;
    private String id;
    private String author;
    private String authorName;
//    private Rectangle bounds;
    private RectF bounds;
    private String text;

    public Note() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public RectF getBounds() {
//    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(RectF bounds) {
//      public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public void setBounds(String x, String y, String w, String h) {
        int xi = Integer.parseInt(x);
        int yi = Integer.parseInt(y);
        int wi = Integer.parseInt(w);
        int hi = Integer.parseInt(h);
//        setBounds(new Rectangle(xi, yi, wi, hi));
        setBounds(new RectF(xi, yi, xi+wi, yi+hi));	// Contructor for RectF uses left, top, right, bottom
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
