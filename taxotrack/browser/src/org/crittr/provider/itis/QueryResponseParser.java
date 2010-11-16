package org.crittr.provider.itis;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.crittr.shared.browser.itis.ItisUtils;
import org.crittr.task.NetworkUnavailableException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Log;
import android.util.Xml;


public class QueryResponseParser extends DefaultHandler {


	static final String LOGGING_TAG = "ITIS";
	final String TAG = "ITIS";
	
	
	String ROOT_TAG;
	String MAIN_RESULT_CONTAINER;
	String AXIS_NAMESPACE;
	String REQUEST_BASE = "http://www.itis.gov/ITISWebService/services/ITISService/";
	static String ITIS_NAMESPACE = "http://itis_service.itis.usgs.org";
	static String AXIS210_NAMESPACE = "http://data.itis_service.itis.usgs.org/xsd";
	static String AXIS212_NAMESPACE = "http://metadata.itis_service.itis.usgs.org/xsd";
	static String AXIS28_NAMESPACE = "http://itis_service.itis.usgs.org/xsd";

	String[] result_element_keys;
    URL url;
	List<Map<String, String>> results_list = new ArrayList<Map<String, String>>(); 
	Map<String, String> current_item = new HashMap<String, String>();
	
	
    public QueryResponseParser(String method_name, String result_container_name, String query_argument, String argument_key, String[] keys) {

    	
    	result_element_keys = keys;
    	MAIN_RESULT_CONTAINER = result_container_name;
    	ROOT_TAG = method_name + "Response";
        
    	String full_url = null;
    	if (query_argument != null) {

			full_url = REQUEST_BASE + method_name + "?" + argument_key + "=" + Uri.encode(query_argument, "UTF-8");
    		AXIS_NAMESPACE = AXIS210_NAMESPACE;
    	}
    	else {
    		
    		full_url = REQUEST_BASE + method_name;
    		AXIS_NAMESPACE = AXIS212_NAMESPACE;
    	}
    	
//    	Log.i(LOGGING_TAG, "Query URL: " + full_url);
    	
        
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }

    protected InputStream getInputStream() throws NetworkUnavailableException {
        try {
            return url.openConnection().getInputStream();
		} catch (UnknownHostException e) {
        	throw new NetworkUnavailableException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	

    
    public List<Map<String, String>> parse() throws NetworkUnavailableException {
    	
        RootElement root = new RootElement( ITIS_NAMESPACE, ROOT_TAG );
        Element query_response = root.getChild( ITIS_NAMESPACE, ItisUtils.QUERY_RETURN_CONTAINER );
        
        Element result_item_container;
        if (MAIN_RESULT_CONTAINER != null)
        	result_item_container = query_response.getChild( AXIS_NAMESPACE, MAIN_RESULT_CONTAINER );	// These are the items of interest.
        else
        	result_item_container = query_response;
    	

        if (result_element_keys != null) {
	        for (final String key : result_element_keys) {
		        result_item_container.getChild( AXIS_NAMESPACE, key ).setEndTextElementListener(new EndTextElementListener(){
		            public void end(String body) {
		            	current_item.put(key, body);
		            }
		        });
	        }
	        
	        result_item_container.setEndElementListener(new EndElementListener() {
	            public void end() {
	            	results_list.add( current_item );
	            	current_item = new HashMap<String, String>();
	            }
	        });
        } else {
	        
	        result_item_container.setEndTextElementListener(new EndTextElementListener() {
				@Override
				public void end(String body) {
	            	current_item.put(MAIN_RESULT_CONTAINER, body);
	            	results_list.add( current_item );
	            	current_item = new HashMap<String, String>();
				}
	        });
        }
        
        
        
        
        
        
        try {
            Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (ParseException e) {
        	// FIXME: Sometimes we get a "ParseException: no element found" error.
        	Log.e(TAG, "Caught parsing exception in ITIS response...");

        	throw new NetworkUnavailableException();
		} catch (UnknownHostException e) {

			Log.e(TAG, "Caught UnknownHostException.");
        	throw new NetworkUnavailableException();
			
        } catch (Exception e) {

        	Log.e(TAG, "Caught generic exception.");
        	
        	throw new NetworkUnavailableException();
        	
//            throw new RuntimeException(e);
        } catch (AssertionError e) {
        	
        	Log.e(TAG, "No network access.");
        	Log.e(TAG, "Message: " + e.getCause().getMessage());
        	
        	throw new NetworkUnavailableException();
        }
                
        return results_list;
    }
}

