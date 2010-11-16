package org.crittr.track.provider.appengine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crittr.track.Market;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;


public class AppEngineResponseParser extends DefaultHandler {


	final static String TAG = Market.DEBUG_TAG;
	
    URL url;
    public final static String REQUEST_BASE = "http://bugdroid.appspot.com/";
	
    public AppEngineResponseParser(String query_argument) {

    	String full_url = REQUEST_BASE + query_argument;
//    	Log.i(TAG, "Query URL: " + full_url);
    	
        
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }


    // ===============================================    
    public static InputStream getInputStream(URL this_url) throws UnknownHostException {
        try {
            return this_url.openConnection().getInputStream();
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // ===============================================
    
    protected InputStream getInputStream() {
        try {
            return url.openConnection().getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    // ===============================================

    public static Map<Long, Integer> parse_taxon_popularity_response(String query_argument) throws UnknownHostException {

    	URL this_url = null;
		try {
			this_url = new URL(REQUEST_BASE + query_argument);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
            return new HashMap<Long, Integer>();
		}

    	
    	
        RootElement root = new RootElement( "xml" );
        Element result_item_container = root.getChild( "taxons" );

        final Map<Long, Integer> popularity_hash = new HashMap<Long, Integer>(); 
        result_item_container.getChild("taxon").setStartElementListener(new StartElementListener() {

			
			public void start(Attributes attributes) {
				long tsn = Long.parseLong(attributes.getValue("tsn"));
				int frequency = Integer.parseInt(attributes.getValue("hits"));
				popularity_hash.put(tsn, frequency);
			}
        });


        try {
            Xml.parse(getInputStream(this_url), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (UnknownHostException e) {
            throw e;
        } catch (Exception e) {
        	e.printStackTrace();
        }
                
        return popularity_hash;
    }

    // ===============================================
    
    static public String parse_generic_response_string(String xml_string) {
    	
        RootElement root = new RootElement( "xml" );
        Element result_item_container = root;

        final List<String> result = new ArrayList<String>(); 
        result_item_container.getChild("request").setStartElementListener(new StartElementListener() {

			
			public void start(Attributes attributes) {
				String status = attributes.getValue("status");
				result.add( status );
			}
        });


        try {

            Xml.parse(xml_string, root.getContentHandler());
           
        } catch (Exception e) {
        	e.printStackTrace();
//            throw new RuntimeException(e);
        }
        
        if (result.size() > 0)
        	return result.get(0);
        return null;
    }
    // ===============================================

    public List<Map<String, String>> parse_photos_response() {
    	
        RootElement root = new RootElement( "xml" );
        Element result_item_container = root.getChild( "photos" );

        final List<Map<String, String>> result_hash = new ArrayList<Map<String, String>>(); 
        result_item_container.getChild("photo").setStartElementListener(new StartElementListener() {
			
			public void start(Attributes attributes) {
				Map<String, String> row_result = new HashMap<String, String>();
				String photo_id = attributes.getValue("id");
				Log.d(TAG, "Got photo id: " + photo_id);
				row_result.put("id", photo_id);
				result_hash.add(row_result);
			}
        });


        try {

            Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
            
        } catch (Exception e) {
        	e.printStackTrace();
//            throw new RuntimeException(e);
        }
                
        return result_hash;
    }
    
    
    
    
    static public List<Map<String, String>> parse_photos_response_string(String xml_string) {
    	
        RootElement root = new RootElement( "xml" );
        Element result_item_container = root.getChild( "photos" );

        final List<Map<String, String>> result_hash = new ArrayList<Map<String, String>>(); 
        result_item_container.getChild("photo").setStartElementListener(new StartElementListener() {

			
			public void start(Attributes attributes) {
				Map<String, String> row_result = new HashMap<String, String>();
				String photo_id = attributes.getValue("id");
				Log.d(TAG, "Got photo id: " + photo_id);
				row_result.put("id", photo_id);
				result_hash.add(row_result);
			}
        });


        try {

            Xml.parse(xml_string, root.getContentHandler());
           
        } catch (Exception e) {
        	e.printStackTrace();
//            throw new RuntimeException(e);
        }
                
        return result_hash;
    }
}

