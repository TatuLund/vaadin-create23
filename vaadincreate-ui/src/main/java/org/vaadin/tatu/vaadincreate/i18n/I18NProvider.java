package org.vaadin.tatu.vaadincreate.i18n;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.util.CookieUtil;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;

@SuppressWarnings("serial")
public class I18NProvider implements Serializable {

    private static final String BUNDLE_PREFIX = "translate";

    public static final Locale LOCALE_FI = new Locale("fi", "FI");
    public static final Locale LOCALE_EN = new Locale("en", "GB");

    public static List<Locale> locales = Collections
            .unmodifiableList(Arrays.asList(LOCALE_FI, LOCALE_EN));

    private static I18NProvider INSTANCE;

    public synchronized static I18NProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new I18NProvider();
        }
        return INSTANCE;
    }

    public List<Locale> getLocales() {
        return locales;
    }

    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            logger.warn("Got lang request for key with null value!");
            return "";
        }

        ResourceBundle bundle;
        if (getLocales().contains(locale)) {
            bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        } else {
            bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, LOCALE_EN);
        }

        String value;
        try {
            value = bundle.getString(key);
        } catch (final MissingResourceException e) {
            logger.warn("Missing resource", e);
            return "!" + locale.getLanguage() + ": " + key;
        }
        if (params.length > 0) {
            value = MessageFormat.format(value, params);
        }
        return value;
    }

    public static Locale fetchLocaleFromCookie() {
        var request = VaadinRequest.getCurrent();
        Locale locale = null;
        // First try to find locale Cookie
        if (request != null) {
            Cookie localeCookie = CookieUtil.getCookieByName("language",
                    request);
            if (localeCookie != null && localeCookie.getValue() != null) {
                String lang = localeCookie.getValue();
                Optional<Locale> localeFromCookie = getSupportedLocale(lang);
                if (localeFromCookie.isPresent()) {
                    locale = localeFromCookie.get();
                }
            }
            logger.info("Found locale '{}' from cookie", locale);
        }
        return locale;
    }

    public static Locale getCurrentSupportedLocale() {
        return getSupportedLocale(
                VaadinSession.getCurrent().getLocale().getLanguage())
                        .orElse(LOCALE_EN);
    }

    private static Optional<Locale> getSupportedLocale(String lang) {
        Optional<Locale> locale = I18NProvider.getInstance().getLocales()
                .stream().filter(loc -> loc.getLanguage().equals(lang))
                .findFirst();
        return locale;
    }

    private static Logger logger = LoggerFactory.getLogger(I18NProvider.class);
}
