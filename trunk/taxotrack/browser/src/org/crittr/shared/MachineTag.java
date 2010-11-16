package org.crittr.shared;

import com.aetrion.flickr.tags.Tag;

public class MachineTag implements Comparable<MachineTag> {
	
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
	
	public MachineTag(String full_machine_tag) {
		
		int colon_pos = full_machine_tag.indexOf(':');
		if (colon_pos >= 0) {
			namespace = full_machine_tag.substring(0, colon_pos);
			int equals_pos = full_machine_tag.indexOf('=', colon_pos + 1);
			if (equals_pos >= 0) {
				predicate = full_machine_tag.substring(colon_pos + 1, equals_pos);
				value = full_machine_tag.substring(equals_pos + 1);
			}
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
    	t.setRaw( this.toRaw() );
    	return t;
	}
    
	public String toRaw() {
		return namespace + ":" + predicate + "=" + value;
	}
	
	
	@Override
	public String toString() {
		// TODO: FlickrJ needs a workaround...
		return namespace + ":" + predicate + "=" + "\"" + value + "\"";
	}
	
	
	
	public String getQueryString() {
		String machinetag_query_string;
		String namespace_string = this.namespace != null ? this.namespace : "*";
		
		if (this.predicate != null) {
			if (this.value != null) {
				
				machinetag_query_string = "\"machine_tags\" => \"" + namespace_string + ":" + this.predicate + "=" + this.value + "\"";
			} else {
				
				machinetag_query_string = "\"machine_tags\" => \"" + namespace_string + ":" + this.predicate + "=\"";
			}
		} else {
			if (this.value != null) {
				
				machinetag_query_string = "\"machine_tags\" => \"" + namespace_string + ":*=" + this.value + "\"";
			} else {
				
				machinetag_query_string = "\"machine_tags\" => \"" + namespace_string + ":\"";
			}
			
		}
		
		return machinetag_query_string;
	}
}