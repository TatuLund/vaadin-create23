package org.vaadin.tatu.vaadincreate.i18n;

import org.junit.Assert;
import org.junit.Test;
import com.vaadin.testbench.uiunittest.UIUnitTest;

import com.vaadin.server.ServiceException;
import com.vaadin.ui.Label;

public class Has18NTest extends UIUnitTest {

    @Test
    public void testLocalization() throws ServiceException {
        var ui = mockVaadin();

        var session = ui.getSession();

        session.setLocale(DefaultI18NProvider.LOCALE_EN);
        var label = new LocalizedLabel("save");
        Assert.assertEquals("Save", label.getValue());

        session.setLocale(DefaultI18NProvider.LOCALE_FI);
        label = new LocalizedLabel("save");
        Assert.assertEquals("Tallenna", label.getValue());
        tearDown();
    }

    @SuppressWarnings("serial")
    public static class LocalizedLabel extends Label implements HasI18N {

        public LocalizedLabel(String key) {
            super();
            setValue(getTranslation(key));
        }
    }
}
