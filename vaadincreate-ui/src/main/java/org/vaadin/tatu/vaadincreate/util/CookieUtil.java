package org.vaadin.tatu.vaadincreate.util;

import javax.servlet.http.Cookie;

import com.vaadin.server.VaadinRequest;

public class CookieUtil {

    private CookieUtil() {
        // private constructor to hide the implicit public one
    }

    /**
     * Retrieves a cookie by its name from the given VaadinRequest.
     *
     * @param name
     *            the name of the cookie to retrieve
     * @param request
     *            the VaadinRequest from which to retrieve the cookie
     * @return the Cookie object with the specified name, or null if not found
     */
    public static Cookie getCookieByName(String name, VaadinRequest request) {
        // Fetch all cookies from the request
        Cookie[] cookies = request.getCookies();

        // Iterate to find cookie by its name
        try {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        } catch (NullPointerException e) {
            // We return null if not found.
        }
        return null;
    }

}