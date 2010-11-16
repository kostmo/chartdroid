package org.crittr.track.provider.appengine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;
import android.util.Xml.Encoding;


public class AppEngineStatsParser extends DefaultHandler {


	final static String TAG = "Crittr";
	
    public final static String REQUEST_BASE = "http://bugdroid.appspot.com/";
	
    public AppEngineStatsParser(String query_argument) {


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
    
    public static class CountContainer {
    	public int photos, users;
    }
    
    static public CountContainer parse_stats(String query_argument) {
    	
        RootElement root = new RootElement( "xml" );
        Element result_item_container = root.getChild("stats");

        
        final CountContainer counts = new CountContainer();
        result_item_container.getChild("users").setStartElementListener(new StartElementListener() {

			public void start(Attributes attributes) {
				String status = attributes.getValue("count");
				counts.users = Integer.parseInt( status );
			}
        });

        
        
        result_item_container.getChild("photos").setStartElementListener(new StartElementListener() {

			public void start(Attributes attributes) {
				String status = attributes.getValue("count");
				counts.photos = Integer.parseInt( status );
			}
        });
        
        
        

        try {

        	
        	String full_url = REQUEST_BASE + query_argument;
//        	Log.i(TAG, "Query URL: " + full_url);
        	

            URL url = null;
            try {
    			url = new URL(full_url);
    		} catch (MalformedURLException e) {
    			e.printStackTrace();
    		}
    		
            Xml.parse(getInputStream(url), Encoding.UTF_8, root.getContentHandler());
           
        } catch (Exception e) {
        	e.printStackTrace();
//            throw new RuntimeException(e);
        }

        return counts;
    }
}

