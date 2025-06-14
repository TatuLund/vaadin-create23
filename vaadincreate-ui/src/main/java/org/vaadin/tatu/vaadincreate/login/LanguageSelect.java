package org.vaadin.tatu.vaadincreate.login;

import java.util.Locale;

import org.jspecify.annotations.NullMarked;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;
import org.vaadin.tatu.vaadincreate.i18n.HasI18N;
import org.vaadin.tatu.vaadincreate.i18n.I18n;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.ComboBox;

@NullMarked
@SuppressWarnings({ "serial", "java:S2160", "java:S110" })
public class LanguageSelect extends ComboBox<Locale> implements HasI18N {

    public LanguageSelect() {
        setCaption(getTranslation(I18n.Select.LANGUAGE));
        setItems(DefaultI18NProvider.getInstance().getLocales());
        setItemCaptionGenerator(item -> getTranslation(item.getLanguage()));
        setItemIconGenerator(locale -> new ThemeResource(
                String.format("flags/%s.gif", locale.getLanguage())));
        setEmptySelectionAllowed(false);
        setTextInputAllowed(false);
        addValueChangeListener(valueChange -> getDataProvider().refreshAll());
    }
}
