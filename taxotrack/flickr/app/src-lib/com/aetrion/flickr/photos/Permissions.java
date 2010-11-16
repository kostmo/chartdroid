/*
 * Copyright (c) 2005 Aetrion LLC.
 */
package com.aetrion.flickr.photos;

/**
 * @author Anthony Eden
 */
public class Permissions {
	private static final long serialVersionUID = 12L;

    private String id;
    private boolean publicFlag;
    private boolean friendFlag;
    private boolean familyFlag;
    private int comment = 0;
    private int addmeta = 0;

    public Permissions() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isPublicFlag() {
        return publicFlag;
    }

    public void setPublicFlag(boolean publicFlag) {
        this.publicFlag = publicFlag;
    }

    public boolean isFriendFlag() {
        return friendFlag;
    }

    public void setFriendFlag(boolean friendFlag) {
        this.friendFlag = friendFlag;
    }

    public boolean isFamilyFlag() {
        return familyFlag;
    }

    public void setFamilyFlag(boolean familyFlag) {
        this.familyFlag = familyFlag;
    }

    public int getComment() {
        return comment;
    }

    public void setComment(int comment) {
        this.comment = comment;
    }

    public void setComment(String comment) {
        if (comment != null) setComment(Integer.parseInt(comment));
    }

    public int getAddmeta() {
        return addmeta;
    }

    public void setAddmeta(int addmeta) {
        this.addmeta = addmeta;
    }

    public void setAddmeta(String addmeta) {
        if (addmeta != null) setAddmeta(Integer.parseInt(addmeta));
    }

}
