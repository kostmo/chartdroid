/*
 * Copyright (c) 2005 Aetrion LLC.
 */

package com.aetrion.flickr.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.aetrion.flickr.Parameter;
import com.aetrion.flickr.ParameterAlphaComparator;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.util.ByteUtilities;

/**
 * Utilities used by the authentication API.
 *
 * @author Anthony Eden
 * @version $Id: AuthUtilities.java,v 1.10 2008/07/09 21:13:59 x-mago Exp $
 */
public class AuthUtilities {

    /**
     * Get a signature for a list of parameters using the shared secret from the RequestContext.
     *
     * @param parameters The parameters
     * @return The signature String
     * @deprecated
     */
    public static String getSignature(List parameters) {
        RequestContext requestContext = RequestContext.getRequestContext();
        return getSignature(requestContext.getSharedSecret(), parameters);
    }

    /**
     * 
     * @param parameters
     * @return The signature String
     * @deprecated
     */
    public static String getMultipartSignature(List parameters) {
        RequestContext requestContext = RequestContext.getRequestContext();
        return getMultipartSignature(requestContext.getSharedSecret(), parameters);
    }

    /**
     * Get a signature for a list of parameters using the given shared secret.
     *
     * @param sharedSecret The shared secret
     * @param params The parameters
     * @return The signature String
     */
    public static String getSignature(String sharedSecret, List params) {
        addAuthToken(params);

        StringBuffer buffer = new StringBuffer();
        buffer.append(sharedSecret);
        Collections.sort(params, new ParameterAlphaComparator());
        Iterator iter = params.iterator();
        while (iter.hasNext()) {
            Parameter param = (Parameter) iter.next();
            buffer.append(param.getName());
            buffer.append(param.getValue());
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return ByteUtilities.toHexString(md.digest(buffer.toString().getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException u) {
            throw new RuntimeException(u);
        }
    }

    public static String getMultipartSignature(String sharedSecret, List params) {
        List ignoreParameters = new ArrayList();
        ignoreParameters.add("photo");

        addAuthToken(params);

        StringBuffer buffer = new StringBuffer();
        buffer.append(sharedSecret);
        Collections.sort(params, new ParameterAlphaComparator());
        Iterator iter = params.iterator();
        while (iter.hasNext()) {
            Parameter param = (Parameter) iter.next();
            if (!ignoreParameters.contains(param.getName().toLowerCase())) {
                buffer.append(param.getName());
                buffer.append(param.getValue());
            }
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return ByteUtilities.toHexString(md.digest(buffer.toString().getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException u) {
            throw new RuntimeException(u);
        }
    }

    /**
     * Adds the auth_token to the parameter list if it is necessary.
     * @param params
     */
    public static void addAuthToken(List params) {
        //Checking for the auth_token parameter
        Iterator it = params.iterator();
        boolean tokenFlag = false;
        while (it.hasNext()) {
            if (((Parameter) it.next()).getName().equals("auth_token")) {
                tokenFlag = true;
            }
        }

        if (!tokenFlag) {
        	
            if (RequestContext.getRequestContext().getAuth() != null) {
                String authToken = RequestContext.getRequestContext().getAuth().getToken();
                if(authToken != null && !authToken.equals(""))
                    params.add(new Parameter("auth_token", authToken));
            }
        }
    }

}
