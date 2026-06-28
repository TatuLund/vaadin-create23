package org.vaadin.tatu.vaadincreate;

import javax.servlet.http.Cookie;

import org.vaadin.tatu.vaadincreate.util.CookieUtils;

import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;

class LocaleBootstrapListener implements BootstrapListener {
    @Override
    public void modifyBootstrapPage(BootstrapPageResponse response) {
        Cookie localeCookie = CookieUtils.getCookieByName(
                CookieUtils.COOKIE_LANGUAGE, response.getRequest());
        response.getDocument().getElementsByTag("html").get(0).attributes()
                .add("lang", localeCookie != null ? localeCookie.getValue()
                        : "en");
        var contextPath = response.getRequest().getContextPath();
        var manifestPath = contextPath + "/manifest.webmanifest";
        var swPath = contextPath + "/sw.js";
        var icon192 = contextPath + "/icons/icon-192.png";
        var icon512 = contextPath + "/icons/icon-512.png";

        var head = response.getDocument().head();
        head.appendElement("link").attr("rel", "manifest").attr("href",
                manifestPath);
        head.appendElement("meta").attr("name", "theme-color")
                .attr("content", "#0b5fff");
        head.appendElement("meta").attr("name", "mobile-web-app-capable")
                .attr("content", "yes");
        head.appendElement("meta")
                .attr("name", "apple-mobile-web-app-capable")
                .attr("content", "yes");
        head.appendElement("link").attr("rel", "apple-touch-icon")
                .attr("href", icon192);
        head.appendElement("link").attr("rel", "icon")
                .attr("sizes", "192x192").attr("href", icon192);
        head.appendElement("link").attr("rel", "icon")
                .attr("sizes", "512x512").attr("href", icon512);

        head.appendElement("script")
                .append("""
                        if ('serviceWorker' in navigator) {
                            window.addEventListener('load', function() {
                                navigator.serviceWorker.register('%s')
                                    .catch(function(error) { console.error(error); });

                                var returnUrlKey = 'pwa-return-url';
                                var offlinePath = '%s/offline.html';
                                var appPath = '%s/';

                                window.addEventListener('offline', function() {
                                    if (window.location.pathname !== offlinePath) {
                                        try {
                                            sessionStorage.setItem(returnUrlKey,
                                                window.location.href);
                                        } catch (ignore) {
                                            // No-op
                                        }
                                        window.location.href = offlinePath;
                                    }
                                });

                                window.addEventListener('online', function() {
                                    if (window.location.pathname === offlinePath) {
                                        var returnUrl = '';
                                        try {
                                            returnUrl = sessionStorage.getItem(returnUrlKey)
                                                || '';
                                        } catch (ignore) {
                                            // No-op
                                        }
                                        if (returnUrl.indexOf(offlinePath) !== -1) {
                                            returnUrl = '';
                                        }
                                        window.location.href = returnUrl || appPath;
                                    }
                                });

                                if (!navigator.onLine
                                        && window.location.pathname !== offlinePath) {
                                    try {
                                        sessionStorage.setItem(returnUrlKey,
                                            window.location.href);
                                    } catch (ignore) {
                                        // No-op
                                    }
                                    window.location.href = offlinePath;
                                }
                            });
                        }
                        """
                        .formatted(swPath, contextPath, contextPath));
    }

    @Override
    public void modifyBootstrapFragment(
            BootstrapFragmentResponse response) {
        // No-op: not needed in this case
    }
}
