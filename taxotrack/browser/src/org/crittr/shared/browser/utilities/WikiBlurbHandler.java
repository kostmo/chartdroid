package org.crittr.shared.browser.utilities;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WikiBlurbHandler extends DefaultHandler {

    // ===========================================================
    // Fields
    // ===========================================================
    
	private int template_depth = 0;
	private int extra_depth = 0;
//	private int paragraph_depth = 0;	// NOTE: <p> tags don't exist yet.
//	private int paragraph_count = 0;
    

    private StringBuilder blurb;

    public String getBlurb() {
    	return this.blurb.toString();
    }
    
    // ===========================================================
    // Methods
    // ===========================================================
    @Override
    public void startDocument() throws SAXException {
    	
    	this.blurb = new StringBuilder();
    }

    @Override
    public void endDocument() throws SAXException {
         // Nothing to do
    }

    /** Gets be called on opening tags like:
     * <tag>
     * Can provide attribute(s), when xml was like:
     * <tag attribute="attributeValue">*/
    @Override
    public void startElement(String namespaceURI, String localName,
              String qName, Attributes atts) throws SAXException {
         if (localName.equals("template")) {
        	 this.template_depth++;
         } else if (localName.equals("ext")) {
           	 this.extra_depth++;
        }
    }
    
    /** Gets be called on closing tags like:
     * </tag> */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
              throws SAXException {
        if (localName.equals("template")) {
       	 this.template_depth--;
        } else if (localName.equals("ext")) {
       	 this.extra_depth--;
        }
    }
    
    /** Gets be called on the following structure:
     * <tag>characters</tag> */
    @Override
   public void characters(char ch[], int start, int length) {
         if (this.template_depth <= 0 && this.extra_depth <= 0)
             this.blurb.append(ch, start, length);
   }
}
