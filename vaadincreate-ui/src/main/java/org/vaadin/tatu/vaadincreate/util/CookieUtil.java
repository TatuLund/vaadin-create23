package org.vaadin.tatu.vaadincreate.util;

import javax.servlet.http.Cookie;

import com.vaadin.server.VaadinRequest;

public class CookieUtil {

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
        }
        return null;
    }

}