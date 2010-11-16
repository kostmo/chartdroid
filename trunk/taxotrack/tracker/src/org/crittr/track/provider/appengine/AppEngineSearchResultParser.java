package org.crittr.track.provider.appengine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;


public class AppEngineSearchResultParser extends DefaultHandler {


	final static String TAG = "Crittr";
	
    URL url;
//    public final static String REQUEST_BASE = "http://bugdroid.appspot.com/";
    
    
    public final static String REQUEST_BASE = "http://itismirror.appspot.com/vernacular_query.do?vernacular_query=";


    // ===============================================    
    static InputStream getInputStream(URL this_url) throws UnknownHostException {
        try {
            return this_url.openConnection().getInputStream();
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ===============================================

    public static List<String> parse_vernacular_search_results(String query_argument) throws UnknownHostException {

    	URL this_url = null;
		try {
			this_url = new URL(REQUEST_BASE + query_argument);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
            return new ArrayList<String>();
		}

    	
        RootElement root = new RootElement( "xml" );
        Element result_item_container = root.getChild( "matches" );

        final List<String> vernacular_suggestions = new ArrayList<String>(); 
        result_item_container.getChild("match").setStartElementListener(new StartElementListener() {
			
			public void start(Attributes attributes) {
				
				long tsn = Long.parseLong(attributes.getValue("tsn"));
				String vernacular = attributes.getValue("vernacular");
				
				vernacular_suggestions.add( vernacular );
			}
        });


        try {
            Xml.parse(getInputStream(this_url), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (UnknownHostException e) {
            throw e;
        } catch (Exception e) {
        	e.printStackTrace();
        }
                
        return vernacular_suggestions;
    }

}

