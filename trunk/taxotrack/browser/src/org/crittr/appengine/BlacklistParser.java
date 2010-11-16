package org.crittr.appengine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.crittr.flickr.BlacklistDatabase;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;


public class BlacklistParser extends DefaultHandler {


	final static String TAG = "Crittr";
	
    URL url;
	public final static String REQUEST_BASE = "http://bugdroid.appspot.com/";
	
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

    public static void import_blacklist(BlacklistDatabase db_helper, String query_argument) throws UnknownHostException {

    	URL this_url = null;
		try {
			this_url = new URL(REQUEST_BASE + query_argument);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

    	
        RootElement root = new RootElement( "blacklist" );

        final List<String> blacklist = new ArrayList<String>(); 
        root.getChild("image").setStartElementListener(new StartElementListener() {
			
			public void start(Attributes attributes) {
				
				String blacklisted_image = attributes.getValue("title");
				
				blacklist.add( blacklisted_image );
			}
        });


        try {
            Xml.parse(getInputStream(this_url), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (UnknownHostException e) {
            throw e;
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        
        db_helper.importBlacklist(blacklist);
                
    }

}

