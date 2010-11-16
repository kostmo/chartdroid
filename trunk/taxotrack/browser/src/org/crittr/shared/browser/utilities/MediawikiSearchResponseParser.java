package org.crittr.shared.browser.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.crittr.browse.AsyncTaskModified;
import org.crittr.browse.Market;
import org.crittr.containers.ThumbnailUrlPlusLinkContainer;
import org.crittr.flickr.BlacklistDatabase;
import org.crittr.shared.browser.Constants.CollectionSource;
import org.crittr.task.NetworkUnavailableException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;




public class MediawikiSearchResponseParser {
	
	final static int CATEGORY_NAMESPACE_INDEX = 14;
	final static int IMAGE_NAMESPACE_INDEX = 6;

	public final static String PREFKEY_BLACKLIST_ENABLED = "blacklist_enabled";

	final static String TAG = Market.DEBUG_TAG;
	
    URL url;
	static String COMMONS_REQUEST_BASE = "http://commons.wikimedia.org/w/";
	static String WIKIPEDIA_REQUEST_BASE = "http://en.wikipedia.org/w/";
	
	Context context;
	int max_results;
	
	
	List<String> searchable_image_blacklist;
	
	boolean blacklist_enabled;
	
    public MediawikiSearchResponseParser(Context c) {
        
    	context = c;

    	BlacklistDatabase db_helper = new BlacklistDatabase(context);
    	searchable_image_blacklist = db_helper.getBlacklist();
    	
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		
    	blacklist_enabled = settings.getBoolean(PREFKEY_BLACKLIST_ENABLED, true);
    	

    	max_results = 500;
    }


    
    protected InputStream getInputStream() {
        try {
        	
//            return url.openConnection().getInputStream();
        	return fetch( url.toString() );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
    
    
	// =============================================================
	
	public static List<ThumbnailUrlPlusLinkContainer> getCommonsPhotoMatchesRecursive(Context context, AsyncTaskModified task, String initial_taxon_name, int search_quota) {
		
		
		List<ThumbnailUrlPlusLinkContainer> commons_thumbnail_results = new ArrayList<ThumbnailUrlPlusLinkContainer>();
		
		
		MediawikiSearchResponseParser mwsrp = new MediawikiSearchResponseParser(context);

    	// Attempt BFS recursion.
    	List<String> pending_children_queue = new ArrayList<String>();
    	List<String> visited_children_queue;
    	List<String> parental_queue = new ArrayList<String>();
    	
    	String seed_category = "Category:" + initial_taxon_name;
		parental_queue.add( seed_category );
		pending_children_queue.add( seed_category );
    	
//    	Log.e(TAG, "Starting recursive search with: " + seed_category);

		int i=0;
		boolean quota_unmet = true;
		while (quota_unmet) {
			
			visited_children_queue = new ArrayList<String>();
			
			// We initially populate this with 1 element.
			for (String parent_category_name: parental_queue) {
				
				// Contract: I should have at least the taxon name already populated.
				for (String immediate_category_name: pending_children_queue) {

					List<ThumbnailUrlPlusLinkContainer> new_results;
					try {
						new_results = mwsrp.parse_category_thumbnails(immediate_category_name, false);
					} catch (NetworkUnavailableException e) {
						new_results = new ArrayList<ThumbnailUrlPlusLinkContainer>();
					}
					
//			    	Log.e(TAG, "Found " + new_results.size() + " thumbnails in the immediate category " + immediate_category_name);

					commons_thumbnail_results.addAll(new_results);
					i += new_results.size();
					

					if (i >= search_quota) break;
				}
				
				if (task.isCancelled()) {
					break;
				}
				
	
				visited_children_queue.addAll(pending_children_queue);
	
				if (i < search_quota) {
					
//					Log.e(TAG, "We have not met (" + i + ") our quota of " + search_quota + "!  Iterating again...");
	
					// If we need to, get the children:
					List<String> taxon_members;
					try {
						taxon_members = mwsrp.parse_subcategories(parent_category_name);
					} catch (NetworkUnavailableException e) {
						taxon_members = new ArrayList<String>();
					}

					pending_children_queue = taxon_members;

				} else {
					quota_unmet = false;
				}
			}
			
			if (task.isCancelled() || visited_children_queue.isEmpty()) {
//				Log.w(TAG, "Terminated without meeting quota for " + initial_taxon_name);
				break;
			}
			
			parental_queue = visited_children_queue;
			
			// We've exhausted the parents at the current level; advance to the next level deep
		}
		
		
		return commons_thumbnail_results;
	}

	// =============================================================

    public List<String> parse_subcategories(String prefixed_category_name) throws NetworkUnavailableException {

    	
    	// Strategy:
    	// Search once in namespace 6 (for images), then
    	// once in namespace 14 (for subcategories), breadth-first.

    	
    	String query_url = null;
		try {
			query_url = "api.php?action=query&format=xml&redirects&generator=categorymembers&gcmnamespace=" + CATEGORY_NAMESPACE_INDEX +
			"&gcmlimit=" +
			max_results +
			"&gcmtitle=" + URLEncoder.encode(prefixed_category_name, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
    	
    	String full_url = null;

		full_url = COMMONS_REQUEST_BASE + query_url;
		
    	
    	
    	
//    	Log.i(TAG, "Category query URL: " + full_url);
        
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

    	final List<String> subcategory_list = new ArrayList<String>();
    	
        RootElement root = new RootElement( "api" );
        Element query_response = root.getChild( "query");
        Element page_container = query_response.getChild( "pages");
        Element page_element = page_container.getChild( "page");

        
        page_element.setStartElementListener(new StartElementListener(){

			public void start(Attributes attributes) {

				String namespace_string = attributes.getValue( "ns" );
				if (namespace_string != null && namespace_string.length() > 0) {
					int namespace_index = Integer.parseInt(namespace_string);
					if (namespace_index == 14) {
				
						String category_title = attributes.getValue( "title" );
						if (category_title != null && category_title.length() > 0)
							subcategory_list.add( category_title );
					}
				}
			}
        });
        
        
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
        	
        	e.printStackTrace();
        	
        	throw new NetworkUnavailableException();
//            throw new RuntimeException(e);
        } catch (AssertionError e) {
        	
        	Log.e(TAG, "No network access.");
        	Log.e(TAG, "Message: " + e.getCause().getMessage());
        	
        	throw new NetworkUnavailableException();
        }
         
        
       return subcategory_list;
       
    }
    
    
    
    
    class PendingWrapper {
    	
    	ThumbnailUrlPlusLinkContainer tuplc;
    }
    
    boolean skip_page = false;
    public List<ThumbnailUrlPlusLinkContainer> parse_category_thumbnails(String prefixed_taxon_name, final boolean wikipedia_search) throws NetworkUnavailableException {

    	
    	int image_width = Market.THUMBNAIL_SIZE;
    	int image_height = Market.THUMBNAIL_SIZE;
    	
    	
    	String full_url = null;
    	
    	// This method does not operate on categories:
    	if (wikipedia_search) {
    		

        	String query_url = null;
			try {
				query_url = "api.php?action=query&format=xml&generator=images&prop=imageinfo&iiprop=url&iiurlwidth=" +
				image_width +
				"&iiurlheight=" +
				image_height +
				"&gimlimit=" +
				max_results +
				"&redirects&titles=" +
				URLEncoder.encode(prefixed_taxon_name, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}


    		full_url = WIKIPEDIA_REQUEST_BASE + query_url;
    				

	    	
    	} else {

    		

	    	// This method operates on categories:
        	String query_url = null;
			try {
				query_url = "api.php?action=query&format=xml&redirects&generator=categorymembers&gcmnamespace=" + IMAGE_NAMESPACE_INDEX +
				"&prop=imageinfo&iiprop=url" +
				"&iiurlwidth=" + image_width +	
				"&iiurlheight=" + image_height + // NOTE: iiurlheight can only be used if iiurlwidth is present!
				"&gcmlimit=" +
				max_results +
				"&gcmtitle=" + URLEncoder.encode(prefixed_taxon_name, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			full_url = COMMONS_REQUEST_BASE + query_url;
    	}



//    	Log.i(TAG, "Image query URL: " + full_url);
        
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

    	
    	
    	final List<ThumbnailUrlPlusLinkContainer> thumbnail_url_list = new ArrayList<ThumbnailUrlPlusLinkContainer>();
    	
        RootElement root = new RootElement( "api" );
        Element query_response = root.getChild( "query");
        Element page_container = query_response.getChild( "pages");
        Element page_element = page_container.getChild( "page");
        Element ii_container = page_element.getChild( "imageinfo");
        Element ii_element = ii_container.getChild( "ii");
        
        
        
        final PendingWrapper wrapper = new PendingWrapper();
        
        
        page_element.setStartElementListener(new StartElementListener() {

			public void start(Attributes attributes) {
				
				String page_title = attributes.getValue( "title" );
//				Log.d(TAG, "Image title: " + page_title);
				

				
				
				if (blacklist_enabled) {
					skip_page = searchable_image_blacklist.contains( page_title );
				}
				
				if (!skip_page) {
					ThumbnailUrlPlusLinkContainer tuplc = new ThumbnailUrlPlusLinkContainer();
					tuplc.setIdentifier(page_title);
					wrapper.tuplc = tuplc;
				}
			}
        });
         
        
        
        
    	ii_element.setStartElementListener(new StartElementListener(){

			public void start(Attributes attributes) {
				if (skip_page) return;
				
				String thumnail_url = attributes.getValue( "thumburl" );
				if (thumnail_url != null && thumnail_url.length() > 0) {
					
					String description_url = attributes.getValue( "descriptionurl" );
					URL parsed_thumnail_url = null;
					try {
						parsed_thumnail_url = new URL(thumnail_url);
						
						wrapper.tuplc.setThumbnailUrl(parsed_thumnail_url);
						wrapper.tuplc.setLink(description_url);
						
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					
					wrapper.tuplc.setSource(wikipedia_search ? CollectionSource.WIKIPEDIA : CollectionSource.COMMONS);

					thumbnail_url_list.add( wrapper.tuplc );
				}				
			}
        });
        
        
        
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
        	Log.w(TAG, "Caught generic exception:");
        	e.printStackTrace();
        	throw new NetworkUnavailableException();	
//            throw new RuntimeException(e);
        } catch (AssertionError e) {
        	
        	Log.e(TAG, "No network access.");
        	Log.e(TAG, "Message: " + e.getCause().getMessage());
        	
        	throw new NetworkUnavailableException();
        }
         
        
       return thumbnail_url_list;
       
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    // =================================================================
    

    public static class StringHolder {
    	public String content;
    }
    
    public static class TitleDescription {
    	public String title, description;
    }
    
    public static TitleDescription getWikipediaBlurb(String article_title) throws NetworkUnavailableException {
    	
    	String full_url = null;
    	
    	final int blurb_section = 0;
    	
    	String query_url = null;
		try {

			query_url = "api.php?action=query&format=xml" +
			"&prop=revisions&rvgeneratexml&rvprop=content&rvsection=" + blurb_section +
			"&redirects&titles=" +
			URLEncoder.encode(article_title, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	
		full_url = WIKIPEDIA_REQUEST_BASE + query_url;


    	Log.i(TAG, "Blurb query URL: " + full_url);

        URL url = null;
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		

        final TitleDescription string_holder = new TitleDescription();
		
        RootElement root = new RootElement( "api" );
        Element query_response = root.getChild( "query");
        Element page_container = query_response.getChild( "pages");
        Element page_element = page_container.getChild( "page");
        page_element.setStartElementListener(new StartElementListener(){

			public void start(Attributes attributes) {

				String parsetree_text = attributes.getValue( "title" );	
		        
//		        if (parsetree_text != null && parsetree_text.length() > 0)
				string_holder.title = parsetree_text;

			}
        });
        
        Element ii_container = page_element.getChild( "revisions");
        Element ii_element = ii_container.getChild( "rev");
        

        
    	ii_element.setStartElementListener(new StartElementListener(){

			public void start(Attributes attributes) {

				String parsetree_text = attributes.getValue( "parsetree" );	
		        
//		        if (parsetree_text != null && parsetree_text.length() > 0)
				string_holder.description = parsetree_text;

			}
        });
        
        
        
        try {
        	
            Xml.parse(getInputStream(url), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (ParseException e) {
        	// FIXME: Sometimes we get a "ParseException: no element found" error.
        	Log.e(TAG, "Caught parsing exception in ITIS response...");

        	throw new NetworkUnavailableException();
		} catch (UnknownHostException e) {

			Log.e(TAG, "Caught UnknownHostException.");
        	throw new NetworkUnavailableException();
			
        } catch (Exception e) {

        	Log.w(TAG, "Caught generic exception:");
        	e.printStackTrace();
        	
        	throw new NetworkUnavailableException();	
//            throw new RuntimeException(e);
        } catch (AssertionError e) {
        	
        	Log.e(TAG, "No network access.");
        	Log.e(TAG, "Message: " + e.getCause().getMessage());
        	
        	throw new NetworkUnavailableException();
        }
         

		return string_holder;
    }
    
    public static String extractBlurbContent(String preparsed_stuff) throws NetworkUnavailableException {



		try {
		    /* Get a SAXParser from the SAXPArserFactory. */
		    SAXParserFactory spf = SAXParserFactory.newInstance();
		    SAXParser sp;
			sp = spf.newSAXParser();
			
		    /* Get the XMLReader of the SAXParser we created. */
		    XMLReader xr = sp.getXMLReader();
		    /* Create a new ContentHandler and apply it to the XML-Reader*/
		    WikiBlurbHandler myExampleHandler = new WikiBlurbHandler();
		    xr.setContentHandler(myExampleHandler);
		    
		    xr.parse(new InputSource(new StringReader(preparsed_stuff))); 
		    
		    return myExampleHandler.getBlurb();
		    
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    
    
    // TODO
    public static String extractTemplateValues(String preparsed_stuff) throws NetworkUnavailableException {


        RootElement root = new RootElement( "root" );
        Element template_container = root.getChild( "template");
        Element template_part = template_container.getChild( "part");

        Element template_part_name = template_part.getChild( "name");
        Element template_part_value = template_part.getChild( "value");
        

        final StringHolder string_holder = new StringHolder();
        root.setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				string_holder.content = body;
			}
        });
        
        
        try {
        	
            Xml.parse(preparsed_stuff, root.getContentHandler());
        } catch (ParseException e) {
        	// FIXME: Sometimes we get a "ParseException: no element found" error.
        	Log.e(TAG, "Caught parsing exception in ITIS response...");

        	throw new NetworkUnavailableException();

        } catch (Exception e) {
        	Log.w(TAG, "Caught generic exception:");
        	e.printStackTrace();
        	throw new NetworkUnavailableException();	
//            throw new RuntimeException(e);
        } catch (AssertionError e) {
        	
        	Log.e(TAG, "No network access.");
        	Log.e(TAG, "Message: " + e.getCause().getMessage());
        	
        	throw new NetworkUnavailableException();
        }

		return string_holder.content;
    }
    
    // TODO: Move to shared library (duplicated in WiCat)
    public static String renderWikipediaBlurb(String wikitext) throws NetworkUnavailableException {
    	
    	String full_url = null;

    	String query_url = null;
		try {

			query_url = "api.php?action=parse&format=xml&prop=text&text=" +
			URLEncoder.encode(wikitext, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	
		full_url = WIKIPEDIA_REQUEST_BASE + query_url;


    	Log.i(TAG, "Blurb render URL: " + full_url);

        URL url = null;
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

        RootElement root = new RootElement( "api" );
        Element query_response = root.getChild( "parse");
        Element page_container = query_response.getChild( "text");


        final StringHolder string_holder = new StringHolder();
        page_container.setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				string_holder.content = body;
			}
        });
        
        
        try {
        	
            Xml.parse(getInputStream(url), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (ParseException e) {
        	// FIXME: Sometimes we get a "ParseException: no element found" error.
        	Log.e(TAG, "Caught parsing exception in ITIS response...");

        	throw new NetworkUnavailableException();
		} catch (UnknownHostException e) {

			Log.e(TAG, "Caught UnknownHostException.");
        	throw new NetworkUnavailableException();
			
        } catch (Exception e) {
        	Log.w(TAG, "Caught generic exception:");
        	e.printStackTrace();
        	
        	throw new NetworkUnavailableException();	
//            throw new RuntimeException(e);
        } catch (AssertionError e) {
        	
        	Log.e(TAG, "No network access.");
        	Log.e(TAG, "Message: " + e.getCause().getMessage());
        	
        	throw new NetworkUnavailableException();
        }
         

		return string_holder.content;
    }
    
    // =================================================================
    

    public static class URLholder {
    	public URL url;
    }
    
    public static URLholder getSingleThumbnailUrl(String image_title, int image_width, int image_height, boolean wikipedia_search) throws NetworkUnavailableException {
    	
    	String full_url = null;
    	
    	
    	String query_url = null;
		try {
			query_url = "api.php?action=query&format=xml&prop=imageinfo&iiprop=url&iiurlwidth=" +
			image_width +
			"&redirects&titles=" +
			URLEncoder.encode(image_title, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	
    	if (wikipedia_search) {
    		full_url = WIKIPEDIA_REQUEST_BASE + query_url;
    	} else {
			full_url = COMMONS_REQUEST_BASE + query_url;
    	}



//    	Log.i(TAG, "Image query URL: " + full_url);

        URL url = null;
        try {
			url = new URL(full_url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

        RootElement root = new RootElement( "api" );
        Element query_response = root.getChild( "query");
        Element page_container = query_response.getChild( "pages");
        Element page_element = page_container.getChild( "page");
        Element ii_container = page_element.getChild( "imageinfo");
        Element ii_element = ii_container.getChild( "ii");
        

        final URLholder url_holder = new URLholder();
        
    	ii_element.setStartElementListener(new StartElementListener(){

			public void start(Attributes attributes) {

				String retrieved_thumnail_url = attributes.getValue( "thumburl" );	
		        
		        if (retrieved_thumnail_url != null && retrieved_thumnail_url.length() > 0) {
					try {
						url_holder.url = new URL(retrieved_thumnail_url);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}	
		        }
			}
        });
        
        
        
        try {
        	
            Xml.parse(getInputStream(url), Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (ParseException e) {
        	// FIXME: Sometimes we get a "ParseException: no element found" error.
        	Log.e(TAG, "Caught parsing exception in ITIS response...");

        	throw new NetworkUnavailableException();
		} catch (UnknownHostException e) {

			Log.e(TAG, "Caught UnknownHostException.");
        	throw new NetworkUnavailableException();
			
        } catch (Exception e) {
        	Log.w(TAG, "Caught generic exception:");
        	e.printStackTrace();
        	
        	throw new NetworkUnavailableException();	
//            throw new RuntimeException(e);
        } catch (AssertionError e) {
        	
        	Log.e(TAG, "No network access.");
        	Log.e(TAG, "Message: " + e.getCause().getMessage());
        	
        	throw new NetworkUnavailableException();
        }
         

		return url_holder;
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
    public static InputStream fetch(String urlString) throws MalformedURLException, IOException {
       	DefaultHttpClient httpClient = new DefaultHttpClient();
       	HttpGet request = new HttpGet(urlString);
       	HttpResponse response = httpClient.execute(request);
       	return response.getEntity().getContent();
    }
}

