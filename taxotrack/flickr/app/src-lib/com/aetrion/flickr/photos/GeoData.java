package com.aetrion.flickr.photos;

/**
 * A geographic position.
 *
 * @author mago
 * @version $Id: GeoData.java,v 1.3 2009/07/12 22:43:07 x-mago Exp $
 */
public class GeoData {
	private static final long serialVersionUID = 12L;
    private float longitude;
    private float latitude;
    private int accuracy;

    public GeoData() {
        super();
    }

    public GeoData(String longitudeStr, String latitudeStr, String accuracyStr) {
        longitude = Float.parseFloat(longitudeStr);
        latitude = Float.parseFloat(latitudeStr);
        accuracy = Integer.parseInt(accuracyStr);
    }

    public int getAccuracy() {
        return accuracy;
    }

    /**
     * Set the accuracy level.<p>
     *
     * World level is 1, Country is ~3, Region ~6, City ~11, Street ~16.
     *
     * @param accuracy
     * @see com.aetrion.flickr.Flickr#ACCURACY_WORLD
     * @see com.aetrion.flickr.Flickr#ACCURACY_COUNTRY
     * @see com.aetrion.flickr.Flickr#ACCURACY_REGION
     * @see com.aetrion.flickr.Flickr#ACCURACY_CITY
     * @see com.aetrion.flickr.Flickr#ACCURACY_STREET
     */
    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String toString() {
        return "GeoData[longitude=" + longitude +
        " latitude=" + latitude + " accuracy=" + accuracy + "]";
    }

}
