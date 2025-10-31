package org.vaadin.tatu.vaadincreate.i18n;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
@SuppressWarnings("serial")
public class DefaultI18NProvider implements I18NProvider {

    private static final Logger logger = LoggerFactory
            .getLogger(DefaultI18NProvider.class);

    private static final String BUNDLE_PREFIX = "translate";

    public static final Locale LOCALE_FI = Locale.of("fi", "FI");
    public static final Locale LOCALE_EN = Locale.UK;
    public static final Locale LOCALE_DE = Locale.GERMANY;
    public static final Locale LOCALE_SV = Locale.of("sv", "SE");

    public static final List<Locale> locales = Collections.unmodifiableList(
            Arrays.asList(LOCALE_FI, LOCALE_EN, LOCALE_DE, LOCALE_SV));

    protected static I18NProvider instance;

    public static synchronized I18NProvider getInstance() {
        if (instance == null) {
            instance = new DefaultI18NProvider();
        }
        return instance;
    }

    @Override
    public List<Locale> getLocales() {
        return locales;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
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

}
