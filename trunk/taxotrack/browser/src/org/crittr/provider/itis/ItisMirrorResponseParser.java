package org.crittr.provider.itis;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.crittr.shared.browser.containers.TaxonInfo;


import android.net.Uri;
import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;


public class ItisMirrorResponseParser extends DefaultHandler {


	final static String TAG = "VernacularAutoComplete";
	
	
    URL url;
	String REQUEST_BASE = "http://itismirror.appspot.com/vernacular_query.do";
	

	
    public ItisMirrorResponseParser(String query_argument) {
        
    	String full_url = null;
		full_url = REQUEST_BASE + "?" + "vernacular_query" + "=" + Uri.encode(query_argument, "UTF-8");

		// FIXME:
		full_url += "&kingdom_key=agppdGlzbWlycm9ychELEgtLaW5nZG9tTmFtZRgFDA" +
				"&rank_inequality=" +
				Uri.encode("<", "UTF-8") +
				"&rank_key=220";
		

//    	Log.i(TAG, "Query URL: " + full_url);
    	
        
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }

    protected InputStream getInputStream() {
        try {
            return url.openConnection().getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	

    public ArrayList<TaxonInfo> parse() {
    	
        RootElement root = new RootElement( "xml" );
        Element result_item_container = root.getChild( "matches" );

        final ArrayList<TaxonInfo> result_hash = new ArrayList<TaxonInfo>(); 
        result_item_container.getChild("match").setStartElementListener(new StartElementListener() {

			@Override
			public void start(Attributes attributes) {
				TaxonInfo taxon_info = new TaxonInfo();

				ArrayList<String> unit_names = new ArrayList<String>();
				for (String unit_name : new String[] {"unit_name1", "unit_name2", "unit_name3"})
					if (unit_name != null && unit_name.length() > 0) unit_names.add(unit_name);

				taxon_info.taxon_name = TextUtils.join(" ", unit_names);
				taxon_info.rank_name = attributes.getValue("rankname");
				taxon_info.tsn = Long.parseLong( attributes.getValue("tsn") );
				taxon_info.vernacular_name = attributes.getValue("vernacular_name");
				Log.d(TAG, "Got vernacular name: " + taxon_info.vernacular_name);

				result_hash.add(taxon_info);
			}
        });


        try {
            Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
                
        return result_hash;
    }
}

