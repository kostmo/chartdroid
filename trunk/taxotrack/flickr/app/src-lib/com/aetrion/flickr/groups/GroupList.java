package com.aetrion.flickr.groups;

import com.aetrion.flickr.SearchResultList;

public class GroupList extends SearchResultList {
    private static final long serialVersionUID = 3344960036515265775L;

    public Group [] getGroupsArray() {
        return (Group[]) toArray(new Group[size()]);
    }

    public boolean add(Object obj) {
        // forces type to be group. Otherwise a class cast exception is thrown
        return super.add((Group)obj);
    }

}
