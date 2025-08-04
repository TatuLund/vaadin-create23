package org.vaadin.tatu.vaadincreate.util;

import javax.servlet.http.Cookie;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.server.VaadinRequest;

@NullMarked
public class CookieUtils {

    public static final String COOKIE_LANGUAGE = "language";

    private CookieUtils() {
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
    @Nullable
    public static Cookie getCookieByName(String name, VaadinRequest request) {
        // Fetch all cookies from the request
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }
        // Iterate to find cookie by its name
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        // Cookie not found
        return null;
    }

}