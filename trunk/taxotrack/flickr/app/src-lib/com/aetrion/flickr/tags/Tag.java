/*
 * Copyright (c) 2005 Aetrion LLC.
 */
package com.aetrion.flickr.tags;

/**
 * @author Anthony Eden
 */
public class Tag {
	private static final long serialVersionUID = 12L;

    private String id;
    private String author;
    private String authorName;
    private String raw;
    private String value;
    private int count;

	private boolean isMachineTag;

    public Tag() {

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

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setCount(String count) {
        setCount(Integer.parseInt(count));
    }

    public boolean isMachineTag() {
		return this.isMachineTag;
    }

    public void setIsMachineTag(boolean is_machine) {
        this.isMachineTag = is_machine;
    }
}
