package org.vaadin.tatu.vaadincreate.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jsoup.Jsoup;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.VaadinCreateTheme;
import org.vaadin.tatu.vaadincreate.backend.data.Availability;
import org.vaadin.tatu.vaadincreate.common.NumberField;
import org.vaadin.tatu.vaadincreate.crud.form.AvailabilitySelector;

import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class BooksViewResponsiveTest extends AbstractBooksViewTest {

    @Test
    @SuppressWarnings({ "unchecked", "java:S5961" })
    public void tooltip_in_narrow_mode_and_edited_field_has_content_sanitized_to_prevent_xss() {
        // WHEN: Making window small in order to show tooltip
        ui.getPage().updateBrowserWindowSize(500, 1024, true);

        // WHEN: Creating a book with offending content
        test($(view, Button.class).id("new-product")).click();
        test($(form, TextField.class).id("product-name")).setValue(
                "<b><img src=1 onerror=alert(document.domain)>A new book</b>");
        test($(form, TextField.class).id("price")).setValue("10.0 €");
        test($(form, AvailabilitySelector.class).id("availability"))
                .clickItem(Availability.AVAILABLE);
        test($(form, NumberField.class).id("stock-count")).setValue(10);

        var cat = ui.getProductService().getAllCategories().stream().findFirst()
                .get();
        test($(form, CheckBoxGroup.class).id("category")).clickItem(cat);

        // WHEN: Clicking save button
        test($(form, Button.class).id("save-button")).click();

        // THEN: Form is closed and the product is on the last row
        assertFalse(form.isShown());

        int row = test(grid).size() - 1;

        // THEN: The JS sanitized away and the text content remain
        assertFalse(test(grid).description(row).contains("alert"));
        assertTrue(test(grid).description(row).contains("A new book"));

        // WHEN: Clicking the row
        test(grid).click(1, row);
        var id = test(grid).item(row).getId();
        assertNotNull(id);

        // THEN: Form is shown
        assertTrue(form.isShown());

        // WHEN: Editing the book and clicking cancel button
        test($(form, TextField.class).id("product-name"))
                .setValue("The new book");
        test($(form, Button.class).id("cancel-button")).click();

        // THEN: Confirm dialog is shown
        var dialog = $(Window.class).id("confirm-dialog");

        // WHEN: Canceling the cancel operation
        test($(dialog, Button.class).id("cancel-button")).click();

        // THEN: Form is still shown and field is dirty and has tooltip
        // with the original content where JS is sanitized and text content
        // remain
        assertTrue($(form, TextField.class).id("product-name").getStyleName()
                .contains(VaadinCreateTheme.BOOKFORM_FIELD_DIRTY));
        assertFalse($(form, TextField.class).id("product-name").getDescription()
                .contains("alert"));
        assertTrue($(form, TextField.class).id("product-name").getDescription()
                .contains("A new book"));

        // WHEN: Clicking discard button
        test($(form, Button.class).id("discard-button")).click();

        // THEN: Cancel button is clicked and form is closed without
        // showing the confirm dialog
        test($(form, Button.class).id("cancel-button")).click();
        assertFalse(form.isShown());

        // Cleanup
        ui.getProductService().deleteProduct(id);
    }

    @SuppressWarnings("java:S5961")
    @Test
    public void different_columns_are_shown_based_on_browser_window_width() {
        // WHEN: Making window large
        ui.getPage().updateBrowserWindowSize(1600, 1024, true);

        // THEN: All columns are shown and no description is shown
        grid.getColumns().forEach(col -> {
            assertFalse(col.isHidden());
        });
        // THEN: Aria label is set for stock count cell when stock count is
        // zero.
        assertEquals(null, test(grid).description(0));
        var html = Jsoup.parse((String) test(grid).cell(4, 0));
        assertEquals("-", html.getElementsByTag("span").get(0).text());
        assertEquals("0",
                html.getElementsByTag("span").get(0).attr("aria-label"));

        // WHEN: Making window smaller
        ui.getPage().updateBrowserWindowSize(1200, 1024, true);

        // THEN: First and last columns are hidden and no description is
        // shown
        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertFalse(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        assertEquals(null, test(grid).description(0));

        // WHEN: Making window even smaller
        ui.getPage().updateBrowserWindowSize(900, 1024, true);

        // THEN: First, fith and last columns are hidden and description
        // is still not shown
        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertFalse(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        assertEquals(null, test(grid).description(0));

        // WHEN: Making window even smaller
        ui.getPage().updateBrowserWindowSize(700, 1024, true);

        // THEN: Stock count is shown in description
        assertEquals("Coming", test(grid).description(3, 0));
        assertEquals("Available: 378", test(grid).description(3, 1));

        // WHEN: Making window even smaller
        ui.getPage().updateBrowserWindowSize(500, 1024, true);

        // THEN: Only second and third columns are shown and description
        // is shown
        assertTrue(grid.getColumns().get(0).isHidden());
        assertFalse(grid.getColumns().get(1).isHidden());
        assertFalse(grid.getColumns().get(2).isHidden());
        assertTrue(grid.getColumns().get(3).isHidden());
        assertTrue(grid.getColumns().get(4).isHidden());
        assertTrue(grid.getColumns().get(5).isHidden());

        var doc = Jsoup.parse(test(grid).description(0));
        var book = test(grid).item(0);
        assertEquals(book.getProductName(),
                doc.getElementsByTag("b").get(0).text());
        assertEquals(1, doc.getElementsByClass("v-icon").size());
    }
}
