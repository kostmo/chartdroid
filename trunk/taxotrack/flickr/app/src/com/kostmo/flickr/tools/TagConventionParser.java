package com.kostmo.flickr.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.kostmo.flickr.bettr.Market;
import com.kostmo.flickr.containers.NetworkUnavailableException;
import com.kostmo.flickr.data.BetterMachineTagDatabase;


public class TagConventionParser {


	static final String TAG = Market.DEBUG_TAG; 
	
    URL url;
	public final static String REQUEST_BASE = "http://bugdroid.appspot.com/";
	
	Context context;
	
	static String NAMESPACE_ELEMENT = "namespace";
	static String PREDICATE_ELEMENT = "predicate";
	static String VALUE_ELEMENT = "value";
	
	static String TITLE_ATTRIBUTE = "title";
	static String COLOR_ATTRIBUTE = "color";
	
	
	static String convention_url;
	
    public TagConventionParser(Context c, String query_argument) {
        
    	context = c;
    	String full_url = REQUEST_BASE + query_argument;

    	convention_url = full_url;

    	Log.i(TAG, "Query URL: " + full_url);
        
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }


    
    protected InputStream getInputStream() throws NetworkUnavailableException, IOException {
        try {
            return url.openConnection().getInputStream();
        } catch (UnknownHostException e) {
        	throw new NetworkUnavailableException();
        }
    }
	

    public void parse() throws NetworkUnavailableException {

    	// XML parser setup:
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document dom = null;
		try {
			builder = factory.newDocumentBuilder();
			dom = builder.parse(this.getInputStream());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Element document_root = dom.getDocumentElement();
    	
    	
    	// Database setup:
    	BetterMachineTagDatabase helper = new BetterMachineTagDatabase(context);
	    SQLiteDatabase db = helper.getWritableDatabase();
		
		db.beginTransaction();
//		final long start_time = System.nanoTime();
		
		
		ContentValues c0 = new ContentValues();
        c0.put(BetterMachineTagDatabase.KEY_URI, convention_url);
        long convention_id = db.insert(BetterMachineTagDatabase.TABLE_CONVENTIONS, null, c0);
		
		
		NodeList namespaces = document_root.getElementsByTagName( NAMESPACE_ELEMENT );
		
//		Log.e(TAG, "How many elements with tag name '" + NAMESPACE_ELEMENT + "'?" + namespaces.getLength());
		
		for (int i=0; i<namespaces.getLength(); i++) {
			
			Node namespace = namespaces.item(i);
            ContentValues c = new ContentValues();
            c.put(BetterMachineTagDatabase.KEY_TITLE, namespace.getAttributes().getNamedItem(TITLE_ATTRIBUTE).getNodeValue());
            c.put(BetterMachineTagDatabase.KEY_CONVENTION_ID, convention_id);
            long namespace_id = db.insert(BetterMachineTagDatabase.TABLE_NAMESPACES, null, c);
            
            
            NodeList predicates = ((Element) namespace).getElementsByTagName( PREDICATE_ELEMENT );
            for (int j=0; j<predicates.getLength(); j++) {
            	
    			Node predicate = predicates.item(j);
                ContentValues c1 = new ContentValues();
                c1.put(BetterMachineTagDatabase.KEY_TITLE, predicate.getAttributes().getNamedItem(TITLE_ATTRIBUTE).getNodeValue());
                c1.put(BetterMachineTagDatabase.KEY_PARENT, namespace_id);
                c1.put(BetterMachineTagDatabase.KEY_CONVENTION_ID, convention_id);
                long predicate_id = db.insert(BetterMachineTagDatabase.TABLE_PREDICATES, null, c1);
                
                
                NodeList values = ((Element) predicate).getElementsByTagName( VALUE_ELEMENT );
                for (int k=0; k<values.getLength(); k++) {
                	
        			Node value = values.item(k);
                    ContentValues c2 = new ContentValues();
                    c2.put(BetterMachineTagDatabase.KEY_TITLE, value.getAttributes().getNamedItem(TITLE_ATTRIBUTE).getNodeValue());
                    Node color = value.getAttributes().getNamedItem(COLOR_ATTRIBUTE);
                    if (color != null)
                    	c2.put(BetterMachineTagDatabase.KEY_COLOR, color.getNodeValue());
                    c2.put(BetterMachineTagDatabase.KEY_PARENT, predicate_id);
                    c2.put(BetterMachineTagDatabase.KEY_CONVENTION_ID, convention_id);
                    db.insert(BetterMachineTagDatabase.TABLE_VALUES, null, c2);
                }
            }
		}
		

	    try {
	    	db.setTransactionSuccessful();
	    } finally {
	    	db.endTransaction();
	    }

	    db.close();
	    
		Log.e(TAG, "Tag convention imported.");
    }
}

