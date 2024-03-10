package org.vaadin.tatu.vaadincreate.i18n;

import com.vaadin.ui.Component;

/**
 * Mixin interface for the views that need translation. Has default
 * implementation of getTranslation method.
 */
public interface HasI18N extends Component {

    /**
     * Get translation with given key and parameter from I18NProvider. Locale is
     * fetched from a "language" Cookie and using system Locale as fallback.
     *
     * @see I18NProvider
     *
     * @param key
     *            Key for the translation
     * @param params
     *            Optional parameters
     * @return String composed with Locale.
     */
    public default String getTranslation(String key, Object... params) {
        return I18NProvider.getInstance().getTranslation(key,
                I18NProvider.getCurrentSupportedLocale(), params);
    }

}
