package org.vaadin.tatu.vaadincreate.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.i18n.DefaultI18NProvider;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.ServiceException;
import com.vaadin.testbench.uiunittest.UIUnitTest;
import com.vaadin.ui.UI;

public class LanguageSelectTest extends UIUnitTest {

    UI ui;
    LanguageSelect languageSelect;

    @Before
    public void setUp() throws ServiceException {
        Locale.setDefault(Locale.of("en", "GB"));
        ui = mockVaadin();
        languageSelect = new LanguageSelect();
        languageSelect.addValueChangeListener(valueChange -> {
            Locale.setDefault(valueChange.getValue());
        });
        ui.setContent(languageSelect);
    }

    @After
    public void cleanUp() {
        tearDown();
    }

    @Test
    public void testLanguageSelect() {
        // Assert that the initial value is correct
        assertNull(languageSelect.getValue());
        // Assert that caption is set correctly
        assertEquals("Language", languageSelect.getCaption());

        @SuppressWarnings("unchecked")
        var items = (ListDataProvider<Locale>) languageSelect.getDataProvider();
        // Assert that the items in the data provider match the locales
        assertEquals(DefaultI18NProvider.getInstance().getLocales(),
                items.getItems());

        // Expected captions for each locale
        Map<Locale, String> expectedCaptions = Map.of(Locale.of("fi", "FI"),
                "Finnish", Locale.of("en", "GB"), "English",
                Locale.of("de", "DE"), "German", Locale.of("sv", "SE"),
                "Swedish");
        // Assert that item captions are set correctly
        items.getItems().forEach(item -> {
            assertEquals(expectedCaptions.get(item),
                    languageSelect.getItemCaptionGenerator().apply(item));
        });
    }

    @Test
    public void testLanguageSelectWithDifferentValue() {
        // Change the selected locale to Finnish
        languageSelect.setValue(Locale.of("fi", "FI"));

        // Assert that the selected value is now Finnish
        assertEquals("fi", languageSelect.getValue().getLanguage());

        // Assert that item captions are set correctly
        @SuppressWarnings("unchecked")
        var items = (ListDataProvider<Locale>) languageSelect.getDataProvider();
        Map<Locale, String> expectedCaptions = Map.of(Locale.of("fi", "FI"),
                "Suomi", Locale.of("en", "GB"), "Englanti",
                Locale.of("de", "DE"), "Saksa", Locale.of("sv", "SE"),
                "Ruotsi");
        items.getItems().forEach(item -> {
            assertEquals(expectedCaptions.get(item),
                    languageSelect.getItemCaptionGenerator().apply(item));
        });
    }
}
