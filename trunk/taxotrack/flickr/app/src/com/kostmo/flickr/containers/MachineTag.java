// Version 1.0 of this File!!!!
// Date: 12/5/2009
// Author: Karl Ostmo

package com.kostmo.flickr.containers;

import java.util.ArrayList;
import java.util.Collection;

import android.text.TextUtils;

import com.aetrion.flickr.tags.Tag;



public class MachineTag extends Tag implements Comparable<MachineTag> {
	
	public static enum TagPart {NAMESPACE, PREDICATE, VALUE}
	
	public String namespace;
	public String predicate;
	public String value;
	
	public MachineTag() {
	}

	public MachineTag(Tag tag) {
		this(tag.getRaw() == null || tag.getRaw().length() == 0 ? tag.getValue() : tag.getRaw());
	}
	
	public MachineTag(String namespace, String predicate, String value) {
		this.namespace = namespace;
		this.predicate = predicate;
		this.value = value;
	}
	
	
	public static boolean checkIsParseable(String tag_string) {

		String full_machine_tag = tag_string.trim();
		int colon_pos = full_machine_tag.indexOf(':');
		
		return colon_pos >= 0;
	}
	
	
	public MachineTag(String machine_tag_string) {
		
		String full_machine_tag = machine_tag_string.trim();
		int colon_pos = full_machine_tag.indexOf(':');
		namespace = full_machine_tag.substring(0, colon_pos);
		int equals_pos = full_machine_tag.indexOf('=', colon_pos + 1);
		predicate = full_machine_tag.substring(colon_pos + 1, equals_pos);
		
		// Strip quotation marks from the value
		String possibly_quoted_value = full_machine_tag.substring(equals_pos + 1);
		if (possibly_quoted_value.startsWith("\"") && possibly_quoted_value.endsWith("\"")) {
			value = possibly_quoted_value.substring(1, possibly_quoted_value.length()-1);
		} else {
			value = possibly_quoted_value;
		}
	}
	
	@Override
	public int compareTo(MachineTag another) {
    	
    	if (namespace != null) {
			int by_namespace = namespace.compareTo(another.namespace);
			if (by_namespace != 0) return by_namespace;
    	}

    	if (predicate != null) {
			int by_predicate = predicate.compareTo(another.predicate);
			if (by_predicate != 0) return by_predicate;
    	}
		
    	if (value != null) {
			int by_value = value.compareTo(another.value);
			if (by_value != 0) return by_value;
    	}
		return 0;
	}
	
	public Tag toTag() {
		
    	Tag t = new Tag();
    	t.setIsMachineTag(true);
    	t.setRaw( this.getRaw() );
    	return t;
	}
    
	@Override
	public String getRaw() {
		return namespace + ":" + predicate + "=" + value;
	}
	
	@Override
	public boolean isMachineTag() {
		return true;
	}
	
	
	@Override
	public String toString() {
		// TODO: FlickrJ needs a workaround...
		return namespace + ":" + predicate + "=" + "\"" + value + "\"";
	}
	
	
	static String wrapQuotes(String string) {
		return "\"" + string + "\"";
	}
	

	
	/** "null" for a field indicates a wildcard. */
	public String getQueryString() {
		String machinetag_query_string;
		String namespace_string = this.namespace != null ? this.namespace : "*";
		
		if (this.predicate != null) {
			if (this.value != null) {
				machinetag_query_string = namespace_string + ":" + this.predicate + "=" + this.value;
			} else {
				machinetag_query_string = namespace_string + ":" + this.predicate + "=";
			}
		} else {
			if (this.value != null) {
				machinetag_query_string = namespace_string + ":*=" + this.value;
			} else {
				machinetag_query_string = namespace_string + ":";
			}
		}
		
		return machinetag_query_string;
	}
}