/*
 *-------------------------------------------------------
 * (c) 2006 Das B&uuml;ro am Draht GmbH - All Rights reserved
 *-------------------------------------------------------
 */
package com.aetrion.flickr.interestingness;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.Parameter;
import com.aetrion.flickr.Response;
import com.aetrion.flickr.Transport;
import com.aetrion.flickr.auth.AuthUtilities;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotoUtils;
import com.aetrion.flickr.util.StringUtilities;

/**
 *
 * @author till
 * @version $Id: InterestingnessInterface.java,v 1.9 2009/07/11 20:30:27 x-mago Exp $
 */
public class InterestingnessInterface {

    public static final String METHOD_GET_LIST = "flickr.interestingness.getList";

    private static final String KEY_METHOD = "method";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_DATE = "date";
    private static final String KEY_EXTRAS = "extras";
    private static final String KEY_PER_PAGE = "per_page";
    private static final String KEY_PAGE = "page";

    private static final ThreadLocal DATE_FORMATS = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    private String apiKey;
    private String sharedSecret;
    private Transport transportAPI;

    public InterestingnessInterface(
        String apiKey,
        String sharedSecret,
        Transport transportAPI
    ) {
        this.apiKey = apiKey;
        this.sharedSecret = sharedSecret;
        this.transportAPI = transportAPI;
    }

    /**
     * Returns the list of interesting photos for the most recent day or a user-specified date.
     *
     * This method does not require authentication.
     *
     * @param date
     * @param extras A set of Strings controlling the extra information to fetch for each returned record. Currently supported fields are: license, date_upload, date_taken, owner_name, icon_server, original_format, last_update, geo. Set to null or an empty set to not specify any extras.
     * @param perPage The number of photos to show per page
     * @param page The page offset
     * @return PhotoList
     * @throws FlickrException
     * @throws IOException
     * @throws SAXException
     * @see com.aetrion.flickr.photos.Extras
     */
    public PhotoList getList(String date, Set extras, int perPage, int page) throws FlickrException, IOException, SAXException {
        List parameters = new ArrayList();
        PhotoList photos = new PhotoList();

        parameters.add(new Parameter(KEY_METHOD, METHOD_GET_LIST));
        parameters.add(new Parameter(KEY_API_KEY, apiKey));

        if (date != null) {
             parameters.add(new Parameter(KEY_DATE, date));
        }

        if (extras != null) {
            parameters.add(new Parameter(KEY_EXTRAS, StringUtilities.join(extras, ",")));
        }

        if (perPage > 0) {
            parameters.add(new Parameter(KEY_PER_PAGE, String.valueOf(perPage)));
        }
        if (page > 0) {
            parameters.add(new Parameter(KEY_PAGE, String.valueOf(page)));
        }

        Response response = transportAPI.get(transportAPI.getPath(), parameters);
        if (response.isError()) {
            throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
        }
        Element photosElement = response.getPayload();
        photos.setPage(photosElement.getAttribute("page"));
        photos.setPages(photosElement.getAttribute("pages"));
        photos.setPerPage(photosElement.getAttribute("perpage"));
        photos.setTotal(photosElement.getAttribute("total"));

        NodeList photoNodes = photosElement.getElementsByTagName("photo");
        for (int i = 0; i < photoNodes.getLength(); i++) {
            Element photoElement = (Element) photoNodes.item(i);
            Photo photo = PhotoUtils.createPhoto(photoElement);
            photos.add(photo);
        }
        return photos;
    }

    /**
     * 
     * @param date
     * @param extras
     * @param perPage
     * @param page
     * @return PhotoList
     * @throws FlickrException
     * @throws IOException
     * @throws SAXException
     * @see com.aetrion.flickr.photos.Extras
     */
    public PhotoList getList(Date date, Set extras, int perPage, int page)
      throws FlickrException, IOException, SAXException {
        String dateString = null;
        if (date != null) {
            DateFormat df = (DateFormat)DATE_FORMATS.get();
            dateString = df.format(date);
        }
        return getList(dateString, extras, perPage, page);
    }

    /**
     * convenience method to get the list of all 500 most recent photos
     * in flickr explore with all known extra attributes.
     *
     * @return a List of Photos
     * @throws FlickrException
     * @throws IOException
     * @throws SAXException
     */
    public PhotoList getList() throws FlickrException, IOException, SAXException {
        return getList((String) null, Extras.ALL_EXTRAS, 500, 1);
    }

}
