package org.vaadin.tatu.vaadincreate.i18n;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.MockVaadinSession;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Label;

public class Has18NTest {

    @Test
    public void testLocalization() {
        var session = new MockVaadinSession(null);
        VaadinSession.setCurrent(session);
        session.lock();

        session.setLocale(I18NProvider.LOCALE_EN);
        var label = new LocalizedLabel("save");
        Assert.assertEquals("Save", label.getValue());

        session.setLocale(I18NProvider.LOCALE_FI);
        label = new LocalizedLabel("save");
        Assert.assertEquals("Tallenna", label.getValue());
    }

    @SuppressWarnings("serial")
    public static class LocalizedLabel extends Label implements HasI18N {

        public LocalizedLabel(String key) {
            super();
            setValue(getTranslation(key));
        }
    }
}
